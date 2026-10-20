# ZeekrWake

Wakes the Zeekr app and presses its connect button when your car's Bluetooth LE
advertisement comes into range. No modification of the Zeekr app, no root.

## Why this works where Samsung Modes doesn't

Samsung Modes triggers on geofence / Wi-Fi / time. All of those are approximate,
throttled and routinely skipped.

This uses `BluetoothLeScanner.startScan(filters, settings, pendingIntent)`,
added in Android 8.0. You hand the Bluetooth stack a filter and a PendingIntent.
The filter is offloaded to the Bluetooth controller, so matching costs almost no
battery, and the OS fires your PendingIntent **even if your app has been killed**.
It is the same mechanism Google's own Fast Pair uses. The radio itself tells you
the car is there, which is exactly the "the phone knows it's there" you noticed.

Caveats, so there are no surprises:
- A PendingIntent scan does not survive a reboot. `BootReceiver` re-registers it.
- Background activity launch is blocked on Android 10+ unless the app holds
  "Display over other apps". That permission is the documented exemption.
- The tap step needs an accessibility service. It is armed for 20 seconds after a
  detection and disarmed after one successful tap, so it is not watching your
  screen the rest of the day.

## Build

No Android SDK in this container, so this has not been compiled here. Build it
yourself:

1. Open the `zeekrwake` folder in Android Studio (File > Open).
2. Let it sync, then Run on your phone with USB debugging on.

Or headless, with the SDK installed and `ANDROID_HOME` set:

    ./gradlew assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk

## Setup, in order

1. Open ZeekrWake.
2. Tap **Find the Zeekr package**. It fills in the package name.
3. Stand next to the car with the Zeekr app connected, tap
   **Scan 15s: list nearby BLE devices**. Lock/unlock with the fob while it runs
   to make the car advertise. Find the row that is the car (strongest rssi, name
   often contains ZEEKR / a VIN fragment / the model).
   - Put its MAC in the **MAC** field.
   - If the MAC changes between scans the car is using a rotating private
     address: leave MAC blank and put one of its **service UUIDs** in instead.
     That is stable.
4. **Grant: nearby devices permission**
5. **Grant: display over other apps**
6. **Open accessibility settings** and turn on "ZeekrWake auto-tap".
7. **Battery: set unrestricted** for ZeekrWake *and* for the Zeekr app.
8. Set **Button text to tap** to whatever the Zeekr app's connect control
   actually says. Default is `Disconnected,Connect,Bluetooth,连接`. Check the
   real label on your app version and trim the list to just that one, it makes
   the tap faster and stops it hitting the wrong thing.
9. Tap **Test: launch + tap now**. Zeekr should come forward and connect.
10. Tap **ARM**. Done. Lock the phone, walk away, walk back.

Watch it work with:

    adb logcat -s ZeekrWake

## Tuning

- `Wake.COOLDOWN_MS` (60s) stops it re-launching Zeekr repeatedly while you are
  sitting in range.
- `ScanSettings.SCAN_MODE_LOW_POWER` is a ~5s duty cycle. If detection feels
  slow, `SCAN_MODE_BALANCED` is quicker and still cheap. Don't use LOW_LATENCY
  for the background scan.
- If `startScan` returns a non-zero code, the usual cause is that Android has hit
  its limit of registered scans, or Bluetooth was off when you armed it.

## If you want to actually patch the Zeekr app

Only worth it if your phone is rooted. Use **LSPosed/Xposed**, not apktool: a
repackaged APK has a different signature, which can fail the backend's integrity
check and, more annoyingly, is a fresh install as far as the digital key is
concerned, so you may have to re-enrol the key in the car.

LSPosed hooks the app in place, signature untouched. The approach:

1. `jadx-gui` the APK, search for `BluetoothGatt`, `connectGatt`, `startScan`,
   and the string resource behind the connect button.
2. Find the method the connect button's click listener calls.
3. Write an LSPosed module that hooks `Application.onCreate` in the Zeekr
   package, registers its own PendingIntent scan, and on detection calls that
   method directly by reflection.

That gets you a connect with no UI involved at all. It also breaks on every app
update, which is why the companion app above is the better trade.
