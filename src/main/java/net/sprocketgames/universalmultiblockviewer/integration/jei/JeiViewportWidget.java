package net.sprocketgames.universalmultiblockviewer.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.sprocketgames.universalmultiblockviewer.client.BlockModelViewportRenderer;
import net.sprocketgames.universalmultiblockviewer.client.ViewerButton;
import net.sprocketgames.universalmultiblockviewer.client.SelectedBlockOptions;
import net.sprocketgames.universalmultiblockviewer.client.SelectedBlockInspector;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerLayout;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewportProjection;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerPanelLayout;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;

/**
 * A JEI-owned viewport. JEI translates this widget to the recipe's actual screen
 * location before drawing it, so it remains inside the recipe panel on every GUI.
 */
final class JeiViewportWidget implements IRecipeWidget, IJeiGuiEventListener, mezz.jei.api.gui.inputs.IJeiInputHandler {
    static final int X = 4;
    static final int Y = 18;
    static final int WIDTH = ViewerPanelLayout.CONTENT_WIDTH;
    static final int HEIGHT = 116;
    static final int CONTROL_SIZE = 11;
    static final int CONTROL_GAP = 1;

    private final ViewerState state;
    private final ScreenPosition position = new ScreenPosition(X, Y);
    private final ScreenRectangle area = new ScreenRectangle(X, Y, WIDTH, HEIGHT);
    private boolean selectedItemLeftClick;
    private boolean selectedItemRightClick;
    private boolean resetClick;
    private boolean backgroundClick;
    private boolean gridClick;
    private boolean alternativeHighlightClick;
    private boolean optionalClick;
    private boolean alternativeScrollbarClick;
    /** Prevents JEI drag events from a different widget using the previous camera drag origin. */
    private boolean viewportPointerCaptured;

    JeiViewportWidget(ViewerState state) {
        this.state = state;
    }

    @Override
    public ScreenPosition getPosition() {
        return position;
    }

    @Override
    public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
        int viewportX = viewportX();
        int viewportWidth = viewportWidth();
        BlockModelViewportRenderer.render(state, graphics, viewportX, 0, viewportWidth, HEIGHT);
        SelectedBlockOptions.render(state, graphics, viewportX, 0);
        int resetX = viewportX + controlX(viewportWidth, 0);
        int resetY = HEIGHT - CONTROL_SIZE - 3;
        int gridX = viewportX + controlX(viewportWidth, 1);
        int backgroundX = viewportX + controlX(viewportWidth, 2);
        int alternativesX = viewportX + controlX(viewportWidth, 3);
        if (state.hasOptionalBlocks()) {
            ViewerButton.drawCompact(graphics, net.minecraft.client.Minecraft.getInstance().font,
                viewportX + controlX(viewportWidth, 4), resetY, CONTROL_SIZE, CONTROL_SIZE, "O",
                state.showOptionalBlocks() ? ViewerButton.OPTIONAL_OUTLINE_ORANGE : 0xFF302D27);
        }
        ViewerButton.drawCompact(graphics, net.minecraft.client.Minecraft.getInstance().font, alternativesX, resetY, CONTROL_SIZE, CONTROL_SIZE, "A",
            state.showAlternativeHighlights() ? ViewerButton.ALTERNATIVE_OUTLINE_PURPLE : 0xFF302D27);
        ViewerButton.drawCompact(graphics, net.minecraft.client.Minecraft.getInstance().font, backgroundX, resetY, CONTROL_SIZE, CONTROL_SIZE,
            state.darkViewportBackground() ? "D" : "L");
        ViewerButton.drawCompact(graphics, net.minecraft.client.Minecraft.getInstance().font, gridX, resetY, CONTROL_SIZE, CONTROL_SIZE, "G",
            state.showFloorGrid() ? ViewerButton.FLOOR_GRID_DARK_GREY : 0xFF302D27);
        ViewerButton.drawCompact(graphics, net.minecraft.client.Minecraft.getInstance().font, resetX, resetY, CONTROL_SIZE, CONTROL_SIZE, "R");
        graphics.flush();
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if (SelectedBlockInspector.isToggle(state, mouseX, mouseY)) {
            tooltip.add(Component.literal(state.helpCollapsed() ? "Expand help" : "Minimize help"));
            return;
        }
        if (!inViewport(mouseX, mouseY)) return;
        double viewportMouseX = mouseX - viewportX();
        int option = SelectedBlockOptions.optionAt(state, viewportMouseX, mouseY);
        if (option >= 0) {
            tooltip.addAll(net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver.stackFor(state.selectedRequirement().options().get(option))
                .getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY, net.minecraft.client.Minecraft.getInstance().player,
                    net.minecraft.world.item.TooltipFlag.Default.NORMAL));
            return;
        }
        if (isResetButton(viewportMouseX, mouseY, viewportWidth())) {
            tooltip.add(Component.literal("Reset view"));
        } else if (isGridButton(viewportMouseX, mouseY, viewportWidth())) {
            tooltip.add(Component.literal(state.showFloorGrid() ? "Hide Floor Grid" : "Show Floor Grid"));
        } else if (isAlternativeHighlightButton(viewportMouseX, mouseY, viewportWidth())) {
            tooltip.add(Component.literal(state.showAlternativeHighlights() ? "Hide Alternative Highlights" : "Show Alternative Highlights"));
        } else if (isOptionalButton(viewportMouseX, mouseY, viewportWidth())) {
            tooltip.add(Component.literal(state.showOptionalBlocks() ? "Hide Optional Blocks" : "Show Optional Blocks"));
        } else if (isBackgroundButton(viewportMouseX, mouseY, viewportWidth())) {
            tooltip.add(Component.literal(state.darkViewportBackground() ? "Background: Dark (switch to Light)" : "Background: Light (switch to Dark)"));
        }
    }

    @Override
    public ScreenRectangle getArea() {
        return area;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && SelectedBlockInspector.isToggle(state, mouseX, mouseY)) {
            viewportPointerCaptured = false;
            state.toggleHelp();
            ViewerButton.playClick();
            return true;
        }
        if (!inViewport(mouseX, mouseY)) return false;
        double viewportMouseX = mouseX - viewportX();
        int viewportWidth = viewportWidth();
        if (button == InputConstants.MOUSE_BUTTON_LEFT && isAlternativeHighlightButton(viewportMouseX, mouseY, viewportWidth)) {
            viewportPointerCaptured = false;
            state.toggleAlternativeHighlights();
            ViewerButton.playClick();
            alternativeHighlightClick = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && isGridButton(viewportMouseX, mouseY, viewportWidth)) {
            viewportPointerCaptured = false;
            state.toggleFloorGrid();
            ViewerButton.playClick();
            gridClick = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && isOptionalButton(viewportMouseX, mouseY, viewportWidth)) {
            viewportPointerCaptured = false;
            state.toggleOptionalBlocks();
            ViewerButton.playClick();
            optionalClick = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && isBackgroundButton(viewportMouseX, mouseY, viewportWidth)) {
            viewportPointerCaptured = false;
            state.toggleViewportBackground();
            ViewerButton.playClick();
            backgroundClick = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && isResetButton(viewportMouseX, mouseY, viewportWidth)) {
            viewportPointerCaptured = false;
            state.resetView();
            ViewerButton.playClick();
            resetClick = true;
            return true;
        }
        int option = SelectedBlockOptions.optionAt(state, viewportMouseX, mouseY);
        if (button == InputConstants.MOUSE_BUTTON_LEFT && SelectedBlockOptions.scrollbarAt(state, viewportMouseX, mouseY)) {
            viewportPointerCaptured = false;
            SelectedBlockOptions.dragScrollbar(state, mouseY);
            alternativeScrollbarClick = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && option >= 0) {
            viewportPointerCaptured = false;
            var runtime = UniversalMultiblockViewerJeiPlugin.runtime();
            if (runtime != null) runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT,
                VanillaTypes.ITEM_STACK, net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver.stackFor(state.selectedRequirement().options().get(option))));
            selectedItemLeftClick = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && option >= 0) {
            viewportPointerCaptured = false;
            state.setSelectedOption(option);
            selectedItemRightClick = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            state.beginDrag(viewportMouseX, mouseY);
            viewportPointerCaptured = true;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            state.beginPanDrag(viewportMouseX, mouseY);
            viewportPointerCaptured = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && alternativeHighlightClick) {
            alternativeHighlightClick = false;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && gridClick) {
            gridClick = false;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && optionalClick) {
            optionalClick = false;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && backgroundClick) {
            backgroundClick = false;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && resetClick) {
            resetClick = false;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && selectedItemLeftClick) {
            selectedItemLeftClick = false;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT && alternativeScrollbarClick) {
            alternativeScrollbarClick = false;
            return true;
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && selectedItemRightClick) {
            selectedItemRightClick = false;
            return true;
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT) {
            return false;
        }
        if (!viewportPointerCaptured) return false;
        viewportPointerCaptured = false;
        if (state.endDrag()) {
            if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                state.clearSelection();
            } else {
                GridPos cell = ViewportProjection.findCell(state, mouseX - viewportX(), mouseY, viewportWidth(), HEIGHT);
                if (cell != null) {
                    state.selectOrToggle(cell);
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && alternativeScrollbarClick) {
            SelectedBlockOptions.dragScrollbar(state, mouseY);
            return true;
        }
        if (!viewportPointerCaptured || (button != InputConstants.MOUSE_BUTTON_LEFT && button != InputConstants.MOUSE_BUTTON_RIGHT)) {
            return false;
        }
        state.dragTo(mouseX - viewportX(), mouseY);
        return true;
    }

    /** JEI's input-handler channel retains a drag that begins on a narrow scrollbar thumb. */
    @Override
    public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key input, double dragX, double dragY) {
        if (!alternativeScrollbarClick) return false;
        SelectedBlockOptions.dragScrollbar(state, mouseY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!inViewport(mouseX, mouseY)) return false;
        double viewportMouseX = mouseX - viewportX();
        if (SelectedBlockOptions.scrollAreaAt(state, viewportMouseX, mouseY)) {
            state.scrollAlternativesSmooth(-verticalAmount, SelectedBlockOptions.VISIBLE);
            return true;
        }
        state.zoomBy(verticalAmount);
        return true;
    }

    private int viewportX() { return ViewerPanelLayout.viewportX(state); }
    private int viewportWidth() { return ViewerPanelLayout.viewportWidth(state); }
    private boolean inViewport(double mouseX, double mouseY) {
        return mouseX >= viewportX() && mouseX < WIDTH && mouseY >= 0 && mouseY < HEIGHT;
    }

    static boolean isResetButton(double mouseX, double mouseY, int width) {
        return inControl(mouseX, mouseY, width, 0);
    }

    static boolean isBackgroundButton(double mouseX, double mouseY, int width) {
        return inControl(mouseX, mouseY, width, 2);
    }

    private boolean isGridButton(double mouseX, double mouseY, int width) {
        return inControl(mouseX, mouseY, width, 1);
    }

    private boolean isAlternativeHighlightButton(double mouseX, double mouseY, int width) {
        return inControl(mouseX, mouseY, width, 3);
    }

    private boolean isOptionalButton(double mouseX, double mouseY, int width) {
        return state.hasOptionalBlocks() && inControl(mouseX, mouseY, width, 4);
    }

    private static int controlX(int width, int indexFromRight) {
        return width - (indexFromRight + 1) * CONTROL_SIZE - indexFromRight * CONTROL_GAP - 3;
    }

    private static boolean inControl(double mouseX, double mouseY, int width, int indexFromRight) {
        int x = controlX(width, indexFromRight);
        return mouseX >= x && mouseX < x + CONTROL_SIZE
            && mouseY >= HEIGHT - CONTROL_SIZE - 3 && mouseY < HEIGHT - 3;
    }
}
