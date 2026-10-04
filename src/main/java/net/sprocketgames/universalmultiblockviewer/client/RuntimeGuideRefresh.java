package net.sprocketgames.universalmultiblockviewer.client;

import java.util.function.BooleanSupplier;

/** Bridges an optional recipe-viewer runtime refresh without loading its classes in common code. */
public final class RuntimeGuideRefresh {
    private static volatile BooleanSupplier jeiRefresh = () -> false;

    private RuntimeGuideRefresh() { }

    public static void installJei(BooleanSupplier refresh) {
        jeiRefresh = refresh;
    }

    public static void clearJei() {
        jeiRefresh = () -> false;
    }

    public static boolean refreshJei() {
        return jeiRefresh.getAsBoolean();
    }
}
