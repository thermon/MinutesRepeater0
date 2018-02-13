package com.example.therm;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.therm.R.id.nowTime;

public class MainActivity  extends AppCompatActivity  {
    // 正時のときの鳴動間隔
    public static final int[] intervalList = {2, 3, 4, 5, 6, 10, 12, 15, 20, 30};
    // 最長鳴動間隔
    public static final int[] intervalMin = {1, intervalList[0]};
    public static final int timeButtonId[][][] = new int[][][]
            {
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
    public final int seekBarMax[] = {60-intervalMin[0], intervalList.length - 1};
    // 時刻表示のフォーマット
    final SimpleDateFormat mSimpleDataFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
    public Timer mTimer;
    public Timer mTimer2;
    public Handler mHandler;
    // 音関係の変数
    public minutesRepeat repeater;
    public Handler repeaterHandler;

    // 時間帯
    public AlarmManager am = null;
      public int[][][] zonesArray= new int[timeButtonId.length][2][2];
    public boolean [] zonesEnable= new boolean[timeButtonId.length];
    public int seekBarProgress = 0;
    public int intervalMinutes = 2;
    public boolean BasedOnMinute_00 = false;
    public boolean executeOnBootCompleted;
    public SimpleDateFormat sd = new SimpleDateFormat("HH:mm", Locale.US);
    private myTimeZoneUI timeButtonIdClass[];
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
        new myApplication();

//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        timeButtonIdClass = new myTimeZoneUI[timeButtonId.length];

        for (int i=0;i<timeButtonId.length;i++) {
            timeButtonIdClass[i]=new myTimeZoneUI(
                    timeButtonId[i][0][0],
                    timeButtonId[i][1],
                    timeButtonId[i][2]
            );
            timeButtonIdClass[i].zoneIndex=i;
        }

        // 自分のreceiverを登録
        HashMap<String,BroadcastReceiver> receivers=new HashMap<>();
        receivers.put("AlarmTimeChanged",receiver);
//        receivers.put("ring",ringReceiver);

        for (Map.Entry<String,BroadcastReceiver> e:receivers.entrySet()){
            IntentFilter filter = new IntentFilter();
            filter.addAction(e.getKey());
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(e.getValue(), filter);
        }

        // リピーター鳴動用インスタンス初期化
        repeater = new minutesRepeat(this);
        repeater.loadData();

        zonesArray = repeater.getZonesArray();
        zonesEnable = repeater.getZonesEnable();
        seekBarProgress = repeater.getIntervalProgress();
        BasedOnMinute_00 = repeater.getBasedOnHour();
        executeOnBootCompleted = repeater.getExecuteOnBootCompleted();
        intervalMinutes = repeater.getIntervalValue();

        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);

        // フィールドの値から変数を初期化
        getFieldValues();

        //       intervalNum = findViewById(R.id.intervalNumber);
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
        ((CheckBox) findViewById(R.id.ReferencedToTheHour)).setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
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
                    if (unitTime > intervalList[seekBarMax[1]]) unitTime = intervalList[seekBarMax[1]];

                    for (int i = 0; i < intervalList.length; i++) { // 分からintervalListのインデックスに変換
                        if (unitTime <= intervalList[i]) {
                            intervalSeek.setProgress(i);
                            unitTime = intervalList[i];
                            Log.d("basedOnHour",
                                    String.format("%1$d(%2$d)->%3$d(%4$d)",
                                            seekBarProgress,seekBarProgress+intervalMin[0],
                                            i,intervalList[i]));
                            break;
                        }
                    }
                    intervalSeek.setMax(intervalList.length - 1);

                } else {
                    unitTime = intervalList[seekBarProgress];
                    Log.d("basedOnHour",
                    String.format("%1$d(%2$d)->%3$d(%4$d)",
                        seekBarProgress,intervalList[seekBarProgress],
                            unitTime - intervalMin[0],unitTime)
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
                        Log.d("SeekBar", "Changed "+ seekBarProgress+" to "+i);
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
                repeater.ring();

                // AudioManagerを取得する
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (am != null) {
                    // 現在の音量を取得する
                    int ringVolume = am.getStreamVolume(minutesRepeat.myStreamId);

                    // ストリームごとの最大音量を取得する
                    int ringMaxVolume = am.getStreamMaxVolume(minutesRepeat.myStreamId);

                    // 音量を設定する（UI表示かつサウンドを再生する）
                    int flags = AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND;
                    am.setStreamVolume(minutesRepeat.myStreamId, ringVolume, flags);
                }
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

        for (int i = 0; i < timeButtonIdClass.length; i++) {
            myTimeZoneUI tz=timeButtonIdClass[i];
            for (int j = 0; j < tz.time.length; j++) {
                int minute  = zonesArray[i][j][1];
                int hour    = zonesArray[i][j][0];
                tz.time[j].text.setText(
                        String.format(Locale.US, "%1$02d:%2$02d", hour, minute))
                ;
            }
        }

        // 正時基準チェックボックスの値を取得
        ((CheckBox) findViewById(R.id.ReferencedToTheHour)).setChecked(BasedOnMinute_00);
//        BasedOnMinute_00 = ((CheckBox) findViewById(R.id.ReferencedToTheHour)).isChecked();

        // 時間間隔を取得
        int flag=BasedOnMinute_00 ? 1: 0;
        if (seekBarProgress>seekBarMax[flag]) {
            seekBarProgress=seekBarMax[flag];
        }
        ((SeekBar) findViewById(R.id.intervalSeekBar)).setProgress(seekBarProgress);
        ((SeekBar) findViewById(R.id.intervalSeekBar)).setMax(seekBarMax[BasedOnMinute_00 ? 1 : 0]);
//        seekBarProgress = ((SeekBar) findViewById(R.id.intervalSeekBar)).getProgress();

        if (zonesEnable != null) {
            for (int i = 0; i <timeButtonIdClass.length; i++) {
                timeButtonIdClass[i].checkBox.setChecked(zonesEnable[i]);
            }
        }
    }

    class myTimeZoneUI {
        private static final String className = "myTimeZoneUI";
        int zoneIndex = 0;
        private CheckBox checkBox;
        private myTimeUI time[]=null;

        myTimeZoneUI(int checkBoxId,int start[],int end[] ){

            checkBox=findViewById(checkBoxId);
            time=new myTimeUI[]{
                    new myTimeUI(start[0], start[1]),
                    new myTimeUI(end[0], end[1])
            };
            for (int i=0;i<time.length;i++) {
                time[i].timeIndex=i;
            }

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    Log.d("TimeZone", compoundButton.getText() + ":" + (b ? "Enable" : "Disable"));
                    zonesEnable[zoneIndex] = b;

                    for (int i = 0; i < zonesArray.length; i++) {
                        for (int j = 0; j < zonesArray[i].length; j++) {
                            for (int k = 0; k < zonesArray[i][j].length; k++) {
                                Log.d(className, String.format("zoneArray[%d][%d][%d]=%d", i, j, k, zonesArray[i][j][k]));
                            }
                        }
                    }

                    Log.d(className, "index0=" + zoneIndex);
                    Log.d(className, "enable=" + String.valueOf(zonesEnable[zoneIndex]));

                    repeater.setZonesEnable(zonesEnable);

                    repeater.AlarmSet(Calendar.getInstance());
                }
            });
        }

        class myTimeUI {
            private static final String className = "myTImeUI";
            private Button button=null;
            private TextView text=null;
            private int timeIndex=0;

            myTimeUI(int  buttonId, int textViewId) {
                button=findViewById(buttonId);
                text=findViewById(textViewId);

                // 時間帯入力用のリスナー
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            Date d = sd.parse((String) text.getText());

                            // 時刻入力ダイアログの処理
                            TimePickerDialog dialog = new TimePickerDialog(
                                    MainActivity.this,
                                    new TimePickerDialog.

                                            OnTimeSetListener() {
                                        @Override
                                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
                                            Log.d(className, String.format("time changed to %02d:%02d", hourOfDay, minute));
                                            text.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));

                                            if (zonesArray[zoneIndex] == null) {
                                                for (int j = 0; j < zonesArray[zoneIndex].length; j++) {
                                                    zonesArray[zoneIndex][j] = new int[]{0, 0};
                                                }
                                            }
                                            // zones[idxList[0]][idxList[1]]=hourOfDay*60+minute;
                                            zonesArray[zoneIndex][timeIndex] = new int[]{hourOfDay, +minute};
                                            repeater.setZonesArray(zonesArray);
                                            Log.d(className, "index0=" + zoneIndex + ",index1=" + timeIndex);
                                            Log.d(className, "time:" +
                                                    String.format("%1$02d:%2$02d", hourOfDay, minute));
                                            repeater.AlarmSet(Calendar.getInstance());
                                        }
                                    },
                                    d.getHours(), d.getMinutes(), true);
                            dialog.show();

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        }
    }
}

