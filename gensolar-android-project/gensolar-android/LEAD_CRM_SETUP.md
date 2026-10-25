# GenSolar Pro Lead CRM — setup and production notes

## Included in this source build
- Role selection: Customer, Representative, Admin/Manager.
- Demo-mode workspace and local demo lead records.
- Existing solar calculator remains available after choosing a profile.
- Save a calculation as a demo lead, update status, add follow-up notes, and export CSV for company demo profiles.
- Customer demo view is filtered to the entered mobile number.
- Android icon resources are retained; build workflow runs from repository root.

## IMPORTANT — demo mode is not production authentication
The current login screen is explicitly marked DEMO MODE. It does NOT send an OTP, verify a phone number, securely authenticate employees, synchronize records between phones, or enforce server-side access controls. Demo records are stored in WebView local storage on this device. Do not enter real customer data until production backend is connected.

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
