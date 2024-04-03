package com.jieli.btsmart.ui.soundcard;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.soundcard.SoundCard;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.NetworkStateHelper;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_http.bean.ProductModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 *
 */
public class SoundCardFragment extends Jl_BaseFragment implements NetworkHelper.OnNetworkEventCallback, NetworkStateHelper.Listener, ProductCacheManager.OnUpdateListener {
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private boolean hasCache = false;
    private final static boolean USE_LOCAL = true;//没有获取到服务器配置文件时是否使用本地的配置文件
    private TextView tvRightBtn;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //eq弹出按钮
        tvRightBtn = requireActivity().findViewById(R.id.tv_content_right);
        tvRightBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_eq_nol, 0);
        tvRightBtn.setOnClickListener(v -> new SoundCardEqDialog().show(getChildFragmentManager(), SoundCardEqDialog.class.getCanonicalName()));

        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        updateView(scrollView);
        return scrollView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        NetworkHelper.getInstance().registerNetworkEventCallback(this);
        NetworkStateHelper.getInstance().registerListener(this);
        ProductCacheManager.getInstance().registerListener(this);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
    }


    @Override
    public void onDestroyView() {
        NetworkStateHelper.getInstance().unregisterListener(this);
        ProductCacheManager.getInstance().unregisterListener(this);
        NetworkHelper.getInstance().unregisterNetworkEventCallback(this);
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onDestroyView();
    }

    @Override
    public void onUpdateImage() {
        if (hasCache || getView() == null) return;
        //检测文件是否已经下载到本地，如没有则返回
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        if (deviceInfo == null) return;
        String scene = ProductModel.MODEL_PRODUCT_SOUND_CARD.getValue();
        String path = ProductUtil.findCacheDesign(MainApplication.getApplication(), deviceInfo.getVid(), deviceInfo.getUid(),
                deviceInfo.getPid(), scene);
        if (TextUtils.isEmpty(path)) return;
        updateView((ViewGroup) requireView());
    }


    private void updateView(ViewGroup parent) {
        String json = readJson();
        SoundCard soundCard = null;
        try {
            soundCard = new Gson().fromJson(json, SoundCard.class);//json解析异常直接执行finally代码块
            tvRightBtn.setVisibility(soundCard.hasEq ? View.VISIBLE : View.INVISIBLE);//soundCard为null时会抛异常直接执行finally代码块
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            parent.removeAllViews();
            parent.addView(generateUI(soundCard));
        }
        mRCSPController.getSoundCardStatusInfo(mRCSPController.getUsingDevice(), null);
    }

    private String readJson() {
        String path = getJsonUrl();
        JL_Log.e(TAG, "path=" + path);
        String json = "";
        InputStream is = null;
        try {
            if (TextUtils.isEmpty(path)) {
                if (!USE_LOCAL) {
                    return "";
                }
                is = requireContext().getAssets().open("sound_card/config.json");
            } else {
                hasCache = true;
                is = new FileInputStream(path);
            }
            byte[] tmp = new byte[Math.max(is.available(), 1024 * 30)];
            int len = is.read(tmp);
            json = new String(tmp, 0, len);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return json;
    }

    private String getJsonUrl() {
        DeviceInfo deviceInfo = mRCSPController.getDeviceInfo();
        String scene = ProductModel.MODEL_PRODUCT_SOUND_CARD.getValue();
        String path = "";
        if (deviceInfo != null && SConstant.CHANG_DIALOG_WAY) {
            path = ProductCacheManager.getInstance().getProductUrl(deviceInfo.getUid(), deviceInfo.getPid(), deviceInfo.getVid(), scene);
        } else if (deviceInfo != null) {
            path = ProductUtil.findCacheDesign(MainApplication.getApplication(), deviceInfo.getVid(), deviceInfo.getUid(),
                    deviceInfo.getPid(), scene);
        }
        return path;
    }

    //生成ui
    private View generateUI(SoundCard soundCard) {
        //控件父容器
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(0, 0, 0, ValueUtil.dp2px(getContext(), 18));
        View view = new View(getContext());
        view.setBackgroundColor(getResources().getColor(R.color.gray_F8FAFC));
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ValueUtil.dp2px(getContext(), 10)));
        linearLayout.addView(view);

        //没有读取到json配置，显示tip
        if (soundCard == null) {
            View noDataView = LayoutInflater.from(getContext()).inflate(R.layout.view_no_data, null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (AppUtil.getScreenHeight(getContext()) * 0.25f);
            linearLayout.addView(noDataView, lp);
            return linearLayout;
        }


        boolean firstFunctions = true;

        for (SoundCard.Functions functions : soundCard.function) {
            //标题
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            titleLp.leftMargin = ValueUtil.dp2px(getContext(), 18);
            titleLp.topMargin = ValueUtil.dp2px(requireContext(), firstFunctions ? 18 : 28);
            titleLp.bottomMargin = ValueUtil.dp2px(requireContext(), 15);
            TitleView titleView = new TitleView(functions, getContext());
            titleView.setLayoutParams(titleLp);
            linearLayout.addView(titleView);
            //功能内容
            View itemView;
            if (functions.paging) {
                PageContainer pageContainer = new PageContainer(getContext(), functions);
                itemView = pageContainer;
            } else {
                FunctionContainer functionContainer = new FunctionContainer(getContext());
                functionContainer.setFunctions(functions);
                itemView = functionContainer;
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = lp.rightMargin = ValueUtil.dp2px(getContext(), 18);
            itemView.setLayoutParams(lp);
            linearLayout.addView(itemView);
            firstFunctions = false;
        }
        return linearLayout;

    }


    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {
        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (!mRCSPController.isDeviceConnected()) {
                requireActivity().finish();
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (deviceInfo == null || !deviceInfo.isSupportSoundCard()) {
                requireActivity().finish();
            }
        }

    };


    @Override
    public void onNetworkState(boolean isAvailable) {
        if (isAvailable) {
            onUpdateImage();
        }
    }

    @Override
    public void onUpdateConfigureSuccess() {

    }

    @Override
    public void onUpdateConfigureFailed(int code, String message) {

    }


    @Override
    public void onImageUrlUpdate(BleScanMessage bleScanMessage) {

    }

    @Override
    public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {
        if (getJsonUrl().equalsIgnoreCase(path)) {
            updateView((ViewGroup) requireView());
        }
    }

    @Override
    public void onNetworkStateChange(int type, boolean available) {
        if (available) {
            onUpdateImage();
        }
    }
}