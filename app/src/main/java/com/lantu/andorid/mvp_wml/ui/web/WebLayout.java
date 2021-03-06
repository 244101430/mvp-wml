package com.lantu.andorid.mvp_wml.ui.web;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.just.agentwebX5.IWebLayout;
import com.lantu.andorid.mvp_wml.R;
import com.tencent.smtt.sdk.WebView;

/**
 * Created by wml on 2017/10/19.
 */

public class WebLayout implements IWebLayout {

    private Activity mActivity;
    private WebView mWebView = null;

    public WebLayout(Activity activity) {
        this.mActivity = activity;

        mWebView = (WebView) LayoutInflater.from(activity).inflate(R.layout.fragment_twk_web, null);
    }

    @NonNull
    @Override
    public ViewGroup getLayout() {
        return mWebView;
    }

    @Nullable
    @Override
    public WebView getWeb() {
        return mWebView;
    }



}
