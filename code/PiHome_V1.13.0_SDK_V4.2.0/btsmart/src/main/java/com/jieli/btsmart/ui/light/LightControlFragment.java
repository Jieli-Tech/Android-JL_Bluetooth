package com.jieli.btsmart.ui.light;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.GsonBuilder;
import com.jieli.bluetooth.bean.device.light.LightControlInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.LightColorCollectAdapter;
import com.jieli.btsmart.data.model.light.ColorCollect;
import com.jieli.btsmart.data.model.light.ColorCollectList;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.widget.CircleBgImageView;
import com.jieli.btsmart.ui.widget.ColorPicker2View;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ColorHSB;
import com.jieli.btsmart.util.ColorHSL;
import com.jieli.btsmart.util.ColorRGB;
import com.jieli.btsmart.util.RGB2HSLUtil;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jieli.btsmart.util.AnimationUtil.parabolaAnimation;

/**
 * @author : chensenhuaÒ
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/6/29 2:10 PM
 * @desc : 灯光控制
 * 使用hue和saturation代替color进行同步，减少seekebar出现抖动。
 * 点击收藏的时候判断hue和saturation是否发生变化再进行ColorView的同步，避免colorPickView偏移。
 * 拉动luminance的Seekbar直接不同步到ColorPickView。
 * 小机帮忙保存HSL，是因为当luminance为100的时候，就无法保留hs，而且color换算hsl容易产生误差
 */
public class LightControlFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener {
    private final boolean isRealTimeRefresh = false;
    private final String KEY_COLLECT_COLORS_LIST = "key_collect_colors_list";
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private ColorPicker2View colorPicker;
    private RecyclerView rvLightColorCollect;
    private CircleBgImageView ibtnColorAdd;
    private SeekBar sbHSLColdAndWarm;
    private SeekBar sbHSLGrayAndColorful;
    private SeekBar sbHSLDarkAndSun;
    private float mValueColdAndWarm = 0;
    private float mValueGrayAndColorful = 0;
    private float mValueDarkAndLight = 0;
    private LightColorCollectAdapter mLightColorCollectAdapter;
    private View root;
    private int currentHSLColor = 0;

    private ImageView mAnimationImageView;
    private ColorCollect mCurrentAddingCollectColor;
    private List<ColorCollect> mDefaultColorList = new ArrayList(Arrays.asList(new ColorCollect(), new ColorCollect(), new ColorCollect(),
            new ColorCollect(), new ColorCollect(), new ColorCollect(),
            new ColorCollect(), new ColorCollect(), new ColorCollect(),
            new ColorCollect(), new ColorCollect(), new ColorCollect()));
    private boolean isParabolaAnimationShowing = false;

    public static LightControlFragment newInstance() {
        Bundle args = new Bundle();
        LightControlFragment fragment = new LightControlFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_light_control, container, false);

        colorPicker = root.findViewById(R.id.color_picker);
        rvLightColorCollect = root.findViewById(R.id.rv_light_color_collect);
        ibtnColorAdd = root.findViewById(R.id.ibtn_color_add);
        sbHSLColdAndWarm = root.findViewById(R.id.sb_coloured_lights_hsl_warm_and_cold);
        sbHSLGrayAndColorful = root.findViewById(R.id.sb_coloured_lights_hsl_gray_and_colorful);
        sbHSLDarkAndSun = root.findViewById(R.id.sb_coloured_lights_hsl_dark_and_sun);

        mLightColorCollectAdapter = new LightColorCollectAdapter();
        rvLightColorCollect.setAdapter(mLightColorCollectAdapter);
        rvLightColorCollect.setLayoutManager(new GridLayoutManager(getContext(), 6));
        List<ColorCollect> colorCollectList = readCollectColors();
        if (null != colorCollectList) {
            mDefaultColorList = colorCollectList;
        }
        mLightColorCollectAdapter.setNewInstance(mDefaultColorList);
        rvLightColorCollect.addItemDecoration(new CommonDecoration(getContext(), OrientationHelper.VERTICAL, Color.TRANSPARENT, ValueUtil.dp2px(getContext(), 14)));
        rvLightColorCollect.addItemDecoration(new CommonDecoration(getContext(), OrientationHelper.HORIZONTAL, Color.TRANSPARENT, ValueUtil.dp2px(getContext(), 8)));
        colorPicker.setColorPickerListener((color, end) -> {
            if (end || isRealTimeRefresh) {
                JL_Log.i("zhm", "setColorPickerListener:  color:  " + color);

                int colorHSL = handleOnlyHSColorToHSLColor(color, mValueDarkAndLight);
                syncHSLSeekBarGroup(color);
                sendColorCmdToDevice(colorHSL);
                if (!mRCSPController.isDeviceConnected()) {
                    ibtnColorAdd.setColor(color);
                    currentHSLColor = colorHSL;
                }
            }
        });
        mLightColorCollectAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (position == 11) {/**点击添加收藏的Item*/
                JL_Log.e("sen", "isParabolaAnimationShowing--->" + isParabolaAnimationShowing);
                if (isParabolaAnimationShowing) return;
                List<ColorCollect> dataList = (List<ColorCollect>) adapter.getData();
                for (ColorCollect color : dataList) {
                    if (null != color.getColor() && color.getColor() == currentHSLColor)
                        return;
                }
                isParabolaAnimationShowing = true;
                mAnimationImageView = new ImageView(getContext());
                int finalColor = handleOnlyHSColorToHSLColor(colorPicker.getCurrentColor(), mValueDarkAndLight);
                ColorDrawable colorDrawable = new ColorDrawable(finalColor);
                Glide.with(requireContext())
                        .load(colorDrawable)
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(mAnimationImageView);
                ColorCollect colorCollect = new ColorCollect(0xFFF8FAFC);
                mLightColorCollectAdapter.remove(mLightColorCollectAdapter.getData().size() - 2);
                mLightColorCollectAdapter.addData(0, colorCollect);
                mCurrentAddingCollectColor = new ColorCollect();
                mCurrentAddingCollectColor.setColor(currentHSLColor/*colorPicker.getCurrentColor()*/);
                /*mCurrentAddingCollectColor.setHue(colorPicker.getHue());//为了点击不偏移
                mCurrentAddingCollectColor.setSaturation(colorPicker.getSaturation() * 100f);*/
                mCurrentAddingCollectColor.setHue((int) mValueColdAndWarm);
                mCurrentAddingCollectColor.setSaturation((int) mValueGrayAndColorful);
                mCurrentAddingCollectColor.setLuminance((int) mValueDarkAndLight);
                parabolaAnimation((ViewGroup) root, ibtnColorAdd, rvLightColorCollect.getChildAt(0), mAnimationImageView, (animation, canceled, value, velocity) -> {
                    JL_Log.e("sen", "onAnimationEnd");
                    mLightColorCollectAdapter.setData(0, mCurrentAddingCollectColor);
                    isParabolaAnimationShowing = false;
                    mAnimationImageView = null;
                    mCurrentAddingCollectColor = null;
                });

                return;
            }
            ColorCollect currentColor = (ColorCollect) adapter.getItem(position);
            if (null == view.getTag()) return;/**是空的item*/
            int color = (int) view.getTag();
            if (!((int) mValueColdAndWarm == (int) currentColor.getHue() && (int) mValueGrayAndColorful == (int) currentColor.getSaturation())) {//优化收藏之后点击收藏列表会产生偏移
                syncColorPickerView(currentColor.getHue(), currentColor.getSaturation());
            }
            if (currentHSLColor != color) {
                syncHSLSeekBarGroup(currentColor.getHue(), currentColor.getSaturation(), currentColor.getLuminance());
                sendColorCmdToDevice(color);
            }
        });
        sbHSLColdAndWarm.setOnSeekBarChangeListener(this);
        sbHSLGrayAndColorful.setOnSeekBarChangeListener(this);
        sbHSLDarkAndSun.setOnSeekBarChangeListener(this);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
    }

    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCollectColors(mLightColorCollectAdapter.getData());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progressInt, boolean fromUser) {
       /* float progress = ((float) progressInt);
        if (isRealTimeRefresh && fromUser) {
            if (sbHSLColdAndWarm.equals(seekBar)) {
                mValueColdAndWarm = progress;
            } else if (sbHSLGrayAndColorful.equals(seekBar)) {
                mValueGrayAndColorful = progress;
            } else if (sbHSLDarkAndSun.equals(seekBar)) {
                mValueDarkAndLight = progress;
            }
            handleSeekBarChange(mValueColdAndWarm, mValueGrayAndColorful, mValueDarkAndLight);
        }*/
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (!isRealTimeRefresh) {
            boolean isNeedChangeColorPickView = true;
            if (sbHSLColdAndWarm.equals(seekBar)) {
                mValueColdAndWarm = seekBar.getProgress();
            } else if (sbHSLGrayAndColorful.equals(seekBar)) {
                mValueGrayAndColorful = seekBar.getProgress();
            } else if (sbHSLDarkAndSun.equals(seekBar)) {
                isNeedChangeColorPickView = false;
                mValueDarkAndLight = seekBar.getProgress();
            }
            handleSeekBarChange(mValueColdAndWarm, mValueGrayAndColorful, mValueDarkAndLight, isNeedChangeColorPickView);
        }
    }

    /**
     * 读取缓存中的收藏列表
     *
     * @return 收藏的Color列表
     */
    private List<ColorCollect> readCollectColors() {
        List<ColorCollect> resultList = null;
        String colorStr = PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(KEY_COLLECT_COLORS_LIST, null);
        if (null == colorStr) return null;
        ColorCollectList colorCollectList = null;
        if (!TextUtils.isEmpty(colorStr)) {
            try {
                colorCollectList = new GsonBuilder().create().fromJson(colorStr, ColorCollectList.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != colorCollectList) {
            resultList = colorCollectList.getList();
        }
        return resultList;
    }

    /**
     * 保存收藏列表
     */
    private void saveCollectColors(List<ColorCollect> list) {
        ColorCollectList colorCollectList = new ColorCollectList();
        colorCollectList.setList(list);
        PreferencesHelper.putStringValue(AppUtil.getContext(), KEY_COLLECT_COLORS_LIST, colorCollectList.toString());
    }

    private void handleSeekBarChange(float valueH, float valueS, float valueL, boolean isNeedChangeColorPickView) {
        ColorRGB colorRGB;
        if (ColorPicker2View.isHSL) {
            ColorHSL colorHSL = new ColorHSL(valueH, valueS, valueL);
            colorRGB = RGB2HSLUtil.HSLtoRGB(colorHSL);
        } else {
            ColorHSB colorHSB = new ColorHSB(valueH, valueS, valueL);
            colorRGB = RGB2HSLUtil.HSBtoRGB(colorHSB);
        }
        int color = Color.rgb(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue());
        if (isNeedChangeColorPickView) {
            syncColorPickerView(valueH, valueS);//此处不用color因为经过来回转换的color会发生变化
        }
        sendColorCmdToDevice(color);
    }

    private void syncColorPickerView(int color) {
        colorPicker.setColor(color);
    }

    /**
     * 根据hue和saturation同步ColorPickerView的位置和currentColor
     *
     * @param hue        HUe
     * @param saturation Saturation
     */
    private void syncColorPickerView(float hue, float saturation) {
        colorPicker.setHueAndSaturation(hue, saturation);
    }

    /**
     * 根据color值算出HSL值，并同步给Hue 和saturation的SeekBar(此处会产生精度丢失，故此不适合多次来回使用，导致偏移越来越大)
     *
     * @param color 同步的color的int型
     */
    private void syncHSLSeekBarGroup(int color) {
        if (ColorPicker2View.isHSL) {
            ColorHSL colorHSL = RGB2HSLUtil.RGBtoHSL(color);
            mValueColdAndWarm = colorHSL.getHue();
            mValueGrayAndColorful = colorHSL.getSaturation();
            sbHSLColdAndWarm.setProgress((int) colorHSL.getHue());
            sbHSLGrayAndColorful.setProgress((int) colorHSL.getSaturation());
        } else {
            ColorHSB colorHSB = RGB2HSLUtil.RGBtoHSB(color);
            mValueColdAndWarm = colorHSB.getHue();
            mValueGrayAndColorful = colorHSB.getSaturation();
            sbHSLColdAndWarm.setProgress((int) colorHSB.getHue());
            sbHSLGrayAndColorful.setProgress((int) colorHSB.getSaturation());
        }
    }

    /**
     * 根据对应的值同步对应的SeekBar(用于处理精度丢失出现偏移)
     *
     * @param valueH 同步的hue
     * @param valueS 同步的saturation
     * @param valueL 同步的luminance
     */
    private void syncHSLSeekBarGroup(Float valueH, Float valueS, Float valueL) {
        if (null != valueH) {
            sbHSLColdAndWarm.setProgress(valueH.intValue());
            mValueColdAndWarm = valueH.intValue();
        }
        if (valueS != null) {
            sbHSLGrayAndColorful.setProgress(valueS.intValue());
            mValueGrayAndColorful = valueS.intValue();
        }
        if (valueL != null) {
            sbHSLDarkAndSun.setProgress(valueL.intValue());
            mValueDarkAndLight = valueL.intValue();
        }
    }

    /**
     * 只有HS有效的Color转换HSL的Color
     *
     * @param onlyHSColor 只有HS有效的Color
     * @param luminance   有效的亮度(luminance)
     * @return 返回完整的HSL的color
     */
    private int handleOnlyHSColorToHSLColor(int onlyHSColor, float luminance) {
        ColorRGB colorRGB;
        if (ColorPicker2View.isHSL) {
            ColorHSL colorOnlyHS = RGB2HSLUtil.RGBtoHSL(onlyHSColor);
            colorOnlyHS.setLuminance(luminance);
            colorRGB = RGB2HSLUtil.HSLtoRGB(colorOnlyHS);
        } else {
            ColorHSB colorOnlyHS = RGB2HSLUtil.RGBtoHSB(onlyHSColor);
            colorOnlyHS.setBrightness(luminance);
            colorRGB = RGB2HSLUtil.HSBtoRGB(colorOnlyHS);
        }
        return Color.rgb(colorRGB.getRed(), colorRGB.getGreen(), colorRGB.getBlue());
    }

    private void sendColorCmdToDevice(int color) {
        if (!mRCSPController.isDeviceConnected()) {
            return;
        }
        if (currentHSLColor == color) return;
        currentHSLColor = color;
        ibtnColorAdd.setColor(color);
        LightControlInfo lightControlInfo = mRCSPController.getDeviceInfo().getLightControlInfo();
        if (null == lightControlInfo) return;
        lightControlInfo.setSwitchState(LightControlInfo.STATE_SETTING)
                .setLightMode(LightControlInfo.LIGHT_MODE_COLOURFUL)
                .setColor(color)
                .setHue((int) mValueColdAndWarm)
                .setSaturation((int) mValueGrayAndColorful)
                .setLuminance((int) mValueDarkAndLight);
        mRCSPController.setLightControlInfo(mRCSPController.getUsingDevice(), lightControlInfo, null);
    }

    private final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (status != StateCode.CONNECTION_OK && mRCSPController.isUsingDevice(device)) {
                requireActivity().finish();
            }
        }

        @Override
        public void onLightControlInfo(BluetoothDevice device, LightControlInfo lightControlInfo) {
            if (!isAdded() || isDetached()) return;
            syncColorPickerView(lightControlInfo.getHue(), lightControlInfo.getSaturation());
            syncHSLSeekBarGroup((float) lightControlInfo.getHue(), (float) lightControlInfo.getSaturation(), (float) lightControlInfo.getLuminance());
            ibtnColorAdd.setColor(lightControlInfo.getColor());
            currentHSLColor = lightControlInfo.getColor();
        }
    };

}
