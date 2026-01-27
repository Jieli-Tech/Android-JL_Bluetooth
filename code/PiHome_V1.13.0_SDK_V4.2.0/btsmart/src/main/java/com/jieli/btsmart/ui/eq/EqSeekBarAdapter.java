package com.jieli.btsmart.ui.eq;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.eq.EqSeekBarBean;
import com.jieli.btsmart.ui.widget.VerticalSeekBarView;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Des:
 * Author: Bob
 * Date:20-5-15
 * UpdateRemark:
 */
public final class EqSeekBarAdapter extends BaseQuickAdapter<EqSeekBarBean, BaseViewHolder> {
    private int selectMode = -1;
    private ValueChange mValueChange;
    private boolean mHasHoverView = false;
    private EqInfo mEqInfo = new EqInfo();

    private boolean ban;


    @SuppressLint("NotifyDataSetChanged")
    public void setBan(boolean ban) {
        this.ban = ban;
        notifyDataSetChanged();
    }

    public EqSeekBarAdapter(List<EqSeekBarBean> list, ValueChange valueChange) {
        super(R.layout.item_eq_seekbar, list);
        this.mValueChange = valueChange;
    }


    @Override
    protected void convert(@NonNull BaseViewHolder helper, EqSeekBarBean item) {
        if (!getData().isEmpty()) {
            int itemWidth = 0;
            ViewGroup relativeLayout = helper.getView(R.id.cl_main);
            ViewGroup.LayoutParams params = relativeLayout.getLayoutParams();
            //小于7段
            if (getData().size() < 7) {
                if (getRecyclerView().getWidth() != 0) {
                    itemWidth = getRecyclerView().getWidth() / getData().size();
                } else {
                    itemWidth = (AppUtil.getScreenWidth(getContext()) - ValueUtil.dp2px(getContext(), 60)) / getData().size();
                }
            } else {
                //超过6段
                params.width = ValueUtil.dp2px(getContext(), 50);
            }
            //如果计算到的宽度是0，则忽略
            if (itemWidth != 0) {
                params.width = itemWidth;
                relativeLayout.setLayoutParams(params);
            }
        }
        VerticalSeekBarView verticalSeekBarView = helper.getView(R.id.vsb_eq);
        verticalSeekBarView.setText(item.getFreq());
        verticalSeekBarView.setValue(item.getValue());
        verticalSeekBarView.setIndex(item.getIndex());
        verticalSeekBarView.setEnable(!ban);
        this.mEqInfo.getValue()[item.getIndex()] = (byte) item.getValue();
        verticalSeekBarView.setValueListener((value, end) -> {
            if (getItemPosition(item) < 0) {
                return;
            }
            item.setValue(value);
            this.mEqInfo.getValue()[item.getIndex()] = (byte) item.getValue();
            mValueChange.onChange(item.getIndex(), this.mEqInfo, end);
        });
        verticalSeekBarView.setHoverListener(hover -> mHasHoverView = hover);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void select(int selectMode) {
        this.selectMode = selectMode;
        notifyDataSetChanged();
    }

    public boolean hasHoverView() {
        return mHasHoverView;
    }

    public void updateSeekBar(EqInfo eqInfo) {
        this.mEqInfo = eqInfo;
        List<EqSeekBarBean> barBeans = new ArrayList<>();
        for (int i = 0; i < eqInfo.getValue().length && i < eqInfo.getFreqs().length; i++) {
            EqSeekBarBean eqSeekBarBean = new EqSeekBarBean(i, AppUtil.freqValueToFreqShowText(eqInfo.getFreqs()[i]), eqInfo.getValue()[i]);
            barBeans.add(eqSeekBarBean);
        }
        setNewInstance(barBeans);
    }


    @SuppressLint("NotifyDataSetChanged")
    public void updateSeekBar(int[] value) {
        //有拖动时不更新
        if (mHasHoverView) {
            return;
        }
        List<EqSeekBarBean> list = getData();
        //先检测是否有数据变化，有变化才更新。没有就不更新，防止recycleView view缓存导致的抖动问题
        int changeCount = 0;
        int changeIndex = -1;
        for (int i = 0; i < list.size() && i < value.length; i++) {
            if (value[i] != list.get(i).getValue()) {
                changeIndex = i;
                changeCount++;
            }
            list.get(i).setValue(value[i]);
        }
        if (changeCount > 1) {
            notifyDataSetChanged();
        } else if (changeIndex != -1) {
            notifyItemChanged(changeIndex);
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    public void reset() {
        List<EqSeekBarBean> list = getData();
        //先检测是否有数据变化，有变化才更新。没有就不更新，防止recycleView view缓存导致的抖动问题
        for (EqSeekBarBean barBean : list) {
            barBean.setValue(0);
        }
        notifyDataSetChanged();
    }

    public int[] getValues() {
        int[] values = new int[getData().size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = getData().get(i).getValue();
        }
        return values;
    }


    public interface ValueChange {
        void onChange(int index, EqInfo eqInfo, boolean end);
    }


    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }
}
