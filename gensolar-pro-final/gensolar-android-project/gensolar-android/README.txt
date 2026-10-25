GenSolar Pro v1.2 — Firebase OTP integration preview

WHAT IS INCLUDED
- Existing solar calculator preserved.
- Four-step workflow: project details, engineering, commercial, final estimate.
- Firebase Phone Authentication OTP (requires Firebase project setup and Android SHA fingerprints for verification).
- New accounts are assigned customer role by default; company roles must be provisioned securely by an administrator.
- Device-local profile details and profile photo.
- CRM contact entry, follow-up and CSV export (demo data only).
- Capacity input supports up to 1,000 kWp (1 MWp); panel quantity is calculated from module wattage.
- Module manufacturer and DCR selection, inverter brand/configuration choices.
- State and customer-segment tariff placeholders that update automatically.
- Indicative residential PM Surya Ghar CFA calculation for eligible on-grid DCR selections.
- Print/save PDF, CSV and official reference links.
- Android display cutout safe-area handling for punch-hole/notch screens.

IMPORTANT LIMITATIONS
- This is a test/demo build, not a production CRM.
- OTP integration is implemented but has not been compiled or tested in this environment. Confirm SMS, Firebase App Check/reCAPTCHA fallback and device behavior before release.
- Firestore profile lookup is connected; full customer/lead/quote data synchronization is not yet implemented.
- Management/representative role provisioning is not implemented. Do not grant elevated roles by trusting a client-side selection.
- Existing CRM/profile workflows still contain local WebView storage and are not yet fully synchronized to Firestore.
- Role separation is not enforced by a trusted backend; do not enter real customer data.
- Tariff defaults are indicative planning placeholders, not verified official tariff values. Tariffs depend on DISCOM, consumer category, slab, demand, fuel adjustment, taxes and the current tariff order. Verify the actual bill/tariff order before issuing a quote.
- Subsidy output is a ceiling estimate only, not confirmation of eligibility or approval. PM Surya Ghar applies under prescribed residential, grid-connected, vendor, DCR and portal/DISCOM conditions. Commercial/industrial customers must not be shown residential CFA as applicable.
- Module brands are selectable examples, not a current endorsement or confirmation of each specific model's ALMM eligibility. Check current MNRE ALMM list and model/cell requirements.
- Inverter brand list is for quoting inputs only. Actual inverter capacity, MPPT range, DC/AC ratio, protection, grid code, warranty and approval must be engineered per project.
- Cost defaults are illustrative. Replace them with current supplier quotations and tax/installation assumptions.
- No compiled APK is included in this source package. Use the included GitHub Actions workflow to build a test APK after uploading the source to the repository.

OFFICIAL REFERENCES
- PM Surya Ghar: https://pmsuryaghar.gov.in/
- MNRE rooftop solar programme: https://mnre.gov.in/en/grid-connected-solar-rooftop-programme/
- MNRE ALMM list: https://mnre.gov.in/en/approved-list-of-models-and-manufacturers-almm/
- Andhra Pradesh tariff orders (APSPDCL): https://apspdcl.in/electricity-tariff.php
- Telangana state solar resource links are available in the app.
