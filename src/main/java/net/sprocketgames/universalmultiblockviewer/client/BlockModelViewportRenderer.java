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
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.GlStateBackup;
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
                        minecraft.getBlockRenderer().renderSingleBlock(
                            resolve(state.displayedBlock(position)), pose, sceneBuffers,
                            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
                        );
                        if (position.equals(state.selected())) {
                            minecraft.getBlockRenderer().renderSingleBlock(Blocks.YELLOW_STAINED_GLASS.defaultBlockState(), pose,
                                sceneBuffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                            LevelRenderer.renderLineBox(pose, sceneBuffers.getBuffer(RenderType.lines()),
                                -0.003D, -0.003D, -0.003D, 1.003D, 1.003D, 1.003D,
                                1.0F, 0.08F, 0.04F, 1.0F);
                        }
                    } finally {
                        pose.popPose();
                    }
                }
            } finally {
                pose.popPose();
                sceneBuffers.endBatch();
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

    private static BlockState resolve(BlockOption option) {
        if (option.kind() == BlockOption.Kind.TAG) {
            return ViewerIngredientResolver.blockFor(option).defaultBlockState();
        }
        return DISPLAY_STATES.computeIfAbsent(option, ViewerIngredientResolver::stateFor);
    }
}
