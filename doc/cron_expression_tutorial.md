# CronExpression Minitutorial

`CronExpression` is a parser and evaluator for standard Cron expressions in the `io.nut.base.time` package. It supports 5-field (Unix), 6-field (Spring/Quartz), and 7-field (Quartz) cron formats.

---

## 1. Cron Expression Structure

A cron expression is a string consisting of 5 to 7 fields separated by whitespace:

```text
 ┌────────────── second (0 - 59) (optional, defaults to 0 if 5 fields)
 │ ┌──────────── minute (0 - 59)
 │ │ ┌────────── hour (0 - 23)
 │ │ │ ┌──────── day of month (1 - 31)
 │ │ │ │ ┌────── month (1 - 12 or JAN-DEC)
 │ │ │ │ │ ┌──── day of week (0 - 7 or SUN-SAT, 0 and 7 are Sunday)
 │ │ │ │ │ │ ┌── year (1970 - 2099) (optional, defaults to * if not provided)
 │ │ │ │ │ │ │
 * * * * * * *
```

### Supported Formats

| Format | Number of Fields | Example | Default Second | Default Year |
| :--- | :---: | :--- | :---: | :---: |
| **Unix** | 5 | `* * * * *` | `0` | `*` |
| **Spring/Quartz (6-field)** | 6 | `0 * * * * *` | *Explicit* | `*` |
| **Quartz (7-field)** | 7 | `0 0 12 1 1 ? 2026` | *Explicit* | *Explicit* |

---

## 2. Special Operators & Wildcards

| Operator | Description | Example |
| :---: | :--- | :--- |
| `*` | Matches all possible values for that field. | `* * * * *` (runs every minute) |
| `?` | Used in Day of Month or Day of Week fields to indicate no specific value (same as `*`). | `0 0 12 ? * MON` (runs every Monday at 12:00) |
| `-` | Defines ranges. | `0 9-17 * * *` (runs hourly between 9 AM and 5 PM) |
| `,` | Defines list of values. | `0 0 8,12,18 * * *` (runs at 8:00, 12:00, and 18:00) |
| `/` | Specifies increments. | `*/15 * * * *` (runs every 15 minutes) |

> [!NOTE]
> **Textual Names**:
> - Months can be specified using English abbreviations: `JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC`.
> - Days of the week can be specified using English abbreviations: `SUN, MON, TUE, WED, THU, FRI, SAT`. Note that both `0` and `7` represent Sunday.

---

## 3. Important Evaluation Rules

### Combined Day-of-Month & Day-of-Week (OR Logic)
If both the **Day of Month** and the **Day of Week** fields are restricted (i.e. they are not `*` or `?`), then `CronExpression` combines them using an **`OR`** logic. The cron will fire when *either* condition is met.

For example, `0 0 12 15 * MON` will execute at 12:00:00:
- On the 15th of any month.
- **OR** on any Monday.

---

## 4. API Usage & Java Examples

### Basic Parsing & Verification
To parse a cron expression, use the static method `CronExpression.parse(String cron)`:

```java
import io.nut.base.time.CronExpression;

try 
{
    CronExpression cron = CronExpression.parse("*/15 9-17 * * MON-FRI");
    System.out.println("Cron parsed successfully: " + cron);
} 
catch (IllegalArgumentException e) 
{
    System.err.println("Invalid cron expression: " + e.getMessage());
}
```

### Calculating Next Execution (LocalDateTime)
Use the `next(LocalDateTime start)` method to find the first execution time strictly *after* the given time.

```java
import io.nut.base.time.CronExpression;
import java.time.LocalDateTime;

CronExpression cron = CronExpression.parse("0 0 12 * * MON-FRI"); // Weekdays at 12:00
LocalDateTime start = LocalDateTime.of(2026, 8, 7, 12, 0, 0); // Friday 12:00:00

LocalDateTime nextExecution = cron.next(start);
// Will print: 2026-08-10T12:00 (Next Monday 12:00:00)
System.out.println("Next execution: " + nextExecution);
```

### Calculating Next Execution (ZonedDateTime)
If you need to account for specific timezones, pass a `ZonedDateTime`:

```java
import io.nut.base.time.CronExpression;
import java.time.ZoneId;
import java.time.ZonedDateTime;

CronExpression cron = CronExpression.parse("0 12 * * *"); // Daily at 12:00:00
ZonedDateTime start = ZonedDateTime.of(2026, 8, 3, 10, 15, 0, 0, ZoneId.of("Europe/Madrid"));

ZonedDateTime nextExecution = cron.next(start);
// Will print: 2026-08-03T12:00+02:00[Europe/Madrid]
System.out.println("Next execution in Madrid: " + nextExecution);
```

---

## 5. Limits & Edge Cases

- **Year Boundaries**: The evaluator only calculates executions up to the year **2099**. If the next execution time exceeds the year 2099, the `next()` method will return `null`.
- **Thread Safety**: `CronExpression` instances are immutable and fully thread-safe.
