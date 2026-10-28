/*
 * Copyright (C) 2017-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.io.ansi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class AnsiPrintStreamTest
{
    
    public AnsiPrintStreamTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

    /**
     * Test of ansiColor method, of class Utils.
     */
    @Test
    public void testAnsiColor()
    {
        AnsiPrintStream out = new AnsiPrintStream(System.out);
        
        for(AnsiPrintStream.Color fg : AnsiPrintStream.Color.values())
        {
            for(AnsiPrintStream.Color bg : AnsiPrintStream.Color.values())
            {
                int row = fg.ordinal()*10;
                int col = bg.ordinal()*10;
                String s = fg.name()+";"+bg.name();
                out.cursor(row,col).color(fg,bg,true).println(s);
            }
        }
    }
    
}
