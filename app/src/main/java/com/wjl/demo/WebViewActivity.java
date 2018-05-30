package com.wjl.demo;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * author: WuJinLi
 * time  : 2018/5/8
 * desc  :
 */

public class WebViewActivity extends AppCompatActivity {
    public WebView wv_webView;
    public static final String WEB_URL = "http://192.168.1.103:8080/login";
//    public static final String WEB_URL = "https://www.baidu.com/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_webview);
        initView();
        wv_webView.loadUrl(WEB_URL);
    }

    private void initView() {
        wv_webView = findViewById(R.id.wv_webView);


        if (Build.VERSION.SDK_INT >= 19) {
            wv_webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//解决图片不显示的问题
            wv_webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        wv_webView.getSettings().setJavaScriptEnabled(true);
        wv_webView.getSettings().setSupportZoom(false);// 设置可以支持缩放
        wv_webView.getSettings().setBuiltInZoomControls(false);// 设置出现缩放工具
        wv_webView.getSettings().setUseWideViewPort(true);//扩大比例的缩放
        wv_webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);//自适应屏幕
        wv_webView.getSettings().setLoadWithOverviewMode(true);
        wv_webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);//允许js弹出窗口
        wv_webView.getSettings().setUserAgentString(wv_webView.getSettings().getUserAgentString() + " " +
                "DLB/DLB");
        wv_webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
        wv_webView.getSettings().setDomStorageEnabled(true);
        wv_webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        wv_webView.getSettings().setAppCachePath(appCachePath);
        wv_webView.getSettings().setAllowFileAccess(true);
        wv_webView.getSettings().setAppCacheEnabled(true);

        wv_webView.setWebViewClient(new WebViewClient());
        wv_webView.setWebChromeClient(new WebChromeClient());
    }
}
