package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.CuboidParticle;
import com.leclowndu93150.particular.particles.splashes.WaterSplashEmitterParticle;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;
import java.util.WeakHashMap;

/**
 * Entity water/lava-entry splash trigger. Port of the 1.20.1 {@code InjectEntity}
 * mixin (which hooked {@code Entity.tick} + {@code doWaterSplashEffect}).
 *
 * <p>1.12.2 has no clean "entered water" event, so each client tick we scan
 * {@code world.loadedEntityList} and track per-entity velocity history, fall
 * accumulation, and prior in-water/in-lava state in a {@link WeakHashMap}.
 * On a fluid-entry transition (was-out → now-in) with the configured fall/speed
 * gate met, we spawn a {@link WaterSplashEmitterParticle}. Pure client-side.
 */
@SideOnly(Side.CLIENT)
public final class SplashTrigger {

    private static final double LARGE_SPLASH_MIN_FALL = 1.0;
    private static final double LARGE_SPLASH_MIN_DOWN_SPEED = 0.35;
    private static final int SURFACE_SCAN = 5;
    private static final int SPLASH_COOLDOWN = 10;

    private static final Random RANDOM = new Random();
    private static final WeakHashMap<Entity, State> STATE = new WeakHashMap<Entity, State>();

    private SplashTrigger() {
    }

    private static final class State {
        final double[] vel = new double[4];
        int velIdx = 0;
        double accumFall = 0.0;
        boolean wasInWater = false;
        boolean wasInLava = false;
        int cooldown = 0;
    }

    public static void tick(World world) {
        if (!ParticularConfig.waterSplash && !ParticularConfig.lavaSplash) {
            return;
        }
        // Iterate a snapshot-safe copy is unnecessary on client tick; list is stable here.
        for (int i = 0; i < world.loadedEntityList.size(); i++) {
            Entity e = world.loadedEntityList.get(i);
            if (e == null || e instanceof EntityArrow) {
                continue;
            }
            update(world, e);
        }
    }

    private static void update(World world, Entity e) {
        State s = STATE.get(e);
        if (s == null) {
            s = new State();
            STATE.put(e, s);
        }

        s.vel[s.velIdx % 4] = Math.abs(e.motionY);
        s.velIdx = (s.velIdx + 1) % 4;
        if (e.motionY < 0.0) {
            s.accumFall += Math.abs(e.motionY);
        } else if (e.motionY > 0.0) {
            s.accumFall = 0.0;
        }

        if (s.cooldown > 0) {
            s.cooldown--;
        }

        boolean inWater = e.isInWater();
        boolean inLava = e.isInLava();

        if (ParticularConfig.lavaSplash && inLava && !s.wasInLava) {
            doSplash(world, e, s, true);
        }
        if (ParticularConfig.waterSplash && inWater && !s.wasInWater) {
            doSplash(world, e, s, false);
        }

        s.wasInWater = inWater;
        s.wasInLava = inLava;
    }

    private static void doSplash(World world, Entity e, State s, boolean lava) {
        if (s.cooldown > 0) {
            return; // avoid rapid re-splash from an entity bobbing at the surface
        }
        Material fluid = lava ? Material.LAVA : Material.WATER;
        int feetX = MathHelper.floor(e.posX);
        int feetZ = MathHelper.floor(e.posZ);
        int feetY = MathHelper.floor(e.posY);

        // Find the TOPMOST matching-fluid block in the entity's column (its block above is
        // NOT the same fluid) - that's the real surface. Scan from above the entity downward,
        // so deeply-submerged entities still resolve the surface instead of floating at depth.
        int topFluidY = Integer.MIN_VALUE;
        for (int dy = SURFACE_SCAN; dy >= -SURFACE_SCAN; dy--) {
            BlockPos p = new BlockPos(feetX, feetY + dy, feetZ);
            if (world.getBlockState(p).getMaterial() == fluid
                    && world.getBlockState(p.up()).getMaterial() != fluid) {
                topFluidY = feetY + dy;
                break;
            }
        }
        if (topFluidY == Integer.MIN_VALUE) {
            return; // entity isn't actually adjacent to this fluid's surface
        }
        // Sit the splash right on the fluid surface: lava sources fill the block; water
        // renders at ~0.9 of the block.
        double surfaceY = topFluidY + (lava ? 1.0 : 0.9);

        double velocityValue = Math.max(Math.max(s.vel[0], s.vel[1]), Math.max(s.vel[2], s.vel[3]));
        double fallDistance = e.fallDistance > 0.0 ? e.fallDistance : s.accumFall;
        double downwardSpeed = -e.motionY;
        double minFall = Math.max(ParticularConfig.waterSplashMinFallDistance, LARGE_SPLASH_MIN_FALL);

        if (!lava && (fallDistance < minFall || downwardSpeed < LARGE_SPLASH_MIN_DOWN_SPEED)) {
            if (ParticularConfig.waterSplashSoftEntryParticles) {
                softEntry(world, e, surfaceY, velocityValue);
            }
            s.accumFall = 0.0;
            s.cooldown = SPLASH_COOLDOWN;
            return;
        }
        if (lava && fallDistance < ParticularConfig.waterSplashMinFallDistance) {
            s.accumFall = 0.0;
            return;
        }

        s.accumFall = 0.0;
        s.cooldown = SPLASH_COOLDOWN;
        WaterSplashEmitterParticle.spawn(world, e.posX, surfaceY, e.posZ, e.width, (float) velocityValue, lava);
    }

    private static void softEntry(World world, Entity e, double surfaceY, double speed) {
        float width = e.width;
        double radius = Math.max(0.25, width * 0.5);
        int count = Math.max(4, (int) (12.0f * width));
        for (int i = 0; i < count; i++) {
            double xOff = RANDOM.nextGaussian() * radius;
            double zOff = RANDOM.nextGaussian() * radius;
            double x = e.posX + xOff;
            double y = surfaceY + RANDOM.nextDouble() * 0.08;
            double z = e.posZ + zOff;
            if (RANDOM.nextBoolean()) {
                world.spawnParticle(EnumParticleTypes.WATER_BUBBLE,
                        x, y - 0.08, z, xOff * 0.04, 0.02 + RANDOM.nextDouble() * 0.03, zOff * 0.04);
            } else {
                double dropletSpeed = 0.02 + Math.min(speed, 0.25) * 0.08;
                if (ParticularConfig.cuboidSplashDroplets) {
                    CuboidParticle.spawnWhiteSplash(world, x, y + 0.02, z, xOff * 0.02, dropletSpeed, zOff * 0.02);
                } else {
                    world.spawnParticle(EnumParticleTypes.WATER_SPLASH,
                            x, y + 0.02, z, xOff * 0.02, dropletSpeed, zOff * 0.02);
                }
            }
        }
    }
}
