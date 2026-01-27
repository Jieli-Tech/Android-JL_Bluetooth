package com.jieli.btsmart.util;

import android.Manifest;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.jieli.bluetooth.utils.CommonUtil;

import java.io.File;

import permissions.dispatcher.PermissionUtils;

/**
 * FileUtil
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 文件工具类
 * @since 2025/7/25
 */
public class FileUtil {

    public static final String DIR_PI_HOME = "PiHome";

    public static String getDownloadFilePath(String fileName) {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + DIR_PI_HOME + File.separator + fileName;
    }

    /**
     * 判断文件是否Download文件夹
     *
     * @param context  Context 上下文
     * @param fileName String 文件名
     * @return Boolean 结果
     */
    public static boolean isFileInDownload(@NonNull Context context, @NonNull String fileName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !PermissionUtils.hasSelfPermissions(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            return false;
        }
        String selection = MediaStore.Downloads.DISPLAY_NAME + " = ?";
        String[] args = new String[]{fileName};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Cursor cursor = context.getContentResolver().query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        null,
                        selection,
                        args,
                        null
                );
                if (null == cursor) return false;
                boolean result = false;
                while (cursor.moveToFirst()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME));
                    result = fileName.equals(name);
                    if (result) break;
                }
                cursor.close();
                if (result) return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            return new File(getDownloadFilePath(fileName)).exists();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String formatFileSize(long fileSize) {
        if (fileSize < 1024) {
            return "$fileSize Bytes";
        }
        float value = fileSize / 1024f;
        if (value < 1024) {
            return CommonUtil.formatString("%.1f KB", value);
        }
        value /= 1024f;
        if (value < 1024) {
            return CommonUtil.formatString("%.1f MB", value);
        }
        value /= 1024f;
        return CommonUtil.formatString("%.1f GB", value);
    }

    public static Uri getUriByPath(@NonNull Context context, String path) {
        if (null == path || path.isEmpty()) return null;
        return getUriByFile(context, new File(path));
    }


    public static Uri getUriByFile(@NonNull Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public static boolean deleteFile(File file) {
        if (null == file || !file.exists()) return false;
        if (file.isFile()) {
            return file.delete();
        }
        File[] childFiles = file.listFiles();
        if (null == childFiles) {
            //空文件夹，直接删除
            return file.delete();
        }
        for (File child : childFiles) {
            if (!deleteFile(child)) {
                //删除文件失败
                return false;
            }
        }
        //已删除子文件，空文件夹，删除
        return file.delete();
    }
}
