package com.jieli.btsmart.data.model.auracast;

import com.jieli.bluetooth.bean.auracast.AuracastRecord;

/**
 * AuracastRecordOp
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: Auracast历史纪录操作信息
 */
public class AuracastRecordOp {
    private final int op;
    private final AuracastRecord record;

    public AuracastRecordOp(int op, AuracastRecord record) {
        this.op = op;
        this.record = record;
    }

    public int getOp() {
        return op;
    }

    public AuracastRecord getRecord() {
        return record;
    }

    @Override
    public String toString() {
        return "AuracastRecordOp{" +
                "op=" + op +
                ", record=" + record +
                '}';
    }
}
