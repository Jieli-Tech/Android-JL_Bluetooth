package com.jieli.btsmart.ui.settings.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.jl_bt_ota.util.JL_Log;
import com.jieli.jl_http.bean.ProductModel;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

/**
 * 设备使用说明
 */
public class DeviceInstructionFragment extends BaseFragment {

     PhotoView ivDeviceInstruction;
    private View mNoDataView;
    private View mNoNetworkView;
    private CommonActivity mActivity;

    private NetworkHelper mNetworkHelper;
    private String mImageUrl;

    public DeviceInstructionFragment() {
        // Required empty public constructor
    }

    public static DeviceInstructionFragment newInstance() {
        return new DeviceInstructionFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        mNetworkHelper = NetworkHelper.getInstance();
        mNetworkHelper.registerNetworkEventCallback(mOnNetworkEventCallback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_instruction, container, false);
        mNoDataView = view.findViewById(R.id.view_device_instruction_no_data);
        mNoNetworkView = view.findViewById(R.id.view_device_instruction_no_network);
        ivDeviceInstruction = view.findViewById(R.id.iv_device_instruction);
        if (mActivity != null) {
            mActivity.updateTopBar(getString(R.string.device_instructions), R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);
        }
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageUrl = null;
        final Bundle bundle = getArguments();
        if (bundle != null) {
            mImageUrl = bundle.getString(SConstant.KEY_DEV_INSTRUCTION_PATH);
        }
        JL_Log.e(TAG, "mImageUrl:" + mImageUrl);

        updateDevInstruction(mImageUrl);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mNetworkHelper != null) {
            mNetworkHelper.unregisterNetworkEventCallback(mOnNetworkEventCallback);
            mNetworkHelper = null;
        }
    }

    private void updateDevInstruction(String imageUrl) {
        if (!isAdded() || isDetached() || ivDeviceInstruction == null) return;
        if (null == imageUrl) {
            updateNoDataUI(0);
            mNetworkHelper.checkNetworkIsAvailable();
            return;
        }
        if (mNoDataView != null) {
            mNoDataView.setVisibility(View.GONE);
        }
        if (mNoNetworkView != null) {
            mNoNetworkView.setVisibility(View.GONE);
        }
        ivDeviceInstruction.setVisibility(View.VISIBLE);
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .skipMemoryCache(false)
                .override(SIZE_ORIGINAL)
                .fallback(R.drawable.ic_empty_box_gray);

        Glide.with(MainApplication.getApplication())
                .asBitmap()
                .apply(options)
                .load(imageUrl)
                .error(R.drawable.ic_empty_box_gray)
                .into(ivDeviceInstruction);
    }

    private void updateNoDataUI(int type) {
        if (!isAdded() || isDetached()) return;
        if (mImageUrl == null) {
            if (ivDeviceInstruction != null) {
                ivDeviceInstruction.setVisibility(View.GONE);
            }
            if (type == 1) { //没有网络
                if (mNoDataView != null) {
                    mNoDataView.setVisibility(View.GONE);
                }
                if (mNoNetworkView != null) {
                    mNoNetworkView.setVisibility(View.VISIBLE);
                }
            } else {
                if (mNoNetworkView != null) {
                    mNoNetworkView.setVisibility(View.GONE);
                }
                if (mNoDataView != null) {
                    mNoDataView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private String getCacheImageUrl() {
        if (!RCSPController.getInstance().isDeviceConnected()) return null;
        DeviceInfo deviceInfo = RCSPController.getInstance().getDeviceInfo();
        String scene = ProductUtil.isChinese() ? ProductModel.MODEL_PRODUCT_INSTRUCTIONS_CN.getValue()
                : ProductModel.MODEL_PRODUCT_INSTRUCTIONS_EN.getValue();

        return ProductUtil.findCacheDesign(MainApplication.getApplication(), deviceInfo.getVid(), deviceInfo.getUid(),
                deviceInfo.getPid(), scene);
    }

    private final NetworkHelper.OnNetworkEventCallback mOnNetworkEventCallback = new NetworkHelper.OnNetworkEventCallback() {
        @Override
        public void onNetworkState(boolean isAvailable) {
            if (!isAvailable) {
                if (mImageUrl == null) {
                    updateNoDataUI(1);
                }
            } else {
                if (mImageUrl == null) {
                    updateNoDataUI(0);
                }
            }
        }

        @Override
        public void onUpdateConfigureSuccess() {

        }

        @Override
        public void onUpdateImage() {
            if (mImageUrl == null) {
                mImageUrl = getCacheImageUrl();
                if (mImageUrl != null) {
                    updateDevInstruction(mImageUrl);
                }
            }
        }

        @Override
        public void onUpdateConfigureFailed(int code, String message) {

        }
    };
}
