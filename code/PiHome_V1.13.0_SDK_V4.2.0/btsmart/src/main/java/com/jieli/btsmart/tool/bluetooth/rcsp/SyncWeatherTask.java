package com.jieli.btsmart.tool.bluetooth.rcsp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.weather.LocalWeatherForecastResult;
import com.amap.api.services.weather.LocalWeatherLive;
import com.amap.api.services.weather.LocalWeatherLiveResult;
import com.amap.api.services.weather.WeatherSearch;
import com.amap.api.services.weather.WeatherSearchQuery;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.command.health.message.Weather;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.interfaces.IActionCallback;
import com.jieli.bluetooth.interfaces.OnThreadStateListener;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.data.model.chargingcase.CityInfo;
import com.jieli.btsmart.util.NetworkStateHelper;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * SyncWeatherTask
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 同步天气任务
 * @since 2024/4/29
 */
public class SyncWeatherTask {
    private final String tag = getClass().getSimpleName();

    /**
     * 成功间隔 - 5分钟
     */
    private final static long SUCCESS_INTERVAL = 5 * 60 * 1000L;
    /**
     * 失败间隔 - 2分钟
     */
    private final static long FAIL_INTERVAL = 2 * 60 * 1000L;

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);

    private final static String DEFAULT_CITY = "北京市";

    private final static int MSG_EXECUTE_TASK = 10024;

    private final RCSPController mRCSPController;
    private final NetworkStateHelper networkStateHelper = NetworkStateHelper.getInstance();
    private final long interval;
    private final OnThreadStateListener listener;

    private final Gson gson = new GsonBuilder().create();

    private BluetoothDevice usingDevice;

    private AMapLocationClient locationClient;
    private OkHttpClient httpClient;

    private static volatile boolean isRunning;

    private final Handler uiHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (MSG_EXECUTE_TASK == msg.what) {
            executeTask();
        }
        return true;
    });

    public SyncWeatherTask(@NonNull RCSPController controller, OnThreadStateListener listener) {
        this(controller, SUCCESS_INTERVAL, listener);
    }

    public SyncWeatherTask(@NonNull RCSPController controller, long interval, OnThreadStateListener listener) {
        mRCSPController = controller;
        this.interval = interval;
        this.listener = listener;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public BluetoothDevice getUsingDevice(){
        return usingDevice;
    }

    public boolean start(@NonNull BluetoothDevice device) {
        if (isRunning()) {
            JL_Log.w(tag, "start", "Task is running.");
            return false;
        }
        JL_Log.d(tag, "start", "");
        isRunning = true;
        usingDevice = device;
        mRCSPController.addBTRcspEventCallback(mBTRcspEventCallback);
        networkStateHelper.registerListener(mNetworkListener);
        uiHandler.sendEmptyMessage(MSG_EXECUTE_TASK);
        if (null != listener) listener.onStart(1, tag);
        return true;
    }

    public boolean stop() {
        if (!isRunning()) {
            JL_Log.w(tag, "stop", "Task is stopped.");
            return false;
        }
        JL_Log.d(tag, "stop", "");
        destroyLocationClient();
        uiHandler.removeMessages(MSG_EXECUTE_TASK);
        mRCSPController.removeBTRcspEventCallback(mBTRcspEventCallback);
        networkStateHelper.unregisterListener(mNetworkListener);
        usingDevice = null;
        httpClient = null;
        isRunning = false;
        if (null != listener) listener.onEnd(1, tag);
        return true;
    }

    private void executeTask() {
        if (!isRunning()) {
            JL_Log.d(tag, "executeTask", "Task is stopped.");
            return;
        }
        if (!mRCSPController.isDeviceConnected() || MainApplication.getApplication().isOTA()) {
            JL_Log.i(tag, "executeTask", "device is disconnected or Ota in progress.");
            stop();
            return;
        }
        if (!networkStateHelper.isNetworkIsAvailable()) {
            handleFinishEvent(ErrorCode.SUB_ERR_OP_FAILED, "There is an issue with the network.");
            return;
        }
        syncWeatherInfo(MainApplication.getApplication().getApplicationContext(), new IActionCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean message) {
                handleFinishEvent(0, "Success");
            }

            @Override
            public void onError(BaseError error) {
                handleFinishEvent(error.getSubCode(), error.getMessage());
            }
        });
    }

    private void handleFinishEvent(int code, String message) {
        JL_Log.i(tag, "handleFinishEvent", "code = " + code + ", " + message);
        uiHandler.removeMessages(MSG_EXECUTE_TASK);
        if (!isRunning()) return;
        if (code == 0) { //成功
            uiHandler.sendEmptyMessageDelayed(MSG_EXECUTE_TASK, interval);
        } else { //失败
            uiHandler.sendEmptyMessageDelayed(MSG_EXECUTE_TASK, FAIL_INTERVAL);
        }
    }

    private void syncWeatherInfo(@NonNull Context context, @NonNull IActionCallback<Boolean> callback) {
        requestLocation(context, new IActionCallback<String>() {
            @Override
            public void onSuccess(String message) {
                requestWeather(context, message, new IActionCallback<LocalWeatherLive>() {
                    @Override
                    public void onSuccess(LocalWeatherLive message) {
                        syncWeatherToDevice(message, new IActionCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean message) {
                                callback.onSuccess(message);
                            }

                            @Override
                            public void onError(BaseError error) {
                                JL_Log.i(tag, "syncWeatherInfo", "syncWeatherToDevice fail. " + error);
                                callback.onError(error);
                            }
                        });
                    }

                    @Override
                    public void onError(BaseError error) {
                        JL_Log.i(tag, "syncWeatherInfo", "requestWeather fail. " + error);
                        callback.onError(error);
                    }
                });
            }

            @Override
            public void onError(BaseError error) {
                callback.onError(error);
            }
        });
    }

    private void requestLocation(@NonNull Context context, @NonNull IActionCallback<String> callback) {
        requestLocationBySDK(context, new IActionCallback<String>() {
            @Override
            public void onSuccess(String message) {
                JL_Log.d(tag, "requestLocation", "onSuccess ---> " + message);
                if (null == message || message.isEmpty()) {
                    onError(new BaseError(ErrorCode.SUB_ERR_PARAMETER, ErrorCode.code2Msg(ErrorCode.SUB_ERR_PARAMETER)));
                    return;
                }
                callback.onSuccess(message);
            }

            @Override
            public void onError(BaseError error) {
                JL_Log.i(tag, "requestLocation", "requestLocationBySDK fail. " + error);
                requestLocationByIp(new IActionCallback<String>() {
                    @Override
                    public void onSuccess(String message) {
                        callback.onSuccess(message);
                    }

                    @Override
                    public void onError(BaseError error) {
                        JL_Log.i(tag, "requestLocation", "requestLocationByIp fail. " + error);
                        callback.onError(error);
                    }
                });
            }
        });
    }

    private void requestLocationBySDK(@NonNull Context context, @NonNull IActionCallback<String> callback) {
        try {
            if (locationClient == null) {
                locationClient = new AMapLocationClient(context);
                locationClient.setLocationListener(aMapLocation -> {
                    JL_Log.d(tag, "requestLocationBySDK", "location result = " + aMapLocation.getErrorCode()
                            + ", " + aMapLocation.getErrorInfo());
                    if (aMapLocation.getErrorCode() == 0) {
                        final String city = aMapLocation.getCity();
                        JL_Log.d(tag, "requestLocationBySDK", "city : " + city);
                        callback.onSuccess(city);
                    } else {
                        callback.onError(new BaseError(aMapLocation.getErrorCode(), aMapLocation.getErrorInfo()));
                    }
                });
                AMapLocationClientOption option = new AMapLocationClientOption()
                        .setOnceLocation(true).setOnceLocationLatest(true).setHttpTimeOut(10000)
                        .setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                locationClient.setLocationOption(option);
            }
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            locationClient.stopLocation();
            JL_Log.d(tag, "requestLocationBySDK", "startLocation");
            locationClient.startLocation();
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError(new BaseError(ErrorCode.SUB_ERR_IO_EXCEPTION, e.getMessage()));
        }
    }

    private void destroyLocationClient() {
        if (null != locationClient) {
            locationClient.stopLocation();
            locationClient.onDestroy();
            locationClient = null;
        }
    }

    private void requestLocationByIp(@NonNull IActionCallback<String> callback) {
        String url = "http://restapi.amap.com/v3/ip?key=2824c5044e6cd020c3af5e5d923cb883";
        if (null == httpClient) {
            httpClient = new OkHttpClient.Builder()
                    .callTimeout(10, TimeUnit.SECONDS)
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build();
        }
        final Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                JL_Log.i(tag, "requestLocationByIp", "onFailure. " + e);
                callback.onSuccess(DEFAULT_CITY);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    onFailure(call, new IOException("request is fail."));
                    return;
                }
                final ResponseBody body = response.body();
                if (null == body) {
                    onFailure(call, new IOException("body is null."));
                    return;
                }
                String res = body.string();
                JL_Log.d(tag, "requestLocationByIp#onResponse", res);
                String city = DEFAULT_CITY;
                try {
                    CityInfo cityInfo = gson.fromJson(res, CityInfo.class);
                    city = cityInfo.getCity();
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                    JL_Log.w(tag, "requestLocationByIp#onResponse", "json error : " + e.getMessage());
                }
                callback.onSuccess(city);
            }
        });
    }

    private void requestWeather(@NonNull Context context, @NonNull String city, @NonNull IActionCallback<LocalWeatherLive> callback) {
        JL_Log.d(tag, "requestWeather", "city = " + city);
        try {
            WeatherSearch weatherSearch = new WeatherSearch(context);
            weatherSearch.setOnWeatherSearchListener(new WeatherSearch.OnWeatherSearchListener() {
                @Override
                public void onWeatherLiveSearched(LocalWeatherLiveResult localWeatherLiveResult, int code) {
                    if (code != 1000) {
                        JL_Log.i(tag, "requestWeather", "onWeatherLiveSearched. code = " + code);
                        callback.onError(new BaseError(ErrorCode.ERR_UNKNOWN, code));
                        return;
                    }
                    if (localWeatherLiveResult == null || localWeatherLiveResult.getLiveResult() == null) {
                        JL_Log.i(tag, "requestWeather", "onWeatherLiveSearched. no result.");
                        callback.onError(BaseError.buildResponseBadResult(code, -1));
                        return;
                    }
                    callback.onSuccess(localWeatherLiveResult.getLiveResult());
                }

                @Override
                public void onWeatherForecastSearched(LocalWeatherForecastResult localWeatherForecastResult, int code) {
                    callback.onError(new BaseError(ErrorCode.ERR_UNKNOWN, code));
                }
            });
            weatherSearch.setQuery(new WeatherSearchQuery(city, WeatherSearchQuery.WEATHER_TYPE_LIVE));
            weatherSearch.searchWeatherAsyn();
        } catch (AMapException e) {
            e.printStackTrace();
            callback.onError(new BaseError(ErrorCode.SUB_ERR_IO_EXCEPTION, e.getMessage()));
        }
    }

    private void syncWeatherToDevice(@NonNull LocalWeatherLive weatherLive, IActionCallback<Boolean> callback) {
        try {
//            JL_Log.d(tag, "syncWeatherToDevice", "" + weatherLive);
            int weatherCode = getWeatherCode(weatherLive.getWeather());
            int windDirectionCode = getWindDirectionCode(weatherLive.getWindDirection());
            int temperature = Integer.parseInt(weatherLive.getTemperature());
            int humidity = Integer.parseInt(weatherLive.getHumidity());
            int windPower = (weatherLive.getWindPower().equals("≤3")) ? 3 : Integer.parseInt(weatherLive.getWindPower());
            long time = 0;
            Date date = dateFormat.parse(weatherLive.getReportTime());
            if (date != null) {
                time = date.getTime();
            }
            Weather weather = new Weather(weatherLive.getProvince(), weatherLive.getCity(), weatherCode, temperature,
                    humidity, windPower, windDirectionCode, time);
            JL_Log.d(tag, "syncWeatherToDevice", "" + weather);
            ChargingCaseOpImpl.instance(mRCSPController.getRcspOp()).syncWeatherInfo(usingDevice, weather, new OnRcspActionCallback<Boolean>() {
                @Override
                public void onSuccess(BluetoothDevice device, Boolean message) {
                    if (callback != null) callback.onSuccess(message);
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {
                    if (callback != null) callback.onError(error);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                callback.onError(new BaseError(ErrorCode.SUB_ERR_IO_EXCEPTION, e.getMessage()));
            }
        }
    }

    private int getWeatherCode(String weather) {
        List<String> weathers = new ArrayList<>();
        weathers.add("晴");//0
        weathers.add("少云");//1
        weathers.add("晴间多云");//2
        weathers.add("多云");//3
        weathers.add("阴");//4
        weathers.add("有风/和风/清风/微风");//5
        weathers.add("平静");//6
        weathers.add("大风/强风/劲风/疾风");//7
        weathers.add("飓风/狂爆风");//8
        weathers.add("热带风暴/风暴");//9
        weathers.add("霾/中度霾/重度霾/严重霾");//10
        weathers.add("阵雨");//11
        weathers.add("雷阵雨");//12
        weathers.add("雷阵雨并伴有冰雹");//13
        weathers.add("雨/小雨/毛毛雨/细雨/小雨-中雨");//14
        weathers.add("中雨/中雨-大雨");//15
        weathers.add("大雨/大雨-暴雨");//16
        weathers.add("暴雨/暴雨-大暴雨");//17
        weathers.add("大暴雨/大暴雨-特大暴雨");//18
        weathers.add("特大暴雨");//19
        weathers.add("强阵雨");//20
        weathers.add("强雷阵雨");//21
        weathers.add("极端降雨");//22
        weathers.add("雨夹雪/阵雨夹雪/冻雨/雨雪天气");//23
        weathers.add("雪");//24
        weathers.add("阵雪");//25
        weathers.add("小雪/小雪-中雪");//26
        weathers.add("中雪/中雪-大雪");//27
        weathers.add("大雪/大雪-暴雪");//28
        weathers.add("暴雪");//29
        weathers.add("浮尘");//30
        weathers.add("扬沙");//31
        weathers.add("沙尘暴");//32
        weathers.add("强沙尘暴");//33
        weathers.add("龙卷风");//34
        weathers.add("雾/轻雾/浓雾/强浓雾/特强浓雾");//35
        weathers.add("热");//36
        weathers.add("冷");//37
        int code = 38;
        for (int i = 0; i < weathers.size(); i++) {
            String tmp = weathers.get(i);
            if (weather.equals(tmp) || tmp.contains("/" + weather) || tmp.contains(weather + "/")) {
                code = i;
                break;
            }
        }
        return code;
    }

    private int getWindDirectionCode(String windDirection) {
        List<String> windDirections = new ArrayList<>();
        windDirections.add("无风向");//0
        windDirections.add("东");//1
        windDirections.add("南");//2
        windDirections.add("西");//3
        windDirections.add("北");//4
        windDirections.add("东南");//5
        windDirections.add("东北");//6
        windDirections.add("西北");//7
        windDirections.add("西南");//8
        windDirections.add("旋转不定");//9
        int code = 0;
        for (int i = 0; i < windDirections.size(); i++) {
            String tmp = windDirections.get(i);
            if (windDirection.equals(tmp)) {
                code = i;
                break;
            }
        }
        return code;
    }

    private final BTRcspEventCallback mBTRcspEventCallback = new BTRcspEventCallback() {

        @Override
        public void onAdapterStatus(boolean bEnabled, boolean bHasBle) {
            if (!isRunning()) return;
            if (!bEnabled) {
                JL_Log.d(tag, "onAdapterStatus", "bt is close.");
                stop();
            }
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (!isRunning()) return;
            if (BluetoothUtil.deviceEquals(device, usingDevice) && status != StateCode.CONNECTION_OK) {
                JL_Log.i(tag, "executeTask", "device is disconnected.");
                stop();
            }
        }
    };

    private final NetworkStateHelper.Listener mNetworkListener = (type, available) -> {
        if (!isRunning()) return;
        if (available && uiHandler.hasMessages(MSG_EXECUTE_TASK)) {
            uiHandler.removeMessages(MSG_EXECUTE_TASK);
            uiHandler.sendEmptyMessage(MSG_EXECUTE_TASK);
        }
    };
}
