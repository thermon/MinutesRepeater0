package com.example.therm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Calendar;

import static com.example.therm.myApplication.sdf_HHmm;
import static com.example.therm.myApplication.sdf_HHmmss;
import static com.example.therm.myApplication.sdf_yyyyMMddHHmmss;

public class myService extends Service {
    // private Notification notification;
    minutesRepeater repeater = null;
    private String className = "";
    // 繰り返し間隔、1分
    private long repeatPeriod = 1000*60;
    private int offset =-2;
    // setWindow()でのwindow幅、4秒
    private long windowLengthMillis = 1000*4;
    private Context context = null;
    //    private int [][] zones;
    private int[][][] zonesArray;
    private int [][] zones;
    private boolean[] zonesEnable;
    private Calendar time=null;
    private boolean basedOnHour;
    private int intervalProgress;
    private NotificationManager notificationManager = null;

    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        Log.d(className, "destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
    public myService() {
        super(myApplication.getClassName());
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        start(intent);
    }
*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        start(intent);
        //return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
    }

    // Alarm によって呼び出される

    void start(@Nullable Intent intent) {
        if (className.equals("")) className = myApplication.getClassName();
        Log.d(className, "start");
        if (context == null) context = getApplicationContext();
        if (notificationManager == null)
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        repeater = new minutesRepeater(this);

        // アラーム時刻が設定されていないときはサービスを止める
        if (intent == null ||
                intent.getExtras() == null ||
                !intent.getExtras().containsKey("triggerTime") ||
                intent.getSerializableExtra("triggerTime") == null
                ) {
            // time = null;
            stopAlarmService();
        } else {
            // intentから情報を取り出す
            Calendar t = (Calendar) (intent.getSerializableExtra("triggerTime"));
            Log.d(className, String.format("intent time=%s", (t != null) ? sdf_HHmmss.format(t.getTime()) : "undefined"));

            zones = (int[][]) (intent.getSerializableExtra("zones"));
            Gson gson = new Gson();
            String z = intent.getStringExtra("zonesArray");
            zonesArray = gson.fromJson(z, int[][][].class);
            zonesEnable = intent.getBooleanArrayExtra("zonesEnable");
            basedOnHour = intent.getBooleanExtra("basedOnHour", false);
            intervalProgress = intent.getIntExtra("intervalProgress", 0);

            repeater.setZonesArray(zonesArray);
            repeater.setZonesEnable(zonesEnable);
            repeater.setBasedOnHour(basedOnHour);
            repeater.setIntervalProgress(intervalProgress);

            boolean alarmTimeIsChanged;
            // 現在時刻がtimeを過ぎていた場合、次のアラーム時刻をtimeにセットする
            long now = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
//            now+= windowLengthMillis;  // 4秒進める

            assert t != null;
            if (now >= t.getTimeInMillis()) {
                // 起動時刻を過ぎた
                Calendar next = repeater.getNextAlarmTime(cal);
                Log.d(className, String.format("next alarm time=%s", (next != null) ? sdf_HHmmss.format(next.getTime()) : "undefined"));

                if (next != null) {
                    // アラーム時刻が更新された
                    alarmTimeIsChanged = (t.getTimeInMillis() != next.getTimeInMillis());
                    setNextAlarmService(alarmTimeIsChanged, alarmTimeIsChanged ? next : t);

                } else {
                    // アラーム時刻未設定
                    stopAlarmService();
                }

            } else {
                // 起動時刻を過ぎていない
                Log.d(className, String.format("elder time=%s", (time != null) ? sdf_HHmmss.format(time.getTime()) : "undefined"));
                if (intent.getBooleanExtra("loop", false)) {
                    // AlarmServiceから呼ばれてて、時刻が後になっていたら
                    alarmTimeIsChanged = (time == null || t.getTimeInMillis() > time.getTimeInMillis());
                    if (time == null || t.getTimeInMillis() > time.getTimeInMillis())
                        Log.d(className, "time changed(called by Service)");
                } else {
                    // AlarmServiceから呼ばれてなくて、時刻が違っていたらtrue
                    alarmTimeIsChanged = (time == null || t.getTimeInMillis() != time.getTimeInMillis());
                    if (time == null || t.getTimeInMillis() != time.getTimeInMillis())
                        Log.d(className, "time changed(called by other)");
                }
                //time = t;

                setNextAlarmService(alarmTimeIsChanged, t);

            }
        }

        //if (time!=null) time.add(Calendar.SECOND,offset);
        Log.d(className, String.format("Broadcast time=%s", (time != null) ? sdf_HHmmss.format(time.getTime()) : "undefined"));
        // Local Broadcast で発信する
        // 次回アラーム時刻をAlarmEventアクションを受け取れるレシーバーに送る

        Intent messageIntent = new Intent("AlarmTimeChanged");
        messageIntent.putExtra("time", (time != null) ? time.getTimeInMillis() : -1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    }

    // 次のアラームの設定
    private void setNextAlarmService(boolean b,Calendar cal){
        // intentはAlarmServiceクラス（つまり自分自身）に動作を遷移するよう設定する
        Intent intent = new Intent(context, myService.class);
        time = cal;

        Gson gson = new Gson();
        intent.putExtra("triggerTime", time);
        intent.putExtra("zonesArray", gson.toJson(zonesArray));
        intent.putExtra("zonesEnable", zonesEnable);
        intent.putExtra("basedOnHour", basedOnHour);
        intent.putExtra("intervalProgress", intervalProgress);
        intent.putExtra("loop", true);
        PendingIntent pendingIntent
                = PendingIntent.getService(context, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager
                = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(alarmManager != null){

            Log.d(className, "next AlarmService trigger time=" + sdf_yyyyMMddHHmmss.format(cal.getTimeInMillis()));

            // repeatPeriod間隔でAlarmServiceを起動する
            // long startMillis;
            // startMillis = System.currentTimeMillis();
            // startMillis+=repeatPeriod - startMillis % repeatPeriod;
            // SDK 19 以下ではsetを使う
            // 次回鳴動時刻の更新時刻を一瞬遅らせる（負荷低減のため？）
            if(android.os.Build.VERSION.SDK_INT < 19) {
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        //startMillis,
                        cal.getTimeInMillis() + 50, pendingIntent);
            } else{
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
//                        startMillis
                        cal.getTimeInMillis() + 50, windowLengthMillis, pendingIntent);
            }

            // 現在時刻がtimeを過ぎていた場合、次のアラーム時刻をtimeにセットする
            if (b) {
                // timeが指す時刻にAlarmReceiverを起動する
                // intent_ringはAlarmReceiverクラスに動作を遷移するよう設定する
                Intent intent_ring = new Intent(context, myReceiver.class);
                intent_ring.setAction("ring");

                PendingIntent pendingIntent_ring
                        = PendingIntent.getBroadcast(context, 1, intent_ring,PendingIntent.FLAG_CANCEL_CURRENT);

                Log.d(className, "next ring trigger time=" + sdf_yyyyMMddHHmmss.format(cal.getTimeInMillis()));
                // SDK 19 以下ではsetを使う
                if (android.os.Build.VERSION.SDK_INT < 19) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),pendingIntent_ring);
                } else {
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP,
                            cal.getTimeInMillis(), windowLengthMillis, pendingIntent_ring);
                }
            }
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher_round) // アイコン
                .setTicker("ミニッツリピーター:次回通知時刻は" + sdf_HHmm.format(time.getTime()) + "です。") // 通知バーに表示する簡易メッセージ
                .setWhen(System.currentTimeMillis()) // 時間
                .setContentTitle("ミニッツリピーター") // 展開メッセージのタイトル
                .setContentText("次回通知時刻：" + sdf_HHmm.format(time.getTime())) // 展開メッセージの詳細メッセージ
                .setContentIntent(contentIntent) // PendingIntent
                .build();

        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(R.string.app_name, notification);
        stopSelf();
    }

    private void stopAlarmService(){
        Intent indent = new Intent(context, myService.class);
        time = null;

        PendingIntent pendingIntent = PendingIntent.getService(context, 0, indent, PendingIntent.FLAG_CANCEL_CURRENT);

        // アラームを解除する
        AlarmManager alarmManager
                = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(alarmManager != null){
            alarmManager.cancel(pendingIntent);

            // timeが指す時刻にAlarmReceiverを起動する
            // intent_ringはAlarmReceiverクラスに動作を遷移するよう設定する
            Intent intent_ring = new Intent(context, myReceiver.class);
            intent_ring.setAction("ring");

            PendingIntent pendingIntent_ring
                    = PendingIntent.getBroadcast(context, 1, intent_ring, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.cancel(pendingIntent_ring);

        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
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

        Log.d(className, "alarm stop");
    }

}
