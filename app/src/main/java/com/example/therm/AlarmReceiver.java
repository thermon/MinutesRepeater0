package com.example.therm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import java.util.concurrent.TimeUnit;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        minutesRepeat repeater=new minutesRepeat(context);
        repeater.start();

        Log.d("AlarmReceiver", "Alarm Received! : " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));
//        Intent intent1 = new Intent(context, MainActivity.nextTime.class);
//        context.startActivity(intent1);

        // スクリーンオン
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire(20000);
    }
}
