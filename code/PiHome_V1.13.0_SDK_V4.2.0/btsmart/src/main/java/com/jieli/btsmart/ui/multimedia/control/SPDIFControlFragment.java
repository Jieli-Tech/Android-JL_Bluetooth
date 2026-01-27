package com.jieli.btsmart.ui.multimedia.control;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.device.spdif.SPDIFAudioSourceInfo;
import com.jieli.bluetooth.bean.device.spdif.SPDIFPlayStatusInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentSpdifBinding;
import com.jieli.btsmart.ui.base.BaseViewModelFragment;
import com.jieli.btsmart.viewmodel.SPDIFControlViewModel;


/**
 * Use the {@link SPDIFControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SPDIFControlFragment extends BaseViewModelFragment<FragmentSpdifBinding> {
    private SPDIFControlViewModel mSPDIFControlViewModel;
    private int mSPDIFAudioSource;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    public SPDIFControlFragment() {

    }

    public static SPDIFControlFragment newInstance() {
        return new SPDIFControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(SPDIFControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(SPDIFControlFragment.class.getSimpleName());
        }
        return new SPDIFControlFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_spdif;
    }

    @Override
    public void actionsOnViewInflate() {
        super.actionsOnViewInflate();
        ViewModelProvider provider = new ViewModelProvider(this);
        mSPDIFControlViewModel = provider.get(SPDIFControlViewModel.class);
        mBinding.getRoot().setOnClickListener(v -> {
        });//拦截点击事件而已
        mBinding.ibPlayOrPause.setOnClickListener(view -> mSPDIFControlViewModel.playOrPause());
        mBinding.btnHdmi.setSelected(true);
        mBinding.btnHdmi.setOnClickListener(view -> {
            if (mSPDIFAudioSource != SPDIFAudioSourceInfo.AUDIO_SOURCE_HDMI) {
                mSPDIFControlViewModel.setAudioSourceHDMI();
            }
        });
        mBinding.btnOptical.setOnClickListener(view -> {
            if (mSPDIFAudioSource != SPDIFAudioSourceInfo.AUDIO_SOURCE_OPTICAL) {
                mSPDIFControlViewModel.setAudioSourceOptical();
            }
        });
        mBinding.btnCoaxial.setOnClickListener(view -> {
            if (mSPDIFAudioSource != SPDIFAudioSourceInfo.AUDIO_SOURCE_COAXIAL) {
                mSPDIFControlViewModel.setAudioSourceCoaxial();
            }
        });
        mSPDIFControlViewModel.spdifPlayStatusInfoLiveData.observe(this, new Observer<SPDIFPlayStatusInfo>() {
            @Override
            public void onChanged(SPDIFPlayStatusInfo spdifPlayStatusInfo) {
                mBinding.ibPlayOrPause.setSelected(spdifPlayStatusInfo.getPlayStatus() == SPDIFPlayStatusInfo.PLAY_STATUS_PLAY);
            }
        });
        mSPDIFControlViewModel.spdifAudioSourceInfoLiveData.observe(this, new Observer<SPDIFAudioSourceInfo>() {
            @Override
            public void onChanged(SPDIFAudioSourceInfo audioSourceInfo) {
                mSPDIFAudioSource = audioSourceInfo.getAudioSource();
                mBinding.btnHdmi.setSelected(mSPDIFAudioSource == SPDIFAudioSourceInfo.AUDIO_SOURCE_HDMI);
                mBinding.btnOptical.setSelected(mSPDIFAudioSource == SPDIFAudioSourceInfo.AUDIO_SOURCE_OPTICAL);
                mBinding.btnCoaxial.setSelected(mSPDIFAudioSource == SPDIFAudioSourceInfo.AUDIO_SOURCE_COAXIAL);
            }
        });
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
            mSPDIFControlViewModel.getSPDIFInfo();
        }
    }
}
