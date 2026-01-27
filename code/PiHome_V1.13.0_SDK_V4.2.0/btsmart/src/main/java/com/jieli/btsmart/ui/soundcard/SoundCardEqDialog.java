package com.jieli.btsmart.ui.soundcard;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.eq.EqSeekBarBean;
import com.jieli.btsmart.ui.eq.EqSeekBarAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/11 11:24 AM
 * @desc :
 */
public class SoundCardEqDialog extends DialogFragment implements EqSeekBarAdapter.ValueChange {

    private final RCSPController mRCSPController = RCSPController.getInstance();
    private EqSeekBarAdapter mAdapter;

    @Override
    public void onStart() {
        super.onStart();
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        assert window != null;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(null);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_sound_card_eq, container, false);
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.horizontalMargin = 0;
        lp.windowAnimations = R.style.BottomToTopAnim;
        window.setAttributes(lp);
        List<EqSeekBarBean> list = new ArrayList<>();

        mAdapter = new EqSeekBarAdapter(list, this);
        RecyclerView rv = root.findViewById(R.id.rv_mic_eq_parent);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rv.setAdapter(mAdapter);
        root.findViewById(R.id.btn_mic_eq_reset).setOnClickListener(v -> sendResetCmd());
        root.findViewById(R.id.ibtn_mic_eq_close).setOnClickListener(v -> dismiss());
        return root;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(btEventCallback);
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (deviceInfo == null || deviceInfo.getSoundCardEqInfo() == null) {
            mRCSPController.getSoundCardEqInfo(mRCSPController.getUsingDevice(), null);
            return;
        }
        EqInfo eqInfo = deviceInfo.getSoundCardEqInfo();
        mAdapter.updateSeekBar(eqInfo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRCSPController.removeBTRcspEventCallback(btEventCallback);
    }

    @Override
    public void onChange(int index, EqInfo eqInfo, boolean end) {
        if (end) {
            setEqValue(eqInfo.getValue());
        }
    }

    private void sendResetCmd() {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (deviceInfo == null || deviceInfo.getSoundCardEqInfo() == null) {
            return;
        }
        EqInfo eqInfo = deviceInfo.getSoundCardEqInfo();
        eqInfo.setValue(new byte[eqInfo.getValue().length]);
        setEqValue(eqInfo.getValue());
        mAdapter.updateSeekBar(eqInfo);
    }

    private void setEqValue(byte[] value) {
        mRCSPController.setSoundCardEqInfo(mRCSPController.getUsingDevice(), value, null);
    }

    private final BTRcspEventCallback btEventCallback = new BTRcspEventCallback() {

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (deviceInfo != null && deviceInfo.isSupportSoundCard()) {
                mRCSPController.getSoundCardEqInfo(device, null);
            } else {
                dismiss();
            }
        }

        @Override
        public void onSoundCardEqChange(BluetoothDevice device, EqInfo eqInfo) {
            mAdapter.updateSeekBar(eqInfo);
        }

    };

}