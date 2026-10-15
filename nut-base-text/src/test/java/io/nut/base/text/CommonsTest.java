/*
 * Copyright (C) 2013-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.text;

import java.util.Arrays;
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
public class CommonsTest
{
    
    public CommonsTest()
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
     * Test of getCommons method, of class Commons.
     */
    @Test
    public void testGetCommons()
    {
        String[][][] tests = 
        {
            {
                {"a","e","i","o","u"},
                {},
            },
            {
                {"aeiou"},
                {"aeiou"},
            },
            {
                {"a","ae","aei","aeio","aeiou"},
                {"a"},
            },
            {
                {"aeiou","eioua","iouae","ouaei","uaeio"},
                {"a","e","i","o","u"},
            },
            {
                {"aeiou","aeiou","aeiou","aeiou","aeiou"},
                {"aeiou","aeio","eiou","aei","eio","iou","ae","ei","io","ou","a","e","i","o","u"},
            }
        };
        
        for(int i=0;i<tests.length;i++)
        {
            String[] res = Commons.getCommons(tests[i][0]);
            assertArrayEquals(tests[i][1], res);
        }
        
        String[] test={"aeiou","aeiou","aeiou","aeiou","aeiou"};
        for(int i=0;i<15;i++)
        {
            String[] res = Commons.getCommons(test,i);
            System.out.println(i);
            System.out.println(Arrays.toString(res));
            assertEquals(i,res.length);
        }
    }

    /**
     * Test of getCommonsIgnoreCase method, of class Commons.
     */
    @Test
    public void testGetCommonsIgnoreCase()
    {
        String[][][] tests = 
        {
            {
                {"AEIOU"},
                {"aeiou"}
            },
            {
                {"AEIOU","EIOUa","IOUae","OUaei","Uaeio"},
                {"a","e","i","o","u"}
            },
            {
                {"aeiou","Aeiou","AEiou","AEIou","AEIOu","AEIOU"},
                {"aeiou","aeio","eiou","aei","eio","iou","ae","ei","io","ou","a","e","i","o","u"}
            }
        };
        
        for(int i=0;i<tests.length;i++)
        {
            String[] res = Commons.getCommonsIgnoreCase(tests[i][0]);
            for(int j=0;j<res.length;j++)
            {
                res[j] = res[j].toLowerCase();
            }
            assertArrayEquals(tests[i][1], res);
        }
    }
}
