/*
 * Copyright (C) 2021-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util.properties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

/**
 *
 * @author franci
 */
public abstract class AbstractPropertiesSettings
{
    protected final PropertiesSettings properties;

    public AbstractPropertiesSettings(PropertiesSettings properties)
    {
        this.properties = properties;
    }

    public boolean exists()
    {
        return properties.exists();
    }

    public final boolean isModified()
    {
        return properties.isModified();
    }

    public final boolean isLoaded()
    {
        return properties.isLoaded();
    }

    public final void load() throws FileNotFoundException, IOException
    {
        properties.load();
    }

    public final void save(String comments) throws IOException
    {
        properties.save(comments);
    }

    public void save(String comments, boolean sort) throws IOException
    {
        properties.save(comments, sort);
    }

    public final void sort()
    {
        properties.sort();
    }

    public final void clear()
    {
        properties.clear();
    }

    public final String getProperty(String key)
    {
        return properties.getProperty(key);
    }

    public final String getProperty(String key, String defaultValue)
    {
        return properties.getProperty(key, defaultValue);
    }

    public final Set<String> stringPropertyNames()
    {
        return properties.stringPropertyNames();
    }

    public final Object setProperty(String key, String value)
    {
        return properties.setProperty(key, value);
    }

    public final boolean containsKey(String key)
    {
        return properties.containsKey(key);
    }
    
}
