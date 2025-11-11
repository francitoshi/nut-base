/*
 *  CommandOption.java
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
package io.nut.base.options;

/**
 * Represents a command-line option that extends the functionality of {@link BooleanOption}.
 * This class is designed for options that are boolean in nature but may have specific behaviors
 * for command handling, such as always requiring an argument (set to true in constructors).
 */
public class CommandOption extends BooleanOption
{
    /**
     * Constructs a new {@code CommandOption} with the specified long name.
     * The short name is set to none (0), and the option is configured to have an argument,
     * with empty argument name and description.
     *
     * @param longName the long name of the option (e.g., "--help")
     */
    public CommandOption(String longName)
    {
        super((char) 0, longName, true, "", "");
    }

    /**
     * Constructs a new {@code CommandOption} with the specified short and long names.
     * The option is configured to have an argument, with empty argument name and description.
     *
     * @param shortName the short name character of the option (e.g., 'h' for "-h")
     * @param longName the long name of the option (e.g., "--help")
     */
    public CommandOption(char shortName, String longName)
    {
        super(shortName, longName, true, "", "");
    }
    
    /**
     * Checks if any of the provided {@code CommandOption} instances have been used.
     * This method iterates through the varargs array and returns {@code true} if at least
     * one option is marked as used.
     *
     * @param cmd varargs array of {@code CommandOption} instances to check
     * @return {@code true} if at least one option is used, {@code false} otherwise
     */
    public static boolean isUsed(CommandOption... cmd)
    {
        for(CommandOption item : cmd)
        {
            if (item.isUsed())
            {
                return true;
            }
        }
        return false;
    }
    
}
