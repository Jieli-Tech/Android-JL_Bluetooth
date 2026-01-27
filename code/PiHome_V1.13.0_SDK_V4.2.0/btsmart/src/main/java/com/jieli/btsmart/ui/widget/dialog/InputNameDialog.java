package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.DialogInputNameBinding;

/**
 * InputNameDialog
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: 输入广播名称弹窗
 */
public class InputNameDialog extends CommonDialog {

    private DialogInputNameBinding binding;

    protected InputNameDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogInputNameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSure.setOnClickListener(v -> {
            String name = binding.etInputName.getText().toString().trim();
            byte[] data = name.getBytes();
            if (data.length < 4 || data.length > 32) {
                showTips(getString(R.string.hint_input_name));
                return;
            }
            final OnResultCallback<String> callback = builder.getCallback();
            if (null != callback) {
                callback.onResult(name);
            }
            dismiss();
        });
        String name = builder.getBroadcastName();
        if (null != name) {
            binding.etInputName.setText(name);
            binding.etInputName.setSelection(name.length());
        }
    }

    public static class Builder extends CommonDialog.Builder {
        private String broadcastName;
        private OnResultCallback<String> callback;

        public Builder() {
            broadcastName = "";
            setCancelable(false).setWidthRate(0.8f);
        }

        public String getBroadcastName() {
            return broadcastName;
        }

        public Builder setBroadcastName(String broadcastName) {
            this.broadcastName = broadcastName;
            return this;
        }

        public OnResultCallback<String> getCallback() {
            return callback;
        }

        public Builder setCallback(OnResultCallback<String> callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public InputNameDialog build() {
            return new InputNameDialog(this);
        }
    }
}
