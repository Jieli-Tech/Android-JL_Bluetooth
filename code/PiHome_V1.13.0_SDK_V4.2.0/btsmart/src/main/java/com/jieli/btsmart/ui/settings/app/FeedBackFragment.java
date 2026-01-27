package com.jieli.btsmart.ui.settings.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.databinding.FragmentFeedbackBinding;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.util.AppUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author : HuanMing
 * @e-mail :
 * @date : 2020/6/17 15:18
 * @desc :用户反馈
 */
public class FeedBackFragment extends BaseFragment {

    private static final int CONTENT_LIMIT = 200;

    private FragmentFeedbackBinding mBinding;

    public static FeedBackFragment newInstance() {
        return new FeedBackFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentFeedbackBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    private void initUI() {
        if (requireActivity() instanceof CommonActivity) {
            CommonActivity activity = (CommonActivity) requireActivity();
            activity.updateTopBar(getString(R.string.feedback), R.drawable.ic_back_black, v -> finish(), 0, null);
        }

        mBinding.etFeedback.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null) return;
                int num = s.length();
                if (num > CONTENT_LIMIT) {
                    mBinding.etFeedback.setText(s.subSequence(0, CONTENT_LIMIT));
                    mBinding.etFeedback.setSelection(CONTENT_LIMIT);
                    num = CONTENT_LIMIT;
                }
                updateContentLen(num);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mBinding.btFeedbackCommit.setOnClickListener(v -> {
            String feedBackContentText = String.valueOf(mBinding.etFeedback.getText());
            String feedBackPhoneText = String.valueOf(mBinding.etFeedbackPhone.getText());
            if (TextUtils.isEmpty(feedBackContentText)) {
                showTips(R.string.tips_input_feedback);
                return;
            }
            if (TextUtils.isEmpty(feedBackPhoneText)) {
                showTips(R.string.tips_input_phone);
                return;
            }
            if (feedBackPhoneText.contains(" ") || feedBackPhoneText.length() != 11) {
                showTips(R.string.tips_input_right_phone);
                return;
            }
            postAsynsRequest(feedBackContentText, feedBackPhoneText);
        });
        updateContentLen(0);
    }

    private void updateContentLen(int len) {
        if (isInvalid()) return;
        mBinding.tvFeedbackTextLen.setText(AppUtil.formatString("%d/%d", len, CONTENT_LIMIT));
    }

    /**
     * 异步发送数据
     */
    private void postAsynsRequest(String feedbackContent, String feedbackPhone) {
        OkHttpClient okhttpClient = new OkHttpClient();
        FormBody.Builder formBody = new FormBody.Builder();//创建表单请求体
        formBody.add("feedbackContent", feedbackContent);
        formBody.add("feedbackPhone", feedbackPhone);
        Request request = new Request.Builder()
                .url("https://www.zh-jieli.com/")
                .post(formBody.build())
                .build();
        Call call2 = okhttpClient.newCall(request);
        call2.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                JL_Log.w(TAG, "postAsynsRequest", "onFailure ---> " + e);
                uiHandler.post(() -> showTips(getString(R.string.tips_commit_fail)));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                JL_Log.d(TAG, "postAsynsRequest", "onResponse ---> success");
                uiHandler.post(() -> {
                    showTips(getString(R.string.tips_commit_success));
                    finish();
                });
            }
        });
    }
}
