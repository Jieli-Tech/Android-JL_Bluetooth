package com.jieli.btsmart.ui.widget.visualizer.record.data;

/**
 * StartState
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 开始状态
 * @since 2025/7/9
 */
public class StartState extends State {
    /**
     * 采样率
     */
    private final int sampleRate;
    /**
     * 声道数
     */
    private final int channelNum;
    /**
     * 编码格式
     */
    private final int encoding;

    public StartState(int sampleRate, int channelNum, int encoding) {
        super(STATE_START);
        this.sampleRate = sampleRate;
        this.channelNum = channelNum;
        this.encoding = encoding;
    }


    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannelNum() {
        return channelNum;
    }

    public int getEncoding() {
        return encoding;
    }

    @Override
    public String toString() {
        return "StartState{" +
                "sampleRate=" + sampleRate +
                ", channelNum=" + channelNum +
                ", encoding=" + encoding +
                '}';
    }
}
