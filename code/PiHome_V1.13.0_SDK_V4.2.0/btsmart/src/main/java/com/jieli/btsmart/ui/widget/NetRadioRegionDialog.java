package com.jieli.btsmart.ui.widget;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.NetRadioRegionAdapter;
import com.jieli.btsmart.databinding.DialogNetRadioRegionBinding;
import com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel;
import com.jieli.jl_http.bean.NetRadioRegionInfo;

import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/2 11:31
 * @desc :
 */
public class NetRadioRegionDialog {
    PopupWindow mPopupWindow;
    Context mContext;
    DismissCallback mDismissCallback;
    NetRadioRegionAdapter mAdapter;

    public NetRadioRegionDialog(Context context, Fragment fragment, NetRadioDetailsViewModel viewModel) {
        this.mContext = context;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_net_radio_region, null);
        DialogNetRadioRegionBinding mBinding = DialogNetRadioRegionBinding.bind(view);
        mAdapter = new NetRadioRegionAdapter(fragment, viewModel);
        mBinding.rvRegions.setAdapter(mAdapter);
        mBinding.rvRegions.setLayoutManager(new GridLayoutManager(context, 4));
        mPopupWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setClippingEnabled(false);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0x000000));
        mPopupWindow.setTouchable(true);
        mPopupWindow.setAnimationStyle(R.style.MyDialogAlphaExitTheme);
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
                    parentView
            );
            mPopupWindow.update();
        }
    }

    public void setNetRegionsData(List<NetRadioRegionInfo> list) {
        mAdapter.setNewInstance(list);
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
