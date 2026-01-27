package com.jieli.btsmart.data.model.ota;

import androidx.annotation.NonNull;

/**
 * OTA阶段信息
 *
 * @author zqjasonZhong
 * @since 2020/5/19
 */
@SuppressWarnings("UnusedReturnValue")
public class OtaStageInfo {
    //阶段
    private int stage;
    //进度
    private int progress;
    //信息
    private String message;

    //空闲-无更新阶段
    public final static int STAGE_IDLE = 0;
    //空闲-有更新阶段
    public final static int STAGE_IDLE_DOWNLOAD = 1;
    //准备阶段
    public final static int STAGE_PREPARE = 2;
    //升级就绪阶段
    public final static int STAGE_UPGRADE = 3;
    //升级中阶段
    public final static int STAGE_UPGRADING = 4;

    public OtaStageInfo(int stage){
        this(stage, 0);
    }

    public OtaStageInfo(int stage, int progress){
        this(stage, progress, null);
    }

    public OtaStageInfo(int stage, int progress, String message){
        setStage(stage);
        setProgress(progress);
        setMessage(message);
    }

    public int getStage() {
        return stage;
    }

    public OtaStageInfo setStage(int stage) {
        this.stage = stage;
        return this;
    }

    public int getProgress() {
        return progress;
    }

    public OtaStageInfo setProgress(int progress) {
        this.progress = progress;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public OtaStageInfo setMessage(String message) {
        this.message = message;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return "OtaStageInfo{" +
                "stage=" + stage +
                ", progress=" + progress +
                ", message='" + message + '\'' +
                '}';
    }
}
