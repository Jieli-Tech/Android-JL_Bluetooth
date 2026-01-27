package com.jieli.btsmart.ui.multimedia.control.net_radio;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.bluetooth.utils.JL_Log;
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
 * NetRadioControlFragment
 * @author zqjasonZhong
 * @since 2025/5/9
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 网路电台控制界面
 */
public class NetRadioControlFragment extends BaseViewModelFragment<FragmentNetRadioControlBinding> {

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
        mBinding.ibNetRadioControlLast.setOnClickListener(v -> PlayControlImpl.getInstance().playPre());
        mBinding.ibNetRadioControlPlay.setOnClickListener(v -> PlayControlImpl.getInstance().playOrPause());
        mBinding.ibNetRadioControlNext.setOnClickListener(v -> PlayControlImpl.getInstance().playNext());
        mBinding.tvNetRadioControl.setOnClickListener(v -> CommonActivity.startCommonActivity(getActivity(), NetRadioFragment.class.getCanonicalName()));
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
            JL_Log.e(TAG, "onTimeChange: current:" + current + "total:" + total);
        }

    };

}
