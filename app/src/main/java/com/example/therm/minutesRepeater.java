package com.example.therm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

import com.google.gson.Gson;

import org.jetbrains.annotations.Contract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

import static com.example.therm.MainActivity.PreferencesName;
import static com.example.therm.MainActivity.viewIdArray;
import static com.example.therm.myApplication.getClassName;
import static com.example.therm.myApplication.sdf_yyyyMMddHHmmss;

// ミニッツリピーター鳴動クラス
class minutesRepeater {

    private final static int minutesOfHour = 60;
    private final static int hoursOfDay = 24;
    private final static int minutesOfDay = hoursOfDay * minutesOfHour;
    static int myStreamId = AudioManager.STREAM_ALARM;

    private String className;
    private Context MainContext;
    private SharedPreferences sharedPreferences;
    private int[][][] zonesArray = new int[viewIdArray.length][][];
    private boolean[] zonesEnable = new boolean[viewIdArray.length];
    private int intervalProgress = 0;
    private int intervalMinutes = 2;
    private boolean basedOnHour = false;
    private boolean executeOnBootCompleted;
    /*
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

        }
        */

    // コンストラクター
    minutesRepeater(Context context) {
        className = getClassName();
        MainContext = context;
        sharedPreferences = MainContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
        loadData();
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

    private void loadData() {
        Gson gson = new Gson();
        zonesArray = null;
        zonesArray = gson.fromJson(sharedPreferences.getString("zonesArray", null), int[][][].class);
        zonesEnable = gson.fromJson(sharedPreferences.getString("zonesEnable", null), boolean[].class);
        intervalProgress = sharedPreferences.getInt("intervalSeekBar", 0);
        basedOnHour = sharedPreferences.getBoolean("basedOnHour", false);
        executeOnBootCompleted = sharedPreferences.getBoolean("executeOnBootCompleted", false);

        Log.d("Preferences", "load intervalSeekBar:" + intervalProgress);

        if (zonesArray == null) {
            zonesArray = new int[viewIdArray.length][][];
            for (int i = 0; i < zonesArray.length; i++) {
                zonesArray[i] = new int[][]{{0, 0}, {0, 0}};
            }
        }
        if (zonesEnable == null) zonesEnable = new boolean[viewIdArray.length];
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

    @Contract(pure = true)
    private int makeTime(int[] time) {
        return time[0] * minutesOfHour + time[1];
    }

    private int getRemainedOfMinute(Calendar cal, int interval) { // 毎時00分を基準に、interval分に満たない余りを求める
        int minute = cal.get(Calendar.MINUTE);
        return
                (new div_qr(minute)
                        .divide(interval)
                        .getArray()[1]
                );
        // return minute % interval;
    }

    private int getUntilAlarmStart(Calendar time, Calendar Start, Calendar End) {
        int minute = (int) ((Start.getTimeInMillis() - time.getTimeInMillis()) / (1000 * 60));   // timeからStartまでの時間差を分で求める;
        if (Start.compareTo(End) < 0) {         // 0時～開始時刻～終了時刻～24時のとき
            if (time.compareTo(End) >= 0) {                                    // 予定時刻が終了時刻以降のとき
                // 開始時刻及び終了時刻は翌日にずらして、24時～開始時刻～終了時刻にする
                minute += 24 * 60;   // 時間差に１日を足す
            }
        } else {                                        // 0時～終了時刻、    開始時刻～24時のとき
            if (time.compareTo(End) < 0) {          // 予定時刻が終了時刻前
                // 開始時刻を一日早めて、  開始時刻～0時～終了時刻にする
                minute -= 24 * 60;
                // minute=0;// 期間内フラグを立てる
            }
        }
        return minute;
    }


    Calendar getNextAlarmTime(Calendar time) {
        if (time == null) return null;
        int interval = getIntervalValue();

        Log.d("getNextAlarmTime", "interval=" + String.valueOf(interval));

        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);

        // basedOnHourがtrueなら、毎時00分を基準にする
        time.add(Calendar.MINUTE,
                interval - (basedOnHour ? getRemainedOfMinute(time, interval) : 0));

        Log.d("getNextAlarmTime", "next=" + sdf_yyyyMMddHHmmss.format(time.getTimeInMillis()));

        // アラーム時刻候補リスト
        ArrayList<Calendar> alarmTime = new ArrayList<>();
        Calendar laterTime = null;

        // アラーム時刻変更
        // 現在時刻と時間帯をもとにアラーム時刻を設定
        for (int i = 0; i < zonesArray.length; i++) {
            if (!zonesEnable[i]) continue;  // 時間帯が無効の場合はスキップ

            int zone[][] = zonesArray[i];

            Calendar timeArray[] = new Calendar[myApplication.timeIndexEnum.values().length];
            for (int j = 0; j < timeArray.length; j++) {
                timeArray[j] = Calendar.getInstance();
                timeArray[j].set(Calendar.HOUR_OF_DAY, zone[j][myApplication.timeFieldEnum.hour.ordinal()]);
                timeArray[j].set(Calendar.MINUTE, zone[j][myApplication.timeFieldEnum.minute.ordinal()]);
                timeArray[j].set(Calendar.SECOND, 0);
                timeArray[j].set(Calendar.MILLISECOND, 0);
            }
            Calendar StartTime = timeArray[myApplication.timeIndexEnum.start.ordinal()];
            Calendar EndTime = timeArray[myApplication.timeIndexEnum.end.ordinal()];
            // boolean inPeriod    = false;

            int untilStartMinute = getUntilAlarmStart(time, StartTime, EndTime);
            StartTime.setTimeInMillis(
                    time.getTimeInMillis() + untilStartMinute * 60 * 1000
            );

            int timeData[] = new div_qr(
                    (untilStartMinute >= 0)
                            ? untilStartMinute
                            : -untilStartMinute
            )
                    .divide(60)
                    .getArray();

            Log.d("getNextAlarmTime",
                    Arrays.toString(timeData));

            Log.d("getNextAlarmTime",
                    String.format(Locale.US, "untilStart=%c%02d:%02d",
                            (untilStartMinute >= 0) ? ' ' : '-',
                            timeData[0],
                            timeData[1]
                    ));

            // Log.d("getNextAlarmTime", String.format("untilStart=%d", untilStart));

            if (basedOnHour) {
                int remain = getRemainedOfMinute(StartTime, interval);
                if (remain > 0) StartTime.add(Calendar.MINUTE, interval - remain);
            }


            Calendar potentialTime = (untilStartMinute <= 0) ? time : StartTime;   // 次回アラーム候補時刻

            Log.d("getNextAlarmTime", "time=" + sdf_yyyyMMddHHmmss.format(time.getTimeInMillis()));
            Log.d("getNextAlarmTime", String.format(" potentialTime=%s",
                    sdf_yyyyMMddHHmmss.format(potentialTime.getTimeInMillis())
            ));
            if (time.compareTo(potentialTime) <= 0 &&
                    (laterTime == null ||
                            potentialTime.compareTo(laterTime) < 0
                    )
                    ) {
                // 予定時刻≦laterTime＝開始時刻にする
                laterTime = potentialTime;
                Log.d("getNextAlarmTime", String.format(
                        "laterTimes=%s",
                        sdf_yyyyMMddHHmmss.format(laterTime.getTimeInMillis())
                ));
            }
        }

        return laterTime;
    }

    void AlarmSet(Calendar time) {
        // Serviceを呼び出す
        Intent serviceIntent =
                new Intent(MainContext, myService.class);
        // new Intent("service");

        time = getNextAlarmTime(time);

        if (time != null) {
            // サービスを起動してアラームを予約
//            time.set(Calendar.SECOND, 0);
//            time.set(Calendar.MILLISECOND, 0);

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
            // サービスを起動してアラーム予約を解除
            MainContext.startService(serviceIntent);
            // サービスを停止
            MainContext.stopService(serviceIntent);

        }
    }
}
