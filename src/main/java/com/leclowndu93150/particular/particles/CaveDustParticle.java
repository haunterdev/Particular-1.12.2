package com.leclowndu93150.particular.particles;

import com.leclowndu93150.particular.config.ParticularConfig;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Cave dust - a faithful 1.12.2 reimplementation of the 1.20.1
 * {@code BaseAshSmokeParticle} that {@code CaveDustParticle} extended.
 *
 * <p>Key behaviours from the vanilla parent (verified against the 1.20.1 source):
 * <ul>
 *   <li>Air friction {@code 0.96}.</li>
 *   <li>"Acceleration" is a small <b>random vertical gravity</b>, re-rolled
 *       occasionally - the dust drifts gently up/down, barely moving horizontally.</li>
 *   <li>Render size <b>grows in from 0 → full over the first 1/32 of life</b>
 *       ({@code m_5902_} ramp) - this is the characteristic "feel".</li>
 *   <li>Lifetime is randomised: {@code baseMaxAge / (rand*0.8 + 0.2)}.</li>
 * </ul>
 * Colour comes from {@link ParticularConfig#caveDustColor}; alpha fades in/out.
 */
@SideOnly(Side.CLIENT)
public class CaveDustParticle extends Particle {

    private static final float FRICTION = 0.96f;
    private final int fadeDuration;
    private final float baseScale;

    public CaveDustParticle(World world, double x, double y, double z,
                            float scaleMultiplier, TextureAtlasSprite sprite, boolean fullSprite) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleTexture(sprite);

        // Match the 1.20.1 on-screen footprint exactly.
        //   vanilla half-size = (rand*0.5+0.5) * 0.2 * 0.75   [0.2 = base 0.1*2, 0.75 = BaseAshSmokeParticle mult]
        //   vanilla shows only its ONE opaque texel = 1/8 of that quad.
        // 1.12.2 render half-size = 0.1 * particleScale, so to render that visible texel
        // as a full opaque quad: particleScale = (visible half)/0.1 = (rand*0.5+0.5)*0.1875.
        //
        // The solid square sprite (cave_dust.png) is fully opaque → it IS the visible
        // square, no compensation. The legacy single-pixel generic_0 ("round") sprite
        // fills only 1/8 of its quad, so it needs ×8 to reach the same visible size.
        // NOTE: generic_0 on the mipmapped block atlas mip-averages its lone pixel toward
        // transparency (faded round blur at distance) - the solid square is the faithful,
        // mip-stable path and the default.
        float visibleScale = (this.rand.nextFloat() * 0.5f + 0.5f) * 0.1875f * scaleMultiplier;
        this.baseScale = fullSprite ? visibleScale : visibleScale * 8.0f;
        this.particleScale = 0.0f; // grows in (see onUpdate)

        // Randomised lifetime, like the vanilla parent.
        int base = ParticularConfig.caveDustBaseMaxAge;
        this.particleMaxAge = Math.max(1, (int) (base / (this.rand.nextFloat() * 0.8 + 0.2)));

        // Colour from config (RGB hex → float components).
        int c = ParticularConfig.caveDustColor;
        this.particleRed = ((c >> 16) & 0xFF) / 255.0f;
        this.particleGreen = ((c >> 8) & 0xFF) / 255.0f;
        this.particleBlue = (c & 0xFF) / 255.0f;
        this.particleAlpha = 0.0f;

        // "Acceleration" = tiny random vertical gravity (re-rolled in onUpdate).
        this.particleGravity = (float) ((this.rand.nextFloat() - 0.5f) * ParticularConfig.caveDustMaxAcceleration);

        this.fadeDuration = ParticularConfig.caveDustFadeDuration;
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

        // Occasionally re-roll the gentle vertical drift.
        if (this.rand.nextInt(ParticularConfig.caveDustAccelChangeChance) == 0) {
            this.particleGravity = (float) ((this.rand.nextFloat() - 0.5f) * ParticularConfig.caveDustMaxAcceleration);
        }

        this.motionY -= 0.04 * this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= FRICTION;
        this.motionY *= FRICTION;
        this.motionZ *= FRICTION;

        // Grow in from 0 → full over the first 1/32 of life (vanilla m_5902_ ramp).
        this.particleScale = this.baseScale
                * MathHelper.clamp((float) this.particleAge / (float) this.particleMaxAge * 32.0f, 0.0f, 1.0f);

        // Fade alpha in then out.
        if (this.particleAge <= this.fadeDuration) {
            this.particleAlpha = (float) this.particleAge / (float) this.fadeDuration;
        } else if (this.particleAge > this.particleMaxAge - this.fadeDuration) {
            this.particleAlpha = (float) (this.particleMaxAge - this.particleAge) / (float) this.fadeDuration;
        } else {
            this.particleAlpha = 1.0f;
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
