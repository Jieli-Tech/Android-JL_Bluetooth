package com.jieli.btsmart.ui.multimedia.control.music;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentMusicControlBinding;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.music.device.ContainerFragment;
import com.jieli.btsmart.ui.music.local.LocalMusicFragment;
import com.jieli.filebrowse.bean.SDCardBean;


/**
 * 设备音乐控制器
 */
public class MusicControlFragment extends BaseFragment {

    private FragmentMusicControlBinding mBinding;
    private final RCSPController mRCSPController = RCSPController.getInstance();

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentMusicControlBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
        PlayControlImpl.getInstance().registerPlayControlListener(mControlCallback);
        PlayControlImpl.getInstance().refresh();
    }

    @Override
    public void onDestroyView() {
        PlayControlImpl.getInstance().unregisterPlayControlListener(mControlCallback);
        super.onDestroyView();
    }

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

    private void initUI() {
        mBinding.ibPlaymode.setOnClickListener(v -> PlayControlImpl.getInstance().setNextPlaymode());
        mBinding.ibPlaylast.setOnClickListener(v -> PlayControlImpl.getInstance().playPre());
        mBinding.ibPlayOrPause.setOnClickListener(v -> PlayControlImpl.getInstance().playOrPause());
        mBinding.ibPlaynext.setOnClickListener(v -> PlayControlImpl.getInstance().playNext());
        mBinding.ibPlaylist.setOnClickListener(v -> toMusicListByFun());
        mBinding.sbMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
        });
    }

    private void toMusicListByFun() {
        if (!mRCSPController.isDeviceConnected()) return;
//        todo 跳转到音乐列表
        if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_BT) {
            ContentActivity.startActivity(requireContext(), LocalMusicFragment.class.getCanonicalName(), getString(R.string.multi_media_local));
        } else if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_MUSIC) {
            //如果时音乐模式，则跳转到对应的文件列表
            JL_Log.d(TAG, "toMusicListByFun : >>> music , " + mRCSPController.getDeviceInfo().getCurrentDevIndex());
            Bundle bundle = new Bundle();
            bundle.putInt(ContainerFragment.KEY_TYPE, mRCSPController.getDeviceInfo().getCurrentDevIndex() == SDCardBean.INDEX_USB ? SDCardBean.USB : SDCardBean.SD);
            bundle.putInt(ContainerFragment.KEY_DEVICE_INDEX, mRCSPController.getDeviceInfo().getCurrentDevIndex());
            ContentActivity.startActivity(requireContext(), ContainerFragment.class.getCanonicalName(), bundle);
        }
    }

    private int getModeResId(JL_PlayMode playMode) {
        switch (playMode) {
            case ONE_LOOP:
                return R.drawable.ic_playmode_single_selector;
            case ALL_RANDOM:
                return R.drawable.ic_playmode_random_selector;
            case SEQUENCE:
                return R.drawable.ic_playmode_sequence_nor;
            case FOLDER_LOOP:
                return R.drawable.ic_playmode_folder_loop_selector;
            case DEVICE_LOOP:
                return R.drawable.ic_playmode_device_loop_selector;
            default:
                return R.drawable.ic_playmode_circle_selector;
        }
    }

    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTitleChange(String title) {
            super.onTitleChange(title);
            JL_Log.d(TAG, "onTitleChange : " + title);
            if (isInvalid()) return;
            mBinding.tvTitle.setText(title);
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
            boolean isPressed = mBinding.sbMusic.isPressed();
            JL_Log.d(TAG, "onTimeChange : " + current + " : " + total + ", " + isPressed + ", isInvalid = " + isInvalid());
            if (isInvalid()) return;
            if (!isPressed) {
                mBinding.sbMusic.setMax(total);
                mBinding.sbMusic.setProgress(current);
            }
            mBinding.tvEndTime.setText(PlayControlImpl.formatTime(total));
            mBinding.tvStartTime.setText(PlayControlImpl.formatTime(current));
        }

        @Override
        public void onPlayModeChange(JL_PlayMode mode) {
            super.onPlayModeChange(mode);
            if (isInvalid()) return;
            mBinding.ibPlaymode.setImageResource(getModeResId(mode));
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            if (isInvalid()) return;
            mBinding.tvTitle.setSelected(isPlay);
            mBinding.ibPlayOrPause.setSelected(isPlay);
        }

    };
}
