package top.canyie.settingsfirewall;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author canyie
 */
public class App extends Application {
    public static final int MY_UID = Process.myUid();
    private static ISettingsFirewall service;

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            HiddenApiBypass.addHiddenApiExemptions("");
    }

    public static ISettingsFirewall getService(Context context) {
        if (service == null) {
            Uri uri = Uri.parse("content://" + Settings.AUTHORITY);
            try {
                Bundle result = context.getContentResolver().call(uri, SettingsProviderHook.METHOD,
                        null, null);
                if (result != null)
                    service = ISettingsFirewall.Stub.asInterface(result.getBinder(Settings.NameValueTable.VALUE));
            } catch (Exception ignored) {
            }
        }
        return service;
    }

    public static List<AppInfo> getSortedList(Context context, ISettingsFirewall service) {
        PackageManager pm = context.getPackageManager();
        var apps = pm.getInstalledPackages(0);
        int[] enabledUids;
        try {
            enabledUids = service.getTargets();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
        Arrays.sort(enabledUids);
        List<AppInfo> list = new ArrayList<>();
        Set<String> addedSharedUserIds = new HashSet<>();
        Drawable androidIcon = null;
        for (var app : apps) {
            var appInfo = app.applicationInfo;
            int uid = appInfo.uid;
            // Skip system core process and ourselves
            if (uid < Process.SHELL_UID) continue;
            if (uid == App.MY_UID) continue;
            AppInfo info;
            if (TextUtils.isEmpty(app.sharedUserId)) {
                info = new AppInfo();
                info.name = appInfo.loadLabel(pm).toString();
                info.icon = appInfo.loadIcon(pm);
            } else {
                var sharedUserId = app.sharedUserId;
                if (!addedSharedUserIds.add(sharedUserId)) continue;
                info = new AppInfo();
                info.name = "[SharedUserID] " + sharedUserId;
                if (androidIcon == null)
                    androidIcon = context.getResources().getDrawable(android.R.mipmap.sym_def_app_icon);
                info.icon = androidIcon;
                info.isSharedUid = true;
            }
            info.enabled = Arrays.binarySearch(enabledUids, info.uid = uid) >= 0;
            list.add(info);
        }
        Collections.sort(list, AppInfo.COMPARATOR);
        return list;
    }

    public static List<Replacement> getSettings(ISettingsFirewall service, int uid) {
        Replacement[] replacements;
        try {
            replacements = service.getReplacements(uid);
        } catch (RemoteException e) {
            // Should never happen: the remote service runs in Settings Provider which is in system_server
            throw new RuntimeException(e);
        }
        List<Replacement> out = new ArrayList<>();
        addSettings(Settings.System.class, out, Replacement.FLAG_SYSTEM, replacements,
                "MOVED_TO_SECURE", "MOVED_TO_GLOBAL", "MOVED_TO_SECURE_THEN_GLOBAL");
        addSettings(Settings.Secure.class, out, Replacement.FLAG_SECURE, replacements,
                "MOVED_TO_GLOBAL");
        addSettings(Settings.Global.class, out, Replacement.FLAG_GLOBAL, replacements);
        Collections.sort(out, Replacement.COMPARATOR);
        return out;
    }
    private static void addSettings(Class<? extends Settings.NameValueTable> cls,
                                    List<Replacement> out, int flag, Replacement[] replacements,
                                    String... ignore) {
        Set<String>[] ignoreSets = new Set[ignore.length];
        for (int i = 0;i < ignore.length;i++) {
            Set<String> set;
            try {
                Field field = cls.getDeclaredField(ignore[i]);
                field.setAccessible(true);
                set = (Set<String>) field.get(null);
            } catch (Exception e) {
                Log.w("SettingsFirewall", "Unable to access " + cls + "." + ignore[i], e);
                set = Collections.emptySet();
            }
            ignoreSets[i] = set;
        }
        Field[] fields = cls.getDeclaredFields();
        outer: for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (!(Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers))) continue;
            if (field.getType() != String.class) continue;
            field.setAccessible(true);
            String key;
            try {
                key = (String) field.get(null);
            } catch (Exception e) {
                // Should never happen
                throw new RuntimeException(e);
            }
            if (TextUtils.isEmpty(key)) continue;
            for (Set<String> set : ignoreSets)
                if (set.contains(key))
                    continue outer;
            if (replacements != null) {
                for (Replacement existing : replacements) {
                    if (key.equals(existing.key)) {
                        out.add(existing);
                        continue outer;
                    }
                }
            }
            out.add(new Replacement(key, null, flag));
        }
    }
}
