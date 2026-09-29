package net.sprocketgames.universalmultiblockviewer.integration.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;
import net.sprocketgames.universalmultiblockviewer.data.MultiblockDefinitionRegistry;

/** Optional JEI discovery point; the adapter registers only parsed, validated viewer definitions. */
@JeiPlugin
public final class UniversalMultiblockViewerJeiPlugin implements IModPlugin {
    private static volatile IJeiRuntime runtime;
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
        UniversalMultiblockViewer.MOD_ID,
        "jei_plugin"
    );

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) { UniversalMultiblockViewerJeiPlugin.runtime = runtime; }

    static IJeiRuntime runtime() { return runtime; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new JeiMultiblockCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var definitions = MultiblockDefinitionRegistry.all();
        int useItems = definitions.stream().mapToInt(definition -> definition.useLookupItems().size()).sum();
        int recipeItems = definitions.stream().mapToInt(definition -> definition.recipeLookupItems().size()).sum();
        UniversalMultiblockViewer.LOGGER.info(
            "Universal Multiblock Viewer JEI registered {} guide(s), indexed {} U item(s) and {} R item(s)",
            definitions.size(), useItems, recipeItems
        );
        registration.addRecipes(JeiMultiblockCategory.TYPE, definitions);
    }

}
