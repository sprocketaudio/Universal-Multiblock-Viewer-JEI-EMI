package net.sprocketgames.universalmultiblockviewer.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.BlockRequirement;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.StructureVariant;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;
import org.junit.jupiter.api.Test;

class DevInstantBuildPlanTest {
    @Test void usesTheResolvedChoicesAndOptionalToggleForTheWholeVariant() {
        GridPos required = new GridPos(0, 0, 0); GridPos optional = new GridPos(1, 2, 0);
        BlockOption stone = new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse("minecraft:stone"));
        BlockOption granite = new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse("minecraft:granite"));
        Map<GridPos, BlockRequirement> cells = new LinkedHashMap<>();
        cells.put(required, new BlockRequirement(List.of(stone, granite), 0, "", false));
        cells.put(optional, new BlockRequirement(List.of(granite), 0, "", true));
        StructureVariant variant = new StructureVariant("base", "Base", 2, 3, 1, cells);
        ViewerState state = new ViewerState(new MultiblockDefinition(ResourceLocation.parse("test:plan"), "Test Build", "", List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base", variant)));
        state.selectOrToggle(required); state.setSelectedOption(1); state.setLayer(0);
        assertEquals(List.of(granite), DevInstantBuildPlan.from(state).placements().stream().map(DevInstantBuildPlan.Placement::option).toList());
        state.toggleOptionalBlocks();
        assertEquals(List.of(granite, granite), DevInstantBuildPlan.from(state).placements().stream().map(DevInstantBuildPlan.Placement::option).toList());
    }

    @Test void rotatesAndNormalisesTheBuildFootprint() {
        BlockOption stone = new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse("minecraft:stone"));
        DevInstantBuildPlan plan = new DevInstantBuildPlan("Rotation", List.of(
            new DevInstantBuildPlan.Placement(new GridPos(0, 0, 0), stone),
            new DevInstantBuildPlan.Placement(new GridPos(2, 0, 1), stone)
        ));

        assertEquals(List.of(new GridPos(1, 0, 0), new GridPos(0, 0, 2)),
            plan.rotatedPlacements(1).stream().map(DevInstantBuildPlan.Placement::offset).toList());
        assertEquals(List.of(new GridPos(2, 0, 1), new GridPos(0, 0, 0)),
            plan.rotatedPlacements(2).stream().map(DevInstantBuildPlan.Placement::offset).toList());
    }
}
