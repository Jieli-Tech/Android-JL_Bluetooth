package com.jieli.btsmart.ui.eq;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.DialogEqModeBinding;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.btsmart.ui.widget.dialog.CommonDialog;
import com.jieli.btsmart.util.EqCacheUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EqModeDialog
 * @author zqjasonZhong
 * @since 2025/5/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc EQ模式对话框
 */
public class EqModeDialog extends CommonDialog {

    private DialogEqModeBinding mBinding;
    private EqModeAdapter mAdapter;

    private EqInfo mEqInfo;

    private EqModeDialog(Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogEqModeBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEqInfo = EqCacheUtil.getCurrentCacheEqInfo();
        initUI();
    }

    private void initUI() {
        mAdapter = new EqModeAdapter(getData());
        mAdapter.setOnItemClickListener((adapter, view1, position) -> {
            mAdapter.select(position);
            dismiss();
            if (mBuilder instanceof Builder) {
                Builder builder = (Builder) mBuilder;
                final OnSelectedChange callback = builder.getCallback();
                if (callback != null) {
                    EqInfo eqInfo = mAdapter.getItem(position).copy();
                    if (eqInfo.getMode() == EqInfo.MODE_CUSTOM) {
                        //如果增益是0x7f时认为时切换到自定义模式
                        byte[] value = new byte[eqInfo.getCount()];
                        Arrays.fill(value, (byte) 0x7f);
                        eqInfo.setValue(value);
                    }
                    callback.onChange(eqInfo);
                }
            }
        });
        mAdapter.select(mEqInfo.getMode());
        mBinding.rvEqMode.setLayoutManager(new LinearLayoutManager(getContext()));
        mBinding.rvEqMode.addItemDecoration(new CommonDecoration(requireContext(), RecyclerView.VERTICAL,
                requireContext().getResources().getColor(R.color.gray_eeeeee), 1));
        mBinding.rvEqMode.setAdapter(mAdapter);


        mBinding.tvEqModeCancel.setOnClickListener(v -> dismiss());
    }

    private List<EqInfo> getData() {
        EqPresetInfo eqPresetInfo = EqCacheUtil.getPresetEqInfo();
        if (null == eqPresetInfo) return new ArrayList<>();
        return eqPresetInfo.getEqInfos();
    }

    public interface OnSelectedChange {
        void onChange(EqInfo eqInfo);
    }

    public static class Builder extends CommonDialog.Builder {
        private final OnSelectedChange callback;

        public Builder(OnSelectedChange callback) {
            this.callback = callback;
            setCancelable(false)
                    .setWidthRate(-1.0f)
                    .setGravity(Gravity.BOTTOM);
        }

        public OnSelectedChange getCallback() {
            return callback;
        }

        @Override
        public EqModeDialog build() {
            return new EqModeDialog(this);
        }
    }
}