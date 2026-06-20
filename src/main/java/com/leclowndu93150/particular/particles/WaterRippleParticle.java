package com.leclowndu93150.particular.particles;

import com.leclowndu93150.particular.client.ParticleSprites;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Flat horizontal ripple on a water surface. Port of 1.20.1 {@code WaterRippleParticle}.
 *
 * <p>Renders as a single quad lying in the XZ plane (normal up), NOT billboarded.
 * 7-frame animation over a 7-tick life. Custom render ports {@code m_5744_}.
 */
@SideOnly(Side.CLIENT)
public class WaterRippleParticle extends Particle {

    private final TextureAtlasSprite[] frames;

    public WaterRippleParticle(World world, double x, double y, double z, TextureAtlasSprite[] frames) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.frames = frames;
        this.particleMaxAge = 7;
        this.particleAlpha = 0.55f;
        // Small surface ring (was 2.5 - droplet rings looked massive)
        this.particleScale = 1.0f;
        this.motionX = this.motionY = this.motionZ = 0.0;
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
        int idx = this.particleAge;
        if (idx >= this.frames.length) {
            idx = this.frames.length - 1;
        }
        this.setParticleTexture(this.frames[idx]);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partial,
                               float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        float cx = (float) (this.prevPosX + (this.posX - this.prevPosX) * partial - interpPosX);
        float cy = (float) (this.prevPosY + (this.posY - this.prevPosY) * partial - interpPosY);
        float cz = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partial - interpPosZ);

        float s = this.particleScale;
        TextureAtlasSprite sprite = this.particleTexture;
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        int light = this.getBrightnessForRender(partial);
        int sky = light >> 16 & 0xFFFF;
        int block = light & 0xFFFF;
        float r = this.particleRed, g = this.particleGreen, b = this.particleBlue, a = this.particleAlpha;

        // Flat quad in the XZ plane (corners: -1,-1 / -1,+1 / +1,+1 / +1,-1 scaled)
        buffer.pos(cx - s, cy, cz - s).tex(u1, v1).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx - s, cy, cz + s).tex(u1, v0).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx + s, cy, cz + s).tex(u0, v0).color(r, g, b, a).lightmap(sky, block).endVertex();
        buffer.pos(cx + s, cy, cz - s).tex(u0, v1).color(r, g, b, a).lightmap(sky, block).endVertex();
    }

    /** Spawn a ripple at a water surface point. */
    public static void spawn(World world, double x, double y, double z) {
        Minecraft.getMinecraft().effectRenderer.addEffect(
                new WaterRippleParticle(world, x, y, z, ParticleSprites.waterRipple()));
    }
}
