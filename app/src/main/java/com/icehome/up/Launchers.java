package com.icehome.up;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * Supported launchers:
 * - Samsung One UI Home (com.sec.android.app.launcher)
 * - Xiaomi HyperOS Launcher (com.miui.home)
 * - POCO Launcher (com.mi.android.globallauncher)
 */
public final class Launchers {

    public static final String ONEUI = "com.sec.android.app.launcher";
    public static final String HYPEROS = "com.miui.home";
    public static final String POCO = "com.mi.android.globallauncher";
    public static final String[] ALL = {ONEUI, HYPEROS, POCO};

    public static boolean is(String pkg) {
        return ONEUI.equals(pkg) || HYPEROS.equals(pkg) || POCO.equals(pkg);
    }

    public static boolean isSamsung(String pkg) {
        return ONEUI.equals(pkg);
    }

    static String name(String pkg) {
        if (ONEUI.equals(pkg)) return "One UI Home";
        return POCO.equals(pkg) ? "POCO Launcher" : "HyperOS Launcher";
    }

    static boolean kashiFeatures(String pkg) {
        return HYPEROS.equals(pkg);
    }

    static String current(Context c) {
        PackageManager pm = c.getPackageManager();
        try {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo r = pm.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY);
            String pkg = r != null && r.activityInfo != null ? r.activityInfo.packageName : null;
            if (is(pkg)) return pkg;
        } catch (Throwable ignored) {
        }
        for (String p : ALL) {
            try {
                pm.getPackageInfo(p, 0);
                return p;
            } catch (Throwable ignored) {
            }
        }
        return com.icehome.up.compat.DeviceCompat.isSamsungOneUi() ? ONEUI : HYPEROS;
    }

    private Launchers() {
    }
}
