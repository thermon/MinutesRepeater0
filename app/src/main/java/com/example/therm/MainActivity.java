package com.example.therm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.PowerManager;
import android.sax.StartElementListener;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.Serializable;
import java.nio.channels.SeekableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.example.therm.R.id.checkBox;
import static com.example.therm.R.id.nowTime;
import static com.example.therm.R.id.time;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class MainActivity  extends AppCompatActivity {
    public Timer mTimer;
    public Handler mHandler;
    public boolean AlarmServe = false;

    // 正時のときの鳴動間隔
    public final int intervalList[] = {2, 3, 4, 5, 6, 10, 12, 15, 20, 30};
    // 最長鳴動間隔
    public final int intervalMax[] = {60, intervalList[intervalList.length - 1]};
    public final int intervalMin[] = {2, intervalList[0]};


    // 音関係の変数
    public minutesRepeat repeater;

    // 時刻表示のフォーマット
    final SimpleDateFormat mSimpleDataFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public Handler repeaterHandler;
    public AlarmManager am = null;

    // 時間帯
    public boolean ZoneEnable[] = {false, false};
    public int timeFieldId[] = {
            R.id.TimeZone1_Start_Text,
            R.id.TimeZone1_End_Text,
            R.id.TimeZone2_Start_Text,
            R.id.TImeZone2_End_Text
    };
    public int timeButtonId[] = {
            R.id.TimeZone1_Start,
            R.id.TImeZone1_End,
            R.id.TimeZone2_Start,
            R.id.TImeZone2_End
    };
    public int timeArray[] = {0, 0, 0, 0};
    public int seekBarProgress = 0;
    public int intervalMinutes = 2;
    public boolean BasedOnMinute_00 = false;
    public SimpleDateFormat sd = new SimpleDateFormat("HH:mm");

    public int getIntervals() {
        return intervalMinutes;
    }

    private void getFieldValues() {
        // 時間帯チェックボックス
        ZoneEnable[0] = ((CheckBox) findViewById(R.id.TimeZone1_Enable)).isChecked();
        ZoneEnable[1] = ((CheckBox) findViewById(R.id.TimeZone2_Enable)).isChecked();

        // 時間情報を取得
        for (int i = 0; i < timeFieldId.length; i++) {
            int id = timeFieldId[i];
            TextView Field = findViewById(id);
            try {
                // TextViewから設定時刻の情報を取得
                Date d = sd.parse((String) Field.getText());
                timeArray[i] = d.getHours() * 60 + d.getMinutes();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // 時間間隔を取得
        seekBarProgress = ((SeekBar) findViewById(R.id.intervalSeekBar)).getProgress();

        // 正時基準チェックボックスの値を取得
        BasedOnMinute_00 = ((CheckBox) findViewById(R.id.ReferencedToTheHour)).isChecked();

    }

    // シークバーの値から鳴動間隔に変換
    public void setIntervalValue(int progress, boolean regulate) {
        int unitTime;
        if (regulate) {
            // 鳴動時刻を00分基準とした場合
            unitTime = seekBarProgress + intervalMin[1];
            if (unitTime > intervalMax[1]) unitTime = intervalMax[1];

            for (int i = 0; i < intervalList.length; i++) { // 分からintervalListのインデックスに変換
                if (unitTime <= intervalList[i]) {
                    unitTime = intervalList[i];
                    break;
                }
            }
        } else {
            unitTime = seekBarProgress + intervalMin[0];
        }
        ((TextView) findViewById(R.id.intervalNumber)).setText(String.valueOf(unitTime));
        intervalMinutes = unitTime;
    }

    // 初期化ブロック
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round) // アイコン
                .setTicker("Hello") // 通知バーに表示する簡易メッセージ
                .setWhen(System.currentTimeMillis()) // 時間
                .setContentTitle("My notification") // 展開メッセージのタイトル
                .setContentText("Hello Notification!!") // 展開メッセージの詳細メッセージ
                .setContentIntent(contentIntent) // PendingIntent
                .build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        notificationManager.notify(1, notification);

        // フィールドの値から変数を初期化
        getFieldValues();
        setIntervalValue(seekBarProgress, BasedOnMinute_00);

        //       intervalNum = findViewById(R.id.intervalNumber);
        Log.d("onCreate", "call AlarmSet()");
        AlarmSet();


        // 正時基準かどうかの変更
        ((CheckBox) findViewById(R.id.ReferencedToTheHour)).setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                BasedOnMinute_00 = b;
                Log.d("refHour", compoundButton.getText() + ":" + (compoundButton.isChecked() ? "Enable" : "Disable"));
                // 鳴動間隔のチェック状態を取得します
                int unitTime;
                // 鳴動間隔変更用シークバー
                SeekBar intervalSeek = findViewById(R.id.intervalSeekBar);

                // シークバー周りの情報を変更
                if (b) {
                    unitTime = seekBarProgress + intervalMin[0];
                    if (unitTime > intervalMax[1]) unitTime = intervalMax[1];

                    for (int i = 0; i < intervalList.length; i++) { // 分からintervalListのインデックスに変換
                        if (unitTime <= intervalList[i]) {
                            intervalSeek.setProgress(i);
                            unitTime = intervalList[i];
                            break;
                        }
                    }
                    intervalSeek.setMax(intervalList.length - 1);

                } else {
                    unitTime = intervalList[seekBarProgress];
                    intervalSeek.setMax(intervalMax[0] - intervalMin[0]);
                    intervalSeek.setProgress(unitTime - intervalMin[0]);
                }
                intervalMinutes = unitTime;
                Log.d("Ref", "call AlarmSet()");
                AlarmSet();
            }
        });

        // 鳴動間隔シークバーのリスナー設定
        ((SeekBar) findViewById(R.id.intervalSeekBar))
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        Log.d("SeekBar", "Changed");
                        seekBarProgress = i;
                        setIntervalValue(i, BasedOnMinute_00);
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
        for (int key : new int[]{
                R.id.TimeZone1_Start,
                R.id.TimeZone2_Start,
                R.id.TImeZone1_End,
                R.id.TImeZone2_End

        }) {
            findViewById(key).setOnClickListener(timeButton);
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

        // リピーター鳴動用インスタンス初期化
        repeater = new minutesRepeat(this);

        //　現在時刻鳴動
        Button nowTimeButton = findViewById(R.id.nowTimeButton);
        nowTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                repeaterHandler.post(repeater);    // UIスレッドとは別スレッドでリピーターを鳴らす
            }
        });


        // 現在時刻表示
        // UIスレッドにpost(Runnable)やsendMessage(message)を送りつけるハンドラーを作成
        mHandler = new Handler(getMainLooper());
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
        //リピーター有効化スイッチのリスナー設定
        /*
        Switch repeaterEnable = findViewById(R.id.repeaterEnable);
        repeaterEnable.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d("TimeZone", compoundButton.getText() + ":" + (b ? "Enable" : "Disable"));

                int id=compoundButton.getId();
                switch (id) {
                    case R.id.TimeZone1_Enable:
                        ZoneEnable[0]=b;
                        break;
                    case R.id.TimeZone2_Enable:
                        ZoneEnable[1]=b;
                        break;
                }

                Log.d("TimeZone", "bx");
                boolean Enable=false;
                for  (int bx:new int [] {0,1}) {
                    if (ZoneEnable[bx]) {
                        Enable=true;
                        Log.d("TimeZone", "bx=true");
                        break;
                    } else {
                        Log.d("TimeZone", "bx=false");

                    }
                }

                if (Enable) {
                    Log.d("TimeZone", "true");
                    // アラーム設定
                    AlarmSet();
                } else {
                    // アラーム解除
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    if (am != null) {
                        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
                        PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
                        am.cancel(sender);
                    }

                }
            }
        });
        */
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 定期実行をcancelする
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        // 時間帯のどれかが有効の場合
        Intent intent = new Intent(MainActivity.this,AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        // アラーム設定
        am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am!=null) {
            am.cancel(sender);
        }
    }


    // 時間帯入力用のリスナー
    private View.OnClickListener timeButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final int i = view.getId();
            Log.d("timeButton", String.valueOf(i));
//            int time=timeArray[TimeZoneMap.get(i)];
            int minute1;
            int hour1;
//            minute1=time % 60;
//            hour1= (time-minute1) /60;

            final SparseIntArray TimeZoneMap = new SparseIntArray(4);
            TimeZoneMap.append(R.id.TimeZone1_Start, 0);
            TimeZoneMap.append(R.id.TImeZone1_End, 1);
            TimeZoneMap.append(R.id.TimeZone2_Start, 2);
            TimeZoneMap.append(R.id.TImeZone2_End, 3);

            SparseIntArray Button2Text = new SparseIntArray(4);
            Button2Text.append(R.id.TimeZone1_Start, R.id.TimeZone1_Start_Text);
            Button2Text.append(R.id.TImeZone1_End, R.id.TimeZone1_End_Text);
            Button2Text.append(R.id.TimeZone2_Start, R.id.TimeZone2_Start_Text);
            Button2Text.append(R.id.TImeZone2_End, R.id.TImeZone2_End_Text);

            SimpleDateFormat sd = new SimpleDateFormat("HH:mm");

            try {
                // ボタンに対応するTextViewから設定時刻の情報を取得
                final TextView field = findViewById(Button2Text.get(i));
                Date d = sd.parse((String) field.getText());

                // 時刻入力ダイアログの処理
                TimePickerDialog dialog = new TimePickerDialog(
                        MainActivity.this,
                        new TimePickerDialog.

                                OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                Log.d("timeChanged", String.format("%02d:%02d", hourOfDay, minute));
                                field.setText(String.format("%02d:%02d", hourOfDay, minute));
                                timeArray[TimeZoneMap.get(i)] = hourOfDay * 60 + minute;
                                Log.d("Time", "call AlarmSet()");
                                AlarmSet();
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
            Log.d("IntervalButton", "Clicked");

            int i = view.getId();
            if (i == R.id.intervalDecrease) {
                seekBarProgress--;
            } else if (i == R.id.intervalIncrease) {
                seekBarProgress++;
            }
            ((SeekBar) findViewById(R.id.intervalSeekBar)).setProgress(seekBarProgress);

            setIntervalValue(seekBarProgress, BasedOnMinute_00);
            Log.d("intervalButton", "call AlarmSet()");
            AlarmSet();
        }
    };


    // 時間帯有効化チェックボックスのリスナー
    private CheckBox.OnCheckedChangeListener timeEnableSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()) {
                case R.id.TimeZone1_Enable:
                    ZoneEnable[0] = b;
                    break;
                case R.id.TimeZone2_Enable:
                    ZoneEnable[1] = b;
                    break;
            }
            Log.d("TimeZone", compoundButton.getText() + ":" + (b ? "Enable" : "Disable"));
            AlarmSet();
        }
    };

    private void AlarmSet() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int next = (hour * 60 + minute) % (24 * 60);
        Log.d("AlarmSet", "next:" + String.valueOf(next % 60));

        if (BasedOnMinute_00) {
            int next2 = next % intervalMinutes;
            next += intervalMinutes - next2;
        } else {
            next += intervalMinutes;
        }
        Log.d("AlarmSet", "next2:" + String.valueOf(next % 60));

        ArrayList<Integer> AlarmTime = new ArrayList<Integer>();

        boolean Enable = false;
        for (boolean bx : ZoneEnable) {
            if (bx) {
                Enable = true;
                Log.d("TimeZone", "bx=true");
                break;
            } else {
                Log.d("TimeZone", "bx=false");

            }
        }
        // 時間帯のどれかが有効の場合
        Intent intent = new Intent(MainActivity.this,AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        // アラーム設定

        am = (AlarmManager) getSystemService(ALARM_SERVICE);
        AlarmServe = false;
        if (Enable) {
            Log.d("AlarmTime", "Alarm Enabled");
            // アラーム時刻変更
            // 現在時刻と時間帯をもとにアラーム時刻を設定

            for (int index = 0; index < ZoneEnable.length; index++) {
                int StartTimeIndex = index * 2;
                int EndTimeIndex = StartTimeIndex + 1;

                if (ZoneEnable[index]) {
                    int StartTime = timeArray[StartTimeIndex];
                    int EndTime = timeArray[EndTimeIndex];
                    if (StartTime > EndTime) {
                        // 開始時刻より終了時刻が早い＝翌日とみなす
                        EndTime += 24 * 60;
                    }
                    if (EndTime > next && next > StartTime) {
                        // 次回予定時刻がタイムゾーン内にある場合、次回予定時刻より早い直近の開始時間を探す
                        AlarmTime.add(next);
                    } else {
                        // 現在時刻がタイムゾーン外の場合、次回予定時刻より遅い直近の開始時刻を探す。
                        if (StartTime < next) {
                            StartTime += 24 * 60;
                        }
                        AlarmTime.add(StartTime);
                    }
                }
            }
            int Start1 = -1;
            int Start2 = 2 * 24 * 60;
            for (int t : AlarmTime) {
                if (t < next) {
                    if (Start1 < t) Start1 = t;
                } else {
                    if (Start2 > t) Start2 = t;
                }
            }
            int Start = (Start1 == -1) ? Start2 : Start1;
            minute = Start % 60;
            hour = (Start - minute) / 60;
            int day = (hour >= 24) ? 1 : 0;
            hour %= 24;

            cal.add(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long mi = cal.getTimeInMillis();
//                 mi -= mi % (1000 * 60); // 秒を0にする
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Log.d("AlarmTime", sdf.format(cal.getTime()));


            String time = String.format(Locale.US, "%1$02d:%2$02d", hour, minute);
            ((TextView) findViewById(R.id.nextTime)).setText(time);

            if (am != null) {
                am.setRepeating(AlarmManager.RTC_WAKEUP, mi, intervalMinutes * 60 * 1000, sender);
                AlarmServe = true;
            }
        } else {
            if (am != null) {
                am.cancel(sender);
            }
        }
    }

    class nextTime extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d("nextTime", "onCreate");
        }

        @Override
        public void onStart() {
            super.onStart();
            Log.d("nextTime", "onStart");
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, MainActivity.this.intervalMinutes);
            String time = String.format(Locale.US, "%1$02d:%2$02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
            ((TextView) findViewById(R.id.nextTime)).setText(time);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Log.d("AlarmTime", sdf.format(cal.getTime()));
        }


        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected void onResume() {
            super.onResume();
        }


    }

}
// ミニッツリピーター鳴動クラス
class minutesRepeat extends Thread {
    // 音関係の変数
    private SoundPool SoundsPool;
    private final int resId[][]= {
            {R.raw.hour},
            {R.raw.min15},
            {R.raw.min1}
    };
    private int soundId[][] =new int [resId.length][];

    private int[][] ListofWaitList = new int [resId.length][];

    // コンストラクター
    public minutesRepeat(Context context) {

        for (int i=0;i<resId.length;i++) {
            soundId[i]=new int[resId[i].length];
            ListofWaitList[i]=new int [resId[i].length];
        }
        // 音読み込み
        //ロリポップより前のバージョンに対応するコード
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SoundsPool = new SoundPool(12+3+14+3, AudioManager.STREAM_ALARM, 0);
        } else {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            SoundsPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(12+3+14+3)
                    .build();
        }
        SoundsPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.d("debug", "sampleId=" + sampleId);
                Log.d("debug", "status=" + status);
            }
        });
        //あらかじめ音をロードする必要がある　※直前にロードしても間に合わないので早めに


        for (int i = 0; i < resId.length; i++) {
            for (int j = 0; j < soundId[i].length; j++) {
                int id=  SoundsPool.load(
                      context,resId[i][j],1);
                soundId[i][j]=id;
            MediaPlayer mp=MediaPlayer.create(context,resId[i][j]);
            ListofWaitList[i][j]=mp.getDuration();
            mp.release();
            }
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

        hour%=12;
        if (hour == 0) {
            hour = 12;
        }

        int count[] = { hour, min_15, min_1}; // 鳴動回数

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
/*
                for (int i = 0; i < count[k]; i++) {  // リピート回数
                    Log.d("debug", "i=" + i);
                    if (i == 0) {

                                try {
                                    TimeUnit.MILLISECONDS.sleep(soundWait[0]);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                    }
                    for (int j = 0; j < soundSeaquence.length; j++) {   //指定された音を指定された順番、ウェイトで鳴らす
 //                       Log.d("debug", "j=" + j);


                    }

                }
                */
            }
        }
    }

    // SoundPoolから音を鳴らし、waitミリ秒待つ
    private void ring(final int sound, final int loop) {
        if (soundId[sound].length ==1) {
            SoundsPool.play(soundId[sound][0], 1.0f, 1.0f, 0, loop-1, 1.0f);
             try {
                TimeUnit.MILLISECONDS.sleep(ListofWaitList[sound][0]*loop+200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if (loop>1) {
                SoundsPool.play(soundId[sound][1], 1.0f, 1.0f, 0, loop-2, 1.0f);
                try {
                    TimeUnit.MILLISECONDS.sleep(ListofWaitList[sound][1]*(loop-1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            SoundsPool.play(soundId[sound][0], 1.0f, 1.0f, 0, 0, 1.0f);
            try {
                TimeUnit.MILLISECONDS.sleep(ListofWaitList[sound][0]+200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}

