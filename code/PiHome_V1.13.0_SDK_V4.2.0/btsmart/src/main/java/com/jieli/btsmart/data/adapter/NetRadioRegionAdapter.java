package com.jieli.btsmart.data.adapter;

import androidx.fragment.app.Fragment;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.ItemNetRadioRegionBinding;
import com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel;
import com.jieli.jl_http.bean.NetRadioRegionInfo;

import java.lang.ref.WeakReference;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/9 9:20
 * @desc :
 */
public class NetRadioRegionAdapter extends BaseQuickAdapter<NetRadioRegionInfo, BaseDataBindingHolder<ItemNetRadioRegionBinding>> {
    private final WeakReference<Fragment> fragmentWeakReference;
    private final NetRadioDetailsViewModel netRadioDetailsViewModel;

    public NetRadioRegionAdapter(Fragment fragment, NetRadioDetailsViewModel netRadioViewModel) {
        super(R.layout.item_net_radio_region);
        fragmentWeakReference = new WeakReference<>(fragment);
        this.netRadioDetailsViewModel = netRadioViewModel;
    }

    @Override
    protected void convert(BaseDataBindingHolder<ItemNetRadioRegionBinding> itemNetRadioRegionBindingBaseDataBindingHolder, NetRadioRegionInfo regionInfo) {
        ItemNetRadioRegionBinding mBinding = itemNetRadioRegionBindingBaseDataBindingHolder.getDataBinding();
        if (mBinding == null) return;
        if (netRadioDetailsViewModel != null) {
            mBinding.setNetRadioDetailsViewModel(netRadioDetailsViewModel);
            mBinding.setRegionInfo(regionInfo);
        }
        if (fragmentWeakReference != null && fragmentWeakReference.get() != null) {
            mBinding.setLifecycleOwner(fragmentWeakReference.get());
        }
    }
}
