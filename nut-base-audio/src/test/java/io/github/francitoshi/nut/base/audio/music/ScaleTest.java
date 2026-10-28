/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.audio.music;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * @author franci
 */
public class ScaleTest
{
    
    /**
     * Test of buildEnglish method, of class Scale.
     */
    @Test
    public void testBuildEnglish()
    {
        Scale scale = Scale.buildEnglish();
        assertNotNull(scale);
        
        Note a3 = scale.get("a3");
        assertNotNull(a3);
        assertEquals(220, a3.freq, 0.0);
        
        Note a4 = scale.get("A4");
        assertNotNull(a4);
        assertEquals(440, a4.freq, 0.0);
        
        Note a5 = scale.get("A5");
        assertNotNull(a5);
        assertEquals(880, a5.freq, 0.0);
        
    }

    /**
     * Test of buildGerman method, of class Scale.
     */
    @Test
    public void testBuildGerman()
    {
        Scale scale = Scale.buildGerman();
        assertNotNull(scale);
        
        Note a3 = scale.get("a3");
        assertNotNull(a3);
        assertEquals(220, a3.freq, 0.0);
        
        Note a4 = scale.get("A4");
        assertNotNull(a4);
        assertEquals(440, a4.freq, 0.0);
        
        Note a5 = scale.get("A5");
        assertNotNull(a5);
        assertEquals(880, a5.freq, 0.0);
        
        Note h4 = scale.get("H4");
        assertNotNull(h4);
        assertEquals(493.883, h4.freq, 0.0005);
        
    }

    /**
     * Test of buildNeoLatin method, of class Scale.
     */
    @Test
    public void testBuildNeoLatin()
    {
        Scale scale = Scale.buildNeoLatin();
        assertNotNull(scale);
        
        Note a3 = scale.get("la3");
        assertNotNull(a3);
        assertEquals(220, a3.freq, 0.0);
        
        Note a4 = scale.get("La4");
        assertNotNull(a4);
        assertEquals(440, a4.freq, 0.0);
        
        Note a5 = scale.get("LA5");
        assertNotNull(a5);
        assertEquals(880, a5.freq, 0.0);
        
    }

}
