package com.jieli.btsmart.ui.settings.device;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 设备控制基类
 * @since 2023/2/22
 */
public abstract class DeviceControlFragment extends BaseFragment implements DevicePopDialogFilter.IgnoreFilter {

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
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }
}
