package com.jieli.btsmart.demo.auracast;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.bean.auracast.AuracastBroadcast;
import com.jieli.bluetooth.bean.auracast.AuracastRecord;
import com.jieli.bluetooth.bean.auracast.ScanOption;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.impl.rcsp.auracast.AuracastAssistant;
import com.jieli.bluetooth.impl.rcsp.auracast.AuracastRecordHelper;
import com.jieli.bluetooth.interfaces.rcsp.auracast.receiver.AuracastReceiverCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.btsmart.MainApplication;

import java.util.List;

/**
 * AuracastReceiverDemo
 *
 * @author zhongzhuocheng
 * email: zhongzhuocheng@zh-jieli.com
 * create: 2025/12/22
 * note: Auracast接收端功能演示
 */
public class AuracastReceiverDemo {


    private AuracastAssistant auracastAssistant;

    public void init() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        //上下文
        Context context = MainApplication.getApplication();
        //初始化AuracastAssistant对象
        auracastAssistant = new AuracastAssistant(context, controller.getRcspOp(), usingDevice);
        //设备是否支持Auracast功能
        auracastAssistant.isSupportAuracast();
    }


    public void destroy() {
        if (null == auracastAssistant) return;
        //销毁对象
        auracastAssistant.destroy();
    }

    public boolean isSupportAuracastReceiver() {
        return null != auracastAssistant && auracastAssistant.isSupportAuracastReceiver();
    }

    public void getListeningSource() {
        if (!isSupportAuracastReceiver()) return; //不支持Auracast接收端功能
        auracastAssistant.requestListeningSource(new OnRcspActionCallback<AuracastBroadcast>() {
            @Override
            public void onSuccess(BluetoothDevice device, AuracastBroadcast message) {
                //回调操作成功
                //message --- 正在操作的Auracast广播信息。 null表示不存在
                //广播状态会通过 AuracastReceiverCallback#onBroadcastState 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
            }
        });
    }

    public boolean isScanningAuracastBroadcast() {
        return null != auracastAssistant && auracastAssistant.isScanning();
    }

    public void checkScanAuracastStatus() {
        if (!isSupportAuracastReceiver()) return; //不支持Auracast接收端功能
        //执行查询搜索广播状态
        auracastAssistant.checkScanStatus(new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                // message --- 搜索状态
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
            }
        });
    }

    public void startScanAuracastBroadcast() {
        if (!isSupportAuracastReceiver()) return; //不支持Auracast接收端功能
        if (isScanningAuracastBroadcast()) return; //正在搜索
        final AuracastReceiverCallback receiverCallback = new AuracastReceiverCallback() {
            @Override
            public void onSearchStarted(int reason) {
                //回调搜索开始
                //reason --- 原因
                //  - Constants.REASON_BY_SDK       --- SDK触发
                //  - Constants.REASON_BY_DEVICE    --- 设备触发
            }

            @Override
            public void onSearchStopped(int reason) {
                //回调搜索结束
                //reason --- 原因
                //  - Constants.REASON_BY_SDK       --- SDK触发
                //  - Constants.REASON_BY_DEVICE    --- 设备触发

                //获取所有搜索到的广播
                List<AuracastBroadcast> broadcasts = auracastAssistant.getDiscoveredBroadcast();
                //移除接收器事件监听
                auracastAssistant.removeAuracastReceiverCallback(this);
            }

            @Override
            public void onBroadcastFound(@NonNull AuracastBroadcast broadcast) {
                //回调搜索到的Auaracast广播信息
            }
        };
        //注册接收端事件监听器
        auracastAssistant.addAuracastReceiverCallback(receiverCallback);
        //搜索配置信息
        //timeout -- 超时时间, 单位是毫秒，默认是 48秒
        //isFilterBroadcast -- 是否过滤重复广播信息，默认是 true
        ScanOption option = new ScanOption();
        //执行搜索Auracast广播
        auracastAssistant.startScan(option, new OnRcspActionCallback<Integer>() {
            @Override
            public void onSuccess(BluetoothDevice device, Integer message) {
                //回调操作成功
                // message --- 操作结果
                // - ScanResponse.RESULT_SUCCESS --- 操作成功/不在搜索状态
                // - ScanResponse.RESULT_SCANNING --- 正在搜索，请勿重复操作
                // - ScanResponse#RESULT_LISTENING_BROADCAST_BAN_SCAN --- 收听广播过程不允许扫描
                // - ScanResponse#RESULT_DEVICE_BUSY --- 设备繁忙
                //搜索状态和搜索设备结果将在 AuracastReceiverCallback#onSearchStarted,
                // AuracastReceiverCallback#onBroadcastFound 和 AuracastReceiverCallback#onSearchStopped 返回
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
                auracastAssistant.removeAuracastReceiverCallback(receiverCallback);
            }
        });
    }

    public void stopScanAuracastBroadcast() {
        if (!isSupportAuracastReceiver()) return; //不支持Auracast接收端功能
        if (!isScanningAuracastBroadcast()) return; //不在搜索状态
        final AuracastReceiverCallback receiverCallback = new AuracastReceiverCallback() {
            @Override
            public void onSearchStopped(int reason) {
                //回调搜索结束
                //reason --- 原因
                //  - Constants.REASON_BY_SDK       --- SDK触发
                //  - Constants.REASON_BY_DEVICE    --- 设备触发
                auracastAssistant.removeAuracastReceiverCallback(this);
            }
        };
        //注册接收端事件监听器
        auracastAssistant.addAuracastReceiverCallback(receiverCallback);
        //执行停止搜索Auracast广播
        auracastAssistant.stopScan(new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //搜索状态将在 AuracastReceiverCallback#onSearchStopped 返回
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
                auracastAssistant.removeAuracastReceiverCallback(receiverCallback);
            }
        });
    }

    public AuracastBroadcast getListeningBroadcast() {
        if (null == auracastAssistant) return null;
        //获取正在收听的广播信息
        return auracastAssistant.getListeningBroadcast();
    }

    public void addSource() {
        if (!isSupportAuracastReceiver()) return; //不支持Auracast接收端功能
        stopScanAuracastBroadcast(); //建议先关闭搜索广播
        final AuracastBroadcast addBroadcast = new AuracastBroadcast(); //选择的广播信息
        if (addBroadcast.isEncrypted()) { //如果广播是解密的, 需要增加广播密钥
            addBroadcast.setBroadcastCode(new byte[16]);
        }
        final AuracastReceiverCallback receiverCallback = new AuracastReceiverCallback() {
            @Override
            public void onBroadcastState(@NonNull BluetoothDevice device, @NonNull AuracastBroadcast broadcast) {
                //回调广播状态变化
                if (addBroadcast.equals(broadcast)) {
                    if (broadcast.getSyncState() != StateCode.STATE_SYNCING) {
                        auracastAssistant.removeAuracastReceiverCallback(this);
                    }
                }
            }
        };
        //注册接收端事件监听器
        auracastAssistant.addAuracastReceiverCallback(receiverCallback);
        //执行添加音频的功能
        auracastAssistant.addSource(addBroadcast, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //音频广播同步状态，将在 AuracastReceiverCallback#onBroadcastState 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
                auracastAssistant.removeAuracastReceiverCallback(receiverCallback);
            }
        });
    }

    public void removeSource() {
        if (!isSupportAuracastReceiver()) return; //不支持Auracast接收端功能
        final AuracastBroadcast broadcast = getListeningBroadcast(); //获取正在收听的广播信息
        if (null == broadcast) return; //不在收听音乐状态
        final AuracastReceiverCallback receiverCallback = new AuracastReceiverCallback() {
            @Override
            public void onBroadcastState(@NonNull BluetoothDevice device, @NonNull AuracastBroadcast broadcast) {
                //回调广播状态变化
                if (broadcast.getSyncState() != StateCode.STATE_SYNCING) {
                    auracastAssistant.removeAuracastReceiverCallback(this);
                }
            }
        };
        //注册接收端事件监听器
        auracastAssistant.addAuracastReceiverCallback(receiverCallback);
        //执行移除音频的功能
        auracastAssistant.removeSource(new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //回调操作成功
                //音频广播同步状态，将在 AuracastReceiverCallback#onBroadcastState 回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //回调操作失败
                //error --- 错误信息
                auracastAssistant.removeAuracastReceiverCallback(receiverCallback);
            }
        });
    }

    public void observerRecordChange(){
        if (!isSupportAuracastReceiver()) return; //不支持Auracast接收端功能
        final  AuracastReceiverCallback receiverCallback = new AuracastReceiverCallback() {
            @Override
            public void onRecordChange(int op, AuracastRecord record) {
                //回调记录改变
                //op --- 操作码
                //   - AuracastRecordHelper.OP_ADD --- 添加操作
                //   - AuracastRecordHelper.OP_MODIFY --- 修改操作
                //   - AuracastRecordHelper.OP_DELETE --- 删除操作
                //   - AuracastRecordHelper.OP_CLEAR --- 清除操作
                //record --- 历史记录
            }
        };
        //注册接收端事件监听器
        auracastAssistant.addAuracastReceiverCallback(receiverCallback);
        //不再需要监听时，记得移除监听器
        //        auracastAssistant.removeAuracastRecords(receiverCallback);
    }

    public void getAuracastRecords() {
        if (null == auracastAssistant) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        //获取Auracast广播历史记录
        List<AuracastRecord> records = auracastAssistant.getAuracastRecords(usingDevice.getAddress());
    }

    public void findAuracastRecord(AuracastBroadcast broadcast) {
        if (null == auracastAssistant) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        //查找对应的Auracast广播历史记录
        AuracastRecord record = auracastAssistant.findAuracastRecord(usingDevice.getAddress(), broadcast);
    }


    public void removeAuracastRecord(AuracastRecord record) {
        if (null == auracastAssistant) return;
        //移除对应的Auracast广播历史记录
        auracastAssistant.removeAuracastRecord(record);
    }

    public void removeAuracastRecords() {
        if (null == auracastAssistant) return;
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取当前操作设备
        BluetoothDevice usingDevice = controller.getUsingDevice();
        if (null == usingDevice) return;
        //移除设备对应的所有Auracast广播记录
        auracastAssistant.removeAuracastRecords(usingDevice.getAddress());
    }
}
