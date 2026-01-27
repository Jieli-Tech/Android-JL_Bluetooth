package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.audio.AudioFormat;
import com.jieli.bluetooth.constant.AudioFormatMap;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.DialogAudioFormatBinding;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AudioFormatDialog
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: 音频格式弹窗
 */
public class AudioFormatDialog extends CommonDialog {

    private DialogAudioFormatBinding binding;
    private AudioFormatAdapter adapter;

    protected AudioFormatDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogAudioFormatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        Builder builder = (Builder) mBuilder;
        binding.btnClose.setOnClickListener(v -> dismiss());

        adapter = new AudioFormatAdapter();
        adapter.setOnItemClickListener((adapter, view, position) -> {
            final AudioFormat audioFormat = AudioFormatDialog.this.adapter.getItem(position);
            if (AudioFormatDialog.this.adapter.isItemSelected(audioFormat)) return;
            AudioFormatDialog.this.adapter.updateSelectedItem(audioFormat.getId());
            final OnResultCallback<Integer> callback = builder.getCallback();
            if (null != callback) {
                callback.onResult(audioFormat.getId());
            }
            dismiss();
        });
        binding.rvAudioFormat.setAdapter(adapter);
        binding.rvAudioFormat.addItemDecoration(new CommonDecoration(requireContext(),
                ContextCompat.getColor(requireContext(), R.color.gray_F0F0F0), RecyclerView.VERTICAL,
                ValueUtil.dp2px(requireContext(), 1)));
        AudioFormat[] array = AudioFormatMap.getInstance().getAudioFormats();
        List<AudioFormat> list = new ArrayList<>(Arrays.asList(array));
        adapter.setList(list);

        if (builder.getAudioFormatID() != 0) {
            adapter.updateSelectedItem(builder.getAudioFormatID());
        }
    }

    public static class Builder extends CommonDialog.Builder {
        private int audioFormatID;
        private OnResultCallback<Integer> callback;

        public Builder() {
            setCancelable(false)
                    .setGravity(Gravity.BOTTOM)
                    .setWidthRate(1.0f);
        }

        public int getAudioFormatID() {
            return audioFormatID;
        }

        public Builder setAudioFormatID(int audioFormatID) {
            this.audioFormatID = audioFormatID;
            return this;
        }

        public OnResultCallback<Integer> getCallback() {
            return callback;
        }

        public Builder setCallback(OnResultCallback<Integer> callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public AudioFormatDialog build() {
            return new AudioFormatDialog(this);
        }
    }
}
