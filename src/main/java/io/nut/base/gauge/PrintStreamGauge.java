/*
 *  ConsoleGaugeView.java
 *
 *  Copyright (C) 2012-2025 francitoshi@gmail.com
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
 *
 */
package io.nut.base.gauge;

import io.nut.base.io.ansi.Ansi;
import java.io.PrintStream;

/**
 *
 * @author franci
 */
public class PrintStreamGauge extends AbstractGauge
{
    private boolean debug = false;

    private final PrintStream out;
    
    public PrintStreamGauge()
    {
        super();
        this.out = System.out;
    }
    public PrintStreamGauge(PrintStream out)
    {
        super();
        this.out = out;
        this.debug=true;
    }
    
    public void println(String s)
    {
        this.out.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).append('\r').append(s).append('\n'));
        this.out.flush();
        invalidate();
    }

    private static final String[] TIME_FMT = 
    {
        "",                 //000   0
        "%3$s",             //001   1 
        "%2$s",             //020   2   
        "%2$s = %3$s",      //021   3
        "%1$s",             //400   4
        "%1$s / %3$s",      //401   5
        "%1$s + %2$s",      //420   6
        "%1$s + %2$s = %3$s"//421   7
    };
    public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
    {
        StringBuilder txt = new StringBuilder("\r");
        
        if(prefix!=null && !(prefix=prefix.trim()).isEmpty())
        {
            txt.append(prefix).append(' ');
        }
        
        txt.append(String.format("%d/%d %.2f%%", val, max, done*100));
        
        int index = (prev!=null?4:0) + (prev!=null?2:0) + (prev!=null?1:0);
        if(index!=0)
        {
            txt.append(' ').append(String.format(TIME_FMT[index], prev, next, full));
        }
        
        if(debug) txt.append('\n');
        this.out.print(txt);
        this.out.flush();
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }
    
    //hacer que consoleGaugeView herede de esta
}
