package net.sprocketgames.universalmultiblockviewer.model;

import java.util.List;
import java.util.Objects;

/** A palette entry and its presentation default. Alternatives are never double-counted in the BOM. */
public record BlockRequirement(List<BlockOption> options, int defaultOption, String label, boolean optional) {
    public BlockRequirement(List<BlockOption> options, int defaultOption, String label) {
        this(options, defaultOption, label, false);
    }
    public BlockRequirement {
        options = List.copyOf(options);
        if (options.isEmpty()) {
            throw new IllegalArgumentException("a block requirement needs at least one option");
        }
        if (defaultOption < 0 || defaultOption >= options.size()) {
            throw new IllegalArgumentException("default option is outside the option list");
        }
        label = Objects.requireNonNullElse(label, "");
    }

    public BlockOption defaultBlock() {
        return options.get(defaultOption);
    }
}
