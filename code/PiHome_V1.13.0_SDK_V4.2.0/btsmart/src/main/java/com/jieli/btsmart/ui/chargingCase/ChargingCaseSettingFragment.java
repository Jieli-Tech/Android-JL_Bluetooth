package com.jieli.btsmart.ui.chargingCase;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfoChange;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.databinding.FragmentChargingCaseSettingBinding;
import com.jieli.btsmart.ui.widget.GridSpacingItemDecoration;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ValueUtil;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 智能充电仓设置页面
 * @since 2023/12/5
 */
public class ChargingCaseSettingFragment extends SelectPhotoFragment {

    /**
     * 更新屏幕亮度
     */
    private static final int MSG_UPDATE_BRIGHTNESS = 0x1230;

    /**
     * 彩屏仓功能逻辑实现
     */
    private ChargingCaseSettingViewModel mViewModel;
    /**
     * 彩屏仓功能UI处理
     */
    private FragmentChargingCaseSettingBinding mBinding;
    /**
     * 屏保适配器
     */
    private ResourceFileAdapter mScreenSaversAdapter;
    /**
     * 开机动画适配器
     */
    private ResourceFileAdapter mBootAnimAdapter;
    /**
     * 墙纸适配器
     */
    private ResourceFileAdapter mWallpaperAdapter;

    /**
     * 事件接收器
     */
    private EventReceiver mReceiver;

    /**
     * UI事件处理
     */
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViewModel.chargingCaseInfoMLD.removeObserver(mCaseInfoObserver);
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
        mBinding.tvMore.setOnClickListener(v -> ResourceListFragment.goToFragment(ChargingCaseSettingFragment.this,
                ChargingCaseInfo.TYPE_SCREEN_SAVER));
        mScreenSaversAdapter = new ResourceFileAdapter();
        mScreenSaversAdapter.setOnItemClickListener((adapter, view, position) -> {
            final ChargingCaseInfo chargingCaseInfo = mViewModel.getChargingCaseInfo();
            BaseMultiItem<ResourceFile> item = mScreenSaversAdapter.getItem(position);
            if (null == item) return;
            if (item.getItemType() == ResourceFileAdapter.TYPE_ADD_ITEM) {
                showSelectPhotoDialog(chargingCaseInfo, ChargingCaseInfo.TYPE_SCREEN_SAVER);
                return;
            }
            if (item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM
                    || mScreenSaversAdapter.isSelectedItem(item.getData())) {
                return;
            }
            final ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            handleResourceFile(mViewModel, resourceFile);
        });
        mScreenSaversAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mScreenSaversAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM) return;
            if (view.getId() == R.id.btn_edit) {
                CustomResourceFragment.goToFragment(ChargingCaseSettingFragment.this, ChargingCaseInfo.TYPE_SCREEN_SAVER);
            }
        });
        mBinding.rvScreenSavers.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mBinding.rvScreenSavers.setAdapter(mScreenSaversAdapter);
        mBinding.rvScreenSavers.addItemDecoration(new GridSpacingItemDecoration(2, ValueUtil.dp2px(requireContext(), 10), false));

        mBinding.tvMoreWallpaper.setOnClickListener(v -> ResourceListFragment.goToFragment(ChargingCaseSettingFragment.this,
                ChargingCaseInfo.TYPE_WALLPAPER));
        mWallpaperAdapter = new ResourceFileAdapter();
        mWallpaperAdapter.setOnItemClickListener((adapter, view, position) -> {
            final ChargingCaseInfo chargingCaseInfo = mViewModel.getChargingCaseInfo();
            BaseMultiItem<ResourceFile> item = mWallpaperAdapter.getItem(position);
            if (null == item) return;
            if (item.getItemType() == ResourceFileAdapter.TYPE_ADD_ITEM) {
                showSelectPhotoDialog(chargingCaseInfo, ChargingCaseInfo.TYPE_WALLPAPER);
                return;
            }
            if (item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM
                    || mWallpaperAdapter.isSelectedItem(item.getData())) {
                return;
            }
            final ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            handleResourceFile(mViewModel, resourceFile);
        });
        mWallpaperAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mWallpaperAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM) return;
            if (view.getId() == R.id.btn_edit) {
                CustomResourceFragment.goToFragment(ChargingCaseSettingFragment.this, ChargingCaseInfo.TYPE_WALLPAPER);
            }
        });
        mBinding.rvWallpaper.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mBinding.rvWallpaper.setAdapter(mWallpaperAdapter);
        mBinding.rvWallpaper.addItemDecoration(new GridSpacingItemDecoration(2, ValueUtil.dp2px(requireContext(), 10), false));

        mBootAnimAdapter = new ResourceFileAdapter();
        mBootAnimAdapter.setOnItemClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mBootAnimAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM
                    || mBootAnimAdapter.isSelectedItem(item.getData())) {
                return;
            }
            final ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            mBootAnimAdapter.updateSelectedIndex(position);
        });
        mBinding.rvBootAnim.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mBinding.rvBootAnim.setAdapter(mBootAnimAdapter);
        mBinding.rvBootAnim.addItemDecoration(new GridSpacingItemDecoration(2, ValueUtil.dp2px(requireContext(), 10), false));
    }

    @SuppressLint("WrongConstant")
    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        mViewModel.initMLD.observe(getViewLifecycleOwner(), isInit -> {
            if (isInit) {
                final ChargingCaseInfo info = mViewModel.getChargingCaseInfo();
                mScreenSaversAdapter.chip = info.getChip();
                mWallpaperAdapter.chip = info.getChip();
                mWallpaperAdapter.chip = info.getChip();
                mViewModel.syncDeviceState();
                return;
            }
            finish(100, () -> {
                if (mViewModel.isDevConnected()) {
                    showTips(getString(R.string.function_init_fail));
                }
            });
        });
        mViewModel.resourcePathMLD.observe(getViewLifecycleOwner(), dirPath -> {
            final ChargingCaseInfo info = mViewModel.getChargingCaseInfo();
            loadResources(info, ChargingCaseInfo.TYPE_SCREEN_SAVER);
            if (info.isJL701N()) {
                UIHelper.gone(mBinding.cvWallpaper);
                UIHelper.show(mBinding.cvBootAnim);
                loadResources(info, ChargingCaseInfo.TYPE_BOOT_ANIM);
            } else {
                UIHelper.gone(mBinding.cvBootAnim);
                UIHelper.gone(mBinding.cvWallpaper);
//                loadResources(info, ChargingCaseInfo.TYPE_WALLPAPER);
            }
        });
        mViewModel.functionResultMLD.observe(getViewLifecycleOwner(), functionResult -> {
            final int function = functionResult.getData();
            JL_Log.d(TAG, "functionResultMLD", "function : " + ChargingCaseInfo.printFunction(function)
                    + ", code : " + functionResult.getCode() + ", message : " + functionResult.getMessage());
            if (!functionResult.isSuccess()) {
                if (function == ChargingCaseInfo.FUNC_CURRENT_WALLPAPER) {
                    UIHelper.gone(mBinding.cvWallpaper);
                }
                return;
            }
            final ChargingCaseInfo info = mViewModel.getChargingCaseInfo();
            switch (function) {
                case ChargingCaseInfo.FUNC_BRIGHTNESS: {
                    mBinding.sbBrightness.setProgress(info.getBrightness());
                    break;
                }
                case ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER: {
                    loadResources(info, ChargingCaseInfo.TYPE_SCREEN_SAVER);
                    break;
                }
                case ChargingCaseInfo.FUNC_CURRENT_BOOT_ANIM: {
                    loadResources(info, ChargingCaseInfo.TYPE_BOOT_ANIM);
                    break;
                }
//                case ChargingCaseSettingViewModel.FUNC_BROWSE_WALLPAPER:
                case ChargingCaseInfo.FUNC_CURRENT_WALLPAPER: {
                    if (info.getCurrentWallpaper() == null) {
                        UIHelper.gone(mBinding.cvWallpaper);
                    } else {
                        UIHelper.show(mBinding.cvWallpaper);
                    }
                    loadResources(info, ChargingCaseInfo.TYPE_WALLPAPER);
                    break;
                }
            }
        });
        mViewModel.chargingCaseInfoMLD.observeForever(mCaseInfoObserver);
    }

    private void loadResources(@NonNull ChargingCaseInfo info, @ChargingCaseInfo.ResourceType int resourceType) {
        List<BaseMultiItem<ResourceFile>> list = ChargingBinUtil.readResourceFiles(requireContext(), info, resourceType);
        JL_Log.d(TAG, "loadResources", "resourceType : " + resourceType + ", resource size : " + list.size()
                + "\n " + info);
        int end = Math.min(list.size(), 4);
        String usingResourcePath;
        switch (resourceType) {
            case ChargingCaseInfo.TYPE_SCREEN_SAVER: {
                usingResourcePath = info.getCurrentScreenSaverPath();
                mScreenSaversAdapter.setList(list.subList(0, end));
                if (mScreenSaversAdapter.getOptionalItemSize() > 0 || list.size() > 4) {
                    UIHelper.show(mBinding.tvMore);
                } else {
                    UIHelper.gone(mBinding.tvMore);
                }
                if (!TextUtils.isEmpty(usingResourcePath)) {
                    mScreenSaversAdapter.updateSelectedItemByPath(usingResourcePath);
                }
                break;
            }
            case ChargingCaseInfo.TYPE_BOOT_ANIM:
                usingResourcePath = info.getCurrentBootAnimPath();
                mBootAnimAdapter.setList(list.subList(0, end));
                if (!TextUtils.isEmpty(usingResourcePath)) {
                    mBootAnimAdapter.updateSelectedItemByPath(usingResourcePath);
                }
                break;
            case ChargingCaseInfo.TYPE_WALLPAPER: {
                usingResourcePath = info.getCurrentWallpaperPath();
                mWallpaperAdapter.setList(list.subList(0, end));
                if (mWallpaperAdapter.getOptionalItemSize() > 0 || list.size() > 4) {
                    UIHelper.show(mBinding.tvMoreWallpaper);
                } else {
                    UIHelper.gone(mBinding.tvMoreWallpaper);
                }
                if (!TextUtils.isEmpty(usingResourcePath)) {
                    mWallpaperAdapter.updateSelectedItemByPath(usingResourcePath);
                }
                break;
            }
        }
    }

    private void updateDeviceInfo(ChargingCaseInfoChange infoChange) {
        if (null == infoChange) return;
        final ChargingCaseInfo info = infoChange.getChargingCaseInfo();
        switch (infoChange.getFunc()) {
            case ChargingCaseInfo.FUNC_BRIGHTNESS:
                mBinding.sbBrightness.setProgress(info.getBrightness());
                break;
            case ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER:
                if (info.isJL701N()) {
                    mScreenSaversAdapter.updateSelectedIndexByResource(info.getCurrentScreenSaver());
                } else {
                    loadResources(info, ChargingCaseInfo.TYPE_SCREEN_SAVER);
                }
                break;
            case ChargingCaseInfo.FUNC_CURRENT_BOOT_ANIM:
                mBootAnimAdapter.updateSelectedIndexByResource(info.getCurrentBootAnim());
                break;
            case ChargingCaseInfo.FUNC_CURRENT_WALLPAPER:
                if (info.getCurrentWallpaper() == null) {
                    UIHelper.gone(mBinding.cvWallpaper);
                    return;
                }
                UIHelper.show(mBinding.cvWallpaper);
                loadResources(info, ChargingCaseInfo.TYPE_WALLPAPER);
                break;
        }
    }

    private void registerEventReceiver() {
        if (null != mReceiver) return;
        mReceiver = new EventReceiver();
        ContextCompat.registerReceiver(requireContext(), mReceiver, new IntentFilter(SConstant.ACTION_USING_RESOURCE_CHANGE), ContextCompat.RECEIVER_EXPORTED);
    }

    private void unregisterEventReceiver() {
        if (null == mReceiver) return;
        requireActivity().unregisterReceiver(mReceiver);
        mReceiver = null;
    }

    private final Observer<ChargingCaseInfoChange> mCaseInfoObserver = this::updateDeviceInfo;

    private class EventReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) return;
            final String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;
            if (SConstant.ACTION_USING_RESOURCE_CHANGE.equals(action)) { //上传成功，更新当前屏幕信息
                int resourceType = intent.getIntExtra(SConstant.KEY_RESOURCE_TYPE, ChargingCaseInfo.TYPE_SCREEN_SAVER);
                JL_Log.d(TAG, "ACTION_USING_RESOURCE_CHANGE", "resourceType : " + ChargingCaseInfo.printResourceType(resourceType));
                syncResource(mViewModel, resourceType);
            }
        }
    }
}