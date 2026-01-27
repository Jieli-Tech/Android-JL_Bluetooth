package com.jieli.btsmart.ui.chargingCase;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.charging_case.ChargingCaseInfo;
import com.jieli.bluetooth.bean.file.op.CreateFileParam;
import com.jieli.bluetooth.bean.settings.v0.SDKMessage;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.impl.rcsp.file.FileOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnTaskStateListener;
import com.jieli.bluetooth.utils.CommonUtil;
import com.jieli.bluetooth.utils.CryptoUtil;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.bmp_convert.BmpConvert;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.data.model.chargingcase.ResourceMsg;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.ChargingBinUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.FileUtil;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.lib.gif.GifConverter;
import com.jieli.lib.gif.model.GifConvertResult;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 上传资源逻辑实现
 * @since 2023/12/8
 */
public class UploadResourceViewModel extends BtBasicVM {

    /**
     * 空闲状态
     */
    public static final int STATE_IDLE = 0;
    /**
     * 转码状态
     */
    public static final int STATE_CONVERTING = 1;
    /**
     * 传输状态
     */
    public static final int STATE_WORKING = 2;
    /**
     * 传输被取消状态
     */
    public static final int STATE_CANCEL = 3;
    /**
     * 传输停止状态
     */
    public static final int STATE_STOP = 4;

    /**
     * 二进制文件后缀名
     */
    private static String getBinFileSuffix(int chip) {
        if (chip == SDKMessage.CHIP_701N) {
            return ".RES";
        }
        return "";
    }


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

    /**
     * 资源信息
     */
    @NonNull
    private final ResourceMsg resourceMsg;
    /**
     * 文件操作功能实现
     */
    @NonNull
    private final FileOpImpl mFileOp;
    /**
     * 彩屏仓功能实现
     */
    @NonNull
    private final ChargingCaseOpImpl mChargingCaseOp;

    /**
     * 上传状态回调
     */
    public final MutableLiveData<StateResult<Integer>> uploadStateMLD = new MutableLiveData<>();

    /**
     * 是否准备取消上传资源
     */
    private boolean isReadyCancel;

    public UploadResourceViewModel(@NonNull ResourceMsg resourceMsg) {
        this.resourceMsg = resourceMsg;
        mBmpConvert = new BmpConvert();
        mGifConverter = GifConverter.getInstance();
        mChargingCaseOp = ChargingCaseOpImpl.instance(mRCSPController.getRcspOp());
        mFileOp = mChargingCaseOp.getFileOp();
    }

    public int getState() {
        StateResult<Integer> value = uploadStateMLD.getValue();
        if (null == value) return STATE_IDLE;
        return value.getState();
    }

    @NonNull
    public ResourceMsg getResourceMsg() {
        return resourceMsg;
    }

    /**
     * 是否正在上传资源
     *
     * @return boolean 结果
     */
    public boolean isWorking() {
        final int state = getState();
        return state == STATE_CONVERTING || state == STATE_WORKING;
    }

    /**
     * 上传资源
     * <p>
     * 流程步骤:<br/>
     * 1. 资源文件转码成特定的二进制文件<br/>
     * 2. 把二进制文件传输到设备<br/>
     * 3. 使能资源为正在使用资源<br/>
     * </p>
     */
    public void uploadResource() {
        if (isWorking()) {
            JL_Log.d(tag, "uploadResource", "Uploading...");
            return;
        }
        uploadStateMLD.setValue(new StateResult<Integer>().setState(STATE_CONVERTING).setCode(0).setData(0));
        final SDCardBean storage = getOnlineFlash();
        if (null == storage) {
            onUpdateError("uploadResource", ErrorCode.SUB_ERR_STORAGE_OFFLINE, ErrorCode.code2Msg(ErrorCode.SUB_ERR_STORAGE_OFFLINE));
            return;
        }
        JL_Log.d(tag, "uploadResource", "" + resourceMsg);
        transcodingResource(storage);
    }

    /**
     * 取消上传资源
     */
    public void cancelUpload() {
        if (!isWorking()) {
            JL_Log.d(tag, "cancelUpload", "Not working.");
            return;
        }
        final int state = getState();
        if (state == STATE_CONVERTING) {
            //取消
            isReadyCancel = true;
            uploadStateMLD.postValue(new StateResult<Integer>().setState(STATE_CANCEL).setCode(0));
            return;
        }
        mFileOp.cancelBigFileTransfer(getConnectedDevice(), 0, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                JL_Log.d(tag, "cancelUpload", "onSuccess : " + message);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.i(tag, "cancelUpload", "onError : " + error);
                if (null == error) return;
                onUpdateError("cancelUpload", error.getSubCode(), error.getMessage());
            }
        });
    }

    @Override
    protected void release() {
        mBmpConvert.release();
        mThreadTool.shutdownNow();
        if (isWorking()) {
            cancelUpload();
        }
        super.release();
    }

    private SDCardBean getOnlineFlash() {
        SDCardBean storage = null;
        final List<SDCardBean> onlineStorages = mChargingCaseOp.getOnlineStorages(getConnectedDevice());
        if (null != onlineStorages) {
            for (SDCardBean sdCardBean : onlineStorages) {
                if (sdCardBean.isFlash() && sdCardBean.isOnline()) {
                    storage = sdCardBean;
                    break;
                }
            }
        }
        return storage;
    }

    private void onUpdateError(String method, int code, String message) {
        if (!isWorking()) return;
        onTransferFinish(false);
        JL_Log.w(tag, "onUpdateError", CommonUtil.formatString("(%s) --> code : 0x%X(%d), message : %s.", method, code, code, message));
        uploadStateMLD.postValue(new StateResult<Integer>().setState(STATE_STOP)
                .setCode(code).setMessage(message));
    }

    private void onTransferFinish(boolean isUploadOk) {
        final String srcFilePath = resourceMsg.getSrcFilePath();
        final String binFilePath = resourceMsg.getBinFilePath();
        final int resourceType = resourceMsg.getResourceType();
        final short crc = resourceMsg.getBinFileCrc();
        JL_Log.d(tag, "onTransferFinish", CommonUtil.formatString("start... isUploadOk : %s, srcFilePath : %s," +
                        "\n binFilePath : %s, crc : 0x%X(%d), resourceType : %d.",
                isUploadOk, srcFilePath, binFilePath, crc, crc, resourceType));
        String outputDirPath = ChargingBinUtil.getFolderPathByPath(binFilePath);
        boolean isOutputDir = ChargingBinUtil.isOutputDir(outputDirPath);
        JL_Log.d(tag, "onTransferFinish", "isOutputDir : " + isOutputDir + ", outputDirPath : " + outputDirPath);
        boolean ret;
        if (ChargingBinUtil.isCustomResourceByPath(binFilePath)) { //自定义屏保处理
            String srcFileDirPath = isOutputDir ? ChargingBinUtil.getFolderPathByPath(outputDirPath) : outputDirPath;
            JL_Log.d(tag, "onTransferFinish", "srcFileDirPath : " + srcFileDirPath);
            File cropFile = new File(srcFileDirPath, ChargingBinUtil.getCropFileName(resourceType)); //裁剪文件
            if (isUploadOk) {
                if (!cropFile.exists() || !cropFile.isFile()) {
                    cropFile = new File(srcFilePath);
                }
                String oldFilePath = cropFile.getPath();
                String customName = ChargingBinUtil.getCustomName(resourceType);
                File newFile = new File(srcFileDirPath, AppUtil.formatString("%s-%x.jpeg", customName, crc));
                String newFilePath = newFile.getPath();
                JL_Log.d(tag, "onTransferFinish", "oldFilePath : " + oldFilePath + ",\n newFilePath : " + newFilePath);
                if (newFile.exists() && newFile.isFile()) {
                    ret = newFile.setLastModified(Calendar.getInstance().getTimeInMillis());
                    JL_Log.d(tag, "onTransferFinish", "Change File : " + ret);
                    resourceMsg.setSrcFilePath(newFilePath);
                } else {
                    ret = cropFile.renameTo(newFile);
                    JL_Log.d(tag, "onTransferFinish", "Rename File : " + ret);
                    if (ret) {
                        ret = newFile.setLastModified(Calendar.getInstance().getTimeInMillis());
                        JL_Log.d(tag, "onTransferFinish", "setLastModified : " + ret);
                        resourceMsg.setSrcFilePath(newFilePath);
                    }
                }
            } else { //传输被中止
                FileUtil.deleteFile(cropFile);
            }
        }
        if (isOutputDir) {
            FileUtil.deleteFile(new File(outputDirPath));
        } else {
            FileUtil.deleteFile(new File(binFilePath));
        }
        JL_Log.d(tag, "onTransferFinish", "end... srcFilePath : " + resourceMsg.getSrcFilePath());
    }

    /**
     * 转码资源
     * <p>
     * 说明: 把源文件转码为特殊编码的二进制文件
     * </p>
     *
     * @param storage SDCardBean 存储器对象
     */
    private void transcodingResource(@NonNull SDCardBean storage) {
        if (mThreadTool.isShutdown()) {
            onUpdateError("transcodingResource", ErrorCode.SUB_ERR_PARAMETER, "ThreadTool is dead.");
            return;
        }
        mThreadTool.submit(() -> {
            final String inFilePath = resourceMsg.getSrcFilePath();
            final String dirPath = ChargingBinUtil.getFolderPathByPath(inFilePath);
            final String srcFileName = ChargingBinUtil.getFileNameByPath(inFilePath);
            short crc;
            String outputPath = ChargingBinUtil.getOutputDir(dirPath);
            if (ChargingBinUtil.isGif(inFilePath)) { //Gif
                int chip = resourceMsg.getChip() == SDKMessage.CHIP_707N ? GifConverter.CHIP_707N : GifConverter.CHIP_701N;
                GifConvertResult result = mGifConverter.gif2Bin(inFilePath, GifConverter.MODE_LOW_COMPRESSION_RATE, chip);
                JL_Log.d(tag, "transcodingResource", "gif2Bin : " + result + ", inFilePath : " + inFilePath);
                if (!result.isSuccess()) { //GIF编码失败
                    onUpdateError("transcodingResource", ErrorCode.SUB_ERR_OP_FAILED,
                            AppUtil.formatString("Failed to convert file. \noutputPath : %s." + "\ncode : 0x%X(%d), message : %s.",
                                    outputPath, result.getCode(), result.getCode(), result.getMessage()));
                    return;
                }
                byte[] binData = result.getGifBin().getData();
                outputPath += File.separator + ChargingBinUtil.getNameNoSuffix(srcFileName) + getBinFileSuffix(resourceMsg.getChip());
                if (!FileUtil.bytesToFile(binData, outputPath)) {
                    onUpdateError("transcodingResource", ErrorCode.SUB_ERR_IO_EXCEPTION, "Failed to save output file. \noutputPath : " + outputPath);
                    return;
                }
                crc = CryptoUtil.CRC16(binData, (short) 0);
            } else {//图片
                if (ChargingBinUtil.isCustomResourceByPath(inFilePath)) {
                    outputPath += File.separator + ChargingBinUtil.getCustomName(resourceMsg.getResourceType()) + getBinFileSuffix(resourceMsg.getChip());
                } else {
                    outputPath += File.separator + ChargingBinUtil.getNameNoSuffix(srcFileName) + getBinFileSuffix(resourceMsg.getChip());
                }

                int type = resourceMsg.getChip() == SDKMessage.CHIP_707N ? BmpConvert.TYPE_707N_RGB : BmpConvert.TYPE_701N_RGB;
                int ret = mBmpConvert.bitmapConvertBlock(type, inFilePath, outputPath);
                JL_Log.d(tag, "transcodingResource", "bitmapConvertBlock : " + ret + ", type : " + type + ", \ninFilePath : " + inFilePath);
                if (ret <= 0) {
                    onUpdateError("transcodingResource", ErrorCode.SUB_ERR_OP_FAILED, AppUtil.formatString("Failed to convert file. \noutputPath : %s. code : %d.",
                            outputPath, ret));
                    return;
                }
                byte[] outputData = FileUtil.getBytes(outputPath);
                crc = CryptoUtil.CRC16(outputData, (short) 0);
            }
            JL_Log.d(tag, "transcodingResource", "crc : " + CommonUtil.formatInt(crc) + ", outputPath : " + outputPath);
            resourceMsg.setBinFilePath(outputPath).setBinFileCrc(crc);
            transferringResource(storage);
        });
    }

    /**
     * 传输资源
     * <p>
     * 说明: 把编码文件传输到设备，并使能
     * </p>
     *
     * @param storage SDCardBean 存储器对象
     */
    private void transferringResource(@NonNull SDCardBean storage) {
        if (!isWorking()) {
            JL_Log.d(tag, "transferringResource", "Not in work");
            if (isReadyCancel) {
                isReadyCancel = false;
                onTransferFinish(false);
            }
            return;
        }
        final BluetoothDevice device = getConnectedDevice();
        mFileOp.createBigFile(new CreateFileParam(device, new File(resourceMsg.getBinFilePath()), storage), new OnTaskStateListener() {
            @Override
            public void onStart() {
                uploadStateMLD.postValue(new StateResult<Integer>().setState(STATE_WORKING).setCode(0).setData(0));
            }

            @Override
            public void onProgress(int progress) {
                uploadStateMLD.postValue(new StateResult<Integer>().setState(STATE_WORKING).setCode(0).setData(progress));
            }

            @Override
            public void onStop() {
                setUsingResource(device, storage);
            }

            @Override
            public void onCancel(int reason) {
                onTransferFinish(false);
                uploadStateMLD.postValue(new StateResult<Integer>().setState(STATE_CANCEL).setCode(0));
            }

            @Override
            public void onError(int code, String message) {
                onUpdateError("transferResource", code, message);
            }
        });
    }

    /**
     * 设置正在使用的资源
     *
     * @param device  BluetoothDevice 操作设备
     * @param storage SDCardBean 存储对象
     */
    private void setUsingResource(BluetoothDevice device, @NonNull SDCardBean storage) {
        final File file = new File(resourceMsg.getBinFilePath());
        String deviceFilePath = "/" + file.getName();
        final OnRcspActionCallback<Boolean> callback = new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                onTransferFinish(true);
                uploadStateMLD.postValue(new StateResult<Integer>().setState(STATE_STOP).setCode(0).setData(100));
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                if (null == error) return;
                onUpdateError("setUsingResource", error.getSubCode(), error.getMessage());
            }
        };
        JL_Log.d(tag, "setUsingResource", "ResourceType : " + resourceMsg.getResourceType() + ", DevHandler : " + storage.getDevHandler()
        + ", deviceFilePath : " + deviceFilePath);
        switch (resourceMsg.getResourceType()) {
            case ChargingCaseInfo.TYPE_SCREEN_SAVER: {
                mChargingCaseOp.setCurrentScreenSaver(device, storage.getDevHandler(), deviceFilePath, callback);
                break;
            }
            case ChargingCaseInfo.TYPE_BOOT_ANIM: {
                mChargingCaseOp.setCurrentBootAnim(device, storage.getDevHandler(), deviceFilePath, callback);
                break;
            }
            case ChargingCaseInfo.TYPE_WALLPAPER: {
                mChargingCaseOp.setCurrentWallPaper(device, storage.getDevHandler(), deviceFilePath, callback);
                break;
            }
        }
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
            return (T) new UploadResourceViewModel(resource);
        }
    }
}
