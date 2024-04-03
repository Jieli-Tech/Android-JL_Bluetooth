package com.jieli.btsmart.data.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseDataBindingHolder;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.ItemFreqCollectBinding;
import com.jieli.btsmart.tool.room.entity.FMCollectInfoEntity;
import com.jieli.btsmart.viewmodel.FMControlViewModel;

import java.lang.ref.WeakReference;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/1 15:10
 * @desc :
 */
public class FMFreqCollectAdapter extends BaseQuickAdapter<FMCollectInfoEntity, BaseDataBindingHolder<ItemFreqCollectBinding>> {
    private final WeakReference<Fragment> weakFragment;
    private final FMControlViewModel mFMControlViewModel;

    public FMFreqCollectAdapter(Fragment fragment, FMControlViewModel fmControlViewModel) {
        super(R.layout.item_freq_collect);
        weakFragment = new WeakReference<>(fragment);
        mFMControlViewModel = fmControlViewModel;
    }

    @Override
    protected void convert(@NonNull BaseDataBindingHolder<ItemFreqCollectBinding> itemFreqCollectBindingBaseDataBindingHolder, FMCollectInfoEntity fmCollectInfoEntity) {
        if (null == itemFreqCollectBindingBaseDataBindingHolder.getDataBinding()) return;
        if (weakFragment != null) {
            itemFreqCollectBindingBaseDataBindingHolder.getDataBinding().setLifecycleOwner(weakFragment.get());
        }
        itemFreqCollectBindingBaseDataBindingHolder.getDataBinding().setFmCollectInfoEntity(fmCollectInfoEntity);
        itemFreqCollectBindingBaseDataBindingHolder.getDataBinding().setFmControlViewModel(mFMControlViewModel);
        itemFreqCollectBindingBaseDataBindingHolder.getDataBinding().executePendingBindings();//立即执行
    }

}
