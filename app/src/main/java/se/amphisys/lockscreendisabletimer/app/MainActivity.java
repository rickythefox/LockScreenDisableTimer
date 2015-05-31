package se.amphisys.lockscreendisabletimer.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import com.google.inject.Inject;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import roboguice.receiver.RoboBroadcastReceiver;
import roboguice.util.Ln;

import java.util.concurrent.TimeUnit;

@ContentView(R.layout.activity_main)
public class MainActivity extends RoboActionBarActivity {

    @InjectView(R.id.numberPicker)
    private NumberPicker numberPicker;
    @InjectView(R.id.disableButton)
    private Button disableButton;
    @InjectView(R.id.statusText)
    private TextView statusText;
    @InjectView(R.id.timerText)
    private TextView timerText;
    @Inject
    private AlarmManager alarmManager;
    @Inject
    private NotificationManager notificationManager;
    @Inject
    private KeyguardHandler keyguardHandler;
    private CountDownTimer countDownTimer;
    private boolean lockscreenDisabled = false;

    private final RoboBroadcastReceiver alarmReceiver = new RoboBroadcastReceiver() {
        @Override
        protected void handleReceive(Context context, Intent intent) {
            super.handleReceive(context, intent);
            if(intent.getAction().equals(context.getString(R.string.REENABLE_KEYGUARD_ACTION))) {
                setLockscreenDisabled(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] items = getResources().getStringArray(R.array.timeintervals);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(items.length-1);
        numberPicker.setDisplayedValues(items);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
    }

    public void clickDisable(View v) {
        if(!lockscreenDisabled) {
            setLockscreenDisabled(true);
        } else {
            setLockscreenDisabled(false);
        }
    }

    private void setLockscreenDisabled(boolean disabled) {
        lockscreenDisabled = disabled;
        setupGui();

        if(lockscreenDisabled) {
            registerReceiver(alarmReceiver, new IntentFilter(getString(R.string.REENABLE_KEYGUARD_ACTION)));
            long ms = getSelectedTimeMillis();
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ms, getEnableLockscreenPendingIntent());

            displayNotification();
            startCountdownTimer(ms);
        } else {
            unregisterReceiver(alarmReceiver);
            alarmManager.cancel(getEnableLockscreenPendingIntent());

            keyguardHandler.setEnablednessOfKeyguard(true);
            cancelCountdownTimer();
            hideNotification();
        }
    }

    private void setupGui() {
        if(lockscreenDisabled) {
            disableButton.setText("Reenable lockscreen");
            statusText.setText("Lockscreen disabled");
            numberPicker.setEnabled(false);
        } else {
            disableButton.setText("Disable lockscreen");
            statusText.setText("Lockscreen enabled");
            numberPicker.setEnabled(true);
        }
    }

    private void startCountdownTimer(final long ms) {
        countDownTimer = new CountDownTimer(ms, 1000) {
            public void onTick(long millisUntilFinished) {
                String timeLeft = String.format("%dh %dm %ds",
                    TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                    TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                    TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))
                );
                timerText.setText(String.format("%s until reenabling", timeLeft));
            }
            public void onFinish() {
                timerText.setText("Lockscreen enabled");
            }
         }.start();
    }

    private void cancelCountdownTimer() {
        if(countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer.onFinish();
        }
    }

    private void displayNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.lock_timer)
                .setContentTitle(getTitle())
                .setContentText("Lockscreen disabled")
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                .addAction(R.drawable.ic_lock_lock_alpha, "Reenable!", getEnableLockscreenPendingIntent())
                .setOngoing(true)
                .setWhen(0)
                .setAutoCancel(false);
        notificationManager.notify(0, builder.build());
    }

    private void hideNotification() {
        notificationManager.cancel(0);
    }

    private PendingIntent getEnableLockscreenPendingIntent() {
        Intent intent = new Intent(getString(R.string.REENABLE_KEYGUARD_ACTION));
        return PendingIntent.getBroadcast(this, 1, intent, 0);
    }

    private long getSelectedTimeMillis() {
        long ms = 0;
        switch (numberPicker.getValue()) {
            case 0: // 5m
                ms = 5*60*1000;
                break;
            case 1: // 10m
                ms = 10*60*1000;
                break;
            case 2: // 30m
                ms = 30*60*1000;
                break;
            case 3: // 1h
                //noinspection PointlessArithmeticExpression
                ms = 1*60*60*1000;
                break;
            case 4: // 2h
                ms = 2*60*60*1000;
                break;
            case 5: // 4h
                ms = 4*60*60*1000;
                break;
            case 6: // 8h
                ms = 8*60*60*1000;
                break;
            case 7: // 24h
                ms = 24*60*60*1000;
                break;
        }
        return ms;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            String versionName;
            try {
                versionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = "v?";
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(String.format(getString(R.string.about_text), getString(R.string.app_name), versionName));
            builder.setPositiveButton(getString(R.string.got_it), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
