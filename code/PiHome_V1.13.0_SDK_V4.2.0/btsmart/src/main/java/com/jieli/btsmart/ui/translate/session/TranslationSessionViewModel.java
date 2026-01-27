package com.jieli.btsmart.ui.translate.session;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.tool.room.repository.TranslationRepository;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * TranslationSessionFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译记录逻辑实现
 * @since 2025/8/29
 */
public class TranslationSessionViewModel extends BtBasicVM {

    /**
     * 查询一天的翻译会议记录
     */
    public static final int OP_QUERY_BY_DAY = 0x6542;
    /**
     * 查询一个月的翻译会议记录
     */
    public static final int OP_QUERY_BY_MONTH = 0x6543;

    /**
     * 操作设备
     */
    @NonNull
    private final BluetoothDevice mDevice;
    /**
     * 翻译记录数据库
     */
    private final TranslationRepository mRepository = TranslationRepository.getInstance();
    /**
     * 翻译会议记录回调
     */
    public final MutableLiveData<StateResult<List<TranslationSessionRecord>>> sessionRecordMLD = new MutableLiveData<>();
    /**
     * 删除会议记录回调
     */
    public final MutableLiveData<StateResult<Map<Integer, Boolean>>> deleteSessionMLD = new MutableLiveData<>();

    public TranslationSessionViewModel(@NonNull BluetoothDevice device) {
        mDevice = device;
    }

    @NonNull
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    public void querySessionRecordsByDay(int mode, @NonNull Date date) {
        final StateResult<List<TranslationSessionRecord>> stateResult = sessionRecordMLD.getValue();
        if (stateResult != null && stateResult.getState() == StateResult.STATE_WORKING) { //查询中
            return;
        }
        long startTime = TranslateUtil.getDayStartTime(date);
        long endTime = TranslateUtil.getDayEndTime(date);
        sessionRecordMLD.setValue(new StateResult<List<TranslationSessionRecord>>(OP_QUERY_BY_DAY)
                .setState(StateResult.STATE_WORKING).setCode(OpResult.RES_SUCCESS));
        mRepository.querySessionRecordByTime(mode, startTime, endTime, 1, records ->
                sessionRecordMLD.postValue(new StateResult<List<TranslationSessionRecord>>(OP_QUERY_BY_DAY)
                        .setState(StateResult.STATE_FINISH)
                        .setCode(OpResult.RES_SUCCESS)
                        .setData(records)));
    }


    public void querySessionRecordsByMonth(int mode, @NonNull Date date) {
        final StateResult<List<TranslationSessionRecord>> stateResult = sessionRecordMLD.getValue();
        if (stateResult != null && stateResult.getState() == StateResult.STATE_WORKING) { //查询中
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        int maxDaySize = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, maxDaySize);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long endTime = calendar.getTimeInMillis();
        sessionRecordMLD.setValue(new StateResult<List<TranslationSessionRecord>>(OP_QUERY_BY_MONTH)
                .setState(StateResult.STATE_WORKING).setCode(OpResult.RES_SUCCESS));
        mRepository.querySessionRecordByTime(mode, startTime, endTime, 1, records ->
                sessionRecordMLD.postValue(new StateResult<List<TranslationSessionRecord>>(OP_QUERY_BY_MONTH)
                        .setState(StateResult.STATE_FINISH)
                        .setCode(OpResult.RES_SUCCESS)
                        .setData(records)));
    }

    public void deleteSessionRecord(List<Integer> sessionIds) {
        final StateResult<Map<Integer, Boolean>> stateResult = deleteSessionMLD.getValue();
        if (null != stateResult && stateResult.getState() == StateResult.STATE_WORKING) {
            //正在删除记录
            return;
        }
        if (null == sessionIds || sessionIds.isEmpty()) {
            int code = ErrorCode.SUB_ERR_PARAMETER;
            deleteSessionMLD.postValue(new StateResult<Map<Integer, Boolean>>()
                    .setState(StateResult.STATE_FINISH)
                    .setCode(code)
                    .setMessage(ErrorCode.code2Msg(code)));
            return;
        }
        deleteSessionMLD.setValue(new StateResult<Map<Integer, Boolean>>()
                .setState(StateResult.STATE_WORKING).setCode(OpResult.RES_SUCCESS));
        mRepository.removeTranslationSession(sessionIds, resultMap ->
                deleteSessionMLD.postValue(new StateResult<Map<Integer, Boolean>>()
                        .setState(StateResult.STATE_FINISH)
                        .setCode(OpResult.RES_SUCCESS)
                        .setData(resultMap)));
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final BluetoothDevice mDevice;

        public Factory(BluetoothDevice device) {
            mDevice = device;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new TranslationSessionViewModel(mDevice);
        }
    }
}