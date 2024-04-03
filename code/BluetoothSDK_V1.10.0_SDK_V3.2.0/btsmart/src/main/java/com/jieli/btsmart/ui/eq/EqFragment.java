package com.jieli.btsmart.ui.eq;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.bluetooth.bean.device.voice.VolumeInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.EqSeekBarAdapter;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.widget.EqWaveView;
import com.jieli.btsmart.ui.widget.RotatingView;
import com.jieli.btsmart.util.EqCacheUtil;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.List;


public class EqFragment extends Jl_BaseFragment implements RotatingView.OnValueChangeListener {
    private static final boolean SEND_CMD_REALTIME = false;
    private RotatingView rotatBass;
    private RotatingView rotatMain;
    private RotatingView rotatHigh;
    private EqWaveView wvFreq;
    private TextView tvEqModeSelectName;
    private Button btnEqReset;
    private Button btnEqAdvancedSet;
    private Button btnEqMode;
    private EqSeekBarAdapter mEqSeekBarAdapter;

    private final RCSPController mRCSPController = RCSPController.getInstance();

    private final int MSG_NO_SUPPORT_HIGH_AND_BASS = -13;

    private EqInfo mEqInfo = EqCacheUtil.getCurrentCacheEqInfo();
    private long sendVolTime;//记录音量命令发送的最后时间


    public static EqFragment newInstance() {
        return new EqFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_eq, container, false);
        rotatBass = view.findViewById(R.id.rotat_bass);
        rotatMain = view.findViewById(R.id.rotat_main);
        rotatHigh = view.findViewById(R.id.rotat_height);
        rotatMain.setOnValueChangeListener(this);
        rotatBass.setOnValueChangeListener(this);
        rotatHigh.setOnValueChangeListener(this);

        wvFreq = view.findViewById(R.id.wv_freq);

        tvEqModeSelectName = view.findViewById(R.id.tv_eq_mode_select_name);
        btnEqReset = view.findViewById(R.id.btn_eq_reset);
        btnEqAdvancedSet = view.findViewById(R.id.btn_eq_advanced_setting);
        btnEqMode = view.findViewById(R.id.btn_eq_mode);
        btnEqMode.setOnClickListener(mOnClickListener);
        btnEqReset.setOnClickListener(mOnClickListener);
        btnEqAdvancedSet.setOnClickListener(mOnClickListener);

        RecyclerView rvVsbs = view.findViewById(R.id.rv_vsbs);
        mEqSeekBarAdapter = new EqSeekBarAdapter(new ArrayList<>(), (index, eqInfo, end) -> {
            wvFreq.updateData(index, eqInfo.getValue()[index]);
            if (end || SEND_CMD_REALTIME) {
                eqInfo.setMode(6);
                setEqInfo(eqInfo);
            }
        });
        rvVsbs.setAdapter(mEqSeekBarAdapter);
        rvVsbs.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        updateEqInfo(mEqInfo);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
        requestEqInfo();
        rotatMain.setValue(0, 25, 0);
        if (mRCSPController.isDeviceConnected()) {
            if (mRCSPController.getDeviceInfo().isSupportVolumeSync()) {
                JL_Log.w(TAG, "device is connected before create eq ui  sync is true");
                updateVolumeUiFromPhone();
            } else {
                JL_Log.w(TAG, "device is connected before create eq ui  sync is false");
                updateVolumeUiFromDevice();
            }
        } else {
            disableEqViewIfBan(false);
        }
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");//监听手机输出设备变化
        requireContext().registerReceiver(mVolumeReceiver, filter);

    }

    @Override
    public void onDestroyView() {
        JL_Log.d(TAG, "onDestroyView");
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        requireContext().unregisterReceiver(mVolumeReceiver);
        super.onDestroyView();
    }


    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnEqMode) {
                EqModeDialog eqModeDialog = EqModeDialog.newInstance(eqInfo -> setEqInfo(eqInfo));
                eqModeDialog.show(getChildFragmentManager(), EqModeDialog.class.getCanonicalName());
            } else if (v == btnEqReset) {
                List<EqInfo> list = EqCacheUtil.getPresetEqInfo().getEqInfos();
                EqInfo eqInfo = list.get(0).copy();
                eqInfo.setMode(6);
                eqInfo.setValue(new byte[eqInfo.getValue().length]);
                setEqInfo(eqInfo);
            } else if (v == btnEqAdvancedSet) {
                if (!mRCSPController.isDeviceConnected()) {
                    ToastUtil.showToastShort(getString(R.string.first_connect_device));
                    return;
                }
                CommonActivity.startCommonActivity(getActivity(), EqAdvancedSetFragment.class.getCanonicalName());
            }
        }
    };


    //旋转按钮调节
    @Override
    public void change(RotatingView view, int value, boolean end) {
        if (view == rotatMain) {
            if (getContext() != null) {
                setVolumeByRotate(value);
            }
        } else if (view == rotatBass || view == rotatHigh) {
            if (end || SEND_CMD_REALTIME) {
                mRCSPController.setHighAndBassValue(mRCSPController.getUsingDevice(), rotatHigh.getValue(), rotatBass.getValue(), null);
            }
        }
    }


    private void setEqInfo(EqInfo eqInfo) {
        if (mRCSPController.isDeviceConnected()) {
            mRCSPController.configEqInfo(mRCSPController.getUsingDevice(), eqInfo, null);
        } else {
            //将自定义的的调节值保存到预设
            if (eqInfo.getMode() == 6) {
                EqPresetInfo eqPresetInfo = EqCacheUtil.getPresetEqInfo();
                eqPresetInfo.getEqInfos().get(6).setValue(eqInfo.getValue());
                EqCacheUtil.savePresetEqInfo(eqPresetInfo);
            }
            EqCacheUtil.saveEqValue(eqInfo);
        }
        updateEqInfo(eqInfo);
    }


    private void setVolumeByRotate(int value) {
        if (!mRCSPController.isDeviceConnected()) {
            return;
        }
        if (mRCSPController.getDeviceInfo().isSupportVolumeSync()) {
            AudioManager audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
            JL_Log.d(TAG, "set Phone Volume By Rotate \t");
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        } else {
            if (value != mRCSPController.getDeviceInfo().getVolume()) {
                sendVolTime = System.currentTimeMillis();
                BTRcspHelper.adjustVolume(mRCSPController, requireContext(), value, new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) {

                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        ToastUtil.showToastShort(R.string.settings_failed);
                        if (mRCSPController.isDeviceConnected(device)) {
                            updateVolumeUiFromDevice();
                        }
                    }
                });
            }
        }
    }

    private void updateEqInfo(EqInfo eqInfo) {
        JL_Log.d(TAG, "set updateEqInfo -->" + eqInfo.toString());
        this.mEqInfo = eqInfo;
        tvEqModeSelectName.setText(getResources().getStringArray(R.array.eq_mode_list)[mEqInfo.getMode()]);
        wvFreq.setData(ValueUtil.bytes2ints(eqInfo.getValue(), eqInfo.getValue().length));
        wvFreq.setFreqs(eqInfo.getFreqs());
        mEqSeekBarAdapter.updateSeekBar(eqInfo.copy());

        boolean isBan = mRCSPController.getDeviceInfo() != null && mRCSPController.getDeviceInfo().isBanEq();

        boolean enableResetBtn = mEqInfo.getMode() == 6 && !isBan;
        //判断是否全部值都是0，如果是则禁止点击
        if (enableResetBtn) {
            boolean allZero = true;
            for (int v : eqInfo.getValue()) {
                if (v != 0) {
                    allZero = false;
                    break;
                }
            }
            enableResetBtn = !allZero;
        }
        btnEqReset.setSelected(enableResetBtn);
        btnEqReset.setClickable(enableResetBtn);
    }


    private void requestEqInfo() {
        if (mRCSPController.isDeviceConnected()) {
            JL_Log.d("sen", "requestEqInfo");
            mRCSPController.getEqInfo(mRCSPController.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
                @Override
                public void onSuccess(BluetoothDevice device, Boolean message) {
                    mRCSPController.getHighAndBassValue(device, new OnRcspActionCallback<Boolean>() {
                        @Override
                        public void onSuccess(BluetoothDevice device, Boolean message) {
                            //如果是音量同步的话收到eq变化时同步一下音量
                            if (mRCSPController.isDeviceConnected(device) && mRCSPController.getDeviceInfo(device).isSupportVolumeSync()) {
                                updateVolumeUiFromPhone();
                            }
                        }

                        @Override
                        public void onError(BluetoothDevice device, BaseError error) {

                        }
                    });
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {

                }
            });
        }
    }


    //更新手机音量
    private void updateVolumeUiFromPhone() {
        AudioManager audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        JL_Log.d(TAG, "updateVolumeUiFromPhone\tcurrent=" + current + "\tmax=" + max);
        rotatMain.setValue(0, max, isBan() ? 0 : current);
    }

    //更新设备音量
    private void updateVolumeUiFromDevice() {
        JL_Log.d(TAG, "updateVolumeUiFromDevice  ");
        if (!mRCSPController.isDeviceConnected() || mRCSPController.getDeviceInfo().isSupportVolumeSync()) {
            JL_Log.d(TAG, "updateVolumeUiFromDevice : device is disconnect or device not support volume sync. ");
            return;
        }
        int current = mRCSPController.getDeviceInfo().getVolume();
        int max = mRCSPController.getDeviceInfo().getMaxVol();
        JL_Log.d(TAG, "updateVolumeUiFromDevice  : current = " + current + ", max = " + max);
        rotatMain.setValue(0, max, isBan() ? 0 : current);
    }


    private boolean isBan() {
        return mRCSPController.isDeviceConnected() && mRCSPController.getDeviceInfo().isBanEq();
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {

        @Override
        public void onEqChange(BluetoothDevice device, EqInfo eqInfo) {
            JL_Log.d(TAG, "eq  change -->" + eqInfo.toString());
            //有拖动的时候不更新eq info
            if (!mEqSeekBarAdapter.hasHoverView()) {
                updateEqInfo(eqInfo);
            }
        }

        @Override
        public void onVolumeChange(BluetoothDevice device, VolumeInfo volume) {
            if (!isAdded() || isDetached()) return;
            JL_Log.d(TAG, "onVolumeChange--->" + volume + "\tvolume view is press-->" + rotatMain.isPressed());
            if (System.currentTimeMillis() - sendVolTime > 500 && !rotatMain.isPressed()) {
                if (mRCSPController.isDeviceConnected() && !mRCSPController.getDeviceInfo().isSupportVolumeSync()) {
                    updateVolumeUiFromDevice();
                }
            }
        }


        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            if (status == StateCode.CONNECTION_OK) {
                handleDeviceConnected();
            } else if (status == StateCode.CONNECTION_DISCONNECT && !mRCSPController.isDeviceConnected(device)) {
                resetHighAndBassRotateView();
                disableEqViewIfBan(isBan());
            }
        }


        @Override
        public void onHighAndBassChange(BluetoothDevice device, int high, int bass) {
            if (!isAdded() || isDetached()) return;
            if (high != MSG_NO_SUPPORT_HIGH_AND_BASS) {
                changeRotateViewStyle(rotatHigh, !isBan());
                rotatHigh.setValue(isBan() ? MSG_NO_SUPPORT_HIGH_AND_BASS : high);
            }
            if (bass != MSG_NO_SUPPORT_HIGH_AND_BASS) {
                changeRotateViewStyle(rotatBass, !isBan());
                rotatBass.setValue(isBan() ? MSG_NO_SUPPORT_HIGH_AND_BASS : bass);
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            resetHighAndBassRotateView();
            handleDeviceConnected();
        }
    };

    private void changeRotateViewStyle(RotatingView view, boolean isOpen) {
        if (isOpen) {
            view.setContentStartColor(R.color.color_rotating_view_start);
            view.setContentEndColor(R.color.color_rotating_view_end);
            view.setContentTextColor(R.color.black_242424);
            view.setIndicatorImage(R.drawable.ic_rotatview_indicator_sup);
            view.setClickable(true);
        } else {
            view.setContentStartColor(R.color.gray_CECECE);
            view.setContentEndColor(R.color.gray_CECECE);
            view.setContentTextColor(R.color.gray_CECECE);
            view.setIndicatorImage(R.drawable.ic_rotatview_indicator_nol);
            view.setClickable(false);
        }
        view.invalidate();
    }


    private void disableEqViewIfBan(boolean ban) {
        changeRotateViewStyle(rotatHigh, !ban);
        changeRotateViewStyle(rotatBass, !ban);
        changeRotateViewStyle(rotatMain, !ban);
        btnEqMode.setClickable(!ban);
        btnEqReset.setClickable(!ban && mEqInfo.getMode() == 6);
        btnEqAdvancedSet.setClickable(!ban);

        tvEqModeSelectName.setTextColor(getResources().getColor(ban ? R.color.gray_959595 : R.color.black_242424));
        tvEqModeSelectName.setCompoundDrawablesWithIntrinsicBounds(0, 0, ban ? R.drawable.ic_eq_icon_up_disable : R.drawable.ic_eq_icon_up, 0);
        btnEqAdvancedSet.setTextColor(getResources().getColor(ban ? R.color.gray_959595 : R.color.black_242424));
        btnEqReset.setSelected(!ban);
        mEqSeekBarAdapter.setBan(ban);
        if (ban) {
            setEqInfo(EqCacheUtil.getPresetEqInfo().getEqInfos().get(0));
        }
        wvFreq.setEnabled(!ban);
    }

    private void resetHighAndBassRotateView() {
        rotatHigh.setValue(MSG_NO_SUPPORT_HIGH_AND_BASS);
        rotatBass.setValue(MSG_NO_SUPPORT_HIGH_AND_BASS);
        changeRotateViewStyle(rotatHigh, false);
        changeRotateViewStyle(rotatBass, false);
    }

    private void handleDeviceConnected() {
        disableEqViewIfBan(isBan());
        mRCSPController.getEqInfo(mRCSPController.getUsingDevice(), null);
        if (mRCSPController.isDeviceConnected()) {
            if (!mRCSPController.getDeviceInfo().isSupportVolumeSync()) {
                updateVolumeUiFromDevice();
            } else {
                updateVolumeUiFromPhone();
            }
        }
        requestEqInfo();
    }

    //手机媒体音量广播
    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            JL_Log.d(TAG, "mVolumeReceiver type=\t" + intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1));
            //未连接设备或非音乐类型 忽略
            if (!mRCSPController.isDeviceConnected()
                    || intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != AudioManager.STREAM_MUSIC) {
                return;
            }
            if (intent.getAction() != null) {
                if (mRCSPController.getDeviceInfo().isSupportVolumeSync()) {
                    updateVolumeUiFromPhone();
                }
            }
        }
    };


}
