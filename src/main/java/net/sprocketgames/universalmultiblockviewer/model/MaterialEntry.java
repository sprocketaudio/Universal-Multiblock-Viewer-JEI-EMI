package net.sprocketgames.universalmultiblockviewer.model;

/** One BOM total, retaining both placed positions and the collection count shown to the player. */
public record MaterialEntry(BlockRequirement requirement, int count, int placedCount, MaterialPresentation.Kind kind) {
}
