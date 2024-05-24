/*
 *  OptionParser.java
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

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/*

    * An option is a hyphen followed by a single alphanumeric character, like this: -o.
    * An option may require an argument (which must appear immediately after the option); for example, -o argument or -oargument.
    * Options that do not require arguments can be grouped after a hyphen, so, for example, -lst is equivalent to -t -l -s.
    * Options can appear in any order; thus -lst is equivalent to -tls.
    * Options can appear multiple times.
    * Options precede other nonoption arguments: -lst nonoption.
    * The -- argument terminates options.
    * The - option is typically used to represent one of the standard input streams.

*/

/**
 *
 * @author franci
 */
public class OptionParser
{
    final String name;
    private static final String ONE_HYPHEN = "-";
    private static final String TWO_HYPHEN = "--";
    private boolean ignoreShort = false;
    private boolean ignoreUnknownShort = false;
    private boolean oneHyphen = true;
    private boolean twoHyphen = true;
    private boolean posixly = false;
    final List<Option> optionList = new ArrayList<>();
    private final Option oneHyphenOption = new BooleanOption(ONE_HYPHEN);
    private final Option twoHyphenOption = new BooleanOption(TWO_HYPHEN);

    public OptionParser(String name)
    {
        this.name = name;
    }
    public OptionParser()
    {
        this(null);
    }

    public <T extends Option> T add(T item)
    {
        optionList.add(item);
        return item;
    }

    public String[] parse(String[] args) throws InvalidOptionException
    {
        Option[] rules = sort(optionList.toArray(new Option[0]));
        String remainder[] = new String[args.length];
        int remainderSize = 0;
        boolean noMoreOptions = false;

        for (int i = 0; i < args.length; i++)
        {
            if (!noMoreOptions)
            {
                if (oneHyphen && args[i].equals(ONE_HYPHEN))
                {
                    // opcion - input by stdin
                    oneHyphenOption.addCount();
                    continue;
                }
                else if (twoHyphen && args[i].equals(TWO_HYPHEN))
                {
                    // option -- no more options
                    twoHyphenOption.addCount();
                    noMoreOptions = true;
                    continue;
                }
                else
                {
                    // long options
                    int size = parseLong(i, args, rules);
                    if (size == 0 && !ignoreShort)
                    {
                        size = parseShort(i, args, rules);
                    }
                    if (size > 0)
                    {
                        i += (size - 1);
                        continue;
                    }

                    if (posixly)
                    {
                        noMoreOptions = true;
                    }
                }
            }
            remainder[remainderSize++] = args[i];
        }
        return Arrays.copyOf(remainder, remainderSize);
    }

    public boolean isPosixly()
    {
        return posixly;
    }

    public void setPosixly(boolean posixly)
    {
        this.posixly = posixly;
    }

    private int parseLong(int index, String[] args, Option[] rules)
    {
        int size = 0;
        for (int i = 0; i < rules.length && size == 0; i++)
        {
            size = rules[i].parseLong(index, args);
        }
        return size;
    }

    private int parseShort(int index, String[] args, Option[] rules) throws InvalidOptionException
    {
        if (args[index].equals(ONE_HYPHEN) || !args[index].startsWith(ONE_HYPHEN))
        {
            return 0;
        }

        int num = args[index].length();
        for (int i = 1; i < num; i++)
        {
            int size = 0;
            for (int j = 0; j < rules.length && size == 0; j++)
            {
                size = rules[j].parseShort(index, i, args);
            }
            if (size == 1)
            {
                continue;
            }
            if (size > 1)
            {
                return (size - 1);
            }
            if(ignoreUnknownShort)
            {
                return 0;
            }
            throw new InvalidOptionException("Invalid option -"+args[index].charAt(i));
        }
        return 1;
    }

    public boolean isIgnoreShort()
    {
        return ignoreShort;
    }

    public void setIgnoreShort(boolean ignoreShort)
    {
        this.ignoreShort = ignoreShort;
    }

    public boolean isIgnoreUnknownShort()
    {
        return ignoreUnknownShort;
    }

    public void setIgnoreUnknownShort(boolean val)
    {
        this.ignoreUnknownShort = val;
    }

    public boolean isOneHyphen()
    {
        return oneHyphen;
    }

    public void setOneHyphen(boolean oneHyphen)
    {
        this.oneHyphen = oneHyphen;
    }
    

    public Option getOneHyphenOption()
    {
        return oneHyphenOption;
    }

    private Option[] sort(Option[] list)
    {
        //reverse order longer options first
        Comparator<Option> cmp;
        cmp = new Comparator<Option>()
        {
            @Override
            public int compare(Option opt1, Option opt2)
            {
                return opt2.getLongName().length() - opt1.getLongName().length();
            }
        };
        Arrays.sort(list,cmp);
        return list;
    }
    public void log()
    {
        Logger logger = Logger.getLogger(OptionParser.class.getName());
        if(logger.isLoggable(Level.CONFIG))
        {
            for(Option op : optionList)
            {
                if(op.isUsed())
                {
                    logger.config(op.toString());
                }
            }
        }
    }   
}
