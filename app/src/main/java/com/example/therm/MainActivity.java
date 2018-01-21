package com.example.therm;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity  extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        int viewId;
        String resViewName;
        Resources res = getResources();
        boolean IntervalSw=false;

        CheckBox referencedHour= findViewById(R.id.ReferencedToTheHour);

        referencedHour.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                NumberPicker interval = findViewById(R.id.intervalTime);
                String intervalList[] = {"2", "3", "4", "5", "6", "10", "12", "15", "20", "30"};
                // チェックボックスのチェック状態を取得します
                int maxOld = interval.getMaxValue();
                int maxNew;
                int minNew;
                String Lists[];

                maxNew = b ? intervalList.length +1 : 60;
                minNew = 2;
                Lists = b ? intervalList : null;

                if (maxNew > maxOld) {
                    interval.setDisplayedValues(Lists);
                    interval.setMinValue(minNew);
                    interval.setMaxValue(maxNew);
                } else {
                    interval.setMinValue(minNew);
                    interval.setMaxValue(maxNew);
                    interval.setDisplayedValues(Lists);
                }
            }
        });

        referencedHour.setChecked(!referencedHour.isChecked());

                /*
        if (referencedHour.isChecked()) {
            interval.setDisplayedValues(intervalList);
            interval.setMinValue(0);
            interval.setMaxValue(intervalList.length-1);

        } else {
            interval.setDisplayedValues(null);
            interval.setMinValue(2);
            interval.setMaxValue(60);
        }
        */
    }
    /*
    public void onCheckedChanged(RadioButton button, int checkedId) {
        // TODO Auto-generated method stub
        if(button.isChecked() == true) {
            // チェックされた状態の時の処理を記述
            for(int i = 0; i < intervals.length; i++){
                intervalsID[i].setChecked(intervalsID[i] == button ? true :false);
            }
            ((TextView) findViewById(R.id.textView00)).setText(checkedId);
            Toast.makeText(MainActivity.this,
                    button.getText() + "が選択されました",Toast.LENGTH_SHORT).show();
        }
        else {
            // チェックされていない状態の時の処理を記述
        }
    }
    */

}
