package com.example.therm;

import android.app.AlarmManager;
import android.app.NotificationManager;
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
import android.util.SparseArray;
import android.util.SparseIntArray;
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
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.therm.R.id.nowTime;

public class MainActivity  extends AppCompatActivity  {
    // 正時のときの鳴動間隔
    public static final int[] intervalList = {2, 3, 4, 5, 6, 10, 12, 15, 20, 30};
    // 最長鳴動間隔
    public static final int[] intervalMin = {1, intervalList[0]};
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
    public AlarmManager am = null;

    // 時間帯
//    public int[][] zones=new int[2][2];
    public int[][][] zonesArray = new int[2][2][2];
    public boolean [] zonesEnable=new boolean[2];
    public int timeTextId[][] = {
        {
            R.id.TimeZone1_Start_Text,
            R.id.TimeZone1_End_Text
        },
        {
            R.id.TimeZone2_Start_Text,
            R.id.TImeZone2_End_Text
        }
    };
    public int timeButtonId[][] = {
        {
            R.id.TimeZone1_Start,
            R.id.TImeZone1_End
        },
        {
            R.id.TimeZone2_Start,
            R.id.TImeZone2_End
        }
    };
    public int seekBarProgress = 0;
    public int intervalMinutes = 2;
    public boolean BasedOnMinute_00 = false;
    public boolean executeOnBootCompleted;
    public SimpleDateFormat sd = new SimpleDateFormat("HH:mm", Locale.US);
    //    private Notification notification;
    NotificationManager notificationManager;
    /*
        // シークバーの値から鳴動間隔に変換
        public void setIntervalValue(int progress, boolean regulate) {
            int unitTime=repeater.getIntervalValue();
             Log.d("setIntervalValue",String.format("%1$d(%2$d)",progress,unitTime));
            ((TextView) findViewById(R.id.intervalNumber)).setText(String.valueOf(unitTime));
            intervalMinutes = unitTime;
        }
    */
    // 時間帯入力用のリスナー
    private View.OnClickListener timeButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final int id = view.getId();
            Log.d("timeButton", String.valueOf(id));


//            final HashMap<Integer, int[]> TimeZoneButtonMap=new HashMap<>();
            final SparseArray<int[]> TimeZoneButtonMap = new SparseArray<>();
            SparseIntArray Button2Text = new SparseIntArray(4);
            for (int ix = 0; ix < timeButtonId.length; ix++) {
                for (int iy = 0; iy < timeButtonId[ix].length; iy++) {
                    TimeZoneButtonMap.append(timeButtonId[ix][iy], new int[]{ix, iy});
                    Button2Text.append(timeButtonId[ix][iy], timeTextId[ix][iy]);
                }
            }

            try {
                // ボタンに対応するTextViewから設定時刻の情報を取得
                final TextView field = findViewById(Button2Text.get(id));
                Date d = sd.parse((String) field.getText());

                // 時刻入力ダイアログの処理
                TimePickerDialog dialog = new TimePickerDialog(
                        MainActivity.this,
                        new TimePickerDialog.

                                OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
                                Log.d("timeChanged", String.format("%02d:%02d", hourOfDay, minute));
                                field.setText(String.format(Locale.US, "%02d:%02d", hourOfDay, minute));
                                int idxList[] = TimeZoneButtonMap.get(id);
                                /*
                                if (zones[idxList[0]]== null) {
                                    for (int j = 0; j < zones[idxList[0]].length; j++) {
                                        zones[idxList[0]][j] = 0;
                                    }
                                }
                                */
                                if (zonesArray[idxList[0]] == null) {
                                    for (int j = 0; j < zonesArray[idxList[0]].length; j++) {
                                        zonesArray[idxList[0]][j] = new int[]{0, 0};
                                    }
                                }
                                // zones[idxList[0]][idxList[1]]=hourOfDay*60+minute;
                                zonesArray[idxList[0]][idxList[1]] = new int[]{hourOfDay, +minute};
                                repeater.setZonesArray(zonesArray);
                                Log.d("timeButton", "index0=" + idxList[0] + ",index1=" + idxList[1]);
                                Log.d("timeButton", "time:" +
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
    };
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
    // 時間帯有効化チェックボックスのリスナー
    private CheckBox.OnCheckedChangeListener timeEnableSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            int idx;

            switch (compoundButton.getId()) {
                case R.id.TimeZone1_Enable:
                    idx = 0;
                    break;
                case R.id.TimeZone2_Enable:
                    idx = 1;
                    break;
                default:
                    return;
            }
            Log.d("TimeZone", compoundButton.getText() + ":" + (b ? "Enable" : "Disable"));
            zonesEnable[idx] = b;

            for (int i = 0; i < zonesArray.length; i++) {
                for (int j = 0; j < zonesArray[i].length; j++) {
                    for (int k = 0; k < zonesArray[i][j].length; k++) {
                        Log.d("timeEnableSwitch", String.format("zoneArray[%d][%d][%d]=%d", i, j, k, zonesArray[i][j][k]));
                    }
                }
            }

            Log.d("timeEnableSwitch", "index0=" + idx);
            Log.d("timeEnableSwitch", "enable=" + String.valueOf(zonesEnable[idx]));

            repeater.setZonesEnable(zonesEnable);

            repeater.AlarmSet(Calendar.getInstance());
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("main:Reciever", "receive : " + intent.getAction());
            String message = intent.getStringExtra("Message");
            Log.d("main:Reciever", "Message : " + message);
            ((TextView) findViewById(R.id.nextTime)).setText(message);
            long time = intent.getLongExtra("time", -1);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);
            Log.d("main:Reciever", "time : " + sdf.format(time));

            am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent_ring = new Intent(getApplication(), minutesRepeat.class);
            PendingIntent sender = PendingIntent.getBroadcast(getApplication(), 0, intent_ring, PendingIntent.FLAG_CANCEL_CURRENT);
/*
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
            if (time>=0) {
                Notification notification = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher_round) // アイコン
                        .setTicker("Hello") // 通知バーに表示する簡易メッセージ
                        .setWhen(System.currentTimeMillis()) // 時間
                        .setContentTitle("ミニッツリピーター") // 展開メッセージのタイトル
                        .setContentText("次回通知時刻：" + sdf.format(time)) // 展開メッセージの詳細メッセージ
                        .setContentIntent(contentIntent) // PendingIntent
                        .build();

//            notification.flags |= Notification.FLAG_ONGOING_EVENT;
                notificationManager.notify(R.string.app_name, notification);
            } else {
                notificationManager.cancel(R.string.app_name);
            }
            */

            /*
            if (am != null && time>=0) {
                if (android.os.Build.VERSION.SDK_INT < 19) {
                    am.set(AlarmManager.RTC_WAKEUP, time, sender);
                } else {
                    am.setWindow(AlarmManager.RTC_WAKEUP,
                            time, 4 * 1000, sender);
                }
            }
*/
        }
    };

    public int getIntervals() {
        return intervalMinutes;
    }

    // 初期化ブロック
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // 自分のreceiverを登録
        IntentFilter filter = new IntentFilter();
        filter.addAction("AlarmTimeChanged");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        // リピーター鳴動用インスタンス初期化
        repeater = new minutesRepeat(this);
        repeater.loadData();

        zonesArray = repeater.getZonesArray();
        zonesEnable = repeater.getZonesEnable();
        seekBarProgress = repeater.getIntervalProgress();
        BasedOnMinute_00 = repeater.getBasedOnHour();
        executeOnBootCompleted = repeater.getExecuteOnBootCompleted();
        intervalMinutes = repeater.getIntervalValue();
/*
        {
            // Serviceを呼び出す
            Intent intent = new Intent(getApplication(), AlarmService.class);
            // Alarm の停止
            intent.putExtra("StopAlarm", true);
            startService(intent);
        }
*/
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US);

        // フィールドの値から変数を初期化
        getFieldValues();

        //       intervalNum = findViewById(R.id.intervalNumber);
        Log.d("onCreate", "call AlarmSet()");

        // UIスレッドにpost(Runnable)やsendMessage(message)を送りつけるハンドラーを作成
        mHandler = new Handler(getMainLooper());

        repeater.setHandler(mHandler);
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
//        for (int i=0;i<timeButtonId.length;i++) {
        for (int[] array : timeButtonId) {
            for (int j : array) {
                findViewById(j).setOnClickListener(timeButton);
            }
        }

        for (int key : new int[]{
                R.id.TimeZone1_Enable,
                R.id.TimeZone2_Enable
        }) {
            ((CheckBox) findViewById(key)).setOnCheckedChangeListener(timeEnableSwitch);
        }


        // looperを内包するスレッドを作る
        HandlerThread repeaterThread = new HandlerThread("rep");
        // スレッド起動
        repeaterThread.start();

        // 先程作ったスレッドにRunnableを送りつけられるハンドルを作る
        repeaterHandler = new Handler(repeaterThread.getLooper());

        //　現在時刻鳴動
        Button nowTimeButton = findViewById(R.id.nowTimeButton);
        nowTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                repeaterHandler.post(repeater);    // UIスレッドとは別スレッドでリピーターを鳴らす
                // AudioManagerを取得する
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                if (am != null) {
                    // 現在の音量を取得する
                    int ringVolume = am.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

                    // ストリームごとの最大音量を取得する
                    int ringMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);

                    // 音量を設定する（UI表示かつサウンドを再生する）
                    int flags = AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND;
                    am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, ringVolume, flags);
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
        /*
            Intent intent = new Intent(getApplication(), AlarmService.class);
            intent.putExtra("StopAlarm", true);
            stopService(intent);
*/
        /*
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
 //       notification.flags -= Notification.FLAG_ONGOING_EVENT;

        notificationManager.cancel(R.string.app_name);
        */
    }

    @org.jetbrains.annotations.Contract("false -> null")
    private Calendar [] getCalendarArray(boolean b) {
        if (b) {
            Calendar c[] = new Calendar[2];
            for (int i = 0; i < c.length; i++) {
                c[i] = Calendar.getInstance();
                c[i].set(Calendar.SECOND,0);
                c[i].set(Calendar.MILLISECOND,0);
            }
            return c;
        } else {
            return null;
        }
    }

/*
    private void AlarmSet(Calendar time) {
        // 時間帯のどれかが有効の場合
        // Serviceを呼び出す
        Intent serviceIntent = new Intent(getApplication(), AlarmService.class);

        if (time != null) {
            time.set(Calendar.SECOND, 0);
            time.set(Calendar.MILLISECOND, 0);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Log.d("AlarmSet", sdf.format(time.getTime()));
            Gson gson = new Gson();

            serviceIntent.putExtra("interval", intervalMinutes);
            serviceIntent.putExtra("zonesArray", gson.toJson(zonesArray));
            serviceIntent.putExtra("zonesEnable", zonesEnable);
            serviceIntent.putExtra("triggerTime", time);
            serviceIntent.putExtra("basedOnHour", BasedOnMinute_00);
            startService(serviceIntent);

        } else {
            // Alarm の停止を解除
            startService(serviceIntent);

        }
    }
*/

    public void getFieldValues() {
        ((ToggleButton) findViewById(R.id.runOnBootComplete)).setChecked(executeOnBootCompleted);
        ((TextView) findViewById(R.id.intervalNumber)).setText(String.valueOf(intervalMinutes));

        for (int i = 0; i < timeTextId.length; i++) {
            for (int j = 0; j < timeTextId[i].length; j++) {
                int minute = zonesArray[i][j][1];
                int hour = zonesArray[i][j][0];
                ((TextView) findViewById(timeTextId[i][j])).setText(
                        String.format(Locale.US, "%1$02d:%2$02d", hour, minute))
                ;
            }
        }

        // 正時基準チェックボックスの値を取得
        ((CheckBox) findViewById(R.id.ReferencedToTheHour)).setChecked(BasedOnMinute_00);
//        BasedOnMinute_00 = ((CheckBox) findViewById(R.id.ReferencedToTheHour)).isChecked();

        // 時間間隔を取得
        if (seekBarProgress>seekBarMax[BasedOnMinute_00 ? 1: 0]) {
            seekBarProgress=seekBarMax[BasedOnMinute_00 ? 1: 0];
        }
        ((SeekBar) findViewById(R.id.intervalSeekBar)).setProgress(seekBarProgress);
        ((SeekBar) findViewById(R.id.intervalSeekBar)).setMax(seekBarMax[BasedOnMinute_00 ? 1 : 0]);
//        seekBarProgress = ((SeekBar) findViewById(R.id.intervalSeekBar)).getProgress();
        int TimeZoneId[]={
                R.id.TimeZone1_Enable,
                R.id.TimeZone2_Enable
        };
        if (zonesEnable != null) {
            for (int i = 0; i < TimeZoneId.length; i++) {
                ((CheckBox) findViewById(TimeZoneId[i])).setChecked(zonesEnable[i]);
            }
        }
    }
}

