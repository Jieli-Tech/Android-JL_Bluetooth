package com.jieli.btsmart.ui.test;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.BuildConfig;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.databinding.ActivityTestConfigurationBinding;
import com.jieli.btsmart.databinding.ItemSettingsBinding;
import com.jieli.btsmart.databinding.ItemSettingsSwitchBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.test.log.LogFileFragment;
import com.jieli.btsmart.ui.widget.DevicePopDialog.DevicePopDialogFilter;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.ActivityManager;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 测试配置界面
 * @since 2025/9/19
 */
public class TestConfigurationActivity extends BaseActivity implements DevicePopDialogFilter.IgnoreFilter {
    private ActivityTestConfigurationBinding mBinding;

    private boolean isStartReStartApp;
    private final Handler mUIHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        mBinding = ActivityTestConfigurationBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setWindowStatus();
        DevicePopDialogFilter.getInstance().addIgnoreFilter(this);
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DevicePopDialogFilter.getInstance().removeIgnoreFilter(this);
        mUIHandler.removeCallbacksAndMessages(null);
        isStartReStartApp = false;
    }

    private void initView() {
        mBinding.viewTopBar.tvTitle.setText(getString(R.string.test_configuration));
        mBinding.viewTopBar.tvLeft.setOnClickListener(v -> finish());
        UIHelper.show(mBinding.viewTopBar.tvRight);
        mBinding.viewTopBar.tvRight.setText(getString(R.string.save_configuration));
        mBinding.viewTopBar.tvRight.setOnClickListener(v -> saveTestConfiguration());

        boolean isUseSaveLog = PreferencesHelper.getSharedPreferences(getApplicationContext())
                .getBoolean(SConstant.KEY_USE_SAVE_LOG, BuildConfig.DEBUG || SConstant.IS_USE_SAVE_LOG);
        updateSwitchUI(mBinding.viewSaveLog, getString(R.string.use_save_log), isUseSaveLog,
                (buttonView, isChecked) -> updateSaveLogUI(isChecked));
        updateSaveLogUI(isUseSaveLog);

        boolean isUseDeviceAuth = PreferencesHelper.getSharedPreferences(getApplicationContext())
                .getBoolean(SConstant.KEY_USE_DEVICE_AUTH, SConstant.IS_USE_DEVICE_AUTH);
        updateSwitchUI(mBinding.viewUseDeviceAuth, getString(R.string.use_device_auth), isUseDeviceAuth, null);

        boolean isAllowShowBtDialog = PreferencesHelper.getSharedPreferences(getApplicationContext())
                .getBoolean(SConstant.KEY_ALLOW_SHOW_BT_DIALOG, SConstant.ALLOW_SHOW_BT_DIALOG);
        updateSwitchUI(mBinding.viewShowDialog, getString(R.string.support_show_product_dialog), isAllowShowBtDialog,
                (buttonView, isChecked) -> updateInputRSSIUI(isChecked));
        updateInputRSSIUI(isAllowShowBtDialog);

        boolean isLocalOTA = PreferencesHelper.getSharedPreferences(getApplicationContext())
                .getBoolean(SConstant.KEY_LOCAL_OTA_TEST, SConstant.IS_LOCAL_OTA_TEST);
        updateSwitchUI(mBinding.viewLocalOta, getString(R.string.support_local_ota), isLocalOTA,
                (buttonView, isChecked) -> updateLocalOTAUI(isChecked));
        updateLocalOTAUI(isLocalOTA);

        mBinding.viewCustomCommand.getRoot().setBackgroundResource(R.drawable.bg_c8_white_shape);
        mBinding.viewCustomCommand.tvItemSettingsName.setTextSize(15);
        mBinding.viewCustomCommand.tvItemSettingsName.setTypeface(Typeface.DEFAULT_BOLD);
        updateTextUI(mBinding.viewCustomCommand, getString(R.string.test_custom_cmd), "", v -> goToTestCustomCmdActivity());
    }

    private void updateSwitchUI(@NonNull ItemSettingsSwitchBinding binding, String title,
                                boolean isCheck, CompoundButton.OnCheckedChangeListener listener) {
        binding.tvTitle.setText(title);
        UIHelper.gone(binding.ivImage);
        binding.switchBtn.setCheckedNoEvent(isCheck);
        if (null != listener) {
            binding.switchBtn.setOnCheckedChangeListener(listener);
        }
    }

    private void updateTextUI(@NonNull ItemSettingsBinding binding, String title, String value, View.OnClickListener listener) {
        binding.tvItemSettingsName.setText(title);
        binding.tvItemSettingsValue.setText(value);
        UIHelper.gone(binding.viewItemSettingsLine);
        if (null != listener) {
            binding.getRoot().setOnClickListener(listener);
        }
    }

    private void updateSaveLogUI(boolean isShow) {
        if (!isShow) {
            UIHelper.gone(mBinding.viewLogPath.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewLogPath.getRoot());
        String logPath = JL_Log.getSaveLogPath(this).replaceAll("/storage/emulated/0", "");
        updateTextUI(mBinding.viewLogPath, getString(R.string.log_file), logPath, v -> goToLogFileFragment());
    }

    private void updateInputRSSIUI(boolean isShow) {
        if (!isShow) {
            mBinding.viewInputRssi.etRssi.removeTextChangedListener(mTextWatcher);
            UIHelper.gone(mBinding.viewInputRssi.getRoot());
            return;
        }
        UIHelper.show(mBinding.viewInputRssi.getRoot());
        mBinding.viewInputRssi.etRssi.addTextChangedListener(mTextWatcher);
        int rssi;
        String value = mBinding.viewInputRssi.etRssi.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            rssi = PreferencesHelper.getSharedPreferences(getApplicationContext())
                    .getInt(SConstant.KEY_BLE_ADV_RSSI_LIMIT, SConstant.BLE_ADV_RSSI_LIMIT);
        } else {
            rssi = getIntValue(value);
        }
        mBinding.viewInputRssi.etRssi.setText(String.valueOf(rssi));
    }

    private void updateLocalOTAUI(boolean isShow) {
        if (!isShow) {
            UIHelper.gone(mBinding.tvLocalOtaTips);
            return;
        }
        UIHelper.show(mBinding.tvLocalOtaTips);
        String packageName = getApplicationContext().getPackageName();
        String otaTips = getString(R.string.local_ota_desc, packageName, packageName);
        mBinding.tvLocalOtaTips.setText(otaTips);
    }

    private void goToLogFileFragment() {
        ContentActivity.startActivity(this, LogFileFragment.class.getCanonicalName(), getString(R.string.log_file));
    }

    private void goToTestCustomCmdActivity() {
        if (!RCSPController.getInstance().isDeviceConnected()) {
            showTips(getString(R.string.first_connect_device));
            return;
        }
        startActivity(new Intent(this.getApplicationContext(), TestCustomCmdActivity.class));
        finish();
    }

    private void saveTestConfiguration() {
        if (!isStartReStartApp) {
            boolean isUseSaveLog = mBinding.viewSaveLog.switchBtn.isChecked();
            boolean isUseDeviceAuth = mBinding.viewUseDeviceAuth.switchBtn.isChecked();
            boolean isAllowShowBtDialog = mBinding.viewShowDialog.switchBtn.isChecked();
            boolean isLocalOTA = mBinding.viewLocalOta.switchBtn.isChecked();
            int rssi = getIntValue(mBinding.viewInputRssi.etRssi.getText().toString().trim());
            JL_Log.w(TAG, "saveTestConfiguration", "isUseSaveLog : " + isUseSaveLog
                    + ",\nisUseDeviceAuth = " + isUseDeviceAuth
                    + ",\nisAllowShowBtDialog : " + isAllowShowBtDialog + ", rssi = " + rssi
                    + ",\nisLocalOTA : " + isLocalOTA);
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_ALLOW_SHOW_BT_DIALOG, isAllowShowBtDialog);
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_LOCAL_OTA_TEST, isLocalOTA);
            PreferencesHelper.putIntValue(getApplicationContext(), SConstant.KEY_BLE_ADV_RSSI_LIMIT, rssi);
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_USE_DEVICE_AUTH, isUseDeviceAuth);
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_USE_SAVE_LOG, isUseSaveLog);

            ToastUtil.showToastLong(R.string.save_configuration_success);
            //保存配置，退出APP
            mUIHandler.postDelayed(() -> {
                SystemUtil.restartApp(getApplicationContext());
                finish();
                ActivityManager.getInstance().popAllActivity();
            }, 1000);
            isStartReStartApp = true;
        }
    }

    private int getIntValue(String value) {
        int rssi = SConstant.BLE_ADV_RSSI_LIMIT;
        try {
            int tempValue = Integer.parseInt(value);
            if (tempValue <= 0 && tempValue > -100) {
                rssi = tempValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rssi;
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String value = s.toString();
            if (!TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value)) {
                int rssi = Integer.parseInt(value);
                if (rssi > 0 || rssi < -100) {
                    mBinding.viewInputRssi.etRssi.setText("");
                    showTips(getString(R.string.rssi_over_limit));
                }
            }
        }
    };

    @Override
    public boolean shouldIgnore(BleScanMessage bleScanMessage) {
        return true;
    }
}
