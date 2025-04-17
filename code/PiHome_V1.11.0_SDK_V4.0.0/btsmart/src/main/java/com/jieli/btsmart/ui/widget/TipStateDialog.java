package com.jieli.btsmart.ui.widget;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.DialogTipStateBinding;
import com.jieli.btsmart.ui.base.BaseDialogFragment;

/**
 * FM提示状态窗
 *
 * @author zqjasonZhong
 * @since 2020/6/6
 */
public class TipStateDialog extends BaseDialogFragment {
    private String mTips;
    private int mResId;
    private Callback mCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null) {
            //设置dialog的基本样式参数
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
            Window window = getDialog().getWindow();
            if (window != null) {
                //去掉dialog默认的padding
                window.getDecorView().setPadding(0, 0, 0, 0);
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
//                //设置dialog的位置在底部
                lp.gravity = Gravity.CENTER;
                lp.dimAmount = 0.15f;

                window.setAttributes(lp);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        DialogTipStateBinding binding = DataBindingUtil.inflate(inflater, R.layout.dialog_tip_state, container, false);
        binding.setResId(mResId);
        binding.setTips(mTips);
        return binding.getRoot();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mCallback != null) {
            mCallback.onDismiss();
        }
    }

    public void setTips(String tips) {
        mTips = tips;
    }

    public void setImageResource(int resId) {
        mResId = resId;
    }

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    public interface Callback {
        void onDismiss();
    }
}
