package com.gensolar.pro;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new PrintBridge(), "GenSolarNative");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    private class PrintBridge {
        @JavascriptInterface
        public void printPdf() {
            runOnUiThread(() -> {
                try {
                    PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    if (printManager == null) {
                        Toast.makeText(MainActivity.this, "Print service unavailable on this device", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String jobName = "GenSolar Project Estimate";
                    printManager.print(jobName, webView.createPrintDocumentAdapter(jobName), new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build());
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not open PDF print service", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
