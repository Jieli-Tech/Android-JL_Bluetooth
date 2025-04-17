package com.jieli.btsmart.ui.search;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.SearchDeviceListAdapter;
import com.jieli.btsmart.databinding.FragmentSearchDeviceListBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/8/18 14:17
 * @desc :查找历史设备列表Fragment
 */
public class SearchDeviceListFragment extends DeviceControlFragment {
    private FragmentSearchDeviceListBinding mBinding;
    private SearchDeviceViewModel mViewModel;
    private SearchDeviceListAdapter mAdapter;

    /**
     * 查询位置信息的任务列表
     */
    private final List<LatLonPoint> taskList = new ArrayList<>();
    private boolean isTaskStart = false;
    private int failedCount = 0;

    public static SearchDeviceListFragment newInstance() {
        return new SearchDeviceListFragment();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentSearchDeviceListBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(SearchDeviceViewModel.class);
        initUI();
        addObserver();
        refreshHistoryDeviceList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        taskList.clear();
        isTaskStart = false;
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            final CommonActivity activity = (CommonActivity) requireActivity();
            activity.updateTopBar(getString(R.string.multi_media_search_device), R.drawable.ic_back_black,
                    v -> requireActivity().onBackPressed(), 0, null);
        }
        mAdapter = new SearchDeviceListAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            LocationDeviceInfo deviceInfo = mAdapter.getItem(position);
            String address = deviceInfo.getHistoryBluetoothDevice().getAddress();
            if (address != null) {
                taskList.clear();
                isTaskStart = false;
                Bundle bundle = new Bundle();
                bundle.putString(SConstant.KEY_SEARCH_DEVICE_ADDR, address);
                CommonActivity.startCommonActivity(requireActivity(), SearchDeviceFragment.class.getCanonicalName(), bundle);
            }
        });
        mBinding.rvSearchDeviceList.setAdapter(mAdapter);
        mBinding.rvSearchDeviceList.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void addObserver() {
        mViewModel.deviceConnectionMLD.observe(getViewLifecycleOwner(), connection -> mAdapter.notifyDataSetChanged());
        mViewModel.switchDeviceMLD.observe(getViewLifecycleOwner(), device -> mAdapter.notifyDataSetChanged());
        mViewModel.historyRemoveMLD.observe(getViewLifecycleOwner(), history -> refreshHistoryDeviceList());
        mViewModel.regeocodeResultMLD.observe(getViewLifecycleOwner(), this::synLocationDeviceInfoByRegeocodeResult);
        mViewModel.locationInfoMLD.observe(getViewLifecycleOwner(), data -> {
            AMapLocation location = data.getR();
            if (location == null || isDetached() || !isAdded() || mAdapter.getData().isEmpty())
                return;
            int position = 0;
            for (LocationDeviceInfo deviceInfo : mAdapter.getData()) {
                boolean isConnectedDevice = mViewModel.isConnectedDevice(deviceInfo.getHistoryBluetoothDevice().getAddress());
                String srcLocationStr = deviceInfo.locationString;
                String targetLocationStr = getLocationMsg(location);
                boolean isChangedLocationStr = !TextUtils.equals(srcLocationStr, targetLocationStr);
                boolean isNeedUpdateLocationStr = isConnectedDevice && isChangedLocationStr;
//            JL_Log.i(TAG, "locationString :  before update" + deviceInfo.getHistoryBluetoothDevice().getName()+ "   isConnectedDevice:  " + isConnectedDevice) ;
                if (isNeedUpdateLocationStr && isAdded() && !isDetached() && mAdapter != null) {
//                JL_Log.i(TAG, "locationString :  update" + deviceInfo.getHistoryBluetoothDevice().getName());
                    deviceInfo.locationString = targetLocationStr;
                    mAdapter.notifyItemChanged(position);
                }
                position++;
            }
        });
    }

    private void refreshHistoryDeviceList() {
        List<LocationDeviceInfo> list = mViewModel.getHistoryBtDeviceList();
        mAdapter.setList(list);
        taskList.clear();
        isTaskStart = false;
        for (LocationDeviceInfo info : list) {
            if (TextUtils.isEmpty(info.locationString) && info.location != null && info.location.getLatitude() != 0 && info.location.getLongitude() != 0) {
                addTask(info.location);
            }
        }
        if (list.isEmpty()) {
            Jl_Dialog.builder()
                    .right(getString(R.string.confirm))
                    .title(getString(R.string.tips))
                    .content(getString(R.string.unconnected_device_tips))
                    .rightColor(getResources().getColor(R.color.color_main))
                    .rightClickListener((v, dialogFragment) -> {
                        dialogFragment.dismiss();
                    })
                    .build()
                    .show(getChildFragmentManager(), "delete_alarm");
        }
    }

    private void addTask(LatLonPoint info) {
        if (null == info) return;
        if (!taskList.contains(info)) {
            taskList.add(info);
        }
        if (taskList.size() == 1 && !isTaskStart) {
            isTaskStart = true;
            LatLonPoint location = taskList.get(0);
            mViewModel.getFromLocation(location.getLatitude(), location.getLongitude());
        }
    }

    private void synLocationDeviceInfoByRegeocodeResult(RegeocodeResult regeocodeResult) {
        if (null == regeocodeResult) return;
        JL_Log.d(TAG, "[synLocationDeviceInfoByRegeocodeResult] >>>");
        LatLonPoint destLocationPoint = regeocodeResult.getRegeocodeQuery() != null ? regeocodeResult.getRegeocodeQuery().getPoint() : null;
        if (null == destLocationPoint) {
            failedCount++;
            if (failedCount >= 3) { //失败太多次，尝试下一个任务
                taskList.remove(0);
            } else {
                return;
            }
        } else {
            int position = -1;
            for (int i = 0; i < mAdapter.getData().size(); i++) {
                LocationDeviceInfo info = mAdapter.getItem(i);
                if (info.location.getLatitude() == destLocationPoint.getLatitude() && info.location.getLongitude() == destLocationPoint.getLongitude()) {
                    String destFormatAddressStr = getFormatAddress(regeocodeResult);
                    if (!TextUtils.isEmpty(destFormatAddressStr) && !destFormatAddressStr.equals(info.locationString)) {
                        info.locationString = destFormatAddressStr;
                        position = i;
                    }
                    break;
                }
            }
            if (position != -1) { //有位置信息更新
                mAdapter.notifyItemChanged(position);
            }
            taskList.remove(destLocationPoint);
        }
        failedCount = 0;
        if (taskList.size() > 0) {
            LatLonPoint lonPoint = taskList.get(0);
            mViewModel.getFromLocation(lonPoint.getLatitude(), lonPoint.getLongitude());
        }
    }

    private String getFormatAddress(RegeocodeResult regeocodeResult) {
        String resultFormatAddress = null;
        if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null) {
            RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
            resultFormatAddress = regeocodeAddress.getFormatAddress();
        }
        return resultFormatAddress;
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
}
