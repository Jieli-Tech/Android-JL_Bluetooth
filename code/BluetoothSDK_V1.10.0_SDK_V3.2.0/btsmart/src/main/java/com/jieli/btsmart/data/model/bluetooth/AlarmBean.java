package com.jieli.btsmart.data.model.bluetooth;

import com.jieli.bluetooth.bean.base.AttrBean;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.AppUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 闹钟数据
 * Created by chensenhua on 2018/1/10.
 */

@SuppressWarnings("WeakerAccess")
public class AlarmBean {
    //索引
    private byte index;
    //名称
    private String name;
    //小时
    private byte hour;
    //分钟
    private byte min;
    //类型
    private byte repeatMode;
    //铃声
    @Deprecated
    private String bell;
    //拓展字段
    private String reserved;
    //是否开启
    private boolean open;
    //铃声类型
    private byte bellType;//0: 默认类型  1：设备文件类型
    //铃声簇号
    private int bellCluster; //当bellType=0时，这里是铃声序号 当bellType=1时 这里是文件簇号
    //铃声名称
    private String bellName; //当bellType=1时有效
    //铃声所在设备序号
    private byte devIndex;//当bellType=1时有效

    private int version;

    public AlarmBean setVersion(int version) {
        this.version = version;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public AlarmBean setDevIndex(byte devIndex) {
        this.devIndex = devIndex;
        return this;
    }

    public byte getDevIndex() {
        return devIndex;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isOpen() {
        return open;
    }

    public AlarmBean setOpen(boolean open) {
        this.open = open;
        return this;
    }

    public AlarmBean setBell(String bell) {
        this.bell = bell;
        return this;
    }

    public String getBell() {
        return bell;
    }


    public String getName() {
        return name == null ? "" : name;
    }

    public AlarmBean setName(String name) {
        this.name = name;
        return this;
    }

    public byte getHour() {
        return hour;
    }

    public AlarmBean setHour(byte hour) {
        this.hour = hour;
        return this;
    }

    public byte getMin() {
        return min;
    }

    public AlarmBean setMin(byte min) {
        this.min = min;
        return this;
    }

    public AlarmBean setRepeatMode(byte repeatMode) {
        this.repeatMode = repeatMode;
        return this;
    }

    public byte getRepeatMode() {
        return repeatMode;
    }

    public AlarmBean setReserved(String reserved) {
        this.reserved = reserved;
        return this;
    }

    public String getReserved() {
        return reserved;
    }


    public AlarmBean setIndex(byte index) {
        this.index = index;
        return this;
    }

    public byte getIndex() {
        return index;
    }


    public AlarmBean() {
    }


    public byte getBellType() {
        return bellType;
    }

    public AlarmBean setBellType(byte bellType) {
        this.bellType = bellType;
        return this;
    }

    public int getBellCluster() {
        return bellCluster;
    }

    public AlarmBean setBellCluster(int cluster) {
        this.bellCluster = cluster;
        return this;
    }

    public String getBellName() {
        return bellName;
    }

    public AlarmBean setBellName(String bellName) {
        this.bellName = bellName;
        return this;
    }

    public AlarmBean copy() {
        return new AlarmBean()
                .setIndex(this.getIndex())
                .setHour(this.getHour())
                .setMin(this.getMin())
                .setName(this.getName())
                .setRepeatMode(this.getRepeatMode())
                .setBell(this.getBell())
                .setReserved(this.getReserved())
                .setBellType(this.bellType)
                .setBellName(this.bellName)
                .setBellCluster(this.bellCluster)
                .setVersion(this.version)
                .setDevIndex(this.devIndex)
                .setOpen(this.isOpen());

    }

    @Override
    public String toString() {
        return "AlarmBean{" +
                "index=" + index +
                ", name='" + name + '\'' +
                ", hour=" + hour +
                ", min=" + min +
                ", repeatMode=" + repeatMode +
                ", bell='" + bell + '\'' +
                ", reserved='" + reserved + '\'' +
                ", open=" + open +
                ", bellType=" + bellType +
                ", bellCluster=" + bellCluster +
                ", bellName='" + bellName + '\'' +
                ", devIndex='" + devIndex + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    /**
     * 获取
     *
     * @param alarmBean
     * @return
     */

    public static String getRepeatDesc(AlarmBean alarmBean) {
        StringBuilder sb = new StringBuilder();
        String[] dayOfWeek = {"一", "二", "三", "四", "五", "六", "日"};
        int mode = alarmBean.getRepeatMode() & 0xff;
        if (mode == 0x00) {
            sb.append("");
        } else if ((mode & 0x01) == 0x01) {
            sb.append("单次");
        } else if (mode == 0xfe) {
            sb.append("全部");
        } else {
            sb.append("周");
            for (int i = 1; i < 8; i++) {
                int temp = mode;
                temp = temp >> i;
                temp = temp & 0x01;
                if (temp == 0x01) {
                    sb.append(dayOfWeek[i - 1]);
                    sb.append(" ");
                }
            }
        }
        return sb.toString().trim().replace(" ", "、");
    }

    /**
     * 获取闹钟信息的重复模式描述
     *
     * @param alarmBean 闹钟信息
     * @return 重复模式描述
     */
    public static String getRepeatDescModify(AlarmBean alarmBean) {
        StringBuilder sb = new StringBuilder();
        int mode = alarmBean.getRepeatMode() & 0xff;
        if (mode == 0x00) {
            sb.append(AppUtil.getContext().getString(R.string.alarm_repeat_single));
        } else if ((mode & 0x01) == 0x01) {
            sb.append(AppUtil.getContext().getString(R.string.alarm_repeat_every_day));
        } else if (mode == 0x3e) {
            sb.append(AppUtil.getContext().getString(R.string.alarm_repeat_on_workday));
        } else {
            String week = AppUtil.getContext().getString(R.string.alarm_repeat_week);
            sb.append(week);
            String[] dayOfWeek = AppUtil.getContext().getResources().getStringArray(R.array.alarm_weeks);
            for (int i = 0; i < dayOfWeek.length; i++) {
                int temp = mode;
                temp = temp >> i+1;
                temp = temp & 0x01;
                if (temp == 0x01) {
                    sb.append(dayOfWeek[i].replace(week, ""));
                    sb.append(" ");
                }
            }
        }
        return sb.toString().trim().replace(" ", "、");
    }


    public static AttrBean toAttrbean(AlarmBean alarmBean, boolean del) {
        List<AlarmBean> list = new ArrayList<>();
        list.add(alarmBean);
        return toAttrbean(list, del);
    }

    public static AttrBean toAttrbean(List<AlarmBean> list, boolean del) {
        AttrBean attrBean = new AttrBean();
        attrBean.setType((byte) 0x01);
        byte[] data;
        if (del) {
            data = new byte[2 + list.size()];
            data[0] = 0x01;
            data[1] = (byte) list.size();
            int index = 2;
            for (AlarmBean alarmBean : list) {
                data[index++] = alarmBean.getIndex();
            }
        } else {
            List<Byte> temp = new ArrayList<>();
            for (AlarmBean alarmBean : list) {
                temp.add(alarmBean.getIndex());
                temp.add((byte) (alarmBean.isOpen() ? 0x01 : 0x00));
                temp.add(alarmBean.getRepeatMode());
                temp.add(alarmBean.getHour());
                temp.add(alarmBean.getMin());
                byte[] nameData = alarmBean.getName().getBytes();
                temp.add((byte) nameData.length);
                for (byte nameDatum : nameData) {
                    temp.add(nameDatum);
                }
                if (alarmBean.version == 1) {
                    //版本1新增闹钟铃声结构
                    temp.add(alarmBean.bellType);
                    temp.add(alarmBean.devIndex);
                    temp.add((byte) (alarmBean.bellCluster >> 24));
                    temp.add((byte) ((alarmBean.bellCluster >> 16) & 0xff));
                    temp.add((byte) ((alarmBean.bellCluster >> 8) & 0xff));
                    temp.add((byte) (alarmBean.bellCluster & 0xff));

                    byte[] bellNameData = alarmBean.getBellName().getBytes();
                    temp.add((byte) bellNameData.length);
                    for (Byte b : bellNameData) {
                        temp.add(b);
                    }
                }

            }
            data = new byte[2 + temp.size()];
            data[0] = 0x00;
            data[1] = (byte) list.size();
            for (int i = 0; i < temp.size(); i++) {
                data[i + 2] = temp.get(i);
            }
        }
        attrBean.setAttrData(data);
        return attrBean;
    }

}
