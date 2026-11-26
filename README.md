# Concentrate

Focus mode for Android. Pick the apps you need, and Concentrate cuts internet access for everything else until you stop the session.

## How it works

Concentrate uses Android's `VpnService`, which is the built-in, non-root way for an app to control other apps' network traffic.

- Apps you keep online are excluded from the VPN, so they use the normal connection.
- Every other app is routed into a virtual network interface.
- Concentrate reads those packets and drops them, so those apps get no internet.

Stopping the session closes the interface and restores normal access.

## Limits

- Concentrate does not make your mobile connection faster. The radio link, carrier, and signal set the speed.
- Blocking background traffic can reduce lag spikes caused by sync and updates. Results depend on what else your phone is doing.
- Only one VPN can be active at a time, so Concentrate conflicts with other VPN apps.
- Android shows a key icon while the VPN is active.
- Android only. iOS doesn't allow this kind of per-app blocking.

## Status

Early stage. Scaffolding and the focus service come first, then the app picker, then measured ping and jitter before and after a session.

## Build

Open the project in Android Studio (Hedgehog or newer), let Gradle sync, and run on a device with Android 8.0 (API 26) or newer.

## Permissions

- `INTERNET` is required to route traffic through the focus VPN.
- `QUERY_ALL_PACKAGES` lists installed apps in the picker. Google Play restricts this permission, so a public release would need a declared use or a narrower approach.
- The VPN consent dialog appears the first time you start a session.
