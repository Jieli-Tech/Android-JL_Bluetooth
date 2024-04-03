package com.jieli.btsmart.ui.search;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.search.LocationInfo;
import com.jieli.btsmart.data.model.search.MarkerInfo;
import com.jieli.btsmart.databinding.FragmentSearchDeviceBinding;
import com.jieli.btsmart.tool.product.DefaultResFactory;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.ui.widget.color_cardview.CardView;
import com.jieli.btsmart.util.ProductUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_dialog.Jl_Dialog;
import com.jieli.jl_http.bean.ProductModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.bumptech.glide.request.target.Target.SIZE_ORIGINAL;
import static com.jieli.jl_http.bean.ProductModel.MODEL_DOUBLE_HEADSET_LOCATION;
import static com.jieli.jl_http.bean.ProductModel.MODEL_LEFT_DEVICE_LOCATION;
import static com.jieli.jl_http.bean.ProductModel.MODEL_PRODUCT_LOGO;
import static com.jieli.jl_http.bean.ProductModel.MODEL_RIGHT_DEVICE_LOCATION;

/**
 * 设备查找
 */
public class SearchDeviceFragment extends DeviceControlFragment {
    private FragmentSearchDeviceBinding mBinding;
    private SearchDeviceViewModel mViewModel;
    private final ProductCacheManager mProductCacheManager = ProductCacheManager.getInstance();

    private CommonActivity mActivity;
    private Jl_Dialog mPlaySoundTipsDialog;
//    private PlaySoundCtrlDialog mPlaySoundCtrlDialog;

    private AMap mAMap;
    private LatLng mTargetLocation;
    private LatLng mPhoneLocation;
    private MarkerInfo mConnectedDevMarker;
    private boolean isNeedMoveCamera;

    private final List<MarkerInfo> mMarkers = new ArrayList<>();
    private final List<LatLng> taskList = new ArrayList<>();
    private boolean isTaskStart = false;
    private int failedCount = 0;
    private boolean isShowedPlaySoundTipsDialog; //记录是否显示过播放提示框

    private final static int MAP_DISPLAY_ZOOM_LEVEL = 17;
    private final static int DELAY_UPDATE = 6050;
    private final static int MSG_UPDATE_LOCATION_INFO = 964;
    private final static int MSG_UPDATE_TWS_DISCONNECT_UI = 965;
    private final Handler mHandler = new Handler(msg -> {
        switch (msg.what) {
            case MSG_UPDATE_LOCATION_INFO:
                AMapLocation location = (AMapLocation) msg.obj;
                if (location != null && location.getErrorCode() == 0) {
                    updateLocationMsgUI(location);
                }
                break;
            case MSG_UPDATE_TWS_DISCONNECT_UI:
                if (msg.obj instanceof BluetoothDevice) {
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    if (mViewModel.isTargetDevice(device)) {
                        updateDeviceLocationUI();
                    }
                }
                break;
        }
        return false;
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBinding = FragmentSearchDeviceBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String address = getBundle() == null ? null : getBundle().getString(SConstant.KEY_SEARCH_DEVICE_ADDR);
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            requireActivity().finish();
            return;
        }
        if (mActivity == null && requireActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        mViewModel = new ViewModelProvider(requireActivity()).get(SearchDeviceViewModel.class);
        mViewModel.setTargetDevAddress(address);
        HistoryBluetoothDevice history = mViewModel.getHistoryBtRecordByAddress();
        if (null == history) {
            requireActivity().finish();
            return;
        }
        mBinding.mapSearchDevice.onCreate(savedInstanceState);
        updateTopBar(UIHelper.getCacheDeviceName(history));
        initView();
        addObserver();
    }

    @Override
    public void onStart() {
        super.onStart();
        isNeedMoveCamera = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mBinding.mapSearchDevice.onResume();
        mViewModel.startLocation();
        mViewModel.syncSearchDeviceStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        mBinding.mapSearchDevice.onPause();
        mViewModel.stopLocation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        dismissPlaySoundCtrlDialog();
        mBinding.mapSearchDevice.onDestroy();
        dismissPlaySoundTipsDialog();
        mHandler.removeCallbacksAndMessages(null);
        mMarkers.clear();
        taskList.clear();
        isTaskStart = false;
        isShowedPlaySoundTipsDialog = false;
        mPhoneLocation = null;
        mActivity = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mBinding.mapSearchDevice.onSaveInstanceState(outState);
    }

    private void initMap() {
        if (mAMap == null) {
            mAMap = mBinding.mapSearchDevice.getMap();
        }
        mAMap.getUiSettings().setMyLocationButtonEnabled(false);//设置默认定位按钮是否显示，非必需设置。
        mAMap.getUiSettings().setScaleControlsEnabled(true);
        mAMap.getUiSettings().setCompassEnabled(false);
        mAMap.getUiSettings().setZoomControlsEnabled(false);
        mAMap.setMyLocationEnabled(false);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        mAMap.setOnMarkerClickListener(mMarkerClickListener);
    }


    private void initView() {
        mBinding.tvSearchDevicePlay.setOnClickListener(v -> {
            if (!isConnectedDevice()) return;
            if (!isUsingDevice()) {
                ToastUtil.showToastShort(getString(R.string.device_not_using));
                return;
            }
            mViewModel.playSound(Constants.RING_WAY_ALL);
        });
        mBinding.tvSearchDeviceStop.setOnClickListener(v -> {
            if (!isConnectedDevice()) return;
            if (!isUsingDevice()) {
                ToastUtil.showToastShort(getString(R.string.device_not_using));
                return;
            }
            mViewModel.stopSound();
        });
        mBinding.ivSearchDeviceLocation.setOnClickListener(v -> {
            if (mPhoneLocation != null) {
                mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mPhoneLocation, MAP_DISPLAY_ZOOM_LEVEL));
            } else {
                if (!isNeedMoveCamera) isNeedMoveCamera = true;
            }
        });
        mBinding.cvSearchDeviceHeadsetLeftCtrl.setOnClickListener(v -> playSoundByWay(mViewModel.getTargetDevAddress(), Constants.RING_WAY_LEFT));
        mBinding.cvSearchDeviceHeadsetRightCtrl.setOnClickListener(v -> playSoundByWay(mViewModel.getTargetDevAddress(), Constants.RING_WAY_RIGHT));
        initMap();
        updateDeviceSearchControlUI();
    }

    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> {
            if (!mViewModel.isTargetDevice(connection.getDevice())) return;
            if (connection.getStatus() != StateCode.CONNECTION_CONNECTING) {
                if (connection.getStatus() == StateCode.CONNECTION_DISCONNECT) {
                    mConnectedDevMarker = null;
                /*if (isShowPlaySoundCtrDialog()) {
                    mPlaySoundCtrlDialog.onDeviceDisconnected(device);
                }*/
                }
                updateDeviceLocationUI();
            }
        });
        mViewModel.historyRemoveMLD.observe(getViewLifecycleOwner(), history -> {
            if (mViewModel.isTargetDevice(history)) {
                requireActivity().onBackPressed();
                return;
            }
            updateDeviceLocationUI();
        });
        mViewModel.twsStatusMLD.observe(getViewLifecycleOwner(), twsStatus -> {
            mHandler.removeMessages(MSG_UPDATE_TWS_DISCONNECT_UI);
            JL_Log.i(TAG, "[twsStatusMLD] >> twsStatus : " + twsStatus);
            if (twsStatus.getR()) {
                updateDeviceLocationUI();
            } else {
                //考虑到更新位置需要3秒以上，延时一段时间再更新
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_UPDATE_TWS_DISCONNECT_UI, twsStatus.getT()), DELAY_UPDATE);
            }
        });
        mViewModel.playSoundStatusMLD.observe(getViewLifecycleOwner(), playSound -> updateDeviceSearchControlUI());
        mViewModel.locationInfoMLD.observe(getViewLifecycleOwner(), locationInfo -> {
            AMapLocation location = locationInfo.getR();
            if (null == location) return;
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            if (isNeedMoveCamera) {
                isNeedMoveCamera = false;
                //首次定位,选择移动到地图中心点并修改级别到17级
                mAMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_DISPLAY_ZOOM_LEVEL));
                updateDeviceLocationUI(latLng);
                findTargetLocation();
            }
            updateMyLocationMark(latLng);
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_LOCATION_INFO, location));
            if (mViewModel.isTargetDevice(locationInfo.getT()) && mHandler.hasMessages(MSG_UPDATE_TWS_DISCONNECT_UI)) {
                mHandler.removeMessages(MSG_UPDATE_TWS_DISCONNECT_UI);
                mHandler.obtainMessage(MSG_UPDATE_TWS_DISCONNECT_UI, locationInfo.getT()).sendToTarget();
            }
        });
        mViewModel.regeocodeResultMLD.observe(getViewLifecycleOwner(), regeocodeResult -> {
            RegeocodeAddress addressResponse = regeocodeResult != null ? regeocodeResult.getRegeocodeAddress() : null;
            if (addressResponse != null && addressResponse.getFormatAddress() != null) { //获取地址
                String addressName = addressResponse.getFormatAddress();
                LatLonPoint latLon = regeocodeResult.getRegeocodeQuery() != null ? regeocodeResult.getRegeocodeQuery().getPoint() : null;
                if (latLon != null) {
                    MarkerInfo marker = findMark(latLon.getLatitude(), latLon.getLongitude());
                    if (marker != null) {
                        JL_Log.d(TAG, "onRegeocodeSearched >> update location : " + latLon + ", addressName : " + addressName);
                        marker.setAddressName(addressName);
                    }
                    removeTask(new LatLng(latLon.getLatitude(), latLon.getLongitude()));
                } else {
                    removeTask(null);
                }
            }
        });
    }

    private void updateTopBar(String title) {
        if (!isAdded() || isDetached() || mActivity == null) return;
        mActivity.updateTopBar(title, R.drawable.ic_back_black, v -> requireActivity().onBackPressed(), 0, null);
    }

    private void updateDeviceLocationUI() {
        updateDeviceLocationUI(null);
        updateDeviceSearchControlUI();
    }

    private void updateDeviceLocationUI(LatLng myPhoneLatLng) {
        if (!isAdded() || isDetached()) return;
        mAMap.clear(true);
        mMarkers.clear();
        mPhoneLocation = null;
        if (myPhoneLatLng != null) {
            updateMyLocationMark(myPhoneLatLng);
        }
        HistoryBluetoothDevice history = mViewModel.getHistoryBtRecordByAddress();
        if (history == null) {
            JL_Log.w(TAG, "updateDeviceLocationUI >> no cache target device.");
            return;
        }
        List<LocationInfo> locationInfos = UIHelper.getLocationInfosByHistoryDevice(history);
        boolean isConnected = isConnectedDevice();
        for (int i = locationInfos.size() - 1; i >= 0; i--) {
            LocationInfo info = locationInfos.get(i);
            JL_Log.v(TAG, "updateDeviceLocationUI :: " + info + ", i = " + i);
            LatLng latLng = new LatLng(info.getLatitude(), info.getLongitude());
            if (i == 0) {
                long time = isConnected ? 0 : info.getUpdateTime();
                setTargetLocation(latLng);
                addDeviceMark(latLng, history.getChipType(), history.getAdvVersion(), history.getUid(), history.getPid(), history.getVid(), history.getName(), getLastUpdateTime(time), info.getDeviceFlag());
                if (isConnected) mConnectedDevMarker = findMark(latLng.latitude, latLng.longitude);
            } else {
                addDeviceMark(latLng, history.getChipType(), history.getAdvVersion(), history.getUid(), history.getPid(), history.getVid(), history.getName(), getLastUpdateTime(info.getUpdateTime()), info.getDeviceFlag());
            }
        }
    }

    private void updateLocationMsgUI(AMapLocation location) {
        if (!isAdded() || isDetached() || location == null) return;
        MarkerInfo marker = null;
        if (mTargetLocation != null) {
            marker = findMark(mTargetLocation.latitude, mTargetLocation.longitude);
        }
        String locationStr = null;
        if (marker != null) {
            locationStr = marker.getAddressName();
        }
        if (TextUtils.isEmpty(locationStr)) {
            locationStr = getLocationMsg(location);
        }
        if (locationStr == null || !locationStr.equals(mBinding.tvSearchDeviceLocation.getText().toString().trim())) {
            mBinding.tvSearchDeviceLocation.setText(locationStr);
        }
        String timeStr = UIHelper.descriptiveData(MainApplication.getApplication(), location.getTime());
        mBinding.tvSearchDeviceLocationTime.setText(timeStr);
        /*if (isShowPlaySoundCtrDialog()) {
            mPlaySoundCtrlDialog.updateDeviceGpsUI(locationStr, timeStr);
        }*/
        if (isConnectedDevice()) {
            mBinding.tvSearchDeviceDistance.setVisibility(View.INVISIBLE);
        } else {
            mBinding.tvSearchDeviceDistance.setVisibility(View.VISIBLE);
            String distance = calcDistance();
            if (!TextUtils.isEmpty(distance)) {
                mBinding.tvSearchDeviceDistance.setText(calcDistance());
            }
        }
    }

    private void updateSoundControlUI(boolean isConnected, boolean isPlaying) {
        if (!isAdded() || isDetached()) return;
        mBinding.tvSearchDevicePlay.setEnabled(isConnected);
        mBinding.tvSearchDevicePlay.setBackgroundResource(isConnected ? R.drawable.bg_search_device_play_selector
                : R.drawable.bg_search_device_gray_shape);
        mBinding.tvSearchDevicePlay.setTextColor(getResources().getColor(isConnected ? R.color.white_ffffff : R.color.black_646464));
        mBinding.tvSearchDeviceStop.setEnabled(isConnected);
        mBinding.tvSearchDeviceStop.setBackgroundResource(isConnected ? R.drawable.bg_search_device_stop_selector
                : R.drawable.bg_search_device_btn_shape);
        mBinding.tvSearchDeviceStop.setTextColor(getResources().getColor(isConnected ? R.color.color_main : R.color.gray_8D8D8D));
    }

    private boolean isConnectedDevice() {
        return mViewModel.isConnectedDevice(mViewModel.getTargetDevAddress());
    }

    private boolean isUsingDevice() {
        return mViewModel.isUsingDevice(mViewModel.getTargetDevAddress());
    }

    private void setTargetLocation(LatLng targetLocation) {
        mTargetLocation = targetLocation;
    }

    private String getLocationMsg(AMapLocation location) {
        if (null == location) return null;
        String message = location.getProvince(); //省信息
        String city = location.getCity();
        if (TextUtils.isEmpty(message)) { //城市信息
            message = city;
        } else {
            message += city;
        }
        String district = location.getDistrict();
        if (TextUtils.isEmpty(message)) { //城区信息
            message = district;
        } else {
            message += district;
        }
        String street = location.getStreet();
        if (TextUtils.isEmpty(message)) { //街道信息
            message = street;
        } else {
            message += street;
        }
        String streetNum = location.getStreetNum();
        if (TextUtils.isEmpty(message)) { //街道门牌号信息
            message = streetNum;
        } else {
            message += streetNum;
        }
        return message;
    }

    private String getLastUpdateTime(long time) {
        if (time == 0) {
            return getString(R.string.connected_device_time_tips);
        } else {
            return getString(R.string.last_time_tips, UIHelper.descriptiveData(MainApplication.getApplication(), time));
        }
    }

    private String calcDistance() {
        String distance = "";
        if (mPhoneLocation != null && mTargetLocation != null) {
            float value = AMapUtils.calculateLineDistance(mPhoneLocation, mTargetLocation);
            if (value > 0) {
                if (value >= 1000) {
                    distance = getString(R.string.distance_km, String.format(Locale.getDefault(), "%.1f", (value / 1000.0f)));
                } else {
                    distance = getString(R.string.distance_m, String.format(Locale.getDefault(), "%.1f", value));
                }
            }
        }
        JL_Log.d(TAG, "calcDistance : " + distance);
        return distance;
    }

    private void findTargetLocation() {
        if (mTargetLocation != null) return;
        HistoryBluetoothDevice history = mViewModel.getHistoryBtRecordByAddress();
        if (history == null) return;
        List<LocationInfo> locationInfos = UIHelper.getLocationInfosByHistoryDevice(history);
        if (locationInfos.size() > 0) {
            LatLng latLng = new LatLng(locationInfos.get(0).getLatitude(), locationInfos.get(0).getLongitude());
            setTargetLocation(latLng);
        }
    }

    private MarkerInfo findMark(double latitude, double longitude) {
        MarkerInfo marker = null;
        if (mMarkers.size() > 0) {
            for (MarkerInfo info : mMarkers) {
                Marker m = info.getMarker();
                if (m == null) continue;
                LatLng latLng = m.getPosition();
                if (latLng != null && latLng.latitude == latitude
                        && latLng.longitude == longitude) {
                    marker = info;
                    break;
                }
            }
        }
        return marker;
    }

    private final RequestOptions options = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .override(SIZE_ORIGINAL);

    private void addDeviceMark(LatLng latLng, int sdkType, int advVersion, int uid, int pid, int vid, String title, String desc, int deviceFlag) {
        if (null == latLng) return;
        String iconUrl = mProductCacheManager.getProductUrl(uid, pid, vid, MODEL_LEFT_DEVICE_LOCATION.getValue());
        int failRes = DefaultResFactory.createBySdkType(sdkType, advVersion).getOnMapIcon();
        if (UIHelper.isHeadsetType(sdkType)) {
            if (advVersion != SConstant.ADV_INFO_VERSION_NECK_HEADSET) {//挂脖耳机不区分左右耳
                if (deviceFlag == LocationInfo.DEVICE_FLAG_LEFT) {//左侧设备
                    iconUrl = mProductCacheManager.getProductUrl(uid, pid, vid, MODEL_LEFT_DEVICE_LOCATION.getValue());
                    failRes = R.drawable.ic_headset_left_location_white;
                } else if (deviceFlag == LocationInfo.DEVICE_FLAG_RIGHT) { //右侧设备
                    iconUrl = mProductCacheManager.getProductUrl(uid, pid, vid, MODEL_RIGHT_DEVICE_LOCATION.getValue());
                    failRes = R.drawable.ic_headset_right_location_white;
                } else {
                    iconUrl = mProductCacheManager.getProductUrl(uid, pid, vid, MODEL_DOUBLE_HEADSET_LOCATION.getValue());
                    failRes = R.drawable.ic_headset_location_white;
                }
            }
        }
        JL_Log.d(TAG, "addDeviceMark: iconUrl:" + iconUrl);

//        int iconId = DefaultResFactory.createBySdkType(sdkType, advVersion).getOnMapIcon();
////        UIHelper.isHeadsetType(sdkType) ? R.drawable.ic_headset_location_white : R.drawable.ic_soundbox_location_white;
//
//        if (UIHelper.isHeadsetType(sdkType)) {
//            if (advVersion != SConstant.ADV_INFO_VERSION_NECK_HEADSET) {//挂脖耳机不区分左右耳
//                if (deviceFlag == LocationInfo.DEVICE_FLAG_LEFT) {//左侧设备
//                    iconId = R.drawable.ic_headset_left_location_white;
//                } else if (deviceFlag == LocationInfo.DEVICE_FLAG_RIGHT) { //右侧设备
//                    iconId = R.drawable.ic_headset_right_location_white;
//                }
//            }
//        }
        int finalFailRes = failRes;
        Glide.with(MainApplication.getApplication())
                .asBitmap()
                .load(iconUrl)
                .apply(options)
                .error(failRes)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        int width = resource.getWidth();
                        int height = resource.getHeight();
                        JL_Log.d(TAG, "onResourceReady url: " + resource + " width: " + width + " height: " + height);
                        float scale = (float) 159 / width;
                        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(scaleBitmap(resource, scale));
                        updateMarkIcon(latLng, bitmapDescriptor, title, desc);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        JL_Log.d(TAG, "onLoadFailed url: " + errorDrawable);
                        BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(finalFailRes);
                        updateMarkIcon(latLng, bitmapDescriptor, title, desc);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
//                        Log.d("ZHM", "onLoadCleared url: " + placeholder);
                    }
                });
    }

    /**
     * Bitmap放大的方法
     */
    private static Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale); //长和宽放大缩小的比例
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }


    private void updateMarkIcon(LatLng latLng, BitmapDescriptor icon, String title, String desc) {
        if (isDetached() || !isAdded()) return;
        Circle circle = mAMap.addCircle(new CircleOptions().center(latLng).radius(SearchDeviceViewModel.DEFAULT_RADIUS)
                .fillColor(getResources().getColor(R.color.blue_406ac0_half))
                .strokeColor(getResources().getColor(R.color.text_transparent)));
        Marker marker = mAMap.addMarker(new MarkerOptions().position(latLng).anchor(0.5f, 0.5f)
                .icon(icon));
        if (!TextUtils.isEmpty(title)) {
            marker.setTitle(title);
//            marker.showInfoWindow();
            addTask(latLng);
        }
        if (!TextUtils.isEmpty(desc)) {
            marker.setSnippet(desc);
        }
        mMarkers.add(new MarkerInfo().setMarker(marker).setCircle(circle).setType(MarkerInfo.MARKER_TYPE_DEVICE));
    }

    private void updateMyLocationMark(LatLng latLng) {
        if (null == latLng || !isAdded() || isDetached()) return;
        if (mPhoneLocation == null) {
            Circle circle = mAMap.addCircle(new CircleOptions().center(latLng).radius(SearchDeviceViewModel.DEFAULT_RADIUS)
                    .fillColor(getResources().getColor(R.color.blue_406ac0_half))
                    .strokeColor(getResources().getColor(R.color.text_transparent)).zIndex(0.0f));
            Marker marker = mAMap.addMarker(new MarkerOptions().position(latLng).anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(),
                            R.drawable.ic_phone_location_purple))).autoOverturnInfoWindow(true).zIndex(0.0f));
            marker.setTitle(getString(R.string.my_location));
            marker.setSnippet(mBinding.tvSearchDeviceLocation.getText().toString());
            MarkerInfo markerInfo = new MarkerInfo().setMarker(marker).setCircle(circle).setType(MarkerInfo.MARKER_TYPE_PHONE);
            mMarkers.add(markerInfo);
            mPhoneLocation = latLng;
            addTask(latLng);
            JL_Log.d(TAG, "updateMyLocationMark >> add phone location : " + latLng);
        } else {
            MarkerInfo markerInfo = findMark(mPhoneLocation.latitude, mPhoneLocation.longitude);
            if (markerInfo != null && markerInfo.getType() == MarkerInfo.MARKER_TYPE_PHONE && !TextUtils.isEmpty(markerInfo.getAddressName())) {
                if (!markerInfo.getAddressName().equals(markerInfo.getMarker().getSnippet())) {
                    markerInfo.getMarker().setSnippet(markerInfo.getAddressName());
                }
                if (latLng.latitude != mPhoneLocation.latitude && latLng.longitude != mPhoneLocation.longitude) {
                    JL_Log.d(TAG, "updateMyLocationMark >> update phone location : " + latLng);
                    markerInfo.getMarker().setPosition(latLng);
                    markerInfo.getCircle().setCenter(latLng);
                    if (isConnectedDevice()) {
                        HistoryBluetoothDevice history = mViewModel.getHistoryBtRecordByAddress();
                        if (null != history && mConnectedDevMarker != null
                                && history.getLeftDevLatitude() != mConnectedDevMarker.getMarker().getPosition().latitude
                                && history.getLeftDevLongitude() != mConnectedDevMarker.getMarker().getPosition().longitude) {
                            LatLng devLat = new LatLng(history.getLeftDevLatitude(), history.getLeftDevLongitude());
                            JL_Log.d(TAG, "updateMyLocationMark >> update connect device location : " + devLat);
                            mConnectedDevMarker.getMarker().setPosition(devLat);
                            mConnectedDevMarker.getCircle().setCenter(devLat);
                        }
                    }
                    mPhoneLocation = latLng;
                    addTask(latLng);
                }
            }
        }
    }

    private void showPlaySoundTipsDialog(final int playWay) {
        if (!isAdded() || isDetached()) return;
        if (mPlaySoundTipsDialog == null) {
            mPlaySoundTipsDialog = Jl_Dialog.builder()
                    .title(getString(R.string.tips))
                    .content(getString(R.string.search_device_tips))
                    .left(getString(R.string.cancel))
                    .leftColor(getResources().getColor(R.color.gray_text_989898))
                    .leftClickListener((v, dialogFragment) -> {
                        dismissPlaySoundTipsDialog();
                        isShowedPlaySoundTipsDialog = false;
                    }).right(getString(R.string.play_sound))
                    .rightColor(getResources().getColor(R.color.blue_448eff))
                    .rightClickListener((v, dialogFragment) -> {
                        dismissPlaySoundTipsDialog();
                        isShowedPlaySoundTipsDialog = true;
                        mViewModel.playSound(playWay);
                    })
                    .build();
        }
        if (!mPlaySoundTipsDialog.isShow()) {
            mPlaySoundTipsDialog.show(getChildFragmentManager(), "play_sound_tips");
        }
    }

    private void dismissPlaySoundTipsDialog() {
        if (mPlaySoundTipsDialog != null) {
            if (mPlaySoundTipsDialog.isShow()) {
                mPlaySoundTipsDialog.dismiss();
            }
            mPlaySoundTipsDialog = null;
        }
    }

    private boolean isPlayingSound(int way) {
        if (!isConnectedDevice()) return false;
        return mViewModel.isPlayingSound(mViewModel.getTargetDevAddress()) && (mViewModel.getPlayWay() == Constants.RING_WAY_ALL || mViewModel.getPlayWay() == way);
    }

    private void updateDeviceStatusUI(boolean isDevConnected, boolean isPlayingSound, CardView cardView, TextView tvConnection, TextView tvPlaySound) {
        if (!isAdded() || isDetached()) return;
        if (cardView == null || tvConnection == null || tvPlaySound == null) return;
        if (isDevConnected) {
            cardView.setClickable(true);
            tvConnection.setText(getString(R.string.device_status_connected));
            tvPlaySound.setTextColor(getResources().getColor(R.color.color_main));
            cardView.setCardBackgroundColor(getResources().getColor(isPlayingSound ? R.color.color_main : R.color.white_ffffff));
            tvConnection.setTextColor(getResources().getColor(isPlayingSound ? R.color.white_ffffff : R.color.black_242424));
            tvPlaySound.setCompoundDrawablesRelativeWithIntrinsicBounds(isPlayingSound ? R.drawable.ic_playing_sound_white : 0, 0, 0, 0);
            tvPlaySound.setText(isPlayingSound ? "" : getString(R.string.play_sound));
        } else {
            cardView.setCardBackgroundColor(getResources().getColor(R.color.white_ffffff));
            cardView.setClickable(false);
            tvConnection.setText(getString(R.string.device_status_unconnected));
            tvConnection.setTextColor(getResources().getColor(R.color.gray_959595));
            tvPlaySound.setText(getString(R.string.play_sound));
            tvPlaySound.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            tvPlaySound.setTextColor(getResources().getColor(R.color.gray_959595));
        }
    }

    private void updateDeviceStatus(boolean leftDevConnected, boolean rightDevConnect) {
        updateDeviceStatusUI(leftDevConnected, isPlayingSound(Constants.RING_WAY_LEFT), mBinding.cvSearchDeviceHeadsetLeftCtrl,
                mBinding.tvSearchDeviceHeadsetLeftConnection, mBinding.tvSearchDeviceHeadsetLeftPlay);
        updateDeviceStatusUI(rightDevConnect, isPlayingSound(Constants.RING_WAY_RIGHT), mBinding.cvSearchDeviceHeadsetRightCtrl,
                mBinding.tvSearchDeviceHeadsetRightConnection, mBinding.tvSearchDeviceHeadsetRightPlay);
    }

    private void updateImageView(ImageView imageView, boolean isGif, String url, int failResId) {
//        Log.d(TAG, "updateImageView: url : "+url+ " failResId : "+failResId );
        if (getContext() != null && isAdded() && !isDetached() && imageView != null) {
            if (failResId <= 0) {
                failResId = R.drawable.ic_default_product_design;
            }
            RequestOptions options = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .override(SIZE_ORIGINAL);
//                    .fallback(failResId);
            if (isGif) {
                Glide.with(MainApplication.getApplication())
                        .asGif()
                        .apply(options)
                        .load(url)
                        .error(failResId)
                        .into(imageView);
            } else {
                Glide.with(MainApplication.getApplication())
                        .asBitmap()
                        .apply(options)
                        .load(url)
                        .error(failResId)
                        .into(imageView);
            }
        }
    }

    private void updateDeviceUI(ImageView imageView, HistoryBluetoothDevice history, String scene) {
        if (imageView == null || history == null || scene == null || !isAdded() || isDetached())
            return;
        String imgUrl = ProductUtil.findCacheDesign(getContext(), history.getVid(), history.getUid(), history.getPid(), scene);
        boolean isGif = ProductUtil.isGifFile(imgUrl);
        int failResId = DefaultResFactory.createBySdkType(history.getChipType(), history.getAdvVersion()).getLogoImg();
        if (UIHelper.isHeadsetType(history.getChipType()) && !scene.equals(MODEL_PRODUCT_LOGO.getValue())) {
            if (scene.equals(ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue()) || scene.equals(ProductModel.MODEL_DEVICE_LEFT_CONNECTED.getValue())) {
                failResId = DefaultResFactory.createBySdkType(history.getChipType(), history.getAdvVersion()).getLeftImg();
            } else if (scene.equals(ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue()) || scene.equals(ProductModel.MODEL_DEVICE_RIGHT_CONNECTED.getValue())) {
                failResId = DefaultResFactory.createBySdkType(history.getChipType(), history.getAdvVersion()).getRightImg();
            }
        }
        updateImageView(imageView, isGif, imgUrl, failResId);
    }

    private void updateHeadsetDeviceUI(HistoryBluetoothDevice history) {
        if (!isAdded() || isDetached()) return;
        if (null == history) {
            updateDeviceStatus(false, false);
            return;
        }
        updateDeviceUI(mBinding.ivSearchDeviceHeadsetLeftDev, history, ProductModel.MODEL_DEVICE_LEFT_IDLE.getValue());
        updateDeviceUI(mBinding.ivSearchDeviceHeadsetRightDev, history, ProductModel.MODEL_DEVICE_RIGHT_IDLE.getValue());
        ADVInfoResponse advInfo = mViewModel.getADVInfo(BluetoothUtil.getRemoteDevice(history.getAddress()));
        if (advInfo != null) {
            updateDeviceStatus(advInfo.getLeftDeviceQuantity() > 0, advInfo.getRightDeviceQuantity() > 0);
        } else {
            updateDeviceStatus(isConnectedDevice(), false);
        }
    }

    private void updateDeviceSearchControlUI() {
        if (!isAdded() || isDetached()) return;
        HistoryBluetoothDevice history = mViewModel.getHistoryBtRecordByAddress();
        boolean isHeadsetType = history != null && UIHelper.isHeadsetType(history.getChipType());
        isHeadsetType = isHeadsetType && history.getAdvVersion() != SConstant.ADV_INFO_VERSION_NECK_HEADSET;//广播包类型不为挂脖
        mBinding.llSearchDeviceControl.setVisibility(isHeadsetType ? View.GONE : View.VISIBLE);
        mBinding.cvSearchDeviceHeadsetLeftCtrl.setVisibility(isHeadsetType ? View.VISIBLE : View.GONE);
        mBinding.cvSearchDeviceHeadsetRightCtrl.setVisibility(isHeadsetType ? View.VISIBLE : View.GONE);
        if (isHeadsetType) {
            updateHeadsetDeviceUI(history);
        } else {
            updateSoundControlUI(isConnectedDevice(), mViewModel.isPlayingSound(mViewModel.getTargetDevAddress()));
        }
    }

    private void playSoundByWay(String address, int way) {
        if (!isConnectedDevice()) return;
        if (!isUsingDevice()) {
            ToastUtil.showToastShort(getString(R.string.device_not_using));
            return;
        }
        boolean isTwsConnected = mViewModel.isTwsConnected(address);
        boolean isPlayingSound = mViewModel.isPlayingSound(address);
        JL_Log.d(TAG, "[playSoundByWay] >>> way = " + way + ", PlayWay = " + mViewModel.getPlayWay()
                + ", isTwsConnected = " + isTwsConnected + ", isPlayingSound = " + isPlayingSound);
        if (isPlayingSound) { //铃声在响
            if (mViewModel.getPlayWay() == way) {  //相同播放方式，停止播放
                mViewModel.stopSound();
            } else if (isTwsConnected) { //不同播放方式且TWS连接
                int playWay = way;
                //播放方式时其他侧边播放类型，因为已有一侧播放，所以认为全部播放
                if (mViewModel.getPlayWay() != Constants.RING_WAY_ALL) {
                    playWay = Constants.RING_WAY_ALL;
                } else {
//                            mPresenter.stopSound();
                    if (way == Constants.RING_WAY_LEFT) {
                        playWay = Constants.RING_WAY_RIGHT;
                    } else if (way == Constants.RING_WAY_RIGHT) {
                        playWay = Constants.RING_WAY_LEFT;
                    }
                }
                playHeadsetSound(playWay);
            } else {//不同播放方式且不是TWS连接，实际上不可能出现
                mViewModel.stopSound();
            }
        } else { //铃声不在播放状态
            playHeadsetSound(way);
        }
    }

    private void playHeadsetSound(int playWay) {
        if (!isShowedPlaySoundTipsDialog) {
            showPlaySoundTipsDialog(playWay);
        } else {
            mViewModel.playSound(playWay);
        }
    }

    private void addTask(LatLng latLng) {
        if (null == latLng) return;
        boolean isHasTask = false;
        for (LatLng location : taskList) {
            if (location.equals(latLng)) {
                isHasTask = true;
                break;
            }
        }
        if (!isHasTask) {
            taskList.add(latLng);
        }
        if (taskList.size() == 1 && !isTaskStart) {
            isTaskStart = true;
            mViewModel.getFromLocation(latLng.latitude, latLng.longitude);
        }
    }

    private void removeTask(LatLng latLng) {
        boolean isRemoveOk = latLng != null && taskList.remove(latLng);
        if (!isRemoveOk) {
            failedCount++;
            if (failedCount >= 3) {
                taskList.remove(0);
            } else {
                return;
            }
        }
        failedCount = 0;
        if (taskList.isEmpty()) return;
        LatLng location = taskList.get(0);
        mViewModel.getFromLocation(location.latitude, location.longitude);
    }

    private final AMap.OnMarkerClickListener mMarkerClickListener = marker -> {
        if (marker.isInfoWindowShown()) {
            marker.hideInfoWindow();
        } else {
            marker.showInfoWindow();
        }
        return true;
    };

}
