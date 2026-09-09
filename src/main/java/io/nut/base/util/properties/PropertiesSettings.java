/*
 * Copyright (C) 2018-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util.properties;

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
            try(FileInputStream in = new FileInputStream(file))
            {
                properties.load(in);
            }
            loaded = true;
        }
    }
    public void save(String comments) throws IOException
    {
        synchronized(lock)
        {
            try(FileOutputStream out = new FileOutputStream(file))
            {
                properties.store(out, comments);
            }
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
