package com.jieli.btsmart.ui.multimedia;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.audio.media_player.Music;
import com.jieli.bluetooth.bean.base.BaseError;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.constant.StateCode;
import com.jieli.bluetooth.interfaces.rcsp.callback.OnRcspActionCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.FunctionListAdapter;
import com.jieli.btsmart.data.model.FunctionItemData;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.alarm.AlarmListFragment;
import com.jieli.btsmart.ui.home.HomeActivity;
import com.jieli.btsmart.ui.light.LightContainerFragment;
import com.jieli.btsmart.ui.multimedia.control.BlankControlFragment;
import com.jieli.btsmart.ui.multimedia.control.BlankMusicControlFragment;
import com.jieli.btsmart.ui.multimedia.control.FMControlFragment;
import com.jieli.btsmart.ui.multimedia.control.FMTXControlFragment;
import com.jieli.btsmart.ui.multimedia.control.ID3EmptyControlFragment;
import com.jieli.btsmart.ui.multimedia.control.LineInControlFragment;
import com.jieli.btsmart.ui.multimedia.control.MusicControlFragment;
import com.jieli.btsmart.ui.multimedia.control.NetRadioControlFragment;
import com.jieli.btsmart.ui.multimedia.control.id3.ID3ControlFragment;
import com.jieli.btsmart.ui.multimedia.control.id3.ID3ControlPresenterImpl;
import com.jieli.btsmart.ui.music.device.ContainerFragment;
import com.jieli.btsmart.ui.music.local.LocalMusicFragment;
import com.jieli.btsmart.ui.music.net_radio.NetRadioFragment;
import com.jieli.btsmart.ui.search.SearchDeviceFragment;
import com.jieli.btsmart.ui.search.SearchDeviceListFragment;
import com.jieli.btsmart.ui.soundcard.SoundCardFragment;
import com.jieli.btsmart.ui.widget.color_cardview.CardView;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.JLShakeItManager;
import com.jieli.btsmart.util.JL_MediaPlayerServiceManager;
import com.jieli.btsmart.util.PermissionUtil;
import com.jieli.component.base.BasePresenter;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.jl_dialog.Jl_Dialog;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

/**
 * 多媒体界面
 */
@RuntimePermissions
public class MultimediaFragment extends Jl_BaseFragment implements IMultiMediaContract.IMultiMediaView {
    private final JLShakeItManager mShakeItManager = JLShakeItManager.getInstance();

    private FrameLayout flMultiMediaShadow;
    private FrameLayout flControlSuspension;
    private ImageView arcView;
    private CardView cvMultimediaContainerSuspension;
    private RecyclerView rvMultiMediaFunctionList;
    private FrameLayout flControl;
    private Fragment mSuspensionFragment;
    private IMultiMediaContract.IMultiMediaPresenter mPresenter;
    private FunctionListAdapter mAdapter;
    private Fragment mCurrentControlFragment;

    public static MultimediaFragment newInstance() {
        return new MultimediaFragment();
    }

    public MultimediaFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multimedia, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mPresenter != null) {
            mPresenter.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPresenter != null) {
            mPresenter.getDeviceSupportFunctionList();
            mPresenter.refreshDevMsg();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPresenter != null) {
            mPresenter.stopUpdateDevMsg();
        }
    }

    @Override
    public void onDestroyView() {
        if (mPresenter != null) {
            mPresenter.destroy();
        }
        super.onDestroyView();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MultimediaFragmentPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void setPresenter(BasePresenter presenter) {
        if (mPresenter == null) {
            mPresenter = (IMultiMediaContract.IMultiMediaPresenter) presenter;
        }
    }

    private void initView(@NonNull View root) {
        flMultiMediaShadow = root.findViewById(R.id.fl_multimedia_shadow);
        flControlSuspension = root.findViewById(R.id.fl_control_suspension);
        arcView = root.findViewById(R.id.view_home_top_bg);
        cvMultimediaContainerSuspension = root.findViewById(R.id.cv_multimedia_container_suspension);
        rvMultiMediaFunctionList = root.findViewById(R.id.rv_multimedia_function_list);
        flControl = root.findViewById(R.id.fl_control);

        mAdapter = new FunctionListAdapter();
        mPresenter = new MultiMediaPresenterImpl(this);
        rvMultiMediaFunctionList.setLayoutManager(new GridLayoutManager(getContext(), 3));
        int spacing = ValueUtil.dp2px(getContext(), 4);
        rvMultiMediaFunctionList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view); // item position
                int current = position % 3;
                if (current == 1) {
                    outRect.right = spacing;
                } else if (current == 2) {
                    outRect.right = spacing;
                } else {
                    outRect.right = spacing;
                }
            }
        });
        rvMultiMediaFunctionList.setAdapter(mAdapter);
        if (!mPresenter.isDevConnected()) {
            mAdapter.setNewInstance(getFunctionItemDataList());
        }
        flMultiMediaShadow.setOnClickListener(v -> onDismissSuspensionShade());
        switchTopControlFragment(mPresenter.isDevConnected(), AttrAndFunCode.SYS_INFO_FUNCTION_BT);
        mAdapter.setOnItemClickListener((adapter, view1, position) -> {
            FunctionItemData itemData = (FunctionItemData) adapter.getData().get(position);
            if (!mPresenter.isDevConnected()) {
                if (SConstant.ALLOW_SWITCH_FUN_DISCONNECT) {
                    handleItemClickOnTest(itemData.getItemType());
                    return;
                } else {
                    if (itemData.getItemType() != FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE) {
                        ToastUtil.showToastShort(getString(R.string.first_connect_device));
                        return;
                    }
                }
            }
            if (!itemData.isSupport()) return;
            Bundle bundle = new Bundle();
            switch (itemData.getItemType()) {
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL:
                    goToLocalMusicFragment();
                    break;

                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD:
                    if (!FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_SD0) && !FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_SD1)) {
                        ToastUtil.showToastShort(getString(R.string.msg_read_file_err_offline));
                        return;
                    }
                    goToDevStorage(SDCardBean.SD);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB:
                    if (!FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_USB)) {
                        ToastUtil.showToastShort(getString(R.string.msg_read_file_err_offline));
                        return;
                    }
                    goToDevStorage(SDCardBean.USB);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX:
                    switchTopControlFragment(mPresenter.isDevConnected(), AttrAndFunCode.SYS_INFO_FUNCTION_FMTX);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM:
                    mPresenter.switchToFMMode();
//                    switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_FM);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN:
                    if (!FileBrowseManager.getInstance().isOnline(SDCardBean.INDEX_LINE_IN)) {
                        ToastUtil.showToastShort(getString(R.string.msg_read_file_err_offline));
                        return;
                    }
                    mPresenter.switchToLineInMode();
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LIGHT_SETTINGS:
                    ContentActivity.startActivity(getContext(), LightContainerFragment.class.getCanonicalName(), getString(R.string.multi_media_light_settings));
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_ALARM:
                    ContentActivity.startActivity(getContext(), AlarmListFragment.class.getCanonicalName(), getString(R.string.multi_media_alarm));
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE:
                    tryToRequestLocationPermission(0);
//                    if(mPresenter.isDevConnected()){
//                        bundle.putString(SConstant.KEY_SEARCH_DEVICE_ADDR, mPresenter.getConnectedDevice().getAddress());
//                    }
//                    CommonActivity.startCommonActivity(getActivity(), SearchDeviceFragment.class.getCanonicalName(), bundle);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO:
                    tryToRequestLocationPermission(1);
                    break;
                case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SOUND_CARD:
                    ContentActivity.startActivity(getContext(), SoundCardFragment.class.getCanonicalName(), R.string.multi_media_sound_card);
                    break;
            }
        });
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE})
    public void onExternalStoragePermission() {
        toLocalMusicFragment();
    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE})
    public void showRelationForExternalStoragePermission(PermissionRequest request) {
        showStoragePermissionDialog(request);
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE})
    public void onStorageDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_storage));
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @NeedsPermission({Manifest.permission.READ_MEDIA_AUDIO})
    public void onMediaAudioPermission() {
        toLocalMusicFragment();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnShowRationale({Manifest.permission.READ_MEDIA_AUDIO})
    public void onMediaAudioPermissionShowRationale(PermissionRequest request) {
        showStoragePermissionDialog(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @OnPermissionDenied({Manifest.permission.READ_MEDIA_AUDIO})
    public void onMediaAudioPermissionDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_storage));
    }

    @NeedsPermission({Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            /*Manifest.permission.READ_PHONE_STATE,*/})
    public void goToFragmentByType(int type) {
        toNeedLocationPermissionFragment(type);
    }

    @OnShowRationale({
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            /*Manifest.permission.READ_PHONE_STATE,*/
    })
    public void showRelationForLocationPermission(PermissionRequest request) {
        showLocationPermissionTipsDialog(request, -1);
    }

    @OnPermissionDenied({
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            /*Manifest.permission.READ_PHONE_STATE,*/
    })
    public void onLocationDenied() {
        showAppSettingDialog(getString(R.string.permissions_tips_02) + getString(R.string.permission_location));
    }

    private void goToLocalMusicFragment() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasSelfPermissions(requireContext(), Manifest.permission.READ_MEDIA_AUDIO)) {
                showStoragePermissionDialog(null);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!PermissionUtil.isHasStoragePermission(requireContext())){
                showStoragePermissionDialog(null);
                return;
            }
        }
        toLocalMusicFragment();
    }

    private void toLocalMusicFragment() {
        JL_MediaPlayerServiceManager.getInstance().bindService();
        ContentActivity.startActivity(getContext(), LocalMusicFragment.class.getCanonicalName(), getString(R.string.multi_media_local));
    }

    private void tryToRequestLocationPermission(int type) {
        if (!PermissionUtil.isHasLocationPermission(requireContext())) {
            showLocationPermissionTipsDialog(null, type);
            return;
        }
        toNeedLocationPermissionFragment(type);
    }

    private void toNeedLocationPermissionFragment(int type) {
        if (type == 1) {
            CommonActivity.startCommonActivity(getActivity(), NetRadioFragment.class.getCanonicalName());
        } else {
            CommonActivity.startCommonActivity(getActivity(), SearchDeviceListFragment.class.getCanonicalName());
        }
    }

    private void showLocationPermissionTipsDialog(PermissionRequest request, int type) {
        Jl_Dialog dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
                .content(getString(R.string.permissions_tips_01) + getString(R.string.permission_location))
                .right(getString(R.string.setting))
                .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    if (request != null) {
                        request.proceed();
                    } else {
                        MultimediaFragmentPermissionsDispatcher.goToFragmentByTypeWithPermissionCheck(this, type);
                    }
                })
                .left(getString(R.string.cancel))
                .leftColor(ContextCompat.getColor(requireContext(), R.color.gray_CECECE))
                .leftClickListener((v, dialogFragment) -> dialogFragment.dismiss())
                .cancel(false)
                .build();
        dialog.show(getChildFragmentManager(), "request_location_permission");
    }

    private void showStoragePermissionDialog(PermissionRequest request) {
        Jl_Dialog dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
                .content(getString(R.string.permissions_tips_01) + getString(R.string.permission_storage))
                .right(getString(R.string.setting))
                .rightColor(ContextCompat.getColor(requireContext(), R.color.blue_448eff))
                .rightClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    if (request != null) {
                        request.proceed();
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            MultimediaFragmentPermissionsDispatcher.onMediaAudioPermissionWithPermissionCheck(this);
                        } else {
                            MultimediaFragmentPermissionsDispatcher.onExternalStoragePermissionWithPermissionCheck(this);
                        }
                    }
                })
                .left(getString(R.string.cancel))
                .leftColor(ContextCompat.getColor(requireContext(), R.color.gray_CECECE))
                .leftClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                    if (request != null) request.cancel();
                })
                .cancel(false)
                .build();
        dialog.show(getChildFragmentManager(), "request_storage_permission");
    }

    private void showAppSettingDialog(String content) {
        Jl_Dialog jl_dialog = Jl_Dialog.builder()
                .title(getString(R.string.permission))
                .content(content)
                .right(getString(R.string.setting))
                .rightClickListener((v, dialogFragment) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                    dialogFragment.dismiss();
                })
                .left(getString(R.string.cancel))
                .leftClickListener((v, dialogFragment) -> {
                    dialogFragment.dismiss();
                })
                .build();
        jl_dialog.show(getChildFragmentManager(), "showAppSettingDialog");
    }

    // 测试使用
    private void handleItemClickOnTest(int type) {
        Bundle bundle = new Bundle();
        switch (type) {
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL:
                ContentActivity.startActivity(getContext(), LocalMusicFragment.class.getCanonicalName(), getString(R.string.multi_media_local));
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD:
                bundle.putInt(ContainerFragment.KEY_TYPE, 0);
                ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB:
                bundle.putInt(ContainerFragment.KEY_TYPE, 1);
                ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX:
                switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_FMTX);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM:
                switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_FM);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN:
                switchTopControlFragment(true, AttrAndFunCode.SYS_INFO_FUNCTION_AUX);
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LIGHT_SETTINGS:
                ContentActivity.startActivity(getContext(), LightContainerFragment.class.getCanonicalName(), getString(R.string.multi_media_light_settings));
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_ALARM:
                ContentActivity.startActivity(getContext(), AlarmListFragment.class.getCanonicalName(), getString(R.string.multi_media_alarm));
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE:
                CommonActivity.startCommonActivity(getActivity(), SearchDeviceFragment.class.getCanonicalName());
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO:
                CommonActivity.startCommonActivity(getActivity(), NetRadioFragment.class.getCanonicalName());
                break;
            case FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SOUND_CARD:
                ContentActivity.startActivity(getActivity(), SoundCardFragment.class.getCanonicalName(), R.string.multi_media_sound_card);
                break;
        }
    }


    //生成所有功能项列表
    public ArrayList<FunctionItemData> getFunctionItemDataList() {
        String[] nameArray = getResources().getStringArray(R.array.multi_media_function_name_arr);
        TypedArray selectIconArray = getResources().obtainTypedArray(R.array.multi_media_function_sel_icon_arr);
        TypedArray unselectedIconArray = getResources().obtainTypedArray(R.array.multi_media_function_un_sel_icon_arr);
        ArrayList<FunctionItemData> list = new ArrayList<>();
        for (int i = 0; i < nameArray.length; i++) {
            FunctionItemData itemData = new FunctionItemData();
            String name = nameArray[i];
            int selIconResId = selectIconArray.getResourceId(i, R.drawable.ic_local_music_blue);
            int unSelIconResId = unselectedIconArray.getResourceId(i, R.drawable.ic_local_music_gray);
            itemData.setName(name);
            itemData.setNoSupportIconResId(unSelIconResId);
            itemData.setSupportIconResId(selIconResId);
            itemData.setItemType(i);
            itemData.setSupport(itemData.getItemType() == FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SEARCH_DEVICE);
            list.add(itemData);
        }
        selectIconArray.recycle();
        unselectedIconArray.recycle();
        return list;
    }


    //更新功能列表
    @Override
    public void updateFunctionItemDataList(ArrayList<FunctionItemData> arrayList) {
        if (isAdded() && !isDetached()) {
            mAdapter.setNewInstance(arrayList);
        }
    }

    @Override
    public void switchTopControlFragment(boolean isConnect, byte fun) {
        JL_Log.d(TAG, "switchTopControlFragment= isConnect=" + isConnect + "\tfun=" + fun);
        Fragment fragment;
        if (!isConnect) {
            if (ALLOW_SWITCH_FUN_DISCONNECT && mCurrentControlFragment != null) {
                return;
            }
            fragment = BlankControlFragment.newInstanceForCache(getChildFragmentManager());
            dismissSuspensionFragment(mSuspensionFragment);
            onDismissSuspensionShade();
        } else {
            if (fun != AttrAndFunCode.SYS_INFO_FUNCTION_FM) {
                mHandler.removeMessages(MSG_DISMISS_SUSPENSION);
                mHandler.sendEmptyMessageDelayed(MSG_DISMISS_SUSPENSION, 70);
            }
            if (fun == AttrAndFunCode.SYS_INFO_FUNCTION_BT && mPresenter.getDeviceInfo() != null
                    && (!mPresenter.getDeviceInfo().isBtEnable() || mPresenter.getDeviceInfo().isSupportDoubleConnection())) {
                fun = -1;
            }
            mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_DEFAULT);
            JL_Log.d(TAG, "switchTopControlFragment= after fun=" + fun);
            switch (fun) {
                case AttrAndFunCode.SYS_INFO_FUNCTION_BT:
                case AttrAndFunCode.SYS_INFO_FUNCTION_LOW_POWER://低功耗模式
                    List<Music> dataList = JL_MediaPlayerServiceManager.getInstance().getLocalMusic();
                    if (dataList == null || dataList.size() <= 0) {
                        fragment = BlankMusicControlFragment.newInstanceForCache(getChildFragmentManager());
                    } else {
                        fragment = MusicControlFragment.newInstanceForCache(getChildFragmentManager());
                    }
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC://音乐模式
                    fragment = MusicControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_ID3:
                    mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_ID3);
                    if (mCurrentControlFragment instanceof ID3ControlFragment) return;
                    fragment = ID3ControlFragment.newInstanceForCache(getChildFragmentManager());
                    Bundle bundle = new Bundle();
                    /*JL_Log.e("ZHM-id3","mPresenter.getCurrentID3Info() :"+mPresenter.getCurrentID3Info());*/
                    bundle.putParcelable(SConstant.KEY_ID3_INFO, mPresenter.getCurrentID3Info());
                    ((ID3ControlFragment) fragment).setBundle(bundle);
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_ID3_EMPTY:
                    if (mCurrentControlFragment instanceof ID3EmptyControlFragment) return;
                    fragment = ID3EmptyControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FMTX:
                    mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_FM);
                    if (mCurrentControlFragment instanceof FMTXControlFragment) return;
                    fragment = FMTXControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FM:
                    mShakeItManager.setCutSongType(JLShakeItManager.MODE_CUT_SONG_TYPE_FM);
                    if (mSuspensionFragment instanceof FMControlFragment) return;
                    mHandler.removeMessages(MSG_DISMISS_SUSPENSION);
                    fragment = FMControlFragment.newInstanceForCache(getChildFragmentManager());
                    FMControlFragment fmReceiveControlFragment = (FMControlFragment) fragment;
                    fmReceiveControlFragment.setFragmentCallback(new FMControlFragment.FragmentCallback() {
                        @Override
                        public void showFMSuspension() {
                            onShowSuspensionShade();
                        }

                        @Override
                        public void dismissFMSuspension() {
                            onDismissSuspensionShade();
                        }
                    });
                    showSuspensionFragment(fmReceiveControlFragment);
                    switchFunctionSelect(isConnect, fun);
                    return;
                case AttrAndFunCode.SYS_INFO_FUNCTION_AUX:
                    fragment = LineInControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_LIGHT:
                case MultiMediaPresenterImpl.FRAGMENT_NET_RADIO:
                    fragment = NetRadioControlFragment.newInstanceForCache(getChildFragmentManager());
                    break;
                default:
                    if (mPresenter.getDeviceInfo() != null && mPresenter.getDeviceInfo().isBtEnable()
                            && !mPresenter.getDeviceInfo().isSupportDoubleConnection()) {
                        fun = AttrAndFunCode.SYS_INFO_FUNCTION_BT;
                        List<Music> musicList = JL_MediaPlayerServiceManager.getInstance().getLocalMusic();
                        if (musicList == null || musicList.size() <= 0) {
                            fragment = BlankMusicControlFragment.newInstanceForCache(getChildFragmentManager());
                        } else {
                            fragment = MusicControlFragment.newInstanceForCache(getChildFragmentManager());
                        }
                    } else {
                        fragment = BlankControlFragment.newInstanceForCache(getChildFragmentManager());
                        Bundle bundle1 = new Bundle();
                        bundle1.putString(BlankControlFragment.KEY_CONTENT_TEXT, getString(R.string.no_data));
                        ((Jl_BaseFragment) fragment).setBundle(bundle1);
                        if (fragment.isAdded()) {
                            fragment.onResume();
                        }
                    }
                    break;
            }
        }
        switchFunctionSelect(isConnect, fun);
        if (fragment != null) {
            mCurrentControlFragment = fragment;
            JL_Log.d(TAG, "switchTopControlFragment= " + fragment.getClass().getSimpleName());
            changeFragment(R.id.fl_control, fragment, fragment.getClass().getSimpleName());
        }
    }

    private void switchFunctionSelect(boolean isSelect, Byte fun) {
        if (mAdapter == null || !isAdded() || isDetached()) return;
        if (!isSelect) {
            mAdapter.setSelectedType(-1);
        } else {
            switch (fun) {
                case AttrAndFunCode.SYS_INFO_FUNCTION_BT:
                case AttrAndFunCode.SYS_INFO_FUNCTION_LOW_POWER://低功耗模式
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LOCAL);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_AUX:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_LINEIN);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FM:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_FMTX:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_FM_TX);
                    break;
                case AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC:
                    DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
                    if (deviceInfo == null) return;
                    int deviceIndex = deviceInfo.getCurrentDevIndex();//读取当前播放的卡设备类型索引
                    if (deviceIndex == SDCardBean.INDEX_USB) {
                        mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_USB);
                    } else {
                        mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_SD_CARD);
                    }
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_NET_RADIO:
                    mAdapter.setSelectedType(FunctionItemData.FunctionItemType.FUNCTION_ITEM_TYPE_NET_RADIO);
                    break;
                case MultiMediaPresenterImpl.FRAGMENT_ID3:
                case MultiMediaPresenterImpl.FRAGMENT_ID3_EMPTY:
                    mAdapter.setSelectedType(-1);
                    break;
            }
        }
    }

    @Override
    public void onBtAdapterStatus(boolean enable) {

    }

    @Override
    public void onDeviceConnection(BluetoothDevice device, int status) {
        if (status == StateCode.CONNECTION_OK && mPresenter != null && mPresenter.isUsedDevice(device)) {
            mPresenter.getDeviceSupportFunctionList();
            mPresenter.refreshDevMsg();
            mPresenter.getCurrentModeInfo();
        }
    }

    @Override
    public void onSwitchDevice(BluetoothDevice device) {
        if (mPresenter != null) {
            mPresenter.getDeviceSupportFunctionList();
            mPresenter.refreshDevMsg();
            mPresenter.getCurrentModeInfo();
        }
    }

    @Override
    public void onChangePlayerFlag(int flag) {
        JL_Log.w(TAG, "onChangePlayerFlag : " + flag);
        if (mCurrentControlFragment instanceof ID3ControlFragment) {
            ((ID3ControlFragment) mCurrentControlFragment).updateShowPlayerFlag(flag);
        }
        boolean isDevConnect = mPresenter.isDevConnected() || ALLOW_SWITCH_FUN_DISCONNECT;
        switch (flag) {
            case ID3ControlPresenterImpl.PLAYER_FLAG_LOCAL:
                switchTopControlFragment(isDevConnect, AttrAndFunCode.SYS_INFO_FUNCTION_BT);
                break;
            case ID3ControlPresenterImpl.PLAYER_FLAG_OTHER:
                JL_Log.i(TAG, "onChangePlayerFlag: switchTopControlFragment To ID3");
                switchTopControlFragment(isDevConnect, MultiMediaPresenterImpl.FRAGMENT_ID3);
                break;
            case ID3ControlPresenterImpl.PLAYER_FLAG_NET_RADIO:
                switchTopControlFragment(isDevConnect, MultiMediaPresenterImpl.FRAGMENT_NET_RADIO);
                break;
            case ID3ControlPresenterImpl.PLAYER_FLAG_OTHER_EMPTY:
                switchTopControlFragment(isDevConnect, MultiMediaPresenterImpl.FRAGMENT_ID3_EMPTY);
                break;
        }
    }

    /**
     * 显示拓展部分
     */
    private void showSuspensionFragment(Fragment fragment) {
        mSuspensionFragment = fragment;
        cvMultimediaContainerSuspension.setVisibility(View.VISIBLE);
        changeFragment(R.id.fl_control_suspension, mSuspensionFragment, mSuspensionFragment.getClass().getSimpleName());
    }

    /**
     * 消失拓展部分
     */
    private void dismissSuspensionFragment(Fragment fragment) {
        if (mSuspensionFragment != null) {
            cvMultimediaContainerSuspension.setVisibility(View.GONE);
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(mSuspensionFragment);
            fragmentTransaction.commitAllowingStateLoss();
            mSuspensionFragment = null;
        }
    }

    /**
     * 显示拓展部分的阴影
     */
    public void onShowSuspensionShade() {
        if (getActivity() == null) return;
        HomeActivity homeActivity = (HomeActivity) requireActivity();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) arcView.getLayoutParams();
        layoutParams.setMargins(0, -(homeActivity.getToolbarHeight() + AppUtil.getStatusBarHeight(requireContext())), 0, 0);
        arcView.setLayoutParams(layoutParams);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(500);
        flMultiMediaShadow.startAnimation(alphaAnimation);
        flMultiMediaShadow.setVisibility(View.VISIBLE);
        cvMultimediaContainerSuspension.setVisibility(View.VISIBLE);
        arcView.setVisibility(View.VISIBLE);
    }

    /**
     * 消失拓展部分的阴影
     */
    private void onDismissSuspensionShade() {
        flMultiMediaShadow.setVisibility(View.GONE);
        arcView.setVisibility(View.GONE);
        if (mSuspensionFragment != null) {
            if (mSuspensionFragment instanceof FMControlFragment) {
                ((FMControlFragment) mSuspensionFragment).setFreqCollectManageState(false);
                ((FMControlFragment) mSuspensionFragment).setSuspensionState(false);
            }
        }
    }

    private int getOnLineDev(int type) {
        int devHandler = -1;
        List<SDCardBean> onLineDevices = FileBrowseManager.getInstance().getSdCardBeans();
        if (null != onLineDevices && !onLineDevices.isEmpty()) {
            for (SDCardBean sdCardBean : onLineDevices) {
                if (sdCardBean.getType() == type) {
                    devHandler = sdCardBean.getDevHandler();
                    break;
                }
            }
        }
        return devHandler;
    }

    private void toContainer(int type) {
        Bundle bundle = new Bundle();
        bundle.putInt(ContainerFragment.KEY_TYPE, type);
        ContentActivity.startActivity(getContext(), ContainerFragment.class.getCanonicalName(), bundle);
    }

    private void goToDevStorage(int type) {
        DeviceInfo deviceInfo = mPresenter.getDeviceInfo();
        if (deviceInfo != null && deviceInfo.getDevStorageInfo() != null && deviceInfo.getDevStorageInfo().isDeviceReuse()) {
            int devHandler = getOnLineDev(type);
            JL_Log.e(TAG, "goToDevStorage : " + devHandler + ", " + type);
            mPresenter.setDevStorage(devHandler, new OnRcspActionCallback<Boolean>() {
                @Override
                public void onSuccess(BluetoothDevice device, Boolean message) {
                    toContainer(type);
                }

                @Override
                public void onError(BluetoothDevice device, BaseError error) {
                    JL_Log.e(TAG, "goToDevStorage : " + error);
                    ToastUtil.showToastLong("设置存储设备出错\n" + error.getMessage());
                }
            });
            return;
        }
        toContainer(type);
    }

    private final int MSG_DISMISS_SUSPENSION = 1;
    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_DISMISS_SUSPENSION) {
                dismissSuspensionFragment(mSuspensionFragment);
            }
            return false;
        }
    });
}
