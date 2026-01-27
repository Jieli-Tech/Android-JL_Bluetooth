package com.jieli.btsmart.ui.widget;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.bluetooth.bean.base.VoiceMode;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.VoiceModeAdapter;
import com.jieli.btsmart.data.model.settings.VoiceModeItem;
import com.jieli.btsmart.ui.base.BaseDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zqjasonZhong
 * @email zhongzhuocheng@zh-jieli.com
 * @desc 声音模式列表弹窗
 * @since 2021/3/26
 */
public class VoiceModeListDialog extends BaseDialogFragment {
    private RecyclerView rvVoiceModeList;
    private TextView tvSure;

    private VoiceModeAdapter mAdapter;
    private List<VoiceMode> modes;
    private byte[] selectModes;

    private OnVoiceModeListListener mListListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_voice_mode_list, container, false);
        rvVoiceModeList = view.findViewById(R.id.rv_voice_mode_list);
        tvSure = view.findViewById(R.id.tv_voice_mode_list_sure);
        rvVoiceModeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getDialog() == null) return;
        Window window = getDialog().getWindow();
        if (window == null) return;
        WindowManager.LayoutParams mLayoutParams = window.getAttributes();
        mLayoutParams.gravity = Gravity.CENTER;
        mLayoutParams.dimAmount = 0.5f;
        mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;

        mLayoutParams.width = Math.round(0.9f * getScreenWidth());
        mLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        window.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_transparent)));
        window.getDecorView().getRootView().setBackgroundColor(Color.TRANSPARENT);
        window.setAttributes(mLayoutParams);

        mAdapter = new VoiceModeAdapter();
        mAdapter.setOnItemClickListener((adapter, view, position) -> {
            VoiceModeItem item = mAdapter.getItem(position);
            if (null == item) return;
            mAdapter.handleVoiceModeItem(item);
        });
        rvVoiceModeList.setAdapter(mAdapter);
        tvSure.setOnClickListener(v -> submitResult());
        if (modes != null) {
            mAdapter.setNewInstance(convertItemList(modes));
        }
        if (selectModes != null) {
            mAdapter.setSelectList(selectModes);
        }
    }

    public void setOnVoiceModeListListener(OnVoiceModeListListener listener) {
        mListListener = listener;
    }

    public void setModes(List<VoiceMode> list) {
        modes = list;
        if (mAdapter != null && !isDetached() && isAdded()) {
            mAdapter.setList(convertItemList(modes));
        }
    }

    public void setSelectModes(byte[] modes) {
        selectModes = modes;
        if (mAdapter != null && !isDetached() && isAdded()) {
            mAdapter.setSelectList(modes);
        }
    }

    private List<VoiceModeItem> convertItemList(List<VoiceMode> list) {
        List<VoiceModeItem> itemList = new ArrayList<>();
        if (list != null) {
            for (int i = list.size() - 1; i >= 0; i--) {
                VoiceMode mode = list.get(i);
                VoiceModeItem item = new VoiceModeItem();
                item.setMode(mode.getMode());
                item.setName(VoiceModeItem.getVoiceModeName(requireContext(), mode.getMode()));
                item.setDesc(VoiceModeItem.getVoiceModeDesc(requireContext(), mode.getMode()));
                itemList.add(item);
            }
        }
        return itemList;
    }

    private void submitResult() {
        List<VoiceModeItem> selectItem = mAdapter.getSelectList();
        byte[] result = new byte[selectItem.size()];
        for (int i = 0; i < selectItem.size(); i++) {
            result[i] = (byte) selectItem.get(i).getMode();
        }
        if (mListListener != null) mListListener.onSelectList(result);
    }

    public interface OnVoiceModeListListener {

        void onSelectList(byte[] modes);
    }
}
