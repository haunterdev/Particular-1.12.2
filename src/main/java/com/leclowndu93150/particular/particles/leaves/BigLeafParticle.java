package com.leclowndu93150.particular.particles.leaves;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Larger, slower-swaying leaf. Port of 1.20.1 {@code BigLeafParticle}.
 */
@SideOnly(Side.CLIENT)
public class BigLeafParticle extends LeafParticle {

    public BigLeafParticle(World world, double x, double y, double z,
                           float r, float g, float b, TextureAtlasSprite sprite) {
        super(world, x, y, z, r, g, b, sprite);
        this.rotateFactor = 9.0f + (float) Math.random() * 3.0f;
        // 1.20.1 quadSize 0.28125 → particleScale = quadSize * 10.
        this.particleScale = 2.8125f;
    }
}
