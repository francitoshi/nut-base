/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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


    /**
     * Test of addRowHead method, of class Table.
     */
    @Test
    public void testExample3()
    {
        Table instance = new Table(null,null, null, true);
        
        assertEquals(0, instance.rows);
        assertEquals(0, instance.cols);

        String[] rowNames = {"A","B"};
        String[] colNames = {"a","b"};
        String[][] cells1 = {};
        String[][] cells2 = {{"1","2"},{"1","2"}};
        
        instance = new Table(null,null, cells1, true);
        
        assertEquals(0, instance.rows);
        assertEquals(0, instance.cols);

        instance = new Table(null,null, cells2, true);

        assertEquals(2, instance.rows);
        assertEquals(2, instance.cols);

        instance = new Table(rowNames,colNames, null, true);

        assertEquals(2, instance.rows);
        assertEquals(2, instance.cols);
        assertEquals("A", instance.getRowName(0));
        assertEquals("B", instance.getRowName(1));
        assertEquals("a", instance.getColName(0));
        assertEquals("b", instance.getColName(1));
    }


    
}
