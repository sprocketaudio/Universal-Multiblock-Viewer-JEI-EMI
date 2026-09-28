package net.sprocketgames.universalmultiblockviewer.data;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;

/** Client-side snapshot replaced atomically after each resource reload. */
public final class MultiblockDefinitionRegistry {
    private static volatile Map<ResourceLocation, MultiblockDefinition> definitions = Map.of();

    private MultiblockDefinitionRegistry() {
    }

    public static void replace(Map<ResourceLocation, MultiblockDefinition> loaded) {
        definitions = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
    }

    public static List<MultiblockDefinition> all() {
        return List.copyOf(definitions.values());
    }

    public static Collection<MultiblockDefinition> forUseLookupItem(ResourceLocation itemId) {
        return definitions.values().stream().filter(definition -> definition.useLookupItems().contains(itemId)).toList();
    }

    public static Collection<MultiblockDefinition> forRecipeLookupItem(ResourceLocation itemId) {
        return definitions.values().stream().filter(definition -> definition.recipeLookupItems().contains(itemId)).toList();
    }
}
