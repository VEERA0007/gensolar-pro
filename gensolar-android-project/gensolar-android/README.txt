GenSolar Pro Android wrapper project

This project packages the existing GenSolar Pro calculator HTML as an Android app using WebView.

Build an APK:
1. Open this folder in Android Studio (recent version) on a computer with internet access.
2. Let Android Studio sync and install required Android SDK components.
3. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
4. The debug APK will be created at app/build/outputs/apk/debug/app-debug.apk.

Alternative cloud build:
1. Upload this project folder to a GitHub repository.
2. Open Actions and run "Build GenSolar APK" (or push to main/master).
3. Download the artifact named GenSolar-Pro-debug-APK from the completed workflow.

Note: This is a debug APK build setup, not a signed production release. For Play Store distribution, create and securely manage a release signing key. The calculator is bundled locally and its calculation interface can run offline.
