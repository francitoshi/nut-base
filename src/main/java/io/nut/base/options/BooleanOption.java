/*
 *  BooleanOption.java
 *
 *  Copyright (C) 2009-2024 francitoshi@gmail.com
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
package io.nut.base.options;

import java.text.MessageFormat;

/**
 *
 * @author franci
 */
public class BooleanOption implements Option
{
    private final boolean command;
    private final char shortName;
    protected final String longName;
    protected final String _longName;
    protected final String __longName;
    protected String usedName;
    protected int count = 0;
    protected int lastUsed = 0;           //index of the last item using this option
    protected boolean oneHyphen = true;
    protected boolean twoHyphen = true;
    protected boolean nonHyphen = false;
    protected String shortHelp = "";
    protected String longHelp = "";

    BooleanOption(char shortName, String longName, boolean command,String shortHelp, String longHelp)
    {
        this.shortName = shortName;
        this.longName = longName;
        this._longName = "-" + longName;
        this.__longName = "--" + longName;
        this.command = command;
        if(command)
        {
            oneHyphen = false;
            twoHyphen = false;
            nonHyphen = true;
        }
        this.shortHelp = shortHelp;
        this.longHelp  = longHelp;
    }
    public BooleanOption(String longName)
    {
        this((char) 0, longName, false,"","");
    }
    public BooleanOption(char shortName, String longName)
    {
        this(shortName,longName,false,"","");
    }
    public BooleanOption(String longName, String shortHelp, String longHelp)
    {
        this((char) 0, longName, false, shortHelp, longHelp);
    }
    public BooleanOption(char shortName, String longName, String shortHelp, String longHelp)
    {
        this(shortName,longName,false, shortHelp, longHelp);
    }

    @Override
    public String getLongName()
    {
        return longName;
    }

    @Override
    public char getShortName()
    {
        return shortName;
    }

    @Override
    public int getCount()
    {
        return count;
    }

    @Override
    public void addCount()
    {
        count++;
    }

    @Override
    public boolean isUsed()
    {
        return (count > 0);
    }

    @Override
    public int getLastUsed()
    {
        return lastUsed;
    }

    public int parseLong(int index, String[] args)
    {
        String option = args[index];
        if (twoHyphen && option.equals(__longName))
        {
            count++;
            lastUsed = index;
            usedName = option;
            return 1;
        }
        if (oneHyphen && option.equals(_longName))
        {
            count++;
            lastUsed = index;
            usedName = option;
            return 1;
        }
        if (nonHyphen && option.equals(longName))
        {
            count++;
            lastUsed = index;
            usedName = option;
            return 1;
        }
        if (nonHyphen && shortName!=0 && option.equals(Character.toString(shortName)))
        {
            count++;
            lastUsed = index;
            usedName = option;
            return 1;
        }

        return 0;
    }

    @Override
    public int parseShort(int argIndex, int charIndex, String[] args)
    {
        String option = args[argIndex];
        if (option.charAt(charIndex) == shortName)
        {
            count++;
            lastUsed = argIndex;
            usedName = Character.toString(shortName);
            return 1;
        }
        return 0;
    }

    @Override
    public String getUsedName()
    {
        return usedName;
    }

    @Override
    public String toString()
    {
        return MessageFormat.format("--{0}={1} (count={2})", longName, (count>0), count);
    }

    @Override
    public boolean isCommand()
    {
        return command;
    }
}
