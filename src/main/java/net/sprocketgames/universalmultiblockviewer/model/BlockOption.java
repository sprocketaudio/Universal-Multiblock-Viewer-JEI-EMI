package net.sprocketgames.universalmultiblockviewer.model;

import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** One exact block or block tag accepted by a structure position. */
public record BlockOption(Kind kind, ResourceLocation id, Map<String, String> stateProperties,
                          MaterialPresentation material) {
    public BlockOption(Kind kind, ResourceLocation id) {
        this(kind, id, Map.of(), MaterialPresentation.DEFAULT);
    }

    public BlockOption(Kind kind, ResourceLocation id, Map<String, String> stateProperties) {
        this(kind, id, stateProperties, MaterialPresentation.DEFAULT);
    }

    public BlockOption {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        stateProperties = Map.copyOf(Objects.requireNonNullElse(stateProperties, Map.of()));
        material = Objects.requireNonNullElse(material, MaterialPresentation.DEFAULT);
    }

    public enum Kind {
        BLOCK,
        TAG
    }
}
