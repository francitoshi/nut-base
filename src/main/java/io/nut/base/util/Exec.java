/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import io.nut.base.os.OS;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Exec
{
    private static final Logger LOG = Logger.getLogger(Exec.class.getName());
    public static Process exec(boolean stdin, boolean stdout, boolean stderr, String... commandArray) throws IOException
    {
        final ProcessBuilder.Redirect PIPE = ProcessBuilder.Redirect.PIPE;
        final ProcessBuilder.Redirect INHERIT = ProcessBuilder.Redirect.INHERIT;
        ProcessBuilder p = new ProcessBuilder().redirectInput(stdin ? INHERIT : PIPE).redirectOutput(stdout ? INHERIT : PIPE).redirectError(stderr ? INHERIT : PIPE);
        return p.command(commandArray).start();
    }

    public static Process exec(String... commandArray) throws IOException
    {
        return Runtime.getRuntime().exec(commandArray);
    }

    public static Process safeExec(String... commandArray)
    {
        try
        {
            return Runtime.getRuntime().exec(commandArray);
        }
        catch (IOException ex)
        {
            Exceptions.severe(LOG, ex);
            return null;
        }
    }
    
    /**
     * Check whether {@code name} is available as an executable on the system PATH.
     *
     * @param name binary name without extension (e.g. {@code "tor"})
     * @return {@code true} if found and executable
     */
    public static boolean isBinaryOnPath(String name) 
    {
        if(name==null || name.isEmpty())
        {
            return false;
        }
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null)
        {
            return false;
        }
        String suffix = OS.getInstance().isWindows() ? ".exe" : "";
        for (String dir : pathEnv.split(File.pathSeparator))
        {
            if(new File(dir, name + suffix).canExecute())
            {
                return true;
            }
        }
        return false;
    }    
    
}
