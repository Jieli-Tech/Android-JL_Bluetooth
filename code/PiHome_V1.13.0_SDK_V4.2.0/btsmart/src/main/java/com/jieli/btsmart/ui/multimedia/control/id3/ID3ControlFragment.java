package com.jieli.btsmart.ui.multimedia.control.id3;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.util.JLShakeItManager;
import com.jieli.component.base.BasePresenter;

import static com.jieli.btsmart.util.JLShakeItManager.MODE_CUT_SONG_TYPE_ID3;


/**
 * ID3信息显示界面
 */
public class ID3ControlFragment extends BaseFragment implements ID3ControlContract.ID3ControlView {

    private TextView tvTitleTitle;
    private TextView tvTitleAlbum;
    private TextView tvTitleArtist;
    private TextView tvStartTime;
    private SeekBar sbMusic;
    private TextView tvEndTime;
    private ImageButton ibPlaylast;
    private ImageButton ibPlayOrPause;
    private ImageButton ibPlaynext;

    private ID3MusicInfo mLastMusicInfo;
    private ID3ControlContract.ID3ControlPresenter mPresenter;
    private int needFilterNum = 0;
    private boolean skipFirstTime = false;

    private final JLShakeItManager mShakeItManager = JLShakeItManager.getInstance();

    public static ID3ControlFragment newInstance() {
        return new ID3ControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(ID3ControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(ID3ControlFragment.class.getSimpleName());
        }
        return new ID3ControlFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_id3_control, container, false);
        tvTitleTitle = view.findViewById(R.id.tv_title_title);
        tvTitleAlbum = view.findViewById(R.id.tv_title_album);
        tvTitleArtist = view.findViewById(R.id.tv_title_artist);
        tvStartTime = view.findViewById(R.id.tv_start_time);
        sbMusic = view.findViewById(R.id.sb_music);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        ibPlaylast = view.findViewById(R.id.ib_playlast);
        ibPlayOrPause = view.findViewById(R.id.ib_play_or_pause);
        ibPlaynext = view.findViewById(R.id.ib_playnext);

        ibPlaylast.setOnClickListener(mOnClickListener);
        ibPlayOrPause.setOnClickListener(mOnClickListener);
        ibPlaynext.setOnClickListener(mOnClickListener);

        ibPlayOrPause.setSelected(true);
        sbMusic.setEnabled(false);
        mPresenter = new ID3ControlPresenterImpl(this);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        skipFirstTime = false;
        mShakeItManager.getOnShakeItStartLiveData().observeForever( mode -> {
            if (!skipFirstTime) {
                skipFirstTime = true;
                return;
            }
            if (mode == JLShakeItManager.SHAKE_IT_MODE_CUT_SONG && isVisible()) {
                if (mShakeItManager.getCutSongType() == MODE_CUT_SONG_TYPE_ID3) {
                    if (mPresenter != null) {
                        mPresenter.playID3Next();
                    }
                }
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Bundle bundle = getArguments();
        if(null == bundle){
            finish();
            return;
        }
        ID3MusicInfo musicInfo = bundle.getParcelable(SConstant.KEY_ID3_INFO);
        updateShowPlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_OTHER);
        if (musicInfo != null) {
            updateID3MusicInfo(musicInfo);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    @Override
    public void onDestroyView() {
        JL_Log.i(TAG, "MusicControlFragment     onDestroyView ");
        super.onDestroyView();
        if (mPresenter != null) {
            mPresenter.destroy();
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == ibPlaylast) {
                if (mPresenter != null) {
                    mPresenter.playID3Prev();
                }
            } else if (v == ibPlayOrPause) {
                if (mPresenter != null) {
                    mPresenter.playOrPauseID3(ibPlayOrPause.isSelected());
                }
            } else if (v == ibPlaynext) {
                if (mPresenter != null) {
                    mPresenter.playID3Next();
                }
            }
        }
    };


    public void updateShowPlayerFlag(int flag) {
        /*JL_Log.e("ZHM-id3", "updateShowPlayerFlag: mPresenter: " + mPresenter + "flag :" + flag);*/
        if (mPresenter != null && mPresenter.showPlayerFlag() != flag) {
            mPresenter.updatePlayerFlag(flag);
        }
    }

    private void updateID3MusicInfo(ID3MusicInfo musicInfo) {
        if (musicInfo == null || isInvalid()) return;
        if (mLastMusicInfo != null) {//过滤不完整的musicInfo，否则会闪
            if (musicInfo.getTitle() == null) return;
            if (!mLastMusicInfo.getTitle().equals(musicInfo.getTitle()) && mLastMusicInfo.getTotalTime() == musicInfo.getTotalTime()
                    && mLastMusicInfo.getCurrentTime() == musicInfo.getCurrentTime()) {
                mLastMusicInfo = new ID3MusicInfo();
                mLastMusicInfo.setTitle(musicInfo.getTitle());
                mLastMusicInfo.setArtist(musicInfo.getArtist());
                mLastMusicInfo.setAlbum(musicInfo.getAlbum());
                mLastMusicInfo.setTotalTime(musicInfo.getTotalTime());
                mLastMusicInfo.setCurrentTime(musicInfo.getCurrentTime());
                needFilterNum = 1;
                return;
            }
        }
        if (needFilterNum != 0) {
            needFilterNum--;
            return;
        }
        mLastMusicInfo = new ID3MusicInfo();
        mLastMusicInfo.setTitle(musicInfo.getTitle());
        mLastMusicInfo.setArtist(musicInfo.getArtist());
        mLastMusicInfo.setAlbum(musicInfo.getAlbum());
        mLastMusicInfo.setTotalTime(musicInfo.getTotalTime());
        mLastMusicInfo.setCurrentTime(musicInfo.getCurrentTime());
        JL_Log.i(TAG, "ID3ControlFragment onID3MusicInfo" + musicInfo);
//        if (!TextUtils.isEmpty(musicInfo.getArtist())) {
//            tvTitle.setText(AppUtil.formatString("%s-%s", musicInfo.getTitle(), musicInfo.getArtist()));
//        } else {
//            tvTitle.setText(musicInfo.getTitle());
//        }
        updateShowPlayerFlag(ID3ControlPresenterImpl.PLAYER_FLAG_OTHER);
        if (musicInfo.getArtist() != null) {
            tvTitleArtist.setText(musicInfo.getArtist());
        }
        if (musicInfo.getAlbum() != null && !musicInfo.getAlbum().equals(musicInfo.getTitle())) {
            tvTitleAlbum.setText(musicInfo.getAlbum());
        } else {
            tvTitleAlbum.setText(null);
        }
        tvTitleTitle.setText(musicInfo.getTitle());
        int totalTime = musicInfo.getTotalTime();
        String totalTimeString = PlayControlImpl.formatTime(totalTime * 1000);
        tvEndTime.setText(totalTimeString);
        sbMusic.setMax(totalTime);
        int currentTime = musicInfo.getCurrentTime();
        String currentTimeString = PlayControlImpl.formatTime(currentTime * 1000);
        tvStartTime.setText(currentTimeString);
        sbMusic.setProgress(currentTime);
        tvTitleTitle.setSelected(musicInfo.isPlayStatus());
        ibPlayOrPause.setSelected(musicInfo.isPlayStatus());
    }

    @Override
    public void onID3CmdSuccess(ADVInfoResponse advInfo) {

    }

    @Override
    public void onID3CmdFailed(BaseError error) {

    }

    @Override
    public void onID3MusicInfo(ID3MusicInfo id3MusicInfo) {
        if (mPresenter == null || mPresenter.showPlayerFlag() != ID3ControlPresenterImpl.PLAYER_FLAG_OTHER) {
            return;
        }
        updateID3MusicInfo(id3MusicInfo);
    }

    @Override
    public void onBtAdapterStatus(boolean enable) {

    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {

    }

    @Override
    public void onSwitchDevice(BluetoothDevice device) {

    }

    @Override
    public void setPresenter(BasePresenter presenter) {
        if (mPresenter == null) {
            mPresenter = (ID3ControlContract.ID3ControlPresenter) presenter;
        }
    }
}
