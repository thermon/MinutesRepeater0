package com.example.therm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class AlarmService extends Service {
    // 繰り返し間隔、1分
    private long repeatPeriod = 1000*60;
    // setWindow()でのwindow幅、4秒
    private long windowLengthMillis = 1000*4;

    private Context context;
    private int [][] zones;
    private boolean [] zonesEnable;
    private Calendar time=null;
    private boolean basedOnHour;
    private int interval;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AlarmService", "start");

        context = getApplicationContext();
    }

    // Alarm によって呼び出される
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        Boolean stopFlag = intent.getBooleanExtra("StopAlarm", false);
        Calendar t = (Calendar) (intent.getSerializableExtra("triggerTime"));

        Bundle bundle = intent.getExtras();
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Log.d("AlarmService", "key=" + key + ",value=" + bundle.get(key).toString());
        }

        // Local Broadcast で発信する
        Intent messageIntent = new Intent("AlarmEvent");

        // アラーム時刻が設定されていれば継続
        if(t!=null){
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            boolean timeChanged=false;

            if (time==null) {
                timeChanged = true;            // timeが未設定だったら時刻変更フラグを立てる
            } else {
                // timeとtの値が違っていたら時刻変更フラグを立てる

                timeChanged = (t.getTimeInMillis() != time.getTimeInMillis()) ? true : false;
            }
            time=t;
            Log.d("AlarmService", "time="+sdf.format(time.getTime()));

            zones = (int [][]) (intent.getSerializableExtra("zones"));
            zonesEnable=intent.getBooleanArrayExtra("zonesEnable");
            basedOnHour = intent.getBooleanExtra("basedOnHour",false);
            interval = intent.getIntExtra("interval",1);

            for (int i = 0; i < zones.length; i++) {
                if (!zonesEnable[i]) continue;

                Log.d("AlarmService", "zone[" + i + "][0]=" + String.format("%1$02d:%2$02d",(zones[i][0]-zones[i][0]%60)/60 ,zones[i][0] %60));
                Log.d("AlarmService", "zone[" + i + "][1]=" + String.format("%1$02d:%2$02d",(zones[i][1]-zones[i][1]%60)/60 ,zones[i][1] %60));
            }

            if ( System.currentTimeMillis()  >= time.getTimeInMillis()) {
                minutesRepeat repeater = new minutesRepeat(context);
                Calendar next = repeater.getNextAlarmTime(zonesEnable,zones, interval, basedOnHour, time);
                time = next;
                timeChanged = true;
            }
            // 次回アラーム時刻を取り出す
            messageIntent.putExtra("Message", sdf.format(time.getTime()));
            messageIntent.putExtra("time",time.getTimeInMillis());
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            setNextAlarmService();
        }
        else{
            // 次回アラーム時刻を取り出す
            messageIntent.putExtra("Message","undefined");
            messageIntent.putExtra("time",(long)-1);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            stopAlarmService();
        }


        return super.onStartCommand(intent, flags, startId);
    }

    // 次のアラームの設定
    private void setNextAlarmService(){

        Intent intent = new Intent(getApplicationContext(), AlarmService.class);
        Intent intent_ring = new Intent(getApplicationContext(),AlarmReceiver.class);
/*
        if ( System.currentTimeMillis()  >= next.getTimeInMillis()) {
            next.add(Calendar.MINUTE,interval);
        }
*/
        intent.putExtra("interval", interval);
        intent.putExtra("zones", zones);
        intent.putExtra("zonesEnable", zonesEnable);
        intent.putExtra("triggerTime", time);
        intent.putExtra("basedOnHour", basedOnHour);
/*
        intent_ring.putExtra("interval", interval);
        intent_ring.putExtra("zones", zones);
        intent_ring.putExtra("triggerTime", time);
        intent_ring.putExtra("basedOnHour", basedOnHour);
*/
        long startMillis = System.currentTimeMillis() ;

        PendingIntent pendingIntent
                = PendingIntent.getService(context, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingIntent_ring
                = PendingIntent.getBroadcast(context, 1, intent_ring,PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager
                = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(alarmManager != null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            Log.d("AlarmService", "next="+sdf.format(time.getTime()));
            // SDK 19 以下ではsetを使う
            if(android.os.Build.VERSION.SDK_INT < 19) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, startMillis, pendingIntent);
                 alarmManager.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent_ring);
            }
            else{
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
                        startMillis, windowLengthMillis, pendingIntent);
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
                        time.getTimeInMillis(), windowLengthMillis, pendingIntent_ring);
            }
        }
    }

    private void stopAlarmService(){
        Intent indent = new Intent(context, AlarmService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, indent, 0);

        // アラームを解除する
        AlarmManager alarmManager
                = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarmManager != null){
            alarmManager.cancel(pendingIntent);
        }
    }
}
