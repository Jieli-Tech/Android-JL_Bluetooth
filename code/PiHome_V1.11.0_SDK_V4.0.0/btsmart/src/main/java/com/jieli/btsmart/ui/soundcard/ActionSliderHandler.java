package com.jieli.btsmart.ui.soundcard;

import android.widget.SeekBar;

import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.data.model.soundcard.SoundCard;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/19 7:27 PM
 * @desc :
 */
class ActionSliderHandler implements SeekBar.OnSeekBarChangeListener {
    SoundCard.Functions.ListBean listBean;

    public ActionSliderHandler(SoundCard.Functions.ListBean listBean) {
        this.listBean = listBean;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        RCSPController.getInstance().setSoundCardFunction(RCSPController.getInstance().getUsingDevice(), (byte) listBean.index, seekBar.getProgress(), null);
    }


}
