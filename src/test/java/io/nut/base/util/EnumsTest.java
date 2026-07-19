/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.util;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EnumsTest
{
    
    enum TestEnum
    {
        A, B, C
    }

    /**
     * Test of safeValueOf method, of class Enums.
     */
    @Test
    public void testSafeValueOf_3args_1()
    {
        assertEquals(null, Enums.safeValueOf(TestEnum.class,null,null));
        assertEquals(null, Enums.safeValueOf(TestEnum.class,"null",null));
        assertEquals(null, Enums.safeValueOf(TestEnum.class,"null",null));
        assertEquals(TestEnum.A, Enums.safeValueOf(TestEnum.class,"null",TestEnum.A));
        
        assertEquals(TestEnum.A, Enums.safeValueOf(TestEnum.class,"a",TestEnum.B));
        assertEquals(TestEnum.A, Enums.safeValueOf(TestEnum.class,"a",null));
    }

    /**
     * Test of safeValueOf method, of class Enums.
     */
    @Test
    public void testSafeValueOf_3args_2()
    {
        assertNull(Enums.safeValueOf(TestEnum.class,-1,null));
        assertEquals(TestEnum.A, Enums.safeValueOf(TestEnum.class,0,null));
        assertEquals(TestEnum.A, Enums.safeValueOf(TestEnum.class,0,TestEnum.A));
        assertEquals(TestEnum.B, Enums.safeValueOf(TestEnum.class,1,TestEnum.A));
    }

    /**
     * Test of toEnumSet method, of class Enums.
     */
    @Test
    public void testToEnumSet()
    {
        EnumSet<TestEnum> result1 = Enums.toEnumSet(TestEnum.class, TestEnum.A, TestEnum.B, TestEnum.C);
        assertEquals(3, result1.size());

        EnumSet<TestEnum> result2 = Enums.toEnumSet(TestEnum.class, TestEnum.A);
        assertEquals(1, result2.size());
    }
    
}
