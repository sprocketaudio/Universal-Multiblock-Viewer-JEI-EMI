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
        // Requirements describe positions. The BOM describes items, so orientations and separate
        // requirements that present the same item are deliberately merged.
        Map<MaterialKey, MaterialEntry> counts = new LinkedHashMap<>();
        variant.cells().values().stream().filter(requirement -> requirement.optional() == optional).forEach(requirement ->
            counts.compute(MaterialKey.of(requirement.defaultBlock()), (key, previous) -> previous == null
                ? new MaterialEntry(requirement, 1, 1, key.kind())
                : new MaterialEntry(previous.requirement(), key.kind() == MaterialPresentation.Kind.REUSABLE_TOOL
                    ? 1 : previous.count() + 1, previous.placedCount() + 1, key.kind())));
        List<MaterialEntry> entries = new ArrayList<>();
        entries.addAll(counts.values());
        entries.sort(Comparator.comparing(entry -> MaterialKey.of(entry.requirement().defaultBlock()).itemId().toString()));
        return List.copyOf(entries);
    }

    /** Keyed by the displayed/collected item, never by a rendered state orientation. */
    public record MaterialKey(net.minecraft.resources.ResourceLocation itemId, MaterialPresentation.Kind kind) {
        static MaterialKey of(BlockOption option) {
            var presentation = option.material();
            return new MaterialKey(presentation.item() == null ? option.id() : presentation.item(), presentation.kind());
        }
    }
}
