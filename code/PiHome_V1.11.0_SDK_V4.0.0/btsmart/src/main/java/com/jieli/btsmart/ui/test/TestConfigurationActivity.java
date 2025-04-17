package com.jieli.btsmart.ui.test;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.BuildConfig;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.databinding.ActivityTestConfigurationBinding;
import com.jieli.component.ActivityManager;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;


public class TestConfigurationActivity extends Jl_BaseActivity {
    private ActivityTestConfigurationBinding mBinding;

    private boolean isStartReStartApp;
    private final Handler mUIHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        mBinding = ActivityTestConfigurationBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUIHandler.removeCallbacksAndMessages(null);
        isStartReStartApp = false;
    }

    private void initView() {
        mBinding.viewTopBar.tvCommonTopBarTitle.setText(getString(R.string.test_configuration));
        mBinding.viewTopBar.ivCommonTopBarLeftImg.setVisibility(View.VISIBLE);
        mBinding.viewTopBar.ivCommonTopBarLeftImg.setImageResource(R.drawable.ic_back_black);
        mBinding.viewTopBar.ivCommonTopBarLeftImg.setOnClickListener(v -> finish());

        mBinding.tvTestCustomCmd.setOnClickListener(v -> toTestCustomCmdActivity());
        mBinding.tvSaveConfiguration.setOnClickListener(v -> saveTestConfiguration());

        String packageName = getApplicationContext().getPackageName();
        mBinding.tvLocalOtaDesc.setText(getString(R.string.local_ota_desc, packageName, packageName, packageName));
        boolean isUseSaveLog = PreferencesHelper.getSharedPreferences(getApplicationContext()).getBoolean(SConstant.KEY_USE_SAVE_LOG, BuildConfig.DEBUG ? true : SConstant.IS_USE_SAVE_LOG);
        mBinding.cbUseSaveLog.setChecked(isUseSaveLog);
        boolean isUseDeviceAuth = PreferencesHelper.getSharedPreferences(getApplicationContext()).getBoolean(SConstant.KEY_USE_DEVICE_AUTH, SConstant.IS_USE_DEVICE_AUTH);
        mBinding.cbUseDeviceAuth.setChecked(isUseDeviceAuth);
        boolean isAllowShowBtDialog = PreferencesHelper.getSharedPreferences(getApplicationContext()).getBoolean(SConstant.KEY_ALLOW_SHOW_BT_DIALOG, SConstant.ALLOW_SHOW_BT_DIALOG);
        mBinding.cbShowDialog.setChecked(isAllowShowBtDialog);
        boolean isLocalOTA = PreferencesHelper.getSharedPreferences(getApplicationContext()).getBoolean(SConstant.KEY_LOCAL_OTA_TEST, SConstant.IS_LOCAL_OTA_TEST);
        mBinding.cbLocalOta.setChecked(isLocalOTA);
        int rssi = PreferencesHelper.getSharedPreferences(getApplicationContext()).getInt(SConstant.KEY_BLE_ADV_RSSI_LIMIT, SConstant.BLE_ADV_RSSI_LIMIT);
        mBinding.etRssi.setText(String.valueOf(rssi));
        mBinding.etRssi.addTextChangedListener(mTextWatcher);
    }

    private void toTestCustomCmdActivity() {
        startActivity(new Intent(this.getApplicationContext(), TestCustomCmdActivity.class));
        finish();
    }

    private void saveTestConfiguration() {
        if (!isStartReStartApp) {
            boolean isUseSaveLog = mBinding.cbUseSaveLog.isChecked();
            boolean isUseDeviceAuth = mBinding.cbUseDeviceAuth.isChecked();
            boolean isAllowShowBtDialog = mBinding.cbShowDialog.isChecked();
            boolean isLocalOTA = mBinding.cbLocalOta.isChecked();
            int rssi = getIntValue(mBinding.etRssi.getText().toString().trim());
            JL_Log.w(TAG, "saveTestConfiguration : rssi = " + rssi + ", isAllowShowBtDialog : " + isAllowShowBtDialog + ", isLocalOTA : " + isLocalOTA);
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
            if (TextUtils.isDigitsOnly(value)) {
                int rssi = Integer.parseInt(value);
                if (rssi > 0 || rssi < -100) {
                    ToastUtil.showToastShort(R.string.rssi_over_limit);
                }
            }
        }
    };
}
