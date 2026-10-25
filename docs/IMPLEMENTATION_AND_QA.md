# GenSolar implementation and QA plan

## Goal
Recreate the approved three-screen reference (Welcome → Login → Role selection) while preserving the existing solar estimator and existing working capabilities.

## Safe delivery stages
1. **Protect the baseline:** work on a feature branch or additive files first; never delete the existing WebView estimator or Firebase configuration during migration.
2. **UI parity:** match the supplied reference for layout, typography, solar imagery, deep-green buttons, rounded cards, and role selection. Keep screen components separate.
3. **Navigation and behavior:** wire Welcome → Login → Roles → existing calculator. A UI-only Google button must not imply Google authentication is configured; SMS OTP must use the configured Firebase provider.
4. **Maintainable code:** use TypeScript strict mode, reusable React components and shared types for the web app. Existing Android wrapper remains compatible while migration is validated.
5. **Automated validation:** run type-check/build/test tasks and Android Gradle build on GitHub Actions. Fix failing checks before publishing an artifact.
6. **Release QA:** inspect web and Android screenshots, verify all navigation, role selection, form validation, calculator calculations, app launch, and back navigation. A successful compile alone is not end-to-end verification.

## Current baseline findings
- The Android app currently hosts a large HTML/JavaScript application inside WebView.
- Welcome screen handlers exist (`openLogin()` and `exploreWebsite()`). These must not be replaced with hard-coded URLs or DOM bypasses.
- The current login dialog is OTP-focused and does not visually match the approved email/mobile/employee-ID login reference.
- The role-selection reference includes Admin, Sales Head, Regional Manager, Sales Manager, Sales Representative, Channel Partner, and Customer. Authorization must be enforced server-side; hiding UI is not access control.
- The calculator includes residential/commercial/industrial segments and Telangana, Andhra Pradesh, Odisha and Chhattisgarh options. Preserve it during UI changes.

## Security and release checks
- Do not commit production secrets, private signing keys, OTPs, or real customer data.
- Keep Firebase client config separate from server credentials; client config is not a substitute for Firestore security rules.
- Never grant Admin privileges based only on a client-selected role.
- Test Firebase OTP on a configured device/app and authorized test number before declaring login fixed.
- Publish release APKs only after checks pass; debug APK success is not production readiness.

## Status
This document records the intended delivery gate. Items are complete only when implemented and verified in CI/device testing; do not mark a stage complete based solely on a plan or successful compile.
