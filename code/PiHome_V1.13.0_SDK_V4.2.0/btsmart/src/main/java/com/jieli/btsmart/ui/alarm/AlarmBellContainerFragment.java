package com.jieli.btsmart.ui.alarm;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.jieli.bluetooth.bean.device.alarm.AlarmBean;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.FileObserver;

import java.util.List;

/**
 * AlarmBellContainerFragment
 * @author zqjasonZhong
 * @since 2025/5/7
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 闹钟铃声界面
 */
public class AlarmBellContainerFragment extends BaseFragment implements FileObserver, TabLayout.OnTabSelectedListener {
    private final RCSPController mRCSPController = RCSPController.getInstance();
    final static String KET_ALARM = "key_alarm";
    private TabLayout tlDevice;
    private ViewPager2 vp2Device;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm_bell_container, container, false);

        tlDevice = view.findViewById(R.id.tl_device);
        vp2Device = view.findViewById(R.id.vp2_device);

        vp2Device.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return (Fragment) tlDevice.getTabAt(position).getTag();
            }

            @Override
            public int getItemCount() {
                return tlDevice.getTabCount();
            }
        });

        vp2Device.registerOnPageChangeCallback(pageChangeCallback);
        tlDevice.addOnTabSelectedListener(this);
        refreshView();
        FileBrowseManager.getInstance().addFileObserver(this);
        mRCSPController.addBTRcspEventCallback(mBtEventCallback);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ContentActivity contentActivity = (ContentActivity) requireActivity();
        contentActivity.setCustomBackPress(() -> {
            requireActivity().finish();
            return true;
        });
    }

    @Override
    public void onDestroyView() {
        stopAlarmAudition();
        vp2Device.unregisterOnPageChangeCallback(pageChangeCallback);
        FileBrowseManager.getInstance().removeFileObserver(this);
        tlDevice.removeOnTabSelectedListener(this);
        mRCSPController.removeBTRcspEventCallback(mBtEventCallback);
        super.onDestroyView();
    }


    @Override
    public void onSdCardStatusChange(List<SDCardBean> onLineCards) {
        refreshView();
    }


    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        vp2Device.setCurrentItem(tab.getPosition(), false);
    }


    @SuppressLint("NotifyDataSetChanged")
    private void refreshView() {
        tlDevice.removeAllTabs();
        AlarmBean alarmBean = getAlarmBean();
        //默认铃声tab
        tlDevice.addTab(createDefaultTab(), alarmBean == null || alarmBean.getBellType() == 0);
        //设备铃声tab
        List<SDCardBean> list = FileBrowseManager.getInstance().getOnlineDev();
        for (SDCardBean sdCardBean : list) {
            if (sdCardBean.getType() < 3) {
                boolean selected = alarmBean != null && alarmBean.getBellType() == 1
                        && alarmBean.getDevIndex() == sdCardBean.getIndex();
                tlDevice.addTab(createFileTab(sdCardBean), selected);
            }
        }
        vp2Device.setUserInputEnabled(tlDevice.getTabCount() > 1);
        tlDevice.setVisibility(tlDevice.getTabCount() > 1 ? View.VISIBLE : View.GONE);
        if (vp2Device.getAdapter() != null) {
            vp2Device.getAdapter().notifyDataSetChanged();
        }
    }

    //设备类型铃声选择tab
    private TabLayout.Tab createFileTab(SDCardBean bean) {
        TabLayout.Tab tab = tlDevice.newTab();
        int cluster = -1;
        int dev = -1;
        //设置选中文件
        AlarmBean alarmBean = getAlarmBean();
        if (alarmBean != null && alarmBean.getBellType() == 1 && alarmBean.getDevIndex() == bean.getIndex()) {
            cluster = alarmBean.getBellCluster();
            dev = alarmBean.getDevIndex();
        }
        FileBellFragment filesFragment = FileBellFragment.newInstance(bean, dev, cluster);
        tab.setTag(filesFragment);
        tab.setText(bean.getName());
        return tab;
    }


    private AlarmBean getAlarmBean() {
        if (getArguments() == null) {
            return null;
        }
        Bundle bundle = requireArguments();
        String text = bundle.getString(KET_ALARM);
        return new Gson().fromJson(text, AlarmBean.class);
    }


    //默认类型铃声选择tab
    private TabLayout.Tab createDefaultTab() {
        TabLayout.Tab tab = tlDevice.newTab();
        DefaultBellFragment bellFragment = new DefaultBellFragment();
        AlarmBean alarmBean = getAlarmBean();
        if (alarmBean != null && alarmBean.getBellType() == 0) {
            bellFragment.setInitIndex(alarmBean.getBellCluster());
        }

        tab.setTag(bellFragment);
        tab.setText(getString(R.string.alarm_default_bell));
        return tab;
    }

    private void stopAlarmAudition() {
        if (mRCSPController.isDeviceConnected()) {
            mRCSPController.stopAlarmBell(mRCSPController.getUsingDevice(), null);
        }
    }


    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            tlDevice.setScrollPosition(position, positionOffset, positionOffset > 0.7);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                tlDevice.selectTab(tlDevice.getTabAt(vp2Device.getCurrentItem()));
            }
        }
    };

    private final BTRcspEventCallback mBtEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            if (status != StateCode.CONNECTION_OK && status != StateCode.CONNECTION_CONNECTED) {
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        }
    };


    //忽略处理的回调 放文件末端
    @Override
    public void onFileReceiver(List<FileStruct> fileStructs) {

    }

    @Override
    public void onFileReadStop(boolean isEnd) {

    }

    @Override
    public void onFileReadStart() {

    }

    @Override
    public void onFileReadFailed(int reason) {

    }

    @Override
    public void OnFlayCallback(boolean success) {

    }


    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }


}
