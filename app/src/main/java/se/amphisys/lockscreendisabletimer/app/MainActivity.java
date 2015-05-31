package se.amphisys.lockscreendisabletimer.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.Toast;
import com.google.inject.Inject;
import roboguice.activity.RoboActionBarActivity;
import roboguice.inject.ContentView;
import roboguice.inject.InjectView;
import roboguice.util.Ln;

@ContentView(R.layout.activity_main)
public class MainActivity extends RoboActionBarActivity {
    boolean disable;

    @InjectView(R.id.numberPicker)
    private NumberPicker numberPicker;
    @Inject
    private AlarmManager alarmManager;
    @Inject
    private KeyguardHandler keyguardHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] items = getResources().getStringArray(R.array.timeintervals);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(items.length-1);
        numberPicker.setDisplayedValues(items);
    }


    public void clickDisable(View v) {
        long ms = getSelectedTimeMillis();

        keyguardHandler.setEnablednessOfKeyguard(false);

        // TODO: test code
        ms = 5000;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction(getString(R.string.REENABLE_KEYGUARD_ACTION));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ms, pendingIntent);
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
