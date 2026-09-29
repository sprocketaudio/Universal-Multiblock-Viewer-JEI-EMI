package net.sprocketgames.universalmultiblockviewer.model;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Describes the item shown in the BOM and how obtaining it relates to a placed position. */
public record MaterialPresentation(ResourceLocation item, Kind kind) {
    public static final MaterialPresentation DEFAULT = new MaterialPresentation(null, Kind.PLACED_BLOCK);

    public MaterialPresentation {
        kind = Objects.requireNonNullElse(kind, Kind.PLACED_BLOCK);
        if (item == null && kind != Kind.PLACED_BLOCK) {
            throw new IllegalArgumentException(kind + " material presentation requires an item");
        }
    }

    public enum Kind {
        /** One placeable material is required for every matching position. */
        PLACED_BLOCK,
        /** A separate, consumed item is required for every matching position. */
        REQUIRED_ITEM,
        /** One reusable item is required regardless of how many matching positions exist. */
        REUSABLE_TOOL
    }
}
