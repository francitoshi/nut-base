/*
 * ParagraphsTest.java
 *
 * Copyright (c) 2013-2025 francitoshi@gmail.com
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ParagraphsTest
{
    
    public ParagraphsTest()
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

    
    static final String[] docs = 
    {
        "a\n\n",
        "\n\na",
        "\ufffc\n\na",
        "a\n\n\ufffc\n\nb",
        " a\n\n\ufffc\n\nb ",
        " a\n\n \ufffc\n\ufffc \n\n\n \n\nb ",
    };
    static final String[][] expDirty = 
    {
        {"a"},
        {"a"},
        {"\ufffc","a"},
        {"a","\ufffc","b"},
        {"a","\ufffc","b"},
        {"a","\ufffc\n\ufffc","b"},
    };
    static final String[][] expClean = 
    {
        {"a"},
        {"a"},
        {"a"},
        {"a","b"},
        {"a","b"},
        {"a","b"},
    };
    /**
     * Test of split method, of class Paragraphs.
     */
    @Test
    public void testSplit_String_boolean()
    {
        String[] result;
        for(int i=0;i<docs.length;i++)
        {
            result = Paragraphs.split(docs[i],false);
            assertArrayEquals(expDirty[i], result, ""+i);
            
            result = Paragraphs.split(docs[i],true);
            assertArrayEquals(expClean[i], result, ""+i);
        }
    }

    /**
     * Test of split method, of class Paragraphs.
     */
    @Test
    public void testSplit_String()
    {
        String[] result;
        for(int i=0;i<docs.length;i++)
        {
            result = Paragraphs.split(docs[i]);
            assertArrayEquals( expDirty[i], result, ""+i);
        }
    }
}
