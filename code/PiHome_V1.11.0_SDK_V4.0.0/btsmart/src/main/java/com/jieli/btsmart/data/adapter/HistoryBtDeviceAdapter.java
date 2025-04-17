package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.listeners.OnDeleteItemListener;
import com.jieli.btsmart.data.listeners.OnLocationItemListener;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.List;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
import static com.jieli.jl_http.bean.ProductModel.MODEL_PRODUCT_LOGO;

/**
 * 历史记录适配器
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
public class HistoryBtDeviceAdapter extends BaseQuickAdapter<HistoryBluetoothDevice, BaseViewHolder> {
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private boolean isEditMode;
    private BluetoothDevice mConnectingDev;

    private OnDeleteItemListener mOnDeleteItemListener;
    private OnLocationItemListener mOnLocationItemListener;

    public HistoryBtDeviceAdapter() {
        super(R.layout.item_history_device_1);
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(boolean editMode) {
        if (isEditMode != editMode) {
            if (editMode) {
                if (getData().size() == 1 && getItem(0) != null &&
                        isConnectedDevice(getItem(0).getAddress())) {
                    //因为条件不成立，不能进入编辑模式。
                    return;
                }
            }
            isEditMode = editMode;
            notifyDataSetChanged();
        }
    }

    public void setOnDeleteItemListener(OnDeleteItemListener onDeleteItemListener) {
        mOnDeleteItemListener = onDeleteItemListener;
    }

    public void setOnLocationItemListener(OnLocationItemListener onLocationItemListener) {
        mOnLocationItemListener = onLocationItemListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateConnectingDev(BluetoothDevice device) {
        mConnectingDev = device;
        notifyDataSetChanged();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void convert(@NonNull BaseViewHolder helper, HistoryBluetoothDevice item) {
        if (item != null) {
            boolean isConnected = isConnectedDevice(item.getAddress());
            boolean isUseDev = isUseDevice(item.getAddress());
            String deviceName = UIHelper.getCacheDeviceName(item);
            ImageView ivLocation = helper.getView(R.id.iv_history_device_1_location);
            if (!AppUtil.checkDeviceIsSupportSearch(item.getAddress())) {
                ivLocation.setVisibility(View.GONE);
            } else {
                ivLocation.setVisibility(View.VISIBLE);
            }
            TextView tvStatus = helper.getView(R.id.tv_search_device_status);
            int textColor;
            Drawable leftDrawable;
            String status;
            if (isConnected) {
                boolean isMandatoryUpgrade = DeviceStatusManager.getInstance().isMandatoryUpgrade(BluetoothUtil.getRemoteDevice(item.getAddress()));
                if(isMandatoryUpgrade){
                    status = getContext().getString(R.string.device_status_mandatory_upgrade);
                    leftDrawable = getContext().getDrawable(R.drawable.ic_dot_red_shape);
                    textColor = getContext().getResources().getColor(R.color.yellow_CA8014);
                }else{
                    if (isUseDev) {
                        status = getContext().getString(R.string.device_status_using);
                        leftDrawable = getContext().getDrawable(R.drawable.ic_dot_blue_shape);
                        textColor = getContext().getResources().getColor(R.color.color_gradients_bg1_end);
                    } else {
                        status = getContext().getString(R.string.device_status_connected);
                        leftDrawable = getContext().getDrawable(R.drawable.ic_dot_green_shape);
                        textColor = getContext().getResources().getColor(R.color.green_text_1BC017);
                    }
                }
            } else {
                if (isConnectingDevice(item)) {
                    status = getContext().getString(R.string.bt_connecting);
                    leftDrawable = getContext().getDrawable(R.drawable.ic_dot_green_shape);
                    textColor = getContext().getResources().getColor(R.color.green_text_1BC017);
                } else {
                    status = getContext().getString(R.string.device_status_unconnected);
                    leftDrawable = getContext().getDrawable(R.drawable.ic_dot_gray_shape);
                    textColor = getContext().getResources().getColor(R.color.gray_deep_8C8C96);
                }
            }
            if (leftDrawable != null) {
                leftDrawable.setBounds(0, 0, leftDrawable.getMinimumWidth(), leftDrawable.getMinimumHeight());
            }
            tvStatus.setTextColor(textColor);
            tvStatus.setCompoundDrawables(leftDrawable, null, null, null);
            tvStatus.setText(status);
            helper.setText(R.id.tv_history_device_1_name, deviceName);
            ImageView imageView = helper.getView(R.id.iv_history_device_1_img);
            updateProductDesign(imageView, item);
            int position = getItemPosition(item);
            ivLocation.setTag(position);
            ivLocation.setOnClickListener(mLocationClickListener);
            ImageView closeBtn = helper.getView(R.id.iv_history_device_1_close);
            closeBtn.setTag(position);
            int visibility = isEditMode ? View.VISIBLE : View.GONE;
            if (isEditMode && isConnected) visibility = View.GONE;
            closeBtn.setVisibility(visibility);
            tvStatus.setVisibility((visibility != View.VISIBLE) ? View.VISIBLE : View.GONE);
            closeBtn.setOnClickListener(mOnClickListener);
        }
    }

    private boolean isConnectingDevice(HistoryBluetoothDevice history) {
        boolean ret = false;
        if (history != null && mConnectingDev != null) {
            ret = mConnectingDev.getAddress().equals(history.getAddress());
            if (!ret) {
                String cacheBLeAddr = UIHelper.getCacheBleAddr(history);
                if (BluetoothAdapter.checkBluetoothAddress(cacheBLeAddr)) {
                    ret = cacheBLeAddr.equals(mConnectingDev.getAddress());
                }
            }
        }
        return ret;
    }

    public boolean isConnectedDevice(String addr) {
        boolean isConnect = false;
        if (BluetoothAdapter.checkBluetoothAddress(addr)) {
            List<BluetoothDevice> connectedList = mRCSPController.getConnectedDeviceList();
            for (BluetoothDevice device : connectedList) {
                isConnect = DeviceAddrManager.getInstance().isMatchDevice(device.getAddress(), addr);
                if (isConnect) {
                    break;
                }
            }
        }
        return isConnect;
    }

    public boolean isUseDevice(String addr) {
        boolean isUse = false;
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(addr);
        if (device != null) {
            isUse = mRCSPController.isUsingDevice(device);
        }
        return isUse;
    }

    private void updateProductDesign(ImageView imageView, HistoryBluetoothDevice item) {
        String fileUrl = ProductUtil.findCacheDesign(getContext(), item.getVid(), item.getUid(), item.getPid(), MODEL_PRODUCT_LOGO.getValue());
        updateImageView(imageView, ProductUtil.isGifFile(fileUrl), fileUrl, DefaultResFactory.createBySdkType(item.getChipType(),item.getAdvVersion()).getLogoImg() );
    }


    private void updateImageView(ImageView imageView, boolean isGif, String url, int failResId) {
        if (imageView != null) {
            if (failResId <= 0) {
                failResId = R.drawable.ic_default_product_design;
            }
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .override(SIZE_ORIGINAL)
                    .fallback(failResId);
            if (isGif) {
                Glide.with(getContext())
                        .asGif()
                        .apply(options)
                        .load(url)
                        .into(imageView);
            } else {
                Glide.with(getContext())
                        .asBitmap()
                        .apply(options)
                        .load(url)
                        .into(imageView);
            }
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            if (position >= 0 && position < getData().size()) {
                HistoryBluetoothDevice item = getItem(position);
                if (mOnDeleteItemListener != null) {
                    mOnDeleteItemListener.onItemClick(v, position, item);
                }
            }
        }
    };

    private final View.OnClickListener mLocationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            if (position >= 0 && position < getData().size()) {
                HistoryBluetoothDevice item = getItem(position);
                if (mOnLocationItemListener != null) {
                    mOnLocationItemListener.onItemClick(v, position, item);
                }
            }
        }
    };
}
