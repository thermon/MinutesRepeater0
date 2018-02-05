package com.example.therm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.example.therm.MainActivity.PreferencesName;

// ミニッツリピーター鳴動クラス
public class minutesRepeat implements Runnable {


    private final int resId[][] = {
            {R.raw.hour},
            {R.raw.min15},
            {R.raw.min1}
    };
    // 音関係の変数
    private SoundPool SoundsPool;
    private int soundId[][] = new int[resId.length][];

    private int[][] ListofWaitList = new int[resId.length][];
    private Context MainContext;
    private SharedPreferences sharedPreferences;

    private int[][][] zonesArray = new int[2][][];
    private boolean[] zonesEnable = new boolean[2];
    private int intervalProgress = 0;
    private int intervalMinutes;
    private boolean basedOnHour = false;
    private boolean executeOnBootCompleted;
    /*
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

        }
        */
    private Handler myHandler;

    // コンストラクター
    minutesRepeat(Context context) {
        MainContext = context;


        sharedPreferences = MainContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);

        for (int i = 0; i < resId.length; i++) {
            soundId[i] = new int[resId[i].length];
            ListofWaitList[i] = new int[resId[i].length];
        }
        // 音読み込み
        //ロリポップより前のバージョンに対応するコード
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SoundsPool = new SoundPool(12 + 3 + 14 + 3, AudioManager.STREAM_NOTIFICATION, 0);
        } else {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            SoundsPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(12 + 3 + 14 + 3)
                    .build();
        }
        SoundsPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.d("debug", "sampleId=" + sampleId);
                Log.d("debug", "status=" + status);
            }
        });

        // AudioManagerを取得する
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (am != null) {
            // 現在の音量を取得する
            int ringVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

            // ストリームごとの最大音量を取得する
            int ringMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

            // 音量を設定する
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, ringVolume, 0);
        }

        //あらかじめ音をロードする必要がある　※直前にロードしても間に合わないので早めに
        for (int i = 0; i < resId.length; i++) {
            for (int j = 0; j < soundId[i].length; j++) {
                int id = SoundsPool.load(
                        context, resId[i][j], 1);
                soundId[i][j] = id;
                MediaPlayer mp = MediaPlayer.create(context, resId[i][j]);
                ListofWaitList[i][j] = mp.getDuration();
                mp.release();
            }
        }
        intervalMinutes = 2;
    }

    int[][][] getZonesArray() {
        return zonesArray;
    }

    void setZonesArray(int[][][] z) {
        zonesArray = z;

        Gson gson = new Gson();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("zonesArray", gson.toJson(zonesArray));
        editor.apply();
    }

    boolean[] getZonesEnable() {
        return zonesEnable;
    }

    void setZonesEnable(boolean b[]) {
        zonesEnable = b;

        Gson gson = new Gson();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("zonesEnable", gson.toJson(zonesEnable));
        editor.apply();
    }

    int getIntervalProgress() {
        return intervalProgress;
    }

    void setIntervalProgress(int progress) {
        intervalProgress = progress;

        Gson gson = new Gson();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("intervalSeekBar", progress);
        editor.apply();
    }

    boolean getBasedOnHour() {
        return basedOnHour;
    }

    void setBasedOnHour(boolean b) {
        basedOnHour = b;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("basedOnHour", b);
        editor.apply();
    }

    boolean getExecuteOnBootCompleted() {
        return executeOnBootCompleted;
    }

    void setExecuteOnBootCompleted(boolean b) {
        executeOnBootCompleted = b;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("executeOnBootCompleted", b);
        editor.apply();
    }

    void loadData() {
        Gson gson = new Gson();
//        zones=null;
        zonesArray = null;
//        zones=gson.fromJson(sharedPreferences.getString("zones",null),int[][].class);
        zonesArray = gson.fromJson(sharedPreferences.getString("zonesArray", null), int[][][].class);
        zonesEnable = gson.fromJson(sharedPreferences.getString("zonesEnable", null), boolean[].class);
        intervalProgress = sharedPreferences.getInt("intervalSeekBar", 0);
        basedOnHour = sharedPreferences.getBoolean("basedOnHour", false);
        executeOnBootCompleted = sharedPreferences.getBoolean("executeOnBootCompleted", false);

        Log.d("Preferences", "load intervalSeekBar:" + intervalProgress);

        if (zonesArray == null) {
            zonesArray = new int[2][][];
            for (int i = 0; i < zonesArray.length; i++) {
                zonesArray[i] = new int[2][];
                for (int j = 0; j < zonesArray[i].length; j++) {
                    zonesArray[i][j] = new int[]{0, 0};
                }
            }
        }
        if (zonesEnable == null) {
            zonesEnable = new boolean[2];
        }
    }

    // シークバーの値から鳴動間隔に変換
    int getIntervalValue() {
        int unitTime;
        if (basedOnHour) {
            // 鳴動時刻を00分基準とした場合
            unitTime = MainActivity.intervalList[intervalProgress];

        } else {
            unitTime = intervalProgress + MainActivity.intervalMin[0];
        }
        return unitTime;
    }

    public void testSound(int id) {
        SoundsPool.play(soundId[1][0], 1.0f, 1.0f, 0, 9, 1.0f);
    }

    Calendar getNextAlarmTime(
//            boolean [] zonesEnable, int [][][] zonesArray, int interval, boolean basedOnHour,
            Calendar time
    ) {
//        int [][][] zonesArray;
        int interval = getIntervalValue();

        StackTraceElement[] ste = new Throwable().getStackTrace();
        for (int i = 1; i < 2; i++) {
            Log.d("getNextAlarmTime", "called:" + ste[i].getClassName() + "." + ste[i].getMethodName() +
                    ", line " + ste[i].getLineNumber() + " of " + ste[i].getFileName());
        }
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        Log.d("getNextAlarmTime", "now=" + sdf2.format(time.getTime()));

        Log.d("getNextAlarmTime", "interval=" + String.valueOf(interval));
        // Serviceを呼び出す

        // timeがnullならnullを返す
        // if (time == null) return null;
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);

        // zonesがひとつでも有効かチェック
        boolean enable = false;
        for (int i = 0; i < zonesEnable.length; i++) {
            Log.d("getNextAlarmTime", String.format("zonesEnable[%d]=%b", i, zonesEnable[i]));
            if (zonesEnable[i]) {
                enable = true;
                break;
            }
        }
        // すべてnullだったらnullを返す
        if (!enable) return null;

        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);

        int nextTime = hour * 60 + minute;
        // basedOnHourがtrueなら、毎時00分を基準にする
        if (basedOnHour) {
            nextTime += interval - (minute % interval);
        } else {
            nextTime += interval;
        }
        Log.d("getNextAlarmTime", "next=" + timeFormat(nextTime));
        // アラーム時刻変更
        // 現在時刻と時間帯をもとにアラーム時刻を設定
        ArrayList<Integer> AlarmTime = new ArrayList<>();


        for (int i = 0; i < zonesArray.length; i++) {
            if (!zonesEnable[i]) continue;
//            int StartTime = zones[i][0];
//            int EndTime = zones[i][1];

            int zone[][] = zonesArray[i];
            int StartTime = zone[0][0] * 60
                    + zone[0][1];
            int EndTime = zone[1][0] * 60
                    + zone[1][1];

            if (EndTime > StartTime) { // 時間帯が日を跨がない場合
                if (nextTime >= EndTime) {  // 時間帯終了後のとき
                    // 開始時刻及び終了時刻は翌日になる
                    StartTime += 24 * 60;
                    EndTime += 24 * 60;
                }
            } else {    // 時間帯が日を跨ぐ場合
                if (nextTime < EndTime) {   // 予定時刻が終了時刻より前　＝　時間帯を過ぎていない
                    // 開始時刻は前日になる
                    StartTime -= 24 * 60;
                } else {
                    // 終了時刻は翌日
                    EndTime += 24 * 60;
                }
            }
/*
            if (StartTime > EndTime) {  // 終了時間が開始時間より早い
                if (StartTime > nextTime) {
                    StartTime-=24*60;
                } else {
                    EndTime+=24*60; // 終了時間を翌日に
                }
              } else {
                if ( nextTime > EndTime) {
                    // 開始時刻より終了時刻が早い＝翌日とみなす
                    StartTime += 24 * 60;
                    EndTime += 24 * 60;
                }
            }
*/

            Log.d("getNextAlarmTime", String.format("start[%d]=%s", i, timeFormat(StartTime)));
            Log.d("getNextAlarmTime", String.format("  end[%d]=%s", i, timeFormat(EndTime)));
            if (EndTime > nextTime && nextTime >= StartTime) {
                // 次回予定時刻がタイムゾーン内にある場合
                AlarmTime.add(nextTime);
                Log.d("getNextAlarmTime", String.format(" in:[%d]=%s", i, timeFormat(nextTime)));
            } else {
                // 現在時刻がタイムゾーン外の場合、次回予定時刻より遅い直近の開始時刻を探す。
                if (basedOnHour) {
                    int StartTime2 = StartTime;
                    StartTime2 -= (StartTime2 % 60) % interval;
                    while (StartTime > StartTime2) StartTime2 += interval;
                    StartTime = StartTime2;
                }
                AlarmTime.add(StartTime);
                Log.d("getNextAlarmTime", String.format("out:[%d]=%s", i, timeFormat(StartTime)));

            }
        }

        for (int t : AlarmTime) {
            Log.d("getNextAlarmTime", String.format("StartTimes=%s", timeFormat(t)));
        }

        int Start1 = -1;
        int Start2 = 2 * 24 * 60;

        if (AlarmTime.size() < 1) return null;
        // 開始時刻リストでループ
        for (int t : AlarmTime) {
            // 開始時刻が予定時刻よりも早い && Start1が開始時刻よりも早いなら、Start1に開始時刻を入れる
            if (t < nextTime) {
                if (Start1 < t) Start1 = t;
            }
            // 開始時刻が予定時刻よりも遅い && Start2が開始時刻よりも遅いなら、Start2に開始時刻を入れる
            else {
                if (Start2 > t) Start2 = t;
            }
        }
        // 全ての開始時刻が予定時刻よりも遅いなら、開始時刻の中で一番早い時刻を予定時刻にする
        // 予定時刻よりも早い開始時刻があれば、そのなかで一番遅い開始時刻を予定時刻にする

        final int Start = (Start1 == -1) ? Start2 : Start1;

        minute = Start % 60;
        hour = (Start - minute) / 60;
        int day = (hour >= 24) ? 1 : 0;
        hour -= day * 24;

        Log.d("getNextAlarmTime", String.format("next=%1$02d:%2$02d", hour, minute));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.add(Calendar.DATE, day);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

    private String timeFormat(int time) {
        int day = 0;
        while (time < 0) {
            time += 24 * 60;
            day--;
        }
        while (time >= 24 * 60) {
            time -= 24 * 60;
            day++;
        }
        int minute = time % 60;
        int hour = (time - minute) / 60;
        return String.format(Locale.US, "%+d %02d:%02d", day, hour, minute);
    }

    void AlarmSet(Calendar time) {
        time = getNextAlarmTime(time);
        // 時間帯のどれかが有効の場合
        // Serviceを呼び出す
        Intent serviceIntent = new Intent(MainContext, AlarmService.class);

        if (time != null) {
            time.set(Calendar.SECOND, 0);
            time.set(Calendar.MILLISECOND, 0);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
            Log.d("AlarmSet", sdf.format(time.getTime()));
            Gson gson = new Gson();

            serviceIntent.putExtra("intervalProgress", intervalProgress);
            serviceIntent.putExtra("zonesArray", gson.toJson(zonesArray));
            serviceIntent.putExtra("zonesEnable", zonesEnable);
            serviceIntent.putExtra("triggerTime", time);
            serviceIntent.putExtra("basedOnHour", basedOnHour);
            MainContext.startService(serviceIntent);

        } else {
            // Alarm の停止を解除
            MainContext.startService(serviceIntent);

        }
    }

    // リピーター音を鳴らす処理
    public void run() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int minuteDivide = 15;
        int min_1 = minute % minuteDivide;
        int min_15 = (minute - min_1) / minuteDivide;

        hour %= 12;
        if (hour == 0) {
            hour = 12;
        }

        int count[] = {hour, min_15, min_1}; // 鳴動回数

        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int k = 0; k < resId.length; k++) {  // チャイム、時間、15分、5分、1分の順で鳴らす
//            Log.d("debug", "k=" + k);

            if (count[k] > 0) {   // カウントする場合
                // 鳴動前時間待ち
                try {
                    TimeUnit.MILLISECONDS.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }
                ring(k, count[k]);
            }
        }
    }

    // SoundPoolから音を鳴らし、waitミリ秒待つ
    private void ring(final int sound, final int loop) {
        if (soundId[sound].length == 1) {
            SoundsPool.play(soundId[sound][0], 1.0f, 1.0f, 0, loop - 1, 1.0f);
            try {
                TimeUnit.MILLISECONDS.sleep(ListofWaitList[sound][0] * loop + 200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if (loop > 1) {
                SoundsPool.play(soundId[sound][1], 1.0f, 1.0f, 0, loop - 2, 1.0f);
                try {
                    TimeUnit.MILLISECONDS.sleep(ListofWaitList[sound][1] * (loop - 1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            SoundsPool.play(soundId[sound][0], 1.0f, 1.0f, 0, 0, 1.0f);
            try {
                TimeUnit.MILLISECONDS.sleep(ListofWaitList[sound][0] + 200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    void setHandler(Handler mHandler) {
        myHandler = mHandler;
    }


}
