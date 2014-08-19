package com.saic.uicds.clients.em.deldotAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.xmlbeans.XmlObject;

import com.saic.uicds.clients.util.Common;

public class DeldotUtils {

    static final String timestampXPath = "timestamp";

    public static String getDateStringFromRTTA(XmlObject rtta) {

        XmlObject[] dates = rtta.selectPath(timestampXPath);
        String dateString = null;
        if (dates.length > 0) {
            String event_datetime = Common.getTextFromAny(dates[0]);
            dateString = DeldotUtils.getDateStringForActivityDate(event_datetime);
        }
        return dateString;
    }

    public static Date getDateFromRTTA(XmlObject rtta) {

        String dateStr = getDateStringFromRTTA(rtta);
        return getDateFromActivityDateString(dateStr);
    }

    public static String getNowAsString() {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat ISO8601Local = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        TimeZone timeZone = TimeZone.getDefault();
        ISO8601Local.setTimeZone(timeZone);
        return ISO8601Local.format(cal.getTime());
    }

    public static Date getDateFromRTTATimestamp(String event_datetime) {

        Date dateTime = null;
        if (event_datetime != null && event_datetime.length() > 0) {
            SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            TimeZone timeZone = TimeZone.getDefault();
            ISO8601Local.setTimeZone(timeZone);
            try {
                dateTime = (Date) ISO8601Local.parse(event_datetime.trim());
            } catch (ParseException e) {
                System.err.println(event_datetime);
                System.err.println("Error parsing date string should be yyyy-MM-dd HH:mm:ss format: "
                    + e.getMessage());
            }
        }
        return dateTime;
    }

    public static Date getDateFromActivityDateString(String event_datetime) {

        Date dateTime = null;
        if (event_datetime != null && event_datetime.length() > 0) {
            SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            TimeZone timeZone = TimeZone.getDefault();
            ISO8601Local.setTimeZone(timeZone);
            try {
                dateTime = (Date) ISO8601Local.parse(event_datetime.trim());
            } catch (ParseException e) {
                System.err.println(event_datetime);
                System.err.println("Error parsing date string should be yyyy-MM-dd'T'HH:mm:ss format: "
                    + e.getMessage());
            }
        }
        return dateTime;
    }

    public static String getDateStringForActivityDate(String event_datetime) {

        String dateTimeValue = null;
        // if (event_datetime != null && event_datetime.length() > 0) {
        // SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        TimeZone timeZone = TimeZone.getDefault();
        // ISO8601Local.setTimeZone(timeZone);
        // try {
        // Date dateTime = (Date) ISO8601Local.parse(event_datetime.trim());
        Date dateTime = getDateFromRTTATimestamp(event_datetime);
        SimpleDateFormat newDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        newDateFormat.setTimeZone(timeZone);
        dateTimeValue = newDateFormat.format(dateTime);
        return dateTimeValue;
        // } catch (ParseException e) {
        // System.err.println("Error parsing date string should be yyyy-MM-dd'T'HH:mm:ss format: "
        // + e.getMessage());
        // }
        // }
        // return dateTimeValue;

    }

}
