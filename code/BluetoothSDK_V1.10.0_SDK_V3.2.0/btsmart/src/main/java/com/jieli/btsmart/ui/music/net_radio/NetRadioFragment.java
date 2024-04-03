package com.jieli.btsmart.ui.music.net_radio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.flyco.tablayout.listener.CustomTabEntity;
import com.flyco.tablayout.listener.OnTabSelectListener;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.model.NetRadioTabEntity;
import com.jieli.btsmart.databinding.FragmentNetRadioBinding;
import com.jieli.btsmart.databinding.ItemCurrentRegionBinding;
import com.jieli.btsmart.databinding.ItemRegionCollectManageBinding;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.tool.room.DataRepository;
import com.jieli.btsmart.tool.room.entity.UserEntity;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseViewModelFragment;
import com.jieli.btsmart.ui.widget.NetRadioRegionDialog;
import com.jieli.btsmart.ui.widget.TipStateDialog;
import com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel;
import com.jieli.btsmart.viewmodel.NetRadioViewModel;
import com.jieli.component.utils.ToastUtil;
import com.jieli.jl_http.bean.NetRadioListInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/7 15:56
 * @desc :
 */
public class NetRadioFragment extends BaseViewModelFragment<FragmentNetRadioBinding> {
    private final ArrayList<CustomTabEntity> mTabEntities = new ArrayList<>();
    private CommonActivity mActivity;
    private NetRadioRegionDialog mRegionDialog;
    private boolean mOnceRefreshRadioIcon = true;
    private NetRadioViewModel mNetRadioViewModel;
    private NetRadioDetailsViewModel mNetRadioDetailsViewModel;
    //网络电台地区List
    //token
    private TipStateDialog mTipStateDialog;

    public NetRadioFragment() {
    }

    public static NetRadioFragment newInstance() {
        return new NetRadioFragment();
    }

    private final Fragment[] fragments = new Fragment[]{
            new NetRadioDetailsFragment(NetRadioDetailsFragment.TAG_LOCAL_NET_RADIO),
            new NetRadioDetailsFragment(NetRadioDetailsFragment.TAG_COUNTRY_NET_RADIO),
            new NetRadioDetailsFragment(NetRadioDetailsFragment.TAG_PROVINCE_NET_RADIO),
            new NetRadioDetailsFragment(NetRadioDetailsFragment.TAG_COLLECT_NET_RADIO)
    };

    @Override
    public int getLayoutId() {
        return R.layout.fragment_net_radio;
    }

    @Override
    public void actionsOnViewInflate() {
        super.actionsOnViewInflate();
        if (mActivity == null && getActivity() instanceof CommonActivity) {
            mActivity = (CommonActivity) getActivity();
        }
        mActivity.updateTopBar(getString(R.string.multi_media_net_radio), R.drawable.ic_back_black, v -> {
            if (mActivity != null) {
                mActivity.onBackPressed();
            }
        }, 0, null);
        for (Fragment fragment : fragments) {
            NetRadioDetailsFragment netRadioDetailsFragment = (NetRadioDetailsFragment) fragment;
            netRadioDetailsFragment.setParentView(mBinding.clLayout);
            netRadioDetailsFragment.setTargetView(mBinding.ivNetControlLogo);
        }
        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        mNetRadioViewModel = provider.get(NetRadioViewModel.class);
        mNetRadioDetailsViewModel = provider.get(NetRadioDetailsViewModel.class);
        requireActivity().getLifecycle().addObserver(mNetRadioViewModel);
        requireActivity().getLifecycle().addObserver(mNetRadioDetailsViewModel);
        mNetRadioDetailsViewModel.setNetRadioDetailsCallback(mNetRadioDetailsCallback);
        mBinding.setNetRadioViewModel(mNetRadioViewModel);
        mBinding.setNetRadioDetailsViewModel(mNetRadioDetailsViewModel);
        mBinding.setLifecycleOwner(this);
        saveUserEntity();
        observeNetRadioViewModel();
        initView();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetworkHelper.getInstance().registerNetworkEventCallback(mNetworkEventCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().getLifecycle().removeObserver(mNetRadioViewModel);
        requireActivity().getLifecycle().removeObserver(mNetRadioDetailsViewModel);
        NetworkHelper.getInstance().unregisterNetworkEventCallback(mNetworkEventCallback);
    }

    private void initView() {
        String[] mTitles = {getString(R.string.fm_type_local), getString(R.string.fm_type_country), getString(R.string.fm_type_province), getString(R.string.fm_type_mycollect)};
        for (String mTitle : mTitles) {
            mTabEntities.add(new NetRadioTabEntity(mTitle));
        }
        mBinding.navView.setTabData(mTabEntities);
        mBinding.navView.setCurrentTab(0);
        mBinding.navView.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelect(int position) {
                mBinding.vp2NetRadio.setCurrentItem(position, false);
            }

            @Override
            public void onTabReselect(int position) {

            }
        });
        mBinding.vp2NetRadio.setOffscreenPageLimit(4);
        mBinding.vp2NetRadio.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return fragments[position];
            }

            @Override
            public int getItemCount() {
                return fragments.length;
            }
        });
        mBinding.vp2NetRadio.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            public void onPageScrollStateChanged(@ViewPager2.ScrollState int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    mBinding.navView.setCurrentTab(mBinding.vp2NetRadio.getCurrentItem());
                }
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (mRegionDialog != null && mRegionDialog.isShowing()) {
                    mRegionDialog.dismissDialog(true);
                }
                mNetRadioDetailsViewModel.collectedManageStateLiveData.setValue(false);
                if (position == 2) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View view = inflater.inflate(R.layout.item_current_region, null);
                    ItemCurrentRegionBinding itemCurrentRegionBinding = DataBindingUtil.bind(view);
                    itemCurrentRegionBinding.setNetRadioDetailsViewModel(mNetRadioDetailsViewModel);
                    itemCurrentRegionBinding.setNetRadioViewModel(mNetRadioViewModel);
                    itemCurrentRegionBinding.setLifecycleOwner(NetRadioFragment.this);
                    view.setOnClickListener(v -> {
                        Log.e(TAG, "onClick: ");
                        if (mRegionDialog == null) {
                            mRegionDialog = new NetRadioRegionDialog(NetRadioFragment.this.getContext(), NetRadioFragment.this, mNetRadioDetailsViewModel);
                            mRegionDialog.setNetRegionsData(mNetRadioDetailsViewModel.netRadioRegionsListLiveData.getValue());
                            mRegionDialog.showDialog(mBinding.navView);
                            mNetRadioViewModel.dropDownState.setValue(true);
                            mRegionDialog.setDismissCallback(() -> {
                                mRegionDialog = null;
                                AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
                                alphaAnimation.setDuration(250);
                                mBinding.flCover.startAnimation(alphaAnimation);
                                mBinding.flCover.setVisibility(View.GONE);
                                mNetRadioViewModel.dropDownState.setValue(false);
                            });
                            AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                            alphaAnimation.setDuration(500);
                            mBinding.flCover.startAnimation(alphaAnimation);
                            mBinding.flCover.setVisibility(View.VISIBLE);
                        } else {
                            mRegionDialog.dismissDialog(true);
                        }
                    });
                    mActivity.updateTopBar(getString(R.string.multi_media_net_radio), null, view);
                } else if (position == 3) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    View view = inflater.inflate(R.layout.item_region_collect_manage, null);
                    ItemRegionCollectManageBinding itemBinding = DataBindingUtil.bind(view);
                    itemBinding.setManageState(mNetRadioDetailsViewModel.collectedManageStateLiveData);
                    itemBinding.setLifecycleOwner(NetRadioFragment.this);
                    view.setOnClickListener(v -> mNetRadioDetailsViewModel.collectedManageStateLiveData.setValue(!mNetRadioDetailsViewModel.collectedManageStateLiveData.getValue()));
                    mActivity.updateTopBar(getString(R.string.multi_media_net_radio), null, view);
                } else {
                    mActivity.updateTopBar(getString(R.string.multi_media_net_radio), null, null);
                }
            }
        });
        mBinding.vp2NetRadio.setUserInputEnabled(true);
        mBinding.flCover.setOnClickListener(v -> {
            if (mRegionDialog != null) {
                mRegionDialog.dismissDialog(true);
            }
        });
    }

    private void saveUserEntity() {
        UserEntity entity = new UserEntity();
        entity.setUserId(1);
        entity.setUserName("123456");
        entity.setPassword("123456");
        DataRepository.getInstance().insertUser(entity, null);
        mNetRadioViewModel.setUserEntity(entity);
        mNetRadioDetailsViewModel.setUserEntity(entity);
    }

    private void observeNetRadioViewModel() {
        mNetRadioViewModel.currentPlayRadioInfoLiveData.observe(this, radioListInfo -> {
            JL_Log.e(TAG, "currentPlayRadioInfoLiveData : " + radioListInfo.toString());
            if (mOnceRefreshRadioIcon) {//第一次打开主动刷新当前播放icon
                mOnceRefreshRadioIcon = false;
                Glide.with(requireContext()).load(radioListInfo.getIcon())/*.placeholder(R.drawable.ic_radio_placeholder)*/.error(R.drawable.ic_radio_placeholder).into(mBinding.ivNetControlLogo);
            }
        });
        mNetRadioViewModel.loadingFailedLiveData.observe(this, aBoolean -> {
            if (aBoolean) {
                showLoadingFailedDialog();
            }
        });
        mNetRadioViewModel.refreshIconLiveData.observe(this, aBoolean -> {//刷新当前播放icon
            if (aBoolean) {
                refreshCurrentPlayNetRadioIcon();
            }
        });
        mNetRadioViewModel.collectNetRadioLiveData.observe(NetRadioFragment.this, infos -> {//因为必须要有observe，MediatorLiveData才会触发onChange
        });
        mNetRadioDetailsViewModel.loadingFailedLiveData.observe(this, aBoolean -> {
            if (aBoolean) {
                showLoadingFailedDialog();
            }
        });
        mNetRadioDetailsViewModel.refreshIconLiveData.observe(this, aBoolean -> {//刷新当前播放icon
            if (aBoolean) {
                refreshCurrentPlayNetRadioIcon();
            }
        });
        mNetRadioDetailsViewModel.localNetRadioLiveData.observe(this, radioListInfos -> mNetRadioViewModel.setLocalNetRadioInfo(radioListInfos));
    }

    private void refreshCurrentPlayNetRadioIcon() {
        Glide.with(requireContext())
                .load(mNetRadioViewModel.currentPlayRadioInfoLiveData.getValue().getIcon())
                .dontAnimate()
//                .placeholder(R.drawable.ic_radio_placeholder)
                .error(R.drawable.ic_radio_placeholder).into(mBinding.ivNetControlLogo);
    }

    //不动
    private void showLoadingFailedDialog() {
        if (mTipStateDialog != null) {
            return;
        }
        mTipStateDialog = new TipStateDialog();
        mTipStateDialog.setImageResource(R.drawable.ic_fm_collect_freq_existed);
        mTipStateDialog.setTips(getString(R.string.loading_failed));
        mTipStateDialog.setCallback(() -> {
            mTipStateDialog = null;
            handler.removeMessages(MSG_DISMISS_TIP_STATE_DIALOG);
        });
        mTipStateDialog.show(getChildFragmentManager(), TipStateDialog.class.getSimpleName());
        handler.sendEmptyMessageDelayed(MSG_DISMISS_TIP_STATE_DIALOG, 700);
    }

    private final int MSG_DISMISS_TIP_STATE_DIALOG = 1;

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_DISMISS_TIP_STATE_DIALOG) {
                if (mTipStateDialog != null && mTipStateDialog.isShow()) {
                    mTipStateDialog.dismiss();
                    mTipStateDialog = null;
                }
            }
            return false;
        }
    });
    private final NetworkHelper.OnNetworkEventCallback mNetworkEventCallback = new NetworkHelper.OnNetworkEventCallback() {
        @Override
        public void onNetworkState(boolean isAvailable) {
            if (!isAvailable) {
                ToastUtil.showToastLong(R.string.no_network);
            }
        }

        @Override
        public void onUpdateConfigureSuccess() {
        }

        @Override
        public void onUpdateImage() {
        }

        @Override
        public void onUpdateConfigureFailed(int code, String message) {
        }
    };

    private final NetRadioDetailsViewModel.NetRadioDetailsCallback mNetRadioDetailsCallback = new NetRadioDetailsViewModel.NetRadioDetailsCallback() {
        @Override
        public void playRadioCallback(List<NetRadioListInfo> radioInfos, int position, int listType) {
            mNetRadioViewModel.handlePlayRadioCallback(radioInfos, position, listType);
        }

        @Override
        public void deleteCollectNetRadioCallback(NetRadioListInfo deleteNetRadio) {
            if (deleteNetRadio != null) {
                mNetRadioViewModel.handleDeleteCollectedNetRadio(deleteNetRadio);
            }
        }

        @Override
        public void addCollectRadioCallback(NetRadioListInfo addNetRadio) {
            if (addNetRadio != null) {
                mNetRadioViewModel.handleAddCollectedNetRadio(addNetRadio);
            }
        }
    };
}
