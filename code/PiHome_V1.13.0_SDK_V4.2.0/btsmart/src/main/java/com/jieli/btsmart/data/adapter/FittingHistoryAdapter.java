package com.jieli.btsmart.data.adapter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.room.entity.HearingAidFittingRecordEntity;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: FittingHistoryAdapter
 * @Description: 验配记录
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/6/27 10:14
 */
public class FittingHistoryAdapter extends BaseQuickAdapter<HearingAidFittingRecordEntity, BaseViewHolder> {
    private OnFittingHistoryEventListener mOnFittingHistoryEventListener;

    public FittingHistoryAdapter() {
        super(R.layout.item_fitting_history);
    }

    public void setOnFittingHistoryEventListener(OnFittingHistoryEventListener onFittingHistoryEventListener) {
        mOnFittingHistoryEventListener = onFittingHistoryEventListener;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, HearingAidFittingRecordEntity entity) {
        String name = entity.recordName;
        if (TextUtils.isEmpty(name)) {
            name = formatTime(entity.time);
        }
        baseViewHolder.setText(R.id.tv_item_fitting_record_name, name);
        View deleteView = baseViewHolder.getView(R.id.btn_del_fitting_history);
        deleteView.setOnClickListener(v -> {
            if (mOnFittingHistoryEventListener != null) {
                mOnFittingHistoryEventListener.onFittingHistoryDelete(this, entity, getItemPosition(entity));
            }
        });
        View contentView = baseViewHolder.findView(R.id.ll_item_fitting_history);
        contentView.setOnClickListener(v -> {
            if (mOnFittingHistoryEventListener != null) {
                mOnFittingHistoryEventListener.onFittingHistoryClick(this, entity, getItemPosition(entity));
            }
        });
        int tvGainsTypeBackgroundResource;
        int tvGainsTypeTextResource;
        String colorsString;
        switch (entity.gainsType) {
            case 0:
                tvGainsTypeBackgroundResource = R.drawable.ic_fitting_record_bg_blue;
                tvGainsTypeTextResource = R.string.left_ear_single;
                colorsString = "#1677FF";
                break;
            case 1:
                tvGainsTypeBackgroundResource = R.drawable.ic_fitting_record_bg_orange;
                tvGainsTypeTextResource = R.string.right_ear_single;
                colorsString = "#FF9E39";
                break;
            default:
            case 2:
                tvGainsTypeBackgroundResource = R.drawable.ic_fitting_record_bg_purple;
                tvGainsTypeTextResource = R.string.both_ear_single;
                colorsString = "#805BEB";
                break;
        }
        baseViewHolder.setBackgroundResource(R.id.tv_item_fitting_record_gains_type, tvGainsTypeBackgroundResource);
        baseViewHolder.setText(R.id.tv_item_fitting_record_gains_type, tvGainsTypeTextResource);
        baseViewHolder.setTextColor(R.id.tv_item_fitting_record_gains_type, Color.parseColor(colorsString));
    }

    private String formatTime(long time) {
        Date date = new Date(time);
        final SimpleDateFormat mFormatWeek = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String timeString = mFormatWeek.format(date);
        return timeString;
    }

    public interface OnFittingHistoryEventListener {

        void onFittingHistoryDelete(BaseQuickAdapter adapter, HearingAidFittingRecordEntity entity, int itemPosition);

        void onFittingHistoryClick(BaseQuickAdapter adapter, HearingAidFittingRecordEntity entity, int itemPosition);
    }
}
