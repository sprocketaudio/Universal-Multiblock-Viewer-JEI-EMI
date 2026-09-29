package net.sprocketgames.universalmultiblockviewer.dev;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;

public record DevInstantBuildPayload(BlockPos anchor, String title, List<Placement> placements) implements CustomPacketPayload {
    public static final Type<DevInstantBuildPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(UniversalMultiblockViewer.MOD_ID, "dev_instant_build"));
    public static final StreamCodec<FriendlyByteBuf, DevInstantBuildPayload> STREAM_CODEC = StreamCodec.of(DevInstantBuildPayload::write, DevInstantBuildPayload::read);

    public record Placement(GridPos offset, ResourceLocation blockId, Map<String, String> stateProperties) { }

    public static DevInstantBuildPayload from(BlockPos anchor, DevInstantBuildPlan plan) {
        return new DevInstantBuildPayload(anchor, plan.title(), plan.placements().stream().map(placement ->
            new Placement(placement.offset(), placement.option().id(), placement.option().stateProperties())).toList());
    }

    private static DevInstantBuildPayload read(FriendlyByteBuf buffer) {
        BlockPos anchor = BlockPos.of(buffer.readLong());
        String title = readString(buffer, 128);
        int count = buffer.readVarInt();
        if (count < 1 || count > 4096) throw new IllegalArgumentException("invalid placement count " + count);
        List<Placement> placements = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            GridPos offset = new GridPos(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
            ResourceLocation id = ResourceLocation.parse(readString(buffer, 256));
            int states = buffer.readVarInt();
            if (states > 32) throw new IllegalArgumentException("too many state properties");
            Map<String, String> properties = new LinkedHashMap<>();
            for (int state = 0; state < states; state++) properties.put(readString(buffer, 64), readString(buffer, 128));
            placements.add(new Placement(offset, id, Map.copyOf(properties)));
        }
        return new DevInstantBuildPayload(anchor, title, List.copyOf(placements));
    }

    private static void write(FriendlyByteBuf buffer, DevInstantBuildPayload payload) {
        buffer.writeLong(payload.anchor.asLong());
        writeString(buffer, payload.title, 128);
        buffer.writeVarInt(payload.placements.size());
        for (Placement placement : payload.placements) {
            buffer.writeVarInt(placement.offset.x()); buffer.writeVarInt(placement.offset.y()); buffer.writeVarInt(placement.offset.z());
            writeString(buffer, placement.blockId.toString(), 256);
            buffer.writeVarInt(placement.stateProperties.size());
            placement.stateProperties.forEach((key, value) -> { writeString(buffer, key, 64); writeString(buffer, value, 128); });
        }
    }

    private static String readString(FriendlyByteBuf buffer, int maximum) {
        return buffer.readUtf(maximum);
    }
    private static void writeString(FriendlyByteBuf buffer, String value, int maximum) {
        buffer.writeUtf(value, maximum);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
