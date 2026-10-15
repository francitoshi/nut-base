/*
 * Copyright (C) 2012-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.cli.options;

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
