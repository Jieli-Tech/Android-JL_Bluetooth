package com.jieli.btsmart.data.adapter;

import androidx.fragment.app.Fragment;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.ItemNetRadioBinding;
import com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel;
import com.jieli.jl_http.bean.NetRadioListInfo;

import java.lang.ref.WeakReference;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/1 15:10
 * @desc :
 */
public class NetRadioInfoAdapter extends BaseQuickAdapter<NetRadioListInfo, BaseDataBindingHolder<ItemNetRadioBinding>> {
    private final WeakReference<Fragment> weakFragment;
    private NetRadioDetailsViewModel mNetRadioDetailsViewModel;

    public NetRadioInfoAdapter(Fragment fragment) {
        super(R.layout.item_net_radio);
        weakFragment = new WeakReference<>(fragment);
    }

    @Override
    protected void convert(BaseDataBindingHolder<ItemNetRadioBinding> itemNetRadioBindingBaseDataBindingHolder, NetRadioListInfo info) {
        if(null == itemNetRadioBindingBaseDataBindingHolder.getDataBinding()) return;
        itemNetRadioBindingBaseDataBindingHolder.getDataBinding().setRadioInfo(info);
        if (mNetRadioDetailsViewModel != null) {
            itemNetRadioBindingBaseDataBindingHolder.getDataBinding().setViewModel(mNetRadioDetailsViewModel);
        }
        if (weakFragment != null) {
            itemNetRadioBindingBaseDataBindingHolder.getDataBinding().setLifecycleOwner(weakFragment.get());
        }
        itemNetRadioBindingBaseDataBindingHolder.getDataBinding().executePendingBindings();
    }

    public void setNetRadioDetailsViewModel(NetRadioDetailsViewModel viewModel) {
        mNetRadioDetailsViewModel = viewModel;
    }
}
