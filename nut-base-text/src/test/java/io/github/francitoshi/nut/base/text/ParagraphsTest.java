/*
 * Copyright (C) 2013-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.text;

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
