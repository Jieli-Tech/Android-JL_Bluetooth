package com.jieli.btsmart.ui.settings.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.util.NetworkStateHelper;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.jl_http.bean.ProductModel;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

/**
 * 设备使用说明
 */
public class DeviceInstructionFragmentModify extends BaseFragment implements ProductCacheManager.OnUpdateListener, NetworkStateHelper.Listener {

    PhotoView ivDeviceInstruction;
    private View mNoDataView;
    private View mNoNetworkView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_instruction, container, false);
        mNoDataView = view.findViewById(R.id.view_device_instruction_no_data);
        mNoNetworkView = view.findViewById(R.id.view_device_instruction_no_network);
        ivDeviceInstruction = view.findViewById(R.id.iv_device_instruction);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadImage();
        ProductCacheManager.getInstance().registerListener(this);
        NetworkStateHelper.getInstance().registerListener(this);
    }

    @Override
    public void onDestroyView() {
        NetworkStateHelper.getInstance().unregisterListener(this);
        ProductCacheManager.getInstance().unregisterListener(this);
        super.onDestroyView();
    }



    private String getUrl() {
        DeviceInfo deviceInfo = RCSPController.getInstance().getDeviceInfo();
        if (deviceInfo == null) return "";
        String scene = ProductUtil.isChinese() ? ProductModel.MODEL_PRODUCT_INSTRUCTIONS_CN.getValue() : ProductModel.MODEL_PRODUCT_INSTRUCTIONS_EN.getValue();
        return ProductCacheManager.getInstance().getProductUrl(deviceInfo.getUid(), deviceInfo.getPid(), deviceInfo.getVid(), scene);
    }


    @Override
    public void onImageUrlUpdate(BleScanMessage bleScanMessage) {
        if (mNoDataView.getVisibility() == View.VISIBLE) {
            loadImage();
        }
    }

    @Override
    public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {

    }

    @Override
    public void onNetworkStateChange(int type, boolean available) {
        if (available && mNoNetworkView.getVisibility() == View.VISIBLE) {
            loadImage();
        }
    }

    private void loadImage() {
        String url = getUrl();
        JL_Log.d(TAG, "mImageUrl:" + url);
        if (TextUtils.isEmpty(url)) {
            //没有使用说明书
            mNoDataView.setVisibility(View.VISIBLE);
            mNoNetworkView.setVisibility(View.GONE);
            ivDeviceInstruction.setVisibility(View.GONE);
        } else {
            mNoDataView.setVisibility(View.GONE);
            mNoNetworkView.setVisibility(View.GONE);
            ivDeviceInstruction.setVisibility(View.VISIBLE);
            Glide.with(MainApplication.getApplication()).asBitmap().load(url).diskCacheStrategy(DiskCacheStrategy.RESOURCE).override(SIZE_ORIGINAL).listener(new RequestListener<Bitmap>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                    JL_Log.e(TAG, "onLoadFailed");
                    mNoNetworkView.setVisibility(View.VISIBLE);
                    return false;
                }

                @Override
                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                    JL_Log.e(TAG, "onResourceReady");
                    return false;
                }
            }).into(ivDeviceInstruction);
        }
    }
}
