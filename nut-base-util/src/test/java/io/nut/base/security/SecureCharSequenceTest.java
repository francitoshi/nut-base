/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.security;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecureCharSequenceTest
{
    static final String HELLO_WORLD = "hello world";
  
     @Test
    public void testLength()
    {
        SecureCharSequence instance = new SecureCharSequence(HELLO_WORLD.toCharArray());
        assertEquals(HELLO_WORLD.length(), instance.length());
    }

    @Test
    public void testCharAt()
    {
        SecureCharSequence instance = new SecureCharSequence(HELLO_WORLD.toCharArray());
        assertEquals('h', instance.charAt(0));
        assertEquals('w', instance.charAt(6));
    }

    @Test
    public void testSubSequence()
    {
        SecureCharSequence instance = new SecureCharSequence(HELLO_WORLD.toCharArray());
        CharSequence expResult = instance.subSequence(6, 11);
        assertEquals('w', expResult.charAt(0));
        assertEquals('d', expResult.charAt(4));
    }  

    @Test
    public void testAcquire()
    {
        final AtomicInteger counter = new AtomicInteger();
        SecureCharSequence instance = new SecureCharSequence(HELLO_WORLD.toCharArray())
        {
            @Override
            protected void fillChars()
            {
                counter.incrementAndGet();
                super.fillChars();
            }
        };
        instance.acquire();
        instance.acquire();
        instance.acquire();
        assertEquals(1, counter.get());
    }

    @Test()
    public void testRelease()
    {
        final SecureCharSequence instance = new SecureCharSequence(HELLO_WORLD.toCharArray());
        instance.acquire();
        instance.acquire();
        instance.release();
        instance.release();
        assertThrows(IllegalStateException.class, () -> instance.release());
    }
    
}
