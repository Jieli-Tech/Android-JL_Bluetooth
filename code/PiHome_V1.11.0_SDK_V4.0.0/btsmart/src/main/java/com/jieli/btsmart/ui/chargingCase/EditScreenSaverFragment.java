package com.jieli.btsmart.ui.chargingCase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.chargingcase.ChargingCaseInfo;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.databinding.FragmentEditScreenSaverBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.widget.GridSpacingItemDecoration;
import com.jieli.btsmart.ui.widget.dialog.SelectPhotoDialog;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.base.Jl_BaseActivity;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 自定义屏幕保护图片页面
 * @since 2023/12/8
 */
public class EditScreenSaverFragment extends SelectPhotoFragment {
    private FragmentEditScreenSaverBinding mBinding;
    private ChargingCaseSettingViewModel mViewModel;
    private ResourceFileAdapter mAdapter;

    private TextView btnTopRight;

    private EventReceiver mReceiver;

    private ChargingCaseInfo chargingCaseInfo;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentEditScreenSaverBinding.inflate(inflater, container, false);
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
        chargingCaseInfo = bundle.getParcelable(SConstant.KEY_CHARGING_CASE_INFO);
        if (chargingCaseInfo == null) {
            requireActivity().finish();
            return;
        }
        mViewModel = new ViewModelProvider(requireActivity()).get(ChargingCaseSettingViewModel.class);
        registerEventReceiver();
        initUI();
        addObserver();
        loadResource(chargingCaseInfo.getCurrentScreenSaverPath());
    }

    @Override
    public void onDestroyView() {
        unregisterEventReceiver();
        super.onDestroyView();
    }

    private void initUI() {
        if (requireActivity() instanceof Jl_BaseActivity) {
            ((Jl_BaseActivity) requireActivity()).setCustomBackPress(() -> {
                if (mAdapter.isEditMode()) {
                    mAdapter.updateState(ResourceFileAdapter.MODE_NORMAL);
                    return true;
                }
                return false;
            });
        }

        ImageButton btnTopLeft = requireActivity().findViewById(R.id.ib_content_back);
        btnTopRight = requireActivity().findViewById(R.id.tv_content_right);
        btnTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_edit_black, 0);
        btnTopLeft.setOnClickListener(v -> {
            if (mAdapter.isEditMode()) {
                mAdapter.updateState(ResourceFileAdapter.MODE_NORMAL);
                return;
            }
            requireActivity().finish();
        });
        btnTopRight.setOnClickListener(v -> {
            if (!mAdapter.isEditMode()) {
                mAdapter.updateState(ResourceFileAdapter.MODE_EDIT);
                return;
            }
            if (mAdapter.isAllSelected()) {
                mAdapter.clearAllSelected();
                return;
            }
            mAdapter.updateState(ResourceFileAdapter.MODE_EDIT_ALL_SELECTED);
        });
        mBinding.fabAdd.setOnClickListener(v -> {
            if (mAdapter.isEditMode()) return;
            showSelectPhotoDialog();
        });
        mBinding.tvDelete.setOnClickListener(v -> {
            if (!mAdapter.isEditMode()) return;
            final List<ResourceFile> list = mAdapter.getSelectedItem();
            if (list.isEmpty()) {
                ToastUtil.showToastShort(getString(R.string.select_tips));
                return;
            }
            for (ResourceFile file : list) {
                File deleteFile = new File(file.getPath());
                FileUtil.deleteFile(deleteFile);
            }
            mAdapter.updateState(ResourceFileAdapter.MODE_NORMAL);
        });

        mAdapter = new ResourceFileAdapter(false, state -> {
            switch (state) {
                case ResourceFileAdapter.MODE_EDIT: {
                    if (btnTopRight != null) {
                        btnTopRight.setText(getString(R.string.select_all));
                        btnTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    }
                    UIHelper.gone(mBinding.fabAdd);
                    UIHelper.show(mBinding.tvDelete);
                    break;
                }
                case ResourceFileAdapter.MODE_EDIT_ALL_SELECTED: {
                    if (btnTopRight != null) {
                        btnTopRight.setText(getString(R.string.unselect_all));
                        btnTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                    }
                    break;
                }
                default: {
                    if (btnTopRight != null) {
                        btnTopRight.setText("");
                        btnTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_edit_black, 0);
                    }
                    UIHelper.gone(mBinding.tvDelete);
                    UIHelper.show(mBinding.fabAdd);
                    loadResource(chargingCaseInfo.getCurrentScreenSaverPath());
                    break;
                }
            }
        });
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM)
                return;
            if (mAdapter.isEditMode()) {
                mAdapter.updateSelectedIndex(position);
                return;
            }
            if (mAdapter.isSelectedItem(item.getData())) return;
            final ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            Bundle bundle = new Bundle();
            bundle.putString(SConstant.KEY_FILE_PATH, resourceFile.getPath());
            bundle.putInt(SConstant.KEY_DEVICE_SCREEN_WIDTH, getDeviceScreenWidth());
            bundle.putInt(SConstant.KEY_DEVICE_SCREEN_HEIGHT, getDeviceScreenHeight());
            ContentActivity.startActivity(requireContext(), ConfirmScreenSaversFragment.class.getCanonicalName(), getString(R.string.screen_savers), bundle);
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

    private void loadResource(String currentScreenPath) {
        List<BaseMultiItem<ResourceFile>> list = new ArrayList<>();
        File[] customFiles = getCustomFiles(chargingCaseInfo.getAddress());
        if (null != customFiles && customFiles.length > 0) {
            Arrays.sort(customFiles, (o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified()));
            for (File customFile : customFiles) {
                list.add(new BaseMultiItem<ResourceFile>(ResourceFileAdapter.TYPE_FILE_ITEM).setData(
                        new ResourceFile(customFile.hashCode(), ResourceFile.TYPE_SCREEN_SAVER).setName(customFile.getName())
                                .setPath(customFile.getPath())));
            }
        }
        mAdapter.setList(list);
        mAdapter.updateSelectedItemByPath(currentScreenPath);
        if (mAdapter.isEditMode()) {
            UIHelper.show(btnTopRight);
        } else {
            if (mAdapter.getData().size() > mAdapter.getSelectedItem().size()) {
                UIHelper.show(btnTopRight);
            } else {
                UIHelper.gone(btnTopRight);
            }
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

    private void registerEventReceiver() {
        if (null == mReceiver) {
            mReceiver = new EventReceiver();
            IntentFilter filter = new IntentFilter(SConstant.ACTION_RESOURCE_INFO_CHANGE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireActivity().registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
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
        return chargingCaseInfo.getScreenWidth();
    }

    @Override
    public int getDeviceScreenHeight() {
        return chargingCaseInfo.getScreenHeight();
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
                        chargingCaseInfo.setCurrentScreenSaver(resourceInfo);
                        loadResource(chargingCaseInfo.getCurrentScreenSaverPath());
                    }
                }
            }
        }
    }
}