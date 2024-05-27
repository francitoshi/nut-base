/*
 * PropertiesSettings.java
 *
 * Copyright (c) 2018-2023 francitoshi@gmail.com
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
package io.tea.base.util.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author franci
 */
public class PropertiesSettings
{
    private final Object lock = new Object();
    private final File file;
    private final boolean autoset;
    private final Properties properties = new Properties();
    private volatile boolean modified;
    private volatile boolean loaded;

    public PropertiesSettings(File file, boolean autoset)
    {
        this.file = file;
        this.autoset = autoset;
    }

    public boolean exists()
    {
        return file.exists();
    }

    public boolean isModified()
    {
        return modified;
    }

    public boolean isLoaded()
    {
        return loaded;
    }
    
    public void load() throws FileNotFoundException, IOException
    {
        synchronized(lock)
        {
            properties.load(new FileInputStream(file));
            loaded = true;
        }
    }
    public void save(String comments) throws IOException
    {
        synchronized(lock)
        {
            properties.store(new FileOutputStream(file), comments);
            this.modified = false;
        }
    }
    public void save(String comments, boolean sort) throws IOException
    {
        synchronized(lock)
        {
            this.save(comments);
            this.sort();
        }
    }

    public void clear()
    {
        synchronized(lock)
        {
            properties.clear();
            this.modified = true;
        }
    }

    public String getProperty(String key)
    {
        synchronized(lock)
        {
            if(this.autoset && !this.properties.containsKey(key))
            {
                this.setProperty(key, "");
                return "";
            }
            return this.properties.getProperty(key);
        }
    }

    public String getProperty(String key, String defaultValue)
    {
        synchronized(lock)
        {
            if(this.autoset && !this.properties.containsKey(key) && defaultValue!=null)
            {
                this.setProperty(key, defaultValue);
                return defaultValue;
            }
            return properties.getProperty(key, defaultValue);
        }
    }

    public Set<String> stringPropertyNames()
    {
        synchronized(lock)
        {
            return properties.stringPropertyNames();
        }
    }

    public Object setProperty(String key, String value)
    {
        synchronized(lock)
        {
            this.modified = true;
            return properties.setProperty(key, value);
        }
    }

    public boolean containsKey(String key)
    {
        synchronized(lock)
        {
            return properties.containsKey(key);
        }
    }
    
    public void sort()
    {
        synchronized(lock)
        {
            sort(this.file);
        }
    }
    
    private static void sort(File properties)
    {
        try
        {
            ArrayList<String> lines = new ArrayList<>();
            try( Scanner sc = new Scanner(new FileInputStream(properties)) )
            {
                while(sc.hasNextLine())
                {
                    String line = sc.nextLine();
                    lines.add(line);
                }
            }
            Collections.sort(lines);
            try( PrintWriter pw = new PrintWriter(new FileOutputStream(properties)) )
            {
                for(String line : lines)
                {
                    pw.println(line);
                }
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(PropertiesSettings.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
