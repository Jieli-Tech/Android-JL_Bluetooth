package com.jieli.btsmart.ui.alarm;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.command.AlarmExpandCmd;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.AlarmRepeatAdapter;
import com.jieli.btsmart.data.model.alarm.RepeatBean;
import com.jieli.btsmart.tool.bluetooth.rcsp.BTRcspHelper;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.widget.InputTextDialog;
import com.jieli.btsmart.ui.widget.wheelview.adapter.ArrayWheelAdapter;
import com.jieli.btsmart.ui.widget.wheelview.view.WheelView;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.jieli.btsmart.ui.alarm.AlarmListFragment.KEY_ALARM_EDIT;
import static com.jieli.btsmart.ui.alarm.AlarmListFragment.KEY_ALARM_EDIT_FLAG;


/**
 * Created by chensenhua on 2018/5/18.
 */

public class AlarmSettingFragment extends Jl_BaseFragment {
    static final int BELL_REQUEST_CODE = 0xf1;

    private RecyclerView rvRepeat;
    private TextView tvAlarmName;
    private TextView tvAlarmRing;
    private Button btnDelAlarm;

    private final RCSPController mRCSPController = RCSPController.getInstance();
    private BluetoothDevice mTargetDevice;

    private AlarmBean alarmBean = new AlarmBean();
    private boolean isAdd;

    private AlarmExpandCmd.BellArg bellArg;

    public static Jl_BaseFragment newInstance() {
        return new AlarmSettingFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            String text = bundle.getString(KEY_ALARM_EDIT);
            isAdd = bundle.getBoolean(KEY_ALARM_EDIT_FLAG, true);
            alarmBean = new Gson().fromJson(text, AlarmBean.class);
        }

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_alarm_setting, container, false);
        rvRepeat = root.findViewById(R.id.rv_repeat);
        tvAlarmName = root.findViewById(R.id.tv_alarm_name);
        tvAlarmRing = root.findViewById(R.id.tv_alarm_ring);
        btnDelAlarm = root.findViewById(R.id.btn_del_alarm);

        tvAlarmName.setOnClickListener(v -> showNamedDialog());
        tvAlarmRing.setOnClickListener(v -> toSelectBell());
        btnDelAlarm.setOnClickListener(v -> showDeleteDialog());

        mRCSPController.addBTRcspEventCallback(callback);
        initTopView();
        initAlarmInfoView(root);
        initTimeView(root);
        initRepeatView();


        return root;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mRCSPController.isDeviceConnected()) {
            mTargetDevice = mRCSPController.getUsingDevice();
            if (hasBellArgs()) {
                readBellArgs();
            }

        }
    }


    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(callback);
        super.onDestroyView();
        mTargetDevice = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != BELL_REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        byte type = data.getByteExtra("type", (byte) 0);
        byte dev = data.getByteExtra("dev", (byte) 0);
        int cluster = data.getIntExtra("cluster", 0);
        String name = data.getStringExtra("name");
        alarmBean.setDevIndex(dev)
                .setBellCluster(cluster)
                .setBellName(name)
                .setBellType(type);
        tvAlarmRing.setText(alarmBean.getBellName());
    }


    private void readBellArgs() {
        mRCSPController.readAlarmBellArgs(mTargetDevice, (byte) (0x01 << alarmBean.getIndex()), new OnRcspActionCallback<List<AlarmExpandCmd.BellArg>>() {
            @Override
            public void onSuccess(BluetoothDevice device, List<AlarmExpandCmd.BellArg> message) {
                if (message == null || message.size() != 1) return;
                bellArg = message.get(0);
                initBellArgsView();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(TAG, "read rct bell arg failed ,errot = " + error);
            }
        });
    }


    private void setBellArgs() {
        if (bellArg == null) return;
        mRCSPController.setAlarmBellArg(mTargetDevice, bellArg, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                saveSuccess();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.w(TAG, "set rct bell arg failed ,errot = " + error);
                ToastUtil.showToastShort(R.string.save_alarm_failed);
            }
        });
    }


    private void toSelectBell() {
        Bundle bundle = new Bundle();
        bundle.putString(AlarmBellContainerFragment.KET_ALARM, new Gson().toJson(this.alarmBean));
        ContentActivity.startActivityForRequest(this, BELL_REQUEST_CODE, AlarmBellContainerFragment.class.getCanonicalName(), getString(R.string.bell), bundle);
    }

    private void initAlarmInfoView(View root) {
        tvAlarmName.setText(alarmBean.getName());
        tvAlarmRing.setText(alarmBean.getBellName());
        btnDelAlarm.setVisibility((isAdd ? View.GONE : View.VISIBLE));
        //根据闹钟版本决定是否显示闹钟铃声
        if (alarmBean.getVersion() == 1) {
            root.findViewById(R.id.rl_alarm_ring).setVisibility(alarmBean.getVersion() == 0 ? View.GONE : View.VISIBLE);
        }
    }


    private void initTopView() {
        TextView tvTopRight = requireActivity().findViewById(R.id.tv_content_right);
        if (tvTopRight != null) {
            tvTopRight.setVisibility(View.VISIBLE);
            tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_alarm_save, 0);
            tvTopRight.setOnClickListener(v -> saveAlarm());
        }
    }

    private void initRepeatView() {
        if (getContext() == null || !isAdded() || isDetached()) return;
        rvRepeat.setLayoutManager(new GridLayoutManager(getContext(), 7));
        AlarmRepeatAdapter alarmRepeatAdapter = new AlarmRepeatAdapter();
        String[] weeks = getResources().getStringArray(R.array.alarm_weeks_simple);
        String alarmBeanRepeatModeString = BTRcspHelper.getRepeatDescModify(requireContext(), alarmBean);
        final int FINAL_REPEAT_MODE_SINGLE = 1;
        final int FINAL_REPEAT_MODE_WORKDAY = 2;
        final int FINAL_REPEAT_MODE_EVERY_DAY = 3;
        final int FINAL_REPEAT_MODE_ELSE = 4;
        int repeatMode;
        if (alarmBeanRepeatModeString.contains(getString(R.string.alarm_repeat_single))) {
            repeatMode = FINAL_REPEAT_MODE_SINGLE;
        } else if (alarmBeanRepeatModeString.contains(getString(R.string.alarm_repeat_on_workday))) {
            repeatMode = FINAL_REPEAT_MODE_WORKDAY;
        } else if (alarmBeanRepeatModeString.contains(getString(R.string.alarm_repeat_every_day))) {
            repeatMode = FINAL_REPEAT_MODE_EVERY_DAY;
        } else {
            repeatMode = FINAL_REPEAT_MODE_ELSE;
        }
        for (int i = 0; i < weeks.length; i++) {
            String week = weeks[i];
//            String weekSimple = weeksSimple[i];
            boolean isSelected = false;
            if (repeatMode == FINAL_REPEAT_MODE_WORKDAY && i < 5) {
                isSelected = true;
            } else if (repeatMode == FINAL_REPEAT_MODE_EVERY_DAY) {
                isSelected = true;
            } else if (repeatMode == FINAL_REPEAT_MODE_ELSE && alarmBeanRepeatModeString.contains(week)) {
                isSelected = true;
            }
            alarmRepeatAdapter.addData(new RepeatBean(isSelected, week));
        }
        alarmRepeatAdapter.setOnItemClickListener((adapter, view, position) -> {
            alarmRepeatAdapter.getData().get(position).selected = !alarmRepeatAdapter.getData().get(position).selected;
            alarmBean.setRepeatMode(alarmRepeatAdapter.getAlarmMode());
            alarmRepeatAdapter.notifyDataSetChanged();
            //修改闹钟模式
        });
        rvRepeat.setAdapter(alarmRepeatAdapter);
    }


    private void initTimeView(View view) {
        if (getActivity() == null || !isAdded() || isDetached()) {
            return;
        }

        WheelView hourPicker = view.findViewById(R.id.hour);
        WheelView minPicker = view.findViewById(R.id.min);
        int visibilityCount = 5;
        int dividerMore = -ValueUtil.dp2px(getContext(), 10);
        List<String> hours = new ArrayList<>();
        List<String> mins = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format(Locale.getDefault(), "%02d", i));
        }
        for (int i = 0; i < 60; i++) {
            mins.add(String.format(Locale.getDefault(), "%02d", i));
        }

        hourPicker.setDividerType(WheelView.DividerType.WRAP);
        hourPicker.setItemsVisibleCount(visibilityCount);
        hourPicker.setAdapter(new ArrayWheelAdapter(hours));
        hourPicker.setDividerMore(dividerMore);
        hourPicker.setCurrentItem(alarmBean.getHour());
        hourPicker.setOnItemSelectedListener(index -> alarmBean.setHour((byte) index));

        minPicker.setDividerType(WheelView.DividerType.WRAP);
        minPicker.setItemsVisibleCount(visibilityCount);
        minPicker.setAdapter(new ArrayWheelAdapter(mins));
        minPicker.setDividerMore(dividerMore);
        minPicker.setCurrentItem(alarmBean.getMin());
        minPicker.setOnItemSelectedListener(index -> alarmBean.setMin((byte) index));
    }


    private void initBellArgsView() {
        JL_Log.e(TAG, "initBellArgsView");
        if (getView() == null) return;

        if (bellArg == null) {
            bellArg = new AlarmExpandCmd.BellArg(new byte[]{0x05, alarmBean.getIndex(), (byte) 0x83, (byte) 0x8a, (byte) 0x85});
        }

        requireView().findViewById(R.id.rl_alarm_bell_alarm_time).setVisibility(bellArg.isCanSetAlarmBellTime() ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.rl_alarm_bell_alarm_time).setOnClickListener(v -> showBellTimeDialog());

        requireView().findViewById(R.id.rl_alarm_bell_interval).setVisibility(bellArg.isCanSetCount() || bellArg.isCanSetInterval() ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.line_bell).setVisibility(bellArg.isCanSetCount() || bellArg.isCanSetInterval() ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.rl_alarm_bell_interval).setOnClickListener(v -> showBellIntervalDialog());
        updateBellArgsView();

    }

    private void showBellIntervalDialog() {
        DialogBellIntervalChose dialogBellIntervalChose = new DialogBellIntervalChose(bellArg, (count, interval) -> {
            bellArg.setCount((byte) count);
            bellArg.setInterval((byte) interval);
            updateBellArgsView();
        });
        dialogBellIntervalChose.show(getChildFragmentManager(), dialogBellIntervalChose.getClass().getCanonicalName());
    }

    private void showBellTimeDialog() {
        DialogBellTimeChose dialogBellTimeChose = new DialogBellTimeChose(time -> {
            bellArg.setAlarmBellTime((byte) time);
            updateBellArgsView();
        });
        dialogBellTimeChose.setCurrentTime(bellArg.getAlarmBellTime());
        dialogBellTimeChose.show(getChildFragmentManager(), dialogBellTimeChose.getClass().getCanonicalName());
    }

    private void updateBellArgsView() {
        TextView tv = requireView().findViewById(R.id.tv_alarm_bell_alarm_time);
        tv.setText(getString(R.string.min_format, bellArg.getAlarmBellTime()));

        tv = requireView().findViewById(R.id.tv_alarm_bell_interval);
        String interval = "";
        if (bellArg.isCanSetInterval()) {
            interval += getString(R.string.min_format, bellArg.getInterval());
        }

        if (bellArg.isCanSetCount()) {
            if (interval.length() > 0) {
                interval += ",";
            }
            interval += getString(R.string.count_format, bellArg.getCount());
        }
        tv.setText(interval);

    }

    private void saveAlarm() {
        JL_Log.d(TAG, "save alarm:" + alarmBean);
        if (!mRCSPController.isDeviceConnected()) {
            ToastUtil.showToastShort(getString(R.string.first_connect_device));
            return;
        }
        if (alarmBean != null && checkName(alarmBean.getName())) {
            alarmBean.setOpen(true);//编辑即认为是打开闹钟
            mRCSPController.addOrModifyAlarm(mTargetDevice, alarmBean, new OnRcspActionCallback<Boolean>() {
                @Override
                public void onSuccess(BluetoothDevice device, Boolean message) {
                    if (!isAdded() || isDetached()) return;
                    mRCSPController.readAlarmList(device, null);//重新读取闹钟列表

                    if (hasBellArgs()) {
                        setBellArgs();
                    } else {
                        saveSuccess();
                    }
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {
                    if (isAdded() && !isDetached()) {
                        ToastUtil.showToastShort(R.string.save_alarm_failed);
                    }
                }
            });
        }
    }


    private void saveSuccess() {
        if (getActivity() == null) {
            return;
        }
        ToastUtil.showToastShort(R.string.save_alarm_success);
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    private boolean hasBellArgs() {
        if (mRCSPController.getDeviceInfo(mTargetDevice) != null) {
            int flag = mRCSPController.getDeviceInfo(mTargetDevice).getAlarmExpandFlag();
            return (flag & 0x01) == 0x01;
        }
        return false;
    }


    private boolean checkName(String name) {
        if (TextUtils.isEmpty(name)) {
            ToastUtil.showToastShort(R.string.alarm_name_can_not_be_empty);
            return false;
        }

        byte[] data = name.getBytes();
        if (data.length > 20) {
            ToastUtil.showToastShort(R.string.alarm_name_length_too_long);
            return false;
        }
        return true;
    }


    //闹钟名称编辑
    private void showNamedDialog() {
        if (alarmBean == null || !isAdded() || isDetached()) return;
        new InputTextDialog.Builder()
                .setTitle(getString(R.string.named))
                .setWidth(0.9f)
                .setLeftText(getString(R.string.cancel))
                .setRightText(getString(R.string.confirm))
                .setRightColor(getResources().getColor(R.color.blue_448eff))
                .setInputText(alarmBean.getName())
                .setOnInputTextListener(new InputTextDialog.OnInputTextListener() {
                    @Override
                    public void onDismiss(InputTextDialog dialog) {

                    }

                    @Override
                    public void onInputText(InputTextDialog dialog, String text) {

                    }

                    @Override
                    public void onInputFinish(InputTextDialog dialog, String value, String lastValue) {
                        if (checkName(value)) {
                            //todo 更新闹钟名称
                            alarmBean.setName(value);
                            tvAlarmName.setText(value);
                            dialog.dismiss();
                        }
                    }
                })
                .create()
                .show(getChildFragmentManager(), "named_edit");

    }

    private final BTRcspEventCallback callback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
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


    private void showDeleteDialog() {
        if (!isAdded() || isDetached()) return;
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
                    delAlarmActual();
                    dialogFragment.dismiss();
                })
                .build()
                .show(getChildFragmentManager(), "delete_alarm");
    }

    private void delAlarmActual() {
        mRCSPController.deleteAlarm(mTargetDevice, alarmBean, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                ToastUtil.showToastShort(R.string.alarm_delete_success);
                mRCSPController.readAlarmList(device, null);
                if (getActivity() == null) {
                    return;
                }
                //删除成功重新读取闹钟列表
                getActivity().finish();
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                ToastUtil.showToastShort(R.string.alarm_delete_failure);
            }
        });
    }


}
