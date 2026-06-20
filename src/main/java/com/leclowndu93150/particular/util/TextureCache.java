package com.leclowndu93150.particular.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

public class TextureCache {
    public static final Map<ResourceLocation, Data> INST = new HashMap<>();

    private TextureCache() {
    }

    public static final class Data {
        private final double[] color;

        public Data(double[] color) {
            if (color.length != 3) {
                throw new IllegalArgumentException("texture color should have 3 components");
            }
            this.color = Arrays.copyOf(color, color.length);
        }

        public double[] getColor() {
            return Arrays.copyOf(this.color, this.color.length);
        }
    }
}
