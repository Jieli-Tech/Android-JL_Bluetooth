package com.jieli.btsmart.ui.settings.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.FuncSettingsAdapter;
import com.jieli.btsmart.data.model.settings.FunctionBean;
import com.jieli.btsmart.data.model.settings.KeyBean;
import com.jieli.btsmart.data.model.settings.LedBean;
import com.jieli.btsmart.data.model.settings.SettingsItem;
import com.jieli.btsmart.databinding.FragmentDevSettingsDetailsBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.widget.VoiceModeListDialog;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.jl_http.bean.KeySettingsBean;
import com.jieli.jl_http.bean.LedSettingsBean;
import com.jieli.jl_http.bean.ValueBean;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备设置详情界面
 */
public class DevSettingsDetailsFragment extends DeviceControlFragment {

    private FragmentDevSettingsDetailsBinding mBinding;
    private DeviceSettingsViewModel mViewModel;
    private FuncSettingsAdapter mAdapter;

    private KeyBean mKeyBean;
    private SettingsItem mSettingsItem;
    private LedBean mLedBean;
    private int funcType = -1;

    private int retryCount = 0;
    private VoiceModeListDialog mModeListDialog;

    public static DevSettingsDetailsFragment newInstance() {
        return new DevSettingsDetailsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentDevSettingsDetailsBinding.inflate(inflater, container, false);
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
        mViewModel = new ViewModelProvider(this, new DeviceSettingsViewModel.Factory(device)).get(DeviceSettingsViewModel.class);
        mKeyBean = bundle.getParcelable(SConstant.KEY_DEV_KEY_BEAN);
        mSettingsItem = bundle.getParcelable(SConstant.KEY_SETTINGS_ITEM);
        mLedBean = bundle.getParcelable(SConstant.KEY_DEV_LED_BEAN);
        funcType = getFuncType();
        if (funcType == -1) {
            finish();
            return;
        }
        initUI();
        addObserver();
//        updateKeyFuncList(funcType, mKeyBean, mSettingsItem, mLedBean);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissVoiceModeListDialog();
        funcType = -1;
        mKeyBean = null;
        mSettingsItem = null;
        mLedBean = null;
    }

    private void initUI() {
        mAdapter = new FuncSettingsAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            final FunctionBean item = mAdapter.getItem(position);
            if (item == null || item.isSelected()) return;
            byte[] paramData = null;
            switch (funcType) {
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
            if (null == paramData) return;
            mViewModel.modifyDeviceFunction(position, funcType, paramData);
        });
        mBinding.rvFuncList.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.rvFuncList.setAdapter(mAdapter);
        mBinding.llAnc.setOnClickListener(v -> {
            if (mKeyBean.getFuncId() == AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE) {
                showVoiceModeListDialog(mViewModel.getVoiceModeList(), mViewModel.getSwitchVoiceModes());
            } else {
                byte[] paramData = new byte[]{(byte) mKeyBean.getKeyId(), (byte) mKeyBean.getActionId(), (byte) AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE};
                mViewModel.modifyDeviceFunction(-1, funcType, paramData);
            }
        });
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
        mViewModel.mDeviceSettingInfoMLD.observe(getViewLifecycleOwner(), this::updateDeviceSettings);
        mViewModel.mOpResultMLD.observe(getViewLifecycleOwner(), opResult -> {
            if (opResult.getOp() != DeviceSettingsViewModel.OP_CHANGE_DEVICE_FUNCTION){
                if(!opResult.isSuccess()){
                   JL_Log.w(TAG, "mOpResultMLD", CommonUtil.formatString("op : %d, code : %s, message : %s",
                           opResult.getOp(), CommonUtil.formatInt(opResult.getCode()), opResult.getMessage()));
                }
                return;
            }
            if (opResult.isSuccess()) {
                handleSetFunctionSuccess((int) opResult.getData());
                return;
            }
            showTips(opResult.getMessage());
        });
    }

    private int getFuncType() {
        int func = mKeyBean == null ? -1 : mKeyBean.getAttrType();
        if (func == -1) {
            func = mSettingsItem == null ? -1 : mSettingsItem.getType();
        }
        if (func == -1) {
            func = mLedBean == null ? -1 : mLedBean.getAttrType();
        }
        return func;
    }

    private void updateTopBar(String title) {
        if (!(requireActivity() instanceof CommonActivity) || null == title) return;
        ((CommonActivity) requireActivity()).updateTopBar(title, R.drawable.ic_back_black, v -> finish(), 0, null);
    }

    private void updateDeviceSettings(ADVInfoResponse advInfo) {
        if (advInfo == null || isInvalid()) return;
        switch (funcType) {
            case AttrAndFunCode.ADV_TYPE_KEY_SETTINGS: {
                if (null == mKeyBean) break;
                final List<ADVInfoResponse.KeySettings> list = advInfo.getKeySettingsList();
                if (null == list || list.isEmpty()) break;
                for (ADVInfoResponse.KeySettings settings : list) {
                    if (settings.getKeyNum() == mKeyBean.getKeyId() && settings.getAction() == mKeyBean.getActionId()) {
                        mKeyBean.setFuncId(settings.getFunction());
                        break;
                    }
                }
                break;
            }
            case AttrAndFunCode.ADV_TYPE_LED_SETTINGS: {
                if (null == mLedBean) break;
                final List<ADVInfoResponse.LedSettings> list = advInfo.getLedSettingsList();
                if (null == list || list.isEmpty()) break;
                for (ADVInfoResponse.LedSettings settings : advInfo.getLedSettingsList()) {
                    if (settings.getScene() == mLedBean.getSceneId()) {
                        mLedBean.setEffectId(settings.getEffect());
                        break;
                    }
                }
                break;
            }
            case AttrAndFunCode.ADV_TYPE_WORK_MODE: {
                if (null == mSettingsItem) break;
                mSettingsItem.setValueId(advInfo.getWorkModel());
                break;
            }
            case AttrAndFunCode.ADV_TYPE_MIC_CHANNEL_SETTINGS: {
                if (null == mSettingsItem) break;
                mSettingsItem.setValueId(advInfo.getMicChannel());
                break;
            }
            case AttrAndFunCode.ADV_TYPE_IN_EAR_CHECK: {
                if (null == mSettingsItem) break;
                mSettingsItem.setValueId(advInfo.getInEarSettings());
                break;
            }
        }
        updateKeyFuncList(funcType, mKeyBean, mSettingsItem, mLedBean);
        updateSelectedVoiceModes(advInfo.getModes());
    }

    private void updateKeyFuncList(int attrType, KeyBean keyBean, SettingsItem item, LedBean ledBean) {
        if (!isAdded() || isDetached()) return;
        if (attrType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS) {
            updateTopBar(keyBean == null ? null : keyBean.getKeyName());
            updateActionTv(keyBean == null ? null : keyBean.getAction());
            updateANCUI(keyBean);
        } else if (attrType == AttrAndFunCode.ADV_TYPE_LED_SETTINGS) {
            updateTopBar(ledBean == null ? null : ledBean.getScene());
            updateActionTv(null);
        } else {
            updateTopBar(item == null ? null : item.getName());
            updateActionTv(null);
        }
        updateFuncList(getKeyFuncListFromKeyBean(attrType, keyBean, item, ledBean));
    }

    private void updateANCUI(KeyBean keyBean) {
        boolean isSupportAnc = mViewModel.isSupportAnc();
        if (!isSupportAnc) {
            UIHelper.gone(mBinding.llAnc);
            return;
        }
        UIHelper.show(mBinding.llAnc);
        boolean enableAnc = keyBean != null && keyBean.getFuncId() == AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE;
        mBinding.tvAncState.setText(enableAnc ? getString(R.string.enable) : "");
        byte[] modes = mViewModel.getSwitchVoiceModes();
        JL_Log.d(TAG, "updateANCUI", "modes : " + CHexConver.byte2HexStr(modes));
        if (modes == null || modes.length == 0) {
            mViewModel.syncSwitchVoiceModes();
        }
    }


    private List<FunctionBean> getKeyFuncListFromKeyBean(int attrType, KeyBean keyBean, SettingsItem settingsItem, LedBean ledBean) {
        ADVInfoResponse advInfo = mViewModel.getADVInfo(mViewModel.mDevice);
        if (advInfo == null) {
            if (retryCount < 3) {
                mViewModel.syncDeviceSettings();
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
        if (isInvalid()) return;
        if (text != null) {
            UIHelper.show(mBinding.tvAction);
            mBinding.tvAction.setText(text);
            return;
        }
        UIHelper.gone(mBinding.tvAction);
    }

    private void updateFuncList(List<FunctionBean> list) {
        if (isInvalid()) return;
        if (list == null) list = new ArrayList<>();
        if (list.isEmpty()) {
            if (funcType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS) {
                UIHelper.gone(mBinding.rvFuncList);
            }
            return;
        } else {
            if (funcType == AttrAndFunCode.ADV_TYPE_KEY_SETTINGS) {
                UIHelper.show(mBinding.rvFuncList);
            }
        }
        mAdapter.setList(list);
    }

    private void showVoiceModeListDialog(List<VoiceMode> list, byte[] selectMode) {
        if (isInvalid()) return;
        if (null == mModeListDialog) {
            mModeListDialog = new VoiceModeListDialog();
            mModeListDialog.setModes(list);
            mModeListDialog.setSelectModes(selectMode);
            mModeListDialog.setOnVoiceModeListListener(modes -> {
                dismissVoiceModeListDialog();
                mViewModel.changeSwitchVoiceModes(modes);
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
        if (isInvalid()) return;
        if (null != mModeListDialog) {
            if (mModeListDialog.isShow()) {
                mModeListDialog.dismiss();
            }
            mModeListDialog = null;
        }
    }

    private void updateSelectedVoiceModes(byte[] array) {
        if (isInvalid() || null == array || array.length == 0) return;
        if (null != mModeListDialog && mModeListDialog.isShow()) {
            mModeListDialog.setSelectModes(array);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleSetFunctionSuccess(int position) {
        if (isInvalid()) return;
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
        switch (funcType) {
            case AttrAndFunCode.ADV_TYPE_KEY_SETTINGS: {
                if (null == mKeyBean) break;
                mKeyBean.setFuncId(funcId);
                if (mViewModel.isSupportAnc()) {
                    if (-1 == position) {
                        mKeyBean.setFuncId(AttrAndFunCode.KEY_FUNC_ID_SWITCH_ANC_MODE);
                        showVoiceModeListDialog(mViewModel.getVoiceModeList(), mViewModel.getSwitchVoiceModes());
                    }
                    updateANCUI(mKeyBean);
                }
                break;
            }
            case AttrAndFunCode.ADV_TYPE_LED_SETTINGS: {
                if (mLedBean != null) mLedBean.setEffectId(funcId);
                break;
            }
            default: {
                if (mSettingsItem != null) mSettingsItem.setValueId(funcId);
                break;
            }
        }
    }
}
