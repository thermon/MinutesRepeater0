package com.example.therm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.therm.R.id.nowTime;
import static com.example.therm.myApplication.getClassName;

public class MainActivity extends AppCompatActivity {
    // 正時のときの鳴動間隔
    public static final int[] intervalList = {2, 3, 4, 5, 6, 10, 12, 15, 20, 30};
    // 最長鳴動間隔
    public static final int[] intervalMin = {1, intervalList[0]};
    public static final int timeButtonId[][][] = new int[][][]{
            {
                    {R.id.TimeZone1_Enable},
                    {R.id.TimeZone1_Start, R.id.TimeZone1_Start_Text},
                    {R.id.TImeZone1_End, R.id.TimeZone1_End_Text}
            },
            {
                    {R.id.TimeZone2_Enable},
                    {R.id.TimeZone2_Start, R.id.TimeZone2_Start_Text},
                    {R.id.TImeZone2_End, R.id.TImeZone2_End_Text}
            }
    };
    static public String PreferencesName = "minutesRepeater";
    public final int seekBarMax[] = {60 - intervalMin[0], intervalList.length - 1};
    // 時刻表示のフォーマット
    final SimpleDateFormat mSimpleDataFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
    public Timer mTimer;
    public Handler mHandler;
    // 音関係の変数
    public minutesRepeat repeater;
    public Handler repeaterHandler;

    // 時間帯
    public AlarmManager am = null;
    //    public int[][][] zonesArray = new int[timeButtonId.length][2][2];
//    public boolean[] zonesEnable = new boolean[timeButtonId.length];
    public int seekBarProgress = 0;
    public int intervalMinutes = 2;
    public boolean BasedOnMinute_00 = false;
    public boolean executeOnBootCompleted;
    public SimpleDateFormat sd = new SimpleDateFormat("HH:mm", Locale.US);
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
            Log.d("main:Receiver", "receive : " + intent.getAction());
            String message = intent.getStringExtra("Message");
            Log.d("main:Receiver", "Message : " + message);
            ((TextView) findViewById(R.id.nextTime)).setText(message);
            long time = intent.getLongExtra("time", -1);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
            Log.d("main:Receiver", "time : " + sdf.format(time));

            am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent_ring = new Intent(getApplication(), minutesRepeat.class);
            PendingIntent sender = PendingIntent.getBroadcast(getApplication(), 0, intent_ring, PendingIntent.FLAG_CANCEL_CURRENT);

        }
    };

    // 初期化ブロック
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

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
        repeater = new minutesRepeat(this);
        repeater.loadData();

        seekBarProgress = repeater.getIntervalProgress();
        BasedOnMinute_00 = repeater.getBasedOnHour();
        executeOnBootCompleted = repeater.getExecuteOnBootCompleted();
        intervalMinutes = repeater.getIntervalValue();

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);

        // boolean zoneEnable[]=   repeater.getZonesEnable();
        // int zoneArray[][][] =   repeater.getZonesArray();
        // フィールドの値から変数を初期化
        getFieldValues();

        myTimeZoneArray timeZoneArray = new myTimeZoneArray(
                repeater.getZonesEnable(),
                repeater.getZonesArray(),
                timeButtonId
        );

        Log.d("onCreate", "call AlarmSet()");

        // UIスレッドにpost(Runnable)やsendMessage(message)を送りつけるハンドラーを作成
        mHandler = new Handler(getMainLooper());
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
                new ringAlarm(getApplicationContext());
            }
        });

        // 現在時刻表示
        mTimer = new Timer();        // 一秒ごとに定期的に実行します。

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    public void run() {
                        Calendar calendar = Calendar.getInstance();
                        String nowDate = mSimpleDataFormat.format(calendar.getTime());
                        // 時刻表示をするTextView
                        ((TextView) findViewById(nowTime)).setText(nowDate);

                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 定期実行をcancelする
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
//        repeater.releaseSound();
        repeater = null;
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

    class myTimeZoneArray {
        //private myTimeZoneUI zones[]=new myTimeZoneUI[timeButtonId.length];
        private ArrayList<myTimeZoneUI> zonesList = new ArrayList<>(timeButtonId.length);
        private int timeZoneTable[][][];
        private boolean enableArray[];
        private String className;

        myTimeZoneArray(boolean enable[], int timeZone[][][], int timeButtonArray[][][]) {
            className = myApplication.getClassName();
            timeZoneTable = timeZone;
            enableArray = enable;

            for (int i = 0; i < timeButtonId.length; i++) {
                zonesList.add(new myTimeZoneUI(this, enable[i], timeZone[i], timeButtonArray[i]));
            }
        }

        void pushTimeZone(myTimeZoneUI timeZoneUI) {
            int index = zonesList.indexOf(timeZoneUI);
            if (index >= 0) timeZoneTable[index] = timeZoneUI.timeZone;

            for (int i = 0; i < timeZoneTable.length; i++) {
                for (int j = 0; j < timeZoneTable[i].length; j++) {
                    for (int k = 0; k < timeZoneTable[i][j].length; k++) {
                        Log.d(className, String.format("timeZoneTable[%d][%d][%d]=%d", i, j, k, timeZoneTable[i][j][k]));
                    }
                }
            }
            repeater.setZonesArray(timeZoneTable);
            repeater.AlarmSet(Calendar.getInstance());
        }

        void pushEnable(myTimeZoneUI timeZoneUI) {
            int index = zonesList.indexOf(timeZoneUI);
            if (index >= 0) enableArray[index] = timeZoneUI.enable;

            for (int i = 0; i < enableArray.length; i++) {
                Log.d(className, String.format("enableArray[%d]=%b", i, enableArray[i]));
            }
            repeater.setZonesEnable(enableArray);
            repeater.AlarmSet(Calendar.getInstance());
        }

        class myTimeZoneUI {
            private String className;
            private CheckBox checkBox;
            //private myTimeUI timeUI[] = new myTimeUI[2];
            private ArrayList<myTimeUI> timeUIList = new ArrayList<>(2);
            private int timeZone[][];
            private myTimeZoneArray folderClass;
            private boolean enable;

            myTimeZoneUI(final myTimeZoneArray parent, boolean enableData, int timeZoneData[][], int timeButtonArray[][]) {
                final myTimeZoneUI self = this;
                folderClass = parent;
                className = myApplication.getClassName();

                enable = enableData;
                timeZone = timeZoneData;

                for (int i = 0; i < timeZone.length; i++) {
                    timeUIList.add(new myTimeUI(this, timeZone[i], timeButtonArray[i + 1]));
                }

                checkBox = findViewById(timeButtonArray[0][0]);
                checkBox.setChecked(enableData);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        Log.d("TimeZone", String.format("%s:%b", compoundButton.getText(), b));
                        enable = b;
                        folderClass.pushEnable(self);
                    }
                });
            }

            private void pushTime(myTimeUI timeUI) {
                int index = timeUIList.indexOf(timeUI);
                if (index >= 0) timeZone[index] = timeUI.time;
                folderClass.pushTimeZone(this);
            }

            class myTimeUI {
                private String className = "";
                private Button button = null;
                private TextView text = null;
                View.OnClickListener buttonListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Date d = sd.parse((String) text.getText());
                            // 時刻入力ダイアログの処理
                            TimePickerDialog dialog = new TimePickerDialog(MainActivity.this, timeListener, d.getHours(), d.getMinutes(), true);
                            dialog.show();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                };
                private myTimeZoneUI folderClass;
                private int time[];
                private myTimeUI self;
                TimePickerDialog.OnTimeSetListener timeListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
                        String timeText = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);

                        Log.d(className, String.format(Locale.US, "time changed to %s", timeText));
                        text.setText(timeText);
                        time = new int[]{hourOfDay, minute};

                        // folderClass.pushTime(folderClassIndex, new int[]{hourOfDay, minute});
                        folderClass.pushTime(self);
                    }
                };

                myTimeUI(final myTimeZoneUI parent, int hourAndMinute[], int ids[]) {
                    self = this;
                    folderClass = parent;
                    className = getClassName();

                    time = hourAndMinute;

                    button = findViewById(ids[0]);
                    button.setOnClickListener(buttonListener);

                    text = findViewById(ids[1]);
                    text.setText(String.format(Locale.US, "%02d:%02d", time[0], time[1]));
                }

            }
        }
    }
}