package com.jieli.btsmart.ui.settings.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.FunctionAdapter;
import com.jieli.btsmart.data.adapter.HeadsetKeyAdapter;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.settings.KeyBean;
import com.jieli.btsmart.data.model.settings.SettingsItem;
import com.jieli.btsmart.data.model.settings.VoiceModeItem;
import com.jieli.btsmart.data.model.settings.item.SettingsSwitch;
import com.jieli.btsmart.databinding.FragmentDeviceSettings2Binding;
import com.jieli.btsmart.databinding.ItemKeySettingsTwoBinding;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.auracast.receiver.AuracastReceiverFragment;
import com.jieli.btsmart.ui.auracast.transmitter.AuracastTransmitterFragment;
import com.jieli.btsmart.ui.chargingCase.ChargingCaseSettingFragment;
import com.jieli.btsmart.ui.chargingCase.message.MessagePushFragment;
import com.jieli.btsmart.ui.ota.FirmwareOtaFragment;
import com.jieli.btsmart.ui.settings.ModifyVoiceConfigFragment;
import com.jieli.btsmart.ui.settings.SettingsAdapter;
import com.jieli.btsmart.ui.settings.device.assistivelistening.AssistiveListeningFragment;
import com.jieli.btsmart.ui.settings.device.assistivelistening.HearingAssitstViewModel;
import com.jieli.btsmart.ui.settings.device.dual_connect.DualDevConnectFragment;
import com.jieli.btsmart.ui.settings.device.voice.SceneDenoiseFragment;
import com.jieli.btsmart.ui.settings.device.voice.SmartNoPickFragment;
import com.jieli.btsmart.ui.widget.InputTextDialog;
import com.jieli.btsmart.ui.widget.dialog.LoginDialog;
import com.jieli.btsmart.util.NotificationUtil;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
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
public class DeviceSettingsFragment extends DeviceControlFragment {


    public static void updateSettingItem(ItemKeySettingsTwoBinding binding, SettingsItem item) {
        if (null == binding || null == item) return;
        final int resId = item.getResId();
        if (resId == 0) {
            UIHelper.gone(binding.ivKeySettingsTwoIcon);
        } else {
            binding.ivKeySettingsTwoImg.setImageResource(resId);
        }
        final String name = item.getName();
        if (null != name) {
            binding.tvKeySettingsTwoKey.setText(name);
        }
        final String value = item.getValue();
        if (null == value) {
            UIHelper.gone(binding.tvKeySettingsTwoValue);
        } else {
            binding.tvKeySettingsTwoValue.setText(value);
        }
        if (item.isShowIcon()) {
            UIHelper.show(binding.ivKeySettingsTwoIcon);
        } else {
            UIHelper.gone(binding.ivKeySettingsTwoIcon);
        }
    }

    private final String TAG_GET_ADV_INFO_EMPTY = "tag_get_adv_info_empty";


    private FragmentDeviceSettings2Binding mBinding;
    private DeviceSettingsViewModel mViewModel;
    private HearingAssitstViewModel mHearingAssitstViewModel;

    private HeadsetKeyAdapter mKeyAdapter;
    private FunctionAdapter mFunctionAdapter;

    private Jl_Dialog mDisConnectNotifyDialog;
    private InputTextDialog mInputTextDialog;
    private Jl_Dialog mWaringDialog;

    private Jl_Dialog mANCCheckLoadingDialog;


    private int retryCount = 0;
    private List<VoiceModeItem> mModeItemList;
    private final static int MSG_GET_ADV_INFO_TIME_OUT = 0x0146;
    private final Handler mHandler = new Handler(msg -> {
        if (isInvalid()) return false;
        if (msg.what == MSG_GET_ADV_INFO_TIME_OUT) {
            mBinding.tvErrorMsg.setVisibility(View.VISIBLE);
            mBinding.tvErrorMsg.setTag(TAG_GET_ADV_INFO_EMPTY);
            mBinding.tvErrorMsg.setText(R.string.require_tws_info_error);
        }
        return true;
    });

    private final ActivityResultLauncher<Intent> functionSettingLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> mViewModel.syncDeviceSettings());

    private final ActivityResultLauncher<Intent> sceneDenoiseLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> mViewModel.syncSceneDenoising());

    private final ActivityResultLauncher<Intent> smartNoPickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> mViewModel.syncSmartNoPick());

    private final ActivityResultLauncher<Intent> adjustVoiceModeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> mViewModel.syncCurrentVoiceMode());

    private final ActivityResultLauncher<Intent> dulConnectionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> mViewModel.syncDoubleConnectionState());

    private final ActivityResultLauncher<Intent> openNotificationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

    });

    private final ActivityResultLauncher<Intent> syncMessageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        updateSettingItem(mBinding.viewSyncMessage, new SettingsItem(R.drawable.ic_message_black,
                getString(R.string.message_push), (mViewModel.isSyncMessage() ? getString(R.string.function_open) : getString(R.string.function_close)), true));
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentDeviceSettings2Binding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if (null == bundle) {
            finish();
            return;
        }
        final BluetoothDevice device = bundle.getParcelable(SConstant.KEY_DEVICE);
        if (null == device) {
            finish();
            return;
        }
        ADVInfoResponse advInfo = bundle.getParcelable(SConstant.KEY_ADV_INFO);
        mViewModel = new ViewModelProvider(requireActivity(), new DeviceSettingsViewModel.Factory(device))
                .get(DeviceSettingsViewModel.class);
        mHearingAssitstViewModel = new ViewModelProvider(requireActivity()).get(HearingAssitstViewModel.class);
        initUI();
        addObserver();

        if (null == advInfo) {
            mViewModel.syncDeviceSettings();
        } else {
            mViewModel.mDeviceSettingInfoMLD.postValue(advInfo);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        syncDeviceFunctionState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissANCCheckLoadingDialog();
        dismissDisconnectNotifyDialog();
        dismissInputTextDialog();
        dismissRebootDialog();
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            ((CommonActivity) requireActivity()).updateTopBar(UIHelper.getCacheDeviceName(mViewModel.mDevice),
                    R.drawable.ic_back_black, v -> finish(), 0, null);
        }

        mKeyAdapter = new HeadsetKeyAdapter();
        mKeyAdapter.setOnItemClickListener((adapter, view, position) -> {
            KeyBean item = mKeyAdapter.getItem(position);
            if (item == null || item.isHeader()) return;
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_DEVICE, mViewModel.mDevice);
            bundle.putParcelable(SConstant.KEY_DEV_KEY_BEAN, item);
            CommonActivity.startActivityForRequest(DeviceSettingsFragment.this,
                    DevSettingsDetailsFragment.class.getCanonicalName(), bundle, functionSettingLauncher);
        });

        mBinding.rvKeyList.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.rvKeyList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = ValueUtil.dp2px(requireContext(), 3);
            }
        });
        mBinding.rvKeyList.setAdapter(mKeyAdapter);

        mFunctionAdapter = new FunctionAdapter();
        mFunctionAdapter.setOnItemClickListener((adapter, view, position) -> {
            SettingsItem item = mFunctionAdapter.getItem(position);
            if (item == null) return;
            final BluetoothDevice device = mViewModel.mDevice;
            switch (item.getType()) {
                case AttrAndFunCode.ADV_TYPE_DEVICE_NAME:
                    if (mViewModel.isSupportTwsFunction()) {
                        showInputTextDialog(item.getValue());
                    } else {
                        showTips(getString(R.string.not_support_tips));
                    }
                    break;
                case AttrAndFunCode.ADV_TYPE_LED_SETTINGS: {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(SConstant.KEY_DEVICE, device);
                    bundle.putParcelable(SConstant.KEY_ADV_INFO, mViewModel.getDeviceSettingInfo());
                    CommonActivity.startActivityForRequest(DeviceSettingsFragment.this, LedSettingsFragment.class.getCanonicalName(),
                            bundle, functionSettingLauncher);
                    break;
                }
                case AttrAndFunCode.ADV_TYPE_WORK_MODE:
                case AttrAndFunCode.ADV_TYPE_MIC_CHANNEL_SETTINGS:
                case AttrAndFunCode.ADV_TYPE_IN_EAR_CHECK:
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(SConstant.KEY_DEVICE, device);
                    bundle.putParcelable(SConstant.KEY_SETTINGS_ITEM, item);
                    CommonActivity.startActivityForRequest(DeviceSettingsFragment.this, DevSettingsDetailsFragment.class.getCanonicalName(),
                            bundle, functionSettingLauncher);
                    break;
            }
        });
        mBinding.rvFuncList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvFuncList.setAdapter(mFunctionAdapter);

        mBinding.tvDisconnect.setOnClickListener(v -> showDisconnectNotifyDialog());
        mBinding.tvNoNetwork.setOnClickListener(v -> startActivityForResult(new Intent("android.settings.SETTINGS"), SConstant.REQUEST_CODE_NETWORK));
        mBinding.viewNoiseControl.ivVoiceAdjust.setOnClickListener(v -> {
            final VoiceMode currentMode = mViewModel.getCurrentVoiceMode();
            if (currentMode == null || currentMode.getMode() == VoiceMode.VOICE_MODE_CLOSE) return;
            String text = currentMode.getMode() == VoiceMode.VOICE_MODE_DENOISE ? getString(R.string.denoise_value) : getString(R.string.transparent_value);
            Bundle bundle = new Bundle();
            bundle.putParcelable(ModifyVoiceConfigFragment.KEY_VOICE_MODE, currentMode);
            ContentActivity.startActivityForRequest(this, ModifyVoiceConfigFragment.class.getCanonicalName(), text, bundle, adjustVoiceModeLauncher);
        });
        mBinding.viewNoiseControl.ivNoiseModeMid.setOnClickListener(v -> setVoiceMode(mModeItemList.get(2).getMode()));
        mBinding.viewNoiseControl.ivNoiseModeStart.setOnClickListener(v -> setVoiceMode(mModeItemList.get(0).getMode()));
        mBinding.viewNoiseControl.ivNoiseModeEnd.setOnClickListener(v -> setVoiceMode(mModeItemList.get(1).getMode()));
        mBinding.tvErrorMsg.setOnClickListener(v -> {
            String tag = (String) v.getTag();
            if (TextUtils.equals(tag, TAG_GET_ADV_INFO_EMPTY)) {
                mViewModel.syncDeviceSettings();
                showTips(getString(R.string.retrieved));
            }
        });
        mBinding.viewOtaFunction.getRoot().setOnClickListener(v -> {
            CommonActivity.startCommonActivity(requireActivity(), FirmwareOtaFragment.class.getCanonicalName());
            requireActivity().sendBroadcast(new Intent(SConstant.ACTION_DEVICE_UPGRADE));
        });
        mBinding.viewAssistiveListeningSettings.getRoot().setOnClickListener(v -> {
            mHearingAssitstViewModel.getFittingConfigure(new OnRcspActionCallback<HearingAssistInfo>() {
                @Override
                public void onSuccess(BluetoothDevice device, HearingAssistInfo hearingAssistInfo) {
                   /* hearingAssistInfo = new HearingAssistInfo();
                    hearingAssistInfo.setChannels(6);
                    hearingAssistInfo.setVersion(1);
                    hearingAssistInfo.setFrequencies(new int[]{250, 500, 1000, 2000, 4000, 6000});*/
                    if (hearingAssistInfo == null) {
                        showTips(getString(R.string.msg_read_file_err_reading));
                        return;
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString(AssistiveListeningFragment.KEY_HEARING_ASSIST_INFO, new Gson().toJson(hearingAssistInfo));
                    CommonActivity.startCommonActivity(requireActivity(), AssistiveListeningFragment.class.getCanonicalName(), bundle);
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {
                    showTips(getString(R.string.msg_read_file_err_reading));
                }
            });
        });
        mBinding.viewSmartNoPick.switchSmartNoPick.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SmartNoPick smartNoPick = new SmartNoPick();
            smartNoPick.setOn(isChecked);
            mViewModel.changeSmartNoPick(smartNoPick);
        });
        mBinding.viewNoiseControl.viewAncFunc.switchAdaptiveAnc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                mViewModel.setAdaptiveANC(new AdaptiveData());
                return;
            }
            showAdaptiveANCTipsDialog();
        });
        mBinding.viewNoiseControl.viewAncFunc.tvSceneDenoisingMode.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_DEVICE, mViewModel.mDevice);
            CommonActivity.startActivityForRequest(DeviceSettingsFragment.this, SceneDenoiseFragment.class.getSimpleName(),
                    bundle, sceneDenoiseLauncher);
        });
        mBinding.viewNoiseControl.viewAncFunc.switchWindNoiseDetection.setOnCheckedChangeListener((buttonView, isChecked) -> {
            WindNoiseDetection detection = new WindNoiseDetection();
            detection.setOn(isChecked);
            mViewModel.changeWindNoiseDetection(detection);
        });
        mBinding.viewNoiseControl.viewTransparentFunc.switchVocalBooster.setOnCheckedChangeListener((buttonView, isChecked) -> {
            VocalBooster vocalBooster = new VocalBooster();
            vocalBooster.setOn(isChecked);
            mViewModel.changeVocalBooster(vocalBooster);
        });
        mBinding.viewSmartNoPick.tvSmartNoPickTips.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_DEVICE, mViewModel.mDevice);
            CommonActivity.startActivityForRequest(DeviceSettingsFragment.this, SmartNoPickFragment.class.getSimpleName(),
                    bundle, smartNoPickLauncher);
        });

        mBinding.viewDualDevConnect.getRoot().setOnClickListener(v -> {
            if (mViewModel.isSupportDoubleConnection() && mViewModel.getDoubleConnectionState() != null) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_DEVICE, mViewModel.mDevice);
                CommonActivity.startActivityForRequest(DeviceSettingsFragment.this, DualDevConnectFragment.class.getCanonicalName(),
                        bundle, dulConnectionLauncher);
            }
        });
        mBinding.viewChargingCaseSetting.getRoot().setOnClickListener(v ->
                ContentActivity.startActivity(requireContext(), ChargingCaseSettingFragment.class.getCanonicalName(), getString(R.string.charging_case_setting)));
        mBinding.viewSyncMessage.getRoot().setOnClickListener(v -> {
            if (!NotificationUtil.isNotificationEnable(requireContext())) {
                showOpenAppSettingsDialog();
                return;
            }
            if (!NotificationUtil.isNotificationServiceEnabled(requireContext())) {
                showOpenNotificationServerDialog();
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_DEVICE, mViewModel.mDevice);
            ContentActivity.startActivityForRequest(DeviceSettingsFragment.this, MessagePushFragment.class.getCanonicalName(),
                    getString(R.string.message_push), bundle, syncMessageLauncher);
        });
        mBinding.viewSyncWeather.switchBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mViewModel.changeSyncWeather(isChecked);
        });
        mBinding.viewAuracastReceiver.getRoot().setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_BLUETOOTH_DEVICE, mViewModel.mDevice);
            ContentActivity.startActivity(requireContext(), AuracastReceiverFragment.class.getCanonicalName(),
                    getString(R.string.listen_auracast_broadcast), bundle);
        });
        mBinding.viewAuracastTransmitter.getRoot().setOnClickListener(v -> showLoginDialog(mViewModel.mDevice));
        updateOtaSettings();
    }

    private void addObserver() {
        mViewModel.switchDeviceMLD.observe(getViewLifecycleOwner(), device -> {
            if (!BluetoothUtil.deviceEquals(mViewModel.mDevice, device)) {
                finish();
            }
        });
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> {
            if (!BluetoothUtil.deviceEquals(mViewModel.mDevice, connection.getDevice())) return;
            if (connection.getStatus() != StateCode.CONNECTION_OK
                    || connection.getStatus() != StateCode.CONNECTION_CONNECTED) {
                finish();
            }
        });

        mViewModel.mNetworkStateMLD.observe(getViewLifecycleOwner(), available -> {
            if (available) {
                UIHelper.gone(mBinding.tvNoNetwork);
            } else {
                UIHelper.show(mBinding.tvNoNetwork);
            }
        });
        mViewModel.mActionEventMLD.observe(getViewLifecycleOwner(), action -> {
            switch (action) {
                case DeviceSettingsViewModel.ACTION_UPDATE_JSON: {
                    updateDeviceSettings(mViewModel.getDeviceSettingInfo());
                    break;
                }
                case DeviceSettingsViewModel.ACTION_OPEN_SMART_NO_PICK_SUCCESS: {
                    mBinding.viewSmartNoPick.tvSmartNoPickTips.performClick();
                    break;
                }
            }
        });
        mViewModel.mOpResultMLD.observe(getViewLifecycleOwner(), opRet -> {
            if (!opRet.isSuccess()) {
                showTips(opRet.getMessage());
            }
        });
        mViewModel.mFlowStateMLD.observe(getViewLifecycleOwner(), flowState -> {
            JL_Log.d(TAG, "mFlowStateMLD", "flowState = " + flowState);
            switch (flowState.getOp()) {
                case DeviceSettingsViewModel.OP_START_ADAPTIVE_ANC_FLOW: {
                    if (flowState.getState() == StateResult.STATE_WORKING) {
                        showANCCheckLoadingDialog();
                        return;
                    }
                    dismissANCCheckLoadingDialog();
                    if (flowState.getState() == StateResult.STATE_FINISH) {
                        if (flowState.isSuccess()) {
                            mViewModel.syncAdaptiveANc();
                            return;
                        }
                        showANCCheckResultDialog(flowState.getCode());
                    }
                    break;
                }
                case DeviceSettingsViewModel.OP_SET_SMART_NO_PICK: {
                    break;
                }
            }
        });
        mViewModel.mDeviceSettingInfoMLD.observe(getViewLifecycleOwner(), this::updateDeviceSettings);
        mViewModel.mVoiceModeListMLD.observe(getViewLifecycleOwner(), this::updateNoiseCtrlUI);
        mViewModel.mCurrentVoiceModeMLD.observe(getViewLifecycleOwner(), this::updateCurrentVoiceMode);
        mViewModel.mVoiceFunctionMLD.observe(getViewLifecycleOwner(), this::updateVoiceFunc);
        mViewModel.mDualConnectionStateMLD.observe(getViewLifecycleOwner(), this::updateDoubleConnectionState);
        mViewModel.mChargingCaseInitMLD.observe(getViewLifecycleOwner(), initState -> {
            if (initState == ErrorCode.ERR_NONE) {
                updateChargingCaseSetting(true);
                updateDeviceSettings(mViewModel.getDeviceSettingInfo());
                return;
            }
            updateChargingCaseSetting(false);
        });
    }

    private void updateDeviceSettings(ADVInfoResponse advInfo) {
        if (isInvalid()) return;
        final BluetoothDevice device = mViewModel.mDevice;
        final boolean isSupportTwsFunc = mViewModel.isSupportTwsFunction();
        JL_Log.d(TAG, "updateDeviceSettings", "device : " + device + ", isSupportTwsFunc : " + isSupportTwsFunc
                + ",  " + advInfo);
        if (advInfo != null) {
            mHandler.removeMessages(MSG_GET_ADV_INFO_TIME_OUT);
            mBinding.tvErrorMsg.setVisibility(View.GONE);
            final HistoryBluetoothDevice history = mViewModel.findHistory(device);
            updateKeySettingsList(getKeyListFromADVInfo(advInfo, history == null ? -1 : history.getAdvVersion()));
            updateFunctionList(getFuncListFromADVInfo(advInfo));
        } else if (isSupportTwsFunc) {
            if (retryCount < 3) {
                mViewModel.syncDeviceSettings();
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
        } else {
            updateKeySettingsList(null);
            updateFunctionList(getFuncListFromBtDevice(mViewModel.mDevice));
        }
        retryCount = 0;
    }

    private void updateOtaSettings() {
        boolean isHasOTA = SConstant.HAS_OTA;
        ProductMessage.DeviceBean deviceMsg;
        final DeviceInfo deviceInfo = mViewModel.getDeviceInfo(mViewModel.mDevice.getAddress());
        if (deviceInfo == null) {
            finish();
            return;
        }
        if (SConstant.CHANG_DIALOG_WAY) {
            deviceMsg = ProductCacheManager.getInstance().getDeviceMessageModify(MainApplication.getApplication(), deviceInfo.getVid(), deviceInfo.getUid(), deviceInfo.getPid());
        } else {
            deviceMsg = ProductUtil.getDeviceMessage(MainApplication.getApplication(), deviceInfo.getVid(), deviceInfo.getUid(), deviceInfo.getPid());
        }
        if (deviceMsg != null) {
            isHasOTA = deviceMsg.getHasOta() > 0;
        }
        JL_Log.i(TAG, "updateOtaSettings", "deviceMsg >>> " + deviceMsg + ", isHasOTA = " + isHasOTA);
        updateOtaFunction(isHasOTA);
    }

    private List<KeyBean> getKeyListFromADVInfo(ADVInfoResponse advInfo, int advVersion) {
        if (advInfo == null || !isAdded() || isDetached()) return null;
        List<ADVInfoResponse.KeySettings> keySettingsList = advInfo.getKeySettingsList();
        if (keySettingsList == null) return null;
        List<KeyBean> list = new ArrayList<>();
        Map<Integer, ArrayList<KeyBean>> keyBeanMap = new HashMap<>();
        JL_Log.d(TAG, "getKeyListFromADVInfo", "keySettingsList size:" + keySettingsList.size());
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
        if (ledSettings != null && !ledSettings.isEmpty()) {
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
        if (isInvalid()) return;
        if (list == null) list = new ArrayList<>();
        if (list.isEmpty()) {
            UIHelper.gone(mBinding.rvKeyList);
            return;
        }
        UIHelper.show(mBinding.rvKeyList);
        mKeyAdapter.setList(list);
    }

    private void updateFunctionList(List<SettingsItem> list) {
        if (isInvalid()) return;
        if (list == null) list = new ArrayList<>();
        if (list.isEmpty()) {
            UIHelper.gone(mBinding.rvFuncList);
            return;
        }
        UIHelper.show(mBinding.rvFuncList);
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mBinding.rvFuncList.getLayoutParams();
        /*if (list.size() == 1) {
            layoutParams.setMargins(0, 0, 0, 0);
        } else */
        {
            layoutParams.setMargins(0, ValueUtil.dp2px(requireContext(), 12), 0, ValueUtil.dp2px(requireContext(), 12));
        }
        mBinding.rvFuncList.setLayoutParams(layoutParams);
        mFunctionAdapter.setList(list);
    }

    private void updateOtaFunction(boolean isShow) {
        if (!isShow) {
            UIHelper.gone(mBinding.viewOtaFunction.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewOtaFunction.getRoot());
        mBinding.viewOtaFunction.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        updateSettingItem(mBinding.viewOtaFunction, new SettingsItem(R.drawable.ic_upgrade,
                getString(R.string.firmware_update), "", true));
    }

    private void syncDeviceFunctionState() {
        final DeviceInfo deviceInfo = mViewModel.getDeviceInfo(mViewModel.mDevice.getAddress());
        if (null == deviceInfo) return;
        updateNoiseCtrlUI(mViewModel.getVoiceModeList());
        updateAssistiveListening();
        updateSmartNoPickUI();
        updateDualDevConnect();
        updateAuracastReceiverUI();
        updateAuracastTransmitterUI();
    }

    private void showDisconnectNotifyDialog() {
        if (isInvalid()) return;
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
                        dismissDisconnectNotifyDialog();
                        mViewModel.disconnectDevice();
                    })
                    .build();
        }
        if (!mDisConnectNotifyDialog.isShow()) {
            mDisConnectNotifyDialog.show(getChildFragmentManager(), "notify_dialog");
        }
    }

    private void dismissDisconnectNotifyDialog() {
        if (isInvalid()) return;
        if (mDisConnectNotifyDialog != null) {
            if (mDisConnectNotifyDialog.isShow()) {
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
        if (isInvalid()) return;
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
                    mViewModel.changeDeviceName(deviceName, true);
                }
                dismissRebootDialog();
            });
            tvLeftButton.setOnClickListener(v -> {
                Bundle bundle = mWaringDialog.getArguments();
                if (bundle != null) {
                    String deviceName = bundle.getString(SConstant.KEY_DEV_NAME);
                    mViewModel.changeDeviceName(deviceName, false);
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
        if (isInvalid()) return;
        DeviceInfo deviceInfo = mViewModel.getDeviceInfo(mViewModel.mDevice.getAddress());
        if (deviceInfo == null) return;
        boolean isHasData = deviceInfo.isSupportHearingAssist();
        if (!isHasData) {
            UIHelper.gone(mBinding.viewAssistiveListeningSettings.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewAssistiveListeningSettings.getRoot());
        mBinding.viewAssistiveListeningSettings.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        updateSettingItem(mBinding.viewAssistiveListeningSettings, new SettingsItem(R.drawable.ic_assistive_listening,
                getString(R.string.hearing_aid_fitting), "", true));
    }

    private void updateDualDevConnect() {
        if (isInvalid()) return;
        boolean isSupportDualDev = mViewModel.isSupportDoubleConnection(); //目前固件暂不支持
        JL_Log.d(TAG, "updateDualDevConnect", "isSupportDualDev : " + isSupportDualDev);
        if (!isSupportDualDev) {
            UIHelper.gone(mBinding.viewDualDevConnect.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewDualDevConnect.getRoot());
        mBinding.viewDualDevConnect.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        DoubleConnectionState state = mViewModel.getDoubleConnectionState();
        updateDoubleConnectionState(state);
        mViewModel.syncDoubleConnectionState();
        ConnectedBtInfo connectedBtInfo = mViewModel.getConnectedBtInfo();
        if (null == connectedBtInfo) {
            mViewModel.syncConnectedBtInfo();
        }
    }

    private void updateDoubleConnectionState(DoubleConnectionState state) {
        boolean isOn = state != null && state.isOn();
        final String value = isOn ? getString(R.string.function_open) : getString(R.string.function_close);
        updateSettingItem(mBinding.viewDualDevConnect, new SettingsItem(R.drawable.ic_function_dual_dev_connection,
                getString(R.string.double_connection), value, true));
    }

    private void updateNoiseCtrlUI(List<VoiceMode> list) {
        if (isInvalid()) return;
        boolean isHasData = list != null && !list.isEmpty();
        JL_Log.d(TAG, "updateNoiseCtrlUI", "isHasData : " + isHasData);
        if (!isHasData) {
            UIHelper.gone(mBinding.viewNoiseControl.getRoot());
            if (mViewModel.isSupportAnc()) {
                mViewModel.syncVoiceModeList();
            }
            return;
        }
        UIHelper.show(mBinding.viewNoiseControl.getRoot());
        mModeItemList = convertItemList(list);
        //获取当前模式
        int currentMode = -1;
        final VoiceMode mode = mViewModel.getCurrentVoiceMode();
        JL_Log.d(TAG, "updateNoiseCtrlUI", "mode : " + mode);
        if (null != mode) {
            currentMode = mode.getMode();
        } else {
            mViewModel.syncCurrentVoiceMode();
        }
        updateVoiceUI(mModeItemList, currentMode);
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
            imageView.setBackgroundColor(getResources().getColor(R.color.color_transparent));
        }
        textView.setText(item.getName());
        boolean isHide = item.getMode() == VoiceMode.VOICE_MODE_CLOSE && isCurrentMode;
        if (isHide) {
            UIHelper.hide(mBinding.viewNoiseControl.ivVoiceAdjust);
            return;
        }
        UIHelper.show(mBinding.viewNoiseControl.ivVoiceAdjust);
    }

    private void updateVoiceUI(List<VoiceModeItem> list, int currentMode) {
        for (int i = 0; i < list.size(); i++) {
            VoiceModeItem item = list.get(i);
            boolean isCurrentMode = item.getMode() == currentMode;
            switch (i) {
                case 0:
                    updateVoiceMode(mBinding.viewNoiseControl.ivNoiseModeStart, mBinding.viewNoiseControl.tvNoiseModeStart, item, isCurrentMode);
                    break;
                case 1:
                    updateVoiceMode(mBinding.viewNoiseControl.ivNoiseModeEnd, mBinding.viewNoiseControl.tvNoiseModeEnd, item, isCurrentMode);
                    break;
                case 2:
                    updateVoiceMode(mBinding.viewNoiseControl.ivNoiseModeMid, mBinding.viewNoiseControl.tvNoiseModeMid, item, isCurrentMode);
                    break;
            }
        }
        updateANCFuncUI();
    }

    private void setVoiceMode(int modeID) {
        mViewModel.setCurrentVoiceMode(modeID);
    }

    private void updateANCFuncUI() {
        if (isInvalid()) return;
        final VoiceMode current = mViewModel.getCurrentVoiceMode();
        if (null == current || current.getMode() == VoiceMode.VOICE_MODE_CLOSE) {
            UIHelper.gone(mBinding.viewNoiseControl.viewAncFunc.getRoot());
            UIHelper.gone(mBinding.viewNoiseControl.viewTransparentFunc.getRoot());
            return;
        }
        boolean isAncMode = current.getMode() == VoiceMode.VOICE_MODE_DENOISE;
        boolean isTransparentMode = current.getMode() == VoiceMode.VOICE_MODE_TRANSPARENT;
        if (isTransparentMode) {
            UIHelper.show(mBinding.viewNoiseControl.viewTransparentFunc.getRoot());
        } else {
            UIHelper.gone(mBinding.viewNoiseControl.viewTransparentFunc.getRoot());
        }
        if (!isAncMode) {
            UIHelper.gone(mBinding.viewNoiseControl.viewAncFunc.getRoot());
            if (!isTransparentMode) return;
            boolean isSupportVocalBooster = mViewModel.isSupportVocalBooster();
            if (!isSupportVocalBooster) {
                UIHelper.gone(mBinding.viewNoiseControl.viewTransparentFunc.gtoupVocalBooster);
                return;
            }
            UIHelper.show(mBinding.viewNoiseControl.viewTransparentFunc.gtoupVocalBooster);
            VocalBooster vocalBooster = mViewModel.getVocalBooster();
            JL_Log.d(TAG, "updateANCFuncUI", "" + vocalBooster);
            mBinding.viewNoiseControl.viewTransparentFunc.switchVocalBooster.setEnabled(null != vocalBooster);
            if (null == vocalBooster) {
                mBinding.viewNoiseControl.viewTransparentFunc.switchVocalBooster.setCheckedImmediatelyNoEvent(false);
                mViewModel.syncVocalBooster();
            } else {
                mBinding.viewNoiseControl.viewTransparentFunc.switchVocalBooster.setCheckedImmediatelyNoEvent(vocalBooster.isOn());
            }
            return;
        }
        UIHelper.show(mBinding.viewNoiseControl.viewAncFunc.getRoot());
        boolean isShowAdaptiveANC = mViewModel.isSupportAdaptiveANC();
        JL_Log.d(TAG, "updateANCFuncUI", "isShowAdaptiveANC : " + isShowAdaptiveANC);
        mBinding.viewNoiseControl.viewAncFunc.groupAdaptiveAnc.setVisibility(isShowAdaptiveANC ? View.VISIBLE : View.GONE);
        if (isShowAdaptiveANC) {
            AdaptiveData adaptiveData = mViewModel.getAdaptiveANCData();
            JL_Log.d(TAG, "updateANCFuncUI", "" + adaptiveData);
            mBinding.viewNoiseControl.viewAncFunc.switchAdaptiveAnc.setEnabled(adaptiveData != null);
            if (adaptiveData == null) {
                mBinding.viewNoiseControl.viewAncFunc.switchAdaptiveAnc.setCheckedImmediatelyNoEvent(false);
                mViewModel.syncAdaptiveANc();
            } else {
                mBinding.viewNoiseControl.viewAncFunc.switchAdaptiveAnc.setCheckedImmediatelyNoEvent(adaptiveData.isOn());
            }
        }
        boolean isSupportSceneDenoising = mViewModel.isSupportSceneDenoising();
        mBinding.viewNoiseControl.viewAncFunc.groupSceneDenoising.setVisibility(isSupportSceneDenoising ? View.VISIBLE : View.GONE);
        JL_Log.d(TAG, "updateANCFuncUI", "isSupportSceneDenoising : " + isShowAdaptiveANC);
        if (isSupportSceneDenoising) {
            SceneDenoising sceneDenoising = mViewModel.getSceneDenoising();
            JL_Log.d(TAG, "updateANCFuncUI", "" + sceneDenoising);
            mBinding.viewNoiseControl.viewAncFunc.tvSceneDenoisingMode.setEnabled(null != sceneDenoising);
            if (null == sceneDenoising) {
                mBinding.viewNoiseControl.viewAncFunc.tvSceneDenoisingMode.setText("");
                mViewModel.syncSceneDenoising();
            } else {
                mBinding.viewNoiseControl.viewAncFunc.tvSceneDenoisingMode.setText(getSceneDenoisingMode(sceneDenoising.getMode()));
            }
        }
        boolean isSupportWindNoiseDetection = mViewModel.isSupportWindNoiseDetection();
        mBinding.viewNoiseControl.viewAncFunc.groupWindNoiseDetection.setVisibility(isSupportWindNoiseDetection ? View.VISIBLE : View.GONE);
        JL_Log.d(TAG, "updateANCFuncUI", "isSupportWindNoiseDetection : " + isSupportWindNoiseDetection);
        if (isSupportWindNoiseDetection) {
            WindNoiseDetection detection = mViewModel.getWindNoiseDetection();
            JL_Log.d(TAG, "updateANCFuncUI", "" + detection);
            mBinding.viewNoiseControl.viewAncFunc.switchWindNoiseDetection.setEnabled(null != detection);
            if (null == detection) {
                mBinding.viewNoiseControl.viewAncFunc.switchWindNoiseDetection.setCheckedImmediatelyNoEvent(false);
                mViewModel.syncWindNoiseDetection();
            } else {
                mBinding.viewNoiseControl.viewAncFunc.switchWindNoiseDetection.setCheckedImmediatelyNoEvent(detection.isOn());
            }
        }
    }

    private void updateVoiceFunc(VoiceFunc voiceFunc) {
        if (isInvalid() || null == voiceFunc) return;
        switch (voiceFunc.getType()) {
            case VoiceFunc.FUNC_ADAPTIVE:
                AdaptiveData adaptiveData = (AdaptiveData) voiceFunc;
                JL_Log.d(TAG, "onVoiceFuncChange", "" + adaptiveData);
                if (!mBinding.viewNoiseControl.viewAncFunc.switchAdaptiveAnc.isEnabled()) {
                    mBinding.viewNoiseControl.viewAncFunc.switchAdaptiveAnc.setEnabled(true);
                }
                mBinding.viewNoiseControl.viewAncFunc.switchAdaptiveAnc.setCheckedImmediatelyNoEvent(adaptiveData.isOn());
                break;
            case VoiceFunc.FUNC_SMART_NO_PICK:
                SmartNoPick smartNoPick = (SmartNoPick) voiceFunc;
                JL_Log.d(TAG, "onVoiceFuncChange", "" + smartNoPick);
                if (!mBinding.viewSmartNoPick.switchSmartNoPick.isEnabled()) {
                    mBinding.viewSmartNoPick.switchSmartNoPick.setEnabled(true);
                }
                mBinding.viewSmartNoPick.switchSmartNoPick.setCheckedImmediatelyNoEvent(smartNoPick.isOn());
                break;
            case VoiceFunc.FUNC_SCENE_DENOISING:
                SceneDenoising sceneDenoising = (SceneDenoising) voiceFunc;
                JL_Log.d(TAG, "onVoiceFuncChange", "" + sceneDenoising);
                if (!mBinding.viewNoiseControl.viewAncFunc.tvSceneDenoisingMode.isEnabled()) {
                    mBinding.viewNoiseControl.viewAncFunc.tvSceneDenoisingMode.setEnabled(true);
                }
                mBinding.viewNoiseControl.viewAncFunc.tvSceneDenoisingMode.setText(getSceneDenoisingMode(sceneDenoising.getMode()));
                break;
            case VoiceFunc.FUNC_WIND_NOISE_DETECTION:
                WindNoiseDetection detection = (WindNoiseDetection) voiceFunc;
                JL_Log.d(TAG, "onVoiceFuncChange", "" + detection);
                if (!mBinding.viewNoiseControl.viewAncFunc.switchWindNoiseDetection.isEnabled()) {
                    mBinding.viewNoiseControl.viewAncFunc.switchWindNoiseDetection.setEnabled(true);
                }
                mBinding.viewNoiseControl.viewAncFunc.switchWindNoiseDetection.setCheckedImmediatelyNoEvent(detection.isOn());
                break;
            case VoiceFunc.FUNC_VOCAL_BOOSTER:
                VocalBooster vocalBooster = (VocalBooster) voiceFunc;
                JL_Log.d(TAG, "onVoiceFuncChange", "" + vocalBooster);
                if (!mBinding.viewNoiseControl.viewTransparentFunc.switchVocalBooster.isEnabled()) {
                    mBinding.viewNoiseControl.viewTransparentFunc.switchVocalBooster.setEnabled(true);
                }
                mBinding.viewNoiseControl.viewTransparentFunc.switchVocalBooster.setCheckedImmediatelyNoEvent(vocalBooster.isOn());
                break;
        }
    }

    private void updateSmartNoPickUI() {
        if (isInvalid()) return;
        boolean isSupportSmartNoPick = mViewModel.isSupportSmartNoPick();
        JL_Log.d(TAG, "updateSmartNoPickUI", "isSupportSmartNoPick : " + isSupportSmartNoPick);
        if (!isSupportSmartNoPick) {
            UIHelper.gone(mBinding.viewSmartNoPick.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewSmartNoPick.getRoot());
        SmartNoPick smartNoPick = mViewModel.getSmartNoPick();
        if (null == smartNoPick) {
            mBinding.viewSmartNoPick.switchSmartNoPick.setCheckedImmediatelyNoEvent(false);
            mViewModel.syncSmartNoPick();
        } else {
            JL_Log.d(TAG, "updateSmartNoPickUI", "" + smartNoPick);
            mBinding.viewSmartNoPick.switchSmartNoPick.setCheckedImmediatelyNoEvent(smartNoPick.isOn());
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
                    mViewModel.startAdaptiveANCCheck();
                    dialogFragment.dismiss();
                })
                .right(getString(R.string.enable_now))
                .rightColor(getResources().getColor(R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    mViewModel.setAdaptiveANC(new AdaptiveData().setOn(true));
                    dialogFragment.dismiss();
                })
                .build().show(getChildFragmentManager(), "Adaptive_ANC_tips");
    }

    private void showANCCheckLoadingDialog() {
        if (isInvalid()) return;
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
        if (isInvalid()) return;
        if (null != mANCCheckLoadingDialog) {
            if (mANCCheckLoadingDialog.isShow()) {
                mANCCheckLoadingDialog.dismiss();
            }
            mANCCheckLoadingDialog = null;
        }
    }

    private void showANCCheckResultDialog(int code) {
        if (isInvalid()) return;
        Jl_Dialog.builder()
                .width(0.95f)
                .title(getString(R.string.adaptive_anc_failure))
                .content(getString(R.string.adaptive_anc_failure_tips))
                .left(getString(R.string.give_up))
                .leftColor(getResources().getColor(R.color.blue_448eff))
                .leftClickListener((v, dialog) -> {
                    mViewModel.setAdaptiveANC(new AdaptiveData().setOn(true));
                    dialog.dismiss();
                })
                .right(getString(R.string.retry))
                .rightColor(getResources().getColor(R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    mViewModel.startAdaptiveANCCheck();
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
        if (!isShow) {
            UIHelper.gone(mBinding.viewChargingCaseSetting.getRoot());
            UIHelper.gone(mBinding.viewSyncMessage.getRoot());
            UIHelper.gone(mBinding.viewSyncWeather.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewChargingCaseSetting.getRoot());
        mBinding.viewChargingCaseSetting.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        updateSettingItem(mBinding.viewChargingCaseSetting, new SettingsItem(R.drawable.ic_charging_case_black,
                getString(R.string.charging_case_setting), "", true));
        if (!mViewModel.is707NChargingCase()) return;
        UIHelper.show(mBinding.viewSyncMessage.getRoot());
        mBinding.viewSyncMessage.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        UIHelper.show(mBinding.viewSyncWeather.getRoot());
        mBinding.viewSyncWeather.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        boolean isSyncMessage = mViewModel.isSyncMessage();
        if (isSyncMessage) { //如果开启同步消息，检查下应用权限。怀疑部分手机会回收权限
            if (!NotificationUtil.isNotificationEnable(requireContext())
                    || !NotificationUtil.isNotificationServiceEnabled(requireContext())) {
                //检查权限，如果权限没有，就设置为false
                mViewModel.closeSyncMessage();
                isSyncMessage = false;
            }
        }
        updateSettingItem(mBinding.viewSyncMessage, new SettingsItem(R.drawable.ic_message_black,
                getString(R.string.message_push), (isSyncMessage ? getString(R.string.function_open) : getString(R.string.function_close)), true));
        SettingsAdapter.updateSettingSwitch(mBinding.viewSyncWeather, new SettingsSwitch(getString(R.string.weather_push),
                R.drawable.ic_weather_black, mViewModel.isSyncWeather()));
    }

    public void updateAuracastReceiverUI() {
        boolean isSupportAuracastReceiver = mViewModel.isSupportAuracastReceiver();
        JL_Log.d(TAG, "updateAuracastReceiverUI", "isSupportAuracastReceiver : " + isSupportAuracastReceiver);
        if (!isSupportAuracastReceiver) {
            UIHelper.gone(mBinding.viewAuracastReceiver.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewAuracastReceiver.getRoot());
        mBinding.viewAuracastReceiver.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        updateSettingItem(mBinding.viewAuracastReceiver, new SettingsItem(R.drawable.ic_auracast_receiver_black,
                getString(R.string.listen_auracast_broadcast), "", true));
    }

    public void updateAuracastTransmitterUI() {
        boolean isSupportAuracastTransmitter = mViewModel.isSupportAuracastTransmitter();
        JL_Log.d(TAG, "updateAuracastTransmitterUI", "isSupportAuracastTransmitter : " + isSupportAuracastTransmitter);
        if (!isSupportAuracastTransmitter) {
            UIHelper.gone(mBinding.viewAuracastTransmitter.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewAuracastTransmitter.getRoot());
        mBinding.viewAuracastTransmitter.getRoot().setBackgroundResource(R.drawable.bg_card_white_12_shape);
        updateSettingItem(mBinding.viewAuracastTransmitter, new SettingsItem(R.drawable.ic_auracast_transmitter_black,
                getString(R.string.configure_auracast_broadcast), "", true));
    }

    private void showOpenAppSettingsDialog() {
        if (isInvalid()) return;
        Jl_Dialog.builder()
                .content(getString(R.string.notification_permission_desc))
                .left(getString(R.string.cancel))
                .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                .right(getString(R.string.to_setting))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    NotificationUtil.openAppNotificationSettings(requireContext(), openNotificationLauncher);
                })
                .cancel(false)
                .build().show(getChildFragmentManager(), "OpenAppSettingsDialog");
    }

    private void showOpenNotificationServerDialog() {
        if (isInvalid()) return;
        Jl_Dialog.builder()
                .content(getString(R.string.notification_server_listener_desc))
                .left(getString(R.string.cancel))
                .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                .right(getString(R.string.to_setting))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    NotificationUtil.openNotificationListenerSettings(openNotificationLauncher);
                })
                .cancel(false)
                .build().show(getChildFragmentManager(), "OpenNotificationServerDialog");
    }

    private void showLoginDialog(BluetoothDevice device) {
        if (isInvalid()) return;
        new LoginDialog.Builder(device)
                .setCallback(result -> {
                    if (result) {
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(SConstant.KEY_BLUETOOTH_DEVICE, device);
                        ContentActivity.startActivity(requireContext(), AuracastTransmitterFragment.class.getCanonicalName(),
                                getString(R.string.configure_auracast_broadcast), bundle);
                        return;
                    }
                    showTips(getString(R.string.operation_failed, getString(R.string.op_login)));
                })
                .build().show(getChildFragmentManager(), LoginDialog.class.getSimpleName());
    }

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
            JL_Log.d(TAG, "onInputFinish", "Name : " + value);
            if (TextUtils.isEmpty(value)) {
                showTips(getString(R.string.tip_empty_device_name));
                return;
            }
            if (value.equals(lastValue)) {
                showTips(getString(R.string.tip_same_device_name));
                return;
            }
            if (value.getBytes().length > (SConstant.LIMIT_DEVICE_NAME)) {
                showTips(getString(R.string.device_name_over_limit, SConstant.LIMIT_DEVICE_NAME));
                return;
            }
            showRebootDialog(value);
            dismissInputTextDialog();
        }
    };
}
