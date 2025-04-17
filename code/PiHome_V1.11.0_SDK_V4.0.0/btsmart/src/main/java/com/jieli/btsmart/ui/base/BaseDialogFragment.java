package com.jieli.btsmart.ui.base;

import android.content.DialogInterface;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.ToastUtil;

/**
 * DialogFragment基类
 *
 * @author zqjasonZhong
 * @date 2019/4/20
 */
public abstract class BaseDialogFragment extends DialogFragment {
    protected static String TAG = BaseDialogFragment.class.getSimpleName();
    private boolean isShow = false;

    public boolean isShow() {
        return isShow;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
//        super.show(manager, tag);
        setShow(true);
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onResume() {
        setShow(true);
        super.onResume();
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

    protected void showTips(String tips) {
        ToastUtil.showToastLong(tips);
        JL_Log.d(TAG, tips);
    }

    protected void showTips(String format, Object... args) {
        showTips(AppUtil.formatString(format, args));
    }

    private void setShow(boolean isShow) {
        this.isShow = isShow;
    }

    public int getScreenWidth() {
        DisplayMetrics displayMetrics = getDisplayMetrics();
        return displayMetrics == null ? 0 : displayMetrics.widthPixels;
    }

    public int getScreenHeight() {
        DisplayMetrics displayMetrics = getDisplayMetrics();
        return displayMetrics == null ? 0 : displayMetrics.heightPixels;
    }

    private DisplayMetrics getDisplayMetrics() {
        if (getContext() == null) return null;
        if (getContext().getResources() == null) return null;
        return getContext().getResources().getDisplayMetrics();
    }
}
