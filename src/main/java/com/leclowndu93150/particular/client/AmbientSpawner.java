package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.CaveDustParticle;
import com.leclowndu93150.particular.particles.FireflyParticle;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Ambient particle spawner - fireflies and cave dust.
 *
 * <p>Replaces the 1.20.1 {@code InjectBlock.animateTick} mixin. 1.12.2 has no
 * per-block client display-tick hook we can subscribe to, so we replicate
 * vanilla's {@code WorldClient.doVoidFogParticles} sampling: each client tick
 * we sample {@code 667 × 2} random block positions around the player (radii 16
 * and 32) and apply the original per-block spawn math. Pure client-side.
 */
@SideOnly(Side.CLIENT)
public final class AmbientSpawner {

    private static final Random RANDOM = new Random();

    /** Per-day firefly frequency factor, re-rolled at {@code fireflyStartTime}. */
    private static float fireflyFrequency = 1.0f;
    private static long lastFrequencyRollDay = Long.MIN_VALUE;

    private AmbientSpawner() {
    }

    /** Called once per client tick (END phase) with a non-null client world. */
    public static void tick(World world) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (!ParticularConfig.fireflies && !ParticularConfig.caveDust) {
            return;
        }

        updateFireflyFrequency(world);

        int px = MathHelper.floor(mc.player.posX);
        int py = MathHelper.floor(mc.player.posY);
        int pz = MathHelper.floor(mc.player.posZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int i = 0; i < 667; i++) {
            sample(world, pos, px, py, pz, 16);
            sample(world, pos, px, py, pz, 32);
        }
    }

    /** Re-roll the daily firefly frequency once when day-time hits the start time. */
    private static void updateFireflyFrequency(World world) {
        long timeOfDay = world.getWorldTime() % 24000L;
        long day = world.getWorldTime() / 24000L;
        if (timeOfDay == (ParticularConfig.fireflyStartTime % 24000L) && day != lastFrequencyRollDay) {
            double[] daily = ParticularConfig.fireflyDailyRandom;
            if (daily != null && daily.length > 0) {
                fireflyFrequency = (float) daily[RANDOM.nextInt(daily.length)];
            }
            lastFrequencyRollDay = day;
        }
    }

    private static void sample(World world, BlockPos.MutableBlockPos pos, int px, int py, int pz, int radius) {
        int bx = px + RANDOM.nextInt(radius) - RANDOM.nextInt(radius);
        int by = py + RANDOM.nextInt(radius) - RANDOM.nextInt(radius);
        int bz = pz + RANDOM.nextInt(radius) - RANDOM.nextInt(radius);
        pos.setPos(bx, by, bz);

        if (!world.isBlockLoaded(pos)) {
            return;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        if (ParticularConfig.fireflies && tryFirefly(world, pos, block)) {
            return;
        }
        if (ParticularConfig.caveDust) {
            tryCaveDust(world, pos, block);
        }
    }

    /**
     * Port of {@code InjectBlock.spawnParticles} firefly branch + {@code CommonClass.spawnFirefly}.
     * @return true if this block was a firefly source (so cave dust is skipped)
     */
    private static boolean tryFirefly(World world, BlockPos pos, Block block) {
        double val = RANDOM.nextDouble();
        boolean source =
                (block == Blocks.GRASS && val < ParticularConfig.fireflyGrassFrequency)
             || (block == Blocks.TALLGRASS && val < ParticularConfig.fireflyTallGrassFrequency)
             || (block instanceof BlockFlower && val < ParticularConfig.fireflyFlowersFrequency)
             || (block instanceof BlockDoublePlant && val < ParticularConfig.fireflyTallFlowersFrequency);
        if (!source) {
            return false;
        }
        spawnFirefly(world, pos);
        return true;
    }

    private static void spawnFirefly(World world, BlockPos pos) {
        if (RANDOM.nextDouble() > fireflyFrequency) {
            return;
        }
        Biome biome = world.getBiome(pos);
        float downfall = biome.getRainfall();
        if (world.isRaining() && !ParticularConfig.fireflyCanSpawnInRain) {
            return;
        }
        int rainGate = 30 - (int) (10.0f * downfall);
        if (rainGate < 1 || RANDOM.nextInt(rainGate) != 0) {
            return;
        }
        long time = world.getWorldTime() % 24000L;
        float temp = biome.getDefaultTemperature();
        if (time >= ParticularConfig.fireflyStartTime && time <= ParticularConfig.fireflyEndTime
                && temp >= ParticularConfig.fireflyMinTemp && temp <= ParticularConfig.fireflyMaxTemp) {
            add(new FireflyParticle(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ParticleSprites.firefly()));
        }
    }

    /** Port of {@code InjectBlock.spawnParticles} cave-dust branch. */
    private static void tryCaveDust(World world, BlockPos pos, Block block) {
        if (block != Blocks.AIR) {
            return;
        }
        if (RANDOM.nextInt(Math.max(1, ParticularConfig.caveDustSpawnChance)) != 0) {
            return;
        }
        int seaLevel = world.getSeaLevel();
        if (pos.getY() >= seaLevel || !isValidBiome(world.getBiome(pos))) {
            return;
        }
        float lightChance = 1.0f - (float) Math.min(8, world.getLightFor(EnumSkyBlock.SKY, pos)) / 8.0f;
        // Original (1.20.1) normalised depth over 96 blocks inside a 127-deep below-sea span
        // (sea 63 → world bottom -64), so dust saturates well before the cave floor and the
        // whole deep layer renders at full rate. 1.12.2's below-sea span is only `seaLevel`
        // blocks (world bottom 0), so the raw /96 tops out at ~0.66 at bedrock and never
        // saturates - dust ends up far too rare. Scale the divisor by the span ratio so dust
        // saturates at the SAME fraction of the way down, matching 1.20.1 amounts floor-to-floor.
        float depthSpan = seaLevel * 96.0f / 127.0f; // ≈ 48 for sea level 63
        float depthChance = Math.min(1.0f, (float) (seaLevel - pos.getY()) / depthSpan);
        if (RANDOM.nextFloat() < lightChance * depthChance) {
            double x = pos.getX() + RANDOM.nextDouble();
            double y = pos.getY() + RANDOM.nextDouble();
            double z = pos.getZ() + RANDOM.nextDouble();
            boolean square = ParticularConfig.caveDustSquareParticle;
            TextureAtlasSprite dustSprite = square
                    ? ParticleSprites.caveDust() : ParticleSprites.caveDustRound();
            add(new CaveDustParticle(world, x, y, z, 1.0f, dustSprite, square));
        }
    }

    private static boolean isValidBiome(Biome biome) {
        ResourceLocation id = ForgeRegistries.BIOMES.getKey(biome);
        if (id == null) {
            return true;
        }
        String key = id.toString();
        String[] excluded = ParticularConfig.caveDustExcludeBiomes;
        if (excluded != null) {
            for (String e : excluded) {
                if (key.equals(e)) {
                    return false;
                }
            }
        }
        return true;
    }

    static void add(net.minecraft.client.particle.Particle particle) {
        Minecraft.getMinecraft().effectRenderer.addEffect(particle);
    }
}
