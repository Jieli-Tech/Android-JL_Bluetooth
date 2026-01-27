package com.jieli.btsmart.demo.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.bean.BluetoothOption;
import com.jieli.bluetooth.constant.BluetoothConstant;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.ParseDataUtil;

/**
 * ScanSettingDemo
 *
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 搜索设备设置示例代码
 * @since 2024/12/9
 */
class ScanSettingDemo {

    void setNoFilterScan() {
        BluetoothOption option = BluetoothOption.createDefaultOption();
        option.setBleScanStrategy(BluetoothConstant.NONE_FILTER); //设置无过滤规则
//        option.setSkipNoNameDev(false); //是否跳过无名称的设备
        //配置SDK参数
//        Context context = null;
//        RCSPController.init(context, option);
        RCSPController.getInstance().configure(option);
    }


    void filterDeviceByOtherWay() {
        BluetoothOption option = BluetoothOption.createDefaultOption()
                .setBleScanStrategy(BluetoothConstant.NONE_FILTER) //设置无过滤规则
                .setSkipNoNameDev(false); //是否跳过无名称的设备
        //配置SDK参数
//        Context context = null;
//        RCSPController.init(context, option);
        RCSPController.getInstance().configure(option);

        //添加事件监听
        RCSPController.getInstance().addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onDiscoveryStatus(boolean bBle, boolean bStart) {
                //回调搜索状态
                if (!bStart) {
                    RCSPController.getInstance().removeBTRcspEventCallback(this);
                }
            }

            @Override
            public void onDiscovery(BluetoothDevice device, BleScanMessage bleScanMessage) {
                if (null == bleScanMessage) return;
                final byte[] rawData = bleScanMessage.getRawData(); //广播数据
                //TODO: 自行实现过滤规则
                //搜索杰理设备
                BluetoothOption bluetoothOption = BluetoothOption.createDefaultOption();
                BleScanMessage message = ParseDataUtil.isFilterBleDevice(bluetoothOption, rawData);
                if(null == message){
                    //解析其他设备数据
                }
            }
        });
        //执行扫描BLE设备
        RCSPController.getInstance().startBleScan(30 * 1000);
    }
}
