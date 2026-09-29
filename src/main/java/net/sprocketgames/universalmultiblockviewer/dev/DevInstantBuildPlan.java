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
}
