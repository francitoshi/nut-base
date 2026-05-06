/*
 *  PASS.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto.gpg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a thin Java wrapper around the
 * <a href="https://www.passwordstore.org/">pass</a> command-line password
 * manager.
 *
 * <p>{@code pass} stores GPG-encrypted secrets in a directory tree.  Each
 * secret is identified by a path-like name (e.g. {@code "email/gmail"}).
 * This class exposes three basic operations: store, retrieve, and list.</p>
 *
 * <p>All methods spawn a child {@code pass} process and communicate through
 * its standard streams.  They return {@code null} or {@code false} on failure
 * instead of throwing, unless an explicit {@link IOException} is declared.</p>
 *
 * @author franci
 */
public class PASS
{
    /**
     * Stores or replaces a passphrase in the {@code pass} password store.
     *
     * <p>Equivalent to running {@code pass insert -f <passName>} and writing
     * the passphrase twice on standard input (as expected by {@code pass} for
     * confirmation).</p>
     *
     * @param passName   the name (path) of the entry in the password store,
     *                   e.g. {@code "gpg/mykey"}
     * @param passphrase the passphrase to store; written twice to satisfy the
     *                   {@code pass insert} confirmation prompt
     * @return {@code true} if the {@code pass} process exited with code 0;
     *         {@code false} on I/O error, process interruption, or non-zero exit
     */
    public static boolean setKey(String passName, String passphrase)
    {
        try
        {
            String[] cmd = {"pass","insert", "-f",passName};
            Process process = Runtime.getRuntime().exec(cmd);
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream())))
            {
                writer.write(passphrase);
                writer.newLine();
                writer.write(passphrase);
                writer.newLine();
            }
            return process.waitFor()==0;
        }
        catch (IOException | InterruptedException e)
        {
            return false;
        }
    }

    /**
     * Retrieves a passphrase from the {@code pass} password store.
     *
     * <p>Equivalent to running {@code pass show <passName>} and reading the
     * first line of its standard output.  The GPG decryption is performed
     * transparently by {@code pass}.</p>
     *
     * @param passName the name (path) of the entry in the password store,
     *                 e.g. {@code "gpg/mykey"}
     * @return the stored passphrase (trimmed of leading/trailing whitespace),
     *         or {@code null} if the entry does not exist, {@code pass} exits
     *         with a non-zero code, the first line is blank, or an exception
     *         occurs
     */
    public static String getKey(String passName)
    {
        try
        {
            String[] cmd = {"pass","show", passName};
            Process process = Runtime.getRuntime().exec(cmd);
            String password;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                password = reader.readLine();
            }

            int exitCode = process.waitFor();
            if (exitCode != 0)
            {
                return null;
            }

            if (password == null || password.trim().isEmpty())
            {
                return null;
            }

            return password.trim(); // Devolver la contraseña sin espacios
        }
        catch (IOException | InterruptedException e)
        {
            return null;
        }
    }

    /**
     * Lists the top-level entries in the {@code pass} password store.
     *
     * <p>Runs {@code pass} without arguments and returns each line that starts
     * with the tree-listing character {@code '├'}, which corresponds to
     * entries at the first level of the store tree.</p>
     *
     * @return a mutable list of raw tree-formatted lines beginning with
     *         {@code '├'}; never {@code null}, may be empty
     * @throws IOException if starting or reading from the {@code pass} process
     *                     fails
     */
    public static List<String> listKeys() throws IOException
    {
        Process process = Runtime.getRuntime().exec("pass");
        List<String> keys;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
        {
            String line;
            keys = new ArrayList<>();
            while((line=reader.readLine())!=null)
            {
                if(line.startsWith("├"))
                {
                    keys.add(line);
                }
            }
            return keys;
        }


//        for (String line : output.split("\n"))
//        {
//            line = line.trim();
//            if (line.startsWith("Search Terms:") || line.isEmpty())
//            {
//                continue;
//            }
//            // Eliminar prefijos de árbol y tomar solo el nombre de la clave (no carpetas)
//            String key = line.replaceAll("^[├└│─ ]+", "").trim();
//            // Verificar si es una clave (archivo .gpg en el almacén)
//            File file = new File(System.getenv().getOrDefault("PASSWORD_STORE_DIR", System.getProperty("user.home") + "/.password-store") + "/" + key + ".gpg");
//            if (file.isFile())
//            {
//                keys.add(key);
//            }
//        }
    }
}
