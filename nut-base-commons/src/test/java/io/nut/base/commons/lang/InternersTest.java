/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.commons.lang;

import java.lang.ref.WeakReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class InternersTest
{
    private static String string(String value)
    {
        return new String(value.toCharArray());
    }

    static class Point3D
    {
        final int x;
        final int y;
        final int z;

        public Point3D(int x, int y, int z)
        {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int hashCode()
        {
            int hash = 7;
            hash = 29 * hash + this.x;
            hash = 29 * hash + this.y;
            hash = 29 * hash + this.z;
            return hash;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null)
            {
                return false;
            }
            if (getClass() != obj.getClass())
            {
                return false;
            }
            final Point3D other = (Point3D) obj;
            if (this.x != other.x)
            {
                return false;
            }
            if (this.y != other.y)
            {
                return false;
            }
            return this.z == other.z;
        }
        
    }
    private static Point3D point3D(int x, int y, int z)
    {
        return new Point3D(x, y, z);
    }

    @Test
    public void testStrongInterner()
    {
        Interner<Point3D> interner = Interners.strong();
        Point3D a = point3D(1,2,3);
        Point3D b = point3D(1,2,3);

        assertNotSame(a, b);
        assertSame(a, interner.intern(a));
        assertSame(a, interner.intern(b));
        assertSame(a, interner.intern(a));
    }

    @Test
    public void testWeakInterner()
    {
        Interner<String> interner = Interners.weak();
        String a = string("hello");
        String b = string("hello");

        assertNotSame(a, b);
        assertSame(a, interner.intern(a));
        assertSame(a, interner.intern(b));
        assertSame(a, interner.intern(a));
    }

    @Test
    public void testDifferentValues()
    {
        Interner<String> strong = Interners.strong();
        Interner<String> weak = Interners.weak();

        for (Interner<String> interner : new Interner[]{strong, weak})
        {
            String a = string("a");
            String b = string("b");

            assertSame(a, interner.intern(a));
            assertSame(b, interner.intern(b));
            assertNotSame(interner.intern(a), interner.intern(b));
        }
    }

    @Test
    public void testNullRejected()
    {
        assertThrows(NullPointerException.class, () -> Interners.strong().intern(null));
        assertThrows(NullPointerException.class, () -> Interners.weak().intern(null));
    }

    @Test
    public void testWeakInternerReleasesValues()
    {
        Interner<String> interner = Interners.weak();
        WeakReference<String> ref = internWeak(interner);

        for (int i = 0; i < 50 && ref.get() != null; i++)
        {
            System.gc();
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertNull(ref.get());
    }

    private static WeakReference<String> internWeak(Interner<String> interner)
    {
        String sample = string("weak");
        interner.intern(sample);
        return new WeakReference<>(sample);
    }
}
