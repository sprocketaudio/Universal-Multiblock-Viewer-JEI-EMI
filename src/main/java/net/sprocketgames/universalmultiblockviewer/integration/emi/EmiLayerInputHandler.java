package net.sprocketgames.universalmultiblockviewer.integration.emi;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import dev.emi.emi.api.EmiApi;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import net.sprocketgames.universalmultiblockviewer.client.ViewerButton;

final class EmiLayerInputHandler {
    private static Active active;
    static void record(ViewerState state, GuiGraphics graphics) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) return;
        var matrix = graphics.pose().last().pose();
        active = new Active(screen, state, Math.round(matrix.m30()), Math.round(matrix.m31()));
    }
    static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        Active current = active;
        if (event.getButton() != InputConstants.MOUSE_BUTTON_LEFT || current == null || current.screen != Minecraft.getInstance().screen) return;
        double x = event.getMouseX() - current.left;
        double y = event.getMouseY() - current.top;
        if (y >= 0 && y < 13 && x >= 2 && x < 15) { current.state.cycleVariant(-1); ViewerButton.playClick(); event.setCanceled(true); }
        if (y >= 0 && y < 13 && x >= 40 && x < 53) { current.state.cycleVariant(1); ViewerButton.playClick(); event.setCanceled(true); }
        if (y >= 0 && y < 13 && x >= 56 && x < 69) { current.state.cycleLayer(-1); ViewerButton.playClick(); event.setCanceled(true); }
        if (y >= 0 && y < 13 && x >= 92 && x < 105) { current.state.cycleLayer(1); ViewerButton.playClick(); event.setCanceled(true); }
        if (y >= 0 && y < 13 && x >= 107 && x < 120) { EmiApi.displayRecipeCategory(UniversalMultiblockViewerEmiPlugin.CATEGORY); ViewerButton.playClick(); event.setCanceled(true); }
    }
    static void tooltip(ScreenEvent.Render.Post event) {
        Active current = active;
        if (current == null || current.screen != Minecraft.getInstance().screen) return;
        double x = event.getMouseX() - current.left;
        double y = event.getMouseY() - current.top;
        if (y < 0 || y >= 13) return;
        String text = x >= 2 && x < 15 ? "Previous version"
            : x >= 40 && x < 53 ? "Next version"
            : x >= 56 && x < 69 ? "Previous layer"
            : x >= 92 && x < 105 ? "Next layer"
            : x >= 107 && x < 120 ? "Show All Multiblock Guides" : null;
        if (text != null) event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
            net.minecraft.network.chat.Component.literal(text), event.getMouseX(), event.getMouseY());
    }
    private record Active(Screen screen, ViewerState state, int left, int top) { }
}
