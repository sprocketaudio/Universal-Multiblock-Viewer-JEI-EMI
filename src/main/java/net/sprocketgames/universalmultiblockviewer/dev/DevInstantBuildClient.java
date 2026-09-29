package net.sprocketgames.universalmultiblockviewer.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

public final class DevInstantBuildClient {
    private DevInstantBuildClient() { }
    public static boolean available() {
        var player = Minecraft.getInstance().player;
        return DevInstantBuildConfig.enabled() && player != null && player.getAbilities().instabuild && player.hasPermissions(2);
    }
    public static void buildHere(ViewerState state) {
        if (!available()) return;
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Dev instant build requires a block target."), false);
            return;
        }
        // The clicked block is commonly the floor. Anchor to its selected face so the
        // air-only safety rule can still build a structure directly beside that block.
        PacketDistributor.sendToServer(DevInstantBuildPayload.from(hit.getBlockPos().relative(hit.getDirection()), DevInstantBuildPlan.from(state)));
    }
}
