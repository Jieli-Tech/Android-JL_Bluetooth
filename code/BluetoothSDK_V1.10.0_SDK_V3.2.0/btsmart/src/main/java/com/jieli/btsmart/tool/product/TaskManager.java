package com.jieli.btsmart.tool.product;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.util.NetworkStateHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/25 3:37 PM
 * @desc : 请求和文件下载管理类
 */
class TaskManager implements NetworkStateHelper.Listener {
    private final List<TaskInfo> tasks = new ArrayList<>();
    protected ExecutorService executorService = Executors.newCachedThreadPool();

    private final ListenerHandler listenerHandler = new ListenerHandler();
    private final String tag = getClass().getSimpleName();

    TaskManager() {
        NetworkStateHelper.getInstance().registerListener(this);
    }

    void addTask(TaskInfo taskInfo) {
        if (tasks.contains(taskInfo)) {
            int index = tasks.indexOf(taskInfo);
            TaskInfo info = tasks.get(index);
            //如果这个task处于失败状态就重试一下
            if (info.isFailed()) {
                executorService.execute(info);
            }
        } else {
            tasks.add(taskInfo);
            taskInfo.setTaskManager(this);
            executorService.execute(taskInfo);
        }
    }


    public void registerListener(ProductCacheManager.OnUpdateListener listener) {
        listenerHandler.registerListener(listener);
    }

    public void unregisterListener(ProductCacheManager.OnUpdateListener listener) {
        listenerHandler.unregisterListener(listener);
    }

    public ListenerHandler getListenerHandler() {
        return listenerHandler;
    }

    @Override
    public void onNetworkStateChange(int type, boolean available) {
        for (TaskInfo taskInfo : tasks) {
            if (taskInfo.isFailed()) {
                JL_Log.e(tag, "重新开始失败的任务-->" + taskInfo);
                executorService.execute(taskInfo);
            }
        }
    }
    @Override
    protected void finalize() throws Throwable {
        executorService.shutdownNow();
        NetworkStateHelper.getInstance().unregisterListener(this);
        JL_Log.e(tag, "--------------finalize---------");
        super.finalize();
    }
}
