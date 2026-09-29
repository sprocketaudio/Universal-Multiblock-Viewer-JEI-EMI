package net.sprocketgames.universalmultiblockviewer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Keeps authored block models that extend beyond their voxel centred in the viewer's compact icon cells. */
public final class ViewerItemIconRenderer {
    private static final float SAFE_MODEL_SPAN = 1.04F;

    private ViewerItemIconRenderer() {
    }

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        float scale = compactScale(stack);
        if (scale == 1.0F) {
            graphics.renderItem(stack, x, y);
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + 8.0F, y + 8.0F, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.pose().translate(-x - 8.0F, -y - 8.0F, 0.0F);
            graphics.renderItem(stack, x, y);
        } finally {
            graphics.pose().popPose();
        }
    }

    /**
     * Block-model authors may intentionally put geometry outside the normal 0..1 voxel bounds.
     * Read those baked vertices generically and only shrink models that would exceed the compact
     * icon frame. Custom renderers keep their own declared presentation because they expose no
     * stable baked bounds to inspect.
     */
    private static float compactScale(ItemStack stack) {
        var model = Minecraft.getInstance().getItemRenderer().getModel(stack, Minecraft.getInstance().level, null, 0);
        if (model.isCustomRenderer()) return 1.0F;
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        RandomSource random = RandomSource.create(42L);
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, direction, random)) {
                float[] bounds = includeQuad(quad, minX, minY, minZ, maxX, maxY, maxZ);
                minX = bounds[0]; minY = bounds[1]; minZ = bounds[2];
                maxX = bounds[3]; maxY = bounds[4]; maxZ = bounds[5];
            }
        }
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(null, null, random)) {
            float[] bounds = includeQuad(quad, minX, minY, minZ, maxX, maxY, maxZ);
            minX = bounds[0]; minY = bounds[1]; minZ = bounds[2];
            maxX = bounds[3]; maxY = bounds[4]; maxZ = bounds[5];
        }
        if (!Float.isFinite(minX)) return 1.0F;
        float largestSpan = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        return largestSpan > SAFE_MODEL_SPAN ? SAFE_MODEL_SPAN / largestSpan : 1.0F;
    }

    private static float[] includeQuad(BakedQuad quad, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
        }
        return new float[] {minX, minY, minZ, maxX, maxY, maxZ};
    }
}
