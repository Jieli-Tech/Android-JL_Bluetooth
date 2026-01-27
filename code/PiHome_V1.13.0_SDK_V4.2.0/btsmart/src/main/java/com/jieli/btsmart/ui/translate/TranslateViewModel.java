package com.jieli.btsmart.ui.translate;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.record.RecordParam;
import com.jieli.bluetooth.bean.record.RecordState;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.bean.translation.AudioData;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.bean.translation.TranslationResult;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.impl.rcsp.record.RecordOpImpl;
import com.jieli.bluetooth.impl.rcsp.translation.TranslationImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallbackManager;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.record.OnRecordStateCallback;
import com.jieli.bluetooth.interfaces.rcsp.translation.TranslationCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.RoleInfo;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSession;
import com.jieli.btsmart.data.model.translation.ai_auth.AIAuthMessage;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.tool.room.repository.TranslationRepository;
import com.jieli.btsmart.tool.translate.AITranslationImpl;
import com.jieli.btsmart.tool.translate.TranslateState;
import com.jieli.btsmart.tool.translate.TranslationStateCallback;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import java.util.ArrayList;
import java.util.List;

/**
 * TranslateViewModel
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译功能逻辑实现
 * @since 2025/5/27
 */
public class TranslateViewModel extends BtBasicVM {
    /**
     * 进入翻译模式操作
     */
    public static final int OP_ENTER_MODE = 0x20;
    /**
     * 退出翻译模式操作
     */
    public static final int OP_EXIT_MODE = 0x21;
    /**
     * 调节音量
     */
    public static final int OP_ADJUST_VOLUME = 0x22;
    /**
     * 设备录音
     */
    public static final int OP_DEVICE_RECORD = 0x23;
    /**
     * AI翻译
     */
    public static final int OP_AI_TRANSLATION = 0x24;
    /**
     * 同步翻译模式
     */
    public static final int OP_SYNC_TRANSLATION_MODE = 0x25;

    /**
     * 工作时间计数消息
     */
    private static final int MSG_COUNT_WORK_TIME = 0x3211;

    private static final int INIT_SESSION_ID = -1;

    /**
     * 操作设备
     */
    @NonNull
    private final BluetoothDevice mDevice;
    /**
     * AI云翻译代理实现
     */
    private final AITranslationImpl mAITranslation;
    /**
     * 翻译功能实现
     */
    private final TranslationImpl mTranslation;
    /**
     * 翻译数据库操作
     */
    private final TranslationRepository mRepository;
    /**
     * 配置管理工具
     */
    private final ConfigureKit mConfigureKit;
    /**
     * 录音
     */
    private RecordOpImpl mRecordOp;

    /**
     * 初始化结果回调
     */
    public final MutableLiveData<Integer> initMLD = new MutableLiveData<>();
    /**
     * 静音状态回调
     */
    public final MutableLiveData<Boolean> muteStateMLD = new MutableLiveData<>();
    /**
     * 模式切换回调
     */
    public final MutableLiveData<TranslationMode> modeChangeMLD = new MutableLiveData<>();
    /**
     * 工作时间回调
     */
    public final MutableLiveData<Integer> workTimeMLD = new MutableLiveData<>();
    /**
     * 操作结果回调
     */
    public final MutableLiveData<OpResult<Object>> opResultMLD = new MutableLiveData<>();
    /**
     * 翻译状态回调
     */
    public final MutableLiveData<TranslateState> translateStateMLD = new MutableLiveData<>();
    /**
     * 翻译结果回调
     */
    public final MutableLiveData<OpResult<TranslationRecord>> translationRecordMLD = new MutableLiveData<>();
    /**
     * 设备录音回调
     */
    public final MutableLiveData<StateResult<Boolean>> deviceRecordStateMLD = new MutableLiveData<>();
    /**
     * 保存会议记录结果回调
     */
    public final MutableLiveData<OpResult<Integer>> saveSessionRecordMLD = new MutableLiveData<>();

    /**
     * 翻译会议ID
     */
    private int sessionId = INIT_SESSION_ID;
    /**
     * 当前音量
     */
    private final int currentVol;

    /**
     * UI处理
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_COUNT_WORK_TIME) {
            handleWorkTime(msg.arg1);
        }
        return true;
    });

    /**
     * 翻译事件回调
     */
    private final TranslationCallback mTranslationCallback = new TranslationCallback() {
        @Override
        public void onModeChange(@NonNull BluetoothDevice device, @NonNull TranslationMode mode) {
            if (!BluetoothUtil.deviceEquals(device, mDevice)) return;
            JL_Log.d(tag, "onModeChange by device", "device : " + device + ", " + mode);
            handleModeChange(mode);
        }

        @Override
        public void onReceiveAudioData(@NonNull BluetoothDevice device, @NonNull AudioData audioData) {

        }

        @Override
        public void onError(BluetoothDevice device, int code, String message) {
            JL_Log.w(tag, "onError by device", "device : " + printDeviceInfo(device) + ", code : " + code + ", " + message);
        }
    };

    /**
     * RCSP事件回调
     */
    private final BTRcspEventCallbackManager mRcspEventCallbackManager = new BTRcspEventCallbackManager() {

        @Override
        public void onPhoneCallStatusChange(BluetoothDevice device, int status) {
            //回调来电状态
            final TranslationMode mode = getTranslationMode();
            final int translationMode = TranslateUtil.getTranslationMode(mode);
            if (null != mode && isTranslating() && status == 1 && translationMode != TranslationMode.MODE_CALL_TRANSLATION
                    && TranslateUtil.isPhoneInCall(getContext())) {
                //非通话翻译模式，翻译过程中来电， APP主动退出翻译模式
                JL_Log.w(tag, "onPhoneCallStatusChange", "device : " + device + ", status : " + status + ",\n Because the translation mode is interrupted by the incoming call.");
                exitMode();
            }
        }
    };

    public TranslateViewModel(@NonNull BluetoothDevice device) {
        this.mDevice = device;
        mRepository = TranslationRepository.getInstance();
        mConfigureKit = ConfigureKit.getInstance();
        mAITranslation = new AITranslationImpl(getEdrAddress());
        mAITranslation.setTranslateCallback(new TranslationStateCallback() {

            @Override
            public void onTranslateState(TranslateState state) {
                if (state == TranslateState.STATE_WORKING) {
                    if (!mHandler.hasMessages(MSG_COUNT_WORK_TIME)) {
                        final Integer value = workTimeMLD.getValue();
                        int time = value == null ? 0 : value;
                        mHandler.sendMessage(mHandler.obtainMessage(MSG_COUNT_WORK_TIME, time, 0));
                    }
                } else {
                    mHandler.removeMessages(MSG_COUNT_WORK_TIME);
                }
                translateStateMLD.postValue(state);
            }

            @Override
            public void onTranslateRecord(@NonNull TranslationRecord record) {
                translationRecordMLD.postValue(new OpResult<TranslationRecord>(OP_AI_TRANSLATION)
                        .setCode(ErrorCode.ERR_NONE).setData(record));
            }

            @Override
            public void onStart() {

            }

            @Override
            public void onTranslateResult(@NonNull TranslationResult result) {

            }

            @Override
            public void onTranslateError(long id, int code, String message) {
                translationRecordMLD.postValue(new OpResult<TranslationRecord>(OP_AI_TRANSLATION)
                        .setCode(code).setMessage(message));
            }

            @Override
            public void onStop(int reason, String message) {

            }
        });
        mTranslation = new TranslationImpl(mRCSPController.getRcspOp(), mAITranslation);
        int volume = getCurrentVolume();
        currentVol = volume == 0 ? getMaxVolume() / 2 : volume;
        muteStateMLD.postValue(volume == 0);
        if (!mTranslation.isSupportTranslation()) {
            initMLD.postValue(ErrorCode.SUB_ERR_UNSUPPORTED_FUNCTION);
            return;
        }
        mRCSPController.addBTRcspEventCallback(mRcspEventCallbackManager);
        mTranslation.addTranslationCallback(mTranslationCallback);
        syncDeviceState();
        initMLD.postValue(ErrorCode.ERR_NONE);
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public String getEdrAddress() {
        return getEdrAddress(mDevice);
    }

    @Override
    public void release() {
        if (sessionId != INIT_SESSION_ID) {
            handleModeChange(new TranslationMode(TranslationMode.MODE_IDLE));
        }
        mRCSPController.removeBTRcspEventCallback(mRcspEventCallbackManager);
        mTranslation.removeTranslationCallback(mTranslationCallback);
        mAITranslation.setTranslateCallback(null);
        mAITranslation.stopTranslating();
        mTranslation.destroy();
        super.release();
    }

    public boolean isInCalling() {
        DeviceInfo deviceInfo = getDeviceInfo(mDevice);
        if (null == deviceInfo) {
            JL_Log.d(tag, "isInCalling", "no device info.");
            return false;
        }
        JL_Log.d(tag, "isInCalling", "PhoneStatus : " + deviceInfo.getPhoneStatus());
        return TranslateUtil.isPhoneInCall(getContext()) || deviceInfo.getPhoneStatus() == 1;
    }

    public boolean isTwsConnected() {
        final ADVInfoResponse advInfo = mRCSPController.getADVInfo(mDevice);
        if (null == advInfo) return false;
        return advInfo.getLeftDeviceQuantity() > 0 && advInfo.getRightDeviceQuantity() > 0;
    }

    public boolean isSupportCallTranslationWithStereo() {
        return mTranslation.isSupportCallTranslationWithStereo();
    }

    public boolean isTranslating() {
        return mTranslation.isWorking();
    }

    public boolean isMute() {
        return muteStateMLD.getValue() == Boolean.TRUE;
    }

    public AIAuthMessage getAIAuthMessage() {
        return mConfigureKit.getAIAuthMessage();
    }

    public void updateAIAuthMessage(AIAuthMessage authMessage) {
        mConfigureKit.updateAIAuthMessage(authMessage);
    }

    public TranslationMode getTranslationMode() {
        return mTranslation.getTranslationMode();
    }

    public int getCurrentVolume() {
        DeviceInfo deviceInfo = getDeviceInfo(mDevice);
        if (null == deviceInfo) return 0;
        return deviceInfo.getVolume();
    }

    public int getMaxVolume() {
        DeviceInfo deviceInfo = getDeviceInfo(mDevice);
        if (null == deviceInfo) return 0;
        return deviceInfo.getMaxVol();
    }

    public void setMuteState(boolean enable) {
        final int volume = enable ? 0 : currentVol;
        JL_Log.d(tag, "setMuteState", "enable : " + enable + ", volume : " + volume);
        mRCSPController.adjustVolume(mDevice, volume, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                opResultMLD.postValue(new OpResult<>(OP_ADJUST_VOLUME).setCode(ErrorCode.ERR_NONE));
                muteStateMLD.postValue(volume == 0);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                opResultMLD.postValue(new OpResult<>(OP_ADJUST_VOLUME).setCode(error.getSubCode()).setMessage(error.getMessage()));
            }
        });
    }

    public void enterMode(@NonNull TranslationMode translationMode, @NonNull String srcLanguage, @NonNull String destLanguage) {
        String srcLang;
        String destLang;
        int mode = TranslateUtil.getTranslationMode(translationMode);
        if (mode == TranslationMode.MODE_CALL_TRANSLATION) {
            srcLang = destLanguage;
            destLang = srcLanguage;
        } else {
            srcLang = srcLanguage;
            destLang = destLanguage;
        }
        List<String> list = new ArrayList<>();
        list.add(destLang);
        mAITranslation.updateLanguage(srcLang, list);
        JL_Log.d(tag, "enterMode", "srcLanguage : " + srcLang + ", destLanguage : " + destLang + "\n" + translationMode);
        mTranslation.enterMode(translationMode, new TranslationCallback() {
            @Override
            public void onModeChange(@NonNull BluetoothDevice device, @NonNull TranslationMode mode) {
                JL_Log.d(tag, "onModeChange by SDK", "device : " + printDeviceInfo(device) + ", " + mode);
                if (mode.getMode() == translationMode.getMode()) {
                    opResultMLD.postValue(new OpResult<>(OP_ENTER_MODE)
                            .setCode(OpResult.RES_SUCCESS).setData(mode));
                }
                handleModeChange(mode);
            }

            @Override
            public void onReceiveAudioData(@NonNull BluetoothDevice device, @NonNull AudioData audioData) {
//                JL_Log.v(tag, "onReceiveAudioData by SDK", "device : " + printDeviceInfo(device) + ", " + audioData);
            }

            @Override
            public void onError(BluetoothDevice device, int code, String message) {
                JL_Log.w(tag, "onError by SDK", "device : " + printDeviceInfo(device) + ", code : " + code + ", " + message);
                opResultMLD.postValue(new OpResult<>(OP_ENTER_MODE)
                        .setCode(code).setMessage(message));
            }
        });
    }

    public void exitMode() {
        exitMode(false);
    }

    public void exitMode(boolean isNoSaveRecord) {
        if (!isTranslating()) return;
        JL_Log.d(tag, "exitMode", getTranslationMode() + ", isNoSaveRecord : " + isNoSaveRecord);
        if (isNoSaveRecord && sessionId != INIT_SESSION_ID) {
            mRepository.removeTranslationSession(sessionId, ret -> {
                JL_Log.d(tag, "exitMode", "remove session success. id = " + sessionId);
                sessionId = INIT_SESSION_ID;
                exitMode();
            });
            return;
        }
        mTranslation.exitMode(new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                opResultMLD.postValue(new OpResult<>(OP_EXIT_MODE)
                        .setCode(OpResult.RES_SUCCESS));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                opResultMLD.postValue(new OpResult<>(OP_EXIT_MODE)
                        .setCode(error.getSubCode()).setMessage(error.getMessage()));
            }
        });
    }

    public void writeAudioData(AudioData audioData) {
        if (null == audioData) return;
        final TranslationMode mode = getTranslationMode();
        if (null == mode || mode.getMode() <= TranslationMode.MODE_RECORD) return;
        if (mAITranslation != null && mAITranslation.isWorking()) {
            mAITranslation.writeAudio(audioData);
        }
    }

    public void writePcmFile(String pcmPath) {
        if (null == mAITranslation) return;
        TranslationMode mode = getTranslationMode();
        if (null == mode || mode.getMode() == TranslationMode.MODE_IDLE) return;
        mAITranslation.writeAudioFile(new RoleInfo(mode.getMode(), RoleInfo.ROLE_PHONE), pcmPath);
    }

    public boolean isDeviceRecoding() {
        return mRecordOp != null && mRecordOp.isRecording(mDevice);
    }

    public boolean startDeviceRecording() {
        final TranslationMode mode = getTranslationMode();
        if (null == mode || mode.getMode() != TranslationMode.MODE_FACE_TO_FACE_TRANSLATION || null == mRecordOp) {
            JL_Log.i(tag, "startDeviceRecording", "Not in face-to-face mode.");
            return false;
        }
        if (isDeviceRecoding()) {
            JL_Log.i(tag, "startDeviceRecording", "Recording");
            return false;
        }
        RecordParam param = new RecordParam(mode.getAudioType(), mode.getSampleRate() / 1000, RecordParam.VAD_WAY_SDK);
        mRecordOp.startRecord(mDevice, param, null);
        return true;
    }

    public boolean stopDeviceRecording() {
        if (mRecordOp == null) return false;
        if (isDeviceRecoding()) {
            mRecordOp.stopRecord(mDevice, 0, null);
            return true;
        }
        return false;
    }

    public boolean isTextTranslating() {
        return null != mAITranslation && mAITranslation.isTextTranslating();
    }

    public boolean translateText(String text) {
        if (null == mAITranslation || null == text || text.isEmpty()) return false;
        TranslationMode mode = getTranslationMode();
        if (null == mode || mode.getMode() == TranslationMode.MODE_IDLE) return false;
        List<String> list = new ArrayList<>();
        list.add(text);
        return mAITranslation.translateText(new RoleInfo(mode.getMode(), RoleInfo.ROLE_PHONE), list);
    }

    public boolean isPaused() {
        return mAITranslation != null && mAITranslation.isPaused();
    }

    public void pauseTranslate() {
        if (null == mAITranslation) return;
        mAITranslation.pauseTranslate();
    }

    public void resumeTranslate() {
        if (null == mAITranslation) return;
        mAITranslation.resumeTranslate();
    }

    private void syncDeviceState() {
        mAITranslation.setUseA2DP(mTranslation.isUseA2DPPlay());
        //同步当前翻译模式
        mTranslation.requestTranslationMode(new OnRcspActionCallback<TranslationMode>() {
            @Override
            public void onSuccess(BluetoothDevice device, TranslationMode message) {
                opResultMLD.postValue(new OpResult<>(OP_SYNC_TRANSLATION_MODE)
                        .setCode(ErrorCode.ERR_NONE)
                        .setData(message));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                opResultMLD.postValue(new OpResult<>(OP_SYNC_TRANSLATION_MODE)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
        //同步电话状态
        syncPhoneState();
    }

    private void syncPhoneState() {
        JL_Log.d(tag, "syncPhoneState", "start");
        mRCSPController.getDevSysInfo(mDevice, AttrAndFunCode.SYS_INFO_FUNCTION_PUBLIC,
                1 << AttrAndFunCode.SYS_INFO_ATTR_PHONE_STATUS, null);
    }

    private void handleModeChange(TranslationMode mode) {
        if (mode.getMode() != TranslationMode.MODE_IDLE) {
            long currentTime = TranslateUtil.currentTime();
            int translationMode = TranslateUtil.getTranslationMode(mode);
            TranslationSession session = new TranslationSession();
            session.setMac(getEdrAddress());
            session.setTranslationMode(translationMode);
            session.setStartTime(currentTime);
            session.setTitle(TranslateUtil.formatSessionTime(currentTime));
            session.setEndTime(0);
            JL_Log.d(tag, "handleModeChange", "try to insert session. " + session);
            //保存翻译会议信息
            mRepository.addTranslationSession(session, id -> {
                sessionId = id;
                mAITranslation.setSessionId(id);
                mHandler.removeMessages(MSG_COUNT_WORK_TIME);
                mHandler.sendMessage(mHandler.obtainMessage(MSG_COUNT_WORK_TIME, 0, 0));
                JL_Log.d(tag, "handleModeChange", "insert session success. id = " + id);
            });
            if (mode.getMode() == TranslationMode.MODE_FACE_TO_FACE_TRANSLATION) {
                JL_Log.d(tag, "handleModeChange", "init record operation.");
                if (null == mRecordOp) {
                    mRecordOp = new RecordOpImpl(mRCSPController.getRcspOp());
                    mRecordOp.addOnRecordStateCallback(mRecordStateCallback);
                }
            }
        } else {
            if (mRecordOp != null) {
                mRecordOp.removeOnRecordStateCallback(mRecordStateCallback);
                mRecordOp.destroy();
                mRecordOp = null;
            }
            if (sessionId != INIT_SESSION_ID) {
                mRepository.queryTranslationRecords(sessionId, 0, translationRecords -> {
                    final TranslationMode translationMode = modeChangeMLD.getValue();
                    int cacheMode = TranslateUtil.getTranslationMode(translationMode);
                    if (translationRecords.isEmpty() || (translationRecords.size() == 1 && cacheMode == TranslationMode.MODE_CALL_TRANSLATION)) {
                        //翻译记录为空，无效的翻译会话，删除数据库记录
                        mRepository.removeTranslationSession(sessionId, result -> {
                            JL_Log.d(tag, "handleModeChange", "remove session success. id = " + sessionId);
                            sessionId = INIT_SESSION_ID;
                            int code = ErrorCode.SUB_ERR_OP_FAILED;
                            saveSessionRecordMLD.postValue(new OpResult<Integer>().setCode(code)
                                    .setMessage(ErrorCode.code2Msg(code)));
                        });
                    } else {
                        //有效翻译记录，更新结束时间
                        mRepository.updateSessionEndTime(sessionId, TranslateUtil.currentTime(), result -> {
                            JL_Log.d(tag, "handleModeChange", "update session success. id = " + sessionId);
                            saveSessionRecordMLD.postValue(new OpResult<Integer>().setCode(OpResult.RES_SUCCESS)
                                    .setData(sessionId));
                        });
                    }
                });
            }
        }
        modeChangeMLD.postValue(mode);
    }

    private void handleWorkTime(int time) {
        workTimeMLD.postValue(time);
        if (isTranslating() && !isPaused()) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_COUNT_WORK_TIME, time + 1, 0), 1000L);
        }
    }

    private final OnRecordStateCallback mRecordStateCallback = new OnRecordStateCallback() {
        @Override
        public void onStateChange(BluetoothDevice device, RecordState state) {
            JL_Log.d(tag, "onStateChange", "" + state);
            switch (state.getState()) {
                case RecordState.RECORD_STATE_START: {
                    final TranslationMode mode = getTranslationMode();
                    int translationMode = null == mode ? TranslationMode.MODE_IDLE : mode.getMode();
                    mAITranslation.startDeviceRecordDecoder(new RoleInfo(translationMode, RoleInfo.ROLE_DEVICE));
                    deviceRecordStateMLD.postValue(new StateResult<Boolean>(OP_DEVICE_RECORD).setState(StateResult.STATE_WORKING)
                            .setCode(0));
                    break;
                }
                case RecordState.RECORD_STATE_WORKING: {
                    RecordParam param = state.getRecordParam();
                    byte[] data = state.getVoiceDataBlock();
                    mAITranslation.writeAudio(new AudioData(AudioData.SOURCE_DEVICE_MIC, param.getVoiceType(), data));
                    break;
                }
                case RecordState.RECORD_STATE_IDLE: {
                    int code = state.getReason();
                    String message = state.getMessage();
                    mAITranslation.stopDeviceRecordDecoder();
                    deviceRecordStateMLD.postValue(new StateResult<Boolean>(OP_DEVICE_RECORD).setState(StateResult.STATE_FINISH)
                            .setCode(code).setMessage(message));
                    break;
                }
            }
        }
    };

    public static class Factory implements ViewModelProvider.Factory {
        private final BluetoothDevice device;

        public Factory(BluetoothDevice device) {
            this.device = device;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new TranslateViewModel(device);
        }
    }
}