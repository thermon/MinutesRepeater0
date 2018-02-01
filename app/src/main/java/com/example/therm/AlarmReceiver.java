package com.example.therm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Set;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        minutesRepeat repeater=new minutesRepeat(context);
        repeater.start();

        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Log.d("AlarmReceiver", "Alarm Received! : " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));
        // Bundle zone= intent.getBundleExtra("zones");
        Bundle bundle=intent.getExtras();
        Set<String> keys=bundle.keySet();
        for (String key : keys) {
            Log.d("AlarmReciever", "key="+key+",value="+bundle.get(key).toString());
        }
        Calendar[][] zones = (Calendar[][])  (intent.getSerializableExtra("zones"));
        Log.d("AlarmReceiver:zones", String.valueOf(zones[1][1].getTimeInMillis()));
//        getClass().getSimpleName();
                //        Intent intent1 = new Intent(context, MainActivity.nextTime.class);
//        context.startActivity(intent1);

        // スクリーンオン
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire(20000);
    }
}
