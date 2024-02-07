package top.canyie.settingsfirewall;

import android.content.ContentProvider;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * @author canyie
 */
public class SettingsProviderHook extends XC_MethodHook implements IXposedHookLoadPackage {
    public static final String METHOD = "GET_SettingsFirewall";
    private volatile Context context;

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"com.android.providers.settings".equals(lpparam.packageName)) return;
        XposedHelpers.findAndHookMethod(
                "com.android.providers.settings.SettingsProvider",
                lpparam.classLoader,
                "call",
                String.class,
                String.class,
                Bundle.class,
                this
        );
    }

    @Override protected void beforeHookedMethod(MethodHookParam param) {
        ContentProvider contentProvider = (ContentProvider) param.thisObject;
        String method = (String) param.args[0];
        String name = (String) param.args[1];
//        Bundle args = (Bundle) param.args[2];
        if (context == null) {
            synchronized (this) {
                if (context == null) {
                    context = contentProvider.getContext();
                    SettingsFirewallService.init(context);
                }
            }
        }
        int callingUid = Binder.getCallingUid();
        int flag;
        switch (method) {
            case METHOD: {
                // Verify the calling package is actually our module. If not, don't send anything.
                // We catch all possible exception to make sure unauthorized apps can't fool us.
                // For example, if the caller tries to lie us that it is another package,
                // getCallingPackage will throw an exception but we avoid delivering the exception
                // back to the caller because it is a side channel and can be detected
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                            && !BuildConfig.APPLICATION_ID.equals(contentProvider.getCallingPackage()))
                        return;
                    param.setResult(SettingsFirewallService.BUNDLE);
                } catch (Exception ignored) {
                }
                return;
            }
            case "GET_global":
                flag = Replacement.FLAG_GLOBAL;
                break;
            case "GET_secure":
                flag = Replacement.FLAG_SECURE;
                break;
            case "GET_system":
                flag = Replacement.FLAG_SYSTEM;
                break;
            default:
                return;
        }

        String replacement = SettingsFirewallService.getReplacement(callingUid, name, flag);
        if (replacement != null) {
            Bundle result = new Bundle(1);
            result.putString(Settings.NameValueTable.VALUE, replacement);
            param.setResult(result);
        }
    }
}
