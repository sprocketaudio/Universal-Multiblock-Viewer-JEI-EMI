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
        return entries(variant, false);
    }

    /** Optional cells are intentionally calculated separately from the required BOM. */
    public static List<MaterialEntry> optionalForVariant(StructureVariant variant) {
        return entries(variant, true);
    }

    private static List<MaterialEntry> entries(StructureVariant variant, boolean optional) {
        // Requirements describe position semantics. The BOM describes presentation materials, so
        // distinct positions that share an authored default must be counted in one stack.
        Map<BlockOption, MaterialEntry> counts = new LinkedHashMap<>();
        variant.cells().values().stream().filter(requirement -> requirement.optional() == optional).forEach(requirement ->
            counts.compute(requirement.defaultBlock(), (block, previous) -> previous == null
                ? new MaterialEntry(requirement, 1)
                : new MaterialEntry(previous.requirement(), previous.count() + 1)));
        List<MaterialEntry> entries = new ArrayList<>();
        entries.addAll(counts.values());
        entries.sort(Comparator.comparing(entry -> entry.requirement().defaultBlock().id().toString()));
        return List.copyOf(entries);
    }
}
