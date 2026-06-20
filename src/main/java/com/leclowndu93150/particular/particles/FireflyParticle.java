package com.leclowndu93150.particular.particles;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FireflyParticle extends Particle {

    private static final int MIN_OFF_TIME = 40;
    private static final int MAX_OFF_TIME = 80;
    private static final int MIN_ON_TIME = 10;
    private static final int MAX_ON_TIME = 20;

    private final NoiseGeneratorSimplex noise;
    private int ageOffset;
    private int ticksUntilNextSwitch;
    private boolean isOn;

    public FireflyParticle(World world, double x, double y, double z,
                           TextureAtlasSprite sprite) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleTexture(sprite);
        this.particleGravity = 0.0f;
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        // Gotcha #12: particleScale = 1.20 quadSize (0.25) × 10
        this.particleScale = 2.5f;

        // Default color: yellow-green (0.7333, 1.0, 0.4196)
        this.particleRed = 0.73333335f;
        this.particleGreen = 1.0f;
        this.particleBlue = 0.41960785f;

        // 1-in-10: cyan variant
        if (this.rand.nextInt(10) == 1) {
            this.particleRed = 0.41960785f;
            this.particleGreen = 0.98039216f;
            this.particleBlue = 1.0f;

            // Nested 1-in-10: warm orange variant
            if (this.rand.nextInt(10) == 1) {
                this.particleRed = 1.0f;
                this.particleGreen = 0.4862745f;
                this.particleBlue = 0.41960785f;
            }
        }

        this.particleMaxAge = 200;
        this.noise = new NoiseGeneratorSimplex(this.rand);
        this.ageOffset = 0;
        this.ticksUntilNextSwitch = 40;
        this.isOn = false;
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

        this.move(this.motionX, this.motionY, this.motionZ);

        // Nudge on collision - unstick and keep alive
        if (this.onGround) {
            this.ageOffset += 5;
        }

        // Blink toggle
        if (--this.ticksUntilNextSwitch <= 0) {
            if (this.isOn) {
                this.isOn = false;
                this.ticksUntilNextSwitch = MIN_OFF_TIME + this.rand.nextInt(MAX_OFF_TIME - MIN_OFF_TIME + 1);
            } else {
                this.isOn = true;
                this.ticksUntilNextSwitch = MIN_ON_TIME + this.rand.nextInt(MAX_ON_TIME - MIN_ON_TIME + 1);
            }
        }

        // Alpha ramp: toward 1.0 when on and not near death, toward 0.0 otherwise
        this.particleAlpha = this.isOn && this.particleMaxAge - this.particleAge > 3
                ? Math.min(1.0f, this.particleAlpha + 0.33f)
                : Math.max(0.0f, this.particleAlpha - 0.33f);

        // Two-octave Simplex noise drift (matches 1.20 FireflyParticle.tick lines 79-88)
        float speedFactor = 0.1f;
        float noiseFactor = 0.0033333334f;

        this.motionX = this.noise.getValue(
                (double) ((float) this.particleAge * noiseFactor),
                (double) ((float) this.particleAge * noiseFactor)
        ) * (double) speedFactor;

        this.motionY = this.noise.getValue(
                (double) ((float) (this.particleAge + this.ageOffset) * noiseFactor - 50.0f),
                (double) ((float) (this.particleAge + this.ageOffset) * noiseFactor + 100.0f)
        ) * (double) speedFactor * 0.5;

        this.motionZ = this.noise.getValue(
                (double) ((float) this.particleAge * noiseFactor + 100.0f),
                (double) ((float) this.particleAge * noiseFactor - 50.0f)
        ) * (double) speedFactor;

        // Second octave
        speedFactor = 1.0f / (10.0f + (float) Math.sin(Math.PI + (double) ((float) this.particleAge / 30.0f)) * 2.0f);
        noiseFactor = 0.033333335f;

        this.motionX += this.noise.getValue(
                (double) ((float) this.particleAge * noiseFactor),
                (double) ((float) this.particleAge * noiseFactor)
        ) * (double) speedFactor * 0.5;

        this.motionY += this.noise.getValue(
                (double) ((float) (this.particleAge + this.ageOffset) * noiseFactor - 50.0f),
                (double) ((float) (this.particleAge + this.ageOffset) * noiseFactor + 100.0f)
        ) * (double) speedFactor;

        this.motionZ += this.noise.getValue(
                (double) ((float) this.particleAge * noiseFactor + 100.0f),
                (double) ((float) this.particleAge * noiseFactor - 50.0f)
        ) * (double) speedFactor * 0.5;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        // Full brightness both sky and block light (emissive / glow-in-the-dark)
        return 0xF000F0;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }
}
