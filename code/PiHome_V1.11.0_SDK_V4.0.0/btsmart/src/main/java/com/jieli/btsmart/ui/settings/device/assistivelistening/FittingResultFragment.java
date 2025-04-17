package com.jieli.btsmart.ui.settings.device.assistivelistening;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.hearing.HearingFrequenciesInfo;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.room.AppDatabase;
import com.jieli.btsmart.tool.room.dao.FittingRecordDao;
import com.jieli.btsmart.tool.room.entity.HearingAidFittingRecordEntity;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.settings.device.assistivelistening.charts.FittingChart;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.ui.widget.TipStateDialog;
import com.jieli.btsmart.ui.widget.WriteFittingDialog;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.utils.ToastUtil;

import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 验配结果记录-界面
 * 该界面，单个设备下线，不影响写入验配结果
 */
public class FittingResultFragment extends DeviceControlFragment implements DevicePopDialogFilter.IgnoreFilter {
    public static final String KEY_FITTING_RESULT_TYPE = "key_fitting_result_type";//0：验配结果，1：验配历史记录
    public static final String KEY_FITTING_RESULT = "key_fitting_result";
    private CommonActivity mActivity;
    private HearingAidFittingRecordEntity mFittingRecord;
    private boolean mIsWriteDevice = false;
    private FittingChart mFittingChart;
    private WriteFittingDialog mWriteFittingDialog;
    private WriteFittingDialog mSaveRecordDialog;
    private HearingAssitstViewModel mViewModel;
    private TipStateDialog mTipStateDialog;
    private final int MSG_DISMISS_TIP_STATE_DIALOG = 1;
    private final int MSG_ACTIVITY_FINISH = 2;
    private int mFittingResultType = 0;//0：验配结果，1：验配历史记录
    private Jl_BaseActivity.CustomBackPress mCustomBackPress = () -> !checkIsSaveFittingRecord();


    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_ACTIVITY_FINISH:
                    requireActivity().finish();
                    break;
                case MSG_DISMISS_TIP_STATE_DIALOG:
                    if (mTipStateDialog != null && mTipStateDialog.isShow()) {
                        mTipStateDialog.dismiss();
                        mTipStateDialog = null;
                    }
                    break;
            }
            return false;
        }
    });

    public static FittingResultFragment newInstance() {
        return new FittingResultFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        return inflater.inflate(R.layout.fragment_fitting_result, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
            mActivity.setCustomBackPress(mCustomBackPress);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(HearingAssitstViewModel.class);
        mViewModel.mDeviceDisconnectMLD.observe(getViewLifecycleOwner(), isDisconnect -> {
            requireActivity().onBackPressed();
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mFittingRecord = bundle.getParcelable(KEY_FITTING_RESULT);
            mFittingResultType = bundle.getInt(KEY_FITTING_RESULT_TYPE);
        }
        if (mActivity != null) {
            String topBarString = getString(R.string.fitting_results);
            if (mFittingRecord != null) {//验配数据不为空
                if (mFittingResultType == 0) {//验配结果
                    switch (mFittingRecord.gainsType) {
                        case 0:
                            topBarString = getString(R.string.left_ear_fitting_result);
                            break;
                        case 1:
                            topBarString = getString(R.string.right_ear_fitting_result);
                            break;
                        default:
                        case 2:
                            topBarString = getString(R.string.both_ear_fitting_result);
                            break;
                    }
                } else {//验配历史
                    if (TextUtils.isEmpty(mFittingRecord.recordName)) {
                        topBarString = formatTime(mFittingRecord.time);
                    } else {
                        topBarString = mFittingRecord.recordName;
                    }
                }
            }
            mActivity.updateTopBar(topBarString, R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);
        }
        mFittingChart = view.findViewById(R.id.view_fitting_chart);
        TextView tvWriteDevice = view.findViewById(R.id.tv_write_device);
        tvWriteDevice.setOnClickListener(v -> {
            Date date = Calendar.getInstance().getTime();
            String fittingRecordName = mFittingRecord.recordName;
            if (TextUtils.isEmpty(fittingRecordName)) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                fittingRecordName = format.format(date);
            }
            showWriteToDeviceDialog(fittingRecordName, date.getTime());
        });
        mFittingChart.setValueFormatter(mValueFormatter);
        updateFittingResultUI(mFittingRecord);
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }

    private void updateFittingResultUI(HearingAidFittingRecordEntity entity) {
        if (entity == null) return;
        View cvSingle = getView().findViewById(R.id.cv_threshold_mean_single);
        View cvBoth = getView().findViewById(R.id.cv_threshold_mean_both);
        View tvLeftLabel = getView().findViewById(R.id.tv_left_ear);
        View tvRightLabel = getView().findViewById(R.id.tv_right_ear);
        TextView mLeftThresholdBothTv = getView().findViewById(R.id.tv_left_ear_threshold_mean_value);
        TextView mRightThresholdBothTv = getView().findViewById(R.id.tv_right_ear_threshold_mean_value);
        TextView mThresholdSingleTv = getView().findViewById(R.id.tv_single_threshold_mean_value);
        switch (entity.gainsType) {
            case 0:
                cvSingle.setVisibility(View.VISIBLE);
                cvBoth.setVisibility(View.GONE);
                tvLeftLabel.setVisibility(View.VISIBLE);
                mThresholdSingleTv.setTextColor(Color.parseColor("#ff4e89f4"));
                break;
            case 1:
                cvSingle.setVisibility(View.VISIBLE);
                TextView tvSingleThresholdMean = getView().findViewById(R.id.tv_single_threshold_mean);
                tvSingleThresholdMean.setText(R.string.right_ear_threshold_mean);
                cvBoth.setVisibility(View.GONE);
                tvRightLabel.setVisibility(View.VISIBLE);
                mThresholdSingleTv.setTextColor(Color.parseColor("#E7933B"));
                break;
            case 2:
                tvLeftLabel.setVisibility(View.VISIBLE);
                tvRightLabel.setVisibility(View.VISIBLE);
                break;
        }
        //列表1 左耳
        List<FittingChart.LineChartData> lineChartDataLeft = new ArrayList<>();
        if (entity.leftChannelsValues != null && (entity.gainsType != 1)) {
            int index = 0;
            int leftSum = 0;
            for (float value : entity.leftChannelsValues) {
                lineChartDataLeft.add(new FittingChart.LineChartData(index, value));
                leftSum += value;
                index++;
            }
            //更新左耳听阈均值
            mThresholdSingleTv.setText(String.valueOf(leftSum / entity.leftChannelsValues.length));
            mLeftThresholdBothTv.setText(String.valueOf(leftSum / entity.leftChannelsValues.length));
        }
        //列表2 右耳
        List<FittingChart.LineChartData> lineChartDataRight = new ArrayList<>();
        if (entity.rightChannelsValues != null && (entity.gainsType != 0)) {
            int index = 0;
            int rightSum = 0;
            for (float value : entity.rightChannelsValues) {
                lineChartDataRight.add(new FittingChart.LineChartData(index, value));
                rightSum += value;
                index++;
            }
            //更新右耳听阈均值
            mThresholdSingleTv.setText(String.valueOf(rightSum / entity.rightChannelsValues.length));
            mRightThresholdBothTv.setText(String.valueOf(rightSum / entity.rightChannelsValues.length));
        }
        List<List<FittingChart.LineChartData>> lineData = new ArrayList<>();
        lineData.add(lineChartDataLeft);
        lineData.add(lineChartDataRight);
        mFittingChart.setLineData(lineData);
        mFittingChart.setDataLen(mFittingRecord.channelsNum);
    }

    private void showWriteStateDialog(boolean aBoolean) {
        if (mTipStateDialog != null) {
            return;
        }
        mTipStateDialog = new TipStateDialog();
        if (aBoolean) {
            mTipStateDialog.setImageResource(R.drawable.ic_fm_collect_freq_success);
            mTipStateDialog.setTips(getString(R.string.write_saved_successfuly));
        } else {
            mTipStateDialog.setImageResource(R.drawable.ic_fm_collect_freq_existed);
            mTipStateDialog.setTips(getString(R.string.write_saved_failed));
        }
        mTipStateDialog.setCallback(() -> {
            mTipStateDialog = null;
            handler.removeMessages(MSG_DISMISS_TIP_STATE_DIALOG);
        });
        mTipStateDialog.show(getChildFragmentManager(), TipStateDialog.class.getSimpleName());
        handler.sendEmptyMessageDelayed(MSG_DISMISS_TIP_STATE_DIALOG, 700);
    }

    private void showWriteToDeviceDialog(String fittingRecordName, long time) {
        if (!isAdded() || isDetached()) return;
        if (mWriteFittingDialog == null) {
            mWriteFittingDialog = new WriteFittingDialog.Builder()
                    .setWidth(0.95f)
                    .setCancelable(false)
                    .setInputText(fittingRecordName)
                    .setTime(time)
                    .setLeftText(getString(R.string.cancel))
                    .setLeftColor(getResources().getColor(R.color.gray_text_989898))
                    .setRightText(getString(R.string.confirm))
                    .setRightColor(getResources().getColor(R.color.blue_448eff))
                    .setOnInputTextListener(mOnInputTextListener)
                    .create();
        }
        mWriteFittingDialog.updateEditText();
        mWriteFittingDialog.updateDialog();
        if (!mWriteFittingDialog.isShow() && !isDetached() && getActivity() != null) {
            mWriteFittingDialog.show(getActivity().getSupportFragmentManager(), "input_text_dialog");
        }
    }

    private void showSaveRecordDialog(String fittingRecordName, long time) {
        if (!isAdded() || isDetached()) return;
        if (mSaveRecordDialog == null) {
            mSaveRecordDialog = new WriteFittingDialog.Builder()
                    .setWidth(0.95f)
                    .setTitle(getString(R.string.is_save_to_fitting_record))
                    .setCancelable(false)
                    .setSaveViewVisible(false)
                    .setInputText(fittingRecordName)
                    .setTime(time)
                    .setLeftText(getString(R.string.cancel))
                    .setLeftColor(getResources().getColor(R.color.gray_text_989898))
                    .setRightText(getString(R.string.confirm))
                    .setRightColor(getResources().getColor(R.color.blue_448eff))
                    .setOnInputTextListener(new WriteFittingDialog.OnInputTextListener() {
                        @Override
                        public void onDismiss(WriteFittingDialog dialog) {
                            requireActivity().finish();
                        }

                        @Override
                        public void onInputText(WriteFittingDialog dialog, String text) {

                        }

                        @Override
                        public void onInputFinish(WriteFittingDialog dialog, String value, String lastValue, boolean isSave, long time) {
                            if (value.length() > 20) {
                                ToastUtil.showToastShort(R.string.fitting_record_too_long);
                                return;
                            }
                            dialog.dismiss();
                            if (mFittingRecord != null) {
                                //成功回调
                                boolean isDateString = isValidDate(value);
                                if (!isDateString || !TextUtils.isEmpty(mFittingRecord.recordName)) {//不是时间类型，或者记录名不为空,需要更新名字
                                    mFittingRecord.recordName = value;
                                }
                                //更新记录的时间
                                mFittingRecord.time = time;
                                WeakReference<Executor> weakExecutor = AppDatabase.getInstance().getWeakExecutor();
                                weakExecutor.get().execute(() -> {
                                    FittingRecordDao dao = AppDatabase.getInstance().fittingRecordDao();
                                    HearingAidFittingRecordEntity srcEntity = dao.getFittingRecord(mFittingRecord.id);
                                    if (srcEntity == null) {
                                        dao.insertFittingRecord(mFittingRecord);
                                    } else {
                                        dao.updateFittingRecord(mFittingRecord);
                                    }
                                });
                            }
                        }
                    })
                    .create();
        }
        mSaveRecordDialog.updateEditText();
        mSaveRecordDialog.updateDialog();
        if (!mSaveRecordDialog.isShow() && !isDetached() && getActivity() != null) {
            mSaveRecordDialog.show(getActivity().getSupportFragmentManager(), "input_text_dialog");
        }
    }

    private boolean isValidDate(String str) {
        if (str.length() > 16) return false;
        boolean convertSuccess = true;// 指定日期格式为四位年/两位月份/两位日期，注意yyyy/MM/dd区分大小写；
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            format.setLenient(false);
            format.parse(str);
        } catch (ParseException e) {
            convertSuccess = false;
        }
        return convertSuccess;
    }

    /**
     * 检查验配结果是否保存，验配记录则不检查
     */
    private boolean checkIsSaveFittingRecord() {
        boolean result = true;
        if (mFittingResultType == 0) {
            result = false;
            if (!mIsWriteDevice) {//未写入
                Date date = Calendar.getInstance().getTime();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String fittingRecordName = format.format(date);
                showSaveRecordDialog(fittingRecordName, date.getTime());
            }
        }
        return result;
    }

    private String formatTime(long time) {
        Date date = new Date(time);
        final SimpleDateFormat mFormatWeek = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeString = mFormatWeek.format(date);
        return timeString;
    }

    private WriteFittingDialog.OnInputTextListener mOnInputTextListener = new WriteFittingDialog.OnInputTextListener() {
        @Override
        public void onDismiss(WriteFittingDialog dialog) {

        }

        @Override
        public void onInputText(WriteFittingDialog dialog, String text) {

        }

        @Override
        public void onInputFinish(WriteFittingDialog dialog, String value, String lastValue, boolean isSave, long time) {
            if (value.length() > 20) {
                ToastUtil.showToastShort("名字过长，请限制在20个字符内");
                return;
            }
            dialog.dismiss();
            if (mFittingRecord != null) {
               /* float[] leftGainArray = new float[0];
                float[] rightGainArray = new float[0];
                float[] tempGainArray = mFittingRecord.leftChannelsValues;
                if (tempGainArray != null && tempGainArray.length > 0) {
                    leftGainArray = new float[tempGainArray.length];
                    for (int i = 0; i < tempGainArray.length; i++) {
                        leftGainArray[i] = tempGainArray[i];
                    }
                }
                tempGainArray = mFittingRecord.rightChannelsValues;
                if (tempGainArray != null && tempGainArray.length > 0) {
                    rightGainArray = new float[tempGainArray.length];
                    for (int i = 0; i < tempGainArray.length; i++) {
                        rightGainArray[i] = tempGainArray[i];
                    }
                }*/
                HearingFrequenciesInfo hearingFrequenciesInfo = new HearingFrequenciesInfo();
                hearingFrequenciesInfo.setGainsType(mFittingRecord.gainsType);
                hearingFrequenciesInfo.setLeftGains(mFittingRecord.leftChannelsValues);
                hearingFrequenciesInfo.setRightGains(mFittingRecord.rightChannelsValues);
               /* if (SConstant.TEST_FUNC) {
                    if (isSave) {
                        boolean isDateString = isValidDate(value);
                        if (!isDateString || !TextUtils.isEmpty(mFittingRecord.recordName)) {//不是时间类型，或者记录名不为空,需要更新名字
                            mFittingRecord.recordName = value;
                        }
                        //更新记录的时间
                        mFittingRecord.time = time;
                        WeakReference<Executor> weakExecutor = AppDatabase.getInstance().getWeakExecutor();
                        weakExecutor.get().execute(() -> {
                            AppDatabase.getInstance().fittingRecordDao().updateFittingRecord(mFittingRecord);
                        });
                    }
                    showWriteStateDialog(true);
                    return;
                }*/
                mViewModel.setHearingAssistFrequencies(hearingFrequenciesInfo, new OnRcspActionCallback<Boolean>() {
                    @Override
                    public void onSuccess(BluetoothDevice device, Boolean message) {
                        //成功回调
                        mIsWriteDevice = true;
                        if (isSave) {
                            boolean isDateString = isValidDate(value);
                            if (!isDateString || !TextUtils.isEmpty(mFittingRecord.recordName)) {//不是时间类型，或者记录名不为空,需要更新名字
                                mFittingRecord.recordName = value;
                            }
                            //更新记录的时间
                            mFittingRecord.time = time;
                            WeakReference<Executor> weakExecutor = AppDatabase.getInstance().getWeakExecutor();
                            weakExecutor.get().execute(() -> {
                                FittingRecordDao dao = AppDatabase.getInstance().fittingRecordDao();
                                HearingAidFittingRecordEntity srcEntity = dao.getFittingRecord(mFittingRecord.id);
                                if (srcEntity == null) {
                                    dao.insertFittingRecord(mFittingRecord);
                                } else {
                                    dao.updateFittingRecord(mFittingRecord);
                                }
                            });
                        }
                        if (!isAdded() || isDetached()) return;
                        showWriteStateDialog(true);
                        handler.sendEmptyMessageDelayed(MSG_ACTIVITY_FINISH, 1000);
                    }

                    @Override
                    public void onError(BluetoothDevice device, BaseError error) {
                        //失败回调
                        if (!isAdded() || isDetached()) return;
                        showWriteStateDialog(false);
                        Log.e(TAG, "onError: " + error.toString());
                    }
                });
            }
        }
    };

    private FittingChart.ValueFormatter mValueFormatter = new FittingChart.ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
            int freqIndex = (int) value;
            return AppUtil.freqValueToFreqShowText(mFittingRecord.channelsFreqs[freqIndex]);
        }
    };
}