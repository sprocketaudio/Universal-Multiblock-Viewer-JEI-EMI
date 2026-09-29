package net.sprocketgames.universalmultiblockviewer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

/** Compact host-neutral selected-position panel. Its controls only change display choices. */
public final class SelectedBlockInspector {
    public static final int WIDTH = 78;
    public static final int HEIGHT = 116;
    private static final int PANEL = 0xE8252421;
    private static final int BORDER = 0xFF70695E;

    private SelectedBlockInspector() {
    }

    public static void render(ViewerState state, GuiGraphics graphics, int left, int top) {
        state.animateHelp();
        int width = state.helpWidth();
        var font = Minecraft.getInstance().font;
        graphics.fill(left, top, left + width, top + HEIGHT, PANEL);
        graphics.renderOutline(left, top, width, HEIGHT, BORDER);
        int toggleX = left + width - 16;
        ViewerButton.draw(graphics, font, toggleX, top + 3, 13, 13, state.helpCollapsed() ? ">" : "<", -1);
        if (width < 45) return;
        var pose = graphics.pose().last().pose();
        int screenLeft = Math.round(pose.m30()) + left;
        int screenTop = Math.round(pose.m31()) + top;
        // The collapse control overlays only the title row, not a permanent text column.
        graphics.enableScissor(screenLeft, screenTop, screenLeft + width - 3, screenTop + HEIGHT);
        try {
        graphics.drawString(font, Component.literal("Help"), left + 6, top + 7, 0xFFFFC36A, false);
        graphics.drawString(font, "Click a block", left + 6, top + 25, 0xFFF1E7D5, false);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left + 6, top + 38, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.drawString(font, "Right click cancel", 0, 0, 0xFFC9C1B4, false);
        } finally { graphics.pose().popPose(); }
        drawSmall(font, graphics, "Left drag: Rotate", left + 6, top + 70);
        drawSmall(font, graphics, "Right drag: Move", left + 6, top + 82);
        drawSmall(font, graphics, "Wheel: Zoom", left + 6, top + 94);
        } finally {
            graphics.disableScissor();
        }
    }

    public static boolean isToggle(ViewerState state, double x, double y) {
        int width = state.helpWidth();
        return x >= width - 16 && x < width - 3 && y >= 3 && y < 16;
    }

    private static void drawSmall(net.minecraft.client.gui.Font font, GuiGraphics graphics, String text, int x, int y) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.drawString(font, text, 0, 0, 0xFFC9C1B4, false);
        } finally { graphics.pose().popPose(); }
    }

}
