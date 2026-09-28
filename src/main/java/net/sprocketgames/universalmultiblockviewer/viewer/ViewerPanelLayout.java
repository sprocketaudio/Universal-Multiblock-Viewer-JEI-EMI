package net.sprocketgames.universalmultiblockviewer.viewer;

/** Shared recipe-panel geometry. The BOM deliberately keeps its fixed full width. */
public final class ViewerPanelLayout {
    public static final int CONTENT_X = 4;
    public static final int CONTENT_Y = 18;
    public static final int CONTENT_WIDTH = 248;
    public static final int CONTENT_HEIGHT = 116;
    public static final int HELP_GAP = 2;

    private ViewerPanelLayout() {
    }

    /** X position relative to the content panel, after the animated Help region. */
    public static int viewportX(ViewerState state) {
        return state.helpWidth() + HELP_GAP;
    }

    public static int viewportWidth(ViewerState state) {
        return CONTENT_WIDTH - viewportX(state);
    }
}
