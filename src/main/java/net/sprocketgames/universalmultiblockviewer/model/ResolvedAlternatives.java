package net.sprocketgames.universalmultiblockviewer.model;

import java.util.LinkedHashSet;
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
        LinkedHashSet<BlockOption> resolved = new LinkedHashSet<>();
        addResolved(resolved, requirement.defaultBlock(), tagBlocks);
        requirement.options().forEach(option -> addResolved(resolved, option, tagBlocks));
        if (resolved.isEmpty()) {
            // A tag can be absent during an early resource reload. Retain the authored default so the
            // normal missing-resource fallback is rendered instead of rejecting the whole guide.
            resolved.add(requirement.defaultBlock());
        }
        return new BlockRequirement(List.copyOf(resolved), 0, requirement.label(), requirement.optional());
    }

    private static void addResolved(LinkedHashSet<BlockOption> resolved, BlockOption option,
                                    Function<ResourceLocation, Stream<ResourceLocation>> tagBlocks) {
        if (option.kind() == BlockOption.Kind.BLOCK) {
            resolved.add(option);
            return;
        }
        tagBlocks.apply(option.id()).map(id -> new BlockOption(BlockOption.Kind.BLOCK, id)).forEach(resolved::add);
    }
}
