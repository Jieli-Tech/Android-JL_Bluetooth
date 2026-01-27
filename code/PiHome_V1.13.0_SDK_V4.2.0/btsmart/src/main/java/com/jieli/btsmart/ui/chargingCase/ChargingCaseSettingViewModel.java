package com.jieli.btsmart.ui.chargingCase;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.bean.settings.v0.SettingFunction;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.charging_case.OnChargingCaseListener;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfoChange;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.FileUtil;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.RegFile;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.DeleteCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 智能充电仓设置逻辑实现
 * @since 2023/12/5
 */
public class ChargingCaseSettingViewModel extends BtBasicVM {

    /**
     * 彩屏仓功能实现
     */
    @NonNull
    private final ChargingCaseOpImpl mChargingCaseOp;

    /**
     * 初始化回调
     */
    public final MutableLiveData<Boolean> initMLD = new MutableLiveData<>();
    /**
     * 资源路径回调
     */
    public final MutableLiveData<String> resourcePathMLD = new MutableLiveData<>();
    /**
     * 彩屏仓信息回调
     */
    public final MutableLiveData<ChargingCaseInfoChange> chargingCaseInfoMLD = new MutableLiveData<>();
    /**
     * 功能操作结果回调
     */
    public final MutableLiveData<OpResult<Integer>> functionResultMLD = new MutableLiveData<>();
    /**
     * 删除资源回调
     */
    public final MutableLiveData<StateResult<Boolean>> deleteResourceMLD = new MutableLiveData<>();
    /**
     * 同步状态参数
     */
    private final SyncStateWrapper syncStateWrapper = new SyncStateWrapper();

    public ChargingCaseSettingViewModel() {
        super();
        mChargingCaseOp = ChargingCaseOpImpl.instance(mRCSPController.getRcspOp());
        mChargingCaseOp.addOnChargingCaseListener(mChargingCaseListener);
    }

    @NonNull
    public ChargingCaseInfo getChargingCaseInfo() {
        ChargingCaseInfo info = mChargingCaseOp.getChargingCaseInfo(getConnectedDevice());
        if (null == info) {
            final DeviceInfo deviceInfo = getDeviceInfo();
            info = new ChargingCaseInfo(deviceInfo == null ? "" : deviceInfo.getEdrAddr());
        }
        return info;
    }

    public void syncDeviceState() {
        if (syncStateWrapper.isSyncing()) return;
        final ChargingCaseInfo info = getChargingCaseInfo();
        syncStateWrapper.reset();
        syncStateWrapper.setSyncing(true)
                .setSkipCurrentScreenSaver(null != info.getCurrentScreenSaver())
                .setSkipCurrentBootAnim(null != info.getCurrentBootAnim())
                .setSkipWallpaper(null != info.getCurrentWallpaper());
        tryToSyncDeviceState(getConnectedDevice());
    }

    public void getBrightness() {
        mChargingCaseOp.getBrightness(getConnectedDevice(), new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                JL_Log.d(tag, "getBrightness", "onSuccess ---> " + message);
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_BRIGHTNESS)
                        .setCode(0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_BRIGHTNESS)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void setBrightness(int value) {
        mChargingCaseOp.setBrightness(getConnectedDevice(), value, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                JL_Log.d(tag, "setBrightness", "onSuccess ---> " + value);
                /*functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_BRIGHTNESS)
                        .setCode(0));*/
                getBrightness();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_BRIGHTNESS)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void browseScreenSavers() {
        final SDCardBean storage = getOnlineStorage();
        if (null == storage) {
            BaseError error = new BaseError(ErrorCode.SUB_ERR_STORAGE_OFFLINE);
            functionResultMLD.postValue(new OpResult<Integer>()
                    .setData(ChargingCaseInfo.FUNC_SCREEN_SAVERS)
                    .setCode(error.getSubCode())
                    .setMessage(error.getMessage()));
            return;
        }
        final int devHandle = storage.getDevHandler();
        mChargingCaseOp.browseScreenSavers(getConnectedDevice(), devHandle, new OnRcspActionCallback<List<RegFile>>() {
            @Override
            public void onSuccess(BluetoothDevice device, List<RegFile> message) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_SCREEN_SAVERS)
                        .setCode(0));
                getCurrentScreenSaver();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_SCREEN_SAVERS)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void getCurrentScreenSaver() {
        mChargingCaseOp.getCurrentScreenSaver(getConnectedDevice(), new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                JL_Log.d(tag, "getCurrentScreenSaver", "" + message);
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER)
                        .setCode(0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void setCurrentScreenSaver(int devHandle, @NonNull String filePath) {
        mChargingCaseOp.setCurrentScreenSaver(getConnectedDevice(), devHandle, filePath, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER)
                        .setCode(0));
                getCurrentScreenSaver();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void getCurrentBootAnim() {
        mChargingCaseOp.getCurrentBootAnim(getConnectedDevice(), new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                JL_Log.d(tag, "getCurrentBootAnim", "" + message);
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_BOOT_ANIM)
                        .setCode(0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_BOOT_ANIM)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void browseWallpapers() {
        final SDCardBean storage = getOnlineStorage();
        if (null == storage) {
            BaseError error = new BaseError(ErrorCode.SUB_ERR_STORAGE_OFFLINE);
            functionResultMLD.postValue(new OpResult<Integer>()
                    .setData(ChargingCaseInfo.FUNC_WALLPAPERS)
                    .setCode(error.getSubCode())
                    .setMessage(error.getMessage()));
            return;
        }
        final int devHandle = storage.getDevHandler();
        mChargingCaseOp.browseWallPapers(getConnectedDevice(), devHandle, new OnRcspActionCallback<List<RegFile>>() {
            @Override
            public void onSuccess(BluetoothDevice device, List<RegFile> message) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_WALLPAPERS)
                        .setCode(0));
                getCurrentWallpaper();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_WALLPAPERS)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void getCurrentWallpaper() {
        mChargingCaseOp.getCurrentWallPaper(getConnectedDevice(), new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                JL_Log.d(tag, "getCurrentWallpaper", "" + message);
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_WALLPAPER)
                        .setCode(0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_WALLPAPER)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void setCurrentWallpaper(int devHandle, @NonNull String filePath) {
        mChargingCaseOp.setCurrentWallPaper(getConnectedDevice(), devHandle, filePath, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_WALLPAPER)
                        .setCode(0));
//                getCurrentWallpaper();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                functionResultMLD.postValue(new OpResult<Integer>()
                        .setData(ChargingCaseInfo.FUNC_CURRENT_WALLPAPER)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void deleteResourceList(@NonNull List<ResourceFile> resourceFiles) {
        if (deleteResourceMLD.getValue() != null && deleteResourceMLD.getValue().getState() == StateResult.STATE_WORKING) {
            JL_Log.d(tag, "deleteResourceList", "It is deleting.");
            return;
        }
        deleteResourceMLD.postValue(new StateResult<Boolean>().setState(StateResult.STATE_WORKING).setCode(0));
        List<ResourceInfo> resources = new ArrayList<>();
        for (ResourceFile file : resourceFiles) {
            if (file.getDevState() == ResourceFile.STATE_NOT_EXIST) {
                if (file.isCustomResource()) { //自定义资源
                    FileUtil.deleteFile(new File(file.getPath()));
                }
                continue;
            }
            final ResourceInfo info = file.getDevFile();
            if (null == info) continue;
            JL_Log.d(tag, "deleteResourceList", info.toString());
            resources.add(info); //添加删除资源对象
            if (file.isCustomResource()) { //自定义资源
                FileUtil.deleteFile(new File(file.getPath()));
            }
        }
        if (resources.isEmpty()) {
            JL_Log.d(tag, "deleteResourceList", "No device resource.");
            deleteResourceMLD.postValue(new StateResult<Boolean>()
                    .setState(StateResult.STATE_FINISH)
                    .setCode(0)
                    .setData(true));
            return;
        }
        mChargingCaseOp.deleteResources(getConnectedDevice(), resources, new DeleteCallback() {

            int error = -1;

            @Override
            public void onSuccess(FileStruct fileStruct) {
                JL_Log.d(tag, "deleteResourceList", "deleteResources ---> " + fileStruct);
                error = 0;
            }

            @Override
            public void onError(int code, FileStruct fileStruct) {
                error = code;
                JL_Log.i(tag, "deleteResourceList", "deleteResources#onError ---> " + fileStruct + ", code : " + code);
            }

            @Override
            public void onFinish() {
                deleteResourceMLD.postValue(new StateResult<Boolean>()
                        .setState(StateResult.STATE_FINISH)
                        .setCode(error)
                        .setMessage(ErrorCode.code2Msg(error))
                        .setData(true));
            }
        });
    }

    @Override
    protected void release() {
        mChargingCaseOp.removeOnChargingCaseListener(mChargingCaseListener);
        super.release();
    }

    private void loadLocalResource(@NonNull Context context, int screenWidth, int screenHeight) {
        JL_Log.d(tag, "loadLocalResource", "screenWidth = " + screenWidth + ", screenHeight = " + screenHeight);
        String dirName = AppUtil.formatString("%dx%d", screenWidth, screenHeight); //动态调整
        final String assetsName = SConstant.DIR_CHARGING_CASE + File.separator + dirName;
        final String outputDirPath = FileUtil.createFilePath(context, context.getPackageName(),
                SConstant.DIR_RESOURCE, SConstant.DIR_CHARGING_CASE, dirName);
        boolean isNeedUpdateResource = ConfigureKit.getInstance().isNeedUpdateResource();
        if(isNeedUpdateResource){
            Executors.newSingleThreadExecutor().submit(() -> {
                //同步资源
                FileUtil.deleteFile(new File(outputDirPath));
                ChargingBinUtil.copyAssets(context, assetsName, outputDirPath);
                ConfigureKit.getInstance().updateResourceVersion();
                resourcePathMLD.postValue(outputDirPath);
            });
            return;
        }
        resourcePathMLD.postValue(outputDirPath);
    }

    private SDCardBean getOnlineStorage() {
        final List<SDCardBean> storages = mChargingCaseOp.getOnlineStorages(getConnectedDevice());
        if (storages.isEmpty()) return null;
        SDCardBean target = storages.get(0);
        if (storages.size() > 1) {
            for (SDCardBean storage : storages) {
                if (storage.getIndex() == SDCardBean.INDEX_FLASH2 || storage.getIndex() == SDCardBean.INDEX_FLASH) {
                    target = storage;
                    break;
                }
            }
        }
        return target;
    }

    private void tryToSyncDeviceState(BluetoothDevice device) {
        if (!syncStateWrapper.isSyncing() || null == device) return;
        JL_Log.d(tag, "tryToSyncDeviceState", "" + syncStateWrapper);
        if (!syncStateWrapper.isSkipBrightness()) {
            tryToGetBrightness(device);
            return;
        }
        if (!syncStateWrapper.isSkipCurrentScreenSaver()) {
            tryToGetCurrentScreenSavers(device);
            return;
        }
        if (getChargingCaseInfo().isJL701N()) {
            if (!syncStateWrapper.isSkipCurrentBootAnim()) {
                tryToGetCurrentBootAnim(device);
                return;
            }
        } else {
            if (!syncStateWrapper.isSkipWallpaper()) {
                tryToLoadWallpaper(device);
                return;
            }
        }
        syncStateWrapper.reset();
    }

    private void tryToGetBrightness(BluetoothDevice device) {
        if (!syncStateWrapper.isSyncing() || null == device) return;
        mChargingCaseOp.getBrightness(device, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                JL_Log.d(tag, "tryToGetBrightness", "onSuccess : " + message);
                onError(device, new BaseError(ErrorCode.ERR_NONE, 0, ""));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "tryToGetBrightness", "onError : " + error);
                syncStateWrapper.setSkipBrightness(true);
                tryToSyncDeviceState(device);
            }
        });
    }

    private void tryToGetCurrentScreenSavers(BluetoothDevice device) {
        if (!syncStateWrapper.isSyncing() || null == device) return;
        mChargingCaseOp.getCurrentScreenSaver(device, new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                JL_Log.d(tag, "tryToGetCurrentScreenSavers", "onSuccess : " + message);
                onError(device, new BaseError(ErrorCode.ERR_NONE, 0, ""));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "tryToGetCurrentScreenSavers", "onError : " + error);
                syncStateWrapper.setSkipCurrentScreenSaver(true);
                tryToSyncDeviceState(device);
            }
        });
    }

    private void tryToGetCurrentBootAnim(BluetoothDevice device) {
        if (!syncStateWrapper.isSyncing() || null == device) return;
        mChargingCaseOp.getCurrentBootAnim(device, new OnRcspActionCallback<ResourceInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                JL_Log.d(tag, "tryToGetCurrentBootAnim", "onSuccess : " + message);
                onError(device, new BaseError(ErrorCode.ERR_NONE, 0, ""));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "tryToGetCurrentBootAnim", "onError : " + error);
                syncStateWrapper.setSkipCurrentBootAnim(true);
                tryToSyncDeviceState(device);
            }
        });
    }

    private void tryToLoadWallpaper(BluetoothDevice device) {
        if (!syncStateWrapper.isSyncing() || null == device) return;
        final SDCardBean storage = getOnlineStorage();
        if (null == storage) {
            JL_Log.w(tag, "tryToLoadWallpaper", "No Storage.");
            syncStateWrapper.setSkipWallpaper(true);
            tryToSyncDeviceState(device);
            return;
        }
        final int devHandle = storage.getDevHandler();
        mChargingCaseOp.browseWallPapers(device, devHandle, new OnRcspActionCallback<List<RegFile>>() {
            @Override
            public void onSuccess(BluetoothDevice device, List<RegFile> message) {
                if (null == message) message = new ArrayList<>();
                JL_Log.d(tag, "tryToLoadWallpaper", "onSuccess : " + message.size());
                mChargingCaseOp.getCurrentWallPaper(device, new OnRcspActionCallback<ResourceInfo>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, ResourceInfo message) {
                        JL_Log.d(tag, "tryToLoadWallpaper", "getCurrentWallPaper#onSuccess : " + message);
                        onError(device, new BaseError(ErrorCode.ERR_NONE, 0, ""));
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        JL_Log.w(tag, "tryToLoadWallpaper", "getCurrentWallPaper#onError : " + error);
                        syncStateWrapper.setSkipWallpaper(true);
                        if (error.getSubCode() != ErrorCode.ERR_NONE) {
                            chargingCaseInfoMLD.postValue(new ChargingCaseInfoChange(ChargingCaseInfo.FUNC_CURRENT_WALLPAPER,
                                    getChargingCaseInfo()));
                        }
                        tryToSyncDeviceState(device);
                    }
                });
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "tryToLoadWallpaper", "onError : " + error);
                syncStateWrapper.setSkipWallpaper(true);
                tryToSyncDeviceState(device);
            }
        });
    }

    private final OnChargingCaseListener mChargingCaseListener = new OnChargingCaseListener() {
        @Override
        public void onInit(BluetoothDevice device, int state) {
            boolean isInit = state == ErrorCode.ERR_NONE;
            initMLD.postValue(isInit);
            if (isInit) {
                final ChargingCaseInfo info = getChargingCaseInfo();
                loadLocalResource(getContext(), info.getScreenWidth(), info.getScreenHeight());
            }
        }

        @Override
        public void onChargingCaseInfoChange(BluetoothDevice device, @ChargingCaseInfo.Function int func, ChargingCaseInfo info) {
            chargingCaseInfoMLD.postValue(new ChargingCaseInfoChange(func, info));
        }

        @Override
        public void onChargingCaseEvent(BluetoothDevice device, SettingFunction function) {

        }
    };
}