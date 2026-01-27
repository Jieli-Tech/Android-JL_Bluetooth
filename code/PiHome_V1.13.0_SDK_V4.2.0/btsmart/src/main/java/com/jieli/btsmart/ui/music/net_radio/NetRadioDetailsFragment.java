package com.jieli.btsmart.ui.music.net_radio;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterInside;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.NetRadioInfoAdapter;
import com.jieli.btsmart.databinding.FragmentNetRadioDetailsBinding;
import com.jieli.btsmart.tool.network.NetworkHelper;
import com.jieli.btsmart.util.AnimationUtil;
import com.jieli.btsmart.util.GlideRoundTransform;
import com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel;
import com.jieli.jl_http.bean.NetRadioListInfo;
import com.jieli.jl_http.bean.NetRadioRegionInfo;

import java.util.List;

import static com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel.NET_RADIO_PLAY_LIST_TYPE_COLLECT;
import static com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel.NET_RADIO_PLAY_LIST_TYPE_COUNTRY;
import static com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel.NET_RADIO_PLAY_LIST_TYPE_LOCAL;
import static com.jieli.btsmart.viewmodel.NetRadioDetailsViewModel.NET_RADIO_PLAY_LIST_TYPE_PROVINCE;


/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/7 18:47
 * @desc :
 */
public class NetRadioDetailsFragment extends Fragment {
    private final String TAG = this.getClass().getSimpleName();
    private FragmentNetRadioDetailsBinding mBinding;
    private List<NetRadioListInfo> mNetRadioListInfo;
    private String tag;
    public static final String TAG_LOCAL_NET_RADIO = "tag_local_net_radio";
    public static final String TAG_COUNTRY_NET_RADIO = "tag_country_net_radio";
    public static final String TAG_PROVINCE_NET_RADIO = "tag_province_net_radio";
    public static final String TAG_COLLECT_NET_RADIO = "tag_collect_net_radio";
    public static final int MAG_CHECK_IS_NEED_REFRESH_FRAGMENT = 1234;
    private volatile boolean isFinishRefresh = true;
    private volatile boolean isRefreshing;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean isFirstShowFragment = true;
    volatile boolean isHandleOnClick = false;
    private ViewGroup mUpperParentView;
    private View mTargetView;
    private String mLoadedProvinceName = null;
    private NetRadioDetailsViewModel mDetailsViewModel;

    public NetRadioDetailsFragment(String tag) {
        this.tag = tag;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewModelProvider provider = new ViewModelProvider(getActivity());
        mDetailsViewModel = provider.get(NetRadioDetailsViewModel.class);
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_net_radio_details, container, false);
        initView();
        return mBinding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetworkHelper.getInstance().registerNetworkEventCallback(mNetworkEventCallback);
    }

    void initView() {
        mBinding.rvNetRadioDetails.setLayoutManager(new GridLayoutManager(getContext(), 3));
        NetRadioInfoAdapter adapter = new NetRadioInfoAdapter(this);
        adapter.setNetRadioDetailsViewModel(mDetailsViewModel);
        adapter.setHasStableIds(true);
        adapter.setOnItemClickListener((adapter1, view, position) -> {
                    if (isHandleOnClick || mUpperParentView == null || mTargetView == null) return;
                    if (!NetworkHelper.getInstance().checkNetworkAvailableAndToast()) return;
                    isHandleOnClick = true;
                    ImageView imageView = view.findViewById(R.id.imageView);
                    Context context = NetRadioDetailsFragment.this.getContext();
                    ImageView animView = new ImageView(context);
                    String url = mNetRadioListInfo.get(position).getIcon();
                    assert context != null;
                    Glide.with(context).load(url).transform(new CenterInside(), new GlideRoundTransform(7)).error(R.drawable.ic_radio_placeholder).into(animView);
                    AnimationUtil.parabolaAnimation(mUpperParentView, imageView, mTargetView, animView, (animation, canceled, value, velocity) -> {
                        mDetailsViewModel.refreshIconLiveData.setValue(true);
                        isHandleOnClick = false;
                    });
                    int playListType = -1;
                    switch (tag) {
                        case TAG_LOCAL_NET_RADIO:
                            playListType = NET_RADIO_PLAY_LIST_TYPE_LOCAL;
                            break;
                        case TAG_COUNTRY_NET_RADIO:
                            playListType = NET_RADIO_PLAY_LIST_TYPE_COUNTRY;
                            break;
                        case TAG_PROVINCE_NET_RADIO:
                            playListType = NET_RADIO_PLAY_LIST_TYPE_PROVINCE;
                            break;
                        case TAG_COLLECT_NET_RADIO:
                            playListType = NET_RADIO_PLAY_LIST_TYPE_COLLECT;
                            break;
                    }
                    mDetailsViewModel.playRadio(mNetRadioListInfo, position, playListType);
                }
        );
        mBinding.rvNetRadioDetails.setAdapter(adapter);
        mBinding.refreshLayoutNetRadioDetails.setColorSchemeColors(getResources().getColor(android.R.color.black),
                getResources().getColor(android.R.color.darker_gray),
                getResources().getColor(android.R.color.black),
                getResources().getColor(android.R.color.background_light));
        mBinding.refreshLayoutNetRadioDetails.setProgressBackgroundColorSchemeColor(Color.WHITE);
        mBinding.refreshLayoutNetRadioDetails.setSize(SwipeRefreshLayout.DEFAULT);
        mBinding.refreshLayoutNetRadioDetails.setOnRefreshListener(this::refreshView);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isFirstShowFragment) {
            isFirstShowFragment = false;
            getDataByTag();
            observeViewModel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        NetworkHelper.getInstance().unregisterNetworkEventCallback(mNetworkEventCallback);
    }

    /**
     * 设置顶级父view，用于做点击动画
     */
    public void setParentView(ViewGroup parentView) {
        mUpperParentView = parentView;
    }

    /**
     * 设置顶级父view，用于做点击动画
     */
    public void setTargetView(View targetView) {
        mTargetView = targetView;
    }

    public void setData(List<NetRadioListInfo> listInfos) {
        if (isEmptyToUpdateProvinceFragment(listInfos)) return;
        isFinishRefresh = true;
        mNetRadioListInfo = listInfos;
        if (mBinding != null && mBinding.rvNetRadioDetails.getAdapter() != null) {
            NetRadioInfoAdapter adapter = (NetRadioInfoAdapter) mBinding.rvNetRadioDetails.getAdapter();
            adapter.setNewInstance(mNetRadioListInfo);
        }
    }

    public void refreshView() {
        isFinishRefresh = false;
        if (!isRefreshing) {
            changeRefreshingState(true);
            getDataByTag();
        }
        mHandler.postDelayed(() -> {
            if (isRefreshing) {
                changeRefreshingState(false);
            }
        }, 800);
    }

    private boolean isEmptyToUpdateProvinceFragment(List<NetRadioListInfo> listInfos) {
        boolean result = false;
        boolean isEmptyDataInfo = listInfos == null || listInfos.isEmpty();
        boolean isProvinceFragment = tag.equals(TAG_PROVINCE_NET_RADIO);
        if (isProvinceFragment && !isEmptyDataInfo) {
            synLoadedSuccessProvinceName();
        } else if (isProvinceFragment && isSameProvinceNameWithLoaded()) {
            result = true;
        }
        return result;
    }

    private void synLoadedSuccessProvinceName() {
        NetRadioRegionInfo browseRegionInfo = mDetailsViewModel.browseRegionLiveData.getValue();
        if (browseRegionInfo != null) {
            mLoadedProvinceName = browseRegionInfo.getName();
        }
    }

    private boolean isSameProvinceNameWithLoaded() {
        boolean result = false;
        NetRadioRegionInfo browseRegionInfo = mDetailsViewModel.browseRegionLiveData.getValue();
        String targetBrowseProvince = null;
        if (browseRegionInfo != null) {
            targetBrowseProvince = browseRegionInfo.getName();
        }
        boolean bothIsNotEmpty = (targetBrowseProvince != null && mLoadedProvinceName != null);
        if (bothIsNotEmpty && mLoadedProvinceName.equals(targetBrowseProvince)) {
            result = true;
        }
        return result;
    }

    private void getDataByTag() {
        if (mDetailsViewModel == null) return;
        switch (NetRadioDetailsFragment.this.tag) {
            case TAG_LOCAL_NET_RADIO:
                mDetailsViewModel.getLocalNetRadios();
                break;
            case TAG_COUNTRY_NET_RADIO:
                mDetailsViewModel.getCountryNetRadios();
                break;
            case TAG_PROVINCE_NET_RADIO:
                mDetailsViewModel.getProvinceNetRadios();
                break;
            case TAG_COLLECT_NET_RADIO:
                mDetailsViewModel.getCollectedNetRadios();
                break;
        }
    }

    private void changeRefreshingState(boolean refreshingState) {
        setRefreshing(refreshingState);
        if (isAdded() && !isDetached()) {
            mBinding.refreshLayoutNetRadioDetails.setRefreshing(refreshingState);
        }
    }

    private void observeViewModel() {
        switch (NetRadioDetailsFragment.this.tag) {
            case TAG_LOCAL_NET_RADIO:
                mDetailsViewModel.localNetRadioLiveData.observe(this, mNetRadioListInfoObserver);
                break;
            case TAG_COUNTRY_NET_RADIO:
                mDetailsViewModel.countryNetRadioLiveData.observe(this, mNetRadioListInfoObserver);
                break;
            case TAG_PROVINCE_NET_RADIO:
                mDetailsViewModel.provinceNetRadioLiveData.observe(this, mNetRadioListInfoObserver);
                break;
            case TAG_COLLECT_NET_RADIO:
                mDetailsViewModel.collectNetRadioLiveData.observe(this, mNetRadioListInfoObserver);
                break;
        }
    }

    private void setRefreshing(boolean refreshing) {
        isRefreshing = refreshing;
    }

    private final Handler handler = new Handler(msg -> {
        if (msg.what == MAG_CHECK_IS_NEED_REFRESH_FRAGMENT) {
            if (!isFinishRefresh) getDataByTag();
        }
        return false;
    });

    private final NetworkHelper.OnNetworkEventCallback mNetworkEventCallback = new NetworkHelper.OnNetworkEventCallback() {
        @Override
        public void onNetworkState(boolean isAvailable) {
            if (isAvailable) {
                handler.sendEmptyMessageDelayed(MAG_CHECK_IS_NEED_REFRESH_FRAGMENT,80L);
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

    private final Observer<List<NetRadioListInfo>> mNetRadioListInfoObserver = NetRadioDetailsFragment.this::setData;
}
