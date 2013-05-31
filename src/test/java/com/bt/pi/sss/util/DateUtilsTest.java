package com.bt.pi.sss.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.sss.exception.InvalidArgumentException;
import com.bt.pi.sss.util.DateUtils;

public class DateUtilsTest {
    private DateUtils dateUtils;

    @Before
    public void setUp() throws Exception {
        this.dateUtils = new DateUtils();
    }

    @Test
    public void testParseHttpDateNull() {
        // setup
        String input = null;

        // act
        Calendar result = this.dateUtils.parseHttpDate(input);

        // assert
        assertNull(result);
    }

    @Test(expected = InvalidArgumentException.class)
    public void testParseHttpDateEmptyString() {
        // setup
        String input = "";

        // act
        this.dateUtils.parseHttpDate(input);
    }

    @Test
    public void testParseHttpDateFirstFormat() {
        // setup
        String input = "Sun, 06 Nov 1994 08:49:37 GMT";

        // act
        Calendar result = this.dateUtils.parseHttpDate(input);

        // assert
        assertEquals(Calendar.SUNDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(6, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH));
        assertEquals(1994, result.get(Calendar.YEAR));
        assertEquals(8, result.get(Calendar.HOUR));
        assertEquals(49, result.get(Calendar.MINUTE));
        assertEquals(37, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.ZONE_OFFSET));
    }

    @Test
    public void testParseHttpDateSecondFormat() {
        // setup
        String input = "Sunday, 06-Nov-94 08:49:37 GMT";

        // act
        Calendar result = this.dateUtils.parseHttpDate(input);

        // assert
        assertEquals(Calendar.SUNDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(6, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH));
        assertEquals(1994, result.get(Calendar.YEAR));
        assertEquals(8, result.get(Calendar.HOUR));
        assertEquals(49, result.get(Calendar.MINUTE));
        assertEquals(37, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.ZONE_OFFSET));
    }

    @Test
    public void testParseHttpDateThirdFormat() {
        // setup
        String input = "Sun Nov 6 08:49:37 1994";

        // act
        Calendar result = this.dateUtils.parseHttpDate(input);

        // assert
        assertEquals(Calendar.SUNDAY, result.get(Calendar.DAY_OF_WEEK));
        assertEquals(6, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.NOVEMBER, result.get(Calendar.MONTH));
        assertEquals(1994, result.get(Calendar.YEAR));
        assertEquals(8, result.get(Calendar.HOUR));
        assertEquals(49, result.get(Calendar.MINUTE));
        assertEquals(37, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.ZONE_OFFSET));
    }

    @Test(expected = InvalidArgumentException.class)
    public void testParseHttpDateDuffFormat() {
        // setup
        String input = "Nov Sun 6 08:49:37 1994";

        // act
        this.dateUtils.parseHttpDate(input);
    }
}
