package com.czsmart.rotate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/** 开机后恢复上次选择的方向。系统开机时可能会再重置一次，所以延迟后再补一次。 */
public class BootReceiver extends BroadcastReceiver {
    private static final long RETRY_DELAY_MS = 8000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final Context app = context.getApplicationContext();
        if (!Rotation.applyOnBoot(app)) return;
        final int rotation = Rotation.saved(app);
        if (rotation < 0) return;

        Rotation.apply(app, rotation);

        final PendingResult pending = goAsync();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Rotation.apply(app, rotation);
            pending.finish();
        }, RETRY_DELAY_MS);
    }
}
