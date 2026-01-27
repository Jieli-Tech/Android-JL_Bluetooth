package com.jieli.btsmart.data.model.auracast;

import com.jieli.bluetooth.annotation.SyncState;
import com.jieli.bluetooth.bean.auracast.AuracastBroadcast;
import com.jieli.bluetooth.bean.auracast.AuracastRecord;
import com.jieli.bluetooth.constant.StateCode;

/**
 * AuracastRecordState
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: 广播记录状态
 */
public class AuracastRecordState {
    private final AuracastRecord record;
    private AuracastBroadcast state;
    private boolean isFoundBroadcast;

    public AuracastRecordState(AuracastRecord record) {
        this.record = record;
    }

    public AuracastRecord getRecord() {
        return record;
    }

    public AuracastBroadcast getState() {
        return state;
    }

    public AuracastRecordState setState(AuracastBroadcast state) {
        this.state = state;
        return this;
    }

    public boolean isFoundBroadcast() {
        return isFoundBroadcast;
    }

    public AuracastRecordState setFoundBroadcast(boolean foundBroadcast) {
        isFoundBroadcast = foundBroadcast;
        return this;
    }

    public AuracastBroadcast getRecordBroadcast() {
        AuracastBroadcast broadcast = record.getBroadcast();
        if (broadcast != null && broadcast.getSyncState() != StateCode.STATE_IDLE) {
            broadcast.setSyncState(StateCode.STATE_IDLE); //重置为空闲状态
        }
        return broadcast;
    }

    public AuracastBroadcast getBroadcast() {
        AuracastBroadcast broadcast = getState();
        if (null != broadcast && broadcast.getSyncState() != StateCode.STATE_IDLE) return broadcast;
        broadcast = getRecordBroadcast();
        return broadcast;
    }

    @SyncState
    public int getSyncState() {
        AuracastBroadcast broadcast = getBroadcast();
        if (null == broadcast) return StateCode.STATE_IDLE;
        return broadcast.getSyncState();
    }

    @Override
    public String toString() {
        return "AuracastRecordState{" +
                "record=" + record +
                ", state=" + state +
                ", isFoundBroadcast=" + isFoundBroadcast +
                '}';
    }
}
