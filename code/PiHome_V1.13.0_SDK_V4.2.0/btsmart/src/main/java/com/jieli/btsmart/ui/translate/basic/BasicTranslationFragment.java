package com.jieli.btsmart.ui.translate.basic;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.btsmart.data.model.device.DeviceConnection;
import com.jieli.btsmart.tool.ai.doubao.translate.model.language.Language;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.translate.TranslateFragment;
import com.jieli.btsmart.ui.translate.TranslateViewModel;
import com.jieli.btsmart.ui.translate.language.SelectLanguageFragment;
import com.jieli.btsmart.util.TranslateUtil;

/**
 * BasicTranslationFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 基础翻译界面
 * @since 2025/8/11
 */
public abstract class BasicTranslationFragment extends DeviceControlFragment {
    /**
     * 翻译功能逻辑实现
     */
    protected TranslateViewModel mViewModel;
    /**
     * 原文语言
     */
    protected String srcLanguage;
    /**
     * 译文语言
     */
    protected String destLanguage;

    public abstract int getTranslationMode();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = TranslateFragment.translateViewModel;
        if (null == mViewModel) {
            finish();
            return;
        }
        final Bundle bundle = getArguments();
        if (!mViewModel.isTranslating() || null == bundle) {
            finish();
            return;
        }
        final TranslationMode mode = mViewModel.getTranslationMode();
        int translationMode = TranslateUtil.getTranslationMode(mode);
        if (translationMode != getTranslationMode()) {
            finish();
            return;
        }
        String srcLang = bundle.getString(SelectLanguageFragment.KEY_SRC_LANG, Language.LANG_ZH);
        String destLang = bundle.getString(SelectLanguageFragment.KEY_DEST_LANG, Language.LANG_EN);
        if (translationMode == TranslationMode.MODE_CALL_TRANSLATION) {
            srcLanguage = destLang;
            destLanguage = srcLang;
        } else {
            srcLanguage = srcLang;
            destLanguage = destLang;
        }

        initUI();
        addObserver();
    }

    @Override
    public void onDestroyView() {
        removeObserver();
        super.onDestroyView();
    }

    protected void initUI() {
        hideTopBar();
    }

    private final Observer<DeviceConnection> connectionObserver = connection -> {
        if (null == connection) return;
        if (BluetoothUtil.deviceEquals(connection.getDevice(), mViewModel.getDevice())
                && connection.getStatus() != StateCode.CONNECTION_OK) {
            finish();
        }
    };

    private final Observer<TranslationMode> modeObserver = mode -> {
        if (null == mode) return;
        int translationMode = TranslateUtil.getTranslationMode(mode);
        if (translationMode != getTranslationMode()) {
            requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            finish();
            return;
        }
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    };

    protected void addObserver() {
        mViewModel.deviceConnectionMLD.observeForever(connectionObserver);
        mViewModel.modeChangeMLD.observeForever(modeObserver);
    }

    protected void removeObserver() {
        if (null == mViewModel) return;
        mViewModel.deviceConnectionMLD.removeObserver(connectionObserver);
        mViewModel.modeChangeMLD.removeObserver(modeObserver);
        mViewModel.translationRecordMLD.setValue(null);
        mViewModel.opResultMLD.setValue(null);
        mViewModel.workTimeMLD.setValue(0);
        mViewModel.translateStateMLD.setValue(null);
        mViewModel.deviceRecordStateMLD.setValue(null);
        mViewModel.saveSessionRecordMLD.setValue(null);
        mViewModel.exitMode(true);
    }


}
