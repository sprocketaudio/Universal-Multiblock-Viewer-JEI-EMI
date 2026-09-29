package net.sprocketgames.universalmultiblockviewer.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;

/** Shared, host-neutral text layout for viewer tooltips. */
public final class ViewerTooltip {
    private ViewerTooltip() {
    }

    /**
     * Presents a guide's authored title as the tooltip heading and its description as
     * separately wrapped body text. This keeps long descriptions from becoming a
     * misleading continuation of the title.
     */
    public static List<Component> description(Font font, MultiblockDefinition definition, int width) {
        List<Component> lines = new ArrayList<>();
        addWrapped(lines, font, definition.displayTitle(), width);
        addWrapped(lines, font, definition.displayDescription(), width);
        return List.copyOf(lines);
    }

    /** Converts the shared component lines to Minecraft's immediate tooltip draw format. */
    public static List<FormattedCharSequence> descriptionVisual(Font font, MultiblockDefinition definition, int width) {
        return description(font, definition, width).stream().map(Component::getVisualOrderText).toList();
    }

    private static void addWrapped(List<Component> lines, Font font, String text, int width) {
        for (String authoredLine : text.split("\\R", -1)) {
            if (authoredLine.isBlank()) {
                lines.add(Component.empty());
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : authoredLine.trim().split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && font.width(candidate) > width) {
                    lines.add(Component.literal(line.toString()));
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            if (!line.isEmpty()) {
                lines.add(Component.literal(line.toString()));
            }
        }
    }
}
