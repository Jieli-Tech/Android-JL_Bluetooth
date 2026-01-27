package com.jieli.btsmart.ui.light;

import android.bluetooth.BluetoothDevice;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.light.LightControlInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.LightModeAdapter;
import com.jieli.btsmart.data.model.light.LightMode;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.JLShakeItManager;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/30 8:55 AM
 * @desc : 灯光闪烁
 */
public class LightColorTemperatureFragment extends BaseFragment {
    private final static String TAG = LightColorTemperatureFragment.class.getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final JLShakeItManager mShakeItManager = JLShakeItManager.getInstance();

    private RadioGroup rgLightFlash;
    private RadioButton rBtnLightFlashQuick;
    private RadioButton rBtnLightFlashSlow;
    private RadioButton rBtnLightFlashSlower;
    private RadioButton rBtnLightFlashMusic;
    private LightModeAdapter lightModeAdapter;
    private boolean skipFirstTime = false;

    private final static int TWINKLE_MODE = 0;
    private final static int TWINKLE_FREQUENCY = 1;

    public static LightColorTemperatureFragment newInstance() {
        Bundle args = new Bundle();
        LightColorTemperatureFragment fragment = new LightColorTemperatureFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        skipFirstTime = false;
        mShakeItManager.getOnShakeItStartLiveData().observe(this, mode -> {
            if (!skipFirstTime) {
                skipFirstTime = true;
                return;
            }
            if (mode == JLShakeItManager.SHAKE_IT_MODE_CUT_LIGHT_COLOR && isVisible()) {
                int twinkleMode = lightModeAdapter.getSelectedLightMode() + 1;
                if (twinkleMode > 7) twinkleMode = 0;
                if (!mRCSPController.isDeviceConnected()) {
                    lightModeAdapter.setSelected(twinkleMode);
                    return;
                }
                if (mRCSPController.getDeviceInfo().getLightControlInfo().getLightMode() != 1 || !lightModeAdapter.isEqualSelectedLightMode(twinkleMode)) {
                    lightModeAdapter.setSelected(twinkleMode);
                    sendSetTwinkleModeCmd(twinkleMode);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_light_color_temperature, container, false);
        RecyclerView rvLightMode = root.findViewById(R.id.rv_light_mode);
        rgLightFlash = root.findViewById(R.id.rg_light_flash);
        rBtnLightFlashQuick = root.findViewById(R.id.rbtn_light_flash_quick);
        rBtnLightFlashSlow = root.findViewById(R.id.rbtn_light_flash_slow);
        rBtnLightFlashSlower = root.findViewById(R.id.rbtn_light_flash_slower);
        rBtnLightFlashMusic = root.findViewById(R.id.rbtn_light_flash_music);

        rBtnLightFlashQuick.setOnClickListener(mOnClickListener);
        rBtnLightFlashSlow.setOnClickListener(mOnClickListener);
        rBtnLightFlashSlower.setOnClickListener(mOnClickListener);
        rBtnLightFlashMusic.setOnClickListener(mOnClickListener);
        rvLightMode.setLayoutManager(new LinearLayoutManager(getContext()));
        //适配器数据初始化
        String[] modes = AppUtil.getContext().getResources().getStringArray(R.array.light_mode_name);
        TypedArray modeRes = getResources().obtainTypedArray(R.array.light_mode_res);
        List<LightMode> list = new ArrayList<>();
        for (int i = 0; i < modeRes.length() && i < modes.length; i++) {
            LightMode lightMode = new LightMode();
            lightMode.setName(modes[i]);
            lightMode.setRes(modeRes.getResourceId(i, R.drawable.icon_colorful));
            list.add(lightMode);
        }
        modeRes.recycle();
        lightModeAdapter = new LightModeAdapter();
        lightModeAdapter.setNewInstance(list);
        rvLightMode.addItemDecoration(new CommonDecoration(getContext(), OrientationHelper.HORIZONTAL,
                requireContext().getResources().getColor(R.color.gray_eeeeee), ValueUtil.dp2px(requireContext(), 1)));
        rvLightMode.setAdapter(lightModeAdapter);
        lightModeAdapter.setOnItemClickListener((adapter, view1, position) -> {
            int mode = (Integer) view1.getTag();
            if (!mRCSPController.isDeviceConnected()) {
                lightModeAdapter.setSelected(mode);
                return;
            }
            if (mRCSPController.getDeviceInfo().getLightControlInfo().getLightMode() != 1 || !lightModeAdapter.isEqualSelectedLightMode(mode)) {
                lightModeAdapter.setSelected(mode);
                sendSetTwinkleModeCmd(mode);
            }
        });
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (null != deviceInfo) {
            LightControlInfo lightControlInfo = deviceInfo.getLightControlInfo();
            if (null != lightControlInfo) {
                int twinkleMode = lightControlInfo.getTwinkleMode();
                int twinkleFreq = lightControlInfo.getTwinkleFreq();
                lightModeAdapter.setSelected(twinkleMode);
                RadioButton radioButton = (RadioButton) rgLightFlash.getChildAt(twinkleFreq);
                if (radioButton != null) {
                    radioButton.setChecked(true);
                }
                rgLightFlash.setOnCheckedChangeListener(onCheckedChangeListener);
            }
        }
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int freq = -1;
            if (v == rBtnLightFlashQuick) {
                freq = LightControlInfo.TWINKLE_FREQ_FAST;
            } else if (v == rBtnLightFlashSlow) {
                freq = LightControlInfo.TWINKLE_FREQ_SLOW;
            } else if (v == rBtnLightFlashSlower) {
                freq = LightControlInfo.TWINKLE_FREQ_BREATHE;
            } else if (v == rBtnLightFlashMusic) {
                freq = LightControlInfo.TWINKLE_FREQ_MUSIC;
            }
            sendSetTwinkleFreqCmd(freq);
        }
    };

    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onDestroyView();
    }

    private final RadioGroup.OnCheckedChangeListener onCheckedChangeListener = (group, checkedId) -> {
      /*  //todo 发送闪烁频率命令
        int freq = -1;
        switch (checkedId) {
            case R.id.rbtn_light_flash_quick:
                freq = 0;
                break;
            case R.id.rbtn_light_flash_slow:
                freq = 1;
                break;
            case R.id.rbtn_light_flash_slower:
                freq = 2;
                break;
            case R.id.rbtn_light_flash_music:
                freq = 3;
                break;
        }
        sendSetTwinkleFreqCmd(freq);*/
    };

    private void sendLightSceneCmd(int changeType, int value) {
        if (!mRCSPController.isDeviceConnected()) return;
        LightControlInfo lightControlInfo = mRCSPController.getDeviceInfo().getLightControlInfo();
        if (null == lightControlInfo) return;
        if (changeType == TWINKLE_MODE) {
            lightControlInfo.setTwinkleMode(value);
        } else if (changeType == TWINKLE_FREQUENCY) {
            lightControlInfo.setTwinkleFreq(value);
        }
        lightControlInfo.setSwitchState(LightControlInfo.STATE_SETTING)
                .setLightMode(LightControlInfo.LIGHT_MODE_TWINKLE);
        mRCSPController.setLightControlInfo(mRCSPController.getUsingDevice(), lightControlInfo, null);
        JL_Log.i(TAG, "sendLightSceneCmd: value: " + value);
    }

    /**
     * 发送闪灯模式
     */
    private void sendSetTwinkleModeCmd(int mode) {
        JL_Log.i(TAG, "sendSetTwinkleModeCmd : mode:  " + mode);
        sendLightSceneCmd(TWINKLE_MODE, mode);
    }

    /**
     * 发送闪灯频率
     */
    private void sendSetTwinkleFreqCmd(int freq) {
        if (freq < 0) return;
        sendLightSceneCmd(TWINKLE_FREQUENCY, freq);
        JL_Log.i(TAG, "sendSetTwinkleFreqCmd : freq:  " + freq);
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {

        @Override
        public void onLightControlInfo(BluetoothDevice device, LightControlInfo lightControlInfo) {
            if (!isAdded() || isDetached()) return;
            int twinkleMode = lightControlInfo.getTwinkleMode();
            int twinkleFreq = lightControlInfo.getTwinkleFreq();
            if (null == lightModeAdapter || null == rgLightFlash) return;
            lightModeAdapter.setSelected(twinkleMode);
            RadioButton radioButton = (RadioButton) rgLightFlash.getChildAt(twinkleFreq);
            radioButton.setChecked(true);
            rgLightFlash.setOnCheckedChangeListener(onCheckedChangeListener);
        }
    };
}
