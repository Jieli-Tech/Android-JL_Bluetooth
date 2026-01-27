package com.jieli.btsmart.ui.translate.call;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.data.model.translation.TranslationRecord;
import com.jieli.btsmart.databinding.FragmentCallTranslationBinding;
import com.jieli.btsmart.tool.ai.doubao.translate.model.language.Language;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.translate.TranslationRecordAdapter;
import com.jieli.btsmart.ui.translate.basic.BasicTranslationFragment;
import com.jieli.btsmart.ui.translate.language.SelectLanguageFragment;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * CallTranslationFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通话翻译界面
 * @since 2025/6/9
 */
public class CallTranslationFragment extends BasicTranslationFragment {

    private FragmentCallTranslationBinding mBinding;
    private TranslationRecordAdapter mAdapter;
    private InputMethodManager mInputMethodManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentCallTranslationBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public int getTranslationMode() {
        return TranslationMode.MODE_CALL_TRANSLATION;
    }

    @Override
    protected void initUI() {
        super.initUI();
        if (requireActivity() instanceof BaseActivity) {
            ((BaseActivity) requireActivity()).setCustomBackPress(() -> {
                exitFragment();
                return true;
            });
        }
        mInputMethodManager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        UIHelper.gone(mBinding.viewToolBar.btnBack);
        UIHelper.gone(mBinding.viewToolBar.btnVolumeCtrl);
        mBinding.viewToolBar.tvReceiverLanguage.setText(SelectLanguageFragment.getLanguage(requireContext(), destLanguage));
        mBinding.viewToolBar.tvReceiverLanguage.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_translation_side_gray, 0);
        mBinding.viewToolBar.tvPlayerLanguage.setText(SelectLanguageFragment.getLanguage(requireContext(), srcLanguage));
        mBinding.viewToolBar.ivTranslationDirection.setImageResource(R.drawable.ic_two_way_gray);

        mBinding.viewVoiceInput.btnSwitchLanguage.setOnClickListener(v -> {

        });
        mBinding.viewVoiceInput.btnStopTranslation.setOnClickListener(v -> {
            JL_Log.i(TAG, "exitFragment", "User actively exits translation mode.");
            mViewModel.exitMode();
        });
        mBinding.viewVoiceInput.btnSwitchKeyboardInput.setOnClickListener(v -> updateBottomBarUI(true));

        mBinding.viewKeyboardInput.btnSwitchVoiceInput.setOnClickListener(v -> updateBottomBarUI(false));
        mBinding.viewKeyboardInput.btnSendMessage.setOnClickListener(v -> {
            String text = mBinding.viewKeyboardInput.etMessage.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                showTips(getString(R.string.tips_empty_text));
                return;
            }
            if (mViewModel.isTextTranslating()) {
                showTips(getString(R.string.tips_translating));
                return;
            }
            //文本翻译
            mViewModel.translateText(text);
            mBinding.viewKeyboardInput.btnSendMessage.postDelayed(() -> {
                mBinding.viewKeyboardInput.etMessage.setText("");
                mBinding.viewKeyboardInput.etMessage.clearFocus();
                // 隐藏软键盘
                mInputMethodManager.hideSoftInputFromWindow(mBinding.viewKeyboardInput.etMessage.getWindowToken(), 0);
            }, 300);
        });
        mAdapter = new TranslationRecordAdapter();
        mBinding.rvTranslationRecord.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvTranslationRecord.setAdapter(mAdapter);

        updateBottomBarUI(false);
        String tips = Language.getCallTranslationTips(srcLanguage);
        mViewModel.translateText(tips);
    }

    @Override
    protected void addObserver() {
        super.addObserver();
        mViewModel.translationRecordMLD.observeForever(translationRecordObserver);
        mViewModel.workTimeMLD.observe(getViewLifecycleOwner(), time -> {
            if (isInvalid()) return;
            mBinding.viewToolBar.tvTitle.setText(TranslateUtil.formatDuration(time));
        });
        mViewModel.saveSessionRecordMLD.observe(getViewLifecycleOwner(), result -> {
            if (isInvalid() || null == result) return;
            if (result.isSuccess()) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_BLUETOOTH_DEVICE, mViewModel.getDevice());
                bundle.putInt(SConstant.KEY_SESSION_ID, result.getData());
                ContentActivity.startActivity(requireContext(), CallRecordFragment.class.getCanonicalName(), bundle);
            }
        });
    }

    @Override
    protected void removeObserver() {
        if (null == mViewModel) return;
        mViewModel.translationRecordMLD.removeObserver(translationRecordObserver);
        super.removeObserver();
    }

    private void exitFragment() {
        if (mViewModel.isTranslating()) {
            new Jl_Dialog.Builder()
                    .content(getString(R.string.exit_call_translation))
                    .contentColor(ContextCompat.getColor(requireContext(), R.color.black_242424))
                    .left(getString(R.string.cancel))
                    .leftColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                    .right(getString(R.string.confirm))
                    .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                    .rightClickListener((v, dialogFragment) -> {
                        dialogFragment.dismiss();
                        JL_Log.i(TAG, "exitFragment", "User actively exits translation mode.");
                        mViewModel.exitMode();
                    })
                    .build().show(getChildFragmentManager(), Jl_Dialog.class.getSimpleName());
        } else {
            finish();
        }
    }

    private void updateBottomBarUI(boolean isKeyBoardInput) {
        if (isKeyBoardInput) {
            UIHelper.gone(mBinding.viewVoiceInput.getRoot());
            UIHelper.show(mBinding.viewKeyboardInput.getRoot());
            mBinding.viewKeyboardInput.etMessage.requestFocus();
        } else {
            mBinding.viewKeyboardInput.etMessage.setText("");
            UIHelper.gone(mBinding.viewKeyboardInput.getRoot());
            UIHelper.show(mBinding.viewVoiceInput.getRoot());
        }
    }

    private final Observer<OpResult<TranslationRecord>> translationRecordObserver = result -> {
        if (null == result || isInvalid()) return;
        if (!result.isSuccess()) {
            showTips(CommonUtil.formatString("%s\n%s : %s, %s",
                    getString(R.string.translation_failed),
                    getString(R.string.error_code), CommonUtil.formatInt(result.getCode()), result.getMessage()));
            return;
        }
        TranslationRecord record = result.getData();
        if (null == record) return;
        mAdapter.addData(record);
        int position = mAdapter.getItemPosition(record);
        if (!UIHelper.isItemVisible(mBinding.rvTranslationRecord, position, true)) {
            mBinding.rvTranslationRecord.scrollToPosition(position);
        }
    };
}