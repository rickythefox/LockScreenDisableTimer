package se.amphisys.lockscreendisabletimer.app;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import com.google.inject.Inject;
import roboguice.receiver.RoboBroadcastReceiver;
import roboguice.util.Ln;

/**
 * Created by richard on 15-05-29.
 */
public class AlarmReceiver extends RoboBroadcastReceiver {

    @Inject
    private KeyguardHandler keyguardHandler;

    @Override
    protected void handleReceive(Context context, Intent intent) {
        super.handleReceive(context, intent);
        if(intent.getAction().equals(context.getString(R.string.REENABLE_KEYGUARD_ACTION))) {
            keyguardHandler.setEnablednessOfKeyguard(true);
        }
    }
}
