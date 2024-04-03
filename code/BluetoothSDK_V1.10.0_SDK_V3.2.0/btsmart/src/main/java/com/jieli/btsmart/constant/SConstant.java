package com.jieli.btsmart.constant;

/**
 * 常量声明
 *
 * @author zqjasonZhong
 * @since 2020/5/15
 */
public class SConstant {

    public final static String TAG_AGREE = "user_agree";

    public final static int BLE_ADV_RSSI_LIMIT = -80;

    //限制没有收到广播包的间隔
    public final static int SHOW_DIALOG_TIMEOUT = 30 * 1000;
    public final static int SCAN_TIME = 30 * 1000;

    //悬浮窗口的权限申请码
    public final static int REQUEST_CODE_OVERLAY = 1345;
    public final static int REQUEST_CODE_DEVICE_SETTINGS = 6537;
    public final static int REQUEST_CODE_DEVICE_LED_SETTINGS = 6538;
    public final static int REQUEST_CODE_CHECK_GPS = 8118;
    public final static int REQUEST_CODE_PERMISSIONS = 9229;
    public final static int REQUEST_CODE_NETWORK = 4668;
    public final static int REQUEST_CODE_ADJUST_VOICE_MODE = 6539;

    public final static int LIMIT_DEVICE_NAME = 20;
    //多设备最大可连接经典蓝牙设备数量
    public final static int MULTI_DEVICE_MAX_NUMBER = 5;

    //key-constant
    public final static String KEY_BLUETOOTH_DEVICE = "bluetooth_device";
    public final static String KEY_BLE_SCAN_MESSAGE = "ble_scan_message";
    public final static String KEY_FRAGMENT_TAG = "fragment_tag";
    public final static String KEY_FRAGMENT_BUNDLE = "fragment_bundle";
    public final static String KEY_ADV_INFO = "adv_info";
    public final static String KEY_DEV_KEY_BEAN = "key_bean";
    public final static String KEY_SETTINGS_ITEM = "settings_item";
    public final static String KEY_DEV_LED_BEAN = "led_bean";
    public final static String KEY_DEV_NAME = "device_name";
    public final static String KEY_WEB_FLAG = "key_flag";
    public final static String KEY_ID3_INFO = "key_id3_info";
    public final static String KEY_UPGRADE_STATUS = "key_upgrade_status";
    public final static String KEY_DEV_INSTRUCTION_PATH = "key_dev_instruction_path";
    public final static String KEY_SEARCH_DEVICE_ADDR = "key_search_device_addr";
    public final static String KEY_UPGRADE_PATH = "key_upgrade_path";
    public final static String KEY_UPGRADE_RESULT = "key_upgrade_result";
    public final static String KEY_CUSTOM_BG_SEQ = "key_custom_bg_seq";
    public final static String KEY_DEVICE = "device";
    public final static String KEY_CONNECTION = "device_connection";

    /*Configure*/
    public final static boolean HAS_OTA = true;
    public final static boolean ALLOW_SHOW_BT_DIALOG = true;
    public final static boolean IS_LOCAL_OTA_TEST = false;
    //是否允许不连接切换功能页面
    public final static boolean ALLOW_SWITCH_FUN_DISCONNECT = false;//BuildConfig.DEBUG;
    //是否启用新的设备列表界面
    public final static boolean IS_USE_DEVICE_LIST_FRAGMENT = true;
    //是否使用设备认证
    public final static boolean IS_USE_DEVICE_AUTH = true;

    //是否模拟ANC测试数据
    public final static boolean TEST_ANC_FUNC = false;

    public final static String KEY_ALLOW_SHOW_BT_DIALOG = "allow_show_bt_dialog";
    public final static String KEY_LOCAL_OTA_TEST = "local_ota_test";
    public final static String KEY_BLE_ADV_RSSI_LIMIT = "ble_adv_rssi";
    public final static String KEY_USE_DEVICE_AUTH = "use_device_auth";

    /*dir*/
    public final static String DIR_DESIGN = "design";
    public final static String DIR_UPDATE = "upgrade";

    /*ota file*/
    public static final String FIRMWARE_UPGRADE_FILE = "updata.bfu";
    public static final String FIRMWARE_UPGRADE_FILE_AC693 = "update.ufw";

    /*Key Settings field*/
    public final static int KEY_FIELD_KEY_NUM = 1;         //key_num
    public final static int KEY_FIELD_KEY_ACTION = 2;      //key_action
    public final static int KEY_FIELD_KEY_FUNCTION = 3;    //key_function

    /*Led Settings field*/
    public final static int KEY_FIELD_LED_SCENE = 1;       //led_scene
    public final static int KEY_FIELD_LED_EFFECT = 2;     //led_effect

    /*device type*/
    public final static String DEVICE_HEADSET = "headset";
    public final static String DEVICE_SOUND_BOX = "sound_box";
    public final static String DEVICE_SOUND_CARD = "sound_card";
    /*advertise version 广播包版本号*/
    public final static int ADV_INFO_VERSION_NECK_HEADSET = 3;//挂脖耳机-3
    public final static int KEY_NUM_IDLE = 0;//耳机按键空闲-0

    public final static int DEVICE_VOLUME_STEP = 6;
    public final static String MEDIA_PLAY_MODE = "media_play_mode";

    //tws default text name
    public final static String AC693_JSON_NAME = "ac693x_headset_json.txt";
    public final static String AC697_JSON_NAME = "ac697x_headset_json.txt";
    public final static String AC696_JSON_NAME = "ac696x_soundbox_json.txt";
    public final static String AC696_TWS_JSON_NAME = "ac696x_soundbox_tws_json.txt";
    public final static String AC695_SOUND_CARD_JSON_NAME = "ac695x_sound_card.json";
    //neck default text name
    public final static String AC693_NECK_JSON_NAME = "ac693x_headset_neck_json.txt";
    public final static String MANIFEST_HEADSET_JSON_NAME = "manifest_headset_json.txt";
    public final static String MANIFEST_SOUNDBOX_JSON_NAME = "manifest_soundbox_json.txt";

    //error code
    public final static int ERR_DEV_CONNECTING = 0xe001;
    public final static int ERR_BLUETOOTH_NOT_ENABLE = 0xe002;
    public final static int ERR_EDR_MAX_CONNECTION = 0xe003;

    //action
    public final static String ACTION_ACTIVITY_RESUME = "com.jieli.btsmart.activity_resume";
    public final static String ACTION_FAST_CONNECT = "com.jieli.btsmart.fast_connect";
    public final static String ACTION_DEVICE_UPGRADE = "com.jieli.btsmart.device_upgrade";
    public final static String ACTION_ACTIVE_DEVICE_CHANGED = "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED";
    public final static String ACTION_ADD_DEVICE = "com.jieli.btsmart.add_device";
    public final static String ACTION_DEVICE_UPGRADE_RESULT = "com.jieli.btsmart.device_upgrade_result";
    public final static String ACTION_DEVICE_CONNECTION_CHANGE = "com.jieli.btsmart.device_connection_change";

    public final static Boolean CHANG_DIALOG_WAY = true; //是否使用重构后的代码

}
