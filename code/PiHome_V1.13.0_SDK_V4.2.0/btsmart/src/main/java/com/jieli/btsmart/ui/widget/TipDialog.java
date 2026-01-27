package com.jieli.btsmart.ui.widget;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
public class TipDialog extends DialogFragment {

    private boolean isShow;
    private Builder mBuilder;

    private TextView mTitleTv;
    private TextView mTipContentTv;
    private View mHorizontalDivider;
    private View mVerticalDivider;
    private TextView mLeftTv;
    private TextView mRightTv;

    public TipDialog() {

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
        View view = inflater.inflate(R.layout.dialog_tip, container);
        initView(view);
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        if (mBuilder != null && mBuilder.getOnTipDialogListener() != null) {
            mBuilder.getOnTipDialogListener().onDismiss(this);
        }
        super.onDismiss(dialog);
    }


    private void initView(View view) {
        mTitleTv = view.findViewById(R.id.dialog_input_text_title_tv);
        mTipContentTv = view.findViewById(R.id.tv_tip_content);

        mHorizontalDivider = view.findViewById(R.id.dialog_input_text_horizontal_divide);
        mVerticalDivider = view.findViewById(R.id.dialog_input_text_vertical_divide);
        mLeftTv = view.findViewById(R.id.dialog_input_text_left_tv);
        mRightTv = view.findViewById(R.id.dialog_input_text_right_tv);

        mLeftTv.setOnClickListener(mOnClickListener);
        mRightTv.setOnClickListener(mOnClickListener);
    }

    public void updateDialog() {
        if (mBuilder != null) {
            if (mTitleTv != null && !TextUtils.isEmpty(mBuilder.getTitle())) {
                mTitleTv.setText(mBuilder.getTitle());
            }
            if (mTipContentTv != null) {
                mTipContentTv.setText(mBuilder.getContent());
                if (mBuilder.getTipsColor() != 0) {
                    mTipContentTv.setTextColor(mBuilder.getTipsColor());
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

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mLeftTv) {
                if (mBuilder != null && mBuilder.getOnTipDialogListener() != null) {
                    mBuilder.getOnTipDialogListener().onLeftBtnClick(TipDialog.this);
                }
            } else if (v == mRightTv) {
                if (mBuilder != null && mBuilder.getOnTipDialogListener() != null) {
                    mBuilder.getOnTipDialogListener().onRightBtnClick(TipDialog.this);
                }
            }
        }
    };

    public static class Builder {
        private float width;
        private float height;
        private boolean isCancelable;

        private String title;
        private String content;
        private int tipsColor;
        private String leftText;
        private int leftColor;
        private String rightText;
        private int rightColor;
        private OnTipDialogListener onTipDialogListener;

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

        public String getContent() {
            return content;
        }

        public Builder setContent(String content) {
            this.content = content;
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

        public int getTipsColor() {
            return tipsColor;
        }

        public Builder setTipsColor(int tipsColor) {
            this.tipsColor = tipsColor;
            return this;
        }

        public OnTipDialogListener getOnTipDialogListener() {
            return onTipDialogListener;
        }

        public Builder setOnTipDialogListener(OnTipDialogListener onTipDialogListener) {
            this.onTipDialogListener = onTipDialogListener;
            return this;
        }

        public TipDialog create() {
            TipDialog dialog = new TipDialog();
            dialog.mBuilder = this;
            return dialog;
        }
    }

    public interface OnTipDialogListener {

        void onDismiss(TipDialog dialog);

        void onRightBtnClick(TipDialog dialog);

        void onLeftBtnClick(TipDialog dialog);
    }
}
