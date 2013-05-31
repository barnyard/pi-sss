/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.bt.pi.sss.exception.InvalidArgumentException;

/**
 * utilities to parse http header dates as per the first three formats shown at
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1
 */
@Component
public class DateUtils {
    private static final String INVALID_DATE_STRING = "invalid http header date: %s";
    private static final String PATTERN_ONE = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final String PATTERN_TWO = "EEE, dd-MMM-yy HH:mm:ss z";
    private static final String PATTERN_THREE = "EEE MMM dd HH:mm:ss yyyy";

    public DateUtils() {
    }

    public Calendar parseHttpDate(String s) {
        if (null == s)
            return null;

        if ("".equals(s))
            throw new InvalidArgumentException(String.format(INVALID_DATE_STRING, s));

        Calendar result = parse(s, PATTERN_ONE);
        if (null == result)
            result = parse(s, PATTERN_TWO);
        if (null == result)
            result = parse(s, PATTERN_THREE);
        if (null == result)
            throw new InvalidArgumentException(String.format(INVALID_DATE_STRING, s));
        return result;
    }

    private Calendar parse(String date, String format) {
        DateFormat df = new SimpleDateFormat(format);
        try {
            Date d = df.parse(date);
            Calendar result = Calendar.getInstance();
            result.setTime(d);
            return result;
        } catch (ParseException e) {
            return null;
        }
    }
}
