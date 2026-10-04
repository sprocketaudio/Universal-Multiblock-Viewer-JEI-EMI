package net.sprocketgames.universalmultiblockviewer.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

public final class DevInstantBuildClient {
    private static DevInstantBuildPlan activePlan;
    private static int quarterTurns;
    private static int verticalOffset;

    private DevInstantBuildClient() { }
    public static boolean available() {
        var player = Minecraft.getInstance().player;
        return player != null && player.getAbilities().instabuild;
    }
    public static boolean captureAvailable() {
        var player = Minecraft.getInstance().player;
        return player != null && player.hasPermissions(2);
    }
    public static boolean placementActive() {
        return activePlan != null;
    }

    public static void buildHere(ViewerState state) {
        if (!available()) return;
        activePlan = DevInstantBuildPlan.from(state);
        quarterTurns = 0;
        verticalOffset = 0;
        Minecraft.getInstance().setScreen(null);
    }

    public static void rotate(int direction) {
        if (placementActive()) quarterTurns = Math.floorMod(quarterTurns + Integer.signum(direction), 4);
    }

    public static void moveUp() {
        if (placementActive()) verticalOffset++;
    }

    public static void moveDown() {
        if (placementActive()) verticalOffset--;
    }

    public static int quarterTurns() { return quarterTurns; }
    public static int verticalOffset() { return verticalOffset; }

    public static BlockPos anchor() {
        if (!placementActive() || !(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)) return null;
        return hit.getBlockPos().relative(hit.getDirection()).above(verticalOffset);
    }

    public static DevInstantBuildPlan activePlan() { return activePlan; }

    public static void confirm() {
        if (!placementActive()) return;
        BlockPos anchor = anchor();
        if (anchor == null) {
            message("Build Here requires a block target.");
            return;
        }
        PacketDistributor.sendToServer(DevInstantBuildPayload.from(anchor, activePlan, quarterTurns));
        cancel();
    }

    public static void cancel() {
        activePlan = null;
        quarterTurns = 0;
        verticalOffset = 0;
    }

    public static int undo() {
        if (!available()) {
            message("Undo requires Creative mode.");
            return 0;
        }
        PacketDistributor.sendToServer(new DevInstantBuildUndoPayload());
        return 1;
    }

    private static void message(String value) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(value), false);
    }

}
