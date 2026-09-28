package net.sprocketgames.universalmultiblockviewer.integration.emi;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
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
        if (y >= 0 && y < 14 && x >= 0 && x < 17) { current.state.cycleVariant(-1); ViewerButton.playClick(); event.setCanceled(true); }
        if (y >= 0 && y < 14 && x >= 41 && x < 58) { current.state.cycleVariant(1); ViewerButton.playClick(); event.setCanceled(true); }
        if (y >= 0 && y < 14 && x >= 62 && x < 79) { current.state.cycleLayer(-1); ViewerButton.playClick(); event.setCanceled(true); }
        if (y >= 0 && y < 14 && x >= 103 && x < 120) { current.state.cycleLayer(1); ViewerButton.playClick(); event.setCanceled(true); }
    }
    static void tooltip(ScreenEvent.Render.Post event) {
        Active current = active;
        if (current == null || current.screen != Minecraft.getInstance().screen) return;
        double x = event.getMouseX() - current.left;
        double y = event.getMouseY() - current.top;
        if (y < 0 || y >= 14) return;
        String text = x >= 0 && x < 17 ? "Previous version"
            : x >= 41 && x < 58 ? "Next version"
            : x >= 62 && x < 79 ? "Previous layer"
            : x >= 103 && x < 120 ? "Next layer" : null;
        if (text != null) event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
            net.minecraft.network.chat.Component.literal(text), event.getMouseX(), event.getMouseY());
    }
    private record Active(Screen screen, ViewerState state, int left, int top) { }
}
