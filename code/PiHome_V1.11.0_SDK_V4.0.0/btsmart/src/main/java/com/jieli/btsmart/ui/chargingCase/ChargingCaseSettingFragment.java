package com.jieli.btsmart.ui.chargingCase;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfo;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.databinding.FragmentChargingCaseSettingBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.widget.GridSpacingItemDecoration;
import com.jieli.btsmart.ui.widget.dialog.SelectPhotoDialog;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.ValueUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 智能充电仓设置页面
 * @since 2023/12/5
 */
public class ChargingCaseSettingFragment extends SelectPhotoFragment {

    private static final int MSG_UPDATE_BRIGHTNESS = 0x1230;

    private ChargingCaseSettingViewModel mViewModel;
    private FragmentChargingCaseSettingBinding mBinding;
    private ResourceFileAdapter mScreenSaversAdapter;
    private ResourceFileAdapter mBootAnimAdapter;

    private EventReceiver mReceiver;
    private boolean isSyncState;

    private final Handler uiHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_UPDATE_BRIGHTNESS) {
                int progress = msg.arg1;
                mViewModel.setBrightness(progress);
            }
            return true;
        }
    });

    private final ActivityResultLauncher<Intent> screenSaverLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
        ResourceFile file = result.getData().getParcelableExtra(SConstant.KEY_CURRENT_SCREEN);
        if (null == file) return;
        loadResourceData(file.getPath());
    });

    private final ActivityResultLauncher<Intent> editSaverLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        loadResourceData(mViewModel.getChargingCaseInfo().getCurrentScreenSaverPath());
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentChargingCaseSettingBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(ChargingCaseSettingViewModel.class);
        initUI();
        addObserver();
        registerEventReceiver();
        mViewModel.readScreenInfo();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isSyncState = false;
        mViewModel.deviceInfoMLD.removeObserver(mCaseInfoObserver);
        unregisterEventReceiver();
    }

    private void initUI() {
        mBinding.sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                uiHandler.removeMessages(MSG_UPDATE_BRIGHTNESS);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                uiHandler.removeMessages(MSG_UPDATE_BRIGHTNESS);
                uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_UPDATE_BRIGHTNESS, seekBar.getProgress(), 0), 300);
            }
        });
        mBinding.tvMore.setOnClickListener(v -> {
            final DeviceInfo deviceInfo = mViewModel.getDeviceInfo();
            if (null == deviceInfo) return;
            final ChargingCaseInfo info = mViewModel.getChargingCaseInfo();
            Bundle bundle = new Bundle();
            bundle.putParcelable(SConstant.KEY_CHARGING_CASE_INFO, info);
            ContentActivity.startActivityForRequest(ChargingCaseSettingFragment.this, ScreenSaversFragment.class.getCanonicalName(),
                    getString(R.string.screen_savers), bundle, screenSaverLauncher);
        });
        mScreenSaversAdapter = new ResourceFileAdapter();
        mScreenSaversAdapter.setOnItemClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mScreenSaversAdapter.getItem(position);
            if (null == item) return;
            if (item.getItemType() == ResourceFileAdapter.TYPE_ADD_ITEM) {
                showSelectPhotoDialog();
                return;
            }
            if (item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM || mScreenSaversAdapter.isSelectedItem(item.getData()))
                return;
            final ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            Bundle bundle = new Bundle();
            bundle.putString(SConstant.KEY_FILE_PATH, resourceFile.getPath());
            bundle.putInt(SConstant.KEY_DEVICE_SCREEN_WIDTH, getDeviceScreenWidth());
            bundle.putInt(SConstant.KEY_DEVICE_SCREEN_HEIGHT, getDeviceScreenHeight());
            ContentActivity.startActivity(requireContext(), ConfirmScreenSaversFragment.class.getCanonicalName(), getString(R.string.screen_savers), bundle);
//            mScreenSaversAdapter.updateSelectedIndex(position);
        });
        mScreenSaversAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mScreenSaversAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM) return;
            if (view.getId() == R.id.btn_edit) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_CHARGING_CASE_INFO, mViewModel.getChargingCaseInfo());
                ContentActivity.startActivityForRequest(ChargingCaseSettingFragment.this, EditScreenSaverFragment.class.getCanonicalName(),
                        getString(R.string.custom), bundle, editSaverLauncher);
            }
        });
        mBinding.rvScreenSavers.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mBinding.rvScreenSavers.setAdapter(mScreenSaversAdapter);
        mBinding.rvScreenSavers.addItemDecoration(new GridSpacingItemDecoration(2, ValueUtil.dp2px(requireContext(), 10), false));

        mBootAnimAdapter = new ResourceFileAdapter();
        mBootAnimAdapter.setOnItemClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mBootAnimAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM || mScreenSaversAdapter.isSelectedItem(item.getData()))
                return;
            final ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            mBootAnimAdapter.updateSelectedIndex(position);
        });
        mBinding.rvBootAnim.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mBinding.rvBootAnim.setAdapter(mBootAnimAdapter);
        mBinding.rvBootAnim.addItemDecoration(new GridSpacingItemDecoration(2, ValueUtil.dp2px(requireContext(), 10), false));
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                requireActivity().finish();
            }
        });
        mViewModel.resourcePathMLD.observe(getViewLifecycleOwner(), dirPath -> {
            final ChargingCaseInfo info = mViewModel.getChargingCaseInfo();
            loadResourceData(info.getCurrentScreenSaverPath());
            loadBootAnimResource(info.getCurrentBootAnimPath());
        });
        mViewModel.functionResultMLD.observe(getViewLifecycleOwner(), functionResult -> {
            final int function = functionResult.getData();
            JL_Log.d(TAG, "functionResultMLD", "function = " + function + ", code = " + functionResult.getCode()
                    + ", " + functionResult.getMessage());
            if (!functionResult.isSuccess()) {
                functionFailureHandle(function);
                return;
            }
            final ChargingCaseInfo info = mViewModel.getChargingCaseInfo();
            switch (function) {
                case ChargingCaseSettingViewModel.FUNC_BRIGHTNESS: {
                    mBinding.sbBrightness.setProgress(info.getBrightness());
                    break;
                }
                case ChargingCaseSettingViewModel.FUNC_SCREEN_SAVER: {
                    loadResourceData(mViewModel.getChargingCaseInfo().getCurrentScreenSaverPath());
                    break;
                }
                case ChargingCaseSettingViewModel.FUNC_BOOT_ANIM: {
                    loadBootAnimResource(mViewModel.getChargingCaseInfo().getCurrentBootAnimPath());
                    break;
                }
            }
            functionFailureHandle(function);
        });
        mViewModel.deviceInfoMLD.observeForever(mCaseInfoObserver);
    }

    private void loadResourceData(String currentScreenPath) {
        DeviceInfo deviceInfo = mViewModel.getDeviceInfo();
        if (null == deviceInfo) return;
        List<BaseMultiItem<ResourceFile>> list = new ArrayList<>();
        File[] customFiles = getCustomFiles(deviceInfo.getEdrAddr());
        if (customFiles.length == 0) {
            list.add(new BaseMultiItem<>(ResourceFileAdapter.TYPE_ADD_ITEM));
        } else {
            Arrays.sort(customFiles, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
            final File customFile = customFiles[0];
            list.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(
                    new ResourceFile(customFile.hashCode(), ResourceFile.TYPE_SCREEN_SAVER).setName(customFile.getName())
                            .setPath(customFile.getPath())));
        }
        String dirName = AppUtil.formatString("%dx%d", getDeviceScreenWidth(), getDeviceScreenHeight()); //动态调整屏幕分辨率
//        String dirName = AppUtil.formatString("%dx%d", ChargingCaseInfo.SCREEN_WIDTH, ChargingCaseInfo.SCREEN_HEIGHT);
        String resourceDirPath = FileUtil.createFilePath(requireContext(), requireContext().getPackageName(),
                SConstant.DIR_RESOURCE, SConstant.DIR_CHARGING_CASE, dirName, SConstant.DIR_SCREEN);
        List<File> files = new ArrayList<>();
        AppUtil.readFileByDir(resourceDirPath, SConstant.DIR_LOCK, files);
        for (File resource : files) {
            list.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(
                    new ResourceFile(resource.hashCode(), ResourceFile.TYPE_SCREEN_SAVER).setName(resource.getName())
                            .setPath(resource.getPath())));
        }
        mScreenSaversAdapter.setList(list.subList(0, 4));
        mBinding.tvMore.setVisibility(list.size() > 4 ? View.VISIBLE : View.GONE);
        if (TextUtils.isEmpty(currentScreenPath)) {
//            mScreenSaversAdapter.updateSelectedIndex(list.get(0).getItemType() == ResourceFileAdapter.TYPE_ADD_ITEM ? 1 : 0);
        } else {
            mScreenSaversAdapter.updateSelectedItemByPath(currentScreenPath);
        }
    }

    private void loadBootAnimResource(String currentBootPath) {
        List<BaseMultiItem<ResourceFile>> bootAnimArray = new ArrayList<>();
        String dirName = AppUtil.formatString("%dx%d", getDeviceScreenWidth(), getDeviceScreenHeight()); //动态分辨率
//        String dirName = AppUtil.formatString("%dx%d", ChargingCaseInfo.SCREEN_WIDTH, ChargingCaseInfo.SCREEN_HEIGHT);
        String resourceDirPath = FileUtil.createFilePath(requireContext(), requireContext().getPackageName(),
                SConstant.DIR_RESOURCE, SConstant.DIR_CHARGING_CASE, dirName, SConstant.DIR_BOOT);
        List<File> files = new ArrayList<>();
        AppUtil.readFileByDir(resourceDirPath, files);
        for (File resource : files) {
            bootAnimArray.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(
                    new ResourceFile(resource.hashCode(), ResourceFile.TYPE_BOOT_ANIM).setName(resource.getName())
                            .setPath(resource.getPath())));
        }
        mBootAnimAdapter.setList(bootAnimArray);
        if (TextUtils.isEmpty(currentBootPath)) {
            if (bootAnimArray.size() > 0) {
                mBootAnimAdapter.updateSelectedIndex(0);
            }
        } else {
            mBootAnimAdapter.updateSelectedItemByPath(currentBootPath);
        }
    }

    private void showSelectPhotoDialog() {
        final String tag = "select_photo";
        SelectPhotoDialog dialog = (SelectPhotoDialog) getChildFragmentManager().findFragmentByTag(tag);
        if (null == dialog) {
            dialog = new SelectPhotoDialog.Builder()
                    .listener(new SelectPhotoDialog.OnSelectPhotoListener() {
                        @Override
                        public void onTakePhoto(SelectPhotoDialog dialog) {
                            tryToTakePhoto(mViewModel.getDeviceInfo());
                        }

                        @Override
                        public void onSelectFromAlbum(SelectPhotoDialog dialog) {
                            tryToSelectPhotoFromAlbum(mViewModel.getDeviceInfo());
                        }

                        @Override
                        public void onCancel(SelectPhotoDialog dialog) {

                        }
                    }).build();
        }
        if (!dialog.isShow() && !isDetached() && isAdded()) {
            dialog.show(getChildFragmentManager(), tag);
        }
    }

    private void updateDeviceInfo(ChargingCaseInfo info) {
        if (null == info) return;
        mBinding.sbBrightness.setProgress(info.getBrightness());
        mScreenSaversAdapter.updateSelectedIndexByFile(info.getCurrentScreenSaver());
        mScreenSaversAdapter.updateSelectedIndexByFile(info.getCurrentBootAnim());
    }

    private void functionFailureHandle(int function) {
        if (!isSyncState) return;
        final ChargingCaseInfo info = mViewModel.getChargingCaseInfo();
        switch (function) {
            case ChargingCaseSettingViewModel.FUNC_BRIGHTNESS: {
                if (info.getCurrentScreenSaver() == null) {
                    mViewModel.getCurrentScreenSaver();
                    return;
                }
                break;
            }
            case ChargingCaseSettingViewModel.FUNC_SCREEN_SAVER: {
                if (info.getCurrentBootAnim() == null) {
                    mViewModel.getCurrentBootAnim();
                    return;
                }
                break;
            }
        }
        isSyncState = false;
    }

    private void syncState() {
        if (isSyncState) return;
        isSyncState = true;
        mViewModel.getBrightness();
    }

    private void registerEventReceiver() {
        if (null == mReceiver) {
            mReceiver = new EventReceiver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireActivity().registerReceiver(mReceiver, new IntentFilter(SConstant.ACTION_SCREEN_SAVER_CHANGE), Context.RECEIVER_EXPORTED);
            }else{
                requireActivity().registerReceiver(mReceiver, new IntentFilter(SConstant.ACTION_SCREEN_SAVER_CHANGE));
            }
        }
    }

    private void unregisterEventReceiver() {
        if (null != mReceiver) {
            requireActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private final Observer<ChargingCaseInfo> mCaseInfoObserver = this::updateDeviceInfo;

    @Override
    public int getDeviceScreenWidth() {
        return mViewModel.getChargingCaseInfo().getScreenWidth();
    }

    @Override
    public int getDeviceScreenHeight() {
        return mViewModel.getChargingCaseInfo().getScreenHeight();
    }

    private class EventReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) return;
            final String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;
            if (SConstant.ACTION_SCREEN_SAVER_CHANGE.equals(action)) { //上传成功，更新一下当前屏幕信息
                String filePath = intent.getStringExtra(SConstant.KEY_FILE_PATH);
                JL_Log.d(TAG, "ACTION_SCREEN_SAVER_CHANGE", "filePath : " + filePath);
                mViewModel.getCurrentScreenSaver();
//                loadResourceData(mViewModel.getChargingCaseInfo().getCurrentScreenSaverPath());
            }
        }
    }
}