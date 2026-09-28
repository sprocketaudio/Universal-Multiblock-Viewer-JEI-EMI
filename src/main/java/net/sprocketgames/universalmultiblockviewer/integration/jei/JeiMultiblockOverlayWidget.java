package net.sprocketgames.universalmultiblockviewer.integration.jei;

import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import net.sprocketgames.universalmultiblockviewer.client.ViewerButton;
import net.sprocketgames.universalmultiblockviewer.client.ScrollingTitle;

/** Draws dynamic labels above the standard JEI slots, including always-visible quantities. */
final class JeiMultiblockOverlayWidget implements IRecipeWidget, IJeiGuiEventListener {
    private final ViewerState state;
    private final ScreenPosition position = new ScreenPosition(0, 0);
    private final ScreenRectangle headerArea = new ScreenRectangle(0, 0, 256, 16);

    JeiMultiblockOverlayWidget(ViewerState state) {
        this.state = state;
    }

    @Override
    public ScreenPosition getPosition() {
        return position;
    }

    @Override
    public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        ScrollingTitle.draw(graphics, font, state.definition().displayTitle(), 4, 5, 126, 0xFF403B33);
        button(graphics, font, 136, "<");
        button(graphics, font, 177, ">");
        int variant = state.definition().variants().keySet().stream().toList().indexOf(state.variantId()) + 1;
        drawUnshadowedCentered(graphics, font, "V:" + variant, 165, 6);
        button(graphics, font, 198, "<");
        button(graphics, font, 239, ">");
        drawUnshadowedCentered(graphics, font, state.layer() < 0 ? "All" : "L:" + (state.layer() + 1), 227, 6);
        graphics.flush();
    }

    private static void drawUnshadowedCentered(GuiGraphics graphics, Font font, String text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, 0xFF403B33, false);
    }

    @Override
    public ScreenRectangle getArea() {
        return headerArea;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (mouseX >= 136 && mouseX < 153) { state.cycleVariant(-1); ViewerButton.playClick(); return true; }
        if (mouseX >= 177 && mouseX < 194) { state.cycleVariant(1); ViewerButton.playClick(); return true; }
        if (mouseX >= 198 && mouseX < 215) { state.cycleLayer(-1); ViewerButton.playClick(); return true; }
        if (mouseX >= 239 && mouseX < 256) { state.cycleLayer(1); ViewerButton.playClick(); return true; }
        return false;
    }

    @Override
    public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if (mouseY < 1 || mouseY >= 15) return;
        if (mouseX >= 136 && mouseX < 153) tooltip.add(Component.literal("Previous version"));
        else if (mouseX >= 177 && mouseX < 194) tooltip.add(Component.literal("Next version"));
        else if (mouseX >= 198 && mouseX < 215) tooltip.add(Component.literal("Previous layer"));
        else if (mouseX >= 239 && mouseX < 256) tooltip.add(Component.literal("Next layer"));
    }

    private static void button(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, String text) {
        ViewerButton.draw(graphics, font, x, 1, 17, 14, text);
    }
}
