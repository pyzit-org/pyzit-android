package com.pyzit.android;

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

        // Initialize views safely
        initializeViews();

        setupWebView();
        startDotAnimation();

        // Handle deep links
        handleDeepLink();
    }

    private void initializeViews() {
        mywebView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        loadingLayout = findViewById(R.id.loadingLayout);
        logoLoadingView = findViewById(R.id.logoLoadingView);
        loadingText = findViewById(R.id.loadingText);
        progressText = findViewById(R.id.progressText);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
    }

    private void handleDeepLink() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String deepLinkUrl = data.toString();
            loadDeepLinkUrl(deepLinkUrl);
        } else {
            checkInternetAndLoad();
        }
    }

    private void loadDeepLinkUrl(String url) {
        if (isNetworkAvailable()) {
            isOffline = false;
            mywebView.setVisibility(View.VISIBLE);
            showLoadingScreen();
            mywebView.loadUrl(url);
        } else {
            showOfflinePage();
        }
    }

    private void setupWebView() {
        // Set up WebViewClient first
        mywebView.setWebViewClient(new MyWebViewClient());
        mywebView.setWebChromeClient(new MyWebChromeClient());

        WebSettings webSettings = mywebView.getSettings();

        // Basic settings
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        // File access
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // Performance optimizations
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        // Remove problematic settings that might cause crashes
        // webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH); // Can cause issues

        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // User agent
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 PyzitApp/1.0");

        // Safe WebView initialization for newer Android versions
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                WebView.setDataDirectorySuffix("pyzit");
            }
        } catch (Exception e) {
            // Ignore errors in WebView initialization
        }
    }

    private void startDotAnimation() {
        final Runnable dotRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    float alpha1 = dot1.getAlpha();
                    float alpha2 = dot2.getAlpha();
                    float alpha3 = dot3.getAlpha();

                    dot1.setAlpha(alpha3);
                    dot2.setAlpha(alpha1);
                    dot3.setAlpha(alpha2);

                    dotAnimationHandler.postDelayed(this, 300);
                } catch (Exception e) {
                    // Ignore animation errors
                }
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
        mywebView.loadUrl("https://pyzit.com");
    }

    private void showLoadingScreen() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (loadingText != null) {
            loadingText.setText("Loading...");
        }
        if (progressText != null) {
            progressText.setText("0%");
        }
    }

    private void hideLoadingScreen() {
        if (loadingLayout == null) return;

        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500);
        fadeOut.setFillAfter(true);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (loadingLayout != null) {
                    loadingLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        loadingLayout.startAnimation(fadeOut);

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showOfflinePage() {
        isOffline = true;
        if (mywebView != null) {
            mywebView.setVisibility(View.VISIBLE);
            String offlineHtml = createOfflinePage();
            mywebView.loadDataWithBaseURL(
                    "file:///android_asset/",
                    offlineHtml,
                    "text/html",
                    "UTF-8",
                    null
            );
        }
        hideLoadingScreen();
    }

    private String createOfflinePage() {
        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "  <meta charset='UTF-8'>" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "  <title>Pyzit • Offline</title>" +
                "  <style>" +
                "    *{margin:0;padding:0;box-sizing:border-box}" +
                "    body{font-family:'Segoe UI',Roboto,sans-serif;background:#F4F6FA;height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;color:#2E3A59}" +
                "    .card{background:#fff;border-radius:16px;box-shadow:0 10px 30px rgba(0,0,0,0.08);padding:36px 32px;text-align:center;max-width:400px;width:100%;animation:fadeIn .6s ease}" +
                "    img.logo{width:120px;height:auto;margin-bottom:20px;}" +
                "    h1{font-size:22px;font-weight:600;margin-bottom:12px;color:#2E3A59}" +
                "    p{font-size:15px;color:#4D5C7D;margin-bottom:25px;line-height:1.6}" +
                "    button{background:#5A67D8;border:none;color:#fff;padding:14px 28px;border-radius:30px;font-size:15px;font-weight:600;cursor:pointer;transition:all .25s ease;width:100%;box-shadow:0 6px 18px rgba(90,103,216,0.3)}" +
                "    button:hover{background:#4F57C4;transform:translateY(-2px);box-shadow:0 8px 22px rgba(90,103,216,0.35)}" +
                "    .loading{display:none;margin-top:16px;color:#6D748C;font-size:13px}" +
                "    @keyframes fadeIn{from{opacity:0;transform:translateY(15px)}to{opacity:1;transform:translateY(0)}}" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class='card'>" +
                "    <img src='file:///android_asset/pyzit_logo.png' alt='Pyzit Logo' class='logo'>" +
                "    <h1>You're Offline</h1>" +
                "    <p>We couldn't reach Pyzit. Please check your network and try again.</p>" +
                "    <button onclick='retryConnection()'>Reconnect</button>" +
                "    <div class='loading' id='loading'>🔄 Checking connection...</div>" +
                "  </div>" +
                "  <script>" +
                "    function retryConnection(){var btn=document.querySelector('button');var load=document.getElementById('loading');btn.style.display='none';load.style.display='block';if(window.Android){Android.retryConnection();}}" +
                "  </script>" +
                "</body>" +
                "</html>";
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
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
            return handleUrlLoading(url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                return handleUrlLoading(request.getUrl().toString());
            }
            return false;
        }

        private boolean handleUrlLoading(String url) {
            try {
                Uri uri = Uri.parse(url);
                String host = uri.getHost();

                // Handle pyzit.com and ALL subdomains internally
                if (host != null && (host.equals("pyzit.com") || host.endsWith(".pyzit.com"))) {
                    return false; // Let WebView handle it internally
                }

                // Handle OAuth and authentication URLs internally
                if (url.contains("accounts.google.com") ||
                        url.contains("github.com") ||
                        url.contains("oauth") ||
                        url.contains("auth")) {
                    return false;
                }

                // External URLs - open in browser
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;

            } catch (Exception e) {
                return false;
            }
        }
    }

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

    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            // For target="_blank" links - load in main WebView
            WebView.HitTestResult result = view.getHitTestResult();
            if (result != null) {
                String url = result.getExtra();
                if (url != null) {
                    view.loadUrl(url);
                }
            }
            return false; // Don't create new window
        }

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

            if (progressBar != null) {
                progressBar.setProgress(newProgress);
            }
            if (progressText != null) {
                progressText.setText(newProgress + "%");
            }
            if (loadingText != null) {
                if (newProgress < 30) {
                    loadingText.setText("Connecting...");
                } else if (newProgress < 70) {
                    loadingText.setText("Loading content...");
                } else {
                    loadingText.setText("Almost ready...");
                }
            }

            if (newProgress == 100 && progressBar != null) {
                progressBar.setVisibility(View.GONE);
            } else if (progressBar != null && progressBar.getVisibility() == View.GONE) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isOffline) {
            checkInternetAndLoad();
        }
        if (mywebView != null) {
            mywebView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mywebView != null) {
            mywebView.onPause();
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
        if (mywebView != null) {
            mywebView.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (mywebView != null && mywebView.canGoBack() && !isOffline) {
            mywebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}