package com.jieli.btsmart.ui.test.log;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.FileUtil;

import java.io.File;

/**
 * LogFileAdapter
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 日志文件适配器
 * @since 2025/7/25
 */
class LogFileAdapter extends BaseQuickAdapter<File, BaseViewHolder> {

    public LogFileAdapter() {
        super(R.layout.item_log_file);
        addChildClickViewIds(R.id.btn_share, R.id.btn_download, R.id.btn_remove);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, File file) {
        holder.setText(R.id.tv_file_name, file.getName());
        holder.setText(R.id.tv_file_size, FileUtil.formatFileSize(file.length()));
        holder.setImageResource(R.id.btn_download, FileUtil.isFileInDownload(getContext(), file.getName())
                ? R.drawable.ic_downloaded_green : R.drawable.ic_download_blue);
        holder.getView(R.id.cl_content).setOnClickListener(v -> {
            final OnItemClickListener listener = getOnItemClickListener();
            if(null != listener){
                listener.onItemClick(LogFileAdapter.this, v, getItemPosition(file));
            }
        });
    }

    public void updateItemByFilePath(String filePath) {
        final File file = getItemByFilePath(filePath);
        if (null == file) return;
        notifyItemChanged(getItemPosition(file));
    }

    private File getItemByFilePath(String filePath) {
        for (File file : getData()) {
            if (file.getPath().equals(filePath)) {
                return file;
            }
        }
        return null;
    }
}
