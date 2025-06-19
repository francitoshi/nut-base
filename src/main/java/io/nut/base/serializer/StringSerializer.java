/*
 *  StringSerializer.java
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author franci
 */
public class StringSerializer implements Serializer<String>
{
    final private Charset charset;

    public StringSerializer()
    {
        this(StandardCharsets.UTF_8);
    }
    
    public StringSerializer(Charset charset)
    {
        this.charset = charset;
    }

    @Override
    public byte[] toBytes(String s)
    {
        return s!=null ? s.getBytes(charset) : null;
    }

    @Override
    public String fromBytes(byte[] bytes)
    {
        return bytes!=null ? new String(bytes, StandardCharsets.UTF_8) : null;
    }
    
}
