# Agent notes (DayRoute / Daily Curator Android)

## Android device install — do this first, avoid redundant retries

1. **Use one `adb` only** — Prefer the SDK bundled with this project’s Gradle:
   - `export ANDROID_HOME="$HOME/Android/Sdk"` (or match `sdk.dir` in `local.properties`, which is gitignored and machine-specific).
   - `export PATH="$ANDROID_HOME/platform-tools:$PATH"`.
   - **Do not** mix `/usr/bin/adb` with `$ANDROID_HOME/platform-tools/adb`. Different versions (e.g. 39 vs 41) restart the server and **drop** wireless sessions.

2. **Check `adb devices` before pairing or repeating steps** — Run:
   - `adb devices -l`
   - If a device shows as `device` (including `*_adb-tls-connect._tcp` wireless entries), **skip `adb pair`** and go straight to install. Pairing is only needed when there is **no** usable device.

3. **`adb pair` “protocol fault” is not always fatal** — The phone may still appear in `adb devices` from a prior pair or mDNS. **If a device is listed as `device`, install immediately** instead of pairing again in a loop.

4. **Wireless debugging (user gives IP:port + code)** — On the phone: *Developer options → Wireless debugging*. The **pairing** port/code is for `adb pair IP:PAIR_PORT`; the **connection** IP/port may differ. After pairing (when required), `adb connect IP:PORT` may be needed depending on Android version. Again: if `adb devices` already shows the phone, **connect/install without re-pairing**.

5. **Install command** (from repo root, device must show as `device`):
   - `./gradlew :app:installDebug`
   - Or: `adb install -r app/build/outputs/apk/debug/app-debug.apk` after a debug build.

6. **`local.properties`** — Contains `sdk.dir`; it is **not** committed. Adjust on each machine if Gradle cannot find the SDK.

Following the above avoids repeated `adb pair` attempts when the session is already valid and avoids adb version mismatches that look like “no devices.”
