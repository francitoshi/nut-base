/*
 *  ConsoleAppendable.java
 *
 *  Copyright (C) 2012-2024 francitoshi@gmail.com
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
 */
package io.nut.base.text;

import java.io.IOException;



/**
 * Writes on an StringBuilder, if colums is specified add an \n character to
 * keep under that number of colums
 * 
 * @author franci
 */
public class ConsoleAppendable implements Appendable
{
    private final Object lock = new Object();

    public static class Options
    {
        final int colums;
        volatile int leftMargin=0;
        volatile int rightMargin=0;
        final int tab=8;

        public Options(int colums, boolean trim)
        {
            this.colums = colums;
        }
        public Options setLeftMargin(int val)
        {
            this.leftMargin=val;
            return this;
        }
        public Options setRightMargin(int val)
        {
            this.rightMargin=val;
            return this;
        }
    }
    static final Options DEFAULT = new Options(80, true);
    final Appendable appendable;
    final Options options;
    int count=0;

    public ConsoleAppendable(Appendable dest, Options options)
    {
        this.appendable = dest;
        this.options = options;
    }
    public ConsoleAppendable(Appendable dest)
    {
        this.appendable = dest;
        this.options = DEFAULT;
    }

    @Override
    public Appendable append(char c) throws IOException
    {
        return append(Character.toString(c),0,1);
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException
    {
        synchronized(lock)
        {
mainloop:   while(start<end)
            {
                // line feed
                if( csq.charAt(start)=='\n' )
                {
                    appendln();
                    start++;
                    continue;
                }
                // space char
                if( csq.charAt(start)==' ' )
                {
                    if(count<options.colums)
                    {
                        appendable.append(' ');
                        count++;
                        start++;
                        continue;
                    }
                    appendln();
                    start++;
                    continue;
                }

                // space char
                if( csq.charAt(start)=='\t' )
                {
                    do
                    {
                        append(" ",0,1);
                    }
                    while( count%options.tab != 0 );
                    start++;
                    continue;
                }

                // more than one line
                for(int i=start;i<end;i++)
                {
                    char c = csq.charAt(i);
                    if(c==' ' || c=='\t' || c=='\n')
                    {
                        append(csq,start,i);
                        start=i;
                        continue mainloop;
                    }
                }

                // one non empty line

                // right margin is the limit to start appending
                if(options.colums>0 && count>=options.colums-options.rightMargin)
                {
                    appendln();
                }
                // new line if word can't fit
                if(options.colums!=0 && count>options.leftMargin && end-start+count>options.colums)
                {
                    appendln();
                    continue;
                }

                // go to the left margin
                while(count<options.leftMargin && (options.colums==0 || end-start+count<options.colums )  )
                {
                    appendable.append(" ");
                    count++;
                }
                appendable.append(csq, start, end);
                count += (end-start);
                break;
            }
        }
        return this;
    }
    @Override
    public Appendable append(CharSequence csq) throws IOException
    {
        return append(csq,0,csq.length());
    }
    private void appendln() throws IOException
    {
        synchronized(lock)
        {
            appendable.append("\n");
            count=0;
        }
    }

    @Override
    public String toString()
    {
        return appendable.toString();
    }
    
}
