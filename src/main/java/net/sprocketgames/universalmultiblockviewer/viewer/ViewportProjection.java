package net.sprocketgames.universalmultiblockviewer.viewer;

import java.util.Comparator;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Mirrors the viewport camera for reliable screen-space block selection. */
public final class ViewportProjection {
    private ViewportProjection() {
    }

    public static GridPos findCell(ViewerState state, double mouseX, double mouseY, int width, int height) {
        return ViewerLayout.visibleCells(state).stream()
            .map(position -> hit(state, position, mouseX, mouseY, width, height))
            .filter(candidate -> candidate != null)
            .min(Comparator.comparingDouble((Candidate candidate) -> -candidate.depth)
                .thenComparingDouble(candidate -> candidate.distance))
            .map(Candidate::position)
            .orElse(null);
    }

    private static Candidate hit(ViewerState state, GridPos position, double mouseX, double mouseY, int width, int height) {
        Matrix4f transform = transform(state, width, height);
        Point[] corners = new Point[8];
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            corners[x * 4 + y * 2 + z] = project(transform, position.x() + x, position.y() + y, position.z() + z);
        }
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

    private static Point project(Matrix4f transform, float x, float y, float z) {
        Vector4f point = new Vector4f(x, y, z, 1.0F).mul(transform);
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
