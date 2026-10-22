package com.leclowndu93150.particular.particles.leaves;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Leaf that spins continuously instead of swaying. Port of 1.20.1
 * {@code SpinningLeafParticle}: overrides {@link #getAngle()} with a monotonic
 * accumulating rotation rather than a damped sine.
 */
@SideOnly(Side.CLIENT)
public class SpinningLeafParticle extends LeafParticle {

    private final float angleFactor = (float) (Math.random() * Math.PI * 2.0);

    public SpinningLeafParticle(World world, double x, double y, double z,
                                float r, float g, float b, TextureAtlasSprite sprite) {
        super(world, x, y, z, r, g, b, sprite);
        this.particleAngle = this.getAngle();
    }

    @Override
    protected float getAngle() {
        int a = Math.max(0, this.particleAge - 10);
        return (this.angleFactor
                + (float) a / (this.rotateFactor + (float) (this.particleMaxAge - a) / 100.0f) / 2.0f)
                * (float) this.flippedDirection;
    }
}
