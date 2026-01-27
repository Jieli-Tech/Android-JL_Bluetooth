package com.jieli.btsmart.ui.widget.upgrade_dialog;

import android.content.DialogInterface;
import android.content.res.Configuration;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import com.jieli.btsmart.R;

import java.io.Serializable;

/**
 * 提示窗口
 *
 * @author zqjasonZhong
 * @date 2019/9/10
 */
public class NotifyDialog extends DialogFragment {
    private static final String TAG = NotifyDialog.class.getSimpleName();

    private ImageView mCloseImg;
    private TextView mTitleTv;
    private TextView mMessageTv;
    private LinearLayout mBottomLayout;
    private TextView mLeftTv;
    private ImageView mTitleIv;

    private Builder mBuilder;

    private boolean isShow = false;


    public final static String KEY_DIALOG_PARAM = "dialog_param";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_notify, container, false);
        initView(view);
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        if (getArguments() != null) {
            Builder temp = (Builder) getArguments().getSerializable(KEY_DIALOG_PARAM);
            if (temp != null) {
                mBuilder = temp;
            }
        }
        return view;
    }

    public boolean isShow() {
        return isShow;
    }

    public Builder getBuilder() {
        return mBuilder;
    }

    private void initView(View view) {
        mCloseImg = view.findViewById(R.id.dialog_notify_top_close);
        mTitleTv = view.findViewById(R.id.dialog_notify_top_title);
        mTitleIv = view.findViewById(R.id.dialog_notify_message_iv);

        mMessageTv = view.findViewById(R.id.dialog_notify_message_tv);
        mBottomLayout = view.findViewById(R.id.dialog_notify_bottom_layout);
        mLeftTv = view.findViewById(R.id.dialog_notify_bottom_left_tv);

        updateView(mBuilder);
    }

    public void updateView(Builder builder) {
        if (builder != null) {
            mBuilder = builder;
            if (mCloseImg != null) {
                mCloseImg.setVisibility(builder.isHasCloseBtn() ? View.VISIBLE : View.GONE);
                mCloseImg.setOnClickListener(v -> dismiss());
            }
            if (mTitleTv != null) {
                if (builder.getTitle() != null) {
                    mTitleTv.setText(builder.getTitle());
                }
                if (builder.getTitleColor() > 0) {
                    mTitleTv.setTextColor(getResources().getColor(builder.getTitleColor()));
                }
            }

            if (mTitleIv != null) {
                if (builder.getTitleImg() > 0) {
                    mTitleIv.setImageResource(builder.getTitleImg());
                    mTitleIv.setVisibility(View.VISIBLE);
                } else {
                    mTitleIv.setVisibility(View.GONE);
                }
            }

            if (mMessageTv != null) {
                if (builder.getMessage() != null) {
                    mMessageTv.setVisibility(View.VISIBLE);
                    mMessageTv.setText(builder.getMessage());
                } else {
                    mMessageTv.setVisibility(View.GONE);
                }
                if (builder.getMessageColor() > 0) {
                    mMessageTv.setTextColor(getResources().getColor(builder.getMessageColor()));
                }
            }
            if (mBottomLayout != null && mLeftTv != null) {
                if (builder.getLeftText() != null) {
                    mBottomLayout.setVisibility(View.VISIBLE);
                    mLeftTv.setText(builder.getLeftText());
                    if (builder.getLeftTextColor() > 0) {
                        mLeftTv.setTextColor(getResources().getColor(builder.getLeftTextColor()));
                    }
                    if (builder.getLeftClickListener() != null) {
                        mLeftTv.setOnClickListener(builder.getLeftClickListener());
                    }
                } else {
                    mBottomLayout.setVisibility(View.GONE);
                }
            }
            setCancelable(builder.isCancel());
        }
    }

    private void configure(Builder builder) {
        Window window = getDialog().getWindow();
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams mLayoutParams = window.getAttributes();
        mLayoutParams.gravity = Gravity.BOTTOM;
        mLayoutParams.dimAmount = 0.5f;
        mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        mLayoutParams.width = builder != null && builder.getWidth() > 0 ? (int) (builder.getWidth() * getScreenWidth()) : WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = builder != null && builder.getHeight() > 0 ? (int) (builder.getHeight() * getScreenHeight()) : WindowManager.LayoutParams.WRAP_CONTENT;

        window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_transparent)));
        window.getDecorView().getRootView().setBackgroundColor(Color.TRANSPARENT);
        window.setAttributes(mLayoutParams);
    }

    private int getScreenWidth() {
        if (getContext() == null) return 0;
        return getContext().getResources().getDisplayMetrics().widthPixels;
    }

    private int getScreenHeight() {
        if (getContext() == null) return 0;
        return getContext().getResources().getDisplayMetrics().heightPixels;
    }

    @Override
    public void onStart() {
        super.onStart();
        configure(mBuilder);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        configure(mBuilder);
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        isShow = true;
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public int show(@NonNull FragmentTransaction transaction, String tag) {
        isShow = true;
        return super.show(transaction, tag);
    }

    @Override
    public void showNow(@NonNull FragmentManager manager, String tag) {
        isShow = true;
        super.showNow(manager, tag);
    }

    @Override
    public void dismiss() {
        isShow = false;
        super.dismissAllowingStateLoss();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        isShow = false;
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroy() {
        isShow = false;
        super.onDestroy();
    }

    public static class Builder implements Serializable {
        //dialog的宽度比例(范围[0, 1]）
        private float width;
        //dialog的高度比例(范围[0, 1]）
        private float height;
        //是否允许点击外部消失
        private boolean cancel;

        //头部Image
        private int titleImg;
        //标题
        private String title;
        //标题颜色
        private int titleColor;
        //展示内容
        private String message;
        //内容颜色
        private int messageColor;
        //是否显示关闭按钮
        private boolean isHasCloseBtn;
        //左按钮的内容
        private String leftText;
        //左按钮的内容颜色
        private int leftTextColor;
        //左按钮的点击事件
        private View.OnClickListener mLeftClickListener;


        public float getWidth() {
            return width;
        }

        public Builder setWidth(float width) {
            if (width < 0) {
                width = 0;
            }
            if (width > 1) {
                width = 1.0f;
            }
            this.width = width;
            return this;
        }

        public float getHeight() {
            return height;
        }

        public Builder setHeight(float height) {
            if (height < 0) {
                height = 0;
            }
            if (height > 1) {
                height = 1.0f;
            }
            this.height = height;
            return this;
        }

        public boolean isCancel() {
            return cancel;
        }

        public Builder setCancel(boolean cancel) {
            this.cancel = cancel;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public int getTitleImg() {
            return titleImg;
        }

        public Builder setTitleImg(int titleImg) {
            this.titleImg = titleImg;
            return this;
        }

        public int getTitleColor() {
            return titleColor;
        }

        public Builder setTitleColor(int titleColor) {
            this.titleColor = titleColor;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public int getMessageColor() {
            return messageColor;
        }

        public Builder setMessageColor(int messageColor) {
            this.messageColor = messageColor;
            return this;
        }

        public boolean isHasCloseBtn() {
            return isHasCloseBtn;
        }

        public Builder setHasCloseBtn(boolean hasCloseBtn) {
            isHasCloseBtn = hasCloseBtn;
            return this;
        }

        public String getLeftText() {
            return leftText;
        }

        public Builder setLeftText(String leftText) {
            this.leftText = leftText;
            return this;
        }

        public int getLeftTextColor() {
            return leftTextColor;
        }

        public Builder setLeftTextColor(int leftTextColor) {
            this.leftTextColor = leftTextColor;
            return this;
        }

        public View.OnClickListener getLeftClickListener() {
            return mLeftClickListener;
        }

        public Builder setLeftClickListener(View.OnClickListener leftClickListener) {
            mLeftClickListener = leftClickListener;
            return this;
        }

        public NotifyDialog create() {
            NotifyDialog dialog = new NotifyDialog();
            dialog.mBuilder = this;
            return dialog;
        }

        @NonNull
        @Override
        public String toString() {
            return "Builder{" +
                    "width=" + width +
                    ", height=" + height +
                    ", cancel=" + cancel +
                    ", titleImg=" + titleImg +
                    ", title='" + title + '\'' +
                    ", titleColor=" + titleColor +
                    ", message='" + message + '\'' +
                    ", messageColor=" + messageColor +
                    ", isHasCloseBtn=" + isHasCloseBtn +
                    ", leftText='" + leftText + '\'' +
                    ", leftTextColor=" + leftTextColor +
                    ", mLeftClickListener=" + mLeftClickListener +
                    '}';
        }
    }
}
