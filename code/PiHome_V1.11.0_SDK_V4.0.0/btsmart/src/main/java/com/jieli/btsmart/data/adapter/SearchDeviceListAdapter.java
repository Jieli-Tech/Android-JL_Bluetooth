package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.ui.search.LocationDeviceInfo;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/8/18 14:34
 * @desc :
 */
public class SearchDeviceListAdapter extends BaseQuickAdapter<LocationDeviceInfo, BaseViewHolder> {
    private final RCSPController mRCSPController = RCSPController.getInstance();

    public SearchDeviceListAdapter() {
        super(R.layout.item_search_device_list);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void convert(BaseViewHolder baseViewHolder, LocationDeviceInfo deviceInfo) {
        HistoryBluetoothDevice historyBluetoothDevice = deviceInfo.getHistoryBluetoothDevice();
        TextView tvLocation = baseViewHolder.getView(R.id.tv_search_device_list_location);
        TextView tvDeviceName = baseViewHolder.getView(R.id.tv_search_device_list_device_name);
        TextView tvStatus = baseViewHolder.getView(R.id.tv_search_device_status);
        ImageView ivDeviceType = baseViewHolder.getView(R.id.iv_search_device_list_logo);
        tvDeviceName.setText(historyBluetoothDevice.getName());
        boolean isUsing = isUsingDevice(historyBluetoothDevice.getAddress());
        boolean isConnected = isConnectedDevice(historyBluetoothDevice.getAddress());
        String status;
        Drawable leftDrawable;
        if (isUsing) {
            leftDrawable = getContext().getDrawable(R.drawable.ic_dot_blue_shape);
            tvStatus.setTextColor(getContext().getResources().getColor(R.color.blue_448eff));
            status = getContext().getString(R.string.device_status_using);
        } else if (isConnected) {
            leftDrawable = getContext().getDrawable(R.drawable.ic_dot_green_shape);
//                ivLocation.setVisibility(View.GONE);
            tvStatus.setTextColor(getContext().getResources().getColor(R.color.green_text_1BC017));
            status = getContext().getString(R.string.device_status_connected);
        } else {
            leftDrawable = getContext().getDrawable(R.drawable.ic_dot_gray_shape);
//                ivLocation.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
            tvStatus.setTextColor(getContext().getResources().getColor(R.color.gray_deep_8C8C96));
            status = getContext().getString(R.string.device_status_unconnected);
        }

        if (leftDrawable != null) {
            leftDrawable.setBounds(0, 0, leftDrawable.getMinimumWidth(), leftDrawable.getMinimumHeight());
        }
        tvStatus.setCompoundDrawables(leftDrawable, null, null, null);
        tvStatus.setText(status);
        tvLocation.setText(getLocationString(deviceInfo));
        tvLocation.setSelected(true);
        ivDeviceType.setImageResource(DefaultResFactory.createBySdkType(historyBluetoothDevice.getChipType(), historyBluetoothDevice.getAdvVersion()).getOnMapListIcon());

//        ivDeviceType.setImageResource(UIHelper.isHeadsetType(historyBluetoothDevice.getChipType()) ? R.drawable.ic_search_device_tws_headset : R.drawable.ic_search_device_sound_box);
    }

    private String getLocationString(LocationDeviceInfo info) {
        if (null == info || null == info.location)
            return getContext().getString(R.string.device_no_location_info);
        if (TextUtils.isEmpty(info.locationString))
            return "(" + info.location.getLatitude() + "," + info.location.getLongitude() + ")";
        return info.locationString;
    }

    public boolean isConnectedDevice(String addr) {
        return mRCSPController.isDeviceConnected(BluetoothUtil.getRemoteDevice(addr));
    }

    public boolean isUsingDevice(String addr) {
        return mRCSPController.isUsingDevice(BluetoothUtil.getRemoteDevice(addr));
    }
}
