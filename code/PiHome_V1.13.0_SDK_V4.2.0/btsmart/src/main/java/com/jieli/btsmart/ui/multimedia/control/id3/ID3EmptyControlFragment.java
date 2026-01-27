package com.jieli.btsmart.ui.multimedia.control.id3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.base.BaseFragment;

public class ID3EmptyControlFragment extends BaseFragment {

    public static Fragment newInstance() {
        return new ID3EmptyControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(ID3EmptyControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(ID3EmptyControlFragment.class.getSimpleName());
        }
        return new ID3EmptyControlFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blank_music_control, container, false);
        TextView tipTextView = view.findViewById(R.id.tv_unconnected_device_tips);
        tipTextView.setText(R.string.none_id3_music_data);
        return view;
    }
}
