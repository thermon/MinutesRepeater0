package com.example.therm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        minutesRepeat repeater = new minutesRepeat(context);
        Handler mHandler = new Handler(context.getMainLooper());
        repeater.setHandler(mHandler);

        Thread mThread = new Thread(repeater);
        mThread.start();
        //repeater.run();
/*
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.d("AlarmReceiver", "Alarm Received! : " + intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, 0));
        // Bundle zone= intent.getBundleExtra("zones");
        Bundle bundle = intent.getExtras();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Log.d("AlarmReciever", "key=" + key + ",value=" + bundle.get(key).toString());
        }
        Calendar[][] zones = (Calendar[][]) (intent.getSerializableExtra("zones"));
        boolean basedOnHour=intent.getBooleanExtra("basedOnHour",false);
        int interval=intent.getIntExtra("interval",1);
        Calendar time = (Calendar) (intent.getSerializableExtra("triggerTime"));
        Calendar next=repeater.getNextAlarmTime(zones,interval,basedOnHour,time);

        minutesRepeat.nextAlarmTime=next;
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        for (int i = 0; i < zones.length; i++) {
            if (zones[i] == null) continue;

            Log.d("AlarmReceiver", "zone["+i+"][0]="+sdf.format(zones[i][0].getTime()));
            Log.d("AlarmReceiver", "zone["+i+"][1]="+sdf.format(zones[i][1].getTime()));
        }
        Log.d("AlarmReceiver", "time="+sdf.format(time.getTime()));
//        getClass().getSimpleName();
                //        Intent intent1 = new Intent(context, MainActivity.nextTime.class);
//        context.startActivity(intent1);
*/
        // スクリーンオン
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "My Tag");
        wl.acquire(20000);
    }
}
