package com.pyzit.app;

import android.graphics.Bitmap;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private WebView mywebView;
    private ProgressBar progressBar;
    private View loadingLayout;
    private LogoLoadingView logoLoadingView;
    private TextView loadingText, progressText;
    private View dot1, dot2, dot3;
    private ValueCallback<Uri[]> uploadMessage;
    private Uri cameraImageUri = null;
    private boolean isOffline = false;
    private Handler retryHandler = new Handler();
    private Runnable retryRunnable;
    private Handler dotAnimationHandler = new Handler();

    // Activity result launcher for file chooser
    private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (uploadMessage == null) return;

                Uri[] results = null;
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        String dataString = result.getData().getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                    // If camera image was captured, use it
                    if (cameraImageUri != null) {
                        results = new Uri[]{cameraImageUri};
                    }
                }
                uploadMessage.onReceiveValue(results);
                uploadMessage = null;
                cameraImageUri = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mywebView = (WebView) findViewById(R.id.webview);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        loadingLayout = findViewById(R.id.loadingLayout);
        logoLoadingView = (LogoLoadingView) findViewById(R.id.logoLoadingView);
        loadingText = (TextView) findViewById(R.id.loadingText);
        progressText = (TextView) findViewById(R.id.progressText);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);

        setupWebView();
        startDotAnimation();
        checkInternetAndLoad();
    }

    private void setupWebView() {
        mywebView.setWebViewClient(new MyWebViewClient());
        mywebView.setWebChromeClient(new MyWebChromeClient());

        WebSettings webSettings = mywebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        // Enable file access and upload
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // Enable caching (Modern approach)
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Enable DOM storage and database for better caching
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Set a custom user agent
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 PyzitApp/1.0");
    }

    private void startDotAnimation() {
        final Runnable dotRunnable = new Runnable() {
            @Override
            public void run() {
                // Rotate alpha values for dots
                float alpha1 = dot1.getAlpha();
                float alpha2 = dot2.getAlpha();
                float alpha3 = dot3.getAlpha();

                dot1.setAlpha(alpha3);
                dot2.setAlpha(alpha1);
                dot3.setAlpha(alpha2);

                dotAnimationHandler.postDelayed(this, 300);
            }
        };
        dotAnimationHandler.postDelayed(dotRunnable, 300);
    }

    private void checkInternetAndLoad() {
        if (isNetworkAvailable()) {
            loadWebsite();
        } else {
            showOfflinePage();
        }
    }

    private void loadWebsite() {
        isOffline = false;
        mywebView.setVisibility(View.VISIBLE);
        showLoadingScreen();
        mywebView.loadUrl("https://qwerty.pyzit.com");
    }

    private void showLoadingScreen() {
        loadingLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        loadingText.setText("Loading...");
        progressText.setText("0%");
    }

    private void hideLoadingScreen() {
        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500);
        fadeOut.setFillAfter(true);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                loadingLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        loadingLayout.startAnimation(fadeOut);
        progressBar.setVisibility(View.GONE);
    }

    private void showOfflinePage() {
        isOffline = true;
        mywebView.setVisibility(View.VISIBLE);

        String offlineHtml = createOfflinePage();
        mywebView.loadDataWithBaseURL(
                "file:///android_asset/",
                offlineHtml,
                "text/html",
                "UTF-8",
                null
        );
        hideLoadingScreen();
    }

    private String createOfflinePage() {
        // Your existing offline page HTML
        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>Pyzit - Offline</title>" +
                "    <style>" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }" +
                "        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 20px; }" +
                "        .offline-container { background: rgba(255, 255, 255, 0.95); border-radius: 20px; padding: 40px 30px; text-align: center; box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1); max-width: 400px; width: 100%; }" +
                "        .icon { font-size: 80px; margin-bottom: 20px; color: #667eea; }" +
                "        h1 { color: #333; margin-bottom: 15px; font-size: 24px; font-weight: 600; }" +
                "        p { color: #666; margin-bottom: 30px; line-height: 1.6; font-size: 16px; }" +
                "        .retry-btn { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none; padding: 15px 30px; border-radius: 50px; font-size: 16px; font-weight: 600; cursor: pointer; transition: all 0.3s ease; box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3); width: 100%; }" +
                "        .loading { display: none; margin-top: 20px; color: #666; }" +
                "        .tips { margin-top: 25px; padding: 15px; background: #f8f9fa; border-radius: 10px; text-align: left; }" +
                "        .tips h3 { color: #333; margin-bottom: 10px; font-size: 14px; }" +
                "        .tips ul { color: #666; font-size: 12px; padding-left: 20px; }" +
                "        .tips li { margin-bottom: 5px; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='offline-container'>" +
                "        <div class='icon'>📶</div>" +
                "        <h1>No Internet Connection</h1>" +
                "        <p>It seems you're offline. Please check your connection and try again.</p>" +
                "        <button class='retry-btn' onclick='retryConnection()'>Try Again</button>" +
                "        <div class='loading' id='loading'>Checking connection...</div>" +
                "        <div class='tips'>" +
                "            <h3>Quick Tips:</h3>" +
                "            <ul>" +
                "                <li>Check your Wi-Fi or mobile data</li>" +
                "                <li>Turn on Airplane mode and turn it off</li>" +
                "                <li>Restart your router</li>" +
                "                <li>Move to a better signal area</li>" +
                "            </ul>" +
                "        </div>" +
                "    </div>" +
                "    <script>function retryConnection() { var btn = document.querySelector('.retry-btn'); var loading = document.getElementById('loading'); btn.style.display = 'none'; loading.style.display = 'block'; Android.retryConnection(); }</script>" +
                "</body>" +
                "</html>";
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            isOffline = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            hideLoadingScreen();

            // Inject JavaScript interface for retry functionality
            view.addJavascriptInterface(new WebAppInterface(), "Android");
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (!isNetworkAvailable()) {
                showOfflinePage();
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (!isNetworkAvailable()) {
                showOfflinePage();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("pyzit.com")) {
                return false;
            }
            if (url.contains("accounts.google.com") || url.contains("github.com") || url.contains("oauth") || url.contains("auth")) {
                return false;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    }

    // JavaScript interface for communication
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void retryConnection() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    checkInternetAndLoad();
                }
            });
        }
    }

    // WebChromeClient to handle file uploads and progress
    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
            }
            uploadMessage = filePathCallback;

            Intent intent = fileChooserParams.createIntent();
            try {
                fileChooserLauncher.launch(intent);
            } catch (Exception e) {
                uploadMessage = null;
                Toast.makeText(MainActivity.this, "Cannot open file chooser", Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            // Update progress bar and text
            progressBar.setProgress(newProgress);
            progressText.setText(newProgress + "%");

            // Update loading text based on progress
            if (newProgress < 30) {
                loadingText.setText("Connecting...");
            } else if (newProgress < 70) {
                loadingText.setText("Loading content...");
            } else {
                loadingText.setText("Almost ready...");
            }

            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            } else if (progressBar.getVisibility() == View.GONE) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOffline) {
            checkInternetAndLoad();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (retryHandler != null && retryRunnable != null) {
            retryHandler.removeCallbacks(retryRunnable);
        }
        if (dotAnimationHandler != null) {
            dotAnimationHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onBackPressed() {
        if (mywebView.canGoBack() && !isOffline) {
            mywebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}