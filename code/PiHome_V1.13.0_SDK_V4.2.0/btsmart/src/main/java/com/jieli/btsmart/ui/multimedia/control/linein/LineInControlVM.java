package com.jieli.btsmart.ui.multimedia.control.linein;

import androidx.lifecycle.MutableLiveData;

import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;

/**
 * LineInControlVM
 * @author zqjasonZhong
 * @since 2025/5/8
 * @email zhongzhuocheng@zh-jieli.com
 * @desc LineIn控制逻辑实现
 */
public class LineInControlVM extends BtBasicVM {

    private final PlayControlImpl mPlayControl;
    public final MutableLiveData<Boolean> playStatusMLD = new MutableLiveData<>();

    public LineInControlVM() {
        mPlayControl = PlayControlImpl.getInstance();
        mPlayControl.registerPlayControlListener(mControlCallback);
    }

    public void refresh() {
        mPlayControl.refresh();
    }

    public void playOrPause() {
        mPlayControl.playOrPause();
    }

    @Override
    protected void release() {
        mPlayControl.unregisterPlayControlListener(mControlCallback);
        super.release();
    }

    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onPlayStateChange(boolean isPlay) {
            playStatusMLD.postValue(isPlay);
        }
    };
}
