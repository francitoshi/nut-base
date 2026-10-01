/*
 *  SecureCharsTest.java
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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SecureCharsTest
{
     static final String HELLO_WORLD = "hello world";

    @Test
    public void testNull() throws Exception
    {
        SecureChars instance = new SecureChars(null);
        
        assertNull(instance.getChars());

        assertTrue(instance.isDestroyed());
        
        final AtomicReference ref = new AtomicReference();
        instance.consume(new Consumer<char[]>(){
            @Override
            public void accept(char[] chars)
            {
                ref.set(chars);
            }
        });
        assertNull(ref.get());
        
        instance.close();
        instance.destroy();
    }

    @Test
    public void testGetBytes()
    {
        char[] helloWorld = HELLO_WORLD.toCharArray();
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        assertArrayEquals(helloWorld, instance.getChars());
    }

    @Test
    public void testConsume()
    {
        char[] helloWorld = HELLO_WORLD.toCharArray();
        final char[] hw = new char[helloWorld.length];

        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());

        instance.consume(new Consumer<char[]>()
        {
            @Override
            public void accept(char[] chars)
            {
                for(int i=0;i<chars.length;i++)
                {
                    hw[i] = chars[i];
                }
            }
        });
        
        assertArrayEquals(helloWorld, hw);
        
    }

    @Test
    public void testDestroy()
    {
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        instance.destroy();
    }

    @Test
    public void testIsDestroyed()
    {
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        instance.destroy();
        assertTrue(instance.isDestroyed());
    }

    @Test
    public void testClose() throws Exception
    {
        SecureChars instance = new SecureChars(HELLO_WORLD.toCharArray());
        instance.close();
    }
}
