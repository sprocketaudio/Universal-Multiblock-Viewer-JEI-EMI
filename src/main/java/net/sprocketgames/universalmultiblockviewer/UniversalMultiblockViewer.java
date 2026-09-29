package net.sprocketgames.universalmultiblockviewer;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.sprocketgames.universalmultiblockviewer.dev.DevInstantBuildConfig;
import net.sprocketgames.universalmultiblockviewer.dev.DevInstantBuildNetworking;
import org.slf4j.Logger;

/** Common entry point. Viewer-host adapters are discovered only when their host mod is installed. */
@Mod(UniversalMultiblockViewer.MOD_ID)
public final class UniversalMultiblockViewer {
    public static final String MOD_ID = "universal_multiblock_viewer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UniversalMultiblockViewer(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, DevInstantBuildConfig.SPEC);
        modContainer.getEventBus().addListener(DevInstantBuildNetworking::register);
        LOGGER.info("Universal Multiblock Viewer is loading");
    }
}
