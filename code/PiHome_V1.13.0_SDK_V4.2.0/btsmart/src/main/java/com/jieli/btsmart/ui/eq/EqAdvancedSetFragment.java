package com.jieli.btsmart.ui.eq;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Group;

import com.jieli.bluetooth.bean.device.eq.DynamicLimiterParam;
import com.jieli.bluetooth.bean.device.eq.ReverberationParam;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.widget.RotatingView;
import com.kyleduo.switchbutton.SwitchButton;

import static com.jieli.btsmart.tool.bluetooth.BTEventCallbackManager.MASK_DYNAMIC_LIMITER;
import static com.jieli.btsmart.tool.bluetooth.BTEventCallbackManager.MASK_REVERBERATION;

/**
 * EqFragment
 * @author zqjasonZhong
 * @since 2025/5/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 调节EQ页面
 */
public class EqAdvancedSetFragment extends BaseFragment implements RotatingView.OnValueChangeListener {
    private SwitchButton switchButtonDynamic;
    private SwitchButton switchButtonReverberation;
    private RotatingView rotateDepth;
    private RotatingView rotateStrength;
    private RotatingView rotateDynamicLimiter;
    private Group groupReverberation;
    private Group groupDynamicLis;

    private final int DEFAULT_SUPPORT_MASK = 0;
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final int MAX_DEPTH_VALUE = 100;
    private final int MIN_DEPTH_VALUE = 0;
    private final int MAX_STRENGTH_VALUE = 100;
    private final int MIN_STRENGTH_VALUE = 0;
    private final int MAX_DYNAMIC_LIMITER_VALUE = 0;
    private final int MIN_DYNAMIC_LIMITER_VALUE = -60;
    private int mSupportMask = DEFAULT_SUPPORT_MASK;
    private Integer mReverberationSwitchState = null;

    private static final int SWITCH_STATE_OPEN = 1;
    private static final int SWITCH_STATE_CLOSE = 0;

    public static EqAdvancedSetFragment newInstance() {
        return new EqAdvancedSetFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_eq_advanced_set, container, false);
        switchButtonDynamic = view.findViewById(R.id.sw_eq_dynamic_limiter);
        switchButtonReverberation = view.findViewById(R.id.sw_eq_advanced_reverberation);
        rotateDepth = view.findViewById(R.id.rotate_depth);
        rotateStrength = view.findViewById(R.id.rotate_strength);
        rotateDynamicLimiter = view.findViewById(R.id.rotate_dynamic_limiter);
        groupReverberation = view.findViewById(R.id.group_reverberation);
        groupDynamicLis = view.findViewById(R.id.group_dynamic_limiter);

        switchButtonDynamic.setOnClickListener(mOnClickListener);
        switchButtonReverberation.setOnClickListener(mOnClickListener);
        if (getActivity() instanceof CommonActivity) {
            final CommonActivity activity = (CommonActivity) getActivity();
            activity.updateTopBar(getString(R.string.eq_advanced_setting), R.drawable.ic_back_black, v -> activity.onBackPressed(), 0, null);
        }
        rotateDepth.setOnValueChangeListener(this);
        rotateStrength.setOnValueChangeListener(this);
        rotateDynamicLimiter.setOnValueChangeListener(this);
        rotateDepth.setValue(MIN_DEPTH_VALUE, MAX_DEPTH_VALUE, MIN_DEPTH_VALUE);
        rotateStrength.setValue(MIN_STRENGTH_VALUE, MAX_STRENGTH_VALUE, MIN_STRENGTH_VALUE);
        rotateDynamicLimiter.setValue(MIN_DYNAMIC_LIMITER_VALUE, MAX_DYNAMIC_LIMITER_VALUE, MIN_DYNAMIC_LIMITER_VALUE);
        mRCSPController.getExpandDataInfo(mRCSPController.getUsingDevice(), null);
        return view;
    }

    @Override
    public void onDestroy() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onDestroy();
    }

    @Override
    public void change(RotatingView view, int value, boolean end) {
        if (!end) return;
        if (view == rotateDynamicLimiter) {
            if ((MASK_DYNAMIC_LIMITER & mSupportMask) != MASK_DYNAMIC_LIMITER) return;
            sendDynamicLimiter();
        } else if (view == rotateDepth || view == rotateStrength) {
            if ((MASK_REVERBERATION & mSupportMask) != MASK_REVERBERATION) return;
            mReverberationSwitchState = 1;
            sendReverberationData();
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == switchButtonReverberation) {
                if ((MASK_REVERBERATION & mSupportMask) != MASK_REVERBERATION) {
                    ((SwitchButton) v).setChecked(false);
                    showTips(getString(R.string.no_support_reverberation));
                    return;
                }
                boolean isOpen = mReverberationSwitchState != SWITCH_STATE_OPEN;
                changeRotateViewStyle(rotateDepth, isOpen);
                changeRotateViewStyle(rotateStrength, isOpen);
                mReverberationSwitchState = mReverberationSwitchState == SWITCH_STATE_OPEN ? SWITCH_STATE_CLOSE : SWITCH_STATE_OPEN;
                sendReverberationData();
            } else if (v == switchButtonDynamic) {

            }
        }
    };

    private void sendReverberationData() {
        ReverberationParam param = new ReverberationParam(mReverberationSwitchState == 1, rotateDepth.getValue(), rotateStrength.getValue());
        mRCSPController.setReverberationParameter(mRCSPController.getUsingDevice(), param, null);
    }

    private void sendDynamicLimiter() {
        DynamicLimiterParam param = new DynamicLimiterParam(rotateDynamicLimiter.getValue());
        mRCSPController.setDynamicLimiterParameter(mRCSPController.getUsingDevice(), param, null);
    }

    private void showReverberationView(ReverberationParam param) {
        if (null == param) return;
        rotateDepth.setClickable(true);
        rotateStrength.setClickable(true);
        switchButtonReverberation.setClickable(true);
        groupReverberation.setVisibility(View.VISIBLE);
        updateReverberationView(param);
    }

    private void showDynamicView(DynamicLimiterParam param) {
        if (null == param) return;
        switchButtonDynamic.setClickable(true);
        groupDynamicLis.setVisibility(View.VISIBLE);
        updateDynamicView(param);
    }

    private void changeRotateViewStyle(RotatingView view, boolean isOpen) {
        view.setContentStartColor(isOpen ? R.color.color_rotating_view_start : R.color.gray_CECECE);
        view.setContentEndColor(isOpen ? R.color.color_rotating_view_end : R.color.gray_CECECE);
        view.setContentTextColor(isOpen ? R.color.black_242424 : R.color.gray_CECECE);
        view.setIndicatorImage(isOpen ? R.drawable.ic_rotatview_indicator_big_sup : R.drawable.ic_rotatview_indicator_big_nol);
        view.setClickable(isOpen);
        view.invalidate();
    }

    private void updateReverberationView(ReverberationParam param) {
        mReverberationSwitchState = param.isOn() ? 1 : 0;
        boolean openState = mReverberationSwitchState == 1;
        switchButtonReverberation.setChecked(openState);
        changeRotateViewStyle(rotateDepth, openState);
        changeRotateViewStyle(rotateStrength, openState);
        rotateDepth.setValue(MIN_DEPTH_VALUE, MAX_DEPTH_VALUE, param.getDepthValue());
        rotateStrength.setValue(MIN_STRENGTH_VALUE, MAX_STRENGTH_VALUE, param.getStrengthValue());
    }

    private void updateDynamicView(DynamicLimiterParam param) {
        switchButtonDynamic.setChecked(true);
        changeRotateViewStyle(rotateDynamicLimiter, true);
        int value = param.getValue();
        if (value <= 0) {
            rotateDynamicLimiter.setValue(MIN_DYNAMIC_LIMITER_VALUE, MAX_DYNAMIC_LIMITER_VALUE, value);
        }
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {
        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            if (!isAdded() || isDetached()) return;
            back();
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (!isAdded() || isDetached()) return;
            if ((status == StateCode.CONNECTION_DISCONNECT || status == StateCode.CONNECTION_FAILED) && !mRCSPController.isDeviceConnected()) {
                back();
            }
        }

        @Override
        public void onReverberation(BluetoothDevice device, ReverberationParam param) {
            if (!isAdded() || isDetached()) return;
            showReverberationView(param);
            mSupportMask = mSupportMask | MASK_REVERBERATION;
        }

        @Override
        public void onDynamicLimiter(BluetoothDevice device, DynamicLimiterParam param) {
            if (!isAdded() || isDetached()) return;
            showDynamicView(param);
            mSupportMask = mSupportMask | MASK_DYNAMIC_LIMITER;
        }
    };
}
