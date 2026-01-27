package com.jieli.btsmart.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.jieli.btsmart.BuildConfig;

/**
 * 权限工具类
 *
 * @author zqjasonZhong
 * @date 2020/4/8
 */
public class PermissionUtil {
    /**
     * 是否具有读取位置权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean isHasLocationPermission(Context context) {
        return isHasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                || isHasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * 是否具有读写存储器权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean isHasStoragePermission(Context context) {
        return isHasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) || isHasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    /**
     * 是否具有蓝牙操作权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean hasBluetoothPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return isHasPermission(context, Manifest.permission.BLUETOOTH_SCAN) && isHasPermission(context, Manifest.permission.BLUETOOTH_CONNECT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return isHasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) || isHasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        }
        return true;
    }

    /**
     * 是否具有读写联系人列表的权限
     *
     * @param context 上下文
     * @return 结果
     */
   /* public static boolean isContactPermission(Context context) {
        return isHasPermission(context, Manifest.permission.READ_CONTACTS) && isHasPermission(context, Manifest.permission.READ_CALL_LOG);
    }*/

    /**
     * 是否具有录音的权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean isRecordPermission(Context context) {
        return isHasPermission(context, Manifest.permission.RECORD_AUDIO);
    }

    /**
     * 是否具有指定权限
     *
     * @param context    上下文
     * @param permission 权限
     *                   <p>参考{@link Manifest.permission}</p>
     * @return 结果
     */
    public static boolean isHasPermission(Context context, String permission) {
        return context != null && ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查GPS位置功能是否使能
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean checkGpsProviderEnable(Context context) {
        if (context == null) return false;
        LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locManager != null && locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * 检查指定权限是否还能弹框提示
     *
     * @param activity   显示界面
     * @param permission 指定权限
     * @return 结果
     */
    public static boolean checkPermissionShouldShowDialog(AppCompatActivity activity, String permission) {
        return permission != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * 检测是否有蓝牙连接权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean checkHasConnectPermission(Context context) {
        if (null == context) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return isHasPermission(context, Manifest.permission.BLUETOOTH_CONNECT);
        }
        return true;
    }

    /**
     * 检测是否有蓝牙搜索权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean checkHasScanPermission(Context context) {
        if (null == context) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return isHasPermission(context, Manifest.permission.BLUETOOTH_SCAN);
        }
        return true;
    }

    /**
     * 检查是否具有读取音乐文件的权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean hasReadMusicPermission(Context context) {
        if (null == context) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return isHasPermission(context, Manifest.permission.READ_MEDIA_AUDIO);
        }
        return isHasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    }
}
