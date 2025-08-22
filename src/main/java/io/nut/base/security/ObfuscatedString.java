/*
 *  ObfuscatedString.java
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

import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * A secure string implementation that Obfuscate sensitive data using
 * xor. Implements AutoCloseable and CharSequence interfaces.
 */
public final class ObfuscatedString implements SecureString
{
    private final int[] obfuscatedChars;
    private final int[] pad;
    private volatile boolean closed;

    public ObfuscatedString(CharSequence source)
    {
        if(source==null)
        {
            this.obfuscatedChars=null;
            this.pad=null;
            return;
        }
        if(source.length()==0)
        {
            this.obfuscatedChars=new int[0];
            this.pad=new int[0];
            return;
        }
        this.obfuscatedChars = new int[source.length()];
        this.pad = new int[source.length()];
        scramble(0, source.length(), source);
    }

    public ObfuscatedString(char[] source)
    {
        this(source==null ? null : CharBuffer.wrap(source));
    }

    private ObfuscatedString(int start, int end, CharSequence source)
    {
        int length = end - start;
        this.obfuscatedChars = new int[length];
        this.pad = new int[length];
        scramble(start, end, source);
    }

    public static ObfuscatedString take(char[] password)
    {
        try
        {
            return new ObfuscatedString(password);
        }
        finally
        {
            if(password!=null && password.length>0)
            {
                Arrays.fill(password, '\0');
            }
        }    
    }
    
    private void scramble(int start, int end, CharSequence source)
    {
        SecureRandom random = new SecureRandom();
        for (int i=0, j = start; j < end; j++, i++)
        {
            char originalChar = source.charAt(j);
            int randomPad = random.nextInt();
            this.pad[i] = randomPad;
            this.obfuscatedChars[i] = randomPad ^ originalChar;
        }
    }

    @Override
    public int length()
    {
        return (closed || obfuscatedChars==null)? 0 : obfuscatedChars.length;
    }

    @Override
    public char charAt(int index)
    {
        if (closed)
        {
            throw new IllegalStateException("SecureString is closed");
        }
        if (index < 0 || index >= obfuscatedChars.length)
        {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + obfuscatedChars.length);
        }
        // Revierte la operación XOR para obtener el caracter original.
        return (char) (obfuscatedChars[index] ^ pad[index]);
    }

    @Override
    public CharSequence subSequence(int start, int end)
    {
        if (closed)
        {
            throw new IllegalStateException("SecureString is closed");
        }
        if (start < 0 || end > obfuscatedChars.length || start > end)
        {
            throw new IndexOutOfBoundsException();
        }
        return new ObfuscatedString(start, end, this);
    }

    @Override
    public void close() throws Exception
    {
        Arrays.fill(obfuscatedChars, 0);
        Arrays.fill(pad, 0);
        this.closed=true;
    }

    /**
     * Checks if the SecureString is closed
     *
     * @return true if closed
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * Decrypts the stored data
     *
     * @return Decrypted char array
     * @throws IllegalStateException if already closed
     */
    @Override
    public char[] toCharArray()
    {
        if (closed)
        {
            throw new IllegalStateException("SecureString is closed");
        }
        char[] copy = new char[length()];
        for (int i = 0; i < copy.length; i++)
        {
            copy[i] = (char) (obfuscatedChars[i] ^ pad[i]);
        }
        return copy;
    }

    /**
     * Prevents accidental exposure in logs or debug messages. The contents are 
     * never revealed.
     */
    @Override
    public String toString()
    {
        return "[ObfuscatedString: content hidden]";
    }

}
