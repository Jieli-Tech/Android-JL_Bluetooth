package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.translation.RecordType;
import com.jieli.btsmart.databinding.DialogRecordTypeBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * RecordTypeDialog
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 记录类型弹窗
 * @since 2025/9/1
 */
public class RecordTypeDialog extends CommonDialog {

    private DialogRecordTypeBinding mBinding;
    private RecordTypeAdapter mAdapter;

    protected RecordTypeDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogRecordTypeBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        mBinding.btnClose.setOnClickListener(v -> dismiss());
        mAdapter = new RecordTypeAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            final RecordType type = mAdapter.getItem(position);
            if (mAdapter.isSelectedItem(type)) return;
            mAdapter.updateSelectedItem(type);
            dismiss();
            final OnResultCallback<RecordType> callback = builder.callback;
            if (null != callback) {
                callback.onResult(type);
            }
        });
        mBinding.rvRecordType.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvRecordType.setAdapter(mAdapter);

        List<RecordType> list = new ArrayList<>();
        list.add(new RecordType().setTranslationMode(-1).setTitle(getString(R.string.all_type))
                .setResId(R.drawable.ic_all_record_purple));
        list.add(new RecordType().setTranslationMode(TranslationMode.MODE_CALL_TRANSLATION)
                .setTitle(getString(R.string.call_translation))
                .setResId(R.drawable.ic_call_translation_small));
        list.add(new RecordType().setTranslationMode(TranslationMode.MODE_FACE_TO_FACE_TRANSLATION)
                .setTitle(getString(R.string.face_to_face_translation))
                .setResId(R.drawable.ic_face_to_face_translation_small));
        list.add(new RecordType().setTranslationMode(TranslationMode.MODE_RECORDING_TRANSLATION)
                .setTitle(getString(R.string.simultaneous_interpreting))
                .setResId(R.drawable.ic_simultaneous_interpreting_small));
        mAdapter.setList(list);

        RecordType selectedType = builder.selectedType;
        mAdapter.updateSelectedItem(selectedType);
    }

    public static class Builder extends CommonDialog.Builder {
        private RecordType selectedType;
        private OnResultCallback<RecordType> callback;

        public Builder() {
            setGravity(Gravity.BOTTOM).setWidthRate(1.0f);
        }

        public Builder setSelectedItem(RecordType recordType) {
            this.selectedType = recordType;
            return this;
        }

        public Builder setCallback(OnResultCallback<RecordType> callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public RecordTypeDialog build() {
            return new RecordTypeDialog(this);
        }
    }
}
