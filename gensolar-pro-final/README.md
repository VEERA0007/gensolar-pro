# GenSolar Pro

GenSolar Solar Calculator Android app.

## What is in version 1.2
- New welcome screen with the Gensolar logo, Login and Explore GenSolar.
- Login with mobile number and SMS OTP (Firebase). The role (Customer, Representative, Manager) comes from the account, never from a screen choice.
- Explore GenSolar opens the calculator without signing in.
- Solar estimator with slab-wise electricity tariffs for Telangana, Andhra Pradesh, Odisha and Chhattisgarh (FY 2026-27), per-state sunlight defaults, and commercial and industrial energy rates.
- New yellow bolt app icon.
- A fixed debug signing key (`app/debug.keystore`) so every build has the same fingerprints for Firebase.

## Firebase fingerprints (add once)
Firebase Console, Project settings, your Android app `com.gensolar.pro`, Add fingerprint. Add both:

- SHA-1: `AA:C3:A2:09:8C:14:EC:CC:48:E5:7F:34:14:C3:6E:D4:6C:2A:F9:40`
- SHA-256: `4F:A8:E2:FD:3F:4D:1D:41:BE:E8:25:97:5A:B1:F1:CD:B9:15:A5:12:84:B0:C6:F5:91:40:E5:FC:2A:A6:A7:88`

Also check that Authentication, Sign-in method, Phone is turned on and that Firestore is created.

## Build the APK
Actions tab, Build GenSolar APK, Run workflow. When it finishes, download `GenSolar-Pro-debug-APK` from the run.

More notes are in `gensolar-android-project/gensolar-android/README.txt` and `LEAD_CRM_SETUP.md`.
