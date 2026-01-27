package com.jieli.btsmart.ui.widget;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.base.BaseDialogFragment;
import com.jieli.component.utils.SystemUtil;

/**
 * 用户服务协议弹窗
 *
 * @author zqjasonZhong
 * @since 2020/5/20
 */
public class UserServiceDialog extends BaseDialogFragment {

    private TextView tvTitle;
    private TextView tvContent;
    private TextView tvAgree;
    private TextView tvExit;

    private OnUserServiceListener mOnUserServiceListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //设置dialog的基本样式参数
        requireDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = requireDialog().getWindow();
        if (window != null) {
            //去掉dialog默认的padding
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            //设置dialog的动画
            lp.windowAnimations = R.style.BottomToTopAnim;
            window.setAttributes(lp);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        View view = inflater.inflate(R.layout.dialog_user_service, container, false);
        tvTitle = view.findViewById(R.id.tv_user_service_title);
        tvContent = view.findViewById(R.id.tv_user_service_content);
        tvAgree = view.findViewById(R.id.tv_user_service_agree);
        tvExit = view.findViewById(R.id.tv_user_service_exit);
        tvAgree.setOnClickListener(mOnClickListener);
        tvExit.setOnClickListener(mOnClickListener);
        initView();
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setCancelable(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == tvAgree) {
                onAgree();
            } else if (v == tvExit) {
                onExit();
            }
        }
    };

    public void setOnUserServiceListener(OnUserServiceListener onUserServiceListener) {
        mOnUserServiceListener = onUserServiceListener;
    }

    private void initView() {
        if (getContext() == null) return;
        String content = getString(R.string.user_service_tips, SystemUtil.getAppName(getContext()));
        String content1 = getString(R.string.user_service_tips2, SystemUtil.getAppName(getContext()));
        String userService = getString(R.string.user_service_name);
        String privacyPolicy = getString(R.string.privacy_policy_name);
        int startPos = content1.indexOf(userService);
        int endPos = startPos + userService.length();
        JL_Log.i(TAG, "initView : startPos : " + startPos + ", endPos : " + endPos);
        SpannableString span = new SpannableString(content1);
        span.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                onUserService();
            }
        }, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue_448eff)), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        startPos = content1.indexOf(privacyPolicy);
        endPos = startPos + privacyPolicy.length();
        JL_Log.i(TAG, "initView 2222 : startPos : " + startPos + ", endPos : " + endPos);
        span.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                onPrivacyPolicy();
            }
        }, startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue_448eff)), startPos, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvContent.setText(content);
        tvContent.append(span);
        tvContent.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void onUserService() {
        if (mOnUserServiceListener != null) {
            mOnUserServiceListener.onUserService();
        }
    }

    private void onPrivacyPolicy() {
        if (mOnUserServiceListener != null) {
            mOnUserServiceListener.onPrivacyPolicy();
        }
    }

    private void onExit() {
        if (mOnUserServiceListener != null) {
            mOnUserServiceListener.onExit(this);
        }
    }

    private void onAgree() {
        if (mOnUserServiceListener != null) {
            mOnUserServiceListener.onAgree(this);
        }
    }

    public interface OnUserServiceListener {
        void onUserService();

        void onPrivacyPolicy();

        void onExit(DialogFragment dialogFragment);

        void onAgree(DialogFragment dialogFragment);
    }

}
