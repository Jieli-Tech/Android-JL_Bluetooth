package com.jieli.btsmart.ui.multimedia.control.music;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.base.BaseFragment;

public class BlankMusicControlFragment extends BaseFragment {

    public static Fragment newInstance() {
        return new BlankMusicControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if(fragmentManager.findFragmentByTag(BlankMusicControlFragment.class.getSimpleName())!=null){
            return   fragmentManager.findFragmentByTag(BlankMusicControlFragment.class.getSimpleName());
        }
        return new BlankMusicControlFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank_music_control, container, false);
    }
}
