package net.sprocketgames.universalmultiblockviewer.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.Comparator;
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
    /** Resource locations retain the creator's namespace/folder for catalogue ordering. */
    private volatile Map<ResourceLocation, ResourceLocation> authoredSources = Map.of();

    private MultiblockDefinitionReloadListener() {
        super(new Gson(), "universal_multiblock_viewer/multiblocks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, MultiblockDefinition> loaded = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> sources = new LinkedHashMap<>();
        int parsed = 0;
        int resolved = 0;
        int accepted = 0;
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation source = entry.getKey();
            MultiblockDefinition definition;
            try {
                if (!entry.getValue().isJsonObject()) {
                    throw new IllegalArgumentException("root: must be an object");
                }
                definition = MultiblockDefinitionParser.parse(source, entry.getValue().getAsJsonObject());
                parsed++;
            } catch (RuntimeException exception) {
                UniversalMultiblockViewer.LOGGER.error("Rejected malformed multiblock definition {}: {}", source, exception.getMessage());
                continue;
            }
            try {
                validateResolvedReferences(definition);
                resolved++;
                warnIfGuideVersionDiffers(definition);
                if (loaded.putIfAbsent(definition.id(), definition) != null) {
                    throw new IllegalArgumentException("duplicate definition id " + definition.id());
                }
                sources.put(definition.id(), source);
                accepted++;
            } catch (RuntimeException exception) {
                UniversalMultiblockViewer.LOGGER.error("Rejected multiblock definition {} during {} validation: {}", source,
                    referenceFailureKind(exception.getMessage()), exception.getMessage());
            }
        }
        authoredDefinitions = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        authoredSources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
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
                validateResolvedReferences(expanded);
                validateMaterials(expanded);
                resolved.put(expanded.id(), expanded);
                bomValid++;
            } catch (RuntimeException exception) {
                UniversalMultiblockViewer.LOGGER.error("Skipping multiblock definition {} after tag expansion: {}",
                    definition.id(), exception.getMessage());
            }
        }
        MultiblockDefinitionRegistry.replace(orderCatalogue(resolved));
        UniversalMultiblockViewer.LOGGER.info("Universal Multiblock Viewer tag expansion health: {} definitions, {} BOM-valid",
            authoredDefinitions.size(), bomValid);
    }

    /** Groups catalogue pages by resource namespace and folder, then orders guide names naturally. */
    private Map<ResourceLocation, MultiblockDefinition> orderCatalogue(Map<ResourceLocation, MultiblockDefinition> definitions) {
        return orderCatalogue(definitions, authoredSources);
    }

    static Map<ResourceLocation, MultiblockDefinition> orderCatalogue(Map<ResourceLocation, MultiblockDefinition> definitions,
                                                                        Map<ResourceLocation, ResourceLocation> sources) {
        Comparator<Map.Entry<ResourceLocation, MultiblockDefinition>> order = Comparator
            .comparing((Map.Entry<ResourceLocation, MultiblockDefinition> entry) -> sourceFor(entry.getKey(), sources).getNamespace())
            .thenComparing(entry -> sourceFolder(sourceFor(entry.getKey(), sources)))
            .thenComparing(entry -> entry.getValue().displayTitle(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Map.Entry::getKey);
        Map<ResourceLocation, MultiblockDefinition> ordered = new LinkedHashMap<>();
        definitions.entrySet().stream().sorted(order).forEach(entry -> ordered.put(entry.getKey(), entry.getValue()));
        return ordered;
    }

    private static ResourceLocation sourceFor(ResourceLocation definitionId, Map<ResourceLocation, ResourceLocation> sources) {
        return sources.getOrDefault(definitionId, definitionId);
    }

    private static String sourceFolder(ResourceLocation source) {
        String path = source.getPath();
        String root = "universal_multiblock_viewer/multiblocks/";
        int rootIndex = path.indexOf(root);
        if (rootIndex >= 0) path = path.substring(rootIndex + root.length());
        int separator = path.lastIndexOf('/');
        return separator < 0 ? "" : path.substring(0, separator);
    }

    private static String referenceFailureKind(String message) {
        if (message != null && message.contains("state property")) return "block-state";
        if (message != null && message.contains("missing block")) return "block reference";
        if (message != null && message.contains("missing item")) return "item lookup/material";
        return "registry reference";
    }

    private static MultiblockDefinition resolveAlternatives(MultiblockDefinition definition, Set<ResourceLocation> missingTags) {
        Map<String, StructureVariant> variants = new LinkedHashMap<>();
        definition.variants().forEach((variantId, variant) -> {
            var cells = new LinkedHashMap<net.sprocketgames.universalmultiblockviewer.model.GridPos,
                net.sprocketgames.universalmultiblockviewer.model.BlockRequirement>();
            variant.cells().forEach((position, requirement) -> cells.put(position,
                ResolvedAlternatives.resolve(requirement,
                    (tag, state) -> blocksInTag(definition.id(), variantId, position, tag, state, missingTags))));
            variants.put(variantId, new StructureVariant(variant.id(), variant.title(), variant.width(),
                variant.height(), variant.depth(), cells));
        });
        return new MultiblockDefinition(definition.id(), definition.title(), definition.description(),
            definition.useLookupItems(), definition.recipeLookupItems(), definition.defaultVariant(), variants,
            definition.titleKey(), definition.descriptionKey(), definition.provenance());
    }

    private static Stream<ResourceLocation> blocksInTag(ResourceLocation definitionId, String variantId,
                                                         net.sprocketgames.universalmultiblockviewer.model.GridPos position,
                                                         ResourceLocation tag, Map<String, String> stateProperties,
                                                         Set<ResourceLocation> missingTags) {
        var values = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, tag));
        if (values.isEmpty()) {
            if (missingTags.add(tag)) {
                UniversalMultiblockViewer.LOGGER.warn(
                    "Definition {} variant '{}' cell {} cannot resolve block tag '{}'; alternatives will refresh when Minecraft updates its tags",
                    definitionId, variantId, position, tag);
            }
            return Stream.empty();
        }
        return values.get().stream().map(holder -> holder.value()).filter(block -> {
            String issue = stateIssue(block, stateProperties);
            if (issue == null) return true;
            UniversalMultiblockViewer.LOGGER.warn(
                "Definition {} variant '{}' cell {} ignores tag member '{}' for tag '{}': {}",
                definitionId, variantId, position, BuiltInRegistries.BLOCK.getKey(block), tag, issue);
            return false;
        }).map(BuiltInRegistries.BLOCK::getKey);
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
                    String issue = stateIssue(block, option.stateProperties());
                    if (issue != null) {
                        throw new IllegalArgumentException("variant '" + variantId + "' cell " + position + " block '" + option.id() + "' " + issue);
                    }
                }
                if (option.material().item() != null && !BuiltInRegistries.ITEM.containsKey(option.material().item())) {
                    throw new IllegalArgumentException("variant '" + variantId + "' cell " + position
                        + " material references missing item '" + option.material().item() + "'");
                }
            })));
    }

    private static String stateIssue(net.minecraft.world.level.block.Block block, Map<String, String> stateProperties) {
        for (var propertyEntry : stateProperties.entrySet()) {
            var property = block.getStateDefinition().getProperty(propertyEntry.getKey());
            if (property == null) return "has no state property '" + propertyEntry.getKey() + "'";
            if (property.getValue(propertyEntry.getValue()).isEmpty()) {
                return "state property '" + propertyEntry.getKey() + "' does not accept '" + propertyEntry.getValue() + "'";
            }
        }
        return null;
    }

    /** Ensures host adapters only receive guides whose BOM exactly covers their cells. */
    private static void validateMaterials(MultiblockDefinition definition) {
        definition.variants().forEach((variantId, variant) -> {
            var materials = MultiblockMaterials.forVariant(variant);
            int representedCells = materials.stream().mapToInt(entry -> entry.placedCount()).sum();
            int requiredCells = (int) variant.cells().values().stream().filter(requirement -> !requirement.optional()).count();
            int optionalCells = (int) variant.cells().values().stream().filter(requirement -> requirement.optional()).count();
            int representedOptionalCells = MultiblockMaterials.optionalForVariant(variant).stream().mapToInt(entry -> entry.placedCount()).sum();
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
