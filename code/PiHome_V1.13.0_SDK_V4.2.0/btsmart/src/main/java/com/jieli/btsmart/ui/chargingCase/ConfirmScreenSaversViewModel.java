package com.jieli.btsmart.ui.chargingCase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.chargingcase.ResourceMsg;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.FileUtil;

import java.io.File;
import java.util.concurrent.Executors;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 确认屏幕保护程序逻辑实现
 * @since 2024/1/26
 */
public class ConfirmScreenSaversViewModel extends BtBasicVM {

    /**
     * 资源信息
     */
    @NonNull
    private final ResourceMsg resourceMsg;

    /**
     * 转码状态回调
     */
    public final MutableLiveData<StateResult<Boolean>> convertStateMLD = new MutableLiveData<>();

    public ConfirmScreenSaversViewModel(@NonNull ResourceMsg resourceMsg) {
        this.resourceMsg = resourceMsg;
    }

    @Override
    protected void release() {
        super.release();
    }

    public int getState() {
        final StateResult<Boolean> result = convertStateMLD.getValue();
        if (null == result) return StateResult.STATE_IDLE;
        return result.getState();
    }

    @NonNull
    public ResourceMsg getResourceMsg() {
        return resourceMsg;
    }

    public boolean isWorking() {
        return getState() == StateResult.STATE_WORKING;
    }

    public void convertUploadFile(boolean isMerge) {
        if (isWorking()) {
            JL_Log.i(tag, "convertUploadFile", "Converting...");
            return;
        }
        convertStateMLD.postValue(new StateResult<Boolean>().setState(StateResult.STATE_WORKING).setCode(0));
        Executors.newSingleThreadExecutor().submit(() -> {
            final String inFilePath = resourceMsg.getSrcFilePath();
            final int width = resourceMsg.getScreenWidth();
            final int height = resourceMsg.getScreenHeight();
            if (ChargingBinUtil.isGif(inFilePath)) { //GIF处理
                final String outputPath = ChargingBinUtil.findGifPath(inFilePath, isMerge);
                resourceMsg.setSrcFilePath(outputPath);
                convertStateMLD.postValue(new StateResult<Boolean>().setState(StateResult.STATE_FINISH)
                        .setCode(0).setData(true));
                return;
            }
            //图像处理
            String outputPath = ChargingBinUtil.getOutputDir(ChargingBinUtil.getFolderPathByPath(inFilePath));
            if (isMerge) {
                Bitmap srcBmp = BitmapFactory.decodeFile(inFilePath);
                Bitmap bitmap = mergeBitmap(getContext(), srcBmp, R.drawable.bg_screen_unlock_white, width, height);
                String filename = ChargingBinUtil.formatFileName(inFilePath);
                outputPath = outputPath + File.separator + filename;
                JL_Log.d(tag, "convertUploadFile", "mergeBitmap ---> filename = " + filename
                        + ", inFilePath : " + inFilePath + ",\n outputPath : " + outputPath);
                if (!FileUtil.bitmapToFile(bitmap, outputPath, 100)) {
                    convertStateMLD.postValue(new StateResult<Boolean>().setState(StateResult.STATE_FINISH)
                            .setCode(ErrorCode.SUB_ERR_IO_EXCEPTION)
                            .setMessage("Failed to save bitmap. outputPath : " + outputPath));
                    return;
                }
            } else {
                outputPath = inFilePath;
            }
            resourceMsg.setSrcFilePath(outputPath);
            convertStateMLD.postValue(new StateResult<Boolean>().setState(StateResult.STATE_FINISH)
                    .setCode(0).setData(true));
        });
    }

    private Bitmap mergeBitmap(@NonNull Context context, @NonNull Bitmap srcBmp, int resId, int targetWidth, int targetHeight) {
        Bitmap logoBmp = BitmapFactory.decodeResource(context.getResources(), resId);
        Bitmap destBmp = Bitmap.createBitmap(srcBmp.getWidth(), srcBmp.getHeight(), srcBmp.getConfig());
        if (logoBmp.getWidth() > srcBmp.getWidth() || logoBmp.getHeight() > srcBmp.getHeight()) {
            logoBmp = Bitmap.createScaledBitmap(logoBmp, srcBmp.getWidth(), srcBmp.getHeight(), true);
        }
        Canvas canvas = new Canvas(destBmp);
        canvas.drawBitmap(srcBmp, 0, 0, null);
        int x = (srcBmp.getWidth() - logoBmp.getWidth()) / 2;
        int y = (srcBmp.getHeight() - logoBmp.getHeight()) / 2;
        Paint paintImage = new Paint();
        paintImage.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
//        Rect rect = new Rect(x, y, x + logoBmp.getWidth(), y + logoBmp.getHeight());
        JL_Log.d(tag, "mergeBitmap", "x = " + x + ", y = " + y + ", src width = " + srcBmp.getWidth() + ", scr height = " + srcBmp.getHeight()
                + ", logo width = " + logoBmp.getWidth() + ", logo height = " + logoBmp.getHeight());
        canvas.drawBitmap(logoBmp, x, y, paintImage);
        destBmp = Bitmap.createScaledBitmap(destBmp, targetWidth, targetHeight, true);
        return destBmp;
    }

    public static class Factory implements ViewModelProvider.Factory {
        @NonNull
        private final ResourceMsg resource;

        public Factory(@NonNull ResourceMsg resource) {
            this.resource = resource;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new ConfirmScreenSaversViewModel(resource);
        }
    }
}
