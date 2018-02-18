package com.example.therm;

import android.app.AlarmManager;
import android.app.Application;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;

import static com.example.therm.myApplication.getClassName;
import static com.example.therm.myApplication.sdf_HHmm;

public class MainActivity extends AppCompatActivity {
    // 正時のときの鳴動間隔
    public static final int[] intervalList = {2, 3, 4, 5, 6, 10, 12, 15, 20, 30};
    // 最長鳴動間隔
    public static final int[] intervalMin = {1, intervalList[0]};
    static public String PreferencesName = "minutesRepeater";
    static int[][][] buttonsIdArray = new int[][][]
            {
                    new int[][]{
                            new int[]{R.id.TimeZone1Enable},
                            new int[]{R.id.TimeZone1StartTimeButton, R.id.TImeZone1EndTimeButton},
                            new int[]{R.id.TimeZone1StartTimeValue, R.id.TimeZone1EndTimeValue}
                    },
                    new int[][]{
                            new int[]{R.id.TimeZone2Enable},
                            new int[]{R.id.TimeZone2StartTimeButton, R.id.TImeZone2EndTimeButton},
                            new int[]{R.id.TimeZone2StartTimeValue, R.id.TImeZone2EndTimeValue}
                    }
            };
    public final int seekBarMax[] = {60 - intervalMin[0], intervalList.length - 1};
    public Timer mTimer;
    // 音関係の変数
    public minutesRepeater repeater;
    public Handler repeaterHandler;
    // 時間帯
    public AlarmManager am = null;
    //    public int[][][] zonesArray = new int[timeButtonId.length][2][2];
//    public boolean[] zonesEnable = new boolean[timeButtonId.length];
    public int seekBarProgress = 0;
    public int intervalMinutes = 2;
    public boolean BasedOnMinute_00 = false;
    public boolean executeOnBootCompleted;
    Application mApplication;
    private HandlerThread mHT;
    private Handler mHandler;
    private Runnable timerRun = null;
    // 鳴動間隔調整用ボタンのリスナー
    private View.OnClickListener intervalButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int oldValue = seekBarProgress;
            int i = view.getId();
            if (i == R.id.intervalDecrease) {
                seekBarProgress--;
                if (seekBarProgress < 0) seekBarProgress = 0;
            } else if (i == R.id.intervalIncrease) {
                seekBarProgress++;
                int idx = BasedOnMinute_00 ? 1 : 0;
                if (seekBarProgress > seekBarMax[idx]) seekBarProgress = seekBarMax[idx];
            }
            Log.d("IntervalButton", "seekBar Changed from " + oldValue + " to " + seekBarProgress);
            ((SeekBar) findViewById(R.id.intervalSeekBar)).setProgress(seekBarProgress);

            // setIntervalValue(seekBarProgress, BasedOnMinute_00);
            Log.d("intervalButton", "call AlarmSet()");
            repeater.AlarmSet(Calendar.getInstance());
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String nowDate[] = new String[]{"undefined", "", ""};
            long time = intent.getLongExtra("time", -1);

            if (time >= 0) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(time);
                //String nowDate = sdf_HHmmss.format(Calendar.getInstance().getTime());

                nowDate = timeFormat(cal);
            }
            ((TextView) findViewById(R.id.nextTime)).setText(nowDate[0]);
            ((TextView) findViewById(R.id.nextTimeOffset)).setText(nowDate[2]);
            Log.d("main:Receiver", String.format(Locale.US, "time : %s.%s %s", nowDate[0], nowDate[1], nowDate[2]));
        }
    };

    private String[] timeFormat(Calendar cal) {
        int HH = cal.get(Calendar.HOUR_OF_DAY);
        int mm = cal.get(Calendar.MINUTE);
        int ss = cal.get(Calendar.SECOND);
        int zz = cal.get(Calendar.ZONE_OFFSET);
        int SSS = cal.get(Calendar.MILLISECOND);

        boolean zzf = (zz >= 0);
        int zzm = (zz / (1000 * 60)) % 60;
        int zzh = zz / (1000 * 60 * 60);
        String time = String.format(Locale.US, "%02d:%02d:%02d", HH, mm, ss);
        String timeSSS = String.format(Locale.US, "%03d", SSS);
        String offset = String.format(Locale.US, "%c%02d%02d", (zzf ? '+' : '-'), zzh, zzm);

        return new String[]{time, timeSSS, offset};
    }

    // 初期化ブロック
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mApplication = this.getApplication();
        timerStart();

        // 自分のreceiverを登録
        HashMap<String, BroadcastReceiver> receivers = new HashMap<>();
        receivers.put("AlarmTimeChanged", receiver);

        for (Map.Entry<String, BroadcastReceiver> e : receivers.entrySet()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(e.getKey());

            LocalBroadcastManager
                    .getInstance(getApplicationContext())
                    .registerReceiver(e.getValue(), filter);
        }

        // リピーター鳴動用インスタンス初期化
        repeater = myApplication.getRepeater();

        seekBarProgress = repeater.getIntervalProgress();
        BasedOnMinute_00 = repeater.getBasedOnHour();
        executeOnBootCompleted = repeater.getExecuteOnBootCompleted();
        intervalMinutes = repeater.getIntervalValue();

        // boolean zoneEnable[]=   repeater.getZonesEnable();
        // int zoneArray[][][] =   repeater.getZonesArray();
        // フィールドの値から変数を初期化
        getFieldValues();

        myPeriodsArray timeZoneArray = new myPeriodsArray(
                buttonsIdArray,
                repeater.getZonesEnable(),
                repeater.getZonesArray()
        );

        Log.d("onCreate", "call AlarmSet()");


        repeater.AlarmSet(Calendar.getInstance());

        ((ToggleButton) findViewById(R.id.runOnBootComplete)).setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                repeater.setExecuteOnBootCompleted(b);
            }
        });

        // 正時基準かどうかの変更
        ((CheckBox) findViewById(R.id.ReferencedToTheHour))
                .setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                repeater.setBasedOnHour(b);
                BasedOnMinute_00 = b;
                Log.d("refHour", compoundButton.getText() + ":" + (compoundButton.isChecked() ? "Enable" : "Disable"));
                // 鳴動間隔のチェック状態を取得します
                int unitTime;
                // 鳴動間隔変更用シークバー
                SeekBar intervalSeek = findViewById(R.id.intervalSeekBar);

                // シークバー周りの情報を変更
                if (b) {
                    unitTime = seekBarProgress + intervalMin[0];
                    if (unitTime > intervalList[seekBarMax[1]])
                        unitTime = intervalList[seekBarMax[1]];

                    for (int i = 0; i < intervalList.length; i++) { // 分からintervalListのインデックスに変換
                        if (unitTime <= intervalList[i]) {
                            intervalSeek.setProgress(i);
                            unitTime = intervalList[i];

                            Log.d("basedOnHour",
                                    String.format(
                                            "%1$d(%2$d)->%3$d(%4$d)",
                                            seekBarProgress, seekBarProgress + intervalMin[0],
                                            i, intervalList[i]
                                    )
                            );
                            break;
                        }
                    }
                    intervalSeek.setMax(intervalList.length - 1);

                } else {
                    unitTime = intervalList[seekBarProgress];
                    Log.d("basedOnHour",
                            String.format(
                                    "%1$d(%2$d)->%3$d(%4$d)",
                                    seekBarProgress, intervalList[seekBarProgress],
                                    unitTime - intervalMin[0], unitTime
                            )
                    );

                    intervalSeek.setMax(seekBarMax[0]);
                    intervalSeek.setProgress(unitTime - intervalMin[0]);
                }

                intervalMinutes = unitTime;
                Log.d("Ref", "call AlarmSet()");
                repeater.AlarmSet(Calendar.getInstance());
            }
        });

        // 鳴動間隔シークバーのリスナー設定
        ((SeekBar) findViewById(R.id.intervalSeekBar))
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        Log.d("SeekBar", "Changed " + seekBarProgress + " to " + i);
                        repeater.setIntervalProgress(i);

                        seekBarProgress = i;
                        int unitTime = repeater.getIntervalValue();
                        Log.d("setIntervalValue", String.format("%1$d(%2$d)", seekBarProgress, unitTime));
                        ((TextView) findViewById(R.id.intervalNumber)).setText(String.valueOf(unitTime));
                        repeater.AlarmSet(Calendar.getInstance());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

        // 時刻設定ボタン群のリスナー設定
        for (int v : new int[]{R.id.intervalDecrease, R.id.intervalIncrease}
                ) {
            findViewById(v).setOnClickListener(intervalButton);
        }

        // looperを内包するスレッドを作る
        HandlerThread timerThread = new HandlerThread("timer");
        // スレッド起動
        timerThread.start();
        // 先程作ったスレッドにRunnableを送りつけられるハンドルを作る
        repeaterHandler = new Handler(timerThread.getLooper());

        //　現在時刻鳴動
        Button nowTimeButton = findViewById(R.id.nowTimeButton);
        nowTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 11);
                cal.set(Calendar.MINUTE, 59);
                new ringClass(getApplicationContext()).ring(cal);

                // AudioManagerを取得する
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (am != null) {
                    // 現在の音量を取得する
                    int ringVolume = am.getStreamVolume(minutesRepeater.myStreamId);

                    // ストリームごとの最大音量を取得する
                    int ringMaxVolume = am.getStreamMaxVolume(minutesRepeater.myStreamId);

                    // 音量を設定する（UI表示かつサウンドを再生する）
                    int flags = AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND;
                    am.setStreamVolume(minutesRepeater.myStreamId, ringVolume, flags);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(timerRun);
        // 定期実行をcancelする
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
//        repeater.releaseSound();
        repeater = null;
    }

    public void timerStart() {
        // 現在時刻表示
        final Calendar cal = Calendar.getInstance();
        mHandler = new Handler(getMainLooper());    // UIスレッドにRunnableを放り込むためのハンドラを取得

        timerRun = new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();

                // String nowDate = sdf_HHmmss.format(Calendar.getInstance().getTime());
                String nowDate[] = timeFormat(cal);
                Log.d("timer", String.format(Locale.US, "%s.%s %s", nowDate[0], nowDate[1], nowDate[2]));
                // 時刻表示をするTextView
                ((TextView) findViewById(R.id.nowTime)).setText(nowDate[0]);
                ((TextView) findViewById(R.id.nowTimeOffset)).setText(nowDate[2]);
                // 秒が変わる瞬間に自身を起動
                mHandler.postDelayed(this, 1000 - cal.get(Calendar.MILLISECOND));
            }
        };

        // 毎秒0ミリ秒のタイミングで起動（初回）
        mHandler.postDelayed(timerRun, 1000 - cal.get(Calendar.MILLISECOND));
    }

    public void getFieldValues() {
        ((ToggleButton) findViewById(R.id.runOnBootComplete)).setChecked(executeOnBootCompleted);
        ((TextView) findViewById(R.id.intervalNumber)).setText(String.valueOf(intervalMinutes));

        // 正時基準チェックボックスの値を取得
        ((CheckBox) findViewById(R.id.ReferencedToTheHour)).setChecked(BasedOnMinute_00);
//        BasedOnMinute_00 = ((CheckBox) findViewById(R.id.ReferencedToTheHour)).isChecked();

        // 時間間隔を取得
        int flag = BasedOnMinute_00 ? 1 : 0;
        if (seekBarProgress > seekBarMax[flag]) {
            seekBarProgress = seekBarMax[flag];
        }
        SeekBar isb = findViewById(R.id.intervalSeekBar);
        isb.setProgress(seekBarProgress);
        isb.setMax(seekBarMax[BasedOnMinute_00 ? 1 : 0]);
//        seekBarProgress = ((SeekBar) findViewById(R.id.intervalSeekBar)).getProgress();

    }

    /*
    public static final SimpleImmutableEntry[] timeButtonId = new SimpleImmutableEntry[]{
            new SimpleImmutableEntry<>(R.id.TimeZone1_Enable,
                    new SimpleImmutableEntry[]{
                            new SimpleImmutableEntry<>(R.id.TimeZone1_Start, R.id.TimeZone1_Start_Text),
                            new SimpleImmutableEntry<>(R.id.TImeZone1_End, R.id.TimeZone1_End_Text)
                    }
            ),
            new SimpleImmutableEntry<>(R.id.TimeZone2_Enable,
                    new SimpleImmutableEntry[]{
                            new SimpleImmutableEntry<>(R.id.TimeZone2_Start, R.id.TimeZone2_Start_Text),
                            new SimpleImmutableEntry<>(R.id.TImeZone2_End, R.id.TImeZone2_End_Text)
                    }
            )
    };
*/
    enum buttonsArrayInnerKey {
        CheckBox,
        Buttons,
        Fields
    }

    enum timeFieldEnum {
        Hour,
        Minute
    }

    enum timeIndexEnum {
        Start,
        End
    }

    // 時刻データを、ボタンやテキストビューの情報と一緒に保持するクラス
    class myTime {

        private String className = "";
        private myPeriod caller = null;
        private Button button = null;
        private TextView textView = null;
        private int timeIndex = 0;
        private int hour = 0;
        private int minute = 0;

        myTime(
                final myPeriod caller,
                final int timeIndex,
                final int buttonId,
                final int textViewId,
                final int[] time) {
            className = getClassName();
            this.caller = caller;
            this.timeIndex = timeIndex;

            button = findViewById(buttonId);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Date d = sdf_HHmm.parse((String) textView.getText());
                        // 時刻入力ダイアログの処理
                        TimePickerDialog dialog = new TimePickerDialog(MainActivity.this,
                                new TimePickerDialog.OnTimeSetListener() {
                                    @Override
                                    public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
                                        setTime(hourOfDay, minute)
                                                .getCaller()
                                                .setTime(timeIndex, hourOfDay, minute);
                                    }
                                }, d.getHours(), d.getMinutes(), true);
                        dialog.show();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            });

            textView = findViewById(textViewId);
            setTime(
                    time[timeFieldEnum.Hour.ordinal()],
                    time[timeFieldEnum.Minute.ordinal()]
            );
        }

        myTime setTime(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;

            String timeText = String.format(Locale.US, "%02d:%02d", this.hour, this.minute);
            textView.setText(timeText);
            Log.d(className, String.format(Locale.US, "time changed to %s", timeText));
            return this;
        }

        int getHour() {
            return hour;
        }

        int getMinute() {
            return minute;
        }

        myPeriod getCaller() {
            return caller;
        }
    }

    // 一つの時間帯を、チェックボックスや配下の時刻データとともに保持するクラス
    class myPeriod {
        private String className;
        private CheckBox checkBox;
        private myPeriodsArray caller;
        private int periodNumber;

        myPeriod(
                final myPeriodsArray caller,
                final int periodNumber,
                final boolean enableData,
                final int buttonsIdArray[][],
                final int timeArray[][]
        ) {
            className = myApplication.getClassName();

            this.caller = caller;
            this.periodNumber = periodNumber;

            checkBox = findViewById(buttonsIdArray[buttonsArrayInnerKey.CheckBox.ordinal()][0]);
            checkBox.setChecked(enableData);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Log.d("TimeZone", String.format("%s:%b", compoundButton.getText(), b));
                    setEnable(b).getCaller().setEnable(periodNumber, b);
                }
            });

            for (Enum<timeIndexEnum> _timeIndex : timeIndexEnum.values()) {
                int i = _timeIndex.ordinal();
                new myTime(
                        this,
                        i,
                        buttonsIdArray[buttonsArrayInnerKey.Buttons.ordinal()][i],
                        buttonsIdArray[buttonsArrayInnerKey.Fields.ordinal()][i],
                        timeArray[i]
                );
            }
        }

        myPeriodsArray getCaller() {
            return caller;
        }

        myPeriod setEnable(boolean b) {
            checkBox.setChecked(b);
            return this;
        }

        myPeriod setTime(int timeIndex, int hour, int minute) {
            caller.setTime(periodNumber, timeIndex, hour, minute);
            return this;
        }
    }

    // 複数の時間帯データを保持するクラス
    class myPeriodsArray {
        private int timeZoneTable[][][];
        private boolean enableArray[];
        private String className;

        myPeriodsArray(
                final int[][][] buttonsIdArray,
                final boolean[] enableDatas,
                final int[][][] timeZoneDatas
        ) {
            className = myApplication.getClassName();

            // データ構造
            timeZoneTable = timeZoneDatas;
            enableArray = enableDatas;

            for (int periodNumber = 0; periodNumber < buttonsIdArray.length; periodNumber++) {
                new myPeriod(
                        this,
                        periodNumber,
                        enableDatas[periodNumber],
                        buttonsIdArray[periodNumber],
                        timeZoneDatas[periodNumber]
                );
            }
        }

        myPeriodsArray setEnable(
                final int periodNumber,
                final boolean periodEnable
        ) {
            enableArray[periodNumber] = periodEnable;

            for (int i = 0; i < enableArray.length; i++) {
                Log.d(className, String.format("enableArray[%d]=%b", i, enableArray[i]));
            }
            repeater.setZonesEnable(enableArray);
            repeater.AlarmSet(Calendar.getInstance());
            return this;
        }

        myPeriodsArray setTime(int periodNumber, int timeIndex, int hour, int minute) {
            timeZoneTable[periodNumber][timeIndex] = new int[]{hour, minute};

            for (int i = 0; i < timeZoneTable.length; i++) {
                for (Enum<timeIndexEnum> _timeIndex : timeIndexEnum.values()) {
                    for (Enum<timeFieldEnum> _timeField : timeFieldEnum.values()) {
                        Log.d(className, String.format("timeZoneTable[%d][%d][%d]=%d", i, _timeIndex.ordinal(), _timeField.ordinal(),
                                timeZoneTable[i][_timeIndex.ordinal()][_timeField.ordinal()]));
                    }
                }
            }
            repeater.setZonesArray(timeZoneTable);
            repeater.AlarmSet(Calendar.getInstance());

            return this;
        }


    }
}
