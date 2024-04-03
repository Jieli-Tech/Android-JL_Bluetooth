package com.jieli.btsmart.ui.alarm;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.base.BaseDialogFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 4/1/21
 * @desc :
 */
public class DialogBellTimeChose extends BaseDialogFragment {

    private int currentTime = 5;
    private final OnSelectChange onSelectChange;

    public DialogBellTimeChose(OnSelectChange onSelectChange) {
        this.onSelectChange = onSelectChange;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_alarm_bell_time_chose, container, false);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
        if (getView() == null) return;
        RecyclerView recyclerView = requireView().findViewById(R.id.rv_bell_time_chose);
        if (recyclerView == null || recyclerView.getAdapter() == null) return;
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public int getCurrentTime() {
        return currentTime;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        RecyclerView recyclerView = requireView().findViewById(R.id.rv_bell_time_chose);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<Integer> data = new ArrayList<>();
        data.add(1);
        data.add(5);
        data.add(10);
        data.add(15);
        data.add(20);
        data.add(25);
        data.add(30);
        Adapter adapter = new Adapter(data);
        recyclerView.setAdapter(adapter);
        requireView().findViewById(R.id.tv_cancel).setOnClickListener(v -> dismiss());
        requireView().findViewById(R.id.tv_confirm).setOnClickListener(v -> {
            if (onSelectChange != null) {
                onSelectChange.onSelect(currentTime);
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireDialog().getWindow();
        if (window == null) return;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.dimAmount = 0.5f;
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.width = (int) (0.9f * getScreenWidth());
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.getDecorView().getRootView().setBackgroundColor(Color.TRANSPARENT);

    }

    private class Adapter extends BaseQuickAdapter<Integer, BaseViewHolder> {

        public Adapter(List<Integer> data) {
            super(R.layout.item_alarm_bell_time, data);
        }

        @Override
        protected void convert(@NotNull BaseViewHolder baseViewHolder, Integer item) {
            baseViewHolder.setText(R.id.tv_alarm_bell_time, getString(R.string.min_format, item));
            baseViewHolder.getView(R.id.tv_alarm_bell_time).setSelected(currentTime == item);
            baseViewHolder.getView(R.id.tv_alarm_bell_time).setOnClickListener(v -> setCurrentTime(item));
        }

    }

    public interface OnSelectChange {
        void onSelect(int time);
    }
}
