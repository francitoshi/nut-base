/*
 *  JavaTime.java
 *
 *  Copyright (c) 2020-2025 francitoshi@gmail.com
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

import java.security.InvalidParameterException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utility class providing convenience methods for the Java 8 date/time API.
 *
 * <p>Covers the following areas:
 * <ul>
 * <li>Predefined {@link ZoneId} constants for common time zones.</li>
 * <li>Predefined {@link DateTimeFormatter} constants for a wide range of
 * date/time string patterns, including ISO variants, zone-offset variants,
 * and fractional-second variants.</li>
 * <li>Null-safe {@code min} / {@code max} helpers for {@link LocalDate},
 * {@link LocalDateTime}, and {@link ZonedDateTime}.</li>
 * <li>Flexible date/time parsing that auto-detects the format from the input
 * string without requiring callers to know the format in advance.</li>
 * <li>Boundary helpers: start/end of day, week, month, and year.</li>
 * <li>Periodic scheduling of dates ({@link #schedule}).</li>
 * <li>UTC formatting shortcuts ({@link #utcMinutes}, {@link #utcSeconds},
 * {@link #utcMilliSeconds}).</li>
 * <li>Human-readable {@link Duration} formatting ({@link #toString(Duration,
 * int, Resolution)}).</li>
 * <li>Epoch-second conversions to/from {@link LocalDate},
 * {@link LocalDateTime}, and {@link Instant}.</li>
 * </ul>
 *
 * <p>All methods are static; this class is not instantiable.
 */
public class JavaTime
{
    // -----------------------------------------------------------------------
    // Time-zone constants
    // -----------------------------------------------------------------------

    /** UTC time zone ({@code "UTC"}). */
    public static final ZoneId UTC = ZoneId.of("UTC");
    
    /** Eastern Time – New York ({@code "America/New_York"}). */
    public static final ZoneId AmericaNew_York = ZoneId.of("America/New_York");

    /** Australian Eastern Time – Sydney ({@code "Australia/Sydney"}). */
    public static final ZoneId AustraliaSydney = ZoneId.of("Australia/Sydney");

    /** Central European Time – Madrid ({@code "Europe/Madrid"}). */
    public static final ZoneId EuropeMadrid = ZoneId.of("Europe/Madrid");

    /** Greenwich Mean Time – London ({@code "Europe/London"}). */
    public static final ZoneId EuropeLondon = ZoneId.of("Europe/London");

    /** Central European Time – Paris ({@code "Europe/Paris"}). */
    public static final ZoneId EuropeParis = ZoneId.of("Europe/Paris");

    /** Hawaii–Aleutian Standard Time ({@code "Pacific/Honolulu"}). */
    public static final ZoneId PacificHonolulu = ZoneId.of("Pacific/Honolulu");

    // -----------------------------------------------------------------------
    // DateTimeFormatter constants
    // -----------------------------------------------------------------------

    /** Compact date without separators: {@code yyyyMMdd} (e.g. {@code 20111203}). */
    public static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** ISO local date: {@code yyyy-MM-dd} (e.g. {@code 2011-12-03}). */
    public static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Local date with single-digit month/day allowed: {@code yyyy-M-d} (e.g. {@code 2011-2-3}). */
    public static final DateTimeFormatter YYYY_M_D = DateTimeFormatter.ofPattern("yyyy-M-d");

    /** Short date with abbreviated month name: {@code d-MMM-yy} (e.g. {@code 8-Ago-37}), case-insensitive, US locale. */
    public static final DateTimeFormatter D_MMM_YY = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yy").toFormatter(Locale.US);

    /** Date with abbreviated month name: {@code dd-MMM-yyyy} (e.g. {@code 11-Ago-2037}), case-insensitive, US locale. */
    public static final DateTimeFormatter DD_MMM_YYYY = new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-yyyy").toFormatter(Locale.US);

    /** Date and time to the minute: {@code yyyy-MM-dd HH:mm}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Date and time to the second: {@code yyyy-MM-dd HH:mm:ss}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** ISO-like date/time with {@code T} separator: {@code yyyy-MM-dd'T'HH:mm:ss}. */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Date/time with RFC 822 zone offset ({@code Z} pattern): {@code yyyy-MM-dd HH:mm:ssZ}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");

    /** Date/time with two-digit zone offset ({@code ZZ} pattern): {@code yyyy-MM-dd HH:mm:ssZZ}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZZ");

    /** Date/time with zone id ({@code ZZZ} pattern): {@code yyyy-MM-dd HH:mm:ssZZZ}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZZZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZZZ");

    /** Date/time with long localized zone offset ({@code ZZZZ} pattern): {@code yyyy-MM-dd HH:mm:ssZZZZ}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSZZZZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZZZZ");

    /**
     * ISO offset date/time with {@code T} separator, backed by
     * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
     */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SSZ = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** Date/time with single-digit zone offset ({@code X} pattern): {@code yyyy-MM-dd HH:mm:ssX}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssX");

    /** Date/time with {@code T} separator and single-digit zone offset: {@code yyyy-MM-dd'T'HH:mm:ssX}. */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

    /** Date/time with colon-separated zone offset ({@code XXX} pattern): {@code yyyy-MM-dd HH:mm:ssXXX}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSXXX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");

    /** Date/time with {@code T} separator and colon-separated zone offset: {@code yyyy-MM-dd'T'HH:mm:ssXXX}. */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SSXXX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** Date/time with one fractional-second digit: {@code yyyy-MM-dd HH:mm:ss.S}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_S = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");

    /** Date/time with one fractional-second digit and zone offset: {@code yyyy-MM-dd HH:mm:ss.SX}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SX");

    /** Date/time with {@code T} separator, one fractional-second digit and zone offset: {@code yyyy-MM-dd'T'HH:mm:ss.SX}. */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SX");

    /** Date/time with two fractional-second digits: {@code yyyy-MM-dd HH:mm:ss.SS}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS");

    /** Date/time with two fractional-second digits and zone offset: {@code yyyy-MM-dd HH:mm:ss.SSX}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSX");

    /** Date/time with {@code T} separator, two fractional-second digits and zone offset: {@code yyyy-MM-dd'T'HH:mm:ss.SSX}. */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSX");

    /** Date/time with milliseconds: {@code yyyy-MM-dd HH:mm:ss.SSS}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Date/time with milliseconds and zone offset: {@code yyyy-MM-dd HH:mm:ss.SSSX}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSX");

    /** Date/time with {@code T} separator, milliseconds and zone offset: {@code yyyy-MM-dd'T'HH:mm:ss.SSSX}. */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    /** Date/time with four fractional-second digits: {@code yyyy-MM-dd HH:mm:ss.SSSS}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSSS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSS");

    /** Date/time with four fractional-second digits and zone offset: {@code yyyy-MM-dd HH:mm:ss.SSSSX}. */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSX");

    /** Date/time with {@code T} separator, four fractional-second digits and zone offset: {@code yyyy-MM-dd'T'HH:mm:ss.SSSSX}. */
    public static final DateTimeFormatter YYYY_MM_DDTHH_MM_SS_SSSSX = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSX");
    
    /** Date/time with abbreviated zone name directly appended: {@code yyyy-MM-dd HH:mm:ssz} (e.g. {@code 2019-10-04 02:03:00CEST}). */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSz = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssz");

    /** Date/time with abbreviated zone name separated by a space: {@code yyyy-MM-dd HH:mm:ss z} (e.g. {@code 2019-10-04 02:03:00 CEST}). */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_z = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    /** Date/time with milliseconds and abbreviated zone name: {@code yyyy-MM-dd HH:mm:ss.SSS z} (e.g. {@code 2019-10-04 02:03:00.000 CEST}). */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_SSS_z = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");

    /** Date/time with two-digit zone offset ({@code x} pattern): {@code yyyy-MM-dd HH:mm:ssx} (e.g. {@code 2019-10-04 02:03:00+02}). */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSx = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx");

    /** Date/time with four-digit zone offset ({@code xx} pattern): {@code yyyy-MM-dd HH:mm:ssxx} (e.g. {@code 2019-10-04 02:03:00+0200}). */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSxx = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxx");

    /** Date/time with colon-separated zone offset ({@code xxx} pattern): {@code yyyy-MM-dd HH:mm:ssxxx} (e.g. {@code 2019-10-04 02:03:00+02:00}). */
    public static final DateTimeFormatter YYYY_MM_DD_HH_MM_SSxxx = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssxxx");

    /** Date/time with slash-separated day/month/year: {@code dd'/'MM'/'yyyy HH:mm:ss} (e.g. {@code 04/10/2019 02:03:04}). */
    public static final DateTimeFormatter DD_MM_YYYY_HH_MM_SS = DateTimeFormatter.ofPattern("dd'/'MM'/'yyyy HH:mm:ss");

    /** Time only, to the second: {@code HH:mm:ss} (e.g. {@code 02:03:04}). */
    public static final DateTimeFormatter HH_MM_SS = DateTimeFormatter.ofPattern("HH:mm:ss");
 
    /** Time only, to the minute: {@code HH:mm} (e.g. {@code 02:03}). */
    public static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    
    // -----------------------------------------------------------------------
    // min / max
    // -----------------------------------------------------------------------

    /**
     * Returns the latest of the supplied {@link LocalDate} values, ignoring
     * {@code null} elements.
     *
     * @param t zero or more dates, may contain {@code null} elements
     * @return the maximum date, or {@code null} if {@code t} is empty or all
     * elements are {@code null}
     */
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

    /**
     * Returns the latest of the supplied {@link LocalDateTime} values, ignoring
     * {@code null} elements.
     *
     * @param t zero or more date-times, may contain {@code null} elements
     * @return the maximum date-time, or {@code null} if {@code t} is empty or
     * all elements are {@code null}
     */
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

    /**
     * Returns the latest of the supplied {@link ZonedDateTime} values, ignoring
     * {@code null} elements.
     *
     * <p>Comparison is chronological; time-zone differences are taken into
     * account by the underlying {@link ZonedDateTime#compareTo} implementation.
     *
     * @param t zero or more zoned date-times, may contain {@code null} elements
     * @return the maximum zoned date-time, or {@code null} if {@code t} is
     * empty or all elements are {@code null}
     */
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

    /**
     * Returns the earliest of the supplied {@link LocalDate} values, ignoring
     * {@code null} elements.
     *
     * @param t zero or more dates, may contain {@code null} elements
     * @return the minimum date, or {@code null} if {@code t} is empty or all
     * elements are {@code null}
     */
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

    /**
     * Returns the earliest of the supplied {@link LocalDateTime} values,
     * ignoring {@code null} elements.
     *
     * @param t zero or more date-times, may contain {@code null} elements
     * @return the minimum date-time, or {@code null} if {@code t} is empty or
     * all elements are {@code null}
     */
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

    /**
     * Returns the earliest of the supplied {@link ZonedDateTime} values,
     * ignoring {@code null} elements.
     *
     * <p>Comparison is chronological; time-zone differences are taken into
     * account by the underlying {@link ZonedDateTime#compareTo} implementation.
     *
     * @param t zero or more zoned date-times, may contain {@code null} elements
     * @return the minimum zoned date-time, or {@code null} if {@code t} is
     * empty or all elements are {@code null}
     */
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

    // -----------------------------------------------------------------------
    // Internal parsing infrastructure
    // -----------------------------------------------------------------------

    /**
     * Internal descriptor that pairs a {@link DateTimeFormatter} with the
     * length bounds and regex that characterise its string representation,
     * allowing fast pre-filtering before attempting a full parse.
     */
    static class Parser
    {
        /** Parser type constant: date only. */
        static final int D = 1;
        /** Parser type constant: date and time (no zone). */
        static final int DT = 2;
        /** Parser type constant: date, time, and zone. */
        static final int DTZ = 3;
        
        /** The formatter used to parse matching strings. */
        final DateTimeFormatter dtf;
        /** Type of temporal value produced ({@link #D}, {@link #DT}, or {@link #DTZ}). */
        final int type;
        /** Minimum string length accepted by this parser. */
        final int min;
        /** Maximum string length accepted by this parser. */
        final int max;
        /** Regular expression that the input string must match. */
        final String regex;
        /** Lazily compiled pattern, volatile for safe publication. */
        private volatile Pattern pattern;

        /**
         * Constructs a {@code Parser} descriptor.
         *
         * @param dtf   the formatter to use for parsing
         * @param min   minimum accepted string length
         * @param max   maximum accepted string length
         * @param type  temporal type ({@link #D}, {@link #DT}, or {@link #DTZ})
         * @param regex regular expression the input must fully match
         */
        public Parser(DateTimeFormatter dtf, int min, int max, int type, String regex)
        {
            this.dtf = dtf;
            this.min = min;
            this.max = max;
            this.type = type;
            this.regex = regex;
        }

        /**
         * Returns {@code true} if {@code s} fully matches this parser's regex.
         * The underlying {@link Pattern} is compiled lazily on first use.
         *
         * @param s the string to test; must not be {@code null}
         * @return {@code true} if the string matches
         */
        boolean matches(String s)
        {
            if(pattern==null)
            {
                pattern = Pattern.compile(regex);
            }
            return pattern.matcher(s).matches();
        }
    }

    /**
     * Lazy-initialisation holder for the ordered array of {@link Parser}
     * descriptors used by the auto-detecting parse methods.
     *
     * <p>Uses the enum singleton pattern to guarantee thread-safe,
     * once-only initialisation without explicit synchronisation.
     */
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
            _b(YYYY_MM_DD_HH_MM_SS_z,     21, 24, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9] ([A-Z]{1,4})"),
            _b(YYYY_MM_DD_HH_MM_SS_SSS_z, 25, 28, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][.][0-9][0-9][0-9] ([A-Z]{1,4})"),
            _b(YYYY_MM_DD_HH_MM_SSz,      20, 23, Parser.DTZ, "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] [0-2][0-9]:[0-5][0-9]:[0-5][0-9][A-Z]{1,4}"),   //2019-10-04 02:03:00CEST
        };
            
        /**
         * Factory helper that constructs a {@link Parser} for inclusion in the
         * {@link #parsers} array.
         *
         * @param dtf   the formatter
         * @param min   minimum string length
         * @param max   maximum string length
         * @param type  temporal type constant
         * @param regex matching regular expression
         * @return a new {@link Parser} instance
         */
        static Parser _b(DateTimeFormatter dtf, int min, int max, int type, String regex)
        {
            return new Parser(dtf, min, max, type, regex);
        }
    }

    // -----------------------------------------------------------------------
    // Parsing
    // -----------------------------------------------------------------------

    /**
     * Parses a date/time string into a {@link ZonedDateTime} using the JVM
     * default locale, applying {@code defaultZone} when the string contains no
     * zone information.
     *
     * @param s           the date/time string to parse; must not be {@code null}
     * @param defaultZone the zone to assign when the string encodes a date-only
     *                    or zone-free date-time; must not be {@code null}
     * @return the parsed {@link ZonedDateTime}
     * @throws DateTimeParseException if no registered parser can handle the string
     * @see #parseZonedDateTime(String, ZoneId, Locale)
     */
    public static ZonedDateTime parseZonedDateTime(String s, ZoneId defaultZone)
    {
        return parseZonedDateTime(s, defaultZone, null);
    }

    /**
     * Parses a date/time string into a {@link ZonedDateTime}, auto-detecting
     * the format from the registered {@link Parser} descriptors.
     *
     * <p>Each candidate parser is first filtered by string length and regex
     * before a full parse is attempted, so the method is efficient even with
     * the large number of supported formats. Parsers are tried in registration
     * order; the first successful result is returned.
     *
     * <p>The {@code defaultZone} is used in two situations:
     * <ul>
     * <li>The string encodes a date only — the result is midnight at the start
     * of that date in {@code defaultZone}.</li>
     * <li>The string encodes a date-time without zone info — the result is that
     * date-time interpreted in {@code defaultZone}.</li>
     * </ul>
     *
     * @param s           the date/time string to parse; must not be {@code null}
     * @param defaultZone the zone applied when the input lacks zone information;
     *                    must not be {@code null}
     * @param locale      the locale used to interpret locale-sensitive fields
     *                    such as month names, or {@code null} to use each
     *                    formatter's default locale
     * @return the parsed {@link ZonedDateTime}
     * @throws DateTimeParseException if no registered parser can handle the string
     */
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

    /**
     * Parses a date/time string into a {@link LocalDateTime} expressed in
     * {@code defaultZone}.
     *
     * <p>The string is first parsed as a {@link ZonedDateTime} via
     * {@link #parseZonedDateTime(String, ZoneId)}, then converted to
     * {@code defaultZone} before extracting the local date-time.
     *
     * @param s           the date/time string to parse; must not be {@code null}
     * @param defaultZone the zone used for format detection and the final
     *                    conversion; must not be {@code null}
     * @return the parsed {@link LocalDateTime} in {@code defaultZone}
     * @throws DateTimeParseException if no registered parser can handle the string
     */
    public static LocalDateTime parseLocalDateTime(String s, ZoneId defaultZone)
    {
        return parseZonedDateTime(s, defaultZone).withZoneSameInstant(defaultZone).toLocalDateTime();
    }

    /**
     * Parses a date/time string into a {@link LocalDate} expressed in
     * {@code defaultZone}.
     *
     * <p>The string is first parsed as a {@link ZonedDateTime} via
     * {@link #parseZonedDateTime(String, ZoneId)}, then converted to
     * {@code defaultZone} before extracting the local date.
     *
     * @param s           the date/time string to parse; must not be {@code null}
     * @param defaultZone the zone used for format detection and the final
     *                    conversion; must not be {@code null}
     * @return the parsed {@link LocalDate} in {@code defaultZone}
     * @throws DateTimeParseException if no registered parser can handle the string
     */
    public static LocalDate parseLocalDate(String s, ZoneId defaultZone)
    {
        return parseZonedDateTime(s, defaultZone).withZoneSameInstant(defaultZone).toLocalDate();
    }

    /**
     * Parses a date-only string (no time, no zone) into a {@link LocalDate}.
     *
     * <p>Only parsers of type {@link Parser#D} are consulted; strings that
     * include a time component will not match and a {@link DateTimeParseException}
     * will be thrown.
     *
     * @param s the date string to parse; must not be {@code null}
     * @return the parsed {@link LocalDate}
     * @throws DateTimeParseException if no date-only parser can handle the string
     */
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

    // -----------------------------------------------------------------------
    // isToday / isYesterday / isTomorrow
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if {@code other} falls on the same calendar day as
     * {@code today}, after converting {@code other} to {@code today}'s zone.
     *
     * @param today the reference date-time defining "today" and the zone to use
     * @param other the date-time to test
     * @return {@code true} if both represent the same local date in
     * {@code today}'s zone
     */
    public static boolean isToday(ZonedDateTime today, ZonedDateTime other)
    {
        other = other.withZoneSameInstant(today.getZone());
        return today.toLocalDate().equals(other.toLocalDate());
    }

    /**
     * Returns {@code true} if {@code other} falls on the same calendar day as
     * {@code today}.
     *
     * @param today the reference date-time defining "today"
     * @param other the date-time to test
     * @return {@code true} if both represent the same local date
     */
    public static boolean isToday(LocalDateTime today, LocalDateTime other)
    {
        return today.toLocalDate().equals(other.toLocalDate());
    }

    /**
     * Returns {@code true} if {@code other} is the same date as {@code today}.
     *
     * @param today the reference date defining "today"
     * @param other the date to test
     * @return {@code true} if both dates are equal
     */
    public static boolean isToday(LocalDate today, LocalDate other)
    {
        return today.equals(other);
    }

    /**
     * Returns {@code true} if {@code other} falls on the calendar day
     * immediately before {@code today}, after converting {@code other} to
     * {@code today}'s zone.
     *
     * @param today the reference date-time defining "today" and the zone to use
     * @param other the date-time to test
     * @return {@code true} if {@code other} represents yesterday in
     * {@code today}'s zone
     */
    public static boolean isYesterday(ZonedDateTime today, ZonedDateTime other)
    {
        other = other.withZoneSameInstant(today.getZone());
        return today.minusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     * Returns {@code true} if {@code other} falls on the calendar day
     * immediately before {@code today}.
     *
     * @param today the reference date-time defining "today"
     * @param other the date-time to test
     * @return {@code true} if {@code other} represents yesterday
     */
    public static boolean isYesterday(LocalDateTime today, LocalDateTime other)
    {
        return today.minusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     * Returns {@code true} if {@code other} is the date immediately before
     * {@code today}.
     *
     * @param today the reference date defining "today"
     * @param other the date to test
     * @return {@code true} if {@code other} is yesterday
     */
    public static boolean isYesterday(LocalDate today, LocalDate other)
    {
        return today.minusDays(1).equals(other);
    }

    /**
     * Returns {@code true} if {@code other} falls on the calendar day
     * immediately after {@code today}, after converting {@code other} to
     * {@code today}'s zone.
     *
     * @param today the reference date-time defining "today" and the zone to use
     * @param other the date-time to test
     * @return {@code true} if {@code other} represents tomorrow in
     * {@code today}'s zone
     */
    public static boolean isTomorrow(ZonedDateTime today, ZonedDateTime other)
    {
        other = other.withZoneSameInstant(today.getZone());
        return today.plusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     * Returns {@code true} if {@code other} falls on the calendar day
     * immediately after {@code today}.
     *
     * @param today the reference date-time defining "today"
     * @param other the date-time to test
     * @return {@code true} if {@code other} represents tomorrow
     */
    public static boolean isTomorrow(LocalDateTime today, LocalDateTime other)
    {
        return today.plusDays(1).toLocalDate().equals(other.toLocalDate());
    }

    /**
     * Returns {@code true} if {@code other} is the date immediately after
     * {@code today}.
     *
     * @param today the reference date defining "today"
     * @param other the date to test
     * @return {@code true} if {@code other} is tomorrow
     */
    public static boolean isTomorrow(LocalDate today, LocalDate other)
    {
        return today.plusDays(1).equals(other);
    }

    /**
     * Checks if two {@link LocalDate} objects represent dates in the same year.
     *
     * @param a the first date to compare
     * @param b the second date to compare
     * @return {@code true} if both dates are in the same year and neither is
     * {@code null}, {@code false} otherwise
     */
    public static boolean isSameYear(LocalDate a, LocalDate b)
    {
        if (a != null && b != null)
        {
            return a.getYear() == b.getYear();
        }
        return false;
    }

    /**
     * Checks if two {@link LocalDateTime} objects represent date-times in the
     * same year.
     *
     * @param a the first date-time to compare
     * @param b the second date-time to compare
     * @return {@code true} if both date-times are in the same year and neither
     * is {@code null}, {@code false} otherwise
     */
    public static boolean isSameYear(LocalDateTime a, LocalDateTime b)
    {
        if (a != null && b != null)
        {
            return a.getYear() == b.getYear();
        }
        return false;
    }

    /**
     * Checks if two {@link ZonedDateTime} objects, when converted to the
     * specified time zone, represent date-times in the same year.
     *
     * @param a    the first zoned date-time to compare
     * @param b    the second zoned date-time to compare
     * @param zone the zone to convert both values to before comparing
     * @return {@code true} if both date-times are in the same year in
     * {@code zone} and neither is {@code null}, {@code false} otherwise
     */
    public static boolean isSameYear(ZonedDateTime a, ZonedDateTime b, ZoneId zone)
    {
        if (a != null && b != null)
        {
            return a.withZoneSameInstant(zone).getYear() == b.withZoneSameInstant(zone).getYear();
        }
        return false;
    }
    
    // -----------------------------------------------------------------------
    // Boundary helpers
    // -----------------------------------------------------------------------

    /**
     * Returns a {@link LocalDateTime} at midnight (00:00:00) of the same date
     * as {@code dateTime}, preserving the original zone.
     *
     * @param dateTime the source date-time; must not be {@code null}
     * @return a {@link ZonedDateTime} at the start of the same day
     */
    public static ZonedDateTime atStartOfDay(ZonedDateTime dateTime)
    {
        LocalDateTime localDateTime = dateTime.toLocalDateTime();
        LocalDateTime startOfDay = localDateTime.toLocalDate().atStartOfDay();
        return startOfDay.atZone(dateTime.getZone());
    }

    /**
     * Returns the first day of the ISO week (Monday) containing {@code date}.
     *
     * @param date the reference date; must not be {@code null}
     * @return the Monday that starts the week containing {@code date}
     */
    public static LocalDate atStartOfWeek(LocalDate date)
    {
        return date.minusDays(date.getDayOfWeek().getValue() - 1);
    }

    /**
     * Returns the last day of the ISO week (Sunday) containing {@code date}.
     *
     * @param date the reference date; must not be {@code null}
     * @return the Sunday that ends the week containing {@code date}
     */
    public static LocalDate atEndOfWeek(LocalDate date)
    {
        return date.plusDays(DayOfWeek.SUNDAY.getValue() - date.getDayOfWeek().getValue());
    }

    /**
     * Returns the first day of the month containing {@code date}.
     *
     * @param date the reference date; must not be {@code null}
     * @return the first day of {@code date}'s month
     */
    public static LocalDate atStartOfMonth(LocalDate date)
    {
        return date.withDayOfMonth(1);
    }

    /**
     * Returns the last day of the month containing {@code date}.
     *
     * @param date the reference date; must not be {@code null}
     * @return the last day of {@code date}'s month
     */
    public static LocalDate atEndOfMonth(LocalDate date)
    {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    /**
     * Returns the first day of the year containing {@code date} (January 1st).
     *
     * @param date the reference date; must not be {@code null}
     * @return January 1st of {@code date}'s year
     */
    public static LocalDate atStartOfYear(LocalDate date)
    {
        return LocalDate.of(date.getYear(), 1, 1);
    }

    /**
     * Returns midnight on January 1st of the year represented by {@code zdt},
     * preserving its zone.
     *
     * @param zdt the reference zoned date-time; must not be {@code null}
     * @return January 1st 00:00:00 of {@code zdt}'s year in the same zone
     */
    public static ZonedDateTime atStartOfYear(ZonedDateTime zdt)
    {
        return ZonedDateTime.of(zdt.getYear(), 1, 1, 0, 0, 0, 0, zdt.getZone());
    }

    /**
     * Returns midnight on January 1st of {@code year} in the given zone.
     *
     * @param year the calendar year
     * @param zone the time zone; must not be {@code null}
     * @return January 1st 00:00:00.000000000 of {@code year} in {@code zone}
     */
    public static ZonedDateTime atStartOfYear(int year, ZoneId zone)
    {
        return ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, zone);
    }

    /**
     * Returns the last day of the year containing {@code date} (December 31st).
     *
     * @param date the reference date; must not be {@code null}
     * @return December 31st of {@code date}'s year
     */
    public static LocalDate atEndOfYear(LocalDate date)
    {
        return LocalDate.of(date.getYear(), 12, 31);
    }

    /**
     * Returns the last instant of December 31st of the year represented by
     * {@code zdt} (23:59:59.999999999), preserving its zone.
     *
     * @param zdt the reference zoned date-time; must not be {@code null}
     * @return December 31st 23:59:59.999999999 of {@code zdt}'s year in the
     * same zone
     */
    public static ZonedDateTime atEndOfYear(ZonedDateTime zdt)
    {
        return ZonedDateTime.of(zdt.getYear(), 12, 31, 23, 59, 59, 999_999_999, zdt.getZone());
    }

    /**
     * Returns the last instant of December 31st of {@code year} in the given
     * zone (23:59:59.999999999).
     *
     * @param year the calendar year
     * @param zone the time zone; must not be {@code null}
     * @return December 31st 23:59:59.999999999 of {@code year} in {@code zone}
     */
    public static ZonedDateTime atEndOfYear(int year, ZoneId zone)
    {
        return ZonedDateTime.of(year, 12, 31, 23, 59, 59, 999_999_999, zone);
    }

    // -----------------------------------------------------------------------
    // Scheduling
    // -----------------------------------------------------------------------

    /**
     * Enumerates the supported recurrence periodicities for {@link #schedule}.
     */
    public enum Periodicity
    {
        /** Once per day. */
        Daily,
        /** Once per week. */
        Weekly,
        /** Once per month. */
        Monthly,
        /** Once per year. */
        Yearly
    }

    /**
     * Computes the date that is {@code skip} periods after {@code date}.
     *
     * <p>For {@code Monthly} and {@code Yearly} periodicities, if the resulting
     * month is shorter than the source day-of-month (e.g. Jan 31 + 1 month =
     * Feb 28/29), and {@code after} is {@code true}, the result is pushed one
     * day forward so it falls in the following month rather than being clamped
     * to the last day of the shorter month.
     *
     * @param type  the recurrence periodicity; must not be {@code null}
     * @param date  the base date; must not be {@code null}
     * @param skip  the number of periods to advance
     * @param after if {@code true}, push overflow dates forward by one day
     * @return the computed date, or {@code null} for an unrecognised
     * {@code type}
     */
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

    /**
     * Generates a sequence of dates from {@code startAt} to {@code endAt}
     * (inclusive) spaced by {@code period} occurrences of {@code type}.
     *
     * <p>Example — every 2 weeks from 2024-01-01 to 2024-03-01:
     * <pre>{@code
     * LocalDate[] dates = JavaTime.schedule(Periodicity.Weekly, 2,
     *     LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1), false);
     * }</pre>
     *
     * @param type    the recurrence periodicity; must not be {@code null}
     * @param period  the number of {@code type} units between consecutive dates
     * @param startAt the first date in the sequence; must not be {@code null}
     * @param endAt   the upper bound (inclusive); must not be {@code null}
     * @param after   if {@code true}, month-overflow dates are pushed forward
     *                (see {@link #next})
     * @return an array of scheduled dates in chronological order; never
     * {@code null}
     */
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

    /**
     * Generates a fixed-count sequence of dates starting at {@code startAt},
     * spaced by {@code period} occurrences of {@code type}.
     *
     * @param type    the recurrence periodicity; must not be {@code null}
     * @param period  the number of {@code type} units between consecutive dates
     * @param startAt the first date in the sequence; must not be {@code null}
     * @param count   the total number of dates to generate
     * @param after   if {@code true}, month-overflow dates are pushed forward
     *                (see {@link #next})
     * @return an array of exactly {@code count} scheduled dates in
     * chronological order; never {@code null}
     */
    public static LocalDate[] schedule(Periodicity type, int period, LocalDate startAt, int count, boolean after)
    {
        return schedule(type, period, startAt, next(type, startAt, period * (count - 1), after), after);
    }

    // -----------------------------------------------------------------------
    // UTC formatting
    // -----------------------------------------------------------------------

    /**
     * Converts {@code zdt} to UTC.
     *
     * @param zdt the source zoned date-time; must not be {@code null}
     * @return the same instant expressed in the {@link #UTC} zone
     */
    public static ZonedDateTime utc(ZonedDateTime zdt)
    {
        return zdt.withZoneSameInstant(UTC);
    }

    /**
     * Formats {@code zdt} in UTC as {@code yyyy-MM-dd HH:mm}.
     *
     * @param zdt the source zoned date-time; must not be {@code null}
     * @return the formatted string in UTC to minute precision
     */
    public static String utcMinutes(ZonedDateTime zdt)
    {
        return utc(zdt).format(YYYY_MM_DD_HH_MM);
    }

    /**
     * Formats {@code zdt} in UTC as {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @param zdt the source zoned date-time; must not be {@code null}
     * @return the formatted string in UTC to second precision
     */
    public static String utcSeconds(ZonedDateTime zdt)
    {
        return utc(zdt).format(YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * Formats {@code zdt} in UTC as {@code yyyy-MM-dd HH:mm:ss.SSS}.
     *
     * @param zdt the source zoned date-time; must not be {@code null}
     * @return the formatted string in UTC to millisecond precision
     */
    public static String utcMilliSeconds(ZonedDateTime zdt)
    {
        return utc(zdt).format(YYYY_MM_DD_HH_MM_SS_SSS);
    }

    // -----------------------------------------------------------------------
    // Duration formatting
    // -----------------------------------------------------------------------

    /** Unit labels corresponding to days, hours, minutes, seconds, milliseconds, and nanoseconds. */
    private static final String[] DURATION_UNITS = "d,h,m,s,ms,ns".split(",");

    /**
     * Controls the finest time unit included when formatting a {@link Duration}.
     *
     * <p>Values are ordered from coarsest ({@link #D}) to finest ({@link #NS}).
     */
    public static enum Resolution
    {
        /** Days. */    D,
        /** Hours. */   H,
        /** Minutes. */ M,
        /** Seconds. */ S,
        /** Milliseconds. */ MS,
        /** Nanoseconds (sub-millisecond part). */ NS
    }

    /**
     * Formats {@code duration} as a compact human-readable string using exactly
     * {@code elements} non-zero components down to {@code resolution}.
     *
     * <p>Delegates to {@link #toString(Duration, int, int, Resolution)} with
     * {@code minElements == maxElements == elements}.
     *
     * @param duration   the duration to format; must not be {@code null}
     * @param elements   the exact number of components to include
     * @param resolution the finest unit to consider
     * @return the formatted duration string (e.g. {@code "2h30m"})
     * @throws InvalidParameterException never (kept for symmetry with the
     * multi-argument overload)
     */
    public static String toString(Duration duration, int elements, Resolution resolution)
    {
        return toString(duration, elements, elements, resolution);
    }
    
    /**
     * Formats {@code duration} as a compact human-readable string with a
     * variable number of components.
     *
     * <p>Components are taken from the set {days, hours, minutes, seconds,
     * milliseconds, nanoseconds}, filtered to those finer than or equal to
     * {@code resolution}. Leading zero-value components are suppressed until
     * the first non-zero component is found, but at least {@code minElements}
     * are always emitted. Trailing zero components beyond {@code minElements}
     * are omitted.
     *
     * <p>Examples:
     * <pre>{@code
     * toString(Duration.ofSeconds(90), 1, 2, Resolution.S)  // "1m30s"
     * toString(Duration.ofSeconds(60), 1, 2, Resolution.S)  // "1m"
     * toString(Duration.ofSeconds(60), 2, 2, Resolution.S)  // "1m0s"
     * }</pre>
     *
     * @param duration    the duration to format; must not be {@code null}
     * @param minElements the minimum number of components to emit (may include
     *                    trailing zeros)
     * @param maxElements the maximum number of components to emit
     * @param resolution  the finest unit to include
     * @return the formatted duration string
     * @throws InvalidParameterException if {@code minElements > maxElements}
     */
    public static String toString(Duration duration, int minElements, int maxElements, Resolution resolution)
    {
        if(minElements>maxElements)
        {
            throw new InvalidParameterException("minElements > maxElements");
        }
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
        StringBuilder s = new StringBuilder();
        StringBuilder tail = new StringBuilder();
        for(int i=0;i<values.length && count<maxElements && i<=resolution.ordinal();i++)
        {
            if(values[i]!=0 || !started && i+minElements>resolution.ordinal() || started && count < minElements)
            {
                s.append(tail).append(values[i]).append(DURATION_UNITS[i]);
                tail = new StringBuilder();
                started = true;
                count++;
            }
            else if(started)
            {
                tail.append(values[i]).append(DURATION_UNITS[i]);
                count++;
            }
        }
        return s.toString();
    }

    // -----------------------------------------------------------------------
    // Epoch-second conversions
    // -----------------------------------------------------------------------

    /**
     * Returns the current time as a Unix epoch second.
     *
     * @return seconds elapsed since 1970-01-01T00:00:00Z
     */
    public static long epochSecond()
    {
        return Instant.now().getEpochSecond();
    }

    /**
     * Returns the Unix epoch second for the given {@link ZonedDateTime}.
     *
     * @param zdt the zoned date-time to convert; must not be {@code null}
     * @return seconds elapsed since 1970-01-01T00:00:00Z
     */
    public static long epochSecond(ZonedDateTime zdt)
    {
        return zdt.toEpochSecond();
    }

    /**
     * Returns the Unix epoch second for the given {@link LocalDateTime},
     * interpreted as UTC.
     *
     * @param datetime the local date-time to convert, assumed to be in UTC;
     *                 must not be {@code null}
     * @return seconds elapsed since 1970-01-01T00:00:00Z
     */
    public static long epochSecond(LocalDateTime datetime)
    {
        return datetime.toEpochSecond(ZoneOffset.UTC);
    }

    /**
     * Returns the Unix epoch second for midnight at the start of {@code date},
     * interpreted as UTC.
     *
     * @param date the local date to convert, assumed to be in UTC;
     *             must not be {@code null}
     * @return seconds elapsed since 1970-01-01T00:00:00Z
     */
    public static long epochSecond(LocalDate date)
    {
        return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
    }
    
    /**
     * Returns the Unix epoch second for the given {@link Date},
     * interpreted as UTC.
     *
     * @param date the date to convert, assumed to be in UTC; must not be {@code null}
     * @return seconds elapsed since 1970-01-01T00:00:00Z
     */
    public static long epochSecond(Date date)
    {
        return date.toInstant().getEpochSecond();
    }
    
    /**
     * Converts a Unix epoch second to a {@link LocalDate} in UTC.
     *
     * @param epochSeconds seconds elapsed since 1970-01-01T00:00:00Z
     * @return the corresponding {@link LocalDate} in the {@link #UTC} zone
     */
    public static LocalDate asLocalDate(long epochSeconds)
    {
        return Instant.ofEpochSecond(epochSeconds).atZone(UTC).toLocalDate();
    }

    /**
     * Converts a Unix epoch second to a {@link LocalDateTime} in UTC.
     *
     * @param epochSeconds seconds elapsed since 1970-01-01T00:00:00Z
     * @return the corresponding {@link LocalDateTime} in the {@link #UTC} zone
     */
    public static LocalDateTime asLocalDateTime(long epochSeconds)
    {
        return Instant.ofEpochSecond(epochSeconds).atZone(UTC).toLocalDateTime();
    }   

    /**
     * Converts an {@link Instant} to a {@link LocalDate} in UTC.
     *
     * @param instant the instant to convert; must not be {@code null}
     * @return the corresponding {@link LocalDate} in the {@link #UTC} zone
     */
    public static LocalDate asLocalDate(Instant instant)
    {
        return instant.atZone(UTC).toLocalDate();
    }

    /**
     * Converts an {@link Instant} to a {@link LocalDateTime} in UTC.
     *
     * @param instant the instant to convert; must not be {@code null}
     * @return the corresponding {@link LocalDateTime} in the {@link #UTC} zone
     */
    public static LocalDateTime asLocalDateTime(Instant instant)
    {
        return instant.atZone(UTC).toLocalDateTime();
    }   
}
