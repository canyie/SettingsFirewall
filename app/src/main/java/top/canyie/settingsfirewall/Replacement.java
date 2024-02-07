package top.canyie.settingsfirewall;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;

/**
 * @author canyie
 */
public final class Replacement implements Serializable, Parcelable {
    public static final int FLAG_SYSTEM = 1 << 0;
    public static final int FLAG_SECURE = 1 << 1;
    public static final int FLAG_GLOBAL = 1 << 2;
    @Serial private static final long serialVersionUID = 1145141919810L;
    public final String key;
    public String value;
    public int flags;

    public static final Creator<Replacement> CREATOR = new Creator<>() {
        @Override public Replacement createFromParcel(Parcel in) {
            return new Replacement(in.readString(), in.readString(), in.readInt());
        }

        @Override public Replacement[] newArray(int size) {
            return new Replacement[size];
        }
    };

    public static final Comparator<Replacement> COMPARATOR = (a, b) -> {
        boolean aReplaced = a.value != null;
        boolean bReplaced = b.value != null;
        if (aReplaced != bReplaced)
            return aReplaced ? -1 : 1;
        return a.key.compareTo(b.key);
    };

    public Replacement(String name, String value, int flags) {
        this.key = name;
        this.value = value;
        this.flags = flags;
    }

    @Override public int describeContents() {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(value);
        dest.writeInt(this.flags);
    }

    @Override public boolean equals(Object obj) {
        return obj instanceof Replacement that && key.equals(that.key) && flags == that.flags;
    }

    @Override public int hashCode() {
        return key.hashCode() ^ flags;
    }

    @Override public String toString() {
        return "Replacement{key=" + key + ", value=" + value + ", flags=" + flags + "}";
    }
}
