package net.sprocketgames.universalmultiblockviewer.model;

import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** One exact block or block tag accepted by a structure position. */
public record BlockOption(Kind kind, ResourceLocation id, Map<String, String> stateProperties) {
    public BlockOption(Kind kind, ResourceLocation id) {
        this(kind, id, Map.of());
    }

    public BlockOption {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        stateProperties = Map.copyOf(Objects.requireNonNullElse(stateProperties, Map.of()));
        if (kind == Kind.TAG && !stateProperties.isEmpty()) {
            throw new IllegalArgumentException("a tag option cannot declare block-state properties");
        }
    }

    public enum Kind {
        BLOCK,
        TAG
    }
}
