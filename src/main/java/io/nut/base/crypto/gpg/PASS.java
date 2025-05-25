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
 *
 * @author franci
 */
public class PASS
{
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
