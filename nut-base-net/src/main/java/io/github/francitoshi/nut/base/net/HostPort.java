/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.net;

import java.util.Objects;

public class HostPort
{
    public final String host;
    public final int port;

    public HostPort(String host, int port)
    {
        this.host = host;
        this.port = port;
    }
    
    public static HostPort parse(String s)
    {
        if (s == null || s.isEmpty())
        {
            return null;
        }
        int lastColon = s.lastIndexOf(':');
        if (lastColon <= 0 || lastColon == s.length() - 1)
        {
            return null;
        }
        String host = s.substring(0, lastColon);
        try
        {
            int port = Integer.parseInt(s.substring(lastColon + 1));
            if (port < 0 || port > 65535)
            {
                return null;
            }
            return new HostPort(host, port);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }    

    @Override
    public String toString()
    {
        return host + ':' + port;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.host);
        hash = 97 * hash + this.port;
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final HostPort other = (HostPort) obj;
        if (this.port != other.port)
        {
            return false;
        }
        return Objects.equals(this.host, other.host);
    }    
}
