package com.jieli.btsmart.ui.soundcard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.soundcard.SoundCard;
import com.jieli.component.utils.ValueUtil;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/20 4:06 PM
 * @desc :
 */
@SuppressLint("ViewConstructor")
class TitleView extends LinearLayout {

    public TitleView(SoundCard.Functions functions, Context context) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
        ImageView imageView = new ImageView(getContext());
        imageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//        imageView.setImageResource(R.drawable.ic_effect_nol);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(imageView);
        String url = functions.icon_url;
        //如果是本地文件的时候, 需要在url前面加上文件夹路径
        if (!functions.icon_url.startsWith("http")) {
            url = "file:///android_asset/sound_card/icon/" + functions.icon_url;
        }
        Glide.with(context)
                .load(url)
                .fitCenter()
                .override(SIZE_ORIGINAL)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.bg_sound_card_dafault_title_icon)
                .into(imageView);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(functions.title.getShowText());
        tvTitle.setGravity(Gravity.CENTER_VERTICAL);
        tvTitle.setTextColor(getResources().getColor(R.color.black_242424));
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(tvTitle.getTypeface(), Typeface.BOLD);
        tvTitle.setPadding(ValueUtil.dp2px(context, 7), 0, 0, 0);
        addView(tvTitle);
//        setPadding(ValueUtil.dp2px(context, 0), ValueUtil.dp2px(context, 28), ValueUtil.dp2px(context, 0), ValueUtil.dp2px(context, 15));
    }
}
