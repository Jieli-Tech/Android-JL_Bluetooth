package com.jieli.btsmart.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.bean.device.DevBroadcastMsg;
import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.bean.parameter.NotifyAdvInfoParam;
import com.jieli.bluetooth.bean.response.ADVInfoResponse;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.tool.DeviceStatusManager;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.search.LocationInfo;
import com.jieli.jl_dialog.Jl_Dialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * UI辅助工具
 *
 * @author zqjasonZhong
 * @since 2020/5/16
 */
public class UIHelper {

    public static String getDevName(String devAddr) {
        if (devAddr == null) return null;
        return getDevName(BluetoothUtil.getRemoteDevice(devAddr));
    }

    public static String getDevName(BluetoothDevice device) {
        if (!PermissionUtil.checkHasConnectPermission(MainApplication.getApplication()) || null == device)
            return "";
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = device.getAddress();
        }
        return name;
    }

    public static boolean isHeadsetType(int sdkType) {
        boolean isHeadset = false;
        switch (sdkType) {
            case JLChipFlag.JL_CHIP_FLAG_692X_AI_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_692X_ST_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_695X_CHARGINGBIN:
            case JLChipFlag.JL_CHIP_FLAG_696X_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_696X_TWS_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_695X_SOUND_CARD:
            case JLChipFlag.JL_CHIP_FLAG_MANIFEST_SOUNDBOX:
                isHeadset = false;
                break;
            case JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET:
            case JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET:
            case JLChipFlag.JL_CHIP_FLAG_MANIFEST_EARPHONE:
                isHeadset = true;
                break;
        }
        return isHeadset;
    }

    public static boolean isHeadsetByDeviceType(int deviceType) {
        boolean isHeadset = false;
        switch (deviceType) {
            case JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V1:
            case JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V2:
                isHeadset = true;
                break;
        }
        return isHeadset;
    }

    public static boolean isSoundCardType(int sdkType) {
        return JLChipFlag.JL_CHIP_FLAG_692X_AI_SOUNDBOX == sdkType;
    }

    public static boolean isCanUseTwsCmd(int sdkType) {
        boolean isTwsProduct;
        isTwsProduct = sdkType == JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET
                || sdkType == JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET
                || sdkType == JLChipFlag.JL_CHIP_FLAG_696X_SOUNDBOX
                || sdkType == JLChipFlag.JL_CHIP_FLAG_695X_SOUND_CARD
                || sdkType == JLChipFlag.JL_CHIP_FLAG_696X_TWS_SOUNDBOX
                || sdkType == JLChipFlag.JL_CHIP_FLAG_MANIFEST_EARPHONE
                || sdkType == JLChipFlag.JL_CHIP_FLAG_MANIFEST_SOUNDBOX;

        return isTwsProduct;
    }

    public static String getCacheBleAddr(HistoryBluetoothDevice device) {
        String bleAddr = null;
        if (device != null) {
            if (device.getType() == BluetoothOption.PREFER_BLE) {
                bleAddr = device.getAddress();
            } else {
                bleAddr = DeviceAddrManager.getInstance().getDeviceAddr(device.getAddress());
            }
        }
        return bleAddr;
    }

    public static boolean isContainsBoundedEdrList(Context context, String addr) {
        if (!PermissionUtil.checkHasConnectPermission(context)) return false;
        if (BluetoothAdapter.getDefaultAdapter() == null) return false;
        Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        if (devices != null && BluetoothAdapter.checkBluetoothAddress(addr)) {
            for (BluetoothDevice device : devices) {
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE) continue;
                if (addr.equals(device.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ADVInfoResponse convertADVInfoFromDevBroadcastMsg(DevBroadcastMsg broadcastMsg) {
        if (broadcastMsg == null) return null;
        ADVInfoResponse advInfo = new ADVInfoResponse();
        advInfo.setVid(broadcastMsg.getVid());
        advInfo.setUid(broadcastMsg.getUid());
        advInfo.setPid(broadcastMsg.getPid());
        advInfo.setLeftDeviceQuantity(broadcastMsg.getLeftDeviceQuantity());
        advInfo.setLeftCharging(broadcastMsg.isLeftCharging());
        advInfo.setRightDeviceQuantity(broadcastMsg.getRightDeviceQuantity());
        advInfo.setRightCharging(broadcastMsg.isRightCharging());
        advInfo.setChargingBinQuantity(broadcastMsg.getChargingBinQuantity());
        advInfo.setDeviceCharging(broadcastMsg.isDeviceCharging());
        return advInfo;
    }

    public static ADVInfoResponse convertADVInfoFromBleScanMessage(BleScanMessage bleScanMessage) {
        if (bleScanMessage == null) return null;
        ADVInfoResponse advInfo = new ADVInfoResponse();
        advInfo.setVid(bleScanMessage.getVid());
        advInfo.setUid(bleScanMessage.getUid());
        advInfo.setPid(bleScanMessage.getPid());
        advInfo.setLeftDeviceQuantity(bleScanMessage.getLeftDeviceQuantity());
        advInfo.setLeftCharging(bleScanMessage.isLeftCharging());
        advInfo.setRightDeviceQuantity(bleScanMessage.getRightDeviceQuantity());
        advInfo.setRightCharging(bleScanMessage.isRightCharging());
        advInfo.setChargingBinQuantity(bleScanMessage.getChargingBinQuantity());
        advInfo.setDeviceCharging(bleScanMessage.isDeviceCharging());
        return advInfo;
    }

    public static BleScanMessage convertBleScanMsgFromNotifyADVInfo(NotifyAdvInfoParam advInfo) {
        if (advInfo == null) return null;
        return new BleScanMessage()
                .setSeq(advInfo.getSeq())
                .setAction(advInfo.getAction())
                .setVid(advInfo.getVid())
                .setUid(advInfo.getUid())
                .setPid(advInfo.getPid())
                .setEdrAddr(advInfo.getEdrAddr())
                .setDeviceType(advInfo.getDeviceType())
                .setVersion(advInfo.getVersion())
                .setLeftCharging(advInfo.isLeftCharging())
                .setLeftDeviceQuantity(advInfo.getLeftDeviceQuantity())
                .setRightCharging(advInfo.isRightCharging())
                .setRightDeviceQuantity(advInfo.getRightDeviceQuantity())
                .setDeviceCharging(advInfo.isDeviceCharging())
                .setChargingBinQuantity(advInfo.getChargingBinQuantity())
                .setConnectWay(advInfo.getConnectWay())
                .setSupportChargingCase(advInfo.isSupportChargingCase());
    }

    public static boolean compareBleScanMessage(BleScanMessage last, BleScanMessage current) {
        if (last == null || current == null) return false;
        boolean ret = false;
        if (last.getVid() == current.getVid() && last.getUid() == current.getUid()
                && last.getPid() == current.getPid()) { //判断是同一个设备的广播包信息
            if (last.getAction() == current.getAction() && last.getSeq() == current.getSeq() //判断动作和seq不变
                    && last.getLeftDeviceQuantity() == current.getLeftDeviceQuantity()  //判断左设备电量不变
                    && last.isLeftCharging() == current.isLeftCharging()  //判断左设备充电状态不变
                    && last.getRightDeviceQuantity() == current.getRightDeviceQuantity() //判断右设备电量不变
                    && last.isRightCharging() == current.isRightCharging() //判断右设备充电状态不变
                    && last.getChargingBinQuantity() == current.getChargingBinQuantity() //判断充电仓电量不变
                    && last.isDeviceCharging() == current.isDeviceCharging()) {//判断充电仓充电状态不变
                ret = true; //认为数据没变化
            }
        }
        return ret;
    }

    public static String getCacheDeviceName(BluetoothDevice device) {
        if (device != null) {
            String devName;
            HistoryBluetoothDevice historyBluetoothDevice = DeviceAddrManager.getInstance().findHistoryBluetoothDevice(device);
            devName = getCacheDeviceName(historyBluetoothDevice);
            if (TextUtils.isEmpty(devName)) {
                return getDevName(device);
            }
            return devName;
        }
        return null;
    }

    public static String getCacheDeviceName(HistoryBluetoothDevice history) {
        if (history != null) {
            return getCacheDeviceName(history.getName(), history.getType(), history.getAddress());
        }
        return null;
    }

    public static String getCacheDeviceName(String name, int protocol, String address) {
        String deviceName = name;
        if (protocol == BluetoothConstant.PROTOCOL_TYPE_SPP) {
            String mappedAddr = DeviceAddrManager.getInstance().getDeviceAddr(address);
            if (BluetoothAdapter.checkBluetoothAddress(mappedAddr) && !mappedAddr.equals(address)) {
                BluetoothDevice device = BluetoothUtil.getRemoteDevice(mappedAddr);
                String devName = device == null || !PermissionUtil.checkHasConnectPermission(MainApplication.getApplication()) ? "" : device.getName();
                if (!TextUtils.isEmpty(devName)) {
                    deviceName = devName;
                }
            }
        }
        return deviceName;
    }

    /**
     * 将时间戳转换成描述性时间（昨天、今天、明天）
     *
     * @param timestamp 时间戳
     * @return 描述性日期
     */
    public static String descriptiveData(Context context, long timestamp) {
        String descriptiveText = null;
        String format = "yyyy-MM-dd HH:mm";
        //当前时间
        Calendar currentTime = Calendar.getInstance();
        //要转换的时间
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestamp);
        //年相同
        if (currentTime.get(Calendar.YEAR) == time.get(Calendar.YEAR)) {
            //获取一年中的第几天并相减，取差值
            switch (currentTime.get(Calendar.DAY_OF_YEAR) - time.get(Calendar.DAY_OF_YEAR)) {
                case 1://当前比目标多一天，那么目标就是昨天
                    if (context != null) {
                        descriptiveText = context.getString(R.string.yesterday);
                    } else {
                        descriptiveText = "昨天";
                    }
                    format = "HH:mm";
                    break;
                case 0://当前和目标是同一天，就是今天
                    if (context != null) {
                        descriptiveText = context.getString(R.string.today);
                    } else {
                        descriptiveText = "今天";
                    }
                    format = "HH:mm";
                    break;
                case -1://当前比目标少一天，就是明天
                    if (context != null) {
                        descriptiveText = context.getString(R.string.tomorrow);
                    } else {
                        descriptiveText = "明天";
                    }
                    format = "HH:mm";
                    break;
                default:
                    descriptiveText = null;
                    format = "yyyy-MM-dd HH:mm";
                    break;
            }
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.getDefault());
        String formatDate = simpleDateFormat.format(time.getTime());
        if (!TextUtils.isEmpty(descriptiveText)) {
            return descriptiveText + " " + formatDate;
        }
        return formatDate;
    }

    public static List<LocationInfo> getLocationInfosByHistoryDevice(HistoryBluetoothDevice history) {
        if (history == null) return new ArrayList<>();
        List<LocationInfo> locationInfos = new ArrayList<>();
        ADVInfoResponse advInfo = DeviceStatusManager.getInstance().getAdvInfo(BluetoothUtil.getRemoteDevice(history.getAddress()));
        JL_Log.d("zzc_search", "getLocationInfosByHistoryDevice >> history = " + history + "\n advInfo = " + advInfo);
        boolean isHandler = false;
        if (advInfo != null) {
            if (advInfo.getLeftDeviceQuantity() > 0 && advInfo.getRightDeviceQuantity() > 0) { //TWS已连接
                locationInfos.add(new LocationInfo(history.getLeftDevLatitude(), history.getLeftDevLongitude(), history.getLeftDevUpdateTime()));
                isHandler = true;
            } else if (advInfo.getLeftDeviceQuantity() == 0 && advInfo.getRightDeviceQuantity() > 0) { //仅右耳连接
                locationInfos.add(new LocationInfo(history.getRightDevLatitude(), history.getRightDevLongitude(), history.getRightDevUpdateTime(), LocationInfo.DEVICE_FLAG_RIGHT));
                locationInfos.add(new LocationInfo(history.getLeftDevLatitude(), history.getLeftDevLongitude(), history.getLeftDevUpdateTime(), LocationInfo.DEVICE_FLAG_LEFT));
                isHandler = true;
            } else if (advInfo.getRightDeviceQuantity() == 0 && advInfo.getLeftDeviceQuantity() > 0) { //仅左耳连接
                locationInfos.add(new LocationInfo(history.getLeftDevLatitude(), history.getLeftDevLongitude(), history.getLeftDevUpdateTime(), LocationInfo.DEVICE_FLAG_LEFT));
                locationInfos.add(new LocationInfo(history.getRightDevLatitude(), history.getRightDevLongitude(), history.getRightDevUpdateTime(), LocationInfo.DEVICE_FLAG_RIGHT));
                isHandler = true;
            }
        }
        //其他情况处理
        if (!isHandler) {
            if (history.getLeftDevLatitude() != 0 || history.getLeftDevLongitude() != 0) {
                if (history.getRightDevLatitude() == 0 && history.getRightDevLatitude() == 0) { //右侧设备经纬度为0
                    int deviceFlag = history.getRightDevUpdateTime() > 0 ? LocationInfo.DEVICE_FLAG_LEFT : LocationInfo.DEVICE_FLAG_NONE;
                    locationInfos.add(new LocationInfo(history.getLeftDevLatitude(), history.getLeftDevLongitude(), history.getLeftDevUpdateTime(), deviceFlag));
                } else { //左右侧设备都有经纬度，判断是否相同，再判断更新时间
                    //左右耳经纬度相同且更新时间一致
                    if (history.getLeftDevLatitude() == history.getRightDevLatitude() && history.getLeftDevLongitude() == history.getRightDevLongitude()
                            && history.getLeftDevUpdateTime() == history.getRightDevUpdateTime()) {
                        locationInfos.add(new LocationInfo(history.getLeftDevLatitude(), history.getLeftDevLongitude(),
                                history.getLeftDevUpdateTime()));
                    } else {
                        if (history.getLeftDevUpdateTime() >= history.getRightDevUpdateTime()) { //保证第一个是最新更新的
                            locationInfos.add(new LocationInfo(history.getLeftDevLatitude(), history.getLeftDevLongitude(), history.getLeftDevUpdateTime(), LocationInfo.DEVICE_FLAG_LEFT));
                            locationInfos.add(new LocationInfo(history.getRightDevLatitude(), history.getRightDevLongitude(), history.getRightDevUpdateTime(), LocationInfo.DEVICE_FLAG_RIGHT));
                        } else {
                            locationInfos.add(new LocationInfo(history.getRightDevLatitude(), history.getRightDevLongitude(), history.getRightDevUpdateTime(), LocationInfo.DEVICE_FLAG_RIGHT));
                            locationInfos.add(new LocationInfo(history.getLeftDevLatitude(), history.getLeftDevLongitude(), history.getLeftDevUpdateTime(), LocationInfo.DEVICE_FLAG_LEFT));
                        }
                    }
                }
            } else { //左耳经纬度为0
                if (history.getRightDevLatitude() != 0 || history.getRightDevLongitude() != 0) {
                    locationInfos.add(new LocationInfo(history.getRightDevLatitude(), history.getRightDevLongitude(), history.getRightDevUpdateTime(), LocationInfo.DEVICE_FLAG_RIGHT));
                }
            }
        }
        JL_Log.d("zzc_search", "getLocationInfosByHistoryDevice >> size = " + locationInfos.size());
        return locationInfos;
    }

    public static boolean isEdrConnect(String edrAddress) {
        BluetoothDevice device = BluetoothUtil.getRemoteDevice(edrAddress);
        if (device == null) return false;
        List<BluetoothDevice> connectedList = BluetoothUtil.getSystemConnectedBtDeviceList();
        return connectedList.contains(device);
    }

    public static void setVisibility(View view, int visibility) {
        if (null == view) return;
        final int oldValue = view.getVisibility();
        if (oldValue != visibility) {
            view.setVisibility(visibility);
        }
    }

    public static void show(View view) {
        setVisibility(view, View.VISIBLE);
    }

    public static void gone(View view) {
        setVisibility(view, View.GONE);
    }

    public static void hide(View view) {
        setVisibility(view, View.INVISIBLE);
    }

    public static void showAppSettingDialog(@NonNull Fragment context, @NonNull String content) {
        Jl_Dialog jl_dialog = Jl_Dialog.builder()
                .title(context.getString(R.string.permission))
                .content(content)
                .right(context.getString(R.string.setting))
                .rightClickListener((v, dialogFragment) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.requireContext().getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                    dialogFragment.dismiss();
                })
                .left(context.getString(R.string.cancel))
                .leftClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                })
                .build();
        jl_dialog.show(context.getChildFragmentManager(), "showAppSettingDialog");
    }

    public static void showAppSettingDialog(@NonNull FragmentActivity context, @NonNull String content) {
        Jl_Dialog jl_dialog = Jl_Dialog.builder()
                .title(context.getString(R.string.permission))
                .content(content)
                .right(context.getString(R.string.setting))
                .rightClickListener((v, dialogFragment) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                    intent.setData(uri);
                    context.startActivity(intent);
                    dialogFragment.dismiss();
                })
                .left(context.getString(R.string.cancel))
                .leftClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                })
                .build();
        jl_dialog.show(context.getSupportFragmentManager(), "showAppSettingDialog");
    }
}
