package com.jieli.btsmart.ui.alarm;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.command.AlarmExpandCmd;
import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.base.BaseDialogFragment;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 4/1/21
 * @desc :
 */
public class DialogBellIntervalChose extends BaseDialogFragment {

    private OnSelectChange onSelectChange;

    private int[] timeData = new int[]{5, 10, 15, 20, 25, 30};
    private int[] countData = new int[]{1, 3, 5, 10};


    private AlarmExpandCmd.BellArg bellArg;

    public DialogBellIntervalChose(AlarmExpandCmd.BellArg bellArg, OnSelectChange onSelectChange) {
        this.onSelectChange = onSelectChange;
        this.bellArg = bellArg;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_alarm_bell_interval_chose, container, false);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getView() == null) return;
        NumSeekBar sbInterval = requireView().findViewById(R.id.num_sb_interval_time);
        NumSeekBar sbCount = requireView().findViewById(R.id.num_sb_interval_count);

        sbInterval.setDataAndValue(timeData, bellArg.getInterval());
        sbCount.setDataAndValue(countData, bellArg.getCount());

        sbInterval.setVisibility(bellArg.isCanSetInterval() ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.tv_bell_interval_time_chose_title).setVisibility(bellArg.isCanSetInterval() ? View.VISIBLE : View.GONE);

        sbCount.setVisibility(bellArg.isCanSetCount() ? View.VISIBLE : View.GONE);
        requireView().findViewById(R.id.tv_bell_interval_count_chose_title).setVisibility(bellArg.isCanSetCount() ? View.VISIBLE : View.GONE);


        requireView().findViewById(R.id.tv_cancel).setOnClickListener(v -> dismiss());
        requireView().findViewById(R.id.tv_confirm).setOnClickListener(v -> {
            dismiss();
            if (onSelectChange != null) {
                onSelectChange.onSelect(sbCount.getValue(), sbInterval.getValue());
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        if (window == null) return;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.dimAmount = 0.5f;
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.width = (int) (0.9f * getScreenWidth());
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        window.getDecorView().getRootView().setBackgroundColor(Color.TRANSPARENT);

    }

    public interface OnSelectChange {
        void onSelect(int count, int interval);
    }
}
