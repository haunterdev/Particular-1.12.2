package com.leclowndu93150.particular.util;

import java.util.Locale;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Immutable RGBA color utility. Ported from 1.20.1 OwOColor, adapted for JDK 8 and 1.12.2.
 *
 * <p>1.20 {@code record} → JDK 8 regular class with {@code final} fields.
 * {@code ChatFormatting} → {@link TextFormatting}.
 * {@code Mth.hsvToRgb} / {@code Mth.lerp} → inlined helpers (absent from 1.12.2 MathHelper).
 */
@SideOnly(Side.CLIENT)
public class OwOColor {

    public static final OwOColor BLACK = ofRgb(0x000000);
    public static final OwOColor WHITE = ofRgb(0xFFFFFF);
    public static final OwOColor RED = ofRgb(0xFF0000);
    public static final OwOColor GREEN = ofRgb(0x00FF00);
    public static final OwOColor BLUE = ofRgb(0x0000FF);

    /** Standard Minecraft text color codes, keyed by friendly-name-to-lower-hyphen. */
    // Hardcoded because TextFormatting (1.12.2) has no public getColor().
    // Keys match the original's format: name().toLowerCase().replace('_', '-')
    private static final java.util.Map<String, OwOColor> NAMED_TEXT_COLORS = buildNamedColors();

    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public OwOColor(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public OwOColor(float red, float green, float blue) {
        this(red, green, blue, 1.0f);
    }

    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public float alpha() { return alpha; }

    public static OwOColor ofArgb(int argb) {
        return new OwOColor(
                (float) (argb >> 16 & 0xFF) / 255.0f,
                (float) (argb >> 8 & 0xFF) / 255.0f,
                (float) (argb & 0xFF) / 255.0f,
                (float) (argb >>> 24) / 255.0f);
    }

    public static OwOColor ofRgb(int rgb) {
        return new OwOColor(
                (float) (rgb >> 16 & 0xFF) / 255.0f,
                (float) (rgb >> 8 & 0xFF) / 255.0f,
                (float) (rgb & 0xFF) / 255.0f,
                1.0f);
    }

    public static OwOColor ofHsv(float hue, float saturation, float value) {
        return ofRgb(hsvToRgb(hue - 5.0E-8f, saturation, value));
    }

    public static OwOColor ofHsv(float hue, float saturation, float value, float alpha) {
        return ofArgb(((int) (alpha * 255.0f) << 24) | hsvToRgb(hue - 5.0E-8f, saturation, value));
    }

    /**
     * Stub - TextFormatting in 1.12.2 has no public {@code getColor()}.
     * Always returns BLACK. Kept so the class compiles and any call sites degrade gracefully.
     */
    public static OwOColor ofFormatting(TextFormatting formatting) {
        return BLACK;
    }

    public static OwOColor random() {
        return ofArgb((int) (Math.random() * 1.6777215E7) | 0xFF000000);
    }

    public int rgb() {
        return ((int) (this.red * 255.0f)) << 16
             | ((int) (this.green * 255.0f)) << 8
             | ((int) (this.blue * 255.0f));
    }

    public int argb() {
        return ((int) (this.alpha * 255.0f)) << 24
             | ((int) (this.red * 255.0f)) << 16
             | ((int) (this.green * 255.0f)) << 8
             | ((int) (this.blue * 255.0f));
    }

    public float[] hsv() {
        float cmax = Math.max(Math.max(this.red, this.green), this.blue);
        float cmin = Math.min(Math.min(this.red, this.green), this.blue);
        float saturation = cmax != 0.0f ? (cmax - cmin) / cmax : 0.0f;
        float hue;
        if (saturation == 0.0f) {
            hue = 0.0f;
        } else {
            float redc = (cmax - this.red) / (cmax - cmin);
            float greenc = (cmax - this.green) / (cmax - cmin);
            float bluec = (cmax - this.blue) / (cmax - cmin);
            if (this.red == cmax) {
                hue = bluec - greenc;
            } else if (this.green == cmax) {
                hue = 2.0f + redc - bluec;
            } else {
                hue = 4.0f + greenc - redc;
            }
            hue /= 6.0f;
            if (hue < 0.0f) hue += 1.0f;
        }
        return new float[] { hue, saturation, cmax, this.alpha };
    }

    public String asHexString(boolean includeAlpha) {
        return includeAlpha ? String.format("#%08X", argb()) : String.format("#%06X", rgb());
    }

    public OwOColor interpolate(OwOColor next, float delta) {
        return new OwOColor(
                lerp(delta, this.red, next.red),
                lerp(delta, this.green, next.green),
                lerp(delta, this.blue, next.blue),
                lerp(delta, this.alpha, next.alpha));
    }

    public static OwOColor fromHexString(String hexString) {
        if (!hexString.startsWith("#")) {
            OwOColor color = NAMED_TEXT_COLORS.get(hexString);
            if (color != null) {
                return color;
            }
            throw new IllegalArgumentException(
                    "Invalid color value '" + hexString
                    + "', expected hex color of format #RRGGBB or #AARRGGBB or named text color");
        }
        if (hexString.matches("#([A-Fa-f\\d]{2}){3,4}")) {
            return hexString.length() == 7
                    ? ofRgb(Integer.parseUnsignedInt(hexString.substring(1), 16))
                    : ofArgb(Integer.parseUnsignedInt(hexString.substring(1), 16));
        }
        throw new IllegalArgumentException(
                "Invalid color value '" + hexString
                + "', expected hex color of format #RRGGBB or #AARRGGBB or named text color");
    }

    /* ---- Internal helpers (absent from 1.12.2 MathHelper) ---- */

    /**
     * Convert HSV to packed RGB integer. Replaces {@code Mth.hsvToRgb} (1.20→1.12.2).
     * Implementation mirrors the vanilla algorithm.
     */
    private static int hsvToRgb(float hue, float saturation, float value) {
        int i = (int) (hue * 6.0f) % 6;
        float f = hue * 6.0f - (float) i;
        float f1 = value * (1.0f - saturation);
        float f2 = value * (1.0f - f * saturation);
        float f3 = value * (1.0f - (1.0f - f) * saturation);
        float r, g, b;
        switch (i) {
            case 0: r = value; g = f3;   b = f1;   break;
            case 1: r = f2;   g = value; b = f1;   break;
            case 2: r = f1;   g = value; b = f3;   break;
            case 3: r = f1;   g = f2;   b = value; break;
            case 4: r = f3;   g = f1;   b = value; break;
            case 5: r = value; g = f1;   b = f2;   break;
            default: throw new IllegalStateException("unreachable");
        }
        return ((int) (r * 255.0f)) << 16
             | ((int) (g * 255.0f)) << 8
             | ((int) (b * 255.0f));
    }

    private static float lerp(float delta, float from, float to) {
        return from + delta * (to - from);
    }

    private static java.util.Map<String, OwOColor> buildNamedColors() {
        java.util.Map<String, OwOColor> map = new java.util.HashMap<>();
        // Standard Minecraft text color codes (matched against TextFormatting.getName().toLowerCase().replace('_','-'))
        map.put("black",        ofRgb(0x000000));
        map.put("dark-blue",    ofRgb(0x0000AA));
        map.put("dark-green",   ofRgb(0x00AA00));
        map.put("dark-aqua",    ofRgb(0x00AAAA));
        map.put("dark-red",     ofRgb(0xAA0000));
        map.put("dark-purple",  ofRgb(0xAA00AA));
        map.put("gold",         ofRgb(0xFFAA00));
        map.put("gray",         ofRgb(0xAAAAAA));
        map.put("dark-gray",    ofRgb(0x555555));
        map.put("blue",         ofRgb(0x5555FF));
        map.put("green",        ofRgb(0x55FF55));
        map.put("aqua",         ofRgb(0x55FFFF));
        map.put("red",          ofRgb(0xFF5555));
        map.put("light-purple", ofRgb(0xFF55FF));
        map.put("yellow",       ofRgb(0xFFFF55));
        map.put("white",        ofRgb(0xFFFFFF));
        return java.util.Collections.unmodifiableMap(map);
    }
}
