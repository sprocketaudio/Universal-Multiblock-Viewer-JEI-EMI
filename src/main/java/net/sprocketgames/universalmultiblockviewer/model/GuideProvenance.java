package net.sprocketgames.universalmultiblockviewer.model;

import java.util.Objects;

/** Optional author-supplied evidence about an external machine guide. */
public record GuideProvenance(String sourceMod, String testedVersion, String notes) {
    public static final GuideProvenance NONE = new GuideProvenance("", "", "");

    public GuideProvenance {
        sourceMod = Objects.requireNonNullElse(sourceMod, "");
        testedVersion = Objects.requireNonNullElse(testedVersion, "");
        notes = Objects.requireNonNullElse(notes, "");
    }

    public boolean isPresent() {
        return !sourceMod.isBlank() || !testedVersion.isBlank() || !notes.isBlank();
    }
}
