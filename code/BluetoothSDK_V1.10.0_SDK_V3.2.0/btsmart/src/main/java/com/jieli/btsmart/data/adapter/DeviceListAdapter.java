package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Group;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.listeners.OnDeleteItemListener;
import com.jieli.btsmart.data.listeners.OnLocationItemListener;
import com.jieli.btsmart.data.model.device.HistoryDevice;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.ui.device.IDeviceConnectContract;
import com.jieli.btsmart.ui.widget.product_dialog.ProductDesign;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.List;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_CHARGING_BIN_IDLE;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_CHARGING_BIN_WORKING;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_LEFT_CONNECTED;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_LEFT_IDLE;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_RIGHT_CONNECTED;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DEVICE_RIGHT_IDLE;
import static com.jieli.jl_http.bean.ProductModel.MODEL_PRODUCT_LOGO;

/**
 * 设备列表适配器
 *
 * @author zqjasonZhong
 * @since 2020/9/28
 */
public class DeviceListAdapter extends BaseQuickAdapter<HistoryDevice, BaseViewHolder> {
    private final static String TAG = DeviceListAdapter.class.getSimpleName();
    private final IDeviceConnectContract.IDeviceConnectPresenter mPresenter;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private volatile boolean isEditMode;
    private OnDeleteItemListener mOnDeleteItemListener;
    private OnLocationItemListener mOnLocationItemListener;

    private final static int DEVICE_STATUS_DISCONNECTED = 0;
    private final static int DEVICE_STATUS_CONNECTED = 1;
    private final static int DEVICE_STATUS_USING = 2;
    private final static int DEVICE_STATUS_CONNECTING = 3;
    private final static int DEVICE_STATUS_MANDATORY_UPGRADE = 4;

    public DeviceListAdapter(@NonNull IDeviceConnectContract.IDeviceConnectPresenter presenter) {
        super(R.layout.item_device_list);
        mPresenter = presenter;
    }

    public void setOnDeleteItemListener(OnDeleteItemListener onDeleteItemListener) {
        mOnDeleteItemListener = onDeleteItemListener;
    }

    public void setOnLocationItemListener(OnLocationItemListener onLocationItemListener) {
        mOnLocationItemListener = onLocationItemListener;
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEditMode(boolean editMode) {
        if (editMode && !checkDataArrayHasDisconnectDevice()) return;
        if (isEditMode != editMode) {
            if (editMode) {
                if (getData().size() == 1 && getItem(0) != null &&
                        mPresenter.isConnectedDevice(getItem(0).getDevice().getAddress())) {
                    //因为条件不成立，不能进入编辑模式。
                    return;
                }
            }
            isEditMode = editMode;
            notifyDataSetChanged();
        }
    }

    public boolean checkDataArrayHasDisconnectDevice() {
        return mRCSPController.getConnectedDeviceList().size() < getData().size();
    }

    public HistoryDevice getHistoryDeviceByBtDevice(BluetoothDevice device) {
        if (device == null) return null;
        HistoryDevice historyDevice = null;
        if (!getData().isEmpty()) {
            for (HistoryDevice item : getData()) {
                HistoryBluetoothDevice history = item.getDevice();
                if (history == null) continue;
                if (device.getAddress().equals(history.getAddress())) {
                    historyDevice = item;
                    break;
                }
            }
        }
        return historyDevice;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateHistoryDeviceByBtDevice(BluetoothDevice device, ADVInfoResponse advInfo) {
        HistoryDevice historyDevice = getHistoryDeviceByBtDevice(device);
        if (historyDevice != null) {
            //判断adv显示的信息师傅
            ADVInfoResponse last = historyDevice.getADVInfo();
            if (ADVInfoResponse.baseInfoCompare(last, advInfo)) {
                last.setKeySettingsList(advInfo.getKeySettingsList());
                last.setLedSettingsList(advInfo.getLedSettingsList());
                return;
            }
            historyDevice.setADVInfo(advInfo);
            int index = getData().indexOf(historyDevice);
            if (index > -1) {
                notifyItemChanged(index);
            } else {
                notifyDataSetChanged();
            }
        }
    }


    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, HistoryDevice historyDevice) {
        if (historyDevice == null) return;
        HistoryBluetoothDevice history = historyDevice.getDevice();
        ADVInfoResponse advInfo = historyDevice.getADVInfo();
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(history.getAddress());
        String deviceName = UIHelper.getCacheDeviceName(history);
        //用于占位的View
        Space spaceIsUsing = viewHolder.getView(R.id.space_using);
        View leftMarginView = viewHolder.getView(R.id.place_view_ear_phone_left);
        View rightMarginView = viewHolder.getView(R.id.place_view_ear_phone_right);//只有一个
        View rightMarginViewLast = viewHolder.getView(R.id.place_view_ear_phone_right_islast);//有两个且右耳是最后一个
        ImageView ivLocationNoLeft = viewHolder.getView(R.id.iv_device_list_location_no_left);

        TextView tvDeviceStatus = viewHolder.getView(R.id.tv_device_list_status);
        ImageView ivRemoveHistory = viewHolder.getView(R.id.iv_device_list_remove_history);
        ImageView ivLocation = viewHolder.getView(R.id.iv_device_list_location);
//        RelativeLayout rlLeftDevice = viewHolder.getView(R.id.rl_device_list_left);
        Group groupLeftDevice = viewHolder.getView(R.id.item_device_list_part1);
        Group groupLeftQuantity = viewHolder.getView(R.id.item_device_list_part_left_quantity);
        ImageView ivLeftDev = viewHolder.getView(R.id.iv_device_list_left);
        TextView tvLeftDevName = viewHolder.getView(R.id.tv_device_list_left_name);
        TextView tvLeftQuantity = viewHolder.getView(R.id.tv_device_list_left_quantity);
        TextView tvLeftStatus = viewHolder.getView(R.id.tv_device_list_left_status);
        Group groupRightDevice = viewHolder.getView(R.id.item_device_list_part2);
//        RelativeLayout rlRightDevice = viewHolder.getView(R.id.rl_device_list_right);
        ImageView ivRightDev = viewHolder.getView(R.id.iv_device_list_right);
        TextView tvRightDevName = viewHolder.getView(R.id.tv_device_list_right_name);
        TextView tvRightQuantity = viewHolder.getView(R.id.tv_device_list_right_quantity);
//        RelativeLayout rlChargingBin = viewHolder.getView(R.id.rl_device_list_charging_bin);
        Group groupChargingBin = viewHolder.getView(R.id.item_device_list_part3);
        ImageView ivChargingBin = viewHolder.getView(R.id.iv_device_list_charging_bin);
        TextView tvChargingBinName = viewHolder.getView(R.id.tv_device_list_charging_bin_name);
        TextView tvChargingBinQuantity = viewHolder.getView(R.id.tv_device_list_charging_bin_quantity);

        int status = getDeviceStatus(history.getAddress());
        int leftQuantity = 0;
        int rightQuantity = 0;
        int chargingBinQuantity = 0;
//        JL_Log.w(TAG, "history : " + history + ",\n advInfo = " + advInfo);
//        if (deviceName.contains("HP6699")) advInfo = null;
        if (advInfo == null || status == DEVICE_STATUS_DISCONNECTED) {
            groupLeftQuantity.setVisibility(View.GONE);
            tvLeftStatus.setVisibility(View.GONE);
            groupRightDevice.setVisibility(View.GONE);
            groupChargingBin.setVisibility(View.GONE);
            groupLeftDevice.setVisibility(View.VISIBLE);
            updateProductDesign(ivLeftDev, history);
            tvLeftDevName.setText(deviceName);
        } else {
            List<ProductDesign> designList = convertFromADVInfo(device, advInfo);
            if (designList != null && !designList.isEmpty()) {
                for (ProductDesign design : designList) {
                    if (design == null || design.getScene() == null) continue;
                    if (design.getScene().equals(MODEL_DEVICE_LEFT_IDLE.getValue()) || design.getScene().equals(MODEL_DEVICE_LEFT_CONNECTED.getValue())) {
                        updateImageView(ivLeftDev, design.isGif(), design.getImageUrl(), design.getFailedRes());
                    } else if (design.getScene().equals(MODEL_DEVICE_RIGHT_IDLE.getValue()) || design.getScene().equals(MODEL_DEVICE_RIGHT_CONNECTED.getValue())) {
                        updateImageView(ivRightDev, design.isGif(), design.getImageUrl(), design.getFailedRes());
                    } else if (design.getScene().equals(MODEL_DEVICE_CHARGING_BIN_IDLE.getValue()) || design.getScene().equals(MODEL_DEVICE_CHARGING_BIN_WORKING.getValue())) {
                        updateImageView(ivChargingBin, design.isGif(), design.getImageUrl(), design.getFailedRes());
                    }
                }
            } else {
                updateProductDesign(ivLeftDev, history);
            }
            leftQuantity = advInfo.getLeftDeviceQuantity();
            rightQuantity = advInfo.getRightDeviceQuantity();
            chargingBinQuantity = advInfo.getChargingBinQuantity();
            groupLeftDevice.setVisibility(leftQuantity > 0 ? View.VISIBLE : View.GONE);
            tvLeftDevName.setText(deviceName);
            groupLeftQuantity.setVisibility(leftQuantity > 0 ? View.VISIBLE : View.GONE);
            updateQuantity(tvLeftQuantity, advInfo.isLeftCharging(), leftQuantity);
            tvLeftStatus.setVisibility(leftQuantity > 0 || rightQuantity > 0 ? View.GONE : View.VISIBLE);
            groupRightDevice.setVisibility(rightQuantity > 0 ? View.VISIBLE : View.GONE);
            if (rightQuantity > 0) {
                tvRightDevName.setText(deviceName);
                updateQuantity(tvRightQuantity, advInfo.isRightCharging(), rightQuantity);
            }
//            JL_Log.w(TAG, "chargingBinQuantity : " + chargingBinQuantity);
            groupChargingBin.setVisibility(chargingBinQuantity > 0 ? View.VISIBLE : View.GONE);
            if (chargingBinQuantity > 0) {
                tvChargingBinName.setText(getContext().getString(R.string.device_charging_bin, deviceName));
                updateQuantity(tvChargingBinQuantity, advInfo.isDeviceCharging(), chargingBinQuantity);
            }
        }
        if (leftQuantity == 0 && rightQuantity == 0) {
            groupLeftDevice.setVisibility(View.VISIBLE);
        }
        if (UIHelper.isHeadsetType(history.getChipType())) {
            if (leftQuantity > 0) {
                tvLeftDevName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_headset_left_flag, 0);
            } else {
                tvLeftDevName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
            if (rightQuantity > 0) {
                tvRightDevName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_headset_right_flag, 0);
            } else {
                tvRightDevName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
        } else {
            tvLeftDevName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            tvRightDevName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        }
        spaceIsUsing.setVisibility(status == DEVICE_STATUS_USING ? View.VISIBLE : View.GONE);
        rightMarginView.setVisibility((leftQuantity <= 0) && (chargingBinQuantity <= 0) && rightQuantity > 0 ? View.VISIBLE : View.GONE);// = viewHolder.getView(R.id.place_view_ear_phone_right);//只有一个
        rightMarginViewLast.setVisibility((leftQuantity > 0) && (chargingBinQuantity <= 0) && rightQuantity > 0 ? View.VISIBLE : View.GONE); //= viewHolder.getView(R.id.place_view_ear_phone_right_islast);//有两个且右耳是最后一个
        boolean visibleNoLeft = (!isEditMode && AppUtil.checkDeviceIsSupportSearch(history.getAddress())) && (leftQuantity <= 0 && rightQuantity > 0);
        ivLocationNoLeft.setVisibility(visibleNoLeft ? View.VISIBLE : View.GONE);// = viewHolder.getView(R.id.iv_device_list_location_no_left);
        ivLocationNoLeft.setOnClickListener(mLocationClickListener);

        if (leftQuantity <= 0 && rightQuantity > 0) {
            updateStatusUI(tvDeviceStatus, tvLeftStatus, status, rightQuantity);
        } else {
            updateStatusUI(tvDeviceStatus, tvLeftStatus, status, leftQuantity);
        }
        int position = getItemPosition(historyDevice);
        ivLocation.setVisibility(!visibleNoLeft && !isEditMode && AppUtil.checkDeviceIsSupportSearch(history.getAddress()) ? View.VISIBLE : View.INVISIBLE);
        ivLocation.setTag(position);
        ivLocation.setOnClickListener(mLocationClickListener);
        ivLocationNoLeft.setTag(position);
        ivRemoveHistory.setVisibility((isEditMode && status == DEVICE_STATUS_DISCONNECTED) ? View.VISIBLE : View.INVISIBLE);
        ivRemoveHistory.setTag(position);
        ivRemoveHistory.setOnClickListener(mRemoveHistoryClickListener);
    }

    private int getDeviceStatus(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) return DEVICE_STATUS_DISCONNECTED;
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(address);
        if (device == null) return DEVICE_STATUS_DISCONNECTED;
        int status = DEVICE_STATUS_DISCONNECTED;
        boolean isConnectedDevice = mPresenter.isConnectedDevice(device);
        boolean isUseDevice = mPresenter.isUsedDevice(device);
        boolean isMandatoryUpgrade = DeviceStatusManager.getInstance().isMandatoryUpgrade(device);
        boolean isConnectingDevice = mPresenter.isConnectingDevice(address);
        if (isConnectedDevice) {
            if (isMandatoryUpgrade) {
                status = DEVICE_STATUS_MANDATORY_UPGRADE;
            } else {
                if (isUseDevice) {
                    status = DEVICE_STATUS_USING;
                } else {
                    status = DEVICE_STATUS_CONNECTED;
                }
            }
        } else {
            if (isConnectingDevice) {
                status = DEVICE_STATUS_CONNECTING;
            }
        }
        return status;
    }

    private void updateProductDesign(ImageView imageView, HistoryBluetoothDevice item) {
        String fileUrl = ProductUtil.findCacheDesign(getContext(), item.getVid(), item.getUid(), item.getPid(), MODEL_PRODUCT_LOGO.getValue());
        updateImageView(imageView, ProductUtil.isGifFile(fileUrl), fileUrl, DefaultResFactory.createBySdkType(item.getChipType(),item.getAdvVersion()).getLogoImg());
    }

//    private boolean isHeadsetProduct(BluetoothDevice device, ADVInfoResponse advInfo) {
//        int vid = 0;
//        int uid = 0;
//        int pid = 0;
//        if (advInfo != null) {
//            vid = advInfo.getVid();
//            uid = advInfo.getUid();
//            pid = advInfo.getPid();
//        } else if (mPresenter.getDeviceInfo(device) != null) {
//            vid = mPresenter.getDeviceInfo(device).getVid();
//            uid = mPresenter.getDeviceInfo(device).getUid();
//            pid = mPresenter.getDeviceInfo(device).getPid();
//        }
//        String deviceType = ProductUtil.getProductType(getContext(), vid, uid, pid);
//        String productType = deviceType == null ? SConstant.DEVICE_HEADSET : deviceType;
//        if (device != null && mPresenter.getDeviceInfo(device) != null && !UIHelper.isHeadsetType(mPresenter.getDeviceInfo(device).getSdkType())) {
//            productType = SConstant.DEVICE_SOUND_BOX;
//        }
//        return SConstant.DEVICE_HEADSET.equals(productType);
//    }

    private List<ProductDesign> convertFromADVInfo(BluetoothDevice device, ADVInfoResponse advInfo) {
        if (advInfo == null || mPresenter == null)
            return null;
        String deviceType = ProductUtil.getProductType(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid());
        int sdkType = 0;
        if (device != null && mPresenter.getDeviceInfo(device) != null) {
            sdkType = mPresenter.getDeviceInfo(device).getSdkType();
        }
        List<ProductDesign> list = new ArrayList<>();
        ProductDesign design;
        String fileUrl;
        boolean isGif;
        int failRes =DefaultResFactory.createBySdkType(sdkType).getLeftImg();
        String scene = "";
        boolean isCharging = false;
        int quantity = 0;
        int lefQuantity = advInfo.getLeftDeviceQuantity();
        int rightQuantity = advInfo.getRightDeviceQuantity();
        int chargingBinQuantity = advInfo.getChargingBinQuantity();

        for (int i = 0; i < 3; i++) {
            if (lefQuantity > 0) {
                scene = MODEL_DEVICE_LEFT_IDLE.getValue();
                failRes = DefaultResFactory.createBySdkType(sdkType).getLeftImg();
                isCharging = advInfo.isLeftCharging();
                quantity = lefQuantity;
                lefQuantity = 0;
            } else if (rightQuantity > 0) {
                scene = MODEL_DEVICE_RIGHT_IDLE.getValue();
                failRes = DefaultResFactory.createBySdkType(sdkType).getRightImg();
                isCharging = advInfo.isRightCharging();
                quantity = rightQuantity;
                rightQuantity = 0;
            } else if (chargingBinQuantity > 0) {
                scene = MODEL_DEVICE_CHARGING_BIN_IDLE.getValue();
                failRes = DefaultResFactory.createBySdkType(sdkType).getBinImg();
                isCharging = advInfo.isDeviceCharging();
                quantity = chargingBinQuantity;
                chargingBinQuantity = 0;
            }
            if (!TextUtils.isEmpty(scene)) {
                fileUrl = ProductUtil.findCacheDesign(getContext(), advInfo.getVid(), advInfo.getUid(), advInfo.getPid(), scene);
                isGif = ProductUtil.isGifFile(fileUrl);
                design = new ProductDesign();
                design.setAction(ProductDesign.ACTION_SHOW_QUANTITY);
                design.setCharging(isCharging);
                design.setQuantity(quantity);
                design.setImageUrl(fileUrl);
                design.setGif(isGif);
                design.setScene(scene);
                design.setFailedRes(failRes);
                list.add(design);
                scene = null;
            }
        }
//        JL_Log.d(TAG, "convertFromADVInfo : " + list.size());
        return list;
    }

    private void updateImageView(ImageView imageView, boolean isGif, String url, int failResId) {
        if (imageView != null) {
            if (failResId <= 0) {
                failResId = DefaultResFactory.createBySdkType(-1).getLogoImg();
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

    private void updateQuantity(TextView mQuantityTv, boolean isCharging, int quantity) {
        if (mQuantityTv != null) {
            mQuantityTv.setVisibility(quantity > 0 ? View.VISIBLE : View.GONE);
            if (quantity > 0) {
                if (quantity > 100) {
                    quantity = 100;
                }
                String text;
                if (quantity >= 100) {
                    text = quantity + "%";
                } else if (quantity >= 10) {
                    text = "\t" + quantity + "%";
                } else {
                    text = "\t\t" + quantity + "%";
                }
                mQuantityTv.setText(text);
                int resId;
                if (isCharging) {
                    resId = R.drawable.ic_charging;
                } else {
                    if (quantity < 20) {
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
                }
                mQuantityTv.setCompoundDrawablesRelativeWithIntrinsicBounds(resId, 0, 0, 0);
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateStatusUI(TextView tvDeviceStatus, TextView tvLeftStatus, int status, int leftQuantity) {
        int textColor;
        Drawable leftDrawable;
        String statusText;
        switch (status) {
            case DEVICE_STATUS_CONNECTED:
                statusText = getContext().getString(R.string.device_status_connected);
                leftDrawable = getContext().getDrawable(R.drawable.ic_dot_green_shape);
                textColor = getContext().getResources().getColor(R.color.green_text_1BC017);
                break;
            case DEVICE_STATUS_USING:
                statusText = getContext().getString(R.string.device_status_using);
                leftDrawable = getContext().getDrawable(R.drawable.ic_dot_blue_shape);
                textColor = getContext().getResources().getColor(R.color.color_gradients_bg1_end);
                break;
            case DEVICE_STATUS_MANDATORY_UPGRADE:
                statusText = getContext().getString(R.string.device_status_mandatory_upgrade);
                leftDrawable = getContext().getDrawable(R.drawable.ic_dot_red_shape);
                textColor = getContext().getResources().getColor(R.color.red_FA5252);
                break;
            case DEVICE_STATUS_CONNECTING:
                statusText = getContext().getString(R.string.bt_connecting);
                leftDrawable = getContext().getDrawable(R.drawable.ic_dot_green_shape);
                textColor = getContext().getResources().getColor(R.color.green_text_1BC017);
                break;
            default:
                statusText = getContext().getString(R.string.device_status_unconnected);
                leftDrawable = getContext().getDrawable(R.drawable.ic_dot_gray_shape);
                textColor = getContext().getResources().getColor(R.color.gray_deep_8C8C96);
                break;
        }
        if (status == DEVICE_STATUS_USING) {
            tvLeftStatus.setVisibility(View.GONE);
            tvDeviceStatus.setVisibility(View.VISIBLE);
            tvDeviceStatus.setText(statusText);
            tvDeviceStatus.setTextColor(textColor);
            tvDeviceStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(leftDrawable, null, null, null);
        } else {
            tvDeviceStatus.setVisibility(View.GONE);
            if (leftQuantity == 0) {
                tvLeftStatus.setVisibility(View.VISIBLE);
                tvLeftStatus.setText(statusText);
                tvLeftStatus.setTextColor(textColor);
                tvLeftStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(leftDrawable, null, null, null);
            }
        }
    }

    private final View.OnClickListener mLocationClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            if (position >= 0 && position < getData().size()) {
                HistoryDevice item = getItem(position);
                if (mOnLocationItemListener != null && item.getDevice() != null) {
                    mOnLocationItemListener.onItemClick(v, position, item.getDevice());
                }
            }
        }
    };

    private final View.OnClickListener mRemoveHistoryClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            if (position >= 0 && position < getData().size()) {
                HistoryDevice item = getItem(position);
                if (mOnDeleteItemListener != null && item.getDevice() != null) {
                    mOnDeleteItemListener.onItemClick(v, position, item.getDevice());
                }
            }
        }
    };
}
