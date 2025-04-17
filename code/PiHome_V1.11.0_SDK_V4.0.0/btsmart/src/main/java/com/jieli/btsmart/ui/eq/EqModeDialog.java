package com.jieli.btsmart.ui.eq;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.bluetooth.bean.device.eq.EqPresetInfo;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.EqModeAdapter;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.btsmart.util.EqCacheUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class EqModeDialog extends DialogFragment {

    private EqInfo mEqInfo;
    private OnSelectedChange mOnSelectedChange;


    public static EqModeDialog newInstance(OnSelectedChange onSelectedChange) {
        EqModeDialog fragment = new EqModeDialog();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.mOnSelectedChange = onSelectedChange;
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (null != window) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(null);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Window window = requireDialog().getWindow();
        if (null != window) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.horizontalMargin = 0;
            window.setAttributes(lp);
        }

        View view = inflater.inflate(R.layout.dialog_eq_mode_dialog, container, false);
        EqModeAdapter eqModeAdapter = new EqModeAdapter(getData());
        mEqInfo = EqCacheUtil.getCurrentCacheEqInfo();
        eqModeAdapter.select(mEqInfo.getMode());
        RecyclerView recyclerView = view.findViewById(R.id.rv_eq_mode);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(eqModeAdapter);

        recyclerView.addItemDecoration(new CommonDecoration(requireContext(), RecyclerView.VERTICAL,
                requireContext().getResources().getColor(R.color.gray_eeeeee), 1));

        eqModeAdapter.setOnItemClickListener((adapter, view1, position) -> {
            eqModeAdapter.select(position);
            dismiss();
            if (getActivity() != null) {
                if (mOnSelectedChange != null) {
                    EqInfo eqInfo = eqModeAdapter.getItem(position).copy();
                    if (eqInfo.getMode() == EqInfo.MODE_CUSTOM) {
                        //如果增益是0x7f时认为时切换到自定义模式
                        byte[] value = new byte[eqInfo.getCount()];
                        Arrays.fill(value, (byte) 0x7f);
                        eqInfo.setValue(value);
                    }
                    mOnSelectedChange.onChange(eqInfo);
                }
            }
        });
        view.findViewById(R.id.tv_eq_mode_cancel).setOnClickListener(v -> dismiss());
        return view;
    }


    private List<EqInfo> getData() {
        EqPresetInfo eqPresetInfo = EqCacheUtil.getPresetEqInfo();
        if (null == eqPresetInfo) return new ArrayList<>();
        return eqPresetInfo.getEqInfos();
    }


    public interface OnSelectedChange {
        void onChange(EqInfo eqInfo);
    }
}