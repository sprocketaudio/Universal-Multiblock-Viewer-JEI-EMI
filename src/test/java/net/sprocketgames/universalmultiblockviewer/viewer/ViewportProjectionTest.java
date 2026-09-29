package net.sprocketgames.universalmultiblockviewer.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
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

        assertEquals(CELL, ViewportProjection.findCell(state, 84, 58, 168, 116,
            ignored -> List.of(new AABB(0, 0, 0, 1, 1, 1))));
    }

    @Test
    void blankSpaceDoesNotSelectAVisibleCell() {
        ViewerState state = new ViewerState(definition());

        assertNull(ViewportProjection.findCell(state, 4, 4, 168, 116,
            ignored -> List.of(new AABB(0, 0, 0, 1, 1, 1))));
    }

    @Test
    void emptyPartOfANonFullBlockCellDoesNotCaptureTheClick() {
        ViewerState state = new ViewerState(definition());

        assertNull(ViewportProjection.findCell(state, 84, 58, 168, 116,
            ignored -> List.of(new AABB(0, 0, 0, 0.1D, 0.1D, 0.1D))));
    }

    @Test
    void visiblePartOfANonFullBlockCellRemainsSelectable() {
        ViewerState state = new ViewerState(definition());

        assertEquals(CELL, ViewportProjection.findCell(state, 84, 58, 168, 116,
            ignored -> List.of(new AABB(0.4D, 0.4D, 0.4D, 0.6D, 0.6D, 0.6D))));
    }

    @Test
    void emptyPartOfAFrontPartialBlockAllowsSelectingTheBlockBehind() {
        GridPos first = new GridPos(0, 0, 0);
        GridPos second = new GridPos(1, 0, 1);
        ViewerState state = new ViewerState(definition(2, 1, 2, Map.of(
            first, requirement(),
            second, requirement()
        )));
        AABB fullCube = new AABB(0, 0, 0, 1, 1, 1);
        AABB tinyCorner = new AABB(0, 0, 0, 0.05D, 0.05D, 0.05D);

        for (int mouseY = 0; mouseY < 116; mouseY++) {
            for (int mouseX = 0; mouseX < 168; mouseX++) {
                GridPos firstOnly = ViewportProjection.findCell(state, mouseX, mouseY, 168, 116,
                    position -> position.equals(first) ? List.of(fullCube) : List.of());
                GridPos secondOnly = ViewportProjection.findCell(state, mouseX, mouseY, 168, 116,
                    position -> position.equals(second) ? List.of(fullCube) : List.of());
                if (firstOnly == null || secondOnly == null) continue;

                GridPos front = ViewportProjection.findCell(state, mouseX, mouseY, 168, 116,
                    ignored -> List.of(fullCube));
                GridPos behind = front.equals(first) ? second : first;
                GridPos throughPartial = ViewportProjection.findCell(state, mouseX, mouseY, 168, 116,
                    position -> position.equals(front) ? List.of(tinyCorner) : List.of(fullCube));
                if (behind.equals(throughPartial)) return;
            }
        }

        fail("Expected a click through empty partial-block space to select the block behind");
    }

    private static MultiblockDefinition definition() {
        return definition(1, 1, 1, Map.of(CELL, requirement()));
    }

    private static MultiblockDefinition definition(int width, int height, int depth,
                                                    Map<GridPos, BlockRequirement> blocks) {
        return new MultiblockDefinition(ResourceLocation.parse("test:projection"), "Projection", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base",
                new StructureVariant("base", "Base", width, height, depth, blocks)));
    }

    private static BlockRequirement requirement() {
        return new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
            ResourceLocation.parse("minecraft:stone"))), 0, "");
    }
}
