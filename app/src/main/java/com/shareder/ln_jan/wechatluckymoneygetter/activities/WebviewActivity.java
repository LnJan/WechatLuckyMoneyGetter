package com.shareder.ln_jan.wechatluckymoneygetter.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.shareder.ln_jan.wechatluckymoneygetter.R;

public class WebviewActivity extends AppCompatActivity implements View.OnClickListener {
    private WebView webView;
    private String webViewUrl, webViewTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadUI();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && !bundle.isEmpty()) {
            webViewTitle = bundle.getString("title");
            webViewUrl = bundle.getString("url");

            final TextView webViewBar = findViewById(R.id.webview_bar);
            webViewBar.setText(webViewTitle);

            webView = findViewById(R.id.webView);
            webView.getSettings().setBuiltInZoomControls(false);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.loadUrl(webViewUrl);
        }

        ImageView ivBack=findViewById(R.id.webview_back);
        ivBack.setOnClickListener(this);
        ImageView ivOpenLink=findViewById(R.id.webview_outlink);
        ivOpenLink.setOnClickListener(this);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void loadUI() {
        setContentView(R.layout.activity_webview);

        Window window = this.getWindow();

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        window.setStatusBarColor(0xffE46C62);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.webview_back:
                super.onBackPressed();
                break;
            case R.id.webview_outlink:
                openLink();
                break;
            default:
                break;
        }
    }

    private void openLink() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(this.webViewUrl));
        startActivity(intent);
    }

}
