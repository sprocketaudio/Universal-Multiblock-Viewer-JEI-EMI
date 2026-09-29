package net.sprocketgames.universalmultiblockviewer.viewer;

import java.util.Objects;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.BlockRequirement;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.StructureVariant;

/** Host-neutral camera, variant, layer, and selection state shared by JEI and EMI implementations. */
public final class ViewerState {
    public static final double MIN_ZOOM = 0.45D;
    public static final double MAX_ZOOM = 6.0D;
    public static final int MATERIALS_VISIBLE = 12;
    public static final int EXPANDED_HELP_WIDTH = 78;
    public static final int COLLAPSED_HELP_WIDTH = 18;
    private static final double DRAG_THRESHOLD_SQUARED = 9.0D;

    private final MultiblockDefinition definition;
    private String variantId;
    private int layer = -1;
    private GridPos selected;
    private final Map<GridPos, Integer> selectedOptions = new HashMap<>();
    private double yaw = 45.0D;
    private double pitch;
    private double zoom = 1.0D;
    private double panX;
    private double panY;
    private boolean darkViewportBackground = true;
    private boolean showFloorGrid;
    private boolean showAlternativeHighlights;
    private boolean showOptionalBlocks;
    /** The presentation item currently under the material-strip pointer, if any. */
    private ResourceLocation hoveredMaterial;
    private int materialOffset;
    private double materialScroll;
    private double materialTargetScroll;
    private long lastMaterialAnimationNanos;
    private double alternativeScroll;
    private double alternativeTargetScroll;
    private long lastAlternativeAnimationNanos;
    private double helpWidth = EXPANDED_HELP_WIDTH;
    private double helpTargetWidth = EXPANDED_HELP_WIDTH;
    private long lastHelpAnimationNanos;
    private boolean dragging;
    private double dragStartX;
    private double dragStartY;
    private double lastDragX;
    private double lastDragY;
    private DragMode dragMode = DragMode.ROTATE;

    public ViewerState(MultiblockDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.variantId = definition.defaultVariant();
    }

    public MultiblockDefinition definition() { return definition; }
    public StructureVariant variant() { return definition.variants().get(variantId); }
    public String variantId() { return variantId; }
    public int layer() { return layer; }
    public GridPos selected() { return selected; }
    public double yaw() { return yaw; }
    public double pitch() { return pitch; }
    public double zoom() { return zoom; }
    public double panX() { return panX; }
    public double panY() { return panY; }
    public boolean darkViewportBackground() { return darkViewportBackground; }
    public boolean showFloorGrid() { return showFloorGrid; }
    public boolean showAlternativeHighlights() { return showAlternativeHighlights; }
    public boolean showOptionalBlocks() { return showOptionalBlocks; }
    public ResourceLocation hoveredMaterial() { return hoveredMaterial; }
    public boolean hasOptionalBlocks() { return variant().cells().values().stream().anyMatch(BlockRequirement::optional); }
    public int materialOffset() { return materialOffset; }
    public double materialScroll() { return materialScroll; }
    public double materialTargetScroll() { return materialTargetScroll; }
    public double alternativeScroll() { return alternativeScroll; }
    public int helpWidth() { return (int) Math.round(helpWidth); }
    public boolean helpCollapsed() { return helpTargetWidth == COLLAPSED_HELP_WIDTH; }

    public void setVariant(String id) {
        if (!definition.variants().containsKey(id)) {
            throw new IllegalArgumentException("unknown variant " + id);
        }
        StructureVariant previousVariant = variant();
        Map<GridPos, BlockOption> choicesToRetain = new HashMap<>();
        selectedOptions.forEach((position, optionIndex) -> {
            BlockRequirement requirement = previousVariant.cells().get(position);
            if (requirement != null && optionIndex >= 0 && optionIndex < requirement.options().size()) {
                choicesToRetain.put(position, requirement.options().get(optionIndex));
            }
        });
        BlockOption selectedChoice = selected == null ? null : displayedBlock(selected);
        variantId = id;
        selectedOptions.clear();
        choicesToRetain.forEach((position, choice) -> {
            BlockRequirement requirement = variant().cells().get(position);
            if (requirement == null) return;
            int option = requirement.options().indexOf(choice);
            if (option >= 0) selectedOptions.put(position, option);
        });
        materialOffset = 0;
        materialScroll = 0.0D;
        materialTargetScroll = 0.0D;
        alternativeScroll = 0.0D;
        alternativeTargetScroll = 0.0D;
        if (selected != null && (!variant().cells().containsKey(selected)
            || !variant().cells().get(selected).options().contains(selectedChoice))) {
            selected = null;
        }
        if (selected != null && !showOptionalBlocks && variant().cells().get(selected).optional()) {
            selected = null;
        }
        if (layer >= variant().height()) {
            layer = -1;
        }
    }

    public void cycleVariant(int direction) {
        var ids = definition.variants().keySet().stream().toList();
        int current = ids.indexOf(variantId);
        int next = Math.floorMod(current + Integer.signum(direction), ids.size());
        setVariant(ids.get(next));
    }

    public void setLayer(int value) {
        if (value < -1 || value >= variant().height()) {
            throw new IllegalArgumentException("layer must be -1 through " + (variant().height() - 1));
        }
        layer = value;
        if (selected != null && layer >= 0 && selected.y() != layer) {
            selected = null;
        }
    }

    public void cycleLayer(int direction) {
        int next = layer + Integer.signum(direction);
        if (next < -1) next = variant().height() - 1;
        if (next >= variant().height()) next = -1;
        setLayer(next);
    }

    public void selectOrToggle(GridPos position) {
        if (position == null || !variant().cells().containsKey(position)
            || (!showOptionalBlocks && variant().cells().get(position).optional())) {
            return;
        }
        if (position.equals(selected)) {
            selected = null;
        } else {
            selected = position;
            selectedOptions.putIfAbsent(position, variant().cells().get(position).defaultOption());
            alternativeScroll = 0.0D;
            alternativeTargetScroll = 0.0D;
        }
    }

    public void clearSelection() { selected = null; }

    /**
     * Stores only transient UI hover state. It deliberately does not alter the chosen
     * alternative, material totals, or camera, so both recipe-viewer hosts can share it.
     */
    public void setHoveredMaterial(ResourceLocation material) { hoveredMaterial = material; }

    public BlockRequirement selectedRequirement() {
        return selected == null ? null : variant().cells().get(selected);
    }

    public BlockOption displayedBlock(GridPos position) {
        BlockRequirement requirement = variant().cells().get(position);
        if (requirement == null) {
            return null;
        }
        return requirement.options().get(selectedOptions.getOrDefault(position, requirement.defaultOption()));
    }

    public int selectedOption() {
        BlockRequirement requirement = selectedRequirement();
        return requirement == null ? -1 : selectedOptions.getOrDefault(selected, requirement.defaultOption());
    }

    public void cycleSelectedOption(int direction) {
        BlockRequirement requirement = selectedRequirement();
        if (requirement == null || requirement.options().size() < 2 || direction == 0) {
            return;
        }
        int next = Math.floorMod(selectedOption() + Integer.signum(direction), requirement.options().size());
        selectedOptions.put(selected, next);
    }

    /** Freezes the currently selected position to one of its authored valid alternatives. */
    public void setSelectedOption(int option) {
        BlockRequirement requirement = selectedRequirement();
        if (requirement == null || option < 0 || option >= requirement.options().size()) {
            return;
        }
        selectedOptions.put(selected, option);
    }

    public void scrollAlternativesSmooth(double delta, int visibleChoices) {
        BlockRequirement requirement = selectedRequirement();
        if (requirement == null) return;
        scrollAlternativesSmooth(delta, visibleChoices, requirement.options().size());
    }

    public void scrollAlternativesSmooth(double delta, int visibleChoices, int choiceCount) {
        int maximum = Math.max(0, choiceCount - visibleChoices);
        alternativeTargetScroll = Math.clamp(alternativeTargetScroll + Math.clamp(delta, -1.0D, 1.0D), 0.0D, maximum);
    }

    public void setAlternativeTargetOffset(double offset, int visibleChoices) {
        BlockRequirement requirement = selectedRequirement();
        if (requirement == null) return;
        setAlternativeTargetOffset(offset, visibleChoices, requirement.options().size());
    }

    public void setAlternativeTargetOffset(double offset, int visibleChoices, int choiceCount) {
        alternativeTargetScroll = Math.clamp(offset, 0.0D, Math.max(0, choiceCount - visibleChoices));
    }

    public void animateAlternativeScroll() {
        long now = System.nanoTime();
        if (lastAlternativeAnimationNanos == 0L) {
            lastAlternativeAnimationNanos = now;
            return;
        }
        double elapsedSeconds = (now - lastAlternativeAnimationNanos) / 1_000_000_000.0D;
        double difference = alternativeTargetScroll - alternativeScroll;
        double maximumStep = Math.max(0.0D, elapsedSeconds) * 8.0D;
        alternativeScroll += Math.copySign(Math.min(Math.abs(difference), maximumStep), difference);
        if (Math.abs(alternativeTargetScroll - alternativeScroll) < 0.001D) alternativeScroll = alternativeTargetScroll;
        lastAlternativeAnimationNanos = now;
    }

    public void zoomBy(double delta) {
        zoom = Math.clamp(zoom + delta * 0.1D, MIN_ZOOM, MAX_ZOOM);
    }

    /** Restores the initial camera without changing the current variant, layer, or selection. */
    public void resetView() {
        yaw = 45.0D;
        pitch = 0.0D;
        zoom = 1.0D;
        panX = 0.0D;
        panY = 0.0D;
        dragging = false;
    }

    /** Changes only the viewport clear colour; it deliberately preserves every view control. */
    public void toggleViewportBackground() {
        darkViewportBackground = !darkViewportBackground;
    }

    /** Shows a visual-only, block-aligned floor grid beneath the current structure. */
    public void toggleFloorGrid() {
        showFloorGrid = !showFloorGrid;
    }

    /** Highlights positions that have more than one valid presentation material without changing them. */
    public void toggleAlternativeHighlights() {
        showAlternativeHighlights = !showAlternativeHighlights;
    }

    /** Shows optional structure cells without changing camera, layer, variant, or material scroll state. */
    public void toggleOptionalBlocks() {
        if (!hasOptionalBlocks()) return;
        showOptionalBlocks = !showOptionalBlocks;
        if (!showOptionalBlocks && selected != null && variant().cells().get(selected).optional()) {
            selected = null;
        }
        // The optional group can change the strip's maximum range; retain its nearest valid position.
        setMaterialOffset(materialOffset);
    }

    /** Called when a host creates a newly opened recipe view. */
    public void resetViewportBackground() {
        darkViewportBackground = true;
    }

    /** Animates the Help panel independently of the camera and the fixed BOM strip. */
    public void toggleHelp() {
        helpTargetWidth = helpCollapsed() ? EXPANDED_HELP_WIDTH : COLLAPSED_HELP_WIDTH;
    }

    public void animateHelp() {
        long now = System.nanoTime();
        if (lastHelpAnimationNanos == 0L) {
            lastHelpAnimationNanos = now;
            return;
        }
        advanceHelpAnimation((now - lastHelpAnimationNanos) / 1_000_000_000.0D);
        lastHelpAnimationNanos = now;
    }

    void advanceHelpAnimation(double elapsedSeconds) {
        double difference = helpTargetWidth - helpWidth;
        double maximumStep = Math.max(0.0D, elapsedSeconds) * 280.0D;
        helpWidth += Math.copySign(Math.min(Math.abs(difference), maximumStep), difference);
        if (Math.abs(helpTargetWidth - helpWidth) < 0.01D) helpWidth = helpTargetWidth;
    }

    public void scrollMaterials(int direction) {
        setMaterialOffset(materialOffset + Integer.signum(direction));
    }

    /** Advances the horizontal BOM strip by a partial item for natural wheel scrolling. */
    public void scrollMaterialsSmooth(double delta) {
        int maximum = materialMaximum();
        materialTargetScroll = Math.clamp(materialTargetScroll + Math.clamp(delta, -1.0D, 1.0D), 0.0D, maximum);
    }

    /** Advances the visual BOM position toward the wheel/track target without jumping an item at a time. */
    public void advanceMaterialAnimation(double elapsedSeconds) {
        double difference = materialTargetScroll - materialScroll;
        if (Math.abs(difference) < 0.001D) {
            materialScroll = materialTargetScroll;
        } else {
            double maximumStep = Math.max(0.0D, elapsedSeconds) * 8.0D;
            materialScroll += Math.copySign(Math.min(Math.abs(difference), maximumStep), difference);
        }
        materialOffset = (int) Math.floor(materialScroll);
    }

    /** Called by the shared strip every GUI frame. */
    public void animateMaterialScroll() {
        long now = System.nanoTime();
        if (lastMaterialAnimationNanos == 0L) {
            lastMaterialAnimationNanos = now;
            return;
        }
        advanceMaterialAnimation((now - lastMaterialAnimationNanos) / 1_000_000_000.0D);
        lastMaterialAnimationNanos = now;
    }

    public void setMaterialOffset(int offset) {
        int maximum = materialMaximum();
        materialOffset = Math.clamp(offset, 0, maximum);
        materialScroll = materialOffset;
        materialTargetScroll = materialOffset;
        lastMaterialAnimationNanos = 0L;
    }

    /** Moves the BOM thumb toward an absolute item offset; used by scrollbar dragging. */
    public void setMaterialTargetOffset(double offset) {
        materialTargetScroll = Math.clamp(offset, 0.0D, materialMaximum());
    }

    private int materialMaximum() {
        int count = net.sprocketgames.universalmultiblockviewer.model.MultiblockMaterials.forVariant(variant()).size();
        if (showOptionalBlocks) {
            count += net.sprocketgames.universalmultiblockviewer.model.MultiblockMaterials.optionalForVariant(variant()).size();
        }
        return Math.max(0, count - MATERIALS_VISIBLE);
    }


    public void beginDrag(double mouseX, double mouseY) {
        beginDrag(mouseX, mouseY, DragMode.ROTATE);
    }

    public void beginPanDrag(double mouseX, double mouseY) {
        beginDrag(mouseX, mouseY, DragMode.PAN);
    }

    private void beginDrag(double mouseX, double mouseY, DragMode mode) {
        dragging = false;
        dragMode = mode;
        dragStartX = lastDragX = mouseX;
        dragStartY = lastDragY = mouseY;
    }

    public void dragTo(double mouseX, double mouseY) {
        double totalX = mouseX - dragStartX;
        double totalY = mouseY - dragStartY;
        if (!dragging && totalX * totalX + totalY * totalY >= DRAG_THRESHOLD_SQUARED) {
            dragging = true;
        }
        if (dragging) {
            if (dragMode == DragMode.PAN) {
                panX += mouseX - lastDragX;
                panY += mouseY - lastDragY;
            } else {
                yaw += (mouseX - lastDragX) * 1.5D;
                pitch += (mouseY - lastDragY) * 1.5D;
            }
        }
        lastDragX = mouseX;
        lastDragY = mouseY;
    }

    /** @return true only when this was a click, not a drag. */
    public boolean endDrag() {
        boolean wasDrag = dragging;
        dragging = false;
        return !wasDrag;
    }

    private enum DragMode {
        ROTATE,
        PAN
    }
}
