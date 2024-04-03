package com.jieli.btsmart.ui.multimedia.control;

import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentNetRadioControlBinding;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.tool.room.DataRepository;
import com.jieli.btsmart.tool.room.NetRadioUpdateSelectData;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseViewModelFragment;
import com.jieli.btsmart.ui.music.net_radio.NetRadioFragment;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class NetRadioControlFragment extends BaseViewModelFragment<FragmentNetRadioControlBinding> {

    public NetRadioControlFragment() {
    }

    public static Fragment newInstance() {
        return new NetRadioControlFragment();
    }


    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(NetRadioControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(NetRadioControlFragment.class.getSimpleName());
        }
        return new NetRadioControlFragment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PlayControlImpl.getInstance().unregisterPlayControlListener(mControlCallback);
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_net_radio_control;
    }

    @Override
    public void actionsOnViewInflate() {
        super.actionsOnViewInflate();
        mBinding.ibNetRadioControlLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayControlImpl.getInstance().playPre();
            }
        });
        mBinding.ibNetRadioControlPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayControlImpl.getInstance().playOrPause();
            }
        });
        mBinding.ibNetRadioControlNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayControlImpl.getInstance().playNext();
            }
        });
        mBinding.tvNetRadioControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonActivity.startCommonActivity(getActivity(), NetRadioFragment.class.getCanonicalName());
            }
        });
        PlayControlImpl.getInstance().registerPlayControlListener(mControlCallback);
        PlayControlImpl.getInstance().refresh();
    }


    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTitleChange(String title) {
            super.onTitleChange(title);
            if (null == title) return;
            if (PlayControlImpl.getInstance().getMode() != PlayControlImpl.MODE_NET_RADIO) return;
            mBinding.tvNetRadioControl.setText(title);
            NetRadioUpdateSelectData currentEntity = new NetRadioUpdateSelectData();
            currentEntity.setTitle(title);
            currentEntity.setSelected(true);
            ArrayList<NetRadioUpdateSelectData> list = new ArrayList<>();
            list.add(currentEntity);
            DataRepository.getInstance().updateNetRadioCurrentPlayInfo(list, null);
        }

        @Override
        public void onModeChange(int mode) {
            super.onModeChange(mode);
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            mBinding.ibNetRadioControlPlay.setSelected(isPlay);
        }

        @Override
        public void onTimeChange(int current, int total) {
            super.onTimeChange(current, total);
            Log.e(TAG, "onTimeChange: current:" + current + "total:" + total);
        }

    };

}
