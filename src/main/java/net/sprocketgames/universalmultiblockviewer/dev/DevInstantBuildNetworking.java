package net.sprocketgames.universalmultiblockviewer.dev;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;

public final class DevInstantBuildNetworking {
    private static final Map<UUID, LastBuild> LAST_BUILDS = new HashMap<>();

    private DevInstantBuildNetworking() { }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3")
            .playToServer(DevInstantBuildPayload.TYPE, DevInstantBuildPayload.STREAM_CODEC, DevInstantBuildNetworking::handle)
            .playToServer(DevInstantBuildUndoPayload.TYPE, DevInstantBuildUndoPayload.STREAM_CODEC, DevInstantBuildNetworking::handleUndo);
    }
    private static void handle(DevInstantBuildPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.getAbilities().instabuild) { fail(player, "Build Here requires Creative mode."); return; }
        if (payload.placements().isEmpty() || payload.placements().size() > 4096) { fail(player, "Invalid build plan."); return; }
        Set<BlockPos> targets = new HashSet<>();
        java.util.List<Resolved> resolved = new java.util.ArrayList<>();
        for (DevInstantBuildPayload.Placement placement : payload.placements()) {
            BlockPos target = payload.anchor().offset(placement.offset().x(), placement.offset().y(), placement.offset().z());
            if (!player.level().isInWorldBounds(target) || !player.level().getWorldBorder().isWithinBounds(target)) { fail(player, "Invalid target " + target.toShortString() + "."); return; }
            if (!targets.add(target)) { fail(player, "Duplicate target " + target.toShortString() + "."); return; }
            Block block = BuiltInRegistries.BLOCK.get(placement.blockId());
            if (block == null || block == Blocks.AIR) { fail(player, "Missing or invalid block " + placement.blockId() + "."); return; }
            BlockState state = applyState(block.defaultBlockState(), placement, player);
            if (state == null) return;
            state = state.rotate(rotation(payload.quarterTurns()));
            if (!player.level().isEmptyBlock(target)) { fail(player, "Blocked position " + target.toShortString() + "."); return; }
            resolved.add(new Resolved(target, state));
        }
        resolved.forEach(placement -> player.level().setBlock(placement.position(), placement.state(), 3));
        String title = payload.title().isBlank() ? "multiblock" : payload.title();
        LAST_BUILDS.put(player.getUUID(), new LastBuild(player.serverLevel(), title, List.copyOf(resolved)));
        player.sendSystemMessage(Component.literal("Built " + title + ": " + resolved.size() + " blocks placed."));
        UniversalMultiblockViewer.LOGGER.info("Build Here placed {} blocks for {} at {}", resolved.size(), player.getGameProfile().getName(), payload.anchor().toShortString());
    }
    private static void handleUndo(DevInstantBuildUndoPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!player.getAbilities().instabuild) { fail(player, "Undo requires Creative mode."); return; }
        LastBuild lastBuild = LAST_BUILDS.get(player.getUUID());
        if (lastBuild == null) { fail(player, "There is no Build Here placement to undo."); return; }
        if (lastBuild.level() != player.serverLevel()) { fail(player, "Return to the build's dimension before undoing it."); return; }
        int removed = 0;
        int preserved = 0;
        for (Resolved placement : lastBuild.placements()) {
            if (player.level().getBlockState(placement.position()).equals(placement.state())) {
                player.level().setBlock(placement.position(), Blocks.AIR.defaultBlockState(), 3);
                removed++;
            } else {
                preserved++;
            }
        }
        LAST_BUILDS.remove(player.getUUID());
        String suffix = preserved == 0 ? "" : " " + preserved + " changed block(s) left in place.";
        player.sendSystemMessage(Component.literal("Undid " + lastBuild.title() + ": " + removed + " blocks removed." + suffix));
        UniversalMultiblockViewer.LOGGER.info("Build Here undo removed {} blocks for {}", removed, player.getGameProfile().getName());
    }
    private static BlockState applyState(BlockState state, DevInstantBuildPayload.Placement placement, ServerPlayer player) {
        for (var entry : placement.stateProperties().entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null || property.getValue(entry.getValue()).isEmpty()) {
                fail(player, "Invalid state " + entry.getKey() + "=" + entry.getValue() + " for " + placement.blockId() + "."); return null;
            }
            state = setProperty(state, property, entry.getValue());
        }
        return state;
    }
    @SuppressWarnings({"rawtypes", "unchecked"}) private static BlockState setProperty(BlockState state, Property property, String value) { return state.setValue(property, (Comparable) property.getValue(value).orElseThrow()); }
    private static Rotation rotation(int quarterTurns) {
        return switch (quarterTurns) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
    private static void fail(ServerPlayer player, String message) { player.sendSystemMessage(Component.literal(message)); UniversalMultiblockViewer.LOGGER.warn("Build Here rejected for {}: {}", player.getGameProfile().getName(), message); }
    private record Resolved(BlockPos position, BlockState state) { }
    private record LastBuild(ServerLevel level, String title, List<Resolved> placements) { }
}
