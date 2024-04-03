package com.jieli.btsmart.ui.soundcard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.CHexConver;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.soundcard.SoundCard;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.component.utils.ValueUtil;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/19 1:50 PM
 * @desc : 功能ui容器
 */
class FunctionContainer extends RelativeLayout {
    private SoundCard.Functions functions = new SoundCard.Functions();
    private final String tag = getClass().getSimpleName();
    private final RCSPController mRCSPController = RCSPController.getInstance();

    public FunctionContainer(Context context) {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }


    public void setFunctions(SoundCard.Functions functions) {
        this.functions = functions;
        removeAllViews();
        for (SoundCard.Functions.ListBean listBean : functions.list) {
            if (functions.type.equals("select")) {
                addTextSelectView(listBean);
            } else if (functions.type.equals("img_select")) {
                addImageTextSelectView(listBean);
            } else {
                functions.column = 1;
                addSliderView(listBean);
            }
        }
    }


    @SuppressLint("UseCompatLoadingForColorStateLists")
    private void addTextSelectView(SoundCard.Functions.ListBean listBean) {
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setPadding(ValueUtil.dp2px(getContext(), 5), 0, 0, 0);
        tvTitle.setTextSize(15);
        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            tvTitle.setTextColor(getResources().getColorStateList(R.color.color_white_black242424_selector, getResources().newTheme()));
        } else {
            tvTitle.setTextColor(getResources().getColorStateList(R.color.color_white_black242424_selector));
        }
        tvTitle.setText(listBean.title.getShowText());
        tvTitle.setGravity(Gravity.CENTER);

        //显示组选项的展开表示
        ImageView iv = new ImageView(getContext());
        iv.setImageResource(R.drawable.ic_eq_icon_up);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);

        RelativeLayout.LayoutParams tvLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        tvTitle.setId(View.generateViewId());
        tvLp.addRule(RelativeLayout.CENTER_IN_PARENT);
        tvTitle.setLayoutParams(tvLp);
        tvTitle.setMinHeight(ValueUtil.dp2px(getContext(), 38));

        RelativeLayout parent = new RelativeLayout(getContext());
        parent.setLayoutParams(lp);
        parent.setClickable(true);
        parent.addView(tvTitle);
        parent.setBackgroundResource(R.drawable.bg_btn_purple_gray_border_selector);
        if (listBean.group) {
            RelativeLayout.LayoutParams ivLp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            ivLp.addRule(RelativeLayout.END_OF, tvTitle.getId());
            ivLp.addRule(RelativeLayout.CENTER_VERTICAL);
            ivLp.setMarginStart(ValueUtil.dp2px(getContext(), 5));
            iv.setLayoutParams(ivLp);
            parent.addView(iv);
            parent.setOnClickListener(new ActionGroupClickHandler(listBean));
            listBean.index = generateViewId() + 64;//加上64避免和index冲突

        } else {
            parent.setOnClickListener(new ActionClickHandler(listBean));
        }

        tvTitle.setTag(listBean.index);
        parent.setTag(listBean.index);
        addView(parent);
    }


    @SuppressLint("UseCompatLoadingForColorStateLists")
    private void addImageTextSelectView(SoundCard.Functions.ListBean listBean) {
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        RelativeLayout parent = new RelativeLayout(getContext());
        parent.setLayoutParams(lp);
        parent.setBackgroundResource(R.drawable.bg_btn_sound_card_atmosphere_selector);
        parent.setOnClickListener(new ActionClickHandler(listBean));
        parent.setClickable(true);

        ImageView iv = new ImageView(getContext());
        LayoutParams ivLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        iv.setLayoutParams(ivLp);
        iv.setId(View.generateViewId());
        String url = listBean.icon_url;
        //如果是本地文件的时候, 需要在url前面加上文件夹路径
        if (!listBean.icon_url.startsWith("http")) {
            url = "file:///android_asset/sound_card/icon/" + listBean.icon_url;
        }
        Glide.with(getContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerInside()
                .error(R.drawable.img_sound_card_default)
                .into(iv);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        parent.addView(iv);


        TextView tv = new TextView(getContext());
        LayoutParams tvLp = new LayoutParams(LayoutParams.WRAP_CONTENT, ValueUtil.dp2px(getContext(), 20));
        tvLp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        tvLp.addRule(RelativeLayout.BELOW, iv.getId());
        tv.setLayoutParams(tvLp);
        tv.setTextSize(12);

        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            tv.setTextColor(getResources().getColorStateList(R.color.color_main_color_black242424_selector, getResources().newTheme()));
        } else {
            tv.setTextColor(getResources().getColorStateList(R.color.color_main_color_black242424_selector));
        }
        tv.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        tv.setText(listBean.title.getShowText());
        tv.setTag(listBean.index);
        parent.addView(tv);
        addView(parent);
    }


    //文字的最大宽度
    private int sliderTitleMaxWidth = 0;


    //拖动调布局
    @SuppressLint("UseCompatLoadingForDrawables")
    private void addSliderView(SoundCard.Functions.ListBean listBean) {
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        if (sliderTitleMaxWidth == 0) {
            float max = 0;
            TextView tv = createSliderTitleTv(listBean.title.getShowText());
            for (SoundCard.Functions.ListBean listBean1 : functions.list) {
                max = Math.max(max, tv.getPaint().measureText(listBean1.title.getShowText()));
            }
            sliderTitleMaxWidth = (int) Math.ceil(max);

        }


        //比较low的方式 当字长度变化时就无效了通过多个view组合成一个有间距的title
        String[] texts = listBean.title.getShowText().split(ProductUtil.isChinese() ? "" : " ");
        LinearLayout titleLl = new LinearLayout(getContext());
        LinearLayout.LayoutParams titleLlLp = new LinearLayout.LayoutParams(sliderTitleMaxWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.addView(titleLl, titleLlLp);
        for (int i = 0; i < texts.length; i++) {
            TextView tv = createSliderTitleTv(listBean.title.getShowText());
            LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tvLp.gravity = Gravity.CENTER_VERTICAL;
            tvLp.weight = 1;
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            if (i == 0) {
                tv.setGravity(Gravity.START);
            } else if (i == texts.length - 1) {
                tv.setGravity(Gravity.END);
            } else {
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
            }
            tv.setText(texts[i]);
            titleLl.addView(tv, tvLp);
        }

        SeekBar seekBar = new SeekBar(getContext());
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sbLp.gravity = Gravity.CENTER_VERTICAL;
        sbLp.setMarginStart(ValueUtil.dp2px(getContext(), 18));
        seekBar.setLayoutParams(sbLp);
        if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
            seekBar.setMinHeight(ValueUtil.dp2px(getContext(), 4));
            seekBar.setMaxHeight(ValueUtil.dp2px(getContext(), 4));
        }
        seekBar.setIndeterminate(false);
        seekBar.setSplitTrack(false);
        seekBar.setProgressDrawable(getResources().getDrawable(R.drawable.bg_music_seekbar_drawable));

        seekBar.setThumb(getResources().getDrawable(R.drawable.bg_sound_card_seekbar_thumb));
        seekBar.setMax(listBean.max);
        seekBar.setEnabled(listBean.enable);
        seekBar.setTag(listBean.index);
        seekBar.setThumbOffset(0);
        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setOnSeekBarChangeListener(new ActionSliderHandler(listBean));
        linearLayout.addView(seekBar);
//        linearLayout.setTag(listBean.index);
        addView(linearLayout);

    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mRCSPController.addBTRcspEventCallback(eventCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        mRCSPController.removeBTRcspEventCallback(eventCallback);
        super.onDetachedFromWindow();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildrenWidth(MeasureSpec.getSize(widthMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    //普通按钮
    private void measureChildrenWidth(int parentWidth) {
        int column = functions.column;
        int span = ValueUtil.dp2px(getContext(), 18);
        if (column == 0) return;
        int spanWidth = (column - 1) * span;
        int childWidth = (parentWidth - spanWidth) / column;
        int lastId = -1;
        for (int i = 0; i < getChildCount(); i++) {
            View button = getChildAt(i);
            LayoutParams layoutParams = new LayoutParams(childWidth, LayoutParams.WRAP_CONTENT);
            button.setId(generateViewId());
            if (lastId == -1) {
            } else if (i % column != 0) {
                layoutParams.addRule(END_OF, lastId);
                layoutParams.addRule(ALIGN_TOP, lastId);
                layoutParams.setMarginStart(span);
            } else {
                layoutParams.addRule(BELOW, lastId);
                layoutParams.setMargins(0, span, 0, 0);
            }
            button.setLayoutParams(layoutParams);
            lastId = button.getId();
        }
    }


    private final BTRcspEventCallback eventCallback = new BTRcspEventCallback() {

        @Override
        public void onSoundCardStatusChange(BluetoothDevice device, long mask, byte[] values) {
            if (functions == null || functions.list == null) return;
            //选中控件
            for (SoundCard.Functions.ListBean bean : functions.list) {
                if (bean.group) {
                    //group的子项
                    setViewSelected(FunctionContainer.this, bean.index, isGroupChildSelected(bean, mask));
                } else {
                    boolean selected = ((mask >> bean.index) & 0x01) == 0x01;
                    setViewSelected(FunctionContainer.this, bean.index, selected);
                }
            }

            // 拖动条
            for (int i = 0; i < values.length; i += 3) {
                int value = CHexConver.bytesToInt(values, i + 1, 2);
                int index = values[i];
                View view = findViewWithTag(index);
                if (view instanceof SeekBar) {
                    SeekBar seekBar = (SeekBar) view;
                    seekBar.setProgress(value);
                }
            }
        }
    };


    private boolean isGroupChildSelected(SoundCard.Functions.ListBean listBean, long mask) {
        boolean selected = false;
        for (SoundCard.Functions.ListBean bean : listBean.list) {
            selected = ((mask >> bean.index) & 0x01) == 0x01;
            if (selected) {
                break;
            }
        }
        return selected;
    }

    private void setViewSelected(View parent, int tag, boolean selected) {
        View view = parent.findViewWithTag(tag);
        if (view == null) return;
        if (view.isSelected() == selected) return;
        view.setSelected(selected);
        //同步子view的选中状态
        if (view instanceof ViewGroup) {
            setViewSelected(view, tag, selected);
        }

    }


    private TextView createSliderTitleTv(String title) {
        TextView tv = new TextView(getContext());
        tv.setTextSize(14);
        tv.setTextColor(getResources().getColor(R.color.black_242424));
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setText(title);
        return tv;
    }
}
