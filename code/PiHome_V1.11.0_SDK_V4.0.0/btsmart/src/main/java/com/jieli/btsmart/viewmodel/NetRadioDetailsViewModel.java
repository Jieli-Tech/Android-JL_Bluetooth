package com.jieli.btsmart.viewmodel;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.gson.Gson;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.location.LocationHelper;
import com.jieli.btsmart.tool.location.OnLocationInfoListener;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.tool.room.DataRepository;
import com.jieli.btsmart.tool.room.entity.NetRadioCollectAndUserEntity;
import com.jieli.btsmart.tool.room.entity.UserEntity;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.NetworkStateHelper;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.NetRadioListInfo;
import com.jieli.jl_http.bean.NetRadioRegionInfo;
import com.jieli.jl_http.bean.NetRadioResponse;
import com.jieli.jl_http.interfaces.IActionListener;
import com.jieli.jl_http.util.Constant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

/**
 * @ClassName: NetRadioDetailsViewModel
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2020/11/12 15:33
 */
public class NetRadioDetailsViewModel extends BtBasicVM implements LifecycleObserver {
    private final String TAG = this.getClass().getSimpleName();
    public MutableLiveData<List<NetRadioListInfo>> localNetRadioLiveData = new MutableLiveData<>();
    public MutableLiveData<List<NetRadioListInfo>> countryNetRadioLiveData = new MutableLiveData<>();
    public MutableLiveData<List<NetRadioListInfo>> provinceNetRadioLiveData = new MutableLiveData<>();
    public MediatorLiveData<List<NetRadioListInfo>> collectNetRadioLiveData;
    public MutableLiveData<List<NetRadioRegionInfo>> netRadioRegionsListLiveData = new MutableLiveData<>();

    //当前浏览的省市电台信息
    public MutableLiveData<NetRadioRegionInfo> browseRegionLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> collectedManageStateLiveData = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> loadingFailedLiveData = new MutableLiveData<>();
    public MutableLiveData<Boolean> refreshIconLiveData = new MutableLiveData<>();
    private final Map<String, Boolean> mLoadedStateFragmentMap = new HashMap<>();
    public static final int NET_RADIO_PLAY_LIST_TYPE_LOCAL = 1;
    public static final int NET_RADIO_PLAY_LIST_TYPE_COUNTRY = 2;
    public static final int NET_RADIO_PLAY_LIST_TYPE_PROVINCE = 3;
    public static final int NET_RADIO_PLAY_LIST_TYPE_COLLECT = 4;
    private Thread mThread;
    private boolean isRequestingRegionList = false;
    private NetRadioDetailsCallback mNetRadioDetailsCallback;
    private boolean onceGetLocation = true;
    private String mCurrentRegion = null;
    public NetRadioRegionInfo browseRegionInfo;
    public NetRadioRegionInfo currentRegionInfo;
    public NetRadioRegionInfo countryRegionInfo;
    private UserEntity mUserEntity;
    private String mNetRadioUserName = "13800000000";
    private String mNetRadioPassWord = "13800000000";
    private List<NetRadioRegionInfo> mRegionList = null;
    private final String TAG_NET_RADIO_BROWSE_PROVINCE = "net_radio_browse_province";
    private final String KEY_COUNTRY = "国家";
    //国家
    private boolean stopCheckRegionListAndLocationTask = false;
    private final int MSG_GET_REGION_TIME_OUT = 1;
    private final Handler mHandler = new Handler(msg -> {
        if (msg != null) {
            if (msg.what == MSG_GET_REGION_TIME_OUT) {
                loadingFailedLiveData.setValue(true);
            }
        }
        return false;
    });

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        saveNetRadioServerLoginInfo();
        mThread = new Thread(checkRegionListAndLocationTask);
        mThread.start();
        Gson gson = new Gson();
        String lastBrowseProvince = PreferencesHelper.getSharedPreferences(AppUtil.getContext()).getString(TAG_NET_RADIO_BROWSE_PROVINCE, null);
        NetRadioRegionInfo mLastBrowseProvince = gson.fromJson(lastBrowseProvince, NetRadioRegionInfo.class);
        if (mLastBrowseProvince != null) {
            syncBrowseRegion(mLastBrowseProvince);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        mNetRadioDetailsCallback = null;
    }

    /**
     * 设置用户信息
     */
    public void setUserEntity(UserEntity userEntity) {
        mUserEntity = userEntity;
    }

    public void setNetRadioDetailsCallback(NetRadioDetailsCallback callback) {
        this.mNetRadioDetailsCallback = callback;
    }

    public void addCollectRadio(NetRadioListInfo radioListInfo) {
        JL_Log.i(TAG, "addCollectRadio: " + radioListInfo.toString());
        NetRadioCollectAndUserEntity netRadioCollectAndUserEntity = new NetRadioCollectAndUserEntity();
        netRadioCollectAndUserEntity.userId = mUserEntity.userId;
        netRadioCollectAndUserEntity.radioInfoId = radioListInfo.getId();
        DataRepository.getInstance().insertNetRadioCollect(netRadioCollectAndUserEntity, radioListInfo, null);
        mNetRadioDetailsCallback.addCollectRadioCallback(radioListInfo);
    }

    public void deleteCollectedRadio(NetRadioListInfo radioListInfo) {
        JL_Log.i(TAG, "deleteCollectedRadio: " + radioListInfo.toString());
        NetRadioCollectAndUserEntity deleteEntity = new NetRadioCollectAndUserEntity();
        deleteEntity.radioInfoId = radioListInfo.getId();
        deleteEntity.userId = mUserEntity.userId;
        DataRepository.getInstance().deleteNetRadioCollect(deleteEntity);
        mNetRadioDetailsCallback.deleteCollectNetRadioCallback(radioListInfo);
    }

    public void getLocalNetRadios() {
        getCorrespondingNetRadio(NET_RADIO_PLAY_LIST_TYPE_LOCAL);
    }

    public void getCountryNetRadios() {
        getCorrespondingNetRadio(NET_RADIO_PLAY_LIST_TYPE_COUNTRY);
    }

    public void getProvinceNetRadios() {
        getCorrespondingNetRadio(NET_RADIO_PLAY_LIST_TYPE_PROVINCE);
    }

    public void getCollectedNetRadios() {
        if (collectNetRadioLiveData == null)
            collectNetRadioLiveData = DataRepository.getInstance().getCollectedNetRadiosByUserId(mUserEntity.userId);
    }

    /**
     * 设置当前浏览的省份（立即刷新请求数据）
     *
     * @param regionInfo 省份信息
     */
    public void setBrowseRegion(NetRadioRegionInfo regionInfo) {
        syncBrowseRegion(regionInfo);
        getProvinceNetRadios();
    }

    public void playRadio(List<NetRadioListInfo> radioInfos, int position, int listType) {
        if (mNetRadioDetailsCallback != null) {
            mNetRadioDetailsCallback.playRadioCallback(radioInfos, position, listType);
        }
    }

    @Deprecated
    public void refreshStateChange(String fragmentTag, boolean isFinishRefresh) {
        if (fragmentTag != null) {
            mLoadedStateFragmentMap.put(fragmentTag, isFinishRefresh);
        }
    }

    @Deprecated
    public Map<String, Boolean> getRefreshStateChangeMap() {
        return mLoadedStateFragmentMap;
    }

    public String getNetRadioUserName() {
        return mNetRadioUserName;
    }

    public void setNetRadioUserName(String mNetRadioUserName) {
        this.mNetRadioUserName = mNetRadioUserName;
    }

    public String getNetRadioPassWord() {
        return mNetRadioPassWord;
    }

    public void setNetRadioPassWord(String mNetRadioPassWord) {
        this.mNetRadioPassWord = mNetRadioPassWord;
    }

    /**
     * 登录服务器获取Token
     *
     * @param
     * @desc
     */
    private void saveNetRadioServerLoginInfo() {
        JL_Log.i(TAG, "saveNetRadioServerLoginInfo");
        PreferencesHelper.putStringValue(AppUtil.getContext(), Constant.KEY_USER_NAME, mNetRadioUserName);
        PreferencesHelper.putStringValue(AppUtil.getContext(), Constant.KEY_PASS_WORD, mNetRadioPassWord);
    }

    /**
     * 同步省份信息
     */
    private void syncBrowseRegion(NetRadioRegionInfo regionInfo) {
        browseRegionInfo = regionInfo;
        browseRegionLiveData.setValue(regionInfo);
        PreferencesHelper.putStringValue(AppUtil.getContext(), TAG_NET_RADIO_BROWSE_PROVINCE, regionInfo.toString());
    }

    private void getCorrespondingNetRadio(int NetRadioType) {
        NetRadioRegionInfo targetRegionInfo;
        switch (NetRadioType) {
            case NET_RADIO_PLAY_LIST_TYPE_LOCAL:
                targetRegionInfo = currentRegionInfo;
                break;
            case NET_RADIO_PLAY_LIST_TYPE_COUNTRY:
                targetRegionInfo = countryRegionInfo;
                break;
            case NET_RADIO_PLAY_LIST_TYPE_PROVINCE:
                targetRegionInfo = browseRegionInfo;
                break;
            default:
                targetRegionInfo = null;
        }
        if (targetRegionInfo != null && asyncCheckGotRegionListAndLocation()) {
            requestNetRadioInfoByRegionInfo(targetRegionInfo, NetRadioType);
        } else {
            mHandler.sendEmptyMessageDelayed(MSG_GET_REGION_TIME_OUT, 500);
//            loadingFailedLiveData.setValue(true);
        }
    }

    private final Runnable checkRegionListAndLocationTask = () -> {
        while (!stopCheckRegionListAndLocationTask && !asyncCheckGotRegionListAndLocation()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 异步检查RegionList和Location并获取
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private boolean asyncCheckGotRegionListAndLocation() {
        boolean result = true;
        if (mRegionList == null) {
            if (isNetworkIsAvailable()) {
                requestRegionsInfo();
            }
            result = false;
        }
        if (mCurrentRegion == null) {
            if (isNetworkIsAvailable()) {
                asyncRequestLocation();
            }
            result = false;
        }
        return result;
    }

    /**
     * 获取省份信息
     *
     * @param
     * @desc
     */
    private void requestRegionsInfo() {
        JL_Log.e(TAG, "requestRegionsInfo ------isRequestingRegionList:   " + isRequestingRegionList);
        if (isRequestingRegionList) return;
        isRequestingRegionList = true;
        loadingFailedLiveData.postValue(false);
        JL_HttpClient.getInstance().requestNetRadioRegions(new IActionListener<NetRadioResponse<NetRadioRegionInfo>>() {
            @Override
            public void onSuccess(NetRadioResponse<NetRadioRegionInfo> response) {
                if (response.getData() != null) {
                    List<NetRadioRegionInfo> list = response.getData();
//                    sourceNetRadioRegionInfosLiveData.postValue(list);
                    requestRegionSuccess(list);
                    mHandler.removeMessages(MSG_GET_REGION_TIME_OUT);
                }
                isRequestingRegionList = false;
            }

            @Override
            public void onError(int code, String message) {
                JL_Log.e(TAG, "onError:  code:  " + code + "   message:  " + message);
                isRequestingRegionList = false;
                loadingFailedLiveData.postValue(true);
                if (mThread != null) {
                    mThread.interrupt();
                    stopCheckRegionListAndLocationTask = true;
                    mThread = null;
                }
            }
        });
    }

    private void requestRegionSuccess(List<NetRadioRegionInfo> list) {
        JL_Log.i(TAG, "requestRegionSuccess: " + list);
        mRegionList = list;
        //获取本地电台
        JL_Log.i(TAG, "requestRegionSuccess: mCurrentRegion: " + mCurrentRegion);
        //国家地址信息
        if (countryRegionInfo == null) {
            countryRegionInfo = getRegionInfoByName(mRegionList, KEY_COUNTRY);
        }
        tryRefreshNetRadiosData();
    }

    /**
     * 异步请求当前位置方便
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void asyncRequestLocation() {
        if (!isNetworkIsAvailable() /*|| isRequestingLocation*/) return;
        if (!LocationHelper.getInstance().isStartLocation()) {
            LocationHelper.getInstance().registerOnLocationInfoListener(new OnLocationInfoListener() {
                @Override
                public void onLocationInfoChange(AMapLocation aMapLocation, BluetoothDevice device) {
                    if (device != null || ALLOW_SWITCH_FUN_DISCONNECT) {
                        JL_Log.e(TAG, "requestLocation:  mLocationListener.onLocationChanged22222");
                        onLocationChanged(LocationHelper.getInstance().getLocationInfo());
                    }
                    LocationHelper.getInstance().unregisterOnLocationInfoListener(this);
                }

                @Override
                public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {

                }

                @Override
                public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {

                }
            });
            LocationHelper.getInstance().startLocation();
        } else {
            JL_Log.e(TAG, "requestLocation:  mLocationListener.onLocationChanged111111");
            onLocationChanged(LocationHelper.getInstance().getLocationInfo());
        }
    }

    private void onLocationChanged(AMapLocation aMapLocation) {
        if (onceGetLocation && aMapLocation != null) {
            JL_Log.i(TAG, "aMapLocation : " + aMapLocation);
            if (!aMapLocation.getProvince().isEmpty()) {
                JL_Log.i(TAG, "aMapLocation :  getProvince: " + aMapLocation.getProvince());
                onceGetLocation = false;
                setCurrentLocation(aMapLocation.getProvince());
                tryRefreshNetRadiosData();
            }
        }
    }

    private void tryRefreshNetRadiosData() {
        if (asyncCheckGotRegionListAndLocation()) {
            initRegionInfosByLocation();
            getLocalNetRadios();
            getCountryNetRadios();
            getProvinceNetRadios();
        }
    }

    /**
     * 设置城市信息（当前城市，国家，省市）
     *
     * @param
     * @return
     * @description 描述一下方法的作用
     */
    private void initRegionInfosByLocation() {
        if (mCurrentRegion == null || mRegionList == null) return;
        NetRadioRegionInfo currentRegionInfo = getRegionInfoByName(mRegionList, mCurrentRegion);
        this.currentRegionInfo = currentRegionInfo;
        if (browseRegionLiveData.getValue() == null) {
            browseRegionInfo = currentRegionInfo;
            browseRegionLiveData.postValue(currentRegionInfo);
        }
        if (countryRegionInfo == null) {
            countryRegionInfo = getRegionInfoByName(mRegionList, KEY_COUNTRY);
            mRegionList.remove(countryRegionInfo);
        }
        netRadioRegionsListLiveData.postValue(mRegionList);
    }

    /**
     * 获取省市信息ByName
     */
    private NetRadioRegionInfo getRegionInfoByName(List<NetRadioRegionInfo> list, String currentRegion) {
        for (int i = 0; i < list.size(); i++) {
            NetRadioRegionInfo regionInfo = list.get(i);
            if (currentRegion.contains(regionInfo.getName())) {
                return regionInfo;
            }
        }
        return null;
    }

    /**
     * 设置当前省份
     *
     * @param location 当前地理位置的省市信息(广西壮族自治区)
     * @desc
     */
    private void setCurrentLocation(String location) {
        JL_Log.i(TAG, "setCurrentLocation:" + location);
        mCurrentRegion = location;
    }

    private void requestNetRadioInfoByRegionInfo(NetRadioRegionInfo regionInfo, int type) {
        if (regionInfo == null) return;
        requestNetRadioInfoByRegion(regionInfo.getId(), type);
    }

    /**
     * 根据省份信息的id获取RadioList
     */
    private void requestNetRadioInfoByRegion(String regionID, int type) {
        loadingFailedLiveData.postValue(false);
        JL_HttpClient.getInstance().requestNetRadioListByRegion(regionID, new IActionListener<NetRadioResponse<NetRadioListInfo>>() {
            @Override
            public void onSuccess(NetRadioResponse<NetRadioListInfo> response) {
                if (response.getData() != null) {
                    List<NetRadioListInfo> list = response.getData();
                    requestRadioInfoSuccess(list, type);
                }
            }

            @Override
            public void onError(int code, String message) {
                if (type == NET_RADIO_PLAY_LIST_TYPE_PROVINCE) {//切换的时候如果没网络，照样要切到空的
                    requestRadioInfoSuccess(null, NET_RADIO_PLAY_LIST_TYPE_PROVINCE);
                }
                loadingFailedLiveData.postValue(true);
                JL_Log.e(TAG, "onError:  code:  " + code + "   message:  " + message);
            }
        });
    }

    private void requestRadioInfoSuccess(List<NetRadioListInfo> infos, int type) {
        switch (type) {
            case NET_RADIO_PLAY_LIST_TYPE_LOCAL:
                localNetRadioLiveData.postValue(infos);
                break;
            case NET_RADIO_PLAY_LIST_TYPE_COUNTRY:
                countryNetRadioLiveData.postValue(infos);
                break;
            case NET_RADIO_PLAY_LIST_TYPE_PROVINCE:
                provinceNetRadioLiveData.postValue(infos);
                break;
        }
    }

    private boolean isNetworkIsAvailable() {
        boolean isAvailable;
        if (SConstant.CHANG_DIALOG_WAY) {
            isAvailable = NetworkStateHelper.getInstance().isNetworkIsAvailable();
        } else {
            isAvailable = NetworkHelper.getInstance().isNetworkIsAvailable();
        }
        return isAvailable;
    }

    public interface NetRadioDetailsCallback {
        void playRadioCallback(List<NetRadioListInfo> radioInfos, int position, int listType);

        void deleteCollectNetRadioCallback(NetRadioListInfo deleteNetRadio);

        void addCollectRadioCallback(NetRadioListInfo addNetRadio);
    }
}
