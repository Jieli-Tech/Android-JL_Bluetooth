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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfo;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.databinding.FragmentScreenSaversBinding;
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
 * @desc 屏幕保护程序界面
 * @since 2023/12/7
 */
public class ScreenSaversFragment extends SelectPhotoFragment {

    private static final int MSG_EXIT = 0x1010;

    private FragmentScreenSaversBinding mBinding;
    private ChargingCaseSettingViewModel mViewModel;
    private ResourceFileAdapter mAdapter;

    private EventReceiver mReceiver;
    private ChargingCaseInfo mChargingCaseInfo;

    private final Handler uiHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_EXIT) {
            requireActivity().finish();
        }
        return true;
    });

    private final ActivityResultLauncher<Intent> editSaverLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        loadResourceData(mViewModel.getChargingCaseInfo().getCurrentScreenSaverPath());
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentScreenSaversBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if (bundle == null) {
            requireActivity().finish();
            return;
        }
        mChargingCaseInfo = bundle.getParcelable(SConstant.KEY_CHARGING_CASE_INFO);
        if (null == mChargingCaseInfo) {
            requireActivity().finish();
            return;
        }
        mViewModel = new ViewModelProvider(requireActivity()).get(ChargingCaseSettingViewModel.class);
        initUI();
        addObserver();
        registerEventReceiver();
        loadResourceData(mChargingCaseInfo.getCurrentScreenSaverPath());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterEventReceiver();
        uiHandler.removeCallbacksAndMessages(null);
    }

    private void initUI() {
        final ImageButton btnBack = requireActivity().findViewById(R.id.ib_content_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> exit());
        }
        mAdapter = new ResourceFileAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mAdapter.getItem(position);
            if (null == item) return;
            if (item.getItemType() == ResourceFileAdapter.TYPE_ADD_ITEM) {
                showSelectPhotoDialog();
                return;
            }
            if (item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM || mAdapter.isSelectedItem(item.getData()))
                return;
            ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            Bundle bundle = new Bundle();
            bundle.putString(SConstant.KEY_FILE_PATH, resourceFile.getPath());
            ContentActivity.startActivity(requireContext(), ConfirmScreenSaversFragment.class.getCanonicalName(), getString(R.string.screen_savers), bundle);
//            mAdapter.updateSelectedIndex(position);
        });
        mAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM) return;
            if (view.getId() == R.id.btn_edit) {
                Bundle bundle = new Bundle();
                bundle.putParcelable(SConstant.KEY_CHARGING_CASE_INFO, mChargingCaseInfo);
                ContentActivity.startActivityForRequest(ScreenSaversFragment.this, EditScreenSaverFragment.class.getCanonicalName(),
                        getString(R.string.custom), bundle, editSaverLauncher);
            }
        });
        mBinding.rvScreenSavers.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mBinding.rvScreenSavers.setAdapter(mAdapter);
        mBinding.rvScreenSavers.addItemDecoration(new GridSpacingItemDecoration(2, ValueUtil.dp2px(requireContext(), 10), false));
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                requireActivity().finish();
            }
        });
    }

    private void loadResourceData(String currentScreenPath) {
        JL_Log.d(TAG, "[loadResourceData] >>> " + currentScreenPath);
        List<BaseMultiItem<ResourceFile>> list = new ArrayList<>();
        File[] customFiles = getCustomFiles(mChargingCaseInfo.getAddress());
        if (customFiles.length == 0) {
            list.add(new BaseMultiItem<>(ResourceFileAdapter.TYPE_ADD_ITEM));
        } else {
            Arrays.sort(customFiles, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
            final File customFile = customFiles[0];
            list.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(
                    new ResourceFile(customFile.hashCode(), ResourceFile.TYPE_SCREEN_SAVER).setName(customFile.getName())
                            .setPath(customFile.getPath())));
        }
        //动态分辨率
        String dirName = AppUtil.formatString("%dx%d", getDeviceScreenWidth(), getDeviceScreenHeight());
        String resourceDirPath = FileUtil.createFilePath(requireContext(), requireContext().getPackageName(),
                SConstant.DIR_RESOURCE, SConstant.DIR_CHARGING_CASE, dirName, SConstant.DIR_SCREEN);
        List<File> files = new ArrayList<>();
        AppUtil.readFileByDir(resourceDirPath, SConstant.DIR_LOCK, files);
        for (File resource : files) {
            list.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(
                    new ResourceFile(resource.hashCode(), ResourceFile.TYPE_SCREEN_SAVER).setName(resource.getName())
                            .setPath(resource.getPath())));
        }
        mAdapter.setList(list);
        if (TextUtils.isEmpty(currentScreenPath)) {
//            mAdapter.updateSelectedIndex(list.get(0).getItemType() == ResourceFileAdapter.TYPE_ADD_ITEM ? 1 : 0);
        } else {
            mAdapter.updateSelectedItemByPath(currentScreenPath);
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
                            tryToTakePhoto(RCSPController.getInstance().getDeviceInfo());
                        }

                        @Override
                        public void onSelectFromAlbum(SelectPhotoDialog dialog) {
                            tryToSelectPhotoFromAlbum(RCSPController.getInstance().getDeviceInfo());
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

    private void exit() {
        final Intent intent = new Intent();
        if (mAdapter.getSelectedItem().size() > 0) {
            intent.putExtra(SConstant.KEY_CURRENT_SCREEN, mAdapter.getSelectedItem().get(0));
        }
        requireActivity().setResult(Activity.RESULT_OK, intent);
        uiHandler.removeMessages(MSG_EXIT);
        uiHandler.sendEmptyMessageDelayed(MSG_EXIT, 300L);
    }

    private void registerEventReceiver() {
        if (null == mReceiver) {
            mReceiver = new EventReceiver();
            IntentFilter filter = new IntentFilter(SConstant.ACTION_RESOURCE_INFO_CHANGE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireActivity().registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
            }else{
                requireActivity().registerReceiver(mReceiver, filter);
            }
        }
    }

    private void unregisterEventReceiver() {
        if (null != mReceiver) {
            requireActivity().unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    @Override
    public int getDeviceScreenWidth() {
        return mChargingCaseInfo.getScreenWidth();
    }

    @Override
    public int getDeviceScreenHeight() {
        return mChargingCaseInfo.getScreenHeight();
    }

    private class EventReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) return;
            final String action = intent.getAction();
            if (TextUtils.isEmpty(action)) return;
            if (SConstant.ACTION_RESOURCE_INFO_CHANGE.equals(action)) {
                int type = intent.getIntExtra(SConstant.KEY_RESOURCE_TYPE, 0);
                if (type == ResourceFile.TYPE_SCREEN_SAVER) { //屏幕资源信息
                    ResourceInfo resourceInfo = intent.getParcelableExtra(SConstant.KEY_RESOURCE_INFO);
                    if (null != resourceInfo) {
                        mChargingCaseInfo.setCurrentScreenSaver(resourceInfo);
                        loadResourceData(mChargingCaseInfo.getCurrentScreenSaverPath());
                    }
                }
            }
        }
    }
}