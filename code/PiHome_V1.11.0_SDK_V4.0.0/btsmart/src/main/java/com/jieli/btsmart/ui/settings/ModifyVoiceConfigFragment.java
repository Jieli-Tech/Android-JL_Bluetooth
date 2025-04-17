package com.jieli.btsmart.ui.settings;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.ui.widget.InputTextDialog;
import com.jieli.btsmart.ui.widget.RulerView;
import com.jieli.component.utils.ToastUtil;

public class ModifyVoiceConfigFragment extends DeviceControlFragment implements DevicePopDialogFilter.IgnoreFilter {

    private ModifyVoiceConfigViewModel mViewModel;
    private TextView tvTopTitle;
    private ConstraintLayout clLeft;
    private TextView tvLeftTips;
    private TextView tvLeftValue;
    private RulerView rulerLeft;
    private ImageView ivLeftInput;
    private ConstraintLayout clRight;
    private TextView tvRightTips;
    private TextView tvRightValue;
    private RulerView rulerRight;
    private ImageView ivRightInput;

    private InputTextDialog mInputLeftTextDialog;
    private InputTextDialog mInputRightTextDialog;

    public final static String KEY_VOICE_MODE = "voice_mode";

    public static ModifyVoiceConfigFragment newInstance() {
        return new ModifyVoiceConfigFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_modify_voice_config, container, false);
        tvTopTitle = requireActivity().findViewById(R.id.tv_content_title);
        clLeft = view.findViewById(R.id.cl_modify_voice_left);
        tvLeftTips = view.findViewById(R.id.tv_modify_voice_left);
        tvLeftValue = view.findViewById(R.id.tv_modify_voice_left_value);
        rulerLeft = view.findViewById(R.id.ruler_modify_voice_left);
        ivLeftInput = view.findViewById(R.id.iv_modify_voice_edit_left);
        clRight = view.findViewById(R.id.cl_modify_voice_right);
        tvRightTips = view.findViewById(R.id.tv_modify_voice_right);
        tvRightValue = view.findViewById(R.id.tv_modify_voice_right_value);
        rulerRight = view.findViewById(R.id.ruler_modify_voice_right);
        ivRightInput = view.findViewById(R.id.iv_modify_voice_edit_right);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TextView tvRightTop = requireActivity().findViewById(R.id.tv_content_right);
        tvRightTop.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        tvRightTop.setVisibility(View.VISIBLE);
        tvRightTop.setText(R.string.save);
        tvRightTop.setOnClickListener(v -> saveConfigure());
        ivLeftInput.setOnClickListener(v -> showInputLeftTextDialog());
        ivRightInput.setOnClickListener(v -> showInputRightTextDialog());
        rulerLeft.setOnChooseResulterListener(new RulerView.OnChooseResulterListener() {
            @Override
            public void onEndResult(String result) {
                updateLeftRulerValue(result);
            }

            @Override
            public void onScrollResult(String result) {
                updateLeftRulerValue(result);
            }
        });
        rulerRight.setOnChooseResulterListener(new RulerView.OnChooseResulterListener() {
            @Override
            public void onEndResult(String result) {
                updateRightRulerValue(result);
            }

            @Override
            public void onScrollResult(String result) {
                updateRightRulerValue(result);
            }
        });
        mViewModel = new ViewModelProvider(this).get(ModifyVoiceConfigViewModel.class);
        if (getArguments() != null) {
            mViewModel.mVoiceMode = getArguments().getParcelable(KEY_VOICE_MODE);
        }
        if (mViewModel.mVoiceMode == null) {
            mViewModel.getCurrentVoiceMode();
        } else {
            updateVoiceMode(mViewModel.mVoiceMode);
        }
        mViewModel.mDevConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() == StateCode.CONNECTION_DISCONNECT) {
                requireActivity().finish();
            }
        });
        mViewModel.mVoiceModeMLD.observe(getViewLifecycleOwner(), this::updateVoiceMode);
        mViewModel.mSendResultMLD.observe(getViewLifecycleOwner(), aBoolean -> {
            if (aBoolean) {
                requireActivity().finish();
            } else {
                ToastUtil.showToastShort(R.string.settings_failed);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissInputLeftTextDialog();
        dismissInputRightTextDialog();
    }

    private void updateVoiceMode(VoiceMode mode) {
        RCSPController rcspController = mViewModel.getRCSPController();
        BluetoothDevice device = rcspController.getUsingDevice();
        HistoryBluetoothDevice historyBluetoothDevice = rcspController.findHistoryBluetoothDevice(device);
        int advVersion = historyBluetoothDevice.getAdvVersion();
        JL_Log.w(TAG, "updateVoiceMode ><>> mode = " + mode);
        if (isDetached() || !isAdded() || mode == null) return;
        String title, leftTitle, rightTitle = "";
        switch (mode.getMode()) {
            case VoiceMode.VOICE_MODE_CLOSE:
                ToastUtil.showToastShort(R.string.invalid_mode);
                requireActivity().finish();
                return;
            case VoiceMode.VOICE_MODE_DENOISE:
                title = getString(R.string.denoise_value);
                if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {
                    leftTitle = title;
                    break;
                }
                leftTitle = getString(R.string.left_dev_denoise_value);
                rightTitle = getString(R.string.right_dev_denoise_value);
                break;
            case VoiceMode.VOICE_MODE_TRANSPARENT:
                title = getString(R.string.transparent_value);
                if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {
                    leftTitle = title;
                    break;
                }
                leftTitle = getString(R.string.left_dev_transparent_value);
                rightTitle = getString(R.string.right_dev_transparent_value);
                break;
            default:
                ToastUtil.showToastShort(R.string.unknown_mode);
                requireActivity().finish();
                return;
        }
        if (tvTopTitle != null) tvTopTitle.setText(title);
        clLeft.setVisibility(mode.getLeftMax() > 0 ? View.VISIBLE : View.GONE);
        if (mode.getLeftMax() > 0) {
            tvLeftTips.setText(leftTitle);
            tvLeftValue.setText(String.valueOf(mode.getLeftCurVal()));
            //必须切到整百的位置
            rulerLeft.setScaleLimit(400/*(int) Math.round(mode.getLeftMax() / 16.0)*/);
            float max = mode.getLeftMax();
            double multiple = Math.ceil(max / 200);
            max = (float) (multiple * 200);
            float value = mode.getLeftCurVal();
            multiple = Math.ceil(value / 200);
            value = (float) (multiple * 200);
            rulerLeft.setScale(0, max, value);
        }
        if (advVersion != SConstant.ADV_INFO_VERSION_NECK_HEADSET) {
            clRight.setVisibility(mode.getRightMax() > 0 ? View.VISIBLE : View.GONE);
            if (mode.getRightMax() > 0) {
                tvRightTips.setText(rightTitle);
                tvRightValue.setText(String.valueOf(mode.getRightCurVal()));
                rulerRight.setScaleLimit(400/*(int) Math.round(mode.getRightMax() / 16.0)*/);
                float max = mode.getRightMax();
                double multiple = Math.ceil(max / 200);
                max = (float) (multiple * 200);
                float value = mode.getRightCurVal();
                multiple = Math.ceil(value / 200);
                value = (float) (multiple * 200);
                rulerRight.setScale(0, max, value);
            }
        }
    }

    private void showInputLeftTextDialog() {
        if (isDetached() || !isAdded()) return;
        if (null == mInputLeftTextDialog) {
            mInputLeftTextDialog = new InputTextDialog.Builder()
                    .setCancelable(false)
                    .setTitle(tvLeftTips.getText().toString())
                    .setWidth(0.8f)
                    .setLeftText(getString(R.string.cancel))
                    .setLeftColor(getResources().getColor(R.color.gray_text_989898))
                    .setRightText(getString(R.string.confirm))
                    .setRightColor(getResources().getColor(R.color.blue_448eff))
                    .setInputText(tvLeftValue.getText().toString())
                    .setEditTextType(InputTextDialog.EditTextType.EDIT_TEXT_TYPE_NUMBER)
                    .setOnInputTextListener(new InputTextDialog.OnInputTextListener() {
                        @Override
                        public void onDismiss(InputTextDialog dialog) {
                            dismissInputLeftTextDialog();
                        }

                        @Override
                        public void onInputText(InputTextDialog dialog, String text) {
                            if (!TextUtils.isEmpty(text) && TextUtils.isDigitsOnly(text)) {
                                float v = Float.parseFloat(text);
                                if (v < 0 || v > mViewModel.mVoiceMode.getLeftMax()) {
                                    ToastUtil.showToastShort(getString(R.string.input_valid_value_tips, 0, mViewModel.mVoiceMode.getLeftMax()));
                                }
                            }
                        }

                        @Override
                        public void onInputFinish(InputTextDialog dialog, String value, String lastValue) {
                            if (TextUtils.isEmpty(value) || !TextUtils.isDigitsOnly(value)) {
                                ToastUtil.showToastShort(getString(R.string.input_valid_value_tips, 0, mViewModel.mVoiceMode.getLeftMax()));
                                return;
                            }
                            dismissInputLeftTextDialog();
                            float v = Float.parseFloat(value);
                            double multiple = Math.ceil(v / 200);
                            v = (float) (multiple * 200);
                            int realVal = Math.round(v);
                            mViewModel.isLeftEdit = true;
                            rulerLeft.computeScrollTo(realVal);
                        }
                    }).create();
        }
        if (!mInputLeftTextDialog.isShow()) {
            dismissInputRightTextDialog();
            mInputLeftTextDialog.show(getChildFragmentManager(), "input_left_value");
        }
    }

    private void dismissInputLeftTextDialog() {
        if (isDetached() || !isAdded()) return;
        if (null != mInputLeftTextDialog) {
            if (mInputLeftTextDialog.isShow()) {
                mInputLeftTextDialog.dismiss();
            }
            mInputLeftTextDialog = null;
        }
    }

    private void showInputRightTextDialog() {
        if (isDetached() || !isAdded()) return;
        if (null == mInputRightTextDialog) {
            mInputRightTextDialog = new InputTextDialog.Builder()
                    .setCancelable(false)
                    .setTitle(tvRightTips.getText().toString())
                    .setWidth(0.8f)
                    .setLeftText(getString(R.string.cancel))
                    .setLeftColor(getResources().getColor(R.color.gray_text_989898))
                    .setRightText(getString(R.string.confirm))
                    .setRightColor(getResources().getColor(R.color.blue_448eff))
                    .setInputText(tvRightValue.getText().toString())
                    .setEditTextType(InputTextDialog.EditTextType.EDIT_TEXT_TYPE_NUMBER)
                    .setOnInputTextListener(new InputTextDialog.OnInputTextListener() {
                        @Override
                        public void onDismiss(InputTextDialog dialog) {
                            dismissInputRightTextDialog();
                        }

                        @Override
                        public void onInputText(InputTextDialog dialog, String text) {
                            if (!TextUtils.isEmpty(text) && TextUtils.isDigitsOnly(text)) {
                                float v = Float.parseFloat(text);
                                if (v < 0 || v > mViewModel.mVoiceMode.getRightMax()) {
                                    ToastUtil.showToastShort(getString(R.string.input_valid_value_tips, 0, mViewModel.mVoiceMode.getRightMax()));
                                }
                            }
                        }

                        @Override
                        public void onInputFinish(InputTextDialog dialog, String value, String lastValue) {
                            dismissInputRightTextDialog();
                            float v = Float.parseFloat(value);
                            double multiple = Math.ceil(v / 200);
                            v = (float) (multiple * 200);
                            int realVal = Math.round(v);
                            mViewModel.isRightEdit = true;
                            rulerRight.computeScrollTo(realVal);
                        }
                    }).create();
        }
        if (!mInputRightTextDialog.isShow()) {
            dismissInputLeftTextDialog();
            mInputRightTextDialog.show(getChildFragmentManager(), "input_right_value");
        }
    }

    private void dismissInputRightTextDialog() {
        if (isDetached() || !isAdded()) return;
        if (null != mInputRightTextDialog) {
            if (mInputRightTextDialog.isShow()) {
                mInputRightTextDialog.dismiss();
            }
            mInputRightTextDialog = null;
        }
    }

    private void saveConfigure() {
        String leftValue = tvLeftValue.getText().toString().trim();
        String rightValue = tvRightValue.getText().toString().trim();
        int newLeftValue = TextUtils.isEmpty(leftValue) ? 0 : Integer.parseInt(leftValue);
        int newRightValue = TextUtils.isEmpty(rightValue) ? 0 : Integer.parseInt(rightValue);
        if (mViewModel.mVoiceMode != null) {
            boolean isChange = false;
            if (newLeftValue != mViewModel.mVoiceMode.getLeftCurVal()) {
                mViewModel.mVoiceMode.setLeftCurVal(newLeftValue);
                isChange = true;
            }
            if (newRightValue != mViewModel.mVoiceMode.getRightCurVal()) {
                mViewModel.mVoiceMode.setRightCurVal(newRightValue);
                isChange = true;
            }
            if (isChange) {
                if (!SConstant.TEST_ANC_FUNC) {
//                    mViewModel.mVoiceMode.setLeftMax(16344);
//                    mViewModel.mVoiceMode.setRightMax(16144);
                    mViewModel.setCurrentVoiceMode(mViewModel.mVoiceMode);
                    return;
                }
            }
            exit();
        }
    }
    
    private void updateLeftRulerValue(String value){
        int realVal;
        if (mViewModel.isLeftEdit) {
            mViewModel.isLeftEdit = false;
            realVal = Math.round(Float.parseFloat(value));
        } else {
            realVal = Math.round(Float.parseFloat(value) / 100) * 100;
        }
//                JL_Log.d(TAG, "[rulerLeft][onScrollResult] >>> realVal=" + realVal + ", LeftMax = " + mViewModel.mVoiceMode.getLeftMax());
        if (realVal > mViewModel.mVoiceMode.getLeftMax()) {
            realVal = mViewModel.mVoiceMode.getLeftMax();
            ToastUtil.showToastShort(R.string.maximum_tips);
        }
        tvLeftValue.setText(String.valueOf(realVal));
    }

    private void updateRightRulerValue(String value){
        int realVal;
        if (mViewModel.isRightEdit) {
            mViewModel.isRightEdit = false;
            realVal = Math.round(Float.parseFloat(value));
        } else {
            realVal = Math.round(Float.parseFloat(value) / 100) * 100;
        }
//                JL_Log.d(TAG, "[rulerRight][onEndResult] >>> realVal=" + realVal + ", RightMax = " + mViewModel.mVoiceMode.getRightMax());
        if (realVal > mViewModel.mVoiceMode.getRightMax()) {
            realVal = mViewModel.mVoiceMode.getRightMax();
            ToastUtil.showToastShort(R.string.maximum_tips);
        }
        tvRightValue.setText(String.valueOf(realVal));
    }

    private void exit() {
        Intent intent = new Intent();
        intent.putExtra(KEY_VOICE_MODE, mViewModel.mVoiceMode);
        requireActivity().setResult(Activity.RESULT_OK, intent);
        requireActivity().finish();
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }
}