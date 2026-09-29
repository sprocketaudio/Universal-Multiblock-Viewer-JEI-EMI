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
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.GlStateBackup;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerLayout;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

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
                        renderBlock(minecraft, resolve(state.displayedBlock(position)), pose, sceneBuffers);
                        if (position.equals(state.selected())) {
                            minecraft.getBlockRenderer().renderSingleBlock(Blocks.YELLOW_STAINED_GLASS.defaultBlockState(), pose,
                                sceneBuffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                        }
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
            || state.showAlternativeHighlights() && visibleCells.stream()
                .anyMatch(position -> hasMultipleDistinctOptions(state.variant().cells().get(position)))
            || visibleCells.stream().anyMatch(position -> state.variant().cells().get(position).optional());
        if (!state.showFloorGrid() && !hasOutline) {
            return;
        }
        var vertices = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        if (state.showFloorGrid()) {
            renderFloorGrid(state, pose, vertices);
        }
        for (var position : visibleCells) {
            var requirement = state.variant().cells().get(position);
            if (position.equals(state.selected())) {
                addBoxOutline(vertices, pose, position, 1.0F, 0.08F, 0.04F, 1.0F);
            } else if (state.showAlternativeHighlights() && hasMultipleDistinctOptions(requirement)) {
                addBoxOutline(vertices, pose, position, 0.63F, 0.28F, 0.88F, 0.86F);
            } else if (requirement.optional()) {
                addBoxOutline(vertices, pose, position, 0.82F, 0.55F, 0.18F, 0.70F);
            }
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(vertices.buildOrThrow());
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
        double minX = position.x() + OUTLINE_MIN;
        double minY = position.y() + OUTLINE_MIN;
        double minZ = position.z() + OUTLINE_MIN;
        double maxX = position.x() + OUTLINE_MAX;
        double maxY = position.y() + OUTLINE_MAX;
        double maxZ = position.z() + OUTLINE_MAX;
        addLine(vertices, pose, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, minY, minZ, minX, minY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, maxY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, maxY, minZ, minX, maxY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, maxY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, minX, minY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, maxY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        addLine(vertices, pose, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
    }

    private static void addLine(com.mojang.blaze3d.vertex.VertexConsumer vertices, PoseStack pose,
                                double startX, double startY, double startZ, double endX, double endY, double endZ,
                                float red, float green, float blue, float alpha) {
        var currentPose = pose.last();
        vertices.addVertex(currentPose, (float) startX, (float) startY, (float) startZ).setColor(red, green, blue, alpha);
        vertices.addVertex(currentPose, (float) endX, (float) endY, (float) endZ).setColor(red, green, blue, alpha);
    }

    /**
     * Some genuine blocks intentionally hide their world model because a block entity draws them
     * in a level. The viewer is deliberately level-free, so draw their baked block model directly
     * rather than applying an item transform, which changes its authored scale and origin.
     */
    private static void renderBlock(Minecraft minecraft, BlockState blockState, PoseStack pose,
                                    MultiBufferSource.BufferSource buffers) {
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

    private static BlockState resolve(BlockOption option) {
        if (option.kind() == BlockOption.Kind.TAG) {
            return ViewerIngredientResolver.blockFor(option).defaultBlockState();
        }
        return DISPLAY_STATES.computeIfAbsent(option, ViewerIngredientResolver::stateFor);
    }

    private static boolean hasMultipleDistinctOptions(net.sprocketgames.universalmultiblockviewer.model.BlockRequirement requirement) {
        return requirement.options().stream().distinct().limit(2).count() > 1;
    }
}
