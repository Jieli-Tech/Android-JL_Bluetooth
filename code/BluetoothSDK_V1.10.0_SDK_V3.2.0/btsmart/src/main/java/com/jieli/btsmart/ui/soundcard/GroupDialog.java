package com.jieli.btsmart.ui.soundcard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
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
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.soundcard.SoundCard;
import com.jieli.component.utils.ValueUtil;

import java.util.List;
import java.util.Objects;

/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/11 11:24 AM
 * @desc :
 */
public class GroupDialog extends DialogFragment {

    private final RCSPController mRCSPController = RCSPController.getInstance();
    private List<SoundCard.Functions.ListBean> list;
    private long mask = 0;

    public void setList(List<SoundCard.Functions.ListBean> list) {
        this.list = list;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        assert window != null;
        int size = Math.min(10, list.size() + 1);
        int totalDividerH = size - 2;
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, ValueUtil.dp2px(getContext(), 50) * size + totalDividerH);
        window.setBackgroundDrawable(null);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_sound_card_group, container, false);
        Window window = Objects.requireNonNull(getDialog()).getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.horizontalMargin = 0;
        lp.windowAnimations = R.style.BottomToTopAnim;
        window.setAttributes(lp);

        RecyclerView rv = root.findViewById(R.id.rv_sound_card_group);
        rv.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        BaseQuickAdapter baseQuickAdapter = new BaseQuickAdapter<SoundCard.Functions.ListBean, BaseViewHolder>(R.layout.item_sound_card_group, list) {
            @Override
            protected void convert(BaseViewHolder baseViewHolder, SoundCard.Functions.ListBean listBean) {
                boolean selected = ((mask >> listBean.index) & 0x01) == 0x01;
                TextView tv = baseViewHolder.getView(R.id.tv_name);
                tv.setText(listBean.title.getShowText());
                tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, selected ? R.drawable.ic_check_purple : R.drawable.ic_check_gray, 0);
            }

        };

        rv.setAdapter(baseQuickAdapter);
        baseQuickAdapter.setOnItemClickListener((adapter, view, position) -> new ActionClickHandler(list.get(position)).onClick(view));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getContext(), RecyclerView.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.divier_sound_card_group));

        rv.addItemDecoration(dividerItemDecoration);
        root.findViewById(R.id.btn_sound_card_group_close).setOnClickListener(v -> dismiss());
        return root;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mRCSPController.addBTRcspEventCallback(btEventCallback);
        mRCSPController.getSoundCardStatusInfo(mRCSPController.getUsingDevice(), null);
    }

    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(btEventCallback);
        super.onDestroyView();
    }


    private void updateView(long mask) {
        this.mask = mask;
        int pos = 0;
        boolean selected = false;
        for (SoundCard.Functions.ListBean bean : list) {
            selected = ((mask >> bean.index) & 0x01) == 0x01;
            if (selected) {
                break;
            } else {
                pos++;
            }
        }
        RecyclerView recyclerView = requireView().findViewById(R.id.rv_sound_card_group);
        if (selected) {
            //有选中项才需要滑动到对应的位置
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int lastPos = linearLayoutManager.findLastCompletelyVisibleItemPosition();
            int firstPos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
            if (pos > lastPos) {
                //如果选中项pos大于最后一个可见view，则滑动pos
                recyclerView.scrollToPosition(pos);
            } else if (pos < firstPos) {
                //如果选中项pos小于第一个可见view，则滑动到pos
                recyclerView.scrollToPosition(pos);
            }
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private final BTRcspEventCallback btEventCallback = new BTRcspEventCallback() {

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            dismiss();
        }

        @Override
        public void onSoundCardStatusChange(BluetoothDevice device, long mask, byte[] values) {
            updateView(mask);
        }

    };

    public boolean isShow() {
        return getDialog() != null && getDialog().isShowing();
    }

}