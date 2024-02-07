## SettingsFirewall
An Xposed module that blocks shitty apps from accessing your system settings (for example, check if development settings is enabled on the device, or check if there are running accessibility services).

Please note that only accesses to system settings (e.g. [Settings APIs](https://developer.android.com/reference/android/provider/Settings) or `/system/bin/settings get`) can be intercepted by this module. 
Accesses to system properties (e.g. `android.os.SystemProperties APIs`, `__system_property_get` or `getprop`) or other system APIs cannot be blocked.

### Usage
Requirements: Android 4.3+ devices with a root Xposed framework installed.
This module requires to hook system components, so rootless Xposed frameworks (e.g. LSPatch, VirtualXposed or TaiChi-Ying) cannot be supported.

For LSPosed users, select only "System Framework" and reboot.

For [Dreamland](https://github.com/canyie/Dreamland) users, select "Settings Provider" (`com.android.providers.settings`) and reboot.

### FAQ
- Q: Why is this module using the old Holo / Android Design?
  A: Thanks to all Material Design 3 (Material You) missionaries, 
I had to uninstall many apps and rollback my Android system to an old one
to avoid being attacked by your new "amazing" design. Happy now?

### License
This project is under the MIT license with the following additional restrictions:

- You are **FORBIDDEN** to do anything that would make Android 4.3 users unable to use this app
(e.g. changing `minSdkVersion` to anything higher than `18`)
or use code from this project in a project that does not support Android 4.3.
- You are **FORBIDDEN* to change the UI design style to Material Design 3 (Material You) 
or use code from this project in a project that uses Material Design 3 (Material You).
