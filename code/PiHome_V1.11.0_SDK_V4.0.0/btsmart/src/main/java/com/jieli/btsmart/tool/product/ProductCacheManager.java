package com.jieli.btsmart.tool.product;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.FileUtil;
import com.jieli.jl_http.bean.ProductDesignMessage;
import com.jieli.jl_http.bean.ProductMessage;
import com.jieli.jl_http.bean.ProductModel;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/9 9:15 AM
 * @desc : 产品服务器资源管理类
 */
public class ProductCacheManager extends BTRcspEventCallback {
    private static final String TAG = ProductCacheManager.class.getSimpleName();
    private static ProductCacheManager instance;
    private final TaskManager taskManager = new TaskManager();

    public static ProductCacheManager getInstance() {
        if (instance == null) {
            instance = new ProductCacheManager();
        }
        return instance;
    }

    private ProductCacheManager() {
        TaskInfo.init();
    }

    public ProductDesignMessage getProductDesignMessage(BleScanMessage scanMessage, String scene) {
        String key = Util.toResKey(scanMessage, scene);
        String json = Util.get(key);
        JL_Log.v(TAG, "getProductDesignMessage", "key=" + key + "\t-->" + json);
        if (TextUtils.isEmpty(json)) return null;
        return new Gson().fromJson(json, ProductDesignMessage.class);
    }

    public String getProductUrl(BleScanMessage scanMessage, String scene) {
        ProductDesignMessage message = getProductDesignMessage(scanMessage, scene);
        if (message == null) return "";
        return message.getUrl();
    }

    public String getProductUrl(int uid, int pid, int vid, String scene) {
        BleScanMessage bleScanMessage = new BleScanMessage();
        bleScanMessage.setVid(vid).setUid(uid).setPid(pid);
        return getProductUrl(bleScanMessage, scene);
    }


    public ProductMessage.DeviceBean getDeviceMessageModify(Context context, int vid, int uid, int pid) {
        ProductMessage message = null;
        if (context != null) {
            String url = ProductCacheManager.getInstance().getProductUrl(uid, pid, vid, ProductModel.MODEL_PRODUCT_MESSAGE.getValue());
            DeviceInfo deviceInfo = RCSPController.getInstance().getDeviceInfo();
            String json = "";
            if (TextUtils.isEmpty(url) && deviceInfo != null) {
                switch (deviceInfo.getSdkType()) {
                    case JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET:
                        url = SConstant.AC693_JSON_NAME;
                        break;
                    case JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET:
                        url = SConstant.AC697_JSON_NAME;
                        break;
                    case JLChipFlag.JL_CHIP_FLAG_696X_SOUNDBOX:
                        url = SConstant.AC696_JSON_NAME;
                        break;
                    case JLChipFlag.JL_CHIP_FLAG_696X_TWS_SOUNDBOX:
                        url = SConstant.AC696_TWS_JSON_NAME;
                        break;
                    case JLChipFlag.JL_CHIP_FLAG_695X_SOUND_CARD:
                        url = SConstant.AC695_SOUND_CARD_JSON_NAME;
                        break;
                    case JLChipFlag.JL_CHIP_FLAG_MANIFEST_EARPHONE:
                        url = SConstant.MANIFEST_HEADSET_JSON_NAME;
                        break;
                    case JLChipFlag.JL_CHIP_FLAG_MANIFEST_SOUNDBOX:
                        url = SConstant.MANIFEST_SOUNDBOX_JSON_NAME;
                        break;
                    default:
                        return null;
                }

                json = AppUtil.getTextFromAssets(context, url);
            } else {
                byte[] data = FileUtil.getBytes(url);
                if (data == null || data.length < 1) return null;
                json = new String(data);
            }

            JL_Log.d(TAG, "getDeviceMessageModify", "读取product_messages  json--->" + json);
            if (TextUtils.isEmpty(json)) return null;
            try {
                message = new GsonBuilder().setLenient().create().fromJson(json, ProductMessage.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return message == null ? null : message.getDevice();
    }

    public void registerListener(OnUpdateListener listener) {
        taskManager.registerListener(listener);
    }

    public void unregisterListener(OnUpdateListener listener) {
        taskManager.unregisterListener(listener);
    }

    @Override
    public void onShowDialog(BluetoothDevice device, BleScanMessage bleScanMessage) {
        super.onShowDialog(device, bleScanMessage);
        taskManager.addTask(new MessageTaskInfo(bleScanMessage));
    }


    public interface OnUpdateListener {

        void onImageUrlUpdate(BleScanMessage bleScanMessage);

        void onJsonUpdate(BleScanMessage bleScanMessage, String path);
    }


}


