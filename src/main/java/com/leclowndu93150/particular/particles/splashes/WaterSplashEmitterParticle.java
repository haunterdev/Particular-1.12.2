package com.leclowndu93150.particular.particles.splashes;

import com.leclowndu93150.particular.client.ParticleSprites;
import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.CuboidParticle;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Invisible orchestrator particle that spawns all splash sub-particles
 * (crown, foam, ring, droplets) at creation and a second burst at age 8.
 *
 * <p>1.20 equivalent: {@code NoRenderParticle}. 1.12.2 has no such base -
 * override {@code renderParticle} to do nothing and return {@code getFXLayer()==1}.
 *
 * <p>Port of 1.20.1 {@code WaterSplashEmitterParticle}.
 */
@SideOnly(Side.CLIENT)
public class WaterSplashEmitterParticle extends Particle {

    private static final Logger LOGGER = LogManager.getLogger("Particular|WaterSplashEmitter");

    private final float speed;
    private final float width;
    private final float height;
    private final boolean isLava;
    private int spawnCount;

    public WaterSplashEmitterParticle(World world, double x, double y, double z,
                                      float width, float speed, boolean lava) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        speed = Math.min(2.0f, speed);
        this.particleGravity = 0.0f;
        this.particleMaxAge = 24;
        this.speed = speed;
        this.width = width;
        // Crown wall height. Original used width/3; widened the width term so a big
        // mob (iron golem ~1.4, horse ~1.4) throws a clearly taller crown than a
        // small one (cow 0.9, player 0.6) - the size difference reads in 1.12.2,
        // which has no truly huge LAND mobs to enter water.
        this.height = speed / 2.0f + width * 0.6f;
        this.isLava = lava;

        // Initial burst
        emitBurst(width, this.height, 0.1875f + speed * 1.0f / 8.0f + width / 6.0f, 0.15f, speed > 0.5);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        // (1.20 emitted a doubled-height second crown at age 8 - removed: in 1.12.2 it
        // floated as an "extra crown in the air". Single splash burst reads cleaner.)

        // Die when out of matching fluid (short grace).
        if (this.particleAge > 4) {
            BlockPos pos = new BlockPos(this.posX, this.posY - 0.2, this.posZ);
            Material material = this.world.getBlockState(pos).getMaterial();
            if (this.isLava ? material != Material.LAVA : material != Material.WATER) {
                this.setExpired();
            }
        }
    }

    private void emitBurst(float burstWidth, float burstHeight, float dropletSpeed, float spread, boolean doDroplets) {
        // Crown
        WaterSplashParticle crown = new WaterSplashParticle(
                this.world, this.posX, this.posY, this.posZ,
                burstWidth, burstHeight,
                ParticleSprites.waterSplash(), this.isLava);
        Minecraft.getMinecraft().effectRenderer.addEffect(crown);
        this.spawnCount++;

        // Foam
        WaterSplashFoamParticle foam = new WaterSplashFoamParticle(
                this.world, this.posX, this.posY, this.posZ,
                burstWidth, burstHeight,
                ParticleSprites.waterSplashFoam(), this.isLava);
        Minecraft.getMinecraft().effectRenderer.addEffect(foam);
        this.spawnCount++;

        // Ring
        WaterSplashRingParticle ring = new WaterSplashRingParticle(
                this.world, this.posX, this.posY, this.posZ,
                burstWidth,
                ParticleSprites.waterSplashRing(), this.isLava);
        Minecraft.getMinecraft().effectRenderer.addEffect(ring);
        this.spawnCount++;

        // Droplets
        if (doDroplets) {
            splashDroplets(burstWidth, dropletSpeed, spread);
        }
    }

    /**
     * Spawn droplet particles radiating outward from the splash center.
     * Uses cuboid particles when {@code ParticularConfig.cuboidSplashDroplets}
     * is true, otherwise uses vanilla drip particles. Velocity distribution
     * follows the 1.20.1 original: gaussian for X/Z, gaussian-constrained for Y.
     */
    private void splashDroplets(float width, float speed, float spread) {
        boolean useCuboid = ParticularConfig.cuboidSplashDroplets;
        int count = (int) (width * 10.0f); // fewer droplets (was ×20)

        for (int i = 0; i < count; i++) {
            // Lower velocities so droplets land near the splash, not blocks away.
            double xVel = this.rand.nextGaussian() * (double) spread * 0.5;
            double yVel = (double) speed * (1.0 + this.rand.nextGaussian() * 0.25) * 0.6;
            double zVel = this.rand.nextGaussian() * (double) spread * 0.5;

            // Tighter spawn radius around the splash centre (was ±~width blocks).
            double px = this.posX + xVel / (double) spread * (double) width * 0.35;
            double py = this.posY + 0.0625;
            double pz = this.posZ + zVel / (double) spread * (double) width * 0.35;

            if (useCuboid) {
                if (this.isLava) {
                    CuboidParticle.spawnLava(this.world, px, py, pz, xVel, yVel, zVel);
                } else {
                    CuboidParticle.spawnWhiteSplash(this.world, px, py, pz, xVel, yVel, zVel);
                }
            } else {
                this.world.spawnParticle(
                        this.isLava ? EnumParticleTypes.DRIP_LAVA : EnumParticleTypes.WATER_DROP,
                        px, py, pz, xVel, yVel, zVel, new int[0]);
            }
            this.spawnCount++;
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partial,
                               float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        // Invisible - no-op
    }

    /**
     * Spawn a water/lava splash at a location. Called from entity-trigger
     * hooks (entity hitting water/lava at speed).
     *
     * @param world the client world
     * @param x     X position
     * @param y     Y position (water surface)
     * @param z     Z position
     * @param width crown width (typically {@code min(1.0, speed/2)})
     * @param speed fall speed (m/s), clamped to max 2.0
     * @param lava  {@code true} for lava, {@code false} for water
     */
    public static void spawn(World world, double x, double y, double z,
                              float width, float speed, boolean lava) {
        Minecraft.getMinecraft().effectRenderer.addEffect(
                new WaterSplashEmitterParticle(world, x, y, z, width, speed, lava));
    }
}
