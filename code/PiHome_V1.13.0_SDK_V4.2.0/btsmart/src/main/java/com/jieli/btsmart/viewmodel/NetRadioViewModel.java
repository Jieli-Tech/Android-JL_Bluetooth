package com.jieli.btsmart.viewmodel;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.jieli.audio.media_player.AudioFocusManager;
import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.tool.playcontroller.NetRadioPlayControlImpl;
import com.jieli.btsmart.tool.playcontroller.PlayControlCallback;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.tool.room.DataRepository;
import com.jieli.btsmart.tool.room.entity.UserEntity;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.jl_http.bean.NetRadioListInfo;

import java.util.ArrayList;
import java.util.List;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;
import static com.jieli.btsmart.tool.playcontroller.PlayControlImpl.MODE_NET_RADIO;
import static com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel.NET_RADIO_PLAY_LIST_TYPE_LOCAL;

/**
 * @ClassName: NetRadio2ViewModel
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2020/11/12 15:52
 */
public class NetRadioViewModel extends BtBasicVM implements LifecycleObserver {
    private final String TAG = NetRadioViewModel.class.getSimpleName();
    public MutableLiveData<NetRadioListInfo> currentPlayRadioInfoLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> loadingFailedLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> playStateLiveData = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> refreshIconLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> dropDownState = new MutableLiveData<>(false);
    public MediatorLiveData<List<NetRadioListInfo>> collectNetRadioLiveData;
    private List<NetRadioListInfo> mLocalNetRadioList = null;
    private MutableLiveData<List<NetRadioListInfo>> mCurrentPlayRadioListLiveData = new MutableLiveData<>();
    private boolean isHandlePauseDeviceMusic = false;
    private boolean mIsItemViewClick = false;
    private UserEntity mUserEntity;

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        getNetRadioPlayList();
        PlayControlImpl.getInstance().registerPlayControlListener(mControlCallback);
        PlayControlImpl.getInstance().refresh();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        saveNetRadioPlayList();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        PlayControlImpl.getInstance().unregisterPlayControlListener(mControlCallback);
    }

    /**
     * 设置用户信息
     */
    public void setUserEntity(UserEntity userEntity) {
        mUserEntity = userEntity;
        getCollectedNetRadios();
    }

    public void setLocalNetRadioInfo(List<NetRadioListInfo> localNetRadioList) {
        mLocalNetRadioList = localNetRadioList;
        checkPlayListIsEmpty();
    }

    public void handleAddCollectedNetRadio(NetRadioListInfo radioListInfo) {
        if (currentPlayRadioInfoLiveData.getValue().getId().equals(radioListInfo.getId())) {
            currentPlayRadioInfoLiveData.getValue().getCollectStateLiveData().postValue(true);
        }
        updateNetRadioPlayList(radioListInfo, true);
    }

    public void handleDeleteCollectedNetRadio(NetRadioListInfo deleteNetRadio) {
        if (currentPlayRadioInfoLiveData.getValue().getId().equals(deleteNetRadio.getId())) {
            currentPlayRadioInfoLiveData.getValue().getCollectStateLiveData().postValue(false);
        }
        updateNetRadioPlayList(deleteNetRadio, false);
    }

    public void handlePlayRadioCallback(List<NetRadioListInfo> radioInfos, int position, int listType) {
        mIsItemViewClick = true;
        handleNetRadioListInfo(radioInfos, listType);
        playRadio(radioInfos, position);
    }

    public void playOrPause() {
        if (PlayControlImpl.getInstance().getMode() == MODE_NET_RADIO) {//当前模式是网络电台
            if (!AudioFocusManager.getInstance().isHasAudioFocus() && !PlayControlImpl.getInstance().isPlay()) {//没有焦点且不在播放
                AudioFocusManager.getInstance().requestAudioFocus();//抢焦点
            }
            if (!NetworkHelper.getInstance().checkNetworkAvailableAndToast()) return;//网络不可用的时候就不要播放
            NetRadioPlayControlImpl.getInstance(AppUtil.getContext()).playOrPause();//播放或暂停
        } else {
            NetRadioListInfo currentPlayRadioInfo = currentPlayRadioInfoLiveData.getValue();
            final int noCurrentPlayPosition = -1;
            int currentPlayPosition = currentPlayRadioInfo == null ? noCurrentPlayPosition : currentPlayRadioInfo.getPosition();
            playRadio(mCurrentPlayRadioListLiveData.getValue(), currentPlayPosition);//其他模式需要传输数据进去
        }
    }

    private void getCollectedNetRadios() {
        if (collectNetRadioLiveData == null)
            collectNetRadioLiveData = DataRepository.getInstance().getCollectedNetRadiosByUserId(mUserEntity.userId);//此处可能单独使用的时候没有数据，因为必须要有observe，MediatorLiveData才会触发onChange
    }

    /**
     * 获取播放列表
     */
    private void getNetRadioPlayList() {
        mCurrentPlayRadioListLiveData = DataRepository.getInstance().getNetRadioPlayList();
        //这个本地播放的信息可能可以直接使用
        currentPlayRadioInfoLiveData = DataRepository.getInstance().getCurrentNetRadioPlayInfo();
    }

    private void checkPlayListIsEmpty() {
        List<NetRadioListInfo> playRadioList = mCurrentPlayRadioListLiveData.getValue();
        if (playRadioList == null || playRadioList.isEmpty()) {//没有历史播放记录
            syncLocalNetRadioListInfoToPlayList();
        }
    }

    private void syncLocalNetRadioListInfoToPlayList() {
        if (mLocalNetRadioList != null && !mLocalNetRadioList.isEmpty()) {
            handleNetRadioListInfo(mLocalNetRadioList, NET_RADIO_PLAY_LIST_TYPE_LOCAL);
            syncNetRadioListInfoListToPlayControlList(mLocalNetRadioList);
        }
    }

    /**
     * 同步网络电台列表到播放列表
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void syncNetRadioListInfoListToPlayControlList(List<NetRadioListInfo> list) {
        List<NetRadioListInfo> listInfo;
        listInfo = list;
        NetRadioListInfo radioInfo = listInfo.get(0);
        setCurrentNetRadioPlayInfo(radioInfo);
        mCurrentPlayRadioListLiveData.setValue(listInfo);//如果出现问题就使用切回主线程
        syncPlayControlPlayList(listInfo, 0);
    }

    private int getCurrentPlayPosition(List<NetRadioListInfo> radioListInfo, String currentPlayNetRadioName) {
        int position = -1;
        if (radioListInfo == null || radioListInfo.isEmpty()) return position;
        for (int i = 0; i < radioListInfo.size(); i++) {
            NetRadioListInfo radioInfo = radioListInfo.get(i);
            if (radioInfo.getName().equals(currentPlayNetRadioName)) {
                position = i;
            }
        }
        return position;
    }

    private void handleNetRadioListInfo(List<NetRadioListInfo> radioInfos, int listType) {
        List<NetRadioListInfo> collectedRadioInfos = collectNetRadioLiveData.getValue();
        for (int i = 0; i < radioInfos.size(); i++) {
            NetRadioListInfo netRadioListInfo = radioInfos.get(i);
            netRadioListInfo.setListType(listType);
            netRadioListInfo.setPosition(i);
            LiveData<Boolean> collectStateLiveData = netRadioListInfo.getCollectStateLiveData();
            if (collectStateLiveData == null) {
                netRadioListInfo.setCollectStateLiveData(new MutableLiveData<>(false));
            }
            boolean isEmptyCollectedList = collectedRadioInfos == null || collectedRadioInfos.isEmpty();
            if (!isEmptyCollectedList && collectedRadioInfos.contains(netRadioListInfo)) {
                changNetRadioListInfoCollectState(netRadioListInfo, true);
            }
        }
    }

    private void changNetRadioListInfoCollectState(NetRadioListInfo netRadioListInfo, boolean isCollect) {
        LiveData<Boolean> collectStateLiveData = netRadioListInfo.getCollectStateLiveData();
        if (collectStateLiveData == null) {
            netRadioListInfo.setCollectStateLiveData(new MutableLiveData<>(isCollect));
        } else if (collectStateLiveData.getValue() == null || collectStateLiveData.getValue() != isCollect) {
            netRadioListInfo.getCollectStateLiveData().setValue(isCollect);
        }
    }

    private void updateNetRadioPlayList(NetRadioListInfo updateInfo, boolean isCollect) {
        List<NetRadioListInfo> playList = mCurrentPlayRadioListLiveData.getValue();
        if (playList == null || playList.isEmpty()) return;
        for (NetRadioListInfo radioListInfo : playList) {
            boolean isSameInfo = radioListInfo.getName().equals(updateInfo.getName());
            if (!isSameInfo) continue;
            changNetRadioListInfoCollectState(radioListInfo, isCollect);
        }
    }

    /**
     * 播放网络电台
     *
     * @param radioList 播放的电台列表
     * @param position  序号
     */
    private void playRadio(List<NetRadioListInfo> radioList, int position) {
        if (radioList == null || position < 0) return;
        if (!NetworkHelper.getInstance().checkNetworkAvailableAndToast()) return;//网络不可用的时候就不要播放
        mCurrentPlayRadioListLiveData.setValue(radioList);
        setCurrentNetRadioPlayInfo(radioList.get(position));
        boolean isPlayingDeviceMusic = PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_MUSIC && PlayControlImpl.getInstance().isPlay();
        if (isPlayingDeviceMusic) {
            isHandlePauseDeviceMusic = true;
            PlayControlImpl.getInstance().pause();
        } else {
            handlePlayRadio();
        }
//        setCurrentNetRadioPlayInfo(radioList.get(position));
    }

    private void handlePlayRadio() {
        List<NetRadioListInfo> playRadioList = mCurrentPlayRadioListLiveData.getValue();
        int position = currentPlayRadioInfoLiveData.getValue().getPosition();
        handlePlayRadioOperation(playRadioList, position);
    }

    private void handlePlayRadioOperation(List<NetRadioListInfo> radioList, int position) {
        if (radioList == null || position < 0) return;
        JL_Log.d(TAG, "handlePlayRadioOperation", "");
        if (ALLOW_SWITCH_FUN_DISCONNECT && !mRCSPController.isDeviceConnected()) {
//            PlayControlImpl.getInstance().updateMode(PlayControlImpl.MODE_BT);
            PlayControlImpl.getInstance().updateMode(MODE_NET_RADIO);
        }
        if (PlayControlImpl.getInstance().getMode() != MODE_NET_RADIO) {
            PlayControlImpl.getInstance().updateMode(MODE_NET_RADIO);
        }
        syncPlayControlPlayList(radioList, position);
        PlayControlImpl.getInstance().play();
//            NetRadioPlayControlImpl.getInstance(AppUtil.getContext()).play();
    }

    /**
     * 同步NetRadioPlayControl的播放列表和选中位置
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void syncPlayControlPlayList(List<NetRadioListInfo> radioList, int position) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < radioList.size(); i++) {
            NetRadioListInfo radioInfo = radioList.get(i);
            Music music1 = new Music();
            music1.setId(i + 1);
            music1.setTitle(radioInfo.getName());
            music1.setUrl(radioInfo.getStream());
            music1.setCoverUrl(radioInfo.getIcon());
            music1.setLocal(4);
            arrayList.add(music1);
        }
        NetRadioPlayControlImpl.getInstance(AppUtil.getContext()).setPlayList(arrayList);
        if (position >= 0) {
            NetRadioPlayControlImpl.getInstance(AppUtil.getContext()).setPlayPosition(position);
        }
    }

    /**
     * 设置当前的播放信息
     */
    private void setCurrentNetRadioPlayInfo(NetRadioListInfo netRadioListInfo) {
        if (netRadioListInfo == null) return;
        JL_Log.d(TAG, "setCurrentNetRadioPlayInfo", "name: " + netRadioListInfo.getName() + "  mIsItemViewClick:  " + mIsItemViewClick + "refreshIconLiveData :  " + refreshIconLiveData);
        currentPlayRadioInfoLiveData.setValue(netRadioListInfo);
        if (!mIsItemViewClick && refreshIconLiveData != null) {
            refreshIconLiveData.postValue(true);
        } else {
            mIsItemViewClick = false;
        }
    }

    /**
     * 收藏历史播放列表和当前播放的歌曲信息
     */
    private void saveNetRadioPlayList() {
        DataRepository.getInstance().saveNetRadioPlayList(mCurrentPlayRadioListLiveData.getValue(), null);
        DataRepository.getInstance().saveNetRadioCurrentPlayInfo(currentPlayRadioInfoLiveData.getValue(), null);
    }

    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTitleChange(String title) {
            super.onTitleChange(title);
            JL_Log.d(TAG, "onTitleChange", "title  : " + title);
            if (PlayControlImpl.getInstance().getMode() != MODE_NET_RADIO) return;
            if (null == title) {
                NetRadioListInfo netRadioListInfo = new NetRadioListInfo();
                setCurrentNetRadioPlayInfo(netRadioListInfo);
                return;
            }
            NetRadioListInfo currentPlayRadioInfo = currentPlayRadioInfoLiveData.getValue();
            if (currentPlayRadioInfo != null && title.equals(currentPlayRadioInfo.getName())) {
                JL_Log.d(TAG, "onTitleChange", "title  : " + title);
                return;
            }
            //这里是为了应对蓝牙设备可以让播放器切播放位置
            List<NetRadioListInfo> playRadioList = mCurrentPlayRadioListLiveData.getValue();
            int playPosition = getCurrentPlayPosition(playRadioList, title);
            if (playPosition < 0 || null == playRadioList) return;
            setCurrentNetRadioPlayInfo(playRadioList.get(playPosition));
        }

        @Override
        public void onModeChange(int mode) {
            super.onModeChange(mode);
            //当在模式变为音乐模式且处于可见状态时，onStart一下
            if (mode != MODE_NET_RADIO) {
                NetRadioPlayControlImpl.getInstance(AppUtil.getContext()).pause();
                playStateLiveData.postValue(false);
            }
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_MUSIC && isHandlePauseDeviceMusic) {
                JL_Log.d(TAG, "onPlayStateChange", "MODE_MUSIC : " + isPlay);
                isHandlePauseDeviceMusic = false;
                handlePlayRadio();
            }
            if (PlayControlImpl.getInstance().getMode() == MODE_NET_RADIO) {
                JL_Log.d(TAG, "onPlayStateChange", "MODE_NET_RADIO  : " + isPlay);
                playStateLiveData.postValue(isPlay);
            }
        }
    };
}
