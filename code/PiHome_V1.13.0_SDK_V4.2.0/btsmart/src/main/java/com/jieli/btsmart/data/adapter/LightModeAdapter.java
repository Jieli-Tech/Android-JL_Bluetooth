package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.light.LightMode;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/29 11:49 AM
 * @desc : 灯光情景模式适配器
 */
public class LightModeAdapter extends BaseQuickAdapter<LightMode, BaseViewHolder> {
    public LightModeAdapter() {
        super(R.layout.item_light_mode);
    }

    private int mSelectedLightMode = -1;

    @SuppressLint("NotifyDataSetChanged")
    public void setSelected(int lightMode) {
        this.mSelectedLightMode = lightMode;
        notifyDataSetChanged();
    }

    public int getSelectedLightMode() {
        return mSelectedLightMode;
    }

    public boolean isEqualSelectedLightMode(int lightMode) {
        if (-1 == mSelectedLightMode) return false;
        return mSelectedLightMode == lightMode;
    }

    @Override
    protected void convert(BaseViewHolder holder, LightMode lightMode) {
        holder.setImageResource(R.id.iv_light_mode_left, lightMode.getRes());
        holder.setText(R.id.tv_light_mode_name, lightMode.getName());
        TextView textView = holder.getView(R.id.tv_light_mode_name);
        holder.getView(R.id.cl_root).setTag(getItemPosition(lightMode));
        textView.setSelected(this.mSelectedLightMode == getItemPosition(lightMode));
    }
}
