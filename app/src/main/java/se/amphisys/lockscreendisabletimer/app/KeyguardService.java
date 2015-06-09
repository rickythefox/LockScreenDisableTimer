package se.amphisys.lockscreendisabletimer.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import com.google.inject.Inject;
import roboguice.receiver.RoboBroadcastReceiver;
import roboguice.service.RoboService;

/**
 * Created by richard on 15-06-09.
 */
public class KeyguardService extends RoboService {
    private static KeyguardService instance;
    @Inject
    private AlarmManager alarmManager;
    @Inject
    private NotificationManager notificationManager;
    @Inject
    private KeyguardHandler keyguardHandler;

    private final RoboBroadcastReceiver alarmReceiver = new RoboBroadcastReceiver() {
        @Override
        protected void handleReceive(Context context, Intent intent) {
            super.handleReceive(context, intent);
            if(intent.getAction().equals(context.getString(R.string.REENABLE_KEYGUARD_ACTION))) {
                setLockscreenDisabled(false);
            }
        }
    };

    public static KeyguardService getInstance() {
        return instance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        instance = this;
         // TODO hämta ms
        startForeground(0, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setLockscreenDisabled(boolean disabled) {
        keyguardHandler.setEnablednessOfKeyguard(!disabled);

        if(disabled) {
            registerReceiver(alarmReceiver, new IntentFilter(getString(R.string.REENABLE_KEYGUARD_ACTION)));
            long ms = getSelectedTimeMillis();   // TODO till ui
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ms, getEnableLockscreenPendingIntent());
            displayNotification();
            startCountdownTimer(ms);  // TODO till ui
        } else {
            unregisterReceiver(alarmReceiver);
            alarmManager.cancel(getEnableLockscreenPendingIntent());

            cancelCountdownTimer();  // TODO till ui
            hideNotification();
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.lock_timer)
                .setContentText("Screen lock disabled")
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .addAction(R.drawable.ic_lock_lock_alpha, "Reenable!", getEnableLockscreenPendingIntent())
                .setOngoing(true)
                .setWhen(0)
                .setAutoCancel(false);
        return builder.build();
    }

    private PendingIntent getEnableLockscreenPendingIntent() {
        Intent intent = new Intent(getString(R.string.REENABLE_KEYGUARD_ACTION));
        return PendingIntent.getBroadcast(this, 1, intent, 0);
    }
}
