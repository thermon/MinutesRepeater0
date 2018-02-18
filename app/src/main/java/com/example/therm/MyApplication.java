package com.example.therm;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

import com.example.therm.myApplication.Hour_Minute;
import com.example.therm.myApplication.Start_End;
import com.google.gson.Gson;

import org.jetbrains.annotations.Contract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static com.example.therm.MainActivity.PreferencesName;
import static com.example.therm.MainActivity.buttonsIdArray;

public class myApplication extends Application {
    // 時刻表示のフォーマット
    public static SimpleDateFormat sdf_HHmm = new SimpleDateFormat("HH:mm", Locale.US);
    public static SimpleDateFormat sdf_HHmmss = new SimpleDateFormat("HH:mm:ss Z", Locale.US);
    public static SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyy.MM.dd(E) HH:mm:ss Z", Locale.JAPANESE);
    private static minutesRepeater repeater;
    Application instance = null;

    static int[] div_qr(int a, int b) {
        int m = a % b;
        return new int[]{(a - m) / b, m};
    }

    public static String getClassName() {
        return Thread.currentThread().getStackTrace()[3].getClassName().replace("$", ".");
    }

    static minutesRepeater getRepeater() {
        return repeater;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        repeater = new minutesRepeater(this);
        repeater.loadData();

        // setupLeakCanary();
    }

    public enum Hour_Minute {
        hour,
        minute
    }

    public enum Start_End {
        start,
        end
    }

    public enum Button_TextView {
        button,
        textView
    }

    public enum Checkbox_Times {
        checkbox,
        times
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
    private int[][][] zonesArray = new int[buttonsIdArray.length][][];
    private boolean[] zonesEnable = new boolean[buttonsIdArray.length];
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
        className = myApplication.getClassName();
        MainContext = context;
        sharedPreferences = MainContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
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
        zonesArray = null;
        zonesArray = gson.fromJson(sharedPreferences.getString("zonesArray", null), int[][][].class);
        zonesEnable = gson.fromJson(sharedPreferences.getString("zonesEnable", null), boolean[].class);
        intervalProgress = sharedPreferences.getInt("intervalSeekBar", 0);
        basedOnHour = sharedPreferences.getBoolean("basedOnHour", false);
        executeOnBootCompleted = sharedPreferences.getBoolean("executeOnBootCompleted", false);

        Log.d("Preferences", "load intervalSeekBar:" + intervalProgress);

        if (zonesArray == null) {
            zonesArray = new int[buttonsIdArray.length][][];
            for (int i = 0; i < zonesArray.length; i++) {
                zonesArray[i] = new int[][]{{0, 0}, {0, 0}};
            }
        }
        if (zonesEnable == null) zonesEnable = new boolean[buttonsIdArray.length];
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

    Calendar getNextAlarmTime(Calendar time) {
        int interval = getIntervalValue();
/*
        StackTraceElement[] ste = new Throwable().getStackTrace();
        for (int i = 1; i < 2; i++) {
            Log.d("getNextAlarmTime", "called:" + ste[i].getClassName() + "." + ste[i].getMethodName() +
                    ", line " + ste[i].getLineNumber() + " of " + ste[i].getFileName());
        }
*/

        Log.d("getNextAlarmTime", "now=" + myApplication.sdf_yyyyMMddHHmmss.format(time.getTimeInMillis()));
        Log.d("getNextAlarmTime", "interval=" + String.valueOf(interval));

        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);

        Calendar nextTime = time;
        // basedOnHourがtrueなら、毎時00分を基準にする
        if (basedOnHour) {
            int minute = nextTime.get(Calendar.MINUTE);
            int modulo = minute % interval;
            if (modulo > 0) {
                nextTime.add(Calendar.MINUTE, -modulo);
            }
        }
        nextTime.add(Calendar.MINUTE, interval);
        // nextTime += interval;

        Log.d("getNextAlarmTime", "next=" + myApplication.sdf_yyyyMMddHHmmss.format(nextTime.getTimeInMillis()));
        // アラーム時刻変更
        // 現在時刻と時間帯をもとにアラーム時刻を設定
        ArrayList<Calendar> alarmTime = new ArrayList<>();

        for (int i = 0; i < zonesArray.length; i++) {
            if (!zonesEnable[i]) continue;

            int zone[][] = zonesArray[i];

            Calendar times[] = new Calendar[2];
            for (int j = 0; j < times.length; j++) {
                times[j] = Calendar.getInstance();
                times[j].set(Calendar.HOUR_OF_DAY, zone[j][Hour_Minute.hour.ordinal()]);
                times[j].set(Calendar.MINUTE, zone[j][Hour_Minute.minute.ordinal()]);
                times[j].set(Calendar.SECOND, 0);
                times[j].set(Calendar.MILLISECOND, 0);
            }
            Calendar StartTime = times[Start_End.start.ordinal()];
            Calendar EndTime = times[Start_End.end.ordinal()];

            if (EndTime.compareTo(StartTime) > 0) { // 時間帯が日を跨がない場合
                if (nextTime.compareTo(EndTime) >= 0) {  // 時間帯終了後のとき
                    // 開始時刻及び終了時刻は翌日になる
                    StartTime.add(Calendar.DATE, 1);
                    EndTime.add(Calendar.DATE, 1);
                }
            } else {    // 時間帯が日を跨ぐ場合
                if (nextTime.compareTo(EndTime) < 0) {   // 予定時刻が終了時刻より前　＝　時間帯を過ぎていない
                    // 開始時刻は前日になる
                    StartTime.add(Calendar.DATE, -1);
                } else {
                    // 終了時刻は翌日
                    EndTime.add(Calendar.DATE, 1);
                }
            }

            Log.d("getNextAlarmTime", String.format("start[%d]=%s", i, myApplication.sdf_yyyyMMddHHmmss.format(StartTime.getTimeInMillis())));
            Log.d("getNextAlarmTime", String.format("  end[%d]=%s", i, myApplication.sdf_yyyyMMddHHmmss.format(EndTime.getTimeInMillis())));
            if (EndTime.compareTo(nextTime) > 0 && nextTime.compareTo(StartTime) >= 0) {
                // 次回予定時刻がタイムゾーン内にある場合
                alarmTime.add(nextTime);
                Log.d("getNextAlarmTime", String.format(" next[%d]:in =%s", i, myApplication.sdf_yyyyMMddHHmmss.format(nextTime.getTimeInMillis())));
            } else {
                // 現在時刻がタイムゾーン外の場合、次回予定時刻より遅い直近の開始時刻を探す。
                if (basedOnHour) {
                    int remain = StartTime.get(Calendar.MINUTE) % interval;
                    if (remain > 0) StartTime.add(Calendar.MINUTE, interval - remain);
                }
                alarmTime.add(StartTime);
                Log.d("getNextAlarmTime", String.format(" next[%d]:out=%s", i, myApplication.sdf_yyyyMMddHHmmss.format(StartTime.getTimeInMillis())));

            }
        }
        if (alarmTime.size() == 0) return null;

        for (Calendar t : alarmTime) {
            Log.d("getNextAlarmTime", String.format("StartTimes=%s", myApplication.sdf_yyyyMMddHHmmss.format(t.getTimeInMillis())));
        }

        Calendar Start1 = Calendar.getInstance();
        Start1.setTimeInMillis(0);

        Calendar Start2 = Calendar.getInstance();
        Start2.add(Calendar.DATE, 2);
        // 開始時刻リストでループ
        for (Calendar t : alarmTime) {
            // 開始時刻が予定時刻よりも早い && Start1が開始時刻よりも早いなら、Start1に開始時刻を入れる
            if (t.compareTo(nextTime) < 0) {
                if (Start1.compareTo(t) < 0) Start1 = t;
            }
            // 開始時刻が予定時刻よりも遅い && Start2が開始時刻よりも遅いなら、Start2に開始時刻を入れる
            else {
                if (Start2.compareTo(t) > 0) Start2 = t;
            }
        }
        // 全ての開始時刻が予定時刻よりも遅いなら、開始時刻の中で一番早い時刻を予定時刻にする
        // 予定時刻よりも早い開始時刻があれば、そのなかで一番遅い開始時刻を予定時刻にする

        final Calendar Start = (Start1.getTimeInMillis() == 0) ? Start2 : Start1;
        // Start.set(Calendar.SECOND, 0);
        // Start.set(Calendar.MILLISECOND, 0);
        return Start;
    }

    void AlarmSet(Calendar time) {
        time = getNextAlarmTime(time);
        // 時間帯のどれかが有効の場合
        // Serviceを呼び出す
        Intent serviceIntent =
                new Intent(MainContext, myService.class);
        // new Intent("service");

        if (time != null) {
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
            // Alarm の停止を解除
            MainContext.startService(serviceIntent);

        }
    }
}
