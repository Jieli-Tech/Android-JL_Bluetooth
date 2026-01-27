package com.jieli.btsmart.ui.multimedia.control.id3;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.music.ID3MusicInfo;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.btsmart.ui.base.bluetooth.IBluetoothBase;

/**
 * ID3信息接口
 *
 * @author zqjasonZhong
 * @since 2020/6/5
 */
public interface ID3ControlContract {

    interface ID3ControlPresenter extends IBluetoothBase.IBluetoothPresenter {

        int showPlayerFlag();

        void updatePlayerFlag(int flag);

        ID3MusicInfo getMusicInfo();

        void getID3MusicInfo();

        void playID3Prev();

        void playID3Next();

        void playOrPauseID3(boolean srcPlayState);

        void destroy();
    }

    interface ID3ControlView extends IBluetoothBase.IBluetoothView {

        void onID3CmdSuccess(ADVInfoResponse advInfo);

        void onID3CmdFailed(BaseError error);

        void onID3MusicInfo(ID3MusicInfo id3MusicInfo);
    }
}
