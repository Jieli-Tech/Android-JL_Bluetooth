package com.jieli.btsmart.ui.multimedia;

import android.bluetooth.BluetoothDevice;
import android.content.res.Resources;
import android.content.res.TypedArray;

import androidx.lifecycle.MutableLiveData;

import com.jieli.audio.media_player.AudioFocusManager;
import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.bean.configuration.DeviceConfiguration;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.FunctionItemData;
import com.jieli.btsmart.tool.bluetooth.RingHandler;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.multimedia.control.id3.ID3ControlPresenterImpl;
import com.jieli.btsmart.util.JL_MediaPlayerServiceManager;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.FileObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MultiMediaVM
 * @author zqjasonZhong
 * @since 2025/5/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 多媒体界面逻辑实现
 */
public class MultiMediaVM extends BtBasicVM {

    private PlayControlImpl mPlayControl;
    private AudioFocusManager mAudioFocusManager;

    /**
     * 功能列表回调
     */
    public MutableLiveData<List<FunctionItemData>> functionListMLD = new MutableLiveData<>();
    /**
     * 当前功能回调
     */
    public MutableLiveData<Integer> currentFunctionMLD = new MutableLiveData<>();

    /**
     * 操作设备
     */
    private BluetoothDevice targetDevice;

    //ID3
    private int showPlayerFlag = ID3ControlPresenterImpl.PLAYER_FLAG_NONE;


    public MultiMediaVM() {
        RingHandler.getInstance().registerOnRingStatusListener(mOnRingStatusListener);
        FileBrowseManager.getInstance().addFileObserver(mFileObserver);
        mRCSPController.addBTRcspEventCallback(mRcspEventCallback);
        targetDevice = getConnectedDevice();
        initPlayControl();
    }

    public boolean isConnected() {
        return isConnectedDevice(targetDevice);
    }

    public void initPlayControl() {
        if (mPlayControl == null && PermissionUtil.hasReadMusicPermission(getContext())) {
            mPlayControl = PlayControlImpl.getInstance();
            mPlayControl.registerPlayControlListener(mPlayControlCallback);
        }
    }

    public List<Music> getLocalMusicList() {
        if (PermissionUtil.hasReadMusicPermission(getContext())) {
            return JL_MediaPlayerServiceManager.getInstance().getLocalMusic();
        }
        return Collections.emptyList();
    }

    public int getCurrentFunction() {
        Integer function = currentFunctionMLD.getValue();
        if (function == null) return -1;
        return function;
    }

    public void syncState() {
        syncFunctionList();
        syncCurrentFunction();
    }

    @Override
    protected void release() {
        releasePlayControl();
        RingHandler.getInstance().unregisterOnRingStatusListener(mOnRingStatusListener);
        FileBrowseManager.getInstance().removeFileObserver(mFileObserver);
        mRCSPController.removeBTRcspEventCallback(mRcspEventCallback);
        super.release();
    }

    private boolean isBtEnable(DeviceInfo deviceInfo) {
        if (null == deviceInfo) return false;
        return deviceInfo.isBtEnable() && !deviceInfo.isSupportDoubleConnection()
                && deviceInfo.getSdkType() != JLChipFlag.JL_CHIP_FLAG_695X_CHARGINGBIN
                && deviceInfo.getSdkType() != JLChipFlag.JL_CHIP_FLAG_701X_WATCH
                && deviceInfo.getSdkType() != JLChipFlag.JL_COLOR_SCREEN_CHARGING_CASE;
    }

    private boolean isSupportTranslation() {
        return true;
        /*final DeviceConfiguration configuration = getDeviceConfiguration();
        if (!(configuration instanceof TwsHeadsetConfiguration)) return false;
        return ((TwsHeadsetConfiguration) configuration).isSupportTranslation();*/
    }

    private DeviceConfiguration getDeviceConfiguration() {
        return mRCSPController.getStatusManager().getDeviceConfigure(targetDevice);
    }

    private int getPlayMode() {
        final PlayControlImpl playControl = mPlayControl;
        return playControl == null ? -1 : playControl.getMode();
    }

    private void releasePlayControl() {
        if (null != mPlayControl) {
            mPlayControl.unregisterPlayControlListener(mPlayControlCallback);
            mPlayControl = null;
        }
    }

    private void setPlayerFlag(int flag) {
        showPlayerFlag = flag;
    }

    private void addFunctionItem(List<FunctionItemData> list, FunctionItemData item) {
        if (null == list || null == item) return;
        item.setSupport(true);
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    private List<FunctionItemData> getDefaultFunctionList() {
        final Resources resources = getContext().getResources();
        if (null == resources) return Collections.emptyList();
        String[] nameArray = resources.getStringArray(R.array.multi_media_function_name_arr);
        TypedArray selectIconArray = resources.obtainTypedArray(R.array.multi_media_function_sel_icon_arr);
        TypedArray unselectedIconArray = resources.obtainTypedArray(R.array.multi_media_function_un_sel_icon_arr);
        List<FunctionItemData> list = new ArrayList<>();
        for (int i = 0; i < nameArray.length; i++) {
            FunctionItemData itemData = new FunctionItemData();
            String name = nameArray[i];
            int selIconResId = selectIconArray.getResourceId(i, R.drawable.ic_local_music_blue);
            int unSelIconResId = unselectedIconArray.getResourceId(i, R.drawable.ic_local_music_gray);
            itemData.setName(name);
            itemData.setNoSupportIconResId(unSelIconResId);
            itemData.setSupportIconResId(selIconResId);
            itemData.setItemType(i);
            itemData.setSupport(itemData.getItemType() == FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE);
            list.add(itemData);
        }
        selectIconArray.recycle();
        unselectedIconArray.recycle();
        return list;
    }

    private int convertFunctionType() {
        final DeviceInfo deviceInfo = getDeviceInfo(targetDevice);
        if (null == deviceInfo) return AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC;
        final int playMode = getPlayMode();
        switch (deviceInfo.getCurFunction()) {
            case AttrAndFunCode.SYS_INFO_FUNCTION_BT: //蓝牙模式
                if (playMode == PlayControlImpl.MODE_BT) {
                    if (showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_OTHER) {
                        return MultiMediaPresenterImpl.FRAGMENT_ID3;
                    } else if (showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_OTHER_EMPTY) {
                        return MultiMediaPresenterImpl.FRAGMENT_ID3_EMPTY;
                    } else if (showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO) {
                        return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO;
                    }
                } else if (playMode == PlayControlImpl.MODE_NET_RADIO) {
                    if (showPlayerFlag != ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO) {
                        setPlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO);
                    }
                    return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO;
                }
                return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL;
            case AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC: //设备音乐模式
                MusicStatusInfo musicStatusInfo = deviceInfo.getMusicStatusInfo();
                if (musicStatusInfo != null) {
                    int index = musicStatusInfo.getCurrentDev();
                    if (index == SDCardBean.INDEX_SD0 || index == SDCardBean.INDEX_SD1) {
                        return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD;
                    }
                    return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB;
                }
                boolean isSDOnline = FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_SD0)
                        || FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_SD1);
                boolean isUsbOnline = FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_USB);
                if (isSDOnline && !isUsbOnline) {
                    return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD;
                }
                return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB;
            case AttrAndFunCode.SYS_INFO_FUNCTION_RTC:   //时钟模式
                return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_ALARM;
            case AttrAndFunCode.SYS_INFO_FUNCTION_AUX:   //线控模式
                return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN;
            case AttrAndFunCode.SYS_INFO_FUNCTION_FM:    //广播电台模式
                return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM;
            case AttrAndFunCode.SYS_INFO_FUNCTION_LIGHT: //灯光模式
                return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LIGHT_SETTINGS;
            case AttrAndFunCode.SYS_INFO_ATTR_FM_TX:     //发射广播模式
                return FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX;
        }
        return AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC;
    }

    private void syncFunctionList() {
        List<FunctionItemData> defaultFunctionList = getDefaultFunctionList(); //获取所有支持的功能列表
        if (!isConnectedDevice(targetDevice)) { //设备未连接
            functionListMLD.postValue(defaultFunctionList);
            return;
        }
        final DeviceInfo deviceInfo = getDeviceInfo(targetDevice);
        if (null == deviceInfo) {
            functionListMLD.postValue(defaultFunctionList);
            return;
        }
        //设备已连接
        final List<FunctionItemData> list = new ArrayList<>();
        if (isBtEnable(deviceInfo)) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL));
        }
        if (deviceInfo.isDevMusicEnable()) {
            List<SDCardBean> storageList = FileBrowseManager.getInstance().getOnlineDev();
            if (deviceInfo.isSupportOfflineShow()) {
                storageList = FileBrowseManager.getInstance().getSdCardBeans();
            }
            for (SDCardBean storage : storageList) {
                if (storage.getType() == SDCardBean.SD) {
                    addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD));
                } else if (storage.getType() == SDCardBean.USB) {
                    addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB));
                }
            }
        }
        if (deviceInfo.isFmTxEnable()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX));
        }
        if (deviceInfo.isFmEnable()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM));
        }
        if (deviceInfo.isAuxEnable()) {
            if (deviceInfo.isSupportOfflineShow() || FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_LINE_IN)) {
                addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN));
            }
        }
        if (deviceInfo.isLightEnable()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LIGHT_SETTINGS));
        }
        if (deviceInfo.isRTCEnable()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_ALARM));
        }
        if (deviceInfo.isSupportSearchDevice()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE));
        }
        if (isBtEnable(deviceInfo) && !deviceInfo.isHideNetRadio()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO));
        }
        if (deviceInfo.isSupportSoundCard()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SOUND_CARD));
        }
        /*//@note 暂时关闭支持SPDIF功能
        if (deviceInfo.isSPDIFEnable()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SPDIF));
        }
        //@note 暂时关闭支持PC从机功能
        if (deviceInfo.isPCSlaveEnable()) {
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_PC_SLAVE));
        }*/
        if (isSupportTranslation()) { //是否支持翻译功能
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_TRANSLATE));
            addFunctionItem(list, defaultFunctionList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_TRANSLATE_RECORD));
        }
        functionListMLD.postValue(list);
    }

    private void syncCurrentFunction() {
        int curFunction = convertFunctionType();
        int cacheFunction = getCurrentFunction();
        if (cacheFunction != curFunction) {
            currentFunctionMLD.postValue(convertFunctionType());
        }
    }

    private final RingHandler.OnRingStatusListener mOnRingStatusListener = new RingHandler.OnRingStatusListener() {
        @Override
        public void onRingStatusChange(boolean isPlay) {

        }
    };

    private final FileObserver mFileObserver = new FileObserver() {
        @Override
        public void onFileReceiver(List<FileStruct> fileStructs) {

        }

        @Override
        public void onFileReadStop(boolean isEnd) {

        }

        @Override
        public void onFileReadStart() {

        }

        @Override
        public void onFileReadFailed(int reason) {

        }

        @Override
        public void onSdCardStatusChange(List<SDCardBean> onLineCards) {
            if (BluetoothUtil.deviceEquals(targetDevice, getConnectedDevice())) {
                syncState();
            }
        }

        @Override
        public void OnFlayCallback(boolean success) {

        }
    };

    private final PlayControlCallback mPlayControlCallback = new PlayControlCallback() {

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
        }

        @Override
        public void onModeChange(int mode) {
            super.onModeChange(mode);
        }

        @Override
        public void onFailed(String msg) {
            super.onFailed(msg);
        }
    };

    private final BTRcspEventCallback mRcspEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (targetDevice != null && !BluetoothUtil.deviceEquals(targetDevice, device)) return;
            switch (status) {
                case StateCode.CONNECTION_DISCONNECT:
                case StateCode.CONNECTION_FAILED: {
                    targetDevice = null;
                    syncState();
                    break;
                }
                case StateCode.CONNECTION_OK: {
                    targetDevice = device;
                    syncState();
                    break;
                }
            }
        }

        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (!BluetoothUtil.deviceEquals(targetDevice, device)) return;

        }

        @Override
        public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
            if (!BluetoothUtil.deviceEquals(targetDevice, device)) return;

        }

        @Override
        public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
            if (!BluetoothUtil.deviceEquals(targetDevice, device)) return;

        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            targetDevice = device;
            syncState();
        }
    };
}
