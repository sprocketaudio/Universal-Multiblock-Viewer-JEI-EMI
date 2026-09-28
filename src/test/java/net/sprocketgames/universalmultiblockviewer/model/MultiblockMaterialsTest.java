package net.sprocketgames.universalmultiblockviewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class MultiblockMaterialsTest {
    @Test
    void alternativesContributeOnlyTheirAuthoredPresentationDefault() {
        BlockRequirement alternatives = new BlockRequirement(List.of(
            new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse("minecraft:stone")),
            new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse("minecraft:granite"))), 0, "Frame");
        Map<GridPos, BlockRequirement> cells = new LinkedHashMap<>();
        cells.put(new GridPos(0, 0, 0), alternatives);
        cells.put(new GridPos(1, 0, 0), alternatives);
        cells.put(new GridPos(2, 0, 0), alternatives);
        StructureVariant variant = new StructureVariant("base", "Base", 3, 1, 1, cells);

        List<MaterialEntry> materials = MultiblockMaterials.forVariant(variant);

        assertEquals(1, materials.size());
        assertEquals(ResourceLocation.parse("minecraft:stone"), materials.getFirst().requirement().defaultBlock().id());
        assertEquals(3, materials.getFirst().count());
    }
}
