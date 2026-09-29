package net.sprocketgames.universalmultiblockviewer.viewer;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.sprocketgames.universalmultiblockviewer.model.BlockOption;

/** Resolves display defaults without assuming an external block is installed. */
public final class ViewerIngredientResolver {
    private ViewerIngredientResolver() {
    }

    public static ItemStack stackFor(BlockOption option) {
        return new ItemStack(blockFor(option));
    }

    /** The item an author chose for the BOM; rendering always continues to use {@link #stateFor}. */
    public static ItemStack materialStackFor(BlockOption option) {
        if (option.material().item() == null) return stackFor(option);
        Item item = BuiltInRegistries.ITEM.get(option.material().item());
        return item == null || item == Items.AIR ? new ItemStack(Items.BARRIER) : new ItemStack(item);
    }

    public static Block blockFor(BlockOption option) {
        if (option.kind() == BlockOption.Kind.TAG) {
            return BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, option.id()))
                .flatMap(values -> values.stream().findFirst())
                .map(holder -> holder.value())
                .orElse(Blocks.BARRIER);
        }
        Block block = BuiltInRegistries.BLOCK.get(option.id());
        // Some valid placeable blocks deliberately have no item form. They must still render in
        // the guide; only item presentation needs a separately authored material mapping.
        if (block == null || block == Blocks.AIR) {
            return Blocks.BARRIER;
        }
        return block;
    }

    public static BlockState stateFor(BlockOption option) {
        BlockState state = blockFor(option).defaultBlockState();
        for (Map.Entry<String, String> entry : option.stateProperties().entrySet()) {
            Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null) return Blocks.BARRIER.defaultBlockState();
            state = applyProperty(state, property, entry.getValue());
            if (state == null) return Blocks.BARRIER.defaultBlockState();
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyProperty(BlockState state, Property property, String rawValue) {
        var value = property.getValue(rawValue);
        if (value.isEmpty()) return null;
        return (BlockState) state.setValue(property, (Comparable) value.get());
    }
}
