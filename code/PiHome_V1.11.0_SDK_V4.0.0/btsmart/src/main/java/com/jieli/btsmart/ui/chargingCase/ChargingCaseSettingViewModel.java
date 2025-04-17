package com.jieli.btsmart.ui.chargingCase;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.settings.v0.BrightnessSetting;
import com.jieli.bluetooth.bean.settings.v0.FlashlightSetting;
import com.jieli.bluetooth.bean.settings.v0.FunctionResource;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.bean.settings.v0.ScreenInfo;
import com.jieli.bluetooth.bean.settings.v0.SettingFunction;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.charging_case.OnChargingCaseListener;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfo;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.FileUtil;

import java.io.File;
import java.util.concurrent.Executors;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 智能充电仓设置逻辑实现
 * @since 2023/12/5
 */
public class ChargingCaseSettingViewModel extends BtBasicVM {

    public static final int FUNC_BRIGHTNESS = 0x01;
    public static final int FUNC_SCREEN_SAVER = 0x02;
    public static final int FUNC_BOOT_ANIM = 0x03;


    public static final int ERR_SAVE_FILE = -128;
    public static final int ERR_NONE_DATA = -129;

    @NonNull
    private final ChargingCaseOpImpl mChargingCaseOp;

    @NonNull
    private final ChargingCaseInfo chargingCaseInfo;
    public final MutableLiveData<String> resourcePathMLD = new MutableLiveData<>();
    public final MutableLiveData<ChargingCaseInfo> deviceInfoMLD = new MutableLiveData<>();
    public final MutableLiveData<OpResult<Integer>> functionResultMLD = new MutableLiveData<>();

    @NonNull
    public static String formatFileName(@NonNull String filePath) {
        String filename = AppUtil.getFileName(filePath);
        if (filename.contains("-")) {
            String suffix = AppUtil.getFileSuffix(filename);
            String content = AppUtil.getNameNoSuffix(filename);
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

    public ChargingCaseSettingViewModel() {
        super();
        mChargingCaseOp = ChargingCaseOpImpl.instance(mRCSPController.getRcspOp());
        DeviceInfo deviceInfo = getDeviceInfo();
        chargingCaseInfo = new ChargingCaseInfo(null == deviceInfo ? "" : deviceInfo.getEdrAddr());
        deviceConnectionMLD.observeForever(deviceConnectionObserver);
        mChargingCaseOp.addOnChargingCaseListener(mChargingCaseListener);
    }

    @NonNull
    public ChargingCaseInfo getChargingCaseInfo() {
        return chargingCaseInfo;
    }

    public void readScreenInfo() {
        mChargingCaseOp.readScreenInfo(getConnectedDevice(), new OnRcspActionCallback<ScreenInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ScreenInfo message) {
                JL_Log.d(tag, "readScreenInfo", "onSuccess : " + message);
                chargingCaseInfo.setScreenInfo(message);
                deviceInfoMLD.postValue(chargingCaseInfo);
                loadLocalResource(getContext(), message.getWidth(), message.getHeight());
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.i(tag, "readScreenInfo", "onError : " + error);
                loadLocalResource(getContext(), chargingCaseInfo.getScreenWidth(), chargingCaseInfo.getScreenHeight());
            }
        });
    }

    public void loadLocalResource(@NonNull Context context, int screenWidth, int screenHeight) {
        JL_Log.d(tag, "loadLocalResource", "screenWidth = " + screenWidth + ", screenHeight = " + screenHeight);
        String dirName = AppUtil.formatString("%dx%d", screenWidth, screenHeight); //动态调整
//        String dirName = AppUtil.formatString("%dx%d", ChargingCaseInfo.SCREEN_WIDTH, ChargingCaseInfo.SCREEN_HEIGHT);
        final String assetsName = SConstant.DIR_CHARGING_CASE + File.separator + dirName;
        final String outputDirPath = FileUtil.createFilePath(context, context.getPackageName(),
                SConstant.DIR_RESOURCE, SConstant.DIR_CHARGING_CASE, dirName);
        Executors.newSingleThreadExecutor().submit(() -> {
            //同步资源
            AppUtil.copyAssets(context, assetsName, outputDirPath);
            resourcePathMLD.postValue(outputDirPath);
        });
    }

    public void getBrightness() {
        mChargingCaseOp.getBrightness(getConnectedDevice(), new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                chargingCaseInfo.setBrightness(message);
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_BRIGHTNESS)
                        .setCode(0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_BRIGHTNESS)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void setBrightness(int value) {
        mChargingCaseOp.setBrightness(getConnectedDevice(), value, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                getBrightness();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_BRIGHTNESS)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void getCurrentScreenSaver() {
        mChargingCaseOp.getCurrentScreenSaver(getConnectedDevice(), new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                JL_Log.d(tag, "getCurrentScreenSaver", message.toString());
                chargingCaseInfo.setCurrentScreenSaver(message);
                Intent intent = new Intent(SConstant.ACTION_RESOURCE_INFO_CHANGE);
                intent.putExtra(SConstant.KEY_RESOURCE_TYPE, ResourceFile.TYPE_SCREEN_SAVER);
                intent.putExtra(SConstant.KEY_RESOURCE_INFO, message);
                getContext().sendBroadcast(intent);
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_SCREEN_SAVER)
                        .setCode(0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_SCREEN_SAVER)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void setCurrentScreenSaver(int devHandle, @NonNull String filePath) {
        mChargingCaseOp.setCurrentScreenSaver(getConnectedDevice(), devHandle, filePath, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                getCurrentScreenSaver();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_SCREEN_SAVER)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void getCurrentBootAnim() {
        mChargingCaseOp.getCurrentBootAnim(getConnectedDevice(), new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                chargingCaseInfo.setCurrentBootAnim(message);
                Intent intent = new Intent(SConstant.ACTION_RESOURCE_INFO_CHANGE);
                intent.putExtra(SConstant.KEY_RESOURCE_TYPE, ResourceFile.TYPE_BOOT_ANIM);
                intent.putExtra(SConstant.KEY_RESOURCE_INFO, message);
                getContext().sendBroadcast(intent);
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_BOOT_ANIM)
                        .setCode(0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_BOOT_ANIM)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void setCurrentBootAnim(int devHandle, @NonNull String filePath) {
        mChargingCaseOp.setCurrentBootAnim(getConnectedDevice(), devHandle, filePath, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                getCurrentBootAnim();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(FUNC_BOOT_ANIM)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    @Override
    protected void release() {
        deviceConnectionMLD.removeObserver(deviceConnectionObserver);
        mChargingCaseOp.removeOnChargingCaseListener(mChargingCaseListener);
        super.release();
    }

    private final Observer<DeviceConnectionData> deviceConnectionObserver = deviceConnectionData -> {

    };

    private final OnChargingCaseListener mChargingCaseListener = new OnChargingCaseListener() {

        @Override
        public void onChargingCaseEvent(BluetoothDevice device, SettingFunction function) {
            if (null == function) return;
            boolean isChange = false;
            switch (function.getFunction()) {
                case SettingFunction.FUNC_BRIGHTNESS: {
                    BrightnessSetting setting = (BrightnessSetting) function;
                    chargingCaseInfo.setBrightness(setting.getValue());
                    isChange = true;
                    break;
                }
                case SettingFunction.FUNC_FLASHLIGHT: {
                    FlashlightSetting setting = (FlashlightSetting) function;
                    chargingCaseInfo.setFlashlightOn(setting.getValue() == 1);
                    isChange = true;
                    break;
                }
                case SettingFunction.FUNC_USING_RESOURCE: {
                    FunctionResource state = (FunctionResource) function;
                    ResourceInfo resourceInfo = state.getResourceInfo();
                    if (null == resourceInfo) break;
                    switch (state.getSubFunction()) {
                        case FunctionResource.FUNC_SCREEN_SAVERS: {
                            chargingCaseInfo.setCurrentScreenSaver(resourceInfo);
                            break;
                        }
                        case FunctionResource.FUNC_BOOT_ANIM: {
                            chargingCaseInfo.setCurrentBootAnim(resourceInfo);
                            break;
                        }
                    }
                    Intent intent = new Intent(SConstant.ACTION_RESOURCE_INFO_CHANGE);
                    intent.putExtra(SConstant.KEY_RESOURCE_TYPE, state.getSubFunction());
                    intent.putExtra(SConstant.KEY_RESOURCE_INFO, resourceInfo);
                    getContext().sendBroadcast(intent);
                    isChange = true;
                    break;
                }
            }
            if (isChange) {
                deviceInfoMLD.postValue(chargingCaseInfo);
            }
        }
    };
}