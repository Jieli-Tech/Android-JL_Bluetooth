package com.jieli.btsmart.ui.light;

import android.bluetooth.BluetoothDevice;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.light.LightControlInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.LightSceneAdapter;
import com.jieli.btsmart.data.model.light.Scene;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.util.AppUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/29 1:56 PM
 * @desc : 灯光情景模式
 */
public class LightSceneFragment extends BaseFragment {
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private LightSceneAdapter lightSceneAdapter;

    public static LightSceneFragment newInstance() {
        return new LightSceneFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_light_scene, container, false);
        RecyclerView recyclerView = root.findViewById(R.id.rv_light_scene);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        lightSceneAdapter = new LightSceneAdapter();
        recyclerView.setAdapter(lightSceneAdapter);
        String[] scenes = AppUtil.getContext().getResources().getStringArray(R.array.light_scene_name);
        TypedArray sceneRes = getResources().obtainTypedArray(R.array.light_scene_res);

        //初始化数据
        List<Scene> list = new ArrayList<>();
        for (int i = 0; i < sceneRes.length() && i < scenes.length; i++) {
            Scene scene = new Scene();
            scene.setName(scenes[i]);
            scene.setResId(sceneRes.getResourceId(i, R.drawable.ic_local_music_blue));
            list.add(scene);
        }
        sceneRes.recycle();
        lightSceneAdapter.setNewInstance(list);
        lightSceneAdapter.setOnItemClickListener((adapter, view, position) -> {
            int sceneMode = (Integer) view.getTag();
            sendLightSceneCmd(sceneMode);
            lightSceneAdapter.setSelectedTag(sceneMode);
        });
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (null != deviceInfo) {
            LightControlInfo lightControlInfo = deviceInfo.getLightControlInfo();
            if (null != lightControlInfo) {
                int sceneMode = lightControlInfo.getSceneMode();
                lightSceneAdapter.setSelectedTag(sceneMode);
            }
        }
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
    }

    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onDestroyView();
    }


    private void sendLightSceneCmd(int scene) {
        if (!mRCSPController.isDeviceConnected()) return;
        LightControlInfo lightControlInfo = mRCSPController.getDeviceInfo().getLightControlInfo();
        if (null == lightControlInfo) return;
        lightControlInfo.setSwitchState(LightControlInfo.STATE_SETTING)
                .setLightMode(LightControlInfo.LIGHT_MODE_SCENE)
                .setSceneMode(scene);
        mRCSPController.setLightControlInfo(mRCSPController.getUsingDevice(), lightControlInfo, null);
        JL_Log.i(TAG, "sendLightSceneCmd: scene: " + scene);
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {

        @Override
        public void onLightControlInfo(BluetoothDevice device, LightControlInfo lightControlInfo) {
            if (!isAdded() || isDetached()) return;
            if (lightControlInfo != null) {
                int sceneMode = lightControlInfo.getSceneMode();
                lightSceneAdapter.setSelectedTag(sceneMode);
            }
        }
    };
}