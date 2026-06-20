package com.leclowndu93150.particular.particles;

import com.leclowndu93150.particular.client.ParticleSprites;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Animated billboard cascade particle - 12-frame splash sheet.
 *
 * <p>1.20.1 port: {@code CascadeParticle} extended {@code TextureSheetParticle}.
 * In 1.12.2 we extend {@link Particle} directly and animate via
 * {@link #setParticleTexture(TextureAtlasSprite)} each tick.
 */
@SideOnly(Side.CLIENT)
public class CascadeParticle extends Particle {

    /** 12-frame sprite sheet resolved by {@link com.leclowndu93150.particular.client.ParticleSprites#cascade()}. */
    private final TextureAtlasSprite[] frames;

    /**
     * Construct a cascade particle.
     *
     * @param world  client world
     * @param x      spawn X
     * @param y      spawn Y
     * @param z      spawn Z
     * @param frames 12-frame animated sprite array ({@code ParticleSprites.cascade()})
     */
    public CascadeParticle(World world, double x, double y, double z, TextureAtlasSprite[] frames) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.frames = frames;

        // 1.20 quadSize=0.7 → 1.12.2 particleScale = 0.7×10 = 7.0 (Gotcha #12a)
        this.particleScale = 7.0F;
        this.particleGravity = 0.12F;
        this.particleMaxAge = 18;

        // Initial small random horizontal motion
        this.motionX = (this.rand.nextFloat() * 0.1 - 0.05);
        this.motionY = 0.0;
        this.motionZ = (this.rand.nextFloat() * 0.1 - 0.05);

        // Set first frame
        if (frames.length > 0) {
            this.setParticleTexture(frames[0]);
        }

        this.canCollide = true;
        this.removeIfInsideSolidBlock();
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

        // Custom motion: gravity then move, then horizontal friction 0.92
        this.motionY -= 0.04D * (double) this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.92D;
        this.motionZ *= 0.92D;

        // Advance animation frame (12 frames, driven by age/maxAge ratio)
        int idx = (int) ((long) this.frames.length * this.particleAge / this.particleMaxAge);
        if (idx >= this.frames.length) {
            idx = this.frames.length - 1;
        }
        this.setParticleTexture(this.frames[idx]);

        this.removeIfInsideSolidBlock();
    }

    /**
     * If the particle is inside a full solid cube, hide it (alpha 0) and expire.
     */
    private void removeIfInsideSolidBlock() {
        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        if (this.world.getBlockState(pos).isFullCube()) {
            this.particleAlpha = 0.0F;
            this.setExpired();
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    /** Spawn a cascade particle, scaling its base size by {@code scaleFactor}. */
    public static void spawn(World world, double x, double y, double z, float scaleFactor) {
        CascadeParticle p = new CascadeParticle(world, x, y, z, ParticleSprites.cascade());
        p.multipleParticleScaleBy(scaleFactor);
        Minecraft.getMinecraft().effectRenderer.addEffect(p);
    }
}
