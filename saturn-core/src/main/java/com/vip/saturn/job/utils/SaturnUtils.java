package com.vip.saturn.job.utils;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SaturnUtils {

    public static String convertTime2FormattedString (long time){
        Date date = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(date);
    }
}
