/*
 *  Exec.java
 *
 *  Copyright (c) 2023 francitoshi@gmail.com
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
package io.nut.base.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class Exec
{
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
            Logger.getLogger(Exec.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
}
