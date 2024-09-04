/*
 *  AnsiPrintStreamTest.java
 *
 *  Copyright (C) 2017-2023 francitoshi@gmail.com
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
package io.nut.base.io.ansi;

import io.nut.base.io.ansi.AnsiPrintStream;
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
