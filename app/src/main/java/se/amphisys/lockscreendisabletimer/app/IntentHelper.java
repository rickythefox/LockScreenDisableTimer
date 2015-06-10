package se.amphisys.lockscreendisabletimer.app;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import com.google.inject.Inject;

public class IntentHelper {
    @Inject
    private Resources resources;
    @Inject
    private Application context;

    public PendingIntent getEnableLockscreenPendingIntent() {
        Intent intent = new Intent(resources.getString(R.string.REENABLE_KEYGUARD_ACTION));
        return PendingIntent.getBroadcast(context, 1, intent, 0);
    }
}
