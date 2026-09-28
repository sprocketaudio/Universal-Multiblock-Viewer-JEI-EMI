package net.sprocketgames.universalmultiblockviewer.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/** A complete, creator-authored multiblock document. It describes a guide only, never machine validation. */
public record MultiblockDefinition(ResourceLocation id, String title, String description,
                                   List<ResourceLocation> useLookupItems, List<ResourceLocation> recipeLookupItems, String defaultVariant,
                                   Map<String, StructureVariant> variants, String titleKey, String descriptionKey,
                                   GuideProvenance provenance) {
    public MultiblockDefinition(ResourceLocation id, String title, String description,
                                List<ResourceLocation> useLookupItems, List<ResourceLocation> recipeLookupItems, String defaultVariant,
                                Map<String, StructureVariant> variants) {
        this(id, title, description, useLookupItems, recipeLookupItems, defaultVariant, variants, "", "", GuideProvenance.NONE);
    }

    /** Concise model fixture constructor: its item list is an explicit U lookup list. */
    public MultiblockDefinition(ResourceLocation id, String title, String description,
                                List<ResourceLocation> useLookupItems, String defaultVariant,
                                Map<String, StructureVariant> variants) {
        this(id, title, description, useLookupItems, List.of(), defaultVariant, variants, "", "", GuideProvenance.NONE);
    }

    public MultiblockDefinition(ResourceLocation id, String title, String description,
                                List<ResourceLocation> useLookupItems, List<ResourceLocation> recipeLookupItems, String defaultVariant,
                                Map<String, StructureVariant> variants, String titleKey, String descriptionKey) {
        this(id, title, description, useLookupItems, recipeLookupItems, defaultVariant, variants, titleKey, descriptionKey, GuideProvenance.NONE);
    }

    public MultiblockDefinition {
        Objects.requireNonNull(id, "id");
        title = Objects.requireNonNullElse(title, id.toString());
        description = Objects.requireNonNullElse(description, "");
        titleKey = Objects.requireNonNullElse(titleKey, "");
        descriptionKey = Objects.requireNonNullElse(descriptionKey, "");
        provenance = Objects.requireNonNullElse(provenance, GuideProvenance.NONE);
        useLookupItems = List.copyOf(useLookupItems);
        recipeLookupItems = List.copyOf(recipeLookupItems);
        variants = Collections.unmodifiableMap(new LinkedHashMap<>(variants));
        if (useLookupItems.isEmpty() && recipeLookupItems.isEmpty()) {
            throw new IllegalArgumentException("definition requires at least one U or R lookup item");
        }
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("definition requires a variant");
        }
        if (!variants.containsKey(defaultVariant)) {
            throw new IllegalArgumentException("default_variant does not name a variant");
        }
    }

    public StructureVariant initialVariant() {
        return variants.get(defaultVariant);
    }

    public String displayTitle() {
        return titleKey.isBlank() ? title : Component.translatable(titleKey).getString();
    }

    public String displayDescription() {
        return descriptionKey.isBlank() ? description : Component.translatable(descriptionKey).getString();
    }
}
