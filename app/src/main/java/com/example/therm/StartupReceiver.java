package com.example.therm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class StartupReceiver extends BroadcastReceiver {
    private String className = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        className = myApplication.getClassName();
        Log.d(className, "onReceive:" + "startup");
        if (intent != null) {
            String action = intent.getAction();
            boolean bootCompletedExecute = false;
            boolean timeChangedExecute = false;

            switch (action) {
                case Intent.ACTION_BOOT_COMPLETED:
                    bootCompletedExecute = true;
                    break;
                case Intent.ACTION_PACKAGE_REPLACED:
                    if (intent.getDataString() != null) {
                        if (intent.getDataString().contains(context.getPackageName())) {
                            bootCompletedExecute = true;
                        }
                    }
                    break;
                case Intent.ACTION_TIME_CHANGED:
                case Intent.ACTION_TIMEZONE_CHANGED:
                    timeChangedExecute = true;
                    Log.d(className, "onReceive:" + "time changed");
                    break;
            }
            minutesRepeat repeater = new minutesRepeat(context);
            repeater.loadData();

            boolean b = repeater.getExecuteOnBootCompleted();
            if (bootCompletedExecute && b) {
                repeater.AlarmSet(Calendar.getInstance());
            } else if (timeChangedExecute) {
                repeater.AlarmSet(Calendar.getInstance());
/*
                Intent i = new Intent(context, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
*/
            }
        }
    }
}

