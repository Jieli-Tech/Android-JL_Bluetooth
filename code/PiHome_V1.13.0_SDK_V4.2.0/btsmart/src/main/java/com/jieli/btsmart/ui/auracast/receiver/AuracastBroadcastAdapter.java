package com.jieli.btsmart.ui.auracast.receiver;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.auracast.AuracastBroadcast;
import com.jieli.btsmart.R;

/**
 * AuracastBroadcastAdapter
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: Auracast广播适配器
 */
public class AuracastBroadcastAdapter extends BaseQuickAdapter<AuracastBroadcast, BaseViewHolder> {

    public AuracastBroadcastAdapter() {
        super(R.layout.item_auracast_broadcast);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, AuracastBroadcast broadcast) {
        holder.setText(R.id.tv_broadcast_msg, broadcast.getBroadcastName());
        holder.setGone(R.id.iv_lock_flag, !broadcast.isEncrypted());
    }
}
