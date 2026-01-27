package com.jieli.btsmart.tool.product;

import com.jieli.bluetooth.bean.history.HistoryBluetoothDevice;
import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.JL_DeviceType;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/12/9 8:53 AM
 * @desc :产品默认资源获取
 */
public class DefaultResFactory {

    private static final String TAG = DefaultResFactory.class.getSimpleName();

    public static DefaultRes createByDeviceType(int deviceType, int advVersion) {
        JL_Log.d(TAG, "createByDeviceType", "deviceType : " + deviceType + ", advVersion : " + advVersion);
        switch (deviceType) {
            case JL_DeviceType.JL_DEVICE_TYPE_SOUND_CARD:
                return new SoundCardDefaultResImpl();
            case JL_DeviceType.JL_DEVICE_TYPE_CHARGING_BIN:
//            case JL_DeviceType.JL_DEVICE_TYPE_WATCH:
                return new ChargingCaseResImpl();
            case JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V1:
            case JL_DeviceType.JL_DEVICE_TYPE_TWS_HEADSET_V2:
                if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {
                    return new NeckHeadsetDefaultResImpl();
                }
                return new HeadsetDefaultResImpl();
            case JL_DeviceType.JL_DEVICE_TYPE_DONGLE:
                return new DongleDefaultResImpl();
            default:
                return new SoundBoxDefaultResImpl();
        }
    }

    /**
     * @param advVersion 广播包版本号
     */
    public static DefaultRes createBySdkType(String address, int sdkType, int advVersion) {
        JL_Log.d(TAG, "createBySdkType", "address : " + address +
                ", sdkType : " + sdkType + ", advVersion : " + advVersion);
        switch (sdkType) {
            case JLChipFlag.JL_CHIP_FLAG_695X_SOUND_CARD:
                return new SoundCardDefaultResImpl();
            case JLChipFlag.JL_CHIP_FLAG_695X_CHARGINGBIN:
            case JLChipFlag.JL_COLOR_SCREEN_CHARGING_CASE:
                return new ChargingCaseResImpl();
            case JLChipFlag.JL_CHIP_FLAG_MANIFEST_EARPHONE:
            case JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET:
            case JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET:
                if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {
                    return new NeckHeadsetDefaultResImpl();
                }
                return new HeadsetDefaultResImpl();
            case JLChipFlag.JL_CHIP_FLAG_DONGLE_SDK:
                return new DongleDefaultResImpl();
            case JLChipFlag.COMMON_SDK:
                HistoryBluetoothDevice history = !DeviceAddrManager.isInit() ? null :
                        DeviceAddrManager.getInstance().findHistoryBluetoothDevice(address);
                if (null == history) {
                    return new SoundBoxDefaultResImpl();
                }
                return createByDeviceType(history.getDeviceType(), history.getAdvVersion());
            default:
                return new SoundBoxDefaultResImpl();
        }
    }


    public interface DefaultRes {

        int getLeftImg();

        int getRightImg();

        int getBinImg();

        int getDoubleImg();

        int getLogoImg();

        int getBlackShowIcon();

        int getWhiteShowIcon();

        int getOnMapListIcon();

        int getOnMapIcon();


    }


    private static class HeadsetDefaultResImpl implements DefaultRes {

        @Override
        public int getLeftImg() {
            return R.drawable.ic_headset_left;
        }

        @Override
        public int getRightImg() {
            return R.drawable.ic_headset_right;
        }

        @Override
        public int getDoubleImg() {
            return R.drawable.ic_default_double_headset;
        }

        @Override
        public int getBinImg() {
            return R.drawable.ic_charging_bin;
        }

        @Override
        public int getLogoImg() {
            return R.drawable.ic_tws_headset;
        }

        @Override
        public int getBlackShowIcon() {
            return R.drawable.ic_tws_headset_black;
        }

        @Override
        public int getWhiteShowIcon() {
            return R.drawable.ic_tws_headset_white;
        }

        @Override
        public int getOnMapListIcon() {
            return R.drawable.ic_search_device_tws_headset;
        }

        @Override
        public int getOnMapIcon() {
            return R.drawable.ic_headset_location_white;
        }


    }

    private static class NeckHeadsetDefaultResImpl implements DefaultRes {

        @Override
        public int getLeftImg() {
            return R.drawable.ic_default_neck_headset;
        }

        @Override
        public int getRightImg() {
            return R.drawable.ic_default_neck_headset;
        }

        @Override
        public int getDoubleImg() {
            return R.drawable.ic_default_neck_headset;
        }

        @Override
        public int getBinImg() {
            return R.drawable.ic_default_neck_headset;
        }

        @Override
        public int getLogoImg() {
            return R.drawable.ic_neck_headset;
        }

        @Override
        public int getBlackShowIcon() {
            return R.drawable.ic_neck_headset_black;
        }

        @Override
        public int getWhiteShowIcon() {
            return R.drawable.ic_neck_headset_white;
        }

        @Override
        public int getOnMapListIcon() {
            return R.drawable.ic_search_device_neck_headset;
        }

        @Override
        public int getOnMapIcon() {
            return R.drawable.ic_neck_headset_location_white;
        }


    }

    private static class SoundBoxDefaultResImpl implements DefaultRes {

        @Override
        public int getLeftImg() {
            return R.drawable.ic_default_product_design;
        }

        @Override
        public int getRightImg() {
            return getLeftImg();
        }

        @Override
        public int getBinImg() {
            return getLeftImg();
        }

        @Override
        public int getLogoImg() {
            return getLeftImg();
        }

        @Override
        public int getBlackShowIcon() {
            return R.drawable.ic_soundbox_black;
        }

        @Override
        public int getWhiteShowIcon() {
            return R.drawable.ic_soundbox_logo_white;
        }

        @Override
        public int getDoubleImg() {
            return 0;
        }

        @Override
        public int getOnMapListIcon() {
            return R.drawable.ic_search_device_sound_box;
        }

        @Override
        public int getOnMapIcon() {
            return R.drawable.ic_soundbox_location_white;
        }
    }


    private static class SoundCardDefaultResImpl implements DefaultRes {

        @Override
        public int getLeftImg() {
            return R.drawable.ic_default_soundcard_logo;
        }

        @Override
        public int getRightImg() {
            return getLeftImg();
        }

        @Override
        public int getBinImg() {
            return getLeftImg();
        }

        @Override
        public int getLogoImg() {
            return getLeftImg();
        }

        @Override
        public int getBlackShowIcon() {
            return R.drawable.ic_sound_card_icon_black;
        }

        @Override
        public int getWhiteShowIcon() {
            return R.drawable.ic_sound_card_icon_white;
        }

        @Override
        public int getDoubleImg() {
            return 0;
        }

        @Override
        public int getOnMapListIcon() {
            return R.drawable.ic_search_device_list_sound_card;
        }

        @Override
        public int getOnMapIcon() {
            return R.drawable.ic_search_device_sound_card;
        }
    }


    private static class ChargingCaseResImpl implements DefaultRes {

        @Override
        public int getLeftImg() {
            return R.drawable.ic_headset_left_black;
        }

        @Override
        public int getRightImg() {
            return R.drawable.ic_headset_right_black;
        }

        @Override
        public int getBinImg() {
            return R.drawable.ic_charging_case_default;
        }

        @Override
        public int getDoubleImg() {
            return R.drawable.ic_double_heaset_black;
        }

        @Override
        public int getLogoImg() {
            return getBinImg();
        }

        @Override
        public int getBlackShowIcon() {
            return R.drawable.ic_charging_case_black;
        }

        @Override
        public int getWhiteShowIcon() {
            return R.drawable.ic_charging_case_white;
        }

        @Override
        public int getOnMapListIcon() {
            return R.drawable.ic_charging_case_map_black;
        }

        @Override
        public int getOnMapIcon() {
            return R.drawable.ic_charging_case_map_black;
        }
    }

    private static class DongleDefaultResImpl implements DefaultRes {

        @Override
        public int getLeftImg() {
            return R.drawable.ic_dongle;
        }

        @Override
        public int getRightImg() {
            return getLeftImg();
        }

        @Override
        public int getBinImg() {
            return getLeftImg();
        }

        @Override
        public int getLogoImg() {
            return getLeftImg();
        }

        @Override
        public int getBlackShowIcon() {
            return R.drawable.ic_dongle_black;
        }

        @Override
        public int getWhiteShowIcon() {
            return R.drawable.ic_dongle_white;
        }

        @Override
        public int getDoubleImg() {
            return 0;
        }

        @Override
        public int getOnMapListIcon() {
            return R.drawable.ic_search_dongle;
        }

        @Override
        public int getOnMapIcon() {
            return R.drawable.ic_search_dongle;
        }
    }

}
