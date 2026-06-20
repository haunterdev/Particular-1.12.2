package com.leclowndu93150.particular.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 5-frame animated bubble-pop particle - no motion, one frame per tick.
 *
 * <p>Spawned by {@link EnderBubbleParticle} when it reaches the water surface.
 * The caller passes the scale (typically {@code bubble.particleScale * 2}).
 */
@SideOnly(Side.CLIENT)
public class EnderBubblePopParticle extends Particle {

    private final TextureAtlasSprite[] frames;

    /**
     * Construct a bubble-pop particle.
     *
     * @param world  client world
     * @param x      spawn X
     * @param y      spawn Y
     * @param z      spawn Z
     * @param frames 5-frame sprite array ({@code ParticleSprites.enderBubblePop()})
     * @param scale  render scale (bubble size × 2)
     */
    public EnderBubblePopParticle(World world, double x, double y, double z,
                                   TextureAtlasSprite[] frames, float scale) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.frames = frames;

        this.particleScale = scale;
        this.particleMaxAge = 5;
        this.particleGravity = 0.0F;
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;

        // Set first frame
        if (frames.length > 0) {
            this.setParticleTexture(frames[0]);
        }
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        // One frame per tick - stops exactly at last frame
        int idx = Math.min(this.frames.length - 1, this.particleAge);
        this.setParticleTexture(this.frames[idx]);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
