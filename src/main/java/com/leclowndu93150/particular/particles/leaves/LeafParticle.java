package com.leclowndu93150.particular.particles.leaves;

import com.leclowndu93150.particular.client.AmbientSpawner;
import com.leclowndu93150.particular.client.ParticleSprites;
import com.leclowndu93150.particular.config.ParticularConfig;
import com.leclowndu93150.particular.particles.WaterRippleParticle;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Falling leaf, the 1.12.2 version of the 1.20.1 {@code LeafParticle}
 * (which extended {@code TextureSheetParticle}).
 *
 * <p>Behaviour:
 * <ul>
 *   <li>200-tick life: 10-tick fade-in, then a slow drift, then a 100-tick fade-out
 *       triggered the moment it lands or its vertical motion stalls.</li>
 *   <li>Gentle sinusoidal sway via {@link #getAngle()} (rendered as a screen-space
 *       Z roll while airborne).</li>
 *   <li>On landing it can lay flat on the ground ({@code layFlatOnGround}); on water it
 *       floats at the surface and optionally spawns a ripple.</li>
 *   <li>Random per-leaf sprite, a random horizontal mirror, and a random spin direction.</li>
 * </ul>
 * The 1.12.2 base {@link Particle} already reproduces the airborne camera-facing quad
 * plus {@code particleAngle} roll, so only the lay-flat quad and the sprite mirror need
 * a custom {@link #renderParticle}.
 */
@SideOnly(Side.CLIENT)
public class LeafParticle extends Particle {

    protected static final int FADE_OUT_DURATION = 100;

    protected float rotateFactor;
    protected float gravityFactor = 0.075f;
    protected final boolean flippedSprite;
    protected final int flippedDirection;
    protected boolean expiring = false;
    /**
     * Latched once the leaf settles on ground or water. 1.12.2 {@code posY} is the AABB
     * BOTTOM (not the centre), so a landed leaf rests with {@code posY == surface} and its
     * flat quad would Z-fight the block/water it lies on. We freeze it here and lift it a
     * hair clear of the surface, instead of letting gravity re-seat it every tick (which
     * also ate the source's small landing lift).
     */
    private boolean landed = false;

    public LeafParticle(World world, double x, double y, double z,
                        float r, float g, float b, TextureAtlasSprite sprite) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleTexture(sprite);
        this.canCollide = true;
        this.particleGravity = 0.0f;
        this.particleMaxAge = 200;
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.particleAlpha = 0.0f;
        this.particleRed = r;
        this.particleGreen = g;
        this.particleBlue = b;
        this.rotateFactor = 6.0f + (float) Math.random() * 3.0f;
        this.flippedSprite = this.rand.nextBoolean();
        this.flippedDirection = this.rand.nextBoolean() ? 1 : -1;
        // 1.20.1 quadSize 0.21875 → 1.12.2 particleScale = quadSize * 10 (render half-size 0.1*scale).
        this.particleScale = 2.1875f;
    }

    protected float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    protected float getAngle() {
        int time = this.particleAge - 10;
        float speed = this.rotateFactor;
        float amplitude = this.clamp(time, 0.0f, 30.0f) / 30.0f;
        return (float) Math.sin((float) time / speed) * amplitude * 0.5f * (float) this.flippedDirection;
    }

    @Override
    public void onUpdate() {
        // Settled: hold the lifted position EXACTLY. We must NOT call super.onUpdate() here:
        // its move(0,0,0) re-seats posY to the AABB bottom (the surface) every tick, which
        // would undo our anti-Z-fight lift and snap the leaf back coplanar with the ground.
        if (this.landed) {
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.prevParticleAngle = this.particleAngle;
            if (this.particleAge++ >= this.particleMaxAge) {
                this.setExpired();
                return;
            }
            updateAlpha();
            return;
        }

        // Base physics: prevPos, age++/expire, gravity, move, 0.98 air + 0.7 ground friction.
        super.onUpdate();
        if (this.isExpired) {
            return;
        }

        this.prevParticleAngle = this.particleAngle;

        if (this.particleAge <= 10) {
            this.motionY = 0.0;
        } else if (!this.expiring && (this.onGround || this.motionY == 0.0)) {
            startExpiring();
        }

        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        boolean inWater = this.world.getBlockState(pos).getMaterial() == Material.WATER;
        if (inWater) {
            if (this.particleGravity > 0.0f) {
                // Float just clear of the water surface so the flat quad doesn't Z-fight the
                // water plane (that coplanar overlap is what punched through the water fog).
                this.posY = pos.getY() + 0.95;
                spawnRippleIfEnabled();
            }
            if (!this.expiring) {
                startExpiring();
            }
            freeze();
        } else if (this.onGround && this.expiring) {
            // posY is the AABB bottom (= block top here). Lift clear of the surface and latch.
            this.posY += 0.04;
            freeze();
        } else if (this.particleAge >= 10) {
            this.particleGravity = this.gravityFactor;
            if (!this.onGround) {
                this.particleAngle = this.getAngle();
            }
        }

        updateAlpha();
    }

    private void startExpiring() {
        this.expiring = true;
        this.particleAge = this.particleMaxAge - FADE_OUT_DURATION;
        this.posY += 0.01;
        if (ParticularConfig.fallingLeavesLayFlatOnGround) {
            this.particleAngle = ParticularConfig.fallingLeavesLayFlatRightAngles
                    ? (float) (this.rand.nextInt(4) / 2.0 * Math.PI)
                    : (float) (Math.random() * Math.PI * 2.0);
        }
    }

    private void freeze() {
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.particleGravity = 0.0f;
        this.landed = true;
    }

    private void spawnRippleIfEnabled() {
        if (!ParticularConfig.fallingLeavesSpawnRipples) {
            return;
        }
        TextureAtlasSprite[] ripple = ParticleSprites.waterRipple();
        if (ripple != null && ripple.length > 0) {
            AmbientSpawner.add(new WaterRippleParticle(this.world, this.posX, this.posY, this.posZ, ripple));
        }
    }

    /** Fade in over the first 10 ticks, hold, then fade out over the last 100. */
    private void updateAlpha() {
        if (this.particleAge <= 10) {
            this.particleAlpha = (float) this.particleAge / 10.0f;
        } else if (this.particleAge > this.particleMaxAge - FADE_OUT_DURATION) {
            this.particleAlpha = Math.max(0.0f,
                    (float) (this.particleMaxAge - this.particleAge) / (float) FADE_OUT_DURATION);
        } else {
            this.particleAlpha = 1.0f;
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    /**
     * Render WITHOUT writing the depth buffer. 1.12.2 {@code ParticleManager.renderParticles}
     * buckets particles by {@code shouldDisableDepth()}: false → {@code depthMask(true)}.
     * Translucent terrain (water) renders AFTER particles, so a depth-writing leaf (even once
     * its alpha has faded to ~0) leaves a leaf-shaped depth value that fails the water's LEQUAL
     * test, punching an invisible hole that shows straight through the water surface. Disabling
     * the depth write (depth TEST still applies, so terrain still occludes the leaf) lets the
     * water draw over the fading leaf normally, the way vanilla rain/smoke behave on water.
     */
    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partial,
                               float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        float minU = this.particleTexture.getMinU();
        float maxU = this.particleTexture.getMaxU();
        float minV = this.particleTexture.getMinV();
        float maxV = this.particleTexture.getMaxV();
        if (this.flippedSprite) {
            float t = minU;
            minU = maxU;
            maxU = t;
        }

        float half = 0.1f * this.particleScale;
        float cx = (float) (this.prevPosX + (this.posX - this.prevPosX) * partial - interpPosX);
        float cy = (float) (this.prevPosY + (this.posY - this.prevPosY) * partial - interpPosY);
        float cz = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partial - interpPosZ);

        int light = this.getBrightnessForRender(partial);
        int sky = light >> 16 & 0xFFFF;
        int block = light & 0xFFFF;

        Vec3d[] corners;
        boolean layFlat = this.expiring && ParticularConfig.fallingLeavesLayFlatOnGround;
        if (layFlat) {
            // Horizontal quad rotated around the Y axis by particleAngle (rests on the ground).
            float angle = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partial;
            float cos = MathHelper.cos(angle);
            float sin = MathHelper.sin(angle);
            corners = new Vec3d[]{
                    flatCorner(-1, -1, half, cos, sin),
                    flatCorner(-1, 1, half, cos, sin),
                    flatCorner(1, 1, half, cos, sin),
                    flatCorner(1, -1, half, cos, sin)
            };
        } else {
            // Camera-facing quad with a screen-space Z roll, the same as the base
            // Particle billboard (see net.minecraft.client.particle.Particle.renderParticle).
            corners = new Vec3d[]{
                    new Vec3d(-rotX * half - rotXY * half, -rotZ * half, -rotYZ * half - rotXZ * half),
                    new Vec3d(-rotX * half + rotXY * half, rotZ * half, -rotYZ * half + rotXZ * half),
                    new Vec3d(rotX * half + rotXY * half, rotZ * half, rotYZ * half + rotXZ * half),
                    new Vec3d(rotX * half - rotXY * half, -rotZ * half, rotYZ * half - rotXZ * half)
            };
            if (this.particleAngle != 0.0f) {
                float a = this.particleAngle + (this.particleAngle - this.prevParticleAngle) * partial;
                float qw = MathHelper.cos(a * 0.5f);
                float s = MathHelper.sin(a * 0.5f);
                Vec3d axis = new Vec3d(s * cameraViewDir.x, s * cameraViewDir.y, s * cameraViewDir.z);
                for (int i = 0; i < 4; i++) {
                    corners[i] = axis.scale(2.0 * corners[i].dotProduct(axis))
                            .add(corners[i].scale((double) (qw * qw) - axis.dotProduct(axis)))
                            .add(axis.crossProduct(corners[i]).scale(2.0f * qw));
                }
            }
        }

        emit(buffer, corners, 0, maxU, maxV, cx, cy, cz, sky, block);
        emit(buffer, corners, 1, maxU, minV, cx, cy, cz, sky, block);
        emit(buffer, corners, 2, minU, minV, cx, cy, cz, sky, block);
        emit(buffer, corners, 3, minU, maxV, cx, cy, cz, sky, block);
    }

    private static Vec3d flatCorner(float sx, float sz, float half, float cos, float sin) {
        // Rotate (sx, 0, sz) around the Y axis, then scale to half-size.
        double rx = (sx * cos + sz * sin) * half;
        double rz = (-sx * sin + sz * cos) * half;
        return new Vec3d(rx, 0.0, rz);
    }

    private void emit(BufferBuilder buffer, Vec3d[] corners, int i, float u, float v,
                      float cx, float cy, float cz, int sky, int block) {
        buffer.pos(cx + corners[i].x, cy + corners[i].y, cz + corners[i].z)
                .tex(u, v)
                .color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha)
                .lightmap(sky, block)
                .endVertex();
    }
}
