package com.example.therm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;

import org.jetbrains.annotations.Contract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static com.example.therm.MainActivity.PreferencesName;
import static com.example.therm.MainActivity.timeButtonId;

// ミニッツリピーター鳴動クラス
class minutesRepeat {

    private final static int minutesOfHour = 60;
    private final static int hoursOfDay = 24;
    private final static int minutesOfDay = hoursOfDay * minutesOfHour;


    private String className;
    private Context MainContext;
    private SharedPreferences sharedPreferences;
    private int[][][] zonesArray = new int[timeButtonId.length][][];
    private boolean[] zonesEnable = new boolean[timeButtonId.length];
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
    minutesRepeat(Context context) {
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
            zonesArray = new int[MainActivity.timeButtonId.length][][];
            for (int i = 0; i < zonesArray.length; i++) {
                zonesArray[i] = new int[][]{{0, 0}, {0, 0}};
            }
        }
        if (zonesEnable == null) zonesEnable = new boolean[MainActivity.timeButtonId.length];
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
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
        Log.d("getNextAlarmTime", "now=" + sdf2.format(time.getTime()));

        Log.d("getNextAlarmTime", "interval=" + String.valueOf(interval));

        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);


        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);

        int nextTime = hour * minutesOfHour + minute;
        // basedOnHourがtrueなら、毎時00分を基準にする
        // nextTime += interval - (basedOnHour ? (minute % interval) :0);

//        if (basedOnHour) nextTime-= minute % interval;
        if (basedOnHour) {
            int modulo=((nextTime % minutesOfHour) % interval);
            if (modulo>0) {
                nextTime -= ((nextTime % minutesOfHour) % interval);
            }
        }
        nextTime += interval;

        Log.d("getNextAlarmTime", "next=" + timeFormat(nextTime));
        // アラーム時刻変更
        // 現在時刻と時間帯をもとにアラーム時刻を設定
        ArrayList<Integer> alarmTime = new ArrayList<>();

        for (int i = 0; i < zonesArray.length; i++) {
            if (!zonesEnable[i]) continue;

            int zone[][] = zonesArray[i];
            int StartTime = makeTime(zone[0]);
            int EndTime   = makeTime(zone[1]);

            if (EndTime > StartTime) { // 時間帯が日を跨がない場合
                if (nextTime >= EndTime) {  // 時間帯終了後のとき
                    // 開始時刻及び終了時刻は翌日になる
                    StartTime += minutesOfDay;
                    EndTime += minutesOfDay;
                }
            } else {    // 時間帯が日を跨ぐ場合
                if (nextTime < EndTime) {   // 予定時刻が終了時刻より前　＝　時間帯を過ぎていない
                    // 開始時刻は前日になる
                    StartTime -= minutesOfDay;
                } else {
                    // 終了時刻は翌日
                    EndTime += minutesOfDay;
                }
            }

            Log.d("getNextAlarmTime", String.format("start[%d]=%s", i, timeFormat(StartTime)));
            Log.d("getNextAlarmTime", String.format("  end[%d]=%s", i, timeFormat(EndTime)));
            if (EndTime > nextTime && nextTime >= StartTime) {
                // 次回予定時刻がタイムゾーン内にある場合
                alarmTime.add(nextTime);
                Log.d("getNextAlarmTime", String.format(" next[%d]:in =%s", i, timeFormat(nextTime)));
            } else {
                // 現在時刻がタイムゾーン外の場合、次回予定時刻より遅い直近の開始時刻を探す。
                if (basedOnHour) {
                    int remain= ((StartTime % minutesOfHour ) % interval);
                    if (remain>0) StartTime += interval -remain;
                }
                alarmTime.add(StartTime);
                Log.d("getNextAlarmTime", String.format(" next[%d]:out=%s", i, timeFormat(StartTime)));

            }
        }
        if (alarmTime.size() == 0) return null;

        for (int t : alarmTime) {
            Log.d("getNextAlarmTime", String.format("StartTimes=%s", timeFormat(t)));
        }

        int Start1 = -1;
        int Start2 = 2 * minutesOfDay;
        // 開始時刻リストでループ
        for (int t : alarmTime) {
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

        minute = Start % minutesOfHour;
        hour = (Start - minute) / minutesOfHour;
        int day = (hour >= hoursOfDay) ? 1 : 0;
        hour -= day * hoursOfDay;

        Log.d("getNextAlarmTime", String.format("next=%1$02d:%2$02d", hour, minute));
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.add(Calendar.DATE, day);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
/*
        int offsetMilli=0;
        int count[]=div_qr(minute,15);
        offsetMilli+=hour*waitArray[0];
        for (int i=0;i<2;i++) {
            if (count[i] > 0) offsetMilli += count[i] * waitArray[i + 1] + wait1;
        }
        // cal.add(Calendar.MILLISECOND,-offsetMilli);
*/
        return cal;
    }

    private String timeFormat(int time) {
        int day = 0;
        while (time < 0) {
            time += minutesOfDay;
            day--;
        }
        while (time >= minutesOfDay) {
            time -= minutesOfDay;
            day++;
        }
        int minute = time % minutesOfHour;
        int hour = (time - minute) / minutesOfHour;
        return String.format(Locale.US, "%+d %02d:%02d", day, hour, minute);
    }

    void AlarmSet(Calendar time) {
        time = getNextAlarmTime(time);
        // 時間帯のどれかが有効の場合
        // Serviceを呼び出す
        Intent serviceIntent =
                new Intent(MainContext, AlarmService.class);
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

