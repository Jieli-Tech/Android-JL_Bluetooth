package com.jieli.btsmart.tool.product;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.interfaces.JL_HttpApi;
import com.jieli.jl_http.util.Constant;

import java.io.IOException;
/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/12/9 8:53 AM
 * @desc :网络任务基础类
 */
abstract class TaskInfo implements Runnable {

    protected static final int STATE_RUNNING = 0;
    protected static final int STATE_WAITE = 1;
    protected static final int STATE_FINISH = 2;
    protected static final int STATE_FAILED = 3;

    private int state = STATE_WAITE;
    protected static JL_HttpApi httpApi;
    protected TaskManager taskManager;

    protected String tag = getClass().getSimpleName();
    protected final BleScanMessage bleScanMessage;

    static void init() {
        httpApi = JL_HttpClient.getInstance()
                .getRetrofit()
                .newBuilder()
                .baseUrl(Constant.BASE_URL_JL)
                .build()
                .create(JL_HttpApi.class);
    }

    public TaskInfo(BleScanMessage bleScanMessage) {
        this.bleScanMessage = bleScanMessage;
    }

    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    protected void setState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public boolean isFailed() {
        return STATE_FAILED == getState();
    }

    @Override
    public void run() {
        try {
            setState(STATE_RUNNING);
            execute();
            setState(STATE_FINISH);
        } catch (IOException e) {
            setState(STATE_FAILED);
            e.printStackTrace();
            //是否有必要持久化到磁盘，在下次打开app的是否重新开始任务
        }

    }

    protected abstract void execute() throws IOException;

    @NonNull
    @Override
    public String toString() {
        return "TaskInfo{" +
                "state=" + state +
                ", bleScanMessage=" + bleScanMessage +
                '}';
    }
}