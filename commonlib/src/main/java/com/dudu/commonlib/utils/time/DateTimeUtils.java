package com.dudu.commonlib.utils.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2016/5/23.
 */
public class DateTimeUtils {
    public static String obtainCurrentTime() {
        String datetime;
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        datetime = sdf.format(date);
        return datetime;
    }

    public static Date transformDate(String dateString) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            e.getMessage();
        }
        return date;
    }
}
