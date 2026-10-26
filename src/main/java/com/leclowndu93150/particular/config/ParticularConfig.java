package com.leclowndu93150.particular.config;

import com.leclowndu93150.particular.ParticularMod;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = "particular", type = Config.Type.INSTANCE, name = "particular")
public class ParticularConfig {

    /* ---- Enabled Effects ---- */

    @Config.Comment("Enable water splash particles")
    public static boolean waterSplash = true;

    @Config.Comment("Enable cascade particles")
    public static boolean cascades = true;

    @Config.Comment("Enable waterfall spray particles")
    public static boolean waterfallSpray = true;

    @Config.Comment("Enable firefly particles")
    public static boolean fireflies = true;

    @Config.Comment("Enable cave dust particles")
    public static boolean caveDust = true;

    @Config.Comment("Enable falling leaf particles dropped from leaf blocks")
    public static boolean fallingLeaves = true;

    @Config.Comment("Enable chest bubbles")
    public static boolean chestBubbles = true;

    @Config.Comment("Enable soul sand bubbles (gated stub - unused in 1.12.2)")
    public static boolean soulSandBubbles = true;

    @Config.Comment("Enable barrel bubbles (unused in 1.12.2)")
    public static boolean barrelBubbles = true;

    @Config.Comment("Enable popping bubbles")
    public static boolean poppingBubbles = true;

    @Config.Comment("Enable rain ripples")
    public static boolean rainRipples = true;

    @Config.Comment("Enable water drip ripples")
    public static boolean waterDripRipples = true;

    @Config.Comment("Enable cake eating particles")
    public static boolean cakeEatingParticles = true;

    @Config.Comment("Enable emissive lava drips")
    public static boolean emissiveLavaDrips = true;

    @Config.Comment("Enable lava splash particles when entities hit lava")
    public static boolean lavaSplash = true;

    @Config.Comment("Enable coastal waves: foam crests that roll toward beaches and break on the shore")
    public static boolean coastalWaves = true;

    /* ---- Firefly Settings ---- */

    @Config.RangeInt(min = 0, max = 23999)
    @Config.Comment("Time when fireflies start spawning")
    public static int fireflyStartTime = 12000;

    @Config.RangeInt(min = 0, max = 23999)
    @Config.Comment("Time when fireflies stop spawning")
    public static int fireflyEndTime = 23000;

    @Config.RangeDouble(min = 0.0, max = 2.0)
    @Config.Comment("Minimum temperature for fireflies to spawn")
    public static double fireflyMinTemp = 0.5;

    @Config.RangeDouble(min = 0.0, max = 2.0)
    @Config.Comment("Maximum temperature for fireflies to spawn")
    public static double fireflyMaxTemp = 0.99;

    @Config.Comment("Whether fireflies can spawn in rain")
    public static boolean fireflyCanSpawnInRain = false;

    @Config.Comment("Daily random factors for firefly spawning")
    public static double[] fireflyDailyRandom = {0.0, 0.0, 0.0, 0.33, 0.66, 1.0};

    @Config.RangeDouble(min = 0.0, max = 1.0)
    @Config.Comment("Frequency modifier for grass")
    public static double fireflyGrassFrequency = 0.16666666666666666;

    @Config.RangeDouble(min = 0.0, max = 1.0)
    @Config.Comment("Frequency modifier for tall grass")
    public static double fireflyTallGrassFrequency = 0.08333333333333333;

    @Config.RangeDouble(min = 0.0, max = 1.0)
    @Config.Comment("Frequency modifier for flowers")
    public static double fireflyFlowersFrequency = 1.0;

    @Config.RangeDouble(min = 0.0, max = 1.0)
    @Config.Comment("Frequency modifier for tall flowers")
    public static double fireflyTallFlowersFrequency = 0.5;

    @Config.Comment({"Extra blocks that also spawn fireflies (in addition to grass/flowers).",
            "Use block registry names, e.g. minecraft:fern, biomesoplenty:flower_0.",
            "These spawn at the flower frequency."})
    public static String[] fireflyExtraBlocks = {};

    @Config.Comment({"Biome whitelist for fireflies.",
            "When empty, fireflies may spawn in any biome (subject to the temperature range).",
            "When non-empty, fireflies ONLY spawn in these biomes (temperature range still applies).",
            "Uses biome registry names, e.g. minecraft:forest, minecraft:plains."})
    public static String[] fireflyBiomeWhitelist = {};

    /* ---- Falling Leaf Settings ---- */

    @Config.RangeInt(min = 1, max = Integer.MAX_VALUE)
    @Config.Comment("Chance of spawning falling leaves (higher = less frequent)")
    public static int fallingLeavesSpawnChance = 60;

    @Config.Comment("Whether falling leaves create ripples when landing on water")
    public static boolean fallingLeavesSpawnRipples = true;

    @Config.Comment("Whether falling leaves lay flat on the ground when they land")
    public static boolean fallingLeavesLayFlatOnGround = true;

    @Config.Comment("Whether ground-laid leaves snap to right angles instead of random rotations")
    public static boolean fallingLeavesLayFlatRightAngles = false;

    /* ---- Compatibility ---- */

    @Config.Comment({"Show a one-time chat notice when another mod that also adds leaf/foliage",
            "particles is detected (e.g. Falling Leaves), warning of possible doubled-up leaves."})
    public static boolean showCompatWarning = true;

    /* ---- Water Splash Settings ---- */

    @Config.RangeDouble(min = 0.0, max = 100.0)
    @Config.Comment("Minimum fall distance (in blocks) required to trigger water splash particles")
    public static double waterSplashMinFallDistance = 0.0;

    @Config.Comment("Whether small splashes should emit water droplet particles")
    public static boolean waterSplashSmallDroplets = false;

    @Config.RangeDouble(min = 0.0, max = 1.0)
    @Config.Comment("Opacity of water and lava splash particles")
    public static double waterSplashOpacity = 1.0;

    @Config.Comment("Whether soft water entries should emit bubbles and tiny falling-water droplets instead of a full splash")
    public static boolean waterSplashSoftEntryParticles = true;

    @Config.Comment("Use 1-voxel cuboid particles for waterfall spray")
    public static boolean cuboidWaterfallSpray = false;

    @Config.Comment("Use 1-voxel cuboid particles for splash droplets")
    public static boolean cuboidSplashDroplets = true;

    @Config.Comment("Tint cuboid waterfall spray using the local biome water color")
    public static boolean waterCuboidBiomeTint = true;

    @Config.RangeInt(min = 0, max = 0xFFFFFF)
    @Config.Comment("Fallback color of cuboid waterfall spray when biome tinting is disabled (RGB hex)")
    public static int waterCuboidColor = 0xFFFFFF;

    @Config.RangeInt(min = 0, max = 0xFFFFFF)
    @Config.Comment("Color of cuboid lava droplets (RGB hex)")
    public static int lavaCuboidColor = 16738335;

    /* ---- Cave Dust Settings ---- */

    @Config.RangeInt(min = 1, max = Integer.MAX_VALUE)
    @Config.Comment({"Chance of spawning cave dust (higher = less frequent)",
            "1.20.1 used 700, but 1.12.2 caves are tighter (less air per sample) and the dark",
            "below-sea band is far shorter, so 700 reads as barren. Lowered to match 1.20.1 feel."})
    public static int caveDustSpawnChance = 300;

    @Config.RangeInt(min = 1, max = Integer.MAX_VALUE)
    @Config.Comment("Base maximum age of cave dust particles")
    public static int caveDustBaseMaxAge = 200;

    @Config.RangeInt(min = 0, max = 0xFFFFFF)
    @Config.Comment("Color of cave dust particles (RGB hex)")
    public static int caveDustColor = 0x808080;

    @Config.RangeInt(min = 0, max = Integer.MAX_VALUE)
    @Config.Comment("Duration of fade effect for cave dust particles")
    public static int caveDustFadeDuration = 20;

    @Config.RangeDouble(min = 0.0, max = 1.0)
    @Config.Comment("Maximum acceleration of cave dust particles")
    public static double caveDustMaxAcceleration = 0.03;

    @Config.RangeInt(min = 1, max = Integer.MAX_VALUE)
    @Config.Comment("Chance of changing acceleration for cave dust particles")
    public static int caveDustAccelChangeChance = 180;

    @Config.Comment({"Biomes where cave dust won't spawn", "Uses biome registry names (e.g. minecraft:lush_caves)"})
    public static String[] caveDustExcludeBiomes = {"minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark"};

    @Config.Comment({"Cave dust particle shape",
            "true = solid grey square (faithful to 1.20.1: vanilla draws a single opaque texel = a square; mip-stable)",
            "false = generic_0 single-pixel sprite (mip-averages to a faded round blur on the block atlas - not recommended)"})
    public static boolean caveDustSquareParticle = true;

    /* ---- Coastal Wave Settings ---- */

    @Config.RangeInt(min = 1, max = 64)
    @Config.Comment("How far (in blocks) a wave looks for the nearest shore from where it starts")
    public static int waveSearchDistance = 14;

    @Config.RangeDouble(min = 0.0, max = 8.0)
    @Config.Comment({"Waves started per spawn round. Fractions carry over, so 0.7 starts 7 waves every 10 rounds.",
            "0 turns spawning off."})
    public static double waveSpawnAmount = 0.7;

    @Config.RangeInt(min = 1, max = 1200)
    @Config.Comment("Ticks between spawn rounds (20 ticks = 1 second)")
    public static int waveSpawnFrequency = 20;

    @Config.RangeInt(min = 8, max = 256)
    @Config.Comment("Furthest distance (in blocks) from you that waves start, capped at the render distance")
    public static int waveSpawnDistance = 64;

    @Config.RangeDouble(min = 0.0, max = 64.0)
    @Config.Comment("Waves only start at least this many blocks from the shore")
    public static double waveMinShoreDistance = 4.0;

    @Config.RangeDouble(min = 1.0, max = 256.0)
    @Config.Comment("Waves only start at most this many blocks from the shore")
    public static double waveMaxShoreDistance = 48.0;

    @Config.RangeDouble(min = 0.0, max = 360.0)
    @Config.Comment("Waves only start inside this view angle in degrees (360 = all around you)")
    public static double waveSpawnFovLimit = 140.0;

    @Config.RangeDouble(min = 0.1, max = 4.0)
    @Config.Comment("Size multiplier for waves")
    public static double waveScale = 1.0;

    @Config.RangeInt(min = 1, max = 512)
    @Config.Comment("Most waves on screen at once")
    public static int waveMaxCount = 96;

    @Config.Comment({"Biomes that get waves. Entries are Forge biome dictionary types (OCEAN, BEACH, RIVER, ...)",
            "or biome registry names (minecraft:river, biomesoplenty:lagoon)."})
    public static String[] waveBiomes = {"OCEAN", "BEACH"};

    @Config.Comment({"Biomes that never get waves, even when listed above.",
            "Uses biome registry names, e.g. minecraft:frozen_ocean."})
    public static String[] waveBiomeBlacklist = {};

    /* ---- Config Sync ---- */

    /**
     * Synchronizes the {@code @Config} annotation changes when the user edits
     * values through the in-game GUI.
     */
    @Mod.EventBusSubscriber(modid = ParticularMod.MODID)
    public static class ConfigSync {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (ParticularMod.MODID.equals(event.getModID()))
                ConfigManager.sync(ParticularMod.MODID, Config.Type.INSTANCE);
        }
    }
}
