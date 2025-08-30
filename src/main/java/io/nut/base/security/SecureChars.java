/*
 *  SecureChars.java
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
package io.nut.base.security;

import io.nut.base.crypto.Kripto;
import io.nut.base.util.Byter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.security.auth.Destroyable;

public class SecureChars implements AutoCloseable, Destroyable
{
    private final SecureBytes secureBytes;
    private final Charset charset;
    
    public SecureChars(char[] src, Charset charset, Kripto kripto)
    {
        this.secureBytes = new SecureBytes(Byter.bytes(src, charset), kripto);
        this.charset = charset;
        if(src!=null && src.length>0)
        {
            Arrays.fill(src, '\0');
        }
    }
    
    public SecureChars(Kripto kripto, char[] src)
    {
        this(src, StandardCharsets.UTF_8, kripto);
    }

    public SecureChars(char[] src)
    {
        this(src, StandardCharsets.UTF_8, null);
    }

    public char[] getChars()
    {
        byte[] bytes = this.secureBytes.getBytes();
        char[] chars = Byter.chars(bytes, this.charset);
        if(bytes!=null && bytes.length>0)
        {
            Arrays.fill(bytes, (byte)0);
        }
        return chars;
    }

    @Override
    public void destroy()
    {
        this.secureBytes.destroy();
    }

    @Override
    public boolean isDestroyed()
    {
        return this.secureBytes.isDestroyed();
    }
    
    @Override
    public void close() throws Exception
    {
        this.destroy();
    }
    
    public void consume(Consumer<char[]> consumer)
    {
        char[] tmp = getChars();
        try
        {
            consumer.accept(tmp);
        }
        finally
        {
            if(tmp!=null && tmp.length>0)
            {
                Arrays.fill(tmp, '\0');
            }
        }
    }
    
}
