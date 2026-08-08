# AfetComms

An Android application for emergency communication between families and rescue teams without an internet connection.

## Features

- **I'm Safe / SOS** messages (with location attached)
- **BLE** and **Wi-Fi Direct** transport layer
- **Simulation mode** — for single-device testing
- **Full-screen SOS alert** + notification + vibration
- **Message history**, TTL, outbox / retry mechanism
- **Family member** list and **Settings** screen

## Requirements

- Android 8.0+ (API 26)
- Bluetooth LE (for real device testing)
- Permissions: Bluetooth, Location (BLE), Notifications, Vibration

## Installation

1. Open the project with Android Studio
2. **Sync Project with Gradle Files**
3. **Run** on a physical device (BLE functionality is limited on emulators)

```bash
./gradlew assembleDebug
```

## Single Device Testing (Simulation)

Default for debug builds: **Simulation mode is enabled**.

1. Enter your name and family code on the first launch.
2. Send an **SOS** or **I'm Safe** message.
3. **Messages** → `SENT` and ~2 secs later `SIM_Family_Member` → `RECEIVED`
4. **Settings** → simulation, SOS alerts, profile adjustments.

Details: [TESTING.md](TESTING.md)

## Two Device Testing (Real BLE)

1. Disable **Simulation mode** in **Settings** on both phones.
2. Set the **same family code** but different user IDs on each device.
3. Ensure Bluetooth is turned on and all permissions are granted.
4. Device A: SOS → Device B: Receives message + Full-screen SOS alert.

*Note: Release builds have simulation mode disabled by default.*

## Architecture

```text
ui/          → MainActivity, Messages, Settings, SosAlert
ui/main/     → MainViewModel
transport/   → BLE, Wi-Fi Direct, Fake (sim)
data/        → Room (messages, members)
service/     → BleRelayService (foreground)
```

## Next Steps

- [ ] Field testing with two physical devices
- [ ] Refactor package name from `com.example` to production package
- [ ] App signing for Play Store and privacy policy preparation

## Screenshots
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 22" src="https://github.com/user-attachments/assets/4e3e7537-afa8-416a-8662-7b2e267f4297" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 22 (1)" src="https://github.com/user-attachments/assets/db8d0c89-d0c1-4213-8904-9f69f6ff86f0" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 22 (2)" src="https://github.com/user-attachments/assets/d806dbdb-a33a-4a6a-a2b8-99b4cd4d25db" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 23" src="https://github.com/user-attachments/assets/a872346a-7512-4d46-b412-44ff490300dd" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 23 (1)" src="https://github.com/user-attachments/assets/7cd5c5fc-b9c3-4f10-b77e-6c9f9663d18b" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 23 (2)" src="https://github.com/user-attachments/assets/e9a2b72e-f4fc-4c7f-a555-641173c57700" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24" src="https://github.com/user-attachments/assets/db6349e3-cb7e-4040-b2b4-66f7310ee294" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (1)" src="https://github.com/user-attachments/assets/8422d2f4-cb53-4558-a341-13f0f24088e6" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (2)" src="https://github.com/user-attachments/assets/d222cd15-49f3-499a-a306-67822681d80c" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (3)" src="https://github.com/user-attachments/assets/bcba8065-aa72-4e07-9cc8-fea17a2ad4e4" />
<img width="250" alt="WhatsApp Image 2026-08-08 at 18 21 24 (4)" src="https://github.com/user-attachments/assets/924bfc46-35c2-4d7c-884a-9dad1022ef91" />


