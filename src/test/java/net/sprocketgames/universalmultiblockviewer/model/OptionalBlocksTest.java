package net.sprocketgames.universalmultiblockviewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerLayout;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import org.junit.jupiter.api.Test;

class OptionalBlocksTest {
    private static final BlockOption STONE = block("minecraft:stone");
    private static final BlockOption DIRT = block("minecraft:dirt");

    @Test
    void requiredAndOptionalMaterialsStaySeparateEvenForTheSameBlock() {
        BlockRequirement requiredStone = new BlockRequirement(List.of(STONE), 0, "", false);
        BlockRequirement optionalStone = new BlockRequirement(List.of(STONE), 0, "", true);
        BlockRequirement optionalAlternative = new BlockRequirement(List.of(DIRT, block("minecraft:grass_block")), 0, "", true);
        StructureVariant variant = variant("base", Map.of(
            new GridPos(0, 0, 0), requiredStone,
            new GridPos(1, 0, 0), requiredStone,
            new GridPos(0, 1, 0), optionalStone,
            new GridPos(1, 1, 0), optionalAlternative));

        List<MaterialEntry> required = MultiblockMaterials.forVariant(variant);
        List<MaterialEntry> optional = MultiblockMaterials.optionalForVariant(variant);
        assertEquals(1, required.size());
        assertEquals(2, required.getFirst().count());
        assertEquals(2, optional.size());
        assertEquals(1, optional.stream().filter(entry -> entry.requirement().defaultBlock().equals(STONE)).findFirst().orElseThrow().count());
        assertEquals(DIRT, optional.stream().filter(entry -> entry.requirement().defaultBlock().equals(DIRT)).findFirst().orElseThrow().requirement().defaultBlock());
    }

    @Test
    void optionalTagsResolveToSelectableConcreteBlocks() {
        BlockRequirement taggedOptional = new BlockRequirement(List.of(
            new BlockOption(BlockOption.Kind.TAG, ResourceLocation.parse("test:optional_blocks"))), 0, "", true);

        BlockRequirement resolved = ResolvedAlternatives.resolve(taggedOptional, tag -> Stream.of(
            ResourceLocation.parse("minecraft:stone"), ResourceLocation.parse("minecraft:dirt"), ResourceLocation.parse("minecraft:grass_block")));

        assertTrue(resolved.optional());
        assertEquals(List.of(ResourceLocation.parse("minecraft:stone"), ResourceLocation.parse("minecraft:dirt"),
            ResourceLocation.parse("minecraft:grass_block")), resolved.options().stream().map(BlockOption::id).toList());
    }

    @Test
    void optionalVisibilityControlsSelectionLayersAndVariantChanges() {
        BlockRequirement required = new BlockRequirement(List.of(STONE), 0, "", false);
        BlockRequirement optional = new BlockRequirement(List.of(DIRT), 0, "", true);
        StructureVariant basic = variant("basic", Map.of(
            new GridPos(0, 0, 0), required,
            new GridPos(0, 1, 0), optional));
        StructureVariant upgraded = variant("upgraded", Map.of(
            new GridPos(0, 0, 0), required,
            new GridPos(1, 0, 0), optional));
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:optional"), "Optional", "",
            List.of(ResourceLocation.parse("minecraft:stone")), List.of(), "basic", Map.of("basic", basic, "upgraded", upgraded));
        ViewerState state = new ViewerState(definition);

        assertTrue(state.hasOptionalBlocks());
        assertFalse(state.showOptionalBlocks());
        assertEquals(List.of(new GridPos(0, 0, 0)), ViewerLayout.visibleCells(state));
        state.setLayer(1);
        assertTrue(ViewerLayout.visibleCells(state).isEmpty());
        state.selectOrToggle(new GridPos(0, 1, 0));
        assertNull(state.selected());

        state.toggleOptionalBlocks();
        assertEquals(List.of(new GridPos(0, 1, 0)), ViewerLayout.visibleCells(state));
        state.setLayer(-1);
        state.selectOrToggle(new GridPos(0, 1, 0));
        assertEquals(new GridPos(0, 1, 0), state.selected());
        state.toggleOptionalBlocks();
        assertNull(state.selected());

        state.toggleOptionalBlocks();
        state.setVariant("upgraded");
        assertTrue(state.showOptionalBlocks());
        assertEquals(-1, state.layer());
        assertEquals(2, ViewerLayout.visibleCells(state).size());
    }

    @Test
    void retainsAValidOptionalAlternativeAcrossVariants() {
        BlockRequirement optionalChoices = new BlockRequirement(List.of(DIRT, STONE), 0, "", true);
        StructureVariant first = variant("first", Map.of(new GridPos(0, 0, 0), optionalChoices));
        StructureVariant second = variant("second", Map.of(new GridPos(0, 0, 0), optionalChoices));
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:optional_choice"), "Optional", "",
            List.of(ResourceLocation.parse("minecraft:stone")), List.of(), "first", Map.of("first", first, "second", second));
        ViewerState state = new ViewerState(definition);
        state.toggleOptionalBlocks();
        state.selectOrToggle(new GridPos(0, 0, 0));
        state.setSelectedOption(1);

        state.setVariant("second");

        assertEquals(new GridPos(0, 0, 0), state.selected());
        assertEquals(STONE, state.displayedBlock(state.selected()));
    }

    private static BlockOption block(String id) {
        return new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse(id));
    }

    private static StructureVariant variant(String id, Map<GridPos, BlockRequirement> cells) {
        return new StructureVariant(id, id, 2, 2, 1, cells);
    }
}
