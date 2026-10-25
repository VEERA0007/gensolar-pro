# GenSolar Pro Lead CRM — setup and production notes

## Included in this source build
- Android Firebase Phone OTP login.
- Customer, representative, and manager roles read from the signed-in user's Firestore profile.
- Lead sync bridge for Android: load leads from Firestore, save new estimates/contacts, sync status and follow-up-note changes, and delete leads.
- Local phone storage remains as a fallback, so a cloud error should not silently erase the current device copy.
- Existing solar calculator, estimate sharing, PDF printing, and Engineering & Site Design workspace remain available.

## Important limitations before real customer use
- The Android lead-sync code is committed, but Firestore rules must be published from the included `firestore.rules` file before cloud sync can work safely.
- The browser version does not have phone OTP or the Android Firestore bridge; its lead data stays local to that browser.
- Employee/manager roles must be assigned by an administrator in Firebase Console. A new phone-number account is created as Customer.
- Real SMS delivery, cross-device sync, and permission boundaries still need a live test with real test accounts. Do not enter real customer data until rules are published and those tests pass.

## Recommended production architecture
1. Firebase Authentication Phone provider for customer OTP. Configure supported app verification/reCAPTCHA requirements and authorized domains/app fingerprints as required.
2. Employee accounts should be provisioned by an administrator and use secure company sign-in. Do not allow users to select their own manager role at registration.
3. Cloud Firestore for users, customers, leads, calculations, follow-ups, assignments, quotations, and audit records.
4. Firestore Security Rules must enforce tenant membership, roles, lead ownership, and customer record ownership server-side.
5. Use Cloud Functions or another trusted backend for role assignment, lead assignment, audit events, exports, and sensitive operations.
6. Never put service-account private keys in this app.
7. Publish a privacy policy and collect consent before storing contact details or sending marketing follow-ups.

## Suggested data model
- `users/{uid}`: role (`customer`, `representative`, `manager`), organizationId, displayName, phone, active
- `customers/{customerId}`: ownerUid, contact fields, consent, createdAt
- `leads/{leadId}`: customerUid, assignedRepUid, organizationId, state, district, segment, bill, units, roofArea, system, estimatedCapacity, status, nextFollowupAt, createdAt, updatedAt
- `calculations/{calculationId}`: ownerUid, leadId, inputSnapshot, resultSnapshot, createdAt
- `followups/{followupId}`: leadId, repUid, activityType, notes, nextActionAt, createdAt
- `quotations/{quotationId}`: leadId, version, lineItems, price, tax, status, createdAt
- `auditLogs/{logId}`: actorUid, action, recordType, recordId, timestamp

## Suggested access policy
- Customer: only their own profile and calculations; own lead and quotations; cannot assign leads or change sales status.
- Representative: assigned leads and follow-up activities; no role assignment.
- Manager: leads and reports within their organization; can assign representatives and review performance.
- Admin: manage organization and employee roles. Role changes must be done by a trusted backend, not a client-settable field.
- Validate all writes and use audit records for important changes.

## Before real launch
- Configure Firebase Phone Authentication; test OTP on supported devices.
- Configure Firestore and security rules; test unauthorized reads/writes using the Firebase Emulator Suite.
- Add employee invitation/approval and account recovery.
- Test duplicate leads, phone-number changes, account deletion, data export, backups, offline sync, and poor connectivity.
- Verify PDF print/save on actual Android devices.
- Review privacy, consent, retention, and applicable Indian data-protection requirements with your organization.
- Generate a signed release App Bundle only after testing; do not distribute a debug APK for production.


## One-time Firebase rules setup
1. Open Firebase Console and select the existing GenSolar project.
2. Open **Firestore Database → Rules**.
3. Open the repository file `gensolar-android-project/gensolar-android/firestore.rules`.
4. Copy the complete file into the Rules editor and select **Publish**.
5. Create/verify a test customer by signing in with OTP. Customer accounts are created with the Customer role automatically.
6. For each company employee, open Firestore's `users` collection and edit that employee's document (the document ID is their Firebase Authentication UID). Set `role` to `rep` or `manager`, and set `organizationId` to the same company ID for all members of the company. Never let employees choose their own role in the app.
7. Test with non-sensitive sample leads: a customer should see their own leads; a representative should see assigned leads; a manager should see leads for their organization.

Do not publish permissive rules such as `allow read, write: if true`. If a permission test fails, stop before entering customer data.
