package net.sprocketgames.universalmultiblockviewer.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.sprocketgames.universalmultiblockviewer.model.MaterialEntry;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockMaterials;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerIngredientResolver;
import net.sprocketgames.universalmultiblockviewer.viewer.ViewerState;

/** Shared, host-neutral BOM strip. Hosts supply navigation and recipe lookup around this visual. */
public final class MaterialStrip {
    public static final int X = 4;
    public static final int Y = 138;
    public static final int WIDTH = 248;
    public static final int HEIGHT = 28;
    public static final int FIRST_ITEM = X + 4;
    public static final int VISIBLE = ViewerState.MATERIALS_VISIBLE;
    /** Twelve full cells plus the largest safe glimpse of the next one. */
    public static final int ITEM_VIEWPORT_RIGHT = X + WIDTH - 4;
    private static final float COUNT_SCALE = 0.75F;

    private MaterialStrip() { }

    public static void render(ViewerState state, GuiGraphics graphics, int left, int top) {
        state.animateMaterialScroll();
        List<MaterialEntry> materials = materials(state);
        var font = Minecraft.getInstance().font;
        graphics.fill(left + X, top + Y, left + X + WIDTH, top + Y + HEIGHT, 0xFF262522);
        // The current pose contains the host recipe's screen offset, whereas scissor
        // coordinates are absolute GUI coordinates. Mixing the two hides icons in
        // JEI/EMI panels that are not positioned at the top-left of the screen.
        var pose = graphics.pose().last().pose();
        int screenLeft = Math.round(pose.m30()) + left;
        int screenTop = Math.round(pose.m31()) + top;
        graphics.enableScissor(screenLeft + FIRST_ITEM, screenTop + Y, screenLeft + ITEM_VIEWPORT_RIGHT, screenTop + Y + 22);
        try {
            int first = state.materialOffset();
            double fractional = state.materialScroll() - first;
            for (int index = 0; index <= VISIBLE && index + first < materials.size(); index++) {
                MaterialEntry material = materials.get(index + first);
                int x = left + FIRST_ITEM + index * 19 - (int) Math.round(fractional * 19.0D);
                int y = top + Y + 3;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF716B60);
                graphics.renderItem(ViewerIngredientResolver.stackFor(material.requirement().defaultBlock()), x, y);
                String count = Integer.toString(material.count());
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(x + 16 - font.width(count) * COUNT_SCALE, y + 16 - font.lineHeight * COUNT_SCALE, 200.0F);
                    graphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
                    graphics.drawString(font, count, 0, 0, 0xFFFFFFFF, true);
                } finally {
                    graphics.pose().popPose();
                }
            }
        } finally {
            graphics.disableScissor();
        }
        int trackX = left + X + 2;
        int trackWidth = WIDTH - 4;
        graphics.fill(trackX, top + Y + 22, trackX + trackWidth, top + Y + 25, 0xFF151513);
        if (!materials.isEmpty()) {
            int thumbWidth = Math.max(18, Math.round(trackWidth * Math.min(1.0F, VISIBLE / (float) materials.size())));
            int maximum = Math.max(1, materials.size() - VISIBLE);
            int thumbX = trackX + Math.round((trackWidth - thumbWidth) * (float) (state.materialScroll() / maximum));
            graphics.fill(thumbX, top + Y + 22, thumbX + thumbWidth, top + Y + 25, 0xFFD19545);
        }
    }

    public static MaterialEntry at(ViewerState state, double x, double y) {
        if (y < Y + 3 || y >= Y + 21 || x < FIRST_ITEM || x >= ITEM_VIEWPORT_RIGHT) return null;
        int index = (int) Math.floor((x - FIRST_ITEM + (state.materialScroll() - state.materialOffset()) * 19.0D) / 19.0D)
            + state.materialOffset();
        List<MaterialEntry> materials = materials(state);
        return index >= 0 && index < materials.size() ? materials.get(index) : null;
    }

    public static boolean click(ViewerState state, double x, double y) {
        if (y < Y + 21 || y >= Y + HEIGHT || x < X || x >= X + WIDTH) return false;
        int count = materials(state).size();
        int maximum = Math.max(0, count - VISIBLE);
        if (maximum == 0) return true;
        // The right-most clickable pixel must map to the final entry exactly.
        // Dividing by WIDTH left a fractional gap at the end of a dragged thumb.
        float fraction = Math.clamp((float) ((x - X) / (WIDTH - 1.0D)), 0.0F, 1.0F);
        state.setMaterialTargetOffset(fraction * maximum);
        return true;
    }

    private static List<MaterialEntry> materials(ViewerState state) { return MultiblockMaterials.forVariant(state.variant()); }
}
