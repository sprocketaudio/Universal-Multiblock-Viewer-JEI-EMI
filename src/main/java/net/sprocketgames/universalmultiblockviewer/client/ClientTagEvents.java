package net.sprocketgames.universalmultiblockviewer.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;
import net.sprocketgames.universalmultiblockviewer.data.MultiblockDefinitionReloadListener;

/** Refreshes concrete tag alternatives after the client receives its live registry tags. */
@EventBusSubscriber(modid = UniversalMultiblockViewer.MOD_ID, value = Dist.CLIENT)
public final class ClientTagEvents {
    private ClientTagEvents() {
    }

    @SubscribeEvent
    public static void refreshDefinitionsAfterTags(TagsUpdatedEvent event) {
        MultiblockDefinitionReloadListener.INSTANCE.refreshResolvedDefinitions();
    }
}
