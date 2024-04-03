package com.jieli.btsmart.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.component.utils.PreferencesHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by chensenhua on 2018/5/25.
 */

public class AppUtil {

    private static String tag = "AppUtil";

    public static float[] getScreenResolution(Context context) {
        if (context == null) return null;
        float[] screenResolution = new float[3];
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return new float[]{0, 0, 0};
        DisplayMetrics dm = new DisplayMetrics();
        Display display = wm.getDefaultDisplay();
        display.getMetrics(dm);
        screenResolution[0] = dm.widthPixels;
        screenResolution[1] = dm.heightPixels;
        screenResolution[2] = dm.density;
        return screenResolution;
    }

    public static int getScreenWidth(Context context) {
        if (context == null) return 0;
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight(Context context) {
        if (context == null) return 0;
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static float getScreenDensity(Context context) {
        if (context == null) return 0;
        return context.getResources().getDisplayMetrics().density;
    }

    /*public static List<Music> convertJl_musicToMusic(List<JL_Music> data) {
        return convertJl_musicToMusic(data, 1);
    }

    public static List<Music> convertJl_musicToMusic(List<JL_Music> data, int local) {
        List<Music> result = new ArrayList<>();
        if (data == null || data.size() < 1) {
            return result;
        }
        for (JL_Music jl_music : data) {
            Music music = new Music();
            music.setCoverUrl(jl_music.getCoverUrl());
            music.setLocal(local);
            music.setTitle(jl_music.getTitle());
            music.setUrl(jl_music.getUrl());
            music.setId(ValueUtil.stringNumToNum(jl_music.getId()));
            music.setDuration(jl_music.getDuration());
            music.setArtist(jl_music.getArtist());
            music.setAlbum(jl_music.getAlbum());
            result.add(music);
        }
        return result;
    }*/


    public static int getIndexOfFirstNotEmptyUrl(List<Music> music) {
        int index = 0;
        if (music == null) {
            return index;
        }
        for (int i = 0; i < music.size(); i++) {
            if (!TextUtils.isEmpty(music.get(i).getUrl())) {
                index = i;
                break;
            }
        }
        return index;
    }


    public static void savePcmToWav(String input, String output) throws IOException {
        FileInputStream fis = new FileInputStream(input);
        FileOutputStream fos = new FileOutputStream(output);

        byte[] header = new byte[44];
        int size = fis.available() + 36;


        //资源交换文件标志（RIFF）
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';


        //从下个地址开始到文件尾的总字节数
        header[4] = (byte) (size & 0xff);
        header[5] = (byte) ((size >> 8) & 0xff);
        header[6] = (byte) ((size >> 16) & 0xff);
        header[7] = (byte) ((size >> 24) & 0xff);


        //WAV文件标志（WAVE）
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';

        //波形格式标志（fmt ），最后一位空格。
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';

        //过滤字节（一般为00000010H），若为00000012H则说明数据头携带附加信息（见“附加信息”）。
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;

        //format
        header[20] = 1;
        header[21] = 0;


        //channel
        header[22] = 1;
        header[23] = 0;

        int longSampleRate = 16000;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);

        int channels = 1;
        int byteRate = 16 * channels * longSampleRate / 8;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);


        int bitPerSample = 16;
        int blockAlign = channels * bitPerSample / 8;
        header[32] = (byte) (blockAlign & 0xff);
        header[33] = (byte) ((blockAlign >> 8) & 0xff);

        header[34] = (byte) (bitPerSample & 0xff);
        header[35] = (byte) ((bitPerSample >> 8) & 0xff);


        //data
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';


        size = size - 36;
        header[40] = (byte) (size & 0xff);
        header[41] = (byte) ((size >> 8) & 0xff);
        header[42] = (byte) ((size >> 16) & 0xff);
        header[43] = (byte) ((size >> 24) & 0xff);

        fos.write(header, 0, header.length);
        byte[] temp = new byte[fis.available()];
        //noinspection ResultOfMethodCallIgnored
        fis.read(temp);
        fos.write(temp);
        fos.flush();

    }

    private static long lastClickTime = 0;
    private final static long DOUBLE_CLICK_INTERVAL = 2000; //2 s

    public static boolean isFastDoubleClick() {
        return isFastDoubleClick(DOUBLE_CLICK_INTERVAL);
    }

    public static boolean isFastDoubleClick(long interval) {
        boolean isDoubleClick = false;
        long currentTime = new Date().getTime();
        if (currentTime - lastClickTime <= interval) {
            isDoubleClick = true;
        }
        lastClickTime = currentTime;
        return isDoubleClick;
    }


    public static Class<?> getConnectActivityClass() {
        return HomeActivity.class;
    }

    public static Context getContext() {
        return MainApplication.getApplication();
    }

    public static String getTextFromAssets(Context context, String fileName) {
        if (context == null || fileName == null) return null;
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(fileName);

            int size = inputStream.available();
            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            int len = inputStream.read(buffer);
//            JL_Log.i("zzc", "getTextFromAssets : " + len);
            // Convert the buffer into a string.
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 开启定时任务
     *
     * @param context 上下文
     * @param seconds 重复间隔（单位为秒）
     * @param intent  执行广播
     */
    public static void startTimerTask(Context context, int seconds, PendingIntent intent) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        int type = AlarmManager.RTC_WAKEUP;
        long triggerAtTime = System.currentTimeMillis();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setExactAndAllowWhileIdle(type, triggerAtTime, intent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            manager.setExact(type, triggerAtTime, intent);
        } else {
            manager.setRepeating(type, triggerAtTime, (long) seconds * 1000, intent);
        }
    }

    /**
     * 关闭定时任务
     *
     * @param context 上下文
     * @param intent  执行广播
     */
    public static void stopTimerTask(Context context, PendingIntent intent) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        manager.cancel(intent);
    }

    public static String freqValueToFreqShowText(int value) {
        int qian = value / 1000;
        if (qian > 0) {
            return qian + "K";
        } else {
            return value + "";
        }

    }

    public static Map<String, Boolean> deviceSupportSearchStatus = new HashMap<>();
    private static final String DEVICE_SUPPORT_KEY = "DeviceSupportSearchStatus";

    public static void getAllDeviceSupportSearchStatus() {
        String mapString = PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(DEVICE_SUPPORT_KEY, null);
        if (mapString == null) return;
        mapString = mapString.replace("{", "");
        mapString = mapString.replace("}", "");
        String[] arrays = mapString.split(", ");
        if (arrays.length <= 0) return;
        for (String str : arrays) {
            String[] keyAndValue = str.split("=");
            if (keyAndValue.length <= 1) break;
            deviceSupportSearchStatus.put(keyAndValue[0], Boolean.valueOf(keyAndValue[1]));
        }
//        deviceSupportSearchStatus.
    }

    /**
     * @param address   设备的物理地址
     * @param isSupport 是否支持
     * @desc 保存设备是否支持设备查找状态
     */
    public static void saveDeviceSupportSearchStatus(String address, boolean isSupport) {
        deviceSupportSearchStatus.put(address, isSupport);
        PreferencesHelper.putStringValue(AppUtil.getContext(), DEVICE_SUPPORT_KEY, deviceSupportSearchStatus.toString());
//        PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(DEVICE_SUPPORT_KEY, null);
    }

    public static void deleteDeviceSupportSearchStatus(String address) {
        deviceSupportSearchStatus.remove(address);
        PreferencesHelper.putStringValue(AppUtil.getContext(), DEVICE_SUPPORT_KEY, deviceSupportSearchStatus.toString());
    }

    /**
     * @param address 设备的物理地址
     * @desc 检查设备是否支持设备查找
     */
    public static boolean checkDeviceIsSupportSearch(String address) {
        Boolean result = deviceSupportSearchStatus.get(address);
        boolean ret = result == null ? false : result;
        if (!ret) {
            HistoryBluetoothDevice history = RCSPController.getInstance().findHistoryBluetoothDevice(address);
            if (history != null) {
                ret = (history.getLeftDevLatitude() != 0 && history.getLeftDevLongitude() != 0) ||
                        (history.getRightDevLatitude() != 0 && history.getRightDevLongitude() != 0);
            }
        }
        return ret;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    public static long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public static String getTimeFormatValue(int time) {
        int hour = time / 60 / 60 % 24;
        int min = time / 60 % 60;
        int sec = time % 60;
        if (hour == 0) {
            return MessageFormat.format("{0,number,00}:{1,number,00}", min, sec);
        } else {
            return MessageFormat.format("{0,number,00}:{1,number,00}:{2,number,00}", hour, min, sec);
        }
    }

    public static String formatWatchBgSeq(int seq) {
        if (seq < 0) {
            seq = 0;
        }
        if (seq >= 1000) {
            seq = 999;
        }
        char[] values = new char[]{'0', '0', '0'};
        char[] chars = String.valueOf(seq).toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            values[values.length - i - 1] = c;
        }
        return new String(values);
    }

    private static int mCurrentFittingGainType = -2;

    public static int getCurrentFittingGainType() {
        return mCurrentFittingGainType;
    }

    public static void setCurrentFittingGainType(int currentFittingGainType) {
        mCurrentFittingGainType = currentFittingGainType;
    }

    /**
     * 获取指定文件类型的路径
     *
     * @param dirPath 目录路径
     * @param suffix  文件后续
     * @return 文件路径
     */
    public static String obtainUpdateFilePath(String dirPath, String suffix) {
        if (null == dirPath) return null;
        File dir = new File(dirPath);
        if (!dir.exists()) return null;
        if (dir.isFile()) {
            if (dirPath.endsWith(suffix)) {
                return dirPath;
            } else {
                return null;
            }
        } else if (dir.isDirectory()) {
            String filePath = null;
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    filePath = obtainUpdateFilePath(file.getPath(), suffix);
                    if (filePath != null) {
                        break;
                    }
                }
            }
            return filePath;
        }
        return null;
    }

    /**
     * 获取蓝牙适配器名称
     *
     * @param context 上下文
     * @return 蓝牙适配器名称
     */
    public static String getBtName(Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (null == adapter || !PermissionUtil.checkHasConnectPermission(context)) return null;
        return adapter.getName();
    }
}
