package com.jieli.btsmart.ui.alarm;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.bean.device.alarm.AlarmListInfo;
import com.jieli.bluetooth.bean.device.alarm.DefaultAlarmBell;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.AlarmAdapter;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Created by chensenhua on 2018/5/18.
 */

public class AlarmListFragment extends Jl_BaseFragment {
    private RecyclerView mRvAlarmList;
    private Button btnSyncTime;

    static final String KEY_ALARM_EDIT = "edit_alarm";
    static final String KEY_ALARM_EDIT_FLAG = "key_edit_flag"; //true:增加 false：编辑
    private final String tag = getClass().getSimpleName();
    private final static int RESULT_CODE = 1111;
    private AlarmAdapter mAlarmAdapter;
//    private RecyclerView mRvAlarmList;

    private final RCSPController mRCSPController = RCSPController.getInstance();
    private BluetoothDevice mTargetDevice;

    public static AlarmListFragment newInstance() {
        return new AlarmListFragment();
    }

    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(callback);
        super.onDestroyView();
        mTargetDevice = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_alarm_list, container, false);
        mRvAlarmList = root.findViewById(R.id.rv_alarm_list);
        btnSyncTime = root.findViewById(R.id.btn_sync_time);
        mRCSPController.addBTRcspEventCallback(callback);
        mTargetDevice = mRCSPController.getUsingDevice();
        //导航栏处理
        initTopView();
        initListView();
        btnSyncTime.setOnClickListener(mOnClickListener);
        return root;
    }

    private void initTopView() {
        View activityRoot = requireActivity().findViewById(R.id.rl_content_main);
        if (activityRoot != null) {
            TextView tv = activityRoot.findViewById(R.id.tv_content_right);
            if (tv != null) {
                tv.setVisibility(View.VISIBLE);
                tv.setOnClickListener(v -> {
                    if (mAlarmAdapter.getData().size() > 4) {
                        ToastUtil.showToastShort(R.string.alarm_set_num_is_full);
                    } else {
                        toAlarmSettingFragment(null);
                    }
                });
            }
        }
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        syncAlarmInfo();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_CODE && resultCode == 0 && mRCSPController.isDeviceConnected()) {
            syncAlarmInfo();
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnSyncTime) {
                syncTime();
            }
        }
    };

    private void initListView() {
        mAlarmAdapter = new AlarmAdapter(new ArrayList<>());
        if (mRCSPController.isDeviceConnected(mTargetDevice) && mRCSPController.getDeviceInfo(mTargetDevice).getAlarmListInfo() != null) {
            refreshAlarms(mRCSPController.getDeviceInfo(mTargetDevice).getAlarmListInfo().getAlarmBeans());
        }

        mAlarmAdapter.setOnAlarmEventListener(new AlarmAdapter.OnAlarmEventListener() {
            @Override
            public void onAlarmOpen(View view, AlarmBean alarmBean, boolean isOpen) {
                JL_Log.d(TAG, "关闭或者修改闹钟--->" + alarmBean);
                mRCSPController.addOrModifyAlarm(mTargetDevice, alarmBean, new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) {
                        mRCSPController.readAlarmList(device, null);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {

                    }
                });
            }

            @Override
            public void onAlarmDelete(View view, AlarmBean alarmBean, int position) {
                JL_Log.d(TAG, "删除闹钟--->" + mAlarmAdapter.getItem(position));
                showDeleteDialog(position);
            }

            @Override
            public void onAlarmClick(View view, AlarmBean alarmBean, int position) {
                //编辑闹钟
                JL_Log.d(TAG, "编辑闹钟--->" + mAlarmAdapter.getItem(position));
                toAlarmSettingFragment(mAlarmAdapter.getItem(position));
            }
        });
        mRvAlarmList.setAdapter(mAlarmAdapter);
        mRvAlarmList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void syncTime() {
        if(!mRCSPController.isDeviceConnected(mTargetDevice)) return;
        mRCSPController.syncTime(mTargetDevice, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                ToastUtil.showToastShort(getString(R.string.sync_time_success));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                ToastUtil.showToastShort(getString(R.string.sync_time_failed));
            }
        });
    }

    private void syncAlarmInfo() {
        if(!mRCSPController.isDeviceConnected(mTargetDevice)) return;
        mRCSPController.readAlarmList(mTargetDevice, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                if (mRCSPController.getDeviceInfo(device).getAlarmDefaultBells() == null) {
                    mRCSPController.readAlarmDefaultBellList(device, null);
                }
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {

            }
        });
    }

    //跳转编辑闹钟
    protected void toAlarmSettingFragment(AlarmBean alarmBean) {
        if (getContext() == null || !isAdded() || isDetached()) {
            return;
        }
        Bundle bundle = new Bundle();
        boolean isAdd = alarmBean == null;
        if (isAdd) {
            alarmBean = createNewAlarm();
        }
        bundle.putBoolean(KEY_ALARM_EDIT_FLAG, isAdd);
        bundle.putString(KEY_ALARM_EDIT, new Gson().toJson(alarmBean, AlarmBean.class));
        ContentActivity.startActivity(getContext(), AlarmSettingFragment.class.getCanonicalName(), getString(isAdd ? R.string.alarm_create_title : R.string.alarm_edit_title), bundle);
    }

    //新建一个闹钟
    private AlarmBean createNewAlarm() {
        AlarmBean alarmBean = new AlarmBean();
        alarmBean.setIndex(getAddIndex());
        alarmBean.setName(getString(R.string.default_alarm_name));
        Calendar calendar = Calendar.getInstance();
        alarmBean.setHour((byte) calendar.get(Calendar.HOUR_OF_DAY));
        alarmBean.setMin((byte) calendar.get(Calendar.MINUTE));
        if (mRCSPController.isDeviceConnected(mTargetDevice)) {
            if (mRCSPController.getDeviceInfo(mTargetDevice).getAlarmListInfo() != null) {
                alarmBean.setVersion(mRCSPController.getDeviceInfo(mTargetDevice).getAlarmListInfo().getVersion());
            }
            //版本为1的是否设置铃声
            if (alarmBean.getVersion() == 1) {
                String bellName = getString(R.string.alarm_bell_1);
                //根据默认铃声列表设置默认铃声
                List<DefaultAlarmBell> bells = mRCSPController.getDeviceInfo(mTargetDevice).getAlarmDefaultBells();
                if (bells != null && bells.size() > 0) {
                    bellName = bells.get(0).getName();
                }
                alarmBean.setBellName(bellName)
                        .setBellType((byte) 0)
                        .setBellCluster(0);
            }
        }
        return alarmBean;
    }

    //因为固件端闹钟索引是固定1-5的，所以新增是需要判断插入
    private byte getAddIndex() {
        byte index = 0;
        List<AlarmBean> list = mAlarmAdapter.getData();
        if (list.size() == 0) {
            return index;
        } else {
            //按序号排序
            Collections.sort(list, (o1, o2) -> Byte.compare(o1.getIndex(), o2.getIndex()));
        }
        for (AlarmBean alarmBean : list) {
            if (alarmBean.getIndex() != index) {
                return index;
            } else {
                index++;
            }
        }
        return index;
    }

    private final BTRcspEventCallback callback = new BTRcspEventCallback() {

        @Override
        public void onAlarmListChange(BluetoothDevice device, AlarmListInfo alarmListInfo) {
            JL_Log.d(TAG, "onAlarmListChange-->" + alarmListInfo.getAlarmBeans().size());
            List<AlarmBean> list = alarmListInfo.getAlarmBeans();
            refreshAlarms(list);
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (BluetoothUtil.deviceEquals(mTargetDevice, device)) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            if (!BluetoothUtil.deviceEquals(mTargetDevice, device)) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        }
    };


    private void refreshAlarms(List<AlarmBean> list) {
        //按时间排序
        Collections.sort(list, (o1, o2) -> {
            int time1 = o1.getHour() * 60 + o1.getMin();
            int time2 = o2.getHour() * 60 + o2.getMin();
            return Integer.compare(time1, time2);
        });
        mAlarmAdapter.setNewInstance(list);
    }

    private void showDeleteDialog(int position) {
        Jl_Dialog.builder()
                .left(getString(R.string.cancel))
                .right(getString(R.string.delete))
                .title(getString(R.string.tips))
                .content(getString(R.string.delete_alarm_tip))
                .rightColor(getResources().getColor(R.color.red_EA4F4F))
                .leftColor(getResources().getColor(R.color.gray_A3A3A3))
                .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                .rightClickListener((v, dialogFragment) -> {
                    //删除闹钟
                    AlarmBean alarmBean = mAlarmAdapter.getData().get(position);
                    mRCSPController.deleteAlarm(mTargetDevice, alarmBean, null);
                    mAlarmAdapter.getData().remove(position);
                    mAlarmAdapter.notifyItemRemoved(position);
                    dialogFragment.dismiss();
                })
                .build()
                .show(getChildFragmentManager(), "delete_alarm");
    }


}
