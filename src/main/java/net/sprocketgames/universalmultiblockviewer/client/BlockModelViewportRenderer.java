package net.sprocketgames.universalmultiblockviewer.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Axis;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.GlStateBackup;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerLayout;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

/**
 * Renders a structure into its own texture target, then copies that finished image
 * into the recipe host. The host's GuiGraphics buffer is never used for model
 * geometry or the viewport background.
 */
public final class BlockModelViewportRenderer {
    private static final Map<BlockOption, BlockState> DISPLAY_STATES = new ConcurrentHashMap<>();
    private static final int FLOOR_GRID_MARGIN = 2;
    // Direct lines stay aligned with the block transform. A three-thousandths expansion keeps
    // them from being hidden by a model face at grazing camera angles.
    private static final double OUTLINE_MIN = -0.003D;
    private static final double OUTLINE_MAX = 1.003D;
    private static TextureTarget viewportTarget;

    private BlockModelViewportRenderer() {
    }

    public static void render(ViewerState state, GuiGraphics graphics, int left, int top, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        double guiScale = minecraft.getWindow().getGuiScale();
        int targetWidth = Math.max(1, (int) Math.round(width * guiScale));
        int targetHeight = Math.max(1, (int) Math.round(height * guiScale));
        ensureTarget(targetWidth, targetHeight);

        graphics.flush();

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        GlStateBackup stateBackup = new GlStateBackup();
        RenderSystem.backupGlState(stateBackup);
        RenderSystem.backupProjectionMatrix();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            if (state.darkViewportBackground()) {
                viewportTarget.setClearColor(0.055F, 0.052F, 0.047F, 1.0F);
            } else {
                viewportTarget.setClearColor(0.847F, 0.827F, 0.784F, 1.0F); // #D8D3C8 warm stone
            }
            viewportTarget.clear(Minecraft.ON_OSX);
            // RenderTarget.clear deliberately unbinds its framebuffer afterwards.
            viewportTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, targetWidth, targetHeight);
            RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0.0F, targetWidth, targetHeight, 0.0F, 1000.0F, 3000.0F),
                VertexSorting.ORTHOGRAPHIC_Z
            );
            modelView.identity();
            modelView.translate(0.0F, 0.0F, -2000.0F);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            renderScene(state, minecraft, targetWidth, targetHeight, (float) guiScale);

            mainTarget.bindWrite(false);
        } finally {
            mainTarget.bindWrite(false);
            RenderSystem.viewport(0, 0, mainTarget.viewWidth, mainTarget.viewHeight);
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
            RenderSystem.restoreProjectionMatrix();
            RenderSystem.restoreGlState(stateBackup);
        }
        drawTargetIntoGui(graphics, left, top, width, height);
    }

    private static void drawTargetIntoGui(GuiGraphics graphics, int left, int top, int width, int height) {
        GlStateBackup stateBackup = new GlStateBackup();
        RenderSystem.backupGlState(stateBackup);
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, viewportTarget.getColorTextureId());
            var pose = graphics.pose().last();
            var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            vertices.addVertex(pose, left, top + height, 0.0F).setUv(0.0F, 0.0F);
            vertices.addVertex(pose, left + width, top + height, 0.0F).setUv(1.0F, 0.0F);
            vertices.addVertex(pose, left + width, top, 0.0F).setUv(1.0F, 1.0F);
            vertices.addVertex(pose, left, top, 0.0F).setUv(0.0F, 1.0F);
            BufferUploader.drawWithShader(vertices.buildOrThrow());
        } finally {
            RenderSystem.restoreGlState(stateBackup);
        }
    }

    private static void renderScene(ViewerState state, Minecraft minecraft, int width, int height, float guiScale) {
        int maxDimension = Math.max(state.variant().width(), Math.max(state.variant().height(), state.variant().depth()));
        float scale = Math.max(4.5F, 42.0F / maxDimension) * (float) state.zoom() * guiScale;
        try (ByteBufferBuilder sceneMemory = new ByteBufferBuilder(262_144)) {
            MultiBufferSource.BufferSource sceneBuffers = MultiBufferSource.immediate(sceneMemory);
            PoseStack pose = new PoseStack();
            pose.pushPose();
            try {
                pose.translate(width / 2.0F + (float) state.panX() * guiScale, height * 0.50F + (float) state.panY() * guiScale, 180.0F);
                pose.scale(scale, -scale, scale);
                pose.mulPose(Axis.XP.rotationDegrees(32.0F + (float) state.pitch()));
                pose.mulPose(Axis.YP.rotationDegrees(45.0F + (float) state.yaw()));
                pose.translate(-state.variant().width() / 2.0F, -state.variant().height() / 2.0F, -state.variant().depth() / 2.0F);
                for (var position : ViewerLayout.visibleCells(state)) {
                    pose.pushPose();
                    try {
                        pose.translate(position.x(), position.y(), position.z());
                        renderBlock(minecraft, resolve(state.displayedBlock(position), position), pose, sceneBuffers);
                    } finally {
                        pose.popPose();
                    }
                }
                sceneBuffers.endBatch();
                renderViewportLines(state, pose);
            } finally {
                pose.popPose();
            }
        }
    }

    private static void ensureTarget(int width, int height) {
        if (viewportTarget == null) {
            viewportTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        } else if (viewportTarget.viewWidth != width || viewportTarget.viewHeight != height) {
            viewportTarget.resize(width, height, Minecraft.ON_OSX);
        }
    }

    /**
     * The normal-driven vanilla line render type expands a line in the shader. That expansion is
     * visibly asymmetric after this viewport's mirrored, scaled transform. Direct debug lines
     * instead rasterise the exact endpoints supplied by the shared scene pose.
     */
    private static void renderViewportLines(ViewerState state, PoseStack pose) {
        var visibleCells = ViewerLayout.visibleCells(state);
        boolean hasOutline = state.selected() != null
            || state.hoveredMaterial() != null && visibleCells.stream()
                .anyMatch(position -> usesPresentationItem(state.variant().cells().get(position), state.hoveredMaterial()))
            || state.showAlternativeHighlights() && visibleCells.stream()
                .anyMatch(position -> hasMultipleDistinctOptions(state.variant().cells().get(position)))
            || visibleCells.stream().anyMatch(position -> state.variant().cells().get(position).optional());
        if (!state.showFloorGrid() && !hasOutline) {
            return;
        }
        renderSelectionOverlays(state, pose);
        if (state.showFloorGrid()) {
            var gridVertices = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            renderFloorGrid(state, pose, gridVertices);
            drawViewportLines(gridVertices, true, 1.0F);
        }
        if (!hasOutline) return;
        renderSolidOutlines(state, pose, visibleCells);

        var openOutlineVertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean hasOpenOutline = false;
        for (var position : visibleCells) {
            var requirement = state.variant().cells().get(position);
            OutlineStyle outline = outlineStyle(state, position, requirement);
            if (outline == null || !usesOpenOutline(state, position)) continue;
            addOpenBlockOutline(openOutlineVertices, pose, position, outline.red(), outline.green(), outline.blue(), outline.alpha());
            hasOpenOutline = true;
        }
        if (hasOpenOutline) {
            drawOpenOutlineRibbons(openOutlineVertices);
        }
    }

    private static OutlineStyle outlineStyle(ViewerState state, GridPos position,
                                             net.sprocketgames.universalmultiblockviewer.model.BlockRequirement requirement) {
        if (position.equals(state.selected())) {
            return new OutlineStyle(1.0F, 0.08F, 0.04F, 1.0F);
        }
        if (state.hoveredMaterial() != null && usesPresentationItem(requirement, state.hoveredMaterial())) {
            // The material-strip pointer is intentionally below an explicit selection:
            // it is a temporary inspection aid, not a change to selection state.
            return new OutlineStyle(1.0F, 0.88F, 0.08F, 1.0F);
        }
        if (state.showAlternativeHighlights() && hasMultipleDistinctOptions(requirement)) {
            return new OutlineStyle(0.63F, 0.28F, 0.88F, 0.86F);
        }
        return requirement.optional() ? new OutlineStyle(0.82F, 0.55F, 0.18F, 0.70F) : null;
    }

    /** Uses Minecraft's normal-based line renderer for stable, depth-aware solid-block edges. */
    private static void renderSolidOutlines(ViewerState state, PoseStack pose, Iterable<GridPos> visibleCells) {
        try (ByteBufferBuilder memory = new ByteBufferBuilder(65_536)) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(memory);
            var vertices = buffers.getBuffer(RenderType.lines());
            boolean hasSolidOutline = false;
            for (var position : visibleCells) {
                var requirement = state.variant().cells().get(position);
                OutlineStyle outline = outlineStyle(state, position, requirement);
                if (outline == null || usesOpenOutline(state, position)) continue;
                LevelRenderer.renderLineBox(pose, vertices,
                    position.x() + OUTLINE_MIN, position.y() + OUTLINE_MIN, position.z() + OUTLINE_MIN,
                    position.x() + OUTLINE_MAX, position.y() + OUTLINE_MAX, position.z() + OUTLINE_MAX,
                    outline.red(), outline.green(), outline.blue(), outline.alpha());
                hasSolidOutline = true;
            }
            if (hasSolidOutline) {
                buffers.endBatch(RenderType.lines());
            }
        }
    }

    /**
     * A state that cannot occlude is visually open in the world renderer. Showing every box edge
     * is useful for those shapes, while doing so for a solid cube produces distracting internal
     * lines through pillars and walls.
     */
    private static boolean usesOpenOutline(ViewerState state, GridPos position) {
        BlockOption option = state.displayedBlock(position);
        return option != null && !resolve(option, position).canOcclude();
    }

    private record OutlineStyle(float red, float green, float blue, float alpha) { }

    /**
     * Grids belong behind blocks, but a guide highlight is a complete wireframe annotation.
     * Keeping those passes separate prevents the far edges of an outlined block disappearing
     * into its own rendered faces.
     */
    private static void drawViewportLines(com.mojang.blaze3d.vertex.BufferBuilder vertices, boolean depthTest, float lineWidth) {
        if (depthTest) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(lineWidth);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(vertices.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Projected open-model ribbons are quads, so they must remain visible from either winding. */
    private static void drawOpenOutlineRibbons(com.mojang.blaze3d.vertex.BufferBuilder vertices) {
        RenderSystem.disableCull();
        try {
            drawViewportLines(vertices, false, 1.0F);
        } finally {
            RenderSystem.enableCull();
        }
    }

    /** A texture-free amber selection tint, kept separate from the red selection outline. */
    private static void renderSelectionOverlays(ViewerState state, PoseStack pose) {
        if (state.selected() == null) return;
        var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        addBoxOverlay(vertices, pose, state.selected(), 1.0F, 0.72F, 0.08F, 0.30F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(vertices.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    /** Renders one unit square per block across the full authored footprint plus a two-block visual margin. */
    private static void renderFloorGrid(ViewerState state, PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vertices) {
        double floorY = state.variant().cells().keySet().stream().mapToInt(position -> position.y()).min().orElse(0) - 0.012D;
        float intensity = state.darkViewportBackground() ? 0.34F : 0.45F;
        int minimum = -FLOOR_GRID_MARGIN;
        int maximumX = state.variant().width() + FLOOR_GRID_MARGIN;
        int maximumZ = state.variant().depth() + FLOOR_GRID_MARGIN;
        for (int x = minimum; x <= maximumX; x++) {
            addLine(vertices, pose, x, floorY, minimum, x, floorY, maximumZ, intensity, intensity, intensity, 0.58F);
        }
        for (int z = minimum; z <= maximumZ; z++) {
            addLine(vertices, pose, minimum, floorY, z, maximumX, floorY, z, intensity, intensity, intensity, 0.58F);
        }
    }

    private static void addBoxOutline(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack pose,
                                      net.sprocketgames.universalmultiblockviewer.model.GridPos position,
                                      float red, float green, float blue, float alpha) {
        addBoxOutline(vertices, pose, position, red, green, blue, alpha, OUTLINE_MIN, OUTLINE_MAX);
    }

    /**
     * Renders every edge of an open model as a projected ribbon instead of an OpenGL line.
     * The line primitive can silently drop segments at certain angles on some drivers.
     */
    private static void addOpenBlockOutline(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack pose,
                                            GridPos position, float red, float green, float blue, float alpha) {
        double minX = position.x() + OUTLINE_MIN;
        double minY = position.y() + OUTLINE_MIN;
        double minZ = position.z() + OUTLINE_MIN;
        double maxX = position.x() + OUTLINE_MAX;
        double maxY = position.y() + OUTLINE_MAX;
        double maxZ = position.z() + OUTLINE_MAX;
        Matrix4f matrix = pose.last().pose();
        addProjectedLine(vertices, matrix, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, minX, minY, minZ, minX, minY, maxZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, minX, minY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, maxX, minY, maxZ, maxX, minY, minZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, minX, maxY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addProjectedLine(vertices, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void addProjectedLine(com.mojang.blaze3d.vertex.VertexConsumer vertices, Matrix4f matrix,
                                         double startX, double startY, double startZ, double endX, double endY, double endZ,
                                         float red, float green, float blue, float alpha) {
        Vector4f start = matrix.transform(new Vector4f((float) startX, (float) startY, (float) startZ, 1.0F));
        Vector4f end = matrix.transform(new Vector4f((float) endX, (float) endY, (float) endZ, 1.0F));
        float deltaX = end.x() - start.x();
        float deltaY = end.y() - start.y();
        float length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length < 0.001F) return;
        float halfWidth = 0.85F;
        float offsetX = -deltaY / length * halfWidth;
        float offsetY = deltaX / length * halfWidth;
        Matrix4f identity = new Matrix4f();
        vertices.addVertex(identity, start.x() - offsetX, start.y() - offsetY, start.z()).setColor(red, green, blue, alpha);
        vertices.addVertex(identity, end.x() - offsetX, end.y() - offsetY, end.z()).setColor(red, green, blue, alpha);
        vertices.addVertex(identity, end.x() + offsetX, end.y() + offsetY, end.z()).setColor(red, green, blue, alpha);
        vertices.addVertex(identity, start.x() + offsetX, start.y() + offsetY, start.z()).setColor(red, green, blue, alpha);
    }

    private static void addBoxOutline(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack pose,
                                      net.sprocketgames.universalmultiblockviewer.model.GridPos position,
                                      float red, float green, float blue, float alpha, double minimum, double maximum) {
        double minX = position.x() + minimum;
        double minY = position.y() + minimum;
        double minZ = position.z() + minimum;
        double maxX = position.x() + maximum;
        double maxY = position.y() + maximum;
        double maxZ = position.z() + maximum;
        addLine(vertices, pose, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, minY, minZ, minX, minY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, minY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, minY, maxZ, maxX, minY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, maxY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
    }


    private static void addLine(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack pose,
                                double startX, double startY, double startZ, double endX, double endY, double endZ,
                                float red, float green, float blue, float alpha) {
        var currentPose = pose.last();
        vertices.addVertex(currentPose, (float) startX, (float) startY, (float) startZ).setColor(red, green, blue, alpha);
        vertices.addVertex(currentPose, (float) endX, (float) endY, (float) endZ).setColor(red, green, blue, alpha);
    }



    private static void addBoxOverlay(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack pose,
                                      GridPos position, float red, float green, float blue, float alpha) {
        float minX = (float) (position.x() + OUTLINE_MIN);
        float minY = (float) (position.y() + OUTLINE_MIN);
        float minZ = (float) (position.z() + OUTLINE_MIN);
        float maxX = (float) (position.x() + OUTLINE_MAX);
        float maxY = (float) (position.y() + OUTLINE_MAX);
        float maxZ = (float) (position.z() + OUTLINE_MAX);
        var currentPose = pose.last();
        addQuad(vertices, currentPose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addQuad(vertices, currentPose, maxX, minY, maxZ, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addQuad(vertices, currentPose, minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
        addQuad(vertices, currentPose, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        addQuad(vertices, currentPose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        addQuad(vertices, currentPose, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, minX, minY, minZ, red, green, blue, alpha);
    }

    private static void addQuad(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack.Pose pose,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                float x3, float y3, float z3, float x4, float y4, float z4,
                                float red, float green, float blue, float alpha) {
        vertices.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
        vertices.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
        vertices.addVertex(pose, x3, y3, z3).setColor(red, green, blue, alpha);
        vertices.addVertex(pose, x4, y4, z4).setColor(red, green, blue, alpha);
    }

    /**
     * Some genuine blocks intentionally hide their world model because a block entity draws them
     * in a level. The viewer is deliberately level-free, so draw their baked block model directly
     * rather than applying an item transform, which changes its authored scale and origin.
     */
    private static void renderBlock(Minecraft minecraft, BlockState blockState, PoseStack pose,
                                    MultiBufferSource.BufferSource buffers) {
        if (blockState.getBlock() instanceof AbstractSkullBlock) {
            // Skulls have no baked world model: their block-entity renderer supplies the head.
            // Call that renderer directly so its model stays at block scale rather than using an
            // item transform intended for inventories and item frames.
            minecraft.getItemRenderer().getBlockEntityRenderer().renderByItem(new ItemStack(blockState.getBlock()),
                ItemDisplayContext.FIXED, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            return;
        }
        if (blockState.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED
            && blockState.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
            // Blocks such as skulls are rendered by a block-entity renderer in a real level.
            // The viewer has no level, so use their item's fixed transform as a generic fallback.
            minecraft.getItemRenderer().renderStatic(new ItemStack(blockState.getBlock()), ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pose, buffers, minecraft.level, 0);
            return;
        }
        if (blockState.getRenderShape() == RenderShape.INVISIBLE && !blockState.isAir()
            && blockState.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
            var dispatcher = minecraft.getBlockRenderer();
            var model = dispatcher.getBlockModel(blockState);
            for (RenderType renderType : model.getRenderTypes(blockState, RandomSource.create(42L), ModelData.EMPTY)) {
                dispatcher.getModelRenderer().renderModel(pose.last(),
                    buffers.getBuffer(RenderTypeHelper.getEntityRenderType(renderType, false)), blockState, model,
                    1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY, renderType);
            }
            return;
        }
        minecraft.getBlockRenderer().renderSingleBlock(blockState, pose, buffers,
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    private static BlockState resolve(BlockOption option, GridPos position) {
        if (option.kind() == BlockOption.Kind.TAG) {
            return ViewerIngredientResolver.blockFor(option).defaultBlockState();
        }
        BlockState resolved = DISPLAY_STATES.computeIfAbsent(option, ViewerIngredientResolver::stateFor);
        return withProceduralSign(option, resolved, position);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState withProceduralSign(BlockOption option, BlockState state, GridPos position) {
        if (option.stateProperties().containsKey("sign")
            || state.getBlock().asItem() != net.minecraft.world.item.Items.AIR) {
            return state;
        }
        Property property = state.getBlock().getStateDefinition().getProperty("sign");
        if (property == null || property.getPossibleValues().size() < 2) return state;
        var values = property.getPossibleValues().stream().toList();
        return (BlockState) state.setValue(property, (Comparable) values.get(proceduralSignIndex(position, values.size())));
    }

    static int proceduralSignIndex(GridPos position, int count) {
        int mixed = position.x() * 734287 + position.y() * 912931 + position.z() * 438289;
        return Math.floorMod(mixed ^ (mixed >>> 16), count);
    }


    private static boolean hasMultipleDistinctOptions(net.sprocketgames.universalmultiblockviewer.model.BlockRequirement requirement) {
        // States or rendered variants created by the same reusable tool are not separate
        // material choices. They remain valid internally, but should not trigger the A indicator.
        return requirement.options().stream()
            .map(BlockModelViewportRenderer::presentationKey)
            .distinct()
            .limit(2)
            .count() > 1;
    }

    /** A position matches a strip material when any valid alternative presents that same item. */
    private static boolean usesPresentationItem(net.sprocketgames.universalmultiblockviewer.model.BlockRequirement requirement,
                                                ResourceLocation item) {
        return requirement.options().stream().anyMatch(option -> presentationKey(option).item().equals(item));
    }

    private static PresentationKey presentationKey(BlockOption option) {
        var material = option.material();
        return new PresentationKey(material.item() == null ? option.id() : material.item(), material.kind());
    }

    private record PresentationKey(ResourceLocation item, net.sprocketgames.universalmultiblockviewer.model.MaterialPresentation.Kind kind) { }
}
