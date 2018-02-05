package com.example.therm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:" + "startup");
        if (intent != null) {
            boolean execute = false;
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                execute = true;
            } else if (
                    Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()) &&
                            intent.getDataString().contains(context.getPackageName())
                    ) {
                execute = true;
            }
            minutesRepeat repeatr = new minutesRepeat(context);
            repeatr.loadData();

            boolean b = repeatr.getExecuteOnBootCompleted();
            if (execute && b) {
                repeatr.AlarmSet(Calendar.getInstance());
                /*
                Intent i = new Intent(context, AlarmService.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startService(i);
                */
            }
        }
    }
}

