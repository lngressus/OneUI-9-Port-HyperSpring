package com.icehome.up.compat;

import com.icehome.up.xp.XC_MethodHook;
import com.icehome.up.xp.XposedBridge;
import com.icehome.up.xp.XposedHelpers;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe hook registration and reflection helpers with deduplication and diagnostic logging.
 * Prevents missing OEM classes or methods from crashing critical processes like SystemUI.
 */
public final class HookGuard {

    private static final Set<String> HOOKED_KEYS = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Finds the first existing class from a list of candidate names.
     */
    public static Class<?> findFirstClass(ClassLoader cl, String... candidates) {
        if (cl == null || candidates == null) return null;
        for (String name : candidates) {
            if (name == null || name.isEmpty()) continue;
            Class<?> c = XposedHelpers.findClassIfExists(name, cl);
            if (c != null) return c;
        }
        return null;
    }

    /**
     * Marks a hook key as registered. Returns false if already registered.
     */
    public static boolean claim(String key) {
        return HOOKED_KEYS.add(key);
    }

    /**
     * Safely hooks all methods with the given name on targetClass, claiming key to prevent duplicates.
     * Returns the number of hooked methods, or 0 if targetClass/method is unavailable.
     */
    public static int hookAllSafe(String key, Class<?> targetClass, String methodName, XC_MethodHook callback) {
        if (targetClass == null || methodName == null || callback == null) return 0;
        String fullKey = key + ":" + targetClass.getName() + "#" + methodName;
        if (!claim(fullKey)) return 0;
        try {
            return XposedBridge.hookAllMethods(targetClass, methodName, callback).size();
        } catch (Throwable t) {
            XposedBridge.log("HyperSpring [HookGuard]: failed hooking " + fullKey + " -> " + t);
            return 0;
        }
    }

    /**
     * Safely hooks all constructors on targetClass, claiming key to prevent duplicates.
     */
    public static int hookConstructorsSafe(String key, Class<?> targetClass, XC_MethodHook callback) {
        if (targetClass == null || callback == null) return 0;
        String fullKey = key + ":" + targetClass.getName() + "#<init>";
        if (!claim(fullKey)) return 0;
        try {
            return XposedBridge.hookAllConstructors(targetClass, callback).size();
        } catch (Throwable t) {
            XposedBridge.log("HyperSpring [HookGuard]: failed hooking ctors " + fullKey + " -> " + t);
            return 0;
        }
    }

    private HookGuard() {
    }
}
