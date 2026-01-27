package com.jieli.btsmart.ui.base;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.ContentActivity;
import com.jieli.btsmart.ui.widget.dialog.PermissionTipsDialog;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.btsmart.util.UIHelper;
import com.jieli.component.utils.ToastUtil;


/**
 * Fragment的基类
 *
 * @author zqjasonZhong
 * date : 2017/11/10
 */
public abstract class BaseFragment extends Fragment {

    private static final int OP_FINISH = 0;
    private static final int OP_BACK = 1;

    private static final int MSG_EXIT = 0x1212;

    protected String TAG = getClass().getSimpleName();
    private PermissionTipsDialog mPermissionTipsDialog;
    protected Handler uiHandler = new Handler(Looper.getMainLooper(), msg -> {
        if (msg.what == MSG_EXIT) {
            if (requireActivity().isDestroyed() || requireActivity().isFinishing()) return false;
            int op = msg.arg1;
            if (op == OP_BACK) {
                requireActivity().onBackPressed();
            } else {
                requireActivity().finish();
            }
        }
        return true;
    });

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHandler.removeMessages(MSG_EXIT);
        uiHandler.removeCallbacksAndMessages(null);
    }

    public boolean isInvalid() {
        return isDetached() || !isAdded();
    }

    public void changeFragment(int containerId, Fragment fragment) {
        changeFragment(containerId, fragment, null);
    }

    /**
     * 切换fragment
     *
     * @param containerId 控件id
     * @param fragment    切换fragment
     * @param fragmentTag fragment tag
     */
    public void changeFragment(int containerId, Fragment fragment, String fragmentTag) {
        if (fragment != null && isAdded() && !isDetached()) {
            FragmentManager fragmentManager = getChildFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            if (!TextUtils.isEmpty(fragmentTag)) {
                fragmentTransaction.replace(containerId, fragment, fragmentTag);
            } else {
                fragmentTransaction.replace(containerId, fragment);
            }
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commitAllowingStateLoss();
        }
    }

    public void showPermissionTipsDialog(String tips) {
        if (isInvalid()) return;
        if (null == mPermissionTipsDialog) {
            mPermissionTipsDialog = new PermissionTipsDialog.Builder()
                    .tips(tips)
                    .build();
        }
        if (!mPermissionTipsDialog.isShow()) {
            mPermissionTipsDialog.show(getChildFragmentManager(), PermissionTipsDialog.class.getSimpleName());
        }
    }

    public void disPermissionTipsDialog() {
        if (isInvalid() || mPermissionTipsDialog == null) return;
        if (mPermissionTipsDialog.isShow()) {
            mPermissionTipsDialog.dismiss();
        }
        mPermissionTipsDialog = null;
    }

    protected void hideTopBar() {
        if (requireActivity() instanceof CommonActivity) {
            UIHelper.gone(requireActivity().findViewById(R.id.rl_common_top_bar));
        } else if (requireActivity() instanceof ContentActivity) {
            UIHelper.gone(requireActivity().findViewById(R.id.view_top_bar));
        }
    }

    protected void showTips(int resID) {
        showTips(getString(resID));
    }

    protected void showTips(String tips) {
        ToastUtil.showToastLong(tips);
        JL_Log.d(TAG, tips);
    }

    protected void showTips(String format, Object... args) {
        showTips(AppUtil.formatString(format, args));
    }

    protected void back() {
        back(0L);
    }

    protected void back(long delay) {
        back(delay, null);
    }

    protected void back(long delay, IExitHandler handler) {
        exit(OP_BACK, delay, handler);
    }

    protected void finish() {
        finish(0L);
    }

    protected void finish(long delay) {
        finish(delay, null);
    }

    protected void finish(long delay, IExitHandler handler) {
        exit(OP_FINISH, delay, handler);
    }

    private void exit(int op, long delay, IExitHandler handler) {
        if (isInvalid()) return;
        uiHandler.removeMessages(MSG_EXIT);
        if (null != handler) {
            handler.onRun();
        }
        if (delay <= 0) {
            uiHandler.sendMessage(uiHandler.obtainMessage(MSG_EXIT, op, 0));
            return;
        }
        uiHandler.sendMessageDelayed(uiHandler.obtainMessage(MSG_EXIT, op, 0), delay);
    }

    public interface IExitHandler {

        void onRun();
    }
}
