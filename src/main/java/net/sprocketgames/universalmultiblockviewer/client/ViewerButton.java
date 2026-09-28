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

    private ViewerButton() {
    }

    public static void draw(GuiGraphics graphics, Font font, int x, int y, int width, int height, String label) {
        draw(graphics, font, x, y, width, height, label, 0);
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
