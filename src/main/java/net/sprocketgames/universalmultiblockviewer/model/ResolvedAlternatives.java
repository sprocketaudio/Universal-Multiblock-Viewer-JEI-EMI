package net.sprocketgames.universalmultiblockviewer.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;

/** Expands tag alternatives into concrete blocks for display and selection. */
public final class ResolvedAlternatives {
    private ResolvedAlternatives() {
    }

    /**
     * Resolves every tag option, places the authored default first, and removes only exact duplicates.
     * Exact block options with different state properties remain separate selectable choices.
     */
    public static BlockRequirement resolve(BlockRequirement requirement,
                                           Function<ResourceLocation, Stream<ResourceLocation>> tagBlocks) {
        return resolve(requirement, (tag, ignoredStates) -> tagBlocks.apply(tag));
    }

    /** Tag state properties are retained and can be used by the caller to filter incompatible members. */
    public static BlockRequirement resolve(BlockRequirement requirement,
                                           BiFunction<ResourceLocation, Map<String, String>, Stream<ResourceLocation>> tagBlocks) {
        Map<ResolvedBlockKey, BlockOption> resolved = new LinkedHashMap<>();
        BlockOption defaultBlock = requirement.defaultBlock();
        addResolved(resolved, defaultBlock, defaultBlock.material(), tagBlocks);
        requirement.options().forEach(option -> addResolved(resolved, option, defaultBlock.material(), tagBlocks));
        if (resolved.isEmpty()) {
            // A tag can be absent during an early resource reload. Retain the authored default so the
            // normal missing-resource fallback is rendered instead of rejecting the whole guide.
            resolved.put(ResolvedBlockKey.of(requirement.defaultBlock()), requirement.defaultBlock());
        }
        return new BlockRequirement(List.copyOf(resolved.values()), 0, requirement.label(), requirement.optional());
    }

    private static void addResolved(Map<ResolvedBlockKey, BlockOption> resolved, BlockOption option,
                                    MaterialPresentation defaultPresentation,
                                    BiFunction<ResourceLocation, Map<String, String>, Stream<ResourceLocation>> tagBlocks) {
        if (option.kind() == BlockOption.Kind.BLOCK) {
            resolved.putIfAbsent(ResolvedBlockKey.of(option), option);
            return;
        }
        MaterialPresentation presentation = option.material().equals(MaterialPresentation.DEFAULT)
            ? defaultPresentation : option.material();
        tagBlocks.apply(option.id(), option.stateProperties())
            .map(id -> new BlockOption(BlockOption.Kind.BLOCK, id, option.stateProperties(), presentation))
            .forEach(expanded -> resolved.putIfAbsent(ResolvedBlockKey.of(expanded), expanded));
    }

    /** Presentation metadata does not make an otherwise identical block state a second valid choice. */
    private record ResolvedBlockKey(ResourceLocation id, Map<String, String> stateProperties) {
        static ResolvedBlockKey of(BlockOption option) { return new ResolvedBlockKey(option.id(), option.stateProperties()); }
    }
}
