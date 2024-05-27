/*
 * AbstractPropertiesSettings.java
 *
 * Copyright (c) 2021-2023 francitoshi@gmail.com
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
