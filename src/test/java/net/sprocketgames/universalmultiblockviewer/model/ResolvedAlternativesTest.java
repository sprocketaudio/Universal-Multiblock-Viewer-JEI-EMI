package net.sprocketgames.universalmultiblockviewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ResolvedAlternativesTest {
    private static final ResourceLocation RUNE_TAG = ResourceLocation.parse("test:altar_runes");
    private static final ResourceLocation BLANK_RUNE = ResourceLocation.parse("minecraft:gold_block");
    private static final ResourceLocation RUNE = ResourceLocation.parse("minecraft:stone");
    private static final ResourceLocation CAPSTONE = ResourceLocation.parse("minecraft:dirt");

    @Test
    void expandsTagsDeduplicatesBlocksAndKeepsTheExplicitDefaultFirst() {
        BlockRequirement requirement = new BlockRequirement(List.of(
            new BlockOption(BlockOption.Kind.BLOCK, BLANK_RUNE),
            new BlockOption(BlockOption.Kind.TAG, RUNE_TAG)), 0, "Altar choice");

        BlockRequirement resolved = ResolvedAlternatives.resolve(requirement, tag -> Stream.of(BLANK_RUNE, RUNE, CAPSTONE));

        assertEquals(List.of(
            new BlockOption(BlockOption.Kind.BLOCK, BLANK_RUNE),
            new BlockOption(BlockOption.Kind.BLOCK, RUNE),
            new BlockOption(BlockOption.Kind.BLOCK, CAPSTONE)), resolved.options());
        assertEquals(0, resolved.defaultOption());
    }

    @Test
    void preservesDistinctBlockStatesAsSeparateChoices() {
        ResourceLocation log = ResourceLocation.parse("minecraft:oak_log");
        BlockOption facingNorth = new BlockOption(BlockOption.Kind.BLOCK, log, Map.of("axis", "x"));
        BlockOption facingSouth = new BlockOption(BlockOption.Kind.BLOCK, log, Map.of("axis", "z"));
        BlockRequirement requirement = new BlockRequirement(List.of(facingNorth, facingSouth,
            new BlockOption(BlockOption.Kind.TAG, RUNE_TAG)), 1, "Rune");

        BlockRequirement resolved = ResolvedAlternatives.resolve(requirement, tag -> Stream.of(log));

        assertEquals(List.of(facingSouth, facingNorth,
            new BlockOption(BlockOption.Kind.BLOCK, log)), resolved.options());
        assertEquals(0, resolved.defaultOption());
    }

    @Test
    void retainsTagStateAndMaterialPresentationAfterExpansion() {
        var tool = new MaterialPresentation(ResourceLocation.parse("minecraft:flint_and_steel"), MaterialPresentation.Kind.REUSABLE_TOOL);
        BlockRequirement requirement = new BlockRequirement(List.of(
            new BlockOption(BlockOption.Kind.TAG, RUNE_TAG, Map.of("axis", "y"), tool)), 0, "Marked block");

        BlockRequirement resolved = ResolvedAlternatives.resolve(requirement,
            (tag, state) -> Stream.of(BLANK_RUNE, RUNE));

        assertEquals(Map.of("axis", "y"), resolved.options().getFirst().stateProperties());
        assertEquals(tool, resolved.options().getFirst().material());
        assertEquals(List.of(BLANK_RUNE, RUNE), resolved.options().stream().map(BlockOption::id).toList());
    }

    @Test
    void inheritsTheExplicitDefaultPresentationForUnmappedTagMembers() {
        var chalk = new MaterialPresentation(ResourceLocation.parse("minecraft:stick"), MaterialPresentation.Kind.REUSABLE_TOOL);
        BlockOption defaultGlyph = new BlockOption(BlockOption.Kind.BLOCK, BLANK_RUNE, Map.of(), chalk);
        BlockRequirement requirement = new BlockRequirement(List.of(defaultGlyph,
            new BlockOption(BlockOption.Kind.TAG, RUNE_TAG)), 0, "Glyph");

        BlockRequirement resolved = ResolvedAlternatives.resolve(requirement, tag -> Stream.of(BLANK_RUNE, RUNE, CAPSTONE));

        assertEquals(List.of(chalk, chalk, chalk), resolved.options().stream().map(BlockOption::material).toList());
    }
}
