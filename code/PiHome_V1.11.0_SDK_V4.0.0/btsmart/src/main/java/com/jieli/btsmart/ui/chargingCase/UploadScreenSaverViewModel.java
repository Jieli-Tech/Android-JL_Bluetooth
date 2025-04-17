package com.jieli.btsmart.ui.chargingCase;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.file.op.CreateFileParam;
import com.jieli.bluetooth.bean.settings.v0.ResourceInfo;
import com.jieli.bluetooth.constant.ErrorCode;
import com.jieli.bluetooth.impl.rcsp.charging_case.ChargingCaseOpImpl;
import com.jieli.bluetooth.impl.rcsp.file.FileOpImpl;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnTaskStateListener;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.model.basic.StateResult;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.viewmodel.base.BtBasicVM;
import com.jieli.component.utils.FileUtil;
import com.jieli.filebrowse.bean.SDCardBean;

import java.io.File;
import java.util.Calendar;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 上传屏幕保护图逻辑实现
 * @since 2023/12/8
 */
public class UploadScreenSaverViewModel extends BtBasicVM {

    /**
     * 空闲状态
     */
    public static final int STATE_IDLE = 0;
    /**
     * 传输状态
     */
    public static final int STATE_WORKING = 1;
    /**
     * 传输被取消状态
     */
    public static final int STATE_CANCEL = 2;
    /**
     * 传输停止状态
     */
    public static final int STATE_STOP = 3;

    private static final int MSG_PROGRESS_CHANGE = 0x0101;

    @NonNull
    private final FileOpImpl mFileOp;
    @NonNull
    private final ChargingCaseOpImpl mChargingCaseOp;

    public final MutableLiveData<StateResult<Integer>> transferStateMLD = new MutableLiveData<>();
    /**
     * 转码结果
     */
    public ConfirmScreenSaversViewModel.ConvertResult convertResult;

    private final Handler uiHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_PROGRESS_CHANGE) {
                if (msg.arg1 == 100) {
                    String thumbPath = onTransferFinish(true);
                    transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_STOP).setCode(0).setData(100).setMessage(thumbPath));
                } else {
                    transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_WORKING).setCode(0).setData(msg.arg1));
                    uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_PROGRESS_CHANGE, msg.arg1 + 10, 0), 1000L);
                }
            }
            return true;
        }
    });

    public UploadScreenSaverViewModel() {
        mFileOp = FileOpImpl.instance(mRCSPController.getRcspOp());
        mChargingCaseOp = ChargingCaseOpImpl.instance(mRCSPController.getRcspOp());
    }

    public boolean isTransferring() {
        StateResult<Integer> value = transferStateMLD.getValue();
        if (null == value) return false;
        return value.getState() == STATE_WORKING;
    }

    public void transferScreenSaver() {
        if (isTransferring()) {
            JL_Log.d(tag, "transferScreenSaver", "Transferring...");
            return;
        }
        final SDCardBean storage = getOnlineFlash();
        if (null == storage) {
            transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_STOP).setCode(ErrorCode.SUB_ERR_STORAGE_OFFLINE)
                    .setMessage(ErrorCode.code2Msg(ErrorCode.SUB_ERR_STORAGE_OFFLINE)));
            return;
        }
        transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_IDLE).setCode(0).setData(0));
        final String filePath = convertResult.getOutputFilePath();
        final File file = new File(filePath);
        CreateFileParam param = new CreateFileParam(getConnectedDevice(), file, storage);
        mFileOp.createBigFile(param, new OnTaskStateListener() {
            @Override
            public void onStart() {
                transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_WORKING).setCode(0).setData(0));
            }

            @Override
            public void onProgress(int progress) {
                transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_WORKING).setCode(0).setData(progress));
            }

            @Override
            public void onStop() {
                String deviceFilePath = "/" + file.getName();
                mChargingCaseOp.setCurrentScreenSaver(getConnectedDevice(), storage.getDevHandler(), deviceFilePath,
                        new OnRcspActionCallback<Boolean>() {
                            @Override
                            public void onSuccess(BluetoothDevice device, Boolean message) {
                                String thumbPath = onTransferFinish(true);
                                transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_STOP).setCode(0).setData(100).setMessage(thumbPath));
                            }

                            @Override
                            public void onError(BluetoothDevice device, BaseError error) {
                                transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_STOP).setCode(error.getSubCode())
                                        .setMessage(error.getMessage()));
                            }
                        });
            }

            @Override
            public void onCancel(int reason) {
                onTransferFinish(false);
                transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_CANCEL).setCode(0));
            }

            @Override
            public void onError(int code, String message) {
                transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_STOP).setCode(code).setMessage(message));
            }
        });
    }

    public void cancelTransfer() {
        mFileOp.cancelBigFileTransfer(getConnectedDevice(), 0, new OnRcspActionCallback<Boolean>() {
            @Override
            public void onSuccess(BluetoothDevice device, Boolean message) {
                JL_Log.d(tag, "cancelTransfer", "onSuccess : " + message);
            }

            @Override
            public void onError(BluetoothDevice device, BaseError error) {
                JL_Log.d(tag, "cancelTransfer", "onError : " + error);
            }
        });
    }

    private void testTransferUI() {
        if (transferStateMLD.getValue() != null && (transferStateMLD.getValue().getState() == STATE_WORKING)) {
            JL_Log.d(tag, "transferScreenSaver", "Transferring...");
            return;
        }
        transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_IDLE).setCode(0).setData(0));
        uiHandler.sendMessage(uiHandler.obtainMessage(MSG_PROGRESS_CHANGE, 0, 0));
    }

    private void testCancelTransfer() {
        uiHandler.removeMessages(MSG_PROGRESS_CHANGE);
        onTransferFinish(false);
        transferStateMLD.postValue(new StateResult<Integer>().setState(STATE_CANCEL).setCode(0));
    }

    @Override
    protected void release() {
        cancelTransfer();
        uiHandler.removeCallbacksAndMessages(null);
        super.release();
    }

    private SDCardBean getOnlineFlash() {
        SDCardBean storage = null;
        final List<SDCardBean> onlineStorages = mFileOp.getOnlineStorage();
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

    private String onTransferFinish(boolean isUploadOk) {
        JL_Log.d(tag, "onTransferFinish", convertResult.toString());
        final String filePath = convertResult.getOutputFilePath();
        String srcFilePath = convertResult.getInputFilePath();
        String filename = AppUtil.getFileName(filePath);
        int index = filePath.lastIndexOf(File.separator);
        String outputDirPath = index == -1 ? filePath : filePath.substring(0, index);
        boolean ret;
        if (filename.toUpperCase().startsWith(ResourceInfo.CUSTOM_SCREEN_NAME)) { //自定义屏保处理
            index = outputDirPath.lastIndexOf(File.separator);
            String srcFileDirPath = index == -1 ? outputDirPath : outputDirPath.substring(0, index);
            File srcFile = new File(srcFileDirPath, SConstant.CROP_FILE_NAME);
            if (isUploadOk) {
                if (!srcFile.exists() || !srcFile.isFile()) {
                    srcFile = new File(srcFilePath);
                }
                short crc = convertResult.getOutFileCrc();
                String oldFilePath = srcFile.getPath();
                File newFile = new File(srcFileDirPath, AppUtil.formatString("%s-%x.jpeg", ResourceInfo.CUSTOM_SCREEN_NAME, crc));
                String newFilePath = newFile.getPath();
                JL_Log.d(tag, "onTransferFinish", "oldFilePath = " + oldFilePath + ",\n newFilePath = " + newFilePath);
                if (newFile.exists() && newFile.isFile()) {
                    ret = newFile.setLastModified(Calendar.getInstance().getTimeInMillis());
                    JL_Log.d(tag, "onTransferFinish", "Change File : " + ret);
                    srcFilePath = newFilePath;
                } else {
                    ret = srcFile.renameTo(newFile);
                    JL_Log.d(tag, "onTransferFinish", "Rename File : " + ret);
                    if (ret) {
                        ret = newFile.setLastModified(Calendar.getInstance().getTimeInMillis());
                        srcFilePath = newFilePath;
                    }
                }
            } else { //传输被中止
                if (srcFile.exists() && srcFile.isFile()) {
                    ret = srcFile.delete();
                }
            }
        }
        if (outputDirPath.endsWith(SConstant.DIR_OUTPUT)) {
            FileUtil.deleteFile(new File(outputDirPath));
        } else {
            FileUtil.deleteFile(new File(filePath));
        }
        JL_Log.d(tag, "onTransferFinish", "Delete File : " + outputDirPath + ",\n file Path = " + filePath + ",\n srcFilePath = " + srcFilePath);
        return srcFilePath;
    }
}
