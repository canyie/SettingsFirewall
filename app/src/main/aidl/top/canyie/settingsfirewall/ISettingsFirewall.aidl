// ISettingsFirewall.aidl
package top.canyie.settingsfirewall;

import top.canyie.settingsfirewall.Replacement;

interface ISettingsFirewall {
    int[] getTargets() = 1;
    void setTarget(int uid, boolean enabled) = 2;
    Replacement[] getReplacements(int uid) = 3;
    void setReplacement(int uid, String setting, String value, int flags) = 4;
    void deleteReplacement(int uid, String setting) = 5;
}
