package com.jieli.btsmart.data.adapter;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.ota.FirmwareOtaItem;
import com.jieli.btsmart.data.model.ota.OtaStageInfo;
import com.jieli.component.utils.ValueUtil;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.List;

/**
 * 固件更新适配器
 *
 * @author zqjasonZhong
 * @since 2020/5/19
 */
public class FirmwareOtaAdapter extends BaseMultiItemQuickAdapter<FirmwareOtaItem, BaseViewHolder> {

    private OnFirmwareOtaListener mOnFirmwareOtaListener;


    public FirmwareOtaAdapter(List<FirmwareOtaItem> data) {
        super(data);
        addItemType(FirmwareOtaItem.LAYOUT_ONE, R.layout.item_settings);
        addItemType(FirmwareOtaItem.LAYOUT_TWO, R.layout.item_firmware_upgrade);
    }

    public void setOnFirmwareOtaListener(OnFirmwareOtaListener onFirmwareOtaListener) {
        mOnFirmwareOtaListener = onFirmwareOtaListener;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void convert(@NonNull BaseViewHolder helper, FirmwareOtaItem item) {
        if (item == null) return;
        int position = getItemPosition(item);
        switch (item.getItemType()) {
            case FirmwareOtaItem.LAYOUT_ONE: {
                helper.setText(R.id.tv_item_settings_name, item.getContent());
                TextView tvValue = helper.getView(R.id.tv_item_settings_value);
                tvValue.setVisibility(item.getValue() != null ? View.VISIBLE : View.GONE);
                if (item.getValue() != null) {
                    tvValue.setText(item.getValue());
                    if (item.isShowIcon()) {
                        tvValue.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, getContext().getDrawable(R.drawable.ic_little_right_arrow), null);
                    } else {
                        tvValue.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
                    }
                }
                View line = helper.getView(R.id.view_item_settings_line);
                updateLineView(helper, line, position);
                break;
            }
            case FirmwareOtaItem.LAYOUT_TWO:
                helper.setText(R.id.tv_firmware_upgrade_name, item.getContent());
                updateUpgradeView(helper, item.getOtaStageInfo(), position);
                View line = helper.getView(R.id.view_firmware_upgrade_line);
                updateLineView(helper, line, position);
                break;
        }
    }

    private void updateLineView(BaseViewHolder helper, View view, int position) {
        if (helper != null && view != null) {
            int visibility = (position == getData().size() - 1) ? View.GONE : View.VISIBLE;
            view.setVisibility(visibility);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void updateUpgradeView(BaseViewHolder helper, OtaStageInfo info, int position) {
        if (helper == null || info == null) return;
        RelativeLayout rlUpgradeStage = helper.getView(R.id.rl_firmware_upgrade_stage);
        TextView tvValue = helper.getView(R.id.tv_firmware_upgrade_value);
        TextView tvStage = helper.getView(R.id.tv_firmware_upgrade_stage);
        AVLoadingIndicatorView avLoading = helper.getView(R.id.av_firmware_upgrade_loading);
        RelativeLayout rlProgress = helper.getView(R.id.rl_firmware_upgrade_progress);
        ProgressBar pbProgress = helper.getView(R.id.pb_firmware_upgrade_progress);
        TextView tvProgress = helper.getView(R.id.tv_firmware_upgrade_progress);
        tvStage.setPadding(ValueUtil.dp2px(getContext(), 20), ValueUtil.dp2px(getContext(), 6), ValueUtil.dp2px(getContext(), 20), ValueUtil.dp2px(getContext(), 6));
        switch (info.getStage()) {
            case OtaStageInfo.STAGE_IDLE: //空闲-检查更新
                rlProgress.setVisibility(View.GONE);
                rlUpgradeStage.setVisibility(View.VISIBLE);
                tvValue.setText("");
//                tvStage.setEnabled(false);
//                tvStage.setPadding(0, 0, ValueUtil.dp2px(getContext(), 10), 0);
//                tvStage.setBackground(getContext().getResources().getDrawable(R.color.text_transparent));
//                tvStage.setText(getContext().getString(R.string.laster_version));
//                tvStage.setTextColor(getContext().getResources().getColor(R.color.gray_text_989898));
                int visibility = info.getProgress() > 0 ? View.VISIBLE : View.INVISIBLE;
                if (visibility != avLoading.getVisibility()) {
                    avLoading.setVisibility(visibility);
                }

                tvStage.setEnabled(info.getProgress() == 0);
                tvStage.setBackground(getContext().getResources().getDrawable(R.drawable.bg_btn_purple_gray_selector));
                tvStage.setText(getContext().getString(R.string.check_update));
                tvStage.setTextColor(getContext().getResources().getColor(R.color.white_ffffff));
                tvStage.setTag(position);
                tvStage.setOnClickListener(mOnClickListener);
                break;
            case OtaStageInfo.STAGE_IDLE_DOWNLOAD: //空闲-有更新
                rlProgress.setVisibility(View.GONE);
                rlUpgradeStage.setVisibility(View.VISIBLE);
                avLoading.setVisibility(View.INVISIBLE);
                tvValue.setText(info.getMessage());
                tvStage.setEnabled(true);
                tvStage.setBackground(getContext().getResources().getDrawable(R.drawable.bg_btn_purple_gray_selector));
                tvStage.setText(getContext().getString(R.string.upgrade_tips));
                tvStage.setTextColor(getContext().getResources().getColor(R.color.white_ffffff));

                tvValue.setTag(position);
                tvValue.setOnClickListener(mOnClickListener);
                tvStage.setTag(position);
                tvStage.setOnClickListener(mOnClickListener);
                tvValue.performClick();
                break;
            case OtaStageInfo.STAGE_PREPARE: //准备（下载升级文件）
                rlUpgradeStage.setVisibility(View.GONE);
                rlProgress.setVisibility(View.VISIBLE);
                pbProgress.setProgress(info.getProgress());
                String text = info.getProgress() + " %";
                tvProgress.setText(text);
                break;
            case OtaStageInfo.STAGE_UPGRADE: //准备就绪，可以升级
                rlProgress.setVisibility(View.GONE);
                rlUpgradeStage.setVisibility(View.VISIBLE);
                avLoading.setVisibility(View.INVISIBLE);
                tvValue.setText(info.getMessage());
                tvStage.setEnabled(true);
//                tvStage.setBackground(getContext().getResources().getDrawable(R.drawable.bg_btn_purple_gray_selector));
//                tvStage.setText(getContext().getString(R.string.upgrade_tips));
                tvStage.setBackground(getContext().getResources().getDrawable(R.color.color_transparent));
                tvStage.setText("");
                tvStage.setTextColor(getContext().getResources().getColor(R.color.white_ffffff));

                tvValue.setTag(position);
                tvValue.setOnClickListener(mOnClickListener);
                tvStage.setTag(position);
                tvStage.setOnClickListener(mOnClickListener);
//                tvStage.performClick();
                mOnFirmwareOtaListener.onStartOta();
                break;
            case OtaStageInfo.STAGE_UPGRADING: //升级中
                rlProgress.setVisibility(View.GONE);
                rlUpgradeStage.setVisibility(View.VISIBLE);
                avLoading.setVisibility(View.INVISIBLE);
                tvValue.setText(info.getMessage());
                tvStage.setEnabled(false);
                tvStage.setBackground(getContext().getResources().getDrawable(R.color.color_transparent));
                tvStage.setText(getContext().getString(R.string.upgrading_tips));
                tvStage.setTextColor(getContext().getResources().getColor(R.color.gray_text_989898));

                tvValue.setTag(position);
                tvValue.setOnClickListener(mOnClickListener);
                break;
        }
    }

    private final View.OnClickListener mOnClickListener = v -> {
        if (v == null) return;
        if (v.getId() == R.id.tv_firmware_upgrade_value) {
            int pos = (int) v.getTag();
            FirmwareOtaItem item = getItem(pos);
            if (item != null && mOnFirmwareOtaListener != null) {
                OtaStageInfo info = item.getOtaStageInfo();
                mOnFirmwareOtaListener.onShowMessage(info == null ? "" : info.getMessage());
            }
        } else if (v.getId() == R.id.tv_firmware_upgrade_stage) {
            int pos = (int) v.getTag();
            FirmwareOtaItem item = getItem(pos);
            if (item != null && mOnFirmwareOtaListener != null) {
                OtaStageInfo info = item.getOtaStageInfo();
                if (info != null) {
                    switch (info.getStage()) {
                        case OtaStageInfo.STAGE_IDLE:
                            mOnFirmwareOtaListener.onCheckUpgrade();
                            break;
                        case OtaStageInfo.STAGE_IDLE_DOWNLOAD:
                            mOnFirmwareOtaListener.onDownload(info);
                            break;
                        case OtaStageInfo.STAGE_UPGRADE:
                            mOnFirmwareOtaListener.onStartOta();
                            break;
                    }
                }
            }
        }
    };

    public interface OnFirmwareOtaListener {

        void onCheckUpgrade();

        void onDownload(OtaStageInfo info);

        void onShowMessage(String message);

        void onStartOta();
    }
}
