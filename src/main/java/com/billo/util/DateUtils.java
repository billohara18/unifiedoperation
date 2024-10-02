package com.billo.util;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class DateUtils {

    public static String getDateString(long timeStamp) {
        Timestamp stamp = new Timestamp(timeStamp);
        Date date = new Date(stamp.getTime());

        DateFormat df = new SimpleDateFormat("MM/dd HH:mm");
        df.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")));
        return df.format(date);

    }
}
