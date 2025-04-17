package com.jieli.btsmart.ui.settings.device;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.device.voice.AdaptiveData;
import com.jieli.bluetooth.bean.device.voice.SceneDenoising;
import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.bean.device.voice.VocalBooster;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.device.voice.WindNoiseDetection;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnAdaptiveANCListener;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.configure.DoubleConnectionSp;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.ui.base.bluetooth.BluetoothBasePresenter;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.NetworkStateHelper;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.jl_http.bean.ProductModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 设备设置逻辑实现
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
public class DeviceSettingsPresenterImpl extends BluetoothBasePresenter implements IDeviceSettingsContract.IDeviceSettingsPresenter, NetworkStateHelper.Listener, ProductCacheManager.OnUpdateListener {

    private static final int ANC_SETTING_TIMEOUT = 20 * 1000; //ANC自适应超时时间 - 20s

    private final IDeviceSettingsContract.IDeviceSettingsView mView;
    private NetworkHelper mNetworkHelper;
    private final AppCompatActivity mContext;
    private byte[] selectedVoiceModes;

    private final List<VoiceMode> list = new ArrayList<>();
    private int selectModeId = 1;
    private final AdaptiveData mAdaptiveData = new AdaptiveData();

    private boolean isCmdOpSetting = false;

    private static final int MSG_OPEN_SMART_NO_PICK_TIMEOUT = 0x001;
    private final Handler mUIHandler = new Handler(Looper.getMainLooper());

    public DeviceSettingsPresenterImpl(AppCompatActivity activity, IDeviceSettingsContract.IDeviceSettingsView view) {
        super(view);
        mContext = SystemUtil.checkNotNull(activity);
        mView = SystemUtil.checkNotNull(view);
        mNetworkHelper = NetworkHelper.getInstance();
        mNetworkHelper.registerNetworkEventCallback(mNetworkEventCallback);
        NetworkStateHelper.getInstance().registerListener(this);
        ProductCacheManager.getInstance().registerListener(this);
        synchronizationsViewState();

        getRCSPController().addBTRcspEventCallback(mBTEventCallback);

        if (SConstant.TEST_ANC_FUNC) {
            VoiceMode voiceMode = new VoiceMode().setMode(VoiceMode.VOICE_MODE_CLOSE).setLeftMax(0).setRightMax(0).setLeftCurVal(0).setRightCurVal(0);
            list.add(voiceMode);
            voiceMode = new VoiceMode().setMode(VoiceMode.VOICE_MODE_DENOISE).setLeftMax(5000).setRightMax(5000).setLeftCurVal(1600).setRightCurVal(500);
            list.add(voiceMode);
            voiceMode = new VoiceMode().setMode(VoiceMode.VOICE_MODE_TRANSPARENT).setLeftMax(5000).setRightMax(5000).setLeftCurVal(1000).setRightCurVal(2000);
            list.add(voiceMode);

            selectedVoiceModes = new byte[]{(byte) 1, (byte) 2};
        }
    }

    @Override
    public void checkNetworkAvailable() {
        if (SConstant.CHANG_DIALOG_WAY) {
            mView.onNetworkState(NetworkStateHelper.getInstance().isNetworkIsAvailable());
        } else if (mNetworkHelper != null) {
            mNetworkHelper.checkNetworkIsAvailable();
        }
    }

    @Override
    public void disconnectBtDevice() {
        final BluetoothDevice device = getConnectedDevice();
        if(null == device) return;
        DevicePopDialogFilter.getInstance().addIgnoreDevice(device.getAddress());
        getRCSPController().disconnectDevice(device);
    }

    private boolean isCanUseTws(BluetoothDevice device) {
        return getDeviceInfo(device) != null && UIHelper.isCanUseTwsCmd(getDeviceInfo(device).getSdkType());
    }

    @Override
    public void updateDeviceADVInfo() {
        if (!isCanUseTws(getConnectedDevice())) return;
        mRCSPController.getDeviceSettingsInfo(getConnectedDevice(), 0xffffffff, new OnRcspActionCallback<ADVInfoResponse>() {
            @Override
            public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                JL_Log.w(tag, "updateDeviceADVInfo >> :  " + message);
                if (mNetworkHelper != null && !SConstant.CHANG_DIALOG_WAY) {
                    mNetworkHelper.setADVInfo(message);
                }
                if (message.getModes() != null && !SConstant.TEST_ANC_FUNC) {
                    updateSelectedVoiceModes(message.getModes());
                }
                mView.onADVInfoUpdate(message);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "updateDeviceADVInfo >> onErrCode :  " + error);
                mView.onGetADVInfoFailed(error);
            }
        });
    }

    @Override
    public void changeDeviceName(final String name, final boolean isImmediately) {
        if (TextUtils.isEmpty(name)) return;
        mRCSPController.configDeviceName(getConnectedDevice(), name, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                if (message != null && message == 0) {
                    if (isImmediately) {
                        rebootDevice();
                    } else {
                        updateDeviceADVInfo();
                    }
                    mView.onSetNameSuccess(name);
                } else {
                    int code = message == null ? -1 : message;
                    String msg = mContext.getString(R.string.device_name_failure);
                    if (code == StateCode.ADV_SETTINGS_ERROR_DEVICE_NAME_LENGTH_OVER_LIMIT) {
                        msg = mContext.getString(R.string.settings_failed_dev_name_len_over_limit);
                    }
                    BaseError error = new BaseError(code, msg);
                    error.setOpCode(Command.CMD_ADV_SETTINGS);
                    mView.onSetNameFailed(error);
                }
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                mView.onSetNameFailed(error);
            }
        });
    }

    @Override
    public void modifyHeadsetFunctions(int position, int type, byte[] params) {
        if (params == null) return;
        if (SConstant.TEST_ANC_FUNC && position == -1) {
            mView.onConfigureSuccess(position, 0);
            return;
        }
        mRCSPController.modifyDeviceSettingsInfo(getConnectedDevice(), type, params, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                if (message != null && message == 0) {
                    mView.onConfigureSuccess(position, message);
                } else {
                    int code = message == null ? -1 : message;
                    String msg = mContext.getString(R.string.settings_failed);
                    switch (code) {
                        case StateCode.ADV_SETTINGS_ERROR_IN_GAME_MODE:
                            msg = mContext.getString(R.string.settings_failed_by_game_mode);
                            break;
                        case StateCode.ADV_SETTINGS_ERROR_DEVICE_NAME_LENGTH_OVER_LIMIT:
                            msg = mContext.getString(R.string.settings_failed_dev_name_len_over_limit);
                            break;
                        case StateCode.ADV_SETTINGS_ERROR_LED_SETTINGS_FAILED:
                            msg = mContext.getString(R.string.settings_failed_led_settings);
                            break;
                    }
                    BaseError error = new BaseError(code, msg);
                    error.setOpCode(Command.CMD_ADV_SETTINGS);
                    onError(device, error);
                }
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                mView.onConfigureFailed(error);
            }
        });
    }

    @Override
    public void rebootDevice() {
        mRCSPController.rebootDevice(getConnectedDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                mView.onRebootSuccess();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                mView.onRebootFailed(error);
            }
        });
    }

    @Override
    public List<VoiceMode> getNoiseModeMsg() {
        if (SConstant.TEST_ANC_FUNC) {
            return list;
        } else {
            List<VoiceMode> list = null;
            if (getDeviceInfo() != null) {
                list = getDeviceInfo().getVoiceModeList();
                if (null == list) updateVoiceModeMsg();
            }
            return list;
        }
    }

    @Override
    public VoiceMode getCurrentVoiceMode() {
        if (SConstant.TEST_ANC_FUNC) {
            return list.get(selectModeId);
        } else {
            VoiceMode voiceMode = getCurrVoiceModeByCache();
            if (null == voiceMode && isDevConnected()) {
                getRCSPController().getCurrentVoiceMode(getConnectedDevice(), null);
            }
            return voiceMode;
        }
    }

    @Override
    public void setCurrentVoiceMode(int modeID) {
        if (SConstant.TEST_ANC_FUNC) {
            if (modeID >= 0 && modeID < list.size()) {
                selectModeId = modeID;
                mView.onCurrentVoiceMode(getCurrentVoiceMode());
            }
        } else {
            VoiceMode currentVoiceMode = getCurrVoiceModeByCache();
            if (currentVoiceMode != null && currentVoiceMode.getMode() == modeID) {
                mView.onCurrentVoiceMode(currentVoiceMode);
                return;
            }
            VoiceMode voiceMode = getCacheVoiceModeByMode(modeID);
            if (voiceMode == null) return;
            getRCSPController().setCurrentVoiceMode(getConnectedDevice(), voiceMode, null);
        }
    }

    @Override
    public boolean isCurrentVoiceMode(int modeID) {
        if (SConstant.TEST_ANC_FUNC) {
            return selectModeId == modeID;
        } else {
            VoiceMode currentVoiceMode = getCurrVoiceModeByCache();
            return currentVoiceMode != null && currentVoiceMode.getMode() == modeID;
        }
    }

    @Override
    public boolean isSupportAnc() {
        if (SConstant.TEST_ANC_FUNC) {
            return true;
        } else {
            boolean ret = false;
            DeviceInfo deviceInfo = getDeviceInfo();
            if (deviceInfo != null) {
                ret = deviceInfo.isSupportAnc();
            }
            return ret;
        }
    }

    @Override
    public byte[] getSelectVoiceModes() {
        if (selectedVoiceModes == null) {
            mRCSPController.getDeviceSettingsInfo(getConnectedDevice(), 0x01 << AttrAndFunCode.ADV_TYPE_ANC_MODE_LIST, new OnRcspActionCallback<ADVInfoResponse>() {
                @Override
                public void onSuccess(BluetoothDevice device, ADVInfoResponse message) {
                    updateSelectedVoiceModes(message.getModes());
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {

                }
            });
            return new byte[0];
        }
        return selectedVoiceModes;
    }

    @Override
    public void setSelectVoiceModeList(final byte[] modes) {
        if (SConstant.TEST_ANC_FUNC) {
            updateSelectedVoiceModes(modes);
        } else {
            if (modes == null || modes.length < 2) return;
            int value = 0x00;
            for (byte bit : modes) {
                value = value | (0x01 << bit);
            }
            mRCSPController.modifyDeviceSettingsInfo(getConnectedDevice(), AttrAndFunCode.ADV_TYPE_ANC_MODE_LIST, CHexConver.intToBigBytes(value), new OnRcspActionCallback<Integer>() {
                @Override
                public void onSuccess(BluetoothDevice device, Integer message) {
                    if (message == 0) {
                        updateSelectedVoiceModes(modes);
                    }
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {

                }
            });
        }
    }

    @Override
    public void updateVoiceModeMsg() {
        if (!SConstant.TEST_ANC_FUNC) {
            if (!isSupportAnc()) return;
            mRCSPController.getAllVoiceModes(getConnectedDevice(), null);
        }
    }

    @Override
    public boolean isSupportAdaptiveANC() {
        if (SConstant.TEST_ANC_FUNC) return true;
        return getDeviceInfo() != null && getDeviceInfo().isSupportAdaptiveANC();
    }

    @Override
    public AdaptiveData getAdaptiveANCData() {
        if (!isSupportAdaptiveANC()) return null;
        if (SConstant.TEST_ANC_FUNC) return mAdaptiveData;
        if (getDeviceInfo() == null) return null;
        return getDeviceInfo().getAdaptiveData();
    }

    @Override
    public void updateAdaptiveANc() {
        if (!isSupportAdaptiveANC()) return;
        if (SConstant.TEST_ANC_FUNC) {
            mView.onVoiceFuncChange(mAdaptiveData);
            return;
        }
        mRCSPController.getAdaptiveANCData(getConnectedDevice(), null);
    }

    @Override
    public void setAdaptiveANC(AdaptiveData data) {
        if (!isSupportAdaptiveANC()) return;
        if (SConstant.TEST_ANC_FUNC) {
            mAdaptiveData.setOn(data.isOn());
            mAdaptiveData.setState(data.getState());
            mAdaptiveData.setCode(data.getCode());
            mUIHandler.removeCallbacks(checkANC);
            updateAdaptiveANc();
            return;
        }
        mRCSPController.setAdaptiveANCData(getConnectedDevice(), data, null);
    }

    @Override
    public void startAdaptiveANCCheck() {
        if (!isSupportAdaptiveANC()) return;
        if (SConstant.TEST_ANC_FUNC) {
            mUIHandler.postDelayed(() -> {
                mView.onAdaptiveANCCheck(1, 0);
                mUIHandler.postDelayed(checkANC, 5000);
            }, 30);
            return;
        }
        mRCSPController.startAdaptiveANC(getConnectedDevice(), new OnAdaptiveANCListener() {
            @Override
            public void onStart() {
                mView.onAdaptiveANCCheck(1, 0);
                mUIHandler.postDelayed(ancSettingTimeout, ANC_SETTING_TIMEOUT);
            }

            @Override
            public void onFinish(int code) {
                mUIHandler.removeCallbacks(ancSettingTimeout);
                mView.onAdaptiveANCCheck(0, code);
            }
        });
    }

    @Override
    public boolean isSupportSmartNoPick() {
        return mRCSPController.isSupportSmartNoPick(getConnectedDevice());
    }

    @Override
    public void querySmartNoPick() {
        if (!isSupportSmartNoPick()) return;
        mRCSPController.getSmartNoPick(getConnectedDevice(), null);
    }

    @Override
    public void changeSmartNoPick(SmartNoPick param) {
        if (!isSupportSmartNoPick() || null == param || isCmdOpSetting) return;
        isCmdOpSetting = true;
        if (param.isOn()) {
            mUIHandler.removeMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT);
            mUIHandler.sendEmptyMessageDelayed(MSG_OPEN_SMART_NO_PICK_TIMEOUT, 12 * 1000);
        }
        mRCSPController.setSmartNoPickParam(getConnectedDevice(), param, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                isCmdOpSetting = false;
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                isCmdOpSetting = false;
                if (mUIHandler.hasMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT)) {
                    mUIHandler.removeMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT);
                }
            }
        });
    }

    @Override
    public boolean isSupportSceneDenoising() {
        return mRCSPController.isSupportSceneDenoising(getConnectedDevice());
    }

    @Override
    public void querySceneDenoising() {
        if (!isSupportSceneDenoising()) return;
        JL_Log.i(tag, "querySceneDenoising >> start.");
        mRCSPController.getSceneDenoising(getConnectedDevice(), null);
    }

    @Override
    public void changeSceneDenoising(SceneDenoising param) {
        if (!isSupportSceneDenoising() || null == param) return;
        mRCSPController.setSceneDenoising(getConnectedDevice(), param, null);
    }

    @Override
    public boolean isSupportWindNoiseDetection() {
        return mRCSPController.isSupportWindNoiseDetection(getConnectedDevice());
    }

    @Override
    public void queryWindNoiseDetection() {
        if (!isSupportWindNoiseDetection()) return;
        mRCSPController.getWindNoiseDetection(getConnectedDevice(), null);
    }

    @Override
    public void changeWindNoiseDetection(WindNoiseDetection param) {
        if (!isSupportWindNoiseDetection() || null == param) return;
        mRCSPController.setWindNoiseDetection(getConnectedDevice(), param, null);
    }

    @Override
    public boolean isSupportVocalBooster() {
        return mRCSPController.isSupportVocalBooster(getConnectedDevice());
    }

    @Override
    public void queryVocalBooster() {
        if (!isSupportVocalBooster()) return;
        mRCSPController.getVocalBooster(getConnectedDevice(), null);
    }

    @Override
    public void changeVocalBooster(VocalBooster param) {
        if (!isSupportVocalBooster() || null == param) return;
        mRCSPController.setVocalBooster(getConnectedDevice(), param, null);
    }

    @Override
    public boolean isSupportDoubleConnection() {
        return getDeviceInfo() != null && getDeviceInfo().isSupportDoubleConnection();
    }

    @Override
    public void queryDoubleConnectionState() {
        mRCSPController.queryDoubleConnectionState(getConnectedDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                JL_Log.d(tag, "[queryDoubleConnectionState][onSuccess] >>> ");
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "[queryDoubleConnectionState][onError] >>> " + error);
            }
        });
    }

    @Override
    public void setDoubleConnectionState(DoubleConnectionState state) {
        mRCSPController.setDoubleConnectionState(getConnectedDevice(), state, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                JL_Log.d(tag, "[setDoubleConnectionState][onSuccess] >>> ");
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "[setDoubleConnectionState][onError] >>> " + error);
            }
        });
    }

    @Override
    public void queryConnectedBtInfo() {
        if (getConnectedDevice() == null) return;
        DeviceBtInfo deviceBtInfo = DoubleConnectionSp.getInstance().getDeviceBtInfo(getConnectedDevice().getAddress());
        if (null == deviceBtInfo) {
            String btName = AppUtil.getBtName(MainApplication.getApplication());
            deviceBtInfo = new DeviceBtInfo().setBtName(btName);
        }
        mRCSPController.queryConnectedPhoneBtInfo(getConnectedDevice(), deviceBtInfo, new OnRcspActionCallback<ConnectedBtInfo>() {
            @Override
            public void onSuccess(BluetoothDevice device, ConnectedBtInfo message) {
                JL_Log.d(tag, "[queryConnectedBtInfo][onSuccess] >>> ");
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(tag, "[queryConnectedBtInfo][onError] >>> " + error);
            }
        });
    }

    @Override
    public void destroy() {
        destroyRCSPController(mBTEventCallback);
        NetworkStateHelper.getInstance().unregisterListener(this);
        ProductCacheManager.getInstance().unregisterListener(this);
        mUIHandler.removeCallbacksAndMessages(null);
        if (mNetworkHelper != null) {
            mNetworkHelper.unregisterNetworkEventCallback(mNetworkEventCallback);
            mNetworkHelper = null;
        }
    }

    @Override
    public void start() {

    }

    public void updateCurrentVoiceMode(VoiceMode voiceMode) {
        if (voiceMode == null) return;
        VoiceMode current = getCurrentVoiceMode();
        if (current.getMode() != voiceMode.getMode()) return;
        current.setLeftCurVal(voiceMode.getLeftCurVal());
        current.setRightCurVal(voiceMode.getRightCurVal());
    }

    private final Runnable checkANC = new Runnable() {
        @Override
        public void run() {
            int result = new Random().nextInt(2) % 2;
            mView.onAdaptiveANCCheck(0, result);
        }
    };

    private void synchronizationsViewState() {
        if (SConstant.CHANG_DIALOG_WAY) {
            mView.onNetworkState(NetworkStateHelper.getInstance().isNetworkIsAvailable());
        } else {
            mView.onNetworkState(mNetworkHelper.isNetworkIsAvailable());
        }
    }

    private VoiceMode getCurrVoiceModeByCache() {
        VoiceMode voiceMode = null;
        if (getDeviceInfo() != null) {
            voiceMode = getDeviceInfo().getCurrentVoiceMode();
        }
        return voiceMode;
    }

    private VoiceMode getCacheVoiceModeByMode(int modeID) {
        VoiceMode voiceMode = null;
        DeviceInfo deviceInfo = getDeviceInfo();
        List<VoiceMode> list = deviceInfo.getVoiceModeList();
        if (list != null) {
            for (VoiceMode mode : list) {
                if (mode.getMode() == modeID) {
                    voiceMode = mode;
                    break;
                }
            }
        }
        return voiceMode;
    }

    private final Runnable ancSettingTimeout = new Runnable() {
        @Override
        public void run() {
            JL_Log.e(tag, " ANC setting timeout >>>> callback failed.");
            mView.onAdaptiveANCCheck(0, 1);
        }
    };

    private final NetworkHelper.OnNetworkEventCallback mNetworkEventCallback = new NetworkHelper.OnNetworkEventCallback() {
        @Override
        public void onNetworkState(boolean isAvailable) {
            JL_Log.e(tag, " onNetworkState :   " + isAvailable);
            mView.onNetworkState(isAvailable);
        }

        @Override
        public void onUpdateConfigureSuccess() {
            mView.onUpdateConfigureSuccess();
        }

        @Override
        public void onUpdateImage() {

        }

        @Override
        public void onUpdateConfigureFailed(int code, String message) {
            mView.onUpdateConfigureFailed(code, message);
        }
    };

    @Override
    public void onImageUrlUpdate(BleScanMessage bleScanMessage) {

    }

    @Override
    public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {
        if (path.contains(ProductModel.MODEL_PRODUCT_MESSAGE.getValue())) {
            mView.onUpdateConfigureSuccess();
        }

    }

    @Override
    public void onNetworkStateChange(int type, boolean available) {
        mView.onNetworkState(available);
    }

    private void updateSelectedVoiceModes(byte[] array) {
        selectedVoiceModes = array;
        if (array == null) {
            array = new byte[0];
        }
        mView.onSelectedVoiceModes(array);
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {
        @Override
        public void onCurrentVoiceMode(BluetoothDevice device, VoiceMode voiceMode) {
            mView.onCurrentVoiceMode(voiceMode);
        }

        @Override
        public void onVoiceModeList(BluetoothDevice device, List<VoiceMode> voiceModes) {
            mView.onVoiceModeList(voiceModes);
        }

        @Override
        public void onDeviceSettingsInfo(BluetoothDevice device, int mask, ADVInfoResponse dataInfo) {
            if (mask == 0xffffffff) {
                mView.onADVInfoUpdate(dataInfo);
            }
        }

        @Override
        public void onVoiceFunctionChange(BluetoothDevice device, VoiceFunc voiceFunc) {
            if (null != voiceFunc) {
                if (voiceFunc.getType() == VoiceFunc.FUNC_SMART_NO_PICK) {
                    SmartNoPick pick = (SmartNoPick) voiceFunc;
                    if (pick.isOn() && mUIHandler.hasMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT)) {
                        mView.onOpenSmartNoPickSetting(true);
                        mUIHandler.removeMessages(MSG_OPEN_SMART_NO_PICK_TIMEOUT);
                    }
                }
                mView.onVoiceFuncChange(voiceFunc);
            }
        }

        @Override
        public void onDoubleConnectionChange(BluetoothDevice device, DoubleConnectionState state) {
            JL_Log.d(tag, "[onDoubleConnectionChange] >>> " + state);
            mView.onDoubleConnectionStateChange(state);
        }

        @Override
        public void onConnectedBtInfo(BluetoothDevice device, ConnectedBtInfo info) {
            mView.onConnectedBtInfo(info);
        }
    };
}
