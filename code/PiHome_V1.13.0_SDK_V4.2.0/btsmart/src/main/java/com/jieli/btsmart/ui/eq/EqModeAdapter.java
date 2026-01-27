package com.jieli.btsmart.ui.eq;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.bluetooth.bean.device.eq.EqInfo;
import com.jieli.btsmart.R;
import com.jieli.btsmart.util.EqCacheUtil;

import java.util.List;

/**
 * Des:
 * Author: Bob
 * Date:20-5-15
 * UpdateRemark:
 */
public final class EqModeAdapter extends BaseQuickAdapter<EqInfo, BaseViewHolder> {
    private int selectMode = 0;

    public EqModeAdapter(List<EqInfo> list) {
        super(R.layout.item_eq_mode, list);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder helper, EqInfo item) {
        helper.setText(R.id.tv_eq_mode_name, getModeNmae(item.getMode()));
        helper.getView(R.id.tv_eq_mode_name).setSelected(selectMode == item.getMode());
        ImageView imageView = helper.getView(R.id.iv_eq_mode_bmp);
        Bitmap bitmap;
        if (item.getMode() == EqInfo.MODE_CUSTOM) {
            bitmap = EqCacheUtil.createBitmap(item);
        } else {
            bitmap = BitmapFactory.decodeFile(EqCacheUtil.getEqValueBitMapPath(item));
        }
        imageView.setImageBitmap(bitmap);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void select(int selectMode) {
        this.selectMode = selectMode;
        notifyDataSetChanged();
    }


    public String getModeNmae(int mode) {
        String[] equalizer = getContext().getResources().getStringArray(R.array.eq_mode_list);
        return equalizer[mode];

    }


}
