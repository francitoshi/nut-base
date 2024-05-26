/*
 * CommonsTest.java
 *
 * Copyright (c) 2013-2024 francitoshi@gmail.com
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
package io.tea.base.text;

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