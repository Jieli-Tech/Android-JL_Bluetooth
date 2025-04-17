package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;

import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;

public class SongInfoAdapter extends BaseQuickAdapter<Music, BaseViewHolder> {
    private long mPlayingMusicId = -1L;

    public SongInfoAdapter() {
        super(R.layout.item_song_info);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, Music item) {
        boolean selected = this.mPlayingMusicId == item.getId();
        AnimationDrawable anim = (AnimationDrawable) ((ImageView) helper.getView(R.id.iv_song_info_play)).getDrawable();
        helper.setVisible(R.id.iv_song_info_play, selected);

        if (selected && PlayControlImpl.getInstance().isPlay() && PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_BT) {
            helper.getView(R.id.iv_song_info_play).getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (!anim.isRunning()) {
                        anim.start();
                    }
                    //remove掉绘制监听
                    helper.getView(R.id.iv_song_info_play).getViewTreeObserver().removeOnPreDrawListener(this);
                    return true;
                }
            });
        } else {
            anim.stop();
        }
        int position = getItemPosition(item);
        TextView tvSongName = helper.getView(R.id.tv_song_info_name);
        tvSongName.setSelected(selected);

        helper.setText(R.id.tv_song_info_index, (position + 1) + "");
        helper.setVisible(R.id.tv_song_info_index, !selected);

        helper.setText(R.id.tv_song_info_name, item.getTitle());
        helper.getView(R.id.tv_song_info_name).setSelected(selected);

        helper.setText(R.id.tv_song_info_author, item.getArtist());

    }

    @SuppressLint("NotifyDataSetChanged")
    public void setPlayingMusicId(long mPlayingMusicId) {
        this.mPlayingMusicId = mPlayingMusicId;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatePlayStatus() {
        JL_Log.e("SongInfoAdapter", "updatePlayStatus");
        notifyDataSetChanged();
    }
}
