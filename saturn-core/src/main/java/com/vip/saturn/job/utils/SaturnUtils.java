package com.vip.saturn.job.utils;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaturnUtils {

    public static String convertTime2FormattedString (long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }
}
