package com.icehome.up;

import com.icehome.up.compat.DeviceCompat;
import com.icehome.up.compat.HookGuard;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Samsung One UI 9 compatibility utilities, launcher routing, and hook guards.
 */
public class OneUiCompatTest {

    @Test
    public void versionMetadataIsUpdatedForOneUi9() {
        assertEquals("3.0.0-OneUI9", Version.NAME);
        assertEquals(69, Version.CODE);
    }

    @Test
    public void launcherRoutingRecognizesOneUiAndHyperOs() {
        assertTrue(Launchers.is(Launchers.ONEUI));
        assertTrue(Launchers.is(Launchers.HYPEROS));
        assertTrue(Launchers.is(Launchers.POCO));
        assertFalse(Launchers.is("com.unknown.launcher"));

        assertTrue(Launchers.isSamsung(Launchers.ONEUI));
        assertFalse(Launchers.isSamsung(Launchers.HYPEROS));
        assertFalse(Launchers.kashiFeatures(Launchers.ONEUI));
    }

    @Test
    public void hookGuardDeduplicatesClaims() {
        String key = "test_claim_" + System.nanoTime();
        assertTrue(HookGuard.claim(key));
        assertFalse(HookGuard.claim(key));
    }

    @Test
    public void hookGuardFindFirstClassHandlesMissingGracefully() {
        Class<?> found = HookGuard.findFirstClass(
                OneUiCompatTest.class.getClassLoader(),
                "com.nonexistent.FakeClass",
                "java.lang.String"
        );
        assertEquals(String.class, found);
        assertNull(HookGuard.findFirstClass(OneUiCompatTest.class.getClassLoader(), "com.nonexistent.OnlyFake"));
    }

    @Test
    public void deviceCompatHandlesNonAndroidHostGracefully() {
        // On non-Android JVM (CI / unit test), SystemProperties is absent and must not throw.
        assertNull(DeviceCompat.prop("ro.build.version.oneui"));
        assertEquals(42, DeviceCompat.propInt("ro.build.version.oneui", 42));
    }
}
