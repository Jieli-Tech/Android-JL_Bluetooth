package com.jieli.btsmart.tool.product;

import android.os.Handler;
import android.os.Looper;

import com.jieli.bluetooth.bean.BleScanMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/25 3:47 PM
 * @desc : 资源更新回调
 */
class ListenerHandler implements ProductCacheManager.OnUpdateListener {

    private final List<ProductCacheManager.OnUpdateListener> listeners = new ArrayList<>();//信息更新回调
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void registerListener(ProductCacheManager.OnUpdateListener listener) {
        if (listeners.contains(listener)) return;
        listeners.add(listener);
    }

    public void unregisterListener(ProductCacheManager.OnUpdateListener listener) {
        listeners.remove(listener);
    }


    @Override
    public void onImageUrlUpdate(BleScanMessage bleScanMessage) {
        handler.post(() -> {
            for (ProductCacheManager.OnUpdateListener listener : listeners) {
                listener.onImageUrlUpdate(bleScanMessage);
            }
        });
    }

    @Override
    public void onJsonUpdate(BleScanMessage bleScanMessage, String path) {
        handler.post(() -> {
            for (ProductCacheManager.OnUpdateListener listener : listeners) {
                listener.onJsonUpdate(bleScanMessage, path);
            }
        });
    }
}
