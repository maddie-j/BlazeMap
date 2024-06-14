package com.eerussianguy.blazemap.util;

import java.awt.*;
import java.util.HashMap;

public class Colors {
    public static final int NO_TINT = -1;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int DISABLED = 0x666666;
    public static final int LABEL_COLOR = 0xFF404040;
    public static final int WIDGET_BACKGROUND = 0xA0000000;
    protected static final HashMap<Integer, Float> darknessPointCache = new HashMap<Integer, Float>();

    public static int layerBlend(int bottom, int top) {
        if((top & 0xFF000000) == 0xFF000000) return top; // top is opaque, use top
        if((top & 0xFF000000) == 0) return bottom; // top is transparent, use bottom
        if((bottom & 0xFF000000) == 0) return top; // bottom is transparent, use top

        float point = ((float) (top >> 24)) / 255F;
        return 0xFF000000 | interpolate(bottom, 0, top, 1, point);
    }

    public static int interpolate(int color1, float key1, int color2, float key2, float point) {
        point = (point - key1) / (key2 - key1);
        int b0 = interpolate((color1 >> 24) & 0xFF, (color2 >> 24) & 0xFF, point);
        int b1 = interpolate((color1 >> 16) & 0xFF, (color2 >> 16) & 0xFF, point);
        int b2 = interpolate((color1 >> 8) & 0xFF, (color2 >> 8) & 0xFF, point);
        int b3 = interpolate(color1 & 0xFF, color2 & 0xFF, point);
        return b0 << 24 | b1 << 16 | b2 << 8 | b3;
    }

    public static int interpolate(int a, int b, float p) {
        a *= (1F - p);
        b *= p;
        return Math.max(0, Math.min(255, a + b));
    }

    public static float interpolate(float a, float b, float p) {
        a *= (1F - p);
        b *= p;
        return Math.max(0, Math.min(1.0f, a + b));
    }

    public static float[] filter(float[] bottom, float[] top, float opacity, int depth) {
        // Adjust bottom brightness for light attenuation
        float point = darknessPointCache.computeIfAbsent(depth, (size) -> {
            return Math.max(0.2f, 3 - (float)Math.log(size + 1)) / 3f;
        });

        bottom[2] = bottom[2] * point;

        // // Find average hue
        // top[0] = (top[0] + bottom[0]) / 2f;

        // if (top[0] > 1) {
        //     top[0] -= 1.0;
        // }

        // // Hue
        // float hueDiff = top[0] - bottom[0];
        // // Handle the fact that hue is a circle so want to find distance on smallest edge of said circle
        // float normalizedHueDiff = hueDiff > 0.5f ? hueDiff - 1
        //     : hueDiff < -0.5 ? hueDiff + 1
        //     : hueDiff ;

        // top[0] = top[0] + normalizedHueDiff * opacity;

        // if (top[0] > 1) {
        //     top[0] -= 1.0f;
        // } else if (top[0] < 0) {
        //     top[0] += 1.0f;
        // }

        // Saturation
        top[1] = interpolate(bottom[1], top[1], opacity);

        // Brightness
        top[2] = interpolate(bottom[2], top[2], opacity);

        // Opacity
        // top[3] = Math.min(0.5f, 1 - ((1 - top[3]) * (1 - bottom[3])));
        top[3] = 1 - ((1 - top[3]) * (1 - bottom[3]));

        return top;
    }

    public static int abgr(Color color) {
        return color.getAlpha() << 24 | color.getBlue() << 16 | color.getGreen() << 8 | color.getRed();
    }

    public static int abgr(int color) {
        int r = color & 0xFF0000;
        int b = color & 0x0000FF;
        color &= 0xFF00FF00;
        return color | (b << 16) | (r >> 16);
    }

    public static int randomBrightColor() {
        float hue = ((float) (System.nanoTime() / 1000) % 360) / 360F;
        return Color.HSBtoRGB(hue, 1, 1);
    }

    public static float[] decomposeRGBA(int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = ((color) & 0xFF) / 255f;
        return new float[] {a, r, g, b};
    }

    public static float[] decomposeRGB(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = ((color) & 0xFF) / 255f;
        return new float[] {r, g, b};
    }

    public static int[] decomposeIntRGBA(int color) {
        int a = (int)((color >> 24) & 0xFF);
        int r = (int)((color >> 16) & 0xFF);
        int g = (int)((color >> 8) & 0xFF);
        int b = (int)((color) & 0xFF);
        return new int[] {a, r, g, b};
    }
}
