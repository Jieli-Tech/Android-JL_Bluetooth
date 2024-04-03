package com.jieli.btsmart.util;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.jieli.btsmart.R;
import com.jieli.component.utils.ValueUtil;

import java.util.List;

import static com.jieli.btsmart.util.AppUtil.getContext;

public class BindingUtil {
    @BindingAdapter("android:selected")
    public static void setViewSelected(View view, boolean selectedState) {
        view.setSelected(selectedState);
    }

    /*重写系统的android:src*/
    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, int resId) {
        view.setImageResource(resId);
    }

    @BindingAdapter("android:src")
    public static void setSrc(ImageView view, Bitmap bitmap) {
        view.setImageBitmap(bitmap);
    }

    @BindingAdapter("android:loadSrc")
    public static void setSrc(ImageView view, String url) {
        Glide.with(view.getContext()).load(url).placeholder(R.drawable.ic_radio_placeholder).error(R.drawable.ic_radio_placeholder).into(view);
    }

    @BindingAdapter("android:loadGifSrc")
    public static void setLoadGifSrc(ImageView view, int resourceId) {
        Glide.with(view.getContext()).asGif().load(resourceId).into(view);
    }

    @BindingAdapter("android:loadFilletSrc")
    public static void setFilletSrc(ImageView view, String url) {
        String tag = (String) view.getTag();
        if (!url.equals(tag)) {
            view.setTag(url);
            //设置图片
            Glide.with(view.getContext()).load(url).transform(new CenterInside(), new GlideRoundTransform(7)).placeholder(R.drawable.ic_radio_placeholder).error(R.drawable.ic_radio_placeholder).into(view);
        }
    }

    /**
     * @param view des的RecyclerView
     * @param list 数据List
     * @describe 设置recyclerview的data数据
     */
    @BindingAdapter("android:setRecycleViewData")
    public static void setRecycleViewData(View view, List list) {
        RecyclerView recyclerView = (RecyclerView) view;
        BaseQuickAdapter adapter = (BaseQuickAdapter) recyclerView.getAdapter();
        adapter.setNewInstance(list);
    }

    @BindingAdapter("android:customHeight")
    public static void setHeight(View view, int value) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        int heightValue = value;
        if (value > 0) {
            heightValue = ValueUtil.dp2px(getContext(), value);
        }
        layoutParams.height = heightValue;
    }
}
