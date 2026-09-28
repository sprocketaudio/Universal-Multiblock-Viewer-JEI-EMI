package net.sprocketgames.universalmultiblockviewer.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;
import net.sprocketgames.universalmultiblockviewer.data.MultiblockDefinitionReloadListener;

/** Client-only resource registration keeps viewer data independent of server machine logic. */
@EventBusSubscriber(modid = UniversalMultiblockViewer.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(MultiblockDefinitionReloadListener.INSTANCE);
    }
}
