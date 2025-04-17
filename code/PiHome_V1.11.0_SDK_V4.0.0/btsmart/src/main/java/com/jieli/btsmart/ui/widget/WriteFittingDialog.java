package com.jieli.btsmart.ui.widget;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jieli.btsmart.R;

/**
 * 辅听写入确认弹窗
 *
 * @author zqjasonZhong
 * @date 2019/7/31
 */
public class WriteFittingDialog extends DialogFragment {

    private boolean isShow;
    private Builder mBuilder;

    private TextView mTitleTv;
    private EditText mEditText;
    private TextView mInputTipsTv;
    private View mHorizontalDivider;
    private View mVerticalDivider;
    private TextView mLeftTv;
    private TextView mRightTv;
    private TextView mSaveTv;
    private boolean mIsSave = true;

    public WriteFittingDialog() {

    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams mLayoutParams = window.getAttributes();
        mLayoutParams.dimAmount = 0.5f;
        mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        float width = mBuilder == null ? 0 : mBuilder.getWidth();
        float height = mBuilder == null ? 0 : mBuilder.getHeight();
        mLayoutParams.width = width <= 0 ? WindowManager.LayoutParams.WRAP_CONTENT : (int) (width * getScreenWidth());
        mLayoutParams.height =/* height <= 0 ?*/ WindowManager.LayoutParams.WRAP_CONTENT /*: (int) (height * getScreenHeight())*/;

        window.getDecorView().getRootView().setBackgroundColor(Color.TRANSPARENT);
        window.setAttributes(mLayoutParams);
        setCancelable(mBuilder != null && mBuilder.isCancelable());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_write_fitting, container);
        initView(view);
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateEditText();
        updateDialog();
    }

    public boolean isShow() {
        return isShow;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        isShow = true;
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void dismiss() {
        super.dismissAllowingStateLoss();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        isShow = false;
        if (mBuilder != null && mBuilder.getOnInputTextListener() != null) {
            mBuilder.getOnInputTextListener().onDismiss(this);
        }
        super.onDismiss(dialog);
    }


    private void initView(View view) {
        mTitleTv = view.findViewById(R.id.dialog_input_text_title_tv);
        mEditText = view.findViewById(R.id.dialog_input_text_edit);
        mInputTipsTv = view.findViewById(R.id.dialog_input_text_tip_tv);
        mHorizontalDivider = view.findViewById(R.id.dialog_input_text_horizontal_divide);
        mVerticalDivider = view.findViewById(R.id.dialog_input_text_vertical_divide);
        mLeftTv = view.findViewById(R.id.dialog_input_text_left_tv);
        mRightTv = view.findViewById(R.id.dialog_input_text_right_tv);
        mSaveTv = view.findViewById(R.id.tv_save_fitting_record);

        mEditText.addTextChangedListener(new CustomTextWatcher());
        mLeftTv.setOnClickListener(mOnClickListener);
        mRightTv.setOnClickListener(mOnClickListener);
        mSaveTv.setOnClickListener(mOnClickListener);
    }

    public void updateEditText() {
        if (mEditText != null && mBuilder != null) {
            if (mBuilder.getEditTextType().equals(EditTextType.EDIT_TEXT_TYPE_NUMBER)) {
                mEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                mEditText.setKeyListener(new DigitsKeyListener(false, true));
            }
            String inputText = mBuilder.getInputText();
            if (!TextUtils.isEmpty(inputText)) {
                mEditText.setText(inputText);
            }
        }
    }

    public void updateDialog() {
        if (mBuilder != null) {
            if (mTitleTv != null && !TextUtils.isEmpty(mBuilder.getTitle())) {
                mTitleTv.setText(mBuilder.getTitle());
            }
            if (mSaveTv != null) {
                mSaveTv.setVisibility(mBuilder.isSaveViewVisible() ? View.VISIBLE : View.GONE);
            }
            if (mInputTipsTv != null) {
                String tips = mBuilder.getInputTips();
                if (TextUtils.isEmpty(tips)) {
                    mInputTipsTv.setVisibility(View.GONE);
                } else {
                    mInputTipsTv.setVisibility(View.VISIBLE);
                    mInputTipsTv.setText(tips);
                    if (mBuilder.getTipsColor() != 0) {
                        mInputTipsTv.setTextColor(mBuilder.getTipsColor());
                    }
                }
            }
            String leftText = mBuilder.getLeftText();
            String rightText = mBuilder.getRightText();
            if (!TextUtils.isEmpty(leftText) && !TextUtils.isEmpty(rightText)) {
                updateBottomLayout(leftText, View.VISIBLE, rightText, View.VISIBLE, View.VISIBLE, View.VISIBLE);
            } else if (TextUtils.isEmpty(rightText) && !TextUtils.isEmpty(leftText)) {
                updateBottomLayout(leftText, View.VISIBLE, rightText, View.GONE, View.VISIBLE, View.GONE);
            } else if (TextUtils.isEmpty(leftText) && !TextUtils.isEmpty(rightText)) {
                updateBottomLayout(leftText, View.GONE, rightText, View.VISIBLE, View.VISIBLE, View.GONE);
            } else {
                updateBottomLayout(leftText, View.GONE, rightText, View.GONE, View.GONE, View.GONE);
            }
        }
    }

    private void updateBottomLayout(String leftText, int lefVisibility, String rightText, int rightVisibility, int horizontalVisibility, int verticalVisibility) {
        if (mLeftTv != null) {
            mLeftTv.setVisibility(lefVisibility);
            if (lefVisibility == View.VISIBLE) {
                mLeftTv.setText(leftText);
                if (mBuilder.getLeftColor() != 0) {
                    mLeftTv.setTextColor(mBuilder.getLeftColor());
                }
            }
        }
        if (mRightTv != null) {
            mRightTv.setVisibility(rightVisibility);
            if (rightVisibility == View.VISIBLE) {
                mRightTv.setText(rightText);
                if (mBuilder.getRightColor() != 0) {
                    mRightTv.setTextColor(mBuilder.getRightColor());
                }
            }
        }
        if (mHorizontalDivider != null) {
            mHorizontalDivider.setVisibility(horizontalVisibility);
        }
        if (mVerticalDivider != null) {
            mVerticalDivider.setVisibility(verticalVisibility);
        }
    }


    private int getScreenWidth() {
        if (getContext() == null) return 0;
        return getContext().getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        if (getContext() == null) return 0;
        return getContext().getResources().getDisplayMetrics().heightPixels;
    }


    private class CustomTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mBuilder != null && mBuilder.getOnInputTextListener() != null) {
                mBuilder.getOnInputTextListener().onInputText(WriteFittingDialog.this, s.toString());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mLeftTv) {
                dismiss();
            } else if (v == mRightTv) {
                if (mBuilder != null && mBuilder.getOnInputTextListener() != null) {
                    String text = mEditText.getText().toString().trim();
                    mBuilder.getOnInputTextListener().onInputFinish(WriteFittingDialog.this, text, mBuilder.inputText, mIsSave, mBuilder.time);
                }
            } else if (v == mSaveTv) {
                mIsSave = !mIsSave;
                mSaveTv.setCompoundDrawablesWithIntrinsicBounds(mIsSave ? R.drawable.ic_choose_sel : R.drawable.ic_choose_nol, 0, 0, 0);
            }
        }
    };

    public static class Builder {
        private float width;
        private float height;
        private boolean isCancelable;

        private String title;
        private String inputText;
        private String inputTips;
        private int tipsColor;
        private String leftText;
        private int leftColor;
        private String rightText;
        private int rightColor;
        private long time;
        private boolean saveViewVisible = true;
        private OnInputTextListener onInputTextListener;
        private EditTextType editTextType = EditTextType.EDIT_TEXT_TYPE_NORMAL;

        public float getWidth() {
            return width;
        }

        public Builder setWidth(float width) {
            this.width = width;
            return this;
        }

        public float getHeight() {
            return height;
        }

        public Builder setHeight(float height) {
            this.height = height;
            return this;
        }

        public boolean isCancelable() {
            return isCancelable;
        }

        public Builder setCancelable(boolean cancelable) {
            isCancelable = cancelable;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getInputText() {
            return inputText;
        }

        public Builder setInputText(String inputText) {
            this.inputText = inputText;
            return this;
        }

        public String getInputTips() {
            return inputTips;
        }

        public Builder setInputTips(String inputTips) {
            this.inputTips = inputTips;
            return this;
        }

        public String getLeftText() {
            return leftText;
        }

        public Builder setLeftText(String leftText) {
            this.leftText = leftText;
            return this;
        }

        public int getLeftColor() {
            return leftColor;
        }

        public Builder setLeftColor(int leftColor) {
            this.leftColor = leftColor;
            return this;
        }

        public boolean isSaveViewVisible() {
            return saveViewVisible;
        }

        public Builder setSaveViewVisible(boolean saveViewVisible) {
            this.saveViewVisible = saveViewVisible;
            return this;
        }

        public String getRightText() {
            return rightText;
        }

        public Builder setRightText(String rightText) {
            this.rightText = rightText;
            return this;
        }

        public int getRightColor() {
            return rightColor;
        }

        public Builder setRightColor(int rightColor) {
            this.rightColor = rightColor;
            return this;
        }

        public long getTime() {
            return time;
        }

        public Builder setTime(long time) {
            this.time = time;
            return this;
        }

        public OnInputTextListener getOnInputTextListener() {
            return onInputTextListener;
        }

        public Builder setOnInputTextListener(OnInputTextListener onInputTextListener) {
            this.onInputTextListener = onInputTextListener;
            return this;
        }

        public int getTipsColor() {
            return tipsColor;
        }

        public Builder setTipsColor(int tipsColor) {
            this.tipsColor = tipsColor;
            return this;
        }

        public EditTextType getEditTextType() {
            return editTextType;
        }

        public Builder setEditTextType(EditTextType editTextType) {
            this.editTextType = editTextType;
            return this;
        }

        public WriteFittingDialog create() {
            WriteFittingDialog dialog = new WriteFittingDialog();
            dialog.mBuilder = this;
            return dialog;
        }
    }

    public enum EditTextType {
        EDIT_TEXT_TYPE_NUMBER,
        EDIT_TEXT_TYPE_NORMAL
    }

    public interface OnInputTextListener {

        void onDismiss(WriteFittingDialog dialog);

        void onInputText(WriteFittingDialog dialog, String text);

        void onInputFinish(WriteFittingDialog dialog, String value, String lastValue, boolean isSave, long time);
    }
}
