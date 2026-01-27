package com.jieli.btsmart.ui.translate.language;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.language.LanguageInfo;
import com.jieli.btsmart.databinding.FragmentSelectLanguageBinding;
import com.jieli.btsmart.tool.ai.doubao.translate.model.language.Language;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.translate.TranslateFragment;
import com.jieli.btsmart.ui.translate.TranslateViewModel;
import com.jieli.btsmart.ui.translate.call.CallTranslationFragment;
import com.jieli.btsmart.ui.translate.face_to_face.FaceToFaceTranslationFragment;
import com.jieli.btsmart.ui.translate.record.RecordTranslationFragment;
import com.jieli.btsmart.ui.widget.dialog.CallTranslationTipsDialog;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.TranslateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * SelectLanguageFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 选择语言界面
 * @since 2025/5/27
 */
public class SelectLanguageFragment extends DeviceControlFragment {

    public static final String KEY_SRC_LANG = "src_language";
    public static final String KEY_DEST_LANG = "dest_language";

    public static String getLanguage(@NonNull Context context, @NonNull String languageCode) {
        if (TextUtils.isEmpty(languageCode)) return "";
        switch (languageCode) {
            case Language.LANG_ZH:
                return context.getString(R.string.language_chinese);
            case Language.LANG_EN:
                return context.getString(R.string.language_english);
            case Language.LANG_JA:
                return context.getString(R.string.language_japanese);
        }
        return "";
    }

    private FragmentSelectLanguageBinding mBinding;
    private TranslateViewModel mViewModel;
    private SelectLanguageAdapter mAdapter;

    private TranslationMode translationMode;

    private String srcLanguage = Language.LANG_ZH;
    private String destLanguage = Language.LANG_EN;
    private boolean isSelectSrc = false;

    private final List<LanguageInfo> mLanguageInfoList = new ArrayList<>();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentSelectLanguageBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = TranslateFragment.translateViewModel;
        if (null == mViewModel) {
            finish();
            return;
        }
        final Bundle bundle = getArguments();
        if (null == bundle) {
            finish();
            return;
        }
        translationMode = bundle.getParcelable(TranslateFragment.KEY_TRANSLATION_MODE);
        if (null == translationMode) {
            finish();
            return;
        }
        initLanguageList();
        initUI();
        addObserver();
    }

    private void initUI() {
        int mode = TranslateUtil.getTranslationMode(translationMode);
        switch (mode) {
            case TranslationMode.MODE_CALL_TRANSLATION: {
                srcLanguage = Language.LANG_EN;
                destLanguage = Language.LANG_ZH;
                mBinding.viewSwitchLanguage.tvDestLanguage.setText(getString(R.string.our_side));
                mBinding.viewSwitchLanguage.tvSrcLanguage.setText(getString(R.string.other_side));
                mBinding.viewSwitchLanguage.ivSrcLanguage.setImageResource(R.drawable.ic_translation_side_gray);
                break;
            }
            case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION: {
                mBinding.viewSwitchLanguage.tvDestLanguage.setText(getString(R.string.headphones));
                mBinding.viewSwitchLanguage.ivSrcLanguage.setImageResource(R.drawable.ic_phone_gray);
                mBinding.viewSwitchLanguage.tvSrcLanguage.setText(getString(R.string.mobile_phone));
                break;
            }
            case TranslationMode.MODE_RECORDING_TRANSLATION: {
                mBinding.viewSwitchLanguage.tvDestLanguage.setText(getString(R.string.playback));
                mBinding.viewSwitchLanguage.ivSrcLanguage.setImageResource(R.drawable.ic_phone_gray);
                mBinding.viewSwitchLanguage.tvSrcLanguage.setText(getString(R.string.receiver));
                break;
            }
        }

        mAdapter = new SelectLanguageAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            LanguageInfo item = mAdapter.getItem(position);
            if (mAdapter.isSelectedItem(item)) return;
            mAdapter.updateSelectedItem(item);
            if (isSelectSrc) {
                srcLanguage = item.getFlag();
            } else {
                destLanguage = item.getFlag();
            }
            updateLanguageUI();
        });
        mBinding.rvLanguage.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvLanguage.setAdapter(mAdapter);
        mBinding.viewSwitchLanguage.btnSrcLanguage.setOnClickListener(v -> updateSelectedLanguage(true));
        mBinding.viewSwitchLanguage.btnDestLanguage.setOnClickListener(v -> updateSelectedLanguage(false));
        mBinding.viewSwitchLanguage.ivSwitchLanguage.setOnClickListener(v -> {
            String temp = srcLanguage;
            srcLanguage = destLanguage;
            destLanguage = temp;
            updateLanguageUI();
            updateSelectedLanguage(isSelectSrc);
        });
        mBinding.btnSelectLanguage.setOnClickListener(v -> {
            int cacheMode = TranslateUtil.getTranslationMode(translationMode);
            if (cacheMode == TranslationMode.MODE_CALL_TRANSLATION &&
                    !ConfigureKit.getInstance().isBanCallTranslationIntroduction()) {
                showCallTranslationTipsDialog();
                return;
            }
            showLoadingDialog(getString(R.string.loading));
            mViewModel.enterMode(translationMode, srcLanguage, destLanguage);
        });
        updateLanguageUI();
        updateSelectedLanguage(isSelectSrc);
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> {
            if (BluetoothUtil.deviceEquals(connection.getDevice(), mViewModel.getDevice())
                    && connection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        mViewModel.opResultMLD.observe(getViewLifecycleOwner(), result -> {
            if (null == result || result.getOp() != TranslateViewModel.OP_ENTER_MODE) return;
            dismissLoadingDialog();
            mViewModel.opResultMLD.setValue(null);
            if (!result.isSuccess()) {
                finish(300, () -> showTips(AppUtil.formatString("%s\n%s : %s, %s",
                        getString(R.string.enter_translation_mode_failed),
                        getString(R.string.error_code),
                        CommonUtil.formatInt(result.getCode()), result.getMessage())));
                return;
            }
            goToTranslationMode((TranslationMode) result.getData());
        });
    }

    private void initLanguageList() {
        mLanguageInfoList.add(new LanguageInfo(Language.LANG_ZH, getString(R.string.language_chinese)));
        mLanguageInfoList.add(new LanguageInfo(Language.LANG_EN, getString(R.string.language_english)));
        mLanguageInfoList.add(new LanguageInfo(Language.LANG_JA, getString(R.string.language_japanese)));
    }

    private String getSelectedLanguage() {
        return isSelectSrc ? srcLanguage : destLanguage;
    }

    private LanguageInfo findLanguageInfo(String flag) {
        if (null == flag) return null;
        for (LanguageInfo language : mLanguageInfoList) {
            if (flag.equals(language.getFlag())) {
                return language;
            }
        }
        return null;
    }

    private void loadLanguages(String selectFlag) {
        mAdapter.setList(mLanguageInfoList);
        LanguageInfo info = findLanguageInfo(selectFlag);
        if (null != info) {
            mAdapter.updateSelectedItem(info);
        }
    }

    private void updateLanguageUI() {
        LanguageInfo srcInfo = findLanguageInfo(srcLanguage);
        if (srcInfo != null) {
            mBinding.viewSwitchLanguage.btnSrcLanguage.setText(srcInfo.getLanguage());
            mBinding.viewSwitchLanguage.btnSrcLanguage.setTextColor(ContextCompat.getColor(requireContext(),
                    isSelectSrc ? R.color.purple_7657EC : R.color.black_E6000000));
        }
        LanguageInfo destInfo = findLanguageInfo(destLanguage);
        if (destInfo != null) {
            mBinding.viewSwitchLanguage.btnDestLanguage.setText(destInfo.getLanguage());
            mBinding.viewSwitchLanguage.btnDestLanguage.setTextColor(ContextCompat.getColor(requireContext(),
                    isSelectSrc ? R.color.black_E6000000 : R.color.purple_7657EC));
        }
    }

    private void updateSelectedLanguage(boolean isSrc) {
        isSelectSrc = isSrc;
        mBinding.viewSwitchLanguage.btnSrcLanguage.setSelected(isSrc);
        mBinding.viewSwitchLanguage.btnDestLanguage.setSelected(!isSrc);
        loadLanguages(getSelectedLanguage());
        updateLanguageUI();
    }

    private void goToTranslationMode(TranslationMode mode) {
        if (null == mode || isInvalid()) return;
        Bundle bundle = new Bundle();
        bundle.putParcelable(TranslateFragment.KEY_TRANSLATION_MODE, translationMode);
        bundle.putString(KEY_SRC_LANG, srcLanguage);
        bundle.putString(KEY_DEST_LANG, destLanguage);
        int cacheMode = TranslateUtil.getTranslationMode(mode);
        switch (cacheMode) {
            case TranslationMode.MODE_CALL_TRANSLATION: {
                ContentActivity.startActivity(requireContext(), CallTranslationFragment.class.getCanonicalName(), bundle);
                break;
            }
            case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION: {
                ContentActivity.startActivity(requireContext(), FaceToFaceTranslationFragment.class.getCanonicalName(), bundle);
                break;
            }
            case TranslationMode.MODE_RECORDING_TRANSLATION: {
                ContentActivity.startActivity(requireContext(), RecordTranslationFragment.class.getCanonicalName(), bundle);
                break;
            }
        }
        finish();
    }

    private void showCallTranslationTipsDialog() {
        if (isInvalid()) return;
        new CallTranslationTipsDialog.Builder()
                .listener(dialog -> {
                    dialog.dismiss();
                    ConfigureKit.getInstance().setBanCallTranslationIntroduction(true);
                }).build().show(getChildFragmentManager(), CallTranslationTipsDialog.class.getSimpleName());
    }
}