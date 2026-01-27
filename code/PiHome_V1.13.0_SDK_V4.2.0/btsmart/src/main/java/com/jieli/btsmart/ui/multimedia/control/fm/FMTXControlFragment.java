package com.jieli.btsmart.ui.multimedia.control.fm;

import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentFmtxControlBinding;
import com.jieli.btsmart.ui.base.BaseViewModelFragment;
import com.jieli.btsmart.ui.widget.InputTextDialog;
import com.jieli.btsmart.ui.widget.rulerview.RulerView;
import com.jieli.btsmart.viewmodel.FMTXControlViewModel;
import com.jieli.component.utils.ToastUtil;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/7/22 9:39
 * @desc :
 */
public class FMTXControlFragment extends BaseViewModelFragment<FragmentFmtxControlBinding> {
    private InputTextDialog mInputTextDialog;
    private FMTXControlViewModel mFMtxControlViewModel;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_fmtx_control;
    }

    public static FMTXControlFragment newInstance() {
        return new FMTXControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(FMTXControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(FMTXControlFragment.class.getSimpleName());
        }
        return new FMTXControlFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void actionsOnViewInflate() {
        super.actionsOnViewInflate();
        ViewModelProvider provider = new ViewModelProvider(this);
        mFMtxControlViewModel = provider.get(FMTXControlViewModel.class);
        mBinding.setFmTXControlViewModel(mFMtxControlViewModel);
        mBinding.setLifecycleOwner(this);
        mFMtxControlViewModel.requestFMTXFreq();
        mBinding.rvFm.setMinValueAndMaxValue(875, -1);
        mBinding.rvFm.setActivity(getActivity());
        mBinding.rvFm.setOnValueChangeListener(new RulerView.OnValueChangeListener() {
            @Override
            public void onChange(int value) {
                mFMtxControlViewModel.realTimeFMTxFreqLiveData.setValue(value);
            }

            @Override
            public void onActionUp(int value) {
                mFMtxControlViewModel.realTimeFMTxFreqLiveData.setValue(value);
                float channel = ((float) value) / ((float) 10);
                mFMtxControlViewModel.setFMTXFreq(channel);
            }

            @Override
            public void onFling(int value, boolean isFlingEnd) {
                mFMtxControlViewModel.realTimeFMTxFreqLiveData.setValue(value);
                if (isFlingEnd) {
                    float channel = ((float) value) / ((float) 10);
                    mFMtxControlViewModel.setFMTXFreq(channel);
                }
            }

            @Override
            public void onCanNotSlide() {

            }
        });
        mBinding.tvFmCurrentChannel.setOnClickListener(v -> {
            showInputTextDialog((String) ((TextView) v).getText());
        });
        observerFMControlViewModel();
    }

    private void observerFMControlViewModel() {
        mFMtxControlViewModel.deviceFMTxFreqLiveData.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                mBinding.rvFm.setCurrentPosition(integer);
            }
        });
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
                                ToastUtil.showToastShort(R.string.fm_value_no_change);
                                return;
                            }
                            if (Float.parseFloat(value) < 87.5 || Float.parseFloat(value) > 108) {
                                ToastUtil.showToastShort(R.string.fm_range);
                                return;
                            }
                            if (value.contains(".")) {
                                String[] list = value.split("\\.");
                                if (list.length == 2) {//带小数点
                                    if (list[1].length() > 1) {
                                        ToastUtil.showToastShort(R.string.fm_only_one_decimal);
                                        return;
                                    }
                                }
                                if (list.length == 1) {
                                    ToastUtil.showToastShort(R.string.fm_no_decimal);
                                    return;
                                }
                            }
                            mFMtxControlViewModel.setFMTXFreq(Float.parseFloat(value));
                            dialog.dismiss();
                            mInputTextDialog = null;
                        }
                    })
                    .create();
        }
        mInputTextDialog.updateEditText();
        mInputTextDialog.updateDialog();
        if (!mInputTextDialog.isShow() && !isDetached() && getActivity() != null) {
            mInputTextDialog.show(getActivity().getSupportFragmentManager(), "input_text_dialog");
        }
    }
}
