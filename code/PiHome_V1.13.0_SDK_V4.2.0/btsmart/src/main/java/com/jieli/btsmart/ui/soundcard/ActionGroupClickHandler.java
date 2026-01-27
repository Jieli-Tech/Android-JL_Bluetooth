package com.jieli.btsmart.ui.soundcard;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.jieli.btsmart.data.model.soundcard.SoundCard;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/19 7:27 PM
 * @desc :
 */
class ActionGroupClickHandler implements View.OnClickListener {

    private final SoundCard.Functions.ListBean listBean;

    public ActionGroupClickHandler(SoundCard.Functions.ListBean listBean) {
        this.listBean = listBean;
    }

    @Override
    public void onClick(View v) {
        FragmentActivity context = (FragmentActivity) v.getContext();
        Fragment fragment = context.getSupportFragmentManager().findFragmentByTag(GroupDialog.class.getCanonicalName());
        GroupDialog groupDialog = fragment == null ? new GroupDialog() : (GroupDialog) fragment;
        if (groupDialog.isShow()) return;
        groupDialog.setList(listBean.list);
        groupDialog.show(context.getSupportFragmentManager(), groupDialog.getClass().getCanonicalName());
    }
}
