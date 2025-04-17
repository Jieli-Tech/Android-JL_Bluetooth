package com.jieli.btsmart.data.adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.light.ColorCollect;
import com.jieli.btsmart.ui.light.GlideCircleWithBorder;
import com.jieli.btsmart.util.RGB2HSLUtil;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/29 11:49 AM
 * @desc : 灯光情景模式适配器
 */
public class LightColorCollectAdapter extends BaseQuickAdapter<ColorCollect, BaseViewHolder> {
    public LightColorCollectAdapter() {
        super(R.layout.item_light_color_collect);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, ColorCollect colorCollect) {
        if (getItemPosition(colorCollect) == 11) {
            holder.setImageResource(R.id.iv_color, R.drawable.ic_light_add_collect);
            return;
        }
        Integer color = colorCollect.getColor();
        View root = holder.getView(R.id.cl_root);
        if (null == color) {
            color = 0xFFE1E1E1;
            root.setTag(null);
        } else {
            root.setTag(color);
        }
        ColorDrawable colorDrawable = new ColorDrawable(color);
        if (RGB2HSLUtil.checkIsTendToWhite(color, 90)) {
            Glide.with(getContext())
                    .load(colorDrawable)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .transform(new GlideCircleWithBorder(getContext(), 0.5f, Color.parseColor("#D0D0D0")))
                    .into((ImageView) holder.getView(R.id.iv_color));
        } else {
            Glide.with(getContext())
                    .load(colorDrawable)
                    .circleCrop()
                    .into((ImageView) holder.getView(R.id.iv_color));
        }
    }
}
