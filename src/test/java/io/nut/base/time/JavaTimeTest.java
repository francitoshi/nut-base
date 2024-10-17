/*
 *  JavaTimeTest.java
 *
 *  Copyright (c) 2020-2024 francitoshi@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Report bugs or new features to: francitoshi@gmail.com
 */
package io.nut.base.time;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class JavaTimeTest
{
    
    public JavaTimeTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }
    
    static final String[] DT_ES =
    {
        "31-Ago-17", 
        "31-Ago-2017", 
    };
    static final String[] DT =
    {
        "9-Aug-17", 
        "09-Aug-17", 
        "31-Aug-17", 
        "31-Aug-2017", 
        "20111203", 
        "2011-12-03", 
        "2011-12-3", 
        "2011-2-13", 
        "2011-2-3", 
        "2011-12-03 11:12", 
        "2011-12-03 11:12:13", 
        "2011-12-03T11:12:13", 
        "2011-12-03 11:12:13Z", 
        "2011-12-03T10:15:30Z", 
        "2011-12-03 10:14:30+01", 
        "2011-12-03 10:15:30+0100", 
        "2011-12-03 10:15:30+01:00", 
        "2011-12-03T10:15:30+01:00",
        "2011-12-03T10:15:30",
        "2011-12-03T10:15:30+01:00[Europe/Paris]",
        "2011-12-03T10:15:30+01:00[Europe/Madrid]",
        "2018-09-23 20:38:41.913",
        "2018-02-08 13:04:46.097Z",
        "2018-02-08T13:04:46.097Z",
        "2018-02-08 13:04:46.097 UTC",
    };

    /**
     * Test of parseZonedDateTime method, of class JavaTime.
     */
    @Test
    public void testParseZonedDateTime() throws Exception
    {
        //test that no sample is repeated
        {
            HashSet<String> repeated = new HashSet<>();
            for(int i=0;i<DT.length;i++)
            {
                if(!repeated.add(DT[i]))
                {
                    throw new Exception(i+" repeated "+DT[i]);
                }
            }
        }
        
        //test that no formatter is repeated
        {
            HashSet<String> repeatedRegEx = new HashSet<>();
            for(int i=0;i<JavaTime.ParserHolder.INSTANCE.parsers.length;i++)
            {
                JavaTime.Parser parser = JavaTime.ParserHolder.INSTANCE.parsers[i];
                if(!repeatedRegEx.add(parser.regex))
                {
                    throw new Exception(i+" repeated for "+parser.regex);
                }
            }
        }        
        
        //test dates are properly parsed
        for (String item : DT)
        {
            ZonedDateTime dt = JavaTime.parseZonedDateTime(item, JavaTime.UTC, Locale.ROOT);
            System.out.println(item+" => "+dt.toString());
        }
        for (String item : DT_ES)
        {
//666            JavaTime.parseZonedDateTime(item, JavaTime.UTC, new Locale("es"));
        }
    }

    /**
     * Test of parseLocalDateTime method, of class JavaTime.
     */
    @Test
    public void testParseLocalDateTime()
    {
        LocalDateTime madrid  = JavaTime.parseLocalDateTime("2011-12-03T11:15:30+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDateTime london = JavaTime.parseLocalDateTime("2011-12-03T10:15:30+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDateTime nozone  = JavaTime.parseLocalDateTime("2011-12-03T11:15:30", JavaTime.EuropeMadrid);
        assertEquals(madrid, london);
        assertEquals(madrid, nozone);
    }

    /**
     * Test of parseLocalDate method, of class JavaTime.
     */
    @Test
    public void testParseLocalDate_String_ZoneId()
    {
        LocalDate madrid  = JavaTime.parseLocalDate("2011-12-03T00:15:30+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDate london = JavaTime.parseLocalDate("2011-12-02T23:15:30+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDate nozone  = JavaTime.parseLocalDate("2011-12-03T00:15:30", JavaTime.EuropeMadrid);
        assertEquals(madrid, london);
        assertEquals(madrid, nozone);
    }
    /**
     * Test of parseLocalDate method, of class JavaTime.
     */
    @Test
    public void testParseLocalDate_String()
    {
        LocalDate a  = JavaTime.parseLocalDate("2011-12-03");
        LocalDate b = JavaTime.parseLocalDate("2011-12-03");
        LocalDate c  = JavaTime.parseLocalDate("2011-12-03");
        assertEquals(a, b);
        assertEquals(a, c);
    }

    /**
     * Test of isToday method, of class JavaTime.
     */
    @Test
    public void testIsToday_ZonedDateTime_ZonedDateTime()
    {
        ZonedDateTime yesterday = JavaTime.parseZonedDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        ZonedDateTime today     = JavaTime.parseZonedDateTime("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        ZonedDateTime today2    = JavaTime.parseZonedDateTime("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        ZonedDateTime tomorrow  = JavaTime.parseZonedDateTime("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertFalse(JavaTime.isToday(today, yesterday));
        assertTrue(JavaTime.isToday(today, today2));
        assertFalse(JavaTime.isToday(today, tomorrow));
    }
    /**
     * Test of isToday method, of class JavaTime.
     */
    @Test
    public void testIsToday_LocalDateTime_LocalDateTime()
    {
        LocalDateTime yesterday = JavaTime.parseLocalDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDateTime today     = JavaTime.parseLocalDateTime("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDateTime today2    = JavaTime.parseLocalDateTime("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDateTime tomorrow  = JavaTime.parseLocalDateTime("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertFalse(JavaTime.isToday(today, yesterday));
        assertTrue(JavaTime.isToday(today, today2));
        assertFalse(JavaTime.isToday(today, tomorrow));
    }
    
    /**
     * Test of isToday method, of class JavaTime.
     */
    @Test
    public void testIsToday_LocalDate_LocalDate()
    {
        LocalDate yesterday = JavaTime.parseLocalDate("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDate today     = JavaTime.parseLocalDate("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDate today2    = JavaTime.parseLocalDate("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDate tomorrow  = JavaTime.parseLocalDate("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertFalse(JavaTime.isToday(today, yesterday));
        assertTrue(JavaTime.isToday(today, today2));
        assertFalse(JavaTime.isToday(today, tomorrow));
    }
    
    /**
     * Test of isYesterday method, of class JavaTime.
     */
    @Test
    public void testIsYesterday_ZonedDateTime_ZonedDateTime()
    {
        ZonedDateTime yesterday = JavaTime.parseZonedDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        ZonedDateTime today     = JavaTime.parseZonedDateTime("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        ZonedDateTime today2    = JavaTime.parseZonedDateTime("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        ZonedDateTime tomorrow  = JavaTime.parseZonedDateTime("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertTrue(JavaTime.isYesterday(today, yesterday));
        assertFalse(JavaTime.isYesterday(today, today2));
        assertFalse(JavaTime.isYesterday(today, tomorrow));
    }
    /**
     * Test of isYesterday method, of class JavaTime.
     */
    @Test
    public void testIsYesterday_LocalDateTime_LocalDateTime()
    {
        LocalDateTime yesterday = JavaTime.parseLocalDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDateTime today     = JavaTime.parseLocalDateTime("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDateTime today2    = JavaTime.parseLocalDateTime("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDateTime tomorrow  = JavaTime.parseLocalDateTime("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertTrue(JavaTime.isYesterday(today, yesterday));
        assertFalse(JavaTime.isYesterday(today, today2));
        assertFalse(JavaTime.isYesterday(today, tomorrow));
    }
    
    /**
     * Test of isYesterday method, of class JavaTime.
     */
    @Test
    public void testIsYesterday_LocalDate_LocalDate()
    {
        LocalDate yesterday = JavaTime.parseLocalDate("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDate today     = JavaTime.parseLocalDate("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDate today2    = JavaTime.parseLocalDate("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDate tomorrow  = JavaTime.parseLocalDate("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertTrue(JavaTime.isYesterday(today, yesterday));
        assertFalse(JavaTime.isYesterday(today, today2));
        assertFalse(JavaTime.isYesterday(today, tomorrow));
    }
    
    /**
     * Test of isTomorrow method, of class JavaTime.
     */
    @Test
    public void testIsTomorrow_ZonedDateTime_ZonedDateTime()
    {
        ZonedDateTime yesterday = JavaTime.parseZonedDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        ZonedDateTime today     = JavaTime.parseZonedDateTime("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        ZonedDateTime today2    = JavaTime.parseZonedDateTime("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        ZonedDateTime tomorrow  = JavaTime.parseZonedDateTime("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertFalse(JavaTime.isTomorrow(today, yesterday));
        assertFalse(JavaTime.isTomorrow(today, today2));
        assertTrue(JavaTime.isTomorrow(today, tomorrow));
    }
    /**
     * Test of isTomorrow method, of class JavaTime.
     */
    @Test
    public void testIsTomorrow_LocalDateTime_LocalDateTime()
    {
        LocalDateTime yesterday = JavaTime.parseLocalDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDateTime today     = JavaTime.parseLocalDateTime("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDateTime today2    = JavaTime.parseLocalDateTime("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDateTime tomorrow  = JavaTime.parseLocalDateTime("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertFalse(JavaTime.isTomorrow(today, yesterday));
        assertFalse(JavaTime.isTomorrow(today, today2));
        assertTrue(JavaTime.isTomorrow(today, tomorrow));
    }
    
    /**
     * Test of isTomorrow method, of class JavaTime.
     */
    @Test
    public void testIsTomorrow_LocalDate_LocalDate()
    {
        LocalDate yesterday = JavaTime.parseLocalDate("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDate today     = JavaTime.parseLocalDate("2011-12-04T04:04:04+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);
        LocalDate today2    = JavaTime.parseLocalDate("2011-12-03T23:03:23+00:00[Europe/London]", JavaTime.EuropeMadrid);
        LocalDate tomorrow  = JavaTime.parseLocalDate("2011-12-05T05:05:05+01:00[Europe/Madrid]", JavaTime.EuropeMadrid);

        assertFalse(JavaTime.isTomorrow(today, yesterday));
        assertFalse(JavaTime.isTomorrow(today, today2));
        assertTrue(JavaTime.isTomorrow(today, tomorrow));
    }

    /**
     * Test of atStartOfWeek method, of class JavaTime.
     */
    @Test
    public void testAtStartOfWeek()
    {
        LocalDate date = LocalDate.of(2020, 1, 1);
        for(int i=0;i<100;i++)
        {
            LocalDate today = date.plusDays(i);
            LocalDate monday = JavaTime.atStartOfWeek(date.plusDays(i));
            assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());
            assertTrue(today.compareTo(monday)>=0);
        }
    }

    /**
     * Test of atEndOfWeek method, of class JavaTime.
     */
    @Test
    public void testAtEndOfWeek()
    {
        LocalDate date = LocalDate.of(2020, 1, 1);
        for(int i=0;i<100;i++)
        {
            LocalDate today = date.plusDays(i);
            LocalDate sunday = JavaTime.atEndOfWeek(date.plusDays(i));
            assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
            assertTrue(today.compareTo(sunday)<=0);
        }
    }

    /**
     * Test of atStartOfMonth method, of class JavaTime.
     */
    @Test
    public void testAtStartOfMonth() 
    {
        assertEquals(LocalDate.of(2020, 1, 1), JavaTime.atStartOfMonth(LocalDate.of(2020, 1, 21)));
        assertEquals(LocalDate.of(2020, 2, 1), JavaTime.atStartOfMonth(LocalDate.of(2020, 2, 29)));
        assertEquals(LocalDate.of(2020, 12, 1), JavaTime.atStartOfMonth(LocalDate.of(2020, 12, 31)));
    }

    /**
     * Test of atEndOfMonth method, of class JavaTime.
     */
    @Test
    public void testAtEndOfMonth() 
    {
        assertEquals(LocalDate.of(2020, 1, 31), JavaTime.atEndOfMonth(LocalDate.of(2020, 1, 21)));
        assertEquals(LocalDate.of(2020, 2, 29), JavaTime.atEndOfMonth(LocalDate.of(2020, 2, 1)));
        assertEquals(LocalDate.of(2020, 12, 31), JavaTime.atEndOfMonth(LocalDate.of(2020, 12, 1)));
    }

    /**
     * Test of atStartOfYear method, of class JavaTime.
     */
    @Test
    public void testAtStartOfYear() 
    {
        assertEquals(LocalDate.of(2020, 1, 1), JavaTime.atStartOfYear(LocalDate.of(2020, 1, 1)));
        assertEquals(LocalDate.of(2020, 1, 1), JavaTime.atStartOfYear(LocalDate.of(2020, 2, 29)));
        assertEquals(LocalDate.of(2020, 1, 1), JavaTime.atStartOfYear(LocalDate.of(2020, 12, 31)));
    }

    /**
     * Test of atEndOfYear method, of class JavaTime.
     */
    @Test
    public void testAtEndOfYear() 
    {
        assertEquals(LocalDate.of(2020, 12, 31), JavaTime.atEndOfYear(LocalDate.of(2020, 1, 1)));
        assertEquals(LocalDate.of(2020, 12, 31), JavaTime.atEndOfYear(LocalDate.of(2020, 2, 29)));
        assertEquals(LocalDate.of(2020, 12, 31), JavaTime.atEndOfYear(LocalDate.of(2020, 12, 31)));
    }

    /**
     * Test of schedule method, of class JavaTime.
     */
    @Test
    public void testSchedule_5args_1() 
    {
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Daily, 1, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1), false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Daily, 1, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), false);
            assertEquals(31, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        //----------------------------------------------------------------------
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Weekly, 1, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1), false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Weekly, 1, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), false);
            assertEquals(5, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
            assertEquals(LocalDate.of(2020, 1, 8), result[1]);
            assertEquals(LocalDate.of(2020, 1, 15), result[2]);
            assertEquals(LocalDate.of(2020, 1, 22), result[3]);
            assertEquals(LocalDate.of(2020, 1, 29), result[4]);
        }
        //----------------------------------------------------------------------
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1), false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 31), LocalDate.of(2020, 12, 31), false);
            assertEquals(12, result.length);
            assertEquals(LocalDate.of(2020, 1, 31), result[0]);
            assertEquals(LocalDate.of(2020, 2, 29), result[1]);
            assertEquals(LocalDate.of(2020, 3, 31), result[2]);
            assertEquals(LocalDate.of(2020, 4, 30), result[3]);
            assertEquals(LocalDate.of(2020, 5, 31), result[4]);
            assertEquals(LocalDate.of(2020, 6, 30), result[5]);
            assertEquals(LocalDate.of(2020, 7, 31), result[6]);
            assertEquals(LocalDate.of(2020, 8, 31), result[7]);
            assertEquals(LocalDate.of(2020, 9, 30), result[8]);
            assertEquals(LocalDate.of(2020, 10, 31), result[9]);
            assertEquals(LocalDate.of(2020, 11, 30), result[10]);
            assertEquals(LocalDate.of(2020, 12, 31), result[11]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 31), LocalDate.of(2020, 12, 31), true);
            assertEquals(12, result.length);
            assertEquals(LocalDate.of(2020, 1, 31), result[0]);
            assertEquals(LocalDate.of(2020, 3, 1), result[1]);
            assertEquals(LocalDate.of(2020, 3, 31), result[2]);
            assertEquals(LocalDate.of(2020, 5, 1), result[3]);
            assertEquals(LocalDate.of(2020, 5, 31), result[4]);
            assertEquals(LocalDate.of(2020, 7, 1), result[5]);
            assertEquals(LocalDate.of(2020, 7, 31), result[6]);
            assertEquals(LocalDate.of(2020, 8, 31), result[7]);
            assertEquals(LocalDate.of(2020, 10, 1), result[8]);
            assertEquals(LocalDate.of(2020, 10, 31), result[9]);
            assertEquals(LocalDate.of(2020, 12, 1), result[10]);
            assertEquals(LocalDate.of(2020, 12, 31), result[11]);
        }
        
    }

    /**
     * Test of schedule method, of class JavaTime.
     */
    @Test
    public void testSchedule_5args_2() 
    {
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Daily, 1, LocalDate.of(2020, 1, 1), 1, false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Daily, 1, LocalDate.of(2020, 1, 1), 31, false);
            assertEquals(31, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        //----------------------------------------------------------------------
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Weekly, 1, LocalDate.of(2020, 1, 1), 1, false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Weekly, 1, LocalDate.of(2020, 1, 1), 5, false);
            assertEquals(5, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
            assertEquals(LocalDate.of(2020, 1, 8), result[1]);
            assertEquals(LocalDate.of(2020, 1, 15), result[2]);
            assertEquals(LocalDate.of(2020, 1, 22), result[3]);
            assertEquals(LocalDate.of(2020, 1, 29), result[4]);
        }
        //----------------------------------------------------------------------
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 1), 1, false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 1), 1, false);
            assertEquals(1, result.length);
            assertEquals(LocalDate.of(2020, 1, 1), result[0]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 31), 12, false);
            assertEquals(12, result.length);
            assertEquals(LocalDate.of(2020, 1, 31), result[0]);
            assertEquals(LocalDate.of(2020, 2, 29), result[1]);
            assertEquals(LocalDate.of(2020, 3, 31), result[2]);
            assertEquals(LocalDate.of(2020, 4, 30), result[3]);
            assertEquals(LocalDate.of(2020, 5, 31), result[4]);
            assertEquals(LocalDate.of(2020, 6, 30), result[5]);
            assertEquals(LocalDate.of(2020, 7, 31), result[6]);
            assertEquals(LocalDate.of(2020, 8, 31), result[7]);
            assertEquals(LocalDate.of(2020, 9, 30), result[8]);
            assertEquals(LocalDate.of(2020, 10, 31), result[9]);
            assertEquals(LocalDate.of(2020, 11, 30), result[10]);
            assertEquals(LocalDate.of(2020, 12, 31), result[11]);
        }
        {
            LocalDate[] result = JavaTime.schedule(JavaTime.Periodicity.Monthly, 1, LocalDate.of(2020, 1, 31), 12, true);
            assertEquals(12, result.length);
            assertEquals(LocalDate.of(2020, 1, 31), result[0]);
            assertEquals(LocalDate.of(2020, 3, 1), result[1]);
            assertEquals(LocalDate.of(2020, 3, 31), result[2]);
            assertEquals(LocalDate.of(2020, 5, 1), result[3]);
            assertEquals(LocalDate.of(2020, 5, 31), result[4]);
            assertEquals(LocalDate.of(2020, 7, 1), result[5]);
            assertEquals(LocalDate.of(2020, 7, 31), result[6]);
            assertEquals(LocalDate.of(2020, 8, 31), result[7]);
            assertEquals(LocalDate.of(2020, 10, 1), result[8]);
            assertEquals(LocalDate.of(2020, 10, 31), result[9]);
            assertEquals(LocalDate.of(2020, 12, 1), result[10]);
            assertEquals(LocalDate.of(2020, 12, 31), result[11]);
        }
    }


    /**
     * Test of utc method, of class JavaTime.
     */
    @Test
    public void testUtc()
    {
        ZonedDateTime zdt = JavaTime.parseZonedDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        assertEquals("2011-12-03 03:03:03.000Z", JavaTime.utc(zdt).format(JavaTime.YYYY_MM_DD_HH_MM_SS_SSSX));
    }

    /**
     * Test of utcMinutes method, of class JavaTime.
     */
    @Test
    public void testUtcMinutes()
    {
        ZonedDateTime zdt = JavaTime.parseZonedDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        assertEquals("2011-12-03 03:03", JavaTime.utcMinutes(zdt));
    }

    /**
     * Test of utcSeconds method, of class JavaTime.
     */
    @Test
    public void testUtcSeconds()
    {
        ZonedDateTime zdt = JavaTime.parseZonedDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        assertEquals("2011-12-03 03:03:03", JavaTime.utcSeconds(zdt));
    }

    /**
     * Test of utcMilliSeconds method, of class JavaTime.
     */
    @Test
    public void testUtcMilliSeconds()
    {
        ZonedDateTime zdt = JavaTime.parseZonedDateTime("2011-12-03T03:03:03+00:00[Europe/London]", JavaTime.EuropeMadrid);
        assertEquals("2011-12-03 03:03:03.000", JavaTime.utcMilliSeconds(zdt));
    }

    /**
     * Test of max method, of class JavaTime.
     */
    @Test
    public void testMax_LocalDateArr()
    {
        LocalDate a = LocalDate.of(2022, 4, 1);
        LocalDate b = LocalDate.of(2022, 4, 2);
        LocalDate c = LocalDate.of(2022, 4, 3);
        LocalDate d = LocalDate.of(2022, 4, 4);
        LocalDate d2= LocalDate.of(2022, 4, 4);
        LocalDate e = LocalDate.of(2022, 4, 5);
        
        assertEquals(e, JavaTime.max(a,b,c,d,d2,e));
        assertEquals(e, JavaTime.max(e,d2,d,c,b,a));
        assertEquals(e, JavaTime.max(a,c,e,d,b));
        assertEquals(d, JavaTime.max(d,d2));
        assertEquals(d, JavaTime.max(d,d2,a));
    }

    /**
     * Test of max method, of class JavaTime.
     */
    @Test
    public void testMax_LocalDateTimeArr()
    {
        LocalDateTime a = LocalDateTime.of(2022, 4, 1, 1,2,3,4);
        LocalDateTime b = LocalDateTime.of(2022, 4, 2, 1,2,3,4);
        LocalDateTime c = LocalDateTime.of(2022, 4, 2, 1,2,3,5);
        LocalDateTime d = LocalDateTime.of(2022, 4, 4, 1,2,3,4);
        LocalDateTime d2= LocalDateTime.of(2022, 4, 4, 1,2,3,4);
        LocalDateTime e = LocalDateTime.of(2022, 4, 5, 1,2,3,4);
        
        assertEquals(e, JavaTime.max(a,b,c,d,d2,e));
        assertEquals(e, JavaTime.max(e,d2,d,c,b,a));
        assertEquals(e, JavaTime.max(a,c,e,d,b));
        assertEquals(d, JavaTime.max(d,d2));
        assertEquals(d, JavaTime.max(d,d2,a));
    }

    /**
     * Test of toString method, of class JavaTime.
     */
    @Test
    public void testToString()
    {
        assertEquals("", JavaTime.toString(Duration.ZERO, 0, JavaTime.Resolution.S));
        assertEquals("0s", JavaTime.toString(Duration.ZERO, 1, JavaTime.Resolution.S));
        assertEquals("0ms", JavaTime.toString(Duration.ZERO, 1, JavaTime.Resolution.MS));
        assertEquals("0m0s", JavaTime.toString(Duration.ZERO, 2, JavaTime.Resolution.S));

        assertEquals("1h", JavaTime.toString(Duration.ofMillis(3600_123), 1, JavaTime.Resolution.S));
        assertEquals("1h0m", JavaTime.toString(Duration.ofMillis(3600_123), 2, JavaTime.Resolution.S));
        assertEquals("1h0m0s", JavaTime.toString(Duration.ofMillis(3600_123), 3, JavaTime.Resolution.S));

        assertEquals("1h", JavaTime.toString(Duration.ofMillis(3999_123), 1, JavaTime.Resolution.S));
        assertEquals("1h6m", JavaTime.toString(Duration.ofMillis(3999_123), 2, JavaTime.Resolution.S));
        assertEquals("1h6m39s", JavaTime.toString(Duration.ofMillis(3999_123), 3, JavaTime.Resolution.S));

        long s1 = 24*3600 + 2*3600 + 3*60 + 4;
        assertEquals("1d2h3m4s", JavaTime.toString(Duration.ofSeconds(s1), 4, JavaTime.Resolution.MS));
        long ns1 = 5_000_006;
        assertEquals("1d2h3m4s5ms6ns", JavaTime.toString(Duration.ofSeconds(s1,ns1), 6, JavaTime.Resolution.MS));

        long s2 = 11*24*3600 + 22*3600 + 33*60 + 44;
        assertEquals("11d22h33m44s", JavaTime.toString(Duration.ofSeconds(s2), 4, JavaTime.Resolution.MS));
        long ns2 = 777_008_888;
        assertEquals("11d22h33m44s777ms8888ns", JavaTime.toString(Duration.ofSeconds(s2,ns2), 6, JavaTime.Resolution.MS));
    }

    /**
     * Test of max method, of class JavaTime.
     */
    @Test
    public void testMax_ZonedDateTimeArr()
    {
        ZonedDateTime expResult;
        ZonedDateTime result = JavaTime.max(ZonedDateTime.now(), ZonedDateTime.now(), expResult=ZonedDateTime.now());
        assertEquals(expResult, result);
    }

    /**
     * Test of min method, of class JavaTime.
     */
    @Test
    public void testMin_LocalDateArr()
    {
        LocalDate expResult = LocalDate.now();
        LocalDate result = JavaTime.min(expResult, expResult.plusDays(1), expResult.plusDays(2));
        assertEquals(expResult, result);
    }

    /**
     * Test of min method, of class JavaTime.
     */
    @Test
    public void testMin_LocalDateTimeArr()
    {
        LocalDateTime expResult = LocalDateTime.now();
        LocalDateTime result = JavaTime.min(expResult, LocalDateTime.now(), LocalDateTime.now());
        assertEquals(expResult, result);
    }

    /**
     * Test of min method, of class JavaTime.
     */
    @Test
    public void testMin_ZonedDateTimeArr()
    {
        ZonedDateTime expResult = ZonedDateTime.now();
        ZonedDateTime result = JavaTime.min(expResult, ZonedDateTime.now(), ZonedDateTime.now());
        assertEquals(expResult, result);
    }

    /**
     * Test of atStartOfYear method, of class JavaTime.
     */
    @Test
    public void testAtStartOfYear_int_ZoneId()
    {
        ZoneId zone = JavaTime.UTC;
        ZonedDateTime result = JavaTime.atStartOfYear(2023, zone);
        assertEquals(2023, result.getYear());
    }


    /**
     * Test of atStartOfDay method, of class JavaTime.
     */
    @Test
    public void testAtStartOfDay()
    {
        {
            ZonedDateTime startUTC = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, JavaTime.UTC);

            for (int i = 0; i < 25; i++)
            {
                ZonedDateTime itemUTC = startUTC.plusMinutes(i * 59);
                ZonedDateTime resultUTC = JavaTime.atStartOfDay(itemUTC);
                assertEquals(startUTC, resultUTC);
            }
        }
        {
            ZonedDateTime startNY = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, JavaTime.AmericaNew_York);

            for (int i = 0; i < 25; i++)
            {
                ZonedDateTime itemNY = startNY.plusMinutes(i * 59);
                ZonedDateTime resultNY = JavaTime.atStartOfDay(itemNY);
                assertEquals(startNY, resultNY);
            }
        }

    }

    /**
     * Test of asLocalDate method, of class JavaTime.
     */
    @Test
    public void testAsLocalDate()
    {
        LocalDate expected = LocalDate.of(2014, 1, 1);
        LocalDate result = JavaTime.asLocalDate(1388534400);
        assertEquals(expected, result);
    }

    /**
     * Test of asLocalDateTime method, of class JavaTime.
     */
    @Test
    public void testAsLocalDateTime()
    {
        LocalDateTime expected = LocalDateTime.of(2014, 1, 1, 0, 0, 0);
        LocalDateTime result = JavaTime.asLocalDateTime(1388534400);
        assertEquals(expected, result);
    }
}
