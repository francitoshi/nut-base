/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.os;

import io.nut.base.util.Java;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ShellTest
{

    /**
     * Test of expandShellVariables method, of class Shell.
     */
    @Test
    public void testExpandShellVariables()
    {
        String home = Java.USER_HOME;
        String[][] TESTS =
        {
            {"$HOME/Dropbox", home+"/Dropbox"},
            {"${HOME}/Dropbox", home+"/Dropbox"},
            {"${HOME}rest", home+"rest"},
            {"$HOMErest", ""}, // bash: looks up variable "HOMErest", not "HOME"+"rest"
            {"$VARIABLETHATDOESNOTEXIST/x", "/x"},
            {"price: \\$100", "price: $100"}, // escaped '$' -> literal
            {"no variables here"},
            {"trailing dollar $"}
        };
        for (String[] s : TESTS)
        {
            String r = Shell.expandShellVariables(s[0]);
            assertEquals(s[s.length-1], r);
        }
    }
    
}
