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

    // /**
    //  * TODO: Attempt using HSB(O)
    //  * 
    //  * @param bottom The lower block colour values in hue sat brightness opacity
    //  * @param top The higher block colour values in hue sat brightness opacity
    //  * @param depth How many blocks are between this one and the sun
    //  * @return
    //  */
    // public static float[] filterHSBO(float[] bottom, float[] top, int depth) {
    //     // Adjust bottom brightness for light attenuation
    //     // float point = darknessPointCache.computeIfAbsent(depth, (size) -> {
    //     //     // return Math.max(0.2f, 3 - (float)Math.log(size + 1)) / 3f;
    //     //     return Math.max(0.05f, 1 - (float)Math.log(Math.log(size + 3)));
    //     // });

    //     // bottom[2] = bottom[2] * 0.75f;


    //     // Hue
    //     float hueDiff = (top[0] - bottom[0]) % 1f;
    //     // Handle the fact that hue is a circle so want to find distance on smallest edge of said circle
    //     float normalizedHueDiff = hueDiff > 0.5f ? hueDiff - 1
    //         : hueDiff < -0.5 ? hueDiff + 1
    //         : hueDiff ;
        
    //     // if ((hueDiff > 0.5f || hueDiff < -0.5f) && Math.random() > 0.95) {
    //     //     BlazeMap.LOGGER.info("== {} {} {} {} {} ==", top[0], bottom[0], hueDiff, normalizedHueDiff, ((top[0] + normalizedHueDiff * (1 - top[3])) % 1 + 1) % 1);
    //     // }

    //     top[0] = ((top[0] + normalizedHueDiff * (1 - top[3])) % 1 + 1) % 1;


    //     // Opacity
    //     // top[3] = Math.min(0.5f, 1 - ((1 - top[3]) * (1 - bottom[3])));
    //     // top[3] = Math.min(0.51f, 1 - ((1 - top[3]) * (1 - bottom[3])));
    //     top[3] = 1 - ((1 - top[3]) * (1 - bottom[3]));
    //     // top[3] = 0.25f;
    //     // top[3] = 1f;

    //     // Saturation
    //     top[1] = interpolate(bottom[1], top[1], top[3]);
    //     // top[1] = 1;

    //     // Brightness
    //     // top[2] = interpolate(bottom[2], top[2], (float)(top[3] / Math.log(depth + 1)));
    //     // top[2] = interpolate(bottom[2], top[2], (float)(1f / depth));
    //     top[2] = interpolate(bottom[2], top[2], top[3]);
    //     // top[2] = interpolate(interpolate(bottom[2], top[2], top[3]), 0.1f, Math.min(depth / 15f, 15));
    //     // top[2] = interpolate(interpolate(bottom[2], top[2], 0.5f), 0.1f, Math.min(depth * 0.25f, 1f));
    //     // top[2] = interpolate(bottom[2], interpolate(top[2], 0.1f, Math.min(depth * 0.1f, 1f)), 0.5f);

    //     return top;
    // }

    public static float getDarknessPoint(int depth) {
        return darknessPointCache.computeIfAbsent(depth, (size) -> {
            // 0.1875 == 3/16
            return Math.min(2.90f, 0.1875f * (float)Math.log(size + 1)) / 3;
            // return Math.max(0f, Math.min(0.99f, (double)Math.log(Math.log(size)) * 0.5f));
        });
    }


    /**
     * TODO: Attempt using ARGB
     * 
     * @param bottom The lower block colour values
     * @param top The higher block colour values
     * @param depth How many transparent blocks are below this one
     * @return
     */
    public static float[] filterARGB(float[] bottom, float[] top, int depth) {
        // Adjust bottom brightness for light attenuation
        float point = getDarknessPoint(depth);

        for (int i = 1; i < 4; i++) {
            // if (Math.random() > 0.999) {
            //     BlazeMap.LOGGER.info("== {} {} {} {} ==", bottom[i], point, bottom[i] * (point), bottom[i] - bottom[i] * (point));
            // }
            
            bottom[i] -= bottom[i] * (point);

            // bottom[i] = bottom[i] * (1 - Math.min(1, depth / 16));
            // top[i] = top[i] > bottom[i] ? 
            //     top[i] - (top[i] - bottom[i]) * (1 - top[0]) : 
            //     top[i] * (1 - Math.min(1, depth / 16));

            // Average the colour intensity
            top[i] = interpolate(bottom[i], top[i], top[0]);

            // Attenuate from depth
            // top[i] *= 1 - Math.min(0.995, depth / 16);

        }
        
        // // Attenuate bottom values from shadow
        // bottom[1] = bottom[1] * (1 - Math.min(1, depth / 16));
        // bottom[2] = bottom[2] * (1 - Math.min(1, depth / 16));
        // bottom[3] = bottom[3] * (1 - Math.min(1, depth / 16));

        // // Red
        // // if (top[1] > bottom[1]) {
        // //     top[1] = top[1] - (top[1] - bottom[1]) * (1 - top[0]);
        // // } else {
        // //     top[1] = top[1];
        // // }
        // top[1] = top[1] > bottom[1] ? top[1] - (top[1] - bottom[1]) * (1 - top[0]) : top[1] * (1 - Math.min(1, depth / 16));
        // // top[1] -= (top[1] - bottom[1]) * (1 - top[0]);

        // // Green
        // top[2] = top[2] > bottom[2] ? top[2] - (top[2] - bottom[2]) * (1 - top[0]) : top[2] * (1 - Math.min(1, depth / 16));
        // // top[2] -= (top[2] - bottom[2]) * (1 - top[0]);

        // // Blue
        // top[3] = top[3] > bottom[3] ? top[3] - (top[3] - bottom[3]) * (1 - top[0]) : top[3] * (1 - Math.min(1, depth / 16));
        // // top[3] -= (top[3] - bottom[3]) * (1 - top[0]);

        // Opacity
        top[0] = 1 - ((1 - top[0]) * (1 - bottom[0]));


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
