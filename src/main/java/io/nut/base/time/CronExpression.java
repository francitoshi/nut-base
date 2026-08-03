/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * A parser and evaluator for standard Cron expressions.
 * Supports 5-field (Unix), 6-field (Spring/Quartz), and 7-field (Quartz) cron formats.
 *
 * <p>The fields are:
 * <pre>
 *  ┌────────────── second (0 - 59) (optional)
 *  │ ┌──────────── minute (0 - 59)
 *  │ │ ┌────────── hour (0 - 23)
 *  │ │ │ ┌──────── day of month (1 - 31)
 *  │ │ │ │ ┌────── month (1 - 12 or JAN-DEC)
 *  │ │ │ │ │ ┌──── day of week (0 - 7 or SUN-SAT, 0 and 7 are Sunday)
 *  │ │ │ │ │ │ ┌── year (1970 - 2099) (optional)
 *  │ │ │ │ │ │ │
 *  * * * * * * *
 * </pre>
 */
public final class CronExpression
{
    private static final Map<String, Integer> MONTH_NAMES = new HashMap<>();
    static
    {
        MONTH_NAMES.put("JAN", 1);
        MONTH_NAMES.put("FEB", 2);
        MONTH_NAMES.put("MAR", 3);
        MONTH_NAMES.put("APR", 4);
        MONTH_NAMES.put("MAY", 5);
        MONTH_NAMES.put("JUN", 6);
        MONTH_NAMES.put("JUL", 7);
        MONTH_NAMES.put("AUG", 8);
        MONTH_NAMES.put("SEP", 9);
        MONTH_NAMES.put("OCT", 10);
        MONTH_NAMES.put("NOV", 11);
        MONTH_NAMES.put("DEC", 12);
    }

    private static final Map<String, Integer> DOW_NAMES = new HashMap<>();
    static
    {
        DOW_NAMES.put("SUN", 0);
        DOW_NAMES.put("MON", 1);
        DOW_NAMES.put("TUE", 2);
        DOW_NAMES.put("WED", 3);
        DOW_NAMES.put("THU", 4);
        DOW_NAMES.put("FRI", 5);
        DOW_NAMES.put("SAT", 6);
    }

    private final String expression;
    private final BitSet secondBits;
    private final BitSet minuteBits;
    private final BitSet hourBits;
    private final BitSet dayOfMonthBits;
    private final BitSet monthBits;
    private final BitSet dayOfWeekBits;
    private final BitSet yearBits;
    private final boolean orDayOf;

    private final String command;
    private final boolean reboot;
    private final java.time.Duration everyDuration;
    private final java.time.ZoneId zoneId;

    private CronExpression(String expression, BitSet secondBits, BitSet minuteBits, BitSet hourBits, BitSet dayOfMonthBits, BitSet monthBits, BitSet dayOfWeekBits, BitSet yearBits, boolean orDayOf, String command, boolean reboot, java.time.Duration everyDuration, java.time.ZoneId zoneId)
    {
        this.expression = expression;
        this.secondBits = secondBits;
        this.minuteBits = minuteBits;
        this.hourBits = hourBits;
        this.dayOfMonthBits = dayOfMonthBits;
        this.monthBits = monthBits;
        this.dayOfWeekBits = dayOfWeekBits;
        this.yearBits = yearBits;
        this.orDayOf = orDayOf;
        this.command = command;
        this.reboot = reboot;
        this.everyDuration = everyDuration;
        this.zoneId = zoneId;
    }

    /**
     * Parses a cron expression string.
     *
     * @param cron the cron expression to parse
     * @return the compiled CronExpression
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static CronExpression parse(String cron)
    {
        if (cron == null)
        {
            throw new IllegalArgumentException("Cron expression cannot be null");
        }
        String trimmed = cron.trim();
        if (trimmed.isEmpty())
        {
            throw new IllegalArgumentException("Cron expression cannot be empty");
        }
        String[] parts = trimmed.split("\\s+");

        String first = parts[0].toLowerCase();
        boolean isAlias = first.startsWith("@");
        boolean reboot = false;
        java.time.Duration everyDuration = null;
        java.time.ZoneId zoneId = null;
        String mappedCron = null;
        String exprName = null;

        if (isAlias)
        {
            if (first.equals("@yearly") || first.equals("@annually"))
            {
                mappedCron = "0 0 0 1 1 *";
                exprName = first;
            }
            else if (first.equals("@monthly"))
            {
                mappedCron = "0 0 0 1 * *";
                exprName = first;
            }
            else if (first.equals("@weekly"))
            {
                mappedCron = "0 0 0 * * 0";
                exprName = first;
            }
            else if (first.equals("@daily") || first.equals("@midnight"))
            {
                mappedCron = "0 0 0 * * *";
                exprName = first;
            }
            else if (first.equals("@hourly"))
            {
                mappedCron = "0 0 * * * *";
                exprName = first;
            }
            else if (first.equals("@reboot"))
            {
                reboot = true;
                exprName = first;
            }
            else if (first.equals("@every"))
            {
                if (parts.length < 2)
                {
                    throw new IllegalArgumentException("Missing duration for @every expression: " + cron);
                }
                everyDuration = parseDuration(parts[1]);
                exprName = "@every " + parts[1];
            }
            else if (first.equals("@at"))
            {
                if (parts.length < 2)
                {
                    throw new IllegalArgumentException("Missing time for @at expression: " + cron);
                }
                String timePart = parts[1];
                String[] timeParts = timePart.split(":");
                if (timeParts.length < 2 || timeParts.length > 3)
                {
                    throw new IllegalArgumentException("Invalid time format for @at: " + timePart);
                }
                try
                {
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);
                    int second = timeParts.length == 3 ? Integer.parseInt(timeParts[2]) : 0;
                    if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59)
                    {
                        throw new IllegalArgumentException("Time out of range for @at: " + timePart);
                    }
                    mappedCron = String.format("%d %d %d * * *", second, minute, hour);
                }
                catch (NumberFormatException e)
                {
                    throw new IllegalArgumentException("Invalid time format for @at: " + timePart);
                }
                exprName = "@at " + parts[1];

                if (parts.length >= 3 && java.time.ZoneId.getAvailableZoneIds().contains(parts[2]))
                {
                    zoneId = java.time.ZoneId.of(parts[2]);
                }
            }
            else
            {
                throw new IllegalArgumentException("Unknown cron alias: " + first);
            }
        }

        BitSet secondBits;
        BitSet minuteBits;
        BitSet hourBits;
        BitSet domBits;
        BitSet monthBits;
        BitSet dowBits;
        BitSet yearBits;
        boolean orDayOf;

        int C = 0;
        if (isAlias)
        {
            if (reboot || everyDuration != null)
            {
                secondBits = new BitSet();
                minuteBits = new BitSet();
                hourBits = new BitSet();
                domBits = new BitSet();
                monthBits = new BitSet();
                dowBits = new BitSet();
                yearBits = new BitSet();
                orDayOf = false;
            }
            else
            {
                String[] mappedParts = mappedCron.split("\\s+");
                secondBits = parseField(mappedParts[0], 0, 59, null);
                minuteBits = parseField(mappedParts[1], 0, 59, null);
                hourBits = parseField(mappedParts[2], 0, 23, null);
                domBits = parseField(mappedParts[3], 1, 31, null);
                monthBits = parseField(mappedParts[4], 1, 12, MONTH_NAMES);
                dowBits = parseField(mappedParts[5], 0, 7, DOW_NAMES);
                yearBits = parseField("*", 1970, 2099, null);

                if (dowBits.get(0))
                {
                    dowBits.set(7);
                }
                if (dowBits.get(7))
                {
                    dowBits.set(0);
                }

                boolean domRestricted = !mappedParts[3].equals("*") && !mappedParts[3].equals("?");
                boolean dowRestricted = !mappedParts[5].equals("*") && !mappedParts[5].equals("?");
                orDayOf = domRestricted && dowRestricted;
            }
        }
        else
        {
            while (C < Math.min(parts.length, 7) && isCronFieldToken(parts[C]))
            {
                C++;
            }
            if (C < 5)
            {
                throw new IllegalArgumentException("Invalid cron expression, must have between 5 and 7 fields: " + cron);
            }
            if (C == 7 && parts.length > 7 && isCronFieldToken(parts[7]))
            {
                throw new IllegalArgumentException("Invalid cron expression, too many fields: " + cron);
            }

            String secondPart;
            String minutePart;
            String hourPart;
            String domPart;
            String monthPart;
            String dowPart;
            String yearPart;

            if (C == 5)
            {
                secondPart = "0";
                minutePart = parts[0];
                hourPart = parts[1];
                domPart = parts[2];
                monthPart = parts[3];
                dowPart = parts[4];
                yearPart = "*";
            }
            else if (C == 6)
            {
                secondPart = parts[0];
                minutePart = parts[1];
                hourPart = parts[2];
                domPart = parts[3];
                monthPart = parts[4];
                dowPart = parts[5];
                yearPart = "*";
            }
            else
            {
                secondPart = parts[0];
                minutePart = parts[1];
                hourPart = parts[2];
                domPart = parts[3];
                monthPart = parts[4];
                dowPart = parts[5];
                yearPart = parts[6];
            }

            secondBits = parseField(secondPart, 0, 59, null);
            minuteBits = parseField(minutePart, 0, 59, null);
            hourBits = parseField(hourPart, 0, 23, null);
            domBits = parseField(domPart, 1, 31, null);
            monthBits = parseField(monthPart, 1, 12, MONTH_NAMES);
            dowBits = parseField(dowPart, 0, 7, DOW_NAMES);
            yearBits = parseField(yearPart, 1970, 2099, null);

            if (dowBits.get(0))
            {
                dowBits.set(7);
            }
            if (dowBits.get(7))
            {
                dowBits.set(0);
            }

            boolean domRestricted = !domPart.equals("*") && !domPart.equals("?");
            boolean dowRestricted = !dowPart.equals("*") && !dowPart.equals("?");
            orDayOf = domRestricted && dowRestricted;

            exprName = String.join(" ", java.util.Arrays.copyOfRange(parts, 0, C));
        }

        int commandStartIndex = isAlias ? (first.equals("@every") ? 2 : (first.equals("@at") ? (zoneId != null ? 3 : 2) : 1)) : C;
        String command = null;
        if (commandStartIndex < parts.length)
        {
            int currentToken = 0;
            int charIndex = 0;
            while (currentToken < commandStartIndex && charIndex < trimmed.length())
            {
                while (charIndex < trimmed.length() && Character.isWhitespace(trimmed.charAt(charIndex)))
                {
                    charIndex++;
                }
                while (charIndex < trimmed.length() && !Character.isWhitespace(trimmed.charAt(charIndex)))
                {
                    charIndex++;
                }
                currentToken++;
            }
            while (charIndex < trimmed.length() && Character.isWhitespace(trimmed.charAt(charIndex)))
            {
                charIndex++;
            }
            command = charIndex < trimmed.length() ? trimmed.substring(charIndex) : null;
        }

        return new CronExpression(exprName, secondBits, minuteBits, hourBits, domBits, monthBits, dowBits, yearBits, orDayOf, command, reboot, everyDuration, zoneId);
    }

    private static java.time.Duration parseDuration(String s)
    {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)(ns|us|µs|ms|s|m|h)");
        java.util.regex.Matcher matcher = pattern.matcher(s);
        java.time.Duration duration = java.time.Duration.ZERO;
        boolean matched = false;
        int lastEnd = 0;
        while (matcher.find())
        {
            if (matcher.start() != lastEnd)
            {
                throw new IllegalArgumentException("Invalid duration format: " + s);
            }
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit)
            {
                case "ns":
                    duration = duration.plusNanos(value);
                    break;
                case "us":
                case "µs":
                    duration = duration.plusNanos(value * 1000);
                    break;
                case "ms":
                    duration = duration.plusMillis(value);
                    break;
                case "s":
                    duration = duration.plusSeconds(value);
                    break;
                case "m":
                    duration = duration.plusMinutes(value);
                    break;
                case "h":
                    duration = duration.plusHours(value);
                    break;
            }
            matched = true;
            lastEnd = matcher.end();
        }
        if (!matched || lastEnd != s.length())
        {
            throw new IllegalArgumentException("Invalid duration format: " + s);
        }
        return duration;
    }

    private static boolean isCronFieldToken(String token)
    {
        if (!token.matches("^[0-9a-zA-Z*?\\-/,]+$"))
        {
            return false;
        }
        if (token.matches(".*[a-zA-Z].*"))
        {
            String upper = token.toUpperCase();
            String[] words = upper.split("[^A-Z]+");
            for (String w : words)
            {
                if (!w.isEmpty())
                {
                    if (!MONTH_NAMES.containsKey(w) && !DOW_NAMES.containsKey(w))
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static BitSet parseField(String field, int min, int max, Map<String, Integer> namesMap)
    {
        BitSet bits = new BitSet();
        String[] parts = field.split(",");
        for (String part : parts)
        {
            if (part.equals("*") || part.equals("?"))
            {
                bits.set(min, max + 1);
            }
            else if (part.contains("/"))
            {
                int slashIndex = part.indexOf('/');
                String rangePart = part.substring(0, slashIndex);
                String stepPart = part.substring(slashIndex + 1);
                int step = Integer.parseInt(stepPart);
                if (step <= 0)
                {
                    throw new IllegalArgumentException("Increment step must be positive: " + part);
                }
                int start, end;
                if (rangePart.equals("*") || rangePart.equals("?"))
                {
                    start = min;
                    end = max;
                }
                else if (rangePart.contains("-"))
                {
                    int dashIndex = rangePart.indexOf('-');
                    start = parseVal(rangePart.substring(0, dashIndex), namesMap, min, max);
                    end = parseVal(rangePart.substring(dashIndex + 1), namesMap, min, max);
                }
                else
                {
                    start = parseVal(rangePart, namesMap, min, max);
                    end = max;
                }
                if (start < min || end > max || start > end)
                {
                    throw new IllegalArgumentException("Invalid range for increment: " + part);
                }
                for (int i = start; i <= end; i += step)
                {
                    bits.set(i);
                }
            }
            else if (part.contains("-"))
            {
                int dashIndex = part.indexOf('-');
                int start = parseVal(part.substring(0, dashIndex), namesMap, min, max);
                int end = parseVal(part.substring(dashIndex + 1), namesMap, min, max);
                if (start < min || end > max || start > end)
                {
                    throw new IllegalArgumentException("Invalid range: " + part);
                }
                bits.set(start, end + 1);
            }
            else
            {
                int val = parseVal(part, namesMap, min, max);
                if (val < min || val > max)
                {
                    throw new IllegalArgumentException("Value out of range [" + min + "-" + max + "]: " + part);
                }
                bits.set(val);
            }
        }
        return bits;
    }

    private static int parseVal(String token, Map<String, Integer> namesMap, int min, int max)
    {
        token = token.trim().toUpperCase();
        if (namesMap != null && namesMap.containsKey(token))
        {
            return namesMap.get(token);
        }
        try
        {
            return Integer.parseInt(token);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid token: " + token);
        }
    }

    private boolean isDayAllowed(LocalDate date)
    {
        int dom = date.getDayOfMonth();
        int dow = date.getDayOfWeek().getValue(); // 1 (Mon) to 7 (Sun)
        
        boolean domMatches = dayOfMonthBits.get(dom);
        boolean dowMatches = dayOfWeekBits.get(dow);
        
        if (orDayOf)
        {
            return domMatches || dowMatches;
        }
        else
        {
            return domMatches && dowMatches;
        }
    }

    /**
     * Calculates the next execution time after the given {@link LocalDateTime}.
     *
     * @param start the reference date-time
     * @return the next execution date-time, or {@code null} if it exceeds the year 2099
     */
    public LocalDateTime next(LocalDateTime start)
    {
        if (start == null)
        {
            return null;
        }
        if (reboot)
        {
            return null;
        }
        if (everyDuration != null)
        {
            return start.plus(everyDuration);
        }
        LocalDateTime next = start.plusSeconds(1).withNano(0);
        
        while (true)
        {
            int year = next.getYear();
            if (year > 2099)
            {
                return null;
            }
            
            if (!yearBits.get(year))
            {
                int nextYear = yearBits.nextSetBit(year);
                if (nextYear == -1 || nextYear > 2099)
                {
                    return null;
                }
                next = LocalDateTime.of(nextYear, 1, 1, 0, 0, 0, 0);
                continue;
            }
            
            int month = next.getMonthValue();
            if (!monthBits.get(month))
            {
                int nextMonth = monthBits.nextSetBit(month);
                if (nextMonth == -1)
                {
                    next = LocalDateTime.of(year + 1, 1, 1, 0, 0, 0, 0);
                }
                else
                {
                    next = LocalDateTime.of(year, nextMonth, 1, 0, 0, 0, 0);
                }
                continue;
            }
            
            LocalDate date = next.toLocalDate();
            if (!isDayAllowed(date))
            {
                int lastDayOfMonth = date.lengthOfMonth();
                int currentDay = date.getDayOfMonth();
                int nextDay = -1;
                for (int d = currentDay + 1; d <= lastDayOfMonth; d++)
                {
                    if (isDayAllowed(date.withDayOfMonth(d)))
                    {
                        nextDay = d;
                        break;
                    }
                }
                if (nextDay == -1)
                {
                    if (month == 12)
                    {
                        next = LocalDateTime.of(year + 1, 1, 1, 0, 0, 0, 0);
                    }
                    else
                    {
                        next = LocalDateTime.of(year, month + 1, 1, 0, 0, 0, 0);
                    }
                }
                else
                {
                    next = LocalDateTime.of(year, month, nextDay, 0, 0, 0, 0);
                }
                continue;
            }
            
            int hour = next.getHour();
            if (!hourBits.get(hour))
            {
                int nextHour = hourBits.nextSetBit(hour);
                if (nextHour == -1)
                {
                    next = next.toLocalDate().plusDays(1).atStartOfDay();
                }
                else
                {
                    next = next.withHour(nextHour).withMinute(0).withSecond(0);
                }
                continue;
            }
            
            int minute = next.getMinute();
            if (!minuteBits.get(minute))
            {
                int nextMinute = minuteBits.nextSetBit(minute);
                if (nextMinute == -1)
                {
                    next = next.plusHours(1).withMinute(0).withSecond(0);
                }
                else
                {
                    next = next.withMinute(nextMinute).withSecond(0);
                }
                continue;
            }
            
            int second = next.getSecond();
            if (!secondBits.get(second))
            {
                int nextSecond = secondBits.nextSetBit(second);
                if (nextSecond == -1)
                {
                    next = next.plusMinutes(1).withSecond(0);
                }
                else
                {
                    next = next.withSecond(nextSecond);
                }
                continue;
            }
            
            return next;
        }
    }

    /**
     * Calculates the next execution time after the given {@link ZonedDateTime}.
     *
     * @param start the reference zoned date-time
     * @return the next zoned date-time, or {@code null} if it exceeds the year 2099
     */
    public ZonedDateTime next(ZonedDateTime start)
    {
        if (start == null)
        {
            return null;
        }
        if (reboot)
        {
            return null;
        }
        if (everyDuration != null)
        {
            return start.plus(everyDuration);
        }
        if (zoneId != null)
        {
            ZonedDateTime expressionZoneTime = start.withZoneSameInstant(zoneId);
            LocalDateTime localNext = next(expressionZoneTime.toLocalDateTime());
            if (localNext == null)
            {
                return null;
            }
            return ZonedDateTime.of(localNext, zoneId).withZoneSameInstant(start.getZone());
        }
        LocalDateTime localNext = next(start.toLocalDateTime());
        if (localNext == null)
        {
            return null;
        }
        return ZonedDateTime.of(localNext, start.getZone());
    }

    public String getCommand()
    {
        return command;
    }

    public boolean isReboot()
    {
        return reboot;
    }

    public java.time.Duration getEveryDuration()
    {
        return everyDuration;
    }

    public java.time.ZoneId getZoneId()
    {
        return zoneId;
    }

    public String getExpression()
    {
        return expression;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(expression);
        if (zoneId != null)
        {
            sb.append(" ").append(zoneId.getId());
        }
        if (command != null)
        {
            sb.append(" ").append(command);
        }
        return sb.toString();
    }
}
