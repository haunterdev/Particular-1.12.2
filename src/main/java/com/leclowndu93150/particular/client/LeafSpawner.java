package com.leclowndu93150.particular.client;

import java.util.Random;

import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.leaves.ConiferLeafParticle;
import com.leclowndu93150.particular.particles.leaves.LeafParticle;
import com.leclowndu93150.particular.particles.leaves.SpinningLeafParticle;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Falling-leaf spawner, the 1.12.2 stand-in for the 1.20.1 {@code InjectLeavesBlock}
 * mixin (which hooked {@code LeavesBlock.animateTick}). Driven by the same
 * random-block sampler used for fireflies and cave dust (see {@link AmbientSpawner}).
 *
 * <p>Each leaf block has a "type" deciding which particle class and sprite-set it
 * uses, mirroring the source's per-block mapping plus its modded-leaf name heuristic.
 * Colour is averaged from the block's texture and biome tint by {@link LeafColorUtil}.
 */
@SideOnly(Side.CLIENT)
public final class LeafSpawner {

    private static final Random RANDOM = new Random();

    /**
     * Leaf categories reachable in 1.12.2 (vanilla leaf variants + the source's modded
     * name heuristic). Each maps to a particle class and a sprite-set, matching the
     * 1.20.1 {@code registerParticleFactories} assignments.
     */
    private enum LeafType {
        OAK, BIRCH, SPRUCE, JUNGLE, ACACIA, DARK_OAK, MANGROVE
    }

    private LeafSpawner() {
    }

    static boolean isLeafBlock(Block block, ResourceLocation id) {
        if (block instanceof BlockLeaves) {
            return true;
        }
        if (id == null) {
            return false;
        }
        String path = id.getPath();
        return path.contains("leaves") || path.contains("leaf");
    }

    /** Port of {@code InjectLeavesBlock.dropLeaves}: maybe drop one leaf from this block. */
    static void trySpawn(World world, BlockPos pos, IBlockState state, Block block, ResourceLocation id) {
        if (RANDOM.nextInt(Math.max(1, ParticularConfig.fallingLeavesSpawnChance)) != 0) {
            return;
        }
        // Only drop where there's a gap below (the source checks the block below isn't
        // solid on its up-face, so the leaf has somewhere to fall).
        BlockPos below = pos.down();
        if (world.getBlockState(below).isSideSolid(world, below, EnumFacing.UP)) {
            return;
        }

        LeafType type = resolveType(state, block, id);
        TextureAtlasSprite[] sprites = spritesFor(type);
        if (sprites == null || sprites.length == 0) {
            return; // atlas not stitched yet
        }

        float[] color = LeafColorUtil.getLeafColor(world, pos, state);
        double x = pos.getX() + 0.02 + RANDOM.nextDouble() * 0.96;
        double y = pos.getY() - 0.05;
        double z = pos.getZ() + 0.02 + RANDOM.nextDouble() * 0.96;
        TextureAtlasSprite sprite = sprites[RANDOM.nextInt(sprites.length)];

        AmbientSpawner.add(create(type, world, x, y, z, color, sprite));
    }

    private static LeafParticle create(LeafType type, World world, double x, double y, double z,
                                       float[] c, TextureAtlasSprite sprite) {
        // if/else, not switch-on-enum: synthetic switch-map classes crash under coremod
        // transformers.
        if (type == LeafType.SPRUCE) {
            return new ConiferLeafParticle(world, x, y, z, c[0], c[1], c[2], sprite);
        }
        if (type == LeafType.BIRCH || type == LeafType.ACACIA) {
            return new SpinningLeafParticle(world, x, y, z, c[0], c[1], c[2], sprite);
        }
        return new LeafParticle(world, x, y, z, c[0], c[1], c[2], sprite);
    }

    private static TextureAtlasSprite[] spritesFor(LeafType type) {
        if (type == LeafType.BIRCH) return ParticleSprites.birchLeaf();
        if (type == LeafType.SPRUCE) return ParticleSprites.spruceLeaf();
        if (type == LeafType.JUNGLE) return ParticleSprites.jungleLeaf();
        if (type == LeafType.ACACIA) return ParticleSprites.acaciaLeaf();
        if (type == LeafType.MANGROVE) return ParticleSprites.mangroveLeaf();
        // OAK and DARK_OAK both use the oak sprite-set (dark_oak reuses it in the source).
        return ParticleSprites.oakLeaf();
    }

    /** Vanilla leaf variant first; otherwise the source's modded-name heuristic. */
    private static LeafType resolveType(IBlockState state, Block block, ResourceLocation id) {
        if (block instanceof BlockLeaves) {
            LeafType byVariant = fromVariant(state);
            if (byVariant != null) {
                return byVariant;
            }
        }
        String path = id != null ? id.getPath() : "";
        if (path.contains("spruce") || path.contains("pine") || path.contains("fir")
                || path.contains("conifer")) {
            return LeafType.SPRUCE;
        }
        if (path.contains("birch")) return LeafType.BIRCH;
        if (path.contains("jungle")) return LeafType.JUNGLE;
        if (path.contains("acacia")) return LeafType.ACACIA;
        if (path.contains("dark_oak")) return LeafType.DARK_OAK;
        if (path.contains("mangrove")) return LeafType.MANGROVE;
        return LeafType.OAK;
    }

    /** Read the vanilla {@code variant} enum (oak/spruce/birch/jungle/acacia/dark_oak). */
    private static LeafType fromVariant(IBlockState state) {
        for (IProperty<?> prop : state.getPropertyKeys()) {
            if (!"variant".equals(prop.getName())) {
                continue;
            }
            String variant = state.getValue(prop).toString().toLowerCase();
            if (variant.contains("spruce")) return LeafType.SPRUCE;
            if (variant.contains("birch")) return LeafType.BIRCH;
            if (variant.contains("jungle")) return LeafType.JUNGLE;
            if (variant.contains("acacia")) return LeafType.ACACIA;
            if (variant.contains("dark_oak")) return LeafType.DARK_OAK;
            return LeafType.OAK;
        }
        return null;
    }

    static ResourceLocation idOf(Block block) {
        return ForgeRegistries.BLOCKS.getKey(block);
    }
}
