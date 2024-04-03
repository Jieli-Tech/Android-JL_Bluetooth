package com.jieli.btsmart.ui.light;

import android.content.res.TypedArray;
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

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.LightModeAdapter;
import com.jieli.btsmart.data.model.light.LightMode;
import com.jieli.btsmart.util.AppUtil;

import java.util.ArrayList;
import java.util.List;


public class LightModeDialog extends DialogFragment {


    private OnSelectedChange mOnSelectedChange;

    public static LightModeDialog newInstance(OnSelectedChange onSelectedChange) {
        LightModeDialog fragment = new LightModeDialog();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        fragment.mOnSelectedChange = onSelectedChange;
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if(null == window) return;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Window window = requireDialog().getWindow();
        if(null != window){
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.horizontalMargin = 0;
            window.setAttributes(lp);
        }
        View view = inflater.inflate(R.layout.dialog_light_mode_dialog, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.rv_light_mode);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        //适配器数据初始化
        String[] modes = AppUtil.getContext().getResources().getStringArray(R.array.light_mode_name);
        TypedArray modeRes = getResources().obtainTypedArray(R.array.light_mode_res);
        List<LightMode> list = new ArrayList<>();
        for (int i = 0; i < modeRes.length() && i < modes.length; i++) {
            LightMode lightMode = new LightMode();
            lightMode.setName(modes[i]);
            lightMode.setRes(modeRes.getResourceId(i, R.drawable.icon_colorful));
            list.add(lightMode);
        }
        modeRes.recycle();
        LightModeAdapter lightModeAdapter = new LightModeAdapter();
        lightModeAdapter.setNewInstance(list);

        recyclerView.setAdapter(lightModeAdapter);
//        recyclerView.addItemDecoration(new CommonDecoration(getContext(),RecyclerView.HORIZONTAL,getContext().getResources().getColor(R.color.gray_eeeeee),1));
        lightModeAdapter.setOnItemClickListener((adapter, view1, position) -> {
            if (mOnSelectedChange != null) {
                mOnSelectedChange.onChange(lightModeAdapter.getItem(position));
            }
            dismiss();
        });

        view.findViewById(R.id.tv_light_mode_cancel).setOnClickListener(v -> dismiss());

        return view;
    }


    public interface OnSelectedChange {
        void onChange(LightMode lightMode);
    }
}