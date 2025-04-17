package com.jieli.btsmart.ui.multimedia.control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.btsmart.R;
import com.jieli.component.base.Jl_BaseFragment;


/**
 * A simple {@link Fragment} subclass.
 */
public class BlankControlFragment extends Jl_BaseFragment {

    private TextView tvContent;

    public final static String KEY_CONTENT_TEXT = "content_text";

    public BlankControlFragment() {
        // Required empty public constructor
    }

    public static Fragment newInstance() {
        return new BlankControlFragment();
    }


    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if(fragmentManager.findFragmentByTag(BlankControlFragment.class.getSimpleName())!=null){
            return   fragmentManager.findFragmentByTag(BlankControlFragment.class.getSimpleName());
        }
        return new BlankControlFragment();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blank_control, container, false);
        tvContent = view.findViewById(R.id.tv_unconnected_device_tips);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        String content = getString(R.string.unconnected_device_tips);
        if(getBundle() != null){
            String text = getBundle().getString(KEY_CONTENT_TEXT);
            if(text != null && !text.equals(content)){
                tvContent.setText(text);
            }
        }
    }
}
