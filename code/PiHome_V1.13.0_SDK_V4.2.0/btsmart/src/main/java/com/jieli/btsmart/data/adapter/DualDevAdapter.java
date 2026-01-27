package com.jieli.btsmart.data.adapter;

import android.content.Context;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.device.double_connect.DeviceBtInfo;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.AppUtil;

/**
 * @ClassName: DualDevAdapter
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2023/8/16 11:13
 */
public class DualDevAdapter extends BaseQuickAdapter<DeviceBtInfo, BaseViewHolder> {
    public DualDevAdapter() {
        super(R.layout.item_connected_host_dev);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, DeviceBtInfo item) {
        if(null == item) return;
        //logo类型
        int logoResId = true ? R.drawable.ic_host_phone : R.drawable.ic_host_computer;
        //主机名
        String devName = item.getBtName();
        //是不是本机
        boolean isOwn = isOwn(getContext(), item.getBtName());
        baseViewHolder.setImageResource(R.id.iv_host_logo, logoResId);
        baseViewHolder.setText(R.id.tv_host_name, devName);
        baseViewHolder.setVisible(R.id.tv_is_own, isOwn);
    }

    public static boolean isOwn(@NonNull Context context, String name) {
        String btName = AppUtil.getBtName(context);
        return btName != null && name != null && btName.toLowerCase().startsWith(name.toLowerCase());
    }
}
