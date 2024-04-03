package com.jieli.btsmart.ui.test;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.component.ActivityManager;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;


public class TestConfigurationActivity extends Jl_BaseActivity {

    private CheckBox cbUseDeviceAuth;
    private CheckBox cbShowDialog;
    private CheckBox cbLocalOta;
    private EditText edRssi;
    private TextView tvLocalOtaDesc;
    private ImageView ivCommonTopBarLeftImg;
    private ImageView ivCommonTopBarRightImg;
    private TextView tvCommonTopBarTitle;

    private boolean isStartReStartApp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SystemUtil.setImmersiveStateBar(getWindow(), true);
        setContentView(R.layout.activity_test_configuration);
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isStartReStartApp = false;
    }


    public void updateTopBar(String title, int leftRes, View.OnClickListener leftListener, int rightRes, View.OnClickListener rightListener) {
        if (tvCommonTopBarTitle != null && title != null) {
            tvCommonTopBarTitle.setText(title);
        }
        if (ivCommonTopBarLeftImg != null) {
            if (leftRes != 0) {
                ivCommonTopBarLeftImg.setVisibility(View.VISIBLE);
                ivCommonTopBarLeftImg.setImageResource(leftRes);
            } else {
                ivCommonTopBarLeftImg.setVisibility(View.GONE);
            }
            if (leftListener != null) {
                ivCommonTopBarLeftImg.setOnClickListener(leftListener);
            }
        }
        if (ivCommonTopBarRightImg != null) {
            if (rightRes != 0) {
                ivCommonTopBarRightImg.setVisibility(View.VISIBLE);
                ivCommonTopBarRightImg.setImageResource(rightRes);
            } else {
                ivCommonTopBarRightImg.setVisibility(View.GONE);
            }
            if (rightListener != null) {
                ivCommonTopBarRightImg.setOnClickListener(rightListener);
            }
        }
    }

    private void initView() {
        cbUseDeviceAuth = findViewById(R.id.cb_test_configuration_use_device_auth);
        cbShowDialog = findViewById(R.id.cb_test_configuration_show_dialog);
        cbLocalOta = findViewById(R.id.cb_test_configuration_local_ota);
        edRssi = findViewById(R.id.ed_test_configuration_rssi);
        tvLocalOtaDesc = findViewById(R.id.tv_test_configuration_local_ota_desc);
        ivCommonTopBarLeftImg = findViewById(R.id.iv_common_top_bar_left_img);
        ivCommonTopBarRightImg = findViewById(R.id.iv_common_top_bar_right_img);
        tvCommonTopBarTitle = findViewById(R.id.tv_common_top_bar_title);

        findViewById(R.id.tv_test_configuration_test_custom_cmd).setOnClickListener(v -> toTestCustomCmdActivity());
        findViewById(R.id.tv_test_configuration_save_configuration).setOnClickListener(v -> saveTestConfiguration());


        updateTopBar(getString(R.string.test_configuration), R.drawable.ic_back_black, v -> finish(), 0, null);
        String packageName = getApplicationContext().getPackageName();
        tvLocalOtaDesc.setText(getString(R.string.local_ota_desc, packageName, packageName, packageName));
        boolean isUseDeviceAuth = PreferencesHelper.getSharedPreferences(getApplicationContext()).getBoolean(SConstant.KEY_USE_DEVICE_AUTH, SConstant.IS_USE_DEVICE_AUTH);
        cbUseDeviceAuth.setChecked(isUseDeviceAuth);
        boolean isAllowShowBtDialog = PreferencesHelper.getSharedPreferences(getApplicationContext()).getBoolean(SConstant.KEY_ALLOW_SHOW_BT_DIALOG, SConstant.ALLOW_SHOW_BT_DIALOG);
        cbShowDialog.setChecked(isAllowShowBtDialog);
        boolean isLocalOTA = PreferencesHelper.getSharedPreferences(getApplicationContext()).getBoolean(SConstant.KEY_LOCAL_OTA_TEST, SConstant.IS_LOCAL_OTA_TEST);
        cbLocalOta.setChecked(isLocalOTA);
        int rssi = PreferencesHelper.getSharedPreferences(getApplicationContext()).getInt(SConstant.KEY_BLE_ADV_RSSI_LIMIT, SConstant.BLE_ADV_RSSI_LIMIT);
        edRssi.setText(String.valueOf(rssi));
        edRssi.addTextChangedListener(mTextWatcher);
        cbShowDialog.setOnCheckedChangeListener(mOnCheckedChangeListener);
        cbLocalOta.setOnCheckedChangeListener(mOnCheckedChangeListener);
    }

    private void toTestCustomCmdActivity() {
        startActivity(new Intent(this.getApplicationContext(), TestCustomCmdActivity.class));
        finish();
    }

    private void saveTestConfiguration() {
        if (!isStartReStartApp) {
            boolean isUseDeviceAuth = cbUseDeviceAuth.isChecked();
            boolean isAllowShowBtDialog = cbShowDialog.isChecked();
            boolean isLocalOTA = cbLocalOta.isChecked();
            int rssi = getIntValue(edRssi.getText().toString().trim());
            JL_Log.w(TAG, "saveTestConfiguration : rssi = " + rssi + ", isAllowShowBtDialog : " + isAllowShowBtDialog + ", isLocalOTA : " + isLocalOTA);
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_ALLOW_SHOW_BT_DIALOG, isAllowShowBtDialog);
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_LOCAL_OTA_TEST, isLocalOTA);
            PreferencesHelper.putIntValue(getApplicationContext(), SConstant.KEY_BLE_ADV_RSSI_LIMIT, rssi);
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_USE_DEVICE_AUTH, isUseDeviceAuth);
            ToastUtil.showToastLong(R.string.save_configuration_success);
            //保存配置，退出APP
            SystemUtil.restartApp(getApplicationContext());
            new Handler().postDelayed(() -> {
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

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = (buttonView, isChecked) -> {
       /* if (buttonView == cbShowDialog) {
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_ALLOW_SHOW_BT_DIALOG, isChecked);
        } else if (buttonView == cbLocalOta) {
            PreferencesHelper.putBooleanValue(getApplicationContext(), SConstant.KEY_LOCAL_OTA_TEST, isChecked);
        }*/
    };

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
