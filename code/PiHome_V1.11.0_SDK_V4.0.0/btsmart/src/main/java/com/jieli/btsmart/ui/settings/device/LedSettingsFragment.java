package com.jieli.btsmart.ui.settings.device;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.LedSettingsAdapter;
import com.jieli.btsmart.data.model.settings.LedBean;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.component.base.BasePresenter;

import java.util.ArrayList;
import java.util.List;

/**
 * 闪灯设置界面
 */
public class LedSettingsFragment extends DeviceControlFragment implements IDeviceSettingsContract.IDeviceSettingsView {

    private RecyclerView rvFuncList;
    private LedSettingsAdapter mAdapter;
    private CommonActivity mActivity;
    private IDeviceSettingsContract.IDeviceSettingsPresenter mPresenter;

    private int retryCount = 0;
    private BluetoothDevice mUseDevice;

    public static LedSettingsFragment newInstance() {
        return new LedSettingsFragment();
    }

    public LedSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_led_setings, container, false);
        rvFuncList = view.findViewById(R.id.rv_led_settings_func);
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        mPresenter = new DeviceSettingsPresenterImpl(mActivity, this);
        rvFuncList.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mActivity != null) {
            mActivity.updateTopBar(getString(R.string.led_settings), R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);
        }
        Bundle bundle = getArguments();
        if (bundle != null) {
            ADVInfoResponse advInfo = bundle.getParcelable(SConstant.KEY_ADV_INFO);
            if (advInfo != null) {
                updateLedSettingsFromADVInfo(advInfo);
                return;
            }
        }
        if (mPresenter != null) {
            mPresenter.updateDeviceADVInfo();
            mUseDevice = mPresenter.getConnectedDevice();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPresenter != null) {
            mPresenter.destroy();
        }
        mUseDevice = null;
        mActivity = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == SConstant.REQUEST_CODE_DEVICE_SETTINGS) {
            if (mPresenter != null) {
                mPresenter.updateDeviceADVInfo();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBtAdapterStatus(boolean enable) {

    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (BluetoothUtil.deviceEquals(device, mUseDevice)
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
        updateLedSettingsFromADVInfo(advInfo);
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

    @Override
    public void onConfigureSuccess(int position, int result) {

    }

    @Override
    public void onConfigureFailed(BaseError error) {

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

    private void updateLedSettingsFromADVInfo(ADVInfoResponse advInfo) {
        updateLedSettings(getLedListFromADVInfo(advInfo));
    }

    private List<LedBean> getLedListFromADVInfo(ADVInfoResponse advInfo) {
        if (advInfo == null) {
            if (retryCount < 3) {
                if (mPresenter != null)
                    mPresenter.updateDeviceADVInfo();
                retryCount++;
            } else {
                retryCount = 0;
            }
            return null;
        }
        retryCount = 0;
        List<ADVInfoResponse.LedSettings> ledList = advInfo.getLedSettingsList();
        if (ledList == null) return null;
        List<LedBean> list = new ArrayList<>();
        for (int i = 0; i < ledList.size(); i++) {
            ADVInfoResponse.LedSettings ledSettings = ledList.get(i);
            int sceneId = ledSettings.getScene();
            String scene = ProductUtil.getLedSettingsName(getContext(), advInfo.getVid(),
                    advInfo.getUid(), advInfo.getPid(), SConstant.KEY_FIELD_LED_SCENE, sceneId);
            String effect = ProductUtil.getLedSettingsName(getContext(), advInfo.getVid(),
                    advInfo.getUid(), advInfo.getPid(), SConstant.KEY_FIELD_LED_EFFECT, ledSettings.getEffect());
            LedBean ledBean = new LedBean(ledSettings.getScene(), scene, ledSettings.getEffect(), effect);
            if (sceneId == 1 || sceneId == 4 || sceneId == 6) {
                ledBean.setItemType(LedBean.ITEM_TYPE_ONE);
            } else if (sceneId == 3 || sceneId == 5 || sceneId == 7) {
                ledBean.setItemType(LedBean.ITEM_TYPE_THREE);
            } else {
                ledBean.setItemType(LedBean.ITEM_TYPE_TWO);
            }
            if (i == 0 && ledBean.getItemType() != LedBean.ITEM_TYPE_ONE) {
                ledBean.setItemType(LedBean.ITEM_TYPE_ONE);
            }
            list.add(ledBean);
        }
        return list;
    }

    private void updateLedSettings(List<LedBean> list) {
        if (!isAdded() || isDetached()) return;
        if (list == null) list = new ArrayList<>();
        if (list.size() == 0) {
            rvFuncList.setVisibility(View.GONE);
            return;
        } else {
            rvFuncList.setVisibility(View.VISIBLE);
        }
        if (mAdapter == null) {
            mAdapter = new LedSettingsAdapter();
            mAdapter.setOnItemClickListener(mOnItemClickListener);
        }
        mAdapter.setNewInstance(list);
        rvFuncList.setAdapter(mAdapter);
    }

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
            if (mAdapter != null) {
                LedBean item = mAdapter.getItem(position);
                if (item == null) return;
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_DEV_LED_BEAN, item);
                CommonActivity.startCommonActivity(LedSettingsFragment.this, SConstant.REQUEST_CODE_DEVICE_SETTINGS, DevSettingsDetailsFragment.class.getCanonicalName(), bundle);
            }
        }
    };
}
