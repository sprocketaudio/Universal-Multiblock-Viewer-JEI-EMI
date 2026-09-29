package net.sprocketgames.universalmultiblockviewer.dev;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;

@EventBusSubscriber(modid = UniversalMultiblockViewer.MOD_ID, value = Dist.CLIENT)
public final class DevStructureCaptureClientEvents {
    private DevStructureCaptureClientEvents() { }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        var save = Commands.literal("save")
            .then(Commands.argument("namespace", StringArgumentType.word()).suggests(NAMESPACES)
                .then(Commands.argument("file_name", StringArgumentType.word())
                    .executes(context -> save(StringArgumentType.getString(context, "namespace"), StringArgumentType.getString(context, "file_name")))));
        event.getDispatcher().register(Commands.literal("umvdev")
            .then(Commands.literal("corner1").executes(context -> corner(true)))
            .then(Commands.literal("corner2").executes(context -> corner(false)))
            .then(Commands.literal("clear").executes(context -> { DevStructureCapture.clear(); message("UMV capture cleared."); return 1; }))
            .then(save));
    }
    private static final SuggestionProvider<net.minecraft.commands.CommandSourceStack> NAMESPACES = (context, builder) -> {
        try {
            var root = Minecraft.getInstance().gameDirectory.toPath().resolve("kubejs/assets");
            if (Files.isDirectory(root)) try (var paths = Files.list(root)) { return SharedSuggestionProvider.suggest(paths.filter(Files::isDirectory).map(path -> path.getFileName().toString()).sorted().toList(), builder); }
        } catch (IOException ignored) { }
        return builder.buildFuture();
    };
    private static int corner(boolean first) {
        if (!available()) return 0;
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)) { message("Look at a block before marking a corner."); return 0; }
        if (first) { DevStructureCapture.setFirst(hit.getBlockPos()); message("UMV capture corner 1: " + hit.getBlockPos().toShortString()); }
        else { DevStructureCapture.setSecond(hit.getBlockPos()); message("UMV capture corner 2: " + hit.getBlockPos().toShortString()); }
        return 1;
    }
    private static int save(String namespace, String fileName) {
        if (!available()) return 0;
        try { message("Saved UMV capture: " + DevStructureCapture.save(namespace, fileName)); return 1; }
        catch (IllegalArgumentException | IllegalStateException | IOException exception) { message("UMV capture failed: " + exception.getMessage()); UniversalMultiblockViewer.LOGGER.warn("UMV capture save failed", exception); return 0; }
    }
    private static boolean available() { return DevInstantBuildClient.available(); }
    private static void message(String value) { var player = Minecraft.getInstance().player; if (player != null) player.displayClientMessage(Component.literal(value), false); }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (!available() || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var bounds = DevStructureCapture.bounds();
        if (bounds.isEmpty() && DevStructureCapture.first().isEmpty()) return;
        PoseStack pose = event.getPoseStack(); pose.pushPose(); var camera = event.getCamera().getPosition(); pose.translate(-camera.x, -camera.y, -camera.z);
        try (ByteBufferBuilder memory = new ByteBufferBuilder(4096)) {
            MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(memory); var vertices = buffers.getBuffer(RenderType.lines());
            if (bounds.isPresent()) { var box = bounds.get(); LevelRenderer.renderLineBox(pose, vertices, box.min().getX(), box.min().getY(), box.min().getZ(), box.max().getX() + 1, box.max().getY() + 1, box.max().getZ() + 1, 1.0F, 0.65F, 0.12F, 1.0F); }
            else DevStructureCapture.first().ifPresent(pos -> LevelRenderer.renderLineBox(pose, vertices, pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, 1.0F, 0.65F, 0.12F, 1.0F));
            buffers.endBatch(RenderType.lines());
        } finally { pose.popPose(); }
    }
}
