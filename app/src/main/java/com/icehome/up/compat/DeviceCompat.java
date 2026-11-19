package com.icehome.up.compat;

import android.os.Build;

/**
 * OS and device compatibility detection for HyperSpring (Samsung One UI 9 + HyperOS 3).
 * Uses Android SystemProperties safely without assuming Xiaomi-only properties.
 */
public final class DeviceCompat {

    private static volatile Boolean cachedSamsung;
    private static volatile Boolean cachedXiaomi;

    public static String prop(String key) {
        try {
            Object v = Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class)
                    .invoke(null, key);
            return v instanceof String && !((String) v).isEmpty() ? (String) v : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static int propInt(String key, int def) {
        String s = prop(key);
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean isSamsungOneUi() {
        Boolean c = cachedSamsung;
        if (c != null) return c;
        boolean samsungMfr = "samsung".equalsIgnoreCase(Build.MANUFACTURER)
                || "samsung".equalsIgnoreCase(Build.BRAND);
        int oneUiCode = propInt("ro.build.version.oneui", 0);
        boolean res = samsungMfr || oneUiCode > 0;
        cachedSamsung = res;
        return res;
    }

    public static boolean isXiaomiHyperOs() {
        Boolean c = cachedXiaomi;
        if (c != null) return c;
        boolean res = prop("ro.mi.os.version.name") != null
                || prop("ro.miui.ui.version.name") != null;
        cachedXiaomi = res;
        return res;
    }

    public static boolean isSupportedDevice() {
        return isSamsungOneUi() || isXiaomiHyperOs();
    }

    /**
     * Parses ro.build.version.oneui (e.g. 90000 -> "9.0", 90101 -> "9.1.1", 80500 -> "8.5").
     * Returns null if not present.
     */
    public static String oneUiVersion() {
        int v = propInt("ro.build.version.oneui", 0);
        if (v <= 0) return null;
        int major = v / 10000;
        int minor = (v % 10000) / 100;
        int patch = v % 100;
        if (patch > 0) return major + "." + minor + "." + patch;
        return major + "." + minor;
    }

    public static String systemSummary() {
        String out = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        if (isSamsungOneUi()) {
            String oui = oneUiVersion();
            return oui != null ? out + " · One UI " + oui : out + " · One UI";
        }
        String os = prop("ro.mi.os.version.incremental");
        if (os == null) os = prop("ro.mi.os.version.name");
        if (os != null) return out + " · HyperOS " + (os.startsWith("OS") ? os.substring(2) : os);
        String miui = prop("ro.miui.ui.version.name");
        return miui != null ? out + " · MIUI " + miui.replace("V", "") : out;
    }

    private DeviceCompat() {
    }
}
