package com.jieli.btsmart.tool.product;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.jl_http.bean.JL_Response;
import com.jieli.jl_http.bean.ProductDesignMessage;
import com.jieli.jl_http.util.HttpCode;
import com.jieli.jl_http.util.HttpUtil;

import java.io.IOException;

import retrofit2.Response;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/12/9 8:53 AM
 * @desc :产品资源路径汇总文件下载
 */
class MessageTaskInfo extends TaskInfo {


    public MessageTaskInfo(BleScanMessage bleScanMessage) {
        super(bleScanMessage);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof MessageTaskInfo)) return false;
        return Util.toDesignKey(bleScanMessage).equals(Util.toDesignKey(((MessageTaskInfo) obj).bleScanMessage));
    }

    @Override
    protected void execute() throws IOException {
        requestMessage();
    }


    private void requestMessage() throws IOException {
        Response<String> response = httpApi.requestProductDesign(bleScanMessage.getUid(), bleScanMessage.getPid()).execute();
        if (!response.isSuccessful()) {
            throw new IOException("服务器请求失败");
        }
        JL_Response<ProductDesignMessage[]> jl_response = HttpUtil.parseJsonData(response.body(), ProductDesignMessage.class);
        JL_Log.d(tag, "获取到message -->>>" + jl_response+"\n"+bleScanMessage);
        if (jl_response.getCode() != HttpCode.HTTP_RESPONSE_CODE_OK && jl_response.getCode() != HttpCode.HTTP_RESPONSE_CODE_DEFAULT) {
            if (jl_response.getCode() == 500) {
                //这种状态的原因：pid、uid异常或者服务器没有上传文件,可以认为该任务已完成。
                return;
            } else {
                throw new IOException("服务器请求异常:code = " + jl_response.getCode() + "");//如果code == 500的时候，要么是pid/uid错误，要么是文件没有上传，可以忽略，再重试也没有用了
            }
        }
        Gson gson = new Gson();
        for (ProductDesignMessage message : jl_response.getData()) {
            //如果是json文件，则主动下载，图片资源依赖glide缓存
            if (message.getType().equalsIgnoreCase("json")) {
                taskManager.addTask(new DownloadTaskInfo(bleScanMessage, message));
            } else {
                String json = gson.toJson(message);
                Util.save(Util.toResKey(bleScanMessage, message.getScene()), json);
            }
        }
        taskManager.getListenerHandler().onImageUrlUpdate(bleScanMessage);
    }
}