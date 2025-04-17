package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.databinding.DialogSelectPhotoBinding;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 选择照片方式对话框
 * @since 2023/12/6
 */
public class SelectPhotoDialog extends CommonDialog {

    private DialogSelectPhotoBinding mBinding;

    private SelectPhotoDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogSelectPhotoBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    private void initUI() {
        final Builder builder = (Builder) mBuilder;
        mBinding.btnCancel.setOnClickListener(v -> {
            if (builder.listener != null) {
                builder.listener.onCancel(SelectPhotoDialog.this);
            }
            dismiss();
        });
        mBinding.btnTakePhoto.setOnClickListener(v -> {
            if (builder.listener != null) {
                builder.listener.onTakePhoto(SelectPhotoDialog.this);
            }
            dismiss();
        });
        mBinding.btnSelectFromAlbum.setOnClickListener(v -> {
            if (builder.listener != null) {
                builder.listener.onSelectFromAlbum(SelectPhotoDialog.this);
            }
            dismiss();
        });
    }

    public interface OnSelectPhotoListener {

        void onTakePhoto(SelectPhotoDialog dialog);

        void onSelectFromAlbum(SelectPhotoDialog dialog);

        void onCancel(SelectPhotoDialog dialog);
    }

    public static class Builder extends CommonDialog.Builder {
        private OnSelectPhotoListener listener;

        public Builder() {
            setGravity(Gravity.BOTTOM).setWidthRate(1.0f);
        }

        public Builder listener(OnSelectPhotoListener listener) {
            this.listener = listener;
            return this;
        }

        @Override
        public SelectPhotoDialog build() {
            return new SelectPhotoDialog(this);
        }
    }
}
