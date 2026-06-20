package com.leclowndu93150.particular.particles;

import com.leclowndu93150.particular.client.ParticleSprites;
import com.leclowndu93150.particular.config.ParticularConfig;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Rising bubble particle that ascends through water and pops at the surface.
 *
 * <p>1.20.1 extended {@code TextureSheetParticle}. Negative gravity produces
 * upward motion; friction of 0.85 applies each tick. When the bubble reaches
 * a surface (onGround) it spawns an {@link EnderBubblePopParticle} if
 * {@link ParticularConfig#poppingBubbles} is enabled.
 */
@SideOnly(Side.CLIENT)
public class EnderBubbleParticle extends Particle {

    public EnderBubbleParticle(World world, double x, double y, double z,
                                double mX, double mY, double mZ,
                                TextureAtlasSprite sprite) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleTexture(sprite);

        // Negative gravity → rises. Stronger than 1.20 so bubbles actually climb a
        // multi-block water column to the surface (1.12.2 friction is applied harder).
        this.particleGravity = -0.45F;

        // Size: tiny hitbox, visible variable scale (base bumped for 1.12.2 visibility)
        this.setSize(0.02F, 0.02F);
        this.particleScale = (this.rand.nextFloat() * 0.6F + 0.2F) * 2.5F;

        // Initial motion: damped incoming velocity + tiny random offset
        this.motionX = mX * 0.2 + (this.rand.nextFloat() * 2.0 - 1.0) * 0.02;
        this.motionY = mY * 0.2 + (this.rand.nextFloat() * 2.0 - 1.0) * 0.02;
        this.motionZ = mZ * 0.2 + (this.rand.nextFloat() * 2.0 - 1.0) * 0.02;

        // Long life: the bubble ends by POPPING when it reaches the surface, not by
        // timing out - so it must live long enough to climb a tall water column.
        this.particleMaxAge = (int) (160.0 / (this.rand.nextFloat() * 0.8 + 0.2));

        this.canCollide = true;
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

        // Custom motion: upward gravity (negative particleGravity → rises)
        // Formula matches vanilla: motionY -= 0.04 * particleGravity
        this.motionY -= 0.04D * (double) this.particleGravity;

        this.move(this.motionX, this.motionY, this.motionZ);

        // Lighter friction so the upward drift accumulates into a real climb.
        this.motionX *= 0.90D;
        this.motionY *= 0.95D;
        this.motionZ *= 0.90D;

        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        boolean inWater = this.world.getBlockState(pos).getMaterial() == Material.WATER;

        // Reached the surface (rose out of water) or hit a ceiling → POP and expire.
        if (!inWater || this.onGround) {
            if (ParticularConfig.poppingBubbles) {
                EnderBubblePopParticle pop = new EnderBubblePopParticle(
                        this.world,
                        this.posX, this.posY, this.posZ,
                        ParticleSprites.enderBubblePop(),
                        this.particleScale * 2.0F
                );
                Minecraft.getMinecraft().effectRenderer.addEffect(pop);
            }
            this.setExpired();
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
