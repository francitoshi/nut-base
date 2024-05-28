/*
 *  Shell.java
 *
 *  Copyright (c) 2015-2024 francitoshi@gmail.com
 *-
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
package io.nut.base.os;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

class AndroidShell extends Shell
{
    private static final Logger logger = Logger.getLogger(AndroidShell.class.getName());

    @Override
    public boolean isRootPossible()
    {
        try
        {
            // Check if Superuser.apk exists
            File fileSU = new File("/system/app/Superuser.apk");
            if (fileSU.exists())
            {
                return true;
            }

            fileSU = new File("/system/bin/su");
            if (fileSU.exists())
            {
                return true;
            }

            //Check for 'su' binary
            String[] cmd = { "which su" };
            int exitCode = doShellCommand(null, cmd, new ShellCallback()
            {
                public void shellOut(String msg)
                {
                    //System.out.print(msg);
                }
                public void processComplete(int exitValue)
                {
                    // TODO Auto-generated method stub
                }

            }, false, true).exitValue();

            if (exitCode == 0)
            {
                logger.log(Level.CONFIG, "Can acquire root permissions");
                return true;
            }
        }
        catch (IOException e)
        {
            //this means that there is no root to be had (normally) so we won't log anything
            logger.log(Level.SEVERE, "Error checking for root access", e);
        }
        catch (Exception e)
        {
            logger.log(Level.SEVERE, "Error checking for root access", e);
            //this means that there is no root to be had (normally)
        }

        logger.log(Level.SEVERE, "Could not acquire root permissions");

        return false;
    }
}

class PosixShell extends Shell
{

    @Override
    public boolean isRootPossible()
    {
        return false;
    }
}

public abstract class Shell
{
    private static final Logger logger = Logger.getLogger(Shell.class.getName());
    //various console cmds
    public final static String SHELL_CMD_CHMOD = "chmod";
    public final static String SHELL_CMD_KILL = "kill -9";
    public final static String SHELL_CMD_RM = "rm";
    public final static String SHELL_CMD_PS = "ps";
    public final static String SHELL_CMD_PIDOF = "pidof";

    public final static String CHMOD_EXE_VALUE = "700";
    
    public interface ShellCallback
    {
        void shellOut(String shellLine);
        void processComplete(int exitValue);
    }

    public static Shell getInstance(OSName osName)
    {
        if (osName.isAndroid())
        {
            return new AndroidShell();
        }
        return new PosixShell();
    }

    public final static Shell sh = getInstance(OSName.os);

    public abstract boolean isRootPossible();

    public int findProcessId(String command)
    {
        int procId = -1;

        try
        {
            procId = findProcessIdWithPidOf(command);

            if (procId == -1)
            {
                procId = findProcessIdWithPS(command);
            }
        }
        catch (Exception ex)
        {
            try
            {
                procId = findProcessIdWithPS(command);
            }
            catch (Exception e2)
            {
                logger.log(Level.SEVERE, "Unable to get proc id for: " + command, e2);
            }
        }

        return procId;
    }

    //use 'pidof' command
    public int findProcessIdWithPidOf(String command) throws Exception
    {
        int procId = -1;

        Runtime r = Runtime.getRuntime();

        Process procPs;

        String baseName = new File(command).getName();

        //fix contributed my mikos on 2010.12.10
        procPs = r.exec(new String[] { SHELL_CMD_PIDOF, baseName });
        //procPs = r.exec(SHELL_CMD_PIDOF);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        
        String line;
        while ((line = reader.readLine()) != null)
        {
            try
            {
                //this line should just be the process id
                procId = Integer.parseInt(line.trim());
                break;
            }
            catch (NumberFormatException e)
            {
                logger.log(Level.SEVERE, "unable to parse process pid: " + line, e);
            }
        }

        return procId;
    }

    //use 'ps' command
    public int findProcessIdWithPS(String command) throws Exception
    {
        int procId = -1;

        Runtime r = Runtime.getRuntime();

        Process procPs = r.exec(SHELL_CMD_PS);

        BufferedReader reader = new BufferedReader(new InputStreamReader(procPs.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null)
        {
            if (line.contains(' ' + command))
            {

                StringTokenizer st = new StringTokenizer(line, " ");
                st.nextToken(); //proc owner

                procId = Integer.parseInt(st.nextToken().trim());

                break;
            }
        }
        return procId;
    }

    public int doShellCommand(String[] cmds, ShellCallback sc, boolean runAsRoot, boolean waitFor) throws Exception
    {
        return doShellCommand(null, cmds, sc, runAsRoot, waitFor).exitValue();
    }

    public Process doShellCommand(Process proc, String[] cmds, ShellCallback sc, boolean runAsRoot, boolean waitFor) throws Exception
    {
        if (proc == null)
        {
            proc = Runtime.getRuntime().exec(runAsRoot ? "su" : "sh");
        }

        OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());

        for (String cmd : cmds)
        {
            logger.log(Level.CONFIG, "executing shell cmd: {0}; runAsRoot={1};waitFor={2}", new Object[]
            {
                cmd, runAsRoot, waitFor
            });
            out.write(cmd);
            out.write("\n");
        }

        out.flush();
        out.write("exit\n");
        out.flush();

        if (waitFor)
        {
            final char buf[] = new char[20];

            // Consume the "stdout"
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            int read = 0;
            while ((read = reader.read(buf)) != -1)
            {
                if (sc != null)
                {
                    sc.shellOut(new String(buf));
                }
            }

            // Consume the "stderr"
            reader = new InputStreamReader(proc.getErrorStream());
            read = 0;
            while ((read = reader.read(buf)) != -1)
            {
                if (sc != null)
                {
                    sc.shellOut(new String(buf));
                }
            }
            proc.waitFor();
        }

        if (sc != null)
        {
            sc.processComplete(proc.exitValue());
        }

        return proc;
    }
}
