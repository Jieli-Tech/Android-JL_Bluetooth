package com.jieli.btsmart.ui.chargingCase;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.bean.settings.v0.SDKMessage;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.util.UIHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 资源文件适配器
 * @since 2023/12/7
 */
public class ResourceFileAdapter extends BaseMultiItemQuickAdapter<BaseMultiItem<ResourceFile>, BaseViewHolder> {
    /**
     * 添加资源布局
     */
    public static final int TYPE_ADD_ITEM = 1;
    /**
     * 资源布局
     */
    public static final int TYPE_FILE_ITEM = 2;

    /**
     * 正常模式
     */
    public static final int MODE_NORMAL = 1;
    /**
     * 编辑模式
     */
    public static final int MODE_EDIT = 2;
    /**
     * 全选模式
     */
    public static final int MODE_EDIT_ALL_SELECTED = 3;

    /**
     * 是否显示编辑图标
     */
    private final boolean isShowEditIcon;
    /**
     * 状态监听器
     */
    private final OnStateListener listener;
    /**
     * 当前状态
     */
    private int state = MODE_NORMAL;

    /**
     * 正在使用资源信息
     */
    private ResourceFile usingFile;
    /**
     * 已选择的文件列表
     */
    private final List<ResourceFile> selectedFiles = new ArrayList<>();
    /**
     * 芯片类型
     */
    public int chip = SDKMessage.CHIP_701N;

    public ResourceFileAdapter() {
        this(true, null);
    }

    public ResourceFileAdapter(boolean isShowEditIcon, OnStateListener listener) {
        this.isShowEditIcon = isShowEditIcon;
        this.listener = listener;
        addItemType(TYPE_ADD_ITEM, R.layout.item_add_image);
        addItemType(TYPE_FILE_ITEM, R.layout.item_select_image);

        addChildClickViewIds(R.id.btn_edit);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (listener != null) listener.onStateChange(state);
        }, 30L);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, BaseMultiItem<ResourceFile> item) {
        if (null == item) return;
        final boolean isEditMode = isEditMode();
        switch (item.getItemType()) {
            case TYPE_ADD_ITEM: {
                View maskView = viewHolder.getView(R.id.bg_mask);
                if (isEditMode) {
                    UIHelper.show(maskView);
                } else {
                    UIHelper.gone(maskView);
                }
                break;
            }
            case TYPE_FILE_ITEM: {
                final ImageView image = viewHolder.getView(R.id.iv_image);
                final ResourceFile resource = item.getData();
                if (null == resource) return;
                final String filePath = resource.getPath();
                if (resource.isGif()) {
                    Glide.with(getContext()).asGif().load(filePath)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .placeholder(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_EAEAEA)))
                            .into(image);
                } else {
                    Glide.with(getContext()).asBitmap().load(filePath)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .placeholder(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_EAEAEA)))
                            .into(image);
                }
                final boolean isItemSelected = isSelectedItem(item.getData());
                View groupSelectedView = viewHolder.getView(R.id.group_selected_view);
                ImageView ivResourceState = viewHolder.getView(R.id.iv_resource_state);
                ImageView ivSelectedState = viewHolder.getView(R.id.iv_select_state);
                View maskView = viewHolder.getView(R.id.bg_mask);
                if (resource.getDevState() == ResourceFile.STATE_NOT_EXIST) {
                    UIHelper.show(ivResourceState);
                } else {
                    UIHelper.gone(ivResourceState);
                }
                if (isEditMode) {
                    UIHelper.gone(groupSelectedView);
                    UIHelper.show(maskView);
                    boolean isShowSelectUI = isOptionalItem(resource);
                    if (isShowSelectUI) {
                        UIHelper.show(ivSelectedState);
                        ivSelectedState.setImageResource(isItemSelected ? R.drawable.ic_select_checked : R.drawable.ic_select_normal);
                    } else {
                        UIHelper.gone(ivSelectedState);
                    }
                } else {
                    UIHelper.gone(maskView);
                    UIHelper.gone(ivSelectedState);
                    UIHelper.setVisibility(groupSelectedView, isItemSelected ? View.VISIBLE : View.GONE);
                    boolean isShowEdit = isShowEditIcon && isCustomResource(resource);
                    UIHelper.setVisibility(viewHolder.getView(R.id.btn_edit), isShowEdit ? View.VISIBLE : View.GONE);
                    bindViewClickListener(viewHolder, item.getItemType());
                }
                break;
            }
        }
    }

    public List<ResourceFile> getSelectedItem() {
        if (selectedFiles.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(selectedFiles);
    }

    public int getOptionalItemSize() {
        int count = 0;
        for (BaseMultiItem<ResourceFile> item : getData()) {
            if (item.getItemType() != TYPE_FILE_ITEM) continue;
            final ResourceFile resource = item.getData();
            if (null == resource) continue;
            if (isOptionalItem(resource)) {
                count++;
            }
        }
        return count;
    }

    public boolean isSelectedItem(ResourceFile file) {
        if (null == file) return false;
        return selectedFiles.contains(file);
    }

    public boolean isEditMode() {
        return state != MODE_NORMAL;
    }

    public boolean isAllSelected() {
        return this.state == MODE_EDIT_ALL_SELECTED;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateState(int state) {
//        if (isShowEditIcon) return;
        if (this.state != state) {
            int previous = this.state;
            this.state = state;
            switch (state) {
                case MODE_NORMAL: {
                    selectedFiles.clear();
                    final ResourceFile oldFile = usingFile;
                    if (oldFile != null) {
                        ResourceFile cache = findItemByFilePath(oldFile.getPath());
                        if (null != cache) {
                            cache.setDevState(ResourceFile.STATE_USING);
                        }
                        selectedFiles.add(oldFile);
                    }
                    break;
                }
                case MODE_EDIT: {
                    if (previous == MODE_NORMAL) {
                        selectedFiles.clear();
                    }
                    break;
                }
                case MODE_EDIT_ALL_SELECTED: {
                    selectedFiles.clear();
                    for (BaseMultiItem<ResourceFile> item : getData()) {
                        if (item.getItemType() != TYPE_FILE_ITEM) continue;
                        ResourceFile file = item.getData();
                        if (null == file) continue;
                        if (isOptionalItem(file)) {
                            selectedFiles.add(file);
                        }
                    }
                    break;
                }
            }
            if (this.listener != null) this.listener.onStateChange(state);
            notifyDataSetChanged();
        }
    }

    public void updateSelectedIndex(int position) {
        BaseMultiItem<ResourceFile> item = getItem(position);
        if (null == item || item.getItemType() != TYPE_FILE_ITEM) return;
        updateSelectedIndexByFile(item.getData());
    }

    public void updateSelectedItemByPath(String filePath) {
        if (TextUtils.isEmpty(filePath)) return;
        ResourceFile file = findItemByFilePath(filePath);
//        if (null == file) return;
        updateSelectedIndexByFile(file);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedIndexByFile(ResourceFile file) {
        final boolean isEditMode = isEditMode();
        if (isEditMode) {
            if (null == file) return;
            if (file.equals(usingFile)) return;
            if (!selectedFiles.contains(file)) {
                selectedFiles.add(file);
            } else {
                selectedFiles.remove(file);
            }
            if (selectedFiles.size() == getOptionalItemSize()) {
                updateState(MODE_EDIT_ALL_SELECTED);
            } else {
                updateState(MODE_EDIT);
            }
            notifyDataSetChanged();
            return;
        }
        final ResourceFile oldFile = usingFile;
        if (oldFile == null || !oldFile.equals(file)) {
            if (oldFile != null) { //重置设备状态
                ResourceFile cache = findItemByFilePath(oldFile.getPath());
                if (null != cache && cache.isDeviceUsing()) {
                    boolean isAllowMultiResource = cache.getType() == ChargingCaseInfo.TYPE_WALLPAPER;
                    if(!isAllowMultiResource && cache.getType() == ChargingCaseInfo.TYPE_SCREEN_SAVER
                            && chip == SDKMessage.CHIP_707N){
                        isAllowMultiResource = true;
                    }
                    cache.setDevState(isAllowMultiResource ? ResourceFile.STATE_ALREADY_EXIST : ResourceFile.STATE_NOT_EXIST);
                }
            }
            selectedFiles.clear();
            if (file != null) {
                file.setDevState(ResourceFile.STATE_USING);
                selectedFiles.add(file);
            }
            usingFile = file;
            notifyDataSetChanged();
        }
    }

    public void updateSelectedIndexByResource(ResourceInfo file) {
        if (null == file) return;
        updateSelectedItemByPath(file.getFilePath());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearAllSelected() {
        if (!isEditMode()) return;
        selectedFiles.clear();
        if (state != MODE_EDIT) {
            updateState(MODE_EDIT);
        } else {
            notifyDataSetChanged();
        }
    }

    private boolean isOptionalItem(ResourceFile file) {
        if (null == file) return false;
        return !file.equals(usingFile) && ((file.getDevState() != ResourceFile.STATE_NOT_EXIST )
                || (!isShowEditIcon && isCustomResource(file)));
    }

    private boolean isCustomResource(ResourceFile file) {
        if (null == file) return false;
        return file.isCustomResource();
    }

    private ResourceFile findItemByFilePath(String filePath) {
        String fileName = ChargingBinUtil.getFileNameByPath(filePath, true);
        if (TextUtils.isEmpty(fileName)) return null;
        for (BaseMultiItem<ResourceFile> item : getData()) {
            if (item.getItemType() != TYPE_FILE_ITEM || item.getData() == null) continue;
            ResourceFile resourceFile = item.getData();
            if (null == resourceFile) continue;
            String cacheFileName = ChargingBinUtil.getNameNoSuffix(resourceFile.getName());
            if (cacheFileName.toUpperCase().startsWith(fileName.toUpperCase())) {
                return resourceFile;
            }
        }
        return null;
    }

    public interface OnStateListener {

        void onStateChange(int state);
    }
}
