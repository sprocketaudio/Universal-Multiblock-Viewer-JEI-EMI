package net.sprocketgames.universalmultiblockviewer.dev;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.sprocketgames.universalmultiblockviewer.UniversalMultiblockViewer;

/** Client-only selection and JSON export for the private developer build. */
public final class DevStructureCapture {
    private static final String SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static BlockPos first;
    private static BlockPos second;
    private DevStructureCapture() { }
    public static Optional<BlockPos> first() { return Optional.ofNullable(first); }
    public static Optional<BlockPos> second() { return Optional.ofNullable(second); }
    public static void setFirst(BlockPos position) { first = position.immutable(); second = null; }
    public static void setSecond(BlockPos position) { second = position.immutable(); }
    public static void clear() { first = null; second = null; }
    public static Optional<Bounds> bounds() {
        if (first == null || second == null) return Optional.empty();
        return Optional.of(new Bounds(BlockPos.min(first, second), BlockPos.max(first, second)));
    }
    public static Path save(String namespace, String fileName) throws IOException {
        Bounds bounds = bounds().orElseThrow(() -> new IllegalStateException("Select both corners first."));
        if (!namespace.matches("[a-z0-9_.-]+") || !fileName.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Namespace and file name must use lowercase resource-name characters.");
        int width = bounds.max.getX() - bounds.min.getX() + 1, height = bounds.max.getY() - bounds.min.getY() + 1, depth = bounds.max.getZ() - bounds.min.getZ() + 1;
        if (width > 64 || height > 64 || depth > 64 || (long) width * height * depth > 4096) throw new IllegalArgumentException("Capture must be at most 64 blocks in each direction and 4,096 cells.");
        var level = Minecraft.getInstance().level;
        if (level == null) throw new IllegalStateException("No world is loaded.");
        LinkedHashMap<StateKey, Character> palette = new LinkedHashMap<>();
        JsonArray layers = new JsonArray();
        ResourceLocation lookup = null;
        for (int y = bounds.min.getY(); y <= bounds.max.getY(); y++) {
            JsonArray layer = new JsonArray();
            for (int z = bounds.min.getZ(); z <= bounds.max.getZ(); z++) {
                StringBuilder row = new StringBuilder();
                for (int x = bounds.min.getX(); x <= bounds.max.getX(); x++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (state.isAir()) { row.append(' '); continue; }
                    StateKey key = StateKey.from(state);
                    Character symbol = palette.get(key);
                    if (symbol == null) {
                        if (palette.size() >= SYMBOLS.length()) throw new IllegalArgumentException("Capture has more than " + SYMBOLS.length() + " distinct block states.");
                        symbol = SYMBOLS.charAt(palette.size()); palette.put(key, symbol);
                    }
                    row.append(symbol);
                    if (lookup == null && state.getBlock().asItem() != net.minecraft.world.item.Items.AIR) lookup = BuiltInRegistries.ITEM.getKey(state.getBlock().asItem());
                }
                layer.add(row.toString());
            }
            layers.add(layer);
        }
        if (lookup == null) throw new IllegalArgumentException("Capture has no blocks with an item form for its initial U lookup.");
        JsonObject root = new JsonObject(); root.addProperty("format", 1); root.addProperty("id", namespace + ":" + fileName); root.addProperty("title", title(fileName));
        JsonObject lookups = new JsonObject(); JsonArray uses = new JsonArray(); uses.add(lookup.toString()); lookups.add("U", uses); root.add("lookups", lookups); root.addProperty("default_variant", "captured");
        JsonObject variant = new JsonObject(); variant.addProperty("id", "captured"); variant.addProperty("title", "Captured");
        JsonObject paletteJson = new JsonObject();
        palette.forEach((key, symbol) -> paletteJson.add(String.valueOf(symbol), key.toJson()));
        variant.add("palette", paletteJson); variant.add("layers", layers); JsonArray variants = new JsonArray(); variants.add(variant); root.add("variants", variants);
        Path output = Minecraft.getInstance().gameDirectory.toPath().resolve("kubejs/assets").resolve(namespace).resolve("universal_multiblock_viewer/multiblocks").resolve(fileName + ".json");
        Files.createDirectories(output.getParent()); Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(root) + System.lineSeparator());
        UniversalMultiblockViewer.LOGGER.info("Saved UMV developer capture {} to {}", namespace + ":" + fileName, output);
        return output;
    }
    private static String title(String fileName) { return java.util.Arrays.stream(fileName.replace('-', '_').split("_")).filter(s -> !s.isBlank()).map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1)).collect(java.util.stream.Collectors.joining(" ")); }
    public record Bounds(BlockPos min, BlockPos max) { }
    private record StateKey(ResourceLocation blockId, Map<String, String> state) {
        static StateKey from(BlockState state) {
            Map<String, String> values = new LinkedHashMap<>(); BlockState defaults = state.getBlock().defaultBlockState();
            state.getValues().forEach((property, value) -> { if (!defaults.getValue(property).equals(value)) values.put(property.getName(), propertyName(property, value)); });
            return new StateKey(BuiltInRegistries.BLOCK.getKey(state.getBlock()), Map.copyOf(values));
        }
        @SuppressWarnings({"rawtypes", "unchecked"}) private static String propertyName(Property property, Comparable value) { return property.getName(value); }
        JsonObject toJson() { JsonObject value = new JsonObject(); value.addProperty("block", blockId.toString()); if (!state.isEmpty()) { JsonObject states = new JsonObject(); state.forEach(states::addProperty); value.add("state", states); } return value; }
    }
}
