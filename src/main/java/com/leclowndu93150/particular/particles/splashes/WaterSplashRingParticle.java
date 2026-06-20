package com.leclowndu93150.particular.particles.splashes;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Flat expanding ring on a water/lava surface. Renders a single quad in the
 * XZ plane, scaling from {@code width * 0.8} to {@code width * 1.0} over
 * 18 ticks with 9-frame animation.
 *
 * <p>Render idiom matches {@code WaterRippleParticle} - flat XZ quad, not
 * billboarded. Port of 1.20.1 {@code WaterSplashRingParticle}.
 */
@SideOnly(Side.CLIENT)
public class WaterSplashRingParticle extends Particle {

    private final TextureAtlasSprite[] frames;
    private final float width;
    private final boolean isLava;

    public WaterSplashRingParticle(World world, double x, double y, double z,
                                   float width, TextureAtlasSprite[] frames, boolean lava) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.particleGravity = 0.0f;
        this.particleMaxAge = 18;
        this.width = width;
        this.frames = frames;
        this.isLava = lava;

        if (lava) {
            this.particleRed = 0.8117647f;
            this.particleGreen = 0.36078432f;
            this.particleBlue = 0.05882353f;
        }
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

        int idx = (int) ((long) this.frames.length * this.particleAge / this.particleMaxAge);
        if (idx >= this.frames.length) {
            idx = this.frames.length - 1;
        }
        this.setParticleTexture(this.frames[idx]);

        // Die when out of matching fluid - grace so the surface ring always animates.
        if (this.particleAge > 3) {
            BlockPos pos = new BlockPos(this.posX, this.posY - 0.2, this.posZ);
            Material material = this.world.getBlockState(pos).getMaterial();
            if (this.isLava ? material != Material.LAVA : material != Material.WATER) {
                this.setExpired();
            }
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return this.isLava ? 0xF000F0 : super.getBrightnessForRender(partialTick);
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partial,
                               float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        float cx = (float) (this.prevPosX + (this.posX - this.prevPosX) * partial - interpPosX);
        float cy = (float) (this.prevPosY + (this.posY - this.prevPosY) * partial - interpPosY);
        float cz = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partial - interpPosZ);

        float ageDelta = partial + this.particleAge - 1;
        float progress = (this.particleAge > 0) ? ageDelta / (float) this.particleMaxAge : 0.0f;
        float scale = this.width * (0.8f + 0.2f * progress) * 0.7f;

        TextureAtlasSprite sprite = this.particleTexture;
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        int light = this.getBrightnessForRender(partial);
        int sky = light >> 16 & 0xFFFF;
        int block = light & 0xFFFF;

        float r = this.particleRed, g = this.particleGreen, b = this.particleBlue, a = this.particleAlpha;

        // Flat XZ quad, same UV winding as WaterRippleParticle
        buffer.pos(cx - scale, cy, cz - scale).tex(u1, v1).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx - scale, cy, cz + scale).tex(u1, v0).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx + scale, cy, cz + scale).tex(u0, v0).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx + scale, cy, cz - scale).tex(u0, v1).color(r, g, b, a).lightmap(sky, block).endVertex();
    }
}
