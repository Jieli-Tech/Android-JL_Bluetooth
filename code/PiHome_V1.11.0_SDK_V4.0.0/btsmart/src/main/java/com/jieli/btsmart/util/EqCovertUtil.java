package com.jieli.btsmart.util;


public class EqCovertUtil {
    int w, h;
    float divX;
    float divY;
    float startY;
    float startX;

    float startFreq = 20;

    float endFreq = 22000;
    float padding = 0;//内边距

    public void setStartAndEndFreq(float startFreq, float endFreq) {
        this.startFreq = Math.max(startFreq, 20);
        this.endFreq = Math.min(endFreq, 22000);
        initData();
    }


    private void initData() {
        float xL = (float) (Math.log10(endFreq) - Math.log10(startFreq));//x轴的总长度
        divX = (w - 2 * padding) / xL;//两点之间的屏幕宽度
        startX = (float) (-Math.log10(startFreq) * divX) + padding;//减去最小范围的x偏移

        divY = (h / -24f);//两点之间的屏幕高度
        startY = -12 * divY;// 减去总增益的一半，，增益范围（-20～20）
    }


    public EqCovertUtil(int w, int h) {
        this.w = w;
        this.h = h;
        initData();
    }

    public float px2sx(float x) {
        return (float) (Math.log10(x) * divX) + startX;
    }

    public float py2sy(float y) {
        return y * divY + startY;
    }

    public float[] pPoint2SPoint(float[] data) {
        float max = 0;
        float min = 0;
        float[] result = new float[data.length];
        for (int i = 0; i < result.length; i += 2) {
            result[i] = px2sx(data[i]);
            result[i + 1] = py2sy(data[i + 1]);
            max = Math.max(max, data[i + 1]);
            min = Math.min(min, data[i + 1]);
        }
        return result;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }
}