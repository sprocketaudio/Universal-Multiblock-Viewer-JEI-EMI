package net.sprocketgames.universalmultiblockviewer.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Left-aligned title marquee with a pause before travel and before it restarts. */
public final class ScrollingTitle {
    private static final long START_PAUSE_MS = 1_600L;
    private static final long END_PAUSE_MS = 1_000L;
    private static final double PIXELS_PER_SECOND = 24.0D;
    private static final Map<String, Long> FIRST_SEEN = new ConcurrentHashMap<>();

    private ScrollingTitle() {
    }

    public static void draw(GuiGraphics graphics, Font font, String text, int x, int y, int width, int color) {
        int textWidth = font.width(text);
        if (textWidth <= width) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        long firstSeen = FIRST_SEEN.computeIfAbsent(text + '\u0000' + width, ignored -> System.currentTimeMillis());
        double overflow = textWidth - width;
        long travelMs = Math.max(1L, Math.round(overflow / PIXELS_PER_SECOND * 1_000.0D));
        long cycleMs = START_PAUSE_MS + travelMs + END_PAUSE_MS;
        long phase = Math.floorMod(System.currentTimeMillis() - firstSeen, cycleMs);
        double offset = phase <= START_PAUSE_MS ? 0.0D
            : phase < START_PAUSE_MS + travelMs ? overflow * (phase - START_PAUSE_MS) / travelMs
            : overflow;

        var pose = graphics.pose().last().pose();
        int screenX = Math.round(pose.m30()) + x;
        int screenY = Math.round(pose.m31()) + y;
        graphics.enableScissor(screenX, screenY, screenX + width, screenY + font.lineHeight);
        try {
            graphics.drawString(font, text, x - (int) Math.round(offset), y, color, false);
        } finally {
            graphics.disableScissor();
        }
    }
}
