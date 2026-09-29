package net.sprocketgames.universalmultiblockviewer.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
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
import net.sprocketgames.universalmultiblockviewer.model.ResolvedAlternatives;
import net.sprocketgames.universalmultiblockviewer.model.StructureVariant;

/** Loads client resource definitions from the universal_multiblock_viewer/multiblocks directory. */
public final class MultiblockDefinitionReloadListener extends SimpleJsonResourceReloadListener {
    public static final MultiblockDefinitionReloadListener INSTANCE = new MultiblockDefinitionReloadListener();
    /**
     * Keep the authored form separate from its registry-dependent display form. Block tags are
     * synchronised after client resources on a remote server, so resolving only while parsing
     * can otherwise permanently leave a guide with an incomplete alternative list.
     */
    private volatile Map<ResourceLocation, MultiblockDefinition> authoredDefinitions = Map.of();

    private MultiblockDefinitionReloadListener() {
        super(new Gson(), "universal_multiblock_viewer/multiblocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, MultiblockDefinition> loaded = new LinkedHashMap<>();
        int parsed = 0;
        int resolved = 0;
        int accepted = 0;
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
                warnIfGuideVersionDiffers(definition);
                if (loaded.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("duplicate definition id " + definition.id());
                }
                accepted++;
            } catch (RuntimeException exception) {
                UniversalMultiblockViewer.LOGGER.error("Skipping invalid multiblock definition {}: {}", source, exception.getMessage());
            }
        }
        authoredDefinitions = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        refreshResolvedDefinitions();
        UniversalMultiblockViewer.LOGGER.info("Universal Multiblock Viewer definition health: discovered {}, parsed {}, resolved {}, accepted {}, rejected {}",
            resources.size(), parsed, resolved, accepted, resources.size() - accepted);
    }

    /** Rebuilds concrete alternatives after Minecraft has rebound the live block tags. */
    public void refreshResolvedDefinitions() {
        Map<ResourceLocation, MultiblockDefinition> resolved = new LinkedHashMap<>();
        Set<ResourceLocation> missingTags = ConcurrentHashMap.newKeySet();
        int bomValid = 0;
        for (MultiblockDefinition definition : authoredDefinitions.values()) {
            try {
                MultiblockDefinition expanded = resolveAlternatives(definition, missingTags);
                validateMaterials(expanded);
                resolved.put(expanded.id(), expanded);
                bomValid++;
            } catch (RuntimeException exception) {
                UniversalMultiblockViewer.LOGGER.error("Skipping multiblock definition {} after tag expansion: {}",
                    definition.id(), exception.getMessage());
            }
        }
        MultiblockDefinitionRegistry.replace(resolved);
        UniversalMultiblockViewer.LOGGER.info("Universal Multiblock Viewer tag expansion health: {} definitions, {} BOM-valid",
            authoredDefinitions.size(), bomValid);
    }

    private static MultiblockDefinition resolveAlternatives(MultiblockDefinition definition, Set<ResourceLocation> missingTags) {
        Map<String, StructureVariant> variants = new LinkedHashMap<>();
        definition.variants().forEach((variantId, variant) -> {
            var cells = new LinkedHashMap<net.sprocketgames.universalmultiblockviewer.model.GridPos,
                net.sprocketgames.universalmultiblockviewer.model.BlockRequirement>();
            variant.cells().forEach((position, requirement) -> cells.put(position,
                ResolvedAlternatives.resolve(requirement,
                    tag -> blocksInTag(definition.id(), variantId, position, tag, missingTags))));
            variants.put(variantId, new StructureVariant(variant.id(), variant.title(), variant.width(),
                variant.height(), variant.depth(), cells));
        });
        return new MultiblockDefinition(definition.id(), definition.title(), definition.description(),
            definition.useLookupItems(), definition.recipeLookupItems(), definition.defaultVariant(), variants,
            definition.titleKey(), definition.descriptionKey(), definition.provenance());
    }

    private static Stream<ResourceLocation> blocksInTag(ResourceLocation definitionId, String variantId,
                                                         net.sprocketgames.universalmultiblockviewer.model.GridPos position,
                                                         ResourceLocation tag, Set<ResourceLocation> missingTags) {
        var values = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, tag));
        if (values.isEmpty()) {
            if (missingTags.add(tag)) {
                UniversalMultiblockViewer.LOGGER.warn(
                    "Definition {} variant '{}' cell {} cannot resolve block tag '{}'; alternatives will refresh when Minecraft updates its tags",
                    definitionId, variantId, position, tag);
            }
            return Stream.empty();
        }
        return values.get().stream().map(holder -> BuiltInRegistries.BLOCK.getKey(holder.value()));
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
            })));
    }

    /** Ensures host adapters only receive guides whose BOM exactly covers their cells. */
    private static void validateMaterials(MultiblockDefinition definition) {
        definition.variants().forEach((variantId, variant) -> {
            var materials = MultiblockMaterials.forVariant(variant);
            int representedCells = materials.stream().mapToInt(entry -> entry.count()).sum();
            int requiredCells = (int) variant.cells().values().stream().filter(requirement -> !requirement.optional()).count();
            int optionalCells = (int) variant.cells().values().stream().filter(requirement -> requirement.optional()).count();
            int representedOptionalCells = MultiblockMaterials.optionalForVariant(variant).stream().mapToInt(entry -> entry.count()).sum();
            if ((requiredCells > 0 && materials.isEmpty()) || representedCells != requiredCells
                || representedOptionalCells != optionalCells) {
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
