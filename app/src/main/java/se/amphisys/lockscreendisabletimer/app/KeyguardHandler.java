package se.amphisys.lockscreendisabletimer.app;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import roboguice.util.Ln;

/**
 * Created by richard on 15-05-31.
 */
@Singleton
public class KeyguardHandler {
    @Inject
    private Application application;
    @Inject
    private KeyguardManager keyguardManager;

    private KeyguardManager.KeyguardLock keyguardLock;
    private final Handler handler = new Handler();

    private final Runnable runDisableKeyguard = new Runnable() {
        public void run() {
            keyguardLock = keyguardManager.newKeyguardLock(application.getPackageName());
            keyguardLock.disableKeyguard();
        }
    };

    protected void setEnablednessOfKeyguard(boolean enabled) {
        Ln.d("setEnablednessOfKeyguard: %s", enabled);
        if (enabled) {
            if (keyguardLock != null) {
                application.unregisterReceiver(userPresentReceiver);
                handler.removeCallbacks(runDisableKeyguard);
                keyguardLock.reenableKeyguard();
                keyguardLock = null;
            }
        } else {
            if (keyguardManager.inKeyguardRestrictedInputMode()) {
                application.registerReceiver(userPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
            } else {
                if (keyguardLock != null)
                    keyguardLock.reenableKeyguard();
                else {
                    application.registerReceiver(userPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
                }

                handler.postDelayed(runDisableKeyguard, 300);
            }
        }
    }

    private final BroadcastReceiver userPresentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())){
                setEnablednessOfKeyguard(false);
            }
        }
    };
}
