package com.jieli.btsmart.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.jieli.bluetooth.bean.message.MessageInfo;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.device.DeviceSettings;
import com.jieli.btsmart.tool.configure.ConfigureKit;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 通知栏处理类
 * @since 2021/4/28
 */
public class NotificationUtil {
    private static final String TAG = NotificationUtil.class.getSimpleName();

    //default package name
    public final static String PACKAGE_NAME_SYS_MESSAGE = "com.android.mms";
    public final static String PACKAGE_NAME_WECHAT = "com.tencent.mm";
    public final static String PACKAGE_NAME_QQ = "com.tencent.mobileqq";
    public final static String PACKAGE_NAME_DING_TALK = "com.alibaba.android.rimet";
    public final static String PACKAGE_NAME_LARK = "com.ss.android.lark";

    private static final String PACKAGE_NAME = "com_jieli_btsmart_";
    private static final String CHANNEL = "channel_";
    private static final String ACTION = "action_";
    private static final String NOTIFICATION_CHANNEL_ID_ONE = PACKAGE_NAME + CHANNEL + "001";
    private static final String NOTIFICATION_CHANNEL_ONE_NAME = "Foreground Service Notification";


    public static final String ACTION_NOTIFY_MESSAGE = PACKAGE_NAME + ACTION + "notify_message";
    public static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    public static final String EXTRA_NOTIFICATION_MSG = "notification_msg";
    public static final String EXTRA_NOTIFICATION_ICON = "notification_icon";

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    private final String[] defaultAppPackageNameArray = new String[]{
            PACKAGE_NAME_SYS_MESSAGE,
            PACKAGE_NAME_WECHAT,
            PACKAGE_NAME_QQ,
            PACKAGE_NAME_DING_TALK,
            PACKAGE_NAME_LARK,
    };

    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     *
     * @return True if enabled, false otherwise.
     */
    public static boolean isNotificationServiceEnabled(Context context) {
        if (null == context) return false;
        String pkgName = context.getPackageName();
        final String flat = Settings.Secure.getString(context.getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (TextUtils.isEmpty(flat)) return false;
        final String[] names = flat.split(":");
        for (String name : names) {
            final ComponentName cn = ComponentName.unflattenFromString(name);
            if (cn != null && TextUtils.equals(pkgName, cn.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 开启通知监听服务界面
     *
     * @param launcher 请求
     */
    public static void openNotificationListenerSettings(@NonNull ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS);
        launcher.launch(intent);
    }

    /**
     * 判断应用是否具有通知权限
     *
     * @param context 上下文
     * @return 结果
     */
    public static boolean isNotificationEnable(Context context) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        return notificationManagerCompat.areNotificationsEnabled();
    }

    /**
     * 打开应用通知设置界面
     *
     * @param context  上下文
     * @param launcher 请求
     */
    public static void openAppNotificationSettings(@NonNull Context context, @NonNull ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
        }
        launcher.launch(intent);
    }

    public static Notification createNotification(Context context, String title, String content, int smallIconRes, PendingIntent intent) {
        return createNotification(context, title, content, smallIconRes, intent, null);
    }

    public static Notification createNotification(Context context, String title, String content, int smallIconRes, PendingIntent intent, RemoteViews remoteViews) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_ONE, NOTIFICATION_CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_ONE);
        builder.setContentTitle(title).setContentText(content).setSmallIcon(smallIconRes).setWhen(System.currentTimeMillis());
        if (intent != null) {
            builder.setContentIntent(intent);
        }
        if (remoteViews != null) {
            builder.setCustomContentView(remoteViews);
            builder.setCustomBigContentView(remoteViews);
            builder.setCustomHeadsUpContentView(remoteViews);
        }
        return builder.build();
    }

    public static void updateNotification(Context context, int notifyID, Notification notification) {
        if (null == context || null == notification) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(notifyID, notification);
        }
    }

    public static void cancelNotification(Context context, int notifyID) {
        if (null == context) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notifyID);
        }
    }

    public static int getNotificationFlag(String packageName) {
        if (null == packageName) return 0;
        int flag = MessageInfo.FLAG_DEFAULT;
        switch (packageName) {
            case PACKAGE_NAME_SYS_MESSAGE:
                flag = MessageInfo.FLAG_SMS;
                break;
            case PACKAGE_NAME_WECHAT:
                flag = MessageInfo.FLAG_WECHAT;
                break;
            case PACKAGE_NAME_QQ:
                flag = MessageInfo.FLAG_QQ;
                break;
            case PACKAGE_NAME_DING_TALK:
                flag = MessageInfo.FLAG_DING_TALK;
                break;
        }
        return flag;
    }

    /**
     * 转换消息信息
     *
     * @param sbn 状态栏通知
     * @return 消息信息
     */
    public static MessageInfo convertMessageInfo(StatusBarNotification sbn) {
        if (null == sbn) return null;
        String packageName = sbn.getPackageName();
        Notification notification = sbn.getNotification();
        Bundle bundle = notification.extras;
        CharSequence title = bundle.getCharSequence(Notification.EXTRA_TITLE);//通知title
        CharSequence content = bundle.getCharSequence(Notification.EXTRA_TEXT); //通知内容
        CharSequence bigText = null; //长文本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bigText = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }
        CharSequence subContent = bundle.getCharSequence(Notification.EXTRA_SUB_TEXT); //通知内容子内容
        CharSequence externalContent = bundle.getCharSequence(Notification.EXTRA_INFO_TEXT); //额外信息
        CharSequence tickerText = notification.tickerText;
        long time = notification.when;
        int flags = notification.flags;
        String contentStr = getStr(content);
        String bigString = getStr(bigText);
        if (contentStr == null) {
            contentStr = bigString;
        } else {
            if (null != bigString && bigString.length() > contentStr.length()) {
                contentStr = bigString;
            }
        }
        if (contentStr == null) {
            contentStr = getStr(tickerText);
        }
        String category = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            category = notification.category;
        }
        JL_Log.i(TAG, "convertMessageInfo", CommonUtil.formatString("packageName : %s, title : %s," +
                        "\ncontent : %s\nsubContent : %s,\nexternalContent : %s\n, bigText: %s," +
                        "\ntickerText : %s\ntime : %s,\tcategory : %s,\tflags : %d",
                packageName, title, content, subContent, externalContent, bigText, tickerText, time, category, flags));
        if (TextUtils.isEmpty(contentStr)) return null;
        if (category == null || category.equals(Notification.CATEGORY_MESSAGE)) { //只关心通知信息
            contentStr = formatContent(packageName, contentStr);
            return new MessageInfo(packageName,
                    getNotificationFlag(packageName),
                    getStr(title), contentStr, time);
        }
        return null;
    }

    public static boolean isAllowNotificationApp(String mac, String packageName) {
        if (null == packageName || null == mac) return false;
        final DeviceSettings settings = ConfigureKit.getInstance().getDeviceSettings(mac);
        if (null == settings || !settings.isSyncMessage()) return false;
        List<String> packetNameList = settings.getAppPacketNameList();
        if (null == packetNameList || packetNameList.isEmpty()) return true;
        return packetNameList.contains(packageName);
    }

    private static String getStr(CharSequence charSequence) {
        if (charSequence == null) return null;
        return charSequence.toString();
    }

    private static String formatContent(String packageName, String content) {
        if (null == packageName) return content;
        if (PACKAGE_NAME_WECHAT.equals(packageName)) {
            String pattern = "(\\[)(.+?)(\\])(.+?):";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(content);
            if (m.find()) {
                String sub = m.group();
                content = content.substring(sub.length());
                JL_Log.i(TAG, "formatContent", "find sub : " + sub + ", content = " + content);
            } else {
                JL_Log.i(TAG, "formatContent", "not find.");
            }
        }
        return content;
    }


}
