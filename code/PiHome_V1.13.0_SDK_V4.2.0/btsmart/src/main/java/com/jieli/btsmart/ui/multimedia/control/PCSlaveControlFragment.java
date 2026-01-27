package com.jieli.btsmart.ui.multimedia.control;

import static com.jieli.btsmart.constant.SConstant.ALLOW_SWITCH_FUN_DISCONNECT;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.bluetooth.bean.device.pc_slave.PCSlavePlayStatusInfo;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentPcSlaveBinding;
import com.jieli.btsmart.ui.base.BaseViewModelFragment;
import com.jieli.btsmart.viewmodel.PCSlaveControlViewModel;


/**
 * Use the {@link PCSlaveControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PCSlaveControlFragment extends BaseViewModelFragment<FragmentPcSlaveBinding> {
    private PCSlaveControlViewModel mPCSlaveControlViewModel;
    private final RCSPController mRCSPController = RCSPController.getInstance();

    public PCSlaveControlFragment() {

    }

    public static PCSlaveControlFragment newInstance() {
        return new PCSlaveControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(PCSlaveControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(PCSlaveControlFragment.class.getSimpleName());
        }
        return new PCSlaveControlFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_pc_slave;
    }

    @Override
    public void actionsOnViewInflate() {
        super.actionsOnViewInflate();
        ViewModelProvider provider = new ViewModelProvider(this);
        mPCSlaveControlViewModel = provider.get(PCSlaveControlViewModel.class);
        mBinding.getRoot().setOnClickListener(v -> {
        });//拦截点击事件而已
        mBinding.ibPlayNext.setOnClickListener(view -> mPCSlaveControlViewModel.playNextSong());
        mBinding.ibPlayPre.setOnClickListener(view -> mPCSlaveControlViewModel.playPreSong());
        mBinding.ibPlayOrPause.setOnClickListener(view -> mPCSlaveControlViewModel.playOrPause());
        mPCSlaveControlViewModel.pcSlavePlayStatusInfoLiveData.observe(this, new Observer<PCSlavePlayStatusInfo>() {
            @Override
            public void onChanged(PCSlavePlayStatusInfo pcSlavePlayStatusInfo) {
                mBinding.ibPlayOrPause.setSelected(pcSlavePlayStatusInfo.getPlayStatus() == PCSlavePlayStatusInfo.PLAY_STATUS_PLAY);
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
            mPCSlaveControlViewModel.getPCSlavePlayStatusInfo();
        }
    }
}
