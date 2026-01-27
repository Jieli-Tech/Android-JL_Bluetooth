package com.jieli.btsmart.ui.music.local;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;
import com.jieli.audio.media_player.Music;
import com.jieli.audio.media_player.MusicObserver;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.SongInfoAdapter;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.JL_MediaPlayerServiceManager;
import com.jieli.component.utils.ValueUtil;

import java.util.ArrayList;
import java.util.List;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

public class LocalMusicFragment extends BaseFragment implements OnItemClickListener, MusicObserver {
    private TextView tvMusicListAllNum;
    private SongInfoAdapter mAdapter;

    public static LocalMusicFragment newInstance() {
        return new LocalMusicFragment();
    }

    public LocalMusicFragment() {
        JL_MediaPlayerServiceManager.getInstance().refreshLoadLocalMusic();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_music, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        JL_MediaPlayerServiceManager.getInstance().registerMusicObserver(this);
        PlayControlImpl.getInstance().registerPlayControlListener(mPlayControlCallback);
        updatePlayingMusicId();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        JL_MediaPlayerServiceManager.getInstance().unregisterMusicObserver(this);
        PlayControlImpl.getInstance().unregisterPlayControlListener(mPlayControlCallback);
        super.onDestroyView();
    }

    @Override
    public void onItemClick(@NonNull BaseQuickAdapter adapter, @NonNull View view, int position) {
/*        if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_BT) {
            Log.e(TAG, "playOrPause: radio");
            JL_MediaPlayerServiceManager.getInstance().play(position);
        } else*/
        if (ALLOW_SWITCH_FUN_DISCONNECT && !RCSPController.getInstance().isDeviceConnected()) {
//            PlayControlImpl.getInstance().updateMode(PlayControlImpl.MODE_NET_RADIO);
            PlayControlImpl.getInstance().updateMode(PlayControlImpl.MODE_BT);
        }
        if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_NET_RADIO) {
            PlayControlImpl.getInstance().updateMode(PlayControlImpl.MODE_BT);
            JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().setData(JL_MediaPlayerServiceManager.getInstance().getLocalMusic());
            //  JL_MediaPlayerServiceManager.getInstance().play(position);
            Log.e(TAG, "playOrPause: bt");
        } else if (PlayControlImpl.getInstance().getMode() != PlayControlImpl.MODE_BT) {
            PlayControlImpl.getInstance().updateMode(PlayControlImpl.MODE_BT);
            //TODO 需要和固件商量是否需要处理静音或者用户主动切换到手机输出的情况
//            BluetoothHelper.getInstance().getSysPublicInfo(0x01 << AttrAndFunCode.SYS_INFO_ATTR_CUR_MODE_TYPE, null);
        }
        JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().setData(JL_MediaPlayerServiceManager.getInstance().getLocalMusic());
        JL_MediaPlayerServiceManager.getInstance().play(position);
        mAdapter.setPlayingMusicId(mAdapter.getData().get(position).getId());

    }

    @Override
    public void onChange(List<Music> list) {
        if (list == null) list = new ArrayList<>();
        String temp = getResources().getString(R.string.song_list_top_song_num);
        String timeTip = AppUtil.formatString(temp, list.size());
        tvMusicListAllNum.setText(timeTip);
        mAdapter.setNewInstance(list);
    }

    private final PlayControlCallback mPlayControlCallback = new PlayControlCallback() {
        @Override
        public void onModeChange(int mode) {
            updatePlayingMusicId();
        }

        @Override
        public void onTitleChange(String title) {
            super.onTitleChange(title);
            updatePlayingMusicId();
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            mAdapter.updatePlayStatus();
        }
    };


    private void updatePlayingMusicId() {
        if (mAdapter != null && JL_MediaPlayerServiceManager.getInstance().getCurrentPlayMusic() != null) {
            mAdapter.setPlayingMusicId(JL_MediaPlayerServiceManager.getInstance().getCurrentPlayMusic().getId());
        }
    }

    private void initView(View view) {

        tvMusicListAllNum = view.findViewById(R.id.tv_song_list_top_rise_all_num);
        RecyclerView rvLocalMusic = view.findViewById(R.id.local_music_rc);
        mAdapter = new SongInfoAdapter();
        //设置空布局，可用xml文件代替
        TextView textView = new TextView(getContext());
        textView.setText(getString(R.string.local_music_none));
        textView.setTextSize(16);
        textView.setTextColor(getResources().getColor(R.color.gray_text_5A5A5A));
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_local_img_empty, 0, 0);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(0, ValueUtil.dp2px(getContext(), 98), 0, 0);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(layoutParams);
        mAdapter.setEmptyView(textView);

        rvLocalMusic.setLayoutManager(new LinearLayoutManager(getContext()));
        rvLocalMusic.setAdapter((RecyclerView.Adapter) mAdapter);
        List<Music> dataList = JL_MediaPlayerServiceManager.getInstance().getLocalMusic();
        onChange(dataList);
        mAdapter.setOnItemClickListener(this);
    }


}
