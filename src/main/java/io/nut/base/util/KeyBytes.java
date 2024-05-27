/*
 *  KeyBytes.java
 *
 *  Copyright (c) 2023-2024 francitoshi@gmail.com
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
package io.nut.base.util;

import io.nut.base.encoding.Hex;
import java.io.Serializable;
import java.util.Arrays;

/**
 * This class is intended for encapsulata a byte[] and use it as keys in a Map&lt;KeyBytes,byte[]&gt; because using byte[] as key
 * compare array memory address and not the the content.
 * 
 * @author franci
 */
public class KeyBytes implements Comparable<KeyBytes>, Serializable
{
    
    final byte[] bytes;

    public KeyBytes(byte[] bytes)
    {
        this.bytes = bytes;
    }

    public KeyBytes(String bytes)
    {
        this.bytes = Hex.decode(bytes);
    }

    @Override
    public int compareTo(KeyBytes other)
    {
        return Utils.compare(this.bytes, other.bytes);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + Arrays.hashCode(this.bytes);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final KeyBytes other = (KeyBytes) obj;
        return Arrays.equals(this.bytes, other.bytes);
    }

    @Override
    public String toString()
    {
        return Hex.encode(bytes);
    }
    
}
