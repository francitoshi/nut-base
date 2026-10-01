/*
 *  SecureCharSequence.java
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
import io.nut.base.crypto.Rand;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

public class SecureCharSequence implements CharSequence, Destroyable
{
    private static final String HIDDEN_CONTENT_MESSAGE = "[SecureCharSequence: ****]";
    private static final Rand rand = Kripto.getRand();
    
    private final AtomicInteger acquireCount = new AtomicInteger();
    private final int[] chars;
    private final int[] xorKey;
    private final int length;
    private final SecureChars secureChars;
    
    public SecureCharSequence(char[] src, Charset charset, Kripto kripto)
    {
        if (src == null)
        {
            throw new IllegalArgumentException("El array de caracteres no puede ser nulo.");
        }
        this.length = src.length;
        this.chars = new int[this.length];
        this.xorKey = new int[this.length]; 
        this.secureChars = new SecureChars(src, charset, kripto);
    }
    
    public SecureCharSequence(char[] src, Charset charset)
    {
        this(src, charset, null);
    }
    
    public SecureCharSequence(char[] src)
    {
        this(src, StandardCharsets.UTF_8, null);
    }

    @Override
    public int length()
    {
        return this.length;
    }

    @Override
    public char charAt(int index)
    {
        if (index < 0)
        {
            throw new IndexOutOfBoundsException(index+"< 0");
        }
        if (index >= this.length)
        {
            throw new IndexOutOfBoundsException(index+">="+this.length);
        }
        this.acquire();
        try
        {
            return (char) (this.chars[index] ^ this.xorKey[index]);
        }
        finally
        {
            this.release();
        }
        
    }

    @Override
    public String toString()
    {
        return HIDDEN_CONTENT_MESSAGE;
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        char[] plaintext = this.secureChars.getChars();
        try
        {
            char[] subtext = Arrays.copyOfRange(plaintext, start, end);
            return new SecureCharSequence(subtext);
        }
        finally
        {
            Arrays.fill(plaintext, '\0');
        }
    }

    @Override
    public void destroy() throws DestroyFailedException
    {
        Arrays.fill(this.chars, 0);
        Arrays.fill(this.xorKey, 0);
        this.secureChars.destroy();
    }

    @Override
    public boolean isDestroyed()
    {
        return this.secureChars.isDestroyed();
    }

    public void consume(Consumer<CharSequence> consumer)
    {
        this.acquire();
        try
        {
            consumer.accept(this);
        }
        finally
        {
            this.release();
        }
    }
    
    public char[] getChars()
    {
        return this.secureChars.getChars();
    }

    protected void fillChars()
    {
        rand.nextInts(xorKey);
        char[] src = this.secureChars.getChars();
        for (int i=0; i < src.length; i++)
        {
            this.chars[i] = xorKey[i] ^ src[i];
            src[i] = 0;
        }
    }

    private final Object lock = new Object();
    
    public void acquire()
    {
        synchronized(lock)
        {
            int count = this.acquireCount.getAndIncrement();
            if(count==0)
            {
                fillChars();
            }
        }
    }    
    
    public void release()
    {
        synchronized(lock)
        {
            int count = this.acquireCount.decrementAndGet();
            if(count==0)
            {
                Arrays.fill(chars, 0);
                Arrays.fill(xorKey, 0);
            }
            else if(count<0)
            {
                throw new IllegalStateException("acquireCount="+count);
            }
        }
    }
    
}
