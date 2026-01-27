package com.jieli.btsmart.ui.soundcard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.soundcard.SoundCard;
import com.jieli.component.utils.ValueUtil;


/**
 * @author : chensenhua
 * @e-mail : chensenhua@zh-jieli.com
 * @date : 2020/11/19 11:29 AM
 * @desc : 分页容器
 */
@SuppressLint("ViewConstructor")
class PageContainer extends LinearLayout {
    private SoundCard.Functions functions;

    public PageContainer(Context context, SoundCard.Functions functions) {
        super(context);
        this.functions = functions;
        setOrientation(VERTICAL);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        //分页ui
        ViewPager2 viewPager2 = new ViewPager2(getContext());
        LayoutParams vp2Lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        viewPager2.setLayoutParams(vp2Lp);
        viewPager2.setOffscreenPageLimit(getPageSize()+1);

        addView(viewPager2);
        viewPager2.setAdapter(new RecyclerView.Adapter() {

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                FunctionContainer functionContainer = new FunctionContainer(getContext());
                functionContainer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                RecyclerView.ViewHolder viewHolder = new RecyclerView.ViewHolder(functionContainer) {
                };
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                FunctionContainer functionContainer = (FunctionContainer) holder.itemView;
                SoundCard.Functions f = new SoundCard.Functions();
                f.type = functions.type;
                f.title = functions.title;
                f.row = functions.row;
                f.id = functions.id;
                f.icon_url = functions.icon_url;
                f.column = functions.column;
                f.list = functions.list.subList(getPageSize() * position, Math.min((position + 1) * getPageSize(), functions.list.size()));
                functionContainer.setFunctions(f);
            }

            @Override
            public int getItemCount() {
                int pageSize = getPageSize();
                return pageSize == 0 ? 0 : (int) Math.ceil(functions.list.size() * 1.0 / pageSize);
            }

        });

        //指示器ui
        RecyclerView recyclerView = new RecyclerView(getContext());
        LayoutParams rvLp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        rvLp.gravity = Gravity.CENTER_HORIZONTAL;
        rvLp.topMargin = ValueUtil.dp2px(getContext(), 6);
        recyclerView.setLayoutParams(rvLp);
        addView(recyclerView);
        DotAdapter dotAdapter = new DotAdapter();
        recyclerView.setAdapter(dotAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false));

        //关联页面和指示器
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                dotAdapter.setSelectIndex(position);
            }
        });


    }

    private int getPageSize() {
        return functions.row * functions.column;
    }

    private class DotAdapter extends RecyclerView.Adapter
    {
        private int selectIndex = 0;

        public void setSelectIndex(int selectIndex) {
            this.selectIndex = selectIndex;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = new View(parent.getContext());
            view.setBackgroundResource(R.drawable.indicator_sound_card_page_dot);
            int size = ValueUtil.dp2px(parent.getContext(), 8);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(size,
                    size);
            view.setLayoutParams(lp);
            lp.leftMargin = lp.rightMargin = size / 2;
            return new RecyclerView.ViewHolder(view) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            holder.itemView.setSelected(position == selectIndex);
        }

        @Override
        public int getItemCount() {
            int pageSize = getPageSize();
            return pageSize == 0 ? 0 : (int) Math.ceil(functions.list.size() * 1.0 / pageSize);
        }

    }

}
