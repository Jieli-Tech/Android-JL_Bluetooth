package com.jieli.btsmart.ui.widget.visualizer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.databinding.ViewLongPressRecordBinding;
import com.jieli.btsmart.ui.widget.visualizer.record.RecorderVisualizeView;
import com.jieli.btsmart.ui.widget.visualizer.record.data.IdleState;
import com.jieli.btsmart.ui.widget.visualizer.record.data.State;
import com.jieli.btsmart.ui.widget.visualizer.record.data.WorkingState;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * LongPressRecordView
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 长按录音
 * @since 2025/8/4
 */
public class LongPressRecordView implements View.OnTouchListener {

    private static final String TAG = LongPressRecordView.class.getSimpleName();

    /**
     * 空闲状态
     */
    public static final int STATE_IDLE = 0;
    /**
     * 录音状态
     */
    public static final int STATE_RECORDING = 1;
    /**
     * 取消录音状态
     */
    public static final int STATE_CANCEL = 2;

    private static final long LONG_PRESS_INTERVAL = 500L;

    private static final int CANCEL_MOVE_DISTANCE = -100;

    private static final int MSG_LONG_PRESS_EVENT = 0x65D2;

    /**
     * 上下文
     */
    @NonNull
    private final Context mContext;
    /**
     * 设备地址
     */
    private final String mac;
    /**
     * UI控制
     */
    @NonNull
    private final ViewLongPressRecordBinding mBinding;
    /**
     * 工作状态
     */
    private int state = -1;
    /**
     * 弹出提示
     */
    private PopupWindow mPopupWindow;
    /**
     * 按下的Y值
     */
    private float initY;
    /**
     * 是否取消录音
     */
    private boolean isCancelRecord = false;
    /**
     * 录音文件路径
     */
    private String recordFilePath;
    /**
     * 文件输出流
     */
    private FileOutputStream fout;
    /**
     * 录音事件监听器
     */
    private OnRecordEventListener mListener;
    /**
     * UI处理
     */
    private final Handler uiHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (MSG_LONG_PRESS_EVENT == msg.what) {
            handleLongPressEvent();
        }
        return true;
    });

    @SuppressLint("ClickableViewAccessibility")
    public LongPressRecordView(@NonNull Context context, @NonNull String mac, @NonNull ViewLongPressRecordBinding binding) {
        this.mContext = context;
        this.mac = mac;
        this.mBinding = binding;
        mBinding.tvText.setFocusable(false);
        mBinding.viewRecord.setFocusable(false);
        mBinding.getRoot().setOnTouchListener(this);
        updateState(STATE_IDLE);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (null == event) return false;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                JL_Log.d(TAG, "onTouch", "ACTION_DOWN");
                initY = event.getY();
                uiHandler.removeMessages(MSG_LONG_PRESS_EVENT);
                uiHandler.sendEmptyMessageDelayed(MSG_LONG_PRESS_EVENT, LONG_PRESS_INTERVAL);
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (state == STATE_IDLE) return true;
                if (isCancelRecord(event.getY())) {
                    updateState(STATE_CANCEL);
                } else {
                    updateState(STATE_RECORDING);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (uiHandler.hasMessages(MSG_LONG_PRESS_EVENT)) {
                    uiHandler.removeMessages(MSG_LONG_PRESS_EVENT);
                }
                if (state != STATE_IDLE) {
                    isCancelRecord = state == STATE_CANCEL && isCancelRecord(event.getY());
                    JL_Log.d(TAG, "onTouch", "ACTION_UP, isCancelRecord : " + isCancelRecord);
                    if (mBinding.viewRecord.isRecording()) {
                        mBinding.viewRecord.release();
                    } else {
                        updateState(STATE_IDLE);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void setListener(OnRecordEventListener listener) {
        mListener = listener;
    }

    private boolean isCancelRecord(float y) {
        return initY != 0 && (y - initY < CANCEL_MOVE_DISTANCE);
    }

    private void updateState(int state) {
        if (this.state == state) return;
        JL_Log.d(TAG, "updateState", "state = " + state + ", cache state : " + this.state);
        this.state = state;
        switch (state) {
            case STATE_IDLE: {
                initY = 0;
                isCancelRecord = false;
                recordFilePath = null;
                dismissTips();
                mBinding.clMain.setBackgroundResource(R.drawable.bg_c8_gray_light_shape);
                UIHelper.gone(mBinding.viewRecord);
                UIHelper.show(mBinding.tvText);
                mBinding.tvText.setTextColor(ContextCompat.getColor(mContext, R.color.purple_7657EC));
                mBinding.tvText.setText(mContext.getString(R.string.hold_to_speak));
                break;
            }
            case STATE_RECORDING: {
                mBinding.clMain.setBackgroundResource(R.drawable.bg_c8_purple_shape);
                mBinding.tvText.setText("");
                UIHelper.gone(mBinding.tvText);
                UIHelper.show(mBinding.viewRecord);
                showTips();
                break;
            }
            case STATE_CANCEL: {
                dismissTips();
                mBinding.clMain.setBackgroundResource(R.drawable.bg_c8_red_shape);
                mBinding.viewRecord.setVisibility(View.INVISIBLE);
                mBinding.tvText.setVisibility(View.VISIBLE);
                mBinding.tvText.setTextColor(ContextCompat.getColor(mContext, R.color.white_ffffff));
                mBinding.tvText.setText(mContext.getString(R.string.release_cancel));
                break;
            }
        }
        if (null != mListener) {
            mListener.onStateChange(state);
        }
    }

    private void handleLongPressEvent() {
        if (mBinding.viewRecord.isRecording()) return;
        updateState(STATE_RECORDING);
        mBinding.viewRecord.startRecord(state -> {
            switch (state.getState()) {
                case State.STATE_START: {
                    //创建文件
                    createRecordFile();
                    break;
                }
                case State.STATE_WORKING: {
                    WorkingState workingState = (WorkingState) state;
                    //写入文件数据
                    writeDataToFile(workingState.getData());
                    break;
                }
                default: {
                    IdleState idleState = (IdleState) state;
                    //判断是否取消语音
                    closeRecordFile();
                    if (idleState.getCode() != RecorderVisualizeView.ERR_NONE || isCancelRecord) {
                        FileUtil.deleteFile(new File(recordFilePath));
                        recordFilePath = null;
                    }
                    uiHandler.post(() -> {
                        final String filePath = recordFilePath;
                        if (!TextUtils.isEmpty(filePath) && FileUtil.checkFileExist(filePath)) {
                            JL_Log.d(TAG, "handleLongPressEvent", "onRecordFile : " + filePath);
                            if (null != mListener) mListener.onRecordFile(filePath);
                        }
                        updateState(STATE_IDLE);
                    });
                    break;
                }

            }
        });
    }

    private int dpToPx(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showTips() {
        if (null == mPopupWindow) {
            View root = LayoutInflater.from(mContext).inflate(R.layout.view_recording_tips, null, false);
            mPopupWindow = new PopupWindow(root, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        if (!mPopupWindow.isShowing()) {
            int[] location = new int[2];
            mBinding.clMain.getLocationOnScreen(location);
            int parentStart = location[0];
            int parentTop = location[1];
            View contentView = mPopupWindow.getContentView();
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            mPopupWindow.showAtLocation(mBinding.clMain, Gravity.NO_GRAVITY,
                    parentStart + (mBinding.clMain.getWidth() - contentView.getMeasuredWidth()) / 2,
                    (parentTop - contentView.getMeasuredHeight() - dpToPx(10)));
        }

    }

    private void dismissTips() {
        if (null != mPopupWindow) {
            if (mPopupWindow.isShowing()) {
                mPopupWindow.dismiss();
            }
            mPopupWindow = null;
        }
    }

    private void createRecordFile() {
        closeRecordFile();
        recordFilePath = FileUtil.createFilePath(mContext, mac, SConstant.DIR_RECORD) + File.separator
                + "rec_" + TranslateUtil.getDateString() + ".pcm";
        try {
            fout = new FileOutputStream(recordFilePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fout = null;
            recordFilePath = null;
        }
    }

    private void closeRecordFile() {
        if (fout != null) {
            try {
                fout.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fout = null;
        }
    }

    private boolean writeDataToFile(byte[] pcmData) {
        if (null == fout || null == pcmData) return false;
        try {
            fout.write(pcmData);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public interface OnRecordEventListener {

        void onStateChange(int state);

        void onRecordFile(String filePath);
    }
}
