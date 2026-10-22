package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.WaterRippleParticle;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Rain ripples on water. Port of 1.20.1 {@code InjectLevelRenderer.modifyParticleEffect}, which
 * swaps the rain splash for {@code WATER_RIPPLE} when the splash lands on water.
 *
 * <p>1.12.2 spawns rain splashes in {@code EntityRenderer.addRainParticles}. This class runs the
 * same column sampling each client tick (count, radius, height window, biome and temperature
 * gates) and puts a ripple on every sampled column whose top block is water.
 */
@SideOnly(Side.CLIENT)
public final class RainRippleSpawner {

    private static final Random RANDOM = new Random();

    private RainRippleSpawner() {
    }

    /** Called once per client tick (END phase) with a non-null client world. */
    public static void tick(World world) {
        if (!ParticularConfig.rainRipples) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null || mc.isGamePaused()) {
            return;
        }
        TextureAtlasSprite[] ripple = ParticleSprites.waterRipple();
        if (ripple == null || ripple.length == 0) {
            return;
        }

        float strength = world.getRainStrength(1.0F);
        if (!mc.gameSettings.fancyGraphics) {
            strength /= 2.0F;
        }
        if (strength == 0.0F) {
            return;
        }
        int count = (int) (100.0F * strength * strength);
        if (mc.gameSettings.particleSetting == 1) {
            count >>= 1;
        } else if (mc.gameSettings.particleSetting == 2) {
            return;
        }

        BlockPos center = new BlockPos(viewer);
        for (int i = 0; i < count; i++) {
            BlockPos top = world.getPrecipitationHeight(center.add(
                    RANDOM.nextInt(10) - RANDOM.nextInt(10), 0, RANDOM.nextInt(10) - RANDOM.nextInt(10)));
            if (top.getY() > center.getY() + 10 || top.getY() < center.getY() - 10) {
                continue;
            }
            Biome biome = world.getBiome(top);
            if (!biome.canRain() || biome.getTemperature(top) < 0.15F) {
                continue;
            }
            BlockPos below = top.down();
            if (world.getBlockState(below).getMaterial() != Material.WATER) {
                continue;
            }
            // Same surface height the drip ripple uses (CuboidParticle).
            // DROPPED: the vanilla WATER_DROP splash on water is not removed, 1.12.2 has no hook
            // between the column pick and spawnParticle short of a coremod.
            AmbientSpawner.add(new WaterRippleParticle(world,
                    below.getX() + RANDOM.nextDouble(), below.getY() + 0.9, below.getZ() + RANDOM.nextDouble(), ripple));
        }
    }
}
