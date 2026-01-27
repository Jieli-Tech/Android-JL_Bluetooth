package com.jieli.btsmart.ui.multimedia.control.fm;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.data.adapter.FMFreqCollectAdapter;
import com.jieli.btsmart.databinding.FragmentFmControlBinding;
import com.jieli.btsmart.tool.bluetooth.rcsp.DeviceOpHandler;
import com.jieli.btsmart.tool.room.DataRepository;
import com.jieli.btsmart.ui.base.BaseViewModelFragment;
import com.jieli.btsmart.ui.widget.GridSpacingItemDecoration;
import com.jieli.btsmart.ui.widget.InputTextDialog;
import com.jieli.btsmart.ui.widget.TipStateDialog;
import com.jieli.btsmart.ui.widget.rulerview.RulerView;
import com.jieli.btsmart.util.JLShakeItManager;
import com.jieli.btsmart.viewmodel.FMControlViewModel;
import com.jieli.component.utils.ValueUtil;
import com.jieli.jl_dialog.Jl_Dialog;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;
import static com.jieli.btsmart.util.JLShakeItManager.MODE_CUT_SONG_TYPE_FM;

public class FMControlFragment extends BaseViewModelFragment<FragmentFmControlBinding> {
    private Jl_Dialog scanDialog;
    private FMControlViewModel mFMControlViewModel;
    private FragmentCallback mCallback;
    private InputTextDialog mInputTextDialog;
    private TipStateDialog mTipStateDialog;
    private final MutableLiveData<Boolean> suspensionLiveData = new MutableLiveData<>(false);
    private boolean skipFirstTime = false;
    private final RCSPController mRCSPController = RCSPController.getInstance();
    private final JLShakeItManager mShakeItManager = JLShakeItManager.getInstance();

    public static FMControlFragment newInstance() {
        return new FMControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        Fragment fragment = fragmentManager.findFragmentByTag(FMControlFragment.class.getSimpleName());
        if (fragment != null) {
            return fragment;
        }
        return new FMControlFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        skipFirstTime = false;
        mShakeItManager.getOnShakeItStartLiveData().observeForever(mode -> {
            if (!skipFirstTime) {
                skipFirstTime = true;
                return;
            }
            if (mode == JLShakeItManager.SHAKE_IT_MODE_CUT_SONG && isVisible()) {
                if (mShakeItManager.getCutSongType() == MODE_CUT_SONG_TYPE_FM) {
                    if (mFMControlViewModel != null) {
                        mFMControlViewModel.onFMPlayNextChannel();
                    }
                }
            }
        });
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_fm_control;
    }

    @Override
    public void actionsOnViewInflate() {
        super.actionsOnViewInflate();
        ViewModelProvider provider = new ViewModelProvider(this);
        mFMControlViewModel = provider.get(FMControlViewModel.class);
        mBinding.setFmControlViewModel(mFMControlViewModel);
        mBinding.setFmReceiveSuspension(suspensionLiveData);
        mBinding.setLifecycleOwner(this);
        mBinding.getRoot().setOnClickListener(v -> {
        });//拦截点击事件而已
        mBinding.ibFmReceiveMore.setOnClickListener(v -> {
            if (mCallback != null && null != suspensionLiveData.getValue()) {
                if (!suspensionLiveData.getValue()) {
                    suspensionLiveData.setValue(true);
                    mCallback.showFMSuspension();
                } else {
                    suspensionLiveData.setValue(false);
                    mCallback.dismissFMSuspension();
                }
            }
        });
        mBinding.layoutFmCommon.tvFmCurrentChannel.setOnClickListener(v -> showInputTextDialog((String) ((TextView) v).getText()));
        mBinding.rvFmReceiveFreqCollect.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        mBinding.rvFmReceiveFreqCollect.setAdapter(new FMFreqCollectAdapter(this, mFMControlViewModel));
        mBinding.rvFmReceiveFreqCollect.addItemDecoration(new GridSpacingItemDecoration(3, ValueUtil.dp2px(requireContext(), 10), false));
        observerFMControlViewModel();
        mBinding.layoutFmCommon.rvFm.setMinValueAndMaxValue(875, -1);
        mBinding.layoutFmCommon.rvFm.setActivity(getActivity());
        mBinding.layoutFmCommon.rvFm.setOnValueChangeListener(new RulerView.OnValueChangeListener() {
            @Override
            public void onChange(int value) {
                mFMControlViewModel.fmCurrentFreqLiveData.setValue(value);
            }

            @Override
            public void onActionUp(int value) {
                JL_Log.d(TAG, "   onActionUp  : ");
                mFMControlViewModel.setIsViewFlingStop(true);
                mFMControlViewModel.fmCurrentFreqLiveData.setValue(value);
                float channel = ((float) value) / ((float) 10);
                mFMControlViewModel.onFMPlaySelectFreq(channel);
            }

            @Override
            public void onFling(int value, boolean isFlingEnd) {
                JL_Log.d(TAG, "   onFling  : " + isFlingEnd);
                mFMControlViewModel.fmCurrentFreqLiveData.setValue(value);
                if (isFlingEnd) {
                    mFMControlViewModel.setIsViewFlingStop(true);
                    float channel = ((float) value) / ((float) 10);
                    mFMControlViewModel.onFMPlaySelectFreq(channel);
                } else {
                    mFMControlViewModel.setIsViewFlingStop(false);
                }
            }

            @Override
            public void onCanNotSlide() {
                showTips(getString(R.string.searching));
            }
        });
        mRCSPController.addBTRcspEventCallback(callback);
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (ALLOW_SWITCH_FUN_DISCONNECT) {
//            mBinding.layoutFmCommon.rvFm.setCurrentPosition(875);
//        }
    }

    @Override
    public void onStart() {
        super.onStart();
        String address = null;
        if (ALLOW_SWITCH_FUN_DISCONNECT) {
            address = mRCSPController.getUsingDevice() == null ? "11:22:33:44:55:66" : mRCSPController.getUsingDevice().getAddress();
        } else if (mRCSPController.isDeviceConnected()) {
            address = mRCSPController.getUsingDevice().getAddress();
        }
        if (address != null) {
            getFmCollectFreqListByAddress(address);
            mFMControlViewModel.getFMInfo();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        dismissScanDialog();
        mRCSPController.removeBTRcspEventCallback(callback);
    }

    private void getFmCollectFreqListByAddress(String address) {
        DataRepository.getInstance().getFMCollectInfo(address).observe(getViewLifecycleOwner(), fmCollectInfoEntities
                -> mFMControlViewModel.fmCollectFreqLiveData.setValue(fmCollectInfoEntities));
    }

    //设置收藏管理状态
    public void setFreqCollectManageState(boolean state) {
        if (mFMControlViewModel == null) return;
        mFMControlViewModel.fmCollectManageStateLiveData.setValue(state);
    }

    //设置拓展栏状态
    public void setSuspensionState(boolean suspensionState) {
        suspensionLiveData.setValue(suspensionState);
    }

    public void setFragmentCallback(FragmentCallback callback) {
        mCallback = callback;
    }

    private void observerFMControlViewModel() {
        //小机搜到的频道
        mFMControlViewModel.channelListLiveData.observe(this, integers -> mBinding.layoutFmCommon.rvFm.setChannelList(integers));
        //小机回复的FMStatusInfo
        mFMControlViewModel.fmStatusInfoLiveData.observe(this, fmStatusInfo -> {
            if (fmStatusInfo.getMode() == 0) {
                //fm
                mBinding.layoutFmCommon.rvFm.setMinValueAndMaxValue(875, -1);
            } else {
                //am
                mBinding.layoutFmCommon.rvFm.setMinValueAndMaxValue(760, -1);
            }
            mBinding.layoutFmCommon.rvFm.setCurrentPosition((int) (fmStatusInfo.getFreq() * 10));
        });
        //FM扫描频道过程弹窗
        mFMControlViewModel.scanDialogShowStateLiveData.observe(this, aBoolean -> {
            if (aBoolean) {
                mBinding.layoutFmCommon.rvFm.setCanDragState(false);
//                    showScanDialog();
            } else {
                mBinding.layoutFmCommon.rvFm.setCanDragState(true);
//                    dismissScanDialog();
            }
        });
        //搜索操作栏显示状态
        mFMControlViewModel.fmShowSearchLiveData.observe(this, aBoolean -> {
            WindowManager.LayoutParams lp = requireActivity().getWindow().getAttributes();
            if (aBoolean) {
                lp.alpha = 0.9f;//设置阴影透明度
            } else {
                lp.alpha = 1f;
            }
            requireActivity().getWindow().setAttributes(lp);
        });
        //收藏结果
        mFMControlViewModel.fmCollectStateLiveData.observe(this, aBoolean -> {
            if (mTipStateDialog != null) {
                return;
            }
            mTipStateDialog = new TipStateDialog();
            if (aBoolean) {
                mTipStateDialog.setImageResource(R.drawable.ic_fm_collect_freq_success);
                mTipStateDialog.setTips(getString(R.string.freq_point_collect_success));
            } else {
                mTipStateDialog.setImageResource(R.drawable.ic_fm_collect_freq_existed);
                mTipStateDialog.setTips(getString(R.string.freq_point_collect_existed));
            }
            mTipStateDialog.setCallback(() -> {
                mTipStateDialog = null;
                handler.removeMessages(MSG_DISMISS_TIP_STATE_DIALOG);
            });
            mTipStateDialog.show(getChildFragmentManager(), TipStateDialog.class.getSimpleName());
            handler.sendEmptyMessageDelayed(MSG_DISMISS_TIP_STATE_DIALOG, 700);
        });
        //FM当前选中频点
        mFMControlViewModel.fmSelectedFreqLiveData.observe(this, integer -> {
            if (ALLOW_SWITCH_FUN_DISCONNECT) {
                mBinding.layoutFmCommon.rvFm.setCurrentPosition(integer);
            }
        });
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

    private void showScanDialog() {
        if (scanDialog == null) {
            scanDialog = Jl_Dialog
                    .builder()
                    .width(0.8f)
                    .cancel(false)
                    .showProgressBar(true)
                    .title(getString(R.string.tips))
                    .content(getString(R.string.searching))
                    .leftClickListener((v, dialogFragment) -> {
                        mFMControlViewModel.onFMSearchStop();
                        dialogFragment.dismiss();
                    })
                    .left(getString(R.string.cancel))
                    .build();
        }
        if (!scanDialog.isShow()) {
            scanDialog.show(getChildFragmentManager(), "scandialog");
        }
    }

    private void dismissScanDialog() {
        if (scanDialog != null && scanDialog.isShow()) {
            scanDialog.dismiss();
            scanDialog = null;
        }
    }

    private void showInputTextDialog(String freqPoint) {
        if (!isAdded() || isDetached()) return;
        if (mInputTextDialog == null) {
            mInputTextDialog = new InputTextDialog.Builder()
                    .setWidth(0.9f)
                    .setCancelable(false)
                    .setTitle(getString(R.string.freq_point))
                    .setInputText(freqPoint)
                    .setEditTextType(InputTextDialog.EditTextType.EDIT_TEXT_TYPE_NUMBER)
                    .setLeftText(getString(R.string.cancel))
                    .setLeftColor(getResources().getColor(R.color.gray_text_989898))
                    .setRightText(getString(R.string.confirm))
                    .setRightColor(getResources().getColor(R.color.blue_448eff))
                    .setOnInputTextListener(new InputTextDialog.OnInputTextListener() {
                        @Override
                        public void onDismiss(InputTextDialog dialog) {

                        }

                        @Override
                        public void onInputText(InputTextDialog dialog, String text) {

                        }

                        @Override
                        public void onInputFinish(InputTextDialog dialog, String value, String lastValue) {
                            if (value.equals(lastValue)) {
                                showTips(getString(R.string.fm_value_no_change));
                                return;
                            }
                            if (Float.parseFloat(value) < 87.5 || Float.parseFloat(value) > 108) {
                                showTips(R.string.fm_range);
                                return;
                            }
                            if (value.contains(".")) {
                                String[] list = value.split("\\.");
                                if (list.length == 2) {//带小数点
                                    if (list[1].length() > 1) {
                                        showTips(R.string.fm_only_one_decimal);
                                        return;
                                    }
                                }
                                if (list.length == 1) {
                                    showTips(R.string.fm_no_decimal);
                                    return;
                                }
                            }
                            mFMControlViewModel.onFMPlaySelectFreq(Float.parseFloat(value));
                            dialog.dismiss();
                            mInputTextDialog = null;
                        }
                    })
                    .create();
        }
        mInputTextDialog.updateEditText();
        mInputTextDialog.updateDialog();
        if (!mInputTextDialog.isShow() && !isDetached() && getActivity() != null) {
            mInputTextDialog.show(getChildFragmentManager(), "input_text_dialog");
        }
    }

    private final BTRcspEventCallback callback = new BTRcspEventCallback() {

        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            suspensionLiveData.setValue(false);
            if (mCallback != null) {
                mCallback.dismissFMSuspension();
            }
        }

        @Override
        public void onConnection(BluetoothDevice device, int status) {
            if (DeviceOpHandler.getInstance().isReconnecting()) return;
            suspensionLiveData.setValue(false);
            if (mCallback != null) {
                mCallback.dismissFMSuspension();
            }
        }

        @Override
        public void onSwitchConnectedDevice(BluetoothDevice device) {
            super.onSwitchConnectedDevice(device);
            suspensionLiveData.setValue(false);
            if (mCallback != null) {
                mCallback.dismissFMSuspension();
            }
            if (isAdded() && !isDetached()) {
                String address = device.getAddress();
                getFmCollectFreqListByAddress(address);
            }
        }
    };

    public interface FragmentCallback {
        void showFMSuspension();

        void dismissFMSuspension();
    }
}
