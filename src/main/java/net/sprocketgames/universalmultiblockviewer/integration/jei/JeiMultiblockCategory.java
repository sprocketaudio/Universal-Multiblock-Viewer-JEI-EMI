package net.sprocketgames.universalmultiblockviewer.integration.jei;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;
import net.sprocketgames.universalmultiblockviewer.model.MaterialEntry;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockMaterials;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

/** JEI host adapter. The model, material calculation, projection, and state all remain host-neutral. */
public final class JeiMultiblockCategory implements IRecipeCategory<MultiblockDefinition> {
    public static final RecipeType<MultiblockDefinition> TYPE = new RecipeType<>(
        ResourceLocation.fromNamespaceAndPath(UniversalMultiblockViewer.MOD_ID, "multiblock"), MultiblockDefinition.class);
    private final IDrawable background;
    private final IDrawable icon;
    private final Map<ResourceLocation, ViewerState> states = new ConcurrentHashMap<>();

    public JeiMultiblockCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(256, 166);
        icon = guiHelper.createDrawableItemStack(new ItemStack(Items.SPYGLASS));
    }

    @Override
    public RecipeType<MultiblockDefinition> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.universal_multiblock_viewer.multiblock");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MultiblockDefinition definition, IFocusGroup focuses) {
        state(definition).resetViewportBackground();
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(definition.useLookupItems().stream()
            .map(itemId -> new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId))).toList());
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(definition.recipeLookupItems().stream()
            .map(itemId -> new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(itemId))).toList());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MultiblockDefinition definition, IFocusGroup focuses) {
        ViewerState state = state(definition);
        JeiMultiblockOverlayWidget overlay = new JeiMultiblockOverlayWidget(state);
        builder.addWidget(overlay);
        builder.addGuiEventListener(overlay);
        JeiInspectorWidget inspector = new JeiInspectorWidget(state);
        builder.addWidget(inspector);
        JeiViewportWidget viewport = new JeiViewportWidget(state);
        builder.addWidget(viewport);
        builder.addGuiEventListener(viewport);
        builder.addInputHandler(viewport);
        JeiMaterialStripWidget materials = new JeiMaterialStripWidget(state);
        builder.addWidget(materials);
        builder.addGuiEventListener(materials);
        builder.addInputHandler(materials);
    }

    private ViewerState state(MultiblockDefinition definition) {
        return states.computeIfAbsent(definition.id(), ignored -> new ViewerState(definition));
    }

}
