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

public class KeyguardService extends RoboService {
    @Inject private AlarmManager alarmManager;
    @Inject private NotificationManager notificationManager;
    @Inject private KeyguardHandler keyguardHandler;
    @Inject private IntentHelper intentHelper;

    private static KeyguardService instance;
    private long ms;

    private final RoboBroadcastReceiver alarmReceiver = new RoboBroadcastReceiver() {
        @Override
        protected void handleReceive(Context context, Intent intent) {
            super.handleReceive(context, intent);
            if(intent.getAction().equals(context.getString(R.string.REENABLE_KEYGUARD_ACTION))) {
                stopSelf();
            }
        }
    };

    public static KeyguardService getInstance() {
        return instance;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        instance = this;
        ms = intent.getLongExtra("ms", -1);
        disableLockscreen();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        reenableLockscreen();
        instance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void reenableLockscreen() {
        alarmManager.cancel(intentHelper.getEnableLockscreenPendingIntent());
        keyguardHandler.setEnablednessOfKeyguard(true);
        unregisterReceiver(alarmReceiver);
        stopForeground(true);
    }

    private void disableLockscreen() {
        startForeground(1337, createNotification());
        registerReceiver(alarmReceiver, new IntentFilter(getString(R.string.REENABLE_KEYGUARD_ACTION)));
        keyguardHandler.setEnablednessOfKeyguard(false);
        if(ms > -1) {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ms, intentHelper.getEnableLockscreenPendingIntent());
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.lock_timer)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Screen lock disabled")
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .addAction(R.drawable.ic_lock_lock_alpha, "Reenable!", intentHelper.getEnableLockscreenPendingIntent())
                .setOngoing(true)
                .setWhen(0)
                .setAutoCancel(false);
        return builder.build();
    }
}
