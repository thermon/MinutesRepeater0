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

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
//        Boolean stopFlag = intent.getBooleanExtra("StopAlarm", false);

        // intentから情報を取り出す
        Calendar t = (Calendar) (intent.getSerializableExtra("triggerTime"));
        zones = (int [][]) (intent.getSerializableExtra("zones"));
        zonesEnable=intent.getBooleanArrayExtra("zonesEnable");
        basedOnHour = intent.getBooleanExtra("basedOnHour",false);
        interval = intent.getIntExtra("interval",1);


        if (zones!= null) {
            for (int i = 0; i < zones.length; i++) {
                if (zonesEnable != null && zonesEnable[i]) {
                    Log.d("AlarmService", "zone[" + i + "][0]=" + String.format("%1$02d:%2$02d", (zones[i][0] - zones[i][0] % 60) / 60, zones[i][0] % 60));
                    Log.d("AlarmService", "zone[" + i + "][1]=" + String.format("%1$02d:%2$02d", (zones[i][1] - zones[i][1] % 60) / 60, zones[i][1] % 60));
                }
            }
        }

        // アラーム時刻が設定されていれば継続
        if(t!=null){
            Bundle bundle = intent.getExtras();
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                Log.d("AlarmService", "key=" + key + ",value=" + bundle.get(key).toString());
            }

            boolean alarmTimeIsChanged=false;
            // 現在時刻がtimeを過ぎていた場合、次のアラーム時刻をtimeにセットする
            if ( t!=null) {
                long now=System.currentTimeMillis();
                now+= windowLengthMillis* 2;  // 8秒進める

                if (now >= t.getTimeInMillis()) {
                    minutesRepeat repeater = new minutesRepeat(context);
                    Calendar next = repeater.getNextAlarmTime(zonesEnable, zones, interval, basedOnHour, t);
                    time = next;

                    // アラーム時刻が更新されたらtrue
                    alarmTimeIsChanged=true;
                } else {
                    if (time == null) {
                        alarmTimeIsChanged=true;    // timeにCalendarインスタンスが未設定だったらtrue
                    } else if (t.getTimeInMillis() != time.getTimeInMillis()) {
                        alarmTimeIsChanged=true;    // timeとtの内容が違っていたらtrue
                    }
                    time=t;
                }
            }

            setNextAlarmService(alarmTimeIsChanged);
        }  else{
            stopAlarmService();
        }

        // Local Broadcast で発信する
        // 次回アラーム時刻をAlarmEventアクションを受け取れるレシーバーに送る
        Intent messageIntent = new Intent("AlarmTimeChanged");
        messageIntent.putExtra("Message", (time!=null) ? sdf.format(time.getTime()): "undefined");
        messageIntent.putExtra("time",(time!=null) ? time.getTimeInMillis(): -1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    // 次のアラームの設定
    private void setNextAlarmService(boolean b){

        // intentはAlarmServiceクラス（つまり自分自身）に動作を遷移するよう設定する
        Intent intent = new Intent(getApplicationContext(), AlarmService.class);
        intent.putExtra("interval", interval);
        intent.putExtra("zones", zones);
        intent.putExtra("zonesEnable", zonesEnable);
        intent.putExtra("triggerTime", time);
        intent.putExtra("basedOnHour", basedOnHour);

        // repeatPeriod間隔でAlarmServiceを起動する

        long startMillis = System.currentTimeMillis();
        startMillis+=repeatPeriod - startMillis % repeatPeriod;

        PendingIntent pendingIntent
                = PendingIntent.getService(context, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager
                = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(alarmManager != null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            Log.d("AlarmService", "next AlarmService trigger time="+sdf.format(startMillis));

            // SDK 19 以下ではsetを使う
            if(android.os.Build.VERSION.SDK_INT < 19) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, startMillis, pendingIntent);
            } else{
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
                        startMillis, windowLengthMillis, pendingIntent);
            }

            // 現在時刻がtimeを過ぎていた場合、次のアラーム時刻をtimeにセットする
            if (b) {
                Log.d("AlarmService", "next AlarmReceiver trigger time="+sdf.format(time.getTimeInMillis()));

                // timeが指す時刻にAlarmReceiverを起動する
                // intent_ringはAlarmReceiverクラスに動作を遷移するよう設定する
                Intent intent_ring = new Intent(getApplicationContext(),AlarmReceiver.class);

                PendingIntent pendingIntent_ring
                        = PendingIntent.getBroadcast(context, 1, intent_ring,PendingIntent.FLAG_CANCEL_CURRENT);

                // SDK 19 以下ではsetを使う
                if (android.os.Build.VERSION.SDK_INT < 19) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pendingIntent_ring);
                } else {
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
                            time.getTimeInMillis(), windowLengthMillis, pendingIntent_ring);
                }
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
