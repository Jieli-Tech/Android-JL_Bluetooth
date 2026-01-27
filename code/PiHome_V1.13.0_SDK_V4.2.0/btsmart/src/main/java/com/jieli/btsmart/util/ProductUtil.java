package com.jieli.btsmart.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.google.gson.GsonBuilder;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.bluetooth.utils.PreferencesHelper;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.ui.widget.product_dialog.ProductDesign;
import com.jieli.btsmart.ui.widget.product_dialog.ProductDesignItem;
import com.jieli.component.utils.FileUtil;
import com.jieli.jl_http.bean.KeySettingsBean;
import com.jieli.jl_http.bean.LedSettingsBean;
import com.jieli.jl_http.bean.ProductDesignMessage;
import com.jieli.jl_http.bean.ProductMessage;
import com.jieli.jl_http.bean.ProductModel;
import com.jieli.jl_http.bean.ValueBean;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.jieli.jl_http.bean.ProductModel.MODEL_PRODUCT_MESSAGE;

/**
 * 产品工具类
 *
 * @author zqjasonZhong
 * @date 2019/8/2
 */
public class ProductUtil {

    private final static String SPECIAL_DIVIDER = "+";
    private final static String COMMON_DIVIDER = "_";

    /**
     * 缓存设备是否从服务器更新过的表
     * <p>
     * 说明: key - 设备的经典蓝牙地址  value -   是否从服务器更新过
     * </p>
     */
    private static Map<String, Boolean> mUpdateServiceMap;
    /**
     * 是否允许请求悬浮窗权限
     */
    private static boolean isAllowFloatingWindow = true;

    public static String formatOutPath(Context mContext, int vid, int uid, int pid, String scene, String hash, String type) {
        return FileUtil.createFilePath(mContext, mContext.getPackageName(), SConstant.DIR_DESIGN)
                + "/" + SConstant.DIR_DESIGN + SPECIAL_DIVIDER + vid + SPECIAL_DIVIDER + uid
                + SPECIAL_DIVIDER + pid + SPECIAL_DIVIDER + scene + SPECIAL_DIVIDER + hash + "." + type.toLowerCase();
    }

    public static boolean isGifFile(String url) {
        boolean isGif = false;
        if (!TextUtils.isEmpty(url) && url.contains("\\.")) {
            String suffix = url.substring(url.lastIndexOf("\\."));
            if (ProductDesignMessage.TYPE_GIF.equals(suffix)) {
                isGif = true;
            }
        }
        return isGif;
    }

    public static boolean saveProductMessage(Context context, int vid, int uid, int pid, ProductMessage message) {
        return message != null && saveProductMessage(context, vid, uid, pid, message.toString());
    }

    public static boolean saveProductMessage(Context context, int vid, int uid, int pid, String json) {
        boolean ret = false;
        if (context != null && !TextUtils.isEmpty(json)) {
            PreferencesHelper.putStringValue(context, getProductKey(vid, uid, pid), json);
            ret = true;
        }
        return ret;
    }

    public static ProductMessage getCacheProductMessage(Context context, int vid, int uid, int pid) {
        ProductMessage message = null;
        if (context != null) {
//            String json = PreferencesHelper.getSharedPreferences(context).getString(getProductKey(vid, uid, pid), null);
            String url = ProductCacheManager.getInstance().getProductUrl(uid, pid, vid, ProductModel.MODEL_PRODUCT_MESSAGE.getValue());
            String json = "";
            DeviceInfo deviceInfo = RCSPController.getInstance().getDeviceInfo();
            if (TextUtils.isEmpty(url) && deviceInfo != null && deviceInfo.getUid() == uid && deviceInfo.getPid() == pid) {
                HistoryBluetoothDevice historyBluetoothDevice = DeviceAddrManager.getInstance().findHistoryBluetoothDevice(RCSPController.getInstance().getUsingDevice());
                BleScanMessage bleScanMessage = BleScanMsgCacheManager.getInstance().getBleScanMessage(deviceInfo.getBleAddr());
                int version = -1;
                if (bleScanMessage != null) {
                    version = bleScanMessage.getVersion();
                } else if (historyBluetoothDevice != null) {
                    version = historyBluetoothDevice.getAdvVersion();
                }
                if (UIHelper.isHeadsetType(deviceInfo.getSdkType()) && version == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {//挂脖
                    json = AppUtil.getTextFromAssets(context, SConstant.AC693_NECK_JSON_NAME);
                } else {//非挂脖
                    switch (deviceInfo.getSdkType()) {
                        case JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET:
                            json = AppUtil.getTextFromAssets(context, SConstant.AC693_JSON_NAME);
                            break;
                        case JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET:
                            json = AppUtil.getTextFromAssets(context, SConstant.AC697_JSON_NAME);
                            break;
                        case JLChipFlag.JL_CHIP_FLAG_696X_SOUNDBOX:
                            json = AppUtil.getTextFromAssets(context, SConstant.AC696_JSON_NAME);
                            break;
                        case JLChipFlag.JL_CHIP_FLAG_696X_TWS_SOUNDBOX:
                            json = AppUtil.getTextFromAssets(context, SConstant.AC696_TWS_JSON_NAME);
                            break;
                        case JLChipFlag.JL_CHIP_FLAG_695X_SOUND_CARD:
                            json = AppUtil.getTextFromAssets(context, SConstant.AC695_SOUND_CARD_JSON_NAME);
                            break;
                        case JLChipFlag.JL_COLOR_SCREEN_CHARGING_CASE:
                        case JLChipFlag.JL_CHIP_FLAG_MANIFEST_EARPHONE:
                            json = AppUtil.getTextFromAssets(context, SConstant.MANIFEST_HEADSET_JSON_NAME);
                            break;
                        case JLChipFlag.JL_CHIP_FLAG_MANIFEST_SOUNDBOX:
                            json = AppUtil.getTextFromAssets(context, SConstant.MANIFEST_SOUNDBOX_JSON_NAME);
                            break;
                    }
                }
//                JL_Log.i("zzc", "getCacheProductMessage : " + deviceInfo.getSdkType() +"\n" + json);
                if (!TextUtils.isEmpty(json)) {//把本地缓存写入系统缓存
                    saveProductMessage(context, vid, uid, pid, json);
                }
            } else {
                byte[] data = FileUtil.getBytes(url);
                if (data == null || data.length < 1) return null;
                json = new String(data);
            }

            if (!TextUtils.isEmpty(json)) {
                try {
                    message = new GsonBuilder().setLenient().create().fromJson(json, ProductMessage.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return message;
    }


    public static String getProductType(Context context, int vid, int uid, int pid) {
        String deviceType = SConstant.DEVICE_HEADSET;
        ProductMessage message = getCacheProductMessage(context, vid, uid, pid);
        if (message != null) {
            ProductMessage.DeviceBean device = message.getDevice();
            if (device != null) {
                deviceType = device.getDevType();
            }
        }
        return deviceType;
    }

    public static ProductMessage.ChipBean getChipMessage(Context context, int vid, int uid, int pid) {
        ProductMessage.ChipBean chip = null;
        ProductMessage message = getCacheProductMessage(context, vid, uid, pid);
        if (message != null) {
            chip = message.getChip();
        }
        return chip;
    }

    public static ProductMessage.DeviceBean getDeviceMessage(Context context, int vid, int uid, int pid) {
        ProductMessage.DeviceBean device = null;
        ProductMessage message = getCacheProductMessage(context, vid, uid, pid);
        if (message != null) {
            device = message.getDevice();
        }
        return device;
    }


    public static String getValue(ValueBean value) {
        String string = null;
        if (value != null) {
            if (isChinese()) {
                string = value.getTitle().getZh();
            } else {
                string = value.getTitle().getEn();
            }
        }
        return string;
    }

    public static boolean isChinese() {
        return Locale.CHINESE.getLanguage().equals(Locale.getDefault().getLanguage()) ||
                Locale.SIMPLIFIED_CHINESE.getLanguage().equals(Locale.getDefault().getLanguage()) ||
                Locale.TRADITIONAL_CHINESE.getLanguage().equals(Locale.getDefault().getLanguage());
    }

    private static String getValueFromList(List<ValueBean> list, int key) {
        String content = null;
        if (list != null) {
            for (ValueBean value : list) {
                if (key == value.getValue()) {
                    content = getValue(value);
                }
            }
        }
        return content;
    }

    public static KeySettingsBean getCacheKeySettings(Context context, int vid, int uid, int pid) {
        ProductMessage productMessage = getCacheProductMessage(context, vid, uid, pid);
        if (productMessage != null && productMessage.getDevice() != null) {
            return productMessage.getDevice().getKeySettings();
        }
        return null;
    }

    public static LedSettingsBean getCacheLedSettings(Context context, int vid, int uid, int pid) {
        ProductMessage productMessage = getCacheProductMessage(context, vid, uid, pid);
        if (productMessage != null && productMessage.getDevice() != null) {
            return productMessage.getDevice().getLedSettings();
        }
        return null;
    }


    public static List<ValueBean> getCacheList(Context context, int vid, int uid, int pid, int attr) {
        ProductMessage productMessage = getCacheProductMessage(context, vid, uid, pid);
        if (productMessage != null && productMessage.getDevice() != null) {
            if (attr == AttrAndFunCode.ADV_TYPE_WORK_MODE) {
                return productMessage.getDevice().getWorkModes();
            } else if (attr == AttrAndFunCode.ADV_TYPE_MIC_CHANNEL_SETTINGS) {
                return productMessage.getDevice().getMicChannels();
            } else if (attr == AttrAndFunCode.ADV_TYPE_IN_EAR_CHECK) {
                return productMessage.getDevice().getInEarCheck();
            }
        }
        return null;
    }

    public static String getKeySettingsName(Context context, int vid, int uid, int pid, int type, int key) {
        String string = null;
        KeySettingsBean keySettingsBean = getCacheKeySettings(context, vid, uid, pid);
        if (keySettingsBean != null) {
            switch (type) {
                case SConstant.KEY_FIELD_KEY_NUM: { //key_num
                    List<ValueBean> list = keySettingsBean.getKeyNums();
                    string = getValueFromList(list, key);
                    break;
                }
                case SConstant.KEY_FIELD_KEY_ACTION: { //key_action
                    List<ValueBean> list = keySettingsBean.getKeyActions();
                    string = getValueFromList(list, key);
                    break;
                }
                case SConstant.KEY_FIELD_KEY_FUNCTION: { //key_function
                    List<ValueBean> list = keySettingsBean.getKeyFunctions();
                    string = getValueFromList(list, key);
                    if (string == null && key == AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE) {
                        string = context.getString(R.string.noise_control);
                    }
                    break;
                }
            }
        }
        return string;
    }

    public static String getLedSettingsName(Context context, int vid, int uid, int pid, int type, int key) {
        String string = null;
        LedSettingsBean ledSettingsBean = getCacheLedSettings(context, vid, uid, pid);
        if (ledSettingsBean != null) {
            switch (type) {
                case SConstant.KEY_FIELD_LED_SCENE: {
                    List<ValueBean> list = ledSettingsBean.getScenes();
                    string = getValueFromList(list, key);
                    break;
                }
                case SConstant.KEY_FIELD_LED_EFFECT: {
                    List<ValueBean> list = ledSettingsBean.getEffects();
                    string = getValueFromList(list, key);
                    break;
                }
            }

        }
        return string;
    }

    public static String getWorkModeName(Context context, int vid, int uid, int pid, int code) {
        String name = null;
        if (context != null) {
            ProductMessage productMessage = ProductUtil.getCacheProductMessage(context, vid, uid, pid);
            if (productMessage != null && productMessage.getDevice() != null) {
                List<ValueBean> workList = productMessage.getDevice().getWorkModes();
                if (workList != null) {
                    for (ValueBean workMode : workList) {
                        if (workMode.getValue() == code) {
                            name = getValue(workMode);
                            break;
                        }
                    }
                }
            }
        }
        return name;
    }

    public static String getMicChannelName(Context context, int vid, int uid, int pid, int code) {
        String name = null;
        if (context != null) {
            ProductMessage productMessage = ProductUtil.getCacheProductMessage(context, vid, uid, pid);
            if (productMessage != null && productMessage.getDevice() != null) {
                List<ValueBean> micChannels = productMessage.getDevice().getMicChannels();
                if (micChannels != null) {
                    for (ValueBean mic : micChannels) {
                        if (mic.getValue() == code) {
                            name = getValue(mic);
                            break;
                        }
                    }
                }
            }
        }
        return name;
    }

    public static String getInEarCheckOption(Context context, int vid, int uid, int pid, int code) {
        String name = null;
        if (context != null) {
            ProductMessage productMessage = ProductUtil.getCacheProductMessage(context, vid, uid, pid);
            if (productMessage != null && productMessage.getDevice() != null) {
                List<ValueBean> inEars = productMessage.getDevice().getInEarCheck();
                if (inEars != null) {
                    for (ValueBean inEarOption : inEars) {
                        if (inEarOption.getValue() == code) {
                            name = getValue(inEarOption);
                            break;
                        }
                    }
                }
            }
        }
        return name;
    }

    public static boolean isNeedOta(Context context, int vid, int uid, int pid) {
        boolean isNeedOta = false;
        if (context != null) {
            ProductMessage productMessage = ProductUtil.getCacheProductMessage(context, vid, uid, pid);
            if (productMessage != null && productMessage.getDevice() != null) {
                isNeedOta = productMessage.getDevice().getHasOta() > 0;
            }
        }
        return isNeedOta;
    }

    public static boolean isSupportCancelPair(Context context, int vid, int uid, int pid) {
        boolean isSupport = false;
        if (context != null) {
            ProductMessage productMessage = ProductUtil.getCacheProductMessage(context, vid, uid, pid);
            if (productMessage != null && productMessage.getDevice() != null) {
                isSupport = productMessage.getDevice().getHasCancelPair() > 0;
            }
        }
        return isSupport;
    }

    private static String getProductKey(int vid, int uid, int pid) {
//        return "JL" + COMMON_DIVIDER + vid + COMMON_DIVIDER + uid + COMMON_DIVIDER + pid;
        return "ProductDesign_" + uid + "_" + pid + "_" + vid + "_" + MODEL_PRODUCT_MESSAGE.getValue();
    }

    public static void updateServiceMap(String edrAddr, boolean isUpdateService) {
        if (!TextUtils.isEmpty(edrAddr)) {
            if (mUpdateServiceMap == null) {
                mUpdateServiceMap = new HashMap<>();
            }
            mUpdateServiceMap.put(edrAddr, isUpdateService);
        }
    }

    public static boolean getUpdateService(String edrAddr) {
        boolean isUpdateService = false;
        if (!BluetoothAdapter.checkBluetoothAddress(edrAddr)) return false;
        if (mUpdateServiceMap != null && mUpdateServiceMap.size() > 0) {
            Boolean value = mUpdateServiceMap.get(edrAddr);
            if (value != null) {
                isUpdateService = value;
            }
        }
        return isUpdateService;
    }

    public static void clearUpdateServiceMap() {
        if (mUpdateServiceMap != null) {
            mUpdateServiceMap.clear();
        }
    }

    public static ProductDesign findProductDesign(List<ProductDesignItem> dataList, String scene) {
        ProductDesign design = null;
        if (dataList != null && dataList.size() > 0 && !TextUtils.isEmpty(scene)) {
            for (ProductDesignItem item : dataList) {
                ProductDesign first = item.getFirstProduct();
                if (first == null) continue;
                if (scene.equals(first.getScene())) {
                    design = first;
                    break;
                } else if (item.getItemType() == ProductDesignItem.VIEW_TYPE_DOUBLE) {
                    ProductDesign second = item.getSecondProduct();
                    if (second == null) continue;
                    if (scene.equals(second.getScene())) {
                        design = second;
                        break;
                    }
                }
            }
        }
//        JL_Log.i(TAG, "findProductDesign :: " + design);
        return design;
    }

    public static String findCacheDesign(Context context, int vid, int uid, int pid, String scene) {
        return ProductCacheManager.getInstance().getProductUrl(uid, pid, vid, scene);
//        if (context == null) return null;
//        String path = null;
//        String designDir = FileUtil.createFilePath(context,  context.getPackageName(), SConstant.DIR_DESIGN);
//        File dir = new File(designDir);
//        if (dir.exists() && dir.isDirectory()) {
//            File[] fileList = dir.listFiles();
//            if (fileList != null && fileList.length > 0) {
//                for (File file : fileList) {
//                    if (file.isFile()) {
//                        String fileName = file.getName();
//                        String[] nameArray = fileName.split("\\+");
//                        if (nameArray.length > 5) {
//                            if (String.valueOf(vid).equals(nameArray[1])
//                                    && String.valueOf(uid).equals(nameArray[2])
//                                    && String.valueOf(pid).equals(nameArray[3])
//                                    && (scene != null && scene.equals(nameArray[4]))) {
//                                path = file.getAbsolutePath();
//                                break;
//                            }
//                        }
//                    }
//                }
//            }
//        }
////        JL_Log.d("zzc", "findCacheDesign :: path = " + path);
//        return path;
    }

    public static String getDeviceTypeByADV(Context context, BleScanMessage message) {
        if (null == context || null == message) return null;
        String deviceType = ProductUtil.getProductType(context, message.getVid(),
                message.getUid(), message.getPid());
        if (message.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_SOUNDBOX) {
            deviceType = SConstant.DEVICE_SOUND_BOX;
        }
        return deviceType;
    }

    public static boolean isHeadsetTypeByADV(Context context, BleScanMessage message) {
        String deviceType = getDeviceTypeByADV(context, message);
        boolean ret = false;
        if (deviceType != null) {
            deviceType = deviceType.toLowerCase();
            ret = deviceType.equals(SConstant.DEVICE_HEADSET);
        }
        return ret;
    }

    public static boolean checkEdrIsConnected(String edrAddr) {
        if (!BluetoothAdapter.checkBluetoothAddress(edrAddr)) {
            JL_Log.i("zzc", "checkEdrIsConnected : edrAddr is error.");
            return false;
        }
        boolean isConnected = false;
        List<BluetoothDevice> connectedDeviceList = BluetoothUtil.getSystemConnectedBtDeviceList();
        if (connectedDeviceList != null) {
            for (BluetoothDevice device : connectedDeviceList) {
                JL_Log.i("zzc", "checkEdrIsConnected : device : " + BluetoothUtil.printBtDeviceInfo(device) + ", edrAddr : " + edrAddr);
                if (edrAddr.equals(device.getAddress())) {
                    isConnected = true;
                    break;
                }
            }
        }
        return isConnected;
    }


    public static boolean isCanDrawOverlays(Context context) {
        boolean canDrawOverlays = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canDrawOverlays = Settings.canDrawOverlays(context);
        }
        return canDrawOverlays;
    }

    public static boolean equalScanMessage(BleScanMessage message, BleScanMessage message1) {
        return (message != null && message1 != null && ((message.getEdrAddr() != null && message.getEdrAddr().equals(message1.getEdrAddr()))
                || (message.getVid() == message1.getVid() && message.getVid() == BluetoothConstant.APPLE_VID)));
    }

    public static boolean isAllowFloatingWindow(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        if (isCanDrawOverlays(context)) {
            return true;
        }
        return isAllowFloatingWindow;
    }

    public static void setIsAllowFloatingWindow(boolean enable) {
        isAllowFloatingWindow = enable;
    }
}
