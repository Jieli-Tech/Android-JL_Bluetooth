package com.jieli.btsmart.ui.settings.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.google.gson.Gson;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.device.hearing.HearingAssistInfo;
import com.jieli.bluetooth.bean.device.voice.AdaptiveData;
import com.jieli.bluetooth.bean.device.voice.SceneDenoising;
import com.jieli.bluetooth.bean.device.voice.SmartNoPick;
import com.jieli.bluetooth.bean.device.voice.VocalBooster;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.device.voice.WindNoiseDetection;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.FunctionAdapter;
import com.jieli.btsmart.data.adapter.HeadsetKeyAdapter;
import com.jieli.btsmart.data.model.settings.KeyBean;
import com.jieli.btsmart.data.model.settings.SettingsItem;
import com.jieli.btsmart.data.model.settings.VoiceModeItem;
import com.jieli.btsmart.databinding.FragmentDeviceSettings2Binding;
import com.jieli.btsmart.databinding.ItemKeySettingsTwoBinding;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.chargingCase.ChargingCaseSettingFragment;
import com.jieli.btsmart.ui.ota.FirmwareOtaFragment;
import com.jieli.btsmart.ui.settings.ModifyVoiceConfigFragment;
import com.jieli.btsmart.ui.settings.device.assistivelistening.AssistiveListeningFragment;
import com.jieli.btsmart.ui.settings.device.assistivelistening.HearingAssitstViewModel;
import com.jieli.btsmart.ui.settings.device.voice.SceneDenoiseFragment;
import com.jieli.btsmart.ui.settings.device.voice.SmartNoPickFragment;
import com.jieli.btsmart.ui.widget.InputTextDialog;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.base.BasePresenter;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_dialog.Jl_Dialog;
import com.jieli.jl_http.bean.ProductMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备设置界面
 */
public class DeviceSettingsFragment extends DeviceControlFragment implements IDeviceSettingsContract.IDeviceSettingsView {
    private final String TAG_GET_ADV_INFO_EMPTY = "tag_get_adv_info_empty";
    private FragmentDeviceSettings2Binding mBinding;

    private HeadsetKeyAdapter mKeyAdapter;
    private FunctionAdapter mFunctionAdapter;

    private CommonActivity mActivity;
    private ADVInfoResponse mADVInfo;
    private BluetoothDevice mUseDevice;

    private Jl_Dialog mDisConnectNotifyDialog;
    private InputTextDialog mInputTextDialog;
    private Jl_Dialog mWaringDialog;

    private Jl_Dialog mANCCheckLoadingDialog;

    private IDeviceSettingsContract.IDeviceSettingsPresenter mPresenter;
    private HearingAssitstViewModel mHearingAssitstViewModel;

    private int retryCount = 0;
    private List<VoiceModeItem> mModeItemList;
    private final static int REQUEST_CODE_SMART_NO_PICK = 0x1212;
    private final static int REQUEST_CODE_SCENE_DENOISE = 0x1213;
    private final static int MSG_GET_ADV_INFO_TIME_OUT = 0x0146;
    private final Handler mHandler = new Handler(msg -> {
        if (isAdded() && !isDetached()) {
            if (msg.what == MSG_GET_ADV_INFO_TIME_OUT) {
                mBinding.tvErrorMsg.setVisibility(View.VISIBLE);
                mBinding.tvErrorMsg.setTag(TAG_GET_ADV_INFO_EMPTY);
                mBinding.tvErrorMsg.setText(R.string.require_tws_info_error);
            }
        }
        return false;
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentDeviceSettings2Binding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
        mHearingAssitstViewModel = new ViewModelProvider(this).get(HearingAssitstViewModel.class);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mADVInfo = bundle.getParcelable(SConstant.KEY_ADV_INFO);
            updateDeviceSettings(mADVInfo);
        } else {
            mPresenter.updateDeviceADVInfo();
        }
        mUseDevice = mPresenter.getConnectedDevice();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DeviceInfo deviceInfo = mPresenter.getDeviceInfo(mUseDevice);
        if (deviceInfo != null /*&& deviceInfo.isSupportAnc()*/) {
            List<VoiceMode> list = mPresenter.getNoiseModeMsg();
            if (null != list) updateNoiseCtrlUI(list);
            updateAssistiveListening();
            updateSmartNoPickUI();
            updateDualDevConnect();
//            @note 隐藏掉彩屏充电仓功能
//            updateChargingCaseSetting(deviceInfo.getSdkType() == JLChipFlag.JL_COLOR_SCREEN_CHARGING_CASE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPresenter != null) {
            mPresenter.destroy();
        }
        dismissDisconnectNotifyDialog();
        dismissInputTextDialog();
        dismissRebootDialog();
        mActivity = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case SConstant.REQUEST_CODE_DEVICE_SETTINGS:
            case SConstant.REQUEST_CODE_DEVICE_LED_SETTINGS:
                if (mPresenter != null) {
                    mPresenter.updateDeviceADVInfo();
                }
                break;
            case SConstant.REQUEST_CODE_NETWORK:
                if (mPresenter != null) {
                    mPresenter.checkNetworkAvailable();
                }
                break;
            case SConstant.REQUEST_CODE_ADJUST_VOICE_MODE:
                if (SConstant.TEST_ANC_FUNC) {
                    if (null != data) {
                        VoiceMode voiceMode = data.getParcelableExtra(ModifyVoiceConfigFragment.KEY_VOICE_MODE);
                        if (voiceMode != null) {
                            ((DeviceSettingsPresenterImpl) mPresenter).updateCurrentVoiceMode(voiceMode);
                        }
                    }
                } else {
                    if (mPresenter != null) {
                        updateCurrentVoiceMode(mPresenter.getCurrentVoiceMode());
                        mPresenter.updateVoiceModeMsg();
                    }
                }
                break;
            case REQUEST_CODE_SMART_NO_PICK:
                if (mPresenter != null) mPresenter.querySmartNoPick();
                break;
            case REQUEST_CODE_SCENE_DENOISE:
                if (mPresenter != null) mPresenter.querySceneDenoising();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onBtAdapterStatus(boolean enable) {

    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (status != StateCode.CONNECTION_CONNECTING && BluetoothUtil.deviceEquals(mUseDevice, device)) {
            if (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT) {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            } else {
                mPresenter.updateDeviceADVInfo();
            }
        }
    }

    @Override
    public void onSwitchDevice(BluetoothDevice device) {
        if (!BluetoothUtil.deviceEquals(mUseDevice, device)) {
            if (mActivity != null) {
                mActivity.onBackPressed();
            }
        } else {
            mPresenter.updateDeviceADVInfo();
        }
    }

    @Override
    public void onADVInfoUpdate(ADVInfoResponse advInfo) {
        mADVInfo = advInfo;
        updateDeviceSettings(advInfo);
    }

    @Override
    public void onGetADVInfoFailed(BaseError error) {

    }

    @Override
    public void onRebootSuccess() {

    }

    @Override
    public void onRebootFailed(BaseError error) {

    }

    @Override
    public void onSetNameSuccess(String name) {

    }

    @Override
    public void onSetNameFailed(BaseError error) {
        if (error != null) {
            ToastUtil.showToastShort(error.getMessage());
            mPresenter.updateDeviceADVInfo();
        }
    }

    @Override
    public void onConfigureSuccess(int position, int result) {

    }

    @Override
    public void onConfigureFailed(BaseError error) {

    }

    @Override
    public void onNetworkState(boolean isAvailable) {
        if (isAvailable) {
            updateDeviceSettings(mADVInfo);
        }
        if (!isAdded()) return;
        mBinding.tvDeviceSettingNoNetwork.setVisibility(!isAvailable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onUpdateConfigureSuccess() {
        updateDeviceSettings(mADVInfo);
    }

    @Override
    public void onUpdateConfigureFailed(int code, String message) {

    }

    @Override
    public void onVoiceModeList(List<VoiceMode> list) {
        updateNoiseCtrlUI(list);
    }

    @Override
    public void onCurrentVoiceMode(VoiceMode voiceMode) {
        updateCurrentVoiceMode(voiceMode);
    }

    @Override
    public void onSelectedVoiceModes(byte[] selectedModes) {

    }

    @Override
    public void onVoiceFuncChange(VoiceFunc data) {
        if (data != null && isAdded() && !isDetached()) {
            switch (data.getType()) {
                case VoiceFunc.FUNC_ADAPTIVE:
                    AdaptiveData adaptiveData = (AdaptiveData) data;
                    if (!mBinding.viewAncFunc.switchAdaptiveAnc.isEnabled()) {
                        mBinding.viewAncFunc.switchAdaptiveAnc.setEnabled(true);
                    }
                    mBinding.viewAncFunc.switchAdaptiveAnc.setCheckedImmediatelyNoEvent(adaptiveData.isOn());
                    break;
                case VoiceFunc.FUNC_SMART_NO_PICK:
                    SmartNoPick smartNoPick = (SmartNoPick) data;
                    JL_Log.d(TAG, "onVoiceFuncChange >> smartNoPick = " + smartNoPick);
                    if (!mBinding.switchSmartNoPick.isEnabled()) {
                        mBinding.switchSmartNoPick.setEnabled(true);
                    }
                    mBinding.switchSmartNoPick.setCheckedImmediatelyNoEvent(smartNoPick.isOn());
                    break;
                case VoiceFunc.FUNC_SCENE_DENOISING:
                    SceneDenoising sceneDenoising = (SceneDenoising) data;
                    JL_Log.d(TAG, "onVoiceFuncChange >> sceneDenoising = " + sceneDenoising);
                    if (!mBinding.viewAncFunc.tvSceneDenoisingMode.isEnabled()) {
                        mBinding.viewAncFunc.tvSceneDenoisingMode.setEnabled(true);
                    }
                    mBinding.viewAncFunc.tvSceneDenoisingMode.setText(getSceneDenoisingMode(sceneDenoising.getMode()));
                    break;
                case VoiceFunc.FUNC_WIND_NOISE_DETECTION:
                    WindNoiseDetection detection = (WindNoiseDetection) data;
                    JL_Log.d(TAG, "onVoiceFuncChange >> detection = " + detection);
                    if (!mBinding.viewAncFunc.switchWindNoiseDetection.isEnabled()) {
                        mBinding.viewAncFunc.switchWindNoiseDetection.setEnabled(true);
                    }
                    mBinding.viewAncFunc.switchWindNoiseDetection.setCheckedImmediatelyNoEvent(detection.isOn());
                    break;
                case VoiceFunc.FUNC_VOCAL_BOOSTER:
                    VocalBooster vocalBooster = (VocalBooster) data;
                    JL_Log.d(TAG, "onVoiceFuncChange >> vocalBooster = " + vocalBooster);
                    if (!mBinding.viewTransparentFunc.switchVocalBooster.isEnabled()) {
                        mBinding.viewTransparentFunc.switchVocalBooster.setEnabled(true);
                    }
                    mBinding.viewTransparentFunc.switchVocalBooster.setCheckedImmediatelyNoEvent(vocalBooster.isOn());
                    break;
            }
        }
    }

    @Override
    public void onAdaptiveANCCheck(int state, int code) {
        handleAdaptiveANCCheckState(state, code);
    }

    @Override
    public void onOpenSmartNoPickSetting(boolean isUser) {
        if (isUser && isAdded() && !isDetached()) {
            mBinding.tvSmartNoPickTips.performClick();
        }
    }

    @Override
    public void onDoubleConnectionStateChange(DoubleConnectionState state) {
        updateDoubleConnectionState(state);
    }

    @Override
    public void onConnectedBtInfo(ConnectedBtInfo info) {

    }

    @Override
    public void setPresenter(BasePresenter presenter) {
        if (mPresenter == null) {
            mPresenter = (IDeviceSettingsContract.IDeviceSettingsPresenter) presenter;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissANCCheckLoadingDialog();
    }

    private void initUI() {
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        mPresenter = new DeviceSettingsPresenterImpl(mActivity, this);
        if (mActivity != null) {
            BluetoothDevice device = mPresenter.getConnectedDevice();
            mActivity.updateTopBar(UIHelper.getCacheDeviceName(device), R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);
        }
        mBinding.rvDeviceSettingsKeyList.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.rvDeviceSettingsFuncList.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.rvDeviceSettingsKeyList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = ValueUtil.dp2px(requireContext(), 3);
            }
        });

        mBinding.tvDeviceSettingsDisconnect.setOnClickListener(v -> showDisconnectNotifyDialog());
        mBinding.tvDeviceSettingNoNetwork.setOnClickListener(v -> startActivityForResult(new Intent("android.settings.SETTINGS"), SConstant.REQUEST_CODE_NETWORK));
        mBinding.ivDeviceSettingsVoiceAdjust.setOnClickListener(v -> {
            VoiceMode currentMode = mPresenter.getCurrentVoiceMode();
            if (currentMode == null || currentMode.getMode() == VoiceMode.VOICE_MODE_CLOSE) return;
            String text = currentMode.getMode() == VoiceMode.VOICE_MODE_DENOISE ? getString(R.string.denoise_value) : getString(R.string.transparent_value);
            Bundle bundle = new Bundle();
            bundle.putParcelable(ModifyVoiceConfigFragment.KEY_VOICE_MODE, currentMode);
            ContentActivity.startActivityForRequest(this, SConstant.REQUEST_CODE_ADJUST_VOICE_MODE, ModifyVoiceConfigFragment.class.getCanonicalName(), text, bundle);
        });
        mBinding.ivNoiseModeMid.setOnClickListener(v -> setVoiceMode(mModeItemList.get(2).getMode()));
        mBinding.ivNoiseModeStart.setOnClickListener(v -> setVoiceMode(mModeItemList.get(0).getMode()));
        mBinding.ivNoiseModeEnd.setOnClickListener(v -> setVoiceMode(mModeItemList.get(1).getMode()));
        mBinding.tvErrorMsg.setOnClickListener(v -> {
            String tag = (String) v.getTag();
            if (TextUtils.equals(tag, TAG_GET_ADV_INFO_EMPTY)) {
                mPresenter.updateDeviceADVInfo();
                ToastUtil.showToastShort(R.string.retrieved);
            }
        });
        mBinding.cvOtaFunction.setOnClickListener(v -> {
            CommonActivity.startCommonActivity(requireActivity(), FirmwareOtaFragment.class.getCanonicalName());
            requireActivity().sendBroadcast(new Intent(SConstant.ACTION_DEVICE_UPGRADE));
        });
        mBinding.cvDeviceSettingsAssistiveListeningSettings.setOnClickListener(v -> {
//            CommonActivity.startCommonActivity(getActivity(), TestFittingFragment.class.getCanonicalName());
            mHearingAssitstViewModel.getFittingConfigure(new OnRcspActionCallback<HearingAssistInfo>() {
                @Override
                public void onSuccess(BluetoothDevice device, HearingAssistInfo hearingAssistInfo) {
                   /* hearingAssistInfo = new HearingAssistInfo();
                    hearingAssistInfo.setChannels(6);
                    hearingAssistInfo.setVersion(1);
                    hearingAssistInfo.setFrequencies(new int[]{250, 500, 1000, 2000, 4000, 6000});*/
                    if (hearingAssistInfo == null) {
                        ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putString(AssistiveListeningFragment.KEY_HEARING_ASSIST_INFO, new Gson().toJson(hearingAssistInfo));
                        CommonActivity.startCommonActivity(getActivity(), AssistiveListeningFragment.class.getCanonicalName(), bundle);
                    }
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {
                    ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
                }
            });
        });
        mBinding.switchSmartNoPick.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SmartNoPick smartNoPick = new SmartNoPick();
            smartNoPick.setOn(isChecked);
            mPresenter.changeSmartNoPick(smartNoPick);
        });
        mBinding.viewAncFunc.switchAdaptiveAnc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                mPresenter.setAdaptiveANC(new AdaptiveData());
                return;
            }
            showAdaptiveANCTipsDialog();
        });
        mBinding.viewAncFunc.tvSceneDenoisingMode.setOnClickListener(v -> CommonActivity.startCommonActivity(DeviceSettingsFragment.this, REQUEST_CODE_SCENE_DENOISE, SceneDenoiseFragment.class.getSimpleName(), null));
        mBinding.viewAncFunc.switchWindNoiseDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            WindNoiseDetection detection = new WindNoiseDetection();
            detection.setOn(isChecked);
            mPresenter.changeWindNoiseDetection(detection);
        });
        mBinding.viewTransparentFunc.switchVocalBooster.setOnCheckedChangeListener((buttonView, isChecked) -> {
            VocalBooster vocalBooster = new VocalBooster();
            vocalBooster.setOn(isChecked);
            mPresenter.changeVocalBooster(vocalBooster);
        });
        mBinding.tvSmartNoPickTips.setOnClickListener(v -> CommonActivity.startCommonActivity(DeviceSettingsFragment.this, REQUEST_CODE_SMART_NO_PICK, SmartNoPickFragment.class.getSimpleName(), null));

        mBinding.cvDeviceSettingsDualDevConnect.setOnClickListener(v -> {
            if (mPresenter.isSupportDoubleConnection() && mPresenter.getDeviceInfo().getDoubleConnectionState() != null) {
                CommonActivity.startCommonActivity(requireActivity(), DualDevConnectFragment.class.getCanonicalName());
            }
        });
        mBinding.cvChargingCaseSetting.setOnClickListener(v -> ContentActivity.startActivity(requireContext(), ChargingCaseSettingFragment.class.getCanonicalName(), getString(R.string.charging_case_setting)));
        updateOtaSettings();
    }

    /**
     * 判断是否能用TWS命令
     *
     * @return 结果
     */
    private boolean isCanUseTws() {
        return mPresenter != null && mPresenter.isDevConnected() && mPresenter.getDeviceInfo() != null && UIHelper.isCanUseTwsCmd(mPresenter.getDeviceInfo().getSdkType());
    }

    private void updateDeviceSettings(ADVInfoResponse advInfo) {
        if (!isAdded() || isDetached() || mPresenter == null) return;
        JL_Log.d(TAG, "updateDeviceSettings :: device : " + mPresenter.getConnectedDevice() + ",\n advInfo : " + advInfo + ", " + isCanUseTws());
        if (mADVInfo != null || advInfo != null) {
            mHandler.removeMessages(MSG_GET_ADV_INFO_TIME_OUT);
        }
        if (advInfo != null) {
            mBinding.tvErrorMsg.setVisibility(View.GONE);
            HistoryBluetoothDevice historyBluetoothDevice = mPresenter.getRCSPController().findHistoryBluetoothDevice(mPresenter.getConnectedDevice());
            updateKeySettingsList(getKeyListFromADVInfo(advInfo, historyBluetoothDevice == null ? -1 : historyBluetoothDevice.getAdvVersion()));
            updateFunctionList(getFuncListFromADVInfo(advInfo));
            if (mPresenter.isSupportAnc() && advInfo.getModes() == null) {
//                mPresenter.updateDeviceADVInfo();
            }
        } else if (isCanUseTws()) {
            if (retryCount < 3) {
                mPresenter.updateDeviceADVInfo();
                mHandler.removeMessages(MSG_GET_ADV_INFO_TIME_OUT);
                mHandler.sendEmptyMessageDelayed(MSG_GET_ADV_INFO_TIME_OUT, 1500);
                retryCount++;
                return;
            } else {
                retryCount = 0;
                mBinding.tvErrorMsg.setVisibility(View.VISIBLE);
                mBinding.tvErrorMsg.setTag(TAG_GET_ADV_INFO_EMPTY);
                mBinding.tvErrorMsg.setText(R.string.require_tws_info_error);
            }
        } else if (mADVInfo == null) {
            updateKeySettingsList(null);
            updateFunctionList(getFuncListFromBtDevice(mPresenter.getConnectedDevice()));
        }
        retryCount = 0;
    }

    private void updateOtaSettings() {
        boolean isHasOTA = SConstant.HAS_OTA;
        ProductMessage.DeviceBean deviceMsg;
        DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
        if (deviceInfo == null) {
            requireActivity().finish();
            return;
        }
        if (SConstant.CHANG_DIALOG_WAY) {
            deviceMsg = ProductCacheManager.getInstance().getDeviceMessageModify(MainApplication.getApplication(), deviceInfo.getVid(), deviceInfo.getUid(), deviceInfo.getPid());
        } else {
            deviceMsg = ProductUtil.getDeviceMessage(MainApplication.getApplication(), deviceInfo.getVid(), deviceInfo.getUid(), deviceInfo.getPid());
        }
        if (deviceMsg == null) {
            mPresenter.checkNetworkAvailable();
        } else {
            isHasOTA = deviceMsg.getHasOta() > 0;
        }
        JL_Log.i(TAG, "[updateOtaSettings] deviceMsg >>> " + deviceMsg + ", isHasOTA = " + isHasOTA);
        updateOtaFunction(isHasOTA);
    }

    private List<KeyBean> getKeyListFromADVInfo(ADVInfoResponse advInfo, int advVersion) {
        if (advInfo == null || !isAdded() || isDetached()) return null;
        List<ADVInfoResponse.KeySettings> keySettingsList = advInfo.getKeySettingsList();
        if (keySettingsList == null) return null;
        List<KeyBean> list = new ArrayList<>();
        Map<Integer, ArrayList<KeyBean>> keyBeanMap = new HashMap<>();
        Log.d(TAG, "getKeyListFromADVInfo: keySettingsList size:" + keySettingsList.size());
        for (ADVInfoResponse.KeySettings keySettings : keySettingsList) {
            if (keySettings.getKeyNum() == SConstant.KEY_NUM_IDLE) continue;//按键空闲，不使用
            ArrayList<KeyBean> keyBeanArrayList;
            int resId;
            String key;
            int mapKey;
            if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {//挂脖耳机分组是按keyNum
                mapKey = keySettings.getKeyNum();
                keyBeanArrayList = keyBeanMap.get(mapKey);
                key = ProductUtil.getKeySettingsName(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(),
                        SConstant.KEY_FIELD_KEY_ACTION, keySettings.getAction());
                if (keySettings.getAction() == 1) {//单击
                    resId = R.drawable.ic_once_click;
                } else {//多次点击
                    resId = R.drawable.ic_double_click;
                }
            } else {//普通的tws耳机是按action分组
                mapKey = keySettings.getAction();
                keyBeanArrayList = keyBeanMap.get(mapKey);
                key = ProductUtil.getKeySettingsName(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(),
                        SConstant.KEY_FIELD_KEY_NUM, keySettings.getKeyNum());
                if (keySettings.getKeyNum() == 1) {//左耳
                    resId = R.drawable.ic_headset_left_settings;
                } else {//右耳
                    resId = R.drawable.ic_headset_right_settings;
                }
            }
            if (keyBeanArrayList == null) keyBeanArrayList = new ArrayList<>();
            String action = ProductUtil.getKeySettingsName(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(),
                    SConstant.KEY_FIELD_KEY_ACTION, keySettings.getAction());
            if (TextUtils.isEmpty(action) || TextUtils.isEmpty(key)) continue;
            KeyBean keyBean = new KeyBean();
            keyBean.setResId(resId);
            keyBean.setAction(action)
                    .setActionId(keySettings.getAction())
                    .setKey(key)
                    .setKeyName(ProductUtil.getKeySettingsName(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(),
                            SConstant.KEY_FIELD_KEY_NUM, keySettings.getKeyNum()))
                    .setKeyId(keySettings.getKeyNum())
                    .setFuncId(keySettings.getFunction())
                    .setFunction(ProductUtil.getKeySettingsName(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(),
                            SConstant.KEY_FIELD_KEY_FUNCTION, keySettings.getFunction()))
                    .setShowIcon(true);
            keyBeanArrayList.add(keyBean);
            keyBeanMap.put(mapKey, keyBeanArrayList);
        }
        Object[] collections = keyBeanMap.values().toArray();
        for (Object o : collections) {
            ArrayList<KeyBean> keyArrayList = (ArrayList<KeyBean>) o;
            KeyBean headerBean = new KeyBean();
            headerBean.setHeader(true);
            String action;
            if (keyArrayList.isEmpty()) continue;
            KeyBean keyBean = keyArrayList.get(0);
            if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {//挂脖耳机的头布局是 keynum的描述
                action = ProductUtil.getKeySettingsName(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(),
                        SConstant.KEY_FIELD_KEY_NUM, keyBean.getKeyId());
            } else {//普通的tws耳机头布局是action描述
                action = ProductUtil.getKeySettingsName(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(),
                        SConstant.KEY_FIELD_KEY_ACTION, keyBean.getActionId());
            }
            headerBean.setAction(action);//不管是tws还是挂脖，头描述都放aciton里面
            list.add(headerBean);
            list.addAll(keyArrayList);
        }
        return list;
    }

    private List<SettingsItem> getFuncListFromADVInfo(ADVInfoResponse advInfo) {
        if (advInfo == null || !isAdded() || isDetached()) return null;
        List<SettingsItem> list = new ArrayList<>();
        SettingsItem item;
        if (advInfo.getDeviceName() != null) {
            item = new SettingsItem(R.drawable.ic_dev_name, getString(R.string.bluetooth_name), advInfo.getDeviceName(), 0, true);
            item.setType(AttrAndFunCode.ADV_TYPE_DEVICE_NAME);
            list.add(item);
        }
        if (advInfo.getWorkModel() > 0) {
            item = new SettingsItem(R.drawable.ic_work_mode, getString(R.string.work_mode), ProductUtil.getWorkModeName(getContext(),
                    advInfo.getVid(), advInfo.getUid(), advInfo.getPid(), advInfo.getWorkModel()), advInfo.getWorkModel(), true);
            item.setType(AttrAndFunCode.ADV_TYPE_WORK_MODE);
            list.add(item);
        }
        if (advInfo.getMicChannel() > 0) {
            item = new SettingsItem(R.drawable.ic_mic, getString(R.string.mic_channel), ProductUtil.getMicChannelName(getContext(),
                    advInfo.getVid(), advInfo.getUid(), advInfo.getPid(), advInfo.getMicChannel()), advInfo.getMicChannel(), true);
            item.setType(AttrAndFunCode.ADV_TYPE_MIC_CHANNEL_SETTINGS);
            list.add(item);
        }
        if (advInfo.getInEarSettings() > 0) {
            item = new SettingsItem(R.drawable.ic_mic, getString(R.string.in_ear_check), ProductUtil.getInEarCheckOption(getContext(),
                    advInfo.getVid(), advInfo.getUid(), advInfo.getPid(), advInfo.getInEarSettings()), advInfo.getInEarSettings(), true);
            item.setType(AttrAndFunCode.ADV_TYPE_IN_EAR_CHECK);
            list.add(item);
        }
        List<ADVInfoResponse.LedSettings> ledSettings = advInfo.getLedSettingsList();
        if (ledSettings != null && ledSettings.size() > 0) {
            item = new SettingsItem(R.drawable.ic_lights, getString(R.string.led_settings), null, 0, true);
            item.setType(AttrAndFunCode.ADV_TYPE_LED_SETTINGS);
            list.add(item);
        }
        return list;
    }

    private List<SettingsItem> getFuncListFromBtDevice(BluetoothDevice device) {
        if (device == null || !isAdded() || isDetached()) return null;
        List<SettingsItem> list = new ArrayList<>();
        SettingsItem item;
        String devName = UIHelper.getCacheDeviceName(device);
        if (devName != null) {
            item = new SettingsItem(R.drawable.ic_dev_name, getString(R.string.bluetooth_name), devName, 0, true);
            item.setType(AttrAndFunCode.ADV_TYPE_DEVICE_NAME);
            list.add(item);
        }
        return list;
    }

    private void updateKeySettingsList(List<KeyBean> list) {
        if (!isAdded() || isDetached()) return;
        if (list == null) list = new ArrayList<>();
        if (list.size() == 0) {
            mBinding.cvDeviceSettingsKeySettings.setVisibility(View.GONE);
            return;
        } else {
            mBinding.cvDeviceSettingsKeySettings.setVisibility(View.VISIBLE);
        }
        if (mKeyAdapter == null) {
            mKeyAdapter = new HeadsetKeyAdapter();
            mKeyAdapter.setList(list);
            mKeyAdapter.setOnItemClickListener(mOnItemClickListener);
        } else {
            mKeyAdapter.setNewInstance(list);
        }
        mBinding.rvDeviceSettingsKeyList.setAdapter(mKeyAdapter);
    }

    private void updateFunctionList(List<SettingsItem> list) {
        if (!isAdded() || isDetached()) return;
        if (list == null) list = new ArrayList<>();
        if (list.size() == 0) {
            mBinding.cvDeviceSettingsFuncSettings.setVisibility(View.GONE);
            return;
        } else if (list.size() == 1) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.rvDeviceSettingsFuncList.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, 0);
            mBinding.rvDeviceSettingsFuncList.setLayoutParams(layoutParams);
            mBinding.cvDeviceSettingsFuncSettings.setVisibility(View.VISIBLE);
        } else {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mBinding.rvDeviceSettingsFuncList.getLayoutParams();
            layoutParams.setMargins(0, ValueUtil.dp2px(getContext(), 12), 0, ValueUtil.dp2px(getContext(), 12));
            mBinding.rvDeviceSettingsFuncList.setLayoutParams(layoutParams);
            mBinding.cvDeviceSettingsFuncSettings.setVisibility(View.VISIBLE);
        }
        if (mFunctionAdapter == null) {
            mFunctionAdapter = new FunctionAdapter();
            mFunctionAdapter.setOnItemClickListener(mOnItemClickListener);
        }
        mFunctionAdapter.setList(list);
        mBinding.rvDeviceSettingsFuncList.setAdapter(mFunctionAdapter);
    }

    private void updateSettingItem(ItemKeySettingsTwoBinding binding, SettingsItem settingsItem) {
        if (null == binding || null == settingsItem) return;
        binding.ivKeySettingsTwoImg.setImageResource(settingsItem.getResId());
        binding.tvKeySettingsTwoKey.setText(settingsItem.getName());
        binding.tvKeySettingsTwoValue.setText(settingsItem.getValue());
        binding.ivKeySettingsTwoIcon.setVisibility(settingsItem.isShowIcon() ? View.VISIBLE : View.GONE);
    }

    private void updateOtaFunction(boolean isShow) {
        mBinding.cvOtaFunction.setVisibility(isShow ? View.VISIBLE : View.GONE);
        if (isShow) {
            updateSettingItem(mBinding.viewOtaFunction, new SettingsItem(R.drawable.ic_upgrade, getString(R.string.firmware_update), "", true));
        }
    }

    private void showDisconnectNotifyDialog() {
        if (!isAdded() || isDetached()) return;
        if (mDisConnectNotifyDialog == null) {
            mDisConnectNotifyDialog = Jl_Dialog.builder()
                    .title(getString(R.string.tips))
                    .content(getString(R.string.disconnect_device_tips))
                    .showProgressBar(false)
                    .width(0.8f)
                    .cancel(true)
                    .left(getString(R.string.cancel))
                    .leftColor(getResources().getColor(R.color.gray_text_989898))
                    .leftClickListener((v, dialogFragment) -> dismissDisconnectNotifyDialog())
                    .right(getString(R.string.confirm))
                    .rightColor(getResources().getColor(R.color.blue_text_color))
                    .rightClickListener((v, dialogFragment) -> {
                        if (mPresenter != null) {
                            mPresenter.disconnectBtDevice();
                        }
                        dismissDisconnectNotifyDialog();
                    })
                    .build();
        }
        if (!mDisConnectNotifyDialog.isShow() && !isDetached() && isAdded()) {
            mDisConnectNotifyDialog.show(getChildFragmentManager(), "notify_dialog");
        }
    }

    private void dismissDisconnectNotifyDialog() {
        if (mDisConnectNotifyDialog != null) {
            if (mDisConnectNotifyDialog.isShow() && !isDetached()) {
                mDisConnectNotifyDialog.dismiss();
            }
            mDisConnectNotifyDialog = null;
        }
    }

    private void showInputTextDialog(String deviceName) {
        if (!isAdded() || isDetached()) return;
        if (mInputTextDialog == null) {
            mInputTextDialog = new InputTextDialog.Builder()
                    .setWidth(0.9f)
                    .setCancelable(false)
                    .setTitle(getString(R.string.bluetooth_name))
                    .setInputText(deviceName)
                    .setLeftText(getString(R.string.cancel))
                    .setLeftColor(getResources().getColor(R.color.gray_text_989898))
                    .setRightText(getString(R.string.confirm))
                    .setRightColor(getResources().getColor(R.color.blue_448eff))
                    .setOnInputTextListener(mOnInputTextListener)
                    .create();
        }
        mInputTextDialog.updateEditText();
        mInputTextDialog.updateDialog();
        if (!mInputTextDialog.isShow() && !isDetached() && getActivity() != null) {
            mInputTextDialog.show(getActivity().getSupportFragmentManager(), "input_text_dialog");
        }
    }

    private void dismissInputTextDialog() {
        if (mInputTextDialog != null) {
            if (mInputTextDialog.isShow()) {
                mInputTextDialog.dismiss();
            }
            mInputTextDialog = null;
        }
    }

    private void showRebootDialog(String newDevName) {
        if (!isAdded() || isDetached()) return;
        if (mWaringDialog == null) {
            String tips = getString(R.string.device_name_change_tips);
            String tips1 = getString(R.string.modify_dev_name_tips);
            SpannableString span = new SpannableString(tips + tips1);
            int startIndex = 0;
            int endIndex = startIndex + tips.length();
            span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black_242424)), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.gray_9A9A9A)), endIndex, endIndex + tips1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            @SuppressLint("InflateParams") ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(getContext()).inflate(R.layout.dialog_notify_two_option, null, false);
            TextView tvTopTitle = layout.findViewById(R.id.tv_dialog_notify_top_title);
            TextView tvMessage = layout.findViewById(R.id.tv_dialog_notify_message);
            TextView tvLeftButton = layout.findViewById(R.id.tv_dialog_notify_bottom_left);
            TextView tvRightButton = layout.findViewById(R.id.tv_dialog_notify_bottom_right);
            tvTopTitle.setText(R.string.tips);
            tvMessage.setText(span);
            tvLeftButton.setText(R.string.not_immediately_effective);
            tvRightButton.setText(R.string.immediately_effective);
            tvRightButton.setOnClickListener(v -> {
                Bundle bundle = mWaringDialog.getArguments();
                if (bundle != null) {
                    String deviceName = bundle.getString(SConstant.KEY_DEV_NAME);
                    mPresenter.changeDeviceName(deviceName, true);
                }
                dismissRebootDialog();
            });
            tvLeftButton.setOnClickListener(v -> {
                Bundle bundle = mWaringDialog.getArguments();
                if (bundle != null) {
                    String deviceName = bundle.getString(SConstant.KEY_DEV_NAME);
                    mPresenter.changeDeviceName(deviceName, false);
                }
                dismissRebootDialog();
            });
            mWaringDialog = Jl_Dialog.builder()
                    .width(0.9f)
                    .cancel(false)
                    .title(getString(R.string.tips))
                    .backgroundColor(getResources().getColor(R.color.half_transparent))
                    .containerView(layout)
                    .build();
        }
        Bundle bundle = new Bundle();
        bundle.putString(SConstant.KEY_DEV_NAME, newDevName);
        mWaringDialog.setArguments(bundle);
        if (!mWaringDialog.isShow() && !isDetached() && getActivity() != null) {
            mWaringDialog.show(getActivity().getSupportFragmentManager(), "reboot_notify");
        }
    }

    private void dismissRebootDialog() {
        if (mWaringDialog != null) {
            if (mWaringDialog.isShow() && !isDetached()) {
                mWaringDialog.dismiss();
            }
            mWaringDialog = null;
        }
    }

    private void updateAssistiveListening() {
        if (isDetached() || !isAdded()) return;
        DeviceInfo deviceInfo = mPresenter.getDeviceInfo(mUseDevice);
        if (deviceInfo == null) return;
        boolean isHasData = deviceInfo.isSupportHearingAssist();
        mBinding.cvDeviceSettingsAssistiveListeningSettings.setVisibility(isHasData ? View.VISIBLE : View.GONE);
        if (isHasData) {
            updateSettingItem(mBinding.viewAssistiveListeningSettings, new SettingsItem(R.drawable.ic_assistive_listening, getString(R.string.hearing_aid_fitting), "", true));
        }
    }

    private void updateDualDevConnect() {
        if (isDetached() || !isAdded()) return;
        boolean isSupportDualDev = mPresenter.isSupportDoubleConnection(); //目前固件暂不支持
        JL_Log.d(TAG, "[updateDualDevConnect] >>> " + isSupportDualDev);
        mBinding.cvDeviceSettingsDualDevConnect.setVisibility(isSupportDualDev ? View.VISIBLE : View.GONE);
        if (isSupportDualDev) {
            DoubleConnectionState state = mPresenter.getDeviceInfo().getDoubleConnectionState();
            updateDoubleConnectionState(state);
            mPresenter.queryDoubleConnectionState();
        }
    }

    private void updateDoubleConnectionState(DoubleConnectionState state) {
        boolean isOn = state != null && state.isOn();
        final String value = isOn ? getString(R.string.function_open) : getString(R.string.function_close);
        updateSettingItem(mBinding.viewDeviceSettingsDualDevConnect, new SettingsItem(R.drawable.ic_function_dual_dev_connection,
                getString(R.string.double_connection), value, true));
    }

    private void updateNoiseCtrlUI(List<VoiceMode> list) {
        if (isDetached() || !isAdded()) return;
        boolean isHasData = list != null && !list.isEmpty();
        mBinding.cvDeviceSettingsNoiseControl.setVisibility(isHasData ? View.VISIBLE : View.GONE);
        if (isHasData) {
            mModeItemList = convertItemList(list);
            //获取当前模式
            int currentMode = -1;
            VoiceMode mode = mPresenter.getCurrentVoiceMode();
            if (null != mode) currentMode = mode.getMode();
            updateVoiceUI(mModeItemList, currentMode);
        }
    }

    private List<VoiceModeItem> convertItemList(List<VoiceMode> list) {
        List<VoiceModeItem> temp = new ArrayList<>();
        for (VoiceMode mode : list) {
            if (!VoiceMode.isValidMode(mode.getMode())) continue;
            VoiceModeItem item = new VoiceModeItem();
            item.setMode(mode.getMode());
            item.setName(VoiceModeItem.getVoiceModeName(requireContext(), mode.getMode()));
            item.setResource(VoiceModeItem.getVoiceModeResource(mode.getMode(), false));
            temp.add(item);
        }
        List<VoiceModeItem> result = temp;
        if (!temp.isEmpty()) {
            if (temp.size() == 2) {
                result = new ArrayList<>();
                if (temp.get(0).getMode() != VoiceMode.VOICE_MODE_CLOSE) {
                    result = temp;
                } else {
                    result.add(temp.get(1));
                    result.add(temp.get(0));
                }
            } else if (temp.size() >= 3) {
                VoiceModeItem[] array = new VoiceModeItem[3];
                for (VoiceModeItem item : temp) {
                    switch (item.getMode()) {
                        case VoiceMode.VOICE_MODE_CLOSE:
                            array[2] = item;
                            break;
                        case VoiceMode.VOICE_MODE_DENOISE:
                            array[0] = item;
                            break;
                        case VoiceMode.VOICE_MODE_TRANSPARENT:
                            array[1] = item;
                            break;
                    }
                }
                result = Arrays.asList(array);
            }
        }
        return result;
    }

    private void updateCurrentVoiceMode(VoiceMode voiceMode) {
        if (isDetached() || !isAdded() || voiceMode == null) return;
        int mode = voiceMode.getMode();
        updateVoiceUI(mModeItemList, mode);
    }

    private void updateVoiceMode(ImageView imageView, TextView textView, VoiceModeItem item, boolean isCurrentMode) {
        imageView.setImageResource(VoiceModeItem.getVoiceModeResource(item.getMode(), isCurrentMode));
        if (isCurrentMode) {
            if (item.getMode() == VoiceMode.VOICE_MODE_CLOSE) {
                imageView.setBackgroundResource(R.drawable.bg_round_gray_shape);
            } else {
                imageView.setBackgroundResource(R.drawable.bg_round_blue_shape);
            }
        } else {
            imageView.setBackgroundColor(getResources().getColor(R.color.text_transparent));
        }
        textView.setText(item.getName());
        mBinding.ivDeviceSettingsVoiceAdjust.setVisibility((item.getMode() == VoiceMode.VOICE_MODE_CLOSE && isCurrentMode) ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateVoiceUI(List<VoiceModeItem> list, int currentMode) {
        for (int i = 0; i < list.size(); i++) {
            VoiceModeItem item = list.get(i);
            boolean isCurrentMode = item.getMode() == currentMode;
            switch (i) {
                case 0:
                    updateVoiceMode(mBinding.ivNoiseModeStart, mBinding.tvNoiseModeStart, item, isCurrentMode);
                    break;
                case 1:
                    updateVoiceMode(mBinding.ivNoiseModeEnd, mBinding.tvNoiseModeEnd, item, isCurrentMode);
                    break;
                case 2:
                    updateVoiceMode(mBinding.ivNoiseModeMid, mBinding.tvNoiseModeMid, item, isCurrentMode);
                    break;
            }
        }
        updateANCFuncUI();
    }

    private void setVoiceMode(int modeID) {
        mPresenter.setCurrentVoiceMode(modeID);
    }

    private void updateANCFuncUI() {
        if (isDetached() || !isAdded()) return;
        VoiceMode current = mPresenter.getCurrentVoiceMode();
        if (null == current || current.getMode() == VoiceMode.VOICE_MODE_CLOSE) {
            mBinding.viewAncFunc.getRoot().setVisibility(View.GONE);
            mBinding.viewTransparentFunc.getRoot().setVisibility(View.GONE);
            return;
        }
        boolean isAncMode = current.getMode() == VoiceMode.VOICE_MODE_DENOISE;
        boolean isTransparentMode = current.getMode() == VoiceMode.VOICE_MODE_TRANSPARENT;
        mBinding.viewAncFunc.getRoot().setVisibility(isAncMode ? View.VISIBLE : View.GONE);
        mBinding.viewTransparentFunc.getRoot().setVisibility(isTransparentMode ? View.VISIBLE : View.GONE);
        if (isAncMode) {
            boolean isShowAdaptiveANC = mPresenter.isSupportAdaptiveANC();
            JL_Log.d(TAG, "updateANCFuncUI : isShowAdaptiveANC = " + isShowAdaptiveANC);
            mBinding.viewAncFunc.groupAdaptiveAnc.setVisibility(isShowAdaptiveANC ? View.VISIBLE : View.GONE);
            if (isShowAdaptiveANC) {
                AdaptiveData adaptiveData = mPresenter.getAdaptiveANCData();
                JL_Log.d(TAG, "updateAdaptiveANCUI : " + adaptiveData);
                mBinding.viewAncFunc.switchAdaptiveAnc.setEnabled(adaptiveData != null);
                if (adaptiveData == null) {
                    mBinding.viewAncFunc.switchAdaptiveAnc.setCheckedImmediatelyNoEvent(false);
                    mPresenter.updateAdaptiveANc();
                } else {
                    mBinding.viewAncFunc.switchAdaptiveAnc.setCheckedImmediatelyNoEvent(adaptiveData.isOn());
                }
            }
            boolean isSupportSceneDenoising = mPresenter.isSupportSceneDenoising();
            mBinding.viewAncFunc.groupSceneDenoising.setVisibility(isSupportSceneDenoising ? View.VISIBLE : View.GONE);
            if (isSupportSceneDenoising) {
                SceneDenoising sceneDenoising = mPresenter.getDeviceInfo().getSceneDenoising();
                JL_Log.d(TAG, "updateAdaptiveANCUI : sceneDenoising = " + sceneDenoising);
                mBinding.viewAncFunc.tvSceneDenoisingMode.setEnabled(null != sceneDenoising);
                if (null == sceneDenoising) {
                    mBinding.viewAncFunc.tvSceneDenoisingMode.setText("");
                    mPresenter.querySceneDenoising();
                } else {
                    mBinding.viewAncFunc.tvSceneDenoisingMode.setText(getSceneDenoisingMode(sceneDenoising.getMode()));
                }
            }
            boolean isSupportWindNoiseDetection = mPresenter.isSupportWindNoiseDetection();
            mBinding.viewAncFunc.groupWindNoiseDetection.setVisibility(isSupportWindNoiseDetection ? View.VISIBLE : View.GONE);
            if (isSupportWindNoiseDetection) {
                WindNoiseDetection detection = mPresenter.getDeviceInfo().getWindNoiseDetection();
                JL_Log.d(TAG, "updateAdaptiveANCUI : detection = " + detection);
                mBinding.viewAncFunc.switchWindNoiseDetection.setEnabled(null != detection);
                if (null == detection) {
                    mBinding.viewAncFunc.switchWindNoiseDetection.setCheckedImmediatelyNoEvent(false);
                    mPresenter.queryWindNoiseDetection();
                } else {
                    mBinding.viewAncFunc.switchWindNoiseDetection.setCheckedImmediatelyNoEvent(detection.isOn());
                }
            }
        } else if (isTransparentMode) {
            boolean isSupportVocalBooster = mPresenter.isSupportVocalBooster();
            mBinding.viewTransparentFunc.gtoupVocalBooster.setVisibility(isSupportVocalBooster ? View.VISIBLE : View.GONE);
            if (isSupportVocalBooster) {
                VocalBooster vocalBooster = mPresenter.getDeviceInfo().getVocalBooster();
                JL_Log.d(TAG, "updateAdaptiveANCUI : vocalBooster = " + vocalBooster);
                mBinding.viewTransparentFunc.switchVocalBooster.setEnabled(null != vocalBooster);
                if (null == vocalBooster) {
                    mBinding.viewTransparentFunc.switchVocalBooster.setCheckedImmediatelyNoEvent(false);
                    mPresenter.queryVocalBooster();
                } else {
                    mBinding.viewTransparentFunc.switchVocalBooster.setCheckedImmediatelyNoEvent(vocalBooster.isOn());
                }
            }
        }
    }

    private void updateSmartNoPickUI() {
        if (isDetached() || !isAdded()) return;
        boolean isSupportSmartNoPick = mPresenter.isSupportSmartNoPick();
        mBinding.cvSmartNoPick.setVisibility(isSupportSmartNoPick ? View.VISIBLE : View.GONE);
        if (isSupportSmartNoPick) {
            SmartNoPick smartNoPick = mPresenter.getDeviceInfo().getSmartNoPick();
            if (null == smartNoPick) {
                mBinding.switchSmartNoPick.setCheckedImmediatelyNoEvent(false);
                mPresenter.querySmartNoPick();
            } else {
                mBinding.switchSmartNoPick.setCheckedImmediatelyNoEvent(smartNoPick.isOn());
            }
        }
    }

    private void handleAdaptiveANCCheckState(int state, int code) {
        if (isDetached() || !isAdded()) return;
        JL_Log.d(TAG, "handleAdaptiveANCCheckState : state = " + state + ", code = " + code);
        if (state == 1) {
            showANCCheckLoadingDialog();
        } else {
            dismissANCCheckLoadingDialog();
            if (code == 0 && SConstant.TEST_ANC_FUNC) {
                mPresenter.setAdaptiveANC(new AdaptiveData().setOn(true));
            }
            mPresenter.updateAdaptiveANc();
            if (code != 0) {
                showANCCheckResultDialog(code);
            }
        }
    }

    private void showAdaptiveANCTipsDialog() {
        if (isDetached() || !isAdded()) return;
        Jl_Dialog.builder()
                .title(getString(R.string.enable_adaptive_anc_tips))
                .width(0.95f)
                .cancel(false)
                .content("     ")
                .left(getString(R.string.redetect))
                .leftColor(getResources().getColor(R.color.blue_448eff))
                .leftClickListener((v, dialogFragment) -> {
                    mPresenter.startAdaptiveANCCheck();
                    dialogFragment.dismiss();
                })
                .right(getString(R.string.enable_now))
                .rightColor(getResources().getColor(R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    mPresenter.setAdaptiveANC(new AdaptiveData().setOn(true));
                    dialogFragment.dismiss();
                })
                .build().show(getChildFragmentManager(), "Adaptive_ANC_tips");
    }

    private void showANCCheckLoadingDialog() {
        if (isDetached() || !isAdded()) return;
        if (null == mANCCheckLoadingDialog) {
            mANCCheckLoadingDialog = Jl_Dialog.builder()
                    .showProgressBar(true)
                    .cancel(false)
                    .width(0.95f)
                    .title(getString(R.string.monitoring))
//                    .left(getString(R.string.cancel))
//                    .leftColor(getResources().getColor(R.color.blue_448eff))
//                    .leftClickListener((v, dialogFragment) -> {
//                        mPresenter.setAdaptiveANC(new AdaptiveData().setOn(true));
//                        dismissANCCheckLoadingDialog();
//                    })
                    .build();
        }
        if (!mANCCheckLoadingDialog.isShow()) {
            mANCCheckLoadingDialog.show(getChildFragmentManager(), "ANC_check_loading");
        }
    }

    private void dismissANCCheckLoadingDialog() {
        if (isDetached() || !isAdded()) return;
        if (null != mANCCheckLoadingDialog) {
            if (mANCCheckLoadingDialog.isShow()) {
                mANCCheckLoadingDialog.dismiss();
            }
            mANCCheckLoadingDialog = null;
        }
    }

    private void showANCCheckResultDialog(int code) {
        if (isDetached() || !isAdded()) return;
        Jl_Dialog.builder()
                .width(0.95f)
                .title(getString(R.string.adaptive_anc_failure))
                .content(getString(R.string.adaptive_anc_failure_tips))
                .left(getString(R.string.give_up))
                .leftColor(getResources().getColor(R.color.blue_448eff))
                .leftClickListener((v, dialog) -> {
                    mPresenter.setAdaptiveANC(new AdaptiveData().setOn(true));
                    dialog.dismiss();
                })
                .right(getString(R.string.retry))
                .rightColor(getResources().getColor(R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    mPresenter.startAdaptiveANCCheck();
                    dialogFragment.dismiss();
                })
                .build()
                .show(getChildFragmentManager(), "ANC_check_result");
    }

    private String getSceneDenoisingMode(int mode) {
        String desc;
        switch (mode) {
            case SceneDenoising.MODE_SMART:
                desc = getString(R.string.scene_denoising_auto);
                break;
            case SceneDenoising.MODE_MILD:
                desc = getString(R.string.scene_denoising_mild);
                break;
            case SceneDenoising.MODE_BALANCE:
                desc = getString(R.string.scene_denoising_balance);
                break;
            case SceneDenoising.MODE_DEPTH:
                desc = getString(R.string.scene_denoising_depth);
                break;
            default:
                desc = "";
                break;
        }
        return desc;
    }

    private void updateChargingCaseSetting(boolean isShow) {
        mBinding.cvChargingCaseSetting.setVisibility(isShow ? View.VISIBLE : View.GONE);
        if (isShow) {
            updateSettingItem(mBinding.viewChargingCaseSetting, new SettingsItem(R.drawable.ic_charging_case_black, getString(R.string.charging_case_setting), "", true));
        }
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
            if (adapter == mKeyAdapter) {
                KeyBean item = mKeyAdapter.getItem(position);
                if (item == null || item.isHeader()) return;
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_DEV_KEY_BEAN, item);
                CommonActivity.startCommonActivity(DeviceSettingsFragment.this, SConstant.REQUEST_CODE_DEVICE_SETTINGS,
                        DevSettingsDetailsFragment.class.getCanonicalName(), bundle);
            } else if (adapter == mFunctionAdapter) {
                SettingsItem item = mFunctionAdapter.getItem(position);
                if (item == null) return;
                switch (item.getType()) {
                    case AttrAndFunCode.ADV_TYPE_DEVICE_NAME:
                        if (isCanUseTws()) {
                            showInputTextDialog(item.getValue());
                        } else {
                            ToastUtil.showToastShort(R.string.not_support_tips);
                        }
                        break;
                    case AttrAndFunCode.ADV_TYPE_LED_SETTINGS: {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(SConstant.KEY_ADV_INFO, mADVInfo);
                        CommonActivity.startCommonActivity(DeviceSettingsFragment.this,
                                SConstant.REQUEST_CODE_DEVICE_LED_SETTINGS, LedSettingsFragment.class.getCanonicalName(), bundle);
                        break;
                    }
                    case AttrAndFunCode.ADV_TYPE_WORK_MODE:
                    case AttrAndFunCode.ADV_TYPE_MIC_CHANNEL_SETTINGS:
                    case AttrAndFunCode.ADV_TYPE_IN_EAR_CHECK:
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(SConstant.KEY_SETTINGS_ITEM, item);
                        CommonActivity.startCommonActivity(DeviceSettingsFragment.this,
                                SConstant.REQUEST_CODE_DEVICE_SETTINGS, DevSettingsDetailsFragment.class.getCanonicalName(), bundle);
                        break;
                }
            }
        }
    };

    private final InputTextDialog.OnInputTextListener mOnInputTextListener = new InputTextDialog.OnInputTextListener() {
        @Override
        public void onDismiss(InputTextDialog dialog) {
            dismissInputTextDialog();
        }

        @Override
        public void onInputText(InputTextDialog dialog, String text) {

        }

        @Override
        public void onInputFinish(InputTextDialog dialog, String value, String lastValue) {
            JL_Log.e(TAG, "set name : " + value);
            if (TextUtils.isEmpty(value)) {
                ToastUtil.showToastShort(R.string.tip_empty_device_name);
            } else if (value.equals(lastValue)) {
                ToastUtil.showToastShort(R.string.tip_same_device_name);
            } else if (value.getBytes().length > (SConstant.LIMIT_DEVICE_NAME)) {
                ToastUtil.showToastShort(getString(R.string.device_name_over_limit, SConstant.LIMIT_DEVICE_NAME));
            } else {
                showRebootDialog(value);
                dismissInputTextDialog();
            }
        }
    };
}
