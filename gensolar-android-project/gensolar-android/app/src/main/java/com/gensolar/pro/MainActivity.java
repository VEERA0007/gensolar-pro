package com.gensolar.pro;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import org.json.JSONObject;
import org.json.JSONArray;
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
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER = 1001;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installCrashRecorder();
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
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) { filePathCallback.onReceiveValue(null); }
                filePathCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER);
                } catch (Exception e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Could not open the photo picker", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });
        webView.setBackgroundColor(Color.WHITE);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { reportLastCrash(); restoreSession(); }
        });
        webView.addJavascriptInterface(new PrintBridge(), "GenSolarNative");
        webView.addJavascriptInterface(new AuthBridge(), "GenSolarAuth");
        webView.addJavascriptInterface(new LeadBridge(), "GenSolarLeads");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void installCrashRecorder() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                error.printStackTrace(new java.io.PrintWriter(sw));
                String trace = sw.toString();
                if (trace.length() > 1800) trace = trace.substring(0, 1800);
                getSharedPreferences("gensolar", MODE_PRIVATE).edit().putString("last_crash", trace).commit();
            } catch (Throwable ignored) { }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }
    private void restoreSession() {
        try {
            if (auth != null && auth.getCurrentUser() != null) { ensureCustomerProfile(auth.getCurrentUser()); }
        } catch (Throwable ignored) { }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void reportLastCrash() {
        try {
            String crash = getSharedPreferences("gensolar", MODE_PRIVATE).getString("last_crash", null);
            if (crash == null) return;
            getSharedPreferences("gensolar", MODE_PRIVATE).edit().remove("last_crash").apply();
            js("window.onGenSolarLastCrash && window.onGenSolarLastCrash(" + JSONObject.quote(crash) + ")");
        } catch (Throwable ignored) { }
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
            runOnUiThread(() -> { try {
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
            } catch (Throwable t) { Log.e("GenSolar", "sendOtp failed", t); jsError("Could not start OTP: " + t); } });
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


    private class LeadBridge {
        @JavascriptInterface public void loadLeads() {
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            if (user == null || db == null) { leadError("Sign in before loading cloud leads."); return; }
            db.collection("users").document(user.getUid()).get().addOnSuccessListener(profile -> {
                String role = profile.getString("role");
                if (role == null) role = "customer";
                com.google.firebase.firestore.Query query = db.collection("leads");
                if ("manager".equals(role) || "admin".equals(role)) {
                    String org = profile.getString("organizationId");
                    if (org == null || org.isEmpty()) { leadError("Your manager account needs an organization ID."); return; }
                    query = query.whereEqualTo("organizationId", org);
                } else if ("rep".equals(role) || "representative".equals(role)) {
                    query = query.whereEqualTo("assignedRepUid", user.getUid());
                } else {
                    query = query.whereEqualTo("ownerUid", user.getUid());
                }
                query.get().addOnSuccessListener(snapshot -> {
                    JSONArray rows = new JSONArray();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            JSONObject row = new JSONObject();
                            Map<String,Object> values = doc.getData();
                            if (values != null) for (Map.Entry<String,Object> entry : values.entrySet()) row.put(entry.getKey(), jsonValue(entry.getValue()));
                            row.put("id", doc.getId());
                            rows.put(row);
                        } catch (Exception ignored) { }
                    }
                    js("window.onGenSolarCloudLeads && window.onGenSolarCloudLeads(JSON.parse(" + JSONObject.quote(rows.toString()) + "))");
                }).addOnFailureListener(e -> leadError("Cloud leads could not load. Check the Firestore rules and internet connection."));
            }).addOnFailureListener(e -> leadError("Your account profile could not be loaded."));
        }

        @JavascriptInterface public void saveLead(String json) {
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            if (user == null || db == null) { leadError("Sign in before saving cloud leads."); return; }
            final JSONObject source;
            try { source = new JSONObject(json == null ? "{}" : json); }
            catch (Exception e) { leadError("This lead could not be read."); return; }
            db.collection("users").document(user.getUid()).get().addOnSuccessListener(profile -> {
                String role = profile.getString("role");
                if (role == null) role = "customer";
                String org = profile.getString("organizationId");
                if (("manager".equals(role) || "admin".equals(role) || "rep".equals(role) || "representative".equals(role))
                        && (org == null || org.isEmpty())) {
                    leadError("Your company account needs an organization ID before cloud leads can sync."); return;
                }
                String id = source.optString("id", "");
                if (id.isEmpty()) id = "L" + System.currentTimeMillis() + Long.toString((long)(Math.random()*100000), 36);
                Map<String,Object> data = new HashMap<>();
                copyLeadField(source, data, "name"); copyLeadField(source, data, "phone");
                copyLeadField(source, data, "seg"); copyLeadField(source, data, "state");
                copyLeadField(source, data, "city"); copyLeadField(source, data, "kw");
                copyLeadField(source, data, "net"); copyLeadField(source, data, "payback");
                copyLeadField(source, data, "source"); copyLeadField(source, data, "quote");
                copyLeadField(source, data, "status"); copyLeadField(source, data, "ts");
                copyLeadField(source, data, "notes");
                data.put("id", id);
                data.put("updatedAt", System.currentTimeMillis());
                if (!source.has("createdAt")) data.put("createdAt", System.currentTimeMillis());
                if ("manager".equals(role) || "admin".equals(role)) {
                    data.put("organizationId", org);
                    data.put("ownerUid", source.optString("ownerUid", user.getUid()));
                    if (source.has("assignedRepUid")) data.put("assignedRepUid", source.optString("assignedRepUid"));
                } else if ("rep".equals(role) || "representative".equals(role)) {
                    data.put("organizationId", org);
                    data.put("assignedRepUid", user.getUid());
                    data.put("ownerUid", source.optString("ownerUid", user.getUid()));
                } else {
                    data.put("ownerUid", user.getUid());
                    data.put("customerUid", user.getUid());
                    data.put("organizationId", "");
                    data.put("assignedRepUid", "");
                }
                final String finalId = id;
                db.collection("leads").document(id).set(data, SetOptions.merge())
                    .addOnSuccessListener(v -> js("window.onGenSolarCloudLeadSaved && window.onGenSolarCloudLeadSaved(" + JSONObject.quote(finalId) + ")"))
                    .addOnFailureListener(e -> leadError("Lead is saved on this phone, but cloud sync failed. Check Firestore rules."));
            }).addOnFailureListener(e -> leadError("Your account profile could not be loaded."));
        }

        @JavascriptInterface public void deleteLead(String id) {
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            if (user == null || db == null || id == null || id.isEmpty()) { leadError("Sign in before deleting a cloud lead."); return; }
            db.collection("leads").document(id).delete()
                .addOnSuccessListener(v -> js("window.onGenSolarCloudLeadDeleted && window.onGenSolarCloudLeadDeleted(" + JSONObject.quote(id) + ")"))
                .addOnFailureListener(e -> leadError("Cloud deletion failed. The record may still be stored on this phone."));
        }

        private void copyLeadField(JSONObject source, Map<String,Object> target, String key) {
            if (!source.has(key) || source.isNull(key)) return;
            try {
                Object value = source.get(key);
                if ("notes".equals(key) && value instanceof JSONArray) {
                    JSONArray array = (JSONArray)value;
                    java.util.List<Map<String,Object>> notes = new java.util.ArrayList<>();
                    for (int i=0;i<array.length();i++) {
                        JSONObject note = array.optJSONObject(i);
                        if (note != null) {
                            Map<String,Object> n = new HashMap<>();
                            n.put("ts", note.optLong("ts", System.currentTimeMillis()));
                            n.put("text", note.optString("text", ""));
                            notes.add(n);
                        }
                    }
                    target.put(key, notes);
                } else if (value instanceof String || value instanceof Number || value instanceof Boolean) target.put(key, value);
            } catch (Exception ignored) { }
        }
    }

    private Object jsonValue(Object value) throws Exception {
        if (value instanceof Map) {
            JSONObject obj = new JSONObject();
            for (Map.Entry<?,?> e : ((Map<?,?>)value).entrySet()) obj.put(String.valueOf(e.getKey()), jsonValue(e.getValue()));
            return obj;
        }
        if (value instanceof java.util.List) {
            JSONArray arr = new JSONArray();
            for (Object item : (java.util.List<?>)value) arr.put(jsonValue(item));
            return arr;
        }
        if (value instanceof com.google.firebase.Timestamp) return ((com.google.firebase.Timestamp)value).toDate().getTime();
        return value;
    }

    private void leadError(String message) {
        js("window.onGenSolarCloudError && window.onGenSolarCloudError(" + JSONObject.quote(message) + ")");
    }

    private class PrintBridge {
        @JavascriptInterface public void shareText(String text) {
            runOnUiThread(() -> {
                try {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(send, "Share"));
                } catch (Exception e) { Toast.makeText(MainActivity.this, "Could not open the share menu", Toast.LENGTH_LONG).show(); }
            });
        }
        @JavascriptInterface public void openUrl(String url) {
            runOnUiThread(() -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "Could not open the website", Toast.LENGTH_LONG).show(); }
            });
        }
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
