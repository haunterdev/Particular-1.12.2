package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.CascadeParticle;
import com.leclowndu93150.particular.particles.WaterfallSprayParticle;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Waterfall cascade tracker. Port of the 1.20.1 {@code CommonClass} cascade system
 * (fluid-tick mixin + {@code CascadeCache} + {@code onClientTick} renderer).
 *
 * <p>1.12.2 has no fluid-tick hook and no {@code FluidState}, so instead of an
 * event-fed cache we throttle a volume scan around the player (every
 * {@link #RESCAN_INTERVAL} ticks) to discover waterfall junctions - a flowing-water
 * column landing in a water pool - and render cascade + spray particles at each
 * tracked position every tick. Tracked positions are re-validated cheaply per tick
 * and dropped when they stop being a waterfall or leave range. Pure client-side.
 */
@SideOnly(Side.CLIENT)
public final class CascadeTracker {

    private static final int RESCAN_INTERVAL = 10;
    private static final int SCAN_RADIUS_XZ = 12;
    private static final int SCAN_RADIUS_Y = 8;
    private static final int MAX_TRACKED = 256;

    private static final Random RANDOM = new Random();
    /** cascade position → strength (number of horizontal water-source neighbours). */
    private static final Map<BlockPos, Integer> CASCADES = new HashMap<BlockPos, Integer>();
    private static int rescanTimer = 0;

    private CascadeTracker() {
    }

    public static void tick(World world) {
        if (!ParticularConfig.cascades) {
            if (!CASCADES.isEmpty()) {
                CASCADES.clear();
            }
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        BlockPos playerPos = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);

        if (--rescanTimer <= 0) {
            rescanTimer = RESCAN_INTERVAL;
            rescan(world, playerPos);
        }

        renderAndValidate(world);
    }

    private static void rescan(World world, BlockPos center) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -SCAN_RADIUS_XZ; dx <= SCAN_RADIUS_XZ; dx++) {
            for (int dz = -SCAN_RADIUS_XZ; dz <= SCAN_RADIUS_XZ; dz++) {
                for (int dy = -SCAN_RADIUS_Y; dy <= SCAN_RADIUS_Y; dy++) {
                    pos.setPos(cx + dx, cy + dy, cz + dz);
                    if (!world.isBlockLoaded(pos)) {
                        continue;
                    }
                    int strength = cascadeStrength(world, pos);
                    if (strength > 0 && CASCADES.size() < MAX_TRACKED) {
                        CASCADES.put(pos.toImmutable(), strength);
                    }
                }
            }
        }
    }

    /**
     * A cascade junction: a water-source block whose block above is downward-flowing
     * water and whose block below is a water source, with at least one horizontal
     * water-source neighbour and not fully encased above. Returns strength (1..4) or 0.
     */
    private static int cascadeStrength(World world, BlockPos pos) {
        if (!isSource(world, pos)) {
            return 0;
        }
        if (!isFlowingWater(world, pos.up()) || !isSource(world, pos.down())) {
            return 0;
        }
        int strength = 0;
        if (isSource(world, pos.north())) strength++;
        if (isSource(world, pos.east())) strength++;
        if (isSource(world, pos.south())) strength++;
        if (isSource(world, pos.west())) strength++;
        if (strength == 0) {
            return 0;
        }
        BlockPos up = pos.up();
        boolean encased = !world.isAirBlock(up.north()) && !world.isAirBlock(up.east())
                && !world.isAirBlock(up.south()) && !world.isAirBlock(up.west());
        return encased ? 0 : strength;
    }

    private static boolean isWater(IBlockState state) {
        return state.getMaterial() == Material.WATER;
    }

    private static boolean isSource(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!isWater(state)) {
            return false;
        }
        Integer level = tryLevel(state);
        return level != null && level == 0;
    }

    private static boolean isFlowingWater(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!isWater(state)) {
            return false;
        }
        Integer level = tryLevel(state);
        // Any water above a junction (flowing or a source feeding down) counts.
        return level != null;
    }

    private static Integer tryLevel(IBlockState state) {
        try {
            return state.getValue(BlockLiquid.LEVEL);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static void renderAndValidate(World world) {
        Iterator<Map.Entry<BlockPos, Integer>> it = CASCADES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            BlockPos pos = entry.getKey();
            if (!world.isBlockLoaded(pos) || cascadeStrength(world, pos) == 0) {
                it.remove();
                continue;
            }
            spawnCascade(world, pos, entry.getValue());
        }
    }

    /** Port of the {@code onClientTick} cascade spawn loop (block-water adaptation). */
    private static void spawnCascade(World world, BlockPos pos, int strength) {
        float height = 1.0f;
        int particles = 2 + strength;
        for (int i = 0; i < particles; i++) {
            double x = pos.getX();
            double y = pos.getY() + RANDOM.nextDouble() * height + 1.0;
            double z = pos.getZ();
            int side = RANDOM.nextInt(4);
            if (side == 0 && isSource(world, pos.north())) {
                x += RANDOM.nextDouble();
                z += 0.5 + (RANDOM.nextDouble() * 0.25 - 0.5) * 1.5;
            } else if (side == 1 && isSource(world, pos.east())) {
                x += 0.5 + (0.25 + RANDOM.nextDouble() * 0.25) * 1.5;
                z += RANDOM.nextDouble();
            } else if (side == 2 && isSource(world, pos.south())) {
                x += RANDOM.nextDouble();
                z += 0.5 + (0.25 + RANDOM.nextDouble() * 0.25) * 1.5;
            } else if (isSource(world, pos.west())) {
                x += 0.5 + (RANDOM.nextDouble() * 0.25 - 0.5) * 1.5;
                z += RANDOM.nextDouble();
            } else if (RANDOM.nextBoolean()) {
                x += RANDOM.nextDouble();
                z += RANDOM.nextInt(2);
            } else {
                x += RANDOM.nextInt(2);
                z += RANDOM.nextDouble();
            }
            float size = Math.max(0.75f, (float) strength / 4.0f * height);
            CascadeParticle.spawn(world, x, y, z, 1.0f - (1.0f - size) / 2.0f);

            if (ParticularConfig.waterfallSpray && RANDOM.nextInt(12) == 0) {
                WaterfallSprayParticle.spawn(world, x, y, z);
            }
        }
    }

    /** Discard tracked cascades (e.g. on dimension change / disconnect). */
    public static void clear() {
        CASCADES.clear();
    }

    static int floor(double d) {
        return MathHelper.floor(d);
    }
}
