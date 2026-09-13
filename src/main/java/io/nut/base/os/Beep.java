/*
 * Copyright (C) 2022-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.os;

import io.nut.base.util.Exceptions;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Beep
{
    private static final Logger LOG = Logger.getLogger(Beep.class.getName());
    private static final String BEEP = "beep";
    private static final Shell shellUtils = Shell.getInstance(OS.getInstance());

    public static void beep()
    {
        try
        {
            String[] cmds = {BEEP};
            shellUtils.doShellCommand(cmds, null, false, true);
        }
        catch (Exception ex)
        {
            Exceptions.severe(LOG, ex);
        }
    }
    public static void beep(double[] freq, int[] millis)
    {
        try
        {
            StringBuilder sb = new StringBuilder(BEEP);
            for(int i=0;i<freq.length;i++)
            {
                if(i>0)
                {
                    sb.append(" -n");
                }
                sb.append(" -f ").append(freq[i]).append(" -l ").append(millis[i]);
            }
            String[] cmds = {sb.toString()};
            shellUtils.doShellCommand(cmds, null, false, true);
        }
        catch (Exception ex)
        {
            Exceptions.severe(LOG, ex);
        }
    }
    public static void beep(int freq, int millis)
    {
        try
        {
            String[] cmds = {BEEP+" -f "+freq+" -l "+millis};
            shellUtils.doShellCommand(cmds, null, false, true);
        }
        catch (Exception ex)
        {
            Exceptions.severe(LOG, ex);
        }
    }
}
