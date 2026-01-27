package com.jieli.btsmart.ui.widget.visualizer.record.data;

/**
 * IdleState
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 空闲状态
 * @since 2025/7/9
 */
public class IdleState extends State {
    /**
     * 结果码
     */
    private final int code;
    /**
     * 描述
     */
    private final String message;

    public IdleState(int code, String message) {
        super(STATE_IDLE);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "IdleState{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
