package com.comp3050.hearthealthmonitor.activity;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.comp3050.hearthealthmonitor.R;

/** About Activity
 *  Display more information of this project
 **/

public class AboutActivity extends AppCompatActivity {

    private static final String URL_PROJECT = "https://5656hcx.github.io/HDMProject/index.html";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        webView = findViewById(R.id.webView);
        webView.loadUrl(URL_PROJECT);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.open_in:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri_1 = Uri.parse(webView.getUrl());
                Uri uri_2 = Uri.parse(URL_PROJECT);
                if (uri_1.getHost() != null && uri_1.getHost().equals(uri_2.getHost()))
                    intent.setData(uri_1);
                else
                    intent.setData(uri_2);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
