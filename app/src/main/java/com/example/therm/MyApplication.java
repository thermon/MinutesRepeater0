package com.example.therm;

import android.app.Application;

public class myApplication extends Application {
    static int[] div_qr(int a, int b) {
        int m = a % b;
        return new int[]{(a - m) / b, m};
    }

    public static String getClassName() {
        return Thread.currentThread().getStackTrace()[3].getClassName().replace("$", ".");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // setupLeakCanary();
    }
}
