package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.ParticularMod;
import com.leclowndu93150.particular.config.ParticularConfig;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Coastal waves: foam crests that roll across the sea toward a beach, break and fade.
 *
 * <p>Waves are plain objects, ticked from the client tick and drawn from
 * {@code RenderWorldLastEvent}, which runs after the translucent water layer. 1.12.2 draws
 * particles before water, so a wave made as a particle would sit under the surface.
 *
 * <p>from AnExtraTouch industries/angelic/anextratouch/handlers/client/effects/WaterWaveManager.java
 * (LGPL-3.0), the 1.7.10 backport of Verph's Coastal Waves: spawn budget, sampling, nearest-shore
 * ring scan, size classes, wave motion and fade. Changes: 1.12.2 API, the landmass test written
 * out here, the crest frame driven by distance to the shore as Coastal Waves does, the surface
 * height and light taken the way 1.12.2 draws water, fog on, one draw per frame texture, and no
 * sound or shader mesh. The textures are Coastal Waves' (BSD-2-Clause, see
 * {@code assets/particular/textures/particle/waves/LICENSE-coastal-waves.txt}).
 */
@SideOnly(Side.CLIENT)
public final class WaveManager {

    private static final int MAX_SPAWNS_PER_TICK = 8;
    private static final int SPAWN_ATTEMPTS_PER_WAVE = 18;
    private static final int CACHE_TTL = 600;
    private static final int CACHE_LIMIT = 4096;
    private static final double SURFACE_OFFSET = 0.02D;
    // The quad stops this close to the shore block centre and breaks.
    private static final double CONTACT_DISTANCE = 1.2D;
    private static final int SHORE_TICKS = 28;
    private static final int FRAME_COUNT = 5;

    private static final int SIZE_SMALL = 0;
    private static final int SIZE_MEDIUM = 1;
    private static final int SIZE_LARGE = 2;

    // Coastal Waves ships small, medium and large frames as identical files, so one set serves all.
    private static final ResourceLocation[] FRAMES = new ResourceLocation[FRAME_COUNT];
    // Every 3x3 window of the 5x5 land mask that covers the centre column.
    private static final int[] LAND_WINDOWS = new int[9];

    static {
        for (int i = 0; i < FRAME_COUNT; i++) {
            FRAMES[i] = new ResourceLocation(ParticularMod.MODID, "textures/particle/waves/wave_" + i + ".png");
        }
        int n = 0;
        for (int oz = 0; oz <= 2; oz++) {
            for (int ox = 0; ox <= 2; ox++) {
                int mask = 0;
                for (int dz = 0; dz < 3; dz++) {
                    for (int dx = 0; dx < 3; dx++) {
                        mask |= 1 << ((oz + dz) * 5 + ox + dx);
                    }
                }
                LAND_WINDOWS[n++] = mask;
            }
        }
    }

    private static final Random RANDOM = new Random();
    private static final List<Wave> WAVES = new ArrayList<>();
    private static final Map<Long, CachedShore> SHORE_CACHE = new HashMap<>();
    private static World currentWorld;
    private static float spawnBudget;

    private WaveManager() {
    }

    /** Drops every wave and cached shore. World change, world unload, or waves switched off. */
    public static void clear() {
        WAVES.clear();
        SHORE_CACHE.clear();
        spawnBudget = 0.0F;
        currentWorld = null;
    }

    /** Called once per client tick (END phase) with a non-null client world. */
    public static void tick(World world) {
        if (world != currentWorld) {
            clear();
            currentWorld = world;
        }
        if (!ParticularConfig.coastalWaves) {
            if (!WAVES.isEmpty()) {
                WAVES.clear();
                SHORE_CACHE.clear();
            }
            return;
        }

        Iterator<Wave> iterator = WAVES.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            wave.tick(world);
            if (wave.dead) {
                iterator.remove();
            }
        }

        Minecraft mc = Minecraft.getMinecraft();
        // Minimal particles: the waves already out finish, no new ones start.
        if (mc.gameSettings.particleSetting == 2) {
            return;
        }
        spawn(mc, world);
    }

    private static void spawn(Minecraft mc, World world) {
        Entity viewer = mc.getRenderViewEntity();
        int frequency = ParticularConfig.waveSpawnFrequency;
        if (viewer == null || frequency <= 0 || ParticularConfig.waveSpawnAmount <= 0.0D) {
            return;
        }
        if (world.getTotalWorldTime() % frequency != 0L) {
            return;
        }

        spawnBudget += (float) ParticularConfig.waveSpawnAmount;
        int count = MathHelper.floor(spawnBudget);
        spawnBudget -= count;
        if (RANDOM.nextFloat() < spawnBudget) {
            count++;
            spawnBudget = 0.0F;
        }
        count = Math.min(count, MAX_SPAWNS_PER_TICK);

        int attempts = count * SPAWN_ATTEMPTS_PER_WAVE;
        int spawned = 0;
        for (int i = 0; i < attempts && spawned < count; i++) {
            Sample sample = randomSample(mc, world, viewer);
            if (sample == null || !withinFov(viewer, sample.x + 0.5D, sample.z + 0.5D)) {
                continue;
            }
            Shore shore = nearestShore(world, sample.x, sample.y, sample.z);
            if (shore == null || shore.distance < ParticularConfig.waveMinShoreDistance
                    || shore.distance > ParticularConfig.waveMaxShoreDistance) {
                continue;
            }
            addWave(world, sample, shore);
            spawned++;
        }

        while (WAVES.size() > ParticularConfig.waveMaxCount) {
            WAVES.remove(0);
        }
        if (SHORE_CACHE.size() > CACHE_LIMIT) {
            SHORE_CACHE.clear();
        }
    }

    /** A still surface-water block at a random point of the spawn disc, or null. */
    private static Sample randomSample(Minecraft mc, World world, Entity viewer) {
        double render = Math.max(2, mc.gameSettings.renderDistanceChunks) * 16.0D;
        double radius = Math.min(Math.max(8.0D, ParticularConfig.waveSpawnDistance), render);
        double angle = RANDOM.nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(RANDOM.nextDouble()) * radius;
        int x = MathHelper.floor(viewer.posX + Math.cos(angle) * distance);
        int z = MathHelper.floor(viewer.posZ + Math.sin(angle) * distance);

        BlockPos column = new BlockPos(x, 64, z);
        // allowEmpty=false: on the client the one-argument form is true for any chunk.
        if (!world.isBlockLoaded(column, false) || !biomeAllowed(world.getBiome(column))) {
            return null;
        }
        int top = world.getHeight(column).getY();
        if (top <= 0) {
            return null;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(1, top - 24);
        for (int y = Math.min(255, top); y >= minY; y--) {
            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            // Source blocks only: flowing and falling water has no flat surface to roll on.
            if (!isWaveWater(state) || state.getValue(BlockLiquid.LEVEL) != 0) {
                continue;
            }
            IBlockState above = world.getBlockState(pos.up());
            if (isWaveWater(above) || above.getMaterial().blocksMovement()) {
                continue;
            }
            // A source block's top is drawn at 1 - 1/9 (BlockFluidRenderer.getFluidHeight).
            double surfaceY = y + 1.0D - BlockLiquid.getLiquidHeightPercent(0) + SURFACE_OFFSET;
            return new Sample(x, y, z, surfaceY);
        }
        return null;
    }

    private static boolean biomeAllowed(Biome biome) {
        ResourceLocation id = biome.getRegistryName();
        String name = id == null ? "" : id.toString();
        if (contains(ParticularConfig.waveBiomeBlacklist, name)) {
            return false;
        }
        if (contains(ParticularConfig.waveBiomes, name)) {
            return true;
        }
        for (BiomeDictionary.Type type : BiomeDictionary.getTypes(biome)) {
            if (contains(ParticularConfig.waveBiomes, type.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(String[] entries, String value) {
        if (entries == null || value.isEmpty()) {
            return false;
        }
        for (String entry : entries) {
            if (entry != null && entry.trim().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean withinFov(Entity viewer, double x, double z) {
        double limit = ParticularConfig.waveSpawnFovLimit;
        if (limit >= 360.0D) {
            return true;
        }
        double dx = x - viewer.posX;
        double dz = z - viewer.posZ;
        float targetYaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float delta = MathHelper.wrapDegrees(targetYaw - viewer.rotationYaw);
        return Math.abs(delta) <= limit * 0.5D;
    }

    private static Shore nearestShore(World world, int x, int y, int z) {
        long key = blockKey(x, y, z);
        long now = world.getTotalWorldTime();
        CachedShore cached = SHORE_CACHE.get(key);
        if (cached != null && now - cached.tick <= CACHE_TTL) {
            return cached.shore;
        }
        Shore shore = scanNearestShore(world, x, y, z);
        SHORE_CACHE.put(key, new CachedShore(shore, now));
        return shore;
    }

    /** Expanding square rings around the water block; the closest shore on the first ring that has one. */
    private static Shore scanNearestShore(World world, int waterX, int waterY, int waterZ) {
        int search = Math.max(1, ParticularConfig.waveSearchDistance);
        for (int radius = 1; radius <= search; radius++) {
            Shore best = null;
            for (int dx = -radius; dx <= radius; dx++) {
                best = closerShore(world, waterX, waterY, waterZ, waterX + dx, waterZ - radius, best);
                best = closerShore(world, waterX, waterY, waterZ, waterX + dx, waterZ + radius, best);
            }
            for (int dz = -radius + 1; dz <= radius - 1; dz++) {
                best = closerShore(world, waterX, waterY, waterZ, waterX - radius, waterZ + dz, best);
                best = closerShore(world, waterX, waterY, waterZ, waterX + radius, waterZ + dz, best);
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static Shore closerShore(World world, int waterX, int waterY, int waterZ, int shoreX, int shoreZ,
            Shore current) {
        double dx = shoreX - waterX;
        double dz = shoreZ - waterZ;
        double distanceSq = dx * dx + dz * dz;
        if (current != null && distanceSq >= current.distanceSq) {
            return current;
        }
        if (!isShoreColumn(world, shoreX, waterY, shoreZ)) {
            return current;
        }
        double distance = Math.sqrt(distanceSq);
        return new Shore(shoreX, shoreZ, dx / distance, dz / distance, distance, distanceSq);
    }

    /** Solid ground at water level or one above, touching wave water, part of a 3x3 or larger landmass. */
    private static boolean isShoreColumn(World world, int x, int waterY, int z) {
        return isLandColumn(world, x, waterY, z) && hasAdjacentWaveWater(world, x, waterY, z)
                && hasCoastalLandmass(world, x, waterY, z);
    }

    private static boolean hasAdjacentWaveWater(World world, int x, int waterY, int z) {
        return isWaveWater(world, x - 1, waterY, z) || isWaveWater(world, x + 1, waterY, z)
                || isWaveWater(world, x, waterY, z - 1) || isWaveWater(world, x, waterY, z + 1);
    }

    /** A single post or a 2x2 islet does not turn a wave: the column needs a fully solid 3x3 window around it. */
    private static boolean hasCoastalLandmass(World world, int x, int waterY, int z) {
        int landMask = 0;
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (isLandColumn(world, x + dx, waterY, z + dz)) {
                    landMask |= 1 << ((dz + 2) * 5 + dx + 2);
                }
            }
        }
        for (int window : LAND_WINDOWS) {
            if ((landMask & window) == window) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLandColumn(World world, int x, int waterY, int z) {
        return isShoreBlock(world, x, waterY, z) || isShoreBlock(world, x, waterY + 1, z);
    }

    // An unloaded chunk reads as air on the client, which is neither shore nor water.
    private static boolean isShoreBlock(World world, int x, int y, int z) {
        if (y < 0 || y >= 256) {
            return false;
        }
        Material material = world.getBlockState(new BlockPos(x, y, z)).getMaterial();
        return material != Material.AIR && !material.isLiquid() && material.blocksMovement();
    }

    private static boolean isWaveWater(World world, int x, int y, int z) {
        return y >= 0 && y < 256 && isWaveWater(world.getBlockState(new BlockPos(x, y, z)));
    }

    private static boolean isWaveWater(IBlockState state) {
        return state.getBlock() instanceof BlockLiquid && state.getMaterial() == Material.WATER;
    }

    private static boolean isSurfaceWaveWater(World world, int x, int y, int z) {
        if (!isWaveWater(world, x, y, z)) {
            return false;
        }
        IBlockState above = world.getBlockState(new BlockPos(x, y + 1, z));
        return !isWaveWater(above) && !above.getMaterial().blocksMovement();
    }

    private static void addWave(World world, Sample sample, Shore shore) {
        int size = chooseSize(shore.distance, countSurroundingWater(world, sample.x, sample.y, sample.z));
        Wave wave = new Wave();
        wave.x = wave.prevX = sample.x + 0.5D;
        wave.y = wave.prevY = sample.surfaceY;
        wave.z = wave.prevZ = sample.z + 0.5D;
        wave.lightX = sample.x;
        wave.lightY = sample.y;
        wave.lightZ = sample.z;
        wave.dirX = shore.dirX;
        wave.dirZ = shore.dirZ;
        wave.shoreX = shore.x + 0.5D;
        wave.shoreZ = shore.z + 0.5D;
        wave.startDistance = shore.distance;
        wave.width = width(size) * ParticularConfig.waveScale;
        wave.depth = depth(size) * ParticularConfig.waveScale;
        wave.speed = 0.085D + size * 0.018D + RANDOM.nextDouble() * 0.025D;
        wave.maxAge = Math.max(80, (int) (shore.distance / wave.speed) + 60);
        WAVES.add(wave);
    }

    private static int countSurroundingWater(World world, int x, int y, int z) {
        int count = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (isWaveWater(world, x + dx, y, z + dz)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int chooseSize(double distance, int surroundingWater) {
        if (distance >= 20.0D && surroundingWater >= 18) {
            return SIZE_LARGE;
        }
        if (distance >= 10.0D && surroundingWater >= 12) {
            return SIZE_MEDIUM;
        }
        return SIZE_SMALL;
    }

    private static double width(int size) {
        return size == SIZE_LARGE ? 7.5D : size == SIZE_MEDIUM ? 5.25D : 3.25D;
    }

    private static double depth(int size) {
        return size == SIZE_LARGE ? 4.25D : size == SIZE_MEDIUM ? 3.25D : 2.25D;
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) z & 0x3FFFFFFL) << 12 | (long) (y & 0xFFF);
    }

    /** Called from RenderWorldLastEvent, after the translucent layer. */
    public static void render(float partialTicks) {
        if (WAVES.isEmpty() || !ParticularConfig.coastalWaves) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        World world = mc.world;
        if (viewer == null || world == null || world != currentWorld) {
            return;
        }
        double camX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double camY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double camZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        int[] frames = new int[WAVES.size()];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = WAVES.get(i).frame(partialTicks);
        }

        // Vanilla leaves blend, cull, depth mask and fog in a known state here (the translucent pass
        // just ended), so set what the waves need and put that state back afterwards. GL fog still
        // holds this frame's terrain fog, so far waves fade like the water under them.
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
        GlStateManager.depthMask(false);
        GlStateManager.enableTexture2D();
        GlStateManager.enableFog();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.entityRenderer.enableLightmap();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (int frame = 0; frame < FRAME_COUNT; frame++) {
            boolean started = false;
            for (int i = 0; i < frames.length; i++) {
                if (frames[i] != frame) {
                    continue;
                }
                if (!started) {
                    mc.getTextureManager().bindTexture(FRAMES[frame]);
                    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                    started = true;
                }
                WAVES.get(i).emit(buffer, world, camX, camY, camZ, partialTicks);
            }
            if (started) {
                tessellator.draw();
            }
        }

        mc.entityRenderer.disableLightmap();
        GlStateManager.disableFog();
        GlStateManager.depthMask(true);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    }

    private static final class Wave {
        double x;
        double y;
        double z;
        double prevX;
        double prevY;
        double prevZ;
        double dirX;
        double dirZ;
        double shoreX;
        double shoreZ;
        double startDistance;
        double speed;
        double width;
        double depth;
        int lightX;
        int lightY;
        int lightZ;
        int age;
        int prevAge;
        int shoreAge;
        int prevShoreAge;
        int maxAge;
        boolean reachedShore;
        boolean dead;

        void tick(World world) {
            prevAge = age;
            prevShoreAge = shoreAge;
            prevX = x;
            prevY = y;
            prevZ = z;
            age++;

            if (reachedShore) {
                // Breaking: slow to a stop on the sand and fade.
                shoreAge++;
                speed *= 0.84D;
                x += dirX * speed;
                z += dirZ * speed;
                y += 0.001D;
                updateLightPosition(world);
                if (shoreAge > SHORE_TICKS) {
                    dead = true;
                }
                return;
            }

            x += dirX * speed;
            z += dirZ * speed;
            updateLightPosition(world);
            if (age >= maxAge) {
                dead = true;
                return;
            }

            int blockX = MathHelper.floor(x);
            int blockY = MathHelper.floor(y - SURFACE_OFFSET);
            int blockZ = MathHelper.floor(z);
            boolean hitCoast = distanceToShore(x, z) <= CONTACT_DISTANCE;
            if (!hitCoast && !isWaveWater(world, blockX, blockY, blockZ)) {
                if (!isLandColumn(world, blockX, blockY, blockZ)) {
                    // Missing water or unloaded terrain ends the wave without inventing a shore.
                    dead = true;
                    return;
                }
                // A post can cover part of a crest without stopping the whole wave.
                hitCoast = hasCoastalLandmass(world, blockX, blockY, blockZ);
            }
            if (hitCoast) {
                reachedShore = true;
                shoreAge = 0;
            }
        }

        double distanceToShore(double px, double pz) {
            double dx = shoreX - px;
            double dz = shoreZ - pz;
            return Math.sqrt(dx * dx + dz * dz);
        }

        /** Crest for most of the run, breaking frames over the last stretch, last frame from contact on. */
        int frame(float partialTicks) {
            if (reachedShore) {
                return FRAME_COUNT - 1;
            }
            double px = prevX + (x - prevX) * partialTicks;
            double pz = prevZ + (z - prevZ) * partialTicks;
            double span = Math.max(0.001D, startDistance - CONTACT_DISTANCE);
            double progress = MathHelper.clamp(1.0D - (distanceToShore(px, pz) - CONTACT_DISTANCE) / span, 0.0D, 1.0D);
            int frame = (int) Math.round(progress * progress * progress * (FRAME_COUNT - 1));
            return MathHelper.clamp(frame, 0, FRAME_COUNT - 1);
        }

        float alpha(float partialTicks) {
            if (reachedShore) {
                float shoreDelta = prevShoreAge + (shoreAge - prevShoreAge) * partialTicks;
                if (shoreDelta <= 10.0F) {
                    return 1.0F;
                }
                return Math.max(0.0F, 1.0F - (shoreDelta - 10.0F) / (SHORE_TICKS - 10.0F));
            }
            float ageDelta = prevAge + (age - prevAge) * partialTicks;
            return Math.min(1.0F, ageDelta / 10.0F);
        }

        /** Keep the last surface-water cell so a crest drifting onto the bank is not lit from inside it. */
        void updateLightPosition(World world) {
            int blockX = MathHelper.floor(x);
            int blockZ = MathHelper.floor(z);
            if (isSurfaceWaveWater(world, blockX, lightY, blockZ)) {
                lightX = blockX;
                lightZ = blockZ;
            }
        }

        /** Light the way water itself is lit: brightest of the water cell and the cell above (BlockLiquid). */
        int light(World world) {
            BlockPos pos = new BlockPos(lightX, lightY, lightZ);
            int here = world.getCombinedLight(pos, 0);
            int up = world.getCombinedLight(pos.up(), 0);
            int block = Math.max(here & 0xFF, up & 0xFF);
            int sky = Math.max(here >> 16 & 0xFF, up >> 16 & 0xFF);
            return block | sky << 16;
        }

        void emit(BufferBuilder buffer, World world, double camX, double camY, double camZ, float partialTicks) {
            double px = prevX + (x - prevX) * partialTicks - camX;
            double py = prevY + (y - prevY) * partialTicks - camY;
            double pz = prevZ + (z - prevZ) * partialTicks - camZ;

            double sideX = -dirZ;
            double sideZ = dirX;
            double halfWidth = width * 0.5D;
            double halfDepth = depth * 0.5D;

            // Texture top (the crest's leading edge) faces the shore.
            double x0 = px - sideX * halfWidth - dirX * halfDepth;
            double z0 = pz - sideZ * halfWidth - dirZ * halfDepth;
            double x1 = px - sideX * halfWidth + dirX * halfDepth;
            double z1 = pz - sideZ * halfWidth + dirZ * halfDepth;
            double x2 = px + sideX * halfWidth + dirX * halfDepth;
            double z2 = pz + sideZ * halfWidth + dirZ * halfDepth;
            double x3 = px + sideX * halfWidth - dirX * halfDepth;
            double z3 = pz + sideZ * halfWidth - dirZ * halfDepth;

            float alpha = alpha(partialTicks);
            int light = light(world);
            int sky = light >> 16 & 0xFFFF;
            int block = light & 0xFFFF;
            buffer.pos(x0, py, z0).tex(0.0D, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).lightmap(sky, block).endVertex();
            buffer.pos(x1, py, z1).tex(0.0D, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).lightmap(sky, block).endVertex();
            buffer.pos(x2, py, z2).tex(1.0D, 0.0D).color(1.0F, 1.0F, 1.0F, alpha).lightmap(sky, block).endVertex();
            buffer.pos(x3, py, z3).tex(1.0D, 1.0D).color(1.0F, 1.0F, 1.0F, alpha).lightmap(sky, block).endVertex();
        }
    }

    private static final class Sample {
        final int x;
        final int y;
        final int z;
        final double surfaceY;

        Sample(int x, int y, int z, double surfaceY) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.surfaceY = surfaceY;
        }
    }

    private static final class Shore {
        final int x;
        final int z;
        final double dirX;
        final double dirZ;
        final double distance;
        final double distanceSq;

        Shore(int x, int z, double dirX, double dirZ, double distance, double distanceSq) {
            this.x = x;
            this.z = z;
            this.dirX = dirX;
            this.dirZ = dirZ;
            this.distance = distance;
            this.distanceSq = distanceSq;
        }
    }

    private static final class CachedShore {
        final Shore shore;
        final long tick;

        CachedShore(Shore shore, long tick) {
            this.shore = shore;
            this.tick = tick;
        }
    }
}
