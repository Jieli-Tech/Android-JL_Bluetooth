package com.jieli.btsmart.ui.translate.session;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.translation.RecordType;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.databinding.FragmentTranslationSessionBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.translate.call.CallRecordFragment;
import com.jieli.btsmart.ui.translate.face_to_face.FaceToFaceRecordFragment;
import com.jieli.btsmart.ui.translate.record.SessionRecordFragment;
import com.jieli.btsmart.ui.widget.dialog.CalendarSelector;
import com.jieli.btsmart.ui.widget.dialog.ConfirmationDialog;
import com.jieli.btsmart.ui.widget.dialog.RecordTypeDialog;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * TranslationSessionFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译记录界面
 * @since 2025/8/29
 */
public class TranslationSessionFragment extends DeviceControlFragment {

    private FragmentTranslationSessionBinding mBinding;
    private TranslationSessionViewModel mViewModel;
    private SessionRecordAdapter mAdapter;

    private RecordType mRecordType;
    private Date mDate;

    public static TranslationSessionFragment newInstance() {
        return new TranslationSessionFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentTranslationSessionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        final BluetoothDevice device = null == bundle ? null : bundle.getParcelable(SConstant.KEY_BLUETOOTH_DEVICE);
        if (null == device) {
            finish();
            return;
        }
        mRecordType = new RecordType().setTranslationMode(-1)
                .setTitle(getString(R.string.all_type))
                .setResId(R.drawable.ic_all_record_purple);
        mDate = Calendar.getInstance().getTime();
        mViewModel = new ViewModelProvider(this, new TranslationSessionViewModel.Factory(device))
                .get(TranslationSessionViewModel.class);
        initUI();
        addObserver();
    }

    private void initUI() {
        hideTopBar();
        if (requireActivity() instanceof BaseActivity) {
            ((BaseActivity) requireActivity()).setCustomBackPress(() -> {
                exitFragment();
                return true;
            });
        }
        mBinding.viewToolBar.tvTitle.setText(getString(R.string.translation_record));
        mBinding.viewToolBar.tvLeft.setOnClickListener(v -> exitFragment());
        mBinding.viewToolBar.tvRight.setOnClickListener(v -> {
            if (mAdapter.isEditMode()) {
                final int selectItemSize = mAdapter.getSelectedItemList().size();
                final int itemSize = mAdapter.getData().size();
                if (itemSize == 0) return;
                if (selectItemSize == itemSize) { //全选状态
                    mAdapter.clearSelectedItemList();
                } else {
                    mAdapter.allSelectItemList();
                }
                return;
            }
            mAdapter.setEditMode(true);
        });

        mBinding.tvRecordType.setOnClickListener(v -> showRecordTypeDialog());
        mBinding.tvRecordTime.setOnClickListener(v -> showCalendarSelector());
        mBinding.btnDelete.setOnClickListener(v -> {
            if (!mAdapter.isEditMode()) return;
            List<Integer> sessionIds = mAdapter.getSelectedItemList();
            if (sessionIds.isEmpty()) {
                showTips(getString(R.string.select_record_tips));
                return;
            }
            showConfirmationDialog(sessionIds);
        });

        mAdapter = new SessionRecordAdapter(new SessionRecordAdapter.OnStateListener() {
            @Override
            public void onEditMode(boolean isEditMode) {
                updateEditMode(isEditMode);
            }

            @Override
            public void onSelectItem(int selected, int itemSize) {
                updateSelectItemCount(selected, itemSize);
            }
        });
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            final TranslationSessionRecord sessionRecord = mAdapter.getItem(position);
            if (mAdapter.isEditMode()) {
                mAdapter.updateSelectedItem(sessionRecord);
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_BLUETOOTH_DEVICE, mViewModel.getDevice());
            bundle.putInt(SConstant.KEY_SESSION_ID, sessionRecord.getSession().getId());
            String clazzName = null;
            switch (sessionRecord.getSession().getTranslationMode()) {
                case TranslationMode.MODE_RECORDING_TRANSLATION:
                    clazzName = SessionRecordFragment.class.getCanonicalName();
                    break;
                case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION:
                    clazzName = FaceToFaceRecordFragment.class.getCanonicalName();
                    break;
                case TranslationMode.MODE_CALL_TRANSLATION:
                    clazzName = CallRecordFragment.class.getCanonicalName();
                    break;
            }
            if (null == clazzName) return;
            ContentActivity.startActivity(requireContext(), clazzName, bundle);
        });
        mBinding.rvSessionRecord.setAdapter(mAdapter);
        View emptyView = LinearLayout.inflate(requireContext(), R.layout.view_no_records, null);
        mAdapter.setEmptyView(emptyView);

        updateEditMode(mAdapter.isEditMode());
        querySessionRecords(mRecordType, mDate, true);
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> {
            if (isInvalid() || null == connection) return;
            if (BluetoothUtil.deviceEquals(connection.getDevice(), mViewModel.getDevice())
                    && connection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        mViewModel.sessionRecordMLD.observe(getViewLifecycleOwner(), recordsResult -> {
            if (isInvalid() || null == recordsResult || recordsResult.getOp() != TranslationSessionViewModel.OP_QUERY_BY_DAY)
                return;
            if (recordsResult.getState() == StateResult.STATE_WORKING) {
                showLoadingDialog(getString(R.string.loading));
                return;
            }
            dismissLoadingDialog();
            if (recordsResult.getState() == StateResult.STATE_FINISH) {
                if (recordsResult.isSuccess()) {
                    mAdapter.setList(recordsResult.getData());
                    JL_Log.d(TAG, "sessionRecordMLD", "record size : " + mAdapter.getData().size());
                    updateEditMode(mAdapter.isEditMode());
                }
            }
        });
        mViewModel.deleteSessionMLD.observe(getViewLifecycleOwner(), deleteResult -> {
            if (isInvalid() || null == deleteResult) return;
            if (deleteResult.getState() == StateResult.STATE_WORKING) {
                showLoadingDialog(getString(R.string.deleting));
                return;
            }
            dismissLoadingDialog();
            if (deleteResult.getState() == StateResult.STATE_FINISH) {
                if (mAdapter.isEditMode()) {
                    mAdapter.setEditMode(false);
                }
                if (deleteResult.isSuccess()) {
                    querySessionRecords(mRecordType, mDate, true);
                    return;
                }
                showTips(CommonUtil.formatString("%s\n%s : %s, %s",
                        getString(R.string.delete_record_failed),
                        getString(R.string.error_code), CommonUtil.formatInt(deleteResult.getCode()), deleteResult.getMessage()));
            }
        });
    }

    private void updateEditMode(boolean isEditMode) {
        if (isInvalid()) return;
        JL_Log.d(TAG, "updateEditMode", "isEditMode : " + isEditMode + ", record size : " + mAdapter.getData().size());
        final TextView tvRight = mBinding.viewToolBar.tvRight;
        if (isEditMode) {
            UIHelper.show(mBinding.btnDelete);
            mBinding.viewToolBar.tvLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            mBinding.viewToolBar.tvLeft.setText(getString(R.string.cancel));
            UIHelper.show(tvRight);
            tvRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            tvRight.setText(getString(R.string.select_all));
        } else {
            UIHelper.gone(mBinding.btnDelete);
            mBinding.viewToolBar.tvLeft.setText("");
            mBinding.viewToolBar.tvLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_back_black, 0, 0, 0);
            tvRight.setText("");
            if (mAdapter.getData().isEmpty()) {
                tvRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                UIHelper.gone(tvRight);
            } else {
                UIHelper.show(tvRight);
                tvRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_edit_black, 0);
            }
        }
    }

    private void updateSelectItemCount(int selected, int itemSize) {
        if (isInvalid() || !mAdapter.isEditMode()) return;
        if (selected == itemSize) { //全选状态
            mBinding.viewToolBar.tvRight.setText(getString(R.string.unselect_all));
        } else { //没有全选状态
            mBinding.viewToolBar.tvRight.setText(getString(R.string.select_all));
        }
    }

    private void exitFragment() {
        if (null != mAdapter && mAdapter.isEditMode()) {
            mAdapter.setEditMode(false);
            return;
        }
        finish();
    }

    private RecordType getRecordType() {
        final String type = mBinding.tvRecordType.getText().toString();
        if (getString(R.string.all_type).equals(type)) {
            return new RecordType().setTranslationMode(-1)
                    .setTitle(type).setResId(R.drawable.ic_all_record_purple);
        } else if (getString(R.string.call_translation).equals(type)) {
            return new RecordType().setTranslationMode(TranslationMode.MODE_CALL_TRANSLATION)
                    .setTitle(type).setResId(R.drawable.ic_call_translation_small);
        } else if (getString(R.string.face_to_face_translation).equals(type)) {
            return new RecordType().setTranslationMode(TranslationMode.MODE_FACE_TO_FACE_TRANSLATION)
                    .setTitle(type).setResId(R.drawable.ic_face_to_face_translation_small);
        } else if (getString(R.string.simultaneous_interpreting).equals(type)) {
            return new RecordType().setTranslationMode(TranslationMode.MODE_RECORDING_TRANSLATION)
                    .setTitle(type).setResId(R.drawable.ic_simultaneous_interpreting_small);
        }
        return null;
    }

    private void showRecordTypeDialog() {
        if (isInvalid()) return;
        new RecordTypeDialog.Builder()
                .setSelectedItem(getRecordType())
                .setCallback(result -> querySessionRecords(result, mDate, false))
                .build().show(getChildFragmentManager(), RecordTypeDialog.class.getSimpleName());
    }

    private void showCalendarSelector() {
        if (isInvalid()) return;
        int mode = null == mRecordType ? -1 : mRecordType.getTranslationMode();
        new CalendarSelector.Builder(mViewModel, mode)
                .setDate(mDate)
                .setCallback(result -> {
                    mDate = result.getDate();
                    mBinding.tvRecordTime.setText(TranslateUtil.formatYearDateString(mDate.getTime()));
                    mAdapter.setList(result.getRecords());
                    JL_Log.d(TAG, "showCalendarSelector", "record size : " + mAdapter.getData().size());
                    updateEditMode(mAdapter.isEditMode());
                })
                .build().show(getChildFragmentManager(), CalendarSelector.class.getSimpleName());
    }

    private void showConfirmationDialog(List<Integer> sessionIds) {
        if (isInvalid() || null == sessionIds || sessionIds.isEmpty()) return;
        new ConfirmationDialog.Builder(new ConfirmationDialog.OnClickEventListener() {
            @Override
            public void onConfirm(ConfirmationDialog dialog) {
                dialog.dismiss();
                mViewModel.deleteSessionRecord(sessionIds);
            }

            @Override
            public void onCancel(ConfirmationDialog dialog) {
                dialog.dismiss();
            }
        }).build().show(getChildFragmentManager(), ConfirmationDialog.class.getSimpleName());
    }

    private void querySessionRecords(@NonNull RecordType type, @NonNull Date date, boolean isForce) {
        if (isInvalid()) return;
        if (!isForce && type.equals(mRecordType) && TranslateUtil.isSameDay(date, mDate)) return;
        mRecordType = type;
        mDate = date;
        mBinding.tvRecordType.setText(type.getTitle());
        mBinding.tvRecordTime.setText(TranslateUtil.formatYearDateString(date.getTime()));
        mViewModel.querySessionRecordsByDay(type.getTranslationMode(), date);
    }
}