package com.jieli.btsmart.data.adapter;

import android.text.TextPaint;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.FunctionItemData;
import com.jieli.btsmart.ui.widget.color_cardview.CardView;

public class FunctionListAdapter extends BaseQuickAdapter<FunctionItemData, BaseViewHolder> {
    private int mSelectedType = -1;
//    private int width = 0;
    public FunctionListAdapter() {
        super(R.layout.item_function_list);
    }

    public void setSelectedType(int mSelectedType) {
        this.mSelectedType = mSelectedType;
        notifyDataSetChanged();
    }

    @Override
    protected void convert(BaseViewHolder baseViewHolder, FunctionItemData functionItemData) {
//        View itemView = baseViewHolder.itemView;
//        ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
//        if (width == 0){
//            width = (AppUtil.getScreenWidth(getContext())- ValueUtil.dp2px(getContext(),14))/3;
//        }
//        layoutParams.height = width;
//        itemView.setLayoutParams(layoutParams);
        CardView cardView = baseViewHolder.getView(R.id.cv_function_list);
        ImageView iconView = baseViewHolder.getView(R.id.iv_item_function_icon);
        TextView nameView = baseViewHolder.getView(R.id.tv_item_function_name);
        TextPaint paint = nameView.getPaint();
        paint.setFakeBoldText(true);
        nameView.setText(functionItemData.getName());
//        if (functionItemData.isSupport()) {
            iconView.setImageResource(functionItemData.getSupportIconResId());
            nameView.setTextColor(getContext().getResources().getColor(R.color.black_000000));
        /*} else {
            iconView.setImageResource(functionItemData.getNoSupportIconResId());
            nameView.setTextColor(getContext().getResources().getColor(R.color.gray_text_767676));
        }*/
        cardView.setSelected(functionItemData.getItemType() == mSelectedType);
    }

}
