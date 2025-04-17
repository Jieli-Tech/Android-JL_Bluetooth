package com.jieli.btsmart.ui.music.device;

import static com.jieli.bluetooth.constant.AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.bean.device.status.DevStorageInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.BuildConfig;
import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.FileObserver;

import java.util.List;

/**
 * 设备文件列表界面
 */
public class ContainerFragment extends Jl_BaseFragment implements FileObserver, TabLayout.OnTabSelectedListener {
    public static final String KEY_TYPE = "type";
    public static final String KEY_DEVICE_INDEX = "index";
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private TabLayout tlDevice;
    private ViewPager2 vp2Device;
    private int mType;//0:sd卡，1：usb 参考 SDCardBean type
    private List<SDCardBean> cacheOnLineCards;
    private boolean sdCardBeanOnLineStatusChange = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cacheOnLineCards = mRCSPController.getDeviceInfo().getDevStorageInfo().getStorageStates();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_container, container, false);
        tlDevice = view.findViewById(R.id.tl_device);
        vp2Device = view.findViewById(R.id.vp2_device);

        View topView = requireActivity().findViewById(R.id.rl_content_main_title_top);
        topView.setVisibility(View.GONE);

        if (getArguments() != null) {
            mType = getArguments().getInt(KEY_TYPE, -1);
            if (mType == -1) {
                throw new RuntimeException("没有传入设备类型");
            }
        }
        JL_Log.d(TAG, "onCreateView >> " + mType);
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
        refreshView(mType);
        FileBrowseManager.getInstance().addFileObserver(this);
        mRCSPController.addBTRcspEventCallback(mBtEventCallback);

        view.findViewById(R.id.ib_device_back).setOnClickListener(v -> {
            requireActivity().finish();
        });
        return view;
    }

    @Override
    public void onDestroyView() {
        vp2Device.unregisterOnPageChangeCallback(pageChangeCallback);
        FileBrowseManager.getInstance().removeFileObserver(this);
        tlDevice.removeOnTabSelectedListener(this);
        mRCSPController.removeBTRcspEventCallback(mBtEventCallback);
        super.onDestroyView();
    }


    @Override
    public void onSdCardStatusChange(List<SDCardBean> onLineCards) {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (null == deviceInfo || null == deviceInfo.getDevStorageInfo()
                || deviceInfo.getDevStorageInfo().isDeviceReuse()) {
            JL_Log.d(TAG, "onSdCardStatusChange >> " + mType);
            requireActivity().finish();
        }
    }


    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        vp2Device.setCurrentItem(tab.getPosition(), false);
    }

    private void refreshView(int type) {
        tlDevice.removeAllTabs();
        //过滤type不同的设备
        int i = 0;
        List<SDCardBean> list = FileBrowseManager.getInstance().getOnlineDev();

        if (BuildConfig.DEBUG && list.size() == 0 && !mRCSPController.isDeviceConnected()) {
            SDCardBean sdCardBean = new SDCardBean();
            sdCardBean.setIndex(type);
            sdCardBean.setType(type);
            sdCardBean.setOnline(true);
            if (type == 0) {
                sdCardBean.setName(getString(R.string.sd_card));
            } else {
                sdCardBean.setName(getString(R.string.udisk));
            }
            list.add(sdCardBean);
        }

        for (SDCardBean sdCardBean : list) {
            if (type == sdCardBean.getType()) {
                tlDevice.addTab(createTab(sdCardBean), i++ == 0);
            }
        }

        //在线设备为空时退出activity
        if (tlDevice.getTabCount() < 1) {
            JL_Log.d(TAG, "refreshView >> " + mType);
            requireActivity().finish();
            return;
        }
        vp2Device.setUserInputEnabled(tlDevice.getTabCount() > 1);
        tlDevice.setSelectedTabIndicator(tlDevice.getTabCount() > 1 ? R.drawable.tab_indicator_device_music : R.drawable.tab_indicator_device_music_none);
        if (vp2Device.getAdapter() != null) {
            vp2Device.getAdapter().notifyDataSetChanged();
        }
    }


    private TabLayout.Tab createTab(SDCardBean bean) {
        TabLayout.Tab tab = tlDevice.newTab();
        FilesFragment filesFragment = FilesFragment.newInstance(bean);
        tab.setTag(filesFragment);
        TextView view = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.tab_device, null, false);
        view.setText(bean.getName());
        tab.setCustomView(view);
        return tab;
    }


    private final ViewPager2.OnPageChangeCallback pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            tlDevice.selectTab(tlDevice.getTabAt(position));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            tlDevice.setScrollPosition(position, positionOffset, positionOffset > 0.7);
        }
    };

    private final BTRcspEventCallback mBtEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status != StateCode.CONNECTION_OK && status != StateCode.CONNECTION_CONNECTED) {
                JL_Log.d(TAG, "onConnection >> " + mType);
                requireActivity().finish();
            }
        }

        @Override
        public void onDevStorageInfoChange(BluetoothDevice device, DevStorageInfo storageInfo) {
            super.onDevStorageInfoChange(device, storageInfo);
            //情况一：这次变化sd卡状态没变化
            //情况二：这次SD卡状态变化，但是没有进行播歌
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
            List<SDCardBean> currentOnLineCards = deviceInfo.getDevStorageInfo().getStorageStates();
            //检查当前模式是否还是音乐模式
            if (deviceInfo.getCurFunction() == SYS_INFO_FUNCTION_MUSIC) {//当前模式是音乐模式
                //判断当前类型的设备是否还在线
                boolean isHasCurrentTypeOnlineDevice = false;
                //是否存在在线的存储设备
                boolean isHasOnlineDevice = false;
                for (SDCardBean sdCardBean : currentOnLineCards) {
                    if (sdCardBean.isOnline()) {
                        if (sdCardBean.getType() == mType) {
                            isHasCurrentTypeOnlineDevice = true;
                        }
                        if (sdCardBean.getType() == SDCardBean.SD || sdCardBean.getType() == SDCardBean.USB) {
                            isHasOnlineDevice = true;
                        }
                    }
                }
                if (isHasCurrentTypeOnlineDevice) {  //判断当前类型的设备是否还在线
                    refreshView(mType);
                } else if (isHasOnlineDevice) {//是否存在在线的存储设备-既不同的存储设备类型，切换到不同的页面
                    Bundle bundle = null;
                    if (mType == SDCardBean.USB) {
                        bundle = new Bundle();
                        bundle.putInt(ContainerFragment.KEY_TYPE, SDCardBean.SD);
                    } else if (mType == SDCardBean.SD) {
                        bundle = new Bundle();
                        bundle.putInt(ContainerFragment.KEY_TYPE, SDCardBean.USB);
                    }
                    if (bundle != null) {
                        requireActivity().finish();
                        ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
                    }
                    return;
                } else {//无存储设备在线
                    requireActivity().finish();
                    return;
                }
            }
            if (cacheOnLineCards == null) {
                sdCardBeanOnLineStatusChange = true;
            } else {
                sdCardBeanOnLineStatusChange = isOnLineStatusChange(currentOnLineCards);
            }
            cacheOnLineCards = currentOnLineCards;
        }

        /**
         * 设备模式改变回调
         *
         * @param device 蓝牙设备
         * @param mode   设备当前模式
         */
        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (mode != SYS_INFO_FUNCTION_MUSIC) {
                requireActivity().finish();
            } else {
                checkMusicPlayDevice();
            }
        }

        @Override
        /**
         * 音乐播放状态回调
         *
         * @param device     蓝牙设备
         * @param statusInfo 音乐状态
         */
        public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
            checkMusicPlayDevice();
        }
    };

    private void checkMusicPlayDevice() {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (deviceInfo.getCurFunction() == SYS_INFO_FUNCTION_MUSIC) {//当前模式是音乐模式
            if (!sdCardBeanOnLineStatusChange) return;//与当前浏览(例如：U盘)的不同类型的存储设备状态发生改变(SD卡)
            MusicStatusInfo musicStatusInfo = deviceInfo.getMusicStatusInfo();
            int currentPlaySDCardBean = SDCardBean.SD;
            switch (musicStatusInfo.getCurrentDev()) {
                case SDCardBean.INDEX_USB:
                    currentPlaySDCardBean = SDCardBean.USB;
                    break;
                case SDCardBean.INDEX_SD0:
                case SDCardBean.INDEX_SD1:
                    currentPlaySDCardBean = SDCardBean.SD;
                    break;
                case SDCardBean.INDEX_FLASH:
                case SDCardBean.INDEX_FLASH2:
                    currentPlaySDCardBean = SDCardBean.FLASH;
                    break;
                case SDCardBean.INDEX_LINE_IN:
                    currentPlaySDCardBean = SDCardBean.LINEIN;
                    break;
            }
            boolean isSameDev = currentPlaySDCardBean == this.mType;
            boolean isPlay = musicStatusInfo.isPlay();
            if (!isSameDev && isPlay) {
                Bundle bundle = null;
                if (currentPlaySDCardBean == SDCardBean.USB) {
                    bundle = new Bundle();
                    bundle.putInt(ContainerFragment.KEY_TYPE, 1);
                } else if (currentPlaySDCardBean == SDCardBean.SD) {
                    bundle = new Bundle();
                    bundle.putInt(ContainerFragment.KEY_TYPE, 0);
                }
                if (bundle != null) {
                    requireActivity().finish();
                    ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
                }
            }
        }
    }

    private boolean isOnLineStatusChange(List<SDCardBean> currentOnLineCards) {
        boolean onLineStatusChange = false;
        if (this.mType == SDCardBean.SD) {//当前浏览是SD卡
            boolean cacheOnLine = false;
            boolean currentOnLine = false;
            for (SDCardBean sdCardBean : cacheOnLineCards) {
                if (sdCardBean.getIndex() == SDCardBean.INDEX_USB) {
                    cacheOnLine = sdCardBean.isOnline();
                }
            }
            for (SDCardBean sdCardBean : currentOnLineCards) {
                if (sdCardBean.getIndex() == SDCardBean.INDEX_USB) {
                    currentOnLine = sdCardBean.isOnline();
                }
            }
            onLineStatusChange = cacheOnLine != currentOnLine;
        } else if (this.mType == SDCardBean.USB) {//当前浏览是U盘
            boolean cacheOnLine1 = false;
            boolean cacheOnLine2 = false;
            boolean currentOnLine1 = false;
            boolean currentOnLine2 = false;
            for (SDCardBean sdCardBean : cacheOnLineCards) {
                if (sdCardBean.getIndex() == SDCardBean.INDEX_SD0) {
                    cacheOnLine1 = sdCardBean.isOnline();
                } else if (sdCardBean.getIndex() == SDCardBean.INDEX_SD1) {
                    cacheOnLine2 = sdCardBean.isOnline();
                }
            }
            for (SDCardBean sdCardBean : currentOnLineCards) {
                if (sdCardBean.getIndex() == SDCardBean.INDEX_SD0) {
                    currentOnLine1 = sdCardBean.isOnline();
                } else if (sdCardBean.getIndex() == SDCardBean.INDEX_SD1) {
                    currentOnLine2 = sdCardBean.isOnline();
                }
            }
            onLineStatusChange = (cacheOnLine1 != currentOnLine1) || (cacheOnLine2 != currentOnLine2);
        }
        return onLineStatusChange;
    }

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
