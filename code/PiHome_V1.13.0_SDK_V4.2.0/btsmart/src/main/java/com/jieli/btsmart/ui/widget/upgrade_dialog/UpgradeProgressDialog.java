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
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jieli.btsmart.R;

import java.io.Serializable;

/**
 * 升级进度窗口
 *
 * @author zqjasonZhong
 * @date 2019/9/10
 */
public class UpgradeProgressDialog extends DialogFragment {

    private ProgressBar pbProgress;
    private TextView tvTitle;
    private TextView tvTips;

    private Builder mBuilder;

    private boolean isShow = false;

    public final static String KEY_DIALOG_PARAM = "dialog_param";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_upgrade_progress, container, false);
        pbProgress = view.findViewById(R.id.pb_upgrade_progress);
        tvTitle = view.findViewById(R.id.tv_upgrade_progress_title);
        tvTips = view.findViewById(R.id.tv_upgrade_progress_tips);
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        if (getArguments() != null) {
            Builder temp = (Builder) getArguments().getSerializable(KEY_DIALOG_PARAM);
            if (temp != null) {
                mBuilder = temp;
            }
        }
        updateView(mBuilder);
        return view;
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

    public boolean isShow() {
        return isShow;
    }

    public Builder getBuilder() {
        return mBuilder;
    }

    public void updateView(Builder builder) {
        if (builder != null) {
            if (tvTitle != null) {
                if (builder.getProgressText() != null) {
                    tvTitle.setText(builder.getProgressText());
                }
                if (builder.getProgressTextColor() > 0) {
                    tvTitle.setTextColor(getResources().getColor(builder.getProgressTextColor()));
                }
            }
            if (pbProgress != null) {
                if (builder.getProgress() >= 0) {
                    pbProgress.setProgress(builder.getProgress());
                }
            }
            if (tvTips != null) {
                if (builder.getTips() != null) {
                    tvTips.setText(builder.getTips());
                }
                if (builder.getTipsColor() > 0) {
                    tvTips.setTextColor(builder.getTipsColor());
                }
            }
            mBuilder = builder;
        }
    }

    private void configure(Builder builder) {
        if (getDialog() == null) return;
        Window window = getDialog().getWindow();
        if (window == null) return;

        WindowManager.LayoutParams mLayoutParams = window.getAttributes();
        mLayoutParams.gravity = Gravity.BOTTOM;
        mLayoutParams.dimAmount = 0.5f;
        mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        mLayoutParams.width = builder != null && builder.getWidth() > 0 ? (int) (builder.getWidth() * getScreenWidth()) : WindowManager.LayoutParams.WRAP_CONTENT;
        mLayoutParams.height = builder != null && builder.getHeight() > 0 ? (int) (builder.getHeight() * getScreenHeight()) : WindowManager.LayoutParams.WRAP_CONTENT;

        window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_transparent)));
        window.getDecorView().getRootView().setBackgroundColor(Color.TRANSPARENT);
        window.setAttributes(mLayoutParams);

        setCancelable(builder != null && builder.isCancel());
    }

    private void setShow(boolean show) {
        isShow = show;
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
    public void show(FragmentManager manager, String tag) {
        setShow(true);
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!isShow) setShow(true);
    }

    @Override
    public void dismiss() {
        setShow(false);
        super.dismissAllowingStateLoss();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        setShow(false);
        super.onDismiss(dialog);
    }

    @Override
    public void onDestroyView() {
        setShow(false);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        setShow(false);
        super.onDestroy();
    }

    public static class Builder implements Serializable {
        //dialog的宽度比例(范围[0, 1]）
        private float width;
        //dialog的高度比例(范围[0, 1]）
        private float height;
        //是否允许点击外部消失
        private boolean cancel;

        private String progressText;
        private int progressTextColor;
        private int progress;
        private String tips;
        private int tipsColor;

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

        public String getProgressText() {
            return progressText;
        }

        public Builder setProgressText(String progressText) {
            this.progressText = progressText;
            return this;
        }

        public int getProgressTextColor() {
            return progressTextColor;
        }

        public Builder setProgressTextColor(int progressTextColor) {
            this.progressTextColor = progressTextColor;
            return this;
        }

        public int getProgress() {
            return progress;
        }

        public Builder setProgress(int progress) {
            if (progress < 0) {
                progress = 0;
            }
            if (progress > 100) {
                progress = 100;
            }
            this.progress = progress;
            return this;
        }

        public String getTips() {
            return tips;
        }

        public Builder setTips(String tips) {
            this.tips = tips;
            return this;
        }

        public int getTipsColor() {
            return tipsColor;
        }

        public Builder setTipsColor(int tipsColor) {
            this.tipsColor = tipsColor;
            return this;
        }

        public UpgradeProgressDialog create() {
            UpgradeProgressDialog dialog = new UpgradeProgressDialog();
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
                    ", progressText='" + progressText + '\'' +
                    ", progressTextColor=" + progressTextColor +
                    ", progress=" + progress +
                    ", tips='" + tips + '\'' +
                    ", tipsColor=" + tipsColor +
                    '}';
        }
    }
}
