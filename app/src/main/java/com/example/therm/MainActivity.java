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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.content.Context.ALARM_SERVICE;
import static com.example.therm.R.id.nowTime;
import static com.example.therm.R.id.textView;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class MainActivity  extends AppCompatActivity  {
    private String PreferencesName="minutesRepeater";
    public Timer mTimer;
    public Timer mTimer2;
    public Handler mHandler;

    // 正時のときの鳴動間隔
    public final int intervalList[] = {2, 3, 4, 5, 6, 10, 12, 15, 20, 30};
    // 最長鳴動間隔
    public final int intervalMin[] = {1, intervalList[0]};
    public final int seekBarMax[] = {60-intervalMin[0], intervalList.length - 1};

    // 音関係の変数
    public minutesRepeat repeater;

    // 時刻表示のフォーマット
    final SimpleDateFormat mSimpleDataFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public Handler repeaterHandler;
    public AlarmManager am = null;

    // 時間帯
    public int[][] zones=new int[2][2];
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
    public SimpleDateFormat sd = new SimpleDateFormat("HH:mm");

    public int getIntervals() {
        return intervalMinutes;
    }
    private Notification notification;
    NotificationManager notificationManager;

    // 初期化ブロック
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round) // アイコン
                .setTicker("Hello") // 通知バーに表示する簡易メッセージ
                .setWhen(System.currentTimeMillis()) // 時間
                .setContentTitle("My notification") // 展開メッセージのタイトル
                .setContentText("Hello Notification!!") // 展開メッセージの詳細メッセージ
                .setContentIntent(contentIntent) // PendingIntent
                .build();

        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(1, notification);

        // 自分のreceiverを登録
        IntentFilter filter = new IntentFilter();
        filter.addAction("AlarmTimeChanged");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        {
            // Serviceを呼び出す
            Intent intent = new Intent(getApplication(), AlarmService.class);
            // Alarm の停止
            intent.putExtra("StopAlarm", true);
            startService(intent);
        }
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        // フィールドの値から変数を初期化
        getFieldValues();
        setIntervalValue(seekBarProgress, BasedOnMinute_00);

        //       intervalNum = findViewById(R.id.intervalNumber);
        Log.d("onCreate", "call AlarmSet()");
        // リピーター鳴動用インスタンス初期化
        repeater = new minutesRepeat(this);
        // UIスレッドにpost(Runnable)やsendMessage(message)を送りつけるハンドラーを作成
        mHandler = new Handler(getMainLooper());

        repeater.setHandler(mHandler);
        Calendar n=repeater.getNextAlarmTime(zonesEnable,zones,intervalMinutes,BasedOnMinute_00,Calendar.getInstance());
        if (n!=null ) AlarmSet(n);

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
                Gson gson = new Gson();
                SharedPreferences sharedPreferences = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("basedOnHour", BasedOnMinute_00);
                editor.apply();

                intervalMinutes = unitTime;
                Log.d("Ref", "call AlarmSet()");
                Calendar n=repeater.getNextAlarmTime(zonesEnable,zones,intervalMinutes,BasedOnMinute_00,Calendar.getInstance());
                if (n!=null ) AlarmSet(n);
            }
        });

        // 鳴動間隔シークバーのリスナー設定
        ((SeekBar) findViewById(R.id.intervalSeekBar))
                .setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                        Log.d("SeekBar", "Changed "+ seekBarProgress+" to "+i);
                        seekBarProgress = i;
                        setIntervalValue(i, BasedOnMinute_00);
                        Calendar n=repeater.getNextAlarmTime(zonesEnable,zones,intervalMinutes,BasedOnMinute_00,Calendar.getInstance());

                        Gson gson = new Gson();
                        SharedPreferences sharedPreferences = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("intervalSeekBar", seekBarProgress);
                        editor.apply();

                        if (n!=null ) AlarmSet(n);
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
        for (int i=0;i<timeButtonId.length;i++) {
            for (int j=0;j<timeButtonId[i].length;j++) {
                findViewById(timeButtonId[i][j]).setOnClickListener(timeButton);
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
        {
            Intent intent = new Intent(getApplication(), AlarmService.class);
            intent.putExtra("StopAlarm", true);
            stopService(intent);
        }
        // 時間帯のどれかが有効の場合
//        Intent intent = new Intent(getApplication(),AlarmReceiver.class);
//        PendingIntent sender = PendingIntent.getBroadcast(getApplication(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        // アラーム設定
        /*
        am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am!=null) {
            am.cancel(sender);
        }
        */
        notification.flags -= Notification.FLAG_ONGOING_EVENT;

        notificationManager.notify(1, notification);
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

    private void getFieldValues() {
        Gson gson = new Gson();
        zones=null;
        SharedPreferences sharedPreferences = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
        zones=gson.fromJson(sharedPreferences.getString("zones",null),int[][].class);
        zonesEnable=gson.fromJson(sharedPreferences.getString("zonesEnable",null),boolean[].class);
        seekBarProgress=sharedPreferences.getInt("intervalseekBar",0);
        BasedOnMinute_00=sharedPreferences.getBoolean("basedOnHour",false);

        if (zones==null) {
            // 時間情報を取得
            zones=new int [timeTextId.length][];
            for (int i = 0; i < timeTextId.length; i++) {
                if (zones[i] == null) zones[i] = new int [timeTextId[i].length];
                for (int j = 0; j < timeTextId[i].length; j++) {
//                    if (zones[i][j] == null) zones[i][j] = Calendar.getInstance();
                    int id = timeTextId[i][j];
                    TextView Field = findViewById(id);
                    try {
                        // TextViewから設定時刻の情報を取得
                        Date d = sd.parse((String) Field.getText());
                        zones[i][j]=d.getHours()*60+d.getMinutes();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            for (int i = 0; i < timeTextId.length; i++) {
                for (int j = 0; j < timeTextId[i].length; j++) {
                    int minute=zones[i][j] % 60;
                    int hour=(zones[i][j]-minute) /60;
                    ((TextView) findViewById(timeTextId[i][j])).setText(
                            String.format("%1$02d:%2$02d",hour,minute))
                    ;
                }
            }
        }
        if (zonesEnable==null) {
            zonesEnable=new boolean[2];
        }




        // 正時基準チェックボックスの値を取得
        ((CheckBox) findViewById(R.id.ReferencedToTheHour)).setChecked(BasedOnMinute_00);
//        BasedOnMinute_00 = ((CheckBox) findViewById(R.id.ReferencedToTheHour)).isChecked();

        // 時間間隔を取得
        if (seekBarProgress>seekBarMax[BasedOnMinute_00 ? 1: 0]) {
            seekBarProgress=seekBarMax[BasedOnMinute_00 ? 1: 0];
        }
        ((SeekBar) findViewById(R.id.intervalSeekBar)).setProgress(seekBarProgress);
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

    // シークバーの値から鳴動間隔に変換
    public void setIntervalValue(int progress, boolean regulate) {
        int unitTime;
        if (regulate) {
            // 鳴動時刻を00分基準とした場合
            unitTime = intervalList[progress];

        } else {
            unitTime = progress + intervalMin[0];
        }
        Log.d("setIntervalValue",String.format("%1$d(%2$d)",progress,unitTime));
        ((TextView) findViewById(R.id.intervalNumber)).setText(String.valueOf(unitTime));
        intervalMinutes = unitTime;
    }

    // 時間帯入力用のリスナー
    private View.OnClickListener timeButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final int id = view.getId();
            Log.d("timeButton", String.valueOf(id));


            final HashMap<Integer, int[]> TimeZoneButtonMap=new HashMap<>();
            SparseIntArray Button2Text = new SparseIntArray(4);
            for (int ix=0;ix<timeButtonId.length;ix++) {
                for (int iy=0;iy<timeButtonId[ix].length;iy++) {
                    TimeZoneButtonMap.put(timeButtonId[ix][iy], new int[]{ix, iy});
                    Button2Text.append(timeButtonId[ix][iy],timeTextId[ix][iy]);
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
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                                Log.d("timeChanged", String.format("%02d:%02d", hourOfDay, minute));
                                field.setText(String.format("%02d:%02d", hourOfDay, minute));
                                int idxList[]=TimeZoneButtonMap.get(id);
                                if (zones[idxList[0]]== null) {
                                    for (int j = 0; j < zones[idxList[0]].length; j++) {
                                        zones[idxList[0]][j] = 0;
                                    }
                                }
                                zones[idxList[0]][idxList[1]]=hourOfDay*60+minute;
                                Log.d("timeButton","index0="+idxList[0]+",index1="+idxList[1]);
                                  Log.d("timeButton","time:"+
                                          String.format("%1$02d:%2$02d",hourOfDay,minute));
                                Calendar n = repeater.getNextAlarmTime(zonesEnable,zones, intervalMinutes, BasedOnMinute_00, Calendar.getInstance());
                                if (n != null) AlarmSet(n);

                            }
                        },
                        d.getHours(), d.getMinutes(), true);
                dialog.show();

            } catch (ParseException e) {
                e.printStackTrace();
            }
            Gson gson = new Gson();
            SharedPreferences sharedPreferences = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("zones", gson.toJson(zones));
            editor.apply();

        }
    };

    // 鳴動間隔調整用ボタンのリスナー
    private View.OnClickListener intervalButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int oldValue=seekBarProgress;
             int i = view.getId();
            if (i == R.id.intervalDecrease) {
                seekBarProgress--;
                if (seekBarProgress<0) seekBarProgress=0;
            } else if (i == R.id.intervalIncrease) {
                seekBarProgress++;
                int idx=BasedOnMinute_00 ? 1 : 0;
                if (seekBarProgress>seekBarMax[idx]) seekBarProgress=seekBarMax[idx];
            }
            Log.d("IntervalButton", "seekBar Changed from "+oldValue+ " to "+seekBarProgress);
            ((SeekBar) findViewById(R.id.intervalSeekBar)).setProgress(seekBarProgress);

            // setIntervalValue(seekBarProgress, BasedOnMinute_00);
            Log.d("intervalButton", "call AlarmSet()");
            Calendar n=repeater.getNextAlarmTime(zonesEnable,zones,intervalMinutes,BasedOnMinute_00,Calendar.getInstance());
            if (n!=null ) AlarmSet(n);
        }
    };


    // 時間帯有効化チェックボックスのリスナー
    private CheckBox.OnCheckedChangeListener timeEnableSwitch = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        int idx=-1;
        switch (compoundButton.getId()) {
            case R.id.TimeZone1_Enable:
                idx=0;
                break;
            case R.id.TimeZone2_Enable:
                idx=1;
                break;
            default:
                return;
        }
        Log.d("TimeZone", compoundButton.getText() + ":" + (b ? "Enable" : "Disable"));
        zonesEnable[idx]=b;
        for (int i=0;i<zones.length;i++) {
            if (zones[i] == null) zones[i] = new int[2];
            for (int j = 0; j < zones[idx].length; j++) {

            }
        }

            Log.d("timeEnableSwitch","index0="+idx);
            Log.d("timeEnableSwitch","enable="+String.valueOf(zonesEnable[idx]));

        Gson gson = new Gson();
        SharedPreferences sharedPreferences = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("zonesEnable", gson.toJson(zonesEnable));
        editor.apply();

        Calendar n = repeater.getNextAlarmTime(zonesEnable,zones, intervalMinutes, BasedOnMinute_00, Calendar.getInstance ());
        if (n != null) AlarmSet(n);
        }
    };

    private int NextAlarm;
    
    private void AlarmSet(Calendar time) {

        am = (AlarmManager) getSystemService(ALARM_SERVICE);
//        Intent intent = new Intent(getApplication(),AlarmReceiver.class);

        if (time != null) {
            time.set(Calendar.SECOND, 0);
            time.set(Calendar.MILLISECOND, 0);
            long mi = time.getTimeInMillis();
//                 mi -= mi % (1000 * 60); // 秒を0にする
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Log.d("AlarmSet", sdf.format(time.getTime()));

            //sdf = new SimpleDateFormat("HH:mm");
            // ((TextView) findViewById(R.id.nextTime)).setText(sdf.format(time.getTime()));

            // 時間帯のどれかが有効の場合
            // Serviceを呼び出す
            Intent serviceIntent = new Intent(getApplication(), AlarmService.class);
            // Alarm の停止を解除
            serviceIntent.putExtra("StopAlarm", false);
            serviceIntent.putExtra("interval", intervalMinutes);
            serviceIntent.putExtra("zones", zones);
            serviceIntent.putExtra("zonesEnable", zonesEnable);
            serviceIntent.putExtra("triggerTime", time);
            serviceIntent.putExtra("basedOnHour", BasedOnMinute_00);
            startService(serviceIntent);
/*
            intent.putExtra("interval", intervalMinutes);
            intent.putExtra("zones", zones);
            intent.putExtra("triggerTime", time);
            intent.putExtra("basedOnHour", BasedOnMinute_00);
            PendingIntent sender = PendingIntent.getBroadcast(getApplication(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            */
        }  else{
            // Serviceを呼び出す
            Intent serviceIntent = new Intent(getApplication(), AlarmService.class);
            // Alarm の停止を解除
            serviceIntent.putExtra("StopAlarm", true);
            startService(serviceIntent);
        }
        /*
        if (am != null) {
            PendingIntent sender = PendingIntent.getBroadcast(getApplication(), 0, intent,PendingIntent.FLAG_CANCEL_CURRENT );
            if (time != null) {
                 am.setRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), intervalMinutes * 60 * 1000, sender);
            } else {
                am.cancel(sender);
            }
        }
        */
    }
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("main:Reciever", "receive : " + intent.getAction());
            String message = intent.getStringExtra("Message");
            Log.d("main:Reciever", "Message : " + message);
            ((TextView) findViewById(R.id.nextTime)).setText(message);
            long time= intent.getLongExtra("time",0);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            Log.d("main:Reciever", "time : " + sdf.format(time));

            am = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent_ring = new Intent(getApplication(), AlarmReceiver.class);
            PendingIntent sender = PendingIntent.getBroadcast(getApplication(), 0, intent_ring, PendingIntent.FLAG_CANCEL_CURRENT);
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
    private BroadcastReceiver ringerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            minutesRepeat repeater = new minutesRepeat(context);
            Handler mHandler = new Handler(context.getMainLooper());
            repeater.setHandler(mHandler);

            Thread mThread = new Thread(repeater);
            mThread.start();
        }
    };

}
// ミニッツリピーター鳴動クラス
class minutesRepeat extends Application
        implements Runnable {
    private static minutesRepeat instance = null;

    // 音関係の変数
    private SoundPool SoundsPool;
    private final int resId[][]= {
            {R.raw.hour},
            {R.raw.min15},
            {R.raw.min1}
    };
    private int soundId[][] =new int [resId.length][];

    private int[][] ListofWaitList = new int [resId.length][];
    private Context MainContext;

    // コンストラクター
    public minutesRepeat(Context context) {
       MainContext=context;
       instance=this;

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
    public Calendar getNextAlarmTime(boolean [] zonesEnable,int [][] zones,int interval,boolean basedOnHour,Calendar time) {
        Log.d("getNextAlarmTime", "called");
        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Log.d("getNextAlarmTime", "now="+sdf2.format(time.getTime()));


        // Serviceを呼び出す

        // timeがnullならnullを返す
        if (time == null) return null;
        time.set(Calendar.SECOND,0);
        time.set(Calendar.MILLISECOND,0);

        // zonesがひとつでも有効かチェック
        boolean enable = false;
        for (int i = 0; i < zones.length; i++) {
            if (zonesEnable[i]) {
                enable = true;
                break;
            }
        }
        // すべてnullだったらnullを返す
        if (enable == false) return null;

        int hour = time.get(Calendar.HOUR_OF_DAY);
        int minute = time.get(Calendar.MINUTE);

        int nextTime = hour*60+minute;
        // basedOnHourがtrueなら、毎時00分を基準にする
        if (basedOnHour) {
            nextTime+=interval - (minute % interval);
        } else {
            nextTime+=interval;
        }

        // アラーム時刻変更
        // 現在時刻と時間帯をもとにアラーム時刻を設定
        ArrayList<Integer> AlarmTime = new ArrayList<Integer>();


        for (int i = 0; i < zones.length; i++) {
            if (!zonesEnable[i]) continue;
            int StartTime = zones[i][0];
            int EndTime = zones[i][1];

            if (StartTime > EndTime) {
                // 開始時刻より終了時刻が早い＝翌日とみなす
                EndTime+=24*60;
            }
            Log.d("getNextAlarmTime", "start[" + i + "]=" + String.format("%1$02d:%2$02d",(StartTime-StartTime % 60) /60 ,StartTime % 60));
            Log.d("getNextAlarmTime", "  end[" + i + "]=" + String.format("%1$02d:%2$02d",(EndTime-EndTime % 60) /60 ,EndTime % 60));
            if (EndTime > nextTime &&
                    nextTime > StartTime) {
                // 次回予定時刻がタイムゾーン内にある場合、次回予定時刻より早い直近の開始時間を探す
                AlarmTime.add(nextTime);
            } else {
                // 現在時刻がタイムゾーン外の場合、次回予定時刻より遅い直近の開始時刻を探す。
                if (StartTime < nextTime) {
                    StartTime+=24*60;
                }
                AlarmTime.add(StartTime);
            }
        }

        for (int t : AlarmTime) {
            Log.d("getNextAlarmTime", "StartTimes="+String.format("%1$02d:%2$02d",(t-t % 60) /60 ,t % 60));
        }

        int Start1 = -1;
        int Start2 = 2*24*60;

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
        hour = (Start-minute)/60;
        int day=(hour>=24) ? 1:0;
        hour-=day*24;

        Log.d("getNextAlarmTime", "next=" +String.format("%1$02d:%2$02d",hour ,minute));
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,hour);
        cal.set(Calendar.MINUTE,minute);
        cal.add(Calendar.DATE,day);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);

        return cal;
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
/*
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    */
private Handler myHandler;
    void setHandler(Handler mHandler){
        myHandler=mHandler;
    }
    public static minutesRepeat getInstance() {
        return instance;
    }
}

