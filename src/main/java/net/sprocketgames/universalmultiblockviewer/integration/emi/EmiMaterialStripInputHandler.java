package net.sprocketgames.universalmultiblockviewer.integration.emi;

import com.mojang.blaze3d.platform.InputConstants;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.sprocketgames.universalmultiblockviewer.client.MaterialStrip;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

final class EmiMaterialStripInputHandler {
    private static Active active;
    static void record(ViewerState state, GuiGraphics graphics) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) return;
        var matrix = graphics.pose().last().pose();
        active = new Active(screen, state, Math.round(matrix.m30()), Math.round(matrix.m31()));
    }
    static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        Active current = target(event.getMouseX(), event.getMouseY());
        if (current == null || event.getButton() != InputConstants.MOUSE_BUTTON_LEFT) return;
        double x = event.getMouseX() - current.left + MaterialStrip.X;
        double y = event.getMouseY() - current.top + MaterialStrip.Y;
        if (MaterialStrip.click(current.state, x, y)) { event.setCanceled(true); return; }
        var material = MaterialStrip.at(current.state, x, y);
        if (material != null) { EmiApi.displayRecipes(EmiStack.of(ViewerIngredientResolver.stackFor(material.requirement().defaultBlock()))); event.setCanceled(true); }
    }
    static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        Active current = target(event.getMouseX(), event.getMouseY());
        if (current != null) { current.state.scrollMaterialsSmooth(-event.getScrollDeltaY()); event.setCanceled(true); }
    }
    static void drag(ScreenEvent.MouseDragged.Pre event) {
        Active current = target(event.getMouseX(), event.getMouseY());
        if (current == null || event.getMouseButton() != InputConstants.MOUSE_BUTTON_LEFT) return;
        if (MaterialStrip.click(current.state, event.getMouseX() - current.left + MaterialStrip.X,
            event.getMouseY() - current.top + MaterialStrip.Y)) event.setCanceled(true);
    }
    static void tooltip(ScreenEvent.Render.Post event) {
        Active current = target(event.getMouseX(), event.getMouseY());
        if (current == null) return;
        double x = event.getMouseX() - current.left + MaterialStrip.X;
        double y = event.getMouseY() - current.top + MaterialStrip.Y;
        var material = MaterialStrip.at(current.state, x, y);
        if (material != null) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                ViewerIngredientResolver.stackFor(material.requirement().defaultBlock()), event.getMouseX(), event.getMouseY());
        }
    }
    private static Active target(double mouseX, double mouseY) {
        Active current = active;
        if (current == null || current.screen != Minecraft.getInstance().screen) return null;
        return mouseX >= current.left && mouseX < current.left + MaterialStrip.WIDTH
            && mouseY >= current.top && mouseY < current.top + MaterialStrip.HEIGHT ? current : null;
    }
    private record Active(Screen screen, ViewerState state, int left, int top) { }
}
