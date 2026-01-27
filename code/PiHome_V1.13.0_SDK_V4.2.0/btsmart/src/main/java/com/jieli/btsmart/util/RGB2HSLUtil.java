package com.jieli.btsmart.util;

import android.graphics.Color;

import java.util.Arrays;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/9/11 9:43
 * @desc :http://blog.sina.com.cn/s/blog_4c78d3fb0101m36f.html
 */
public class RGB2HSLUtil {
    public static float min3Value(float value1, float value2, float value3) {
        return ((value1) > (value2) ? (Math.min(value3, value2)) : (Math.min(value3, value1)));
    }

    public static float max3Value(float value1, float value2, float value3) {
        return ((value1) < (value2) ? (Math.max(value2, value3)) : (Math.max(value1, value3)));
    }

    public static ColorHSL RGBtoHSL(int color) {
        ColorRGB colorRGB = new ColorRGB();
        colorRGB.setRed(Color.red(color));
        colorRGB.setGreen(Color.green(color));
        colorRGB.setBlue(Color.blue(color));
        ColorHSL colorHSL = RGB2HSLUtil.RGBtoHSL(colorRGB);
        return colorHSL;
    }

    public static ColorHSL RGBtoHSL(ColorRGB colorRGB) {
        float h = 0, s = 0, l = 0;
        // normalizes red-green-blue values
        float r = colorRGB.getRed() / 255.0f;
        float g = colorRGB.getGreen() / 255.0f;
        float b = colorRGB.getBlue() / 255.0f;
        float maxVal = max3Value(r, g, b);
        float minVal = min3Value(r, g, b);

        // hue
        if (maxVal == minVal) {
            h = 0; // undefined
        } else if (maxVal == r && g >= b) {
            h = 60.0f * (g - b) / (maxVal - minVal);
        } else if (maxVal == r && g < b) {
            h = 60.0f * (g - b) / (maxVal - minVal) + 360.0f;
        } else if (maxVal == g) {
            float test1 = 60.0f * (b - r) / (maxVal - minVal);
            h = test1 + 120.0f;
        } else if (maxVal == b) {
            float test1 = 60.0f * (r - g) / (maxVal - minVal);
            h = test1 + 240.0f;
        }

        // luminance
        l = (maxVal + minVal) / 2.0f;

        // saturation
        if (l == 0 || maxVal == minVal) {
            s = 0;
        } else if (0 < l && l <= 0.5f) {
            s = (maxVal - minVal) / (maxVal + minVal);
        } else if (l > 0.5f) {
            s = (maxVal - minVal) / (2 - (maxVal + minVal)); //(maxVal-minVal > 0)?
        }

        float finalH = (h > 360) ? 360 : ((h < 0) ? 0 : h);
        float finalS = ((s > 1) ? 1 : ((s < 0) ? 0 : s)) * 100;
        float finalL = ((l > 1) ? 1 : ((l < 0) ? 0 : l)) * 100;
        ColorHSL colorHSL = new ColorHSL();
        colorHSL.setHue(finalH);
        colorHSL.setSaturation(finalS);
        colorHSL.setLuminance(finalL);
        return colorHSL;
    }

    public static ColorRGB HSLtoRGB(ColorHSL colorHSL) {
        float h = colorHSL.getHue();                  // h must be [0, 360]
        float s = colorHSL.getSaturation() / 100.f; // s must be [0, 1]
        float l = colorHSL.getLuminance() / 100.f;      // l must be [0, 1]
        float R, G, B;
        if (colorHSL.getSaturation() == 0) {
            // achromatic color (gray scale)
            R = G = B = l * 255.0f;
        } else {
            float q = (l < 0.5f) ? (l * (1.0f + s)) : (l + s - (l * s));
            float p = (2.0f * l) - q;
            float Hk = h / 360.0f;
            float[] T = new float[3];
            T[0] = Hk + 0.3333333f; // Tr   0.3333333f=1.0/3.0
            T[1] = Hk;              // Tb
            T[2] = Hk - 0.3333333f; // Tg
            for (int i = 0; i < 3; i++) {
                if (T[i] < 0) T[i] += 1.0f;
                if (T[i] > 1) T[i] -= 1.0f;
                if ((T[i] * 6) < 1) {
                    T[i] = p + ((q - p) * 6.0f * T[i]);
                } else if ((T[i] * 2.0f) < 1) //(1.0/6.0)<=T[i] && T[i]<0.5
                {
                    T[i] = q;
                } else if ((T[i] * 3.0f) < 2) // 0.5<=T[i] && T[i]<(2.0/3.0)
                {
                    T[i] = p + (q - p) * ((2.0f / 3.0f) - T[i]) * 6.0f;
                } else T[i] = p;
            }
            R = T[0] * 255.0f;
            G = T[1] * 255.0f;
            B = T[2] * 255.0f;
        }

        float finalR = (int) ((R > 255) ? 255 : ((R < 0) ? 0 : R));
        float finalG = (int) ((G > 255) ? 255 : ((G < 0) ? 0 : G));
        float finalB = (int) ((B > 255) ? 255 : ((B < 0) ? 0 : B));
        ColorRGB colorRGB = new ColorRGB();
        colorRGB.setRed((int) finalR);
        colorRGB.setGreen((int) finalG);
        colorRGB.setBlue((int) finalB);
        return colorRGB;
    }

    public static ColorHSB RGBtoHSB(int color) {
        float[] hsv = {0, 0, 1};
        Color.colorToHSV(color, hsv);
        ColorHSB colorHSB = new ColorHSB();
        colorHSB.setHue(hsv[0]);
        colorHSB.setSaturation(hsv[1] * 100f);
        colorHSB.setBrightness(hsv[2] * 100f);
        return colorHSB;
    }

    public static ColorHSB RGBtoHSB(ColorRGB colorRGB) {
        int color = Color.rgb(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue());
        return RGBtoHSB(color);
    }

    public static ColorRGB HSBtoRGB(ColorHSB colorHSB) {
        float[] hsv = {0, 0, 1};
        hsv[0] = colorHSB.getHue();
        hsv[1] = colorHSB.getSaturation() / 100.f;
        hsv[2] = colorHSB.getBrightness() / 100.f;
        int color = Color.HSVToColor(hsv);
        ColorRGB colorRGB = new ColorRGB();
        colorRGB.setRed(Color.red(color));
        colorRGB.setGreen(Color.green(color));
        colorRGB.setBlue(Color.blue(color));
        return colorRGB;
    }

    public static ColorHSB rgb2hsb(ColorRGB colorRGB) {
        int rgbR = colorRGB.getRed();
        int rgbG = colorRGB.getGreen();
        int rgbB = colorRGB.getBlue();
        assert 0 <= rgbR && rgbR <= 255;
        assert 0 <= rgbG && rgbG <= 255;
        assert 0 <= rgbB && rgbB <= 255;
        int[] rgb = new int[]{rgbR, rgbG, rgbB};
        Arrays.sort(rgb);
        int max = rgb[2];
        int min = rgb[0];

        float hsbB = max / 255.0f;
        float hsbS = max == 0 ? 0 : (max - min) / (float) max;

        float hsbH = 0;
        if (max == rgbR && rgbG >= rgbB) {
            hsbH = (rgbG - rgbB) * 60f / (max - min) + 0;
        } else if (max == rgbR && rgbG < rgbB) {
            hsbH = (rgbG - rgbB) * 60f / (max - min) + 360;
        } else if (max == rgbG) {
            hsbH = (rgbB - rgbR) * 60f / (max - min) + 120;
        } else if (max == rgbB) {
            hsbH = (rgbR - rgbG) * 60f / (max - min) + 240;
        }
        ColorHSB colorHSB = new ColorHSB(hsbH, hsbS, hsbB);
        return colorHSB;
    }

    public static ColorRGB hsb2rgb(ColorHSB colorHSB) {
        float h = colorHSB.getHue();
        float s = colorHSB.getSaturation();
        float v = colorHSB.getBrightness();
        assert Float.compare(h, 0.0f) >= 0 && Float.compare(h, 360.0f) <= 0;
        assert Float.compare(s, 0.0f) >= 0 && Float.compare(s, 1.0f) <= 0;
        assert Float.compare(v, 0.0f) >= 0 && Float.compare(v, 1.0f) <= 0;

        float r = 0, g = 0, b = 0;
        int i = (int) ((h / 60) % 6);
        float f = (h / 60) - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        switch (i) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            case 5:
                r = v;
                g = p;
                b = q;
                break;
            default:
                break;
        }
        ColorRGB colorRGB = new ColorRGB();
        colorRGB.setRed((int) (r * 255.0));
        colorRGB.setGreen((int) (g * 255.0));
        colorRGB.setBlue((int) (b * 255.0));
        return colorRGB;
    }

    public static boolean checkIsTendToWhite(int color, int luminanceLimit) {
        ColorHSL colorHSL = RGB2HSLUtil.RGBtoHSL(color);
        boolean isTendToWhite;
        if (colorHSL.getLuminance() > luminanceLimit || color == 0) {
            isTendToWhite = true;
        } else {
            isTendToWhite = false;
        }
        return isTendToWhite;
    }
}

