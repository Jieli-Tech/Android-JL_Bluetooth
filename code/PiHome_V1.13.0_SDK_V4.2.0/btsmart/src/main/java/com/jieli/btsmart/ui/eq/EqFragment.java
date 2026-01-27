package com.jieli.btsmart.ui.eq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.voice.VolumeInfo;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.eq.VolumeCtrl;
import com.jieli.btsmart.databinding.FragmentEqBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.widget.RotatingView;
import com.jieli.btsmart.util.EqCacheUtil;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * EqFragment
 * @author zqjasonZhong
 * @since 2025/5/7
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 调节EQ页面
 */
public class EqFragment extends BaseFragment {
    private final int MSG_NO_SUPPORT_HIGH_AND_BASS = -13;

    private static final long UPDATE_INTERVAL = 200L;

    private static final int MSG_SET_VOLUME = 0x3232;
    private static final int MSG_SET_HIGH_AND_BASS = 0x3233;
    private static final int MSG_SET_EQ_INFO = 0x3234;

    private FragmentEqBinding mBinding;
    private EqViewModel mViewModel;
    private EqSeekBarAdapter mEqSeekBarAdapter;

    private VolumeReceiver mReceiver;

    private final Handler uiHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_SET_VOLUME: {
                    mViewModel.setVolume(requireContext(), msg.arg1);
                    break;
                }
                case MSG_SET_HIGH_AND_BASS: {
                    mViewModel.setHighAndBass(msg.arg1, msg.arg2);
                    break;
                }
                case MSG_SET_EQ_INFO: {
                    if (!(msg.obj instanceof EqInfo)) return false;
                    EqInfo eqInfo = (EqInfo) msg.obj;
                    mViewModel.setEqInfo(eqInfo);
                    break;
                }
            }
            return true;
        }
    });

    public static EqFragment newInstance() {
        return new EqFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentEqBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(EqViewModel.class);
        initUI();
        addObserver();
        registerReceiver();
        mViewModel.syncEqInfo();
    }

    @Override
    public void onDestroyView() {
        unregisterReceiver();
        super.onDestroyView();
    }

    private void initUI() {
        mBinding.rotatMain.setOnValueChangeListener((view, value, end) -> {
            if (end) {
                uiHandler.removeMessages(MSG_SET_VOLUME);
                uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_SET_VOLUME, value, 0), UPDATE_INTERVAL);
            }
        });
        mBinding.rotatBass.setOnValueChangeListener((view, value, end) -> {
            if (end) {
                uiHandler.removeMessages(MSG_SET_HIGH_AND_BASS);
                uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_SET_HIGH_AND_BASS,
                        mBinding.rotatHeight.getValue(), value), UPDATE_INTERVAL);
            }
        });
        mBinding.rotatHeight.setOnValueChangeListener((view, value, end) -> {
            if (end) {
                uiHandler.removeMessages(MSG_SET_HIGH_AND_BASS);
                uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_SET_HIGH_AND_BASS,
                        value, mBinding.rotatBass.getValue()), UPDATE_INTERVAL);
            }
        });

        mBinding.btnEqMode.setOnClickListener(v -> new EqModeDialog.Builder(eqInfo -> {
            uiHandler.removeMessages(MSG_SET_EQ_INFO);
            uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_SET_EQ_INFO, eqInfo), UPDATE_INTERVAL);
        }).build().show(getChildFragmentManager(), EqModeDialog.class.getSimpleName()));
        mBinding.btnEqReset.setOnClickListener(v -> {
            List<EqInfo> list = EqCacheUtil.getPresetEqInfo().getEqInfos();
            EqInfo eqInfo = list.get(0).copy();
            eqInfo.setMode(EqInfo.MODE_CUSTOM);
            eqInfo.setValue(new byte[eqInfo.getValue().length]);
            uiHandler.removeMessages(MSG_SET_EQ_INFO);
            uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_SET_EQ_INFO, eqInfo), UPDATE_INTERVAL);
        });
        mBinding.btnEqAdvancedSetting.setOnClickListener(v -> {
            if (!mViewModel.isDevConnected()) {
                showTips(getString(R.string.first_connect_device));
                return;
            }
            CommonActivity.startCommonActivity(requireActivity(), EqAdvancedSetFragment.class.getCanonicalName());
        });

        mBinding.rvVsbs.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        mEqSeekBarAdapter = new EqSeekBarAdapter(new ArrayList<>(), (index, eqInfo, end) -> {
            mBinding.wvFreq.updateData(index, eqInfo.getValue()[index]);
            if (end) {
                eqInfo.setMode(EqInfo.MODE_CUSTOM);
                uiHandler.removeMessages(MSG_SET_EQ_INFO);
                uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_SET_EQ_INFO, eqInfo), UPDATE_INTERVAL);
            }
        });
        mBinding.rvVsbs.setAdapter(mEqSeekBarAdapter);
        mBinding.rotatMain.setValue(0, 25, 0);
    }

    private void addObserver() {
        mViewModel.eqInfoMLD.observe(getViewLifecycleOwner(), eqInfo -> {
            //有拖动的时候不更新eq info
            if (!mEqSeekBarAdapter.hasHoverView()) {
                updateEqInfo(eqInfo);
            }
        });
        mViewModel.isBanEqMLD.observe(getViewLifecycleOwner(), this::updateEqView);
        mViewModel.volumeInfoMLD.observe(getViewLifecycleOwner(), this::updateMainVolume);
        mViewModel.volumeCtrlMLD.observe(getViewLifecycleOwner(), this::updateHighAndBass);
    }

    private void updateEqInfo(EqInfo eqInfo) {
        if (null == eqInfo || isInvalid()) return;
        JL_Log.d(TAG, "updateEqInfo", "" + eqInfo);
        mBinding.tvEqModeSelectName.setText(getResources().getStringArray(R.array.eq_mode_list)[eqInfo.getMode()]);
        mBinding.wvFreq.setData(ValueUtil.bytes2ints(eqInfo.getValue(), eqInfo.getValue().length));
        mBinding.wvFreq.setFreqs(eqInfo.getFreqs());
        mEqSeekBarAdapter.updateSeekBar(eqInfo.copy());

        boolean enableResetBtn = isEnableResetBtn(eqInfo);
        mBinding.btnEqReset.setSelected(enableResetBtn);
        mBinding.btnEqReset.setClickable(enableResetBtn);
    }

    private boolean isEnableResetBtn(EqInfo eqInfo) {
        boolean isBan = mViewModel.isBanEq();
        boolean enableResetBtn = eqInfo.getMode() == EqInfo.MODE_CUSTOM && !isBan;
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
        return enableResetBtn;
    }

    private void updateMainVolume(VolumeInfo volumeInfo) {
        if (null == volumeInfo) return;
        boolean isBan = mViewModel.isBanEq();
        boolean isPressed = mBinding.rotatMain.isPressed();
        JL_Log.d(TAG, "updateMainVolume", "isPressed : " + isPressed + ", isBanEq : " + isBan + ", \n" + volumeInfo);
        if (isPressed) return;
        int max = volumeInfo.getMaxVol() == 0 ? 15 : volumeInfo.getMaxVol();
        int current = isBan ? 0 : volumeInfo.getVolume();
        mBinding.rotatMain.setValue(0, max, current);
    }

    private void changeRotateViewStyle(RotatingView view, boolean isOpen) {
        if (null == view) return;
        view.setContentStartColor(isOpen ? R.color.color_rotating_view_start : R.color.gray_CECECE);
        view.setContentEndColor(isOpen ? R.color.color_rotating_view_end : R.color.gray_CECECE);
        view.setContentTextColor(isOpen ? R.color.black_242424 : R.color.gray_CECECE);
        view.setIndicatorImage(isOpen ? R.drawable.ic_rotatview_indicator_sup :
                R.drawable.ic_rotatview_indicator_nol);
        view.setClickable(isOpen);
        view.invalidate();
    }

    private void resetHighAndBassRotateView() {
        mBinding.rotatHeight.setValue(MSG_NO_SUPPORT_HIGH_AND_BASS);
        mBinding.rotatBass.setValue(MSG_NO_SUPPORT_HIGH_AND_BASS);
        changeRotateViewStyle(mBinding.rotatHeight, false);
        changeRotateViewStyle(mBinding.rotatBass, false);
    }

    private void updateHighAndBass(VolumeCtrl volumeCtrl) {
        if (isInvalid() || null == volumeCtrl) return;
        boolean isBanEq = mViewModel.isBanEq();
        int high = volumeCtrl.getHigh();
        int bass = volumeCtrl.getBass();
        JL_Log.d(TAG, "updateHighAndBass", "isBanEq = " + isBanEq + ", " + volumeCtrl);
        if (high != MSG_NO_SUPPORT_HIGH_AND_BASS) {
            changeRotateViewStyle(mBinding.rotatHeight, !isBanEq);
            mBinding.rotatHeight.setValue(isBanEq ? MSG_NO_SUPPORT_HIGH_AND_BASS : high);
        }
        if (bass != MSG_NO_SUPPORT_HIGH_AND_BASS) {
            changeRotateViewStyle(mBinding.rotatBass, !isBanEq);
            mBinding.rotatBass.setValue(isBanEq ? MSG_NO_SUPPORT_HIGH_AND_BASS : bass);
        }
    }

    private void updateEqView(boolean ban) {
        resetHighAndBassRotateView();
        boolean enableHighAnBass = !ban && mViewModel.volumeCtrlMLD.getValue() != null;
        JL_Log.d(TAG, "updateEqView", "ban = " + ban + ", enableHighAnBass = " + enableHighAnBass);
        changeRotateViewStyle(mBinding.rotatHeight, enableHighAnBass);
        changeRotateViewStyle(mBinding.rotatBass, enableHighAnBass);
        changeRotateViewStyle(mBinding.rotatMain, !ban);
        final EqInfo eqInfo = mViewModel.getEqInfo();
        boolean isCustomMode = eqInfo != null && eqInfo.getMode() == EqInfo.MODE_CUSTOM;
        mBinding.btnEqMode.setClickable(!ban);
        mBinding.btnEqReset.setClickable(!ban && isCustomMode);
        mBinding.btnEqAdvancedSetting.setClickable(!ban);

        mBinding.tvEqModeSelectName.setTextColor(ContextCompat.getColor(requireContext(),
                ban ? R.color.gray_959595 : R.color.black_242424));
        mBinding.tvEqModeSelectName.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                ban ? R.drawable.ic_eq_icon_up_disable : R.drawable.ic_eq_icon_up, 0);
        mBinding.btnEqAdvancedSetting.setTextColor(ContextCompat.getColor(requireContext(),
                ban ? R.color.gray_959595 : R.color.black_242424));
        mBinding.btnEqReset.setSelected(!ban);
        mEqSeekBarAdapter.setBan(ban);
        mBinding.wvFreq.setEnabled(!ban);
    }

    private void registerReceiver() {
        if (null != mReceiver) return;
        mReceiver = new VolumeReceiver();
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.media.STREAM_DEVICES_CHANGED_ACTION");//监听手机输出设备变化
        requireContext().registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
        if (null == mReceiver) return;
        requireContext().unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    private class VolumeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (null == action) return;
            final int type = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
            JL_Log.d(TAG, "VolumeReceiver", "type : " + type);
            //未连接设备或非音乐类型 忽略
            if (!mViewModel.isDevConnected() || type != AudioManager.STREAM_MUSIC) {
                return;
            }
            if (mViewModel.isSupportVolumeSync()) {
                mViewModel.syncVolumeInfo(requireContext());
            }
        }
    }
}
