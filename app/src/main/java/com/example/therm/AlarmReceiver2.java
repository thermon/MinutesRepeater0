package com.example.therm;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Set;

public class AlarmReceiver2 extends Service {
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                minutesRepeat repeater = new minutesRepeat(context);
                Handler mHandler = new Handler(context.getMainLooper());
                repeater.setHandler(mHandler);

                Thread mThread = new Thread(repeater);
                //mThread.start();
                repeater.run();
/*
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Log.d("AlarmReceiver2", "Alarm Received! : " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));
                // Bundle zone= intent.getBundleExtra("zones");
                Bundle bundle = intent.getExtras();
                Set<String> keys = bundle.keySet();
                for (String key : keys) {
                    Log.d("AlarmReciever2", "key=" + key + ",value=" + bundle.get(key).toString());
                }
                zoneElement[] zones = (zoneElement[])  (intent.getSerializableExtra("zones"));
                boolean basedOnHour = intent.getBooleanExtra("basedOnHour", false);
                int interval = intent.getIntExtra("interval", 1);
                Calendar time = (Calendar) (intent.getSerializableExtra("triggerTime"));
                Calendar next = repeater.getNextAlarmTime(zones, interval, basedOnHour, time);

                minutesRepeat.nextAlarmTime = next;

                for (int i = 0; i < zones.length; i++) {
                    if (zones[i] == null) continue;

                    Log.d("AlarmReceiver2:zones", String.valueOf(zones[i].zone[1].getTimeInMillis()));
                }
                //        getClass().getSimpleName();
                //        Intent intent1 = new Intent(context, MainActivity.nextTime.class);
                //        context.startActivity(intent1);
*/
                // スクリーンオン
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
                wl.acquire(20000);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("alarm");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
