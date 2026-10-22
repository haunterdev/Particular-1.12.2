package com.leclowndu93150.particular.particles.leaves;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Needle/conifer leaf: heavier, falls faster. Port of 1.20.1 {@code ConiferLeafParticle}.
 */
@SideOnly(Side.CLIENT)
public class ConiferLeafParticle extends LeafParticle {

    public ConiferLeafParticle(World world, double x, double y, double z,
                               float r, float g, float b, TextureAtlasSprite sprite) {
        super(world, x, y, z, r, g, b, sprite);
        this.gravityFactor = 0.3f;
    }
}
