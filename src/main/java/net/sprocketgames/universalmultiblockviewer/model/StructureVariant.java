package net.sprocketgames.universalmultiblockviewer.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;

/** Immutable resolved grid for one named structure variant. Empty cells are not stored. */
public record StructureVariant(String id, String title, int width, int height, int depth,
                               Map<GridPos, BlockRequirement> cells) {
    public StructureVariant {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("variant id may not be blank");
        }
        title = Objects.requireNonNullElse(title, id);
        if (width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException("variant dimensions must be positive");
        }
        cells = Collections.unmodifiableMap(new LinkedHashMap<>(cells));
    }
}
