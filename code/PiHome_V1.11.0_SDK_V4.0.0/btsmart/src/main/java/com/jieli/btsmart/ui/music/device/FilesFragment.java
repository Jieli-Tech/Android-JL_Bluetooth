package com.jieli.btsmart.ui.music.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.module.BaseLoadMoreModule;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.music.MusicNameInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.FileListAdapter;
import com.jieli.btsmart.data.adapter.FileRouterAdapter;
import com.jieli.btsmart.tool.playcontroller.PlayControlImpl;
import com.jieli.btsmart.ui.settings.device.DeviceControlFragment;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.JL_MediaPlayerServiceManager;
import com.jieli.component.utils.HandlerManager;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.filebrowse.FileBrowseConstant;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.Folder;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.FileObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备文件浏览界面
 */
public class FilesFragment extends DeviceControlFragment implements FileObserver {
    private RecyclerView rvFilePathNav;

    protected final RCSPController mRCSPController = RCSPController.getInstance();
    private FileBrowseManager mFileBrowseManager;

    private SDCardBean mSdCardBean = new SDCardBean();
    private FileListAdapter mFileListAdapter;
    private BaseLoadMoreModule mLoadMoreModule;
    private final FileRouterAdapter mFileRouterAdapter = new FileRouterAdapter();


    public void setSdCardBean(SDCardBean mSdCardBean) {
        this.mSdCardBean = mSdCardBean;
    }

    public FilesFragment() {
        // Required empty public constructor
    }

    public static FilesFragment newInstance(SDCardBean sdCardBean) {
        Bundle args = new Bundle();
        FilesFragment fragment = new FilesFragment();
        fragment.mSdCardBean = sdCardBean;
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        RecyclerView rvDeviceFiles = view.findViewById(R.id.rv_device_files);
        rvFilePathNav = view.findViewById(R.id.rv_file_path_nav);

        //初始化文件路径导航栏
        rvFilePathNav.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        rvFilePathNav.setAdapter(mFileRouterAdapter);
        mFileRouterAdapter.setOnItemClickListener((adapter, view12, position) -> handleFileRouterClick(position));
        //初始化文件列表ui
        mFileListAdapter = createFileAdapter();
        rvDeviceFiles.setAdapter(mFileListAdapter);
        rvDeviceFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        mLoadMoreModule = mFileListAdapter.getLoadMoreModule();
        mLoadMoreModule.setOnLoadMoreListener(this::onLoadMoreRequested);

        //没有设备则返回
        if (mSdCardBean == null) {
            return view;
        }
        //设置选中文件
        if (mRCSPController.isDeviceConnected()) {
            mFileListAdapter.setSelected(mRCSPController.getDeviceInfo().getCurrentDevIndex()
                    , mRCSPController.getDeviceInfo().getCluster());
        }
        createEmptyView();
        mFileListAdapter.setOnItemClickListener((adapter, view1, position) -> {
            FileStruct fileStruct = mFileListAdapter.getItem(position);
            JL_Log.d(TAG, "click file item -->" + position + "\treading-->" + getFileBrowseManager().isReading());
            if (fileStruct != null && fileStruct.isFile()) {
                handleFileClick(fileStruct);
            } else {
                handleFolderClick(fileStruct);
            }
        });

        //读取当前浏览信息
        initWithCurrentPath();
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
        getFileBrowseManager().addFileObserver(this);
        return view;
    }

    private FileBrowseManager getFileBrowseManager() {
        if (mFileBrowseManager == null) {
            mFileBrowseManager = FileBrowseManager.getInstance();
        }
        return mFileBrowseManager;
    }

    private void initWithCurrentPath() {
        //获取存储设备的缓存数据
        Folder currentFolder = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
        if (currentFolder != null) {
            refreshFileRouterView(getFileBrowseManager().getCurrentReadFile(mSdCardBean));
            mFileListAdapter.setNewInstance(currentFolder.getChildFileStructs()); //缓存的文件列表
            //判断当前目录是否读取完毕
            if (mFileListAdapter.getData().size() < 1 && !currentFolder.isLoadFinished(false)) {
                onLoadMoreRequested(); //未读取完毕，进行加载操作
            } else if (currentFolder.isLoadFinished(false)) {
                mFileListAdapter.setUseEmpty(true);
                mLoadMoreModule.loadMoreEnd(); //读取完毕
            }
        }
    }


    protected FileListAdapter createFileAdapter() {
        return new FileListAdapter();
    }

    protected FileListAdapter getFileListAdapter() {
        return mFileListAdapter;
    }

    //点击文件
    protected void handleFileClick(FileStruct fileStruct) {
        if (getFileBrowseManager().isReading()) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
            return;
        }
        boolean isPlaying = JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().isPlaying();
        if (isPlaying) {
            JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().pause();
        }
        HandlerManager.getInstance().getMainHandler().postDelayed(() -> {
            if (!mRCSPController.isDeviceConnected()) return;
            getFileBrowseManager().playFile(fileStruct, mSdCardBean);
            PlayControlImpl.getInstance().updateMode(PlayControlImpl.MODE_MUSIC);//切换到音乐模式
            mRCSPController.getDeviceInfo().setCurrentDevIndex(fileStruct.getDevIndex());
            mRCSPController.getDeviceInfo().setCluster(fileStruct.getCluster());
            mFileListAdapter.setSelected(fileStruct.getDevIndex(), fileStruct.getCluster());
        }, isPlaying ? 100 : 0);
    }

    //点击文件夹
    protected void handleFolderClick(FileStruct fileStruct) {
        showEmptyView(false);
        int ret = getFileBrowseManager().appenBrowse(fileStruct, mSdCardBean);
        JL_Log.i(TAG, AppUtil.formatString("appenBrowse :[%s], ret : %d", mSdCardBean, ret));
        if (ret == FileBrowseConstant.ERR_READING) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
        } else if (ret == FileBrowseConstant.ERR_OFFLINE) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_offline);
        } else if (ret == FileBrowseConstant.ERR_BEYOND_MAX_DEPTH) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_beyond_max_depth);
        } else if (ret == FileBrowseConstant.SUCCESS) {
            mFileListAdapter.setNewInstance(new ArrayList<>());
            refreshFileRouterView(getFileBrowseManager().getCurrentReadFile(mSdCardBean));
        }
    }

    private void handleFileRouterClick(int position) {
        FileStruct fileStruct = mFileRouterAdapter.getItem(position);
        Folder current = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
        if (current == null) {
            return;
        }
        //当前文件返回
        if (current.getCluster() == fileStruct.getCluster()) {
            return;
        }
        //遍历父文件夹，直到父文件夹是选中文件夹。
        while (current.getParent() != null && current.getParent().getCluster() != fileStruct.getCluster()) {
            //不判断结果，可能会导致死循环
            if(getFileBrowseManager().backBrowse(mSdCardBean, false) != FileBrowseConstant.SUCCESS) break;
            current = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
        }
        //清空文件列表
        mFileListAdapter.setNewInstance(new ArrayList<>());
        getFileBrowseManager().backBrowse(mSdCardBean, true);
        refreshFileRouterView(getFileBrowseManager().getCurrentReadFile(mSdCardBean));
    }


    //更新顶部导航栏
    private void refreshFileRouterView(Folder folder) {
        if (folder == null) {
            return;
        }
        List<FileStruct> list = new ArrayList<>();
        list.add(folder);
        while (folder.getParent() != null) {
            folder = folder.getParent();
            list.add(0, folder);
        }
        JL_Log.d(TAG, "file path count-->" + list.size());
        mFileRouterAdapter.setNewInstance(list);
        rvFilePathNav.scrollToPosition(mFileRouterAdapter.getData().size() - 1);
    }


    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        if (mFileBrowseManager != null) {
            mFileBrowseManager.removeFileObserver(this);
            mFileBrowseManager = null;
        }
        super.onDestroyView();
    }


    @Override
    public void onFileReceiver(List<FileStruct> fileStructs) {
        JL_Log.d(TAG, "onFileReceiver :: " + fileStructs);
        if (mFileListAdapter != null && getActivity() != null && !getActivity().isDestroyed()) {
            mFileListAdapter.addData(fileStructs);
        }
    }

    @Override
    public void onFileReadStop(boolean isEnd) {
        if (isEnd) {
            mLoadMoreModule.loadMoreEnd();
        } else {
            mLoadMoreModule.loadMoreComplete();
        }
        showEmptyView(true);
    }

    @Override
    public void OnFlayCallback(boolean success) {
        if (mRCSPController.isDeviceConnected() && success) {
            mRCSPController.getDeviceMusicInfo(mRCSPController.getUsingDevice(), null);
        }
    }

    @Override
    public void onSdCardStatusChange(List<SDCardBean> onLineCards) {
    }


    protected final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {

        @Override
        public void onMusicNameChange(BluetoothDevice device, MusicNameInfo nameInfo) {
            if (mFileListAdapter != null && isAdded() && !isDetached()) {
                mFileListAdapter.setSelected(mFileListAdapter.getDevIndex(), nameInfo.getCluster());
            }
        }

        @Override
        public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
            JL_Log.d(TAG, "onMusicStatusChange :: " + statusInfo);
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (deviceInfo != null && deviceInfo.getDevStorageInfo() != null && deviceInfo.getDevStorageInfo().isDeviceReuse()) {
                JL_Log.d(TAG, "onMusicStatusChange :: " + mSdCardBean);
                if (statusInfo.getCurrentDev() != mSdCardBean.getDevHandler()) { //设备复用，句柄不一样，退出界面
                    requireActivity().finish();
                    return;
                }
            }
            if (mFileListAdapter != null && isAdded() && !isDetached()) {
                mFileListAdapter.setSelected((byte) statusInfo.getCurrentDev(), mFileListAdapter.getSelectedCluster());
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (mode != AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC) {
                mFileListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void onLoadMoreRequested() {
        int ret = getFileBrowseManager().loadMore(mSdCardBean); //加载更多数据
        if (ret == FileBrowseConstant.ERR_LOAD_FINISHED) { //已加载完毕
            mLoadMoreModule.loadMoreEnd();
        } else if (ret == FileBrowseConstant.ERR_READING) { //正在加载中，请稍后操作
            mLoadMoreModule.loadMoreComplete();
        } else if (ret != FileBrowseConstant.SUCCESS) { //其他错误
            mLoadMoreModule.loadMoreFail();
        }
    }


    @Override
    public void onFileReadStart() {
        showEmptyView(false);
    }

    @Override
    public void onFileReadFailed(int reason) {
        mLoadMoreModule.loadMoreFail();
    }


    private void createEmptyView() {
        //设置空布局，可用xml文件代替
        TextView textView = new TextView(getContext());
        textView.setText(getString(R.string.empty_folder));
        textView.setTextSize(16);
        textView.setTextColor(getResources().getColor(R.color.gray_text_5A5A5A));
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_folder_img_empty, 0, 0);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(0, ValueUtil.dp2px(getContext(), 98), 0, 0);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(layoutParams);
        mFileListAdapter.setEmptyView(textView);
        mFileListAdapter.setUseEmpty(false);
    }


    @SuppressLint("NotifyDataSetChanged")
    private void showEmptyView(boolean show) {
        JL_Log.d(TAG, "showEmptyView--->" + show);
        if (mFileListAdapter.isUseEmpty() != show) {
            mFileListAdapter.setUseEmpty(show);
            mFileListAdapter.notifyDataSetChanged();
        }
    }
}
