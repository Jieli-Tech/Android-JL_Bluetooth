package com.jieli.btsmart.ui.chargingCase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.utils.CryptoUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.bmp_convert.BmpConvert;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.FileUtil;
import com.jieli.lib.gif.GifConverter;
import com.jieli.lib.gif.model.GifConvertResult;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 确认屏幕保护程序逻辑实现
 * @since 2024/1/26
 */
public class ConfirmScreenSaversViewModel extends BtBasicVM {
    /**
     * 图像转码
     */
    private final BmpConvert mBmpConvert;
    /**
     * GIF转码
     */
    private final GifConverter mGifConverter;
    /**
     * 工作线程
     */
    private final ExecutorService mThreadTool = Executors.newSingleThreadExecutor();

    public final MutableLiveData<StateResult<ConvertResult>> convertStateMLD = new MutableLiveData<>();

    public ConfirmScreenSaversViewModel() {
        mBmpConvert = new BmpConvert();
        mGifConverter = GifConverter.getInstance();
    }

    @Override
    protected void release() {
        mBmpConvert.release();
        mThreadTool.shutdownNow();
        super.release();
    }

    public void convertBinFile(@NonNull Context context, String inFilePath, int width, int height, boolean isMerge) {
        if (convertStateMLD.getValue() != null && convertStateMLD.getValue().getState() == StateResult.STATE_WORKING) {
            return;
        }
        convertStateMLD.postValue(new StateResult<ConvertResult>().setState(StateResult.STATE_WORKING).setCode(0));
        mThreadTool.submit(() -> {
            String inputPath = inFilePath;
            final String dirPath = inputPath.substring(0, inputPath.lastIndexOf("/"));
            String outputPath = dirPath + File.separator + SConstant.DIR_OUTPUT;
            File outputDir = new File(outputPath);
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                JL_Log.w(tag, "convertBinFile", "create dir failure. outputPath  = " + outputPath);
                outputPath = dirPath;
            }
            if (AppUtil.isGif(inputPath)) { //GIF处理
                GifConvertResult result = mGifConverter.gif2Bin(inputPath, GifConverter.MODE_LOW_COMPRESSION_RATE);
                JL_Log.d(tag, "convertBinFile", "gif2Bin : " + result + ", inputPath = " + inputPath);
                if (result.isSuccess()) {
                    byte[] binData = result.getGifBin().getData();
                    outputPath += File.separator + AppUtil.getNameNoSuffix(AppUtil.getFileName(inputPath)) + ".RES";
                    if (FileUtil.bytesToFile(binData, outputPath)) {
                        short crc = CryptoUtil.CRC16(binData, (short) 0);
                        JL_Log.d(tag, "convertBinFile", "crc  = " + crc + ", outputPath = " + outputPath);
                        convertStateMLD.postValue(new StateResult<ConvertResult>().setState(StateResult.STATE_FINISH).setCode(0)
                                .setData(new ConvertResult(inputPath, outputPath)
                                        .setWidth(result.getGifBin().getWidth())
                                        .setHeight(result.getGifBin().getHeight())
                                        .setOutFileCrc(crc)));
                        return;
                    }
                    convertStateMLD.postValue(new StateResult<ConvertResult>().setState(StateResult.STATE_FINISH)
                            .setCode(ChargingCaseSettingViewModel.ERR_SAVE_FILE)
                            .setMessage("Failed to save output file. path = " + outputPath));
                    return;
                }
                convertStateMLD.postValue(new StateResult<ConvertResult>().setState(StateResult.STATE_FINISH)
                        .setCode(result.getCode())
                        .setMessage(AppUtil.formatString("Failed to convert file. path = %s. code = %d, %s.",
                                outputPath, result.getCode(), result.getMessage())));
                return;
            }
            //图像处理
            if (isMerge) {
                Bitmap srcBmp = BitmapFactory.decodeFile(inputPath);
                Bitmap bitmap = mergeBitmap(context, srcBmp, R.drawable.bg_screen_unlock_white, width, height);
                String filename = ChargingCaseSettingViewModel.formatFileName(inputPath);
                inputPath = outputPath + File.separator + filename;
                JL_Log.d(tag, "convertBinFile", "filename = " + filename + ", filePath = " + inFilePath + ", outputPath = " + inputPath);
                if (!FileUtil.bitmapToFile(bitmap, inputPath, 100)) {
                    convertStateMLD.postValue(new StateResult<ConvertResult>().setState(StateResult.STATE_FINISH)
                            .setCode(ChargingCaseSettingViewModel.ERR_SAVE_FILE)
                            .setMessage("Failed to save merge bitmap. path = " + inputPath));
                    return;
                }
            }
            outputPath += File.separator + AppUtil.getNameNoSuffix(AppUtil.getFileName(inputPath)) + ".RES";
            int ret = mBmpConvert.bitmapConvertBlock(BmpConvert.TYPE_BR_28, inputPath, outputPath);
            JL_Log.d(tag, "convertBinFile", "bitmapConvertBlock : " + ret + ", filePath = " + inputPath + ", outputPath = " + outputPath);
            if (ret > 0) {
                byte[] outputData = FileUtil.getBytes(outputPath);
                short crc = CryptoUtil.CRC16(outputData, (short) 0);
                convertStateMLD.postValue(new StateResult<ConvertResult>().setState(StateResult.STATE_FINISH).setCode(0)
                        .setData(new ConvertResult(inputPath, outputPath)
                                .setWidth(width)
                                .setHeight(height)
                                .setOutFileCrc(crc)));
            } else {
                if (ret == 0) ret = ChargingCaseSettingViewModel.ERR_NONE_DATA;
                convertStateMLD.postValue(new StateResult<ConvertResult>().setState(StateResult.STATE_FINISH)
                        .setCode(ret)
                        .setMessage(AppUtil.formatString("Failed to convert file. path = %s. code = %d.",
                                outputPath, ret)));
            }
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

    /**
     * 转码结果
     */
    public static class ConvertResult implements Parcelable {
        private String inputFilePath;
        private final String outputFilePath;
        private int width;
        private int height;
        private short outFileCrc;

        public ConvertResult(String inputFilePath, String outputFilePath) {
            this.inputFilePath = inputFilePath;
            this.outputFilePath = outputFilePath;
        }

        protected ConvertResult(Parcel in) {
            inputFilePath = in.readString();
            outputFilePath = in.readString();
            width = in.readInt();
            height = in.readInt();
            outFileCrc = (short) in.readInt();
        }

        public static final Creator<ConvertResult> CREATOR = new Creator<ConvertResult>() {
            @Override
            public ConvertResult createFromParcel(Parcel in) {
                return new ConvertResult(in);
            }

            @Override
            public ConvertResult[] newArray(int size) {
                return new ConvertResult[size];
            }
        };

        public String getInputFilePath() {
            return inputFilePath;
        }

        public String getOutputFilePath() {
            return outputFilePath;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public short getOutFileCrc() {
            return outFileCrc;
        }

        public ConvertResult setInputFilePath(String inputFilePath) {
            this.inputFilePath = inputFilePath;
            return this;
        }

        public ConvertResult setWidth(int width) {
            this.width = width;
            return this;
        }

        public ConvertResult setHeight(int height) {
            this.height = height;
            return this;
        }

        public ConvertResult setOutFileCrc(short outFileCrc) {
            this.outFileCrc = outFileCrc;
            return this;
        }

        @Override
        public String toString() {
            return "ConvertResult{" +
                    "inputFilePath='" + inputFilePath + '\'' +
                    ", outputFilePath='" + outputFilePath + '\'' +
                    ", width=" + width +
                    ", height=" + height +
                    ", outFileCrc=" + outFileCrc +
                    '}';
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(inputFilePath);
            dest.writeString(outputFilePath);
            dest.writeInt(width);
            dest.writeInt(height);
            dest.writeInt(outFileCrc);
        }
    }

}
