package com.czsmart.rotate;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/** 读写屏幕方向，并记住用户选择以便开机恢复。 */
final class Rotation {
    private static final String PREFS = "rotate";
    private static final String KEY_ROTATION = "rotation";
    private static final String KEY_BOOT = "apply_on_boot";

    private Rotation() {}

    static boolean canWrite(Context c) {
        return Settings.System.canWrite(c);
    }

    /** @param rotation 0=0°, 1=90°, 2=180°, 3=270° */
    static boolean apply(Context c, int rotation) {
        if (!canWrite(c)) return false;
        try {
            Settings.System.putInt(c.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(c.getContentResolver(), Settings.System.USER_ROTATION, rotation);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    static int current(Context c) {
        return Settings.System.getInt(c.getContentResolver(), Settings.System.USER_ROTATION, 0);
    }

    private static SharedPreferences prefs(Context c) {
        return c.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void save(Context c, int rotation) {
        prefs(c).edit().putInt(KEY_ROTATION, rotation).apply();
    }

    static int saved(Context c) {
        return prefs(c).getInt(KEY_ROTATION, -1);
    }

    static boolean applyOnBoot(Context c) {
        return prefs(c).getBoolean(KEY_BOOT, true);
    }

    static void setApplyOnBoot(Context c, boolean on) {
        prefs(c).edit().putBoolean(KEY_BOOT, on).apply();
    }
}
