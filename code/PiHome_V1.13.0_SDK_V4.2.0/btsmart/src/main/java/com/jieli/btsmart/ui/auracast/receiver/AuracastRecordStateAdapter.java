package com.jieli.btsmart.ui.auracast.receiver;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.auracast.AuracastBroadcast;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.auracast.AuracastRecordState;
import com.jieli.btsmart.util.UIHelper;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.List;

/**
 * AuracastRecordStateAdapter
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/11/19
 * note: 广播历史记录适配器
 */
public class AuracastRecordStateAdapter extends BaseQuickAdapter<AuracastRecordState, BaseViewHolder> {

    public AuracastRecordStateAdapter() {
        super(R.layout.item_listening_broadcast);
        addChildClickViewIds(R.id.btn_leave, R.id.tv_select_language);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, AuracastRecordState item) {
        final AuracastBroadcast broadcast = item.getBroadcast();
        if (null == broadcast) return;
        holder.setText(R.id.tv_broadcast_msg, broadcast.getBroadcastName());
        ConstraintLayout contentLayout = holder.getView(R.id.cl_content);
        contentLayout.setOnClickListener(v -> {
            final OnItemClickListener listener = getOnItemClickListener();
            if (null != listener) {
                listener.onItemClick(AuracastRecordStateAdapter.this, v, getItemPosition(item));
            }
        });
        ImageView ivState = holder.getView(R.id.iv_broadcast_state);
        ImageView ivArrow = holder.getView(R.id.iv_arrow);
        AVLoadingIndicatorView avSyncing = holder.getView(R.id.aiv_syncing);
        int syncState = item.getSyncState();
        switch (syncState) {
            case StateCode.STATE_SYNCING: { //同步中
                Drawable drawable = ivState.getDrawable();
                if (drawable instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                    if (animationDrawable.isRunning()) {
                        animationDrawable.stop();
                    }
                }
                ivState.setImageResource(item.isFoundBroadcast() ? R.drawable.ic_broadcast_near : R.drawable.ic_broadcast_off);
                UIHelper.hide(ivArrow);
                holder.setText(R.id.btn_leave, getContext().getString(R.string.remove_record));
                UIHelper.show(avSyncing);
                avSyncing.show();
                break;
            }
            case StateCode.STATE_SYNC_OK: { //同步成功
                ivState.setImageResource(R.drawable.anim_audio_broadcast);
                Drawable drawable = ivState.getDrawable();
                if (drawable instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                    if (!animationDrawable.isRunning()) {
                        animationDrawable.start();
                    }
                }
                holder.setText(R.id.btn_leave, getContext().getString(R.string.stop_listening));
                UIHelper.hide(ivArrow);
                avSyncing.hide();
                UIHelper.hide(avSyncing);
                break;
            }
            default: { //未同步 或 同步失败
                Drawable drawable = ivState.getDrawable();
                if (drawable instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) drawable;
                    if (animationDrawable.isRunning()) {
                        animationDrawable.stop();
                    }
                }
                ivState.setImageResource(item.isFoundBroadcast() ? R.drawable.ic_broadcast_near : R.drawable.ic_broadcast_off);
                UIHelper.hide(ivArrow);
                holder.setText(R.id.btn_leave, getContext().getString(R.string.remove_record));
                avSyncing.hide();
                UIHelper.hide(avSyncing);
                break;
            }
        }
    }

    public void updateBroadcastState(AuracastBroadcast state) {
        List<AuracastRecordState> stateList = getData();
        for (int i = 0; i < stateList.size(); i++) {
            AuracastRecordState recordState = stateList.get(i);
            AuracastBroadcast broadcast = recordState.getBroadcast();
            if (null == broadcast) continue;
            if (broadcast.equals(state)) {
                recordState.setState(state);
                notifyItemChanged(i);
                return;
            }
        }
    }
}
