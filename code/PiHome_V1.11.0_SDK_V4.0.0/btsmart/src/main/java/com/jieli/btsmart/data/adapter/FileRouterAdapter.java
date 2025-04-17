package com.jieli.btsmart.data.adapter;


import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.filebrowse.bean.FileStruct;


/**
 * Created by chensenhua on 2018/5/29.
 */

public class FileRouterAdapter extends BaseQuickAdapter<FileStruct, BaseViewHolder> {


    public FileRouterAdapter() {
        super(R.layout.item_file_router);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, FileStruct item) {
        if (item != null) {
            TextView textView = holder.getView(R.id.tv_file_router);
            textView.setText(item.getName());
            textView.setSelected(getItemPosition(item) < getItemCount() - 1);
            if (getItemPosition(item) < getItemCount() - 1) {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_file_nav, 0);
            } else {
                textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            }
        }
    }


}
