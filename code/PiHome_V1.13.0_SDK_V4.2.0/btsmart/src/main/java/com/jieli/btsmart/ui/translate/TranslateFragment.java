package com.jieli.btsmart.ui.translate;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonSyntaxException;
import com.jieli.bluetooth.bean.translation.TranslationMode;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.translation.TranslationModeInfo;
import com.jieli.btsmart.data.model.translation.ai_auth.AIAuthMessage;
import com.jieli.btsmart.databinding.FragmentTranslateBinding;
import com.jieli.btsmart.tool.ai.doubao.basic.AIConfig;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.qrcode.QRCodeScanActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.translate.language.SelectLanguageFragment;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.btsmart.ui.widget.TipDialog;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.TranslateUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ValueUtil;
import com.king.camera.scan.CameraScan;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

/**
 * TranslateFragment
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 翻译界面
 * @since 2025/5/27
 */
@RuntimePermissions
public class TranslateFragment extends DeviceControlFragment {

    public static final String KEY_TRANSLATION_MODE = "translation_mode";
    /**
     * 翻译功能逻辑实现
     */
    public static TranslateViewModel translateViewModel;

    private FragmentTranslateBinding mBinding;
    private TranslationModeAdapter mAdapter;

    private ActivityResultLauncher<Intent> scanQRCodeLauncher;
    /**
     * 需要进入模式信息
     */
    private TranslationMode enterMode;

    public static TranslateFragment newInstance() {
        return new TranslateFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentTranslateBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if (null == bundle) {
            finish();
            return;
        }
        final BluetoothDevice device = bundle.getParcelable(SConstant.KEY_BLUETOOTH_DEVICE);
        if (null == device) {
            finish();
            return;
        }
        scanQRCodeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                Intent data = o.getData();
                if (null != data) {
                    String result = data.getStringExtra(CameraScan.SCAN_RESULT);
                    if (!TextUtils.isEmpty(result)) {
                        try {
                            AIAuthMessage authMessage = AIConfig.gson.fromJson(result, AIAuthMessage.class);
                            if (authMessage.isValid() && null != authMessage.getDoubaoTranslationMessage() && null != authMessage.getDoubaoTTSMessage()) {
                                translateViewModel.updateAIAuthMessage(authMessage);
                                showTips(getString(R.string.add_auth_message_success));
                                return;
                            }
                        } catch (JsonSyntaxException ignored) {

                        }
                    }
                }
                showTips(getString(R.string.scan_code_tips));
            }
        });
        releaseViewModel();
        translateViewModel = new TranslateViewModel(device);
        initUI();
        addObserver();
    }

    @Override
    public void onDestroyView() {
        final TranslateViewModel viewModel = translateViewModel;
        if (viewModel != null) {
            releaseViewModel();
        }
        super.onDestroyView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        TranslateFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.RECORD_AUDIO})
    public void onRecordPermissionGrant(TranslationMode mode) {
        disPermissionTipsDialog();
        tryToEnterTranslationMode(mode);
    }

    @OnShowRationale({Manifest.permission.RECORD_AUDIO})
    public void onRecordPermissionShowRationale(PermissionRequest request) {
        disPermissionTipsDialog();
        if (null != request) request.proceed();
    }

    @OnPermissionDenied({Manifest.permission.RECORD_AUDIO})
    public void onRecordPermissionDenied() {
        disPermissionTipsDialog();
        enterMode = null;
        UIHelper.showAppSettingDialog(TranslateFragment.this, getString(R.string.permissions_tips_02) + getString(R.string.permission_mic));
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCameraPermissionGrant() {
        disPermissionTipsDialog();
        goToScanQRCode();
    }

    @OnShowRationale({Manifest.permission.CAMERA})
    public void onCameraPermissionShowRationale(PermissionRequest request) {
        disPermissionTipsDialog();
        if (null != request) request.proceed();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA})
    public void onCameraPermissionDenied() {
        disPermissionTipsDialog();
        UIHelper.showAppSettingDialog(TranslateFragment.this, getString(R.string.camera_permission_denied_tips));
    }

    @NeedsPermission({Manifest.permission.READ_PHONE_STATE})
    public void onReadPhoneStatePermissionGrant(TranslationMode mode) {
        disPermissionTipsDialog();
        goToTranslationModeFragment(mode);
    }

    @OnShowRationale({Manifest.permission.READ_PHONE_STATE})
    public void onReadPhoneStatePermissionShowRationale(PermissionRequest request) {
        disPermissionTipsDialog();
        if (null != request) request.proceed();
    }

    @OnPermissionDenied({Manifest.permission.READ_PHONE_STATE})
    public void onReadPhoneStatePermissionDenied() {
        JL_Log.d(TAG, "onReadPhoneStatePermissionDenied", "" + enterMode);
        onReadPhoneStatePermissionGrant(enterMode);
    }

    private void releaseViewModel() {
        if (null != translateViewModel) {
            translateViewModel.release();
            translateViewModel = null;
        }
    }

    private void initUI() {
        mAdapter = new TranslationModeAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            if (!checkAIAuthMessage()) return;
            final TranslationModeInfo modeInfo = mAdapter.getItem(position);
            final TranslationMode mode = modeInfo.getMode();
            tryToEnterTranslationMode(mode);
        });
        mBinding.rvTranslationMode.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvTranslationMode.setAdapter(mAdapter);
        mBinding.rvTranslationMode.addItemDecoration(new CommonDecoration(requireContext(), RecyclerView.VERTICAL,
                ContextCompat.getColor(requireContext(), R.color.color_transparent), ValueUtil.dp2px(requireContext(), 10)));

        List<TranslationModeInfo> modeInfoList = new ArrayList<>();
        /// 通话翻译方案
        /// 如果支持【通话翻译立体声功能】，优先使用
        /// 反之，则使用【JLA_v2格式】方案
        // V4.2.0对应的固件版本，暂不支持，先隐藏【通话翻译】UI
        /*modeInfoList.add(new TranslationModeInfo(R.drawable.ic_call_translation, getString(R.string.call_translation),
                getString(R.string.call_translation_desc), translateViewModel.isSupportCallTranslationWithStereo() ?
                new TranslationMode(TranslationMode.MODE_CALL_TRANSLATION_WITH_STEREO, Constants.AUDIO_TYPE_OPUS, 2, 16000)
                : new TranslationMode(TranslationMode.MODE_CALL_TRANSLATION, Constants.AUDIO_TYPE_JLA_V2)));*/
        modeInfoList.add(new TranslationModeInfo(R.drawable.ic_face_to_face_translation, getString(R.string.face_to_face_translation),
                getString(R.string.face_to_face_translation_desc), new TranslationMode(TranslationMode.MODE_FACE_TO_FACE_TRANSLATION,
                Constants.AUDIO_TYPE_OPUS)));
        modeInfoList.add(new TranslationModeInfo(R.drawable.ic_simultaneous_interpreting, getString(R.string.simultaneous_interpreting),
                getString(R.string.simultaneous_interpreting_desc), new TranslationMode(TranslationMode.MODE_RECORDING_TRANSLATION,
                Constants.AUDIO_TYPE_OPUS).setRecordingStrategy(TranslationMode.STRATEGY_CUSTOM_RECORDING)));
        mAdapter.setList(modeInfoList);
    }

    private void addObserver() {
        translateViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> {
            if (BluetoothUtil.deviceEquals(connection.getDevice(), translateViewModel.getDevice())
                    && connection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
    }

    private void goToTranslationModeFragment(TranslationMode mode) {
        if (isInvalid() || null == mode) return;
        int translationMode = TranslateUtil.getTranslationMode(mode);
        if (translationMode == TranslationMode.MODE_CALL_TRANSLATION) {
            if (!translateViewModel.isInCalling()) {
                showTips(getString(R.string.tips_no_call));
                return;
            }
            if (!translateViewModel.isTwsConnected()) {
                showTipsDialog(getString(R.string.tips_wear_earphones), getString(R.string.tips_wear_both_earphones));
                return;
            }
        }
        String title = "";
        switch (translationMode) {
            case TranslationMode.MODE_CALL_TRANSLATION:
                title = getString(R.string.call_translation);
                break;
            case TranslationMode.MODE_FACE_TO_FACE_TRANSLATION:
                title = getString(R.string.face_to_face_translation);
                break;
            case TranslationMode.MODE_RECORDING_TRANSLATION:
                title = getString(R.string.simultaneous_interpreting);
                break;
        }
        if (!TextUtils.isEmpty(title)) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_TRANSLATION_MODE, mode);
            ContentActivity.startActivity(requireContext(), SelectLanguageFragment.class.getCanonicalName(), title, bundle);
        }
        enterMode = null;
    }

    private boolean checkAIAuthMessage() {
        AIAuthMessage authMessage = translateViewModel.getAIAuthMessage();
        if (null == authMessage || !authMessage.isValid()) {
            String content = null == authMessage ? getString(R.string.missing_translation_auth_msg)
                    : getString(R.string.translation_auth_msg_expired);
            showScanQRCodeTipsDialog(content);
            return false;
        }
        return true;
    }

    private void goToScanQRCode() {
        if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.CAMERA)) {
            showPermissionTipsDialog(getString(R.string.camera_permission_desc));
            TranslateFragmentPermissionsDispatcher.onCameraPermissionGrantWithPermissionCheck(this);
            return;
        }
        scanQRCodeLauncher.launch(new Intent(requireContext(), QRCodeScanActivity.class));
    }

    private void tryToEnterTranslationMode(@NonNull TranslationMode mode) {
        enterMode = mode;
        if (!PermissionUtil.isHasPermission(requireContext(), Manifest.permission.RECORD_AUDIO)) {
            showPermissionTipsDialog(getString(R.string.record_audio_permission));
            TranslateFragmentPermissionsDispatcher.onRecordPermissionGrantWithPermissionCheck(this, mode);
            return;
        }
        if (!PermissionUtil.isHasPermission(requireContext(), Manifest.permission.READ_PHONE_STATE)) {
            showPermissionTipsDialog(getString(R.string.read_phone_state_permission));
            TranslateFragmentPermissionsDispatcher.onReadPhoneStatePermissionGrantWithPermissionCheck(this, mode);
            return;
        }
        goToTranslationModeFragment(mode);
    }

    private void showTipsDialog(String title, String content) {
        if (isInvalid()) return;
        new TipDialog.Builder()
                .setTitle(title)
                .setContent(content)
                .setCancelable(true)
                .setLeftText(getString(R.string.i_know_it_2))
                .setOnTipDialogListener(new TipDialog.OnTipDialogListener() {
                    @Override
                    public void onDismiss(TipDialog dialog) {

                    }

                    @Override
                    public void onRightBtnClick(TipDialog dialog) {
                        dialog.dismiss();
                    }

                    @Override
                    public void onLeftBtnClick(TipDialog dialog) {
                        dialog.dismiss();
                    }
                }).create().show(getChildFragmentManager(), TipDialog.class.getSimpleName());
    }

    private void showScanQRCodeTipsDialog(String content) {
        if (isInvalid()) return;
        new TipDialog.Builder()
                .setTitle(getString(R.string.dialog_tips))
                .setContent(content)
                .setCancelable(false)
                .setLeftText(getString(R.string.cancel))
                .setLeftColor(ContextCompat.getColor(requireContext(), R.color.gray_66000000))
                .setRightText(getString(R.string.confirm))
                .setRightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                .setOnTipDialogListener(new TipDialog.OnTipDialogListener() {
                    @Override
                    public void onDismiss(TipDialog dialog) {

                    }

                    @Override
                    public void onRightBtnClick(TipDialog dialog) {
                        dialog.dismiss();
                        goToScanQRCode();
                    }

                    @Override
                    public void onLeftBtnClick(TipDialog dialog) {
                        dialog.dismiss();
                    }
                }).create().show(getChildFragmentManager(), TipDialog.class.getSimpleName());
    }
}