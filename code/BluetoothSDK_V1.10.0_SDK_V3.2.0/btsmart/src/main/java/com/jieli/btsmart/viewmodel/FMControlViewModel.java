package com.jieli.btsmart.viewmodel;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.AttrBean;
import com.jieli.bluetooth.bean.device.fm.ChannelInfo;
import com.jieli.bluetooth.bean.device.fm.FmStatusInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.tool.room.DataRepository;
import com.jieli.btsmart.tool.room.entity.FMCollectInfoEntity;
import com.jieli.btsmart.ui.widget.FMSearchDialog;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @name BluetoothSDKs
 * @class name：com.jieli.btsmart.viewmodel
 * @class describe
 * @anthor HuanMing
 * @time 2020/6/12 19:32
 * @change
 * @chang time
 * @class describe
 */
public class FMControlViewModel extends BtBasicVM {
    //扫描弹窗是否显示
    public MutableLiveData<Boolean> scanDialogShowStateLiveData = new MutableLiveData<>(false);
    //小机扫描到的FM频道
    public MutableLiveData<ArrayList<Integer>> channelListLiveData = new MutableLiveData<>();
    //小机回复的FM状态信息
    public MutableLiveData<FmStatusInfo> fmStatusInfoLiveData = new MutableLiveData<>();
    //FM收藏的频点列表
    public MutableLiveData<List> fmCollectFreqLiveData = new MutableLiveData<>();
    //刻度尺滑到的FM的当前频点，不是小机的实时频点
    public MutableLiveData<Integer> fmCurrentFreqLiveData = new MutableLiveData<>(875);
    //刻度尺选中的FM频点
    public MutableLiveData<Integer> fmSelectedFreqLiveData = new MutableLiveData<>();
    //搜索弹窗显示
    public MutableLiveData<Boolean> fmShowSearchLiveData = new MutableLiveData<>();
    //收藏管理状态
    public MutableLiveData<Boolean> fmCollectManageStateLiveData = new MutableLiveData<>(false);
    //添加收藏
    public MutableLiveData<Boolean> fmCollectStateLiveData = new MutableLiveData<>();
    //设置搜索状态的Gif动画（Gif动画重头开始）
    public MutableLiveData<Integer> fmSearchIngGifLiveData = new MutableLiveData<>(R.drawable.ic_fm_searching);
    //RulerView是不是还在抛
    private boolean mIsViewFlingStop = true;
    private long mLastSendTime = 0L;

    public FMControlViewModel() {
        mRCSPController.addBTRcspEventCallback(callback);
    }

    @Override
    protected void release() {
        mRCSPController.removeBTRcspEventCallback(callback);
        super.release();
    }

    public void getFMInfo() {
        mRCSPController.getFmInfo(mRCSPController.getUsingDevice(), null);
    }

    /**
     * 设置rulerView的Fling状态
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    public void setIsViewFlingStop(boolean isStop) {
        this.mIsViewFlingStop = isStop;
    }
    /************************点击响应事件*******************************/
    /**
     * 选择频点
     *
     * @param freq 频点
     */
    public void onFMPlaySelectFreq(float freq) {
        if (!isCanSendFMCmdToDevice()) return;
        fmSelectedFreqLiveData.setValue((int) (freq * 10));
        JL_Log.d("zhm_fm", "onFMPlaySelectFreq: " + freq);
        mRCSPController.fmPlaySelectedFrequency(getConnectedDevice(), freq, null);
    }

    /**
     * 当前频点添加收藏
     *
     * @param
     */
    public void onFMFreqAddCollect() {
        String address = getConnectedDevice() == null ? "11:22:33:44:55:66" : getConnectedDevice().getAddress();
        FMCollectInfoEntity entity = new FMCollectInfoEntity();
        entity.setBtDeviceSSid(address);
        entity.setPlay(fmStatusInfoLiveData.getValue() != null && fmStatusInfoLiveData.getValue().isPlay());
        entity.setFreq(fmCurrentFreqLiveData.getValue());
        entity.setMode(fmStatusInfoLiveData.getValue() == null ? 0 : fmStatusInfoLiveData.getValue().getMode());
        DataRepository.getInstance().insertFMCollectInfo(entity, new DataRepository.DataRepositoryCallback() {
            @Override
            public void success() {//添加成功
                fmCollectStateLiveData.postValue(true);
            }

            @Override
            public void failed() {//代表已存在
                fmCollectStateLiveData.postValue(false);
            }
        });
    }

    /**
     * 删除已收藏频点(点击事件)
     *
     * @param entity 删除频点的Entity
     */
    public void onFMFreqDeleteCollect(FMCollectInfoEntity entity) {
        if (fmCollectFreqLiveData.getValue().size() == 1) {
            fmCollectManageStateLiveData.setValue(false);
        }
        DataRepository.getInstance().deleteFMCollectInfo(entity);
    }

    public void onFMCollectManageStateChange() {
        fmCollectManageStateLiveData.setValue(!fmCollectManageStateLiveData.getValue());
    }

    /**
     * 显示搜索View（不应该放在ViewModel里面，暂时先这样）
     *
     * @param view 显示在目标View下方
     */
    public void onShowFMSearchView(View view) {
        FMSearchDialog dialog = new FMSearchDialog(view.getContext(), this);
        dialog.showDialog(view);
        dialog.setDismissCallback(() -> fmShowSearchLiveData.setValue(false));
        fmShowSearchLiveData.setValue(true);
    }

    /**
     * 播放上一频道
     */
    public void onFMPlayPrevChannel() {
        if (!isCanSendFMCmdToDevice()) return;
        mRCSPController.fmPlayPrevChannel(getConnectedDevice(), null);
    }

    /**
     * 播放上一频点
     */
    public void onFMPlayPrevFreq() {
        if (!isCanSendFMCmdToDevice()) return;
        mRCSPController.fmPlayPrevFrequency(getConnectedDevice(), null);
    }

    /**
     * 播放暂停
     */
    public void onFMPlayOrPause() {
        if (!isCanSendFMCmdToDevice()) return;
        mRCSPController.fmPlayOrPause(getConnectedDevice(), null);
    }

    /**
     * 播放下一频点
     */
    public void onFMPlayNextFreq() {
        if (!isCanSendFMCmdToDevice()) return;
        mRCSPController.fmPlayNextFrequency(getConnectedDevice(), null);
    }

    /**
     * 播放下一频道
     */
    public void onFMPlayNextChannel() {
        if (!isCanSendFMCmdToDevice()) return;
        mRCSPController.fmPlayNextChannel(getConnectedDevice(), null);
    }

    /**
     * 向前搜索
     */
    public void onFMSearchForward() {
        if (!isCanSendFMCmdToDevice()) return;
        setPlayStateToPause();
        scanDialogShowStateLiveData.setValue(true);
        fmSearchIngGifLiveData.setValue(R.drawable.ic_fm_searching);
        mRCSPController.fmForwardSearchChannels(getConnectedDevice(), null);
        fmShowSearchLiveData.setValue(false);
    }

    /**
     * 全部搜索
     */
    public void onFmSearchAll() {
        if (!isCanSendFMCmdToDevice()) return;
        if (checkIsQuickContinuousSend()) return;
        setPlayStateToPause();
        scanDialogShowStateLiveData.setValue(true);
        fmSearchIngGifLiveData.setValue(R.drawable.ic_fm_searching);
        mRCSPController.fmSearchAllChannels(getConnectedDevice(), null);
        fmShowSearchLiveData.setValue(false);
    }

    /**
     * 向后搜索
     */
    public void onFMSearchBackward() {
        if (!isCanSendFMCmdToDevice()) return;
        setPlayStateToPause();
        scanDialogShowStateLiveData.setValue(true);
        fmSearchIngGifLiveData.setValue(R.drawable.ic_fm_searching);
        mRCSPController.fmBackwardSearchChannels(getConnectedDevice(), null);
        fmShowSearchLiveData.setValue(false);
    }

    /**
     * 搜索暂停
     */
    public void onFMSearchStop() {
        JL_Log.e("TAG", "onFMSearchStop: ");
        if (checkIsQuickContinuousSend()) return;
        mRCSPController.fmStopSearch(getConnectedDevice(), null);
    }

    /**
     * 是否可以发命令给小机（拦截命令操作）
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private boolean isCanSendFMCmdToDevice() {
        boolean result = true;
        if (scanDialogShowStateLiveData.getValue()) {
            ToastUtil.showToastShort(R.string.searching);
            result = false;
        }
        if (!mIsViewFlingStop) {
            result = false;
        }
        JL_Log.e("TAG", "isCanSendFMCmdToDevice:   mIsViewFlingStop ： " + mIsViewFlingStop + "  result : " + result);
        return result;
    }

    /**
     * 检查是不是频繁连续发送命令
     *
     * @param
     * @return true:是频繁发送
     * @description 描述一下方法的作用
     */
    private boolean checkIsQuickContinuousSend() {
        boolean result = true;
        if (System.currentTimeMillis() - mLastSendTime > 200 || mLastSendTime == 0L) {
            mLastSendTime = System.currentTimeMillis();
            result = false;
        }
        return result;
    }

    /**
     * 把当前的播放状态置为stop
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void setPlayStateToPause() {
        if (fmStatusInfoLiveData.getValue() == null) return;
        if (fmStatusInfoLiveData.getValue().isPlay()) {
            FmStatusInfo fmStatusInfo = fmStatusInfoLiveData.getValue();
            fmStatusInfo.setPlay(false);
            fmStatusInfoLiveData.setValue(fmStatusInfo);
        }
    }

    /************************set*******************************/

    private void onFmStatusChange(FmStatusInfo fmStatusInfo) {
        Log.e("ZHM_FM", "onFmStatusChange1: " + fmStatusInfo.toString());
        fmSelectedFreqLiveData.setValue((int) (fmStatusInfo.getFreq() * 10));
        fmStatusInfoLiveData.setValue(fmStatusInfo);
    }

    private void parseFMData(List<AttrBean> list) {
        if (list == null) return;
        for (AttrBean attr : list) {
            byte[] data = attr.getAttrData();
            if (data == null || data.length == 0) continue;
            switch (attr.getType()) {
                case AttrAndFunCode.SYS_INFO_ATTR_FM_FRE_INFO:
                    List<ChannelInfo> channelInfos = new ArrayList<>();
                    int offset = 0;
                    int dataLen = data.length;
                    while (dataLen - offset >= 3) {
                        int index = CHexConver.byteToInt(data[offset]);
                        offset++;
                        float freq = CHexConver.bytesToInt(data[offset], data[offset + 1]) / 10.0f;
                        offset += 2;
                        channelInfos.add(new ChannelInfo(index, freq));
                    }
//                    onFmChannelsChange(channelInfos);
                    break;
                case AttrAndFunCode.SYS_INFO_ATTR_FM_STATU:
                    int data0IntValue = CHexConver.byteToInt(data[0]);
                    Boolean isPlay = null;
                    if (data0IntValue == 0) {
                        isPlay = false;
                    } else if (data0IntValue == 1) {
                        isPlay = true;
                    }
                    int channel = 0;
                    float freq = 0.0f;
                    int mode = 0;
                    if (data.length > 1) {
                        channel = CHexConver.byteToInt(data[1]);
                        if (data.length > 3) {
                            freq = CHexConver.bytesToInt(data[2], data[3]) / 10.0f;
                            if (data.length > 4) {
                                mode = CHexConver.byteToInt(data[4]);
                            }
                        }
                    }
                    if (isPlay != null) {
                        onFmStatusChange(new FmStatusInfo(isPlay, channel, freq, mode));
                    }
                    break;
            }
        }
    }
    //模拟FM播放状态信息
    /*private void analogData() {
        FmStatusInfo statusInfo = new FmStatusInfo();
        statusInfo.setMode(0);
        statusInfo.setFreq(99.0f);
        statusInfo.setPlay(false);
        callback.onFmStatusChange(statusInfo);
        ArrayList<ChannelInfo> list = new ArrayList<>();
        int seq = 850;
        for (int i = 0; i < 20; i++) {
            seq = seq + (int) (Math.random() * 20);
            float seqf = ((float) seq) / ((float) 10);
            list.add(new ChannelInfo(i, seqf));
        }
        callback.onFmChannelsChange(list);
    }*/

    private final BTRcspEventCallback callback = new BTRcspEventCallback() {

        @Override
        public void onFmChannelsChange(BluetoothDevice device, List<ChannelInfo> channels) {
            ArrayList list = new ArrayList();
            for (int i = 0; i < channels.size(); i++) {
                ChannelInfo channelInfo = channels.get(i);
                Log.e("ZHM_FM", "onFmChannelsChange:  channelInfo.getFreq():" + channelInfo.getFreq());
                list.add((int) (channelInfo.getFreq() * 10));
            }
            JL_Log.d("ZHM_FM", "onFmChannelsChange: " + Arrays.toString(new ArrayList[]{list}));
            channelListLiveData.setValue(list);
        }

        @Override
        public void onFmStatusChange(BluetoothDevice device, FmStatusInfo fmStatusInfo) {
            JL_Log.d("ZHM_FM", "onFmStatusChange: " + fmStatusInfo.toString());
            if (fmStatusInfo.isPlay()) {
                scanDialogShowStateLiveData.setValue(false);
            }
            fmSelectedFreqLiveData.setValue((int) (fmStatusInfo.getFreq() * 10));
            fmStatusInfoLiveData.setValue(fmStatusInfo);
        }

        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (mode != 16) {
                scanDialogShowStateLiveData.setValue(false);
            }
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            scanDialogShowStateLiveData.setValue(false);
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            super.onSwitchConnectedDevice(device);
            getFMInfo();
        }
    };
}
