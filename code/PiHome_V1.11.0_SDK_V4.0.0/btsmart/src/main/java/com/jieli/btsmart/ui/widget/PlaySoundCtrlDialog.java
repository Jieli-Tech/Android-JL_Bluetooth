package com.jieli.btsmart.ui.widget;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.ui.base.BaseDialogFragment;
import com.jieli.btsmart.ui.widget.color_cardview.CardView;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.jl_http.bean.ProductModel;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
import static com.jieli.jl_http.bean.ProductModel.MODEL_PRODUCT_LOGO;

/**
 * 播放查找设备音频控制弹窗
 *
 * @author zqjasonZhong
 * @since 2020/11/4
 */
public class PlaySoundCtrlDialog extends BaseDialogFragment {
    private TextView tvAddress;
    private TextView tvUpdateTime;

    private CardView cvLeftDevCtrl;
    private ImageView ivLeftDev;
    private TextView tvLeftConnection;
    private TextView tvLeftPlaySound;

    private CardView cvRightDevCtrl;
    private ImageView ivRightDev;
    private TextView tvRightConnection;
    private TextView tvRightPlaySound;

    private String mTargetAddress;
    private IPlaySoundOp mPlaySoundOp;
    private OnPlaySoundCtrlDialogListener mOnPlaySoundCtrlDialogListener;
    private int playWay = Constants.RING_WAY_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //设置dialog的基本样式参数
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = requireDialog().getWindow();
        if (window != null) {
            //去掉dialog默认的padding
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            //设置dialog的位置在底部
            lp.gravity = Gravity.BOTTOM;
            //去除变透明阴影
            lp.dimAmount = 0.0f;
            //设置dialog的动画
            lp.windowAnimations = R.style.BottomToTopAnim;
            window.setAttributes(lp);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View view = inflater.inflate(R.layout.dialog_play_sound_ctrl, container, false);
        tvAddress = view.findViewById(R.id.tv_dialog_play_sound_address);
        tvUpdateTime = view.findViewById(R.id.tv_dialog_play_sound_update_time);

        cvLeftDevCtrl = view.findViewById(R.id.cv_dialog_play_sound_left_ctrl);
        ivLeftDev = view.findViewById(R.id.iv_dialog_play_sound_left_dev);
        tvLeftConnection = view.findViewById(R.id.tv_dialog_play_sound_left_connection);
        tvLeftPlaySound = view.findViewById(R.id.tv_dialog_play_sound_left_play);

        cvRightDevCtrl = view.findViewById(R.id.cv_dialog_play_sound_right_ctrl);
        ivRightDev = view.findViewById(R.id.iv_dialog_play_sound_right_dev);
        tvRightConnection = view.findViewById(R.id.tv_dialog_play_sound_right_connection);
        tvRightPlaySound = view.findViewById(R.id.tv_dialog_play_sound_right_play);

        cvLeftDevCtrl.setOnClickListener(mOnClickListener);
        cvRightDevCtrl.setOnClickListener(mOnClickListener);

        updateHistoryDeviceUI();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mOnPlaySoundCtrlDialogListener != null) {
            mOnPlaySoundCtrlDialogListener.onDismissDialog(this);
        }
        setTargetDevAddress(null);
        setPlaySoundOp(null);
        setOnPlaySoundCtrlDialogListener(null);
    }

    public PlaySoundCtrlDialog setTargetDevAddress(String address) {
        mTargetAddress = address;
        return this;
    }

    public PlaySoundCtrlDialog setPlaySoundOp(IPlaySoundOp op) {
        mPlaySoundOp = op;
        return this;
    }

    public PlaySoundCtrlDialog setOnPlaySoundCtrlDialogListener(OnPlaySoundCtrlDialogListener onPlaySoundCtrlDialogListener) {
        mOnPlaySoundCtrlDialogListener = onPlaySoundCtrlDialogListener;
        return this;
    }

    public PlaySoundCtrlDialog setPlayWay(int playWay) {
        this.playWay = playWay;
        return this;
    }

    public void updateDeviceGpsUI(String location, String timeStr) {
        if (!isAdded() || isDetached()) return;
        if (location == null || !location.equals(tvAddress.getText().toString().trim())) {
            tvAddress.setText(location);
        }
        tvUpdateTime.setText(timeStr);
    }

    public void onDeviceDisconnected(BluetoothDevice device) {
        if (checkIsTargetDev(device)) {
            dismiss();
        }
    }

    public void onTwsChange(BluetoothDevice device, boolean isTwsConnected) {
        if (checkIsTargetDev(device)) {
            updateHistoryDeviceUI();
        }
    }

    public void onPlaySoundStatus(BluetoothDevice device, int way, boolean isPlaying) {
        if (checkIsTargetDev(device)) {
            playWay = way;
            updateHistoryDeviceUI();
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == cvLeftDevCtrl) {
                if (mOnPlaySoundCtrlDialogListener != null) {
                    mOnPlaySoundCtrlDialogListener.onLeftDevPlaySound(mTargetAddress);
                }
            } else if (v == cvRightDevCtrl) {
                if (mOnPlaySoundCtrlDialogListener != null) {
                    mOnPlaySoundCtrlDialogListener.onRightDevPlaySound(mTargetAddress);
                }
            }
        }
    };

    private boolean checkIsTargetDev(BluetoothDevice device) {
        return device != null && BluetoothAdapter.checkBluetoothAddress(mTargetAddress) && mTargetAddress.equals(device.getAddress());
    }

    private void updateImageView(ImageView imageView, boolean isGif, String url, int failResId) {
        if (isAdded() && !isDetached() && imageView != null) {
            if (failResId <= 0) {
                failResId = R.drawable.ic_default_product_design;
            }
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .override(SIZE_ORIGINAL)
                    .fallback(failResId);
            if (isGif) {
                Glide.with(MainApplication.getApplication())
                        .asGif()
                        .apply(options)
                        .load(url)
                        .into(imageView);
            } else {
                Glide.with(MainApplication.getApplication())
                        .asBitmap()
                        .apply(options)
                        .load(url)
                        .into(imageView);
            }
        }
    }

    private void updateDeviceUI(ImageView imageView, HistoryBluetoothDevice history, String scene) {
        if (imageView == null || history == null || scene == null) return;
        String imgUrl = ProductUtil.findCacheDesign(getContext(), history.getVid(), history.getUid(), history.getPid(), scene);
        boolean isGif = ProductUtil.isGifFile(imgUrl);
        int failResId = DefaultResFactory.createBySdkType(history.getChipType(), history.getAdvVersion()).getLogoImg();
//        UIHelper.isHeadsetType(history.getChipType()) ? R.drawable.ic_tws_headset : R.drawable.ic_default_product_design;
        if (UIHelper.isHeadsetType(history.getChipType()) && !scene.equals(MODEL_PRODUCT_LOGO.getValue())) {
            if (scene.equals(ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue()) || scene.equals(ProductModel.MODEL_DEVICE_LEFT_CONNECTED.getValue())) {
                failResId = DefaultResFactory.createBySdkType(history.getChipType(), history.getAdvVersion()).getLeftImg();
            } else if (scene.equals(ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue()) || scene.equals(ProductModel.MODEL_DEVICE_RIGHT_CONNECTED.getValue())) {
                failResId = DefaultResFactory.createBySdkType(history.getChipType(), history.getAdvVersion()).getRightImg();
            }
        }
        updateImageView(imageView, isGif, imgUrl, failResId);
    }

    private void updateDeviceStatusUI(boolean isDevConnected, boolean isPlayingSound, CardView cardView, TextView tvConnection, TextView tvPlaySound) {
        if (!isAdded() || isDetached()) return;
        if (cardView == null || tvConnection == null || tvPlaySound == null) return;
        if (isDevConnected) {
            cardView.setClickable(true);
            tvConnection.setText(getString(R.string.device_status_connected));
            tvPlaySound.setTextColor(getResources().getColor(R.color.color_main));
            if (isPlayingSound) {
                cardView.setCardBackgroundColor(getResources().getColor(R.color.color_main));
                tvConnection.setTextColor(getResources().getColor(R.color.white_ffffff));
                tvPlaySound.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_playing_sound_white, 0, 0, 0);
                tvPlaySound.setText("");
            } else {
                cardView.setCardBackgroundColor(getResources().getColor(R.color.white_ffffff));
                tvConnection.setTextColor(getResources().getColor(R.color.black_242424));
                tvPlaySound.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                tvPlaySound.setText(getString(R.string.play_sound));
            }
        } else {
            cardView.setCardBackgroundColor(getResources().getColor(R.color.white_ffffff));
            cardView.setClickable(false);
            tvConnection.setText(getString(R.string.device_status_unconnected));
            tvConnection.setTextColor(getResources().getColor(R.color.gray_959595));
            tvPlaySound.setText(getString(R.string.play_sound));
            tvPlaySound.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            tvPlaySound.setTextColor(getResources().getColor(R.color.gray_959595));
        }
    }

    private boolean isPlayingSound(int way) {
        if (mPlaySoundOp == null || !BluetoothAdapter.checkBluetoothAddress(mTargetAddress))
            return false;
        return mPlaySoundOp.checkDeviceIsConnected(mTargetAddress) && mPlaySoundOp.isSoundPlaying(mTargetAddress)
                && (playWay == Constants.RING_WAY_ALL || playWay == way);
    }

    private void updateDeviceStatus(boolean leftDevConnected, boolean rightDevConnect) {
        updateDeviceStatusUI(leftDevConnected, isPlayingSound(Constants.RING_WAY_LEFT), cvLeftDevCtrl, tvLeftConnection, tvLeftPlaySound);
        updateDeviceStatusUI(rightDevConnect, isPlayingSound(Constants.RING_WAY_RIGHT), cvRightDevCtrl, tvRightConnection, tvRightPlaySound);
    }

    private void updateHistoryDeviceUI() {
        if (!isAdded() || isDetached()) return;
        if (mPlaySoundOp != null) {
            if (mPlaySoundOp.checkDeviceIsConnected(mTargetAddress)) {
                HistoryBluetoothDevice history = mPlaySoundOp.getHistoryDevice(mTargetAddress);
                if (history != null) {
                    updateDeviceUI(ivLeftDev, history, ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue());
                    updateDeviceUI(ivRightDev, history, ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue());
                    ADVInfoResponse advInfo = DeviceStatusManager.getInstance().getAdvInfo(BluetoothUtil.getRemoteDevice(history.getAddress()));
                    if (advInfo != null) {
                        updateDeviceStatus(advInfo.getLeftDeviceQuantity() > 0, advInfo.getRightDeviceQuantity() > 0);
                        return;
                    }
                }
            }
        }
        dismiss();
    }

    public interface IPlaySoundOp {

        HistoryBluetoothDevice getHistoryDevice(String address);

        boolean checkDeviceIsConnected(String address);

        boolean isSoundPlaying(String address);
    }

    public interface OnPlaySoundCtrlDialogListener {

        void onLeftDevPlaySound(String address);

        void onRightDevPlaySound(String address);

        void onDismissDialog(DialogFragment dialog);
    }
}
