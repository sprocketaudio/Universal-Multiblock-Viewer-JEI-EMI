package net.sprocketgames.universalmultiblockviewer.integration.emi;

import com.mojang.blaze3d.platform.InputConstants;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerLayout;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewportProjection;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.client.ViewerButton;
import net.sprocketgames.universalmultiblockviewer.client.SelectedBlockOptions;
import net.sprocketgames.universalmultiblockviewer.client.SelectedBlockInspector;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerPanelLayout;

/** Routes EMI screen input to the most recently rendered multiblock viewport. */
final class EmiViewportInputHandler {
    private static final int CONTROL_SIZE = 11;
    private static final int CONTROL_GAP = 1;
    private static ActiveViewport active;
    private static ActiveViewport rotating;
    private static ActiveViewport scrollingAlternatives;
    private static int rotatingButton = -1;

    private EmiViewportInputHandler() {
    }

    static void record(ViewerState state, GuiGraphics graphics, int left, int top, int width, int height) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return;
        }
        var matrix = graphics.pose().last().pose();
        active = new ActiveViewport(screen, state,
            Math.round(matrix.m30()) + left, Math.round(matrix.m31()) + top, width, height);
    }

    static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        ActiveViewport viewport = viewport(event.getMouseX(), event.getMouseY());
        if (viewport == null) {
            return;
        }
        double x = event.getMouseX() - viewport.left();
        double y = event.getMouseY() - viewport.top();
        if (!inViewport(viewport.state(), x, y)) return;
        double viewportX = x - ViewerPanelLayout.viewportX(viewport.state());
        if (SelectedBlockOptions.scrollAreaAt(viewport.state(), viewportX, y)) {
            viewport.state().scrollAlternativesSmooth(-event.getScrollDeltaY(), SelectedBlockOptions.VISIBLE);
            event.setCanceled(true);
            return;
        }
        viewport.state().zoomBy(event.getScrollDeltaY());
        event.setCanceled(true);
    }

    static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        ActiveViewport viewport = viewport(event.getMouseX(), event.getMouseY());
        if (viewport == null || !isRotateButton(event.getButton())) {
            return;
        }
        double x = event.getMouseX() - viewport.left();
        double y = event.getMouseY() - viewport.top();
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT && SelectedBlockInspector.isToggle(viewport.state(), x, y)) {
            viewport.state().toggleHelp();
            ViewerButton.playClick();
            event.setCanceled(true);
            return;
        }
        if (!inViewport(viewport.state(), x, y)) return;
        double viewportX = x - ViewerPanelLayout.viewportX(viewport.state());
        int viewportWidth = ViewerPanelLayout.viewportWidth(viewport.state());
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT
            && isAlternativeHighlightButton(viewportX, y, viewportWidth, viewport.height())) {
            viewport.state().toggleAlternativeHighlights();
            ViewerButton.playClick();
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT
            && isGridButton(viewport.state(), viewportX, y, viewportWidth, viewport.height())) {
            viewport.state().toggleFloorGrid();
            ViewerButton.playClick();
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT
            && isOptionalButton(viewport.state(), viewportX, y, viewportWidth, viewport.height())) {
            viewport.state().toggleOptionalBlocks();
            ViewerButton.playClick();
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT
            && isBackgroundButton(viewportX, y, viewportWidth, viewport.height())) {
            viewport.state().toggleViewportBackground();
            ViewerButton.playClick();
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT
            && isResetButton(viewportX, y, viewportWidth, viewport.height())) {
            viewport.state().resetView();
            ViewerButton.playClick();
            event.setCanceled(true);
            return;
        }
        int option = SelectedBlockOptions.optionAt(viewport.state(), viewportX, y);
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT
            && SelectedBlockOptions.scrollbarAt(viewport.state(), viewportX, y)) {
            SelectedBlockOptions.dragScrollbar(viewport.state(), y);
            scrollingAlternatives = viewport;
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT && option >= 0) {
            EmiApi.displayRecipes(EmiStack.of(ViewerIngredientResolver.stackFor(viewport.state().selectedRequirement().options().get(option))));
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT && option >= 0) {
            viewport.state().setSelectedOption(option);
            event.setCanceled(true);
            return;
        }
        if (event.getButton() == InputConstants.MOUSE_BUTTON_LEFT) {
            viewport.state().beginDrag(viewportX, y);
        } else {
            viewport.state().beginPanDrag(viewportX, y);
        }
        rotating = viewport;
        rotatingButton = event.getButton();
        event.setCanceled(true);
    }

    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        ActiveViewport alternativeViewport = scrollingAlternatives;
        if (alternativeViewport != null && alternativeViewport.screen() == Minecraft.getInstance().screen
            && event.getMouseButton() == InputConstants.MOUSE_BUTTON_LEFT) {
            SelectedBlockOptions.dragScrollbar(alternativeViewport.state(), event.getMouseY() - alternativeViewport.top());
            event.setCanceled(true);
            return;
        }
        ActiveViewport viewport = rotating;
        if (viewport == null || viewport.screen() != Minecraft.getInstance().screen || event.getMouseButton() != rotatingButton) {
            return;
        }
        viewport.state().dragTo(event.getMouseX() - viewport.left() - ViewerPanelLayout.viewportX(viewport.state()), event.getMouseY() - viewport.top());
        event.setCanceled(true);
    }

    static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (scrollingAlternatives != null && event.getButton() == InputConstants.MOUSE_BUTTON_LEFT) {
            scrollingAlternatives = null;
            event.setCanceled(true);
            return;
        }
        ActiveViewport viewport = rotating;
        if (viewport == null || viewport.screen() != Minecraft.getInstance().screen || event.getButton() != rotatingButton) {
            return;
        }
        rotating = null;
        rotatingButton = -1;
        if (viewport.state().endDrag()) {
            if (event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT) {
                viewport.state().clearSelection();
            } else {
                GridPos cell = ViewportProjection.findCell(viewport.state(), event.getMouseX() - viewport.left() - ViewerPanelLayout.viewportX(viewport.state()), event.getMouseY() - viewport.top(), ViewerPanelLayout.viewportWidth(viewport.state()), viewport.height());
                if (cell != null) {
                    viewport.state().selectOrToggle(cell);
                }
            }
        }
        event.setCanceled(true);
    }

    static void tooltip(ScreenEvent.Render.Post event) {
        ActiveViewport viewport = active;
        if (viewport == null || viewport.screen() != Minecraft.getInstance().screen) return;
        double x = event.getMouseX() - viewport.left();
        double y = event.getMouseY() - viewport.top();
        if (SelectedBlockInspector.isToggle(viewport.state(), x, y)) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                net.minecraft.network.chat.Component.literal(viewport.state().helpCollapsed() ? "Expand help" : "Minimize help"), event.getMouseX(), event.getMouseY());
            return;
        }
        if (!inViewport(viewport.state(), x, y)) return;
        double viewportX = x - ViewerPanelLayout.viewportX(viewport.state());
        int option = SelectedBlockOptions.optionAt(viewport.state(), viewportX, y);
        if (option >= 0) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                ViewerIngredientResolver.stackFor(viewport.state().selectedRequirement().options().get(option)), event.getMouseX(), event.getMouseY());
        } else if (isResetButton(viewportX, y, ViewerPanelLayout.viewportWidth(viewport.state()), viewport.height())) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal("Reset view"), event.getMouseX(), event.getMouseY());
        } else if (isGridButton(viewport.state(), viewportX, y, ViewerPanelLayout.viewportWidth(viewport.state()), viewport.height())) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal(
                viewport.state().showFloorGrid() ? "Hide Floor Grid" : "Show Floor Grid"), event.getMouseX(), event.getMouseY());
        } else if (isAlternativeHighlightButton(viewportX, y, ViewerPanelLayout.viewportWidth(viewport.state()), viewport.height())) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal(
                viewport.state().showAlternativeHighlights() ? "Hide Alternative Highlights" : "Show Alternative Highlights"), event.getMouseX(), event.getMouseY());
        } else if (isOptionalButton(viewport.state(), viewportX, y, ViewerPanelLayout.viewportWidth(viewport.state()), viewport.height())) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                net.minecraft.network.chat.Component.literal(viewport.state().showOptionalBlocks()
                    ? "Hide Optional Blocks" : "Show Optional Blocks"), event.getMouseX(), event.getMouseY());
        } else if (isBackgroundButton(viewportX, y, ViewerPanelLayout.viewportWidth(viewport.state()), viewport.height())) {
            event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font,
                net.minecraft.network.chat.Component.literal(viewport.state().darkViewportBackground()
                    ? "Background: Dark (switch to Light)" : "Background: Light (switch to Dark)"), event.getMouseX(), event.getMouseY());
        }
    }

    private static ActiveViewport viewport(double mouseX, double mouseY) {
        ActiveViewport viewport = active;
        if (viewport == null || viewport.screen() != Minecraft.getInstance().screen) {
            return null;
        }
        return mouseX >= viewport.left() && mouseX < viewport.left() + viewport.width()
            && mouseY >= viewport.top() && mouseY < viewport.top() + viewport.height() ? viewport : null;
    }

    private static boolean isRotateButton(int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT;
    }

    private static boolean isResetButton(double x, double y, int width, int height) {
        return inControl(x, y, width, height, 0);
    }

    private static boolean isBackgroundButton(double x, double y, int width, int height) {
        return inControl(x, y, width, height, 2);
    }

    private static boolean isGridButton(ViewerState state, double x, double y, int width, int height) {
        return inControl(x, y, width, height, 1);
    }

    private static boolean isAlternativeHighlightButton(double x, double y, int width, int height) {
        return inControl(x, y, width, height, 3);
    }

    private static boolean isOptionalButton(ViewerState state, double x, double y, int width, int height) {
        return state.hasOptionalBlocks() && inControl(x, y, width, height, 4);
    }

    private static int controlX(int width, int indexFromRight) {
        return width - (indexFromRight + 1) * CONTROL_SIZE - indexFromRight * CONTROL_GAP - 3;
    }

    private static boolean inControl(double x, double y, int width, int height, int indexFromRight) {
        int controlX = controlX(width, indexFromRight);
        return x >= controlX && x < controlX + CONTROL_SIZE
            && y >= height - CONTROL_SIZE - 3 && y < height - 3;
    }

    private static boolean inViewport(ViewerState state, double x, double y) {
        return x >= ViewerPanelLayout.viewportX(state) && x < ViewerPanelLayout.CONTENT_WIDTH
            && y >= 0 && y < ViewerPanelLayout.CONTENT_HEIGHT;
    }

    private record ActiveViewport(Screen screen, ViewerState state, int left, int top, int width, int height) {
    }
}
