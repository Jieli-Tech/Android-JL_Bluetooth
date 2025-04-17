package com.jieli.btsmart.ui.chargingCase;

import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.chargingcase.ResourceFile;
import com.jieli.btsmart.data.model.settings.BaseMultiItem;
import com.jieli.btsmart.util.AppUtil;
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
    public static final int TYPE_ADD_ITEM = 1;
    public static final int TYPE_FILE_ITEM = 2;

    public static final int MODE_NORMAL = 1;
    public static final int MODE_EDIT = 2;
    public static final int MODE_EDIT_ALL_SELECTED = 3;

    private final boolean isAllowEdit;
    private final OnStateListener listener;
    private int state = MODE_NORMAL;

    private ResourceFile usingFile;
    private final List<ResourceFile> selectedFiles = new ArrayList<>();

    public ResourceFileAdapter() {
        this(true, null);
    }

    public ResourceFileAdapter(boolean isAllowEdit, OnStateListener listener) {
        this.isAllowEdit = isAllowEdit;
        this.listener = listener;
        addItemType(TYPE_ADD_ITEM, R.layout.item_add_image);
        addItemType(TYPE_FILE_ITEM, R.layout.item_select_image);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, BaseMultiItem<ResourceFile> item) {
        if (null == item) return;
        switch (item.getItemType()) {
            case TYPE_ADD_ITEM: {
                break;
            }
            case TYPE_FILE_ITEM: {
                final ImageView image = viewHolder.getView(R.id.iv_image);
                final ResourceFile resource = item.getData();
                if (null == resource) return;
                final String filePath = resource.getPath();
                if (resource.isGif()) {
                    Glide.with(getContext()).asGif().load(filePath)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .placeholder(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_EAEAEA)))
                            .into(image);
                } else {
                    Glide.with(getContext()).asBitmap().load(filePath)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .placeholder(new ColorDrawable(ContextCompat.getColor(getContext(), R.color.gray_EAEAEA)))
                            .into(image);
                }
                final boolean isItemSelected = isSelectedItem(item.getData());
                final boolean isEditMode = isEditMode();
                View groupSelectedView = viewHolder.getView(R.id.group_selected_view);
                ImageView ivSelectedState = viewHolder.getView(R.id.iv_select_state);
                if (isEditMode) {
                    UIHelper.gone(groupSelectedView);
                    if (resource.equals(usingFile)) {
                        UIHelper.gone(ivSelectedState);
                    } else {
                        UIHelper.show(ivSelectedState);
                        ivSelectedState.setImageResource(isItemSelected ? R.drawable.ic_select_checked : R.drawable.ic_select_normal);
                    }
                } else {
                    UIHelper.gone(ivSelectedState);
                    UIHelper.setVisibility(groupSelectedView, isItemSelected ? View.VISIBLE : View.GONE);
                    boolean isShowEdit = isAllowEdit && isCustomScreen(item.getData());
                    viewHolder.getView(R.id.btn_edit).setVisibility(isShowEdit ? View.VISIBLE : View.GONE);
                    addChildClickViewIds(R.id.btn_edit);
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
        if (isAllowEdit) return;
        if (this.state != state) {
            int previous = this.state;
            this.state = state;
            switch (state) {
                case MODE_NORMAL: {
                    selectedFiles.clear();
                    if(usingFile != null){
                        selectedFiles.add(usingFile);
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
                        if (item.getItemType() != TYPE_FILE_ITEM || null == item.getData()) return;
                        if (!item.getData().equals(usingFile)) {
                            selectedFiles.add(item.getData());
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
        String fileName = AppUtil.getFileName(filePath, true);
        if (TextUtils.isEmpty(fileName)) return;
        ResourceFile file = null;
        for (BaseMultiItem<ResourceFile> item : getData()) {
            if (item.getItemType() != TYPE_FILE_ITEM || item.getData() == null) continue;
            ResourceFile resourceFile = item.getData();
            if (null == resourceFile) continue;
            String cacheFileName = AppUtil.getNameNoSuffix(resourceFile.getName());
            if (cacheFileName.equalsIgnoreCase(fileName)) {
                file = resourceFile;
                break;
            }
        }
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
            if (selectedFiles.size() == getSelectableItemSize()) {
                updateState(MODE_EDIT_ALL_SELECTED);
            } else {
                updateState(MODE_EDIT);
            }
            notifyDataSetChanged();
            return;
        }
        if (usingFile == null || !usingFile.equals(file)) {
            usingFile = file;
            selectedFiles.clear();
            if (file != null) selectedFiles.add(file);
            notifyDataSetChanged();
        }
    }

    public void updateSelectedIndexByFile(ResourceInfo file) {
        if (null == file) return;
        updateSelectedItemByPath(file.getPath());
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

    private int getSelectableItemSize() {
        final List<BaseMultiItem<ResourceFile>> items = getData();
        int size = items.size();
        for (BaseMultiItem<ResourceFile> item : items) {
            if (item.getItemType() != TYPE_FILE_ITEM || item.getData() == null
                    || item.getData().equals(usingFile)) {
                size--;
            }
        }
        return size;
    }

    private boolean isCustomScreen(ResourceFile file) {
        if (null == file) return false;
        return file.getType() == ResourceFile.TYPE_SCREEN_SAVER && file.getName().startsWith(ResourceInfo.CUSTOM_SCREEN_NAME);
    }

    public interface OnStateListener {

        void onStateChange(int state);
    }
}
