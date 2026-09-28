package net.sprocketgames.universalmultiblockviewer.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.client.MaterialStrip;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockMaterials;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.BlockRequirement;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.StructureVariant;
import org.junit.jupiter.api.Test;

class ViewerStateTest {
    private static final GridPos CELL = new GridPos(0, 0, 0);

    @Test
    void clickTogglesButDragNeverReportsClick() {
        ViewerState state = new ViewerState(definition());
        state.selectOrToggle(CELL);
        assertEquals(CELL, state.selected());
        state.selectOrToggle(CELL);
        assertNull(state.selected());

        state.beginDrag(10, 10);
        state.dragTo(20, 15);
        assertFalse(state.endDrag());
        assertTrue(state.yaw() > 45.0D);

        state.beginPanDrag(20, 15);
        state.dragTo(32, 24);
        assertFalse(state.endDrag());
        assertEquals(12.0D, state.panX());
        assertEquals(9.0D, state.panY());
    }

    @Test
    void variantAndLayerChangesClearInvalidSelections() {
        ViewerState state = new ViewerState(definition());
        state.selectOrToggle(CELL);
        state.setLayer(0);
        assertEquals(CELL, state.selected());
        state.cycleVariant(1);
        assertEquals("other", state.variantId());
        assertNull(state.selected());
    }

    @Test
    void variantsCycleInTheirAuthoredOrder() {
        BlockRequirement requirement = new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
            ResourceLocation.parse("minecraft:stone"))), 0, "");
        Map<String, StructureVariant> variants = new java.util.LinkedHashMap<>();
        variants.put("second", new StructureVariant("second", "Second", 1, 1, 1, Map.of(CELL, requirement)));
        variants.put("first", new StructureVariant("first", "First", 1, 1, 1, Map.of(CELL, requirement)));
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:ordered"), "Ordered", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "second", variants);
        ViewerState state = new ViewerState(definition);

        state.cycleVariant(1);

        assertEquals("first", state.variantId());
    }

    @Test
    void selectedPositionCyclesItsOwnPresentationAlternative() {
        BlockRequirement alternatives = new BlockRequirement(List.of(
            new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse("minecraft:stone")),
            new BlockOption(BlockOption.Kind.BLOCK, ResourceLocation.parse("minecraft:granite"))), 0, "Frame");
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:alternatives"), "Test", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base",
                new StructureVariant("base", "Base", 1, 1, 1, Map.of(CELL, alternatives))));
        ViewerState state = new ViewerState(definition);
        state.selectOrToggle(CELL);
        state.cycleSelectedOption(1);
        assertEquals(ResourceLocation.parse("minecraft:granite"), state.displayedBlock(CELL).id());
        assertEquals(1, state.selectedOption());
        state.setSelectedOption(0);
        assertEquals(ResourceLocation.parse("minecraft:stone"), state.displayedBlock(CELL).id());
    }

    @Test
    void materialScrollIsBoundedToAvailableEntries() {
        ViewerState state = new ViewerState(definition());
        state.scrollMaterials(1);
        assertEquals(0, state.materialOffset());
    }

    @Test
    void materialScrollReachesTheFinalVisibleWindow() {
        Map<GridPos, BlockRequirement> cells = new java.util.LinkedHashMap<>();
        for (int x = 0; x < 14; x++) {
            cells.put(new GridPos(x, 0, 0), new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
                ResourceLocation.parse("test:block_" + x))), 0, ""));
        }
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:materials"), "Test", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base",
                new StructureVariant("base", "Base", 14, 1, 1, cells)));
        ViewerState state = new ViewerState(definition);

        state.setMaterialOffset(99);
        assertEquals(2, state.materialOffset());
        state.setMaterialOffset(-99);
        assertEquals(0, state.materialOffset());
        state.scrollMaterials(99);
        assertEquals(1, state.materialOffset());
        state.scrollMaterials(-99);
        assertEquals(0, state.materialOffset());
    }

    @Test
    void smoothMaterialScrollKeepsFractionalMotionInsideItsBounds() {
        Map<GridPos, BlockRequirement> cells = new java.util.LinkedHashMap<>();
        for (int x = 0; x < 14; x++) {
            cells.put(new GridPos(x, 0, 0), new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
                ResourceLocation.parse("test:block_" + x))), 0, ""));
        }
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:smooth_materials"), "Test", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base",
                new StructureVariant("base", "Base", 14, 1, 1, cells)));
        ViewerState state = new ViewerState(definition);

        state.scrollMaterialsSmooth(1.0D);
        assertEquals(0, state.materialOffset());
        assertEquals(0.0D, state.materialScroll());
        assertEquals(1.0D, state.materialTargetScroll());
        state.advanceMaterialAnimation(0.05D);
        assertEquals(0.4D, state.materialScroll());
        state.scrollMaterialsSmooth(99.0D);
        state.advanceMaterialAnimation(1.0D);
        assertEquals(2.0D, state.materialScroll());
    }

    @Test
    void partialMaterialAtTheRightEdgeCanStillBeHoveredAndClicked() {
        Map<GridPos, BlockRequirement> cells = new java.util.LinkedHashMap<>();
        for (int x = 0; x < 14; x++) {
            cells.put(new GridPos(x, 0, 0), new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
                ResourceLocation.parse("test:block_" + x))), 0, ""));
        }
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:partial_materials"), "Test", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base",
                new StructureVariant("base", "Base", 14, 1, 1, cells)));
        ViewerState state = new ViewerState(definition);
        state.scrollMaterialsSmooth(1.0D);
        state.advanceMaterialAnimation(0.05D);

        assertEquals(MultiblockMaterials.forVariant(state.variant()).get(12).requirement().defaultBlock().id(),
            MaterialStrip.at(state, MaterialStrip.ITEM_VIEWPORT_RIGHT - 1, MaterialStrip.Y + 10).requirement().defaultBlock().id());
    }

    @Test
    void materialScrollbarReachesItsExactFinalPositionAtTheRightmostPixel() {
        Map<GridPos, BlockRequirement> cells = new java.util.LinkedHashMap<>();
        for (int x = 0; x < 14; x++) {
            cells.put(new GridPos(x, 0, 0), new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
                ResourceLocation.parse("test:block_" + x))), 0, ""));
        }
        MultiblockDefinition definition = new MultiblockDefinition(ResourceLocation.parse("test:scrollbar_end"), "Test", "",
            List.of(ResourceLocation.parse("minecraft:stick")), "base", Map.of("base",
                new StructureVariant("base", "Base", 14, 1, 1, cells)));
        ViewerState state = new ViewerState(definition);

        assertTrue(MaterialStrip.click(state, MaterialStrip.X + MaterialStrip.WIDTH - 1, MaterialStrip.Y + 23));
        state.advanceMaterialAnimation(1.0D);

        assertEquals(2.0D, state.materialScroll());
        assertEquals(2, state.materialOffset());
    }

    @Test
    void resetViewRestoresTheCameraWithoutChangingTheSelectedBlock() {
        ViewerState state = new ViewerState(definition());
        state.selectOrToggle(CELL);
        state.beginDrag(0, 0);
        state.dragTo(20, 12);
        state.beginPanDrag(0, 0);
        state.dragTo(15, 9);
        state.zoomBy(8);

        state.resetView();

        assertEquals(45.0D, state.yaw());
        assertEquals(0.0D, state.pitch());
        assertEquals(1.0D, state.zoom());
        assertEquals(0.0D, state.panX());
        assertEquals(0.0D, state.panY());
        assertEquals(CELL, state.selected());
    }

    @Test
    void viewportBackgroundTogglePreservesAllViewControlsAndReopensDark() {
        ViewerState state = new ViewerState(definition());
        state.selectOrToggle(CELL);
        state.beginDrag(0, 0);
        state.dragTo(20, 12);
        state.beginPanDrag(0, 0);
        state.dragTo(15, 9);
        state.zoomBy(4);

        assertTrue(state.darkViewportBackground());
        state.toggleViewportBackground();

        assertFalse(state.darkViewportBackground());
        assertEquals(CELL, state.selected());
        assertEquals(75.0D, state.yaw());
        assertEquals(18.0D, state.pitch());
        assertEquals(1.4D, state.zoom());
        assertEquals(15.0D, state.panX());
        assertEquals(9.0D, state.panY());

        state.resetViewportBackground();
        assertTrue(state.darkViewportBackground());
        assertEquals(CELL, state.selected());
        assertEquals(75.0D, state.yaw());
        assertEquals(18.0D, state.pitch());
        assertEquals(1.4D, state.zoom());
    }

    @Test
    void helpPanelAnimatesBetweenExpandedAndCollapsedWidthsWithoutAffectingTheCamera() {
        ViewerState state = new ViewerState(definition());
        state.zoomBy(3);
        state.toggleHelp();
        state.advanceHelpAnimation(0.1D);

        assertTrue(state.helpWidth() < ViewerState.EXPANDED_HELP_WIDTH);
        assertEquals(1.3D, state.zoom());

        state.advanceHelpAnimation(1.0D);
        assertEquals(ViewerState.COLLAPSED_HELP_WIDTH, state.helpWidth());
        assertTrue(state.helpCollapsed());

        state.toggleHelp();
        state.advanceHelpAnimation(1.0D);
        assertEquals(ViewerState.EXPANDED_HELP_WIDTH, state.helpWidth());
    }

    @Test
    void collapsingHelpExpandsOnlyTheViewport() {
        ViewerState state = new ViewerState(definition());
        assertEquals(168, ViewerPanelLayout.viewportWidth(state));
        state.toggleHelp();
        state.advanceHelpAnimation(1.0D);

        assertEquals(228, ViewerPanelLayout.viewportWidth(state));
        assertEquals(248, ViewerPanelLayout.CONTENT_WIDTH);
    }

    private static MultiblockDefinition definition() {
        BlockRequirement requirement = new BlockRequirement(List.of(new BlockOption(BlockOption.Kind.BLOCK,
            ResourceLocation.parse("minecraft:stone"))), 0, "");
        return new MultiblockDefinition(ResourceLocation.parse("test:viewer"), "Test", "", List.of(ResourceLocation.parse("minecraft:stick")),
            "base", Map.of(
                "base", new StructureVariant("base", "Base", 1, 1, 1, Map.of(CELL, requirement)),
                "other", new StructureVariant("other", "Other", 1, 1, 1, Map.of())
            ));
    }
}
