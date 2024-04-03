package com.jieli.btsmart.tool.product;

import com.jieli.bluetooth.constant.JLChipFlag;
import com.jieli.bluetooth.constant.JL_DeviceType;
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

    public static DefaultRes createByDeviceType(int deviceType, int advVersion) {
        DefaultRes defaultRes;
        switch (deviceType) {
            case JL_DeviceType.JL_DEVICE_TYPE_SOUNDBOX:
                defaultRes = new SoundBoxDefaultResImpl();
                break;
            case JL_DeviceType.JL_DEVICE_TYPE_SOUND_CARD:
                defaultRes = new SoundCardDefaultResImpl();
                break;
            default:
                if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {
                    defaultRes = new NeckHeadsetDefaultResImpl();
                } else {
                    defaultRes = new HeadsetDefaultResImpl();
                }
                break;
        }
        return defaultRes;
    }

    /**
     * @param advVersion 广播包版本号
     */
    public static DefaultRes createBySdkType(int sdkType, int advVersion) {
        JL_Log.d("DefaultResFactory ", " createBySdkType2: " + sdkType + " advVersion: " + advVersion);
        DefaultRes defaultRes;
        switch (sdkType) {
            case JLChipFlag.JL_CHIP_FLAG_692X_AI_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_692X_ST_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_695X_CHARGINGBIN:
            case JLChipFlag.JL_CHIP_FLAG_696X_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_696X_TWS_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_695X_WATCH:
                defaultRes = new SoundBoxDefaultResImpl();
                break;
            case JLChipFlag.JL_CHIP_FLAG_695X_SOUND_CARD:
                defaultRes = new SoundCardDefaultResImpl();
                break;
            case JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET:
            case JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET:
            default:
                if (advVersion == SConstant.ADV_INFO_VERSION_NECK_HEADSET) {
                    defaultRes = new NeckHeadsetDefaultResImpl();
                } else {
                    defaultRes = new HeadsetDefaultResImpl();
                }
        }
        return defaultRes;
    }

    public static DefaultRes createBySdkType(int sdkType) {
        JL_Log.d("DefaultResFactory ", " createBySdkType1: " + sdkType);
        DefaultRes defaultRes;
        switch (sdkType) {
            case JLChipFlag.JL_CHIP_FLAG_692X_AI_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_692X_ST_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_695X_CHARGINGBIN:
            case JLChipFlag.JL_CHIP_FLAG_696X_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_696X_TWS_SOUNDBOX:
            case JLChipFlag.JL_CHIP_FLAG_695X_WATCH:
                defaultRes = new SoundBoxDefaultResImpl();
                break;
            case JLChipFlag.JL_CHIP_FLAG_695X_SOUND_CARD:
                defaultRes = new SoundCardDefaultResImpl();
                break;
            case JLChipFlag.JL_CHIP_FLAG_693X_TWS_HEADSET:
            case JLChipFlag.JL_CHIP_FLAG_697X_TWS_HEADSET:
            default:
                defaultRes = new HeadsetDefaultResImpl();
        }

        return defaultRes;
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

}
