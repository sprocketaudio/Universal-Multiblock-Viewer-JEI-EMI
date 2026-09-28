package net.sprocketgames.universalmultiblockviewer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class OptionalHostIsolationTest {
    @Test
    void commonBootstrapLinksWithoutLoadingJeiOrEmiAdapters() {
        assertDoesNotThrow(() -> Class.forName(UniversalMultiblockViewer.class.getName(), true,
            UniversalMultiblockViewer.class.getClassLoader()));
    }
}
