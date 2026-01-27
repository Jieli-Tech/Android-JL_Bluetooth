package com.jieli.btsmart.ui.alarm;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.alarm.AuditionParam;
import com.jieli.bluetooth.bean.device.alarm.DefaultAlarmBell;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.AlarmDefaultBellAdapter;
import com.jieli.btsmart.ui.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * DefaultBellFragment
 * @author zqjasonZhong
 * @since 2025/5/7
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 默认铃声界面
 */
public class DefaultBellFragment extends BaseFragment {
    private AlarmDefaultBellAdapter mBellAdapter;

    private final RCSPController mRCSPController = RCSPController.getInstance();
    private BluetoothDevice mTargetDevice;

    private int initIndex = -1;

    public void setInitIndex(int initIndex) {
        this.initIndex = initIndex;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_default_ring, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.rv_bell);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
//        String[] bellStrs = getResources().getStringArray(R.array.alarm_default_bell_list);
//        int select = 0;
//        List<DefaultAlarmBell> bells = new ArrayList<>();
//        for (int i = 0; i < bellStrs.length; i++) {
//            bells.add(new DefaultAlarmBell(i, bellStrs[i], select == i));
//        }


        mBellAdapter = new AlarmDefaultBellAdapter(new ArrayList<>());

        mBellAdapter.setOnItemClickListener((adapter, view, position) -> {
            //重复点选中，则重新试听
            if (mBellAdapter.getData().get(position).isSelected()) {
                audition(mBellAdapter.getData().get(position));
                return;
            }
            //将其他项状态修改为false
            for (int i = 0; i < mBellAdapter.getData().size(); i++) {
                if (mBellAdapter.getData().get(i).isSelected()) {
                    mBellAdapter.getData().get(i).setSelected(false);
                    mBellAdapter.notifyItemChanged(i);
                }
            }
            mBellAdapter.getData().get(position).setSelected(true);
            mBellAdapter.notifyItemChanged(position);
            audition(mBellAdapter.getData().get(position));
        });
        recyclerView.setAdapter(mBellAdapter);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
        if(mRCSPController.isDeviceConnected()){
            mTargetDevice = mRCSPController.getUsingDevice();
            getDefaultBellList(mTargetDevice);
        }
    }


    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onDestroyView();
        mTargetDevice = null;

    }


    private void getDefaultBellList(BluetoothDevice device) {
        if (mRCSPController.isDeviceConnected(device)) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (deviceInfo.getAlarmDefaultBells() == null) {
                mRCSPController.readAlarmDefaultBellList(device, null);
            } else {
                setNewData(deviceInfo.getAlarmDefaultBells());
            }
        }
    }

    private void setNewData(List<DefaultAlarmBell> list) {
        for (DefaultAlarmBell bell : list) {
            bell.setSelected(bell.getIndex() == initIndex);
        }
        mBellAdapter.setNewInstance(list);

    }

    private void audition(DefaultAlarmBell bell) {
        Intent intent = new Intent();
        intent.putExtra("type", (byte) 0);
        intent.putExtra("dev", (byte) 0);
        intent.putExtra("cluster", bell.getIndex());
        intent.putExtra("name", bell.getName());
        requireActivity().setResult(Activity.RESULT_OK, intent);
        AuditionParam param = new AuditionParam();
        param.setType((byte) 0);
        param.setDev((byte)0);
        param.setCluster(bell.getIndex());
        param.setName(bell.getName());
        mRCSPController.auditionAlarmBell(mTargetDevice, param, null);
    }


    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {

        @Override
        public void onAlarmDefaultBellListChange(BluetoothDevice device, List<DefaultAlarmBell> bells) {
            setNewData(bells);
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if(BluetoothUtil.deviceEquals(mTargetDevice, device)){
                if(getActivity() != null){
                    getActivity().finish();
                }
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            if(!BluetoothUtil.deviceEquals(mTargetDevice, device)){
                if(getActivity() != null){
                    getActivity().finish();
                }
            }
        }
    };

}
