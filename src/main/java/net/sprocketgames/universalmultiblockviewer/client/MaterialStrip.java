package net.sprocketgames.universalmultiblockviewer.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
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
    /** A two-pixel inset leaves room for the primary-material frame at the left edge. */
    public static final int FIRST_ITEM = X + 6;
    public static final int VISIBLE = ViewerState.MATERIALS_VISIBLE;
    /** Twelve full cells plus the largest safe glimpse of the next one. */
    public static final int ITEM_VIEWPORT_RIGHT = X + WIDTH - 4;
    private static final float COUNT_SCALE = 0.75F;
    public static final int PRIMARY_OUTLINE = 0xFF63B06A;

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
        // Include the padded left edge so the primary-material frame is never clipped.
        graphics.enableScissor(screenLeft + X + 1, screenTop + Y, screenLeft + ITEM_VIEWPORT_RIGHT, screenTop + Y + 22);
        try {
            int first = state.materialOffset();
            double fractional = state.materialScroll() - first;
            int optionalStart = Integer.MAX_VALUE;
            int optionalEnd = Integer.MIN_VALUE;
            for (int index = 0; index <= VISIBLE && index + first < materials.size(); index++) {
                if (!materials.get(index + first).requirement().optional()) continue;
                int x = left + FIRST_ITEM + index * 19 - (int) Math.round(fractional * 19.0D);
                optionalStart = Math.min(optionalStart, x - 2);
                optionalEnd = Math.max(optionalEnd, x + 18);
            }
            if (optionalStart != Integer.MAX_VALUE) {
                // One continuous group boundary distinguishes optional materials without putting
                // a competing amber frame around every icon.
                int groupTop = top + Y + 1;
                int groupBottom = top + Y + 21;
                graphics.fill(optionalStart, groupTop, optionalEnd, groupTop + 1, 0xFFB8873E);
                graphics.fill(optionalStart, groupBottom - 1, optionalEnd, groupBottom, 0xFFB8873E);
                graphics.fill(optionalStart, groupTop, optionalStart + 1, groupBottom, 0xFFB8873E);
                graphics.fill(optionalEnd - 1, groupTop, optionalEnd, groupBottom, 0xFFB8873E);
            }
            for (int index = 0; index <= VISIBLE && index + first < materials.size(); index++) {
                MaterialEntry material = materials.get(index + first);
                int x = left + FIRST_ITEM + index * 19 - (int) Math.round(fractional * 19.0D);
                int y = top + Y + 3;
                if (isPrimaryMaterial(state, material)) {
                    graphics.fill(x - 2, y - 2, x + 18, y + 18, PRIMARY_OUTLINE);
                    graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF262522);
                } else if (!material.requirement().optional()) {
                    graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF716B60);
                }
                ViewerItemIconRenderer.render(graphics, ViewerIngredientResolver.materialStackFor(material.requirement().defaultBlock()), x, y);
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

    /** Updates the shared viewport hover marker while leaving the host's ordinary item tooltip intact. */
    public static void updateHoveredMaterial(ViewerState state, double x, double y) {
        MaterialEntry material = at(state, x, y);
        state.setHoveredMaterial(material == null ? null : presentationItem(material));
    }

    /** The item the strip presents for a material requirement, including configured block-to-item mappings. */
    public static ResourceLocation presentationItem(MaterialEntry material) {
        var presentation = material.requirement().defaultBlock().material();
        return presentation.item() == null ? material.requirement().defaultBlock().id() : presentation.item();
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

    public static List<MaterialEntry> materials(ViewerState state) {
        List<MaterialEntry> required = primaryFirst(state, MultiblockMaterials.forVariant(state.variant()));
        if (!state.showOptionalBlocks()) return required;
        List<MaterialEntry> optional = MultiblockMaterials.optionalForVariant(state.variant());
        if (optional.isEmpty()) return required;
        var combined = new java.util.ArrayList<MaterialEntry>(required.size() + optional.size());
        combined.addAll(required);
        combined.addAll(optional);
        return List.copyOf(combined);
    }

    private static boolean isPrimaryMaterial(ViewerState state, MaterialEntry material) {
        return primaryLookupItem(state).map(material.requirement().defaultBlock().id()::equals).orElse(false);
    }

    /** The first Uses association is the conventional controller/core; Recipes is the fallback. */
    private static java.util.Optional<net.minecraft.resources.ResourceLocation> primaryLookupItem(ViewerState state) {
        if (!state.definition().useLookupItems().isEmpty()) {
            return java.util.Optional.of(state.definition().useLookupItems().getFirst());
        }
        if (!state.definition().recipeLookupItems().isEmpty()) {
            return java.util.Optional.of(state.definition().recipeLookupItems().getFirst());
        }
        return java.util.Optional.empty();
    }

    /** Keeps normal alphabetical ordering, except that the guide's controller/core is always first. */
    private static List<MaterialEntry> primaryFirst(ViewerState state, List<MaterialEntry> materials) {
        var primary = primaryLookupItem(state);
        if (primary.isEmpty()) return materials;
        var ordered = new java.util.ArrayList<>(materials);
        ordered.sort(java.util.Comparator.comparing(entry -> !entry.requirement().defaultBlock().id().equals(primary.get())));
        return List.copyOf(ordered);
    }
}
