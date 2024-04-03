package com.jieli.btsmart.ui.settings.device.assistivelistening;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.device.hearing.HearingAssistInfo;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.FittingHistoryAdapter;
import com.jieli.btsmart.tool.room.AppDatabase;
import com.jieli.btsmart.tool.room.entity.HearingAidFittingRecordEntity;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.ToastUtil;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executor;

import static com.jieli.btsmart.ui.settings.device.assistivelistening.FittingResultFragment.KEY_FITTING_RESULT;
import static com.jieli.btsmart.ui.settings.device.assistivelistening.FittingResultFragment.KEY_FITTING_RESULT_TYPE;

/**
 * 辅听验配-界面
 */
public class AssistiveListeningFragment extends DeviceControlFragment implements DevicePopDialogFilter.IgnoreFilter {
    public static String KEY_HEARING_ASSIST_INFO = "key_hearing_assist_info";
    private CommonActivity mActivity;
    private FittingHistoryAdapter mAdapter;
    private HearingAssitstViewModel mViewModel;
    private HearingAssistInfo mHearingAssistInfo;

    public static AssistiveListeningFragment newInstance() {
        return new AssistiveListeningFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        return inflater.inflate(R.layout.fragment_assistive_listening, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            String hearingAssistInfoString = bundle.getString(KEY_HEARING_ASSIST_INFO);
            mHearingAssistInfo = new Gson().fromJson(hearingAssistInfoString, HearingAssistInfo.class);
        }
        View fittingView = view.findViewById(R.id.cv_device_settings_noise_control);
        RecyclerView fittingRecyclerView = view.findViewById(R.id.rv_fitting_history);
        mAdapter = new FittingHistoryAdapter();
        fittingRecyclerView.setAdapter(mAdapter);
        fittingView.setOnClickListener(v -> {
            if (mHearingAssistInfo == null) {//处理未拿到
                ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
                return;
            }
            Bundle bundle1 = new Bundle();
            bundle1.putString(FittingFragment.KEY_FITTING_CONFIGURE, new Gson().toJson(mHearingAssistInfo));
            CommonActivity.startCommonActivity(getActivity(), FittingFragment.class.getCanonicalName(), bundle1);
        });
        mAdapter.setOnFittingHistoryEventListener(new FittingHistoryAdapter.OnFittingHistoryEventListener() {
            @Override
            public void onFittingHistoryDelete(BaseQuickAdapter adapter, HearingAidFittingRecordEntity entity, int itemPosition) {
                WeakReference<Executor> weakExecutor = AppDatabase.getInstance().getWeakExecutor();
                weakExecutor.get().execute(() -> {
                    AppDatabase.getInstance().fittingRecordDao().deleteFittingRecord(entity);
                });
            }

            @Override
            public void onFittingHistoryClick(BaseQuickAdapter adapter, HearingAidFittingRecordEntity entity, int itemPosition) {
                Bundle bundle = new Bundle();
                bundle.putInt(KEY_FITTING_RESULT_TYPE, 1);
                bundle.putParcelable(KEY_FITTING_RESULT, entity);
                CommonActivity.startCommonActivity(getActivity(), FittingResultFragment.class.getCanonicalName(), bundle);
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof CommonActivity) {
            mActivity = (CommonActivity) context;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mActivity != null) {
            mActivity.updateTopBar(getString(R.string.hearing_aid_fitting), R.drawable.ic_back_black, v -> {
                if (mActivity != null) {
                    mActivity.onBackPressed();
                }
            }, 0, null);
        }
        mViewModel = new ViewModelProvider(this).get(HearingAssitstViewModel.class);
        BluetoothDevice device = mViewModel.getTargetDevice();
        if (device == null) {
            ToastUtil.showToastShort(R.string.ota_error_msg_device_disconnected);
            requireActivity().onBackPressed();
            return;
        }
        if (mHearingAssistInfo != null) {
            String key = HearingAidFittingRecordEntity.createKey(device.getAddress(), mHearingAssistInfo.getChannels());
            requireAllFittingRecord(key);
        }
        mViewModel.mDeviceDisconnectMLD.observe(getViewLifecycleOwner(), isDisconnect -> {
            requireActivity().onBackPressed();
        });
        mViewModel.mHearingChannelsStatusMLD.observe(getViewLifecycleOwner(), hearingChannelsStatus -> {
            //todo 监听当前验配的耳和断线的是不是同一个
            boolean isInterrupt = false;//是否结束验配
            switch (AppUtil.getCurrentFittingGainType()) {
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
    public void onDestroy() {
        super.onDestroy();
//        mViewModel.stopHearingAssistFitting(null);
    }

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }

    private void requireAllFittingRecord(String key) {
        LiveData<List<HearingAidFittingRecordEntity>> allFittingRecordsLiveData = AppDatabase.getInstance().fittingRecordDao().getFittingRecords(key);
        allFittingRecordsLiveData.observe(getViewLifecycleOwner(), entities -> {
            Log.d(TAG, "requireAllFittingRecord: " + entities.size());
            requireActivity().runOnUiThread(() -> updateFittingRecord(entities));
        });
    }

    private void updateFittingRecord(List<HearingAidFittingRecordEntity> entities) {
        mAdapter.setList(entities);
    }

}