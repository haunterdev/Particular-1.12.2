package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.util.TextureCache;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-side particle sprite registry.
 *
 * <p>Registers all Particular particle sprites onto the vanilla block atlas
 * during {@link TextureStitchEvent.Pre}, resolves them after stitching
 * in {@link TextureStitchEvent.Post}, and exposes static getters for
 * particle classes to use.
 */
@SideOnly(Side.CLIENT)
public class ParticleSprites {

    /* ---- Sprite resource locations (filled at init) ---- */
    private static final ResourceLocation[] CASCADE_LOCS = new ResourceLocation[12];
    private static final ResourceLocation[] WATER_RIPPLE_LOCS = new ResourceLocation[7];
    private static final ResourceLocation[] WATER_SPLASH_LOCS = new ResourceLocation[9];
    private static final ResourceLocation[] WATER_SPLASH_FOAM_LOCS = new ResourceLocation[9];
    private static final ResourceLocation[] WATER_SPLASH_RING_LOCS = new ResourceLocation[9];
    private static final ResourceLocation[] ENDER_BUBBLE_POP_LOCS = new ResourceLocation[5];

    /* Leaf sprite-sets: one random frame is picked per spawned leaf (matches 1.20.1). */
    private static final ResourceLocation[] OAK_LEAF_LOCS = new ResourceLocation[6];
    private static final ResourceLocation[] BIRCH_LEAF_LOCS = new ResourceLocation[6];
    private static final ResourceLocation[] SPRUCE_LEAF_LOCS = new ResourceLocation[2];
    private static final ResourceLocation[] JUNGLE_LEAF_LOCS = new ResourceLocation[3];
    private static final ResourceLocation[] ACACIA_LEAF_LOCS = new ResourceLocation[2];
    private static final ResourceLocation[] MANGROVE_LEAF_LOCS = new ResourceLocation[6];

    private static ResourceLocation ENDER_BUBBLE_LOC;
    private static ResourceLocation FIREFLY_LOC;
    private static ResourceLocation CUBOID_LOC;
    private static ResourceLocation GENERIC_0_LOC;
    private static ResourceLocation CAVE_DUST_LOC;

    /* ---- Resolved sprite fields (filled after TextureStitchEvent.Post) ---- */
    private static TextureAtlasSprite[] cascadeSprites;
    private static TextureAtlasSprite[] waterRippleSprites;
    private static TextureAtlasSprite[] waterSplashSprites;
    private static TextureAtlasSprite[] waterSplashFoamSprites;
    private static TextureAtlasSprite[] waterSplashRingSprites;
    private static TextureAtlasSprite[] enderBubblePopSprites;

    private static TextureAtlasSprite[] oakLeafSprites;
    private static TextureAtlasSprite[] birchLeafSprites;
    private static TextureAtlasSprite[] spruceLeafSprites;
    private static TextureAtlasSprite[] jungleLeafSprites;
    private static TextureAtlasSprite[] acaciaLeafSprites;
    private static TextureAtlasSprite[] mangroveLeafSprites;

    private static TextureAtlasSprite enderBubbleSprite;
    private static TextureAtlasSprite fireflySprite;
    private static TextureAtlasSprite cuboidSprite;
    private static TextureAtlasSprite generic0Sprite;
    private static TextureAtlasSprite caveDustSprite;

    private static final String PREFIX = "particular:particle/";

    static {
        // cascade_0 .. cascade_11
        for (int i = 0; i < 12; i++) {
            CASCADE_LOCS[i] = new ResourceLocation("particular", "particle/cascade_" + i);
        }
        // water_ripple_1 .. water_ripple_7
        for (int i = 0; i < 7; i++) {
            WATER_RIPPLE_LOCS[i] = new ResourceLocation("particular", "particle/water_ripple_" + (i + 1));
        }
        // water_splash_1 .. water_splash_9
        for (int i = 0; i < 9; i++) {
            WATER_SPLASH_LOCS[i] = new ResourceLocation("particular", "particle/water_splash_" + (i + 1));
        }
        // water_splash_foam_1 .. water_splash_foam_9
        for (int i = 0; i < 9; i++) {
            WATER_SPLASH_FOAM_LOCS[i] = new ResourceLocation("particular", "particle/water_splash_foam_" + (i + 1));
        }
        // water_splash_ring_1 .. water_splash_ring_9
        for (int i = 0; i < 9; i++) {
            WATER_SPLASH_RING_LOCS[i] = new ResourceLocation("particular", "particle/water_splash_ring_" + (i + 1));
        }
        // ender_bubble_pop_0 .. ender_bubble_pop_4
        for (int i = 0; i < 5; i++) {
            ENDER_BUBBLE_POP_LOCS[i] = new ResourceLocation("particular", "particle/ender_bubble_pop_" + i);
        }
        // leaf sprite-sets (filenames are 1-indexed in the source pack)
        fillLeaf(OAK_LEAF_LOCS, "oak_leaf");
        fillLeaf(BIRCH_LEAF_LOCS, "birch_leaf");
        fillLeaf(SPRUCE_LEAF_LOCS, "spruce_leaf");
        fillLeaf(JUNGLE_LEAF_LOCS, "jungle_leaf");
        fillLeaf(ACACIA_LEAF_LOCS, "acacia_leaf");
        fillLeaf(MANGROVE_LEAF_LOCS, "mangrove_leaf");
        // singles
        ENDER_BUBBLE_LOC = new ResourceLocation("particular", "particle/ender_bubble");
        FIREFLY_LOC = new ResourceLocation("particular", "particle/firefly");
        CUBOID_LOC = new ResourceLocation("particular", "particle/cuboid");
        GENERIC_0_LOC = new ResourceLocation("particular", "particle/generic_0");
        CAVE_DUST_LOC = new ResourceLocation("particular", "particle/cave_dust");
    }

    private static void fillLeaf(ResourceLocation[] locs, String name) {
        for (int i = 0; i < locs.length; i++) {
            locs[i] = new ResourceLocation("particular", "particle/" + name + "_" + (i + 1));
        }
    }

    /**
     * Called from ClientProxy.preInit. Registers this handler on the Forge event bus.
     */
    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ParticleSprites());
    }

    /**
     * Register all particle sprites on the block atlas before stitching.
     */
    @SubscribeEvent
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        if (!"textures".equals(map.getBasePath())) return;

        for (ResourceLocation loc : CASCADE_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : WATER_RIPPLE_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : WATER_SPLASH_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : WATER_SPLASH_FOAM_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : WATER_SPLASH_RING_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : ENDER_BUBBLE_POP_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : OAK_LEAF_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : BIRCH_LEAF_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : SPRUCE_LEAF_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : JUNGLE_LEAF_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : ACACIA_LEAF_LOCS) map.registerSprite(loc);
        for (ResourceLocation loc : MANGROVE_LEAF_LOCS) map.registerSprite(loc);
        map.registerSprite(ENDER_BUBBLE_LOC);
        map.registerSprite(FIREFLY_LOC);
        map.registerSprite(CUBOID_LOC);
        map.registerSprite(GENERIC_0_LOC);
        map.registerSprite(CAVE_DUST_LOC);
    }

    /**
     * Resolve registered sprite locations into stitched TextureAtlasSprite arrays/fields.
     */
    @SubscribeEvent
    public void onTextureStitchPost(TextureStitchEvent.Post event) {
        TextureMap map = event.getMap();
        if (!"textures".equals(map.getBasePath())) return;

        cascadeSprites = resolveArray(map, CASCADE_LOCS);
        waterRippleSprites = resolveArray(map, WATER_RIPPLE_LOCS);
        waterSplashSprites = resolveArray(map, WATER_SPLASH_LOCS);
        waterSplashFoamSprites = resolveArray(map, WATER_SPLASH_FOAM_LOCS);
        waterSplashRingSprites = resolveArray(map, WATER_SPLASH_RING_LOCS);
        enderBubblePopSprites = resolveArray(map, ENDER_BUBBLE_POP_LOCS);

        oakLeafSprites = resolveArray(map, OAK_LEAF_LOCS);
        birchLeafSprites = resolveArray(map, BIRCH_LEAF_LOCS);
        spruceLeafSprites = resolveArray(map, SPRUCE_LEAF_LOCS);
        jungleLeafSprites = resolveArray(map, JUNGLE_LEAF_LOCS);
        acaciaLeafSprites = resolveArray(map, ACACIA_LEAF_LOCS);
        mangroveLeafSprites = resolveArray(map, MANGROVE_LEAF_LOCS);

        enderBubbleSprite = map.getAtlasSprite(ENDER_BUBBLE_LOC.toString());
        fireflySprite = map.getAtlasSprite(FIREFLY_LOC.toString());
        cuboidSprite = map.getAtlasSprite(CUBOID_LOC.toString());
        generic0Sprite = map.getAtlasSprite(GENERIC_0_LOC.toString());
        caveDustSprite = map.getAtlasSprite(CAVE_DUST_LOC.toString());

        TextureCache.INST.clear();
        LeafColorUtil.clearCache();
    }

    private static TextureAtlasSprite[] resolveArray(TextureMap map, ResourceLocation[] locs) {
        TextureAtlasSprite[] sprites = new TextureAtlasSprite[locs.length];
        for (int i = 0; i < locs.length; i++) {
            sprites[i] = map.getAtlasSprite(locs[i].toString());
        }
        return sprites;
    }

    /* ---- Static getters (public API for particle classes) ---- */

    /** cascade_0 .. cascade_11 (12 frames) */
    public static TextureAtlasSprite[] cascade() {
        return cascadeSprites;
    }

    /** water_ripple_1 .. water_ripple_7 (7 frames) */
    public static TextureAtlasSprite[] waterRipple() {
        return waterRippleSprites;
    }

    /** water_splash_1 .. water_splash_9 (9 frames) */
    public static TextureAtlasSprite[] waterSplash() {
        return waterSplashSprites;
    }

    /** water_splash_foam_1 .. water_splash_foam_9 (9 frames) */
    public static TextureAtlasSprite[] waterSplashFoam() {
        return waterSplashFoamSprites;
    }

    /** water_splash_ring_1 .. water_splash_ring_9 (9 frames) */
    public static TextureAtlasSprite[] waterSplashRing() {
        return waterSplashRingSprites;
    }

    /** ender_bubble_pop_0 .. ender_bubble_pop_4 (5 frames) */
    public static TextureAtlasSprite[] enderBubblePop() {
        return enderBubblePopSprites;
    }

    /** Single ender_bubble sprite */
    public static TextureAtlasSprite enderBubble() {
        return enderBubbleSprite;
    }

    /** Single firefly sprite */
    public static TextureAtlasSprite firefly() {
        return fireflySprite;
    }

    /** Single cuboid sprite */
    public static TextureAtlasSprite cuboid() {
        return cuboidSprite;
    }

    /** solid square sprite for cave dust (tinted grey by particle colour) */
    public static TextureAtlasSprite caveDust() {
        return caveDustSprite;
    }

    /** soft round dot for cave dust (config alternative to the square) */
    public static TextureAtlasSprite caveDustRound() {
        return generic0Sprite;
    }

    /** generic_0 sprite - shared by cave dust and waterfall spray */
    public static TextureAtlasSprite waterfallSpray() {
        return generic0Sprite;
    }

    /* ---- Leaf sprite-sets (one random frame chosen per spawned leaf) ---- */

    public static TextureAtlasSprite[] oakLeaf()      { return oakLeafSprites; }
    public static TextureAtlasSprite[] birchLeaf()    { return birchLeafSprites; }
    public static TextureAtlasSprite[] spruceLeaf()   { return spruceLeafSprites; }
    public static TextureAtlasSprite[] jungleLeaf()   { return jungleLeafSprites; }
    public static TextureAtlasSprite[] acaciaLeaf()   { return acaciaLeafSprites; }
    public static TextureAtlasSprite[] mangroveLeaf() { return mangroveLeafSprites; }
}
