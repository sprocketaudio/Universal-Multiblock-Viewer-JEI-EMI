package net.sprocketgames.universalmultiblockviewer.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import org.junit.jupiter.api.Test;

class MultiblockDefinitionParserTest {
    @Test
    void parsesIndependentUseAndRecipeLookups() {
        var definition = parse("""
            {"format":1,"title":"Demo","lookups":{"U":["minecraft:stick"],"R":["minecraft:stick","minecraft:lodestone"]},"variants":[
              {"id":"base","palette":{"A":{"any_of":[{"block":"minecraft:stone"},{"block":"minecraft:dirt"}],"default":"minecraft:stone"}},"layers":[["AA"]]}
            ]}
            """);

        assertEquals(2, definition.initialVariant().cells().size());
        assertEquals("minecraft:stone", definition.initialVariant().cells().values().iterator().next().defaultBlock().id().toString());
        assertEquals(java.util.List.of(ResourceLocation.parse("minecraft:stick")), definition.useLookupItems());
        assertEquals(java.util.List.of(ResourceLocation.parse("minecraft:stick"), ResourceLocation.parse("minecraft:lodestone")), definition.recipeLookupItems());
    }

    @Test
    void parsesOptionalPaletteEntriesWithoutChangingRequiredDefinitions() {
        MultiblockDefinition definition = MultiblockDefinitionParser.parse(ResourceLocation.parse("test:optional"), JsonParser.parseString("""
            {"format":1,"title":"Optional","lookups":{"U":["minecraft:stone"]},"variants":[
              {"id":"base","palette":{
                "R":{"block":"minecraft:stone"},
                "O":{"label":"Optional tagged choice","optional":true,"any_of":[{"tag":"test:optional_blocks"},{"block":"minecraft:dirt"}],"default":"minecraft:dirt"}
              },"layers":[["RO"]]}
            ]}
            """).getAsJsonObject());

        var cells = definition.variants().get("base").cells();
        assertFalse(cells.get(new GridPos(0, 0, 0)).optional());
        assertTrue(cells.get(new GridPos(1, 0, 0)).optional());
        assertEquals(ResourceLocation.parse("minecraft:dirt"), cells.get(new GridPos(1, 0, 0)).defaultBlock().id());
    }

    @Test
    void permitsAnEmptyShortcutWhenTheOtherShortcutHasAnItem() {
        var definition = parse("""
            {"format":1,"title":"Demo","lookups":{"U":[],"R":["minecraft:stick"]},"variants":[
              {"id":"base","palette":{"A":{"block":"minecraft:stone"}},"layers":[["A"]]}
            ]}
            """);
        assertEquals(java.util.List.of(), definition.useLookupItems());
        assertEquals(java.util.List.of(ResourceLocation.parse("minecraft:stick")), definition.recipeLookupItems());
    }

    @Test
    void rejectsTheRetiredAssociatedItemsField() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse("""
            {"format":1,"title":"Old","associated_items":["minecraft:stick"],"variants":[]}
            """));
        assertEquals("associated_items: was replaced by lookups.U and lookups.R", error.getMessage());
    }

    @Test
    void rejectsMissingOrEmptyLookups() {
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> parse("""
            {"format":1,"title":"Missing","variants":[]}
            """));
        assertEquals("lookups: is required", missing.getMessage());

        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class, () -> parse("""
            {"format":1,"title":"Empty","lookups":{"U":[],"R":[]},"variants":[]}
            """));
        assertEquals("lookups: requires at least one item in U or R", empty.getMessage());
    }

    @Test
    void rejectsMalformedUnknownAndDuplicateLookups() {
        IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class, () -> parse("""
            {"format":1,"title":"Bad","lookups":{"U":"minecraft:stick"},"variants":[]}
            """));
        assertEquals("lookups.U: must be an array", malformed.getMessage());

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class, () -> parse("""
            {"format":1,"title":"Bad","lookups":{"X":["minecraft:stick"]},"variants":[]}
            """));
        assertEquals("lookups.X: unknown lookup shortcut; expected U or R", unknown.getMessage());

        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () -> parse("""
            {"format":1,"title":"Bad","lookups":{"U":["minecraft:stick","minecraft:stick"]},"variants":[]}
            """));
        assertEquals("lookups.U: must not contain duplicate item ids", duplicate.getMessage());
    }

    @Test
    void rejectsUnevenRowsWithLocation() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parse("""
            {"format":1,"title":"Bad","lookups":{"U":["minecraft:stick"]},"variants":[
              {"id":"base","palette":{"A":{"block":"minecraft:stone"}},"layers":[["AA","A"]]}
            ]}
            """));
        assertEquals(true, error.getMessage().contains("layers[0][1]"));
    }

    @Test
    void parsesExactBlockStateProperties() {
        var definition = parse("""
            {"format":1,"title":"States","lookups":{"U":["minecraft:stick"]},"variants":[
              {"id":"base","palette":{"A":{"block":"minecraft:oak_log","state":{"axis":"x"}}},"layers":[["A"]]}
            ]}
            """);
        assertEquals("x", definition.initialVariant().cells().values().iterator().next().defaultBlock().stateProperties().get("axis"));
    }

    @Test
    void preservesDistinctStatesOfTheSameExplicitBlockAsAlternatives() {
        var definition = parse("""
            {"format":1,"title":"States","lookups":{"U":["minecraft:stick"]},"variants":[
              {"id":"base","palette":{"A":{"any_of":[
                {"block":"minecraft:oak_log","state":{"axis":"x"}},
                {"block":"minecraft:oak_log","state":{"axis":"z"}}
              ],"default":"minecraft:oak_log"}},"layers":[["A"]]}
            ]}
            """);
        var options = definition.initialVariant().cells().values().iterator().next().options();
        assertEquals(2, options.size());
        assertEquals("x", options.getFirst().stateProperties().get("axis"));
        assertEquals("z", options.get(1).stateProperties().get("axis"));
    }

    private static net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition parse(String json) {
        return MultiblockDefinitionParser.parse(ResourceLocation.parse("test:guide"), JsonParser.parseString(json).getAsJsonObject());
    }
}
