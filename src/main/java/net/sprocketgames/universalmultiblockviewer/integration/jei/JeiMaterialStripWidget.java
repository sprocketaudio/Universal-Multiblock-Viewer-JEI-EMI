package net.sprocketgames.universalmultiblockviewer.integration.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.sprocketgames.universalmultiblockviewer.client.MaterialStrip;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

final class JeiMaterialStripWidget implements IRecipeWidget, IJeiGuiEventListener, mezz.jei.api.gui.inputs.IJeiInputHandler {
    private final ViewerState state;
    /** JEI passes widget-local mouse coordinates, like it does for the viewport. */
    private final ScreenPosition position = new ScreenPosition(MaterialStrip.X, MaterialStrip.Y);
    private final ScreenRectangle area = new ScreenRectangle(MaterialStrip.X, MaterialStrip.Y, MaterialStrip.WIDTH, MaterialStrip.HEIGHT);
    private boolean scrollbarDragActive;
    JeiMaterialStripWidget(ViewerState state) { this.state = state; }
    @Override public ScreenPosition getPosition() { return position; }
    @Override public ScreenRectangle getArea() { return area; }
    @Override public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
        MaterialStrip.render(state, graphics, -MaterialStrip.X, -MaterialStrip.Y);
    }
    @Override public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        var material = MaterialStrip.at(state, stripX(mouseX), stripY(mouseY));
        if (material != null) {
            var stack = ViewerIngredientResolver.stackFor(material.requirement().defaultBlock());
            tooltip.addAll(stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.EMPTY,
                net.minecraft.client.Minecraft.getInstance().player, net.minecraft.world.item.TooltipFlag.Default.NORMAL));
            tooltip.add(Component.literal("x" + material.count()));
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
        if (MaterialStrip.click(state, stripX(mouseX), stripY(mouseY))) {
            scrollbarDragActive = true;
            return true;
        }
        var material = MaterialStrip.at(state, stripX(mouseX), stripY(mouseY));
        IJeiRuntime runtime = UniversalMultiblockViewerJeiPlugin.runtime();
        if (material != null && runtime != null) {
            runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, ViewerIngredientResolver.stackFor(material.requirement().defaultBlock())));
            return true;
        }
        return false;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        state.scrollMaterialsSmooth(-vertical);
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !scrollbarDragActive) return false;
        MaterialStrip.click(state, stripX(mouseX), stripY(mouseY));
        return true;
    }
    /** JEI only guarantees this handler is retained for the duration of a captured drag. */
    @Override public boolean handleMouseDragged(double mouseX, double mouseY, InputConstants.Key input, double dragX, double dragY) {
        if (!scrollbarDragActive) return false;
        MaterialStrip.click(state, stripX(mouseX), stripY(mouseY));
        return true;
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !scrollbarDragActive) return false;
        scrollbarDragActive = false;
        return true;
    }

    private static double stripX(double widgetX) { return widgetX + MaterialStrip.X; }
    private static double stripY(double widgetY) { return widgetY + MaterialStrip.Y; }
}
