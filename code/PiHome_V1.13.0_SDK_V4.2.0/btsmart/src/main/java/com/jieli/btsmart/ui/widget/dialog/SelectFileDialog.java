package com.jieli.btsmart.ui.widget.dialog;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.jieli.btsmart.databinding.DialogSelectFileBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 选择文件对话框
 * @since 2024/1/25
 */
public class SelectFileDialog extends CommonDialog {

    private DialogSelectFileBinding mBinding;
    private SelectFileAdapter mAdapter;

    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    protected SelectFileDialog(@NonNull Builder builder) {
        super(builder);
    }

    @Override
    public View createView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DialogSelectFileBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
        readFileList();
    }

    private void initUI() {
        if (!(mBuilder instanceof Builder)) return;
        Builder builder = (Builder) mBuilder;
        mBinding.tvTitle.setText(builder.title);
        mBinding.btnCancel.setOnClickListener(v -> dismiss());
        mBinding.btnSure.setOnClickListener(v -> {
            File selectedFile = mAdapter.getSelectedFile();
            if (null == selectedFile) {
                showTips("请选择文件");
                return;
            }
            final OnSelectFileCallback callback = builder.callback;
            if (callback != null) {
                callback.onSelected(SelectFileDialog.this, selectedFile);
            }
            dismiss();
        });

        mAdapter = new SelectFileAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> mAdapter.updateSelectedFile(mAdapter.getItem(position)));
        mBinding.rvFile.setLayoutManager(new LinearLayoutManager(requireContext()));
        mBinding.rvFile.setAdapter(mAdapter);
    }

    private void readFileList() {
        if (!(mBuilder instanceof Builder)) return;
        final Builder builder = (Builder) mBuilder;
        Executors.newSingleThreadExecutor().submit(() -> {
            File dirFile = new File(builder.getDirPath());
            final List<File> fileList = new ArrayList<>();
            if (!dirFile.exists()) {
                mUiHandler.post(() -> mAdapter.setList(fileList));
                return;
            }
            if (dirFile.isFile()) {
                mUiHandler.post(() -> mAdapter.setList(fileList));
                return;
            }
            File[] files = dirFile.listFiles();
            if (null == files || files.length == 0) {
                mUiHandler.post(() -> mAdapter.setList(fileList));
                return;
            }
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file);
                }
            }
            mUiHandler.post(() -> mAdapter.setList(fileList));
        });
    }

    public interface OnSelectFileCallback {

        void onSelected(SelectFileDialog dialog, File file);
    }

    public static class Builder extends CommonDialog.Builder {
        private String title;
        private String dirPath;
        private OnSelectFileCallback callback;

        public Builder() {
            setCancelable(false);
        }

        public String getTitle() {
            return title;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getDirPath() {
            return dirPath;
        }

        public Builder setDirPath(String dirPath) {
            this.dirPath = dirPath;
            return this;
        }

        public OnSelectFileCallback getCallback() {
            return callback;
        }

        public Builder setCallback(OnSelectFileCallback callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public SelectFileDialog build() {
            return new SelectFileDialog(this);
        }
    }
}
