package net.sprocketgames.universalmultiblockviewer;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Common entry point. Viewer-host adapters are discovered only when their host mod is installed. */
@Mod(UniversalMultiblockViewer.MOD_ID)
public final class UniversalMultiblockViewer {
    public static final String MOD_ID = "universal_multiblock_viewer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UniversalMultiblockViewer(ModContainer modContainer) {
        LOGGER.info("Universal Multiblock Viewer is loading");
    }
}
