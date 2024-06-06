/*
 * TableTest.java
 *
 * Copyright (c) 2024 francitoshi@gmail.com
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
package io.nut.base.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class TableTest
{
  
    /**
     * Test of addRowHead method, of class Table.
     */
    @Test
    public void testExample1()
    {
        Table instance = new Table(2,2, true);
        instance.setCell(0,0,"1");
        instance.setCell(0,1,"22");
        instance.setCell(1,0,"333");
        instance.setCell(1,1,"4444");
        
        String expected = "---+----+\n  1|  22|\n---+----+\n333|4444|\n---+----+\n";
        
//        System.out.println(expected);
//        System.out.println(instance.toString());
        
        assertEquals(expected, instance.toString());
    }

    /**
     * Test of addRowHead method, of class Table.
     */
    @Test
    public void testExample2()
    {
        Table instance = new Table(2,2, true);
        instance.setTitle("T");
        instance.setRowName(0,"aa");
        instance.setRowName(1,"aa");
        instance.setColName(0,"AA");
        instance.setColName(1,"BB");
        instance.setCell(0,0,"1");
        instance.setCell(0,1,"22");
        instance.setCell(1,0,"333");
        instance.setCell(1,1,"4444");
        
        String expected = "T | AA|  BB|\n--+---+----+\naa|  1|  22|\n--+---+----+\naa|333|4444|\n--+---+----+\n";
        
//        System.out.println(expected);
//        System.out.println(instance.toString());
        
        assertEquals(expected, instance.toString());
    }


    
}
