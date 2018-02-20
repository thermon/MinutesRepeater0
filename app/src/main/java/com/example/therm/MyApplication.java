package com.example.therm;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class myApplication extends android.app.Application {
    // 時刻表示のフォーマット
    public static SimpleDateFormat sdf_HHmm = new SimpleDateFormat("HH:mm", Locale.US);
    public static SimpleDateFormat sdf_HHmmss = new SimpleDateFormat("HH:mm:ss Z", Locale.US);
    public static SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyy.MM.dd(E) HH:mm:ss Z", Locale.JAPANESE);

    public static String getClassName() {
        return Thread.currentThread().getStackTrace()[3].getClassName().replace("$", ".");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public enum timeFieldEnum {
        hour,
        minute
    }

    public enum timeIndexEnum {
        start,
        end
    }

}

