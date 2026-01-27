package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.databinding.DialogCallTranslationTipsBinding;

/**
 * CallTranslationTipsDialog
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通话翻译提示对话框
 * @since 2025/8/15
 */
public class CallTranslationTipsDialog extends CommonDialog {

    private DialogCallTranslationTipsBinding mBinding;

    private CallTranslationTipsDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogCallTranslationTipsBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        mBinding.btnConfirm.setOnClickListener(v -> {
            final OnTipsEventListener listener = builder.getListener();
            if (null != listener) {
                listener.onConfirm(this);
            }
        });
    }

    public static class Builder extends CommonDialog.Builder {

        private OnTipsEventListener listener;

        public Builder() {
            setCancelable(false).setWidthRate(1.0f)
                    .setGravity(Gravity.BOTTOM);
        }

        public Builder listener(OnTipsEventListener listener) {
            this.listener = listener;
            return this;
        }

        public OnTipsEventListener getListener() {
            return listener;
        }

        @Override
        public CallTranslationTipsDialog build() {
            return new CallTranslationTipsDialog(this);
        }
    }

    public interface OnTipsEventListener {

        /**
         * 确定事件回调
         *
         * @param dialog CallTranslationTipsDialog 对话框
         */
        void onConfirm(CallTranslationTipsDialog dialog);
    }
}
