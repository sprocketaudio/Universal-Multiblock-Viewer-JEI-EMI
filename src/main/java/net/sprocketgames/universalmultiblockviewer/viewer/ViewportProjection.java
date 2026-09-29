package net.sprocketgames.universalmultiblockviewer.viewer;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Mirrors the viewport camera for reliable screen-space block selection. */
public final class ViewportProjection {
    private ViewportProjection() {
    }

    public static GridPos findCell(ViewerState state, double mouseX, double mouseY, int width, int height) {
        return findCell(state, mouseX, mouseY, width, height, position -> selectionBoxes(state, position));
    }

    static GridPos findCell(ViewerState state, double mouseX, double mouseY, int width, int height,
                            Function<GridPos, List<AABB>> shapeResolver) {
        Matrix4f transform = transform(state, width, height);
        return ViewerLayout.visibleCells(state).stream()
            .map(position -> hit(transform, position, shapeResolver.apply(position), mouseX, mouseY))
            .filter(candidate -> candidate != null)
            .min(Comparator.comparingDouble((Candidate candidate) -> -candidate.depth)
                .thenComparingDouble(candidate -> candidate.distance))
            .map(Candidate::position)
            .orElse(null);
    }

    private static Candidate hit(Matrix4f transform, GridPos position, List<AABB> boxes, double mouseX, double mouseY) {
        Candidate best = null;
        for (AABB box : boxes) {
            Candidate candidate = hitBox(transform, position, box, mouseX, mouseY);
            if (candidate != null && (best == null || candidate.depth > best.depth)) best = candidate;
        }
        return best;
    }

    private static Candidate hitBox(Matrix4f transform, GridPos position, AABB box, double mouseX, double mouseY) {
        Point[] corners = new Point[8];
        double[] xBounds = {box.minX, box.maxX};
        double[] yBounds = {box.minY, box.maxY};
        double[] zBounds = {box.minZ, box.maxZ};
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
            corners[x * 4 + y * 2 + z] = project(transform,
                position.x() + xBounds[x], position.y() + yBounds[y], position.z() + zBounds[z]);
        int[][] faces = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
        Candidate best = null;
        for (int[] face : faces) {
            Point a = corners[face[0]], b = corners[face[1]], c = corners[face[2]], d = corners[face[3]];
            if (!inside(mouseX, mouseY, a, b, c) && !inside(mouseX, mouseY, a, c, d)) continue;
            double depth = (a.z + b.z + c.z + d.z) * 0.25D;
            double distance = a.distanceSquared(mouseX, mouseY);
            Candidate candidate = new Candidate(position, depth, distance);
            if (best == null || candidate.depth > best.depth) best = candidate;
        }
        return best;
    }

    /** Uses the block's authored selection shape. A full cube is only a last-resort fallback. */
    static List<AABB> selectionBoxes(ViewerState state, GridPos position) {
        var option = state.displayedBlock(position);
        if (option == null) return List.of();
        try {
            var blockState = ViewerIngredientResolver.stateFor(option);
            var shape = blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
            List<AABB> boxes = shape.toAabbs();
            if (!boxes.isEmpty()) return boxes;
        } catch (RuntimeException ignored) {
            // A mod may require a real level for its dynamic shape. Keep that block selectable.
        }
        return List.of(new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
    }

    private static Matrix4f transform(ViewerState state, int width, int height) {
        int maximum = Math.max(state.variant().width(), Math.max(state.variant().height(), state.variant().depth()));
        float scale = Math.max(4.5F, 42.0F / maximum) * (float) state.zoom();
        return new Matrix4f()
            .translate(width / 2.0F + (float) state.panX(), height * 0.50F + (float) state.panY(), 180.0F)
            .scale(scale, -scale, scale)
            .rotateX((float) Math.toRadians(32.0D + state.pitch()))
            .rotateY((float) Math.toRadians(45.0D + state.yaw()))
            .translate(-state.variant().width() / 2.0F, -state.variant().height() / 2.0F, -state.variant().depth() / 2.0F);
    }

    private static Point project(Matrix4f transform, double x, double y, double z) {
        Vector4f point = new Vector4f((float) x, (float) y, (float) z, 1.0F).mul(transform);
        return new Point(point.x, point.y, point.z);
    }

    private static boolean inside(double x, double y, Point a, Point b, Point c) {
        double one = cross(x, y, a, b), two = cross(x, y, b, c), three = cross(x, y, c, a);
        return (one >= 0 && two >= 0 && three >= 0) || (one <= 0 && two <= 0 && three <= 0);
    }

    private static double cross(double x, double y, Point a, Point b) { return (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x); }

    private record Candidate(GridPos position, double depth, double distance) { }
    private record Point(float x, float y, float z) {
        float distanceSquared(double otherX, double otherY) {
            float dx = x - (float) otherX;
            float dy = y - (float) otherY;
            return dx * dx + dy * dy;
        }
    }
}
