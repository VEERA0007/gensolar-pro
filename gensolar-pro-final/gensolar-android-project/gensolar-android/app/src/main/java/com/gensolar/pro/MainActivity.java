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
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;
import android.os.Build;
import android.widget.Toast;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private WebView webView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private String pendingPhone;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(18, 63, 48));
        window.setNavigationBarColor(Color.rgb(18, 63, 48));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(params);
        }
        try {
            FirebaseApp.initializeApp(this);
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) { Log.e("GenSolar", "Firebase initialization failed", e); }
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new PrintBridge(), "GenSolarNative");
        webView.addJavascriptInterface(new AuthBridge(), "GenSolarAuth");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void js(String script) {
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(script, null); });
    }
    private void jsError(String message) {
        String safe = message == null ? "Authentication failed. Please try again." : message;
        safe = safe.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ");
        js("window.onGenSolarAuthError && window.onGenSolarAuthError('" + safe + "')");
    }

    private class AuthBridge {
        @JavascriptInterface public void sendOtp(String tenDigitPhone) {
            runOnUiThread(() -> {
                if (auth == null) { jsError("Firebase is not initialized. Check google-services.json and rebuild."); return; }
                String digits = tenDigitPhone == null ? "" : tenDigitPhone.replaceAll("\\D", "");
                if (!digits.matches("[6-9][0-9]{9}")) { jsError("Enter a valid 10-digit Indian mobile number."); return; }
                pendingPhone = digits;
                PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber("+91" + digits)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(MainActivity.this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override public void onVerificationCompleted(PhoneAuthCredential credential) { signIn(credential); }
                        @Override public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                            Log.e("GenSolar", "Phone verification failed", e);
                            jsError(e.getMessage() == null ? "Could not send OTP. Check Firebase phone auth settings." : e.getMessage());
                        }
                        @Override public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                            verificationId = id; resendToken = token;
                            js("window.onGenSolarOtpSent && window.onGenSolarOtpSent()");
                        }
                    }).build();
                PhoneAuthProvider.verifyPhoneNumber(options);
            });
        }
        @JavascriptInterface public void verifyOtp(String code) {
            runOnUiThread(() -> {
                if (auth == null || verificationId == null) { jsError("Request an OTP first."); return; }
                String otp = code == null ? "" : code.trim();
                if (!otp.matches("[0-9]{6}")) { jsError("Enter the 6-digit OTP."); return; }
                signIn(PhoneAuthProvider.getCredential(verificationId, otp));
            });
        }
        @JavascriptInterface public void signOut() { if (auth != null) auth.signOut(); }
    }

    private void signIn(PhoneAuthCredential credential) {
        if (auth == null) { jsError("Firebase authentication is unavailable."); return; }
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().getUser() == null) {
                jsError(task.getException() == null ? "OTP verification failed." : task.getException().getMessage()); return;
            }
            FirebaseUser user = task.getResult().getUser();
            ensureCustomerProfile(user);
        });
    }

    private void ensureCustomerProfile(FirebaseUser user) {
        if (db == null) { jsError("Firestore is not initialized."); return; }
        DocumentReference ref = db.collection("users").document(user.getUid());
        ref.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String role = doc.getString("role");
                if (role == null || role.isEmpty()) role = "customer";
                String phone = user.getPhoneNumber() == null ? "" : user.getPhoneNumber();
                js("window.onGenSolarAuthSuccess && window.onGenSolarAuthSuccess('" + user.getUid() + "','" + role + "','" + phone + "')");
            } else {
                Map<String,Object> profile = new HashMap<>();
                profile.put("uid", user.getUid());
                profile.put("phone", user.getPhoneNumber() == null ? "" : user.getPhoneNumber());
                profile.put("role", "customer");
                profile.put("createdAt", System.currentTimeMillis());
                ref.set(profile, SetOptions.merge()).addOnSuccessListener(v ->
                    js("window.onGenSolarAuthSuccess && window.onGenSolarAuthSuccess('" + user.getUid() + "','customer','" + (user.getPhoneNumber() == null ? "" : user.getPhoneNumber()) + "')")
                ).addOnFailureListener(e -> jsError("Signed in, but profile setup failed: " + e.getMessage()));
            }
        }).addOnFailureListener(e -> jsError("Could not load account role. Check Firestore rules: " + e.getMessage()));
    }

    private class PrintBridge {
        @JavascriptInterface public void printPdf() {
            runOnUiThread(() -> {
                try {
                    PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    if (printManager == null) { Toast.makeText(MainActivity.this, "Print service unavailable on this device", Toast.LENGTH_LONG).show(); return; }
                    String jobName = "GenSolar Project Estimate";
                    printManager.print(jobName, webView.createPrintDocumentAdapter(jobName), new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build());
                } catch (Exception e) { Toast.makeText(MainActivity.this, "Could not open PDF print service", Toast.LENGTH_LONG).show(); }
            });
        }
    }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
