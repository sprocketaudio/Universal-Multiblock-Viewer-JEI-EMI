package net.sprocketgames.universalmultiblockviewer.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import org.junit.jupiter.api.Test;

class BlockModelViewportRendererTest {
    @Test
    void proceduralGlyphSignIsStableAndWithinTheBlockRange() {
        GridPos position = new GridPos(7, 2, -4);
        int first = BlockModelViewportRenderer.proceduralSignIndex(position, 14);

        assertEquals(first, BlockModelViewportRenderer.proceduralSignIndex(position, 14));
        assertTrue(first >= 0 && first < 14);
    }
}
