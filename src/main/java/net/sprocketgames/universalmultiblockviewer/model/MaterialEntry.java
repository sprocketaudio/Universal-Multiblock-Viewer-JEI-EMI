package net.sprocketgames.universalmultiblockviewer.model;

/** One BOM total based on the authored default for a requirement. */
public record MaterialEntry(BlockRequirement requirement, int count) {
}
