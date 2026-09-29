package net.sprocketgames.universalmultiblockviewer.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.client.BlockModelViewportRenderer;
import net.sprocketgames.universalmultiblockviewer.client.ViewerButton;
import net.sprocketgames.universalmultiblockviewer.client.SelectedBlockOptions;
import net.sprocketgames.universalmultiblockviewer.client.ScrollingTitle;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerLayout;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerPanelLayout;

/** EMI recipe presentation using the same definition, material calculation, and projection as JEI. */
public final class EmiMultiblockRecipe implements EmiRecipe {
    private static final int CONTROL_SIZE = 11;
    private static final int CONTROL_GAP = 1;
    private final MultiblockDefinition definition;
    private final ViewerState state;

    public EmiMultiblockRecipe(MultiblockDefinition definition) {
        this.definition = definition;
        this.state = new ViewerState(definition);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return UniversalMultiblockViewerEmiPlugin.CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return definition.id();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        // EMI indexes inputs for Uses. Only explicit U lookup items belong here;
        // BOM entries remain clickable in the custom strip.
        return definition.useLookupItems().stream().map(itemId -> (EmiIngredient) EmiStack.of(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId))).toList();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return List.of();
    }

    @Override
    public List<EmiStack> getOutputs() {
        return definition.recipeLookupItems().stream().map(itemId -> EmiStack.of(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId))).toList();
    }

    @Override
    public int getDisplayWidth() {
        return 256;
    }

    @Override
    public int getDisplayHeight() {
        return 166;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        state.resetViewportBackground();
        widgets.addDrawable(4, 1, 128, 14, (graphics, mouseX, mouseY, delta) ->
            ScrollingTitle.draw(graphics, Minecraft.getInstance().font, state.definition().displayTitle(), 0, 4, 128, 0xFF403B33));
        widgets.addDrawable(136, 1, 120, 14, (graphics, mouseX, mouseY, delta) -> {
            var font = Minecraft.getInstance().font;
            ViewerButton.draw(graphics, font, 0, 0, 17, 14, "<");
            ViewerButton.draw(graphics, font, 41, 0, 17, 14, ">");
            int variant = state.definition().variants().keySet().stream().toList().indexOf(state.variantId()) + 1;
            drawUnshadowedCentered(graphics, font, "V:" + variant, 29, 5);
            ViewerButton.draw(graphics, font, 62, 0, 17, 14, "<");
            ViewerButton.draw(graphics, font, 103, 0, 17, 14, ">");
            drawUnshadowedCentered(graphics, font, state.layer() < 0 ? "All" : "L:" + (state.layer() + 1), 91, 5);
            EmiLayerInputHandler.record(state, graphics);
        });
        widgets.addDrawable(4, 18, net.sprocketgames.universalmultiblockviewer.client.SelectedBlockInspector.WIDTH, net.sprocketgames.universalmultiblockviewer.client.SelectedBlockInspector.HEIGHT, (graphics, mouseX, mouseY, delta) -> {
            net.sprocketgames.universalmultiblockviewer.client.SelectedBlockInspector.render(state, graphics, 0, 0);
        });
        widgets.addDrawable(4, 18, ViewerPanelLayout.CONTENT_WIDTH, ViewerPanelLayout.CONTENT_HEIGHT, (graphics, mouseX, mouseY, delta) -> {
            // EMI translates the graphics pose to this drawable's location before this
            // callback. Its integer parameters are the current mouse position.
            int viewportX = ViewerPanelLayout.viewportX(state);
            int viewportWidth = ViewerPanelLayout.viewportWidth(state);
            BlockModelViewportRenderer.render(state, graphics, viewportX, 0, viewportWidth, ViewerPanelLayout.CONTENT_HEIGHT);
            SelectedBlockOptions.render(state, graphics, viewportX, 0);
            int resetX = viewportX + controlX(viewportWidth, 0);
            int resetY = ViewerPanelLayout.CONTENT_HEIGHT - CONTROL_SIZE - 3;
            int gridX = viewportX + controlX(viewportWidth, 1);
            int backgroundX = viewportX + controlX(viewportWidth, 2);
            int alternativesX = viewportX + controlX(viewportWidth, 3);
            if (state.hasOptionalBlocks()) {
                ViewerButton.drawCompact(graphics, Minecraft.getInstance().font, viewportX + controlX(viewportWidth, 4),
                    resetY, CONTROL_SIZE, CONTROL_SIZE, "O",
                    state.showOptionalBlocks() ? ViewerButton.OPTIONAL_OUTLINE_ORANGE : 0xFF302D27);
            }
            ViewerButton.drawCompact(graphics, Minecraft.getInstance().font, alternativesX, resetY, CONTROL_SIZE, CONTROL_SIZE, "A",
                state.showAlternativeHighlights() ? ViewerButton.ALTERNATIVE_OUTLINE_PURPLE : 0xFF302D27);
            ViewerButton.drawCompact(graphics, Minecraft.getInstance().font, backgroundX, resetY, CONTROL_SIZE, CONTROL_SIZE,
                state.darkViewportBackground() ? "D" : "L");
            ViewerButton.drawCompact(graphics, Minecraft.getInstance().font, gridX, resetY, CONTROL_SIZE, CONTROL_SIZE, "G",
                state.showFloorGrid() ? ViewerButton.FLOOR_GRID_DARK_GREY : 0xFF302D27);
            ViewerButton.drawCompact(graphics, Minecraft.getInstance().font, resetX, resetY, CONTROL_SIZE, CONTROL_SIZE, "R");
            EmiViewportInputHandler.record(state, graphics, 0, 0, ViewerPanelLayout.CONTENT_WIDTH, ViewerPanelLayout.CONTENT_HEIGHT);
        });
        widgets.addDrawable(net.sprocketgames.universalmultiblockviewer.client.MaterialStrip.X,
            net.sprocketgames.universalmultiblockviewer.client.MaterialStrip.Y,
            net.sprocketgames.universalmultiblockviewer.client.MaterialStrip.WIDTH,
            net.sprocketgames.universalmultiblockviewer.client.MaterialStrip.HEIGHT, (graphics, mouseX, mouseY, delta) -> {
                net.sprocketgames.universalmultiblockviewer.client.MaterialStrip.render(state, graphics,
                    -net.sprocketgames.universalmultiblockviewer.client.MaterialStrip.X,
                    -net.sprocketgames.universalmultiblockviewer.client.MaterialStrip.Y);
                EmiMaterialStripInputHandler.record(state, graphics);
            });
    }

    private static void drawUnshadowedCentered(GuiGraphics graphics, Font font, String text, int centerX, int y) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, 0xFF403B33, false);
    }

    private static int controlX(int viewportWidth, int indexFromRight) {
        return viewportWidth - (indexFromRight + 1) * CONTROL_SIZE - indexFromRight * CONTROL_GAP - 3;
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }
}
