package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.DialogFmSearchBinding;
import com.jieli.btsmart.viewmodel.FMControlViewModel;
import com.jieli.component.utils.ValueUtil;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/2 11:31
 * @desc :
 */
public class FMSearchDialog {
    PopupWindow mPopupWindow;
    Context mContext;
    DismissCallback mDismissCallback;

    public FMSearchDialog(Context context, FMControlViewModel fmControlViewModel) {
        this.mContext = context;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_fm_search, null);
        DialogFmSearchBinding mBinding = DialogFmSearchBinding.bind(view);
        mBinding.tvSearchAll.setOnClickListener(v -> {
            fmControlViewModel.onFmSearchAll();
            dismissDialog(true);
        });
        mBinding.tvSearchForward.setOnClickListener(v -> {
            fmControlViewModel.onFMSearchForward();
            dismissDialog(true);
        });
        mBinding.tvSearchBackward.setOnClickListener(v -> {
            fmControlViewModel.onFMSearchBackward();
            dismissDialog(true);
        });
        mPopupWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.setFocusable(true);
        mPopupWindow.setOutsideTouchable(true);
//        mPopupWindow.setAnimationStyle(R.style.MyDialogTheme);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0x000000));
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOnDismissListener(() -> {
            dismissDialog(true);
            if (mDismissCallback != null) {
                mDismissCallback.dismiss();
            }
        });
    }

    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    public void showDialog(View parentView) {
        if (mPopupWindow != null && !isShowing() && parentView != null && parentView.getWindowToken() != null) {
            mPopupWindow.showAsDropDown(
                    parentView,
                    -ValueUtil.dp2px(mContext, 14),
                    0,
                    Gravity.TOP
            );
            mPopupWindow.update();
        }
    }

    public void setDismissCallback(DismissCallback callback) {
        mDismissCallback = callback;
    }

    public void dismissDialog(boolean isUser) {
        if (mPopupWindow != null) {
            if (mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
            mPopupWindow = null;
        }
    }

    public interface DismissCallback {
        void dismiss();
    }
}
