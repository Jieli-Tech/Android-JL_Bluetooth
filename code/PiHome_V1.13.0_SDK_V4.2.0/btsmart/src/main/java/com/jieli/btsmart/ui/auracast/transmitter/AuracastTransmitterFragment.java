package com.jieli.btsmart.ui.auracast.transmitter;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.audio.AudioFormat;
import com.jieli.bluetooth.bean.auracast.transmitter.EncryptionSettings;
import com.jieli.bluetooth.bean.auracast.transmitter.TransmitterSettings;
import com.jieli.bluetooth.constant.AudioFormatMap;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.auracast.AuracastQRCode;
import com.jieli.btsmart.data.model.basic.OpResult;
import com.jieli.btsmart.databinding.FragmentAuracastTransmitterBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.dialog.InputCodeDialog;
import com.jieli.btsmart.ui.widget.dialog.InputNameDialog;
import com.jieli.btsmart.ui.widget.dialog.SaveQRCodeDialog;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.jl_dialog.Jl_Dialog;

/**
 * AuracastTransmitterFragment
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/20
 * note: Auracast发射端UI实现
 */
public class AuracastTransmitterFragment extends DeviceControlFragment {

    /**
     * 操作延时
     */
    private static final long OPERATION_DELAY = 300L;

    /**
     * 配置设备功能
     */
    private static final int MSG_CONFIGURE_SETTINGS = 0x3510;

    /**
     * Auracast发射端功能逻辑实现
     */
    public static AuracastTransmitterViewModel viewModel;
    /**
     * Auracast发射端UI实现
     */
    private FragmentAuracastTransmitterBinding binding;

    /**
     * 输入广播名弹窗
     */
    private InputNameDialog inputNameDialog;

    /**
     * 提示框
     */
    private Jl_Dialog tipsDialog;

    /**
     * 保存二维码弹窗
     */
    private SaveQRCodeDialog saveQRCodeDialog;

    /**
     * UI处理
     */
    private final Handler uiHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_CONFIGURE_SETTINGS) {
            int op = msg.arg1;
            if (op == AuracastAssistantViewModel.OP_SET_TRANSMIT_POWER) {
                int value = msg.arg2;
                viewModel.adjustTransmitPower(value);
            }
        }
        return true;
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuracastTransmitterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        BluetoothDevice device = null == bundle ? null : (BluetoothDevice) bundle.getParcelable(SConstant.KEY_BLUETOOTH_DEVICE);
        if (null == device) {
            finish();
            return;
        }
        releaseViewModel();
        viewModel = new ViewModelProvider(this).get(AuracastTransmitterViewModel.class);
        initUI();
        addObserver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissInputNameDialog();
        dismissTipsDialog();
        dismissSaveQrCodeDialog();
        uiHandler.removeCallbacksAndMessages(null);
        releaseViewModel();
    }

    private void releaseViewModel() {
        if (null == viewModel) return;
        viewModel.release();
        viewModel = null;
    }

    private void initUI() {
        hideTopBar();

        if (requireActivity() instanceof BaseActivity) {
            ((BaseActivity) requireActivity()).setCustomBackPress(() -> {
                exit();
                return true;
            });
        }

        binding.viewToolBar.tvTitle.setText(getString(R.string.configure_auracast_broadcast));
        binding.viewToolBar.tvLeft.setOnClickListener(v -> exit());

        //设备设置
        binding.viewVolume.sbProgress.setMax(100);
        binding.viewTransmitPower.sbProgress.setMax(10);
        //广播名
        UIHelper.updateItemSettingsTextUI(binding.viewBroadcastName, getString(R.string.broadcast_name), "", true, false, v -> {
            final TransmitterSettings settings = viewModel.getSettingsInfo();
            if (null == settings) return; //未读取配置，不允许操作
            showInputNameDialog(settings.getBroadcastName());
        });
        //音量
        UIHelper.updateItemSettingsProgressUI(binding.viewVolume, getString(R.string.volume), 0, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                UIHelper.updateItemSettingsProgressUI(binding.viewVolume, null, progress, null);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        //发射功率
        UIHelper.updateItemSettingsProgressUI(binding.viewTransmitPower, getString(R.string.transmit_power), 0, "0",
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        UIHelper.updateItemSettingsProgressUI(binding.viewTransmitPower, null, progress, String.valueOf(progress), null);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        uiHandler.removeMessages(MSG_CONFIGURE_SETTINGS);
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        //操作延时300ml, 避免频繁设置广播功耗
                        int progress = seekBar.getProgress();
                        uiHandler.removeMessages(MSG_CONFIGURE_SETTINGS);
                        uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_CONFIGURE_SETTINGS, AuracastAssistantViewModel.OP_SET_TRANSMIT_POWER,
                                progress), OPERATION_DELAY);
                    }
                });

        /// 基础配置
        // 广播配置
        UIHelper.updateItemSettingsTextUI(binding.viewBroadcastConfiguration, getString(R.string.broadcast_configuration), "", true, false,
                v -> {
                    //暂不支持
                });
        // 密钥开关
        UIHelper.updateItemSettingsSwitchUI(binding.viewEncryptionSettings, getString(R.string.encryption_settings), 0, false, (buttonView, isChecked) -> {
            updateEncryptionSettingsUI(isChecked);
            final TransmitterSettings settings = viewModel.getSettingsInfo();
            if (null == settings) return; //未读取配置，不允许操作
            boolean isEncryption = viewModel.isEncryption();
            if (isEncryption != isChecked) {
                byte[] broadcastCode = null;
                EncryptionSettings encryptionSettings = settings.getEncryptionSettings();
                if (null != encryptionSettings && !encryptionSettings.isInValidBroadcastCode()) {
                    broadcastCode = encryptionSettings.getBroadcastCode();
                }
                if (broadcastCode == null) {
                    broadcastCode = new byte[16];
                }
                viewModel.setEncryptionSettings(new EncryptionSettings(isChecked, broadcastCode));
            }
        });
        // 密钥设置
        UIHelper.updateItemSettingsTextUI(binding.viewBroadcastCode, getString(R.string.broadcast_code), "", true, false, v -> {
            if (!binding.viewEncryptionSettings.switchBtn.isChecked()) return;
            OpResult<Object> opResult = viewModel.opResultMLD.getValue();
            if (opResult != null && opResult.getOp() == AuracastAssistantViewModel.OP_SET_ENCRYPTION_SETTINGS) {
                viewModel.opResultMLD.setValue(null); //进入设置界面之前，清空上一次操作结果
            }
            ContentActivity.startActivity(requireContext(), SetBroadcastCodeFragment.class.getCanonicalName(), getString(R.string.broadcast_code));
        });
        updateEncryptionSettingsUI(false);
        // 音频格式
        UIHelper.updateItemSettingsTextUI(binding.viewAudioFormat, getString(R.string.audio_format), "", true, false, v -> {
            OpResult<Object> opResult = viewModel.opResultMLD.getValue();
            if (opResult != null && opResult.getOp() == AuracastAssistantViewModel.OP_SET_AUDIO_FORMAT) {
                viewModel.opResultMLD.setValue(null); //进入设置界面之前，清空上一次操作结果
            }
            ContentActivity.startActivity(requireContext(), AudioFormatFragment.class.getCanonicalName(), getString(R.string.audio_format));
        });
        /// 安全设置
        // 密码
        UIHelper.updateItemSettingsTextUI(binding.viewLoginPassword, getString(R.string.password), "", true, false, v -> {
            ContentActivity.startActivity(requireContext(), ModifyLoginPwdFragment.class.getCanonicalName(), getString(R.string.password));
        });

        //生成二维码
        binding.btnCreateQrCode.setOnClickListener(v -> {
            final TransmitterSettings settings = viewModel.getSettingsInfo();
            if (null == settings) return; //未读取配置，不允许操作
            final EncryptionSettings encryptionSettings = settings.getEncryptionSettings();
            String password;
            try {
                password = null == encryptionSettings ? "" : !encryptionSettings.isEnable() ? "" :
                        encryptionSettings.isInValidBroadcastCode() ? "" : new String(encryptionSettings.getBroadcastCode());
            } catch (Exception e) {
                password = "";
            }
            if (!InputCodeDialog.isValidBroadcastCode(password)) {
                showTips(getString(R.string.pwd_len_err_tips));
                return;
            }
            showSaveQrCodeDialog(settings.getBroadcastName(), password);
        });
    }

    private void addObserver() {
        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (!BluetoothUtil.deviceEquals(deviceConnection.getDevice(), viewModel.getDevice()))
                return;
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        viewModel.transmitterSettingsMLD.observe(getViewLifecycleOwner(), this::updateTransmitterSettingsUI);
        viewModel.opResultMLD.observe(getViewLifecycleOwner(), opResult -> {
            if (opResult.isSuccess()) {
                if (opResult.getOp() == AuracastAssistantViewModel.OP_REBOOT) {
                    viewModel.disconnectDevice();
                }
                return;
            }
            showTips(CommonUtil.formatString("%s\n%s : %s, %s", getString(R.string.operation_failed, viewModel.getOpString(opResult.getOp())),
                    getString(R.string.error_code), CommonUtil.formatInt(opResult.getCode()), opResult.getMessage()));
        });
    }

    private void exit() {
        if (viewModel.isSettingsChange) { //配置被修改
            showTipsDialog();
            return;
        }
        finish();
    }

    private void updateEncryptionSettingsUI(boolean isEncryption) {
        binding.viewEncryptionSettings.switchBtn.setCheckedNoEvent(isEncryption);

        binding.viewBroadcastCode.getRoot().setClickable(isEncryption);
        binding.viewBroadcastCode.tvItemSettingsName.setTextColor(ContextCompat.getColor(requireContext(), isEncryption ? R.color.color_text
                : R.color.gray_4D000000));
    }

    private void updateTransmitterSettingsUI(TransmitterSettings settings) {
        if (isInvalid() || null == settings) return;
        if (settings.hasType(TransmitterSettings.TYPE_BROADCAST_NAME)) {
            UIHelper.updateItemSettingsTextUI(binding.viewBroadcastName, null, settings.getBroadcastName(), true, false, null);
        }
        if (settings.hasType(TransmitterSettings.TYPE_ENCRYPTION_SETTINGS)) {
            boolean isEncryption = settings.isEncryption();
            updateEncryptionSettingsUI(isEncryption);
        }
        if (settings.hasType(TransmitterSettings.TYPE_AUDIO_FORMAT)) {
            AudioFormat audioFormat = AudioFormatMap.getInstance().findAudioFormatByID(settings.getAudioFormat());
            if (audioFormat != null) {
                UIHelper.updateItemSettingsTextUI(binding.viewAudioFormat, null, audioFormat.getName(), true, false, null);
            }
        }
        if (settings.hasType(TransmitterSettings.TYPE_TRANSMIT_POWER)) {
            int power = settings.getTransmitPower();
            if (power > 0 && power <= 10) {
                UIHelper.updateItemSettingsProgressUI(binding.viewTransmitPower, null, power, String.valueOf(power), null);
            }
        }
    }

    private void showInputNameDialog(String broadcastName) {
        if (isInvalid()) return;
        dismissInputNameDialog();
        new InputNameDialog.Builder()
                .setBroadcastName(broadcastName)
                .setCallback(result -> viewModel.setBroadcastName(result))
                .build().show(getChildFragmentManager(), InputNameDialog.class.getSimpleName());
    }

    private void dismissInputNameDialog() {
        if (isInvalid() || null == inputNameDialog) return;
        if (inputNameDialog.isShow()) {
            inputNameDialog.dismiss();
        }
        inputNameDialog = null;
    }

    private void showTipsDialog() {
        if (isInvalid()) return;
        dismissTipsDialog();
        tipsDialog = Jl_Dialog.builder()
                .title(getString(R.string.dialog_tips))
                .content(getString(R.string.reboot_tips))
                .cancel(false)
                .left(getString(R.string.confirm))
                .leftClickListener((v, dialogFragment) -> {
                    dismissTipsDialog();
                    viewModel.reboot();
                }).build();
        tipsDialog.show(getChildFragmentManager(), "TipsDialog");
    }

    private void dismissTipsDialog() {
        if (isInvalid()) return;
        if (null == tipsDialog) return;
        if (tipsDialog.isShow()) {
            tipsDialog.dismiss();
        }
        tipsDialog = null;
    }

    private void showSaveQrCodeDialog(String name, String password) {
        if (isInvalid()) return;
        dismissSaveQrCodeDialog();
        AuracastQRCode auracastQRCode = new AuracastQRCode(name, password);
        new SaveQRCodeDialog.Builder(viewModel.getMac(),
                name, password, auracastQRCode.generateQRContent())
                .setTips(getString(R.string.scan_qr_code_tips))
                .setCallback(result -> {

                }).build().show(getChildFragmentManager(), SaveQRCodeDialog.class.getSimpleName());
    }

    private void dismissSaveQrCodeDialog() {
        if (isInvalid() || null == saveQRCodeDialog) return;
        if (saveQRCodeDialog.isShow()) {
            saveQRCodeDialog.dismiss();
        }
        saveQRCodeDialog = null;
    }
}