package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.databinding.DialogPermissionTipsBinding;

/**
 * PermissionTipsDialog
 * @author zqjasonZhong
 * @since 2025/4/22
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 权限申请说明弹窗
 */
public class PermissionTipsDialog extends CommonDialog {

    private DialogPermissionTipsBinding mBinding;

    protected PermissionTipsDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogPermissionTipsBinding.inflate(inflater, container, false);
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
        updateTextStyle(this, mBinding.tvTips, builder.getTipsStyle());
    }

    public static class Builder extends CommonDialog.Builder {
        private TextStyle tipsStyle;

        public Builder() {
            setGravity(Gravity.TOP);
        }

        public Builder tips(String text) {
            if (null == tipsStyle) {
                tipsStyle = new TextStyle();
            }
            tipsStyle.setText(text);
            return this;
        }

        public TextStyle getTipsStyle() {
            return tipsStyle;
        }

        @Override
        public PermissionTipsDialog build() {
            return new PermissionTipsDialog(this);
        }
    }
}
