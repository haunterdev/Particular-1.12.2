package com.leclowndu93150.particular.particles.splashes;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.Color;

/**
 * Foam layer for water/lava splashes. Extends {@link WaterSplashParticle} and
 * adjusts color: lava uses a warm foam tone (231,174,87); water renders white
 * ({@code colored = false}). Otherwise identical render and physics.
 *
 * <p>Port of 1.20.1 {@code WaterSplashFoamParticle}.
 */
@SideOnly(Side.CLIENT)
public class WaterSplashFoamParticle extends WaterSplashParticle {

    private static final Color LAVA_FOAM_COLOR = new Color(231, 174, 87);

    public WaterSplashFoamParticle(World world, double x, double y, double z,
                                   float width, float height,
                                   TextureAtlasSprite[] frames, boolean lava) {
        super(world, x, y, z, width, height, frames, lava);
        if (lava) {
            this.colored = true;
            this.color = LAVA_FOAM_COLOR;
        } else {
            this.colored = false;
        }
    }
}
