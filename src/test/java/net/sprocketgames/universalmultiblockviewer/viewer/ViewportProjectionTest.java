package net.sprocketgames.universalmultiblockviewer.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.BlockRequirement;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.StructureVariant;
import org.junit.jupiter.api.Test;

class ViewportProjectionTest {
    private static final GridPos CELL = new GridPos(0, 0, 0);

    @Test
    void centreOfAOneBlockPreviewSelectsThatExactCell() {
        ViewerState state = new ViewerState(definition());

        assertEquals(CELL, ViewportProjection.findCell(state, 84, 58, 168, 116));
    }

    @Test
    void blankSpaceDoesNotSelectAVisibleCell() {
        ViewerState state = new ViewerState(definition());

        assertNull(ViewportProjection.findCell(state, 4, 4, 168, 116));
    }

    private static MultiblockDefinition definition() {
        BlockRequirement block = new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
            ResourceLocation.parse("minecraft:stone"))), 0, "");
        return new MultiblockDefinition(ResourceLocation.parse("test:projection"), "Projection", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base",
                new StructureVariant("base", "Base", 1, 1, 1, Map.of(CELL, block))));
    }
}
