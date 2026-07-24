/*
 * Copyright (C) 2015-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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
                @Override
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

    public static Shell getInstance(OS osName)
    {
        if (osName.isAndroid())
        {
            return new AndroidShell();
        }
        return new PosixShell();
    }

    public final static Shell sh = getInstance(OS.getInstance());

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
    
    /**
     * Expands shell-style environment variable references within a string,
     * replicating bash's basic "parameter expansion" rules for this specific
     * case (not tilde expansion, not advanced parameter expansion operators).
     *
     * Supported formats: $VAR -> value of environment variable VAR. The name
     * may contain letters, digits and '_', but must NOT start with a digit
     * (same rule as bash). The longest possible valid identifier is consumed.
     * ${VAR} -> same as $VAR, but with explicit boundaries via braces; useful
     * when you need to concatenate text right after the variable (e.g.
     * "${VAR}rest" vs "$VARrest", which bash would treat as a single variable
     * named "VARrest"). \$ -> an escaped '$' is treated as a literal character
     * and is NOT expanded (same as bash in an unquoted context).
     *
     * Undefined variable: Just like bash without "set -u" (nounset), a variable
     * that doesn't exist is replaced with an empty string, NOT left as the
     * original literal text.
     *
     * Out of scope (not implemented, since these are advanced parameter
     * expansion features, not basic variable expansion): - Default values:
     * ${VAR:-value}, ${VAR:=value} - Substitution/length: ${VAR/pattern/repl},
     * ${#VAR} - Command substitution: $(command) or `command` - Arithmetic
     * expansion: $((expression)) - Arrays: ${ARR[0]} - Special parameters: $$,
     * $1, $@, etc.
     */
    public static String expandShellVariables(String s)
    {
        if (s == null || s.isEmpty())
        {
            return s;
        }

        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        int len = s.length();

        while (i < len)
        {
            char c = s.charAt(i);

            if (c == '\\' && i + 1 < len && s.charAt(i + 1) == '$')
            {
                // Escaped '\$' -> literal '$', do not expand
                out.append('$');
                i += 2;
                continue;
            }

            if (c != '$')
            {
                out.append(c);
                i++;
                continue;
            }

            // c == '$'
            if (i + 1 >= len)
            {
                // Trailing '$' with nothing after it -> literal
                out.append('$');
                i++;
                continue;
            }

            char next = s.charAt(i + 1);

            if (next == '{')
            {
                // ${VAR}
                int closeBrace = s.indexOf('}', i + 2);
                if (closeBrace == -1)
                {
                    // No closing '}' -> bash would raise a syntax error;
                    // here, to be lenient, we just leave it as-is.
                    out.append(c);
                    i++;
                    continue;
                }
                String varName = s.substring(i + 2, closeBrace);
                out.append(resolveVar(varName));
                i = closeBrace + 1;
            }
            else if (isValidVarStart(next))
            {
                // $VAR (no braces): consume the longest possible identifier
                int j = i + 1;
                while (j < len && isValidVarPart(s.charAt(j)))
                {
                    j++;
                }
                String varName = s.substring(i + 1, j);
                out.append(resolveVar(varName));
                i = j;
            }
            else
            {
                // '$' followed by something that doesn't form a valid name
                // (e.g. "$ ", "$5", "$$"). Bash has special cases for $$,
                // $1, $@, etc. (special parameters), which are not
                // arbitrary environment variables and are out of scope
                // for this method -> left as literal.
                out.append(c);
                i++;
            }
        }

        return out.toString();
    }

    private static boolean isValidVarStart(char c)
    {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isValidVarPart(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String resolveVar(String varName)
    {
        if (varName.isEmpty())
        {
            return "";
        }
        String value = System.getenv(varName);
        // Same as bash without "nounset": undefined variable -> empty string
        return (value != null) ? value : "";
    }
}
