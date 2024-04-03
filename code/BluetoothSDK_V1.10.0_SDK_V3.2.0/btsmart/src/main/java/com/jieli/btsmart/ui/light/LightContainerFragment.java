package com.jieli.btsmart.ui.light;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.jieli.bluetooth.bean.device.light.LightControlInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.widget.BlockClickEventLayout;
import com.jieli.btsmart.util.JLShakeItManager;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_bt_ota.util.BluetoothUtil;
import com.kyleduo.switchbutton.SwitchButton;


/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/29 1:56 PM
 * @desc : 灯光功能的容器view
 */
public class LightContainerFragment extends Jl_BaseFragment {
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final JLShakeItManager mShakeItManager = JLShakeItManager.getInstance();
    private BluetoothDevice mUseDevice;
    private TabLayout tlLight;
    private ViewPager2 vp2Light;
    private BlockClickEventLayout blockClickEventLayout;
    private SwitchButton swLight;

    public static LightContainerFragment newInstance() {
        Bundle args = new Bundle();
        LightContainerFragment fragment = new LightContainerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShakeItManager.setShakeItMode(JLShakeItManager.SHAKE_IT_MODE_CUT_LIGHT_COLOR);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_light_container, container, false);
        tlLight = root.findViewById(R.id.tl_light);
        vp2Light = root.findViewById(R.id.vp2_light);
        blockClickEventLayout = root.findViewById(R.id.bl_viewpager2);
        swLight = root.findViewById(R.id.sw_light);
        root.findViewById(R.id.ib_content_back).setOnClickListener(v -> onBack());

        tlLight.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                vp2Light.setCurrentItem(tab.getPosition(), false);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                vp2Light.setCurrentItem(tab.getPosition(), false);
            }
        });
        tlLight.getTabAt(0).setTag(LightControlFragment.newInstance());
        tlLight.getTabAt(1).setTag(LightColorTemperatureFragment.newInstance());
        tlLight.getTabAt(2).setTag(LightSceneFragment.newInstance());
        vp2Light.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return (Fragment) tlLight.getTabAt(position).getTag();
            }

            @Override
            public int getItemCount() {
                return tlLight.getTabCount();
            }
        });
        vp2Light.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                tlLight.setScrollPosition(position, positionOffset, true);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                tlLight.setScrollPosition(position, 0, true);
            }
        });

        swLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //todo 处理灯管开关控制
            if (!mRCSPController.isDeviceConnected()) {
                swLight.setChecked(false);
                return;
            }
            LightControlInfo lightControlInfo = mRCSPController.getDeviceInfo().getLightControlInfo();
            if (null == lightControlInfo) return;
            lightControlInfo.setSwitchState(isChecked ? LightControlInfo.STATE_ON : LightControlInfo.STATE_OFF);
            blockClickEventLayout.setIsIntercept(!isChecked);
            mRCSPController.setLightControlInfo(mRCSPController.getUsingDevice(), lightControlInfo, null);
        });
        blockClickEventLayout.setOnClickListener(v -> {
            if (blockClickEventLayout.isIntercept()) {
                ToastUtil.showToastShort(R.string.light_tip_open_switch);
            }
            Log.i(TAG, "onClick: blockClickEventLayout");
        });
        //隐藏顶部导航栏
        View top = requireActivity().findViewById(R.id.rl_content_main_title_top);
        if (top != null) {
            top.setVisibility(View.GONE);
        }
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUseDevice = mRCSPController.getUsingDevice();
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
        mRCSPController.getLightControlInfo(mUseDevice, null);
    }

    @Override
    public void onDestroyView() {
        mShakeItManager.setShakeItMode(JLShakeItManager.SHAKE_IT_MODE_CUT_SONG);
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onDestroyView();
    }

    public void onBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {

        @Override
        public void onLightControlInfo(BluetoothDevice device, LightControlInfo lightControlInfo) {
            if (!isAdded() || isDetached()) return;
            int switchState = lightControlInfo.getSwitchState();
            swLight.setChecked(!(switchState == 0));
            blockClickEventLayout.setIsIntercept((switchState == 0));
            vp2Light.setCurrentItem(lightControlInfo.getLightMode(), false);
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (!isAdded() || isDetached()) return;
            if (!BluetoothUtil.deviceEquals(mUseDevice, device)) return;
            switch (status) {
                case StateCode.CONNECTION_DISCONNECT:
                case StateCode.CONNECTION_FAILED:
                    onBack();
                    break;
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            super.onSwitchConnectedDevice(device);
            if (!isAdded() || isDetached()) return;
            if (!BluetoothUtil.deviceEquals(mUseDevice, device)) {
                onBack();
            }
        }
    };
}
