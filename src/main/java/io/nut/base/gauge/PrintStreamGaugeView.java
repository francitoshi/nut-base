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

import java.io.PrintStream;

/**
 *
 * @author franci
 */
public class PrintStreamGaugeView implements GaugeView
{
    private int lastLen = 0;
    private boolean debug = true;
    private boolean prefixBreak = true;
    private String prefix = "";

    private final PrintStream out;
    
    public PrintStreamGaugeView()
    {
        super();
        this.out = System.out;
    }
    public PrintStreamGaugeView(PrintStream out)
    {
        super();
        this.out = out;
        this.debug=true;
    }

    public void paint(boolean started, int max, int val, String prefix, double done, String msg)
    {
        boolean newLine = (prefixBreak && !this.prefix.equals(prefix));
        this.prefix=prefix;
        
        StringBuilder buf = new StringBuilder("\r");
        buf.append(msg);
        for (int i = msg.length(); i < lastLen; i++)
        {
            buf.append(" ");
        }
        if (newLine)
        {
            buf.append("\n");
        }

        print(buf.toString());
        lastLen = msg.length();
    }

    public boolean isDebug()
    {
        return debug;
    }

    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    private void print(String text)
    {
        this.out.print(text);
        this.out.flush();
    }
    
    //hacer que consoleGaugeView herede de esta
}
