package com.jieli.btsmart;

import android.app.Application;

import com.github.gzuliyujiang.oaid.DeviceIdentifier;
import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.tool.DeviceAddrManager;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.bluetooth.rcsp.RcspEventHandleTask;
import com.jieli.btsmart.tool.bluetooth.rcsp.ScanBleDeviceTask;
import com.jieli.btsmart.tool.bluetooth.rcsp.UploadDeviceInfoTask;
import com.jieli.btsmart.tool.network.NetworkDetectionHelper;
import com.jieli.btsmart.tool.product.ProductCacheManager;
import com.jieli.btsmart.tool.room.AppDatabase;
import com.jieli.btsmart.tool.room.DataRepository;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.BleScanMsgCacheManager;
import com.jieli.btsmart.util.EqCacheUtil;
import com.jieli.btsmart.util.MultiLanguageUtils;
import com.jieli.btsmart.util.NetworkStateHelper;
import com.jieli.component.ActivityManager;
import com.jieli.component.Logcat;
import com.jieli.component.utils.FileUtil;
import com.jieli.component.utils.PreferencesHelper;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_http.JL_HttpClient;
import com.jieli.jl_http.bean.AppInfo;
import com.jieli.jl_http.bean.LogResponse;
import com.jieli.jl_http.interfaces.IActionListener;
import com.jieli.jl_http.util.Constant;
import com.king.wechat.qrcode.WeChatQRCodeDetector;
import com.tencent.bugly.crashreport.CrashReport;

import org.opencv.OpenCV;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 主程序入口
 *
 * <p>
 * 作用:主要完成库的初始化等操作。不需要耗时操作。
 * </p>
 *
 * @author zqjasonZhong
 * @date 2020/5/12
 */
public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    private static MainApplication sApplication;
    private LogResponse mLogResponse;

    public static String OAID;
    private boolean isNeedBanDialog = false; //是否需要禁止弹窗
    private boolean isOTA = false; //是否进行OTA

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        init();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        handleLog(false, false);
    }

    public void init() {
        EqCacheUtil.clear();//暂时重新打开app时重置eq缓存，如有需要可以放开
        CommonUtil.setMainContext(this);
        ToastUtil.init(this);
        ActivityManager.init(this);

        registerActivityLifecycleCallbacks(MultiLanguageUtils.callbacks);
    }

    public void initSDK() {
        JL_HttpClient.init(this);
        NetworkDetectionHelper.init(this);
        if (!DeviceAddrManager.isInit()) {
            DeviceAddrManager.init(this);
        }
        boolean isLog = PreferencesHelper.getSharedPreferences(this)
                .getBoolean(SConstant.KEY_USE_SAVE_LOG, BuildConfig.DEBUG || SConstant.IS_USE_SAVE_LOG);
        Logcat.setIsLog(isLog);
        handleLog(isLog, isLog);

        DataRepository.getInstance(AppDatabase.getInstance(getApplicationContext()));
        AppUtil.getAllDeviceSupportSearchStatus();
        //init rcsp wrapper
        if (!RCSPController.isInit()) {
            //config RCSPController
            BluetoothOption bluetoothOption = BluetoothOption.createDefaultOption()
                    .setUseMultiDevice(true)
                    .setPriority(BluetoothOption.PREFER_BLE)
                    .setMandatoryUseBLE(SConstant.IS_USE_BLE_WAY)
                    .setMtu(BluetoothConstant.BLE_MTU_MAX)
                    .setUseDeviceAuth(PreferencesHelper.getSharedPreferences(MainApplication.getApplication())
                            .getBoolean(SConstant.KEY_USE_DEVICE_AUTH, SConstant.IS_USE_DEVICE_AUTH))
                    .setBleScanMode(2);
            RCSPController.init(this, bluetoothOption);
        }

        if (SConstant.CHANG_DIALOG_WAY) {

            NetworkStateHelper.getInstance();

            RCSPController controller = RCSPController.getInstance();
            controller.addBTRcspEventCallback(ProductCacheManager.getInstance());
            controller.addBTRcspEventCallback(new RcspEventHandleTask(controller));
            controller.addBTRcspEventCallback(new ScanBleDeviceTask(this, controller));
            controller.addBTRcspEventCallback(new UploadDeviceInfoTask());
            controller.addBTRcspEventCallback(BleScanMsgCacheManager.getInstance());
        }

        //初始化OpenCV
        OpenCV.initOpenCV();
        //初始化WeChatQRCodeDetector
        WeChatQRCodeDetector.init(this);

        printfSystemMsg();
    }

    public void uploadAppInfo() {
        if (PreferencesHelper.getSharedPreferences(getApplicationContext()).
                getBoolean(SConstant.KEY_LOCAL_OTA_TEST, SConstant.IS_LOCAL_OTA_TEST)) {
            FileUtil.createFilePath(sApplication, sApplication.getPackageName(), SConstant.DIR_UPDATE);
        }
        if (mLogResponse == null) {
            AppInfo appInfo = new AppInfo();
            appInfo.setUuid(OAID);
            appInfo.setAppName(SystemUtil.getAppName(this));
            appInfo.setAppVersion(SystemUtil.getVersionName(this));
            appInfo.setPlatform(Constant.PLATFORM_ANDROID_L);
            appInfo.setBrand(SystemUtil.getDeviceBrand());
            appInfo.setSystem(SystemUtil.getDeviceManufacturer());
            appInfo.setVersion(SystemUtil.getSystemVersion());
            appInfo.setPhoneName(SystemUtil.getSystemModel());
            JL_Log.d(TAG, "uploadAppInfo", "---> " + appInfo);
            if (JL_HttpClient.isInit()) {
                JL_HttpClient.getInstance().uploadAppInfo(appInfo, new IActionListener<LogResponse>() {
                    @Override
                    public void onSuccess(LogResponse response) {
                        JL_Log.d(TAG, "uploadAppInfo", "onSuccess ---> " + response);
                        mLogResponse = response;
                    }

                    @Override
                    public void onError(int code, String message) {
                        JL_Log.w(TAG, "uploadAppInfo", "onError ---> code : " + code + ", " + message);
                    }
                });
            }
        }
    }

    public LogResponse getLogResponse() {
        return mLogResponse;
    }

    public static MainApplication getApplication() {
        return sApplication;
    }

    public boolean isNeedBanDialog() {
        return isNeedBanDialog;
    }

    public void setNeedBanDialog(boolean needBanDialog) {
        isNeedBanDialog = needBanDialog;
    }

    public boolean isOTA() {
        return isOTA;
    }

    public void setOTA(boolean OTA) {
        isOTA = OTA;
    }

    private void handleLog(boolean isLog, boolean isSaveLog) {
        JL_Log.setTagPrefix("home");
        JL_Log.configureLog(getApplicationContext(), isLog, isSaveLog);
        com.jieli.jl_bt_ota.util.JL_Log.setLog(isLog);
        com.jieli.jl_bt_ota.util.JL_Log.setLogOutput(JL_Log::addLogOutput);
//        com.jieli.jl_bt_ota.util.JL_Log.setIsSaveLogFile(getApplicationContext(), isSaveLog);
    }

    public static void privacyPolicyAgreed() {
        DeviceIdentifier.register(MainApplication.getApplication());
        CrashReport.initCrashReport(MainApplication.getApplication(), "71c9922361", true);
        String tempOAID = DeviceIdentifier.getOAID(MainApplication.getApplication());
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(tempOAID.getBytes());
            OAID = ValueUtil.byte2HexStr(md5.digest());
            JL_Log.d(TAG, "privacyPolicyAgreed", "id : " + OAID);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void printfSystemMsg() {
        String version = "";
        try {
            version = System.getProperty("os.version", "");
        } catch (Exception ignore) {

        }
        JL_Log.d(TAG, "printfSystemMsg", "brand : " + SystemUtil.getDeviceBrand()
                + "\n Manufacturer : " + SystemUtil.getDeviceManufacturer()
                + "\n SystemVersion : " + SystemUtil.getSystemVersion()
                + "\n SystemModel : " + SystemUtil.getSystemModel()
                + "\n version : " + version);
    }

    /**
     * 判断手机系统是否为纯血鸿蒙
     *
     * @return boolean 结果
     */
    public static boolean isHarmonyOSNext(){
        String manufacturer = SystemUtil.getDeviceManufacturer();
        if(null == manufacturer || manufacturer.isEmpty()) return false;
        if(!manufacturer.equalsIgnoreCase("HUAWEI")) return false;
        String version = "";
        try {
            version = System.getProperty("os.version", "");
        } catch (Exception ignore) {

        }
        if(null == version || version.isEmpty()) return false;
        for (char c : version.toCharArray()) {
            if (Character.isDigit(c)) {
                int firstDigit = Character.getNumericValue(c);
                return firstDigit >= 5; //因为纯血鸿蒙是 5.0.0， 所以判断首位数字大于等于5， 即认为是纯血鸿蒙系统
            }
        }
        return false;
    }

}
