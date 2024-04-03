package com.jieli.btsmart.tool.product;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.jieli.bluetooth.bean.BleScanMessage;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.component.utils.FileUtil;
import com.jieli.jl_http.bean.ProductDesignMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Response;
/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/12/9 8:53 AM
 * @desc :资源文件下载
 */
class DownloadTaskInfo extends TaskInfo {
    ProductDesignMessage message;

    public DownloadTaskInfo(BleScanMessage bleScanMessage, ProductDesignMessage message) {
        super(bleScanMessage);
        this.message = message;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof DownloadTaskInfo)) return false;
        DownloadTaskInfo info = (DownloadTaskInfo) obj;
        return Util.toResKey(bleScanMessage, message.getScene()).equals(Util.toResKey(info.bleScanMessage, info.message.getScene()));
    }



    @Override
   protected void execute() throws IOException {
        download();
    }

    private void download() throws IOException {
        JL_Log.d(tag, "down json file json:  " + message);
        String fileName = message.getUrl().split("/")[message.getUrl().split("/").length - 1];
        String outPath = FileUtil.createFilePath(MainApplication.getApplication(), MainApplication.getApplication().getPackageName(), SConstant.DIR_DESIGN, "json", message.getScene())
                + File.separator + fileName + ".json";
        File file = new File(outPath);
        String key = Util.toResKey(bleScanMessage, message.getScene());
        //已下载直接返回true
        if (file.exists()) {
            message.setUrl(outPath);//将服务器链接修改为本地连接
            String value = new Gson().toJson(message);
            Util.save(key, value);
            return;
        }
        Response<ResponseBody> response = httpApi.downloadFileByUrl(message.getUrl()).execute();
        if (!response.isSuccessful()) {
            throw new IOException("服务器请求失败");
        }
        message.setUrl(outPath);
        if(null == response.body()) return;
        boolean ret = writeDataToFile(response.body().byteStream(), outPath);
        if (!ret) {
            JL_Log.e(tag, "保存json文件失败--->" + ret);
            throw new IOException("文件数据写入失败");
        }
        message.setUrl(outPath);//将服务器链接修改为本地连接
        String value = new Gson().toJson(message);
        Util.save(key, value);
        taskManager.getListenerHandler().onJsonUpdate(bleScanMessage, outPath);
    }

    private boolean writeDataToFile(InputStream is, String path) throws IOException {
        JL_Log.d(tag, "writeDataToFile " + path);
        boolean ret = false;
        byte[] buf = new byte[1024 * 4];
        int len = -1;
        File file = new File(path);
        file.createNewFile();
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
            ret = true;
        } catch (IOException e) {
            file.delete();
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return ret;
    }
}