package com.jieli.btsmart.data.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.View;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.device.ScanBtDevice;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.util.UIHelper;
import com.wang.avi.AVLoadingIndicatorView;

/**
 * 搜索设备适配器
 *
 * @author zqjasonZhong
 * @since 2020/5/16
 */
public class ScanDeviceAdapter extends BaseQuickAdapter<ScanBtDevice, BaseViewHolder> {

    public ScanDeviceAdapter() {
        super(R.layout.item_scan_device);
    }

    public void updateDeviceState(BluetoothDevice device, int state) {
        ScanBtDevice item = null;
        for (ScanBtDevice scanBtDevice : getData()) {
            if (BluetoothUtil.deviceEquals(scanBtDevice.getDevice(), device)) {
                item = scanBtDevice;
                break;
            }
        }
        if (item != null && item.getConnection() != state) {
            notifyItemChanged(getItemPosition(item));
        }
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, ScanBtDevice item) {
        if (item == null) return;
        helper.setText(R.id.tv_scan_device_name, UIHelper.getDevName(item.getDevice()));
        helper.setImageResource(R.id.iv_scan_device_product_type, DefaultResFactory.createByDeviceType(item.getDeviceType(), item.getBleScanMessage().getVersion()).getBlackShowIcon());
        boolean isConnecting = item.getConnection() == StateCode.CONNECTION_CONNECTING;
        AVLoadingIndicatorView avDevStatus = helper.getView(R.id.av_scan_device_status);
        avDevStatus.setVisibility(isConnecting ? View.VISIBLE : View.INVISIBLE);
    }
}
