package com.jieli.btsmart.ui.auracast.transmitter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.audio.AudioFormat;
import com.jieli.bluetooth.bean.auracast.transmitter.TransmitterSettings;
import com.jieli.bluetooth.constant.AudioFormatMap;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentAudioFormatBinding;
import com.jieli.btsmart.databinding.ItemSettingsBinding;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.dialog.AudioFormatDialog;
import com.jieli.btsmart.util.UIHelper;

/**
 * AudioFormatFragment
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/24
 * note: 设置Auracast广播音频格式界面
 */
public class AudioFormatFragment extends DeviceControlFragment {

    private FragmentAudioFormatBinding binding;
    private AuracastTransmitterViewModel viewModel;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAudioFormatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = AuracastTransmitterFragment.viewModel;
        if (null == viewModel) {
            finish();
            return;
        }
        initUI();
        addObserver();
        final TransmitterSettings settings = viewModel.getSettingsInfo();
        if (null == settings || !settings.hasType(TransmitterSettings.TYPE_AUDIO_FORMAT)) {
            viewModel.readSettingsInfo();
        } else {
            updateAudioFormat(settings.getAudioFormat());
        }
    }

    private void initUI() {
        UIHelper.updateItemSettingsTextUI(binding.viewAudioFormat, CommonUtil.formatString("%s : ", getString(R.string.audio_format)), "",
                true, false, v -> {
                    final TransmitterSettings settings = viewModel.getSettingsInfo();
                    if (null == settings || !settings.hasType(TransmitterSettings.TYPE_AUDIO_FORMAT))
                        return; //正在读取配置
                    new AudioFormatDialog.Builder()
                            .setAudioFormatID(settings.getAudioFormat())
                            .setCallback(result -> viewModel.setAudioFormatID(result))
                            .build().show(getChildFragmentManager(), AudioFormatFragment.class.getSimpleName());
                });
        updateItemText(binding.viewSampleRate, getString(R.string.sample_rate));
        updateItemText(binding.viewSduInterval, getString(R.string.sdu_interval));
        updateItemText(binding.viewPacketLength, getString(R.string.packet_length));
        updateItemText(binding.viewCodeRate, getString(R.string.code_rate));
        updateItemText(binding.viewMaxTransportLatency, getString(R.string.max_transport_latency));
        updateItemText(binding.viewPresentationDelay, getString(R.string.presentation_delay));
    }

    private void addObserver() {
        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (!BluetoothUtil.deviceEquals(deviceConnection.getDevice(), viewModel.getDevice()))
                return;
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        viewModel.loginStateMLD.observe(getViewLifecycleOwner(), loginState -> {
            if (!loginState) {
                finish();
            }
        });
        viewModel.transmitterSettingsMLD.observe(getViewLifecycleOwner(), settings -> {
            if (null == settings) return;
            updateAudioFormat(settings.getAudioFormat());
        });
        viewModel.opResultMLD.observe(getViewLifecycleOwner(), opResult -> {
            if (null == opResult || opResult.getOp() != AuracastAssistantViewModel.OP_SET_AUDIO_FORMAT)
                return;
            if (opResult.isSuccess()) { //设置成功
                finish();
                return;
            }
            showTips(CommonUtil.formatString("%s\n%s : %s, %s", getString(R.string.operation_failed, viewModel.getOpString(opResult.getOp())),
                    getString(R.string.error_code), CommonUtil.formatInt(opResult.getCode()), opResult.getMessage()));
        });
    }

    private void updateItemText(@NonNull ItemSettingsBinding binding, String title) {
        UIHelper.updateItemSettingsTextUI(binding, title, "", false, false, null);
    }

    private void updateAudioFormat(int audioFormatID) {
        if (isInvalid()) return;
        AudioFormat audioFormat = AudioFormatMap.getInstance().findAudioFormatByID(audioFormatID);
        if (null == audioFormat) return;
        binding.viewAudioFormat.tvItemSettingsValue.setText(audioFormat.getName());

        binding.viewSampleRate.tvItemSettingsValue.setText(String.valueOf(audioFormat.getSampleRate()));
        binding.viewSduInterval.tvItemSettingsValue.setText(String.valueOf(audioFormat.getSDUInterval()));
        binding.viewPacketLength.tvItemSettingsValue.setText(String.valueOf(audioFormat.getPacketLength()));
        binding.viewCodeRate.tvItemSettingsValue.setText(String.valueOf(audioFormat.getCodeRate()));
        binding.viewResendTimes.tvItemSettingsValue.setText(String.valueOf(audioFormat.getReSend()));
        binding.viewMaxTransportLatency.tvItemSettingsValue.setText(String.valueOf(audioFormat.getMaxTransportLatency()));
        //注意单位转换， ms --> us
        binding.viewPresentationDelay.tvItemSettingsValue.setText(String.valueOf(audioFormat.getPresentationLatency() * 1000));

    }
}