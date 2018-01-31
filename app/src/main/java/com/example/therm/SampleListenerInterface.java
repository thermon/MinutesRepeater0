package com.example.therm;

import java.util.EventListener;

public interface SampleListenerInterface extends EventListener {

    /**
     * アラーム時刻が変わったことを通知する
     */
    public void nextAlarmChanged();
}
