package com.jieli.btsmart.ui.device;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.listener.OnItemLongClickListener;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.ProductAction;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.DeviceListAdapter;
import com.jieli.btsmart.data.listeners.OnDeleteItemListener;
import com.jieli.btsmart.data.listeners.OnLocationItemListener;
import com.jieli.btsmart.data.model.bluetooth.BlackFlagInfo;
import com.jieli.btsmart.data.model.device.HistoryDevice;
import com.jieli.btsmart.tool.configure.ConfigureKit;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.btsmart.ui.search.SearchDeviceFragment;
import com.jieli.btsmart.ui.settings.device.DeviceSettingsFragment;
import com.jieli.btsmart.ui.widget.LoadingDialog;
import com.jieli.btsmart.ui.widget.connect.ScanDeviceDialog;
import com.jieli.btsmart.ui.widget.product_dialog.BlacklistHandler;
import com.jieli.btsmart.ui.widget.product_dialog.FastConnectEdrDialog;
import com.jieli.btsmart.ui.widget.product_dialog.FloatingViewService;
import com.jieli.btsmart.ui.widget.product_dialog.IBTOp;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.base.BasePresenter;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.Context.VIBRATOR_SERVICE;

/**
 * 设备列表界面
 */
@Deprecated
public class DeviceListFragment extends Jl_BaseFragment implements IDeviceConnectContract.IDeviceConnectView {

    private RecyclerView rlDeviceListContainer;
    private TextView tvNoNetwork;
    private RelativeLayout rlDeviceListMain;

    private DeviceListAdapter mDeviceListAdapter;

    private HomeActivity mActivity;
    private IDeviceConnectContract.IDeviceConnectPresenter mPresenter;
    private DeviceListBroadcastReceiver mReceiver;

    //Dialog
    private Jl_Dialog mRequestFloatingDialog;
    private ScanDeviceDialog mScanDeviceDialog;
    private Jl_Dialog mIgnoreDeviceDialog;
    private LoadingDialog mLoadingDialog;
    private Jl_Dialog mEDRMaxConnectionDialog;
    //弹窗
    private FastConnectEdrDialog mFastConnectEdrDialog;
    private FloatingViewService mFloatingViewService;

    private BlacklistHandler mBlacklistHandler;
    private ConcurrentHashMap<String, BleScanMessage> mUnreachableDeviceMap;
    private BluetoothDevice mScanDevice;
    private BleScanMessage mScanMessage;

    private Intent floatingIntent; //请求悬浮窗权限提示框

    private BluetoothDevice needToSettingsDev;
    private BluetoothDevice needToHomeDev;

    private boolean isBindingService;
    private boolean isShowLoadingDialog;


    private final static int DELAY_TIME = 6000;
    private final static int MSG_UPDATE_HEADSET_IMG = 0x3256;
    private final static int MSG_START_ACTIVITY_TIMEOUT = 0x3257;
    private final static int MSG_FAST_CONNECT = 0x3258;
    private final static int MSG_NEED_TO_SETTINGS_TIMEOUT = 0x3259;
    private final static int MSG_NEED_TO_HOME_TIMEOUT = 0x3260;
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_HEADSET_IMG:
                    updateHistoryDeviceListUI(mPresenter.getHistoryBtDeviceList());
                    break;
                case MSG_START_ACTIVITY_TIMEOUT:
                    if (getActivity() != null && !isDetached() && !SystemUtil.isAppInForeground(getActivity())) {
                        ToastUtil.showToastLong(getString(R.string.background_show_activity_failed_tips));
                    }
                    break;
                case MSG_FAST_CONNECT:
                    if (mPresenter != null) {
                        mPresenter.fastConnect();
                    }
                    break;
                case MSG_NEED_TO_SETTINGS_TIMEOUT:
                    setNeedToSettingsDev(null);
                    break;
                case MSG_NEED_TO_HOME_TIMEOUT:
                    setNeedToHomeDev(null);
                    break;
            }
            return false;
        }
    });

    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    public DeviceListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mActivity == null && context instanceof HomeActivity) {
            mActivity = (HomeActivity) context;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
        registerReceiver();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        rlDeviceListContainer = view.findViewById(R.id.rl_device_list_container);
        tvNoNetwork = view.findViewById(R.id.tv_device_list_no_network);
        rlDeviceListMain = view.findViewById(R.id.rl_device_list_main);
        tvNoNetwork.setOnClickListener(mOnClickListener);
        if (mActivity == null && getActivity() instanceof HomeActivity) {
            mActivity = (HomeActivity) getActivity();
        }
        rlDeviceListContainer.setLayoutManager(new LinearLayoutManager(getContext()));
        rlDeviceListContainer.setOnTouchListener((v, event) -> {
            if (v.getId() != 0) {
                if (event.getAction() == MotionEvent.ACTION_DOWN && mDeviceListAdapter != null && mDeviceListAdapter.isEditMode()) {
                    mDeviceListAdapter.setEditMode(false);
                }
            }
            return false;
        });
        mPresenter = new DeviceConnectPresenterImpl(mActivity, this);
        setCustomBackWay();
        mPresenter.checkNetworkAvailable();
        updateHistoryDeviceListUI(mPresenter.getHistoryBtDeviceList());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        handleHiddenEvent(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        handleHiddenEvent(true);
    }

    @Override
    public void onDestroyView() {
        dismissIgnoreDeviceDialog();
        dismissScanDeviceDialog();
        dismissRequestFloatingDialog();
        dismissLoadingDialog();
        handleDismissDialog(true);
        super.onDestroyView();
        if (mPresenter != null) {
            mPresenter.destroy();
        }
        unregisterReceiver();
        destroyBlacklistHandler();
        destroyUnreachableDeviceSet();
        stopFloatingService();

        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case SConstant.REQUEST_CODE_OVERLAY:
                if (!ProductUtil.isCanDrawOverlays(getActivity())) {
                    showRequestFloatingDialog(getString(R.string.request_floating_window_permission_tips));
                } else {
                    startFloatingService(mScanDevice, mScanMessage);
                    setScanDeviceAndMessage(null, null);
                }
                floatingIntent = null;
                break;
            case SConstant.REQUEST_CODE_DEVICE_SETTINGS:
                if (mPresenter != null) {
                    mPresenter.updateDeviceADVInfo(mPresenter.getConnectedDevice());
                }
                break;
            case SConstant.REQUEST_CODE_NETWORK:
                if (mPresenter != null) {
                    mPresenter.checkNetworkAvailable();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPermissionSuccess(String[] permissions) {
        MainApplication.getApplication().uploadAppInfo();
        handleHiddenEvent(false);
    }

    @Override
    public void onPermissionFailed(String permission) {
        JL_Log.w(TAG, "onPermissionFailed >> " + permission);
        if (!PermissionUtil.checkPermissionShouldShowDialog(mActivity, permission)) {
            String msg = getString(R.string.permissions_tips_02) + mPresenter.getPermissionName(permission) + getString(R.string.permission);
            ToastUtil.showToastLong(msg);
        }
    }

    @Override
    public void onDiscovery(int state, @Nullable BluetoothDevice device, @Nullable BleScanMessage bleScanMessage) {
        /*if (state == DeviceConnectPresenterImpl.DISCOVERY_STATUS_FOUND) {
            JL_Log.d(TAG, "onDiscovery : " + BluetoothUtil.printBtDeviceInfo(device));
        }*/
        if (isShowScanDeviceDialog()) {
            mScanDeviceDialog.onDiscovery(state, device, bleScanMessage);
        }
    }

    @Override
    public void onShowDialog(BluetoothDevice device, BleScanMessage scanMessage) {
        handleShowDialog(device, scanMessage);
    }

    @Override
    public void onRemoveHistoryDeviceSuccess(HistoryBluetoothDevice device) {
        JL_Log.d(TAG, "removeHistoryDevice success, >>>>>>>");
        if (device != null) {
            AppUtil.deleteDeviceSupportSearchStatus(device.getAddress());
            ConfigureKit.getInstance().removeAllowSearchDevice(device);
        }
        updateHistoryDeviceListUI(mPresenter.getHistoryBtDeviceList());
    }

    @Override
    public void onRemoveHistoryDeviceFailed(BaseError error) {
        JL_Log.w(TAG, "removeHistoryDevice failed, >>>>>>>" + error);
        updateHistoryDeviceListUI(mPresenter.getHistoryBtDeviceList());
    }

    @Override
    public void onDevConnectionError(int code, String message) {
        JL_Log.i(TAG, "onDevConnectionError : " + code + ", " + message);
        if (code == SConstant.ERR_EDR_MAX_CONNECTION) {
            showEDRMaxConnectionDialog();
            return;
        }
        ToastUtil.showToastShort(message);
    }

    @Override
    public void onCommandSuccess(BluetoothDevice device, CommandBase cmd) {

    }

    @Override
    public void onCommandFailed(BluetoothDevice device, BaseError error) {

    }

    @Override
    public void onADVInfoUpdate(BluetoothDevice device, ADVInfoResponse advInfo) {
        JL_Log.v(TAG, "onADVInfoUpdate >>> ");
        updateAdvInfo(device, advInfo);
    }

    @Override
    public void onDeviceBQStatus(boolean isRunning) {

    }

    @Override
    public void onDeviceBQUpdate(BluetoothDevice device, ADVInfoResponse advInfo) {
        JL_Log.v(TAG, "onDeviceBQUpdate >>> ");
        updateAdvInfo(device, advInfo);
    }

    @Override
    public void onNetworkState(boolean isAvailable) {
        if (!isAdded() || isDetached()) return;
        tvNoNetwork.setVisibility(!isAvailable ? View.VISIBLE : View.GONE);
        if (isAvailable) {
            MainApplication.getApplication().uploadAppInfo();
        }
    }

    @Override
    public void onUpdateConfigureSuccess() {
        JL_Log.d(TAG, "onUpdateConfigureSuccess >>>> ");
    }

    @Override
    public void onUpdateImage() {
        JL_Log.i(TAG, "onUpdateImage >>>> ");
        mHandler.removeMessages(MSG_UPDATE_HEADSET_IMG);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_HEADSET_IMG, 1000);
    }

    @Override
    public void onUpdateConfigureFailed(int code, String message) {
        JL_Log.i(TAG, "onUpdateConfigureFailed >>>> code = " + code + ", message = " + message);
    }

    @Override
    public void onBtAdapterStatus(boolean enable) {
        if (isShowScanDeviceDialog()) {
            mScanDeviceDialog.onBtAdapterStatus(enable);
        }
    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        handleDeviceConnectionEvent(device, status);
    }


    @Override
    public void onSwitchDevice(BluetoothDevice device) {
        JL_Log.d(TAG, "-onSwitchDevice- device : " + BluetoothUtil.printBtDeviceInfo(device));
        handleHiddenEvent(false);
        updateHistoryDeviceListUI(mPresenter.getHistoryBtDeviceList());
    }

    @Override
    public void setPresenter(BasePresenter presenter) {
        if (mPresenter == null) {
            mPresenter = (IDeviceConnectContract.IDeviceConnectPresenter) presenter;
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v == tvNoNetwork){
                startActivityForResult(new Intent("android.settings.SETTINGS"), SConstant.REQUEST_CODE_NETWORK);
            }
        }
    };

    /*===================================================================*
     * UI处理
     *===================================================================*/

    private void setCustomBackWay() {
        if (mActivity != null) {
            mActivity.setCustomBackPress(() -> {
                if (mDeviceListAdapter != null && mDeviceListAdapter.isEditMode()) {
                    mDeviceListAdapter.setEditMode(false);
                    return true;
                }
                return false;
            });
        }
    }

    private List<HistoryDevice> convertHistoryDeviceList(List<HistoryBluetoothDevice> list) {
        List<HistoryDevice> dataList = new ArrayList<>();
        if (list != null) {
            for (HistoryBluetoothDevice history : list) {
                if (history == null) continue;
                HistoryDevice historyDevice = new HistoryDevice(history);
                BluetoothDevice device = BluetoothUtil.getRemoteDevice(history.getAddress());
                if (device != null && mPresenter.isConnectedDevice(device)) {
                    historyDevice.setADVInfo(mPresenter.getADVInfo(device));
                }
                dataList.add(historyDevice);
            }
        }
        return dataList;
    }


    private void updateHistoryDeviceListUI(List<HistoryBluetoothDevice> list) {
        if (isDetached() || getContext() == null || mPresenter == null) return;
        List<HistoryDevice> dataList = convertHistoryDeviceList(list);
        if (mDeviceListAdapter == null) {
            mDeviceListAdapter = new DeviceListAdapter(mPresenter);
            mDeviceListAdapter.setOnItemClickListener(mOnItemClickListener);
            mDeviceListAdapter.setOnItemLongClickListener(mOnItemLongClickListener);
            mDeviceListAdapter.setOnDeleteItemListener(mOnDeleteItemListener);
            mDeviceListAdapter.setOnLocationItemListener(mOnLocationItemListener);
        }
        mDeviceListAdapter.setList(dataList);
        if (rlDeviceListContainer.getAdapter() != mDeviceListAdapter) {
            rlDeviceListContainer.setAdapter(mDeviceListAdapter);
        }
        if (dataList.size() == 0) {
            mDeviceListAdapter.setEmptyView(View.inflate(getContext(), R.layout.view_unconnected_device_1, null));
            mDeviceListAdapter.setEditMode(false);
        }
        if (!mDeviceListAdapter.checkDataArrayHasDisconnectDevice()){
            mDeviceListAdapter.setEditMode(false);
        }
    }

    private void updateAdvInfo(BluetoothDevice device, ADVInfoResponse advInfo) {
        if (!isAdded() || isDetached() || device == null) return;
        if (mDeviceListAdapter != null) {
            mDeviceListAdapter.updateHistoryDeviceByBtDevice(device, advInfo);
        }
    }

    private void handleDeviceConnectionEvent(BluetoothDevice device, int status) {
        if (!isAdded() || isDetached()) return;
        if (isShowScanDeviceDialog()) {
            mScanDeviceDialog.onDeviceConnection(device, status);
        }
        if (status != StateCode.CONNECTION_CONNECTING) {
            dismissLoadingDialog();
            boolean isConnected = (status == StateCode.CONNECTION_OK) || (status == StateCode.CONNECTION_CONNECTED);
            updateHistoryDeviceListUI(mPresenter.getHistoryBtDeviceList());
            if (!isConnected) { //设备断开
                checkNeedDismissShowDialog(device);
                if (BluetoothUtil.deviceEquals(needToHomeDev, device)) {
                    setNeedToHomeDev(null);
                }
                if (BluetoothUtil.deviceEquals(needToSettingsDev, device)) {
                    setNeedToSettingsDev(null);
                }
            } else { //设备连接
                if (BluetoothUtil.deviceEquals(needToSettingsDev, device)) {
                    setNeedToSettingsDev(null);
                    toDeviceSettings();
                } else if (BluetoothUtil.deviceEquals(needToHomeDev, device)) {
                    setNeedToHomeDev(null);
                    toHomeActivity();
                }
                mPresenter.updateDeviceADVInfo(device);
                //黑名单重发C3命令重发次数
                if (mPresenter.getMappedEdrDevice(device) != null) {
                    getBlacklistHandler().resetRepeatTime(mPresenter.getMappedEdrDevice(device).getAddress());
                }
            }
        } else { //连接中
            if (!mPresenter.isDevConnected() || isShowLoadingDialog) {
                showLoadingDialog();
            }
            if (isFastConnectDialogShowing()) {
                BluetoothDevice edrDevice = mPresenter.getMappedEdrDevice(device);
                if (edrDevice == null || !isBtDeviceBonded(edrDevice.getAddress())) { //设备没配对时，在连接中不弹窗
                    handleDismissDialog(false);
                }
            }
            if (isShowScanDeviceDialog()) {
                BluetoothDevice edrDevice = mPresenter.getMappedEdrDevice(device);
                if (edrDevice == null || !isBtDeviceBonded(edrDevice.getAddress())) { //设备没配对时，在连接中不弹窗
                    dismissScanDeviceDialog();
                }
            }
        }
    }

    private void handleHiddenEvent(boolean isHidden) {
        if (isHidden) {
            if (mActivity != null && mActivity.getCurrentFragment() == 2) {
                return;
            }
            if (mDeviceListAdapter != null && mDeviceListAdapter.isEditMode()) {
                mDeviceListAdapter.setEditMode(false);
            }
            notAllowShowDeviceDialog();
        } else if (mPresenter != null && !mPresenter.isDevConnecting()) {
            if (BluetoothUtil.isBluetoothEnable()) {
                mPresenter.startScan();
            }
            if (mPresenter.isDevConnected()) {
                if (mPresenter.getDeviceInfo() != null && UIHelper.isCanUseTwsCmd(mPresenter.getDeviceInfo().getSdkType())) {
                    mPresenter.updateDeviceADVInfo(mPresenter.getConnectedDevice());
                } else if (!mPresenter.isUpdateDevBQ()) {
                    mPresenter.startUpdateDevBQ();
                }
            }
        }
    }

    /*===================================================================*
     * 变量获取
     *===================================================================*/

    private BlacklistHandler getBlacklistHandler() {
        if (mBlacklistHandler == null) {
            mBlacklistHandler = BlacklistHandler.getInstance();
        }
        return mBlacklistHandler;
    }

    private void destroyBlacklistHandler() {
        if (mBlacklistHandler != null) {
            mBlacklistHandler.release();
            mBlacklistHandler = null;
        }
    }

    private void putUnreachableDevice(String addr, BleScanMessage message) {
        if (!BluetoothAdapter.checkBluetoothAddress(addr)) return;
        if (mUnreachableDeviceMap == null) {
            mUnreachableDeviceMap = new ConcurrentHashMap<>();
        }
        if (!mUnreachableDeviceMap.containsKey(addr)) {
            mUnreachableDeviceMap.put(addr, message);
        }
    }

    private boolean isUnreachableDevice(BluetoothDevice device) {
        return device != null && mUnreachableDeviceMap != null && mUnreachableDeviceMap.containsKey(device.getAddress());
    }

    private BleScanMessage getUnreachableDeviceMsg(BluetoothDevice device) {
        BleScanMessage message = null;
        if (isUnreachableDevice(device)) {
            message = mUnreachableDeviceMap.get(device.getAddress());
        }
        return message;
    }

    private void destroyUnreachableDeviceSet() {
        if (mUnreachableDeviceMap != null) {
            mUnreachableDeviceMap.clear();
            mUnreachableDeviceMap = null;
        }
    }

    private void setNeedToSettingsDev(BluetoothDevice needToSettingsDev) {
        this.needToSettingsDev = needToSettingsDev;
        mHandler.removeMessages(MSG_NEED_TO_SETTINGS_TIMEOUT);
        if (needToSettingsDev != null) {
            mHandler.sendEmptyMessageDelayed(MSG_NEED_TO_SETTINGS_TIMEOUT, BluetoothConstant.CONNECT_TIMEOUT + 3000);
        }
    }

    private void setNeedToHomeDev(BluetoothDevice needToHomeDev) {
        this.needToHomeDev = needToHomeDev;
        mHandler.removeMessages(MSG_NEED_TO_HOME_TIMEOUT);
        if (needToHomeDev != null) {
            mHandler.sendEmptyMessageDelayed(MSG_NEED_TO_HOME_TIMEOUT, BluetoothConstant.CONNECT_TIMEOUT + 3000);
        }
    }

    private void setScanDeviceAndMessage(BluetoothDevice scanDevice, BleScanMessage scanMessage) {
        mScanDevice = scanDevice;
        mScanMessage = scanMessage;
    }

    /*===================================================================*
     * 显示弹窗
     *===================================================================*/

    private void showLoadingDialog() {
        isShowLoadingDialog = false;
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog();
            mLoadingDialog.updateTips(getString(R.string.device_connecting));
        }
        if (!mLoadingDialog.isShow() && getActivity() != null && !isDetached()) {
            mLoadingDialog.show(getActivity().getSupportFragmentManager(), LoadingDialog.class.getSimpleName());
        }
    }

    private void dismissLoadingDialog() {
        isShowLoadingDialog = false;
        if (mLoadingDialog != null) {
            if (mLoadingDialog.isShow()) {
                mLoadingDialog.dismiss();
            }
            mLoadingDialog = null;
        }
    }

    private void showRequestFloatingDialog(String text) {
        if (getActivity() == null || isDetached() || !isAdded()) return;
        if (mRequestFloatingDialog == null) {
            mRequestFloatingDialog = Jl_Dialog.builder()
                    .title(getString(R.string.tips))
                    .content(text)
                    .showProgressBar(false)
                    .width(0.8f)
                    .left(getString(R.string.allow))
                    .leftClickListener((v, dialogFragment) -> {
                        ConfigureKit.getInstance().setBanRequestFloatingWindowPermission(requireContext(), false);
                        ProductUtil.setIsAllowFloatingWindow(true);
                        if (floatingIntent == null && !ProductUtil.isCanDrawOverlays(getActivity())) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                floatingIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getActivity().getPackageName()));
                                startActivityForResult(floatingIntent, SConstant.REQUEST_CODE_OVERLAY);
                            }
                        }
                        dismissRequestFloatingDialog();
                    })
                    .right(getString(R.string.ban))
                    .rightClickListener((v, dialogFragment) -> {
                        ConfigureKit.getInstance().setBanRequestFloatingWindowPermission(requireContext(), true);
                        ProductUtil.setIsAllowFloatingWindow(false);
                        setScanDeviceAndMessage(null, null);
                        dismissRequestFloatingDialog();
                    })
                    .cancel(false)
                    .build();
        }
        if (!mRequestFloatingDialog.isShow() && !isDetached() && getActivity() != null) {
            mRequestFloatingDialog.show(getActivity().getSupportFragmentManager(), "request_floating_permission");
        }
    }

    private void dismissRequestFloatingDialog() {
        if (mRequestFloatingDialog != null) {
            if (mRequestFloatingDialog.isShow() && !isDetached()) {
                mRequestFloatingDialog.dismiss();
            }
            mRequestFloatingDialog = null;
        }
    }

    private void showProductMessageDialog(final BluetoothDevice device, BleScanMessage scanMessage) {
        if (device == null || scanMessage == null || isDetached() || !isAdded()) return;
        if (mFastConnectEdrDialog == null) {
            FastConnectEdrDialog.Builder builder = new FastConnectEdrDialog.Builder(getContext())
                    .setBluetoothDevice(device)
                    .setBleScanMessage(null)
                    .setFastConnectListener(mOnFastConnectListener);
            mFastConnectEdrDialog = builder.create();
            mFastConnectEdrDialog.setBTOp(mBTOp);
            mFastConnectEdrDialog.updateBleScanMessage(scanMessage);
        } else if (mFastConnectEdrDialog.isShowing() && ProductUtil.equalScanMessage(mFastConnectEdrDialog.getBuilder().getBleScanMessage(), scanMessage)) {
            mFastConnectEdrDialog.updateBleScanMessage(scanMessage);
        }
        if (scanMessage.getAction() != ProductAction.DEVICE_ACTION_DISMISS && !mFastConnectEdrDialog.isShowing()) {
            if (rlDeviceListMain != null) {
                mFastConnectEdrDialog.showDialog(rlDeviceListMain);
            }
        }
    }

    private void dismissProductMessageDialog(boolean isUser) {
        if (mFastConnectEdrDialog != null) {
            if (mFastConnectEdrDialog.isShowing() && !isDetached()) {
                mFastConnectEdrDialog.dismissDialog(isUser);
            }
            mFastConnectEdrDialog = null;
        }
    }

    private void showFloatingDialog(BluetoothDevice device, BleScanMessage scanMessage) {
        if (getActivity() == null || isDetached() || !isAdded()) return;
        boolean canDrawOverlays = ProductUtil.isCanDrawOverlays(getActivity());
        boolean isBanRequestPermission = ConfigureKit.getInstance().isBanRequestFloatingWindowPermission(requireContext());
        if (!canDrawOverlays && !isBanRequestPermission) {
            if (floatingIntent != null) {
                return;
            }
            setScanDeviceAndMessage(device, scanMessage);
            showRequestFloatingDialog(getString(R.string.request_floating_window_permission_tips));
        } else if (mFloatingViewService == null || !mFloatingViewService.isFloatingWindowShowing()
                || ProductUtil.equalScanMessage(mFloatingViewService.getBleScanMessage(), scanMessage)) {
            startFloatingService(device, scanMessage);
        }
    }

    private void bindFloatingService() {
        if (!isBindingService && mFloatingViewService == null && getActivity() != null) {
            isBindingService = getActivity().bindService(new Intent(getActivity(), FloatingViewService.class),
                    mServiceConnection, Service.BIND_AUTO_CREATE);
        }
    }

    public void unbindFloatingService() {
        if (mFloatingViewService != null) {
            mFloatingViewService = null;
            if (getActivity() != null) {
                getActivity().unbindService(mServiceConnection);
            }
        }
    }

    private void startFloatingService(BluetoothDevice device, BleScanMessage scanMessage) {
        if (getActivity() == null || isDetached() || !isAdded()) return;
        Intent intent = new Intent(getActivity(), FloatingViewService.class);
        intent.putExtra(SConstant.KEY_BLUETOOTH_DEVICE, device);
        intent.putExtra(SConstant.KEY_BLE_SCAN_MESSAGE, scanMessage);
        ContextCompat.startForegroundService(getActivity().getApplicationContext(), intent);
        bindFloatingService();
    }

    private void stopFloatingService() {
        unbindFloatingService();
        if (getActivity() != null) {
            getActivity().stopService(new Intent(getActivity(), FloatingViewService.class));
        }
    }

    private void dismissFloatingDialog(boolean isUser) {
        if (mFloatingViewService != null && mFloatingViewService.isFloatingWindowShowing()) {
            JL_Log.w(TAG, "onShowDialog :: dismissFloatingDialog = " + isUser);
            mFloatingViewService.dismissWindow(isUser);
        }
    }

    private boolean isFastConnectDialogShowing() {
        return (mFloatingViewService != null && mFloatingViewService.isFloatingWindowShowing())
                || (mFastConnectEdrDialog != null && mFastConnectEdrDialog.isShowing());
    }

    private void handleShowDialog(BluetoothDevice device, BleScanMessage message) {
        if (mPresenter == null || getActivity() == null || device == null || message == null)
            return;
        if (mPresenter.isDevConnecting() || mPresenter.isConnectingClassicDevice()) {
            String edrAddr = message.getEdrAddr();
            if (!isBtDeviceBonded(edrAddr)) { //设备没配对时，在连接中不弹窗
//                JL_Log.i(TAG, "handleShowDialog ::  mConnectingDialog is Showing,");
                fastConnectResetTimeout(message);
                return;
            }
        }
        String blackFlag = BlacklistHandler.getBlackFlag(message.getEdrAddr(), message.getSeq());
        if (getBlacklistHandler().isContains(blackFlag)) {
            JL_Log.v(TAG, "handleShowDialog ::  device on the blacklist : " + blackFlag + ", device : " + BluetoothUtil.printBtDeviceInfo(device));
            if (checkIsSameDevice(device, message)) {
                handleDismissDialog(false);
            }
            if (mPresenter.isConnectedDevice(device) && mPresenter.getDeviceInfo(device) != null &&
                    message.getEdrAddr().equals(mPresenter.getDeviceInfo(device).getEdrAddr())) {
                BlackFlagInfo info = getBlacklistHandler().getInfo(message.getEdrAddr(), message.getSeq());
                JL_Log.e(TAG, info.toString());
                info.setRepeatTime(info.getRepeatTime() + 1);
                if (info.getRepeatTime() < 3) {
                    mPresenter.stopDeviceNotifyAdvInfo(device);
                }
            }
            return;
        }
        if (!message.isEnableConnect()) putUnreachableDevice(device.getAddress(), message);
        if (ProductUtil.isAllowFloatingWindow(getActivity().getApplicationContext())) {
            showFloatingDialog(device, message);
        } else { //用户拒绝悬浮窗的授权
            showProductMessageDialog(device, message);
        }

        if (isFastConnectDialogShowing() && isShowScanDeviceDialog()) {
            dismissScanDeviceDialog();
        }
    }

    private void handleDismissDialog(boolean isUser) {
        JL_Log.w(TAG, "onShowDialog :: handleDismissDialog = " + isUser);
        dismissFloatingDialog(isUser);
        dismissProductMessageDialog(isUser);
        setScanDeviceAndMessage(null, null);
    }

    private void showScanDeviceDialog() {
        if (mScanDeviceDialog == null) {
            mScanDeviceDialog = new ScanDeviceDialog();
            mScanDeviceDialog.setOnScanDeviceOpListener(mOnScanDeviceOpListener);
        }
        if (!mScanDeviceDialog.isShow() && !isDetached() && getActivity() != null) {
            mScanDeviceDialog.show(getActivity().getSupportFragmentManager(), "scan_device_dialog");
        }
    }

    private void dismissScanDeviceDialog() {
        if (mScanDeviceDialog != null) {
            if (mScanDeviceDialog.isShow() && !isDetached()) {
                mScanDeviceDialog.dismiss();
            }
            mScanDeviceDialog = null;
        }
    }

    private boolean isShowScanDeviceDialog() {
        return mScanDeviceDialog != null && mScanDeviceDialog.isShow();
    }

    private void showIgnoreDeviceDialog(final HistoryBluetoothDevice historyBluetoothDevice) {
        if (mIgnoreDeviceDialog == null) {
            mIgnoreDeviceDialog = Jl_Dialog.builder()
                    .title(getString(R.string.tips))
                    .content(getString(R.string.remove_history_device_tips))
                    .cancel(false)
                    .left(getString(R.string.cancel))
                    .leftColor(getResources().getColor(R.color.gray_9A9A9A))
                    .leftClickListener((v, dialogFragment) -> dismissIgnoreDeviceDialog())
                    .right(getString(R.string.confirm))
                    .rightColor(getResources().getColor(R.color.blue_text_color))
                    .rightClickListener((v, dialogFragment) -> {
                        if (mPresenter != null && historyBluetoothDevice != null) {
                            mPresenter.removeHistoryBtDevice(historyBluetoothDevice);
                        }
                        dismissIgnoreDeviceDialog();
                    })
                    .build();
        }
        if (!mIgnoreDeviceDialog.isShow() && !isDetached() && getActivity() != null) {
            mIgnoreDeviceDialog.show(getActivity().getSupportFragmentManager(), "ignore_device_dialog");
        }
    }

    private void dismissIgnoreDeviceDialog() {
        if (mIgnoreDeviceDialog != null) {
            if (mIgnoreDeviceDialog.isShow()) {
                mIgnoreDeviceDialog.dismiss();
            }
            mIgnoreDeviceDialog = null;
        }
    }

    private void showEDRMaxConnectionDialog() {
        if (mEDRMaxConnectionDialog == null) {
            String message = getString(R.string.connect_device_over_limit);
            mEDRMaxConnectionDialog = Jl_Dialog.builder()
                    .title(getString(R.string.dialog_tips))
                    .content(message)
                    .width(0.8f)
                    .left(getString(R.string.i_known))
                    .leftColor(getResources().getColor(R.color.blue_448eff))
                    .leftClickListener((v, dialogFragment) -> dismissEDRMaxConnectionDialog())
                    .build();
        }
        if (!mEDRMaxConnectionDialog.isShow() && !isDetached() && getActivity() != null) {
            mEDRMaxConnectionDialog.show(getActivity().getSupportFragmentManager(), "edr_max_connection_dialog");
        }
    }

    private void dismissEDRMaxConnectionDialog() {
        if (mEDRMaxConnectionDialog != null) {
            if (mEDRMaxConnectionDialog.isShow()) {
                mEDRMaxConnectionDialog.dismiss();
            }
            mEDRMaxConnectionDialog = null;
        }
    }
    /*===================================================================*
     * 私有方法
     *===================================================================*/

    private void registerReceiver() {
        if (getActivity() != null) {
            if (mReceiver == null) {
                mReceiver = new DeviceListBroadcastReceiver();
                IntentFilter intentFilter = new IntentFilter(SConstant.ACTION_ACTIVITY_RESUME);
                intentFilter.addAction(SConstant.ACTION_FAST_CONNECT);
                intentFilter.addAction(SConstant.ACTION_DEVICE_UPGRADE);
                intentFilter.addAction(SConstant.ACTION_ADD_DEVICE);
                getActivity().registerReceiver(mReceiver, intentFilter);
            }
        }
    }

    private void unregisterReceiver() {
        if (getActivity() != null && mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void toDeviceSettings() {
        if (getActivity() == null) return;
        Bundle bundle = new Bundle();
        bundle.putParcelable(SConstant.KEY_ADV_INFO, mPresenter == null ? null : mPresenter.getADVInfo(mPresenter.getConnectedDevice()));
        CommonActivity.startCommonActivity(DeviceListFragment.this, SConstant.REQUEST_CODE_DEVICE_SETTINGS,
                DeviceSettingsFragment.class.getCanonicalName(), bundle);
        boolean isAppInForeground = SystemUtil.isAppInForeground(getActivity());
        JL_Log.i(TAG, "toDeviceSettings isAppInForeground : " + isAppInForeground);
        if (!isAppInForeground) {
            mHandler.removeMessages(MSG_START_ACTIVITY_TIMEOUT);
            mHandler.sendEmptyMessageDelayed(MSG_START_ACTIVITY_TIMEOUT, DELAY_TIME);
        }
    }

    private void toHomeActivity() {
        if (getActivity() == null) return;
        boolean isAppInForeground = SystemUtil.isAppInForeground(getActivity());
        JL_Log.i(TAG, "toHomeActivity:: isAppInForeground : " + isAppInForeground);
        if (!isAppInForeground) {
            getActivity().startActivity(new Intent(getActivity(), HomeActivity.class));
            if (mActivity != null) {
                mActivity.resetFragment();
            }
            mHandler.removeMessages(MSG_START_ACTIVITY_TIMEOUT);
            mHandler.sendEmptyMessageDelayed(MSG_START_ACTIVITY_TIMEOUT, DELAY_TIME);
        }
    }

    private boolean isBtDeviceBonded(String addr) {
        boolean result = false;
        if (BluetoothAdapter.checkBluetoothAddress(addr) && PermissionUtil.checkHasConnectPermission(requireContext())) {
            BluetoothDevice device = BluetoothUtil.getRemoteDevice(addr);
            result = (device != null && device.getBondState() == BluetoothDevice.BOND_BONDED);
        }
        return result;
    }

    private boolean checkIsSameDevice(BluetoothDevice device, BleScanMessage message) {
        boolean isSame = false;
        if (device != null && message != null) {
            BluetoothDevice showDevice = null;
            BleScanMessage showDevMsg = null;
            if (mFloatingViewService != null && mFloatingViewService.isFloatingWindowShowing()) {
                showDevice = mFloatingViewService.getScanDevice();
                showDevMsg = mFloatingViewService.getBleScanMessage();
            } else if (mFastConnectEdrDialog != null && mFastConnectEdrDialog.isShowing()) {
                showDevice = mFastConnectEdrDialog.getBuilder().getBluetoothDevice();
                showDevMsg = mFastConnectEdrDialog.getBuilder().getBleScanMessage();
            }
            isSame = BluetoothUtil.deviceEquals(showDevice, device) && showDevMsg != null && showDevMsg.getSeq() == message.getSeq();
        }
        return isSame;
    }

    private void fastConnectResetTimeout(BleScanMessage bleScanMessage) {
        if (isFastConnectDialogShowing()) {
            if (mFloatingViewService != null) {
                mFloatingViewService.resetTimeout(bleScanMessage);
            }
            if (mFastConnectEdrDialog != null) {
                mFastConnectEdrDialog.resetTimeout(bleScanMessage);
            }
        }
    }

    private BluetoothDevice getFastConnectDialogDevice() {
        BluetoothDevice device = null;
        if (isFastConnectDialogShowing()) {
            if (mFloatingViewService != null) {
                device = mFloatingViewService.getScanDevice();
            }
            if (device == null && mFastConnectEdrDialog != null) {
                device = mFastConnectEdrDialog.getBuilder().getBluetoothDevice();
            }
        }
        return device;
    }

    private void notAllowShowDeviceDialog() {
        /*if (mPresenter != null && mPresenter.isDevConnected()) {
            mPresenter.setBanShowDialog(true);
            mPresenter.setBanScanBtDevice(true);
            mPresenter.stopUpdateDevBQ();
            mPresenter.stopScan();
//            mPresenter.stopDeviceNotifyAdvInfo();
            if (isFastConnectDialogShowing() && !BluetoothUtil.isMatchDevice(mPresenter.getConnectedDevice(),
                    getFastConnectDialogDevice())) {
                handleDismissDialog(false);
            }
        }*/
        if (mPresenter != null) {
            checkNeedDismissShowDialog(mPresenter.getConnectedDevice());
        }
    }

    private void checkNeedDismissShowDialog(BluetoothDevice device) {
        if (isFastConnectDialogShowing() && mPresenter.isConnectedDevice(device)
                && !DeviceAddrManager.getInstance().isMatchDevice(device,
                getFastConnectDialogDevice())) {
            handleDismissDialog(false);
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            JL_Log.i(TAG, "onServiceConnected:: ---------> " + name);
            FloatingViewService.FloatingBinder mBinder = (FloatingViewService.FloatingBinder) service;
            if (mBinder != null) {
                mFloatingViewService = mBinder.getService();
                mFloatingViewService.setOnFastConnectListener(mOnFastConnectListener);
                mFloatingViewService.setBTOp(mBTOp);
                isBindingService = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            JL_Log.i(TAG, "onServiceDisconnected:: ---------> " + name);
            setScanDeviceAndMessage(null, null);
            mFloatingViewService = null;
        }
    };

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(@NonNull BaseQuickAdapter<?, ?> adapter, @NonNull View view, int position) {
            if (mDeviceListAdapter != null) {
                if (mDeviceListAdapter.isEditMode()) {
                    mDeviceListAdapter.setEditMode(false);
                    return;
                }
                if (mPresenter != null && mPresenter.isDevConnecting()) {
                    onDevConnectionError(SConstant.ERR_DEV_CONNECTING, getString(R.string.device_connecting_tips));
                    return;
                }
                HistoryDevice historyDevice = mDeviceListAdapter.getItem(position);
                if (historyDevice == null || historyDevice.getDevice() == null) {
                    return;
                }
                HistoryBluetoothDevice history = historyDevice.getDevice();
                if (mPresenter.isConnectedDevice(history.getAddress())) {
                    BluetoothDevice connectedDev = BluetoothUtil.getRemoteDevice(history.getAddress());
                    JL_Log.d(TAG, "control connectedDev : " + BluetoothUtil.printBtDeviceInfo(connectedDev));
                    if (mPresenter.isUsedDevice(connectedDev)) {
                        JL_Log.d(TAG, "control connectedDev..... toDeviceSettings");
                        notAllowShowDeviceDialog();
                        toDeviceSettings();
                    } else {
                        JL_Log.d(TAG, "control connectedDev.....switchConnectedDevice");
                        mPresenter.switchConnectedDevice(connectedDev);
                    }
                } else {
                    if (mPresenter != null) {
                        isShowLoadingDialog = true;
                        mPresenter.connectHistoryDevice(history);
                    }
                    if (mDeviceListAdapter.isEditMode()) {
                        mDeviceListAdapter.setEditMode(false);
                    }
                }
            }
        }
    };

    private final OnItemLongClickListener mOnItemLongClickListener = new OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
            if (mDeviceListAdapter != null) {
                if (mPresenter != null && mPresenter.isDevConnecting()) {
                    onDevConnectionError(SConstant.ERR_DEV_CONNECTING, getString(R.string.device_connecting_tips));
                    return true;
                }
                mDeviceListAdapter.setEditMode(!mDeviceListAdapter.isEditMode());
                if (getActivity() != null) {
                    Vibrator vibrator = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {// 判断手机硬件是否有振动器
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .build();
                        vibrator.vibrate(10, audioAttributes);
                    }
                }
            }
            return true;
        }
    };

    private final OnDeleteItemListener mOnDeleteItemListener = (view, position, device) -> showIgnoreDeviceDialog(device);

    private final OnLocationItemListener mOnLocationItemListener = (view, position, device) -> {
        if (device != null) {
            Bundle bundle = new Bundle();
            bundle.putString(SConstant.KEY_SEARCH_DEVICE_ADDR, device.getAddress());
            CommonActivity.startCommonActivity(getActivity(), SearchDeviceFragment.class.getCanonicalName(), bundle);
        }
    };

    private final ScanDeviceDialog.OnScanDeviceOpListener mOnScanDeviceOpListener = new ScanDeviceDialog.OnScanDeviceOpListener() {
        @Override
        public void startScan() {
            if (mPresenter != null) {
                if (mPresenter.isAppGrantPermissions()) {
                    mPresenter.startScan();
                } else {
                    mPresenter.checkAppPermissions();
                }
            }
        }

        @Override
        public void stopScan() {
            if (mPresenter != null) {
                mPresenter.stopScan();
            }
        }

        @Override
        public void connectBtDevice(BluetoothDevice device, BleScanMessage bleScanMessage) {
            if (mPresenter != null) {
                isShowLoadingDialog = true;
                mPresenter.connectBtDevice(device, bleScanMessage);
            }
        }

        @Override
        public void disconnectBtDevice(BluetoothDevice device) {
            if (mPresenter != null) {
                mPresenter.disconnectBtDevice();
            }
        }
    };

    private final FastConnectEdrDialog.OnFastConnectListener mOnFastConnectListener = new FastConnectEdrDialog.OnFastConnectListener() {
        @Override
        public void onDismiss(BluetoothDevice device, boolean isUser) {
            JL_Log.d(TAG, "mOnFastConnectListener -onDismiss- >>  " + BluetoothUtil.printBtDeviceInfo(device) + ", isUser = " + isUser);
            if (isUser) {
                if (mPresenter != null && mPresenter.isConnectedDevice(device)) {
                    mPresenter.stopDeviceNotifyAdvInfo(device);
                }
            }
            setScanDeviceAndMessage(null, null);
            mFastConnectEdrDialog = null;
        }

        @Override
        public void onConnect(BluetoothDevice device, BleScanMessage bleScanMessage) {
            if (bleScanMessage == null || mPresenter == null) return;
            if (bleScanMessage.isEnableConnect()) {
                JL_Log.i(TAG, "mOnFastConnectListener :: onConnect  ===>> " + BluetoothUtil.printBtDeviceInfo(device));
                if (device != null) {
                    isShowLoadingDialog = true;
                    mPresenter.connectBtDevice(device, bleScanMessage);
                }
            } else {
                JL_Log.w(TAG, "mOnFastConnectListener :: onConnect  ===>> " + bleScanMessage);
                String edrAddr = bleScanMessage.getEdrAddr();
                if (edrAddr != null) {
                    mPresenter.connectEdrDevice(edrAddr);
                }
            }
        }

        @Override
        public void onFinish(BluetoothDevice device, BleScanMessage bleScanMessage) {
            JL_Log.e(TAG, "mOnFastConnectListener -onFinish- >> ");
            if (mPresenter == null || bleScanMessage == null) return;
            getBlacklistHandler().addData(bleScanMessage.getEdrAddr(), bleScanMessage.getSeq());
            handleDismissDialog(true);
            boolean isConnected = mPresenter.isConnectedDevice(device.getAddress()) || (BluetoothAdapter.checkBluetoothAddress(bleScanMessage.getEdrAddr()) && mPresenter.isConnectedDevice(bleScanMessage.getEdrAddr()));
            if ((!isConnected && bleScanMessage.isEnableConnect())) {
                setNeedToHomeDev(device);
                isShowLoadingDialog = true;
                mPresenter.connectBtDevice(device, bleScanMessage);
            } else if (isConnected) {
                toHomeActivity();
            }
            if (mPresenter.isConnectedDevice(device)) {
                mPresenter.stopDeviceNotifyAdvInfo(device);
            }
        }

        @Override
        public void onSettings(BluetoothDevice device, BleScanMessage bleScanMessage) {
            JL_Log.e(TAG, "mOnFastConnectListener -onSettings- >> ");
            if (mPresenter == null || bleScanMessage == null) return;
            getBlacklistHandler().addData(bleScanMessage.getEdrAddr(), bleScanMessage.getSeq());
            handleDismissDialog(true);
            boolean isConnected = mPresenter.isConnectedDevice(device.getAddress());
            if ((!isConnected && bleScanMessage.isEnableConnect())) {
                setNeedToSettingsDev(device);
                isShowLoadingDialog = true;
                mPresenter.connectBtDevice(device, bleScanMessage);
            } else if (isConnected) {
                toDeviceSettings();
            }
            if (mPresenter.isConnectedDevice(device)) {
                mPresenter.stopDeviceNotifyAdvInfo(device);
            }
        }
    };

    private final IBTOp mBTOp = new IBTOp() {
        @Override
        public boolean checkIsConnectingEdrDevice(String address) {
            return mPresenter != null && mPresenter.checkIsConnectingEdrDevice(address);
        }
    };

    private class DeviceListBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            JL_Log.i(TAG, "action : " + intent.getAction());
            switch (intent.getAction()) {
                case SConstant.ACTION_ACTIVITY_RESUME:
                    if (mHandler.hasMessages(MSG_START_ACTIVITY_TIMEOUT)) {
                        mHandler.removeMessages(MSG_START_ACTIVITY_TIMEOUT);
                    }
                    break;
                case SConstant.ACTION_FAST_CONNECT:
                    mHandler.sendEmptyMessage(MSG_FAST_CONNECT);
                    break;
                case SConstant.ACTION_DEVICE_UPGRADE:
                    if (mFloatingViewService != null) {
                        mFloatingViewService.dismissWindow(true);
                        stopFloatingService();
                    }
                    break;
                case SConstant.ACTION_ADD_DEVICE:
                    showScanDeviceDialog();
                    break;
            }
        }
    }
}
