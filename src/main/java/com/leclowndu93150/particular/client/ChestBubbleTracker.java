package com.leclowndu93150.particular.client;

import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.EnderBubbleParticle;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Chest / ender-chest bubble emitter. Port of the 1.20.1 chest bubble mixins
 * ({@code InjectChestBlockEntity}, {@code InjectEnderChestBlockEntity},
 * {@code InvokerViewerCountManager}) + {@code CommonClass.spawnChestBubbles}.
 *
 * <p>1.12.2 has no waterlogging, so a "submerged open chest" is uncommon (it
 * requires a water block directly above the chest). Each client tick we scan
 * {@code world.loadedTileEntityList} for open chests ({@code numPlayersUsing > 0})
 * that have water above and emit rising {@link EnderBubbleParticle}s. Pure client-side.
 *
 * <p>Note: {@code soulSandBubbles}/bubble-column features have no 1.12.2 equivalent
 * (added MC 1.13) - see {@link #soulSandStub()} for the gated no-op kept for a
 * future bubble-column backport.
 */
@SideOnly(Side.CLIENT)
public final class ChestBubbleTracker {

    private static final Random RANDOM = new Random();
    private static final int BUBBLES_PER_TICK = 2;
    private static final int WATER_SCAN_UP = 16;

    private ChestBubbleTracker() {
    }

    public static void tick(World world) {
        if (!ParticularConfig.chestBubbles) {
            return;
        }
        for (int i = 0; i < world.loadedTileEntityList.size(); i++) {
            TileEntity te = world.loadedTileEntityList.get(i);
            if (te == null) {
                continue;
            }
            int viewers;
            boolean ender;
            if (te instanceof TileEntityChest) {
                viewers = ((TileEntityChest) te).numPlayersUsing;
                ender = false;
            } else if (te instanceof TileEntityEnderChest) {
                viewers = ((TileEntityEnderChest) te).numPlayersUsing;
                ender = true;
            } else {
                continue;
            }
            if (viewers <= 0) {
                continue;
            }
            BlockPos pos = te.getPos();
            // Chests can't be waterlogged in 1.12.2 - scan UP for the nearest water block
            // (there may be a gap above the chest). Bubbles spawn in that water and rise.
            int waterY = findWaterColumnBottom(world, pos);
            if (waterY < 0) {
                continue;
            }
            emitBubbles(world, pos, waterY, ender);
        }
    }

    /** Scan up from the chest for the first water block within {@link #WATER_SCAN_UP}. Returns its Y or -1. */
    private static int findWaterColumnBottom(World world, BlockPos pos) {
        for (int dy = 1; dy <= WATER_SCAN_UP; dy++) {
            BlockPos p = pos.up(dy);
            if (!world.isBlockLoaded(p)) {
                return -1;
            }
            if (world.getBlockState(p).getMaterial() == Material.WATER) {
                return p.getY();
            }
        }
        return -1;
    }

    private static void emitBubbles(World world, BlockPos pos, int waterY, boolean ender) {
        TextureAtlasSprite sprite = ParticleSprites.enderBubble();
        for (int i = 0; i < BUBBLES_PER_TICK; i++) {
            double x = pos.getX() + 0.25 + RANDOM.nextDouble() * 0.5;
            double y = waterY + RANDOM.nextDouble() * 0.5;
            double z = pos.getZ() + 0.25 + RANDOM.nextDouble() * 0.5;
            if (ender) {
                // Ender chest → green ender bubble (themed)
                Minecraft.getMinecraft().effectRenderer.addEffect(
                        new EnderBubbleParticle(world, x, y, z, 0.0, 0.0, 0.0, sprite));
            } else {
                // Regular chest → plain white vanilla bubble (not ender-green)
                world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, x, y, z, 0.0, 0.02, 0.0);
            }
        }
    }

    /**
     * Soul-sand / bubble-column bubbles - kept as a gated no-op. MC 1.12.2 has no
     * bubble columns (added 1.13). If a backport mod adds them, this hook can be
     * implemented; {@code ParticularConfig.soulSandBubbles} already gates it.
     */
    static void soulSandStub() {
        if (!ParticularConfig.soulSandBubbles) {
            return;
        }
        // Intentionally empty: no bubble-column block exists in 1.12.2 to detect.
    }
}
