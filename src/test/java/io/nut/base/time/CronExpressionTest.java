/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CronExpression")
class CronExpressionTest
{
    @Nested
    @DisplayName("Parsing and Validation")
    class ParsingAndValidation
    {
        @Test
        @DisplayName("parses standard 5-field unix cron format")
        void standardUnixFormat()
        {
            CronExpression cron = CronExpression.parse("*/15 0-4 * * *");
            assertNotNull(cron);
            assertEquals("*/15 0-4 * * *", cron.getExpression());
            assertEquals("*/15 0-4 * * *", cron.toString());
        }

        @Test
        @DisplayName("parses 6-field format with seconds")
        void sixFieldFormat()
        {
            CronExpression cron = CronExpression.parse("0 0 12 * * *");
            assertNotNull(cron);
            assertEquals("0 0 12 * * *", cron.getExpression());
        }

        @Test
        @DisplayName("parses 7-field format with seconds and year")
        void sevenFieldFormat()
        {
            CronExpression cron = CronExpression.parse("0 0 12 1 1 ? 2026");
            assertNotNull(cron);
            assertEquals("0 0 12 1 1 ? 2026", cron.getExpression());
        }

        @Test
        @DisplayName("parses textual month and weekday names")
        void textualNames()
        {
            CronExpression cron = CronExpression.parse("0 0 12 * JAN,JUN MON-FRI");
            assertNotNull(cron);
        }

        @Test
        @DisplayName("throws exception for invalid field count")
        void invalidFieldCount()
        {
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * * *"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * * * * * * *"));
        }

        @Test
        @DisplayName("throws exception for out of range values")
        void outOfRangeValues()
        {
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("60 * * * *"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* 24 * * *"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * 32 * *"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * * 13 *"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("* * * * 8"));
        }

        @Test
        @DisplayName("throws exception for invalid increment step")
        void invalidIncrementStep()
        {
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("*/0 * * * *"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("*/-5 * * * *"));
        }

        @Test
        @DisplayName("parses cron aliases correctly")
        void cronAliases()
        {
            assertEquals("@yearly", CronExpression.parse("@yearly").getExpression());
            assertEquals("@annually", CronExpression.parse("@annually").getExpression());
            assertEquals("@monthly", CronExpression.parse("@monthly").getExpression());
            assertEquals("@weekly", CronExpression.parse("@weekly").getExpression());
            assertEquals("@daily", CronExpression.parse("@daily").getExpression());
            assertEquals("@midnight", CronExpression.parse("@midnight").getExpression());
            assertEquals("@hourly", CronExpression.parse("@hourly").getExpression());
            
            CronExpression reboot = CronExpression.parse("@reboot");
            assertTrue(reboot.isReboot());
            assertEquals("@reboot", reboot.getExpression());
        }

        @Test
        @DisplayName("parses robfig duration alias @every")
        void robfigEveryAlias()
        {
            CronExpression every10s = CronExpression.parse("@every 10s");
            assertEquals(Duration.ofSeconds(10), every10s.getEveryDuration());
            assertEquals("@every 10s", every10s.getExpression());

            CronExpression everyComplex = CronExpression.parse("@every 2h15m30s10ms5µs3ns");
            Duration d = everyComplex.getEveryDuration();
            assertEquals(8130, d.getSeconds());
            assertEquals(10005003, d.getNano());

            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("@every"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("@every 10x"));
        }

        @Test
        @DisplayName("parses @at execution time and timezone")
        void atExecutionTime()
        {
            CronExpression at = CronExpression.parse("@at 15:30");
            assertEquals("@at 15:30", at.getExpression());
            assertNull(at.getZoneId());

            CronExpression atZone = CronExpression.parse("@at 08:00 Europe/Madrid");
            assertEquals("@at 08:00", atZone.getExpression());
            assertEquals(ZoneId.of("Europe/Madrid"), atZone.getZoneId());

            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("@at"));
            assertThrows(IllegalArgumentException.class, () -> CronExpression.parse("@at 25:00"));
        }

        @Test
        @DisplayName("parses command and arguments")
        void commandsAndArguments()
        {
            CronExpression cron1 = CronExpression.parse("0 * * * * /usr/local/bin/backup.sh");
            assertEquals("0 * * * *", cron1.getExpression());
            assertEquals("/usr/local/bin/backup.sh", cron1.getCommand());
            assertEquals("0 * * * * /usr/local/bin/backup.sh", cron1.toString());

            CronExpression cron2 = CronExpression.parse("@daily /usr/local/bin/backup.sh --full --compress");
            assertEquals("@daily", cron2.getExpression());
            assertEquals("/usr/local/bin/backup.sh --full --compress", cron2.getCommand());

            CronExpression cron3 = CronExpression.parse("@at 15:30  /usr/local/bin/backup.sh");
            assertEquals("@at 15:30", cron3.getExpression());
            assertEquals("/usr/local/bin/backup.sh", cron3.getCommand());

            CronExpression cron4 = CronExpression.parse("@at 08:00 Europe/Madrid  /usr/local/bin/backup.sh --args");
            assertEquals("@at 08:00", cron4.getExpression());
            assertEquals(ZoneId.of("Europe/Madrid"), cron4.getZoneId());
            assertEquals("/usr/local/bin/backup.sh --args", cron4.getCommand());
        }
    }

    @Nested
    @DisplayName("Evaluation / Next Execution Calculation")
    class NextExecutionCalculation
    {
        @Test
        @DisplayName("calculates next minute execution")
        void nextMinute()
        {
            CronExpression cron = CronExpression.parse("* * * * *");
            LocalDateTime start = LocalDateTime.of(2026, 8, 3, 10, 15, 30);
            LocalDateTime next = cron.next(start);
            assertEquals(LocalDateTime.of(2026, 8, 3, 10, 16, 0), next);
        }

        @Test
        @DisplayName("calculates next hour execution with ranges")
        void nextHourWithRange()
        {
            CronExpression cron = CronExpression.parse("0 12-14 * * *");
            LocalDateTime start = LocalDateTime.of(2026, 8, 3, 13, 30, 0);
            LocalDateTime next = cron.next(start);
            assertEquals(LocalDateTime.of(2026, 8, 3, 14, 0, 0), next);
        }

        @Test
        @DisplayName("calculates next execution matching weekday")
        void weekdayMatching()
        {
            CronExpression cron = CronExpression.parse("0 0 12 * * MON-FRI");
            LocalDateTime start = LocalDateTime.of(2026, 8, 7, 12, 0, 0);
            LocalDateTime next = cron.next(start);
            assertEquals(LocalDateTime.of(2026, 8, 10, 12, 0, 0), next);
        }

        @Test
        @DisplayName("combines day of month and day of week using OR when both restricted")
        void combineDomAndDowWithOr()
        {
            CronExpression cron = CronExpression.parse("0 0 12 15 * MON");
            LocalDateTime start = LocalDateTime.of(2026, 8, 4, 12, 0, 0);
            assertEquals(LocalDateTime.of(2026, 8, 10, 12, 0, 0), cron.next(start));

            LocalDateTime start2 = LocalDateTime.of(2026, 8, 11, 12, 0, 0);
            assertEquals(LocalDateTime.of(2026, 8, 15, 12, 0, 0), cron.next(start2));
        }

        @Test
        @DisplayName("supports ZonedDateTime calculations")
        void zonedCalculation()
        {
            CronExpression cron = CronExpression.parse("0 12 * * *");
            ZonedDateTime start = ZonedDateTime.of(2026, 8, 3, 10, 15, 0, 0, ZoneId.of("Europe/Madrid"));
            ZonedDateTime next = cron.next(start);
            assertEquals(ZonedDateTime.of(2026, 8, 3, 12, 0, 0, 0, ZoneId.of("Europe/Madrid")), next);
        }

        @Test
        @DisplayName("returns null if next execution exceeds year 2099 bounds")
        void outOfBoundsYear()
        {
            CronExpression cron = CronExpression.parse("0 0 12 1 1 ? 1980");
            LocalDateTime start = LocalDateTime.of(2026, 8, 3, 10, 15, 0);
            assertNull(cron.next(start));
        }

        @Test
        @DisplayName("calculates next time for @every duration")
        void everyDurationNext()
        {
            CronExpression cron = CronExpression.parse("@every 2h15m");
            LocalDateTime start = LocalDateTime.of(2026, 8, 3, 10, 0, 0);
            assertEquals(LocalDateTime.of(2026, 8, 3, 12, 15, 0), cron.next(start));

            ZonedDateTime zStart = ZonedDateTime.of(2026, 8, 3, 10, 0, 0, 0, ZoneId.of("UTC"));
            assertEquals(ZonedDateTime.of(2026, 8, 3, 12, 15, 0, 0, ZoneId.of("UTC")), cron.next(zStart));
        }

        @Test
        @DisplayName("returns null for @reboot next execution")
        void rebootNext()
        {
            CronExpression cron = CronExpression.parse("@reboot");
            assertNull(cron.next(LocalDateTime.now()));
            assertNull(cron.next(ZonedDateTime.now()));
        }

        @Test
        @DisplayName("calculates next time for @at with specific timezone conversion")
        void atTimezoneNext()
        {
            // Runs at 8:00 AM Europe/Madrid time.
            CronExpression cron = CronExpression.parse("@at 08:00 Europe/Madrid");
            
            // Europe/Madrid is UTC+2 in August.
            // 2026-08-03T10:15:00Z (UTC) is 2026-08-03T12:15:00+02:00 in Europe/Madrid.
            // The next execution after 12:15:00 is tomorrow at 8:00:00 (i.e. 2026-08-04T08:00:00+02:00).
            // Converting 2026-08-04T08:00:00+02:00 back to UTC gives 2026-08-04T06:00:00Z.
            ZonedDateTime start = ZonedDateTime.of(2026, 8, 3, 10, 15, 0, 0, ZoneId.of("UTC"));
            ZonedDateTime expected = ZonedDateTime.of(2026, 8, 4, 6, 0, 0, 0, ZoneId.of("UTC"));
            assertEquals(expected, cron.next(start));
        }
    }

    @Nested
    @DisplayName("CronTab multi-line parsing")
    class CronTabParsing
    {
        @Test
        @DisplayName("parses crontab lines and ignores comments/env vars")
        void parseCronTab() throws IOException
        {
            String crontabContent = "# This is a comment\n" +
                    "MAILTO=root\n" +
                    "PATH=/usr/bin:/bin\n" +
                    "\n" +
                    "0 * * * * /usr/local/bin/backup.sh\n" +
                    "@daily /usr/local/bin/backup.sh --full --compress\n" +
                    "@at 15:30 /usr/local/bin/backup.sh\n";

            CronTab cronTab = CronTab.parse(crontabContent);
            List<CronExpression> list = cronTab.getExpressions();
            assertEquals(3, list.size());

            assertEquals("0 * * * *", list.get(0).getExpression());
            assertEquals("/usr/local/bin/backup.sh", list.get(0).getCommand());

            assertEquals("@daily", list.get(1).getExpression());
            assertEquals("/usr/local/bin/backup.sh --full --compress", list.get(1).getCommand());

            assertEquals("@at 15:30", list.get(2).getExpression());
            assertEquals("/usr/local/bin/backup.sh", list.get(2).getCommand());

            // Test parsing from InputStream
            ByteArrayInputStream in = new ByteArrayInputStream(crontabContent.getBytes(StandardCharsets.UTF_8));
            CronTab cronTabFromStream = CronTab.parse(in);
            assertEquals(3, cronTabFromStream.getExpressions().size());
        }
    }
}
