package com.jieli.btsmart.ui.settings.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jieli.btsmart.R;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.ToastUtil;

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
public class FeedBackFragment extends Jl_BaseFragment {
    private EditText etFeedBackContent;
    private EditText etFeedBackPhone;
    private TextView tvFeedBackLen;

    public static FeedBackFragment newInstance() {
        return new FeedBackFragment();
    }

    public FeedBackFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);
        etFeedBackContent = view.findViewById(R.id.et_feedback);
        tvFeedBackLen = view.findViewById(R.id.tv_feedback_text_len);
        etFeedBackContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s == null) return;
                tvFeedBackLen.setText(AppUtil.formatString("%d/200", s.length()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        etFeedBackPhone = view.findViewById(R.id.et_feedback_phone);
        Button btFeedBack = view.findViewById(R.id.bt_feedback_commit);
        btFeedBack.setOnClickListener(v -> {
            String feedBackContentText = String.valueOf(etFeedBackContent.getText());
            String feedBackPhoneText = String.valueOf(etFeedBackPhone.getText());
            if (TextUtils.isEmpty(feedBackContentText)) {
                ToastUtil.showToastShort(R.string.tips_input_feedback);
                return;
            }
            if (TextUtils.isEmpty(feedBackPhoneText)) {
                ToastUtil.showToastShort(R.string.tips_input_phone);
                return;
            }
            if (feedBackPhoneText.contains(" ") || feedBackPhoneText.length() != 11) {
                ToastUtil.showToastShort(R.string.tips_input_right_phone);
                return;
            }
            postAsynsRequest(feedBackContentText, feedBackPhoneText);
        });
        return view;
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
                .url("http://www.zh-jieli.com/")
                .post(formBody.build())
                .build();
        Call call2 = okhttpClient.newCall(request);
        call2.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                ToastUtil.showToastShort("发送数据失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                new Thread() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        new Handler().post(() -> {
                            ToastUtil.showToastShort("感谢你的反馈");
                            getActivity().onBackPressed();
                        });
                        Looper.loop();
                    }
                }.start();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CommonActivity activity = (CommonActivity) getActivity();
        if (activity != null) {
            activity.updateTopBar(getString(R.string.feedback), R.drawable.ic_back_black, v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }, 0, null);
        }
    }
}
