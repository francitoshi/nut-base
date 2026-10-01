/*
 * HostPort.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.net;

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
