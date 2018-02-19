package com.example.therm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

import com.example.therm.myApplication.timeFieldEnum;
import com.example.therm.myApplication.timeIndexEnum;
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

public class myApplication extends android.app.Application {
    // 時刻表示のフォーマット
    public static SimpleDateFormat sdf_HHmm = new SimpleDateFormat("HH:mm", Locale.US);
    public static SimpleDateFormat sdf_HHmmss = new SimpleDateFormat("HH:mm:ss Z", Locale.US);
    public static SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyy.MM.dd(E) HH:mm:ss Z", Locale.JAPANESE);

    public static String getClassName() {
        return Thread.currentThread().getStackTrace()[3].getClassName().replace("$", ".");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public enum timeFieldEnum {
        hour,
        minute
    }

    public enum timeIndexEnum {
        start,
        end
    }

}

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
        return minute % interval;
    }

    private long getUntilAlarmStart(Calendar time, Calendar Start, Calendar End) {
        long millisecond = (Start.getTimeInMillis() - time.getTimeInMillis());   // timeからStartまでの時間差を分で求める;
        if (Start.compareTo(End) < 0) {         // 0時～開始時刻～終了時刻～24時のとき
            if (time.compareTo(End) >= 0) {                                    // 予定時刻が終了時刻以降のとき
                // 開始時刻及び終了時刻は翌日にずらして、24時～開始時刻～終了時刻にする
                millisecond += 24 * 60 * 60 * 1000;   // 時間差に１日を足す
            }
        } else {                                        // 0時～終了時刻、    開始時刻～24時のとき
            if (time.compareTo(End) < 0) {          // 予定時刻が終了時刻前
                // 開始時刻を一日早めて、  開始時刻～0時～終了時刻にする
                millisecond -= 24 * 60 * 60 * 1000;
                // minute=0;// 期間内フラグを立てる
            }
        }
        return millisecond;
    }


    Calendar getNextAlarmTime(Calendar time) {
        if (time == null) return null;
        int interval = getIntervalValue();
/*
        StackTraceElement[] ste = new Throwable().getStackTrace();
        for (int i = 1; i < 2; i++) {
            Log.d("getNextAlarmTime", "called:" + ste[i].getClassName() + "." + ste[i].getMethodName() +
                    ", line " + ste[i].getLineNumber() + " of " + ste[i].getFileName());
        }
*/

        Log.d("getNextAlarmTime", "now=" + sdf_yyyyMMddHHmmss.format(time.getTimeInMillis()));
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

            Calendar times[] = new Calendar[timeIndexEnum.values().length];
            for (int j = 0; j < times.length; j++) {
                times[j] = Calendar.getInstance();
                times[j].set(Calendar.HOUR_OF_DAY, zone[j][timeFieldEnum.hour.ordinal()]);
                times[j].set(Calendar.MINUTE, zone[j][timeFieldEnum.minute.ordinal()]);
                times[j].set(Calendar.SECOND, 0);
                times[j].set(Calendar.MILLISECOND, 0);
            }
            Calendar StartTime = times[timeIndexEnum.start.ordinal()];
            Calendar EndTime = times[timeIndexEnum.end.ordinal()];
            // boolean inPeriod    = false;

            long untilStart = getUntilAlarmStart(time, StartTime, EndTime);
            long timeData[] = new div_qr(untilStart)
                    .divide(1000)
                    .divide(60)
                    .divide(60)
                    .getArray();
            Log.d("getNextAlarmTime",
                    Arrays.toString(timeData));

            for (int x = 1; x < timeData.length; x++) {
                if (timeData[x] < 0) timeData[x] = -timeData[x];
            }

            Log.d("getNextAlarmTime",
                    String.format(Locale.US, "untilStart=%02d:%02d:%02d.%03d", timeData[0], timeData[1], timeData[2], timeData[3]));


            /*
            if (StartTime.compareTo(EndTime) < 0) {         // 0時～開始時刻～終了時刻～24時のとき
                if (time.compareTo(EndTime) < 0) {          // 予定時刻が終了時刻前
                    if (StartTime.compareTo(time) <= 0) {   // かつ、予定時刻が開始時刻後なら
                        inPeriod = true;                    // 期間内フラグを立てる
                    }
                } else {                                    // 予定時刻が終了時刻以降のとき
                                                            // 開始時刻及び終了時刻は翌日にずらして、24時～開始時刻～終了時刻にする
                    StartTime.add(Calendar.DATE, 1);
                }
            } else {                                        // 0時～終了時刻、    開始時刻～24時のとき
                if (time.compareTo(EndTime) < 0) {          // 予定時刻が終了時刻前
                                                            // 開始時刻を一日早めて、  開始時刻～0時～終了時刻にする
                    StartTime.add(Calendar.DATE, -1);
                    inPeriod = true;                        // 期間内フラグを立てる
                } else if (StartTime.compareTo(time) <= 0) {// 予定時刻が開始時刻後
                    inPeriod = true;
                }
            }
            */

            if (basedOnHour) {
                int remain = getRemainedOfMinute(StartTime, interval);
                if (remain > 0) StartTime.add(Calendar.MINUTE, interval - remain);
            }
//            Log.d("getNextAlarmTime", String.format("start[%d]=%s", i, myApplication.sdf_yyyyMMddHHmmss.format(StartTime.getTimeInMillis())));
//            Log.d("getNextAlarmTime", String.format("  end[%d]=%s", i, myApplication.sdf_yyyyMMddHHmmss.format(EndTime.getTimeInMillis())));
            // Log.d("getNextAlarmTime", String.format("inPeriod=%b", inPeriod));
            Log.d("getNextAlarmTime", String.format("untilStart=%d", untilStart));

            Calendar potentialTime = (untilStart <= 0) ? time : StartTime;   // 次回アラーム候補時刻

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
