package com.example.x280.hourglass.Service;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.Map;

public class TimeValueFormatter extends ValueFormatter {

    public TimeValueFormatter(){
        //nothing
    }

//    @Override
    public String getFormattedValue(float value){//, PieEntry entry, int index, ViewPortHandler viewPortHandler
        return AppUtil.formatMilliSeconds((long)value);
    }
}
