package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.DeviceStatus;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.device.HistoryDevice;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.util.BleScanMsgCacheManager;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_CHARGING_BIN_WORKING;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_LEFT_IDLE;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_RIGHT_IDLE;
import static com.jieli.jl_http.bean.ProductModel.MODEL_PRODUCT_LOGO;

/**
 * 设备列表适配器
 *
 * @author zqjasonZhong
 * @since 2020/9/28
 */
public class DeviceListAdapterModify extends BaseQuickAdapter<HistoryDevice, BaseViewHolder> {
    private final static String TAG = DeviceListAdapterModify.class.getSimpleName();
    private volatile boolean isEditMode;
    private final RCSPController mRCSPController;
    private final ProductCacheManager mProductCacheManager = ProductCacheManager.getInstance();

    public DeviceListAdapterModify(RCSPController rcspController) {
        super(R.layout.item_device_list_modify);
        mRCSPController = rcspController;
    }


    public boolean isEditMode() {
        return isEditMode;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(boolean editMode) {
        //当设置为编辑模式时，需要满足已连接设备的数量不等于历史记录数量
        if (editMode && mRCSPController.getConnectedDeviceList().size() == getData().size()) {
            editMode = false;
        }
        if (editMode != this.isEditMode) {
            this.isEditMode = editMode;
            notifyDataSetChanged();
        }
    }

    public boolean isConnectingDevice(String addr) {
        return BluetoothAdapter.checkBluetoothAddress(addr)
                && mRCSPController.getBtOperation().getConnectingDevice() != null
                && addr.equals(mRCSPController.getBtOperation().getConnectingDevice().getAddress());
    }

    public boolean isConnectedDevice(BluetoothDevice device) {
        if (device == null) return false;
        boolean ret = mRCSPController.isDeviceConnected(device);
        if (!ret) {
            String mappedAddr = mRCSPController.getBluetoothManager().getMappedDeviceAddress(device.getAddress());
            BluetoothDevice mappedDev = BluetoothUtil.getRemoteDevice(mappedAddr);
            ret = mRCSPController.isDeviceConnected(mappedDev);
        }
        return ret;
    }

    public boolean isUsingDevice(HistoryDevice historyDevice) {
        if (null == historyDevice) return false;
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(historyDevice.getDevice().getAddress());
        return device != null && mRCSPController.isUsingDevice(device);
    }

    public boolean isConnectedDevice(HistoryDevice historyDevice) {
        if (null == historyDevice) return false;
        return historyDevice.getState() == HistoryDevice.STATE_CONNECTED || historyDevice.getState() == HistoryDevice.STATE_NEED_OTA;
    }

    public int getHistoryDeviceState(HistoryBluetoothDevice historyBtDevice) {
        int state = HistoryDevice.STATE_DISCONNECT;
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(historyBtDevice.getAddress());
        if (null == device) return state;
        if (isConnectedDevice(device)) {
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

    public HistoryDevice findHistoryDeviceByHistory(String deviceAddress) {
        for (HistoryDevice historyDevice : getData()) {
            if (historyDevice.getDevice().getAddress().equals(deviceAddress)) {
                return historyDevice;
            }
        }
        return null;
    }

    public void updateHistoryDeviceByBtDevice(BluetoothDevice device, ADVInfoResponse advInfo) {
        if (null == device) return;
        HistoryDevice historyDevice = findHistoryDeviceByHistory(device.getAddress());
        if (historyDevice != null && !ADVInfoResponse.baseInfoCompare(historyDevice.getADVInfo(), advInfo)) {
            historyDevice.setADVInfo(advInfo);
            notifyItemChanged(getItemPosition(historyDevice));
        }
    }

    public void updateHistoryDeviceByStatus(BluetoothDevice device, int status) {
        if (null == device) return;
        if (status == StateCode.CONNECTION_OK) {
            syncHistoryDevice();
        } else {
            int newState = HistoryDevice.STATE_DISCONNECT;
            if (status == StateCode.CONNECTION_CONNECTING) {
                newState = HistoryDevice.STATE_CONNECTING;
            }
            HistoryDevice historyDevice = findHistoryDeviceByHistory(device.getAddress());
            if (historyDevice != null) {
                if (newState != historyDevice.getState()) {
                    historyDevice.setState(newState);
                    notifyItemChanged(getItemPosition(historyDevice));
                }
            }
        }
    }

    public void syncHistoryDevice() {
        List<HistoryBluetoothDevice> list = mRCSPController.getHistoryBluetoothDeviceList();
        if (list == null || list.isEmpty()) {
            if (isEditMode) isEditMode = false;
            setList(new ArrayList<>());
            return;
        }
        list = new ArrayList<>(list);
        Collections.reverse(list);
        List<HistoryDevice> historyDevices = new ArrayList<>();
        int connectedCount = 0;
        for (HistoryBluetoothDevice device : list) {
            HistoryDevice item = new HistoryDevice(device);
            item.setState(getHistoryDeviceState(device));
            boolean isUsingDevice = isUsingDevice(item);
            if (item.getState() == HistoryDevice.STATE_CONNECTED) {
                item.setADVInfo(getAdvInfo(device.getAddress()));
                connectedCount++;
            }
            if (isUsingDevice) {
                historyDevices.add(0, item);
            } else {
                historyDevices.add(item);
            }
        }
        JL_Log.d("zzc", "syncHistoryDevice : " + historyDevices);
        if (isEditMode && connectedCount == historyDevices.size()) {
            isEditMode = false;
        }
        setList(historyDevices);
    }

    private ADVInfoResponse getAdvInfo(String address) {
        DeviceStatus deviceStatus = DeviceStatusManager.getInstance().getDeviceStatus(address);
        return deviceStatus == null ? null : deviceStatus.getADVInfo();
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, HistoryDevice historyDevice) {
        if (historyDevice == null) return;
        resetView(viewHolder);//隐藏所有控件

        HistoryBluetoothDevice history = historyDevice.getDevice();
        BleScanMessage bleScanMessage = getCacheBleScanMessage(history.getAddress());
        int version = bleScanMessage == null ? history.getAdvVersion() : bleScanMessage.getVersion();
        //设备上线状态
        if (historyDevice.getState() == HistoryDevice.STATE_CONNECTED || historyDevice.getState() == HistoryDevice.STATE_NEED_OTA) {
            updateDeviceConnectedUI(viewHolder, historyDevice, version);
        } else {
            updateDeviceDisconnectUI(viewHolder, historyDevice, version);
        }
        boolean isAllowSearchDevice = ConfigureKit.getInstance().isAllowSearchDevice(history);
        viewHolder.setVisible(R.id.iv_device_list_location, !isEditMode && isAllowSearchDevice);
        viewHolder.setVisible(R.id.iv_device_list_remove_history, isEditMode && historyDevice.getState() == HistoryDevice.STATE_DISCONNECT);
    }

    private BleScanMessage getCacheBleScanMessage(String address) {
        BleScanMessage bleScanMessage = BleScanMsgCacheManager.getInstance().getBleScanMessage(address);//第一次连接AdvVersion更新没那么快，所以从广播包获取
        if (bleScanMessage == null) {//连接的设备地址可能是edr地址，所以映射不到广播包
            String bleAddress = RCSPController.getInstance().getMappedDeviceAddress(address);
            bleScanMessage = BleScanMsgCacheManager.getInstance().getBleScanMessage(bleAddress);
        }
        return bleScanMessage;
    }

    private void resetView(BaseViewHolder view) {
        ViewGroup parent = view.getView(R.id.cl_device_list_parent);
        int size = parent.getChildCount();
        for (int i = 0; i < size; i++) {
            View child = parent.getChildAt(i);
            child.setVisibility(View.GONE);
        }
    }

    private final RequestOptions options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .override(SIZE_ORIGINAL);

    private void updateImageView(ImageView imageView, boolean isGif, String url, int failResId) {
        imageView.setVisibility(View.VISIBLE);
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

    private void updateDeviceMsgUI(ImageView ivImg, TextView tvName, String url, int failedRes, String deviceName, int endRes) {
        //更新效果图
        updateImageView(ivImg, ProductUtil.isGifFile(url), url, failedRes);
        //更新设备名
        tvName.setText(deviceName);
        tvName.setVisibility(View.VISIBLE);
        tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, endRes, 0);
    }

    private void updateDeviceStateUI(TextView tvContent, String content, int textColorRes, int startRes) {
        //更新内容
        tvContent.setVisibility(View.VISIBLE);

        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) tvContent.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;

        tvContent.setText(content);
        tvContent.setTextColor(getContext().getResources().getColor(textColorRes));
        tvContent.setCompoundDrawablePadding(ValueUtil.dp2px(getContext(), 5));
        tvContent.setCompoundDrawablesRelativeWithIntrinsicBounds(startRes, 0, 0, 0);
    }

    private void updateQuantity(TextView mQuantityTv, boolean isCharging, int quantity) {
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mQuantityTv.getLayoutParams();
        layoutParams.width = ValueUtil.dp2px(getContext(), 85);

        mQuantityTv.setVisibility(View.VISIBLE);
        mQuantityTv.setBackgroundColor(Color.TRANSPARENT);
        mQuantityTv.setTextColor(getContext().getResources().getColor(R.color.black_242424));
        mQuantityTv.setTextSize(12);
        quantity = Math.min(quantity, 100);
        String text = "\t\t\t" + quantity + "%";
        mQuantityTv.setText(text.substring(text.length() - 4));
        int resId;
        if (isCharging) {
            resId = R.drawable.ic_charging;
        } else if (quantity < 20) {
            resId = R.drawable.ic_quantity_0;
        } else if (quantity <= 35) {
            resId = R.drawable.ic_quantity_25;
        } else if (quantity <= 50) {
            resId = R.drawable.ic_quantity_50;
        } else if (quantity <= 75) {
            resId = R.drawable.ic_quantity_75;
        } else {
            resId = R.drawable.ic_quantity_100;
        }
        mQuantityTv.setCompoundDrawablesRelativeWithIntrinsicBounds(resId, 0, 0, 0);
    }

    private void updateDeviceConnectedUI(BaseViewHolder viewHolder, HistoryDevice device, int version) {
        HistoryBluetoothDevice history = device.getDevice();
        boolean isUsingDevice = isUsingDevice(device);
        //更新是否正在使用设备
        viewHolder.getView(R.id.tv_device_list_status).setVisibility(isUsingDevice ? View.VISIBLE : View.GONE);
        ADVInfoResponse advInfo = device.getADVInfo();
        if (null == advInfo) { //重新获取缓存
            advInfo = getAdvInfo(history.getAddress());
        }
        String devName = UIHelper.getCacheDeviceName(history);
        ImageView ivDevice1 = viewHolder.getView(R.id.iv_device_list_left);
        TextView tvDevice1Name = viewHolder.getView(R.id.tv_device_list_left_name);
        TextView tvDevice1Quantity = viewHolder.getView(R.id.tv_device_list_left_quantity);
        if (advInfo == null) {
            String url = mProductCacheManager.getProductUrl(history.getUid(), history.getPid(),
                    history.getVid(), MODEL_PRODUCT_LOGO.getValue());
            int failedRes = DefaultResFactory.createBySdkType(history.getChipType(), version).getLogoImg();
            updateDeviceMsgUI(ivDevice1, tvDevice1Name, url, failedRes, devName, 0);
            int textRes = device.getState() == HistoryDevice.STATE_NEED_OTA ? R.string.device_status_mandatory_upgrade : R.string.device_status_connected;
            int dotRes = device.getState() == HistoryDevice.STATE_NEED_OTA ? R.drawable.ic_dot_red_shape : R.drawable.ic_dot_green_shape;
            int textColorRes = device.getState() == HistoryDevice.STATE_NEED_OTA ? R.color.red_FA5252 : R.color.green_text_1BC017;
            updateDeviceStateUI(tvDevice1Quantity, getContext().getString(textRes), textColorRes, dotRes);
        } else {
            viewHolder.setVisible(R.id.line_space_device_list_right, isUsingDevice || advInfo.getLeftDeviceQuantity() > 0);
            if (advInfo.getLeftDeviceQuantity() > 0) {
                String url = mProductCacheManager.getProductUrl(advInfo.getUid(), advInfo.getPid(), advInfo.getVid(), MODEL_DEVICE_LEFT_IDLE.getValue());
                int failedRes = DefaultResFactory.createBySdkType(history.getChipType(), version).getLeftImg();
                int endRet = UIHelper.isHeadsetType(history.getChipType()) && version != SConstant.ADV_INFO_VERSION_NECK_HEADSET ? R.drawable.ic_headset_left_flag : 0;
                updateDeviceMsgUI(ivDevice1, tvDevice1Name, url, failedRes, devName, endRet);
                updateQuantity(tvDevice1Quantity, advInfo.isLeftCharging(), advInfo.getLeftDeviceQuantity());
            }
            if (advInfo.getRightDeviceQuantity() > 0) {
                ImageView ivDevice2 = viewHolder.getView(R.id.iv_device_list_right);
                TextView tvDevice2Name = viewHolder.getView(R.id.tv_device_list_right_name);
                TextView tvDevice2Quantity = viewHolder.getView(R.id.tv_device_list_right_quantity);
                if (!isUsingDevice && advInfo.getLeftDeviceQuantity() == 0) {
                    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) ivDevice2.getLayoutParams();
                    layoutParams.topMargin = ValueUtil.dp2px(getContext(), 18);
                }
                String url = mProductCacheManager.getProductUrl(advInfo.getUid(), advInfo.getPid(), advInfo.getVid(), MODEL_DEVICE_RIGHT_IDLE.getValue());
                int failedRes = DefaultResFactory.createBySdkType(history.getChipType(), version).getRightImg();
                int endRet = UIHelper.isHeadsetType(history.getChipType()) && version != SConstant.ADV_INFO_VERSION_NECK_HEADSET ? R.drawable.ic_headset_right_flag : 0;
                updateDeviceMsgUI(ivDevice2, tvDevice2Name, url, failedRes, devName, endRet);
                updateQuantity(tvDevice2Quantity, advInfo.isRightCharging(), advInfo.getRightDeviceQuantity());
            }
            if (advInfo.getChargingBinQuantity() > 0) {
                ImageView ivDevice3 = viewHolder.getView(R.id.iv_device_list_charging_bin);
                TextView tvDevice3Name = viewHolder.getView(R.id.tv_device_list_charging_bin_name);
                TextView tvDevice3Quantity = viewHolder.getView(R.id.tv_device_list_charging_bin_quantity);
                String url = mProductCacheManager.getProductUrl(advInfo.getUid(), advInfo.getPid(), advInfo.getVid(), MODEL_DEVICE_CHARGING_BIN_WORKING.getValue());
                int failedRes = DefaultResFactory.createBySdkType(history.getChipType(), version).getBinImg();
                updateDeviceMsgUI(ivDevice3, tvDevice3Name, url, failedRes, devName, 0);
                updateQuantity(tvDevice3Quantity, advInfo.isDeviceCharging(), advInfo.getChargingBinQuantity());
            }
        }
    }

    private void updateDeviceDisconnectUI(BaseViewHolder viewHolder, HistoryDevice device, int version) {
        HistoryBluetoothDevice history = device.getDevice();
        //更新效果图
        ImageView ivLeft = viewHolder.getView(R.id.iv_device_list_left);
        String url = mProductCacheManager.getProductUrl(history.getUid(), history.getPid(),
                history.getVid(), MODEL_PRODUCT_LOGO.getValue());
        int failedRes = DefaultResFactory.createBySdkType(history.getChipType(), version).getLogoImg();
        //更新设备名
        TextView tvName = viewHolder.getView(R.id.tv_device_list_left_name);
        String devName = UIHelper.getCacheDeviceName(history);
        updateDeviceMsgUI(ivLeft, tvName, url, failedRes, devName, 0);
        //更新内容
        TextView tvQuantity = viewHolder.getView(R.id.tv_device_list_left_quantity);
        int textRes = R.string.device_status_unconnected;
        int dotRes = R.drawable.ic_dot_gray_shape;
        int textColorRes = R.color.gray_deep_8C8C96;
        switch (device.getState()) {
            case HistoryDevice.STATE_CONNECTING:
                textRes = R.string.bt_connecting;
                dotRes = R.drawable.ic_dot_green_shape;
                textColorRes = R.color.green_text_1BC017;
                break;
            case HistoryDevice.STATE_RECONNECT:
                textRes = R.string.device_status_reconnect;
                dotRes = R.drawable.ic_dot_blue_shape;
                textColorRes = R.color.blue_text_color;
                break;
        }
        updateDeviceStateUI(tvQuantity, getContext().getString(textRes), textColorRes, dotRes);
    }
}
