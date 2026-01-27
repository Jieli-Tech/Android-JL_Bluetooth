package com.jieli.btsmart.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ValueUtil;
import com.jieli.eq.EQPlotCore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : zpc18-003
 * @e-mail :
 * @date : 2020/6/5 11:42 AM
 * @desc :
 */
public class EqCacheUtil {
    private final static String KEY_EQ_VALUE = "KEY_EQ_VALUE";
    private final static String KEY_EQ_PRESET = "KEY_EQ_PRESET";
    private final static String KEY_EQ_CURRENT_VALUE = "KEY_EQ_CURRENT_VALUE";
    public final static byte[] EQ_NORMAL = new byte[10];
    public final static byte[] EQ_ROCK = new byte[]{-2, 0, 2, 4, -2, -2, 0, 0, 4, 4};
    public final static byte[] EQ_POP = new byte[]{3, 1, 0, -2, -4, -4, -2, 0, 1, 2};
    public final static byte[] EQ_CLASSICAL = new byte[]{0, 8, 8, 4, 0, 0, 0, 0, 2, 2};
    public final static byte[] EQ_JAZZ = new byte[]{0, 0, 0, 4, 4, 4, 0, 2, 3, 4};
    public final static byte[] EQ_COUNTRY = new byte[]{-2, 0, 0, 2, 2, 0, 0, 0, 4, 4};
    public final static byte[] EQ_CUSTOM = new byte[10];
    public final static byte[][] EQ_VALUES = new byte[][]{EQ_NORMAL, EQ_ROCK, EQ_POP, EQ_CLASSICAL, EQ_JAZZ, EQ_COUNTRY, EQ_CUSTOM};

    private static Gson sGson = new Gson();


    public static void saveEqValue(EqInfo eqInfo) {
        String lastValue = getEqInfoString(eqInfo);
        String currentValue = eqInfo2String(eqInfo);
        //上次的值和本次值不同时删除生成的图片且自定义不生成图片
        if (!lastValue.equals(currentValue) && eqInfo.getMode() != 6) {
            String path = generateCacheBitmapPath(eqInfo);
            FileUtil.deleteFile(new File(path));
        }

        PreferencesHelper.putStringValue(AppUtil.getContext(), getStatusKey(KEY_EQ_VALUE + eqInfo.getMode()), currentValue);
        PreferencesHelper.putStringValue(AppUtil.getContext(), getStatusKey(KEY_EQ_CURRENT_VALUE), currentValue);
    }


    public static EqInfo getCacheEqInfo(int modeIndex) {
        String eqInfoStr = PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(getStatusKey(KEY_EQ_VALUE + modeIndex), "");
        if (TextUtils.isEmpty(eqInfoStr)) {
            return new EqInfo(modeIndex, EQ_VALUES[modeIndex]);
        }

        return string2eqInfo(eqInfoStr);
    }

    public static String getEqInfoString(EqInfo eqInfo) {
        return getEqInfoString(eqInfo.getMode());
    }

    public static String getEqInfoString(int modeIndex) {
        String cache = PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(getStatusKey(KEY_EQ_VALUE + modeIndex), "");
        return cache;
    }

    public static String getEqValueBitMapPath(EqInfo eqInfo) {
        String path = generateCacheBitmapPath(eqInfo);
        JL_Log.e("sen", "path--->" + path);
        File file = new File(path);
        if (file.exists()) {
            return path;
        }
        return createBitmapAndSave(eqInfo);
    }


    public static EqInfo getCurrentCacheEqInfo() {
        String string = PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(getStatusKey(KEY_EQ_CURRENT_VALUE), "");
        if (TextUtils.isEmpty(string)) {
            return new EqInfo(0, new byte[10]);
        }
        return string2eqInfo(string);
    }


    public static EqPresetInfo getPresetEqInfo() {
        String string = PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(getStatusKey(KEY_EQ_PRESET), "");
        if (TextUtils.isEmpty(string)) {
            EqPresetInfo eqPresetInfo = new EqPresetInfo();
            eqPresetInfo.setNumber(7);
            List<EqInfo> eqInfos = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                EqInfo eqInfo = new EqInfo();
                eqInfo.setMode(i);
                eqInfo.setValue(EQ_VALUES[i].clone());
                eqInfo.setFreqs(BluetoothConstant.DEFAULT_EQ_FREQS);
                eqInfos.add(eqInfo);
            }
            eqPresetInfo.setEqInfos(eqInfos);
            return eqPresetInfo;
        }
        return sGson.fromJson(string, EqPresetInfo.class);
    }


    public static void savePresetEqInfo(EqPresetInfo eqPresetInfo) {
        String string = sGson.toJson(eqPresetInfo);
        PreferencesHelper.putStringValue(AppUtil.getContext(), getStatusKey(KEY_EQ_PRESET), string);
    }


    private static String getStatusKey(String key) {
        return key;
//        BluetoothDevice bluetoothDevice = BluetoothHelper.getInstance().getConnectedDevice();
//        if (bluetoothDevice != null) {
//            return key + "-" + bluetoothDevice.getAddress();
//        } else {
//            return key + "-" + "local";
//        }
    }

    private static String createBitmapAndSave(EqInfo eqInfo) {
        //图片绘制
        Bitmap bitmap = createBitmap(eqInfo);
        //图片保存
        String path = generateCacheBitmapPath(eqInfo);
        FileUtil.bitmapToFile(bitmap, path, 100);
        return path;

    }

    public static Bitmap createBitmap(EqInfo eqInfo) {
        int w = ValueUtil.dp2px(AppUtil.getContext(), 250);
        int h = ValueUtil.dp2px(AppUtil.getContext(), 50);
        //点计算
        int[] freqs = eqInfo.getFreqs();
        byte[] values = eqInfo.getValue();
        EQPlotCore eqPlotCore = new EQPlotCore(w, freqs.length, freqs);
        float[] pData = new float[4 * (w - 2) + 4];
        for (int i = 0; i < freqs.length && i < values.length; i++) {
            eqPlotCore.updatePara(i, freqs[i], values[i]);
            eqPlotCore.getEQPlotData(pData, i);
        }

        //点转换
        EqCovertUtil eqCovertUtil = new EqCovertUtil(w, h);
        float[] sData = eqCovertUtil.pPoint2SPoint(pData);
        //图片绘制
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setStrokeWidth(ValueUtil.dp2px(AppUtil.getContext(), 2));
        paint.setColor(AppUtil.getContext().getResources().getColor(R.color.colorAccent));
        canvas.drawLines(sData, paint);
        return bitmap;

    }


    private static String generateCacheBitmapPath(EqInfo eqInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append(AppUtil.getContext().getExternalCacheDir().getPath())
                .append(File.separator)
                .append("eq=")
                .append(CHexConver.byte2HexStr(eqInfo.getValue(), eqInfo.getValue().length));
        for (int freq : eqInfo.getFreqs()) {
            sb.append(freq);
        }
        return sb.toString();
    }


    private static String eqInfo2String(EqInfo eqInfo) {
        return sGson.toJson(eqInfo);
    }

    private static EqInfo string2eqInfo(String string) {
        JL_Log.e("sen", "eq cache value-->" + string);
        return sGson.fromJson(string, EqInfo.class);
    }


    public static void clear() {
        PreferencesHelper.remove(AppUtil.getContext(), getStatusKey(KEY_EQ_CURRENT_VALUE));
        PreferencesHelper.remove(AppUtil.getContext(), getStatusKey(KEY_EQ_PRESET));
    }

}


