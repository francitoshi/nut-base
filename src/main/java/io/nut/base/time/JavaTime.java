/*
 *  JavaTime.java
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @author franci
 */
public class JavaTime
{
    public static final ZoneId UTC = ZoneId.of("UTC");
    
    public static final ZoneId AmericaNew_York = ZoneId.of("America/New_York");
    public static final ZoneId EuropeMadrid = ZoneId.of("Europe/Madrid");
    public static final ZoneId EuropeLondon = ZoneId.of("Europe/London");
    public static final ZoneId EuropeParis = ZoneId.of("Europe/Paris");

    public static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter YYYY_M_D = DateTimeFormatter.ofPattern("yyyy-M-d");
    public static final DateTimeFormatter D_MMM_YY = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yy").toFormatter(Locale.US);
    public static final DateTimeFormatter DD_MMM_YYYY = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter(Locale.US);
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZZ");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZZZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZZZ");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZZZZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZZZZ");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SSZ = DateTimeFormatter.ISO_OFFSET_DATE_TIME;//ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSXXX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SSXXX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SX");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SX");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSX");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSX");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSSS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSX");
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SSSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSX");
    
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSz = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssz");//2019-10-04 02:03:00CEST
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_z = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");//2019-10-04 02:03:00 CEST
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSS_z = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");//2019-10-04 02:03:00.000 CEST
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSx = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx");//2019-10-04 02:03:00+02
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSxx = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxx");//2019-10-04 02:03:00+0200
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSxxx = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxxx");//2019-10-04 02:03:00+02:00

    public static final DateTimeFormatter DD_MM_YYYY_HH_MM_SS = DateTimeFormatter.ofPattern("dd'/'MM'/'yyyy HH:mm:ss");//04-10-2019 02:03:04
    
    public static LocalDate max(LocalDate... t)
    {
        LocalDate m = null;
        if(t.length>0)
        {
            for(int i=0;i<t.length;i++)
            {
                LocalDate item = t[i];
                if(m==null || (item!=null && item.compareTo(m)>0))
                {
                    m = item;
                }
            }
        }
        return m;
    }
    public static LocalDateTime max(LocalDateTime... t)
    {
        LocalDateTime m = null;
        if(t.length>0)
        {
            for(int i=0;i<t.length;i++)
            {
                LocalDateTime item = t[i];
                if(m==null || (item!=null && item.compareTo(m)>0))
                {
                    m = item;
                }
            }
        }
        return m;
    }
    public static ZonedDateTime max(ZonedDateTime... t)
    {
        ZonedDateTime m = null;
        if(t.length>0)
        {
            for(int i=0;i<t.length;i++)
            {
                ZonedDateTime item = t[i];
                if(m==null || (item!=null && item.compareTo(m)>0))
                {
                    m = item;
                }
            }
        }
        return m;
    }

    public static LocalDate min(LocalDate... t)
    {
        LocalDate m = null;
        if(t.length>0)
        {
            for(int i=0;i<t.length;i++)
            {
                LocalDate item = t[i];
                if(m==null || (item!=null && item.compareTo(m)<0))
                {
                    m = item;
                }
            }
        }
        return m;
    }
    public static LocalDateTime min(LocalDateTime... t)
    {
        LocalDateTime m = null;
        if(t.length>0)
        {
            for(int i=0;i<t.length;i++)
            {
                LocalDateTime item = t[i];
                if(m==null || (item!=null && item.compareTo(m)<0))
                {
                    m = item;
                }
            }
        }
        return m;
    }
    public static ZonedDateTime min(ZonedDateTime... t)
    {
        ZonedDateTime m = null;
        if(t.length>0)
        {
            for(int i=0;i<t.length;i++)
            {
                ZonedDateTime item = t[i];
                if(m==null || (item!=null && item.compareTo(m)<0))
                {
                    m = item;
                }
            }
        }
        return m;
    }
    static class Parser
    {
        static final int D = 1;
        static final int DT = 2;
        static final int DTZ = 3;
        
        final DateTimeFormatter dtf;
        final int type;
        final int min;
        final int max;
        final String regex;
        private volatile Pattern pattern;
        public Parser(DateTimeFormatter dtf, int min, int max, int type, String regex)
        {
            this.dtf = dtf;
            this.min = min;
            this.max = max;
            this.type = type;
            this.regex = regex;
        }
        boolean matches(String s)
        {
            if(pattern==null)
            {
                pattern = Pattern.compile(regex);
            }
            return pattern.matcher(s).matches();
        }
    }
    enum ParserHolder
    {
        INSTANCE;
        final Parser[] parsers = new Parser[]
        {
            _b(YYYYMMDD,                  8,  8,  Parser.D,   "[0-9][0-9][0-9][0-9][0-1][0-9][0-3][0-9]"),   //yyyyMMdd => 20111203
            _b(YYYY_MM_DD,                10, 10, Parser.D,   "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]"), //yyyy-MM-dd => 2011-12-03
            _b(YYYY_M_D,                  8,  10, Parser.D,   "[0-9][0-9][0-9][0-9]-[0-1]?[0-9]-[0-3]?[0-9]"), //yyyy-M-d => 2011-2-3
            _b(D_MMM_YY,                  8,  9,  Parser.D,   "[0-3]?[0-9]-[A-Za-z][A-Za-z][A-Za-z]-[0-9][0-9]"), //d-MMM-yy => 8-Ago-37 19-Ago-37
            _b(DD_MMM_YYYY,               11, 11, Parser.D,   "[0-3][0-9]-[A-Za-z][A-Za-z][A-Za-z]-[0-9][0-9][0-9][0-9]"), //dd-MMM-yyyy => 11-Ago-2037
            _b(YYYY_MM_DD_HH_MM,          16, 16, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]"),         
            _b(YYYY_MM_DD_HH_MM_SS,       19, 19, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9]"),
            _b(YYYY_MM_DDTHH_MM_SS,       19, 19, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]"),
            _b(YYYY_MM_DD_HH_MM_SSX,      20, 24, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DDTHH_MM_SSX,      20, 24, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DD_HH_MM_SSXXX,    25, 25, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][+-][0-1][0-9]:[0-5][0-9]"),
            _b(YYYY_MM_DDTHH_MM_SSXXX,    25, 25, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][+-][0-1][0-9]:[0-5][0-9]"),
            _b(YYYY_MM_DD_HH_MM_SS_S,     21, 21, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9]"),
            _b(YYYY_MM_DD_HH_MM_SS_SX,    22, 26, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DDTHH_MM_SS_SX,    22, 26, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DD_HH_MM_SS_SS,    22, 22, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9]"),
            _b(YYYY_MM_DD_HH_MM_SS_SSX,   23, 27, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DDTHH_MM_SS_SSX,   23, 27, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DD_HH_MM_SS_SSS,   23, 23, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9]"),
            _b(YYYY_MM_DD_HH_MM_SS_SSSX,  24, 28, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DDTHH_MM_SS_SSSX,  24, 28, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DD_HH_MM_SS_SSSS,  24, 24, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9][0-9]"),
            _b(YYYY_MM_DD_HH_MM_SS_SSSSX, 25, 29, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(YYYY_MM_DDTHH_MM_SS_SSSSX, 25, 29, Parser.DT,  "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9][0-9](Z|[+-][0-1][0-9]([0-5][0-9])?)"),
            _b(DateTimeFormatter.ISO_ZONED_DATE_TIME, 19, 99, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9][+-][0-1][0-9]:[0-5][0-9]\\[[A-zA-Z0-9_]+/[A-zA-Z0-9_]+\\]"),
            _b(YYYY_MM_DD_HH_MM_SS_z, 23, 24, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9] (Z|[A-Z][A-Z][A-Z])"),
            _b(YYYY_MM_DD_HH_MM_SS_SSS_z, 25, 27, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9] (Z|[A-Z][A-Z][A-Z])"),
            
        };
        static Parser _b(DateTimeFormatter dtf, int min, int max, int type, String regex)
        {
            return new Parser(dtf, min, max, type, regex);
        }
    }

    public static ZonedDateTime parseZonedDateTime(String s, ZoneId defaultZone)
    {
        return parseZonedDateTime(s, defaultZone, null);
    }
    public static ZonedDateTime parseZonedDateTime(String s, ZoneId defaultZone, Locale locale)
    {
        int errors=0;
        DateTimeParseException cause=null;
        for(Parser item : ParserHolder.INSTANCE.parsers)
        {
            if(s.length()<item.min || s.length()>item.max)
            {
                continue;
            }
            if(!item.matches(s))
            {
                continue;
            }
            try
            {
                DateTimeFormatter dtf = locale!=null ? item.dtf.withLocale(locale) : item.dtf;
                if(item.type==Parser.D)
                {
                    return LocalDate.parse(s, dtf).atStartOfDay().atZone(defaultZone);
                }
                if(item.type==Parser.DT)
                {
                    return LocalDateTime.parse(s, dtf).atZone(defaultZone);
                }
                if(item.type==Parser.DTZ)
                {
                    return ZonedDateTime.parse(s, dtf);
                }
                throw new DateTimeParseException("Can not parse date "+s, s, errors, cause);
            }
            catch(DateTimeParseException ex)
            {
                Logger.getLogger(JavaTime.class.getName()).log(Level.SEVERE, s, cause);
                errors++;
                cause = ex;
            }
        }
        throw new DateTimeParseException("Can't parse date "+s, s, errors, cause);
    }
    public static LocalDateTime parseLocalDateTime(String s, ZoneId defaultZone)
    {
        return parseZonedDateTime(s, defaultZone).withZoneSameInstant(defaultZone).toLocalDateTime();
    }
    public static LocalDate parseLocalDate(String s, ZoneId defaultZone)
    {
        return parseZonedDateTime(s, defaultZone).withZoneSameInstant(defaultZone).toLocalDate();
    }

    public static LocalDate parseLocalDate(String s)
    {
        int errors = 0;
        DateTimeParseException cause = null;
        for(Parser item : ParserHolder.INSTANCE.parsers)
        {
            if(s.length()<item.min || s.length()>item.max)
            {
                continue;
            }
            if(!item.matches(s))
            {
                continue;
            }
            try
            {
                if(item.type==Parser.D)
                {
                    return LocalDate.parse(s, item.dtf);
                }
            }
            catch (DateTimeParseException ex)
            {
                Logger.getLogger(JavaTime.class.getName()).log(Level.SEVERE, s, cause);
                errors++;
                cause = ex;
            }
        }
        throw new DateTimeParseException("Can't parse date " + s, s, errors, cause);
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isToday(ZonedDateTime today, ZonedDateTime other)
    {
        other = other.withZoneSameInstant(today.getZone());
        return today.toLocalDate().equals(other.toLocalDate());
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isToday(LocalDateTime today, LocalDateTime other)
    {
        return today.toLocalDate().equals(other.toLocalDate());
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isToday(LocalDate today, LocalDate other)
    {
        return today.equals(other);
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isYesterday(ZonedDateTime today, ZonedDateTime other)
    {
        other = other.withZoneSameInstant(today.getZone());
        return today.minusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isYesterday(LocalDateTime today, LocalDateTime other)
    {
        return today.minusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isYesterday(LocalDate today, LocalDate other)
    {
        return today.minusDays(1).equals(other);
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be tested
     * @return true if both dates contains the same day
     */
    public static boolean isTomorrow(ZonedDateTime today, ZonedDateTime other)
    {
        other = other.withZoneSameInstant(today.getZone());
        return today.plusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isTomorrow(LocalDateTime today, LocalDateTime other)
    {
        return today.plusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     *
     * @param today the date to be considered as today
     * @param other the date to be compared with
     * @return true if both dates contains the same day
     */
    public static boolean isTomorrow(LocalDate today, LocalDate other)
    {
        return today.plusDays(1).equals(other);
    }

    public static ZonedDateTime atStartOfDay(ZonedDateTime dateTime)
    {
        LocalDateTime localDateTime = dateTime.toLocalDateTime();
        LocalDateTime startOfDay = localDateTime.toLocalDate().atStartOfDay();
        return startOfDay.atZone(dateTime.getZone());
    }

    public static LocalDate atStartOfWeek(LocalDate date)
    {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    public static LocalDate atEndOfWeek(LocalDate date)
    {
        return date.plusDays(DayOfWeek.SUNDAY.getValue() - date.getDayOfWeek().getValue());
    }

    public static LocalDate atStartOfMonth(LocalDate date)
    {
        return date.withDayOfMonth(1);
    }

    public static LocalDate atEndOfMonth(LocalDate date)
    {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    public static LocalDate atStartOfYear(LocalDate date)
    {
        return LocalDate.of(date.getYear(), 1, 1);
    }

    public static ZonedDateTime atStartOfYear(ZonedDateTime zdt)
    {
        return ZonedDateTime.of(zdt.getYear(), 1, 1, 0, 0, 0, 0, zdt.getZone());
    }

    public static ZonedDateTime atStartOfYear(int year, ZoneId zone)
    {
        return ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, zone);
    }

    public static LocalDate atEndOfYear(LocalDate date)
    {
        return LocalDate.of(date.getYear(), 12, 31);
    }

    public static ZonedDateTime atEndOfYear(ZonedDateTime zdt)
    {
        return ZonedDateTime.of(zdt.getYear(), 12, 31, 23, 59, 59, 999_999_999, zdt.getZone());
    }

    public static ZonedDateTime atEndOfYear(int year, ZoneId zone)
    {
        return ZonedDateTime.of(year, 12, 31, 23, 59, 59, 999_999_999, zone);
    }

    public enum Periodicity
    {
        Daily, Weekly, Monthly, Yearly
    }

    private static LocalDate next(Periodicity type, LocalDate date, int skip, boolean after)
    {
        LocalDate date2;
        switch (type)
        {
            case Daily:
                return date.plusDays(skip);
            case Weekly:
                return date.plusWeeks(skip);
            case Monthly:
                date2 = date.plusMonths(skip);
                break;
            case Yearly:
                date2 = date.plusYears(skip);
                break;
            default:
                return null;
        }
        if(after && date2.getDayOfMonth()<date.getDayOfMonth())
        {
            date2 = date2.plusDays(1);
        }
        return date2;
    }

    public static LocalDate[] schedule(Periodicity type, int period, LocalDate startAt, LocalDate endAt, boolean after)
    {
        ArrayList<LocalDate> items = new ArrayList<>();
        LocalDate date = startAt;
        for(int i = 1; date.isBefore(endAt) || date.isEqual(endAt);i++)
        {
            items.add(date);
            date = next(type, startAt, i * period, after);
        }
        return items.toArray(new LocalDate[items.size()]);
    }

    public static LocalDate[] schedule(Periodicity type, int period, LocalDate startAt, int count, boolean after)
    {
        return schedule(type, period, startAt, next(type, startAt, period * (count - 1), after), after);
    }

    public static ZonedDateTime utc(ZonedDateTime zdt)
    {
        return zdt.withZoneSameInstant(UTC);
    }

    public static String utcMinutes(ZonedDateTime zdt)
    {
        return utc(zdt).format(YYYY_MM_DD_HH_MM);
    }

    public static String utcSeconds(ZonedDateTime zdt)
    {
        return utc(zdt).format(YYYY_MM_DD_HH_MM_SS);
    }

    public static String utcMilliSeconds(ZonedDateTime zdt)
    {
        return utc(zdt).format(YYYY_MM_DD_HH_MM_SS_SSS);
    }

    private static final String[] DURATION_UNITS = "d,h,m,s,ms,ns".split(",");

    public static enum Resolution {D,H,M,S,MS,NS}

    public static String toString(Duration duration, int elements, Resolution resolution)
    {
        long[] values = new long[]
        {
            duration.toDays(),
            duration.toHours() % 24,
            duration.toMinutes() % 60,
            duration.getSeconds() % 60,
            duration.toMillis() % 1000,
            duration.toNanos() % 1000_000
        };
        boolean started = false;
        int count = 0;
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<values.length && count<elements;i++)
        {
            if(started || values[i]!=0 || i + elements>resolution.ordinal())
            {
                sb.append(values[i]).append(DURATION_UNITS[i]);
                started = true;
                count++;
            }
        }
        return sb.toString();
    }

    public static long epochSecond()
    {
        return ZonedDateTime.now().toEpochSecond();
    }

    public static long epochSecond(ZonedDateTime zdt)
    {
        return zdt.toEpochSecond();
    }

    public static long epochSecond(LocalDateTime ldt)
    {
        return ldt.toEpochSecond(ZoneOffset.UTC);
    }

    public static long epochSecond(LocalDate ld)
    {
        return ld.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
    }
}
