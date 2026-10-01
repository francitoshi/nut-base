/*
 *  StringOption.java
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
 *
 */
package io.nut.base.options;

// CAMBIAR LA HERENCIA POR UNA COMPOSICIÓN PARA FACILITAR LA DETECCIÓN DE ERRORES EN OptionParser.add

import java.text.MessageFormat;

/**
 *
 * @author franci
 */
public class StringOption extends BooleanOption
{
    protected final String _longName_;
    protected final String __longName_;

    private String value = null;

    public StringOption(char shortName, String longName)
    {
        super(shortName, longName);
        _longName_ =  _longName  + "=";
        __longName_ = __longName + "=";
    }

    public StringOption(String longName)
    {
        super(longName);
        _longName_  = _longName  + "=";
        __longName_ = __longName + "=";
    }

    @Override
    public int parseLong(int index, String[] args)
    {
        int size = super.parseLong(index, args);
        if ((size > 0) && (index + size < args.length))
        {
            setValue(args[index + size]);
            size++;
        }
        else
        {
            String option = args[index];
            if (twoHyphen && option.startsWith(__longName_))
            {
                count++;
                lastUsed = index;
                usedName = __longName;
                setValue(rest(option, __longName_));
                return 1;
            }
            if (twoHyphen && option.startsWith(__longName))
            {
                count++;
                lastUsed = index;
                usedName = __longName;
                setValue(rest(option, __longName));
                return 1;
            }
            if (oneHyphen && option.startsWith(_longName_))
            {
                count++;
                lastUsed = index;
                usedName = _longName;
                setValue(rest(option, _longName_));
                return 1;
            }
            if (oneHyphen && option.startsWith(_longName))
            {
                count++;
                lastUsed = index;
                usedName = _longName;
                setValue(rest(option, _longName));
                return 1;
            }
            return 0;

        }

        return size;
    }

    @Override
    public int parseShort(int argIndex, int charIndex, String[] args)
    {
        int size = super.parseShort(argIndex, charIndex, args);
        if (size == 1)
        {
            if (args[argIndex].length() > charIndex + size)
            {
                setValue(args[argIndex].substring(charIndex + size));
                size = 2;
            }
            else if( (argIndex+1) == args.length)
            {
                setValue("");
                size = 2;
            }
            else if (argIndex < args.length)
            {
                setValue(args[argIndex + 1]);
                size = 3;
            }
        }
        return size;
    }

    public String getValue() throws MissingOptionParameterException
    {
        if(value==null)
        {
            throw new MissingOptionParameterException(this);
        }
        return value;
    }
    public String getValue(String def)
    {
        return isUsed()?value:def;
    }

    private String rest(String src, String prefix)
    {
        int size = src.length();
        int index = prefix.length();
        return (size >= index) ? src.substring(index) : null;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value)
    {
        this.value = value;
    }
    
    @Override
    public String toString()
    {
        return MessageFormat.format("--{0}={1}", longName, value);
    }
    
}
