/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.time;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A parser for crontab files containing multiple lines.
 * It ignores comments, empty lines, and environment variable settings.
 */
public final class CronTab
{
    private final List<CronExpression> expressions;

    private CronTab(List<CronExpression> expressions)
    {
        this.expressions = new ArrayList<>(expressions);
    }

    /**
     * Gets the parsed cron expressions.
     *
     * @return the list of CronExpression objects
     */
    public List<CronExpression> getExpressions()
    {
        return expressions;
    }

    /**
     * Parses a crontab content string.
     *
     * @param content the crontab content
     * @return a parsed CronTab instance
     * @throws IllegalArgumentException if any active cron expression is invalid
     */
    public static CronTab parse(String content)
    {
        List<CronExpression> list = new ArrayList<>();
        if (content != null)
        {
            String[] lines = content.split("\\r?\\n");
            for (String line : lines)
            {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#"))
                {
                    continue;
                }
                // Skip environment variable definitions (e.g. MAILTO=root)
                if (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=.*"))
                {
                    continue;
                }
                list.add(CronExpression.parse(trimmed));
            }
        }
        return new CronTab(list);
    }

    /**
     * Parses a crontab from an InputStream.
     *
     * @param in the input stream containing crontab lines
     * @return a parsed CronTab instance
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if any active cron expression is invalid
     */
    public static CronTab parse(InputStream in) throws IOException
    {
        List<CronExpression> list = new ArrayList<>();
        if (in != null)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                String line;
                while ((line = reader.readLine()) != null)
                {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#"))
                    {
                        continue;
                    }
                    // Skip environment variable definitions (e.g. MAILTO=root)
                    if (trimmed.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\s*=.*"))
                    {
                        continue;
                    }
                    list.add(CronExpression.parse(trimmed));
                }
            }
        }
        return new CronTab(list);
    }
}
