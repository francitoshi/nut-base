/*
 * Copyright (C) 2020-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.os;

import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class BeepTest
{
    
    /**
     * Test of beep method, of class Beep.
     */
    @Test
    public void testBeep()
    {
        Beep.beep();
        double[] f = {440, 220, 440, 220, 440, 220};
        int[] ms = {1000, 500, 1000, 500,1000, 500};
        
        Beep.beep(f,ms);
    }
    
}
