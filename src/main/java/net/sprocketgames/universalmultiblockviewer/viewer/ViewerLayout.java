package net.sprocketgames.universalmultiblockviewer.viewer;

import java.util.Comparator;
import java.util.List;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;

/** Produces the cells visible in the active all-layer or single-layer view. */
public final class ViewerLayout {
    private ViewerLayout() {
    }

    public static List<GridPos> visibleCells(ViewerState state) {
        return state.variant().cells().keySet().stream()
            .filter(position -> state.layer() < 0 || position.y() == state.layer())
            .filter(position -> state.showOptionalBlocks() || !state.variant().cells().get(position).optional())
            .sorted(Comparator.comparingInt((GridPos position) -> position.y() + position.z()).thenComparingInt(GridPos::x))
            .toList();
    }
}
