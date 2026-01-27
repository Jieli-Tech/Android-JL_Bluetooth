package com.jieli.btsmart.ui.settings.device;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.ui.widget.LoadingDialog;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备控制基类
 * @since 2023/2/22
 */
public abstract class DeviceControlFragment extends BaseFragment implements DevicePopDialogFilter.IgnoreFilter {

    private LoadingDialog mLoadingDialog;

    @Override
    public void onStart() {
        super.onStart();
        DevicePopDialogFilter.getInstance().addIgnoreFilter(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        DevicePopDialogFilter.getInstance().removeIgnoreFilter(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissLoadingDialog();
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }

    public void showLoadingDialog(String content) {
        if (isInvalid()) return;
        dismissLoadingDialog();
        if (null == mLoadingDialog) {
            mLoadingDialog = new LoadingDialog(content);
        }
        if (!mLoadingDialog.isShow()) {
            mLoadingDialog.show(getChildFragmentManager(), LoadingDialog.class.getSimpleName());
        }
    }

    public void dismissLoadingDialog() {
        if (isInvalid()) return;
        if (null != mLoadingDialog) {
            if (mLoadingDialog.isShow()) {
                mLoadingDialog.dismiss();
            }
            mLoadingDialog = null;
        }
    }
}
