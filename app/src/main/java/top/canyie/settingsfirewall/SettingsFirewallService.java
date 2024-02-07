package top.canyie.settingsfirewall;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.SparseArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.robv.android.xposed.XposedBridge;

/**
 * @author canyie
 */
public class SettingsFirewallService extends ISettingsFirewall.Stub {
    public static final SettingsFirewallService INSTANCE = new SettingsFirewallService();
    public static final Bundle BUNDLE;

    private static final String KEY_TARGET_UIDS = "firewall_targets";
    private static final String FILENAME = "settings-firewall";
    private static File policyDir;
    private static SharedPreferences sharedPreferences;
    private static final Set<Integer> targetUids = new HashSet<>();
    private static final SparseArray<List<Replacement>> policyCache = new SparseArray<>();
    private static final Lock readLock, writeLock;

    static {
        BUNDLE = new Bundle(1);
        BUNDLE.putBinder(Settings.NameValueTable.VALUE, INSTANCE.asBinder());
        var lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    private SettingsFirewallService() {
    }

    public static void init(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context = context.createDeviceProtectedStorageContext();
        sharedPreferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
        var uidSet = sharedPreferences.getStringSet(KEY_TARGET_UIDS, null);
        if (uidSet != null) {
            for (String uid : uidSet) {
                try {
                    targetUids.add(Integer.valueOf(uid));
                } catch (NumberFormatException e) {
                    XposedBridge.log("[SettingsFirewall] Found invalid target uid " + uid);
                }
            }
        }
        policyDir = context.getDir(FILENAME, Context.MODE_PRIVATE);
        XposedBridge.log("[SettingsFirewall] Load uid policy from " + policyDir);
        File[] files = policyDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            int uid;
            String filename = file.getName();
            try {
                uid = Integer.parseInt(filename);
            } catch (NumberFormatException e) {
                XposedBridge.log("[SettingsFirewall] Found invalid file " + file);
                continue;
            }
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                Object o = in.readObject();
                policyCache.put(uid, (List<Replacement>) o);
            } catch (Exception e) {
                // Maybe we should delete the file, since the file may be corrupted?
                XposedBridge.log("[SettingsFirewall] Error reading " + file);
                XposedBridge.log(e);
            }
        }
    }

    public static String getReplacement(int uid, String setting, int flag) {
        readLock.lock();
        try {
            if (!targetUids.contains(uid)) return null;
            List<Replacement> replacements = policyCache.get(uid);
            if (replacements != null)
                for (Replacement replacement : replacements)
                    if (setting.equals(replacement.key) && (replacement.flags & flag) != 0)
                        return replacement.value;
        } finally {
            readLock.unlock();
        }
        return null;
    }

    @Override public int[] getTargets() {
        readLock.lock();
        try {
            int N = targetUids.size();
            int[] a = new int[N];
            int i = 0;
            for (Integer uid : targetUids)
                a[i++] = uid;
            return a;
        } finally {
            readLock.unlock();
        }
    }

    @Override public void setTarget(int uid, boolean enabled) {
        writeLock.lock();
        try {
            if (enabled)
                targetUids.add(uid);
            else
                targetUids.remove(uid);
            saveTargets(targetUids);
        } finally {
            writeLock.unlock();
        }
    }

    @Override public Replacement[] getReplacements(int uid) {
        readLock.lock();
        try {
            var list = policyCache.get(uid);
            // Deep-copy the list before releasing the lock, to avoid the content being changed
            // in a time window between returning(unlocking) and writing its content into parcel
            return list != null ? list.toArray(new Replacement[list.size()]) : null;
        } finally {
            readLock.unlock();
        }
    }

    @Override public void setReplacement(int uid, String setting, String value, int flags) {
        writeLock.lock();
        try {
            List<Replacement> replacements = policyCache.get(uid);
            if (replacements == null) {
                policyCache.put(uid, replacements = new ArrayList<>());
            }
            boolean add = true;
            for (Replacement replacement : replacements) {
                if (setting.equals(replacement.key)) {
                    replacement.value = value;
                    replacement.flags = flags;
                    add = false;
                }
            }
            if (add)
                replacements.add(new Replacement(setting, value, flags));
            saveUidRules(uid, replacements);
        } finally {
            writeLock.unlock();
        }
    }

    @Override public void deleteReplacement(int uid, String setting) {
        writeLock.lock();
        try {
            List<Replacement> replacements = policyCache.get(uid);
            if (replacements == null) return;
            for (var iterator = replacements.iterator();iterator.hasNext();) {
                var replacement = iterator.next();
                if (setting.equals(replacement.key)) {
                    iterator.remove();
                }
            }
            saveUidRules(uid, replacements);
        } finally {
            writeLock.unlock();
        }
    }

    private static void saveTargets(Set<Integer> uids) {
        Set<String> stringSet = new HashSet<>(uids.size(), 2);
        for (Integer uid : uids) {
            stringSet.add(uid.toString());
        }
        sharedPreferences.edit().putStringSet(KEY_TARGET_UIDS, stringSet).commit();
    }

    private static void saveUidRules(int uid, List<Replacement> replacements) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(
                policyDir, Integer.toString(uid))))) {
            out.writeObject(replacements);
        } catch (IOException e) {
            XposedBridge.log("[SettingsFirewall] Error saving rules of uid " + uid);
            XposedBridge.log(e);
        }
    }
}
