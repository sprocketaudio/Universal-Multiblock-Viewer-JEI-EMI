package net.sprocketgames.universalmultiblockviewer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.BlockRequirement;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.StructureVariant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MultiblockDefinitionRegistryTest {
    @AfterEach
    void clearRegistry() {
        MultiblockDefinitionRegistry.replace(Map.of());
    }

    @Test
    void explicitLookupItemsNeverTreatBomBlocksAsStructureTriggers() {
        MultiblockDefinition first = definition("first", "test:shared_core");
        MultiblockDefinition second = definition("second", "test:shared_core");
        MultiblockDefinition unrelated = definition("unrelated", "test:other_core");
        var definitions = new LinkedHashMap<ResourceLocation, MultiblockDefinition>();
        definitions.put(first.id(), first);
        definitions.put(second.id(), second);
        definitions.put(unrelated.id(), unrelated);
        MultiblockDefinitionRegistry.replace(definitions);

        assertEquals(List.of(first, second), MultiblockDefinitionRegistry.forUseLookupItem(ResourceLocation.parse("test:shared_core")));
        assertEquals(List.of(), MultiblockDefinitionRegistry.forUseLookupItem(ResourceLocation.parse("minecraft:lodestone")));
        assertEquals(List.of(first, second), MultiblockDefinitionRegistry.forRecipeLookupItem(ResourceLocation.parse("test:shared_core")));
    }

    private static MultiblockDefinition definition(String id, String associatedItem) {
        BlockRequirement requirement = new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
            ResourceLocation.parse("minecraft:lodestone"))), 0, "");
        StructureVariant variant = new StructureVariant("base", "Base", 1, 1, 1,
            Map.of(new GridPos(0, 0, 0), requirement));
        return new MultiblockDefinition(ResourceLocation.parse("test:" + id), id, "",
            List.of(ResourceLocation.parse(associatedItem)), List.of(ResourceLocation.parse(associatedItem)), "base", Map.of("base", variant));
    }
}
