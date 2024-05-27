/*
 *  ShamirSharedSecretTest.java
 *
 *  Copyright (C) 2018-2023 francitoshi@gmail.com
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
 *
 */
package io.nut.base.crypto;

import io.nut.base.util.Shuffles;
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
public class ShamirSharedSecretTest
{
    
    public ShamirSharedSecretTest()
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
     * Test of split method, of class ShamirSharedSecret.
     */
    @Test
    public void testSplit()
    {
        byte[] secret = "Hello World!!!".getBytes();
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(2, 2);
            byte[][] result = instance.split(secret);
            assertNotNull(result);
            assertEquals(2, result.length);
        }
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(3, 2);
            byte[][] result = instance.split(secret);
            assertNotNull(result);
            assertEquals(3, result.length);
        }
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(5, 2);
            byte[][] result = instance.split(secret);
            assertNotNull(result);
            assertEquals(5, result.length);
        }
    }

    /**
     * Test of join method, of class ShamirSharedSecret.
     */
    @Test
    public void testJoin()
    {
        byte[] secret = "Hello World!!!".getBytes();
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(3, 2);
            byte[][] result = instance.split(secret);
            assertArrayEquals(secret, instance.join(new byte[][]{result[0],result[1]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[0],result[2]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[1],result[2]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[2],result[1]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[2],result[0]}));
            assertArrayEquals(secret, instance.join(new byte[][]{result[1],result[0]}));

            assertArrayEquals(secret, instance.join(new byte[][]{result[2],result[1],result[0]}));
            assertArrayEquals(secret, instance.join(result));
            
            assertArrayEquals(secret, instance.join(new byte[][]{result[1],result[1],result[0]}));
        }
        {
            ShamirSharedSecret instance = new ShamirSharedSecret(5, 3);
            byte[][] result = instance.split(secret);
            byte[] phrase = instance.join(result);
            assertArrayEquals(secret, phrase);
        }
        {
            Shuffles shuffles = new Shuffles();
            
            secret = "Somewhere in la Mancha, in a place whose name I do not care to remember, a gentleman lived not long ago, one of those who has a lance and ancient shield on a shelf and keeps a skinny nag and a greyhound for racing.".getBytes();
            // exaustive test
            for(int p=2;p<8;p++)
            {
                for(int t=2;t<=p;t++)
                {
                    System.out.print(t+"/"+p);
                    ShamirSharedSecret instance = new ShamirSharedSecret(p, t);
                    byte[][] result = instance.split(secret);
                    System.out.println("secret.length="+secret.length);
                    System.out.println("result[0].length="+result[0].length);
                    
                    Shuffles.shuffle(result);
                    
                    // with t sub-keys secret should be known
                    result = Arrays.copyOf(result, t);
                    byte[] phrase = instance.join(result);
                    assertArrayEquals(secret, phrase);
                    System.out.print("✔");
                    
                    // with t-1 sub-keys secret should be unknown
                    result = Arrays.copyOf(result, t-1);
                    phrase = instance.join(result);
                    assertFalse(Arrays.equals(secret, phrase));
                    System.out.println("✕");
                }
            }
        }
        
        
    }
    
}
