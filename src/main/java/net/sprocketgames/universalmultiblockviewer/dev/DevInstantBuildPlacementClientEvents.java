package net.sprocketgames.universalmultiblockviewer.dev;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;

/** In-world, non-destructive placement preview for the Build Here tool. */
@EventBusSubscriber(modid = UniversalMultiblockViewer.MOD_ID, value = Dist.CLIENT)
public final class DevInstantBuildPlacementClientEvents {
    private DevInstantBuildPlacementClientEvents() { }

    @SubscribeEvent
    public static void mouseButton(InputEvent.MouseButton.Pre event) {
        if (!DevInstantBuildClient.placementActive() || event.getAction() != InputConstants.PRESS) return;
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT) {
            DevInstantBuildClient.confirm();
            event.setCanceled(true);
        } else if (event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT) {
            DevInstantBuildClient.cancel();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!DevInstantBuildClient.placementActive()) return;
        int direction = event.getScrollDeltaY() > 0.0D ? 1 : event.getScrollDeltaY() < 0.0D ? -1 : 0;
        if (direction == 0) return;
        if (Screen.hasControlDown()) {
            if (direction > 0) DevInstantBuildClient.moveUp();
            else DevInstantBuildClient.moveDown();
        } else {
            DevInstantBuildClient.rotate(direction);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void renderWorld(RenderLevelStageEvent event) {
        if (!DevInstantBuildClient.placementActive() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        BlockPos anchor = DevInstantBuildClient.anchor();
        var plan = DevInstantBuildClient.activePlan();
        var minecraft = Minecraft.getInstance();
        if (anchor == null || plan == null || minecraft.level == null) return;
        List<DevInstantBuildPlan.Placement> placements = plan.rotatedPlacements(DevInstantBuildClient.quarterTurns());
        boolean blocked = placements.stream().anyMatch(placement -> !minecraft.level.isEmptyBlock(anchor.offset(
            placement.offset().x(), placement.offset().y(), placement.offset().z())));
        float red = blocked ? 1.0F : 0.25F;
        float green = blocked ? 0.16F : 0.92F;
        float blue = blocked ? 0.08F : 0.48F;

        PoseStack pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        try (ByteBufferBuilder memory = new ByteBufferBuilder(Math.max(4_096, placements.size() * 320))) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(memory);
            var vertices = buffers.getBuffer(RenderType.lines());
            for (DevInstantBuildPlan.Placement placement : placements) {
                BlockPos target = anchor.offset(placement.offset().x(), placement.offset().y(), placement.offset().z());
                LevelRenderer.renderLineBox(pose, vertices,
                    target.getX() - 0.003D, target.getY() - 0.003D, target.getZ() - 0.003D,
                    target.getX() + 1.003D, target.getY() + 1.003D, target.getZ() + 1.003D,
                    red, green, blue, 0.88F);
            }
            buffers.endBatch(RenderType.lines());
        } finally {
            pose.popPose();
        }
    }

    @SubscribeEvent
    public static void renderHelp(RenderGuiEvent.Post event) {
        if (!DevInstantBuildClient.placementActive()) return;
        var graphics = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        List<Component> lines = List.of(
            Component.literal("Build Preview"),
            Component.literal("Left click: Place"),
            Component.literal("Right click: Cancel"),
            Component.literal("Wheel: Rotate"),
            Component.literal("Ctrl + wheel: Move up or down")
        );
        int width = lines.stream().mapToInt(font::width).max().orElse(0) + 10;
        int height = lines.size() * 10 + 7;
        int x = graphics.guiWidth() - width - 6;
        int y = graphics.guiHeight() - height - 6;
        graphics.fill(x, y, x + width, y + height, 0xDD201E1A);
        graphics.fill(x, y, x + width, y + 1, 0xFFE3A344);
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), x + 5, y + 4 + index * 10,
                index == 0 ? 0xFFFFC55C : 0xFFE8E1D4, false);
        }
    }
}
