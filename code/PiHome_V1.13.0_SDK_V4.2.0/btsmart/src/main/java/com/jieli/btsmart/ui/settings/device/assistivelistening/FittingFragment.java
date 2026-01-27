package com.jieli.btsmart.ui.settings.device.assistivelistening;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.hearing.HearingAssistInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.room.entity.HearingAidFittingRecordEntity;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.settings.device.assistivelistening.charts.FittingChart;
import com.jieli.btsmart.ui.settings.device.assistivelistening.charts.FittingChart.BarChartData;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.ui.widget.TipDialog;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import static com.jieli.btsmart.ui.settings.device.assistivelistening.FittingResultFragment.KEY_FITTING_RESULT;
import static com.jieli.btsmart.ui.settings.device.assistivelistening.FittingResultFragment.KEY_FITTING_RESULT_TYPE;

/**
 * 验配-界面
 * 特殊情况处理：
 * 一、设备全部断开，退出且是否保存记录
 * 二、部分声道关闭，影响接下来的验配，退出且是否保存记录
 * 三、部分声道关闭，不影响接下来的验配，继续验配
 */
public class FittingFragment extends DeviceControlFragment implements DevicePopDialogFilter.IgnoreFilter {
    public static final String KEY_FITTING_CONFIGURE = "KEY_FITTING_CONFIGURE";
    private CommonActivity mActivity;
    private FittingChart mFittingChartLeft;
    private FittingChart mFittingChartRight;
    private ViewPager2 mFittingViewPager2;
    private HearingAssistInfo mHearingAssistInfo;
    private TextView mBackLastStepTv;
    private LinearLayout mBtnInaudibility;//听不见按钮
    private LinearLayout mBtnHear;//听得见按钮
    private FittingViewModel mViewModel;
    private int currentFittingPosition = 0;
    private BluetoothDevice mTargetDevice;
    private int mFittingGainType = -1;//0:左，1：右，2：左右
    private final BaseActivity.CustomBackPress mCustomBackPress = () -> {
        boolean result = mViewModel.isFinishedFitting();
        if (!result) {
            mViewModel.stopFitting();
        }
        return false/*!checkIsFinishFitting()*/;
    };

    public static FittingFragment newInstance() {
        return new FittingFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        return inflater.inflate(R.layout.fragment_fitting, container, false);
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
        Bundle bundle = getArguments();
        if (bundle != null) {
            String hearingAssistInfoStr = bundle.getString(KEY_FITTING_CONFIGURE);
            mHearingAssistInfo = new Gson().fromJson(hearingAssistInfoStr, HearingAssistInfo.class);
        }
        if (mHearingAssistInfo != null) {// 根据Fitting 配置显示 chart 多少条
            if (mFittingChartLeft != null) {
                mFittingChartLeft.setDataLen(mHearingAssistInfo.getChannels());
            }
            if (mFittingChartRight != null) {
                mFittingChartRight.setDataLen(mHearingAssistInfo.getChannels());
            }
        }
        if (mActivity != null) {
            mActivity.updateTopBar(getString(R.string.fitting), R.drawable.ic_back_black, v -> {
                mActivity.onBackPressed();
            }, 0, null);
        }
        mViewModel = new ViewModelProvider(this).get(FittingViewModel.class);
        mTargetDevice = mViewModel.getTargetDevice();
        if (mTargetDevice == null) {
            ToastUtil.showToastShort(R.string.ota_error_msg_device_disconnected);
            requireActivity().onBackPressed();
        }
        mViewModel.setFittingConfigure(mHearingAssistInfo, mTargetDevice == null ? null : mTargetDevice.getAddress());
        mViewModel.mDeviceDisconnectMLD.observe(getViewLifecycleOwner(), isDisconnect -> {
            requireActivity().onBackPressed();
        });
        mViewModel.mCurrentFittingPositionMLD.observe(getViewLifecycleOwner(), position -> {//当前验配position
            currentFittingPosition = position;
            mFittingViewPager2.setCurrentItem(currentFittingPosition, false);
            if (mFittingChartLeft != null) {
                mFittingChartLeft.setHighLightLast(position == 0);
            }
            if (mFittingChartRight != null) {
                mFittingChartRight.setHighLightLast(position == 1);
            }
        });
        mViewModel.mLeftChannelsValuesMLD.observe(getViewLifecycleOwner(), channelsValue -> {
            updateFittingChart(channelsValue, mFittingChartLeft);
        });
        mViewModel.mRightChannelsValuesMLD.observe(getViewLifecycleOwner(), channelsValue -> {
            updateFittingChart(channelsValue, mFittingChartRight);
        });
        mViewModel.mBackLastStepEnableMLD.observe(getViewLifecycleOwner(), enable -> {
            mBackLastStepTv.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        });
        mViewModel.mIsFittingFinishMLD.observe(getViewLifecycleOwner(), isFinish -> {
            toFittingResultFragment();
        });
        mViewModel.mHearingChannelsStatusMLD.observe(getViewLifecycleOwner(), hearingChannelsStatus -> {
            boolean isInterrupt = false;//是否中断结束验配
            switch (mFittingGainType) {
                case 0://验配左耳
                    if (!hearingChannelsStatus.getLeftChannelStatus()) {//左声道关闭
                        isInterrupt = true;
                    }
                    break;
                case 1://验配右耳
                    if (!hearingChannelsStatus.getRightChannelStatus()) {//左声道关闭
                        isInterrupt = true;
                    }
                    break;
                case 2://验配左右耳
                    if (!hearingChannelsStatus.getRightChannelStatus()) {//右声道关闭，右声道最后验配
                        isInterrupt = true;
                    }
                    if (!hearingChannelsStatus.getLeftChannelStatus()) {//左声道关闭
//                        if (currentFittingPosition == 0) {//当前是否已验配完左声道
                        isInterrupt = true;
//                        }
                    }
                    break;
                default:
                    isInterrupt = true;
                    break;
            }
            if (isInterrupt) {
                requireActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBtnInaudibility = view.findViewById(R.id.ll_inaudibility);
        mBtnHear = view.findViewById(R.id.ll_hear);
        mBackLastStepTv = view.findViewById(R.id.tv_back_last_step);
        mFittingViewPager2 = view.findViewById(R.id.vp2_fitting);
        View clPrepare = view.findViewById(R.id.cv_fitting_prepare);
        View scrollViewFitting = view.findViewById(R.id.scrollView_fitting);
        TextView tvIKnow = view.findViewById(R.id.tv_i_know);
        ConstraintLayout cvChooseGainType = view.findViewById(R.id.cv_fitting_choose_gain_type);
        View tvGainTypeBoth = view.findViewById(R.id.tv_fitting_gains_type_both);
        View tvGainTypeLeft = view.findViewById(R.id.tv_fitting_gains_type_left);
        View tvGainTypeRight = view.findViewById(R.id.tv_fitting_gains_type_right);
        tvIKnow.setOnClickListener(v -> {
            clPrepare.setVisibility(View.GONE);
            scrollViewFitting.setBackgroundResource(R.color.gray_F8FAFC);
            ADVInfoResponse advInfoResponse = mViewModel.getADVInfoResponse();
            if (advInfoResponse != null) {
                if (advInfoResponse.getLeftDeviceQuantity() <= 0) {//左设备下线
                    tvGainTypeBoth.setVisibility(View.GONE);
                    tvGainTypeLeft.setVisibility(View.GONE);
                }
                if (advInfoResponse.getRightDeviceQuantity() <= 0) {//右设备下线
                    tvGainTypeBoth.setVisibility(View.GONE);
                    tvGainTypeRight.setVisibility(View.GONE);
                }
            }
            cvChooseGainType.setVisibility(View.VISIBLE);
//            checkNoFinishRecord();
        });
        tvGainTypeBoth.setOnClickListener(v -> {
            mFittingGainType = 2;
            AppUtil.setCurrentFittingGainType(mFittingGainType);
            refreshUIByGainsType(mFittingGainType);
            mViewModel.setFittingGainsType(mFittingGainType);
            mViewModel.startFitting();
        });
        tvGainTypeLeft.setOnClickListener(v -> {
            mFittingGainType = 0;
            AppUtil.setCurrentFittingGainType(mFittingGainType);
            refreshUIByGainsType(mFittingGainType);
            mViewModel.setFittingGainsType(mFittingGainType);
            mViewModel.startFitting();
        });
        tvGainTypeRight.setOnClickListener(v -> {
            mFittingGainType = 1;
            AppUtil.setCurrentFittingGainType(mFittingGainType);
            refreshUIByGainsType(mFittingGainType);
            mViewModel.setFittingGainsType(mFittingGainType);
            mViewModel.startFitting();
        });
        mBackLastStepTv.setOnClickListener(v -> {
            if (!mViewModel.backLastStep()) {// 返回上一步
                ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
            }
        });
        mBtnHear.setOnClickListener(v -> {
            if (!mViewModel.fittingFreqHear()) {//听得见
                ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
            }
        });
        mBtnInaudibility.setOnClickListener(v -> {
            if (!mViewModel.fittingFreqInaudibility()) {//听不见
                ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
            }
        });
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }

    private void initChartView(int gainsType) {
        final TabLayout earTabLayout = this.getView().findViewById(R.id.tblayout_ear);
        if (gainsType == 2) {
            earTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    mFittingViewPager2.setCurrentItem(tab.getPosition(), false);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    mFittingViewPager2.setCurrentItem(tab.getPosition(), false);
                }
            });
        }
        int pagesNum = gainsType == 2 ? 2 : 1;
        mFittingViewPager2.setOffscreenPageLimit(pagesNum);
        mFittingViewPager2.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View viewFitting = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_fitting, parent, false);
                RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(viewFitting) {
                };
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                FittingChart chart = holder.itemView.findViewById(R.id.view_fitting_chart);
                chart.setValueFormatter(mValueFormatter);
                switch (gainsType) {
                    case 0:
                        mFittingChartLeft = chart;
                        mFittingChartLeft.setDataLen(mHearingAssistInfo.getChannels());
                        View tvLeftEar = holder.itemView.findViewById(R.id.tv_left_ear);
                        tvLeftEar.setVisibility(View.VISIBLE);
                        updateFittingChart(mViewModel.mLeftChannelsValuesMLD.getValue(), chart);
                        break;
                    case 1:
                        mFittingChartRight = chart;
                        mFittingChartRight.setDataLen(mHearingAssistInfo.getChannels());
                        View tvRightEar = holder.itemView.findViewById(R.id.tv_right_ear);
                        tvRightEar.setVisibility(View.VISIBLE);
                        updateFittingChart(mViewModel.mRightChannelsValuesMLD.getValue(), chart);
                        break;
                    case 2:
                        if (position == 0) {
                            mFittingChartLeft = chart;
                            mFittingChartLeft.setDataLen(mHearingAssistInfo.getChannels());
                            updateFittingChart(mViewModel.mLeftChannelsValuesMLD.getValue(), chart);
                        } else if (position == 1) {
                            mFittingChartRight = chart;
                            mFittingChartRight.setDataLen(mHearingAssistInfo.getChannels());
                            updateFittingChart(mViewModel.mRightChannelsValuesMLD.getValue(), chart);
                        }
                        break;
                }
            }

            @Override
            public int getItemCount() {
                return pagesNum;
            }
        });
        mFittingViewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                earTabLayout.setScrollPosition(position, positionOffset, true);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                refreshFittingControlView(position);
                earTabLayout.setScrollPosition(position, 0, true);
            }
        });
    }

    /**
     * 检查是否有未完成记录
     */
    private void checkNoFinishRecord() {
        SharedPreferences sharedPreferences = PreferencesHelper.getSharedPreferences(getContext());
        String key = HearingAidFittingRecordEntity.createKey(mTargetDevice.getAddress(), mHearingAssistInfo.getChannels());
        String cacheFittingString = sharedPreferences.getString(key, null);
        HearingAidFittingRecordEntity lastRecordEntity = new Gson().fromJson(cacheFittingString, HearingAidFittingRecordEntity.class);
        if (lastRecordEntity != null) {
            PreferencesHelper.putStringValue(getContext(), key, null);//清除记录
            TipDialog tipDialog = new TipDialog.Builder()
                    .setWidth(0.95f)
                    .setTitle(getString(R.string.tips))
                    .setContent(getString(R.string.check_no_finish_record))
                    .setTipsColor(getResources().getColor(R.color.gray_5F5F5F))
                    .setCancelable(false)
                    .setLeftText(getString(R.string.restart))
                    .setLeftColor(getResources().getColor(R.color.blue_448eff))
                    .setRightText(getString(R.string.continue_str))
                    .setRightColor(getResources().getColor(R.color.blue_448eff))
                    .setOnTipDialogListener(new TipDialog.OnTipDialogListener() {
                        @Override
                        public void onDismiss(TipDialog dialog) {

                        }

                        @Override
                        public void onRightBtnClick(TipDialog dialog) {
                            dialog.dismiss();
                            refreshUIByGainsType(lastRecordEntity.gainsType);
                            mViewModel.importRecord(lastRecordEntity);// 导入记录
                            mViewModel.startFitting();
                        }

                        @Override
                        public void onLeftBtnClick(TipDialog dialog) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            tipDialog.updateDialog();
            if (!tipDialog.isShow() && !isDetached() && getActivity() != null) {
                tipDialog.show(getChildFragmentManager(), "showExitAndSaveCurrentFittingRecord");
            }
        }
    }

    private void refreshUIByGainsType(int fittingGainType) {
        View clTest = getView().findViewById(R.id.cv_fitting_test);
        View tbLayoutEar = getView().findViewById(R.id.tblayout_ear);
        ConstraintLayout cvChooseGainType = getView().findViewById(R.id.cv_fitting_choose_gain_type);
        tbLayoutEar.setVisibility(fittingGainType == 2 ? View.VISIBLE : View.GONE);
        cvChooseGainType.setVisibility(View.GONE);
        clTest.setVisibility(View.VISIBLE);
        int topBarStringRes;
        switch (fittingGainType) {
            case 0:
                topBarStringRes = R.string.left_ear_fitting;
                break;
            case 1:
                topBarStringRes = R.string.right_ear_fitting;
                break;
            default:
            case 2:
                topBarStringRes = R.string.both_ear_fitting;
                break;
        }
        mActivity.updateTopBar(getString(topBarStringRes), R.drawable.ic_back_black, v1 -> {
            mActivity.onBackPressed();
        }, 0, null);
        initChartView(fittingGainType);
    }

    private boolean checkIsFinishFitting() {
        boolean result = mViewModel.isFinishedFitting();
        if (!result) {
            showExitAndSaveCurrentFittingRecord();
        }
        return result;
    }

    /**
     * 保存当前记录
     */
    private void showExitAndSaveCurrentFittingRecord() {
        if (mTargetDevice == null) return;
        TipDialog tipDialog = new TipDialog.Builder()
                .setWidth(0.95f)
                .setTitle(getString(R.string.tips))
                .setContent(getString(R.string.check_exitt_fitting))
                .setTipsColor(getResources().getColor(R.color.gray_5F5F5F))
                .setCancelable(false)
                .setLeftText(getString(R.string.no_save_operation))
                .setLeftColor(getResources().getColor(R.color.blue_448eff))
                .setRightText(getString(R.string.save_operation))
                .setRightColor(getResources().getColor(R.color.blue_448eff))
                .setOnTipDialogListener(new TipDialog.OnTipDialogListener() {
                    @Override
                    public void onDismiss(TipDialog dialog) {

                    }

                    @Override
                    public void onRightBtnClick(TipDialog dialog) {
                        dialog.dismiss();
                        HearingAidFittingRecordEntity noFinishEdEntity = mViewModel.export();// 保存记录
                        String key = HearingAidFittingRecordEntity.createKey("test"/*mTargetDevice.getAddress()*/, mHearingAssistInfo.getChannels());
                        PreferencesHelper.putStringValue(getContext(), key, new Gson().toJson(noFinishEdEntity));
                        mViewModel.stopFitting();
                        if (mActivity != null) {
                            mActivity.setCustomBackPress(null);
                            mActivity.onBackPressed();
                        }
                    }

                    @Override
                    public void onLeftBtnClick(TipDialog dialog) {
                        dialog.dismiss();
                        mViewModel.stopFitting();
                        if (mActivity != null) {
                            mActivity.setCustomBackPress(null);
                            mActivity.onBackPressed();
                        }
                    }
                })
                .create();
        tipDialog.updateDialog();
        if (!tipDialog.isShow() && !isDetached() && getActivity() != null) {
            tipDialog.show(getChildFragmentManager(), "showExitAndSaveCurrentFittingRecord");
        }
    }

    /**
     * 更新图表数据
     */
    private void updateFittingChart(float[] channelsValue, FittingChart chart) {
        if (channelsValue == null || chart == null) return;
        if (chart.getDataLen() > 6) {
            chart.scrollToPosition(channelsValue.length - 1);
        }
        List<FittingChart.BarChartData> barChartData = new ArrayList<>();
        for (int i = 0; i < channelsValue.length; i++) {
            barChartData.add(new BarChartData(i, channelsValue[i]));
        }
        chart.setBarData(barChartData);
        chart.setHighLightLast(true);
    }

    private void refreshFittingControlView(int viewPagerPosition) {
        int isVisible = viewPagerPosition != currentFittingPosition ? View.INVISIBLE : View.VISIBLE;
        mBtnHear.setVisibility(isVisible);
        mBtnInaudibility.setVisibility(isVisible);
        int isBackLastVisible = (viewPagerPosition == currentFittingPosition) && (mViewModel != null && mViewModel.mBackLastStepEnableMLD.getValue()) ? View.VISIBLE : View.INVISIBLE;
        mBackLastStepTv.setVisibility(isBackLastVisible);
    }

    private void toFittingResultFragment() {
        HearingAidFittingRecordEntity data = mViewModel.export();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_FITTING_RESULT_TYPE, 0);
        bundle.putParcelable(KEY_FITTING_RESULT, data);
        CommonActivity.startCommonActivity(getActivity(), FittingResultFragment.class.getCanonicalName(), bundle);
        mActivity.finish();
    }

    private FittingChart.ValueFormatter mValueFormatter = new FittingChart.ValueFormatter() {
        @Override
        public String getFormattedValue(float value) {
            int freqIndex = (int) value;
            return AppUtil.freqValueToFreqShowText(mHearingAssistInfo.getFrequencies()[freqIndex]);
        }
    };
}