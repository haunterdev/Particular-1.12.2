package com.leclowndu93150.particular.particles;

import com.leclowndu93150.particular.client.ParticleSprites;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Small gravity-affected water droplet sprayed from waterfalls.
 *
 * <p>1.20.1 extended {@code WaterDropParticle}. Reimplemented on vanilla
 * {@link Particle} with biome water-colour tinting and surface-expiry.
 */
@SideOnly(Side.CLIENT)
public class WaterfallSprayParticle extends Particle {

    public WaterfallSprayParticle(World world, double x, double y, double z,
                                   double mX, double mY, double mZ,
                                   TextureAtlasSprite sprite) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleTexture(sprite);

        // Water-droplet gravity (vanilla range 0.04–0.06)
        this.particleGravity = 0.04F;
        // 1.20 WaterDropParticle has quadSize ~0.1 → particleScale = 1.0
        this.particleScale = 1.0F;
        this.particleMaxAge = 16;

        // Spray velocity: add incoming horizontal, damp vertical
        this.motionX += mX;
        this.motionY *= 0.75;
        this.motionZ += mZ;

        // Biome water colour
        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        int rgb = BiomeColorHelper.getWaterColorAtPos(world, pos);
        this.particleRed   = ((rgb >> 16) & 0xFF) / 255.0F;
        this.particleGreen = ((rgb >> 8)  & 0xFF) / 255.0F;
        this.particleBlue  = ( rgb        & 0xFF) / 255.0F;

        this.canCollide = true;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }

        // Custom motion (override → self-apply friction per Gotcha #12c)
        this.motionY -= 0.04D * (double) this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);

        // Air friction
        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;

        // Ground: 50 % chance to expire, otherwise slow horizontal
        if (this.onGround) {
            if (this.rand.nextFloat() < 0.5F) {
                this.setExpired();
            } else {
                this.motionX *= 0.7D;
                this.motionZ *= 0.7D;
            }
        }

        // Die when inside water
        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        if (this.world.getBlockState(pos).getMaterial() == Material.WATER) {
            this.setExpired();
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    /** Spawn a waterfall spray droplet with a small random outward burst. */
    public static void spawn(World world, double x, double y, double z) {
        double mX = (world.rand.nextFloat() * 2.0F - 1.0F) * 0.1;
        double mZ = (world.rand.nextFloat() * 2.0F - 1.0F) * 0.1;
        Minecraft.getMinecraft().effectRenderer.addEffect(
                new WaterfallSprayParticle(world, x, y, z, mX, 0.1, mZ, ParticleSprites.waterfallSpray()));
    }
}
