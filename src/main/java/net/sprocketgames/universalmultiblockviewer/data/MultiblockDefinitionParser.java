package net.sprocketgames.universalmultiblockviewer.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;
import net.sprocketgames.universalmultiblockviewer.model.BlockRequirement;
import net.sprocketgames.universalmultiblockviewer.model.GridPos;
import net.sprocketgames.universalmultiblockviewer.model.GuideProvenance;
import net.sprocketgames.universalmultiblockviewer.model.MultiblockDefinition;
import net.sprocketgames.universalmultiblockviewer.model.StructureVariant;

/** Strict parser for the version-one, client-resource definition format. */
public final class MultiblockDefinitionParser {
    public static final int MAX_DIMENSION = 64;
    public static final int MAX_CELLS = 4_096;
    public static final int MAX_ALTERNATIVES = 64;

    private MultiblockDefinitionParser() {
    }

    public static MultiblockDefinition parse(ResourceLocation sourceId, JsonObject root) {
        int format = integer(required(root, "format"), "format");
        if (format != 1) {
            throw error("format", "expected supported format 1, got " + format);
        }
        ResourceLocation id = id(root.has("id") ? string(root.get("id"), "id") : sourceId.toString(), "id");
        if (!root.has("title") && !root.has("title_key")) {
            throw error("title", "title or title_key is required");
        }
        String title = root.has("title") ? string(root.get("title"), "title") : sourceId.toString();
        String titleKey = root.has("title_key") ? string(root.get("title_key"), "title_key") : "";
        String description = root.has("description") ? string(root.get("description"), "description") : "";
        String descriptionKey = root.has("description_key") ? string(root.get("description_key"), "description_key") : "";
        if (root.has("associated_items")) {
            throw error("associated_items", "was replaced by lookups.U and lookups.R");
        }
        LookupItems lookupItems = parseLookups(object(required(root, "lookups"), "lookups"));
        Map<String, StructureVariant> variants = new LinkedHashMap<>();
        JsonArray rawVariants = array(required(root, "variants"), "variants");
        if (rawVariants.isEmpty()) {
            throw error("variants", "must contain at least one variant");
        }
        for (int index = 0; index < rawVariants.size(); index++) {
            StructureVariant variant = parseVariant(object(rawVariants.get(index), "variants[" + index + "]"), index);
            if (variants.putIfAbsent(variant.id(), variant) != null) {
                throw error("variants[" + index + "].id", "duplicate variant id '" + variant.id() + "'");
            }
        }
        String defaultVariant = root.has("default_variant")
            ? string(root.get("default_variant"), "default_variant")
            : variants.keySet().iterator().next();
        GuideProvenance provenance = root.has("provenance")
            ? parseProvenance(object(root.get("provenance"), "provenance"))
            : GuideProvenance.NONE;
        return new MultiblockDefinition(id, title, description, lookupItems.uses(), lookupItems.recipes(), defaultVariant, variants,
            titleKey, descriptionKey, provenance);
    }

    private static LookupItems parseLookups(JsonObject lookups) {
        for (String key : lookups.keySet()) {
            if (!key.equals("U") && !key.equals("R")) {
                throw error("lookups." + key, "unknown lookup shortcut; expected U or R");
            }
        }
        List<ResourceLocation> uses = lookups.has("U") ? lookupIds(array(lookups.get("U"), "lookups.U"), "lookups.U") : List.of();
        List<ResourceLocation> recipes = lookups.has("R") ? lookupIds(array(lookups.get("R"), "lookups.R"), "lookups.R") : List.of();
        if (uses.isEmpty() && recipes.isEmpty()) {
            throw error("lookups", "requires at least one item in U or R");
        }
        if (uses.size() != uses.stream().distinct().count()) {
            throw error("lookups.U", "must not contain duplicate item ids");
        }
        if (recipes.size() != recipes.stream().distinct().count()) {
            throw error("lookups.R", "must not contain duplicate item ids");
        }
        return new LookupItems(uses, recipes);
    }

    private static GuideProvenance parseProvenance(JsonObject provenance) {
        String sourceMod = provenance.has("source_mod") ? string(provenance.get("source_mod"), "provenance.source_mod") : "";
        if (!sourceMod.isEmpty() && !sourceMod.matches("[a-z][a-z0-9_-]{0,63}")) {
            throw error("provenance.source_mod", "must be a lowercase mod id");
        }
        return new GuideProvenance(sourceMod,
            provenance.has("tested_version") ? string(provenance.get("tested_version"), "provenance.tested_version") : "",
            provenance.has("notes") ? string(provenance.get("notes"), "provenance.notes") : "");
    }

    private static StructureVariant parseVariant(JsonObject variant, int index) {
        String path = "variants[" + index + "]";
        String id = string(required(variant, "id", path), path + ".id");
        String title = variant.has("title") ? string(variant.get("title"), path + ".title") : id;
        Map<Character, BlockRequirement> palette = parsePalette(object(required(variant, "palette", path), path + ".palette"), path + ".palette");
        JsonArray layers = array(required(variant, "layers", path), path + ".layers");
        if (layers.isEmpty() || layers.size() > MAX_DIMENSION) {
            throw error(path + ".layers", "must contain 1-" + MAX_DIMENSION + " layers");
        }
        Map<GridPos, BlockRequirement> cells = new LinkedHashMap<>();
        int width = -1;
        int depth = -1;
        for (int y = 0; y < layers.size(); y++) {
            JsonArray layer = array(layers.get(y), path + ".layers[" + y + "]");
            if (layer.isEmpty() || layer.size() > MAX_DIMENSION) {
                throw error(path + ".layers[" + y + "]", "must contain 1-" + MAX_DIMENSION + " rows");
            }
            if (depth == -1) {
                depth = layer.size();
            } else if (depth != layer.size()) {
                throw error(path + ".layers[" + y + "]", "row count must match every other layer");
            }
            for (int z = 0; z < layer.size(); z++) {
                String row = string(layer.get(z), path + ".layers[" + y + "][" + z + "]");
                if (row.isEmpty() || row.length() > MAX_DIMENSION) {
                    throw error(path + ".layers[" + y + "][" + z + "]", "width must be 1-" + MAX_DIMENSION);
                }
                if (width == -1) {
                    width = row.length();
                } else if (width != row.length()) {
                    throw error(path + ".layers[" + y + "][" + z + "]", "width must match every row");
                }
                for (int x = 0; x < row.length(); x++) {
                    char symbol = row.charAt(x);
                    if (symbol == ' ') {
                        continue;
                    }
                    BlockRequirement requirement = palette.get(symbol);
                    if (requirement == null) {
                        throw error(path + ".layers[" + y + "][" + z + "][" + x + "]", "unknown palette symbol '" + symbol + "'");
                    }
                    cells.put(new GridPos(x, y, z), requirement);
                }
            }
        }
        if (cells.size() > MAX_CELLS) {
            throw error(path, "contains " + cells.size() + " blocks; maximum is " + MAX_CELLS);
        }
        if (cells.isEmpty()) {
            throw error(path, "must contain at least one non-air palette cell");
        }
        return new StructureVariant(id, title, width, layers.size(), depth, cells);
    }

    private static Map<Character, BlockRequirement> parsePalette(JsonObject palette, String path) {
        if (palette.isEmpty()) {
            throw error(path, "may not be empty");
        }
        Map<Character, BlockRequirement> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : palette.entrySet()) {
            if (entry.getKey().length() != 1 || entry.getKey().charAt(0) == ' ') {
                throw error(path, "palette keys must be one non-space character");
            }
            JsonObject value = object(entry.getValue(), path + "." + entry.getKey());
            List<BlockOption> options = new ArrayList<>();
            int formCount = (value.has("block") ? 1 : 0) + (value.has("tag") ? 1 : 0) + (value.has("any_of") ? 1 : 0);
            if (formCount != 1) {
                throw error(path + "." + entry.getKey(), "entry needs exactly one of block, tag, or any_of");
            }
            if (value.has("block")) {
                options.add(parseOption(value, path + "." + entry.getKey()));
            } else if (value.has("tag")) {
                options.add(parseOption(value, path + "." + entry.getKey()));
            } else if (value.has("any_of")) {
                JsonArray rawOptions = array(value.get("any_of"), path + "." + entry.getKey() + ".any_of");
                if (rawOptions.isEmpty()) {
                    throw error(path, "any_of may not be empty");
                }
                if (rawOptions.size() > MAX_ALTERNATIVES) {
                    throw error(path + "." + entry.getKey() + ".any_of", "supports at most " + MAX_ALTERNATIVES + " alternatives");
                }
                for (int index = 0; index < rawOptions.size(); index++) {
                    JsonObject option = object(rawOptions.get(index), path + "." + entry.getKey() + ".any_of[" + index + "]");
                    options.add(parseOption(option, path + "." + entry.getKey() + ".any_of[" + index + "]"));
                }
                if (options.size() != options.stream().map(BlockOption::id).distinct().count()) {
                    throw error(path + "." + entry.getKey() + ".any_of", "must not contain duplicate block or tag ids");
                }
            } else {
                throw error(path, "entry needs block, tag, or any_of");
            }
            int defaultOption = 0;
            if (value.has("default")) {
                ResourceLocation defaultId = id(string(value.get("default"), path), path + ".default");
                defaultOption = -1;
                for (int optionIndex = 0; optionIndex < options.size(); optionIndex++) {
                    if (options.get(optionIndex).id().equals(defaultId)) {
                        defaultOption = optionIndex;
                        break;
                    }
                }
                if (defaultOption < 0) {
                    throw error(path + ".default", "must name an option in any_of");
                }
            }
            parsed.put(entry.getKey().charAt(0), new BlockRequirement(options, defaultOption,
                value.has("label") ? string(value.get("label"), path + ".label") : ""));
        }
        return Map.copyOf(parsed);
    }

    private static BlockOption parseOption(JsonObject option, String path) {
        boolean hasBlock = option.has("block");
        boolean hasTag = option.has("tag");
        if (hasBlock == hasTag) {
            throw error(path, "needs exactly one of block or tag");
        }
        Map<String, String> properties = new LinkedHashMap<>();
        if (option.has("state")) {
            if (!hasBlock) {
                throw error(path + ".state", "is only supported for an exact block");
            }
            JsonObject state = object(option.get("state"), path + ".state");
            for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
                if (entry.getKey().isBlank()) {
                    throw error(path + ".state", "property names may not be blank");
                }
                properties.put(entry.getKey(), string(entry.getValue(), path + ".state." + entry.getKey()));
            }
        }
        String idPath = path + (hasBlock ? ".block" : ".tag");
        return new BlockOption(hasBlock ? BlockOption.Kind.BLOCK : BlockOption.Kind.TAG,
            id(string(option.get(hasBlock ? "block" : "tag"), idPath), idPath), properties);
    }

    private static List<ResourceLocation> ids(JsonArray values, String path) {
        if (values.isEmpty()) {
            throw error(path, "may not be empty");
        }
        List<ResourceLocation> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            result.add(id(string(values.get(index), path), path + "[" + index + "]"));
        }
        return List.copyOf(result);
    }

    /** Lookup arrays may be present but empty when the other shortcut supplies the guide's entry point. */
    private static List<ResourceLocation> lookupIds(JsonArray values, String path) {
        List<ResourceLocation> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            result.add(id(string(values.get(index), path), path + "[" + index + "]"));
        }
        return List.copyOf(result);
    }

    private static JsonElement required(JsonObject object, String key) {
        return required(object, key, "");
    }

    private static JsonElement required(JsonObject object, String key, String path) {
        if (!object.has(key)) {
            throw error(path.isEmpty() ? key : path + "." + key, "is required");
        }
        return object.get(key);
    }

    private static String string(JsonElement value, String path) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw error(path, "must be a string");
        }
        return value.getAsString();
    }

    private static JsonArray array(JsonElement value, String path) {
        if (!value.isJsonArray()) {
            throw error(path, "must be an array");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject object(JsonElement value, String path) {
        if (!value.isJsonObject()) {
            throw error(path, "must be an object");
        }
        return value.getAsJsonObject();
    }

    private static int integer(JsonElement value, String path) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw error(path, "must be an integer");
        }
        double parsed = value.getAsDouble();
        if (parsed != Math.rint(parsed) || parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
            throw error(path, "must be an integer");
        }
        return (int) parsed;
    }

    private static ResourceLocation id(String raw, String path) {
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed == null) {
            throw error(path, "invalid resource id '" + raw + "'");
        }
        return parsed;
    }

    private static IllegalArgumentException error(String path, String message) {
        return new IllegalArgumentException(path + ": " + message);
    }

    private record LookupItems(List<ResourceLocation> uses, List<ResourceLocation> recipes) { }
}
