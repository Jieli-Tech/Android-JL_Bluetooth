package com.jieli.btsmart.ui.widget.DevicePopDialog;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.tws.NotifyAdvInfoCmd;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.Command;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.constant.ProductAction;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.jl_http.bean.ProductMessage;
import com.jieli.jl_http.bean.ProductModel;


/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/4 4:04 PM
 * @desc :
 */
public class DevicePopDialogView extends ConstraintLayout implements ProductCacheManager.OnUpdateListener {

    private final String tag = getClass().getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();
    BleScanMessage bleScanMessage = null;
    BluetoothDevice device = null;

    public DevicePopDialogView(@NonNull Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.view_product_message2, this);
        setBackgroundResource(R.color.half_transparent_1);
        setOnClickListener(v -> dismissWithIgnore());
    }


    @SuppressLint("MissingPermission")
    private void initView() {
        TextView mTitleTv = findViewById(R.id.product_message_title);
        mTitleTv.setText(UIHelper.getCacheDeviceName(device));
        //点击事件处理
        findViewById(R.id.product_message_close).setOnClickListener(v -> dismissWithIgnore());
        findViewById(R.id.product_message_connect_btn).setOnClickListener(v -> {
            dismissWithIgnore();
            new ClickActionHandler(device, bleScanMessage).connect();
        });

        findViewById(R.id.product_message_finish_tv).setOnClickListener(v -> {
            dismissWithIgnore();
            new ClickActionHandler(device, bleScanMessage).finish(getContext());
        });


        findViewById(R.id.product_message_check_tv).setOnClickListener(v -> {
            dismissWithIgnore();
            new ClickActionHandler(device, bleScanMessage).info(getContext());
        });

        findViewById(R.id.product_message_main_layout).setOnClickListener(v -> {
        });
    }

    private void dismissWithIgnore() {
        if (isActivated()) {
            dismiss();
            DevicePopDialogFilter.getInstance().addSeqIgnore(bleScanMessage);
        }
    }

    void dismiss() {
        if (isActivated()) {
            WindowManager wm = (WindowManager) getTag();
            if (null != wm) {
                wm.removeViewImmediate(this);
            }
            setTag(null);
            setActivated(false);
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mRCSPController.addBTRcspEventCallback(mBtEventCallback);
        initView();
        refreshView(bleScanMessage);
        ProductCacheManager.getInstance().registerListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        ProductCacheManager.getInstance().unregisterListener(this);
        setActivated(false);
        mRCSPController.removeBTRcspEventCallback(mBtEventCallback);
        super.onDetachedFromWindow();
    }

    @Override
    public void onImageUrlUpdate(BleScanMessage message) {
        if (message.baseEquals(bleScanMessage)) {
            JL_Log.d(tag, "onImageUrlUpdate", " --> " + message);
            refreshContent(this.bleScanMessage);
        }
    }

    @Override
    public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {

    }


    private void refreshView(BleScanMessage bleScanMessage) {
        refreshContent(bleScanMessage); //中间内容
        refreshBottomView(bleScanMessage);//底部内容
    }

    //更新底部对话框
    private void refreshBottomView(BleScanMessage bleScanMessage) {
        TextView mTipsTv = findViewById(R.id.product_message_tips_tv);
        View mFinishLayout = findViewById(R.id.product_message_finish_layout);
        TextView checkTv = findViewById(R.id.product_message_check_tv);
        TextView finishTv = findViewById(R.id.product_message_finish_tv);
        TextView mConnectDeviceButton = findViewById(R.id.product_message_connect_btn);
        mTipsTv.setVisibility(GONE);
        mFinishLayout.setVisibility(GONE);
        finishTv.setVisibility(GONE);
        checkTv.setVisibility(GONE);
        mConnectDeviceButton.setVisibility(GONE);
        //Tips： 兼容多语言切换
        mConnectDeviceButton.setText(getContext().getText(R.string.connect_tip));
        finishTv.setText(getContext().getText(R.string.finish));
        checkTv.setText(getContext().getText(R.string.device_review));

        JL_Log.d(tag, "refreshBottomView", "action = " + bleScanMessage.getAction());
        switch (bleScanMessage.getAction()) {
            case ProductAction.DEVICE_ACTION_UNCONNECTED:
                mConnectDeviceButton.setVisibility(VISIBLE);//未连接设备，显示连接按钮
                break;
            case ProductAction.DEVICE_ACTION_CONNECTED: {
                boolean isPhoneConnected = UIHelper.isEdrConnect(bleScanMessage.getEdrAddr()) ||
                        (bleScanMessage.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_CHARGING_BIN && mRCSPController.isDeviceConnected(device));
                boolean isPaired = UIHelper.isContainsBoundedEdrList(getContext(), bleScanMessage.getEdrAddr());
                if (isPhoneConnected) {
                    mFinishLayout.setVisibility(VISIBLE);
                } else if (!isPaired) {
                    //设备已连接，如果手机没有配对该设备，则提示用户配对设备
                    mTipsTv.setVisibility(VISIBLE);
                    mTipsTv.setText(getTipText(bleScanMessage));
                }
            }
            break;
            case ProductAction.DEVICE_ACTION_CONNECTING:
                boolean isPaired = UIHelper.isContainsBoundedEdrList(getContext(), bleScanMessage.getEdrAddr()) || bleScanMessage.getVid() == BluetoothConstant.APPLE_VID;
                //设备回连中，如果手机没有配对该设备，则提示用户配对设备
                if (!isPaired) {
                    mTipsTv.setVisibility(VISIBLE);
                    mTipsTv.setText(getTipText(bleScanMessage));
                }
                break;
            case ProductAction.DEVICE_ACTION_CONNECTIONLESS:
                mTipsTv.setVisibility(VISIBLE);
                mTipsTv.setText(getContext().getString(R.string.device_connectionless_tips));
                break;
        }
    }

    //更新内容
    @SuppressLint("MissingPermission")
    private void refreshContent(BleScanMessage bleScanMessage) {
        JL_Log.d(tag, "refreshContent", " --> " + bleScanMessage);
        ConstraintLayout clProduct = findViewById(R.id.cl_product);//左右耳
        ConstraintLayout clProduct2 = findViewById(R.id.cl_product_2);//充电仓
        ImageView ivP1 = findViewById(R.id.iv_product_1);
        ImageView ivP2 = findViewById(R.id.iv_product_2);
        ImageView ivP3 = findViewById(R.id.iv_product_3);

        TextView tvQ1 = findViewById(R.id.item_product_quantity_1);
        TextView tvQ2 = findViewById(R.id.item_product_quantity_2);
        TextView tvQ3 = findViewById(R.id.item_product_quantity_3);
        clProduct.setVisibility(VISIBLE);
        clProduct2.setVisibility(GONE);
        tvQ1.setVisibility(GONE);
        tvQ2.setVisibility(GONE);
        tvQ3.setVisibility(GONE);

        ivP1.setVisibility(GONE);
        ivP2.setVisibility(GONE);
        ivP3.setVisibility(GONE);

        int action = bleScanMessage.getAction();
        boolean isHeadset = UIHelper.isHeadsetByDeviceType(bleScanMessage.getDeviceType());
        isHeadset = isHeadset && bleScanMessage.getVersion() != SConstant.ADV_INFO_VERSION_NECK_HEADSET;//广播包类型不为挂脖

        boolean isPhoneConnected = UIHelper.isEdrConnect(bleScanMessage.getEdrAddr());
        boolean isPaired = UIHelper.isContainsBoundedEdrList(getContext(), bleScanMessage.getEdrAddr());
        boolean isChargingCase = bleScanMessage.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_CHARGING_BIN;
        //是否显示电量
        boolean showQuantity = action == ProductAction.DEVICE_ACTION_CONNECTED && (isPhoneConnected || isChargingCase); //经典蓝牙已连接本手机或者是充电仓
        showQuantity = showQuantity || action == ProductAction.DEVICE_ACTION_CONNECTING && (isPaired || isChargingCase); //经典蓝牙正在连接本手机或者是充电仓

        JL_Log.d(tag, "refreshContent", "name=" + UIHelper.getDevName(device) + "\tshowQuantity --->" + showQuantity + "\tisHeadset=" + isHeadset);
        //显示logo
        if (showQuantity) {//显示电量
            if (!isHeadset) {
                //音箱类型设备
                clProduct.setVisibility(GONE);
                clProduct2.setVisibility(VISIBLE);
                showByScene(bleScanMessage, ivP3, ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getLeftImg());
                showQuantity(tvQ3, bleScanMessage.isLeftCharging(), bleScanMessage.getLeftDeviceQuantity());
            } else {
                //耳机类型设备
                //耳机仓不在线,双耳在线
                if (bleScanMessage.getLeftDeviceQuantity() > 0 && bleScanMessage.getRightDeviceQuantity() > 0 && bleScanMessage.getChargingBinQuantity() == 0
                        && !bleScanMessage.isLeftCharging() && !bleScanMessage.isRightCharging()) {
                    clProduct.setVisibility(GONE);
                    clProduct2.setVisibility(VISIBLE);
                    showByScene(bleScanMessage, ivP3, ProductModel.MODEL_DOUBLE_HEADSET.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getDoubleImg());
                    showQuantity(tvQ3, false, Math.min(bleScanMessage.getLeftDeviceQuantity(), bleScanMessage.getRightDeviceQuantity()));
                    return;
                }
                //耳机左侧状态
                if (bleScanMessage.getLeftDeviceQuantity() > 0) {
                    showByScene(bleScanMessage, ivP1, ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getLeftImg());
                    showQuantity(tvQ1, bleScanMessage.isLeftCharging(), bleScanMessage.getLeftDeviceQuantity());
                }
                //耳机右侧状态
                if (bleScanMessage.getRightDeviceQuantity() > 0) {
                    showByScene(bleScanMessage, ivP2, ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getRightImg());
                    showQuantity(tvQ2, bleScanMessage.isRightCharging(), bleScanMessage.getRightDeviceQuantity());
                }

                //充电仓状态
                if (bleScanMessage.getChargingBinQuantity() > 0) {
                    clProduct2.setVisibility(VISIBLE);
                    showByScene(bleScanMessage, ivP3, ProductModel.MODEL_DEVICE_CHARGING_BIN_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getBinImg());
                    showQuantity(tvQ3, bleScanMessage.isDeviceCharging(), bleScanMessage.getChargingBinQuantity());
                }
            }
        } else {
            if (!isHeadset) {
                //音箱类型设备
                clProduct.setVisibility(GONE);
                clProduct2.setVisibility(VISIBLE);
                showByScene(bleScanMessage, ivP3, ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getLeftImg());
            } else {
                //耳机类型设备
                //耳机侧图标
//                showByScene(bleScanMessage, ivP1, ProductModel.MODEL_DOUBLE_HEADSET.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType()).getDoubleImg());
                showByScene(bleScanMessage, ivP1, ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getLeftImg());
                showByScene(bleScanMessage, ivP2, ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getRightImg());
                //充电仓
                clProduct2.setVisibility(VISIBLE);
                showByScene(bleScanMessage, ivP3, ProductModel.MODEL_DEVICE_CHARGING_BIN_IDLE.getValue(), DefaultResFactory.createByDeviceType(bleScanMessage.getDeviceType(), bleScanMessage.getVersion()).getBinImg());
            }

        }
    }

    @SuppressLint("CheckResult")
    private void showByScene(BleScanMessage bleScanMessage, ImageView iv, String scene, int failedRes) {
        iv.setVisibility(VISIBLE);
        String url = ProductCacheManager.getInstance().getProductUrl(bleScanMessage, scene);
        JL_Log.d(tag, "showByScene", "scene = " + scene + "\turl = " + url);
        if (!TextUtils.isEmpty(url)) {
            Glide.with(getContext()).asBitmap().load(url).diskCacheStrategy(DiskCacheStrategy.ALL).error(failedRes).into(iv);
        } else {//否则Glide更新会闪烁
            Glide.with(getContext()).asBitmap().load(failedRes).into(iv);
        }
    }

    private void showQuantity(TextView tv, boolean isCharing, int quantity) {
        int resId;
        if (isCharing) {
            resId = R.drawable.ic_charging;
        } else if (quantity <= 20) {
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
        tv.setVisibility(VISIBLE);
        String text = quantity + "%";
        tv.setText(text);
        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, resId, 0, 0);
    }


    private String getTipText(BleScanMessage bleScanMessage) {
        if (null == bleScanMessage) return "";
        ProductMessage.DeviceBean deviceBean = ProductCacheManager.getInstance().getDeviceMessageModify(getContext(), bleScanMessage.getVid(), bleScanMessage.getUid(), bleScanMessage.getPid());
        boolean isSupportCancelPair = deviceBean != null && (deviceBean.getHasCancelPair() == 1);
        boolean isHeadsetProduct = UIHelper.isHeadsetByDeviceType(bleScanMessage.getDeviceType());
        String tips;
        if (isSupportCancelPair) {
            tips = isHeadsetProduct ? getContext().getString(R.string.long_click_cancel_pair) :
                    getContext().getString(R.string.long_click_cancel_pair_soundbox);
        } else {
            tips = isHeadsetProduct ? getContext().getString(R.string.make_sure_headset_paired) :
                    getContext().getString(R.string.make_sure_soundbox_paired);
        }
        return tips;
    }

    private final BTRcspEventCallback mBtEventCallback = new BTRcspEventCallback() {
        private long lastTime = 0;

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            super.onConnection(device, status);
            if (status != StateCode.CONNECTION_OK || null == bleScanMessage) return;
            if (bleScanMessage.getDeviceType() == JL_DeviceType.JL_DEVICE_TYPE_CHARGING_BIN) {
                bleScanMessage.setAction(ProductAction.DEVICE_ACTION_CONNECTED);
                refreshView(bleScanMessage);
            }
        }

        @Override
        public void onShowDialog(BluetoothDevice device, BleScanMessage message) {
            //不是同一个设备
            if (!message.baseEquals(bleScanMessage)) {
//                JL_Log.v(tag, " -------不同设备--------");
                return;
            }
            //设备信息没有变化
            if (UIHelper.compareBleScanMessage(DevicePopDialogView.this.bleScanMessage, message)) {
                JL_Log.v(tag, "onShowDialog", " -------设备信息没有变化--------");
                //TODO:连接成功状态，小机广播与系统回调状态不一致，就有问题
                if (message.getAction() != ProductAction.DEVICE_ACTION_CONNECTED || (System.currentTimeMillis() - lastTime) <= 3 * 1000) {
                    return;
                }
            }
            JL_Log.d(tag, "onShowDialog", "update dialog by broadcast--> " + message);
            lastTime = System.currentTimeMillis();
            DevicePopDialogView.this.bleScanMessage = message;
            refreshView(message);
        }

        @Override
        public void onDeviceCommand(BluetoothDevice device, CommandBase cmd) {
            if (device == null || cmd.getId() != Command.CMD_ADV_DEVICE_NOTIFY) return;
            JL_Log.i(tag, "onShowDialog", "update dialog by command-->" + cmd);
            NotifyAdvInfoCmd notifyAdvInfoCmd = (NotifyAdvInfoCmd) cmd;
            NotifyAdvInfoParam advInfo = notifyAdvInfoCmd.getParam();
            BleScanMessage message = UIHelper.convertBleScanMsgFromNotifyADVInfo(advInfo);
            message.setConnectWay(BluetoothUtil.getDeviceProtocol(mRCSPController.getBluetoothManager().getBluetoothOption(), device));
            onShowDialog(device, message);
        }
    };
}
