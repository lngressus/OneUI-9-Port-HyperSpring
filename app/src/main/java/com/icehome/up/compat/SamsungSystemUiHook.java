package com.icehome.up.compat;

import android.view.View;

import com.icehome.up.telon.Log;
import com.icehome.up.xp.XC_MethodHook;

/**
 * Samsung One UI 9 SystemUI Quick Panel compatibility probe & safe hook layer.
 *
 * <p>Unlike Xiaomi HyperOS (which uses a separate {@code miui.systemui.plugin} APK and
 * {@code View.setMiBackgroundBlendColors}), Samsung One UI integrates Quick Settings directly
 * inside {@code com.android.systemui}. Because Samsung obfuscates or refactors Quick Panel
 * controller classes across One UI versions, this layer uses narrow, deduplicated probes via
 * {@link HookGuard} and never hooks {@code View.drawBackground} globally on Samsung.
 */
public final class SamsungSystemUiHook {

    private static volatile boolean installed;

    public static synchronized void install(ClassLoader cl) {
        if (installed) return;
        installed = true;

        Class<?> qsPanel = HookGuard.findFirstClass(cl,
                "com.android.systemui.qs.SecQSPanel",
                "com.android.systemui.qs.QSPanel",
                "com.android.systemui.qs.SecQSPanelController");
        Class<?> qsTile = HookGuard.findFirstClass(cl,
                "com.android.systemui.qs.tileimpl.SecQSTileView",
                "com.android.systemui.qs.tileimpl.QSTileViewImpl",
                "com.android.systemui.qs.tileimpl.QSTileBaseView");
        Class<?> blurCtrl = HookGuard.findFirstClass(cl,
                "com.android.systemui.blur.SecQSBlurController",
                "com.android.systemui.statusbar.BlurUtils");

        StringBuilder diag = new StringBuilder("One UI 9 SystemUI probe:");
        diag.append(" qsPanel=").append(qsPanel != null ? qsPanel.getSimpleName() : "none");
        diag.append(", qsTile=").append(qsTile != null ? qsTile.getSimpleName() : "none");
        diag.append(", blur=").append(blurCtrl != null ? blurCtrl.getSimpleName() : "none");
        Log.i(diag.toString());

        if (qsPanel != null) {
            HookGuard.hookAllSafe("oneui_qs_attach", qsPanel, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    if (p.thisObject instanceof View) {
                        Log.state("cc_seen", System.currentTimeMillis() + "|oneui");
                    }
                }
            });
        }
    }

    private SamsungSystemUiHook() {
    }
}
