package com.jieli.btsmart.data.model.bluetooth;

import java.util.List;

public class AlarmListInfo {

    private int version;

    private List<AlarmBean> alarmBeans;

    public AlarmListInfo(List<AlarmBean> list){
        setAlarmBeans(list);
    }

    public AlarmListInfo(int version, List<AlarmBean> alarmBeans) {
        this.version = version;
        this.alarmBeans = alarmBeans;
    }

    public void setAlarmBeans(List<AlarmBean> alarmBeans) {
        this.alarmBeans = alarmBeans;
    }

    public List<AlarmBean> getAlarmBeans() {
        return alarmBeans;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
