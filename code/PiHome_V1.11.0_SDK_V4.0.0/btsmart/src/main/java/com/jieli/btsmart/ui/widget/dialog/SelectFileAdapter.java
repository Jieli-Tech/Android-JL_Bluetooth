package com.jieli.btsmart.ui.widget.dialog;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.AppUtil;

import java.io.File;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 选择文件适配器
 * @since 2024/1/25
 */
public class SelectFileAdapter extends BaseQuickAdapter<File, BaseViewHolder> {
    private File selectedFile;

    public SelectFileAdapter() {
        super(R.layout.item_select_file);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder viewHolder, File file) {
        if (null == file) return;
        viewHolder.setText(R.id.tv_file_msg, AppUtil.formatString("%s(%s)", file.getName(), AppUtil.getFileSizeString(file.length())));
        viewHolder.setImageResource(R.id.iv_state, isSelectedItem(file) ? R.drawable.ic_check_purple : R.drawable.ic_check_gray);
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public boolean isSelectedItem(File file) {
        if (null == file) return false;
        return file.equals(selectedFile);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSelectedFile(File file) {
        boolean isChange = false;
        if (selectedFile == null) {
            selectedFile = file;
            isChange = true;
        } else if (!selectedFile.equals(file)) {
            selectedFile = file;
            isChange = true;
        }
        if (isChange) notifyDataSetChanged();
    }
}
