package com.jieli.btsmart.ui.settings.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jieli.btsmart.BuildConfig;
import com.jieli.btsmart.MainApplication;
import com.jieli.btsmart.R;
import com.jieli.btsmart.constant.SConstant;
import com.jieli.btsmart.data.adapter.CommonListAdapter;
import com.jieli.btsmart.data.model.settings.SettingsItem;
import com.jieli.btsmart.ui.CommonActivity;
import com.jieli.btsmart.ui.base.BaseFragment;
import com.jieli.btsmart.ui.test.TestConfigurationActivity;
import com.jieli.btsmart.util.AppUtil;
import com.jieli.component.utils.SystemUtil;
import com.jieli.component.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Des:App 关于
 * Author: Bob
 * Date:20-5-16
 * UpdateRemark:
 */
public final class AboutFragment extends BaseFragment {
    private RecyclerView rvAboutSettings;
    private CommonListAdapter adapter;
    private final MainApplication application = MainApplication.getApplication();

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_about, container, false);
        rvAboutSettings = view.findViewById(R.id.about_settings_list);
        rvAboutSettings.setLayoutManager(new LinearLayoutManager(application));
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        CommonActivity activity = (CommonActivity) getActivity();
        if (activity != null) {
            activity.updateTopBar(getString(R.string.about), R.drawable.ic_back_black, v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }, 0, null);
        }
        if (adapter == null) {
            List<SettingsItem> list = new ArrayList<>();
            String[] titles = getResources().getStringArray(R.array.app_about_settings_list);
            for (String s : titles) {
                SettingsItem item = new SettingsItem();
                if (getString(R.string.current_version).equals(s)) {
                    item.setType(CommonListAdapter.ValueType.TEXT);
                    item.setValue(getAppVersion());
                }
                list.add(item.setName(s));
            }

            adapter = new CommonListAdapter(list);
            adapter.setOnItemClickListener((adapter, view, position) -> {
                String name = Objects.requireNonNull(((CommonListAdapter) adapter).getItem(position)).getName();
                if (getString(R.string.privacy_policy).equals(name)) {
                    toWebFragment(1);
                } else if (getString(R.string.user_agreement).equals(name)) {
                    toWebFragment(0);
                } else if (getString(R.string.current_version).equals(name)) {
//                    if (BuildConfig.DEBUG) {
                        if (!AppUtil.isFastDoubleClick()) {
                            ToastUtil.showToastShort(R.string.click_again_to_test_configuration);
                        } else {
                            startActivity(new Intent(getContext(), TestConfigurationActivity.class));
                        }
//                    }
                }
            });
        }
        rvAboutSettings.setAdapter(adapter);
    }

    private void toWebFragment(int flag) {
        if (getActivity() == null || getActivity().isDestroyed()) return;
        Bundle bundle = new Bundle();
        bundle.putInt(SConstant.KEY_WEB_FLAG, flag);
        CommonActivity.startCommonActivity(getActivity(),
                WebBrowserFragment.class.getCanonicalName(), bundle);
    }

    private String getAppVersion() {
        String versionName = SystemUtil.getVersionName(requireContext());
        String text = "v" + versionName;
        if (BuildConfig.DEBUG) {
            int versionCode = SystemUtil.getVersion(requireContext());
            text = AppUtil.formatString("%s(%d)", text, versionCode);
        }
        return text;
    }
}
