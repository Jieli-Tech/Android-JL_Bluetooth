package com.jieli.btsmart.demo;

import android.content.Context;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.BuildConfig;

import org.junit.Test;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc Debug测试
 * @since 2022/4/13
 */
public class DebugDemo {

    public void configDebug(Context context) {
        //log配置
        //context    --- 上下文，建议是getApplicationContext()
        //log        --- 是否输出打印，建议是开发时打开，发布时关闭
        //isSaveFile --- 是否保存log文件，建议是开发时打开，发布时关闭
        JL_Log.configureLog(context, BuildConfig.DEBUG, BuildConfig.DEBUG);
    }

    @Test
    public void multiLibsDebug() {
        com.jieli.jl_bt_ota.util.JL_Log.setLog(JL_Log.isLog());
        if(JL_Log.isLog()){
            com.jieli.jl_bt_ota.util.JL_Log.setLogOutput(new com.jieli.jl_bt_ota.util.JL_Log.ILogOutput() {
                @Override
                public void output(String logcat) {
                    JL_Log.i("ota", logcat);
                }
            });
        }
    }
}
