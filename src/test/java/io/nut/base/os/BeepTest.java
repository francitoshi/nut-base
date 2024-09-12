/*
 *  BeepTest.java
 *
 *  Copyright (C) 2020  Francisco Gómez Carrasco
 *
 *  Report bugs or new features to: flikxxi@gmail.com
 *
 */
package io.nut.base.os;

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
