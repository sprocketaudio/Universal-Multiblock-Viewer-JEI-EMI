package net.sprocketgames.universalmultiblockviewer.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;
import net.sprocketgames.universalmultiblockviewer.data.MultiblockDefinitionRegistry;

/** Optional EMI entry point. EMI discovers this class only when EMI itself is installed. */
@EmiEntrypoint
public final class UniversalMultiblockViewerEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
        ResourceLocation.fromNamespaceAndPath(UniversalMultiblockViewer.MOD_ID, "multiblock"), EmiStack.of(Items.SPYGLASS));
    private static boolean inputHandlersRegistered;

    @Override
    public void register(EmiRegistry registry) {
        registerInputHandlers();
        registry.addCategory(CATEGORY);
        var definitions = MultiblockDefinitionRegistry.all();
        UniversalMultiblockViewer.LOGGER.info("Universal Multiblock Viewer EMI registered {} recipe(s)", definitions.size());
        definitions.forEach(definition -> {
            registry.addRecipe(new EmiMultiblockRecipe(definition));
        });
    }

    private static void registerInputHandlers() {
        if (inputHandlersRegistered) return;
        inputHandlersRegistered = true;
        NeoForge.EVENT_BUS.addListener(EmiViewportInputHandler::onMouseScrolled);
        NeoForge.EVENT_BUS.addListener(EmiViewportInputHandler::onMousePressed);
        NeoForge.EVENT_BUS.addListener(EmiViewportInputHandler::onMouseDragged);
        NeoForge.EVENT_BUS.addListener(EmiViewportInputHandler::onMouseReleased);
        NeoForge.EVENT_BUS.addListener(EmiViewportInputHandler::tooltip);
        NeoForge.EVENT_BUS.addListener(EmiMaterialStripInputHandler::click);
        NeoForge.EVENT_BUS.addListener(EmiMaterialStripInputHandler::scroll);
        NeoForge.EVENT_BUS.addListener(EmiMaterialStripInputHandler::drag);
        NeoForge.EVENT_BUS.addListener(EmiMaterialStripInputHandler::tooltip);
        NeoForge.EVENT_BUS.addListener(EmiLayerInputHandler::click);
        NeoForge.EVENT_BUS.addListener(EmiLayerInputHandler::tooltip);
    }
}
