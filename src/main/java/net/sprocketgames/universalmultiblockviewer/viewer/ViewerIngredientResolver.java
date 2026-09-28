package net.sprocketgames.universalmultiblockviewer.viewer;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
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

    public static Block blockFor(BlockOption option) {
        if (option.kind() == BlockOption.Kind.TAG) {
            return BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, option.id()))
                .flatMap(values -> values.stream().findFirst())
                .map(holder -> holder.value())
                .orElse(Blocks.BARRIER);
        }
        Block block = BuiltInRegistries.BLOCK.get(option.id());
        if (block == null || block.asItem() == Items.AIR) {
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
