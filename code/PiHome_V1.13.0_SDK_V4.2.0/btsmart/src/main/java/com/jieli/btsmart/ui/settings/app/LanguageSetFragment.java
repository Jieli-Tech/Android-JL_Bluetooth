package com.jieli.btsmart.ui.settings.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;

import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentLanguageSetBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.btsmart.util.MultiLanguageUtils;
import com.jieli.component.ActivityManager;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.ValueUtil;

import static com.jieli.btsmart.util.MultiLanguageUtils.LANGUAGE_AUTO;


/**
 *
 */
public class LanguageSetFragment extends BaseFragment {

    private FragmentLanguageSetBinding binding;
    private String selectLanguage = null;
    private String setLanguage = null;//已设置的语言
    private TextView mConfirmTextView;

    public static LanguageSetFragment newInstance() {
        return new LanguageSetFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_language_set, container, false);
//        binding.viewTopbar.tvTopbarTitle.setText(getString(R.string.set_language));
//        binding.viewTopbar.tvTopbarLeft.setOnClickListener(v -> requireActivity().onBackPressed());
//        binding.viewTopbar.tvTopbarRight.setVisibility(View.VISIBLE);
//        binding.viewTopbar.tvTopbarRight.setTextColor(getResources().getColor(R.color.gray_D8D8D8));
//        binding.viewTopbar.tvTopbarRight.setText(getString(R.string.confirm));
//        binding.viewTopbar.tvTopbarRight.setOnClickListener(v -> saveChange());
        binding.tvFollowSystem.setTag(LANGUAGE_AUTO);
        binding.tvSimplifiedChinese.setTag(MultiLanguageUtils.LANGUAGE_ZH);
        binding.tvEnglish.setTag(MultiLanguageUtils.LANGUAGE_EN);
        binding.tvJapanese.setTag(MultiLanguageUtils.LANGUAGE_JA);
        initClick();
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CommonActivity activity = (CommonActivity) getActivity();
        if (activity != null) {
            activity.updateTopBar(getString(R.string.set_language), R.drawable.ic_back_black, v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }, 0, null);
            TextView textView = new TextView(getContext());
            textView.setText(R.string.confirm);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0,0, ValueUtil.dp2px(getContext(),16),0);
            textView.setLayoutParams(params);
            textView.setOnClickListener(v -> {
                saveChange();
            });
            textView.setTextColor(getResources().getColor(R.color.gray_CECECE));
            textView.setTextSize(16);
            mConfirmTextView = textView;
            activity.updateTopBar(getString(R.string.set_language), null, textView);
        }
        setLanguage = PreferencesHelper.getSharedPreferences(MainApplication.getApplication()).getString(MultiLanguageUtils.SP_LANGUAGE, MultiLanguageUtils.LANGUAGE_AUTO);
        selectLanguage = setLanguage;
        updateSelectView();
        updateConfirmButton();
    }

    private void initClick() {
        ConstraintLayout parent = (ConstraintLayout) binding.getRoot();
        int count = parent.getChildCount();
        for (int i = 1; i < count; i++) {
            if (!(parent.getChildAt(i) instanceof TextView)) continue;
            TextView textView = (TextView) parent.getChildAt(i);
            textView.setOnClickListener(v -> {
                if (v.getTag() != null) {
                    String t1 = (String) v.getTag();
                    selectLanguage = t1;
                    updateSelectView();
                    updateConfirmButton();
                }
            });
        }
    }

    private void updateConfirmButton() {
        mConfirmTextView.setTextColor(getResources().getColor(!isSetLanguage(selectLanguage) ? R.color.blue_448eff : R.color.gray_CECECE));
        mConfirmTextView.setEnabled(!isSetLanguage(selectLanguage));
    }

    private void updateSelectView() {
        ConstraintLayout parent = (ConstraintLayout) binding.getRoot();
        int count = parent.getChildCount();
        for (int i = 1; i < count; i++) {
            if (!(parent.getChildAt(i) instanceof TextView)) continue;
            TextView textView = (TextView) parent.getChildAt(i);
            String tag = (String) textView.getTag();
            if (selectLanguage != null && tag.equals(selectLanguage)) {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_choose_blue, 0);
            } else {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    }

    private boolean isSetLanguage(String language) {
        return TextUtils.equals(setLanguage, language);
    }

    private void saveChange() {
        Context context = MainApplication.getApplication();
        switch (selectLanguage) {
            case LANGUAGE_AUTO://切换到 跟随系统
                MultiLanguageUtils.changeLanguage(context, LANGUAGE_AUTO, MultiLanguageUtils.AREA_AUTO);
                break;
            case MultiLanguageUtils.LANGUAGE_ZH:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_ZH, MultiLanguageUtils.AREA_ZH);
                break;
            case MultiLanguageUtils.LANGUAGE_EN:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_EN, MultiLanguageUtils.AREA_EN);
                break;
            case MultiLanguageUtils.LANGUAGE_JA:
                MultiLanguageUtils.changeLanguage(context, MultiLanguageUtils.LANGUAGE_JA, MultiLanguageUtils.AREA_JA);
                break;
        }
        //关闭应用所有Activity

        while (!(ActivityManager.getInstance().getTopActivity() instanceof HomeActivity)) {
            Activity activity = ActivityManager.getInstance().getTopActivity();
            activity.finish();
            ActivityManager.getInstance().popActivity(activity);
        }
        Intent intent = new Intent(HomeActivity.HOME_ACTIVITY_RELOAD);
        getActivity().sendBroadcast(intent);
    }
}