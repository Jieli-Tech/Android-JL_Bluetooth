package com.jieli.btsmart.ui.soundcard;

import android.view.View;

import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.data.model.soundcard.SoundCard;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/19 7:27 PM
 * @desc :
 */
class ActionClickHandler implements View.OnClickListener {
    private final String tag = getClass().getSimpleName();

    SoundCard.Functions.ListBean listBean;

    public ActionClickHandler(SoundCard.Functions.ListBean listBean) {
        this.listBean = listBean;
    }

    @Override
    public void onClick(View v) {
        RCSPController.getInstance().setSoundCardFunction(RCSPController.getInstance().getUsingDevice(), (byte) listBean.index, 0, null);
    }


}
