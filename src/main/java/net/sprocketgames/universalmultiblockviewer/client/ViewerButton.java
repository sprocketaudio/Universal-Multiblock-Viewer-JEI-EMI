package net.sprocketgames.universalmultiblockviewer.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/** Original compact control treatment shared by the two recipe-browser adapters. */
public final class ViewerButton {
    private static final int OUTLINE = 0xFF181713;
    private static final int EDGE = 0xFF6E685D;
    private static final int FACE = 0xFF302D27;
    public static final int OPTIONAL_OUTLINE_ORANGE = 0xFFD18C2E;
    public static final int ALTERNATIVE_OUTLINE_PURPLE = 0xFFA147E0;
    public static final int FLOOR_GRID_DARK_GREY = 0xFF68645E;

    private ViewerButton() {
    }

    public static void draw(GuiGraphics graphics, Font font, int x, int y, int width, int height, String label) {
        draw(graphics, font, x, y, width, height, label, 0);
    }

    /** Uses the same treatment at a reduced scale for dense viewport controls. */
    public static void drawCompact(GuiGraphics graphics, Font font, int x, int y, int width, int height, String label) {
        drawCompact(graphics, font, x, y, width, height, label, FACE);
    }

    /** Draws the compact treatment with an optional active inset while retaining its normal border. */
    public static void drawCompact(GuiGraphics graphics, Font font, int x, int y, int width, int height, String label, int inset) {
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, EDGE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, inset);
        float scale = 0.75F;
        float labelWidth = font.width(label) * scale;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + (width - labelWidth) / 2.0F, y + (height - font.lineHeight * scale) / 2.0F + 1.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, label, 0, 0, 0xFFF1E7D5, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    /** A compact three-line icon used where a label would be needlessly noisy. */
    public static void drawCompactList(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, EDGE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, FACE);
        int c = 0xFFF1E7D5;
        for (int row = 0; row < 3; row++) graphics.fill(x + 3, y + 3 + row * 3, x + width - 3, y + 4 + row * 3, c);
    }

    /** A non-clickable compact information marker using the same inset treatment as viewport controls. */
    public static void drawCompactInfo(GuiGraphics graphics, Font font, int x, int y) {
        fillCircle(graphics, x, y, 11, OUTLINE);
        fillCircle(graphics, x + 1, y + 1, 9, EDGE);
        fillCircle(graphics, x + 2, y + 2, 7, FACE);
        float scale = 0.75F;
        float labelWidth = font.width("i") * scale;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + (11 - labelWidth) / 2.0F + 0.5F, y + 3.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, "i", 0, 0, 0xFFF1E7D5, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void fillCircle(GuiGraphics graphics, int x, int y, int diameter, int color) {
        for (int row = 0; row < diameter; row++) {
            int distance = Math.abs(row - diameter / 2);
            int inset = distance >= diameter / 2 ? diameter / 2 - 1 : distance > diameter / 3 ? 2 : 0;
            graphics.fill(x + inset, y + row, x + diameter - inset, y + row + 1, color);
        }
    }

    /** Allows the few exceptionally compact controls to tune only their label baseline. */
    public static void draw(GuiGraphics graphics, Font font, int x, int y, int width, int height, String label, int labelOffsetY) {
        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, EDGE);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, FACE);
        int labelY = y + (height - font.lineHeight) / 2 + 2 + labelOffsetY;
        graphics.drawCenteredString(font, label, x + width / 2, labelY, 0xFFF1E7D5);
    }

    public static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
