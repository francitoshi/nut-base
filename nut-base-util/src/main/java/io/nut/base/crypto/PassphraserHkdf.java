/*
 *  PassphraserHkdf.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto;

import io.nut.base.crypto.kdf.HKDF;
import io.nut.base.encoding.Ascii85;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class PassphraserHkdf implements Passphraser, Closeable
{
    private final HKDF hkdf;
    private final byte[] ikm;
    private final byte[] salt;

    public PassphraserHkdf(HKDF hkdf, byte[] ikm, byte[] salt)
    {
        this.hkdf = hkdf;
        this.ikm = ikm.clone();
        this.salt = salt;
    }
    
    @Override
    public byte[] bytes(byte[] info)
    {
        return this.hkdf.deriveBytes(ikm, salt, info, ikm.length);
    }

    @Override
    public byte[] bytes(String info)
    {
        return bytes(info.getBytes(StandardCharsets.UTF_8));
    }
    @Override
    public char[] chars(byte[] info)
    {
        return Ascii85.encode(bytes(info));
    }
    @Override
    public char[] chars(String info)
    {
        return chars(info.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException
    {
        Arrays.fill(this.ikm, (byte)0);
    }
    
}
