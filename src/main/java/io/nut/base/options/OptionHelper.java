/*
 *  OptionHelper.java
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
 *
 */
package io.nut.base.options;

import io.nut.base.text.ConsoleAppendable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author franci
 */
public class OptionHelper
{
    final OptionParser parser;
    final Class helpedClass;

    public OptionHelper(OptionParser parser, Class HelpedClass)
    {
        this.parser = parser;
        this.helpedClass = HelpedClass;
    }
    
    private final int help_columns = 80;
    private final String USAGE = "usage";
    private final String COMMAND = "command";
    private final String ARGS = "args";
    private final String COMMANDS = "commands";
    private final String OPTIONS = "options";
    
    public void help(PrintStream out, boolean usage, boolean brief,boolean full, Locale locale) throws IOException
    {
        ResourceBundle ohBundle = ResourceBundle.getBundle(OptionHelper.class.getName(), locale);
        ResourceBundle hcBundle = ResourceBundle.getBundle(helpedClass.getName(), locale);
        
        ConsoleAppendable.Options conOp = new ConsoleAppendable.Options(help_columns,true);
        StringBuilder text = new StringBuilder();
        ConsoleAppendable ca = new ConsoleAppendable(text,conOp);
        ca.append(ohBundle.getString(USAGE)).append(": ").append(parser.name);
        conOp.setLeftMargin(text.length()+1);
        
        if(brief)
        {
            boolean cmd=false;
            for(Option op : parser.optionList)
            {
                if(!op.isCommand())
                {
                    StringBuilder item = new StringBuilder();
                    item.append(" [");
                    if(op.getShortName()!=0)
                    {
                        item.append("-").append(op.getShortName()).append("|--").append(op.getLongName());
                    }
                    else 
                    {
                        item.append("--").append(op.getLongName());
                    }
                    item.append("]");
                    ca.append(item.toString());
                }
                else
                {
                    cmd = true;
                }
            }
            if(cmd)
            {
                ca.append(" ").append(ohBundle.getString(COMMAND)).append(" [").append(ohBundle.getString(ARGS)).append("]");
            }
        }
        if(full)
        {
            int maxCmd = getLongNameMaxSize(true);
            conOp.setLeftMargin(0);
            ca.append("\n\n").append(ohBundle.getString(COMMANDS)).append(":\n");
            for(Option op : parser.optionList)
            {
                if(op.isCommand())
                {
                    conOp.setLeftMargin(2);
                    ca.append(op.getLongName());
                    conOp.setLeftMargin(maxCmd+3);
                    ca.append(hcBundle.getString(op.getLongName()));
                    ca.append("\n");
                }
            }

            int maxOp = getLongNameMaxSize(false);
            conOp.setLeftMargin(0);
            ca.append("\n").append(ohBundle.getString(OPTIONS)).append(":\n");
            for(Option op : parser.optionList)
            {
                if(!op.isCommand())
                {
                    conOp.setLeftMargin(1);
                    if(op.getShortName()!=0)
                    {
                        ca.append("-").append(op.getShortName()).append(' ');
                    }
                    conOp.setLeftMargin(4);
                    ca.append("--").append(op.getLongName()).append(' ');
                    conOp.setLeftMargin(maxOp+7);
                    ca.append(hcBundle.getString(op.getLongName()));
                    ca.append("\n");
                }
            }
        }
        out.append(ca.toString());
    }
    public void help(PrintStream out, boolean usage, boolean brief,boolean full) throws IOException
    {
        help(out,usage,brief,full,Locale.getDefault());
    }
    private int getLongNameMaxSize()
    {
        int max = 0;
        for(Option op : parser.optionList)
        {
            max = Math.max(max,op.getLongName().length());
        }
        return max;
    }
    private int getLongNameMaxSize(boolean command)
    {
        int max = 0;
        for(Option op : parser.optionList)
        {
            if(command == op.isCommand())
            {
                max = Math.max(max,op.getLongName().length());
            }
        }
        return max;
    }
}
