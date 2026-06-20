package com.leclowndu93150.particular.particles.splashes;

import com.leclowndu93150.particular.config.ParticularConfig;

import net.minecraft.block.material.Material;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.Color;

/**
 * Vertical water/lava splash crown - a square ring wall extruded upward by
 * {@code height}. Renders 4 double-sided quads (8 verts each) with 9-frame
 * animation over 18 ticks. Port of 1.20.1 {@code WaterSplashParticle}.
 */
@SideOnly(Side.CLIENT)
public class WaterSplashParticle extends Particle {

    private static final Color LAVA_COLOR = new Color(207, 92, 15);

    protected float width;
    protected float height;
    protected Color color;
    protected boolean colored = true;
    protected boolean isLava;
    protected TextureAtlasSprite[] frames;

    public WaterSplashParticle(World world, double x, double y, double z,
                               float width, float height,
                               TextureAtlasSprite[] frames, boolean lava) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.particleGravity = 0.0f;
        this.particleMaxAge = 18;
        this.width = width;
        this.height = height;
        this.frames = frames;
        this.isLava = lava;
        if (frames.length > 0) {
            this.setParticleTexture(frames[0]);
        }
        // 1.12.2 biome water is far greener/teal than 1.20.1's blue, so tinting the
        // crown by the raw water colour reads as a strong green wall. Blend it
        // heavily toward white so the splash is white with only a hint of water
        // colour - matching the 1.20.1 original's look across versions.
        this.color = lava
                ? LAVA_COLOR
                : blendToWhite(new Color(BiomeColorHelper.getWaterColorAtPos(world, new BlockPos(x, y, z))), 0.7f);
    }

    /** Blend {@code c} toward white by {@code t} (0 = unchanged, 1 = white). */
    private static Color blendToWhite(Color c, float t) {
        int r = Math.round(c.getRed() + (255 - c.getRed()) * t);
        int g = Math.round(c.getGreen() + (255 - c.getGreen()) * t);
        int b = Math.round(c.getBlue() + (255 - c.getBlue()) * t);
        return new Color(r, g, b);
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

        // Animate frame
        int idx = (int) ((long) this.frames.length * this.particleAge / this.particleMaxAge);
        if (idx >= this.frames.length) {
            idx = this.frames.length - 1;
        }
        this.setParticleTexture(this.frames[idx]);

        // Die when not in matching fluid - but only after a short grace so the crown,
        // which sits at the fluid SURFACE (its base block may read as air), always plays.
        if (this.particleAge > 3) {
            BlockPos pos = new BlockPos(this.posX, this.posY - 0.2, this.posZ);
            Material material = this.world.getBlockState(pos).getMaterial();
            if (this.isLava ? material != Material.LAVA : material != Material.WATER) {
                this.setExpired();
            }
        }
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        // Lava splash is emissive so it reads against the bright lava glow.
        return this.isLava ? 0xF000F0 : super.getBrightnessForRender(partialTick);
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partial,
                               float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
        float cx = (float) (this.prevPosX + (this.posX - this.prevPosX) * partial - interpPosX);
        float cy = (float) (this.prevPosY + (this.posY - this.prevPosY) * partial - interpPosY);
        float cz = (float) (this.prevPosZ + (this.posZ - this.prevPosZ) * partial - interpPosZ);

        TextureAtlasSprite sprite = this.particleTexture;
        // The source crown art is 16px wide; it's padded into a square 32x32 sprite (art in
        // the LEFT 16 columns) so the 1.12.2 atlas stitcher accepts it as one static frame
        // instead of mis-reading a 16x32 as a 2-frame animation (which renders as the
        // missing-texture checkerboard). Map the LEFT HALF of U + full V to draw exactly the
        // original 16-wide sprite across each crown wall. (Transparent texels are colour-bled
        // so GL_LINEAR/mip sampling can't pull black in at the edges.)
        float u0 = sprite.getMinU();
        float u1 = u0 + (sprite.getMaxU() - u0) * 0.5f;
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();

        int light = this.getBrightnessForRender(partial);
        int sky = light >> 16 & 0xFFFF;
        int block = light & 0xFFFF;

        // Age-based progress and scale - faithful to the 1.20.1 original so the
        // crown wall encloses the entity exactly like the source mod (scale tracks
        // the entity's bounding-box width; bigger mob → wider crown).
        float ageDelta = partial + this.particleAge - 1;
        float progress = (this.particleAge > 0) ? ageDelta / (float) this.particleMaxAge : 0.0f;
        float scale = this.width * (0.8f + 0.2f * progress);

        // Four base corners (XZ plane) scaled and offset
        float x0 = cx - scale; // corner 0: (-1,0,-1)
        float z0 = cz - scale;
        float x1 = cx - scale; // corner 1: (-1,0, 1)
        float z1 = cz + scale;
        float x2 = cx + scale; // corner 2: ( 1,0, 1)
        float z2 = cz + scale;
        float x3 = cx + scale; // corner 3: ( 1,0,-1)
        float z3 = cz - scale;

        // Wall height = full crown height (speed/2 + width/3), faithful to original.
        float topY = cy + this.height;

        // Color
        int rgb = this.colored ? this.color.getRGB() : Color.white.getRGB();
        float r = (float) (rgb >> 16 & 0xFF) / 255.0f;
        float g = (float) (rgb >> 8 & 0xFF) / 255.0f;
        float b = (float) (rgb & 0xFF) / 255.0f;
        float a = (float) Math.max(0.0, Math.min(1.0, ParticularConfig.waterSplashOpacity));

        // Render 4 edges as double-sided vertical quads (8 verts each)
        renderSide(buffer, x0, cy, z0, x1, cy, z1, topY, u0, u1, v0, v1, r, g, b, a, sky, block);
        renderSide(buffer, x1, cy, z1, x2, cy, z2, topY, u0, u1, v0, v1, r, g, b, a, sky, block);
        renderSide(buffer, x2, cy, z2, x3, cy, z3, topY, u0, u1, v0, v1, r, g, b, a, sky, block);
        renderSide(buffer, x3, cy, z3, x0, cy, z0, topY, u0, u1, v0, v1, r, g, b, a, sky, block);
    }

    /**
     * Emit one double-sided vertical quad (8 vertices) for the splash crown wall.
     * Ports 1.20 {@code renderSide} - two passes (front + back) for visibility
     * from both sides.
     */
    private void renderSide(BufferBuilder b,
            float ax, float ay, float az, float bx, float by, float bz,
            float topY,
            float u0, float u1, float v0, float v1,
            float r, float g, float B, float a, int sky, int block) {
        // Front face
        b.pos(ax, ay,    az).tex(u0, v1).color(r, g, B, a).lightmap(sky, block).endVertex();
        b.pos(bx, by,    bz).tex(u1, v1).color(r, g, B, a).lightmap(sky, block).endVertex();
        b.pos(bx, topY,  bz).tex(u1, v0).color(r, g, B, a).lightmap(sky, block).endVertex();
        b.pos(ax, topY,  az).tex(u0, v0).color(r, g, B, a).lightmap(sky, block).endVertex();
        // Back face (reverse winding)
        b.pos(bx, by,    bz).tex(u1, v1).color(r, g, B, a).lightmap(sky, block).endVertex();
        b.pos(ax, ay,    az).tex(u0, v1).color(r, g, B, a).lightmap(sky, block).endVertex();
        b.pos(ax, topY,  az).tex(u0, v0).color(r, g, B, a).lightmap(sky, block).endVertex();
        b.pos(bx, topY,  bz).tex(u1, v0).color(r, g, B, a).lightmap(sky, block).endVertex();
    }
}
