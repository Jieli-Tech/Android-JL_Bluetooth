package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.light.Scene;
import com.jieli.btsmart.ui.widget.color_cardview.CardView;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/29 11:49 AM
 * @desc : 灯光情景模式适配器
 */
public class LightSceneAdapter extends BaseQuickAdapter<Scene, BaseViewHolder> {
    private int mSelectedTag = -1;

    public LightSceneAdapter() {
        super(R.layout.item_scene_list);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelectedTag(int selectedTag) {
        this.mSelectedTag = selectedTag;
        notifyDataSetChanged();
    }

    public int getSelectedTag() {
        return mSelectedTag;
    }

    @Override
    protected void convert(BaseViewHolder holder, Scene scene) {
        holder.setImageResource(R.id.iv_item_function_icon, scene.getResId());
//        if (getItemPosition(scene) > 0) {
//            holder.setVisible(R.id.iv_light_scene_shadow, false);
//        }
        CardView cardView = holder.getView(R.id.cv_function_list);
        holder.setText(R.id.tv_item_function_name, scene.getName());
        int position = getItemPosition(scene);
        holder.getView(R.id.cl_root).setTag(position);
        cardView.setSelected(position == mSelectedTag);
    }
}
