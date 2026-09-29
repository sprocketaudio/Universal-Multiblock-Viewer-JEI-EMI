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

    @Test
    void multipleUseAndRecipeItemsRemainIndependentAlongsideOrdinaryVanillaRecipeItems() {
        MultiblockDefinition forge = definition("forge",
            List.of("minecraft:smithing_table", "minecraft:iron_ingot", "minecraft:gold_ingot"),
            List.of("minecraft:iron_ingot", "minecraft:gold_ingot"));
        MultiblockDefinition anvil = definition("anvil",
            List.of("minecraft:iron_ingot"), List.of("minecraft:anvil"));
        Map<ResourceLocation, MultiblockDefinition> definitions = new LinkedHashMap<>();
        definitions.put(forge.id(), forge);
        definitions.put(anvil.id(), anvil);
        MultiblockDefinitionRegistry.replace(definitions);

        // These vanilla items deliberately already have normal JEI/EMI entries. The registry
        // only contributes guides configured for this lookup role; it does not replace recipes.
        assertEquals(List.of(forge, anvil),
            MultiblockDefinitionRegistry.forUseLookupItem(ResourceLocation.parse("minecraft:iron_ingot")));
        assertEquals(List.of(forge),
            MultiblockDefinitionRegistry.forUseLookupItem(ResourceLocation.parse("minecraft:smithing_table")));
        assertEquals(List.of(forge),
            MultiblockDefinitionRegistry.forRecipeLookupItem(ResourceLocation.parse("minecraft:iron_ingot")));
        assertEquals(List.of(forge),
            MultiblockDefinitionRegistry.forRecipeLookupItem(ResourceLocation.parse("minecraft:gold_ingot")));
        assertEquals(List.of(),
            MultiblockDefinitionRegistry.forRecipeLookupItem(ResourceLocation.parse("minecraft:smithing_table")));
    }

    private static MultiblockDefinition definition(String id, String associatedItem) {
        return definition(id, List.of(associatedItem), List.of(associatedItem));
    }

    private static MultiblockDefinition definition(String id, List<String> useItems, List<String> recipeItems) {
        BlockRequirement requirement = new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
            ResourceLocation.parse("minecraft:lodestone"))), 0, "");
        StructureVariant variant = new StructureVariant("base", "Base", 1, 1, 1,
            Map.of(new GridPos(0, 0, 0), requirement));
        return new MultiblockDefinition(ResourceLocation.parse("test:" + id), id, "",
            useItems.stream().map(ResourceLocation::parse).toList(),
            recipeItems.stream().map(ResourceLocation::parse).toList(), "base", Map.of("base", variant));
    }
}
