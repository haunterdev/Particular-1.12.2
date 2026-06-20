package com.leclowndu93150.particular.particles;

import com.leclowndu93150.particular.client.ParticleSprites;
import com.leclowndu93150.particular.config.ParticularConfig;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1-voxel cuboid particle - a tiny axis-aligned cube rendered as 6 world-aligned
 * quads (NOT billboarded). Port of 1.20.1 {@code CuboidParticle}.
 *
 * <p>Used as the droplet primitive for splash droplets and (optionally) waterfall
 * spray. 1.12.2 has no particle-type registry, so the original
 * {@code CuboidParticleType.Options} is replaced by a {@link Mode} enum + color
 * passed straight to the constructor, and spawn helpers below.
 *
 * <p>Render ports {@code m_5744_}: 1.20 {@code VertexConsumer}+{@code Camera} →
 * 1.12.2 {@link #renderParticle(BufferBuilder, Entity, float, float, float, float, float, float)}
 * with {@code interpPosX/Y/Z} as the camera offset.
 */
@SideOnly(Side.CLIENT)
public class CuboidParticle extends Particle {

    public enum Mode {
        WATERFALL_SPRAY,
        SPLASH_WATER,
        LAVA
    }

    private static final float HALF_SIDE = 0.03125f;

    private final Mode mode;
    private final boolean isLava;
    private final float friction = 0.98f;

    public CuboidParticle(World world, double x, double y, double z,
                          double xd, double yd, double zd,
                          TextureAtlasSprite sprite, Mode mode, int color) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleTexture(sprite);
        this.mode = mode;
        this.isLava = mode == Mode.LAVA;
        this.particleGravity = 0.06f;
        this.particleScale = HALF_SIDE; // unused by our custom render; kept for parity
        this.setSize(0.01f, 0.01f);

        if (mode == Mode.WATERFALL_SPRAY) {
            setWaterfallSprayMotion(xd, zd);
            this.particleMaxAge = (int) (8.0 / (this.rand.nextFloat() * 0.8 + 0.2));
        } else {
            this.motionX = xd;
            this.motionY = yd;
            this.motionZ = zd;
            this.particleMaxAge = (int) (64.0 / (this.rand.nextFloat() * 0.8 + 0.2));
        }

        this.particleRed = (float) (color >> 16 & 0xFF) / 255.0f;
        this.particleGreen = (float) (color >> 8 & 0xFF) / 255.0f;
        this.particleBlue = (float) (color & 0xFF) / 255.0f;
    }

    private void setWaterfallSprayMotion(double xFlow, double zFlow) {
        double xVel = (this.rand.nextFloat() * 2.0f - 1.0f) * 0.4f;
        double yVel = (this.rand.nextFloat() * 2.0f - 1.0f) * 0.4f;
        double zVel = (this.rand.nextFloat() * 2.0f - 1.0f) * 0.4f;
        double speed = (this.rand.nextFloat() + this.rand.nextFloat() + 1.0f) * 0.15f;
        double length = Math.sqrt(xVel * xVel + yVel * yVel + zVel * zVel);
        if (length == 0.0) {
            length = 1.0;
        }
        this.motionX = xVel / length * speed * 0.4 * 0.3 + xFlow;
        this.motionY = (this.rand.nextFloat() * 0.2f + 0.1f) * 0.75f;
        this.motionZ = zVel / length * speed * 0.4 * 0.3 + zFlow;
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

        this.motionY -= this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= this.friction;
        this.motionY *= this.friction;
        this.motionZ *= this.friction;

        if (this.mode == Mode.WATERFALL_SPRAY) {
            if (this.onGround) {
                if (this.rand.nextFloat() < 0.5f) {
                    this.setExpired();
                }
                this.motionX *= 0.7;
                this.motionZ *= 0.7;
            }
        } else if (this.onGround) {
            // splash/lava droplet hitting a surface → vanilla splash/lava puff
            this.setExpired();
            this.world.spawnParticle(
                    this.isLava ? EnumParticleTypes.LAVA : EnumParticleTypes.WATER_SPLASH,
                    this.posX, this.posY, this.posZ, 0.0, 0.0, 0.0);
        }

        // Die when submerged in matching fluid; spawn a ripple where it lands on water.
        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        Material material = this.world.getBlockState(pos).getMaterial();
        boolean inFluid = this.isLava ? material == Material.LAVA : material == Material.WATER;
        if (inFluid && this.posY < pos.getY() + 1.0) {
            if (!this.isLava && ParticularConfig.waterDripRipples) {
                WaterRippleParticle.spawn(this.world, this.posX, pos.getY() + 0.9, this.posZ);
            }
            this.setExpired();
        }
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

        TextureAtlasSprite sprite = this.particleTexture;
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        int light = this.getBrightnessForRender(partial);
        int sky = light >> 16 & 0xFFFF;
        int block = light & 0xFFFF;
        float h = HALF_SIDE;

        // top
        quad(buffer, cx-h,cy+h,cz-h, cx-h,cy+h,cz+h, cx+h,cy+h,cz+h, cx+h,cy+h,cz-h, u0,u1,v0,v1, sky,block);
        // bottom
        quad(buffer, cx-h,cy-h,cz+h, cx-h,cy-h,cz-h, cx+h,cy-h,cz-h, cx+h,cy-h,cz+h, u0,u1,v0,v1, sky,block);
        // +z
        quad(buffer, cx+h,cy-h,cz+h, cx+h,cy+h,cz+h, cx-h,cy+h,cz+h, cx-h,cy-h,cz+h, u0,u1,v0,v1, sky,block);
        // -z
        quad(buffer, cx-h,cy-h,cz-h, cx-h,cy+h,cz-h, cx+h,cy+h,cz-h, cx+h,cy-h,cz-h, u0,u1,v0,v1, sky,block);
        // +x
        quad(buffer, cx+h,cy-h,cz-h, cx+h,cy+h,cz-h, cx+h,cy+h,cz+h, cx+h,cy-h,cz+h, u0,u1,v0,v1, sky,block);
        // -x
        quad(buffer, cx-h,cy-h,cz+h, cx-h,cy+h,cz+h, cx-h,cy+h,cz-h, cx-h,cy-h,cz-h, u0,u1,v0,v1, sky,block);
    }

    private void quad(BufferBuilder b,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      float u0, float u1, float v0, float v1, int sky, int block) {
        b.pos(ax, ay, az).tex(u0, v1).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(sky, block).endVertex();
        b.pos(bx, by, bz).tex(u0, v0).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(sky, block).endVertex();
        b.pos(cx, cy, cz).tex(u1, v0).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(sky, block).endVertex();
        b.pos(dx, dy, dz).tex(u1, v1).color(particleRed, particleGreen, particleBlue, particleAlpha).lightmap(sky, block).endVertex();
    }

    /* ---------------- spawn helpers (replace CuboidParticleType.Options + registry) ---------------- */

    private static int waterColor(World world, double x, double y, double z) {
        if (ParticularConfig.waterCuboidBiomeTint) {
            return BiomeColorHelper.getWaterColorAtPos(world, new BlockPos(x, y, z));
        }
        return ParticularConfig.waterCuboidColor;
    }

    public static void spawnWaterfallSpray(World world, double x, double y, double z, double xFlow, double zFlow) {
        add(new CuboidParticle(world, x, y, z, xFlow, 0.0, zFlow,
                ParticleSprites.cuboid(), Mode.WATERFALL_SPRAY, waterColor(world, x, y, z)));
    }

    public static void spawnWhiteSplash(World world, double x, double y, double z, double xd, double yd, double zd) {
        add(new CuboidParticle(world, x, y, z, xd, yd, zd,
                ParticleSprites.cuboid(), Mode.SPLASH_WATER, 0xFFFFFF));
    }

    public static void spawnLava(World world, double x, double y, double z, double xd, double yd, double zd) {
        add(new CuboidParticle(world, x, y, z, xd, yd, zd,
                ParticleSprites.cuboid(), Mode.LAVA, ParticularConfig.lavaCuboidColor));
    }

    private static void add(Particle p) {
        Minecraft.getMinecraft().effectRenderer.addEffect(p);
    }
}
