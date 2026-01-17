# Privacy Policy

**COSMIC Connect Android**

**Effective Date:** 2026-01-17
**Last Updated:** 2026-01-17
**Version:** 1.0.0

---

## Introduction

COSMIC Connect ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains how COSMIC Connect handles data when you use our Android application.

**Key Principle:** COSMIC Connect does not collect, store, or transmit any of your personal data to external servers. All communication is local and peer-to-peer between your devices.

---

## Summary

**What we do:**
- ✅ Enable local communication between your devices
- ✅ Encrypt all communication with TLS
- ✅ Keep all data on your local network
- ✅ Give you full control over what is shared

**What we DON'T do:**
- ❌ Collect any personal data
- ❌ Use analytics or tracking
- ❌ Share data with third parties
- ❌ Use cloud services
- ❌ Show advertisements
- ❌ Sell your data

---

## Information We Don't Collect

### No Server-Side Data Collection

COSMIC Connect operates entirely peer-to-peer on your local network. We do not:

- Collect personal information
- Store data on remote servers
- Track your usage or behavior
- Use analytics or telemetry
- Collect crash reports (unless you explicitly opt-in)
- Record your location
- Monitor your activity
- Create user profiles
- Use cookies or tracking mechanisms

### No Third-Party Data Sharing

We do not share any data with third parties because:

- We don't collect data in the first place
- There are no third-party services integrated
- No advertising networks
- No analytics platforms
- No cloud storage providers
- No external APIs

---

## Information You Share Locally

### Peer-to-Peer Communication

COSMIC Connect enables direct communication between your Android device and your COSMIC Desktop computer. The following data may be shared between **your own devices** based on which features you enable:

#### Files and Media
- **What:** Files you explicitly choose to share
- **When:** Only when you use the "Share" function
- **Where:** Direct transfer between your devices over local network
- **Storage:** Files are stored only on the receiving device
- **Control:** You choose which files to share

#### Clipboard Content
- **What:** Text you copy on either device
- **When:** Only when Clipboard sync is enabled
- **Where:** Synchronized between your paired devices
- **Storage:** Temporary storage on both devices
- **Control:** Can be disabled anytime in settings

#### Notifications
- **What:** Notification content from apps you select
- **When:** Only when Notification mirroring is enabled
- **Where:** Displayed on your paired desktop
- **Storage:** Not stored, only displayed
- **Control:** Choose which apps' notifications to mirror

#### SMS Messages
- **What:** Text messages from your phone
- **When:** Only when SMS plugin is enabled
- **Where:** Accessible from your paired desktop
- **Storage:** Messages remain on your phone
- **Control:** Can be disabled anytime

#### Battery Status
- **What:** Your phone's battery level and charging status
- **When:** Only when Battery plugin is enabled
- **Where:** Displayed on your paired desktop
- **Storage:** Not stored, only displayed
- **Control:** Can be disabled anytime

#### Media Playback Information
- **What:** Currently playing media information (artist, title, album)
- **When:** Only when MPRIS plugin is enabled
- **Where:** Displayed on your phone
- **Storage:** Not stored, only displayed
- **Control:** Can be disabled anytime

#### Contact Information
- **What:** Contact names associated with notifications/SMS
- **When:** Only when relevant plugins are enabled
- **Where:** Used to display contact names on notifications
- **Storage:** Not shared, only used for display
- **Control:** Disable relevant plugins to prevent

**Important:** All this data is shared **only between your own devices** on your local network. Nothing leaves your network. Nothing goes to our servers or any cloud service.

---

## Android Permissions

COSMIC Connect requests the following Android permissions. Here's why we need each one and how we use it:

### Required Permissions

#### Location Permission (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
- **Why needed:** Android requires this permission for apps that scan for nearby devices via WiFi
- **What we use it for:** WiFi network scanning to discover your COSMIC Desktop computer
- **What we DON'T use it for:** We never access your GPS location, track your movements, or record where you are
- **Android requirement:** This is an Android platform requirement for WiFi scanning, not our choice
- **Can you revoke it:** Yes, but device discovery won't work without it

### Optional Permissions

#### Notification Access (BIND_NOTIFICATION_LISTENER_SERVICE)
- **Why needed:** To read your phone's notifications
- **What we use it for:** Mirror selected app notifications to your desktop
- **Control:** You choose which apps' notifications to mirror
- **Can you revoke it:** Yes, notification mirroring won't work without it

#### Phone & SMS (READ_SMS, SEND_SMS, READ_PHONE_STATE, READ_CALL_LOG)
- **Why needed:** To integrate SMS and phone calls with your desktop
- **What we use it for:** Send/receive SMS from desktop, show incoming call notifications
- **Can you revoke it:** Yes, SMS and telephony features won't work without it

#### Storage (READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
- **Why needed:** To access files you want to share and save received files
- **What we use it for:** Read files you explicitly choose to share, save files you receive
- **Can you revoke it:** Yes, file sharing won't work without it

#### Contacts (READ_CONTACTS)
- **Why needed:** To show contact names in notifications and SMS
- **What we use it for:** Display contact names instead of just phone numbers
- **Can you revoke it:** Yes, you'll see phone numbers instead of names

All optional permissions can be revoked at any time through your Android settings. The app will continue to work, just without the features that require those permissions.

---

## How We Protect Your Privacy

### Local-Only Communication

- **No Internet Required:** COSMIC Connect works entirely on your local WiFi network
- **No Cloud Services:** We don't use any cloud storage or cloud services
- **Peer-to-Peer:** All communication is direct between your devices
- **No External Servers:** We don't operate any servers that your data passes through

### Encryption

- **TLS 1.2+:** All communication is encrypted using industry-standard TLS
- **2048-bit RSA:** Strong cryptographic keys
- **Certificate Pinning:** Prevents man-in-the-middle attacks
- **Secure Pairing:** Initial pairing requires physical access to both devices

### Pairing Security

- **Mutual Authentication:** Both devices must accept pairing
- **Physical Access Required:** You must have both devices to pair them
- **Certificate Exchange:** Secure cryptographic certificates exchanged during pairing
- **Revocable:** Unpair anytime to revoke access

### No User Accounts

- **No Registration:** No need to create an account
- **No Login:** No username or password
- **No Profile:** We don't create user profiles
- **Anonymous:** We don't know who you are

---

## Your Privacy Rights

### Control Over Your Data

You have complete control over what data is shared:

- **Enable/Disable Features:** Turn any feature on or off anytime
- **Choose What to Share:** Explicitly choose which files, apps, and data to share
- **Unpair Devices:** Disconnect and unpair devices anytime
- **Revoke Permissions:** Remove Android permissions anytime

### Right to Access

All your data remains on your devices. You can:

- View all paired devices
- See which plugins are enabled
- Review connection history (if debug logging enabled)
- Access all files shared or received

### Right to Delete

You can delete all COSMIC Connect data:

- Unpair all devices (removes pairing certificates)
- Disable all plugins (stops all data sharing)
- Clear app data (removes all app configuration)
- Uninstall the app (removes everything)

### Right to Export

You can export configuration:

- Backup pairing information
- Export settings
- Save debugging logs (if enabled)

---

## Children's Privacy

COSMIC Connect does not knowingly collect any data from anyone, including children under 13. The app is designed for general use and does not specifically target children.

If you are a parent or guardian and believe your child has used COSMIC Connect, there is no data to be concerned about as we don't collect any data.

---

## Open Source

COSMIC Connect is open source software licensed under GPL-3.0. This means:

- **Transparency:** Anyone can review the source code
- **Auditability:** Security researchers can verify our privacy claims
- **Community:** Developed by and for the community
- **No Hidden Code:** What you see is what you get

**Source Code:**
- Android app: https://github.com/olafkfreund/cosmic-connect-android
- Rust core: https://github.com/olafkfreund/cosmic-connect-core
- Desktop applet: https://github.com/olafkfreund/cosmic-applet-kdeconnect

---

## Third-Party Components

COSMIC Connect uses several open-source libraries. These libraries run locally on your device and do not send data anywhere:

### Android Libraries
- **Kotlin Standard Library:** Apache 2.0
- **Jetpack Compose:** Apache 2.0
- **AndroidX Libraries:** Apache 2.0
- **Material Design 3:** Apache 2.0

### Rust Libraries
- **tokio:** MIT
- **rustls:** Apache 2.0/MIT
- **serde:** MIT/Apache 2.0
- **uniffi-rs:** MPL 2.0

All these libraries run entirely on your device. None of them send data to external servers.

---

## Data Security

### Security Measures

We implement industry-standard security measures:

- **Encryption in Transit:** TLS 1.2+ for all communication
- **Secure Pairing:** Certificate-based authentication
- **No Data at Rest:** We don't store your shared data
- **Secure Coding:** Modern Rust + Kotlin for memory safety
- **Regular Updates:** Security updates as needed

### What Happens If...

**If you lose your phone:**
- Unpair your phone from your desktop to revoke access
- No data has been collected on any servers
- Your desktop remains secure

**If someone steals your phone:**
- Your desktop can only be accessed if the phone is unlocked and paired
- Unpair the device immediately from your desktop
- Consider remote wipe through Android Device Manager

**If you sell your phone:**
- Unpair all devices before factory reset
- Factory reset removes all COSMIC Connect data
- Pairing certificates are removed

---

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Changes will be:

- Posted on this page with a new "Last Updated" date
- Communicated through the app (for significant changes)
- Announced on our GitHub repository

Continued use of COSMIC Connect after changes constitutes acceptance of the updated Privacy Policy.

---

## Compliance

### GDPR (European Union)

COSMIC Connect is GDPR-compliant because:

- **No Personal Data Collection:** We don't collect personal data
- **Local Processing:** All processing happens on your devices
- **User Control:** You control all data sharing
- **No Profiling:** We don't create user profiles
- **Right to Access:** All data is on your device
- **Right to Delete:** Uninstall anytime
- **Data Minimization:** We only process what's necessary
- **Transparency:** This policy explains everything

### CCPA (California)

COSMIC Connect complies with CCPA because:

- **No Sale of Data:** We don't sell personal information (we don't collect it)
- **No Data Collection:** We don't collect personal information
- **User Rights:** You control all aspects of the app
- **Transparency:** This policy is clear and accessible

### Other Jurisdictions

COSMIC Connect's privacy-first design means we comply with privacy laws worldwide because we simply don't collect, store, or process personal data.

---

## Contact Information

### Questions About This Privacy Policy

If you have questions about this Privacy Policy:

- **GitHub Issues:** https://github.com/olafkfreund/cosmic-connect-android/issues
- **GitHub Discussions:** https://github.com/olafkfreund/cosmic-connect-android/discussions
- **Email:** privacy@cosmic-connect.example.com (if available)

### Security Issues

If you discover a security vulnerability:

- **Responsible Disclosure:** Email security@cosmic-connect.example.com (preferred)
- **GitHub Security Advisories:** Use private security advisory feature
- **Please don't:** Publicly disclose until we've had time to fix it

We take security seriously and will respond promptly to security reports.

---

## Your Consent

By using COSMIC Connect, you consent to:

- Local peer-to-peer communication between your devices
- Data sharing that you explicitly enable and configure
- This Privacy Policy

You can withdraw consent at any time by:

- Disabling specific features
- Unpairing devices
- Uninstalling the app

---

## Verification

### How to Verify Our Privacy Claims

Because COSMIC Connect is open source, you can verify everything:

1. **Review Source Code:** Check GitHub repositories
2. **Network Analysis:** Use network monitoring tools (all traffic is local)
3. **Community Audit:** Security researchers have reviewed the code
4. **No Network Calls:** No code makes external API calls
5. **Local Only:** All functionality works without internet

**We encourage** security researchers and privacy advocates to review our code and verify these claims.

---

## Additional Notes

### Based on KDE Connect

COSMIC Connect is based on KDE Connect Android, which has been privacy-focused since its inception. We've maintained and enhanced these privacy principles.

### Community Trust

COSMIC Connect is built by the open-source community, for the community. Our reputation depends on maintaining user trust and privacy.

### No Business Model

COSMIC Connect is:
- Free (no cost)
- No ads
- No premium features
- No subscriptions
- No data monetization

Our goal is to provide a useful tool, not to monetize user data.

---

## Summary

**Simply put:** COSMIC Connect doesn't collect any of your data. Everything stays on your devices, on your local network. We don't know who you are, what you share, or how you use the app. Your privacy is protected because we designed it that way from the start.

---

## Questions?

See our [FAQ](FAQ.md) for common questions or [User Guide](USER_GUIDE.md) for more information.

---

**Privacy Policy Version:** 1.0.0
**Effective Date:** 2026-01-17
**Last Updated:** 2026-01-17

**COSMIC Connect - Your Privacy, Your Data, Your Devices.**
