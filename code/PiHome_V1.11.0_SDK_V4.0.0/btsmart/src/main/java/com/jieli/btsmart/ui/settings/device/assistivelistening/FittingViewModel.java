package com.jieli.btsmart.ui.settings.device.assistivelistening;

import android.bluetooth.BluetoothDevice;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.hearing.HearingAssistInfo;
import com.jieli.bluetooth.bean.device.hearing.HearingFrequencyInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.room.entity.HearingAidFittingRecordEntity;

import java.util.Arrays;

/**
 * @ClassName: FittingViewModel
 * @Description: 辅听验配ViewModel
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/7/3 16:03
 */
public class FittingViewModel extends HearingAssitstViewModel {
    private String TAG = "FittingViewModel";
    public final MutableLiveData<Boolean> mBackLastStepEnableMLD = new MutableLiveData<>(false);
    public final MutableLiveData<Integer> mCurrentFittingPositionMLD = new MutableLiveData<>();
    public final MutableLiveData<Boolean> mIsFittingFinishMLD = new MutableLiveData<>();
    public final MutableLiveData<float[]> mLeftChannelsValuesMLD = new MutableLiveData<>();
    public final MutableLiveData<float[]> mRightChannelsValuesMLD = new MutableLiveData<>();
    private boolean mIsFinishFitting = true;
    private HearingAidFittingRecordEntity mHearingAidFittingRecordEntity;
    private final int MAX_DB = 100;
    private final int DEFAULT_DB = 50;//默认起始DB
    private final int EACH_GEAR_DB = 10;//每档的db值
    private int mCurrentChannel = -1;//当前通道
    private int mCurrentFreq = -1;//当前频率
    private int mCurrentGain = -1;//当前频率的db
    private int mFreqIndex = 0;
    private int mChannelsNum = -1;
    private HearingAssistInfo mHearingAssistInfo;
    private boolean isImported = false;
    private float aUpper = DEFAULT_DB;
    private float aLower = DEFAULT_DB;
    private float JUDGMENT_RANGE = 0.25f;//认为是相等的判定范围
    private float aTemp = -1;
    private int count = 0;
    private int step = 1;

    private boolean isResponseOrTimeout = true;//是否响应或者超时
    private boolean isUpperDirection = true;//起始方向是向上加还是向下减

    public ADVInfoResponse getADVInfoResponse() {
        return getRCSPController().getADVInfo(getRCSPController().getUsingDevice());
    }

    public void setFittingConfigure(HearingAssistInfo hearingAssistInfo, String devMac) {
        mHearingAssistInfo = hearingAssistInfo;
        mHearingAidFittingRecordEntity = new HearingAidFittingRecordEntity();
        mHearingAidFittingRecordEntity.recordKey = HearingAidFittingRecordEntity.createKey(devMac, mHearingAssistInfo.getChannels());
        mHearingAidFittingRecordEntity.version = mHearingAssistInfo.getVersion();
        mHearingAidFittingRecordEntity.channelsNum = mHearingAssistInfo.getChannels();
        mHearingAidFittingRecordEntity.channelsFreqs = mHearingAssistInfo.getFrequencies();
        mHearingAidFittingRecordEntity.leftChannelsValues = new float[0];
        mHearingAidFittingRecordEntity.rightChannelsValues = new float[0];
        mCurrentChannel = 0;
        mCurrentFreq = mHearingAssistInfo.getFrequencies()[0];
        mCurrentGain = DEFAULT_DB;
        mFreqIndex = 0;
        mChannelsNum = mHearingAssistInfo.getChannels();
    }

    public void setFittingGainsType(int gainsType) {
        mHearingAidFittingRecordEntity.gainsType = gainsType;
        if (gainsType == 1) {
            mCurrentChannel = 1;
        }
    }

    public void importRecord(HearingAidFittingRecordEntity entity) {
        isImported = true;
        mHearingAidFittingRecordEntity = entity;
        float[] currentFittingChannels;
        if (entity.gainsType == 0) {
            mCurrentChannel = 0;
        } else if (entity.gainsType == 1) {
            mCurrentChannel = 1;
        } else if (entity.gainsType == 2) {
            mCurrentChannel = (entity.rightChannelsValues != null && entity.rightChannelsValues.length > 0) ? 1 : 0;
            mCurrentFittingPositionMLD.setValue(mCurrentChannel);
        }
        currentFittingChannels = mCurrentChannel == 0 ? entity.leftChannelsValues : entity.rightChannelsValues;
        mFreqIndex = currentFittingChannels.length - 1;
        if (entity.gainsType == 2 && mCurrentChannel == 1) {//双耳 且 验配到右耳
            mFreqIndex += entity.leftChannelsValues.length;
            mCurrentFittingPositionMLD.setValue(mCurrentChannel);
        }
        if (currentFittingChannels != null) {
            mCurrentFreq = entity.channelsFreqs[currentFittingChannels.length - 1];
            if (mFreqIndex > 0) {//不是左耳第一频率
                mBackLastStepEnableMLD.setValue(true);
            }
            currentFittingChannels[currentFittingChannels.length - 1] = DEFAULT_DB;
        }
        mLeftChannelsValuesMLD.setValue(entity.leftChannelsValues);
        mRightChannelsValuesMLD.setValue(entity.rightChannelsValues);
    }

    /**
     * 开始验配
     */
    public void startFitting() {
        mIsFinishFitting = false;
        fittingFreq(mCurrentChannel, mCurrentFreq, mCurrentGain, !isImported);
    }

    /**
     * 返回上一步
     */
    public boolean backLastStep() {
        JL_Log.d(TAG, "backLastStep", "");
        if (!isResponseOrTimeout) return false;//等待设备响应
        if (isFinishedFitting()) return true;//验配结束
        if (mFreqIndex > 0) {
            resetFreqFitting();
            mFreqIndex--;
            int channelIndex = mFreqIndex % mChannelsNum;
            if (channelIndex + 1 == mChannelsNum && mCurrentChannel == 1 && mHearingAidFittingRecordEntity.gainsType == 2) {//切换通道
                mCurrentChannel = 0;
                mCurrentFittingPositionMLD.setValue(mCurrentChannel);
                //清空右耳频率
                mHearingAidFittingRecordEntity.rightChannelsValues = new float[0];
                mRightChannelsValuesMLD.setValue(mHearingAidFittingRecordEntity.rightChannelsValues);
            }
            mCurrentFreq = mHearingAssistInfo.getFrequencies()[channelIndex];
            mCurrentGain = DEFAULT_DB;
            if (mFreqIndex == 0) {
                mBackLastStepEnableMLD.setValue(false);
            }
            fittingFreq(mCurrentChannel, mCurrentFreq, mCurrentGain, true);
        }
        return true;
    }

    /**
     * 当前频率听见
     */
    public boolean fittingFreqHear() {
        JL_Log.d(TAG, "fittingFreqHear", "当前频率听得见");
        if (!isResponseOrTimeout) return false;//等待设备响应
        if (isFinishedFitting()) return true;
        ;//验配结束
        float db = -100;
        switch (step) {
            case 1:
                aLower -= 10;
                step = 2;
                isUpperDirection = false;
                db = aLower;
                break;
            case 2:
                if (isUpperDirection) {//方向向上加
                    step = 3;
                    aTemp = (aUpper + aLower) / 2;
                    db = aTemp;
                } else {//方向向下减
                    aUpper = aLower;
                    aLower -= 10;
                    db = aLower;
                    if (Math.abs(aUpper - 0) < JUDGMENT_RANGE) {//近似值为0.25
                        enterNextFreq(aUpper);
                        return true;
                    }
                }
                break;
            case 3:
                if (isUpperDirection) {//方向向上加
                    aUpper = aTemp;
                    count = 0;
                    if (Math.abs(aTemp - aLower) < JUDGMENT_RANGE) {
                        enterNextFreq(aTemp);
                        return true;
                    }
                    aTemp = (aUpper + aLower) / 2;
                    db = aTemp;
                } else {
                    aUpper = aTemp;
                    count++;
                    if (count == 2) {
                        enterNextFreq(aTemp);
                        return true;
                    }
                    aTemp = (aUpper + aLower) / 2;
                    db = aTemp;
                }
                break;
        }
        fittingFreq(mCurrentChannel, mCurrentFreq, db, true);
        return true;
    }

    /**
     * 当前频率听不见
     */
    public boolean fittingFreqInaudibility() {
        JL_Log.d(TAG, "fittingFreqInaudibility", "当前频率听不见");
        if (!isResponseOrTimeout) return false;//等待设备响应
        if (isFinishedFitting()) return true;
        ;//验配结束
        float db = -100;
        switch (step) {
            case 1:
                aUpper += 10;
                step = 2;
                isUpperDirection = true;
                db = aUpper;
                break;
            case 2:
                if (!isUpperDirection) {// 方向向下减
                    step = 3;
                    aTemp = (aUpper + aLower) / 2;
                    db = aTemp;
                } else {//方向向上加
                    aLower = aUpper;
                    aUpper += 10;
                    db = aUpper;
                    if (Math.abs(aLower - 100) < JUDGMENT_RANGE) {
                        enterNextFreq(aLower);
                        return true;
                    }
                }
                break;
            case 3:
                if (isUpperDirection) {//方向向上加
                    aLower = aTemp;
                    count++;
                    if (count == 2) {
                        enterNextFreq(aTemp);
                        return true;
                    }
                    aTemp = (aUpper + aLower) / 2;
                    db = aTemp;
                } else {
                    aLower = aTemp;
                    count = 0;
                    if (Math.abs(aTemp - aUpper) < JUDGMENT_RANGE) {
                        enterNextFreq(aTemp);
                        return true;
                    }
                    aTemp = (aUpper + aLower) / 2;
                    db = aTemp;
                }
                break;
        }
        JL_Log.d(TAG, "fittingFreqInaudibility", "mCurrentFreq:" + mCurrentFreq + " mCurrentChannel : " + mCurrentChannel + " step: " + step);
        JL_Log.d(TAG, "fittingFreqInaudibility", "aUpper:" + aUpper + " aTemp : " + aTemp + " aLower: " + aLower);
        fittingFreq(mCurrentChannel, mCurrentFreq, db, true);
        return true;
    }

    public boolean isFinishedFitting() {
        return mIsFinishFitting;
    }

    /**
     * 导出记录
     */
    public HearingAidFittingRecordEntity export() {
        return mHearingAidFittingRecordEntity;
    }

    private void enterNextFreq(float resultDB) {
        mFreqIndex++;
        mBackLastStepEnableMLD.setValue(true);
        int channelsSum = mHearingAidFittingRecordEntity.gainsType == 2 ? 2 * mChannelsNum : mChannelsNum;
        boolean isLast = mFreqIndex == channelsSum;//是不是最后一个频率
        JL_Log.d(TAG, "enterNextFreq", "resultDB : " + resultDB + " isLast: " + isLast);
        if (!isLast) {
            //切换到下一个频率
            int channelIndex = mFreqIndex % mChannelsNum;
            if (mHearingAidFittingRecordEntity.gainsType == 2 && mFreqIndex == mChannelsNum) {//  是否要切换通道
                mCurrentChannel = 1;
                mCurrentFittingPositionMLD.setValue(mCurrentChannel);
            }
            mCurrentFreq = mHearingAssistInfo.getFrequencies()[channelIndex];
            mCurrentGain = DEFAULT_DB;
            resetFreqFitting();
            fittingFreq(mCurrentChannel, mCurrentFreq, mCurrentGain, true);
        } else {
            stopFitting();
            mIsFinishFitting = true;
            mIsFittingFinishMLD.setValue(true);
        }
    }

    /**
     * 重置频率的标记
     */
    private void resetFreqFitting() {
        aUpper = DEFAULT_DB;
        aLower = DEFAULT_DB;
        aTemp = -1;
        count = 0;
        step = 1;
    }

    /**
     * 验配频率的增益
     */
    private void fittingFreq(int channel, int freq, float gain, boolean isNotify) {
        JL_Log.d(TAG, "fittingFreq", "gain : " + gain);
        isResponseOrTimeout = false;
        updateChannelFreqGain(channel, freq, gain, isNotify);
        HearingFrequencyInfo hearingFrequencyInfo = new HearingFrequencyInfo();
        hearingFrequencyInfo.setChannel(channel);
        hearingFrequencyInfo.setFrequency(freq);
        hearingFrequencyInfo.setLeftChannelSwitch(channel == 0);
        hearingFrequencyInfo.setRightChannelSwitch(channel == 1);
        hearingFrequencyInfo.setGain(gain);
        setHearingAssistFrequency(hearingFrequencyInfo, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                isResponseOrTimeout = true;
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                isResponseOrTimeout = true;
            }
        });
    }

    /**
     * 停止验配频率
     */
    public void stopFitting() {
        JL_Log.d(TAG, "stopFitting", "");
        stopHearingAssistFitting(null);
    }

    private int getFreqIndex(int freq, int[] frequencies) {
        int index = -1;
        for (int i = 0; i < frequencies.length; i++) {
            int temp = frequencies[i];
            if (temp == freq) {
                index = i;
                break;
            }
        }
        return index;
    }

    private void updateChannelFreqGain(int channel, int freq, float gain, boolean isNotify) {
        float[] tempArray = channel == 0 ? mHearingAidFittingRecordEntity.leftChannelsValues : mHearingAidFittingRecordEntity.rightChannelsValues;
        int freqIndex = getFreqIndex(freq, mHearingAidFittingRecordEntity.channelsFreqs);
        boolean isSavedFreq = tempArray.length == freqIndex + 1;
        boolean isRemoveFreq = tempArray.length > freqIndex + 1;
        JL_Log.d(TAG, "updateChannelFreqGain", "tempArray.length : " + tempArray.length + " freqIndex: " + freqIndex);
        if (isRemoveFreq) {//降频率
            tempArray = Arrays.copyOf(tempArray, tempArray.length - 1);
        } else if (!isSavedFreq || tempArray.length == 0) {//升频率
            tempArray = Arrays.copyOf(tempArray, tempArray.length + 1);
        }
        tempArray[tempArray.length - 1] = gain;
        if (channel == 0) {
            mHearingAidFittingRecordEntity.leftChannelsValues = tempArray;
            if (isNotify) {
                mLeftChannelsValuesMLD.setValue(tempArray);
            }
        } else {
            mRightChannelsValuesMLD.setValue(tempArray);
            if (isNotify) {
                mHearingAidFittingRecordEntity.rightChannelsValues = tempArray;
            }
        }
    }

}
