package com.jieli.btsmart.ui.multimedia.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.component.base.Jl_BaseFragment;


/**
 * Use the {@link LineInControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LineInControlFragment extends Jl_BaseFragment {

    private ImageButton ibPlayOrPause;

    public LineInControlFragment() {

    }

    public static LineInControlFragment newInstance() {
        return new LineInControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(LineInControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(LineInControlFragment.class.getSimpleName());
        }
        return new LineInControlFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_linein_control, container, false);
        ibPlayOrPause = view.findViewById(R.id.ib_play_or_pause);
        ibPlayOrPause.setOnClickListener(v -> PlayControlImpl.getInstance().playOrPause());
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PlayControlImpl.getInstance().registerPlayControlListener(mControlCallback);
        PlayControlImpl.getInstance().refresh();
    }

    @Override
    public void onDestroyView() {
        PlayControlImpl.getInstance().unregisterPlayControlListener(mControlCallback);
        super.onDestroyView();
    }

    private boolean isFragmentAlive() {
        return isAdded() && !isDetached();
    }

    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onPlayStateChange(boolean isPlay) {
            if (isFragmentAlive() && ibPlayOrPause != null) {
                JL_Log.d(TAG, "12134 onPlayStateChange : " + isPlay);
                ibPlayOrPause.setSelected(isPlay);
            }
        }

    };


}
