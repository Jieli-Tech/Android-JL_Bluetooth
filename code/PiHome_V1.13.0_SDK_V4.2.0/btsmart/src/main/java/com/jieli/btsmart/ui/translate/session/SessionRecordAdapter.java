package com.jieli.btsmart.ui.translate.session;

import android.annotation.SuppressLint;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.data.model.translation.TranslationSession;
import com.jieli.btsmart.data.model.translation.TranslationSessionRecord;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ValueUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * SessionRecordAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 会议记录适配器
 * @since 2025/8/29
 */
public class SessionRecordAdapter extends BaseQuickAdapter<TranslationSessionRecord, BaseViewHolder> {

    private final static SimpleDateFormat YEAR_DATA_FORMAT = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);
    private final static SimpleDateFormat DAY_DATA_FORMAT = new SimpleDateFormat("HH:mm", Locale.ENGLISH);

    private final OnStateListener mListener;
    private boolean isEditMode;
    private final List<Integer> selectedItemList = new ArrayList<>();

    public SessionRecordAdapter(OnStateListener listener) {
        super(R.layout.item_session_record);
        this.mListener = listener;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, TranslationSessionRecord sessionRecord) {
        long startTime = sessionRecord.getSession().getStartTime();
        viewHolder.setImageResource(R.id.iv_translation_mode, getTranslationModeRes(sessionRecord.getSession()));
        viewHolder.setText(R.id.tv_time_year, formatYearTime(startTime));
        viewHolder.setText(R.id.tv_time_day, formatDayTime(startTime));
        TranslationRecord record = getFirstRecord(sessionRecord);
        if (null == record) return;
        viewHolder.setText(R.id.tv_src_text, record.getSrcText());
        viewHolder.setText(R.id.tv_desc_text, record.getDestText());
        ImageView ivSelectState = viewHolder.getView(R.id.iv_select_state);
        ConstraintLayout clContent = viewHolder.getView(R.id.cl_content);
        int marginTop = ValueUtil.dp2px(getContext(), 8);
        if (isEditMode) {
            UIHelper.show(ivSelectState);
            boolean isSelectedItem = isSelectedItem(sessionRecord);
            ivSelectState.setImageResource(isSelectedItem ? R.drawable.ic_select_checked : R.drawable.ic_select_gray);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) clContent.getLayoutParams();
            lp.setMargins(ValueUtil.dp2px(getContext(), 4), marginTop, 0, 0);
            clContent.setLayoutParams(lp);
            ivSelectState.post(() -> { //解决首次获取不到真实宽度
                clContent.setTranslationX(ivSelectState.getWidth());
            });
        } else {
            UIHelper.gone(ivSelectState);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) clContent.getLayoutParams();
            int margin = ValueUtil.dp2px(getContext(), 16);
            lp.setMargins(margin, marginTop, margin, 0);
            clContent.setLayoutParams(lp);
            clContent.setTranslationX(0);
        }
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public boolean isSelectedItem(TranslationSessionRecord sessionRecord) {
        if (null == sessionRecord || selectedItemList.isEmpty()) return false;
        for (int sessionId : selectedItemList) {
            if (sessionId == sessionRecord.getSession().getId()) {
                return true;
            }
        }
        return false;
    }

    public List<Integer> getSelectedItemList() {
        return new ArrayList<>(selectedItemList);
    }

    public void setEditMode(boolean enable) {
        if (isEditMode == enable) return;
        isEditMode = enable;
        if (null != mListener) {
            mListener.onEditMode(enable);
        }
        clearSelectedItemList();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedItem(TranslationSessionRecord sessionRecord) {
        if (null == sessionRecord || !isEditMode) return;
        final int sessionId = sessionRecord.getSession().getId();
        if (isSelectedItem(sessionRecord)) {
            int position = selectedItemList.indexOf(sessionId);
            if (position == -1) return;
            selectedItemList.remove(position);
        } else {
            selectedItemList.add(sessionId);
        }
        notifyDataSetChanged();
        if (null != mListener) {
            mListener.onSelectItem(selectedItemList.size(), getItemSize());
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearSelectedItemList() {
        selectedItemList.clear();
        notifyDataSetChanged();
        if (isEditMode) {
            if (null != mListener) {
                mListener.onSelectItem(0, getItemSize());
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void allSelectItemList() {
        if (!isEditMode) return;
        selectedItemList.clear();
        for (TranslationSessionRecord record : getData()) {
            selectedItemList.add(record.getSession().getId());
        }
        notifyDataSetChanged();
        if (null != mListener) {
            mListener.onSelectItem(selectedItemList.size(), getItemSize());
        }
    }

    private int getItemSize() {
        return getData().size();
    }

    private int getTranslationModeRes(@NonNull TranslationSession session) {
        switch (session.getTranslationMode()) {
            case TranslationMode.MODE_RECORDING_TRANSLATION:
                return R.drawable.ic_simultaneous_interpreting_green;
            case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION:
                return R.drawable.ic_face_to_face_translation_orange;
            case TranslationMode.MODE_CALL_TRANSLATION:
                return R.drawable.ic_call_translation_blue;
            default:
                return 0;
        }
    }

    private String formatYearTime(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return YEAR_DATA_FORMAT.format(calendar.getTime());
    }

    private String formatDayTime(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        return DAY_DATA_FORMAT.format(calendar.getTime());
    }

    private TranslationRecord getFirstRecord(TranslationSessionRecord sessionRecord) {
        if (null == sessionRecord) return null;
        final List<TranslationRecord> list = sessionRecord.getRecords();
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    public interface OnStateListener {
        /**
         * 回调编辑模式改变
         *
         * @param isEditMode boolean 是否编辑模式
         */
        void onEditMode(boolean isEditMode);

        /**
         * 回调选择项的变化
         *
         * @param selected int 选中项数量
         * @param itemSize int 数据项数量
         */
        void onSelectItem(int selected, int itemSize);
    }
}
