package com.leclowndu93150.particular.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Derives a falling-leaf colour from a leaf block. 1.12.2 version of the 1.20.1
 * {@code LeafColorUtil}.
 *
 * <p>The original averaged the leaf block's down-face texture and multiplied that by
 * the block's biome tint. We do the same here: average the non-transparent texels of
 * the block's down-face sprite (cached per sprite), then multiply by
 * {@link BlockColors#colorMultiplier} when the quad is tinted.
 *
 * <p>Pixel order from {@link TextureAtlasSprite#getFrameTextureData(int)} is
 * {@code 0xAARRGGBB} (ARGB) on the 1.12.2 block atlas.
 */
@SideOnly(Side.CLIENT)
public final class LeafColorUtil {

    /** Averaged texture colour (RGB, 0-1) keyed by atlas sprite name. */
    private static final Map<String, float[]> CACHE = new HashMap<String, float[]>();

    private LeafColorUtil() {
    }

    static void clearCache() {
        CACHE.clear();
    }

    /** @return {r, g, b} in 0-1, biome-tinted, for the leaf block at {@code pos}. */
    public static float[] getLeafColor(World world, BlockPos pos, IBlockState state) {
        Minecraft mc = Minecraft.getMinecraft();
        IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(state);

        TextureAtlasSprite sprite;
        int tintIndex;
        List<BakedQuad> quads = model.getQuads(state, EnumFacing.DOWN, 0L);
        if (!quads.isEmpty()) {
            BakedQuad quad = quads.get(0);
            sprite = quad.getSprite();
            tintIndex = quad.hasTintIndex() ? quad.getTintIndex() : -1;
        } else {
            sprite = model.getParticleTexture();
            tintIndex = 0;
        }

        float[] base = averageColor(sprite);
        float[] result = new float[]{base[0], base[1], base[2]};
        if (tintIndex >= 0) {
            int tint = mc.getBlockColors().colorMultiplier(state, world, pos, tintIndex);
            if (tint != -1) {
                result[0] *= ((tint >> 16) & 0xFF) / 255.0f;
                result[1] *= ((tint >> 8) & 0xFF) / 255.0f;
                result[2] *= (tint & 0xFF) / 255.0f;
            }
        }
        result[0] = clamp01(result[0]);
        result[1] = clamp01(result[1]);
        result[2] = clamp01(result[2]);
        return result;
    }

    /** Average of the non-transparent texels of {@code sprite} (RGB, 0-1), cached. */
    private static float[] averageColor(TextureAtlasSprite sprite) {
        String key = sprite.getIconName();
        float[] cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        float[] color = {1.0f, 1.0f, 1.0f};
        try {
            int[][] frames = sprite.getFrameTextureData(0);
            if (frames != null && frames.length > 0 && frames[0] != null && frames[0].length > 0) {
                long r = 0, g = 0, b = 0;
                int n = 0;
                for (int argb : frames[0]) {
                    int a = (argb >> 24) & 0xFF;
                    if (a == 0) {
                        continue;
                    }
                    r += (argb >> 16) & 0xFF;
                    g += (argb >> 8) & 0xFF;
                    b += argb & 0xFF;
                    n++;
                }
                if (n > 0) {
                    color[0] = (float) r / n / 255.0f;
                    color[1] = (float) g / n / 255.0f;
                    color[2] = (float) b / n / 255.0f;
                }
            }
        } catch (Exception ignored) {
            // Sprite without retained pixel data: fall back to white (× biome tint still applies).
        }

        CACHE.put(key, color);
        return color;
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
