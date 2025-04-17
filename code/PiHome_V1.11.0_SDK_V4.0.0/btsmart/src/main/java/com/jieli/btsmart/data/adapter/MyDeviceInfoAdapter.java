package com.jieli.btsmart.data.adapter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.IBluetoothOperation;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.device.HistoryDevice;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_http.bean.ProductModel;

public class MyDeviceInfoAdapter extends BaseQuickAdapter<HistoryDevice, BaseViewHolder> {
    private final RCSPController mRCSPController;

    public MyDeviceInfoAdapter(RCSPController rcspController) {
        super(R.layout.item_device_conect_info);
        mRCSPController = rcspController;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, HistoryDevice item) {
        if (null == item) return;
        HistoryBluetoothDevice history = item.getDevice();
        boolean isUsingDevice = isUsingDevice(item);
        //更新背景
        helper.setBackgroundResource(R.id.cl_device_connect_info_container, isUsingDevice ? R.drawable.bg_device_connect_info_selected : R.drawable.bg_device_connect_info_unselect);
        //更新设备名
        helper.setText(R.id.tv_device_connect_info_name, UIHelper.getCacheDeviceName(history));
        //更新产品图
        int failedRes = DefaultResFactory.createBySdkType(history.getChipType(), history.getAdvVersion()).getLogoImg();
        String url = ProductUtil.findCacheDesign(getContext(), history.getVid(), history.getUid(), history.getPid(), ProductModel.MODEL_PRODUCT_LOGO.getValue());
        updateImageView(helper.getView(R.id.iv_device_connect_info_logo), ProductUtil.isGifFile(url), url, failedRes);
        //更新状态
        int stateRes;
        int textColor;
        if (isUsingDevice) {
            stateRes = R.string.device_status_using;
            textColor = R.color.blue_448eff;
        } else {
            switch (item.getState()) {
                case HistoryDevice.STATE_CONNECTING:
                    stateRes = R.string.bt_connecting;
                    textColor = R.color.green_text_1BC017;
                    break;
                case HistoryDevice.STATE_CONNECTED:
                    stateRes = R.string.device_status_connected;
                    textColor = R.color.green_text_1BC017;
                    break;
                case HistoryDevice.STATE_NEED_OTA:
                    stateRes = R.string.device_status_mandatory_upgrade;
                    textColor = R.color.yellow_CA8014;
                    break;
                case HistoryDevice.STATE_RECONNECT:
                    stateRes = R.string.device_status_reconnect;
                    textColor = R.color.blue_448eff;
                    break;
                default:
                    stateRes = R.string.device_status_unconnected;
                    textColor = R.color.gray_A3A3A3;
                    break;
            }
        }

        TextView tvState = helper.getView(R.id.tv_device_connect_info_state);
        tvState.setText(getContext().getString(stateRes));
        tvState.setTextColor(getContext().getResources().getColor(textColor));

        addChildClickViewIds(R.id.iv_device_connect_info_msg);
        bindViewClickListener(helper, getItemViewType(getItemPosition(item)));
    }

    public int getHistoryDeviceState(HistoryBluetoothDevice historyBtDevice) {
        int state = HistoryDevice.STATE_DISCONNECT;
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(historyBtDevice.getAddress());
        if (null == device) return state;
        if (isConnectedDevice(device.getAddress())) {
            state = HistoryDevice.STATE_CONNECTED;
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (deviceInfo != null && deviceInfo.isMandatoryUpgrade()) {
                state = HistoryDevice.STATE_NEED_OTA;
            }
        } else if (isConnectingDevice(device.getAddress())) {
            state = HistoryDevice.STATE_CONNECTING;
        }
        return state;
    }

    public HistoryDevice findHistoryDeviceByAddress(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return null;
        for (HistoryDevice device : getData()) {
            if (address.equals(device.getDevice().getAddress())) {
                return device;
            }
        }
        return null;
    }

    public boolean isConnectedDevice(String addr) {
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(addr);
        if (device == null) return false;
        boolean ret = mRCSPController.isDeviceConnected(device);
        if (!ret) {
            JL_BluetoothManager manager = mRCSPController.getBluetoothManager();
            if(null == manager) return false;
            String mappedAddr = manager.getMappedDeviceAddress(device.getAddress());
            BluetoothDevice mappedDev = BluetoothUtil.getRemoteDevice(mappedAddr);
            ret = mRCSPController.isDeviceConnected(mappedDev);
        }
        return ret;
    }

    public boolean isConnectingDevice(String addr) {
        IBluetoothOperation operation = mRCSPController.getBtOperation();
        if(null == operation) return false;
        BluetoothDevice connectingDev = operation.getConnectingDevice();
        return BluetoothAdapter.checkBluetoothAddress(addr)
                && connectingDev != null
                && addr.equals(connectingDev.getAddress());
    }

    public boolean isUsingDevice(HistoryDevice historyDevice) {
        if (null == historyDevice) return false;
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(historyDevice.getDevice().getAddress());
        return device != null && mRCSPController.isUsingDevice(device);
    }

    private void updateImageView(ImageView imageView, boolean isGif, String url, int failResId) {
        imageView.setVisibility(View.VISIBLE);
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .override(ValueUtil.dp2px(getContext(), 38));
        if (TextUtils.isEmpty(url)) {
            imageView.setImageResource(failResId);
        } else if (isGif) {
            Glide.with(getContext())
                    .asGif()
                    .apply(options)
                    .load(url)
                    .error(failResId)
                    .into(imageView);
        } else {
            Glide.with(getContext())
                    .asBitmap()
                    .apply(options)
                    .load(url)
                    .error(failResId)
                    .into(imageView);
        }
    }
}

