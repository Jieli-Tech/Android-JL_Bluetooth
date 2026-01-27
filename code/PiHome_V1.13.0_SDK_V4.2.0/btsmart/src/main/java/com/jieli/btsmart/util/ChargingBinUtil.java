package com.jieli.btsmart.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.CryptoUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.chargingcase.ResourceMsg;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.ui.chargingCase.ConfirmScreenSaversFragment;
import com.jieli.btsmart.ui.chargingCase.ResourceFileAdapter;
import com.jieli.btsmart.ui.chargingCase.UploadResourceFragment;
import com.jieli.component.utils.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ChargingBinUtil
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 彩屏充电仓工具类
 * @since 2025/2/10
 */
public class ChargingBinUtil {

    /**
     * 是否GIF文件
     *
     * @param filePath String 文件路径
     * @return boolean 结果
     */
    public static boolean isGif(String filePath) {
        if (TextUtils.isEmpty(filePath)) return false;
        return filePath.endsWith(".gif") || filePath.endsWith(".GIF");
    }

    /**
     * 从文件路径中获取文件名
     *
     * @param filePath String 文件路径
     * @return String 文件名
     */
    public static String getFileNameByPath(String filePath) {
        return getFileNameByPath(filePath, false);
    }

    /**
     * 从文件路径中获取文件名
     *
     * @param filePath     String 文件路径
     * @param isSkipSuffix boolean 是否跳过后缀名
     * @return String 文件名
     */
    public static String getFileNameByPath(String filePath, boolean isSkipSuffix) {
        if (TextUtils.isEmpty(filePath)) return "";
        int index = filePath.lastIndexOf(File.separator);
        if (index != -1 || index < filePath.length() - 1) {
            int end = filePath.lastIndexOf(".");
            if (isSkipSuffix && end != -1 && end > index) {
                return filePath.substring(index + 1, end);
            }
            return filePath.substring(index + 1);
        }
        return filePath;
    }

    public static String getFolderPathByPath(String filePath) {
        if (TextUtils.isEmpty(filePath)) return filePath;
        int index = filePath.lastIndexOf(File.separator);
        if (index == -1 || index == filePath.length() - 1) return filePath;
        return filePath.substring(0, index);
    }

    @NonNull
    public static String formatFileName(@NonNull String filePath) {
        String filename = getFileNameByPath(filePath);
        if (filename.contains("-")) {
            String suffix = getFileSuffix(filename);
            String content = getNameNoSuffix(filename);
            String[] array = content.split("-");
            if (array.length > 1) {
                if (!suffix.isEmpty()) {
                    return array[0] + "." + suffix;
                } else {
                    return array[0];
                }
            }
        }
        return filename;
    }

    public static String getNameNoSuffix(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1) return name;
        return name.substring(0, index);
    }

    public static String getFileSuffix(String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1) return "";
        return filename.substring(index + 1);
    }

    /**
     * 读取文件列表
     *
     * @param dirPath String 文件夹路径
     * @param list    List\<File\> 输出文件列表
     */
    public static void readFileByDir(@NonNull String dirPath, @NonNull List<File> list) {
        readFileByDir(dirPath, "", list);
    }

    /**
     * 读取文件列表
     *
     * @param dirPath   String 文件夹路径
     * @param ignoreDir String 忽略文件夹名
     * @param list      List\<File\> 输出文件列表
     */
    public static void readFileByDir(@NonNull String dirPath, @NonNull String ignoreDir, @NonNull List<File> list) {
        File dir = new File(dirPath);
        if (!dir.exists()) return;
        if (dir.isFile()) {
            addFileBySort(dir, list);
            return;
        }
        File[] files = dir.listFiles();
        if (null == files || files.length == 0) return;
        Arrays.sort(files, (o1, o2) -> o1.getName().compareTo(o2.getName()));
        for (File file : files) {
            if (file.isFile()) {
                addFileBySort(file, list);
            } else {
                if (file.getName().equalsIgnoreCase(ignoreDir) || isOutputDir(file.getPath()))
                    continue;
                readFileByDir(file.getPath(), ignoreDir, list);
            }
        }
    }

    /**
     * 复制assets资源
     *
     * @param context Context 上下文
     * @param oldPath String assets路径
     * @param newPath String 复制资源路径
     */
    public static void copyAssets(@NonNull Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);// 获取assets目录下的所有文件及目录名
            if (fileNames == null || fileNames.length == 0) {// 如果是文件
                InputStream is = context.getAssets().open(oldPath);
                File file = new File(newPath);
                if (file.exists() && file.isFile() && is.available() == file.length()) {
                    return;
                }
                FileOutputStream fos = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                    // buffer字节
                    fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                }
                fos.flush();// 刷新缓冲区
                is.close();
                fos.close();
                return;
            }
            //如果是文件夹
            File file = new File(newPath);
            if (!file.exists()) {
                boolean ret = file.mkdirs();// 如果文件夹不存在，则递归
                if (!ret) return;
            }
            for (String fileName : fileNames) {
                copyAssets(context, oldPath + File.separator + fileName, newPath + File.separator + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算文件CRC
     *
     * @param file File 文件
     * @return 计算的CRC
     */
    public static short calcCrcByFile(File file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            int size;
            while ((size = inputStream.read(buf)) != -1) {
                outputStream.write(Arrays.copyOf(buf, size));
            }
            byte[] data = outputStream.toByteArray();
            short crc = CryptoUtil.CRC16(data, (short) 0);
            outputStream.reset();
            inputStream.close();
            return crc;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String findGifPath(String filePath, boolean isDisplayLock) {
        String output = filePath;
        String flag = AppUtil.formatString("/%s/", isDisplayLock ? SConstant.DIR_LOCK : SConstant.DIR_UNLOCK);
        boolean ret = output.contains(flag);
        if (!ret) {
            String replace = AppUtil.formatString("/%s/", isDisplayLock ? SConstant.DIR_UNLOCK : SConstant.DIR_LOCK);
            output = filePath.replace(replace, flag);
        }
        return output;
    }

    public static String getFolderName(@ChargingCaseInfo.ResourceType int resourceType) {
        switch (resourceType) {
            case ChargingCaseInfo.TYPE_BOOT_ANIM:
                return SConstant.DIR_BOOT;
            case ChargingCaseInfo.TYPE_WALLPAPER:
                return SConstant.DIR_WALLPAPER;
            case ChargingCaseInfo.TYPE_SCREEN_SAVER:
            default:
                return SConstant.DIR_SCREEN;
        }
    }

    public static String getCropFileName(@ChargingCaseInfo.ResourceType int resourceType) {
        return getCustomName(resourceType) + "-src.jpeg";
    }

    public static String getCropFilePath(@NonNull Context context, @NonNull String deviceMac,
                                         @ChargingCaseInfo.ResourceType int resourceType) {
        return FileUtil.createFilePath(context, context.getPackageName(), SConstant.DIR_RESOURCE,
                deviceMac, SConstant.DIR_CUSTOM, getFolderName(resourceType)) + File.separator
                + getCropFileName(resourceType);
    }

    @NonNull
    public static File[] readCustomFiles(@NonNull Context context, @NonNull String deviceMac,
                                         @ChargingCaseInfo.ResourceType int resourceType) {
        File customDir = new File(FileUtil.createFilePath(context, context.getPackageName(),
                SConstant.DIR_RESOURCE, deviceMac, SConstant.DIR_CUSTOM, getFolderName(resourceType)));
        File[] customFiles = customDir.listFiles();
        if (null == customFiles || customFiles.length == 0) return new File[0];
        List<File> fileList = new ArrayList<>(Arrays.asList(customFiles));
        List<File> list = new ArrayList<>();
        final String namePrefix = resourceType == ChargingCaseInfo.TYPE_WALLPAPER ? ResourceInfo.WALLPAPER_NAME_PREFIX
                : ResourceInfo.SCREEN_NAME_PREFIX;
        final String cropFileName = ChargingBinUtil.getCropFileName(resourceType);
        for (File file : fileList) {
            if (!file.exists() || file.isDirectory()) continue;
            final String fileName = file.getName();
            if (TextUtils.isEmpty(fileName)) continue;
            if (fileName.startsWith(namePrefix) && !fileName.equals(cropFileName)) {
                list.add(file);
            }
        }
        customFiles = list.toArray(new File[0]);
        return customFiles;
    }

    public static String getCustomName(@ChargingCaseInfo.ResourceType int type) {
        switch (type) {
            case ChargingCaseInfo.TYPE_SCREEN_SAVER:
                return ResourceInfo.CUSTOM_SCREEN_NAME;
            case ChargingCaseInfo.TYPE_BOOT_ANIM:
                return "ANI_CST";
            case ChargingCaseInfo.TYPE_WALLPAPER:
                return ResourceInfo.CUSTOM_WALLPAPER_NAME;
            default:
                return "CUSTOM";
        }
    }

    @NonNull
    public static List<BaseMultiItem<ResourceFile>> readResourceFiles(@NonNull Context context, @NonNull ChargingCaseInfo info,
                                                                      @ChargingCaseInfo.ResourceType int resourceType) {
        List<BaseMultiItem<ResourceFile>> list = new ArrayList<>();
        final List<ResourceInfo> devFiles = resourceType == ChargingCaseInfo.TYPE_SCREEN_SAVER ? info.getScreenSavers()
                : resourceType == ChargingCaseInfo.TYPE_WALLPAPER ? info.getWallpapers() : new ArrayList<>();
        if (resourceType != ChargingCaseInfo.TYPE_BOOT_ANIM) {
            File[] customFiles = readCustomFiles(context, info.getAddress(), resourceType);
            if (customFiles.length == 0) {
                list.add(new BaseMultiItem<>(ResourceFileAdapter.TYPE_ADD_ITEM));
            } else {
                Arrays.sort(customFiles, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
                final File customFile = customFiles[0];
                ResourceFile resourceFile = new ResourceFile(customFile.hashCode(), resourceType)
                        .setName(customFile.getName())
                        .setPath(customFile.getPath());
                if (resourceType == ChargingCaseInfo.TYPE_WALLPAPER || resourceType == ChargingCaseInfo.TYPE_SCREEN_SAVER) {
                    final ResourceInfo devFile = findDeviceFile(devFiles, customFile);
                    if (null != devFile) {
                        resourceFile.setDevState(ResourceFile.STATE_ALREADY_EXIST).setDevFile(devFile);
                    }
                }
                list.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(resourceFile));
            }
        }
        String dirName = AppUtil.formatString("%dx%d", info.getScreenWidth(), info.getScreenHeight()); //动态调整屏幕分辨率
        String resourceDirPath = FileUtil.createFilePath(context, context.getPackageName(),
                SConstant.DIR_RESOURCE, SConstant.DIR_CHARGING_CASE, dirName, getFolderName(resourceType));
        List<File> files = new ArrayList<>();
        if (resourceType == ChargingCaseInfo.TYPE_SCREEN_SAVER) {
            readFileByDir(resourceDirPath, SConstant.DIR_LOCK, files);
        } else {
            readFileByDir(resourceDirPath, files);
        }
        for (File resource : files) {
            if (!info.isSupportGif() && isGif(resource.getPath())) {
                continue;
            }
            ResourceFile resourceFile = new ResourceFile(resource.hashCode(), resourceType)
                    .setName(resource.getName())
                    .setPath(resource.getPath());
            if (resourceType == ChargingCaseInfo.TYPE_WALLPAPER || resourceType == ChargingCaseInfo.TYPE_SCREEN_SAVER) {
                final ResourceInfo devFile = findDeviceFile(devFiles, resource);
                if (null != devFile) {
                    resourceFile.setDevState(ResourceFile.STATE_ALREADY_EXIST).setDevFile(devFile);
                }
            }
            list.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(resourceFile));
        }
        return list;
    }

    public static boolean isOutputDir(String filePath) {
        File outputFolder = new File(filePath);
        return outputFolder.exists() && outputFolder.isDirectory() &&
                outputFolder.getName().equalsIgnoreCase(SConstant.DIR_OUTPUT);
    }

    public static String getOutputDir(String filePath) {
        String outputPath = isOutputDir(filePath) ? filePath : filePath + File.separator + SConstant.DIR_OUTPUT;
        File outputDir = new File(outputPath);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            JL_Log.w(ChargingBinUtil.class.getSimpleName(), "getOutputDir",
                    "Failed to create folder. outputPath : " + outputPath);
            outputPath = filePath;
        }
        return outputPath;
    }

    public static void jumpResourceFragment(@NonNull Context context, @NonNull ResourceMsg resourceMsg) {
        //跳转对应的处理页面
        switch (resourceMsg.getResourceType()) {
            case ChargingCaseInfo.TYPE_SCREEN_SAVER: //屏保
                ConfirmScreenSaversFragment.goToFragment(context, resourceMsg);
                break;
            case ChargingCaseInfo.TYPE_BOOT_ANIM:  //开机动画
            case ChargingCaseInfo.TYPE_WALLPAPER:   //墙纸
                UploadResourceFragment.goToFragment(context, resourceMsg);
                break;
        }
    }

    public static boolean isCustomResourceByPath(String filePath) {
        String fileName = getFileNameByPath(filePath, true);
        if (TextUtils.isEmpty(fileName)) return false;
        return fileName.toUpperCase().startsWith(ResourceInfo.CUSTOM_SCREEN_NAME.toUpperCase())
                || fileName.toLowerCase().startsWith(ResourceInfo.CUSTOM_WALLPAPER_NAME.toLowerCase());
    }

    private static void addFileBySort(@NonNull File file, @NonNull List<File> list) {
        if (!file.isFile()) return;
        if (isGif(file.getName())) {
            int index = 0;
            for (File tmp : list) {
                if (isGif(tmp.getName())) {
                    index++;
                }
            }
            list.add(index, file);
        } else {
            list.add(file);
        }
    }

    public static boolean isCustomFile(ResourceInfo resourceInfo) {
        if (null == resourceInfo) return false;
        String filename = resourceInfo.getFilename();
        return ResourceInfo.CUSTOM_SCREEN_NAME.equalsIgnoreCase(filename)
                || filename.toUpperCase().startsWith(ResourceInfo.CUSTOM_SCREEN_NAME)
                || ResourceInfo.CUSTOM_WALLPAPER_NAME.equalsIgnoreCase(filename)
                || filename.toLowerCase().startsWith(ResourceInfo.CUSTOM_WALLPAPER_NAME);
    }

    /**
     * 获取映射的文件名
     *
     * @return 映射的文件名
     */
    public static String getResourceMappedName(ResourceInfo resourceInfo) {
        if (null == resourceInfo) return "";
        String fileName = resourceInfo.getFilename();
        if (isCustomFile(resourceInfo)) { //自定义屏保 或者 自定义墙纸
            int index = fileName.lastIndexOf(".");
            String name;
            String suffix;
            if (index == -1) {
                name = fileName;
                suffix = "";
            } else {
                name = fileName.substring(0, index);
                suffix = fileName.substring(index);
            }
            if (ResourceInfo.CUSTOM_SCREEN_NAME.equalsIgnoreCase(name) || ResourceInfo.CUSTOM_WALLPAPER_NAME.equalsIgnoreCase(name)) {
                fileName = CommonUtil.formatString("%s-%x%s", name, resourceInfo.getCrc(), suffix);
            } else {
                fileName = name;
            }
        }
        return fileName;
    }

    public static ResourceInfo findDeviceFile(@NonNull List<ResourceInfo> devFiles, File file) {
        if (null == file || devFiles.isEmpty()) return null;
        String fileName = file.getName();
        for (ResourceInfo devFile : devFiles) {
            String mappedName = getResourceMappedName(devFile);
            if (fileName.toUpperCase().startsWith(mappedName.toUpperCase())) {
                return devFile;
            }
        }
        return null;
    }
}
