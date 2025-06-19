/*
 *  Base64Serializer.java
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
package io.nut.base.serializer;

import java.util.Base64;

/**
 *
 * @author franci
 */
public class Base64Serializer implements Serializer<String>
{
    @Override
    public byte[] toBytes(String s)
    {
        return s!=null ? Base64.getDecoder().decode(s) : null;
    }

    @Override
    public String fromBytes(byte[] bytes)
    {
        return bytes!=null ? Base64.getEncoder().encodeToString(bytes) : null;
    }
    
}
