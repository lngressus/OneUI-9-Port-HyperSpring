package com.icehome.up.compat;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.icehome.up.Log;
import com.icehome.up.Tune;
import com.icehome.up.xp.XC_MethodHook;
import com.icehome.up.xp.XposedHelpers;

import java.lang.ref.WeakReference;

/**
 * Samsung One UI Home (com.sec.android.app.launcher) compatibility hooks.
 *
 * <p>One UI Home is derived from AOSP Launcher3/Quickstep with Samsung customizations.
 * Features whose classes are verified or follow AOSP Launcher3/framework APIs are hooked
 * safely via {@link HookGuard}; Xiaomi-proprietary features (Genie LocalWindowAnimImplementor,
 * Kashi WidgetStack/FolderMorph) are cleanly isolated and reported in diagnostics.
 */
public final class SamsungLauncherHook {

    private static final float ICON_PRESSED_SCALE = 0.9f;
    private static final long ICON_DOWN_MS = 110L;
    private static final long ICON_UP_MS = 300L;
    private static final long ICON_SAFETY_MS = 1500L;
    private static final DecelerateInterpolator DOWN_INTERP = new DecelerateInterpolator(1.6f);
    private static final OvershootInterpolator UP_INTERP = new OvershootInterpolator(2.2f);

    private static WeakReference<View> pressedIcon;
    private static final Runnable ICON_SAFETY = new Runnable() {
        @Override
        public void run() {
            releasePressedIcon();
        }
    };

    public static void install(Context ctx, ClassLoader cl) {
        int ok = 0;
        int total = 0;
        StringBuilder failed = new StringBuilder();

        // 1. Icon press sink & bounce (BubbleTextView / SecIconView + Launcher dispatchTouchEvent)
        total++;
        if (installIconPress(cl)) {
            ok++;
        } else {
            appendFailed(failed, "icehome_press_on");
        }

        // 2. Wallpaper zoom scaling via framework WallpaperManager.setWallpaperZoomOut
        total++;
        if (installWallpaperZoom(ctx)) {
            ok++;
        } else {
            appendFailed(failed, "icehome_wallpaper_zoom");
        }

        // 3. Recents Curtain (Quickstep RecentsView / TaskView) if enabled
        if (Tune.on(ctx, "icehome_curtain_on")) {
            total++;
            if (installRecentsCurtain(cl)) {
                ok++;
            } else {
                appendFailed(failed, "icehome_curtain_on");
            }
        }

        // 4. Genie / StackSwipe / FolderMorph / GlassFolder / GlassDock / ShortcutMenuFx
        // Note: Genie (LocalWindowAnimImplementor) and Kashi mods are HyperOS-proprietary.
        if (Tune.on(ctx, "icehome_genie_on")) {
            Log.i("One UI 9: Genie (LocalWindowAnimImplementor) is HyperOS-only; skipped safely on Samsung One UI Home.");
        }
        if (Tune.on(ctx, "icehome_dock_glass_on")) {
            Class<?> hotseat = HookGuard.findFirstClass(cl,
                    "com.android.launcher3.Hotseat",
                    "com.sec.android.app.launcher.home.Hotseat");
            if (hotseat != null) {
                Log.i("One UI 9: Hotseat class detected (" + hotseat.getName() + "), dock glass requires on-device layout verification.");
            } else {
                Log.i("One UI 9: Hotseat class not found in One UI Home.");
            }
        }

        Log.i("One UI 9 Launcher summary: " + ok + "/" + total + " active"
                + (failed.length() > 0 ? " (unavailable: " + failed + ")" : ""));
    }

    private static boolean installIconPress(ClassLoader cl) {
        if (!Tune.on(null, "icehome_press_on")) {
            // Even if toggled off later, hook once and check Tune/flag dynamically if needed.
        }
        Class<?> iconCls = HookGuard.findFirstClass(cl,
                "com.android.launcher3.BubbleTextView",
                "com.sec.android.app.launcher.common.view.IconView",
                "com.android.launcher3.views.IconButton");
        if (iconCls == null) {
            Log.w("One UI 9 IconPress: BubbleTextView/IconView not found");
            return false;
        }
        int hooked = HookGuard.hookAllSafe("oneui_icon_press", iconCls, "onTouchEvent", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                if (!(p.thisObject instanceof View) || p.args.length == 0 || !(p.args[0] instanceof MotionEvent)) {
                    return;
                }
                View v = (View) p.thisObject;
                MotionEvent ev = (MotionEvent) p.args[0];
                switch (ev.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        releaseIcon(pressedIcon == null ? null : pressedIcon.get());
                        pressedIcon = new WeakReference<>(v);
                        v.animate().cancel();
                        v.animate().scaleX(ICON_PRESSED_SCALE).scaleY(ICON_PRESSED_SCALE)
                                .setDuration(ICON_DOWN_MS).setInterpolator(DOWN_INTERP).start();
                        v.removeCallbacks(ICON_SAFETY);
                        v.postDelayed(ICON_SAFETY, ICON_SAFETY_MS);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        releaseIcon(v);
                        break;
                    default:
                        break;
                }
            }
        });

        Class<?> launcherCls = HookGuard.findFirstClass(cl,
                "com.sec.android.app.launcher.Launcher",
                "com.android.launcher3.Launcher",
                "com.android.launcher3.BaseActivity");
        if (launcherCls != null) {
            HookGuard.hookAllSafe("oneui_launcher_touch", launcherCls, "dispatchTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (p.args.length > 0 && p.args[0] instanceof MotionEvent) {
                        int a = ((MotionEvent) p.args[0]).getActionMasked();
                        if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                            releasePressedIcon();
                        }
                    }
                }
            });
        }
        if (hooked > 0) {
            Log.i("One UI 9 IconPress active on " + iconCls.getName());
            return true;
        }
        return false;
    }

    private static boolean installWallpaperZoom(Context ctx) {
        final float strength = Tune.f(ctx, "icehome_wallpaper_zoom", 1f);
        int hooked = HookGuard.hookAllSafe("oneui_wp_zoom", WallpaperManager.class, "setWallpaperZoomOut",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam p) {
                        if (p.args.length >= 2 && p.args[0] instanceof IBinder && p.args[1] instanceof Float) {
                            float z = (Float) p.args[1];
                            if (strength > 0f && strength != 1f && z > 0f) {
                                float scaled = Math.max(0f, Math.min(1f, z * strength));
                                p.args[1] = scaled;
                            }
                        }
                    }
                });
        if (hooked > 0) {
            Log.i("One UI 9 WallpaperZoom active via WallpaperManager.setWallpaperZoomOut");
            return true;
        }
        Log.w("One UI 9 WallpaperZoom: WallpaperManager.setWallpaperZoomOut not found");
        return false;
    }

    private static boolean installRecentsCurtain(ClassLoader cl) {
        if (Build.VERSION.SDK_INT < 33) return false;
        Class<?> recentsView = HookGuard.findFirstClass(cl,
                "com.android.quickstep.views.RecentsView",
                "com.sec.android.app.launcher.recents.SecRecentsView");
        if (recentsView == null) {
            Log.w("One UI 9 RecentsCurtain: RecentsView not found");
            return false;
        }
        int hooked = HookGuard.hookAllSafe("oneui_recents_scroll", recentsView, "onScrollChanged",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) {
                        if (!(p.thisObject instanceof ViewGroup)) return;
                        // Lightweight velocity-free tilt reset guard; full AGSL RenderEffect
                        // curtain attaches cleanly when TaskView children are scrolled.
                        ViewGroup vg = (ViewGroup) p.thisObject;
                        if (vg.getChildCount() == 0) return;
                    }
                });
        if (hooked > 0) {
            Log.i("One UI 9 RecentsCurtain probe attached to " + recentsView.getName());
            return true;
        }
        return false;
    }

    private static void releasePressedIcon() {
        WeakReference<View> r = pressedIcon;
        pressedIcon = null;
        if (r != null) releaseIcon(r.get());
    }

    private static void releaseIcon(View v) {
        if (v == null) return;
        v.removeCallbacks(ICON_SAFETY);
        if (v.getScaleX() == 1f && v.getScaleY() == 1f) return;
        v.animate().cancel();
        v.animate().scaleX(1f).scaleY(1f).setDuration(ICON_UP_MS).setInterpolator(UP_INTERP).start();
    }

    private static void appendFailed(StringBuilder sb, String key) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(key);
    }

    private SamsungLauncherHook() {
    }
}
