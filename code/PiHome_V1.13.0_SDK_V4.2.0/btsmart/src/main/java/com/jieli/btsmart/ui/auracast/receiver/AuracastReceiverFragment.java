package com.jieli.btsmart.ui.auracast.receiver;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.auracast.AuracastBroadcast;
import com.jieli.bluetooth.bean.auracast.AuracastRecord;
import com.jieli.bluetooth.bean.command.auracast.response.ScanResponse;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.auracast.AuracastRecordHelper;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.auracast.AuracastQRCode;
import com.jieli.btsmart.data.model.auracast.AuracastRecordState;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.databinding.FragmentAuracastReceiverBinding;
import com.jieli.btsmart.ui.auracast.AuracastAssistantViewModel;
import com.jieli.btsmart.ui.qrcode.QRCodeScanActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.CommonDecoration;
import com.jieli.btsmart.ui.widget.dialog.InputCodeDialog;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_dialog.Jl_Dialog;
import com.king.camera.scan.CameraScan;
import com.mcxtzhang.swipemenulib.SwipeMenuLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

/**
 * AuracastReceiverFragment
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: Auracast接收端UI实现
 */
@RuntimePermissions
public class AuracastReceiverFragment extends DeviceControlFragment {

    /**
     * 空闲状态
     */
    private static final int RECORD_STATE_IDLE = 0;

    /**
     * 发现广播信息状态
     */
    private static final int RECORD_STATE_FOUND_BROADCAST = 1;

    /**
     * 收听广播状态
     */
    private static final int RECORD_STATE_LISTENING = 2;

    /**
     * 逻辑实现
     */
    private AuracastReceiverViewModel viewModel;
    /**
     * UI实现
     */
    private FragmentAuracastReceiverBinding binding;
    /**
     * 音频广播适配器
     */
    private AuracastBroadcastAdapter broadcastAdapter;
    /**
     * 历史记录适配器
     */
    private AuracastRecordStateAdapter recordAdapter;

    /**
     * 是否保持屏幕常亮
     */
    private boolean isKeepScreenOn;

    /**
     * 提示框
     */
    private Jl_Dialog tipsDialog;


    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK) return;
                Intent intent = result.getData();
                if (null == intent) return;
                String content = intent.getStringExtra(CameraScan.SCAN_RESULT);
                AuracastQRCode qrCode = AuracastQRCode.parseContent(content);
                if (null == qrCode) {
                    showTips(getString(R.string.qr_code_error_tips));
                    return;
                }
                viewModel.tryToSyncBroadcastByQrCode(qrCode);
            });


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuracastReceiverBinding.inflate(inflater, container, false);
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
        viewModel = new ViewModelProvider(this, new AuracastReceiverViewModel.Factory(device))
                .get(AuracastReceiverViewModel.class);
        initUI();
        addObserver();
        updateMyBroadcasts();
        viewModel.syncAuracastBroadcastState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dismissTipsDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AuracastReceiverFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCameraPermissionGrant() {
        disPermissionTipsDialog();
        qrCodeLauncher.launch(new Intent(requireContext(), QRCodeScanActivity.class));
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCameraPermissionShowRationale(PermissionRequest request) {
        disPermissionTipsDialog();
        if (null != request) request.proceed();
    }

    @NeedsPermission({Manifest.permission.CAMERA})
    public void onCameraPermissionDenied() {
        disPermissionTipsDialog();
        UIHelper.showAppSettingDialog(AuracastReceiverFragment.this, getString(R.string.camera_permission_denied_tips));
    }

    private void initUI() {
        hideTopBar();
        binding.viewToolBar.tvLeft.setOnClickListener(v -> finish(100));
        binding.viewToolBar.tvTitle.setText(getString(R.string.listen_auracast_broadcast));
        binding.viewToolBar.tvRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_scan_qr_code_black, 0);
        binding.viewToolBar.tvRight.setOnClickListener(v -> tryToScanQRCode());

        binding.sbtnBroadcast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (viewModel.getOperationBroadcast() != null) { //正在同步广播
                    binding.sbtnBroadcast.setCheckedNoEvent(false);
                    showTips(getString(R.string.syncing_pa));
                    return;
                }
                viewModel.startScan();
            } else {
                viewModel.stopScan();
            }
        });

        broadcastAdapter = new AuracastBroadcastAdapter();
        broadcastAdapter.setOnItemClickListener((adapter, view, position) -> {
            final AuracastBroadcast item = broadcastAdapter.getItem(position);
            if (item.equals(viewModel.getOperationBroadcast()) || item.equals(viewModel.getListeningBroadcast()))
                return;
            if (item.isEncrypted()) { //输入广播密钥
                showInputCodeDialog(item);
                return;
            }
            viewModel.addSource(item);
        });
        binding.rvUsableBroadcast.setAdapter(broadcastAdapter);
        binding.rvUsableBroadcast.addItemDecoration(new CommonDecoration(requireContext(),
                ContextCompat.getColor(requireContext(), R.color.color_transparent),
                RecyclerView.VERTICAL, ValueUtil.dp2px(requireContext(), 8)));

        recordAdapter = new AuracastRecordStateAdapter();
        recordAdapter.setOnItemClickListener((adapter, view, position) -> {
            final AuracastRecordState item = recordAdapter.getItem(position);
            int syncState = item.getSyncState();
            switch (syncState) {
                case StateCode.STATE_SYNCING:
                    //正在同步中，不做处理
                    break;
                case StateCode.STATE_SYNC_OK: //同步成功
                    View itemView = recordAdapter.getViewByPosition(position, R.id.main);
                    if (itemView instanceof SwipeMenuLayout) {
                        ((SwipeMenuLayout) itemView).quickClose();
                    }
                    break;
                default: //未同步 或 同步失败
                    viewModel.tryToSyncBroadcastByRecord(item.getRecordBroadcast());
                    break;
            }
        });
        recordAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            final int viewId = view.getId();
            final AuracastRecordState item = recordAdapter.getItem(position);
            int syncState = item.getSyncState();
            JL_Log.d(TAG, "OnItemChildClick", "syncState : " + StateCode.printSyncState(syncState));
            if (viewId == R.id.btn_leave) {
                switch (syncState) {
                    case StateCode.STATE_SYNCING:
                        //正在同步中，不做处理
                        break;
                    case StateCode.STATE_SYNC_OK:
                        //同步成功
                        viewModel.removeSource();
                        break;
                    default: //未同步 或 同步失败
                        viewModel.removeAuracastRecord(item.getRecord());
                        break;
                }
            } else if (viewId == R.id.tv_select_language) { //选择语音
                if (syncState != StateCode.STATE_SYNC_OK) return;
                //TODO: 暂不支持功能
            }
        });
        binding.rvMyBroadcast.setAdapter(recordAdapter);
        binding.rvMyBroadcast.addItemDecoration(new CommonDecoration(requireContext(),
                ContextCompat.getColor(requireContext(), R.color.color_transparent),
                RecyclerView.VERTICAL, ValueUtil.dp2px(requireContext(), 8)));
    }

    private void addObserver() {
        viewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnection -> {
            if (!BluetoothUtil.deviceEquals(deviceConnection.getDevice(), viewModel.getDevice()))
                return;
            if (deviceConnection.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        viewModel.scanStateMLD.observe(getViewLifecycleOwner(), isStart -> {
            binding.sbtnBroadcast.setCheckedNoEvent(isStart);
            updateScanStateUI(isStart);
            if (!isStart) {
                updateMyBroadcasts();
            }
        });
        viewModel.foundBroadcastMLD.observe(getViewLifecycleOwner(), broadcastList -> broadcastAdapter.setList(broadcastList));
        viewModel.broadcastAudioStateMLD.observe(getViewLifecycleOwner(), this::handleBroadcastMessage);
        viewModel.auracastRecordChangeMLD.observe(getViewLifecycleOwner(), recordOp -> {
            if (recordOp.getOp() == AuracastRecordHelper.OP_MODIFY) return;
            updateMyBroadcasts();
        });
        viewModel.syncBroadcastStateMLD.observe(getViewLifecycleOwner(), stateResult -> {
            final AuracastBroadcast broadcast = stateResult.getData();
            if (null == broadcast) return;
            if (stateResult.getState() == StateResult.STATE_WORKING) {
                showLoadingDialog(getString(R.string.syncing_pa));
                if (broadcast.isValid()) {
                    recordAdapter.updateBroadcastState(broadcast);
                }
                return;
            }
            dismissLoadingDialog();
            if (stateResult.getState() == StateResult.STATE_FINISH) {
                if (broadcast.isValid()) {
                    recordAdapter.updateBroadcastState(broadcast);
                }
                if (stateResult.isSuccess()) {
                    JL_Log.d(TAG, "syncBroadcastState", "Resynchronize Broadcast successfully. " + broadcast);
                    checkAuracastRecord(broadcast);
                    return;
                }
                JL_Log.w(TAG, "syncBroadcastState", CommonUtil.formatString("%s\n%s : %s, %s", getString(R.string.operation_failed,
                                getString(R.string.op_resynchronize_broadcast)),
                        getString(R.string.error_code), CommonUtil.formatInt(stateResult.getCode()), stateResult.getMessage()));
                if (stateResult.getCode() == ErrorCode.SUB_ERR_BAD_CODE) { //密码错误
                    AuracastRecord record = viewModel.findAuracastRecord(broadcast);
                    if (null != record) { //删除记录
                        viewModel.removeAuracastRecord(record);
                    }
                }
                showTipsDialog(getString(R.string.add_source_timeout, broadcast.getBroadcastName()),
                        getString(R.string.add_source_failure));
            }
        });
        viewModel.opResultMLD.observe(getViewLifecycleOwner(), opResult -> {
            if (!opResult.isSuccess()) {
                if (opResult.getOp() == AuracastAssistantViewModel.OP_ADD_SOURCE) {
                    JL_Log.i(TAG, "Add Source", "code : " + CommonUtil.formatInt(opResult.getCode()) + ", " + opResult.getMessage());
                    if (opResult.getData() instanceof AuracastBroadcast) {
                        AuracastBroadcast broadcast = (AuracastBroadcast) opResult.getData();
                        showTipsDialog(getString(R.string.add_source_timeout, broadcast.getBroadcastName()),
                                getString(R.string.add_source_failure));
                    }
                    return;
                } else if (opResult.getOp() == AuracastReceiverViewModel.OP_START_SCAN) {
                    if (opResult.getData() instanceof Integer) {
                        Integer reason = (Integer) opResult.getData();
                        if (reason == ScanResponse.RESULT_LISTENING_BROADCAST_BAN_SCAN) {
                            showTipsDialog(getString(R.string.listening_broadcast_ban_scan_tips), getString(R.string.operation_failed,
                                    viewModel.getOpString(opResult.getOp())));
                            return;
                        }
                    }
                }
                showTips(CommonUtil.formatString("%s\n%s : %s, %s", getString(R.string.operation_failed, viewModel.getOpString(opResult.getOp())),
                        getString(R.string.error_code), CommonUtil.formatInt(opResult.getCode()), opResult.getMessage()));
                return;
            }
            if (opResult.getOp() == AuracastAssistantViewModel.OP_ADD_SOURCE) { //添加音源成功
                if (opResult.getData() instanceof AuracastBroadcast) {
                    AuracastBroadcast broadcast = (AuracastBroadcast) opResult.getData();
                    checkAuracastRecord(broadcast);
                }
            }
        });
    }

    private void requestKeepScreenOn() {
        if (isKeepScreenOn) return;
        final Window window = requireActivity().getWindow();
        if (null == window) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        isKeepScreenOn = true;
    }

    private void releaseKeepScreenOn() {
        if (!isKeepScreenOn) return;
        final Window window = requireActivity().getWindow();
        if (null == window) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        isKeepScreenOn = false;
    }

    private void updateScanStateUI(boolean isScan) {
        if (isScan) {
            requestKeepScreenOn();
            UIHelper.show(binding.aivScanning);
            binding.aivScanning.show();
        } else {
            releaseKeepScreenOn();
            binding.aivScanning.hide();
            UIHelper.gone(binding.aivScanning);
        }
    }

    private void updateMyBroadcasts() {
        List<AuracastRecord> records = viewModel.getAuracastRecords();
        JL_Log.d(TAG, "updateMyBroadcasts", "" + records);
        if (records.isEmpty()) {
            UIHelper.gone(binding.groupListeningBroadcast);
        } else {
            UIHelper.show(binding.groupListeningBroadcast);
        }
        List<AuracastRecordState> recordStateList = new ArrayList<>();
        AuracastBroadcast listeningBroadcast = viewModel.getListeningBroadcast();
        AuracastBroadcast operationBroadcast = viewModel.getOperationBroadcast();
        for (AuracastRecord record : records) {
            AuracastBroadcast broadcast = record.getBroadcast();
            if (null == broadcast) continue;
            AuracastBroadcast state;
            if (broadcast.equals(listeningBroadcast)) {
                state = listeningBroadcast;
            } else if (broadcast.equals(operationBroadcast)) {
                state = operationBroadcast;
            } else {
                state = null;
            }
            recordStateList.add(new AuracastRecordState(record)
                    .setState(state)
                    .setFoundBroadcast(viewModel.isFoundBroadcast(record)));
        }
        //倒序排序
        Collections.sort(recordStateList, (o1, o2) -> {
            AuracastBroadcast broadcast1 = o1 == null ? null : o1.getBroadcast();
            int state1 = null == broadcast1 ? RECORD_STATE_IDLE :
                    broadcast1.isSyncOk() ? RECORD_STATE_LISTENING : o1.isFoundBroadcast() ? RECORD_STATE_FOUND_BROADCAST : RECORD_STATE_IDLE;
            AuracastBroadcast broadcast2 = o2 == null ? null : o2.getBroadcast();
            int state2 = null == broadcast2 ? RECORD_STATE_IDLE :
                    broadcast2.isSyncOk() ? RECORD_STATE_LISTENING : o2.isFoundBroadcast() ? RECORD_STATE_FOUND_BROADCAST : RECORD_STATE_IDLE;
            return Integer.compare(state2, state1);
        });

        recordAdapter.setList(recordStateList);
    }

    private void handleBroadcastMessage(AuracastBroadcast broadcast) {
        if (isInvalid() || null == broadcast) return;
        JL_Log.d(TAG, "handleBroadcastMessage", "" + broadcast);
        int syncState = broadcast.getSyncState();
        if (syncState == StateCode.STATE_UNKNOWN) return;
        recordAdapter.updateBroadcastState(broadcast);
        if (syncState == StateCode.STATE_SYNCING) {
            requestKeepScreenOn();
            showLoadingDialog(getString(R.string.syncing_pa));
            return;
        }
        releaseKeepScreenOn();
        dismissLoadingDialog();
        if (syncState == StateCode.STATE_IDLE) {
            int errorCode = broadcast.getErrorCode();
            JL_Log.d(TAG, "handleBroadcastMessage", "No Sync. errorCode : " + CommonUtil.formatInt(errorCode)
                    + ", " + StateCode.printSyncError(errorCode));
            if (errorCode != StateCode.SYNC_ERR_NONE) {
                updateConnectFailed(broadcast, errorCode);
            }
        } else if (syncState == StateCode.STATE_SYNC_OK) {
            broadcastAdapter.remove(broadcast);
        }

    }

    private void updateConnectFailed(AuracastBroadcast broadcast, int errorCode) {
        String content;
        switch (errorCode) {
            case StateCode.SYNC_ERR_BROADCAST_CODE: {
                content = getString(R.string.bad_password_tips, broadcast.getBroadcastName());
                break;
            }
            case StateCode.SYNC_ERR_TIMEOUT: {
                content = getString(R.string.sync_timeout_tips, broadcast.getBroadcastName());
                break;
            }
            default:
                content = getString(R.string.sync_failed_tips, broadcast.getBroadcastName());
                break;
        }
        showTipsDialog(content, null);
    }

    private void showTipsDialog(String content, String title) {
        if (isInvalid()) return;
        dismissTipsDialog();
        tipsDialog = Jl_Dialog.builder()
                .title(title)
                .content(content)
                .cancel(false)
                .left(getString(R.string.i_know_it_2))
                .leftClickListener((v, dialogFragment) -> dismissTipsDialog()).build();
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

    private void showInputCodeDialog(final AuracastBroadcast broadcast) {
        if (isInvalid() || null == broadcast) return;
        new InputCodeDialog.Builder()
                .setBroadcastName(broadcast.getBroadcastName())
                .setCallback(result -> {
                    broadcast.setBroadcastCode(result.getBytes());
                    viewModel.addSource(broadcast);
                }).setCancelable(false)
                .build().show(getChildFragmentManager(), InputCodeDialog.class.getSimpleName());
    }

    private void tryToScanQRCode() {
        if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.CAMERA)) {
            showPermissionTipsDialog(getString(R.string.camera_permission_desc));
        }
        AuracastReceiverFragmentPermissionsDispatcher.onCameraPermissionGrantWithPermissionCheck(this);
    }

    private void checkAuracastRecord(AuracastBroadcast broadcast) {
        if (null == broadcast) return;
        AuracastRecord record = viewModel.findAuracastRecord(broadcast);
        if (null == record) return;
        if (broadcast.isSyncOk() || broadcast.equals(viewModel.getListeningBroadcast())) {
            //信息不符，更新历史记录
            updateMyBroadcasts();
        }
    }
}