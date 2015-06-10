package se.amphisys.lockscreendisabletimer.app;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
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

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("BindingAnnotationWithoutInject")
@ContentView(R.layout.activity_main)
public class MainActivity extends RoboActionBarActivity {

    @InjectView(R.id.numberPicker) private NumberPicker numberPicker;
    @InjectView(R.id.disableButton) private Button disableButton;
    @InjectView(R.id.statusText) private TextView statusText;
    @InjectView(R.id.timerText) private TextView timerText;
    @InjectView(R.id.errorText) private TextView errorText;
    @Inject private SharedPreferences sharedPreferences;
    @Inject private IntentHelper intentHelper;

    private CountDownTimer countDownTimer;

    private final RoboBroadcastReceiver notificationButtonReceiver = new RoboBroadcastReceiver() {
        @Override
        protected void handleReceive(Context context, Intent intent) {
            super.handleReceive(context, intent);
            if(intent.getAction().equals(context.getString(R.string.REENABLE_KEYGUARD_ACTION))) {
                cancelCountdownTimer();
                setupGui(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver(notificationButtonReceiver, new IntentFilter(getString(R.string.REENABLE_KEYGUARD_ACTION)));

        String[] items = getResources().getStringArray(R.array.timeintervals);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(items.length-1);
        numberPicker.setDisplayedValues(items);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        numberPicker.setValue(sharedPreferences.getInt("numberPicker", 0));

        setupGui(KeyguardService.getInstance() != null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isDeviceSecured()) {
            errorText.setText("This app only works when screen lock is set to Swipe or Pattern. Change it and restart the app.");
            disableButton.setEnabled(false);
        } else {
            errorText.setText("");
            disableButton.setEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(notificationButtonReceiver);
    }

    public void clickDisable(View v)  {
        boolean disabled = KeyguardService.getInstance() != null;
        Intent i = new Intent(this, KeyguardService.class);
        i.putExtra("ms", getSelectedTimeMillis());
        if(!disabled) {
            startService(i);
            startCountdownTimer(getSelectedTimeMillis());
            setupGui(true);
        } else {
            stopService(i);
            cancelCountdownTimer();
            setupGui(false);
        }
    }

    private void setupGui(boolean disabled) {
        if(disabled) {
            disableButton.setText("Reenable screen lock");
            statusText.setText("Screen lock disabled");
            numberPicker.setEnabled(false);
        } else {
            disableButton.setText("Disable screen lock");
            statusText.setText("Screen lock enabled");
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
                timerText.setText(String.format("%s until reenabled", timeLeft));
            }
            public void onFinish() {
                timerText.setText("");
            }
         }.start();
    }

    private void cancelCountdownTimer() {
        if(countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer.onFinish();
        }
    }

    private long getSelectedTimeMillis() {
        sharedPreferences.edit().putInt("numberPicker", numberPicker.getValue()).apply();
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

    private boolean isDeviceSecured()
    {
        String LOCKSCREEN_UTILS = "com.android.internal.widget.LockPatternUtils";
        try {
            Class<?> lockUtilsClass = Class.forName(LOCKSCREEN_UTILS);
            Object lockUtils = lockUtilsClass.getConstructor(Context.class).newInstance(this);
            Method method = lockUtilsClass.getMethod("getActivePasswordQuality");

            int lockProtectionLevel = (Integer)method.invoke(lockUtils); // Thank you esme_louise for the cast hint
            if(lockProtectionLevel >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                return true;
            }
        }
        catch (Exception e) {
            Ln.e("reflectInternalUtils", "ex:" + e);
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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