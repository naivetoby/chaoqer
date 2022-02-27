package com.chaoqer.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 日期工具类
 *
 * @author toby
 */
public class DateUtil {

    private final static Logger logger = LoggerFactory.getLogger(DateUtil.class);

    public static Date getDate(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return cal.getTime();
    }

    public static String getFormatDate(long time) {
        return getFormatDate(time, "yyyy-MM-dd HH:mm:ss");
    }

    public static String getFormatDate(long time, String pattern) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        cal.setTimeInMillis(time);
        return format.format(cal.getTime());
    }

    public static long getFormatDate(String time) {
        return getFormatDate(time, "yyyy-MM-dd HH:mm:ss");
    }

    public static long getFormatDate(String time, String pattern) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            return format.parse(time).getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return -1;
    }

    public static List<String> getRecentHourInMillisList() {
        List<String> recentHourInMillisList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        // 当前整点
        long hourInMillis = getFormatDate(getFormatDate(System.currentTimeMillis()).substring(0, 13).concat(":00:00"));
        recentHourInMillisList.add(Long.toString(hourInMillis));
        cal.setTimeInMillis(hourInMillis);
        // 前一个小时
        cal.add(Calendar.HOUR_OF_DAY, -1);
        long beforeHourInMillis = cal.getTimeInMillis();
        recentHourInMillisList.add(Long.toString(beforeHourInMillis));
        // 后一个小时
        cal.add(Calendar.HOUR_OF_DAY, 2);
        long afterHourInMillis = cal.getTimeInMillis();
        recentHourInMillisList.add(Long.toString(afterHourInMillis));
        return recentHourInMillisList;
    }

}
