package net.sprocketgames.universalmultiblockviewer.dev;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;

/** Requests a safe undo of the sender's latest Build Here placement. */
public record DevInstantBuildUndoPayload() implements CustomPacketPayload {
    public static final Type<DevInstantBuildUndoPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
        UniversalMultiblockViewer.MOD_ID, "build_here_undo"));
    public static final StreamCodec<FriendlyByteBuf, DevInstantBuildUndoPayload> STREAM_CODEC = StreamCodec.of(
        (buffer, payload) -> { }, buffer -> new DevInstantBuildUndoPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
