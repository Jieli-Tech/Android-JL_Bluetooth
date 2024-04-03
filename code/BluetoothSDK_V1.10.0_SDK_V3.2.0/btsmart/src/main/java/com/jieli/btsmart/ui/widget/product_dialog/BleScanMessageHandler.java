package com.jieli.btsmart.ui.widget.product_dialog;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.text.TextUtils;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.SystemUtil;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.ProductDesignMessage;
import com.jieli.jl_http.bean.ProductModel;
import com.jieli.jl_http.interfaces.IActionListener;
import com.jieli.jl_http.interfaces.IDownloadListener;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 广播包信息处理
 *
 * @author zqjasonZhong
 * @date 2019/8/6
 */
public class BleScanMessageHandler {
    private final static String TAG = "BleScanMessageHandler";
    private volatile static BleScanMessageHandler instance;
    private Context mContext;
    private Set<String> hashTaskList;
    private final Set<String> interceptor = new HashSet<>();

    public interface OnServiceUpdateCallback {

        void onUpdate(ProductDesign design);

        void onError(int code, String message);
    }

    private BleScanMessageHandler(Context context) {
        mContext = SystemUtil.checkNotNull(context);
    }

    public static BleScanMessageHandler getInstance() {
        if (instance == null) {
            synchronized (BleScanMessageHandler.class) {
                if (instance == null) {
                    instance = new BleScanMessageHandler(AppUtil.getContext());
                }
            }
        }
        return instance;
    }

    public void release() {
        if (hashTaskList != null) {
            hashTaskList.clear();
            hashTaskList = null;
        }
        interceptor.clear();
        mContext = null;
        instance = null;
    }

    public List<ProductDesignItem> handlerProductDesign(BleScanMessage scanMessage) {
        if (mContext == null) return null;
        if (scanMessage == null) return null;
        List<ProductDesignItem> itemList = null;
        switch (scanMessage.getDeviceType()) {
            case JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V1:
            case JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V2:
                itemList = defaultTwsHeadsetDesign(scanMessage);
                break;
            case JL_DeviceType.JL_DEVICE_TYPE_CHARGING_BIN:
            case JL_DeviceType.JL_DEVICE_TYPE_SOUND_CARD:
            case JL_DeviceType.JL_DEVICE_TYPE_SOUNDBOX:
                itemList = defaultTwsSoundBoxDesign(scanMessage);
                break;
        }

        if (itemList == null || itemList.size() == 0) {
            itemList = defaultTwsHeadsetDesign(scanMessage);
        }
        return itemList;
    }

    public List<ProductDesignItem> handlerConnectedDeviceStatus(BleScanMessage scanMessage, int action) {
        if (mContext == null) return null;
        if (scanMessage == null) return null;
        List<ProductDesignItem> itemList = new ArrayList<>();
        int leftQuantity = scanMessage.getLeftDeviceQuantity();
        int rightQuantity = scanMessage.getRightDeviceQuantity();
        int chargingBinQuantity = scanMessage.getChargingBinQuantity();
//        String deviceType = ProductUtil.getDeviceTypeByADV(mContext, scanMessage);
//        String productType = deviceType == null ? SConstant.DEVICE_HEADSET : deviceType;
//
        String scene;
        boolean isCharging;
        int failedRes;
        ProductDesign left = null;
        ProductDesign right = null;
        ProductDesign chargingBin = null;
        if (leftQuantity > 0) {
            scene = ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue();
            isCharging = scanMessage.isLeftCharging();
            failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getLeftImg();
//                    SConstant.DEVICE_HEADSET.equals(productType) ? R.drawable.ic_headset_left : R.drawable.ic_default_product_design;
            left = fillProductDesign(scanMessage, action, isCharging, leftQuantity, scene, failedRes);
        }
        if (rightQuantity > 0) {
            scene = ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue();
            isCharging = scanMessage.isRightCharging();
            failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getRightImg();
//                    SConstant.DEVICE_HEADSET.equals(productType) ? R.drawable.ic_headset_right : R.drawable.ic_default_product_design;
            right = fillProductDesign(scanMessage, action, isCharging, rightQuantity, scene, failedRes);
        }
        if (chargingBinQuantity > 0) {
            scene = ProductModel.MODEL_DEVICE_CHARGING_BIN_IDLE.getValue();
            isCharging = scanMessage.isDeviceCharging();
            failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getBinImg();
            //R.drawable.ic_charging_bin;
            chargingBin = fillProductDesign(scanMessage, action, isCharging, chargingBinQuantity, scene, failedRes);
        }
        /*
         * 左耳存在（默认左耳是主机）
         * 电量为0，视为设备不存在
         * 电量大于0，视为设备存在
         */
        if (leftQuantity > 0) {
            ProductDesignItem firstItem = new ProductDesignItem();
            ProductDesignItem secondItem = null;

            //第一视图
            firstItem.setFirstProduct(left);  //增加视图1的第一产品信息

            if (rightQuantity > 0) { //右耳存在
                if (chargingBinQuantity > 0) { //充电仓存在， 说明是两只耳机在仓的情况
                    firstItem.setSecondProduct(right); //增加视图1的第二产品信息
                    firstItem.setType(ProductDesignItem.VIEW_TYPE_DOUBLE); //修改为双设备布局

                    //第二视图
                    secondItem = new ProductDesignItem();
                    secondItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE); //默认是单设备布局
                    secondItem.setFirstProduct(chargingBin); //增加充电仓产品设计
                } else { //充电仓不在
                    //左耳不在充电状态和右耳不在充电状态，而且是耳机类型
                    boolean isHeadset = scanMessage.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V1 || scanMessage.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V2;
                    if (!scanMessage.isLeftCharging() && !scanMessage.isRightCharging() && isHeadset) {
                        //重置为双耳机产品信息
                        scene = ProductModel.MODEL_DOUBLE_HEADSET.getValue();
                        isCharging = false;
                        failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getDoubleImg();
                        int quantity = Math.min(leftQuantity, rightQuantity);//取最低值显示
                        ProductDesign doubleDesign = fillProductDesign(scanMessage, action, isCharging, quantity, scene, failedRes);
                        firstItem.setFirstProduct(doubleDesign);
                        firstItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE); //设备单设备布局
                    } else {
                        firstItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);

                        secondItem = new ProductDesignItem();
                        secondItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);
                        secondItem.setFirstProduct(right);
                    }
                }
            } else {
                if (chargingBinQuantity > 0) { //充电仓存在， 说明是一只耳机在仓的情况
                    //第二视图
                    secondItem = new ProductDesignItem();
                    secondItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE); //默认是单设备布局
                    secondItem.setFirstProduct(chargingBin); //增加充电仓产品设计
                }
            }
            itemList.add(firstItem);
            if (secondItem != null) {
                itemList.add(secondItem);
            }
        } else if (rightQuantity > 0) { //右设备在线，考虑到音箱的情况
            ProductDesignItem firstItem = new ProductDesignItem();
            firstItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);
            //第一设备布局
            firstItem.setFirstProduct(right);  //默认右耳机产品设计
            itemList.add(firstItem);

            if (chargingBinQuantity > 0) { //充电仓存在， 说明是一只耳机在仓的情况
                //第二视图
                ProductDesignItem secondItem = new ProductDesignItem();
                secondItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE); //默认是单设备布局
                secondItem.setFirstProduct(chargingBin); //增加充电仓产品设计
                itemList.add(secondItem);
            }
        }
        if (itemList.size() == 0) {
            itemList = handlerProductDesign(scanMessage);
        }
        return itemList;
    }

    private ProductDesign fillProductDesign(BleScanMessage scanMessage, int action, boolean isCharging, int quantity, String scene, int failedRes) {
        if (scanMessage == null) return null;
        return fillProductDesign(scanMessage.getVid(), scanMessage.getUid(), scanMessage.getPid(), action, isCharging, quantity, scene, failedRes);
    }

    private ProductDesign fillProductDesign(int vid, int uid, int pid, int action, boolean isCharging, int quantity, String scene, int failedRes) {
        ProductDesign design = new ProductDesign();
        design.setAction(action);
        design.setCharging(isCharging);
        design.setQuantity(quantity);
        //设置布局的场景
        design.setScene(scene);
        //获取缓存的服务器图片
        String fileUrl = ProductUtil.findCacheDesign(mContext, vid, uid, pid, scene);
        if (!TextUtils.isEmpty(fileUrl)) { //获取缓存图片地址成功
            design.setImageUrl(fileUrl);
            design.setGif(ProductUtil.isGifFile(fileUrl));
        }
        //设置设备默认图(判断是音箱还是耳机)
        design.setFailedRes(failedRes);
        return design;
    }

    public void requestProductDesignFromService(final BleScanMessage scanMessage, final List<ProductDesignItem> dataList, final OnServiceUpdateCallback callback) {
        if (mContext == null || scanMessage == null) return;
        if (!ProductUtil.getUpdateService(scanMessage.getEdrAddr()) && !isContainsInterceptor(scanMessage.getEdrAddr())) { //检查是否已经从服务器更新过数据
            JL_Log.i(TAG, "requestProductDesign :: " + scanMessage);
            interceptor.add(scanMessage.getEdrAddr()); //添加下载黑名单列表
            JL_HttpClient.getInstance().requestProductDesign(scanMessage.getUid(), scanMessage.getPid(), new IActionListener<List<ProductDesignMessage>>() {
                @Override
                public void onSuccess(List<ProductDesignMessage> response) {
                    handleGetProductMessage(response, scanMessage, dataList, callback);
                    interceptor.remove(scanMessage.getEdrAddr());
                }

                @Override
                public void onError(int code, String message) {
                    JL_Log.e(TAG, "requestProductDesign :: error : code = " + code + ",message = " + message);
                    if (callback != null) {
                        callback.onError(code, message);
                    }
                    interceptor.remove(scanMessage.getEdrAddr());
                }
            });
        }
    }

    private void removeTaskList(String edrAddr, String hash) {
        if (hashTaskList != null && !TextUtils.isEmpty(hash)) {
            hashTaskList.remove(hash);
            if (hashTaskList.size() == 0 && !TextUtils.isEmpty(edrAddr)) {
                ProductUtil.updateServiceMap(edrAddr, true);
            }
        }
    }

    private List<ProductDesignItem> defaultTwsHeadsetDesign(BleScanMessage scanMessage) {
        int action = ProductDesign.ACTION_HIDE_QUANTITY;
        List<ProductDesignItem> itemList = new ArrayList<>();
        ProductDesignItem firstItem = new ProductDesignItem();
        ProductDesignItem secondItem = new ProductDesignItem();
        //第一设备布局
        String scene = ProductModel.MODEL_DOUBLE_HEADSET.getValue();
        boolean isCharging = scanMessage.isLeftCharging() && scanMessage.isRightCharging();
        int failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getDoubleImg();
        int quantity = Math.min(scanMessage.getLeftDeviceQuantity(), scanMessage.getRightDeviceQuantity());
        ProductDesign doubleHeadset = fillProductDesign(scanMessage, action, isCharging, quantity, scene, failedRes);
        firstItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);
        firstItem.setFirstProduct(doubleHeadset);  //默认左耳机产品设计

//        ProductDesign second = new ProductDesign();
//        second.setAction(action);
//        second.setCharging(scanMessage.isRightCharging());
//        second.setQuantity(scanMessage.getRightDeviceQuantity());
//        second.setFailedRes(R.drawable.ic_headset_right);
//        second.setScene(ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue());
//        fileUrl = ProductUtil.findCacheDesign(mContext, scanMessage.getVid(), scanMessage.getUid(), scanMessage.getPid(), second.getScene());
//        if (!TextUtils.isEmpty(fileUrl)) {
//            second.setImageUrl(fileUrl);
//            second.setGif(ProductUtil.isGifFile(fileUrl));
//        } else {
//            second.setImageUrl(null);
//            second.setGif(false);
//        }
//        firstItem.setSecondProduct(second); //默认右耳机产品设计
        firstItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);

        //第二设备布局
        secondItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);
        scene = ProductModel.MODEL_DEVICE_CHARGING_BIN_IDLE.getValue();
        isCharging = scanMessage.isDeviceCharging();
        failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getBinImg();
        quantity = scanMessage.getChargingBinQuantity();
        ProductDesign chargingBin = fillProductDesign(scanMessage, action, isCharging, quantity, scene, failedRes);
        secondItem.setFirstProduct(chargingBin);  //默认充电仓产品设计

        itemList.add(firstItem);
        itemList.add(secondItem);
        return itemList;
    }

    private List<ProductDesignItem> defaultTwsSoundBoxDesign(BleScanMessage scanMessage) {
        int action = ProductDesign.ACTION_HIDE_QUANTITY;
        List<ProductDesignItem> itemList = new ArrayList<>();
        ProductDesignItem firstItem = new ProductDesignItem();
        ProductDesignItem secondItem = null;
        //第一设备布局
        String scene = ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue();
        boolean isCharging = scanMessage.isLeftCharging();
        int failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getLeftImg();
        int quantity = scanMessage.getLeftDeviceQuantity();
        ProductDesign left = fillProductDesign(scanMessage, action, isCharging, quantity, scene, failedRes);
        firstItem.setFirstProduct(left);  //默认第一音箱产品设计
        firstItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);

        //第二设备布局
        if (scanMessage.getRightDeviceQuantity() > 0) {
            secondItem = new ProductDesignItem();
            secondItem.setType(ProductDesignItem.VIEW_TYPE_SINGLE);
            scene = ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue();
            isCharging = scanMessage.isRightCharging();
            failedRes = DefaultResFactory.createByDeviceType(scanMessage.getDeviceType(),scanMessage.getVersion()).getRightImg();
            quantity = scanMessage.getRightDeviceQuantity();
            ProductDesign right = fillProductDesign(scanMessage, action, isCharging, quantity, scene, failedRes);
            secondItem.setFirstProduct(right);  //默认第二音箱产品设计
        }

        itemList.add(firstItem);
        if (secondItem != null) {
            itemList.add(secondItem);
        }
        return itemList;
    }

    private boolean isContainsInterceptor(String address) {
        boolean ret = false;
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            ret = interceptor.contains(address);
        }
        return ret;
    }

    private String getUrlKey(String url, String scene) {
        return url + "_" + scene;
    }

    public void handleGetProductMessage(List<ProductDesignMessage> response, BleScanMessage scanMessage,
                                        List<ProductDesignItem> dataList, OnServiceUpdateCallback callback) {
        if (response != null && response.size() > 0) {
            if (hashTaskList == null || hashTaskList.size() == 0) { //记录加载任务
                hashTaskList = new HashSet<>();
                for (ProductDesignMessage data : response) {
                    if (!TextUtils.isEmpty(data.getUrl())) {
                        hashTaskList.add(data.getHash());
                    }
                }
            }
            for (final ProductDesignMessage message : response) {
                ProductDesign design = ProductUtil.findProductDesign(dataList, message.getScene());
                String url = message.getUrl();
                if (!TextUtils.isEmpty(url)) {
                    String outPathUrl = ProductUtil.formatOutPath(mContext, scanMessage.getVid(), scanMessage.getUid(), scanMessage.getPid(),
                            message.getScene(), message.getHash(), message.getType());
                    if (FileUtil.checkFileExist(outPathUrl)) { //确认文件是否存在
                        if (design != null && !outPathUrl.equals(design.getImageUrl())) {
                            design.setImageUrl(outPathUrl);
                            if (ProductDesignMessage.TYPE_GIF.equals(message.getType())) {
                                design.setGif(true);
                            }
                            if (callback != null) {
                                callback.onUpdate(design);
                            }
                        }
                        if (ProductModel.MODEL_PRODUCT_MESSAGE.getValue().equals(message.getScene())) {
                            JL_Log.w(TAG, "update json: " + outPathUrl);
                            byte[] data = FileUtil.getBytes(outPathUrl);
                            String json = new String(data).trim();
                            ProductUtil.saveProductMessage(mContext, scanMessage.getVid(), scanMessage.getUid(), scanMessage.getPid(), json);
                        }
                        removeTaskList(scanMessage.getEdrAddr(), message.getHash());
                    } else { //文件不存在，下载文件
                        String fileUrl = ProductUtil.findCacheDesign(mContext, scanMessage.getVid(), scanMessage.getUid(), scanMessage.getPid(), message.getScene());
                        if (!TextUtils.isEmpty(fileUrl) && FileUtil.checkFileExist(fileUrl)) { //删除旧产品图片
                            JL_Log.w(TAG, "old file path : " + fileUrl);
                            FileUtil.deleteFile(new File(fileUrl));
                        }
                        if (!isContainsInterceptor(getUrlKey(url, message.getScene()))) {
                            interceptor.add(getUrlKey(url, message.getScene()));
                            downloadFile(url, outPathUrl, message, design, scanMessage, callback);
                        } else {
                            JL_Log.d(TAG, "url is exist. url : " + url);
                        }
                    }
                }
            }
        }
    }

    private void downloadFile(final String url, final String outPathUrl, final ProductDesignMessage message, final ProductDesign design,
                              final BleScanMessage scanMessage, final OnServiceUpdateCallback callback) {
        JL_HttpClient.getInstance().downloadFile(url, outPathUrl, new IDownloadListener() {

            @Override
            public void onStart(String startPath) {
                JL_Log.i(TAG, "requestProductDesign :: downloadFile onStart. startPath = " + startPath);
            }

            @Override
            public void onProgress(float progress) {

            }

            @Override
            public void onStop(String outputPath) {
                JL_Log.w(TAG, "requestProductDesign :: downloadFile onStop. outputPath = " + outputPath);
                removeTaskList(scanMessage.getEdrAddr(), message.getHash());
                if (ProductModel.MODEL_PRODUCT_MESSAGE.getValue().equals(message.getScene())) {
                    byte[] data = FileUtil.getBytes(outputPath);
                    String json = new String(data).trim();
                    JL_Log.w(TAG, "requestProductDesign :: length = " + json.getBytes().length);
                    ProductUtil.saveProductMessage(mContext, scanMessage.getVid(), scanMessage.getUid(), scanMessage.getPid(), json);
                    return;
                }
                if (design != null) {
                    design.setImageUrl(outPathUrl);
                    design.setGif(ProductUtil.isGifFile(outPathUrl));
                    if (callback != null) {
                        callback.onUpdate(design);
                    }
                }
                /*ImageCompress.getInstance().compress(mContext, outputPath, new IActionCallback<String>() {
                    @Override
                    public void onSuccess(String path) {
                        if (design != null) {
                            design.setImageUrl(path);
                            design.setGif(ProductUtil.isGifFile(path));
                            if (callback != null) {
                                callback.onUpdate(design);
                            }
                        }
                    }

                    @Override
                    public void onError(BaseError error) {
                        if (design != null) {
                            design.setImageUrl(outputPath);
                            design.setGif(ProductUtil.isGifFile(outputPath));
                            if (callback != null) {
                                callback.onUpdate(design);
                            }
                        }
                    }
                });*/
                interceptor.remove(getUrlKey(url, message.getScene()));
            }

            @Override
            public void onError(int code, String msg) {
                JL_Log.e(TAG, "requestProductDesign :: download error : code = " + code + ",message = " + msg);
                if (callback != null) {
                    callback.onError(code, msg);
                }
                interceptor.remove(getUrlKey(url, message.getScene()));
            }
        });
    }
}
