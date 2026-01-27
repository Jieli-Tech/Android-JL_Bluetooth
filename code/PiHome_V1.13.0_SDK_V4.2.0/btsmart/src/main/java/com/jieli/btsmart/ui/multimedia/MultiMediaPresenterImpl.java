package com.jieli.btsmart.ui.multimedia;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.jieli.audio.media_player.AudioFocusManager;
import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.configuration.DeviceConfiguration;
import com.jieli.bluetooth.bean.configuration.TwsHeadsetConfiguration;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.FunctionItemData;
import com.jieli.btsmart.tool.bluetooth.RingHandler;
import com.jieli.btsmart.tool.playcontroller.NetRadioPlayControlImpl;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.btsmart.ui.multimedia.control.id3.ID3ControlPresenterImpl;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.JL_MediaPlayerServiceManager;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.FileObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_FUNCTION_BT;
import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_FUNCTION_FM;
import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC;
import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_FUNCTION_PC_SLAVE;
import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_FUNCTION_SPDIF;
import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;
import static com.jieli.btsmart.tool.playcontroller.PlayControlImpl.MODE_NET_RADIO;


public class MultiMediaPresenterImpl extends BluetoothBasePresenter implements IMultiMediaContract.IMultiMediaPresenter, FileObserver {
    private static final String TAG = MultiMediaPresenterImpl.class.getSimpleName();
    private final IMultiMediaContract.IMultiMediaView mView;
    private PlayControlImpl mPlayControl;
    private AudioFocusManager mAudioFocusManager;

    private ArrayList<FunctionItemData> mFunctionItemDataArrayList;
    private boolean mIsNetRadioResume = false;

    //ID3
    private final HashMap<String, Boolean> mID3StreamStatusMap = new HashMap<>();
    private int showPlayerFlag = ID3ControlPresenterImpl.PLAYER_FLAG_NONE;
    private int switchDevMode = -1;

    public static final byte FRAGMENT_ID3 = (byte) 0xf0;
    public static final byte FRAGMENT_NET_RADIO = (byte) 0xf1;
    public static final byte FRAGMENT_ID3_EMPTY = (byte) 0xf2;

    private final static int SWITCH_MODE_TIMEOUT = 6 * 1000; //6秒超时
    private final static int MSG_DEV_SWITCH_MODE_TIMEOUT = 1111;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_DEV_SWITCH_MODE_TIMEOUT) {
            JL_Log.w(TAG, "12134 >>> MSG_DEV_SWITCH_MODE_TIMEOUT <<<");
            if (mRCSPController.isDeviceConnected()) {
                if (switchDevMode != -1 && switchDevMode != getDeviceInfo().getCurFunction()) {
                    ToastUtil.showToastShort(R.string.msg_error_switch_mode);
                }
                mRCSPController.getCurrentDevModeInfo(mRCSPController.getUsingDevice(), null);
            }
            switchDevMode = -1;
        }
        return false;
    });

    public MultiMediaPresenterImpl(IMultiMediaContract.IMultiMediaView view) {
        super(view);
        mView = CommonUtil.checkNotNull(view);
        mRCSPController.addBTRcspEventCallback(mEventCallback);
        FileBrowseManager.getInstance().addFileObserver(this);
        RingHandler.getInstance().registerOnRingStatusListener(mOnRingStatusListener);
        getAudioFocusManager().registerOnAudioFocusChangeCallback(mOnAudioFocusChangeCallback);
        initPlayControl();
    }

    @Override
    public void start() {
        super.start();
        mFunctionItemDataArrayList = mView.getFunctionItemDataList();
        getDeviceSupportFunctionList();
        if (getCurrentFunc(-1) == FRAGMENT_ID3) {
            JL_Log.e(TAG, "start: switchTopControlFragment To ID3");
        }
        if (!ALLOW_SWITCH_FUN_DISCONNECT || isDevConnected()) {//
            mView.switchTopControlFragment(isDevConnected(), getCurrentFunc(-1));
        }
    }

    @Override
    public void destroy() {
        JL_Log.e(TAG, "onID3MusicInfo: closeID3InfoStream destroy");
        closeID3InfoStream();
        destroyRCSPController(mEventCallback);
        releasePlayControl();
        getAudioFocusManager().unregisterOnAudioFocusChangeCallback(mOnAudioFocusChangeCallback);
        FileBrowseManager.getInstance().removeFileObserver(this);
        RingHandler.getInstance().unregisterOnRingStatusListener(mOnRingStatusListener);
    }

    @Override
    public void getDeviceSupportFunctionList() {
        final BluetoothDevice device = getConnectedDevice();
        final DeviceInfo deviceInfo = getDeviceInfo(device);
        boolean isDevConnected = isConnectedDevice(device) && null != deviceInfo;
        JL_Log.d(TAG, "getDeviceSupportFunctionList", "isDevConnected : " + isDevConnected);
        if (isDevConnected) {
            ArrayList<FunctionItemData> allList = mView.getFunctionItemDataList();
            if (allList == null || allList.isEmpty()) return;
            ArrayList<FunctionItemData> result = new ArrayList<>();
            boolean isBtEnable = isBtEnable(deviceInfo);
            JL_Log.d(TAG, "getDeviceSupportFunctionList", "isBtEnable : " + isBtEnable);
            if (isBtEnable) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL));
            }
            if (deviceInfo.isDevMusicEnable()) {
                List<SDCardBean> sdCardBeans = FileBrowseManager.getInstance().getOnlineDev();
                if (deviceInfo.isSupportOfflineShow()) {
                    sdCardBeans = FileBrowseManager.getInstance().getSdCardBeans();
                }
                for (SDCardBean bean : sdCardBeans) {
                    JL_Log.d(TAG, bean.toString());
                    if (bean.getType() == SDCardBean.SD) {
                        addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD));
                    } else if (bean.getType() == SDCardBean.USB) {
                        addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB));
                    }
                }
            }
            if (deviceInfo.isFmTxEnable()) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX));
            }
            if (deviceInfo.isFmEnable()) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM));
            }
            if (deviceInfo.isAuxEnable()) {
                if (deviceInfo.isSupportOfflineShow() || FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_LINE_IN)) {
                    addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN));
                }
            }
            if (deviceInfo.isLightEnable()) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LIGHT_SETTINGS));
            }
            if (deviceInfo.isRTCEnable()) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_ALARM));
            }
            if (deviceInfo.isSupportSearchDevice()) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE));
            }
            if (isBtEnable(deviceInfo) && !deviceInfo.isHideNetRadio()) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO));
            }

            if (deviceInfo.isSupportSoundCard()) {
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SOUND_CARD));
            }
//            @note 暂时关闭支持SPDIF功能
//            if (deviceInfo.isSPDIFEnable()) {
//                itemData = allList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SPDIF);
//                itemData.setSupport(true);
//                result.add(itemData);
//            }
//            @note 暂时关闭支持PC从机功能
//            if (deviceInfo.isPCSlaveEnable()) {
//                itemData = allList.get(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_PC_SLAVE);
//                itemData.setSupport(true);
//                result.add(itemData);
//            }
            final DeviceConfiguration configuration = getDeviceConfiguration(device);
            if(null == configuration){
                readDeviceConfiguration(device);
            }else if(isSupportTranslation(device)){  //是否支持翻译功能
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_TRANSLATE));
                addFunctionItem(result, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_TRANSLATE_RECORD));
            }
            mFunctionItemDataArrayList = result;
        } else {
            mFunctionItemDataArrayList = mView.getFunctionItemDataList();
        }
        mView.updateFunctionItemDataList(mFunctionItemDataArrayList);
    }

    @Override
    public void refreshDevMsg() {
        if (isDevConnected()) {
            DeviceInfo deviceInfo = getDeviceInfo();
            if (deviceInfo != null) {
                if (deviceInfo.getCurFunction() == SYS_INFO_FUNCTION_BT) {
                    syncID3Info();
                }
            }
        }
    }

    @Override
    public void stopUpdateDevMsg() {
        if (isDevConnected()) {
            DeviceInfo deviceInfo = getDeviceInfo();
            if (deviceInfo != null && !isID3Play()) {
                if (deviceInfo.getCurFunction() == SYS_INFO_FUNCTION_BT) {
                    JL_Log.d(TAG, "stopUpdateDevMsg", "closeID3InfoStream");
                    closeID3InfoStream();
                }
            }
        }
    }

    @Override
    public int showPlayerFlag() {
        return showPlayerFlag;
    }

    @Override
    public ID3MusicInfo getCurrentID3Info() {
        if (null == getDeviceInfo()) return null;
        return getDeviceInfo().getiD3MusicInfo();
    }

    @Override
    public void switchToFMMode() {
        mRCSPController.switchDeviceMode(mRCSPController.getUsingDevice(), SYS_INFO_FUNCTION_FM, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                tryToSwitchDevMode(AttrAndFunCode.SYS_INFO_FUNCTION_FM);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                ToastUtil.showToastShort(R.string.msg_error_switch_mode);
            }
        });
    }

    @Override
    public void switchToBTMode() {
        mRCSPController.switchDeviceMode(mRCSPController.getUsingDevice(), AttrAndFunCode.SYS_INFO_FUNCTION_BT, null);
    }

    @Override
    public void switchToLineInMode() {
        mRCSPController.switchDeviceMode(mRCSPController.getUsingDevice(), AttrAndFunCode.SYS_INFO_FUNCTION_AUX, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //命令成功不代表进来LineIn模式成功，需要等待设备发送0x09命令推送模式信息确认是否进入该模式，
                //先增加超时限制，限制切换模式2秒内回复
//                    getBluetoothHelper().getAuxStatusInfo(null);
//                    mView.switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_AUX);
                tryToSwitchDevMode(AttrAndFunCode.SYS_INFO_FUNCTION_AUX);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.i(TAG, "12134 switchToLineInMode ::  onErrCode : " + error);
                ToastUtil.showToastShort(R.string.msg_error_switch_mode);
            }
        });
    }

    @Override
    public void switchToSPDIFMode() {
        mRCSPController.switchDeviceMode(mRCSPController.getUsingDevice(), SYS_INFO_FUNCTION_SPDIF, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                tryToSwitchDevMode(AttrAndFunCode.SYS_INFO_FUNCTION_SPDIF);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                ToastUtil.showToastShort(R.string.msg_error_switch_mode);
            }
        });
    }

    @Override
    public void switchToPCSlaveMode() {
        mRCSPController.switchDeviceMode(mRCSPController.getUsingDevice(), SYS_INFO_FUNCTION_PC_SLAVE, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                tryToSwitchDevMode(AttrAndFunCode.SYS_INFO_FUNCTION_PC_SLAVE);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                ToastUtil.showToastShort(R.string.msg_error_switch_mode);
            }
        });
    }

    @Override
    public void getCurrentModeInfo() {
        mRCSPController.getCurrentDevModeInfo(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void setDevStorage(int devHandler, OnRcspActionCallback<Boolean> callback) {
        mRCSPController.setDevStorage(mRCSPController.getUsingDevice(), devHandler, callback);
    }

    @Override
    public List<Music> getLocalMusicList(Context context) {
        if (PermissionUtil.hasReadMusicPermission(context)) {
            return JL_MediaPlayerServiceManager.getInstance().getLocalMusic();
        }
        return Collections.emptyList();
    }

    public boolean isOpenID3Stream() {
        if (getConnectedDevice() == null) return false;
        Boolean isID3StreamOpen = mID3StreamStatusMap.get(getConnectedDevice().getAddress());
        return isID3StreamOpen != null && isID3StreamOpen;
    }

    public boolean isBtEnable(DeviceInfo deviceInfo) {
        if (null == deviceInfo) return false;
        return deviceInfo.isBtEnable() && !deviceInfo.isSupportDoubleConnection()
                && deviceInfo.getSdkType() != JLChipFlag.JL_CHIP_FLAG_695X_CHARGINGBIN
                && deviceInfo.getSdkType() != JLChipFlag.JL_COLOR_SCREEN_CHARGING_CASE;
    }

    public void initPlayControl() {
        if (mPlayControl == null && PermissionUtil.hasReadMusicPermission(MainApplication.getApplication())) {
            mPlayControl = PlayControlImpl.getInstance();
            mPlayControl.registerPlayControlListener(mControlCallback);
        }
    }

    private void releasePlayControl() {
        if (null != mPlayControl) {
            mPlayControl.unregisterPlayControlListener(mControlCallback);
            mPlayControl = null;
        }
    }

    private AudioFocusManager getAudioFocusManager() {
        if (mAudioFocusManager == null) {
            mAudioFocusManager = AudioFocusManager.getInstance();
            mAudioFocusManager.init(AppUtil.getContext());
        }
        return mAudioFocusManager;
    }

    private boolean isSupportTranslation(BluetoothDevice device) {
        final DeviceConfiguration configuration = getDeviceConfiguration(device);
        if (!(configuration instanceof TwsHeadsetConfiguration)) return false;
        return ((TwsHeadsetConfiguration) configuration).isSupportTranslation();
    }

    private void setPlayerFlag(int flag) {
        showPlayerFlag = flag;
    }

    private DeviceConfiguration getDeviceConfiguration(BluetoothDevice device) {
        return mRCSPController.getStatusManager().getDeviceConfigure(device);
    }

    private FunctionItemData getFunctionItem(List<FunctionItemData> list, int functionType) {
        if (null == list || list.isEmpty()) return null;
        for (FunctionItemData itemData : list) {
            if (itemData.getItemType() == functionType) {
                return itemData;
            }
        }
        return null;
    }

    private void addFunctionItem(List<FunctionItemData> list, FunctionItemData item) {
        if (null == list || null == item) return;
        item.setSupport(true);
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    /**
     * 打开ID3推流
     */
    private void syncID3Info() {
        if (!isDevConnected() || getDeviceInfo().getSdkType() < JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET)
            return;
        if (isOpenID3Stream()) {
            getID3MusicInfo();
        } else {
            mRCSPController.openID3MusicNotification(mRCSPController.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
                @Override
                public void onSuccess(BluetoothDevice device, Boolean message) {
                    mID3StreamStatusMap.put(device.getAddress(), true);
                    getID3MusicInfo();
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {
                    JL_Log.e(TAG, "openID3InfoStream >>>>>>>  failed." + error);
                }
            });
        }
    }

    /**
     * 关闭ID3推流
     */
    private void closeID3InfoStream() {
        if (!isDevConnected() || getDeviceInfo().getSdkType() < JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET)
            return;
        JL_Log.d(TAG, "closeID3InfoStream", "start");
        mRCSPController.closeID3MusicNotification(mRCSPController.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                JL_Log.d(TAG, "closeID3InfoStream", "onSuccess ---> " + message);
                mID3StreamStatusMap.remove(device.getAddress());
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(TAG, "closeID3InfoStream", "onError ---> " + error);
            }
        });
    }

    private void getID3MusicInfo() {
        JL_Log.d(TAG, "getID3MusicInfo", " ");
        mRCSPController.getID3MusicInfo(mRCSPController.getUsingDevice(), null);
    }

    private void onChangePlayerFlag(int flag) {
        setPlayerFlag(flag);
        JL_Log.d(TAG, "onChangePlayerFlag", "flag : " + flag);
        if (getDeviceInfo() != null && (getDeviceInfo().getCurFunction() == SYS_INFO_FUNCTION_BT)) {
            mView.onChangePlayerFlag(flag);
            if ((flag == ID3ControlPresenterImpl.PLAYER_FLAG_OTHER || flag == ID3ControlPresenterImpl.PLAYER_FLAG_OTHER_EMPTY)) {
                if (!isOpenID3Stream()) {
                    syncID3Info();
                }
            } else if (isOpenID3Stream()) {
                closeID3InfoStream();
            }
        }
    }

    private boolean isLocalMusicGetFocus() {
        return showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL;
    }

    private boolean isPlay() {
        final PlayControlImpl playControl = mPlayControl;
        if (null == playControl) return false;
        return playControl.isPlay();
    }

    private boolean isID3Play() {//满足的条件：本地没播放 且 没有焦点 且 查找设备的响铃没响
        boolean isPlay = isPlay();
        JL_Log.d(TAG, "isID3Play", "isMusicPlay " + !getAudioFocusManager().isHasAudioFocus() + "     " + getAudioFocusManager().isMusicPlay() + "     " + !isPlay + "     " + !RingHandler.getInstance().isPlayAlarmRing());
        return !isPlay && !getAudioFocusManager().isHasAudioFocus() && getAudioFocusManager().isMusicPlay() && !RingHandler.getInstance().isPlayAlarmRing();
    }


    private int getPlayMode() {
        final PlayControlImpl playControl = mPlayControl;
        return playControl == null ? -1 : playControl.getMode();
    }

    //获取当前模式
    private byte getCurrentFunc(int func) {
        if (func == -1) {
            func = getDeviceInfo() == null ? 0 : getDeviceInfo().getCurFunction();
        }
        final PlayControlImpl playControl = mPlayControl;
        final int mode = getPlayMode();
        JL_Log.d(TAG, "getCurrentFunc", "CurFunction: " + func + ", showPlayerFlag : " + showPlayerFlag + ", mode : " + mode);
        if (playControl != null && func == SYS_INFO_FUNCTION_BT) {
            if (showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_OTHER) {
                func = FRAGMENT_ID3;
            } else if (mode == MODE_NET_RADIO) {
                if (showPlayerFlag != ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO) {
                    onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO);
                }
                func = FRAGMENT_NET_RADIO;
            } else if (showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_OTHER_EMPTY) {
                func = FRAGMENT_ID3_EMPTY;
            }
        } else if (playControl != null && func == SYS_INFO_FUNCTION_MUSIC) {
            if (mode == MODE_NET_RADIO) {
                if (showPlayerFlag != ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO) {
                    onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO);
                }
                func = FRAGMENT_NET_RADIO;
            }
        }
        return CHexConver.intToByte(func);
    }

    private void tryToSwitchDevMode(int mode) {
        JL_Log.i(TAG, "tryToSwitchDevMode", "switchDevMode = " + switchDevMode + ", mode = " + mode);
        if (switchDevMode != mode) {
            switchDevMode = mode;
            mHandler.removeMessages(MSG_DEV_SWITCH_MODE_TIMEOUT);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_DEV_SWITCH_MODE_TIMEOUT, switchDevMode), SWITCH_MODE_TIMEOUT);
        }
    }

    private void readDeviceConfiguration(BluetoothDevice device) {
        mRCSPController.readDeviceConfiguration(device, new OnRcspActionCallback<DeviceConfiguration>() {
            @Override
            public void onSuccess(BluetoothDevice device, DeviceConfiguration message) {
                if (isSupportTranslation(device) && mFunctionItemDataArrayList != null) {
                    List<FunctionItemData> allList = mView.getFunctionItemDataList();
                    addFunctionItem(mFunctionItemDataArrayList, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_TRANSLATE));
                    addFunctionItem(mFunctionItemDataArrayList, getFunctionItem(allList, FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_TRANSLATE_RECORD));
                    mView.updateFunctionItemDataList(mFunctionItemDataArrayList);
                }
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {

            }
        });
    }

    private final RingHandler.OnRingStatusListener mOnRingStatusListener = isPlay -> {
        if ((isPlay && showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_OTHER) && getDeviceInfo() != null
                && getDeviceInfo().getiD3MusicInfo() != null && !getDeviceInfo().getiD3MusicInfo().isPlayStatus()) {
            setPlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL);
        }
    };
    private final BTRcspEventCallback mEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            mID3StreamStatusMap.remove(device.getAddress());
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device))
                return;
            switch (status) {
                case StateCode.CONNECTION_DISCONNECT:
                case StateCode.CONNECTION_FAILED:
                    getDeviceSupportFunctionList();
                    mView.switchTopControlFragment(isDevConnected(), SYS_INFO_FUNCTION_BT);
                    setPlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_NONE);
                    break;
                case StateCode.CONNECTION_OK:
                    getDeviceSupportFunctionList();
                    setPlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_NONE);
                    DeviceInfo info = getDeviceInfo(device);
                    if (info != null) {
                        if (info.getCurFunction() == SYS_INFO_FUNCTION_BT) {
                            if (null != mPlayControl) {
                                mPlayControl.refresh();
                            }
                            syncID3Info();
                        }
                        mView.switchTopControlFragment(isDevConnected(), getCurrentFunc(info.getCurFunction()));
                    }
                    break;
            }
        }

        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device))
                return;
            JL_Log.d(TAG, "onDeviceFunChange", " mode : " + mode + ", switchDevMode : " + switchDevMode);
            if (switchDevMode != -1 && switchDevMode != mode) {
                ToastUtil.showToastShort(R.string.msg_error_switch_mode);
            }
            if (mode == SYS_INFO_FUNCTION_BT) {
                syncID3Info();
            } else {
                if (PlayControlImpl.getInstance().getMode() == MODE_NET_RADIO) {
                    PlayControlImpl.getInstance().pause();
                }
                closeID3InfoStream();
            }
            switchDevMode = -1;
            mHandler.removeMessages(MSG_DEV_SWITCH_MODE_TIMEOUT);
            mView.switchTopControlFragment(isDevConnected(), getCurrentFunc(mode));
        }

        @Override
        public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device))
                return;
            JL_Log.d(TAG, "onDeviceMusic status--->" + statusInfo);
            //设置插拔卡的选中状态
            if (mRCSPController.getDeviceInfo(device).getCurFunction() == AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC) {
                mView.switchTopControlFragment(isDevConnected(), AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC);
            }
        }

        @Override
        public void onID3MusicInfo(BluetoothDevice device, ID3MusicInfo id3MusicInfo) {
            if (mRCSPController.getUsingDevice() != null && !mRCSPController.isUsingDevice(device))
                return;
            final boolean isID3Play = isID3Play();
            JL_Log.d(TAG, "onID3MusicInfo", "showPlayerFlag : " + showPlayerFlag + ", isID3Play : " + isID3Play + "\n" + id3MusicInfo);
            if ((id3MusicInfo.getTitle() == null) && (id3MusicInfo.getAlbum() == null) && (id3MusicInfo.getArtist() == null) && isID3Play) {//ID3播放且数据为空
                onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_OTHER_EMPTY);
                return;
            }
            if (TextUtils.isEmpty(id3MusicInfo.getArtist()) || id3MusicInfo.getTotalTime() == 0) {//不合法ID3信息
                return;
            }
            if (id3MusicInfo.getCurrentTime() == 0 || id3MusicInfo.getCurrentTime() <= id3MusicInfo.getTotalTime() + 1) {
                if (id3MusicInfo.isPlayStatus() != isID3Play) {
                    id3MusicInfo.setPlayStatus(isID3Play);
                    getDeviceInfo().getiD3MusicInfo().setPlayStatus(isID3Play);
                    mRCSPController.getCallbackManager().onID3MusicInfo(device, id3MusicInfo);
                }
            }
            if (showPlayerFlag != ID3ControlPresenterImpl.PLAYER_FLAG_OTHER) {
                if (isID3Play()) {
                    onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_OTHER);
                } else if (showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_NONE) {
                    onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL);
                } else if ((showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL /*|| showPlayerFlag == ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO*/) && getAudioFocusManager().isHasAudioFocus() && isPlay()) {
                    onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL);
                }
            }

            //本地播放器暂时不会传递播放信息
//            if (JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().getCurrentMusicName().equals(id3MusicInfo.getTitle())
//                    && JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().getCurrentMusicArtist().equals(id3MusicInfo.getArtist()) && isID3Play()) {//为处理快应用音乐ID3错误，暂且只过滤本地音乐这一情况
//                onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_OTHER_EMPTY);
//                return;
//            }
        }
    };

    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTimeChange(int current, int total) {
            super.onTimeChange(current, total);
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            final int mode = getPlayMode();
            JL_Log.d(TAG, "onPlayStateChange", "isPlay : " + isPlay + "\tmode=" + mode);
            if (!isLocalMusicGetFocus() && isPlay) {
                if (mode == MODE_NET_RADIO) {
                    onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO);
                } else {
                    onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL);
                }
            }
        }

        @Override
        public void onModeChange(int mode) {
            JL_Log.d(TAG, "onModeChange", "mode : " + mode);
            if (!isDevConnected() && !ALLOW_SWITCH_FUN_DISCONNECT) return;
            if (mode == PlayControlImpl.MODE_BT) {
                mView.switchTopControlFragment(isDevConnected() || ALLOW_SWITCH_FUN_DISCONNECT, SYS_INFO_FUNCTION_BT);
            } else if (mode == MODE_NET_RADIO) {
                mView.switchTopControlFragment(isDevConnected() || ALLOW_SWITCH_FUN_DISCONNECT, FRAGMENT_NET_RADIO);
            }
        }

        @Override
        public void onFailed(String msg) {
            super.onFailed(msg);
            if (msg != null && getPlayMode() == MODE_NET_RADIO) {
                ToastUtil.showToastShort(msg);
            }
        }
    };
    private final NetRadioPlayControlImpl mNetRadioPlayControl = NetRadioPlayControlImpl.getInstance(AppUtil.getContext());
    private final AudioFocusManager.OnAudioFocusChangeCallback mOnAudioFocusChangeCallback = new AudioFocusManager.OnAudioFocusChangeCallback() {
        @Override
        public void onAudioFocusLossTransient() {//短暂失去焦点
            JL_Log.w(TAG, "onAudioFocusLossTransient", "");
            syncID3Info();
//            onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_OTHER);
            if (mNetRadioPlayControl.isPlay()) {
                mIsNetRadioResume = true;
                mNetRadioPlayControl.pause();
            }
        }

        @Override
        public void onAudioFocusGain() {//获得焦点
            JL_Log.w(TAG, "onAudioFocusLossTransient", "");
            if (getPlayMode() == MODE_NET_RADIO) {
                onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO);
                if (mIsNetRadioResume) {
                    mNetRadioPlayControl.play();
                }
            } else {
                onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL);
            }
        }

        @Override
        public void onAudioFocusLossTransientCanDuck() {
//            mLocalMusicGetFocus =true;
        }

        @Override
        public void onAudioLoss() {
            JL_Log.w(TAG, "onAudioLoss", "mIsBuffering : " + mNetRadioPlayControl.isBuffering());
//            if (mNetRadioPlayControl.()) return;//网络电台正在播放就不切换
            if (mNetRadioPlayControl.isBuffering()) return;//没改变为false
            syncID3Info();
//            onChangePlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_OTHER);
            if (mNetRadioPlayControl.isPlay()) {
                mIsNetRadioResume = true;
                mNetRadioPlayControl.pause();
            }
        }
    };

    @Override
    public void onSdCardStatusChange(List<SDCardBean> onLineCards) {
        JL_Log.i(TAG, "onSdCardStatusChange", "isSupportOfflineShow --> " + (mRCSPController.getDeviceInfo() != null && !mRCSPController.getDeviceInfo().isSupportOfflineShow()));
        if (getDeviceInfo() != null) {
            getDeviceSupportFunctionList();
        }
    }

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
    public void OnFlayCallback(boolean success) {

    }
}
