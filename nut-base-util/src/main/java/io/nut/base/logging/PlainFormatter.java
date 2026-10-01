/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.logging;

import io.nut.base.logging.Log.FormatType;
import io.nut.base.time.CachedDateTimeFormatter;
import io.nut.base.time.JavaTime;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class PlainFormatter extends Formatter
{
    private static final DateTimeFormatter DTF_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final DateTimeFormatter dtf;
    private final boolean utc;
    private final ZoneId zoneId;
    private final boolean name;
    private final CachedDateTimeFormatter cache;

    public PlainFormatter(FormatType fmt)
    {
        switch (fmt)
        {
            case LEV_MSG:
                this.dtf = null;
                this.utc = false;
                this.zoneId = null;
                this.name=false;
                break;
            case DT_LEV_MSG:
                this.dtf = DTF_DT;
                this.utc = false;
                this.zoneId = ZoneId.systemDefault();
                this.name=false;
                break;
            case DTZ_LEV_MSG:
                this.dtf = DTF_DT;
                this.utc = true;
                this.zoneId = JavaTime.UTC;
                this.name=false;
                break;
            case DT_LEV_NAME_MSG:
                this.dtf = DTF_DT;
                this.utc = false;
                this.zoneId = ZoneId.systemDefault();
                this.name=true;
                break;
            case DTZ_LEV_NAME_MSG:
            default:
                this.dtf = DTF_DT;
                this.utc = true;
                this.zoneId = JavaTime.UTC;
                this.name=true;
                break;
        }
        this.cache = new CachedDateTimeFormatter(zoneId, dtf);
    }

    @Override
    public String format(LogRecord record)
    {
        StringBuilder sb = new StringBuilder();
        
        if (dtf != null)
        {
            sb.append(cache.format(record.getMillis()));
            if (utc)
            {
                sb.append('Z');
            }
            sb.append(' ');
        }
        String message = formatMessage(record);
        int levelValue = JulLog.fromJulLevel(record.getLevel());
        char level = Log.levelToChar(levelValue);
        sb.append("[").append(level).append("] ").append(message).append('\n');
        
        Throwable tr = record.getThrown();
        if (tr != null) 
        {
            StringWriter sw = new StringWriter();
            tr.printStackTrace(new PrintWriter(sw));
            sb.append(sw);
        }        
        
        return sb.toString();
    }
    
}
