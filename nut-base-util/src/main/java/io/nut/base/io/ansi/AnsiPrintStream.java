/*
 * AnsiPrintStream.java
 *
 * Copyright (c) 2018-2023 francitoshi@gmail.com
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
package io.nut.base.io.ansi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 *
 * @author franci
 */
public class AnsiPrintStream extends PrintStream
{

    public enum Color
    {
        Black, Red, Green, Yellow, Blue, Purple, Cyan, White, Default
    }
    public enum Direction
    {
        Up, Dwn, Fwd, Bck
    }
    
    public static final Color BLACK = Color.Black;
    public static final Color RED   = Color.Red;
    public static final Color GREEN = Color.Green;
    public static final Color YELLOW= Color.Yellow;
    public static final Color BLUE  = Color.Blue;
    public static final Color PURPLE= Color.Purple;
    public static final Color CYAN  = Color.Cyan;
    public static final Color WHITE = Color.White;
    public static final Color DEFAULT = Color.Default;
    
    public static final Direction Up = Direction.Up;
    public static final Direction Dwn = Direction.Dwn;
    public static final Direction Fwd = Direction.Fwd;
    public static final Direction Bck = Direction.Bck;
    
    public static String ansiColor(Color fg, Color bg)
    {
        StringBuilder head = new StringBuilder();
        if(fg!=null || bg!=null)
        {
            boolean semicolon = false;
            head.append("\u001B[");
            if(fg!=null)
            {
                int col = (fg!=DEFAULT) ? 30+fg.ordinal() : 0;
                head.append(col);
                semicolon = true;
            }
            if(bg!=null)
            {
                if(semicolon) head.append(';');
                int col = (bg!=DEFAULT) ? 40+bg.ordinal() : 0;
                head.append(col);
            }
            head.append("m");
        }
        return head.toString();
    }
    public static String ansiReset()
    {
        return "\u001B[0m";
    }
    
    private final Object lock = new Object();
    
    private volatile Color fg=null;
    private volatile Color bg=null;
    private volatile boolean autoReset=false;
    private volatile String head="";
    private volatile String tail="";
    
    private volatile boolean disabledAnsi;

    public AnsiPrintStream(OutputStream out)
    {
        super(out);
    }

    public AnsiPrintStream(OutputStream out, boolean autoFlush)
    {
        super(out, autoFlush);
    }

    public AnsiPrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException
    {
        super(out, autoFlush, encoding);
    }

    public void setDisabledAnsi(boolean value)
    {
        synchronized(lock)
        {
            this.disabledAnsi = value;
            if(value)
            {
                this.fg = null;
                this.bg = null;
                this.autoReset = false;
                this.buildHeadTail();
            }
        }
    }

    public void setFg(Color fg)
    {
        synchronized(lock)
        {
            if(disabledAnsi) return;
            this.fg = fg;
            this.buildHeadTail();
        }
    }

    public void setBg(Color bg)
    {
        synchronized(lock)
        {
            if(disabledAnsi) return;
            this.bg = bg;
            this.buildHeadTail();
        }
    }

    public void setAutoReset(boolean autoReset)
    {
        synchronized(lock)
        {
            if(disabledAnsi) return;
            this.autoReset = autoReset;
            this.buildHeadTail();
        }
    }
    
    public void reset()
    {
        synchronized(lock)
        {
            if(disabledAnsi) return;
            this.fg = null;
            this.bg = null;
            this.autoReset = false;
            this.buildHeadTail();
            super.print("\u001B[0m");
        }
    }
    public AnsiPrintStream color(Color fg)
    {
        synchronized(lock)
        {
            if(disabledAnsi) return this;
            this.fg = fg;
            this.buildHeadTail();
            return this;
        }
    }
    public AnsiPrintStream color(Color fg, Color bg)
    {
        synchronized(lock)
        {
            if(disabledAnsi) return this;
            this.fg = fg;
            this.bg = bg;
            this.buildHeadTail();
            return this;
        }
    }
    public AnsiPrintStream color(Color fg, Color bg, boolean autoReset)
    {
        synchronized(lock)
        {
            if(disabledAnsi) return this;
            this.fg = fg;
            this.bg = bg;
            this.autoReset = autoReset;
            this.buildHeadTail();
            return this;
        }
    }
    private void buildHeadTail()
    {
        synchronized(lock)
        {
            if(disabledAnsi) return;
            
            this.head = ansiColor(fg,bg);
            this.tail = autoReset ? ansiReset() : null;
        }
    }
    private String ansi(String s)
    {
        synchronized(lock)
        {
            if(s!=null && (head!=null || tail!=null))
            {
                return (head!=null?head:"") + s + (tail!=null?tail:"");
            }
            return s;
        }
    }
    
    public AnsiPrintStream cursor(Direction dir, int count)
    {
        synchronized(lock)
        {
            switch(dir)
            {
                case Up: 
                    super.print("\u001B["+count+"A");
                    break;
                case Dwn: 
                    super.print("\u001B["+count+"B");
                    break;
                case Fwd: 
                    super.print("\u001B["+count+"C");
                    break;
                case Bck: 
                    super.print("\u001B["+count+"D");
                    break;
            }
            return this;
        }
    }
    public AnsiPrintStream cursor(int row, int col)
    {
        synchronized(lock)
        {
            super.print("\u001B["+row+";"+col+"H");
            return this;
        }
    }
    public AnsiPrintStream cursor(boolean visible)
    {
        synchronized(lock)
        {
            super.print(visible?"\u001B[?25h":"\u001B[?25l");
            return this;
        }
    }

    @Override
    public PrintStream printf(Locale locale, String s, Object... os)
    {
        synchronized(lock)
        {
            return super.printf(locale, ansi(s), os);
        }
    }
    @Override
    public PrintStream printf(String s, Object... os)
    {
        synchronized(lock)
        {
            return super.printf(ansi(s), os);
        }
    }
    @Override
    public void println(Object value)
    {
        this.println(String.valueOf(value));
    }
    @Override
    public void println(String s)
    {
        synchronized(lock)
        {
            super.println(ansi(s));
        }
    }
    @Override
    public void println(char[] value)
    {
        this.println(new StringBuilder().append(value).toString());
    }

    @Override
    public void println(double value)
    {
        this.println(String.valueOf(value));
    }

    @Override
    public void println(float value)
    {
        this.println(String.valueOf(value));
    }

    @Override
    public void println(long value)
    {
        this.println(String.valueOf(value));
    }

    @Override
    public void println(int value)
    {
        this.println(String.valueOf(value));
    }

    @Override
    public void println(char value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void println(boolean value)
    {
        this.println(String.valueOf(value));
    }

    @Override
    public void println()
    {
        synchronized(lock)
        {
            super.println();
        }
    }

    @Override
    public void print(Object value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void print(String s)
    {
        synchronized(lock)
        {
            super.print(ansi(s));
        }
    }

    @Override
    public void print(char[] value)
    {
        this.print(new StringBuilder().append(value).toString());
    }

    @Override
    public void print(double value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void print(float value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void print(long value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void print(int value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void print(char value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void print(boolean value)
    {
        this.print(String.valueOf(value));
    }

    @Override
    public void write(byte[] bytes) throws IOException
    {
        this.write(bytes, 0, bytes.length);
    }

    @Override
    public void write(byte[] bytes, int i, int i1)
    {
        synchronized(lock)
        {
            if(!this.head.isEmpty())
            {
                byte[] hb = this.head.getBytes();
                super.write(hb, 0, hb.length);
            }
            super.write(bytes, i, i1);
            if(this.tail!=null && !this.tail.isEmpty())
            {
                byte[] tb = this.tail.getBytes();
                super.write(tb, 0, tb.length);
            }
        }
    }

    @Override
    public void write(int i)
    {
        synchronized(lock)
        {
            if(!this.head.isEmpty())
            {
                byte[] hb = this.head.getBytes();
                super.write(hb, 0, hb.length);
            }
            super.write(i);
            if(!this.tail.isEmpty())
            {
                byte[] tb = this.tail.getBytes();
                super.write(tb, 0, tb.length);
            }
        }
    }
    
}
