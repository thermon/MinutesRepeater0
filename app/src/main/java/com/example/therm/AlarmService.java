package com.example.therm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;

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
    //    private int [][] zones;
    private int[][][] zonesArray;
    private int [][] zones;
    private boolean [] zonesEnable;
    private Calendar time=null;
    private boolean basedOnHour;
    private int intervalProgress;
    private NotificationManager notificationManager;
    // private Notification notification;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AlarmService", "created");
        context = getApplicationContext();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    public void onDestroy() {
        super.onDestroy();
//        stopSelf();
        Log.d("AlarmService", "destroied");
    }

    // Alarm によって呼び出される
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        minutesRepeat repeater = new minutesRepeat(context);
        Log.d("AlarmService", "start");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                Log.d("AlarmService",
                        String.format(Locale.US, "key=%s,value=%s", key, bundle.get(key).toString())
                );
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
//        Boolean stopFlag = intent.getBooleanExtra("StopAlarm", false);

        // intentから情報を取り出す
        Calendar t = (Calendar) (intent.getSerializableExtra("triggerTime"));

        zones = (int [][]) (intent.getSerializableExtra("zones"));
        Gson gson = new Gson();
        String z = intent.getStringExtra("zonesArray");
        zonesArray = gson.fromJson(z, int[][][].class);
        zonesEnable=intent.getBooleanArrayExtra("zonesEnable");
        basedOnHour = intent.getBooleanExtra("basedOnHour",false);
        intervalProgress = intent.getIntExtra("intervalProgress", 0);

        repeater.setZonesArray(zonesArray);
        repeater.setZonesEnable(zonesEnable);
        repeater.setBasedOnHour(basedOnHour);
        repeater.setIntervalProgress(intervalProgress);

        // アラーム時刻が設定されていれば継続
        if(t!=null){
            boolean alarmTimeIsChanged=false;
            // 現在時刻がtimeを過ぎていた場合、次のアラーム時刻をtimeにセットする
            long now = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
//            now+= windowLengthMillis;  // 4秒進める

            if (now >= t.getTimeInMillis()) {

                Calendar next = repeater.getNextAlarmTime(t);
                if (next != null) {

                    // アラーム時刻が更新されたらtrue
                    if (t.getTimeInMillis() != next.getTimeInMillis()) {
                        alarmTimeIsChanged = true;
                        time = next;
                    } else {
                        alarmTimeIsChanged = false;
                        time = t;
                    }
                    setNextAlarmService(alarmTimeIsChanged);
                } else {
                    time = null;
                    stopAlarmService();
                }

            } else {
                if (time == null || t.getTimeInMillis() != time.getTimeInMillis()) {
                    // timeにCalendarインスタンスが未設定だったらtrue
                    // timeとtの内容が違っていたらtrue
                    alarmTimeIsChanged = true;
                }
                time = t;
                setNextAlarmService(alarmTimeIsChanged);
            }
        } else {
            time = null;
            stopAlarmService();
        }

        // Local Broadcast で発信する
        // 次回アラーム時刻をAlarmEventアクションを受け取れるレシーバーに送る
        Intent messageIntent = new Intent("AlarmTimeChanged");
        messageIntent.putExtra("Message", (time!=null) ? sdf.format(time.getTime()): "undefined");
        messageIntent.putExtra("time",(time!=null) ? time.getTimeInMillis(): -1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);


        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
        if (time != null) {
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.mipmap.ic_launcher_round) // アイコン
                    .setTicker("ミニッツリピーター:次回通知時刻は" + sdf.format(time.getTime()) + "です。") // 通知バーに表示する簡易メッセージ
                    .setWhen(System.currentTimeMillis()) // 時間
                    .setContentTitle("ミニッツリピーター") // 展開メッセージのタイトル
                    .setContentText("次回通知時刻：" + sdf.format(time.getTime())) // 展開メッセージの詳細メッセージ
                    .setContentIntent(contentIntent) // PendingIntent
                    .build();

            notification.flags = Notification.FLAG_ONGOING_EVENT;
            notificationManager.notify(R.string.app_name, notification);
        } else {
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.mipmap.ic_launcher_round) // アイコン
//                    .setTicker("Hello") // 通知バーに表示する簡易メッセージ
                    .setWhen(System.currentTimeMillis()) // 時間
                    .setContentTitle("ミニッツリピーター") // 展開メッセージのタイトル
                    .setContentText("次回通知時刻：" + "undefined") // 展開メッセージの詳細メッセージ
                    .setContentIntent(contentIntent) // PendingIntent
                    .build();
            notification.flags = 0;
            notificationManager.cancel(R.string.app_name);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    // 次のアラームの設定
    private void setNextAlarmService(boolean b){

        // intentはAlarmServiceクラス（つまり自分自身）に動作を遷移するよう設定する
        Intent intent = new Intent(getApplicationContext(), AlarmService.class);
        Gson gson = new Gson();
        intent.putExtra("triggerTime", time);
        intent.putExtra("zonesArray", gson.toJson(zonesArray));
        intent.putExtra("zonesEnable", zonesEnable);
        intent.putExtra("basedOnHour", basedOnHour);
        intent.putExtra("intervalProgress", intervalProgress);

        // repeatPeriod間隔でAlarmServiceを起動する

        long startMillis = System.currentTimeMillis();
        startMillis+=repeatPeriod - startMillis % repeatPeriod;

        PendingIntent pendingIntent
                = PendingIntent.getService(context, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager
                = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(alarmManager != null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
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
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, indent, PendingIntent.FLAG_CANCEL_CURRENT);

        // アラームを解除する
        AlarmManager alarmManager
                = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarmManager != null){
            alarmManager.cancel(pendingIntent);

            // timeが指す時刻にAlarmReceiverを起動する
            // intent_ringはAlarmReceiverクラスに動作を遷移するよう設定する
            Intent intent_ring = new Intent(getApplicationContext(), AlarmReceiver.class);

            PendingIntent pendingIntent_ring
                    = PendingIntent.getBroadcast(context, 1, intent_ring, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.cancel(pendingIntent_ring);

        }
        Log.d("AlarmService", "stop");
    }

}
