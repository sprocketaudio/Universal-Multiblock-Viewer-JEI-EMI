package net.sprocketgames.universalmultiblockviewer.integration.jei;

import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.sprocketgames.universalmultiblockviewer.client.SelectedBlockInspector;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

final class JeiInspectorWidget implements IRecipeWidget {
    private final ViewerState state;
    private final ScreenPosition position = new ScreenPosition(4, 18);
    JeiInspectorWidget(ViewerState state) { this.state = state; }
    @Override public ScreenPosition getPosition() { return position; }
    @Override public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) { SelectedBlockInspector.render(state, graphics, 0, 0); }
}
