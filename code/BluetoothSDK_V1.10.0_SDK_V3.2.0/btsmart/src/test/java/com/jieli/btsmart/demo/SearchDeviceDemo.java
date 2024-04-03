package com.jieli.btsmart.demo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.base.CommandBase;
import com.jieli.bluetooth.bean.command.search_dev.SearchDevCmd;
import com.jieli.bluetooth.bean.parameter.SearchDevParam;
import com.jieli.bluetooth.bean.response.SearchDevStatusResponse;
import com.jieli.bluetooth.constant.Constants;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.impl.JL_BluetoothManager;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.bluetooth.RcspCommandCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.BluetoothUtil;
import com.jieli.bluetooth.utils.CommandBuilder;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.model.basic.CombineData;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 查找设备测试
 * @since 2021/12/2
 */
public class SearchDeviceDemo {

    @Test
    public void searchDevice(int way) {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //way : 0 -- all  1 -- left  2 -- right
        //执行搜索设备功能并等待结果回调
        controller.searchDev(controller.getUsingDevice(), Constants.RING_OP_OPEN, 60, way, Constants.RING_PLAYER_APP, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    @Test
    public void searchPhone() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //注册蓝牙RCSP事件监听器
        controller.addBTRcspEventCallback(new BTRcspEventCallback() {
            @Override
            public void onSearchDevice(BluetoothDevice device, SearchDevParam searchDevParam) {
                if (searchDevParam.getOp() == Constants.RING_OP_OPEN) { //open ring
                    int timeout = searchDevParam.getTimeoutSec();//timeout, unit : second
                    int player = searchDevParam.getPlayer(); //player (0 --- app play ring  1 --- device play ring)
                } else {
                    //close ring
                }
            }
        });
    }

    @Test
    public void stopSearch() {
        //获取RCSPController对象
        RCSPController controller = RCSPController.getInstance();
        //执行停止搜索设备功能并等待结果回调
        controller.stopSearchDevice(controller.getUsingDevice(), new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                //成功回调
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void syncSearchDeviceStatus() {
        //获取RCSPController对象
        final RCSPController controller = RCSPController.getInstance();
        //获取已连接和正在使用的设备
        final BluetoothDevice device = controller.getUsingDevice();
        //执行同步查找设备状态功能并等待回调
        controller.syncSearchDeviceStatus(device, new OnRcspActionCallback<SearchDevStatusResponse>() {
            @Override
            public void onSuccess(BluetoothDevice device, SearchDevStatusResponse message) {
                //成功回调
                //message -- 查询设备状态
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }

    public void syncSearchDeviceStatusV0(Context context) {
        //Step0: 获取JL_BluetoothManager对象
        final JL_BluetoothManager manager = JL_BluetoothManager.getInstance(context);
        //获取已连接和正在使用的设备
        final BluetoothDevice device = manager.getConnectedDevice();
        //Step1: 构建命令 --- 查询查找设备状态
        CommandBase searchDevStatus = CommandBuilder.buildSearchDevStatusCmd();
        //Step2: 执行查询查找设备状态功能并等待结果回调
        manager.sendRcspCommand(device, searchDevStatus, new RcspCommandCallback() {
            @Override
            public void onCommandResponse(BluetoothDevice device, CommandBase cmd) {
                //Step3: 检查设备状态
                if (cmd.getStatus() != StateCode.STATUS_SUCCESS) { //设备状态异常，进行异常处理
                    onErrCode(device, new BaseError(ErrorCode.SUB_ERR_RESPONSE_BAD_STATUS, "Device reply an bad status: " + cmd.getStatus()));
                    return;
                }
                //成功回调
                SearchDevCmd searchDevCmd = (SearchDevCmd) cmd;
                SearchDevStatusResponse response = (SearchDevStatusResponse) searchDevCmd.getResponse();
                //response -- 查询设备状态
            }

            @Override
            public void onErrCode(BluetoothDevice device, BaseError error) {
                //失败回调
                //error - 错误信息
            }
        });
    }
}
