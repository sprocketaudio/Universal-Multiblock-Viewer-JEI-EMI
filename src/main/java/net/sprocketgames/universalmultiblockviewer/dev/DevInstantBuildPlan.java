package net.sprocketgames.universalmultiblockviewer.dev;

import java.util.ArrayList;
import java.util.List;
import net.sprocketgames.universalmultiblockviewer.model.BlockRequirement;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

/** The client-side placement plan derived solely from the currently resolved viewer state. */
public record DevInstantBuildPlan(String title, List<Placement> placements) {
    public record Placement(GridPos offset, BlockOption option) { }

    public static DevInstantBuildPlan from(ViewerState state) {
        List<Placement> placements = new ArrayList<>();
        state.variant().cells().forEach((position, requirement) -> {
            if (!requirement.optional() || state.showOptionalBlocks()) {
                placements.add(new Placement(position, state.displayedBlock(position)));
            }
        });
        return new DevInstantBuildPlan(state.definition().displayTitle(), List.copyOf(placements));
    }

    /** Rotates the footprint around its lower corner and normalises it back to a zero-based anchor. */
    public List<Placement> rotatedPlacements(int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        List<Placement> rotated = placements.stream().map(placement -> new Placement(rotate(placement.offset(), turns), placement.option())).toList();
        int minX = rotated.stream().mapToInt(placement -> placement.offset().x()).min().orElse(0);
        int minZ = rotated.stream().mapToInt(placement -> placement.offset().z()).min().orElse(0);
        return rotated.stream().map(placement -> new Placement(new GridPos(
            placement.offset().x() - minX, placement.offset().y(), placement.offset().z() - minZ), placement.option())).toList();
    }

    private static GridPos rotate(GridPos offset, int turns) {
        return switch (turns) {
            case 1 -> new GridPos(-offset.z(), offset.y(), offset.x());
            case 2 -> new GridPos(-offset.x(), offset.y(), -offset.z());
            case 3 -> new GridPos(offset.z(), offset.y(), -offset.x());
            default -> offset;
        };
    }
}
