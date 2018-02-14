package com.example.therm;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.Calendar;

class ringAlarm {
    private static int myStreamId = AudioManager.STREAM_ALARM;
    private final int resId[] = {
            R.raw.hour,
            R.raw.min15,
            R.raw.min1
    };
    // 音関係の変数
    private SoundPool SoundsPool;
    private int soundId[] = new int[resId.length];
    private int waitArray[] = new int[resId.length];
    private int wait0 = 400;  // 鳴動前ウェイト
    private int wait1 = 200;  // 鳴動中ベルの音が変わる時のウェイト
    private HandlerThread mHT;
    private Handler mHandler;
    private String className;


    // リピーター音を鳴らす処理
    ringAlarm(Context context) {
        className = myApplication.getClassName();
        // AudioManagerを取得する
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (am != null) {
            // 現在の音量を取得する
            int ringVolume = am.getStreamVolume(myStreamId);

            // ストリームごとの最大音量を取得する
            int ringMaxVolume = am.getStreamMaxVolume(myStreamId);

            // 音量を設定する
            am.setStreamVolume(myStreamId, ringVolume, 0);
        }


        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int[] minArray = myApplication.div_qr(minute, 15);

        hour %= 12;
        if (hour == 0) hour = 12;

//        int count[] = {hour, min_15, min_1}; // 鳴動回数
        final int count[] = {hour, minArray[0], minArray[1]}; // 鳴動回数
        soundLoad(context, count);

        mHT = new HandlerThread("repeater");
        mHT.start();
        mHandler = new Handler(mHT.getLooper());

        Integer errorCount = 0;

        // 全部をlooperに突っ込む場合
        int wait = wait0;
        for (int k = 0; k < resId.length; k++) {  // チャイム、時間、15分、5分、1分の順で鳴らす
//            Log.d("debug", "k=" + k);
            if (count[k] <= 0) continue;
            // カウントする場合
            final int[] mStreamId = new int[1];
            final int sid = soundId[k];
            final int mCount = count[k];
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mStreamId[0] = SoundsPool.play(sid, 1.0f, 1.0f, 0, mCount - 1, 1.0f);
                }
            }, wait);

            wait += waitArray[k] * count[k] + wait1;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    SoundsPool.stop(mStreamId[0]);
                    // SoundsPool.unload(sid);
                }
            }, wait);
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int aSoundId : soundId) {
                    SoundsPool.unload(aSoundId);
                }
                SoundsPool.release();
                SoundsPool = null;
                Log.d(className, "SoundPool released.");
                mHT.quit();
                mHT = null;
            }
        }, wait);

        Log.d(className, "ring.");
    }

    private void soundLoad(Context context, int counts[]) {
        // 音読み込み
        //ロリポップより前のバージョンに対応するコード
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            SoundsPool = new SoundPool(resId.length, myStreamId, 0);
        } else {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            SoundsPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(resId.length)
                    .build();
        }
        SoundsPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                Log.d(className, String.format("load sound id=%d,status=%d", sampleId, status));
            }
        });


        //あらかじめ音をロードする必要がある　※直前にロードしても間に合わないので早めに
        for (int i = 0; i < resId.length; i++) {
            if (counts[i] <= 0) continue;
            int id = SoundsPool.load(
                    context, resId[i], 1);
            soundId[i] = id;
            MediaPlayer mp = MediaPlayer.create(context, resId[i]);
            waitArray[i] = mp.getDuration();
            mp.release();
        }
        Log.d(className, "SoundPool created.");
    }
}
