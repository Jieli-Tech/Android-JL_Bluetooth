package com.jieli.btsmart.ui.settings.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.device.double_connect.ConnectedBtInfo;
import com.jieli.bluetooth.bean.device.double_connect.DoubleConnectionState;
import com.jieli.bluetooth.bean.device.voice.VoiceFunc;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.FuncSettingsAdapter;
import com.jieli.btsmart.data.model.settings.FunctionBean;
import com.jieli.btsmart.data.model.settings.KeyBean;
import com.jieli.btsmart.data.model.settings.LedBean;
import com.jieli.btsmart.data.model.settings.SettingsItem;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.widget.VoiceModeListDialog;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.component.base.BasePresenter;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_http.bean.KeySettingsBean;
import com.jieli.jl_http.bean.LedSettingsBean;
import com.jieli.jl_http.bean.ValueBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备设置详情界面
 */
public class DevSettingsDetailsFragment extends DeviceControlFragment implements IDeviceSettingsContract.IDeviceSettingsView {

    private TextView tvAction;
    private RecyclerView rvFuncList;
    private LinearLayout llNoiseCtrl;
    private TextView tvAncMode;

    private FuncSettingsAdapter mAdapter;
    private CommonActivity mActivity;

    private int attrType;
    private KeyBean mKeyBean;
    private SettingsItem mSettingsItem;
    private LedBean mLedBean;

    private int retryCount = 0;
    private BluetoothDevice mUseDevice;
    private VoiceModeListDialog mModeListDialog;

    private IDeviceSettingsContract.IDeviceSettingsPresenter mPresenter;

    public static DevSettingsDetailsFragment newInstance() {
        return new DevSettingsDetailsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dev_settings_details, container, false);
        tvAction = view.findViewById(R.id.tv_dev_settings_details_action);
        rvFuncList = view.findViewById(R.id.rv_dev_settings_func_list);
        llNoiseCtrl = view.findViewById(R.id.ll_dev_settings_details_anc);
        tvAncMode = view.findViewById(R.id.tv_dev_settings_details_anc_mode);
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        mPresenter = new DeviceSettingsPresenterImpl(mActivity, this);
        rvFuncList.setLayoutManager(new LinearLayoutManager(getContext()));
        llNoiseCtrl.setOnClickListener(v -> {
            if (mKeyBean.getFuncId() == AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE) {
                showVoiceModeListDialog(mPresenter.getNoiseModeMsg(), mPresenter.getSelectVoiceModes());
            } else {
                byte[] paramData = new byte[]{(byte) mKeyBean.getKeyId(), (byte) mKeyBean.getActionId(), (byte) AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE};
                mPresenter.modifyHeadsetFunctions(-1, attrType, paramData);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        mUseDevice = mPresenter.getConnectedDevice();
        if (bundle != null) {
            mKeyBean = bundle.getParcelable(SConstant.KEY_DEV_KEY_BEAN);
            mSettingsItem = bundle.getParcelable(SConstant.KEY_SETTINGS_ITEM);
            mLedBean = bundle.getParcelable(SConstant.KEY_DEV_LED_BEAN);
            attrType = mKeyBean == null ? -1 : mKeyBean.getAttrType();
            if (attrType == -1) {
                attrType = mSettingsItem == null ? -1 : mSettingsItem.getType();
            }
            if (attrType == -1) {
                attrType = mLedBean == null ? -1 : mLedBean.getAttrType();
            }
            updateKeyFuncList(attrType, mKeyBean, mSettingsItem, mLedBean);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissVoiceModeListDialog();
        if (mPresenter != null) {
            mPresenter.destroy();
        }
        mUseDevice = null;
        mKeyBean = null;
        mSettingsItem = null;
        mLedBean = null;
        mActivity = null;
    }

    @Override
    public void onBtAdapterStatus(boolean enable) {

    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (BluetoothUtil.deviceEquals(mUseDevice, device)
                && (status == StateCode.CONNECTION_FAILED || status == StateCode.CONNECTION_DISCONNECT)) {
            if (mActivity != null) {
                mActivity.onBackPressed();
            }
        }
    }

    @Override
    public void onSwitchDevice(BluetoothDevice device) {
        if (mActivity != null) {
            mActivity.onBackPressed();
        }
    }


    @Override
    public void onADVInfoUpdate(ADVInfoResponse advInfo) {
        if (advInfo == null) return;
        switch (attrType) {
            case AttrAndFunCode.ADV_TYPE_KEY_SETTINGS: {
                if (advInfo.getKeySettingsList() != null && mKeyBean != null) {
                    for (ADVInfoResponse.KeySettings settings : advInfo.getKeySettingsList()) {
                        if (settings.getKeyNum() == mKeyBean.getKeyId()
                                && settings.getAction() == mKeyBean.getActionId()) {
                            mKeyBean.setFuncId(settings.getFunction());
                            break;
                        }
                    }
                }
                break;
            }
            case AttrAndFunCode.ADV_TYPE_LED_SETTINGS: {
                if (advInfo.getLedSettingsList() != null && mLedBean != null) {
                    for (ADVInfoResponse.LedSettings settings : advInfo.getLedSettingsList()) {
                        if (settings.getScene() == mLedBean.getSceneId()) {
                            mLedBean.setEffectId(settings.getEffect());
                            break;
                        }
                    }
                }
                break;
            }
            case AttrAndFunCode.ADV_TYPE_WORK_MODE: {
                if (mSettingsItem != null && advInfo.getWorkModel() > 0) {
                    mSettingsItem.setValueId(advInfo.getWorkModel());
                }
                break;
            }
            case AttrAndFunCode.ADV_TYPE_MIC_CHANNEL_SETTINGS: {
                if (mSettingsItem != null && advInfo.getMicChannel() > 0) {
                    mSettingsItem.setValueId(advInfo.getMicChannel());
                }
                break;
            }
            case AttrAndFunCode.ADV_TYPE_IN_EAR_CHECK: {
                if (mSettingsItem != null && advInfo.getInEarSettings() > 0) {
                    mSettingsItem.setValueId(advInfo.getInEarSettings());
                }
                break;
            }
        }
        updateKeyFuncList(attrType, mKeyBean, mSettingsItem, mLedBean);
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

    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onConfigureSuccess(int position, int result) {
        if (mAdapter != null && !isDetached() && isAdded()) {
            int funcId = -1;
            int oldIndex = mAdapter.getSelectPos();
            if (oldIndex != -1) {
                FunctionBean oldItem = mAdapter.getItem(mAdapter.getSelectPos());
                if (oldItem != null) {
                    oldItem.setSelected(false);
                }
            }
            if (position != -1) {
                FunctionBean item = mAdapter.getItem(position);
                if (item != null) {
                    item.setSelected(true);
                    funcId = item.getFuncId();
                }
            }
            mAdapter.notifyDataSetChanged();
            if (attrType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS) {
                if (mKeyBean != null) {
                    mKeyBean.setFuncId(funcId);
                    if (mPresenter.isSupportAnc()) {
                        if (-1 == position) {
                            mKeyBean.setFuncId(AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE);
                            showVoiceModeListDialog(mPresenter.getNoiseModeMsg(), mPresenter.getSelectVoiceModes());
                        }
                        updateANCUI(mKeyBean);
                    }
                }
            } else if (attrType == AttrAndFunCode.ADV_TYPE_LED_SETTINGS) {
                if (mLedBean != null) mLedBean.setEffectId(funcId);
            } else {
                if (mSettingsItem != null) mSettingsItem.setValueId(funcId);
            }
        }
    }

    @Override
    public void onConfigureFailed(BaseError error) {
        if (error != null) {
            ToastUtil.showToastShort(error.getMessage());
        }
    }

    @Override
    public void onNetworkState(boolean isAvailable) {

    }

    @Override
    public void onUpdateConfigureSuccess() {

    }

    @Override
    public void onUpdateConfigureFailed(int code, String message) {

    }

    @Override
    public void onVoiceModeList(List<VoiceMode> list) {

    }

    @Override
    public void onCurrentVoiceMode(VoiceMode voiceMode) {

    }

    @Override
    public void onSelectedVoiceModes(byte[] selectedModes) {
        updateSelectedVoiceModes(selectedModes);
    }

    @Override
    public void onVoiceFuncChange(VoiceFunc data) {

    }

    @Override
    public void onAdaptiveANCCheck(int state, int code) {

    }

    @Override
    public void onOpenSmartNoPickSetting(boolean isUser) {

    }

    @Override
    public void onDoubleConnectionStateChange(DoubleConnectionState state) {

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

    private void updateKeyFuncList(int attrType, KeyBean keyBean, SettingsItem item, LedBean ledBean) {
        if (!isAdded() || isDetached()) return;
        if (attrType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS) {
            updateTopBar(keyBean == null ? null : keyBean.getKeyName());
            updateActionTv(keyBean == null ? null : keyBean.getAction());
            updateANCUI(keyBean);
        } else if (attrType == AttrAndFunCode.ADV_TYPE_LED_SETTINGS) {
//            viewLine.setVisibility(View.GONE);
            updateTopBar(ledBean == null ? null : ledBean.getScene());
            updateActionTv(null);
        } else {
//            viewLine.setVisibility(View.GONE);
            updateTopBar(item == null ? null : item.getName());
            updateActionTv(null);
        }
        updateFuncList(getKeyFuncListFromKeyBean(attrType, keyBean, item, ledBean));
    }

    private void updateANCUI(KeyBean keyBean) {
        boolean isSupportAnc = mPresenter.isSupportAnc();
        llNoiseCtrl.setVisibility(isSupportAnc ? View.VISIBLE : View.GONE);
        if (isSupportAnc) {
            boolean enableAnc = keyBean != null && keyBean.getFuncId() == AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE;
            tvAncMode.setText(enableAnc ? getString(R.string.enable) : "");
            mPresenter.getSelectVoiceModes();
        }
    }

    private void updateTopBar(String title) {
        if (mActivity != null && title != null) {
            mActivity.updateTopBar(title, R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);
        }
    }

    private List<FunctionBean> getKeyFuncListFromKeyBean(int attrType, KeyBean keyBean, SettingsItem settingsItem, LedBean ledBean) {
        if (mPresenter == null) return null;
        ADVInfoResponse advInfo = DeviceStatusManager.getInstance().getAdvInfo(mPresenter.getConnectedDevice());
        if (advInfo == null) {
            if (retryCount < 3) {
                mPresenter.updateDeviceADVInfo();
                retryCount++;
            } else {
                retryCount = 0;
            }
            return null;
        }
        retryCount = 0;
        List<FunctionBean> list = new ArrayList<>();
        List<ValueBean> valueBeanList;
        int selectedId;
        if (attrType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS) {
            if (keyBean == null) return null;
            selectedId = keyBean.getFuncId();
            KeySettingsBean keySettingsBean = ProductUtil.getCacheKeySettings(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid());
            valueBeanList = keySettingsBean == null ? null : keySettingsBean.getKeyFunctions();
        } else if (attrType == AttrAndFunCode.ADV_TYPE_LED_SETTINGS) {
            if (ledBean == null) return null;
            selectedId = ledBean.getEffectId();
            LedSettingsBean ledSettingsBean = ProductUtil.getCacheLedSettings(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid());
            valueBeanList = ledSettingsBean == null ? null : ledSettingsBean.getEffects();
        } else {
            if (settingsItem == null) return null;
            selectedId = settingsItem.getValueId();
            valueBeanList = ProductUtil.getCacheList(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(), attrType);
        }
        if (valueBeanList == null) return null;
        for (ValueBean value : valueBeanList) {
            boolean isSelected = selectedId == value.getValue();
            FunctionBean functionBean = new FunctionBean(value.getValue(), ProductUtil.getValue(value), isSelected);
            list.add(functionBean);
        }
        return list;
    }

    private void updateActionTv(String text) {
        if (!isAdded() || isDetached()) return;
        if (tvAction != null) {
            if (text != null) {
                tvAction.setVisibility(View.VISIBLE);
                tvAction.setText(text);
            } else {
                tvAction.setVisibility(View.GONE);
            }
        }
    }

    private void updateFuncList(List<FunctionBean> list) {
        if (!isAdded() || isDetached()) return;
        if (list == null) list = new ArrayList<>();
        if (list.isEmpty()) {
            if (attrType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS)
//                viewLine.setVisibility(View.GONE);
                rvFuncList.setVisibility(View.GONE);
            return;
        } else {
            if (attrType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS)
//                viewLine.setVisibility(View.VISIBLE);
                rvFuncList.setVisibility(View.VISIBLE);
        }
        if (mAdapter == null) {
            mAdapter = new FuncSettingsAdapter();
            mAdapter.setOnItemClickListener(mOnItemClickListener);
        }
        mAdapter.setNewInstance(list);
        rvFuncList.setAdapter(mAdapter);
    }

    private void showVoiceModeListDialog(List<VoiceMode> list, byte[] selectMode) {
        if (isDetached() || !isAdded()) return;
        if (null == mModeListDialog) {
            mModeListDialog = new VoiceModeListDialog();
            mModeListDialog.setModes(list);
            mModeListDialog.setSelectModes(selectMode);
            mModeListDialog.setOnVoiceModeListListener(modes -> {
                dismissVoiceModeListDialog();
                mPresenter.setSelectVoiceModeList(modes);
            });
        } else {
            if (list != null) {
                mModeListDialog.setModes(list);
            }
            if (selectMode != null) {
                mModeListDialog.setSelectModes(selectMode);
            }
        }
        if (!mModeListDialog.isShow()) {
            mModeListDialog.show(getChildFragmentManager(), VoiceModeListDialog.class.getSimpleName());
        }
    }

    private void dismissVoiceModeListDialog() {
        if (isDetached() || !isAdded()) return;
        if (null != mModeListDialog) {
            if (mModeListDialog.isShow()) {
                mModeListDialog.dismiss();
            }
            mModeListDialog = null;
        }
    }

    private void updateSelectedVoiceModes(byte[] array) {
        if (isDetached() || !isAdded()) return;
        if (null != mModeListDialog && mModeListDialog.isShow()) {
            mModeListDialog.setSelectModes(array);
        }
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
            if (mAdapter != null) {
                FunctionBean item = mAdapter.getItem(position);
                if (item == null || item.isSelected()) return;
                byte[] paramData = null;
                switch (attrType) {
                    case AttrAndFunCode.ADV_TYPE_KEY_SETTINGS: {
                        if (mKeyBean == null) return;
                        paramData = new byte[]{(byte) mKeyBean.getKeyId(),
                                (byte) mKeyBean.getActionId(), (byte) item.getFuncId()};
                        break;
                    }
                    case AttrAndFunCode.ADV_TYPE_LED_SETTINGS:
                        if (mLedBean == null) return;
                        paramData = new byte[]{(byte) mLedBean.getSceneId(), (byte) item.getFuncId()};
                        break;
                    case AttrAndFunCode.ADV_TYPE_WORK_MODE:
                    case AttrAndFunCode.ADV_TYPE_MIC_CHANNEL_SETTINGS:
                    case AttrAndFunCode.ADV_TYPE_IN_EAR_CHECK: {
                        paramData = new byte[]{(byte) item.getFuncId()};
                        break;
                    }
                }
                if (paramData != null) {
                    mPresenter.modifyHeadsetFunctions(position, attrType, paramData);
                }
            }
        }
    };
}
