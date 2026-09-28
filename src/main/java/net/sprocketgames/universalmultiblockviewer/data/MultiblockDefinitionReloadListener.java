package net.sprocketgames.universalmultiblockviewer.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.fml.ModList;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockMaterials;

/** Loads client resource definitions from the universal_multiblock_viewer/multiblocks directory. */
public final class MultiblockDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    public static final MultiblockDefinitionReloadListener INSTANCE = new MultiblockDefinitionReloadListener();

    private MultiblockDefinitionReloadListener() {
        super(new Gson(), "universal_multiblock_viewer/multiblocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, MultiblockDefinition> loaded = new LinkedHashMap<>();
        int parsed = 0;
        int resolved = 0;
        int bomValid = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation source = entry.getKey();
            try {
                if (!entry.getValue().isJsonObject()) {
                    throw new IllegalArgumentException("root: must be an object");
                }
                MultiblockDefinition definition = MultiblockDefinitionParser.parse(source, entry.getValue().getAsJsonObject());
                parsed++;
                validateResolvedReferences(definition);
                resolved++;
                validateMaterials(definition);
                bomValid++;
                warnIfGuideVersionDiffers(definition);
                if (loaded.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("duplicate definition id " + definition.id());
                }
            } catch (RuntimeException exception) {
                UniversalMultiblockViewer.LOGGER.error("Skipping invalid multiblock definition {}: {}", source, exception.getMessage());
            }
        }
        MultiblockDefinitionRegistry.replace(loaded);
        UniversalMultiblockViewer.LOGGER.info("Universal Multiblock Viewer definition health: discovered {}, parsed {}, resolved {}, BOM-valid {}, rejected {}",
            resources.size(), parsed, resolved, bomValid, resources.size() - bomValid);
    }

    private static void validateResolvedReferences(MultiblockDefinition definition) {
        for (ResourceLocation item : definition.useLookupItems()) {
            if (!BuiltInRegistries.ITEM.containsKey(item)) {
                throw new IllegalArgumentException("lookups.U references missing item '" + item + "'");
            }
        }
        for (ResourceLocation item : definition.recipeLookupItems()) {
            if (!BuiltInRegistries.ITEM.containsKey(item)) {
                throw new IllegalArgumentException("lookups.R references missing item '" + item + "'");
            }
        }
        definition.variants().forEach((variantId, variant) -> variant.cells().forEach((position, requirement) ->
            requirement.options().forEach(option -> {
                if (option.kind() == net.sprocketgames.universalmultiblockviewer.model.BlockOption.Kind.BLOCK
                    && !BuiltInRegistries.BLOCK.containsKey(option.id())) {
                    throw new IllegalArgumentException("variant '" + variantId + "' cell " + position + " references missing block '" + option.id() + "'");
                }
                if (option.kind() == net.sprocketgames.universalmultiblockviewer.model.BlockOption.Kind.BLOCK) {
                    var block = BuiltInRegistries.BLOCK.get(option.id());
                    for (var propertyEntry : option.stateProperties().entrySet()) {
                        var property = block.getStateDefinition().getProperty(propertyEntry.getKey());
                        if (property == null) {
                            throw new IllegalArgumentException("variant '" + variantId + "' cell " + position + " block '" + option.id()
                                + "' has no state property '" + propertyEntry.getKey() + "'");
                        }
                        if (property.getValue(propertyEntry.getValue()).isEmpty()) {
                            throw new IllegalArgumentException("variant '" + variantId + "' cell " + position + " block '" + option.id()
                                + "' state property '" + propertyEntry.getKey() + "' does not accept '" + propertyEntry.getValue() + "'");
                        }
                    }
                }
                if (option.kind() == net.sprocketgames.universalmultiblockviewer.model.BlockOption.Kind.TAG
                    && BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, option.id())).isEmpty()) {
                    UniversalMultiblockViewer.LOGGER.warn("Definition {} variant '{}' cell {} references block tag '{}' before client tags are available; it will resolve when tags load, or render as a missing-block fallback if it remains unavailable",
                        definition.id(), variantId, position, option.id());
                }
            })));
    }

    /** Ensures host adapters only receive guides whose BOM exactly covers their cells. */
    private static void validateMaterials(MultiblockDefinition definition) {
        definition.variants().forEach((variantId, variant) -> {
            var materials = MultiblockMaterials.forVariant(variant);
            int representedCells = materials.stream().mapToInt(entry -> entry.count()).sum();
            if (materials.isEmpty() || representedCells != variant.cells().size()) {
                throw new IllegalArgumentException("variant '" + variantId + "' has an inconsistent bill of materials");
            }
        });
    }

    /** Provenance is advisory documentation only; it never controls registration or machine behaviour. */
    private static void warnIfGuideVersionDiffers(MultiblockDefinition definition) {
        var provenance = definition.provenance();
        if (provenance.sourceMod().isBlank() || provenance.testedVersion().isBlank()) {
            return;
        }
        ModList.get().getModContainerById(provenance.sourceMod()).ifPresent(container -> {
            String installed = container.getModInfo().getVersion().toString();
            if (!installed.equals(provenance.testedVersion())) {
                UniversalMultiblockViewer.LOGGER.warn(
                    "Guide {} was authored against {} {}; installed version is {}. The guide remains a read-only reference and is not machine validation.",
                    definition.id(), provenance.sourceMod(), provenance.testedVersion(), installed);
            }
        });
    }
}
