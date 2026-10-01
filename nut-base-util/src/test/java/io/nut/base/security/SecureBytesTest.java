/*
 *  SecureBytesTest.java
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SecureBytesTest
{
    static final String HELLO_WORLD = "hello world";

    @Test
    public void testNull() throws Exception
    {
        SecureBytes instance = new SecureBytes(null);
        
        assertNull(instance.getBytes());

        assertTrue(instance.isDestroyed());
        
        final AtomicReference ref = new AtomicReference();
        instance.consume(new Consumer<byte[]>(){
            @Override
            public void accept(byte[] bytes)
            {
                ref.set(bytes);
            }
        });
        assertNull(ref.get());
        
        instance.close();
        instance.destroy();
    }

    @Test
    public void testEmpty() throws Exception
    {
        SecureBytes instance = new SecureBytes(new byte[0]);
        
        assertEquals(0, instance.getBytes().length);

        assertTrue(instance.isDestroyed());
        
        final AtomicInteger atomic = new AtomicInteger();
        
        instance.consume(new Consumer<byte[]>(){
            @Override
            public void accept(byte[] bytes)
            {
                atomic.set(bytes.length);
            }
        });
        
        assertEquals(0, atomic.get());
        
        instance.close();
        instance.destroy();
    }

    @Test
    public void testGetBytes()
    {
        byte[] helloWorld = HELLO_WORLD.getBytes(StandardCharsets.UTF_8);
        SecureBytes instance = new SecureBytes(HELLO_WORLD.getBytes(StandardCharsets.UTF_8));
        assertArrayEquals(helloWorld, instance.getBytes());
    }

    @Test
    public void testConsume()
    {
        byte[] helloWorld = HELLO_WORLD.getBytes(StandardCharsets.UTF_8);
        final byte[] hw = new byte[helloWorld.length];

        SecureBytes instance = new SecureBytes(HELLO_WORLD.getBytes(StandardCharsets.UTF_8));

        instance.consume(new Consumer<byte[]>()
        {
            @Override
            public void accept(byte[] bytes)
            {
                for(int i=0;i<bytes.length;i++)
                {
                    hw[i] = bytes[i];
                }
            }
        });
        
        assertArrayEquals(helloWorld, hw);
        
    }

    @Test
    public void testDestroy()
    {
        SecureBytes instance = new SecureBytes(HELLO_WORLD.getBytes(StandardCharsets.UTF_8));
        instance.destroy();
    }

    @Test
    public void testIsDestroyed()
    {
        SecureBytes instance = new SecureBytes(HELLO_WORLD.getBytes(StandardCharsets.UTF_8));
        instance.destroy();
        assertTrue(instance.isDestroyed());
    }

    @Test
    public void testClose() throws Exception
    {
        SecureBytes instance = new SecureBytes(HELLO_WORLD.getBytes(StandardCharsets.UTF_8));
        instance.close();
    }

}
