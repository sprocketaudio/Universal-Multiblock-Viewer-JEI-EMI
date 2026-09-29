package net.sprocketgames.universalmultiblockviewer.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.sprocketgames.universalmultiblockviewer.model.MaterialPresentation;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

/** One-column viewport-local alternative list; Help remains independent. */
public final class SelectedBlockOptions {
    public static final int X = 4;
    public static final int Y = 4;
    public static final int VISIBLE = 5;
    private static final int CELL = 18;
    private static final int STEP = 19;
    private static final int ITEM_BOTTOM = Y + VISIBLE * STEP + 8;
    private static final int SCROLL_X = X + CELL + 2;

    private SelectedBlockOptions() { }

    public static void render(ViewerState state, GuiGraphics graphics) {
        render(state, graphics, 0, 0);
    }

    /** Draws choices in viewport-local coordinates, allowing the viewport to expand with Help collapsed. */
    public static void render(ViewerState state, GuiGraphics graphics, int left, int top) {
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0F);
        try {
        var requirement = state.selectedRequirement();
        if (requirement == null) return;
        List<Integer> options = displayOptionIndexes(state);
        state.animateAlternativeScroll();
        int first = (int) Math.floor(state.alternativeScroll());
        double fraction = state.alternativeScroll() - first;
        var pose = graphics.pose().last().pose();
        int screenX = Math.round(pose.m30()) + X;
        int screenY = Math.round(pose.m31()) + Y;
        // Include the one-pixel tile frame on every edge; clipping from X used to
        // shave the left frame of the first alternative.
        graphics.enableScissor(screenX - 1, screenY - 1, screenX + CELL + 1, screenY + ITEM_BOTTOM - Y);
        try {
            for (int displayed = 0; displayed <= VISIBLE && displayed + first < options.size(); displayed++) {
                int option = options.get(displayed + first);
                int y = Y + displayed * STEP - (int) Math.round(fraction * STEP);
                graphics.fill(X - 1, y - 1, X + 17, y + 17, 0xFF6A6256);
                graphics.fill(X, y, X + 16, y + 16, 0xFF2B2925);
                ViewerItemIconRenderer.render(graphics, ViewerIngredientResolver.materialStackFor(requirement.options().get(option)), X, y);
            }
        } finally {
            graphics.disableScissor();
        }
        if (options.size() > VISIBLE) {
            int height = ITEM_BOTTOM - Y;
            int maximum = options.size() - VISIBLE;
            int thumbHeight = Math.max(12, Math.round(height * VISIBLE / (float) options.size()));
            int thumbY = Y + Math.round((height - thumbHeight) * (float) (state.alternativeScroll() / maximum));
            graphics.fill(SCROLL_X, Y, SCROLL_X + 3, ITEM_BOTTOM, 0xFF171613);
            graphics.fill(SCROLL_X, thumbY, SCROLL_X + 3, thumbY + thumbHeight, 0xFFD19545);
        }
        } finally {
            graphics.pose().popPose();
        }
    }

    public static int optionAt(ViewerState state, double mouseX, double mouseY) {
        var requirement = state.selectedRequirement();
        if (requirement == null || mouseX < X || mouseX >= X + CELL || mouseY < Y || mouseY >= ITEM_BOTTOM) return -1;
        List<Integer> options = displayOptionIndexes(state);
        double visualOffset = state.alternativeScroll() - Math.floor(state.alternativeScroll());
        int row = (int) Math.floor((mouseY - Y + visualOffset * STEP) / STEP);
        double localY = mouseY - Y + visualOffset * STEP - row * STEP;
        if (localY >= CELL) return -1;
        int displayed = row + (int) Math.floor(state.alternativeScroll());
        return displayed < options.size() ? options.get(displayed) : -1;
    }

    public static boolean scrollbarAt(ViewerState state, double mouseX, double mouseY) {
        var requirement = state.selectedRequirement();
        return requirement != null && displayOptionIndexes(state).size() > VISIBLE
            && mouseX >= SCROLL_X && mouseX < SCROLL_X + 4 && mouseY >= Y && mouseY < ITEM_BOTTOM;
    }

    /** The whole alternatives column owns the wheel, including the intentional gaps between icons. */
    public static boolean scrollAreaAt(ViewerState state, double mouseX, double mouseY) {
        return state.selectedRequirement() != null
            && mouseX >= X - 1 && mouseX < SCROLL_X + 4
            && mouseY >= Y - 1 && mouseY < ITEM_BOTTOM;
    }

    public static void dragScrollbar(ViewerState state, double mouseY) {
        int count = displayOptionIndexes(state).size();
        int maximum = Math.max(0, count - VISIBLE);
        state.setAlternativeTargetOffset((mouseY - Y) / (ITEM_BOTTOM - Y) * maximum, VISIBLE, count);
    }

    public static void scroll(ViewerState state, double delta) {
        state.scrollAlternativesSmooth(delta, VISIBLE, displayOptionIndexes(state).size());
    }

    /**
     * A reusable tool can produce several block variants, but repeating its identical item icon
     * does not give a player another useful material choice. Keep the variants in the model and
     * render data; present that tool just once in the inspector.
     */
    private static List<Integer> displayOptionIndexes(ViewerState state) {
        var requirement = state.selectedRequirement();
        if (requirement == null) return List.of();
        List<Integer> indexes = new ArrayList<>();
        Set<MaterialPresentation> reusableTools = new HashSet<>();
        for (int index = 0; index < requirement.options().size(); index++) {
            var option = requirement.options().get(index);
            if (option.material().kind() == MaterialPresentation.Kind.REUSABLE_TOOL
                && !reusableTools.add(option.material())) {
                continue;
            }
            indexes.add(index);
        }
        return indexes;
    }
}
