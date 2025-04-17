package com.jieli.btsmart.ui.alarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.bluetooth.bean.device.alarm.AuditionParam;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.data.adapter.BellFileListAdapter;
import com.jieli.btsmart.data.adapter.FileListAdapter;
import com.jieli.btsmart.ui.music.device.FilesFragment;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.SDCardBean;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/9/2 5:41 PM
 * @desc :
 */
public class FileBellFragment extends FilesFragment {

    private int initCluster = -1;
    private int initDev = -1;

    public static FileBellFragment newInstance(SDCardBean sdCardBean, int dev, int cluster) {
        Bundle args = new Bundle();
        FileBellFragment fragment = new FileBellFragment();
        fragment.setSdCardBean(sdCardBean);
        fragment.setArguments(args);
        fragment.initCluster = cluster;
        fragment.initDev = dev;
        return fragment;
    }


    @Override
    protected void handleFileClick(FileStruct fileStruct) {
        Intent intent = new Intent();
        String bellName = fileStruct.getName();
        if (!TextUtils.isEmpty(bellName)) {
            //去掉后缀
            int lastDotIndex = bellName.lastIndexOf(".");
            bellName = bellName.substring(0, lastDotIndex);
            JL_Log.d(TAG, "去掉文件名后缀: " + bellName);
            byte[] data = bellName.getBytes();
            if (data.length > 32) {
                for (int i = 9; i < bellName.length() - 1; i++) {
                    if (bellName.substring(0, i + 1).getBytes().length > 32) {
                        bellName = bellName.substring(0, i);
                        break;
                    }
                }
                JL_Log.d(TAG, "去掉超出32byte的文件名称部分： " + bellName);
            }
        }

        intent.putExtra("type", (byte) 1);
        intent.putExtra("dev", fileStruct.getDevIndex());
        intent.putExtra("cluster", fileStruct.getCluster());
        intent.putExtra("name", bellName);
        requireActivity().setResult(Activity.RESULT_OK, intent);
        getFileListAdapter().setSelected(fileStruct.getDevIndex(), fileStruct.getCluster());
        AuditionParam param = new AuditionParam();
        param.setName(bellName);
        param.setCluster(fileStruct.getCluster());
        param.setDev(fileStruct.getDevIndex());
        param.setType((byte)1);
        mRCSPController.auditionAlarmBell(mRCSPController.getUsingDevice(), param, null);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //忽律音乐模式的状态回调
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getFileListAdapter().setSelected((byte) initDev, initCluster);
    }

    @Override
    protected FileListAdapter createFileAdapter() {
        return new BellFileListAdapter();
    }
}
