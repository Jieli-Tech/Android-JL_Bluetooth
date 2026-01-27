package com.jieli.btsmart.ui.auracast.receiver;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.auracast.AuracastBroadcast;
import com.jieli.bluetooth.bean.auracast.AuracastRecord;
import com.jieli.bluetooth.bean.auracast.ScanOption;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.command.auracast.response.ScanResponse;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.auracast.receiver.AuracastReceiverCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.auracast.AuracastQRCode;
import com.jieli.btsmart.data.model.auracast.AuracastRecordOp;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * AuracastReceiverViewModel
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: Auracast接收端逻辑实现
 */
public class AuracastReceiverViewModel extends AuracastAssistantViewModel {

    /**
     * 找到广播操作
     */
    public static final int OP_FOUND_BROADCAST = 0x1F;
    /**
     * 移除附近有广播操作
     */
    public static final int OP_REMOVE_BROADCAST = 0x20;


    /**
     * 广播记录方式
     */
    private static final int WAY_RECORD = 1;

    /**
     * 二维码方式
     */
    private static final int WAY_QR_CODE = 2;

    /**
     * 记录附近有广播的记录的超时时间
     */
    private static final long RECORD_FOUND_BROADCAST_TIMEOUT = 10 * 1000;

    /**
     * 搜索设备状态回调
     */
    public final MutableLiveData<Boolean> scanStateMLD = new MutableLiveData<>();
    /**
     * 已发现的广播列表回调
     */
    public final MutableLiveData<List<AuracastBroadcast>> foundBroadcastMLD = new MutableLiveData<>();
    /**
     * 广播状态回调
     */
    public final MutableLiveData<AuracastBroadcast> broadcastAudioStateMLD = new MutableLiveData<>();
    /**
     * 广播记录改变回调
     */
    public final MutableLiveData<AuracastRecordOp> auracastRecordChangeMLD = new MutableLiveData<>();
    /**
     * 尝试同步广播状态回调
     */
    public final MutableLiveData<StateResult<AuracastBroadcast>> syncBroadcastStateMLD = new MutableLiveData<>();

    /**
     * 已发现广播的历史记录
     */
    private final List<AuracastRecord> foundBroadcastRecordList = new ArrayList<>();

    /**
     * UI处理
     */
    private final Handler uiHandler = new Handler(msg -> {
        if (!(msg.obj instanceof AuracastRecord)) return false;
        AuracastRecord record = (AuracastRecord) msg.obj;
        if (foundBroadcastRecordList.remove(record)) {
            auracastRecordChangeMLD.postValue(new AuracastRecordOp(OP_REMOVE_BROADCAST, record));
        }
        return true;
    });

    public AuracastReceiverViewModel(BluetoothDevice device) {
        super(device);
        auracastAssistant.addAuracastReceiverCallback(auracastReceiverCallback);
    }

    @Override
    public void release() {
        foundBroadcastRecordList.clear();
        uiHandler.removeCallbacksAndMessages(null);
        stopScan();
        auracastAssistant.removeAuracastReceiverCallback(auracastReceiverCallback);
        super.release();
    }

    public List<AuracastRecord> getAuracastRecords() {
        return auracastAssistant.getAuracastRecords(getMac());
    }

    public AuracastRecord findAuracastRecord(AuracastBroadcast broadcast) {
        return auracastAssistant.findAuracastRecord(getMac(), broadcast);
    }

    public void removeAuracastRecord(AuracastRecord record) {
        JL_Log.d(tag, "removeAuracastRecord", "" + record);
        auracastAssistant.removeAuracastRecord(record);
    }

    public boolean isScanning() {
        return auracastAssistant.isScanning();
    }

    public void startScan() {
        final boolean isScanning = isScanning();
        JL_Log.d(tag, "startScan", "isScanning : " + isScanning);
        if (isScanning) return;
        auracastAssistant.startScan(new ScanOption(), new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                boolean isOk = message == ScanResponse.RESULT_SUCCESS || message == ScanResponse.RESULT_SCANNING;
                if (isOk) {
                    opResultMLD.postValue(new OpResult<>(OP_START_SCAN)
                            .setCode(OpResult.RES_SUCCESS));
                    return;
                }
                onError(device, BaseError.buildResponseBadResult(message, 0));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                opResultMLD.postValue(new OpResult<>(OP_START_SCAN)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage())
                        .setData(error.getReason()));
            }
        });
    }

    public void stopScan() {
        final boolean isScanning = isScanning();
        JL_Log.d(tag, "stopScan", "isScanning : " + isScanning);
        if (!isScanning) return;
        auracastAssistant.stopScan(new OpResultCallback<>(OP_STOP_SCAN));
    }

    public AuracastBroadcast getListeningBroadcast() {
        return auracastAssistant.getListeningBroadcast();
    }

    public AuracastBroadcast getOperationBroadcast() {
        return auracastAssistant.getOperationBroadcast();
    }

    public void syncAuracastBroadcastState() {
        auracastAssistant.requestListeningSource(new OnRcspActionCallback<AuracastBroadcast>() {
            @Override
            public void onSuccess(BluetoothDevice device, AuracastBroadcast message) {
                auracastAssistant.checkScanStatus(null); //同步搜索广播状态
                opResultMLD.postValue(new OpResult<>(OP_SYNC_BROADCAST_STATE)
                        .setCode(OpResult.RES_SUCCESS)
                        .setData(message));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                auracastAssistant.checkScanStatus(null); //同步搜索广播状态
                opResultMLD.postValue(new OpResult<>(OP_SYNC_BROADCAST_STATE)
                        .setCode(error.getSubCode())
                        .setMessage(error.getMessage()));
            }
        });
    }

    public void addSource(AuracastBroadcast broadcast) {
        stopScan();
        auracastAssistant.addSource(broadcast, new OpResultCallback<>(OP_ADD_SOURCE, broadcast));
    }

    public void removeSource() {
        auracastAssistant.removeSource(new OpResultCallback<>(OP_REMOVE_SOURCE, getListeningBroadcast()));
    }

    public boolean isFoundBroadcast(AuracastRecord record) {
        AuracastBroadcast broadcast = record.getBroadcast();
        if (null == broadcast) return false;
        if (foundBroadcastRecordList.contains(record)) return true;
        List<AuracastBroadcast> foundBroadcasts = auracastAssistant.getDiscoveredBroadcast();
        if (foundBroadcasts.isEmpty()) return false;
        for (AuracastBroadcast found : foundBroadcasts) {
            if (broadcast.equals(found)) return true;
        }
        return false;
    }

    public void tryToSyncBroadcastByRecord(AuracastBroadcast broadcast) {
        if (null == broadcast) {
            postSyncBroadcastError("tryToSyncBroadcastByRecord", ErrorCode.SUB_ERR_PARAMETER, "No Broadcast");
            return;
        }
        tryToSyncBroadcast(WAY_RECORD, broadcast);
    }

    public void tryToSyncBroadcastByQrCode(AuracastQRCode qrCode) {
        if (null == qrCode) return;
        tryToSyncBroadcast(WAY_QR_CODE, new AuracastBroadcast()
                .setBroadcastName(qrCode.getName())
                .setBroadcastCode(qrCode.getCode().getBytes()));
    }

    private boolean isBroadcastSyncing() {
        final StateResult<AuracastBroadcast> stateResult = syncBroadcastStateMLD.getValue();
        if (null == stateResult) return false;
        return stateResult.getState() == StateResult.STATE_WORKING;
    }

    private AuracastBroadcast getSyncBroadcast() {
        final StateResult<AuracastBroadcast> stateResult = syncBroadcastStateMLD.getValue();
        if (null == stateResult) return null;
        return stateResult.getData();
    }

    private void postAuracastBroadcast(AuracastBroadcast broadcast) {
        if (!isScanning() || null == broadcast) return;
        if (broadcast.equals(getListeningBroadcast())) { //正在收听的广播，跳过
            return;
        }
        final AuracastRecord record = findAuracastRecord(broadcast);
        if (null != record) { //广播历史 且 附近有广播
            boolean hasRecord = foundBroadcastRecordList.add(record);
            if (!hasRecord) {
                foundBroadcastRecordList.add(record);
                auracastRecordChangeMLD.postValue(new AuracastRecordOp(OP_FOUND_BROADCAST, record));
            }
            //开始超时任务
            uiHandler.removeMessages(record.hashCode());
            uiHandler.sendMessageDelayed(uiHandler.obtainMessage(record.hashCode(), record), RECORD_FOUND_BROADCAST_TIMEOUT);
            return;
        }
        List<AuracastBroadcast> list = foundBroadcastMLD.getValue();
        if (null == list) {
            list = new ArrayList<>();
        }
        int index = list.indexOf(broadcast);
        if (index != -1) return;
        list.add(broadcast);
        foundBroadcastMLD.setValue(list);
    }

    private void tryToSyncBroadcast(int way, AuracastBroadcast broadcast) {
        JL_Log.d(tag, "tryToSyncBroadcast", "way : " + way + ", " + broadcast);
        if (null == broadcast) return;
        final AuracastBroadcast listeningBroadcast = getListeningBroadcast();
        if (null != listeningBroadcast) {
            boolean isSameBroadcast = way == WAY_RECORD ? broadcast.equals(listeningBroadcast) :
                    TextUtils.equals(broadcast.getBroadcastName(), listeningBroadcast.getBroadcastName());
            JL_Log.d(tag, "tryToSyncBroadcast", "isSameBroadcast : " + isSameBroadcast
                    + ", listeningBroadcast :  " + listeningBroadcast);
            if (isSameBroadcast) {
                postSyncBroadcastFinish(listeningBroadcast);
                return;
            }
        }
        final AuracastBroadcast operationBroadcast = getOperationBroadcast();
        if (null != operationBroadcast) {
            boolean isSameBroadcast = way == WAY_RECORD ? broadcast.equals(operationBroadcast) :
                    TextUtils.equals(broadcast.getBroadcastName(), operationBroadcast.getBroadcastName());
            JL_Log.i(tag, "tryToSyncBroadcast", "isSameBroadcast : " + isSameBroadcast
                    + ", Processing broadcast in progress. operationBroadcast : " + operationBroadcast);
            if (!isSameBroadcast) { //正在同步广播
                postSyncBroadcastError("", ErrorCode.SUB_ERR_OPERATION_IN_PROGRESS, "Processing broadcast in progress. " + operationBroadcast);
            }
            return;
        }
        if (isBroadcastSyncing()) {
            //正在尝试同步中
            JL_Log.i(tag, "tryToSyncBroadcast", "Trying to sync broadcast. " + getSyncBroadcast());
            return;
        }
        AuracastBroadcast tempBroadcast = broadcast.cloneObject()
                .setSyncState(StateCode.STATE_SYNCING);
        syncBroadcastStateMLD.setValue(new StateResult<AuracastBroadcast>(OP_SYNC_BROADCAST)
                .setState(StateResult.STATE_WORKING)
                .setCode(0).setData(tempBroadcast));
        tryToAddSource(tempBroadcast);
    }

    private void tryToAddSource(AuracastBroadcast broadcast) {
        stopScan();
        auracastAssistant.addSource(broadcast, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                AuracastBroadcast listeningBroadcast = getListeningBroadcast();
                if (null == listeningBroadcast) {
                    onError(device, new BaseError(ErrorCode.SUB_ERR_SYNC_TIMEOUT));
                    return;
                }
                postSyncBroadcastFinish(listeningBroadcast);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                postSyncBroadcastError("tryToAddSource", error.getSubCode(), error.getMessage());
            }
        });
    }

    private void postSyncBroadcastFinish(AuracastBroadcast broadcast) {
        boolean isBroadcastSyncing = isBroadcastSyncing();
        JL_Log.i(tag, "postSyncBroadcastFinish", "isBroadcastSyncing : " + isBroadcastSyncing + ", " + broadcast);
        if (!isBroadcastSyncing) return;
        syncBroadcastStateMLD.postValue(new StateResult<AuracastBroadcast>(OP_SYNC_BROADCAST)
                .setState(StateResult.STATE_FINISH)
                .setCode(ErrorCode.ERR_NONE)
                .setMessage("")
                .setData(broadcast));
    }

    private void postSyncBroadcastError(String method, int code, String message) {
        boolean isBroadcastSyncing = isBroadcastSyncing();
        JL_Log.w(tag, method, CommonUtil.formatString("isBroadcastSyncing : %s, code :%s, %s", isBroadcastSyncing,
                CommonUtil.formatInt(code), message));
        if (!isBroadcastSyncing) return;
        AuracastBroadcast broadcast = getSyncBroadcast();
        if (null != broadcast) {
            broadcast.setSyncState(StateCode.STATE_IDLE);
        }
        syncBroadcastStateMLD.postValue(new StateResult<AuracastBroadcast>(OP_SYNC_BROADCAST)
                .setState(StateResult.STATE_FINISH)
                .setCode(code)
                .setMessage(message)
                .setData(broadcast));
    }

    private final AuracastReceiverCallback auracastReceiverCallback = new AuracastReceiverCallback() {
        @Override
        public void onSearchStarted(int reason) {
            scanStateMLD.setValue(true);
            foundBroadcastMLD.setValue(new ArrayList<>());
        }

        @Override
        public void onSearchStopped(int reason) {
            scanStateMLD.setValue(false);
        }

        @Override
        public void onBroadcastFound(@NonNull AuracastBroadcast broadcast) {
            postAuracastBroadcast(broadcast);
        }

        @Override
        public void onBroadcastState(@NonNull BluetoothDevice device, @NonNull AuracastBroadcast broadcast) {
            if (!BluetoothUtil.deviceEquals(device, getDevice())) return;
            if (broadcast.getSyncState() == StateCode.STATE_IDLE && broadcast.getErrorCode() == StateCode.SYNC_ERR_TIMEOUT) {
                AuracastRecord cache = null;
                for (AuracastRecord foundRecord : foundBroadcastRecordList) {
                    if (broadcast.equals(foundRecord.getBroadcast())) {
                        cache = foundRecord;
                        break;
                    }
                }
                if (null != cache) {
                    uiHandler.removeMessages(cache.hashCode());
                    if (foundBroadcastRecordList.remove(cache)) {
                        auracastRecordChangeMLD.postValue(new AuracastRecordOp(OP_REMOVE_BROADCAST, cache));
                    }
                }
            }
            broadcastAudioStateMLD.setValue(broadcast);
        }

        @Override
        public void onRecordChange(int op, AuracastRecord record) {
            auracastRecordChangeMLD.postValue(new AuracastRecordOp(op, record));
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
            return (T) new AuracastReceiverViewModel(device);
        }
    }
}