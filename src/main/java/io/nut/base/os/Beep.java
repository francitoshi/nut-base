/*
 *  Beep.java
 *
 *  Copyright (c) 2022-2024 francitoshi@gmail.com
 *-
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
package io.nut.base.os;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Beep
{
    private static final String BEEP = "beep";
    private static final Shell shellUtils = Shell.getInstance(OSName.os);

    public static void beep()
    {
        try
        {
            String[] cmds = {BEEP};
            shellUtils.doShellCommand(cmds, null, false, true);
        }
        catch (Exception ex)
        {
            Logger.getLogger(Beep.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Beep.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Beep.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
