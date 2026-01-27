package com.jieli.btsmart.tool.ai.doubao.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.bean.translation.TranslationResult;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.tool.ai.doubao.tts.model.TtsRequest;
import com.jieli.btsmart.tool.ai.doubao.tts.model.TtsTask;
import com.jieli.btsmart.tool.translate.codec.JLAV2Codec;
import com.jieli.btsmart.tool.translate.codec.OpusCodec;
import com.jieli.btsmart.tool.translate.player.AudioPlayer;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.WavUtil;
import com.jieli.component.utils.FileUtil;
import com.jieli.jl_audio_decode.callback.OnStateCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TtsManager
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc TTS管理器
 * @since 2025/6/30
 */
public class TtsManager {

    /**
     * 空闲状态
     */
    public static final int STATE_IDLE = 0;
    /**
     * 工作状态
     */
    public static final int STATE_WORKING = 1;

    private static final String tag = TtsManager.class.getSimpleName();
    /**
     * 处理线程
     */
    private HandleThread mHandleThread;
    /**
     * 单例对象
     */
    private static volatile TtsManager instance;
    /**
     * 工作状态回调
     */
    private Consumer<Integer> stateCallback;
    /**
     * 工作状态
     */
    private int workState;

    public static TtsManager getInstance() {
        if (null == instance) {
            synchronized (TtsManager.class) {
                if (null == instance) {
                    instance = new TtsManager();
                }
            }
        }
        return instance;
    }

    public boolean isCreateTts() {
        return null != mHandleThread && mHandleThread.isCreateTts();
    }

    public boolean isIdleState() {
        if (null == mHandleThread) return true;
        return mHandleThread.isIdleState();
    }

    public void setStateCallback(Consumer<Integer> stateCallback) {
        this.stateCallback = stateCallback;
    }

    public boolean addTask(@NonNull String mac, @NonNull RoleInfo roleInfo, boolean isUseA2DP,
                           @NonNull TranslationResult result, OnTtsResultCallback callback) {
        if (mHandleThread == null || !mHandleThread.isWorking()) {
            JL_Log.d(tag, "addTask", "create handle thread");
            mHandleThread = new HandleThread(this::postWorkState);
            mHandleThread.start();
            SystemClock.sleep(30);
        }
        boolean ret = mHandleThread.addTtsTask(new TtsTask(mac, roleInfo, isUseA2DP, result, callback));
        if (ret) {
            postWorkState(STATE_WORKING);
        }
        return ret;
    }

    public void stop() {
        JL_Log.d(tag, "stop", "stop tts thread");
        if (mHandleThread != null) {
            if (mHandleThread.isWorking()) {
                mHandleThread.stopThread();
            }
            postWorkState(STATE_IDLE);
            mHandleThread = null;
        }
    }

    private void postWorkState(int state) {
        if (workState != state) {
            workState = state;
            if (null != stateCallback) {
                stateCallback.accept(workState);
            }
        }
    }

    private static class HandleThread extends Thread {

        /**
         * 工作状态回调
         */
        private final Consumer<Integer> stateCallback;
        /**
         * 上下文
         */
        private final Context mContext = MainApplication.getApplication();
        /**
         * TTS生成器
         */
        private final TtsGenerator mTtsGenerator = new TtsGenerator();
        /**
         * 任务队列
         */
        private final LinkedBlockingQueue<TtsTask> mTaskQueue = new LinkedBlockingQueue<>();
        /**
         * UI处理
         */
        private final Handler uiHandler = new Handler(Looper.getMainLooper());

        /**
         * 是否工作中
         */
        private volatile boolean isWorking;
        /**
         * 是否为空队列
         */
        private volatile boolean isQueueEmpty = true;
        /**
         * 是否阻塞线程
         */
        private volatile boolean isBlocked;
        /**
         * 执行任务结果
         */
        private volatile int result = -1;
        /**
         * 是否正在合成TTS
         */
        private volatile boolean isCreateTts;
        private int state;

        public HandleThread(Consumer<Integer> callback) {
            super("tts_handle_thread");
            this.stateCallback = callback;
        }

        @Override
        public synchronized void start() {
            isWorking = true;
            super.start();
        }

        @Override
        public void run() {
            synchronized (mTaskQueue) {
                while (isWorking) {
                    isQueueEmpty = mTaskQueue.isEmpty();
                    if (isQueueEmpty) {
                        //挂起处理线程
                        JL_Log.d(tag, "run", "queue empty. waiting");
                        postWorkState(STATE_IDLE);
                        lockThread();
                    } else {
                        TtsTask task = mTaskQueue.poll();
                        if (null == task) continue;
                        postWorkState(STATE_WORKING);
                        handleTtsTask(task);
                    }
                }
            }
            mTaskQueue.clear();
            isQueueEmpty = true;
            isWorking = false;
            isBlocked = false;
            uiHandler.removeCallbacksAndMessages(null);
        }

        public boolean isWorking() {
            return isWorking;
        }

        public boolean isCreateTts() {
            return isCreateTts;
        }

        public boolean isIdleState() {
            return isQueueEmpty && !isCreateTts;
        }

        public void stopThread() {
            mTaskQueue.clear();
            isWorking = false;
            unlockThread();
        }

        public boolean addTtsTask(@NonNull TtsTask task) {
            if (!isWorking()) return false;
            try {
                mTaskQueue.put(task);
                JL_Log.d(tag, "addTtsTask", "isQueueEmpty : " + isQueueEmpty + ", isBlocked : " + isBlocked);
                if (isQueueEmpty && isBlocked) {
                    isQueueEmpty = false;
                    unlockThread();
                }
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }

        private boolean lockThread() {
            synchronized (mTaskQueue) {
                if (isBlocked) return true;
                boolean ret = false;
                try {
                    isBlocked = true;
                    JL_Log.d(tag, "lockThread", "ready to lock.");
                    mTaskQueue.wait();
                    ret = true;
                } catch (InterruptedException e) {

                }
                isBlocked = false;
                JL_Log.d(tag, "lockThread", "release lock.");
                return ret;
            }
        }

        private boolean unlockThread() {
            synchronized (mTaskQueue) {
                if (!isBlocked) return true;
                try {
                    JL_Log.d(tag, "unlockThread", "notify to release lock.");
                    mTaskQueue.notify();
                    return true;
                } catch (Exception e) {

                }
                return false;
            }
        }

        private void postWorkState(int state) {
            if (this.state != state) {
                this.state = state;
                if (null != stateCallback) {
                    stateCallback.accept(state);
                }
            }
        }

        private void callbackTtsError(TtsTask task, int code, String message) {
            if (null == task) return;
            JL_Log.w(tag, "callbackTtsError", CommonUtil.formatString("code : %s, %s", CommonUtil.formatInt(code), message));
            final OnTtsResultCallback callback = task.getCallback();
            if (null != callback) {
                uiHandler.post(() -> callback.onError(task.getTranslationResult(), code, message));
            }
            result = code;
            unlockThread();
        }

        private void callbackTtsSuccess(TtsTask task, List<String> filePaths) {
            if (null == task) return;
            uiHandler.post(() -> JL_Log.d(tag, "callbackTtsSuccess", "src text : " + task.getTranslationResult().getNlpText()
                    + ", \n dest text : " + task.getTranslationResult().getTranslationText() + ", files : " + (null == filePaths ? 0 : filePaths.size())));
            final OnTtsResultCallback callback = task.getCallback();
            if (null != callback) {
                final String[] paths = filePaths != null ? filePaths.toArray(new String[0]) : null;
                uiHandler.post(() -> callback.onStop(task.getTranslationResult(), paths));
            }
            if (filePaths != null && !filePaths.isEmpty()) {
                this.result = 0;
                unlockThread();
            }
        }

        private void synthesizedDestText(TtsTask task, List<String> filePaths) {
            if (null == task) return;
            TtsRequest request = task.buildTtsRequest(task.getTranslationResult().getDestLanguage(),
                    task.getTranslationResult().getTranslationText());
            if (request == null) {
                callbackTtsError(task, ErrorCode.SUB_ERR_PARAMETER, "No TTS Task.");
                return;
            }
            mTtsGenerator.start(request, stateResult -> {
                if (stateResult.getState() == StateResult.STATE_FINISH) {
                    if (stateResult.isSuccess()) { //TTS合成成功
                        final byte[] data = stateResult.getData();
                        if (null == data || data.length == 0) {
                            callbackTtsError(task, ErrorCode.SUB_ERR_NOT_FOUND_DATA, ErrorCode.code2Msg(ErrorCode.SUB_ERR_NOT_FOUND_DATA));
                            return;
                        }
                        long id = task.getTranslationResult().getId();
                        String filename = AppUtil.formatString("tts_dest_%d_%s", id, TranslateUtil.getDateString());
                        String wavFilePath = FileUtil.createFilePath(mContext, SConstant.DIR_TRANSLATION, task.getMac())
                                + File.separator + filename + TranslateUtil.WAV_SUFFIX;
                        if (!WavUtil.pcmToWav(data, wavFilePath)) { //保存WAV文件
                            callbackTtsError(task, ErrorCode.SUB_ERR_IO_EXCEPTION, "Failed to save wav file.");
                            return;
                        }
                        filePaths.add(wavFilePath);
                        boolean isUseSpeak = task.getRoleInfo().isUseSpeaker();
                        JL_Log.d(tag, "synthesizedDestText", "isUseA2DP = " + task.isUseA2DP() + ", isUseSpeak : " + isUseSpeak);
                        if (task.isUseA2DP() || isUseSpeak) { //使用A2DP播报
                            task.getTranslationResult().setTranslationTTSData(null);
                            callbackTtsSuccess(task, null);
                            //音视频翻译模式不进行播报
                            if (task.getRoleInfo().getTranslationMode() != TranslationMode.MODE_AUDIO_TRANSLATION) {
                                JL_Log.d(tag, "synthesizedDestText", "try to play wav. wavFilePath : " + wavFilePath);
                                if (!AudioPlayer.getInstance().play(new AudioPlayer.PlayTask(wavFilePath, isUseSpeak))) {
                                    JL_Log.w(tag, "synthesizedDestText", "Failed to play wav. path : " + wavFilePath);
                                }
                            }
                            synthesizedSrcText(task, filePaths);
                            return;
                        }
                        final String pcmFilePath = FileUtil.createFilePath(mContext, SConstant.DIR_TRANSLATION, task.getMac())
                                + File.separator + filename + TranslateUtil.PCM_SUFFIX;
                        if (!FileUtil.bytesToFile(data, pcmFilePath)) {//保存PCM文件
                            for (String filePath : filePaths) {
                                FileUtil.deleteFile(new File(filePath));
                            }
                            callbackTtsError(task, ErrorCode.SUB_ERR_IO_EXCEPTION, "Failed to save pcm file.");
                            return;
                        }
                        final AudioData audioData = task.getTranslationResult().getTranslationTTSData();
                        final int audioType = audioData == null ? Constants.AUDIO_TYPE_OPUS : audioData.getType();
                        final int source = audioData == null ? AudioData.SOURCE_PHONE_MIC : audioData.getSource();
                        if (audioType == Constants.AUDIO_TYPE_JLA_V2) { //JLA_V2编码
                            final String jlaFilePath = FileUtil.createFilePath(mContext, SConstant.DIR_TRANSLATION, task.getMac())
                                    + File.separator + filename + TranslateUtil.JLA_SUFFIX;
                            JLAV2Codec.encodeFile(pcmFilePath, jlaFilePath, new com.jieli.lib.audio.v2.callback.OnStateCallback() {
                                @Override
                                public void onStart() {

                                }

                                @Override
                                public void onStop(String content) {
                                    task.getTranslationResult().setTranslationTTSData(new AudioData(source, Constants.AUDIO_TYPE_JLA_V2,
                                            FileUtil.getBytes(jlaFilePath)));
                                    FileUtil.deleteFile(new File(pcmFilePath));
                                    FileUtil.deleteFile(new File(jlaFilePath));
                                    uiHandler.post(() -> {
                                        JL_Log.d(tag, "synthesizedDestText", "JLAV2Codec onStop");
                                        callbackTtsSuccess(task, null);
                                        synthesizedSrcText(task, filePaths);
                                    });
                                }

                                @Override
                                public void onError(int code, String message) {
                                    FileUtil.deleteFile(new File(pcmFilePath));
                                    for (String filePath : filePaths) {
                                        FileUtil.deleteFile(new File(filePath));
                                    }
                                    callbackTtsError(task, ErrorCode.SUB_ERR_OP_FAILED, AppUtil.formatString("Failed to encoding jla file." +
                                            "\ncode : %d, message : %s", code, message));
                                }
                            });
                        } else { //OPUS编码
                            final String opusFilePath = FileUtil.createFilePath(mContext, SConstant.DIR_TRANSLATION, task.getMac())
                                    + File.separator + filename + TranslateUtil.OPUS_SUFFIX;
                            OpusCodec.encodeFile(pcmFilePath, opusFilePath, new OnStateCallback() {
                                @Override
                                public void onStart() {

                                }

                                @Override
                                public void onComplete(String filePath) {
                                    task.getTranslationResult().setTranslationTTSData(new AudioData(source, Constants.AUDIO_TYPE_OPUS,
                                            FileUtil.getBytes(opusFilePath)));
                                    FileUtil.deleteFile(new File(pcmFilePath));
                                    FileUtil.deleteFile(new File(opusFilePath));
                                    uiHandler.post(() -> {
                                        JL_Log.d(tag, "synthesizedDestText", "OpusCodec onStop");
                                        callbackTtsSuccess(task, null);
                                        synthesizedSrcText(task, filePaths);
                                    });
                                }

                                @Override
                                public void onError(int code, String message) {
                                    FileUtil.deleteFile(new File(pcmFilePath));
                                    for (String filePath : filePaths) {
                                        FileUtil.deleteFile(new File(filePath));
                                    }
                                    callbackTtsError(task, ErrorCode.SUB_ERR_OP_FAILED, AppUtil.formatString("Failed to encoding opus file." +
                                            "\ncode : %d, message : %s", code, message));
                                }
                            });
                        }
                    } else { //TTS合成失败
                        callbackTtsError(task, stateResult.getCode(), stateResult.getMessage());
                    }
                }
            });
        }

        private void synthesizedSrcText(TtsTask task, List<String> filePaths) {
            if (null == task) return;
            TtsRequest request = task.buildTtsRequest(task.getTranslationResult().getSrcLanguage(),
                    task.getTranslationResult().getNlpText());
            if (request == null) {
                callbackTtsSuccess(task, filePaths);
                return;
            }
            mTtsGenerator.start(request, stateResult -> {
                if (stateResult.getState() == StateResult.STATE_FINISH) {
                    if (stateResult.isSuccess()) { //TTS合成成功
                        final byte[] data = stateResult.getData();
                        if (null == data || data.length == 0) {
                            callbackTtsSuccess(task, filePaths);
                            return;
                        }
                        long id = task.getTranslationResult().getId();
                        String filename = AppUtil.formatString("tts_src_%d_%s", id, TranslateUtil.getDateString());
                        String wavFilePath = FileUtil.createFilePath(mContext, SConstant.DIR_TRANSLATION, task.getMac())
                                + File.separator + filename + TranslateUtil.WAV_SUFFIX;
                        if (!WavUtil.pcmToWav(data, wavFilePath)) { //保存WAV文件
                            callbackTtsSuccess(task, filePaths);
                            return;
                        }
                        filePaths.add(wavFilePath);
                        callbackTtsSuccess(task, filePaths);
                    } else { //TTS合成失败
                        callbackTtsSuccess(task, filePaths);
                    }
                }
            });
        }

        /**
         * 处理TTS任务
         *
         * <p>
         * 1. 优先合成翻译文本，用于语音下发
         * 2. 再合成原文
         * </p>
         *
         * @param task TtsTask TTS任务
         */
        private synchronized void handleTtsTask(TtsTask task) {
            if (null == task) return;
            result = -1; //重置结果
            isCreateTts = true;
            List<String> filePaths = new ArrayList<>();
            synthesizedDestText(task, filePaths);
            if (result == -1) {
                JL_Log.d(tag, "handleTtsTask", "waiting for result callback");
                lockThread();
            }
            isCreateTts = false;
        }
    }
}
