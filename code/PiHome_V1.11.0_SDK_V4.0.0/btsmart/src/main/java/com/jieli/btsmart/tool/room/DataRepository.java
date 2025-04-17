package com.jieli.btsmart.tool.room;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.jieli.btsmart.tool.room.entity.FMCollectInfoEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioCollectAndUserEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioInfoEntity;
import com.jieli.btsmart.tool.room.entity.NetRadioPlayInfoEntity;
import com.jieli.btsmart.tool.room.entity.UserEntity;
import com.jieli.jl_http.bean.NetRadioListInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/6/29 9:49
 * @desc :存储库-RoomTest
 */
public class DataRepository {
    private static DataRepository sInstance;
    private final AppDatabase mDatabase;
    private final MediatorLiveData<List<NetRadioListInfo>> mObservableNetRadioCollectInfo;

    private DataRepository(final AppDatabase appDatabase) {
        mDatabase = appDatabase;
        mObservableNetRadioCollectInfo = new MediatorLiveData<>();
    }

    public static DataRepository getInstance(final AppDatabase database) {
        if (sInstance == null) {
            synchronized (DataRepository.class) {
                if (sInstance == null) {
                    sInstance = new DataRepository(database);
                }
            }
        }
        return sInstance;
    }

    public static DataRepository getInstance() {
        return sInstance;
    }

    public LiveData<List<FMCollectInfoEntity>> getFMCollectInfo(String address) {
        return mDatabase.fmCollectInfoDao().getAll(address);
    }

    /**
     * 添加用户信息
     *
     * @param
     */
    public void insertUser(UserEntity entity, DataRepositoryCallback callback) {
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                List list = mDatabase.userDao().findUserByUserName(entity.userName);
                Log.e("ZHM_fm", "run: SIZE:" + list.size());
                if (list.size() == 0) {
                    if (callback != null)
                        callback.success();
                    mDatabase.userDao().insert(entity);
                } else {
                    if (callback != null)
                        callback.failed();
                }
            }
        });
    }

    /**
     * 删除用户信息
     *
     * @param entity 用户信息
     */
    public void deleteUser(UserEntity entity) {
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                mDatabase.userDao().delete(entity);
            }
        });
    }

    /**
     * 添加FM收藏信息
     *
     * @param
     * @desc
     */
    public void insertFMCollectInfo(FMCollectInfoEntity entity, DataRepositoryCallback callback) {
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                List list = mDatabase.fmCollectInfoDao().getByAddressAndModeAnd(entity.btDeviceAddress, entity.mode, entity.freq);
                Log.e("ZHM_fm", "run: SIZE:" + list.size());
                if (list.size() == 0) {
                    callback.success();
                    mDatabase.fmCollectInfoDao().insert(entity);
                } else {
                    callback.failed();
                }
            }
        });
    }

    /**
     * 删除FM收藏信息
     */
    public void deleteFMCollectInfo(FMCollectInfoEntity entity) {
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                mDatabase.fmCollectInfoDao().delete(entity);
            }
        });
    }

    private List<LiveData<List<NetRadioInfoEntity>>> mCollectedNetRadioSource = new ArrayList<>();

    /**
     * 获取用户收藏的网络电台信息
     *
     * @param userId 用户的userId(UserEntity)
     */
    public MediatorLiveData<List<NetRadioListInfo>> getCollectedNetRadiosByUserId(int userId) {
        if (!mCollectedNetRadioSource.isEmpty()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    LiveData<List<NetRadioInfoEntity>> lastSource = mCollectedNetRadioSource.get(0);
                    mObservableNetRadioCollectInfo.removeSource(lastSource);
                }
            });
        }
        LiveData<List<NetRadioInfoEntity>> source = mDatabase.netRadioInfoDao().getNetRadioInfoByUserId(userId);
        mCollectedNetRadioSource.add(source);
        mObservableNetRadioCollectInfo.addSource(source, Entities -> {
            ArrayList<NetRadioListInfo> list = new ArrayList<NetRadioListInfo>();
            for (NetRadioInfoEntity entity : Entities) {
                NetRadioListInfo info = new NetRadioListInfo();
                info.setId(entity.radioInfoId);
                info.setUuid(entity.uuid);
                info.setPlaceId(entity.placeId);
                info.setName(entity.name);
                info.setIcon(entity.icon);
                info.setStream(entity.stream);
                info.setCollectState(true);
                list.add(info);
            }
            mObservableNetRadioCollectInfo.setValue(list);
        });
        return mObservableNetRadioCollectInfo;
    }

    /**
     * 添加用户收藏的网络电台
     *
     * @param netRadioCollectAndUserEntity 用户信息和网络电台信息的关系表
     * @param radioListInfo           网络电台信息
     */
    public void insertNetRadioCollect(NetRadioCollectAndUserEntity netRadioCollectAndUserEntity, NetRadioListInfo radioListInfo, DataRepositoryCallback callback) {
        NetRadioInfoEntity netRadioInfoEntity = new NetRadioInfoEntity();
        netRadioInfoEntity.setCollectState(true);
        netRadioInfoEntity.setId(radioListInfo.getId());
        netRadioInfoEntity.setUuid(radioListInfo.getUuid());
        netRadioInfoEntity.setPlaceId(radioListInfo.getPlaceId());
        netRadioInfoEntity.setName(radioListInfo.getName());
        netRadioInfoEntity.setIcon(radioListInfo.getIcon());
        netRadioInfoEntity.setStream(radioListInfo.getStream());
        netRadioInfoEntity.setDescription(radioListInfo.getDescription());
        netRadioInfoEntity.setUpdateTime(radioListInfo.getUpdateTime());
        netRadioInfoEntity.setExplain(radioListInfo.getExplain());
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                List<NetRadioInfoEntity> list1 = mDatabase.netRadioInfoDao().getNetRadioInfoByRadioInfoId(netRadioInfoEntity.radioInfoId);
                if (list1.size() == 0) {
                    mDatabase.netRadioInfoDao().insertNetRadioInfo(netRadioInfoEntity);
                }
                List list = mDatabase.netRadioInfoDao().getNetRadioCollectAndUserEntityByUserIdAndRadioInfoId(netRadioCollectAndUserEntity.userId, netRadioCollectAndUserEntity.radioInfoId);
                if (list.size() == 0) {
                    mDatabase.netRadioInfoDao().insertCollectAndUser(netRadioCollectAndUserEntity);
                }
            }
        });
    }

    /**
     * 删除网络电台收藏
     *
     * @param entity 用户信息和网络电台信息的关系表
     */
    public void deleteNetRadioCollect(NetRadioCollectAndUserEntity entity) {
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                mDatabase.netRadioInfoDao().deleteCollectByUserAndRadio(entity);
            }
        });
    }

    /**
     * 检查网络电台是否已经收藏
     *
     * @param entity 用户信息和网络电台信息的关系表
     */
    public void checkNetRadioIsCollected(NetRadioCollectAndUserEntity entity, DataRepositoryCallback callback) {
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                List list = mDatabase.netRadioInfoDao().getNetRadioCollectAndUserEntityByUserIdAndRadioInfoId(entity.userId, entity.radioInfoId);
                if (list.size() != 0) {
                    callback.success();
                }
            }
        });
    }

    /**
     * 查询网络电台播放列表
     */
    public MutableLiveData<List<NetRadioListInfo>> getNetRadioPlayList() {
        MutableLiveData<List<NetRadioListInfo>> listMutableLiveData = new MutableLiveData<>();
        mDatabase.getWeakExecutor().get().execute(() -> {
            List<NetRadioPlayInfoEntity> entitys = mDatabase.netRadioInfoDao().getAllNetRadioPlayInfo();
            List<NetRadioListInfo> listInfo = new ArrayList<>();
            if (entitys.size() != 0) {//有历史播放记录
                ArrayList<NetRadioListInfo> arrayList = new ArrayList<>();
                for (int i = 0; i < entitys.size(); i++) {
                    NetRadioPlayInfoEntity entity = entitys.get(i);
                    NetRadioListInfo info = new NetRadioListInfo();
                    info.setId(entity.radioInfoId);
                    info.setName(entity.getTitle());
                    info.setStream(entity.getUrl());
                    info.setIcon(entity.getCoverUrl());
                    info.setListType(entity.getListType());
                    info.setPosition(entity.getPosition());
                    info.setCollectStateLiveData(new MutableLiveData<>(entity.isCollect()));
                    arrayList.add(info);
                }
                listInfo = arrayList;
            }
            listMutableLiveData.postValue(listInfo);
        });
        return listMutableLiveData;
    }

    /**
     * 查询网络电台当前播放
     */
    public MutableLiveData<NetRadioListInfo> getCurrentNetRadioPlayInfo() {
        MutableLiveData<NetRadioListInfo> liveData = new MutableLiveData<>();
        mDatabase.getWeakExecutor().get().execute(() -> {
            NetRadioPlayInfoEntity entity = mDatabase.netRadioInfoDao().getCurrentNetRadioPlayInfo(true);
            if (entity == null) return;
            NetRadioListInfo info = new NetRadioListInfo();
            info.setId(entity.radioInfoId);
            info.setName(entity.getTitle());
            info.setStream(entity.getUrl());
            info.setIcon(entity.getCoverUrl());
            info.setListType(entity.getListType());
            info.setPosition(entity.getPosition());
            info.setCollectStateLiveData(new MutableLiveData<>(entity.isCollect()));
            liveData.postValue(info);
        });
        return liveData;
    }

    /**
     * 保存新的网络电台播放列表
     *
     * @param playInfos 网络电台列表
     */
    public void saveNetRadioPlayList(List<NetRadioListInfo> playInfos, DataRepositoryCallback callback) {
        Log.i("TAG", "saveNetRadioPlayList: " + playInfos.toString());
        mDatabase.getWeakExecutor().get().execute(() -> {
            mDatabase.netRadioInfoDao().deleteAllNetRadioPlayInfo();
            if (playInfos == null) return;
            ArrayList netPlayInfoEntityList = new ArrayList();
            for (int i = 0; i < playInfos.size(); i++) {
                NetRadioListInfo radioInfo = playInfos.get(i);
                NetRadioPlayInfoEntity entity = new NetRadioPlayInfoEntity();
                entity.setRadioInfoId(radioInfo.getId());
                entity.setListType(radioInfo.getListType());
                entity.setId(i + 1);
                entity.setTitle(radioInfo.getName());
                entity.setUrl(radioInfo.getStream());
                entity.setCoverUrl(radioInfo.getIcon());
                entity.setPosition(radioInfo.getPosition());
                Boolean isCollect = false;
                if (radioInfo.getCollectStateLiveData() != null) {
                    isCollect = radioInfo.getCollectStateLiveData().getValue();
                }
                entity.setCollect(isCollect == null ? false : isCollect);
                netPlayInfoEntityList.add(entity);
            }
            mDatabase.netRadioInfoDao().insertNetRadioPlayInfos(netPlayInfoEntityList);
            if (callback != null) {
                callback.success();
            }
        });
    }

    public void saveNetRadioCurrentPlayInfo(NetRadioListInfo currentPlayInfo, DataRepositoryCallback callback) {
        mDatabase.getWeakExecutor().get().execute(() -> {
            if (currentPlayInfo == null) return;
            mDatabase.netRadioInfoDao().resetNetRadioPlayInfosSelected();
            NetRadioUpdateSelectData currentEntity = new NetRadioUpdateSelectData();
            currentEntity.setTitle(currentPlayInfo.getName());
            currentEntity.setSelected(true);
            ArrayList<NetRadioUpdateSelectData> list = new ArrayList<>();
            list.add(currentEntity);
            mDatabase.netRadioInfoDao().updateNetRadioPlayInfo(list);
            if (callback != null) {
                callback.success();
            }
        });
    }

    /**
     * 更新播放状态
     */
    public void updateNetRadioCurrentPlayInfo(List<NetRadioUpdateSelectData> entitys, DataRepositoryCallback callback) {
        if (entitys == null) return;
        mDatabase.getWeakExecutor().get().execute(new Runnable() {
            @Override
            public void run() {
                mDatabase.netRadioInfoDao().resetNetRadioPlayInfosSelected();
                mDatabase.netRadioInfoDao().updateNetRadioPlayInfo(entitys);
                if (callback != null) {
                    callback.success();
                }
            }
        });
    }

    public interface DataRepositoryCallback {
        void success();

        void failed();
    }
}
