package com.jieli.btsmart.ui.widget;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.base.BaseDialogFragment;

/**
 * 等待提示窗
 *
 * @author zqjasonZhong
 * @since 2020/6/6
 */
public class LoadingDialog extends BaseDialogFragment {

    private TextView tvTips;

    private String mTips;

    public LoadingDialog() {

    }

    public LoadingDialog(String tips) {
        mTips = tips;
    }

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
                //设置dialog的位置在底部
                lp.gravity = Gravity.CENTER;
                window.setAttributes(lp);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }

        View view = inflater.inflate(R.layout.dialog_loading, container, false);
        tvTips = view.findViewById(R.id.tv_dialog_loading);
        if (mTips != null) {
            updateTips(mTips);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public void updateTips(String tips) {
        if (!isAdded() || isDetached()) return;
        mTips = tips;
        tvTips.setText(tips);
    }

}
