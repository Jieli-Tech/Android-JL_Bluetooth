package com.jieli.btsmart.ui.multimedia.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.music.device.ContainerFragment;
import com.jieli.btsmart.ui.music.local.LocalMusicFragment;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.filebrowse.bean.SDCardBean;


/**
 * 设备音乐控制器
 */
public class MusicControlFragment extends Jl_BaseFragment implements SeekBar.OnSeekBarChangeListener {
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private TextView tvTitle;
    private TextView tvStartTime;
    private SeekBar sbMusic;
    private TextView tvEndTime;
    private ImageButton ibPlaymode;
    private ImageButton ibPlaylast;
    private ImageButton ibPlayOrPause;
    private ImageButton ibPlaynext;
    private ImageButton ibPlaylist;

    public MusicControlFragment() {

    }

    public static MusicControlFragment newInstance() {
        return new MusicControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(MusicControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(MusicControlFragment.class.getSimpleName());
        }
        return new MusicControlFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_music_control, container, false);
        tvTitle = view.findViewById(R.id.tv_title);
        tvStartTime = view.findViewById(R.id.tv_start_time);
        sbMusic = view.findViewById(R.id.sb_music);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        ibPlaymode = view.findViewById(R.id.ib_playmode);
        ibPlaylast = view.findViewById(R.id.ib_playlast);
        ibPlayOrPause = view.findViewById(R.id.ib_play_or_pause);
        ibPlaynext = view.findViewById(R.id.ib_playnext);
        ibPlaylist = view.findViewById(R.id.ib_playlist);

        ibPlaymode.setOnClickListener(mOnClickListener);
        ibPlaylast.setOnClickListener(mOnClickListener);
        ibPlayOrPause.setOnClickListener(mOnClickListener);
        ibPlaynext.setOnClickListener(mOnClickListener);
        ibPlaylist.setOnClickListener(mOnClickListener);
        sbMusic.setOnSeekBarChangeListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PlayControlImpl.getInstance().registerPlayControlListener(mControlCallback);
        PlayControlImpl.getInstance().refresh();
        JL_Log.e("sen", "MusicControlFragment     onActivityCreated ");

    }

    @Override
    public void onDestroyView() {
        JL_Log.e("sen", "MusicControlFragment     onDestroyView ");
       /* if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_BT) {
            PreferencesHelper.putIntValue(getActivity(), HomeActivity.KEY_MEDIA_PLAY_MODE, PlayControlImpl.MODE_BT);
        }*/
        PlayControlImpl.getInstance().unregisterPlayControlListener(mControlCallback);
        super.onDestroyView();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == ibPlaymode) {
                PlayControlImpl.getInstance().setNextPlaymode();
            } else if (v == ibPlaylast) {
                PlayControlImpl.getInstance().playPre();
            } else if (v == ibPlayOrPause) {
                PlayControlImpl.getInstance().playOrPause();
            } else if (v == ibPlaynext) {
                PlayControlImpl.getInstance().playNext();
            } else if (v == ibPlaylist) {
                toMusicListByFun();
            }
        }
    };

    @Override
    public void onResume() {
        PlayControlImpl.getInstance().onStart();
        super.onResume();
    }

    @Override
    public void onPause() {
        PlayControlImpl.getInstance().onPause();
        super.onPause();
    }


    private void toMusicListByFun() {
        if (!mRCSPController.isDeviceConnected()) return;
//        todo 跳转到音乐列表
        if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_BT) {
            ContentActivity.startActivity(requireContext(), LocalMusicFragment.class.getCanonicalName(), getString(R.string.multi_media_local));
        } else if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_MUSIC) {
            //如果时音乐模式，则跳转到对应的文件列表
            JL_Log.d(TAG, "toMusicListByFun : >>> music , " +  mRCSPController.getDeviceInfo().getCurrentDevIndex());
            Bundle bundle = new Bundle();
            bundle.putInt(ContainerFragment.KEY_TYPE, mRCSPController.getDeviceInfo().getCurrentDevIndex() == SDCardBean.INDEX_USB ? SDCardBean.USB : SDCardBean.SD);
            bundle.putInt(ContainerFragment.KEY_DEVICE_INDEX, mRCSPController.getDeviceInfo().getCurrentDevIndex());
            ContentActivity.startActivity(requireContext(), ContainerFragment.class.getCanonicalName(), bundle);
        }
    }

    private boolean isFragmentAlive() {
        return isAdded() && !isDetached();
    }

    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTitleChange(String title) {
            super.onTitleChange(title);
            JL_Log.d(TAG, "onTitleChange : " + title);
            if (isFragmentAlive()) {
                tvTitle.setText(title);
            }
        }

        @Override
        public void onModeChange(int mode) {
            super.onModeChange(mode);
            //当在模式变为音乐模式且处于可见状态时，onStart一下
            if (isResumed() && mode == PlayControlImpl.MODE_MUSIC) {
                PlayControlImpl.getInstance().onStart();
            }
        }

        @Override
        public void onTimeChange(int current, int total) {
            super.onTimeChange(current, total);
            JL_Log.d(TAG, "onTimeChange : " + current + " : " + total + ", " + sbMusic.isPressed() + ", isFragmentAlive = " + isFragmentAlive());
            if (!isFragmentAlive()) return;
            if (!sbMusic.isPressed()) {
                sbMusic.setMax(total);
                sbMusic.setProgress(current);
            }
            tvEndTime.setText(PlayControlImpl.formatTime(total));
            tvStartTime.setText(PlayControlImpl.formatTime(current));
        }

        @Override
        public void onPlayModeChange(JL_PlayMode mode) {
            super.onPlayModeChange(mode);
            if (!isFragmentAlive()) return;
            int resId = R.drawable.ic_playmode_circle_selector;
            switch (mode) {
                case ONE_LOOP:
                    resId = R.drawable.ic_playmode_single_selector;
                    break;
                case ALL_LOOP:
                    resId = R.drawable.ic_playmode_circle_selector;
                    break;
                case ALL_RANDOM:
                    resId = R.drawable.ic_playmode_random_selector;
                    break;
                case SEQUENCE:
                    resId = R.drawable.ic_playmode_sequence_nor;
                    break;
                case FOLDER_LOOP:
                    resId = R.drawable.ic_playmode_folder_loop_selector;
                    break;
                case DEVICE_LOOP:
                    resId = R.drawable.ic_playmode_device_loop_selector;
                    break;
            }
            ibPlaymode.setImageResource(resId);
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            if (!isFragmentAlive()) return;
            tvTitle.setSelected(isPlay);
            ibPlayOrPause.setSelected(isPlay);
        }

    };

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        PlayControlImpl.getInstance().seekTo(progress);

    }
}
