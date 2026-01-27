package com.jieli.btsmart.ui.settings.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.jieli.bluetooth.utils.JL_Log;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.tool.network.NetworkDetectionHelper;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseFragment;

/**
 * Des:网页浏览器
 * Author: Bob
 * Date:20-5-18
 * UpdateRemark:
 */
public final class WebBrowserFragment extends BaseFragment {
    private WebView mWebView = null;

    public static WebBrowserFragment newInstance() {
        return new WebBrowserFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mWebView = new WebView(requireContext());
        RelativeLayout relativeLayout = new RelativeLayout(requireContext());
        relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        relativeLayout.addView(mWebView);
        NetworkDetectionHelper.getInstance().addOnNetworkDetectionListener(mOnNetworkDetectionListener);
        return relativeLayout;
    }

    @Override
    public void onDestroyView() {
        NetworkDetectionHelper.getInstance().removeOnNetworkDetectionListener(mOnNetworkDetectionListener);
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @NonNull Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        int flag = 0;
        if (bundle != null) {
            flag = bundle.getInt(SConstant.KEY_WEB_FLAG, 0);
        }
        final String url = flag == 0 ? getString(R.string.user_agreement_url) : getString(R.string.app_privacy_policy);
        String title = flag == 0 ? getString(R.string.user_agreement) : getString(R.string.privacy_policy);
        mWebView.loadUrl(url);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (url.equals(request.getUrl().toString())) {
                    mWebView.clearHistory();
                    ViewGroup parent = (ViewGroup) getView();
                    if (parent == null) return;
                    parent.removeAllViews();
                    LayoutInflater.from(getContext()).inflate(R.layout.view_no_network, parent, true);
                }
            }

        });
        CommonActivity activity = (CommonActivity) getActivity();
        if (activity != null) {
            activity.updateTopBar(title, R.drawable.ic_back_black, v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }, 0, null);
        }
    }

    @Override
    public void onDestroy() {
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null, "", "text/html",
                    "utf-8", null);
            mWebView.setTag(null);
            mWebView.clearHistory();

            if (mWebView.getParent() != null) {
                ((ViewGroup) mWebView.getParent()).removeView(mWebView);
            }
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    private final NetworkDetectionHelper.OnNetworkDetectionListener mOnNetworkDetectionListener = (type, available) -> {
        JL_Log.w(TAG, "onNetworkStateChange --->" + available + "\tattach=" + mWebView.isAttachedToWindow());
        if (available && isAdded()) {
            ViewGroup parent = (ViewGroup) getView();
            if (parent == null || mWebView.isAttachedToWindow()) return;
            parent.removeAllViews();
            parent.addView(mWebView);
            mWebView.reload();
        }
    };
}
