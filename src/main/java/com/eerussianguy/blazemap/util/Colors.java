package com.eerussianguy.blazemap.util;

import java.awt.*;
import java.util.HashMap;

import com.eerussianguy.blazemap.BlazeMap;

public class Colors {
    public static final int NO_TINT = -1;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int DISABLED = 0x666666;
    public static final int LABEL_COLOR = 0xFF404040;
    public static final int WIDGET_BACKGROUND = 0xA0000000;

    public static final float OPACITY_LOW = 0.1875f; // 3/16ths
    public static final float OPACITY_HIGH = 0.85f;

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


    /**
     * This is a value to represent the lessening light that passes through semitransparent objects.
     * Will always be in the range [0 - 0.96667] (aka 0 to almost but not quite 1).
     * Higher is more shadowed.
     * 
     * @param depth How many transparent blocks the light has passed through
     */
    public static float getDarknessPoint(int depth) {
        return darknessPointCache.computeIfAbsent(depth, (size) -> {
            // return Math.min(2.90f, 0.125f * (float)Math.log(size + 1)) / 3;

            // return Math.min(0.00390625f * (depth * depth), 0.9667f);
            // return Math.min(0.015625f * (depth), 0.9667f);

            return Math.min(2.90f, 0.375f * (float)Math.log(size + 1)) / 3;

            // 0.1875 == 3/16

            // return Math.max(0f, Math.min(0.99f, (double)Math.log(Math.log(size)) * 0.5f));
        });
    }


    /**
     * TODO: Attempt using ARGB
     * 
     * @param top The higher block colour values
     * @param bottom The lower block colour values
     * @param depth How many transparent blocks are below this one
     * @return
     */
    public static float[] filterARGB(float[] top, float[] bottom, int depth) {
        // Adjust bottom brightness for light attenuation
        float point = getDarknessPoint(depth);

        for (int i = 1; i < 4; i++) {
            // if (Math.random() > 0.999) {
            //     BlazeMap.LOGGER.info("== {} {} {} {} ==", bottom[i], point, bottom[i] * (point), bottom[i] - bottom[i] * (point));
            // }

            // Attenuate from depth
            bottom[i] -= bottom[i] * (point);

            // Filter the below colour through the above colour
            bottom[i] = interpolate(bottom[i], top[i], top[0]);
        }

        // Adjust opacity
        bottom[0] = 1 - ((1 - bottom[0]) * (1 - top[0]));

        return bottom;
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

    public static int recomposeRGBA(float[] color) {
        int a = (((int)(color[0] * 255f)) & 0xFF);
        int r = (((int)(color[1] * 255f)) & 0xFF);
        int g = (((int)(color[2] * 255f)) & 0xFF);
        int b = (((int)(color[3] * 255f)) & 0xFF);
        return a << 24 | r << 16 | g << 8 | b;
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
