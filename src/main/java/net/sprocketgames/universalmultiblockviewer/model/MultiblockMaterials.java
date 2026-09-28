package net.sprocketgames.universalmultiblockviewer.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calculates BOM entries from a single resolved variant, so alternatives cannot inflate totals. */
public final class MultiblockMaterials {
    private MultiblockMaterials() {
    }

    public static List<MaterialEntry> forVariant(StructureVariant variant) {
        Map<BlockRequirement, Integer> counts = new LinkedHashMap<>();
        variant.cells().values().forEach(requirement -> counts.merge(requirement, 1, Integer::sum));
        List<MaterialEntry> entries = new ArrayList<>();
        counts.forEach((requirement, count) -> entries.add(new MaterialEntry(requirement, count)));
        entries.sort(Comparator.comparing(entry -> entry.requirement().defaultBlock().id().toString()));
        return List.copyOf(entries);
    }
}
