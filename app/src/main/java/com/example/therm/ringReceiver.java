package com.example.therm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ringReceiver extends BroadcastReceiver {
    // minutesRepeat repeater = null;
    public String className;

    @Override
    public void onReceive(Context context, Intent intent) {
        className = myApplication.getClassName();
        Log.d(className, "called");
        new ringAlarm(context);

        //if (repeater == null) repeater = new minutesRepeat(context);
        // repeater.ring();
        // repeater.releaseSound();
        // repeater=null;

        /*
        // スクリーンオン
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire(20000);
        */
    }
}
