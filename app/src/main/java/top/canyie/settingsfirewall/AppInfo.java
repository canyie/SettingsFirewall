package top.canyie.settingsfirewall;

import android.graphics.drawable.Drawable;
import java.util.Comparator;

/**
 * @author canyie
 */
public class AppInfo {
    public int uid;
    public String name;
    public Drawable icon;
    public boolean enabled;
    public boolean isSharedUid;

    public static final Comparator<AppInfo> COMPARATOR = (a, b) -> {
        if (a.enabled != b.enabled)
            return a.enabled ? -1 : 1;
        if (a.isSharedUid != b.isSharedUid)
            return a.isSharedUid ? -1 : 1;
        return a.name.compareTo(b.name);
    };

    @Override public boolean equals(Object obj) {
        return obj instanceof AppInfo that && uid == that.uid;
    }

    @Override public int hashCode() {
        return uid;
    }
}
