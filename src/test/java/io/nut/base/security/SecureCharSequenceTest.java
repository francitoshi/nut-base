/*
 *  SecureCharSequenceTest.java
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
