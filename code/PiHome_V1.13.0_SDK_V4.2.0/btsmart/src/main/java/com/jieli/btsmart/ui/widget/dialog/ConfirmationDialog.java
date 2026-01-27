package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.databinding.DialogConfirmationBinding;

/**
 * ConfirmationDialog
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 确认对话框
 * @since 2025/2/19
 */
public class ConfirmationDialog extends CommonDialog {

    private DialogConfirmationBinding mBinding;

    protected ConfirmationDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogConfirmationBinding.inflate(inflater, container, false);
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
            dismiss();
            final OnClickEventListener listener = builder.getListener();
            if (null != listener) {
                listener.onConfirm(this);
            }
        });
        mBinding.btnCancel.setOnClickListener(v -> {
            dismiss();
            final OnClickEventListener listener = builder.getListener();
            if (null != listener) {
                listener.onCancel(this);
            }
        });
    }

    public static class Builder extends CommonDialog.Builder {

        private final OnClickEventListener mListener;

        public Builder(OnClickEventListener listener) {
            mListener = listener;
            setGravity(Gravity.BOTTOM)
                    .setWidthRate(1.0f);
        }

        public OnClickEventListener getListener() {
            return mListener;
        }

        @Override
        public ConfirmationDialog build() {
            return new ConfirmationDialog(this);
        }
    }

    public interface OnClickEventListener {

        void onConfirm(ConfirmationDialog dialog);

        void onCancel(ConfirmationDialog dialog);
    }
}
