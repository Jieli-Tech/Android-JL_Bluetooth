package com.jieli.btsmart.ui.widget.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.jieli.btsmart.ui.base.BaseDialogFragment;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通用对话框
 * @since 2023/12/6
 */
public abstract class CommonDialog extends BaseDialogFragment {
    @NonNull
    protected final Builder mBuilder;

    protected CommonDialog(@NonNull Builder builder) {
        this.mBuilder = builder;
    }

    @Override
    public void onStart() {
        super.onStart();
        final Window window = requireDialog().getWindow();
        if (null == window) return;
        final WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = mBuilder.dimAmount;
        params.gravity = mBuilder.gravity;
        params.flags = params.flags | WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        params.width = mBuilder.widthRate == 0f ? WindowManager.LayoutParams.WRAP_CONTENT : (int) (mBuilder.widthRate * getScreenWidth());
        params.height = mBuilder.heightRate == 0f ? WindowManager.LayoutParams.WRAP_CONTENT : (int) (mBuilder.heightRate * getScreenHeight());
        if (mBuilder.x != -1) params.x = mBuilder.x;
        if (mBuilder.y != -1) params.y = mBuilder.y;
        window.setAttributes(params);
        int color = mBuilder.backgroundColor == 0 ? Color.TRANSPARENT : ContextCompat.getColor(requireContext(), mBuilder.backgroundColor);
        window.setBackgroundDrawable(new ColorDrawable(color));
        final View decorView = window.getDecorView();
        if (null != decorView) {
            final View rootView = decorView.getRootView();
            if (null != rootView) rootView.setBackgroundColor(color);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return createView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireDialog().setCancelable(mBuilder.cancelable);
    }

    public abstract View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public static class TextStyle {
        @NonNull
        String text = "";
        int color = 0;
        int size = 0;
        boolean isBold = false;
        int gravity = Gravity.CENTER;
        OnViewClick onClick = null;
        int topDrawableRes = 0;

        public TextStyle setText(@NonNull String text) {
            this.text = text;
            return this;
        }

        public TextStyle setColor(int color) {
            this.color = color;
            return this;
        }

        public TextStyle setSize(int size) {
            this.size = size;
            return this;
        }

        public TextStyle setBold(boolean bold) {
            isBold = bold;
            return this;
        }

        public TextStyle setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public TextStyle setOnClick(OnViewClick onClick) {
            this.onClick = onClick;
            return this;
        }

        public TextStyle setTopDrawableRes(int topDrawableRes) {
            this.topDrawableRes = topDrawableRes;
            return this;
        }
    }

    public interface OnViewClick {

        void onClick(CommonDialog dialog, Object data);
    }

    public static class ButtonStyle extends TextStyle {

    }

    public static abstract class Builder {
        int gravity = Gravity.CENTER;
        @FloatRange(from = 0.0, to = 1.0f)
        float widthRate = 0.95f;
        @FloatRange(from = 0.0, to = 1.0f)
        float heightRate = 0f;
        boolean cancelable = true;
        int x = -1;
        int y = -1;
        int backgroundColor = 0;
        @FloatRange(from = 0.0, to = 1.0f)
        float dimAmount = 0.5f;

        public Builder setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder setWidthRate(float widthRate) {
            this.widthRate = widthRate;
            return this;
        }

        public Builder setHeightRate(float heightRate) {
            this.heightRate = heightRate;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Builder setX(int x) {
            this.x = x;
            return this;
        }

        public Builder setY(int y) {
            this.y = y;
            return this;
        }

        public Builder setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setDimAmount(float dimAmount) {
            this.dimAmount = dimAmount;
            return this;
        }

        public abstract CommonDialog build();
    }
}
