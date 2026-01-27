package com.jieli.btsmart.ui.chargingCase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.databinding.FragmentResourceListBinding;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseActivity;
import com.jieli.btsmart.ui.widget.GridSpacingItemDecoration;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ValueUtil;

import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 资源列表界面
 * @since 2023/12/7
 */
public class ResourceListFragment extends SelectPhotoFragment {

    public static void goToFragment(@NonNull Fragment fragment, @ChargingCaseInfo.ResourceType int resourceType) {
        Bundle bundle = new Bundle();
        bundle.putInt(SConstant.KEY_RESOURCE_TYPE, resourceType);
        String title;
        switch (resourceType) {
            case ChargingCaseInfo.TYPE_BOOT_ANIM:
                title = fragment.getString(R.string.boot_animation);
                break;
            case ChargingCaseInfo.TYPE_WALLPAPER:
                title = fragment.getString(R.string.wallpaper);
                break;
            case ChargingCaseInfo.TYPE_SCREEN_SAVER:
            default:
                title = fragment.getString(R.string.screen_savers);
                break;
        }
        ContentActivity.startActivityForRequest(fragment, ResourceListFragment.class.getCanonicalName(),
                title, bundle, null);
    }

    /**
     * 资源列表UI处理
     */
    private FragmentResourceListBinding mBinding;
    /**
     * 资源列表逻辑
     */
    private ChargingCaseSettingViewModel mViewModel;
    /**
     * 资源文件适配器
     */
    private ResourceFileAdapter mAdapter;

    /**
     * 顶部栏左上角控件
     */
    private TextView tvTopLeft;
    /**
     * 顶部栏右上角控件
     */
    private TextView tvTopRight;

    /**
     * 资源类型
     */
    @ChargingCaseInfo.ResourceType
    private int resourceType;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentResourceListBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if (bundle == null) {
            finish();
            return;
        }
        resourceType = bundle.getInt(SConstant.KEY_RESOURCE_TYPE, ChargingCaseInfo.TYPE_SCREEN_SAVER);
        mViewModel = new ViewModelProvider(requireActivity()).get(ChargingCaseSettingViewModel.class);
        initUI();
        addObserver();
    }

    private void initUI() {
        if (requireActivity() instanceof BaseActivity) {
            ((BaseActivity) requireActivity()).setCustomBackPress(() -> {
                exit();
                return true;
            });
        }

        tvTopLeft = requireActivity().findViewById(R.id.tv_left);
        tvTopRight = requireActivity().findViewById(R.id.tv_right);
        if (tvTopLeft != null) {
            tvTopLeft.setOnClickListener(v -> exit());
        }
        if (tvTopRight != null) {
            tvTopRight.setOnClickListener(v -> {
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
        }

        mAdapter = new ResourceFileAdapter(true, this::handleEditState);
        mAdapter.chip = mViewModel.getChargingCaseInfo().getChip();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mAdapter.getItem(position);
            if (null == item) return;
            if (item.getItemType() == ResourceFileAdapter.TYPE_ADD_ITEM) {
                showSelectPhotoDialog(mViewModel.getChargingCaseInfo(), resourceType);
                return;
            }
            if (item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM)
                return;
            if (mAdapter.isEditMode()) {
                mAdapter.updateSelectedIndex(position);
                return;
            }
            if (mAdapter.isSelectedItem(item.getData())) return;
            ResourceFile resourceFile = item.getData();
            if (null == resourceFile) return;
            handleResourceFile(mViewModel, resourceFile);
        });
        mAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            BaseMultiItem<ResourceFile> item = mAdapter.getItem(position);
            if (null == item || item.getItemType() != ResourceFileAdapter.TYPE_FILE_ITEM) return;
            if (view.getId() == R.id.btn_edit) {
                if (mAdapter.isEditMode()) return;
                CustomResourceFragment.goToFragment(ResourceListFragment.this, resourceType);
            }
        });
        mBinding.rvResourceList.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        mBinding.rvResourceList.setAdapter(mAdapter);
        mBinding.rvResourceList.addItemDecoration(new GridSpacingItemDecoration(2, ValueUtil.dp2px(requireContext(), 10), false));

        mBinding.tvDelete.setOnClickListener(v -> {
            if (!mAdapter.isEditMode()) return;
            final List<ResourceFile> list = mAdapter.getSelectedItem();
            if (list.isEmpty()) {
                showTips(getString(R.string.select_tips));
                return;
            }
            mViewModel.deleteResourceList(list);
        });
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), deviceConnectionData -> {
            if (deviceConnectionData.getStatus() != StateCode.CONNECTION_OK) {
                finish();
            }
        });
        mViewModel.chargingCaseInfoMLD.observe(getViewLifecycleOwner(), infoChange -> {
            int func = 0;
            switch (resourceType) {
                case ChargingCaseInfo.TYPE_SCREEN_SAVER:
                    func = ChargingCaseInfo.FUNC_CURRENT_SCREEN_SAVER;
                    break;
                case ChargingCaseInfo.TYPE_BOOT_ANIM:
                    func = ChargingCaseInfo.FUNC_CURRENT_BOOT_ANIM;
                    break;
                case ChargingCaseInfo.TYPE_WALLPAPER:
                    func = ChargingCaseInfo.FUNC_CURRENT_WALLPAPER;
                    break;
            }
            if (infoChange.getFunc() == func) {
                loadResourceList(infoChange.getChargingCaseInfo(), resourceType);
            }
        });
        mViewModel.deleteResourceMLD.observe(getViewLifecycleOwner(), state -> {
            if (state.getState() == StateResult.STATE_WORKING) {
                showLoadingDialog(getString(R.string.deleting));
                return;
            }
            dismissLoadingDialog();
            if (state.getState() == StateResult.STATE_FINISH) {
                mAdapter.updateState(ResourceFileAdapter.MODE_NORMAL);
                if (state.isSuccess()) {
                    syncResource(mViewModel, resourceType);
                } else {
                    showTips(getString(R.string.delete_file_fail));
                }
            }
        });
    }

    private void loadResourceList(@NonNull ChargingCaseInfo info, @ChargingCaseInfo.ResourceType int type) {
        List<BaseMultiItem<ResourceFile>> list = ChargingBinUtil.readResourceFiles(requireContext(), info, type);
        String usingResourcePath = info.getCurrentResourcePath(type);
        JL_Log.d(TAG, "loadResourceList", "type : " + ChargingCaseInfo.printResourceType(type) + ", usingResourcePath : " + usingResourcePath);
        mAdapter.updateSelectedIndexByFile(null);
        mAdapter.setList(list);
        updateRightBtnUI();
        if (!TextUtils.isEmpty(usingResourcePath)) {
            mAdapter.updateSelectedItemByPath(usingResourcePath);
        }
        if (mAdapter.isEditMode()) {
            UIHelper.show(tvTopRight);
        } else {
            if (mAdapter.getOptionalItemSize() > 0) {
                UIHelper.show(tvTopRight);
            } else {
                UIHelper.gone(tvTopRight);
            }
        }
    }

    private void exit() {
        if (mAdapter.isEditMode()) {
            mAdapter.updateState(ResourceFileAdapter.MODE_NORMAL);
            return;
        }
        finish(100, () -> {
            final Intent intent = new Intent();
            intent.putExtra(SConstant.KEY_RESOURCE_TYPE, resourceType);
            requireActivity().setResult(Activity.RESULT_OK, intent);
        });
    }

    private void updateRightBtnUI() {
        if (tvTopRight != null) {
            tvTopRight.setText("");
            int iconId = mAdapter.getOptionalItemSize() > 0 ? R.drawable.ic_edit_black : 0;
            tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, iconId, 0);
            tvTopRight.setClickable(iconId != 0);
        }
    }

    private void handleEditState(int state) {
        switch (state) {
            case ResourceFileAdapter.MODE_EDIT: { //编辑模式
                if (tvTopLeft != null) {
                    tvTopLeft.setText(getString(R.string.cancel));
                    tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                }
                if (tvTopRight != null) {
                    tvTopRight.setText(getString(R.string.select_all));
                    tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                }
                UIHelper.show(mBinding.tvDelete);
                break;
            }
            case ResourceFileAdapter.MODE_EDIT_ALL_SELECTED: { //全选模式
                if (tvTopLeft != null) {
                    tvTopLeft.setText(getString(R.string.cancel));
                    tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                }
                if (tvTopRight != null) {
                    tvTopRight.setText(getString(R.string.unselect_all));
                    tvTopRight.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                }
                break;
            }
            default: { //正常模式
                if (tvTopLeft != null) {
                    tvTopLeft.setText("");
                    tvTopLeft.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_back_black, 0, 0, 0);
                }
                updateRightBtnUI();
                UIHelper.gone(mBinding.tvDelete);
                loadResourceList(mViewModel.getChargingCaseInfo(), resourceType);
                break;
            }
        }
    }
}