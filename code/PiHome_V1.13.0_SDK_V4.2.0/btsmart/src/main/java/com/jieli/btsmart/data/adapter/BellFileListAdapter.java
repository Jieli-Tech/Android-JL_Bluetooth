package com.jieli.btsmart.data.adapter;

import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.filebrowse.bean.FileStruct;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/9/3 9:43 AM
 * @desc :  铃声文件适配器
 */
public class BellFileListAdapter extends FileListAdapter {
    public BellFileListAdapter() {
        super(R.layout.item_alarm_bell);
    }

    @Override
    protected void convert(BaseViewHolder holder, FileStruct item) {
        boolean isSelected = isSelected(item);
        holder.setText(R.id.tv_bell_name, item.getName());
        holder.getView(R.id.iv_bell_state).setSelected(isSelected);
        holder.setImageResource(R.id.iv_bell_type, item.isFile() ? R.drawable.ic_device_file_file : R.drawable.ic_device_file_floder);
        holder.setVisible(R.id.view_bell_line, getItemPosition(item) < getData().size() - 1);
    }
}
