package com.jieli.btsmart.ui.widget.DevicePopDialog;

import com.jieli.bluetooth.bean.BleScanMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/5 3:16 PM
 * @desc : 过滤管理类
 */
public class DevicePopDialogFilter {
    private static DevicePopDialogFilter instance;
    private final List<BleScanMessage> list = new ArrayList<>();
    private final List<IgnoreFilter> ignoreFilters = new ArrayList<>();

    public static DevicePopDialogFilter getInstance() {
        if (instance == null) {
            instance = new DevicePopDialogFilter();
        }
        return instance;
    }


    public void addIgnoreFilter(IgnoreFilter filter) {
        ignoreFilters.add(filter);
    }

    public void removeIgnoreFilter(IgnoreFilter filter) {
        ignoreFilters.remove(filter);
    }

    boolean shouldIgnore(BleScanMessage bleScanMessage) {
        for (IgnoreFilter filter : ignoreFilters) {
            if (filter.shouldIgnore(bleScanMessage)) {
                return true;
            }
        }
        return filterBySeq(bleScanMessage);
    }

    void addSeqIgnore(BleScanMessage scanMessage) {
        list.add(0, scanMessage);
    }

    private boolean filterBySeq(BleScanMessage bleScanMessage) {
        BleScanMessage tmp = null;
        for (BleScanMessage message : list) {
            if (message.baseEquals(bleScanMessage)) {
                tmp = message;
                break;
            }
        }
        //通过判断seq是否需要过滤，可以在这里增加其他规则
        if (tmp != null && bleScanMessage.getSeq() == tmp.getSeq()) {
            return true;
        } else {
            list.remove(tmp);//清除已失效广播信息缓存
        }
        return false;
    }


    public interface IgnoreFilter {
        boolean shouldIgnore(BleScanMessage bleScanMessage);
    }
}
