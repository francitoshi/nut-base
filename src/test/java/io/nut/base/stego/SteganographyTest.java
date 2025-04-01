/*
 * SteganographyTest.java
 *
 * Copyright (c) 2010-2025 francitoshi@gmail.com
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
package io.nut.base.stego;

import io.nut.base.crypto.Digest;
import io.nut.base.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import javax.crypto.SecretKey;
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
public class SteganographyTest
{
    
    public SteganographyTest()
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
     * Test of splitWords method, of class Steganography.
     */
    @Test
    public void testSplitWords()
    {
        Steganography instance = new Steganography(72, true, true, false);
        
        assertArrayEquals(new String[][]{{"hello", "world"}}, instance.splitWords("hello world"));
        assertArrayEquals(new String[][]{{"hello", "world","I'm","here"}}, instance.splitWords("hello world\nI'm here"));
    }

    /**
     * Test of splitLines method, of class Steganography.
     */
    @Test
    public void testSplitLines()
    {
        {
            Steganography instance = new Steganography(72, true, true, false);
            assertArrayEquals(new String[][][]{{{"a","b","c","d","e","f"}},{{"A","B","C","D","E","F"}}}, instance.splitParagraphs("a b c\nd e f\n\nA B C\nD E F"));
        }
        {
            Steganography instance = new Steganography(72, false, false, false);
            assertArrayEquals(new String[][][]{{{"a","b","c"},{"d","e","f"}},{{"A","B","C"},{"D","E","F"}}}, instance.splitParagraphs("a b c\nd e f\n\nA B C\nD E F"));
        }
        
    }

    /**
     * Test of pack method, of class Steganography.
     */
    @Test
    public void testPack()
    {
        Random random = new Random(0);
        for(int i=0;i<1000;i++)
        {
            StringBuilder plain = new StringBuilder();
            for(int j=0;j<i;j++)
            {
                plain.append(random.nextInt(i));
            }
            byte[] bytes = plain.toString().getBytes();
            byte[] packet = Steganography.pack(bytes, false);
            byte[] bytes2 = Steganography.unpack(packet);
            assertArrayEquals(bytes,bytes2,"i="+i);
        }
    }

    private static final String STEGOWINSTEGOYOTRASHIERBASTXT = "stegowinstegoyotrashierbas.txt";

   /**
     * Test of encode method, of class Steganography.
     */
    @Test
    public void testEncode() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException 
    {
        {
            Steganography instance = new Steganography(72, true, true, false);
            String txt11 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus id ultrices ligula. Praesent in sem at turpis sagittis scelerisque. Nunc massa nulla, eleifend ac maximus sit amet, commodo vitae arcu. Etiam et elit vel magna volutpat consectetur ut quis nisi. Etiam dignissim sem vitae consectetur dapibus. Proin molestie eleifend cursus. Mauris ut fermentum neque, aliquam hendrerit felis. Sed nec quam in erat tincidunt mollis quis nec neque. Aliquam venenatis nibh ac commodo congue. Quisque blandit ligula eu dui ultrices ultrices. Suspendisse placerat efficitur ex, pharetra congue magna viverra in.";
            byte[] msg11 = ".".getBytes();

            String txt12 = instance.encode(txt11, msg11);
    
            byte[] msg2 = instance.decode(txt12);
            assertArrayEquals(msg11, msg2);
        }
        {
            Steganography instance = new Steganography(72, true, true, false);
            byte[] msg21 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit".getBytes();
            String otrashiervas = FileUtils.readFileAsString(new File("doc", STEGOWINSTEGOYOTRASHIERBASTXT));
            char[] passphrase = "eureka".toCharArray();

            String otrashiervas2 = instance.encode(otrashiervas, msg21);
            byte[] msg22 = instance.decode(otrashiervas2);
            assertArrayEquals(msg21, msg22);
            
            SecretKey keyChars = instance.deriveSecretKey(passphrase);

            String otrashiervas3 = instance.encode(otrashiervas, msg21, keyChars);
            byte[] msg3 = instance.decode(otrashiervas3, keyChars);
            assertArrayEquals(msg21, msg3);
            
            SecretKey keyBytes = instance.getSecretKey(Digest.sha256(msg3));

            String otrashiervas4 = instance.encode(otrashiervas, msg21, keyBytes);
            byte[] msg4 = instance.decode(otrashiervas4, keyBytes);
            assertArrayEquals(msg21, msg4);
        }
        {
            Steganography instance = new Steganography(72, true, true, true);
            
            byte[] msg1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.".getBytes();
            char[] pass = "eureka".toCharArray();
            String otrashiervas = FileUtils.readFileAsString(new File("doc", STEGOWINSTEGOYOTRASHIERBASTXT));

            String otrashiervas2 = instance.encode(otrashiervas, msg1);

            byte[] msg2 = instance.decode(otrashiervas2);
            assertArrayEquals(msg1, msg2);

            SecretKey keyChars = instance.deriveSecretKey(pass);
            
            String otrashiervas3 = instance.encode(otrashiervas, msg1, keyChars);
            byte[] msg3 = instance.decode(otrashiervas3, keyChars);
            assertArrayEquals(msg1, msg3);
        }
        {
            Steganography instance = new Steganography(72, true, true, true, false);
            String cypherpunk = FileUtils.readFileAsString(new File("doc", "cypherpunk-manifesto.txt"));

            byte[] msg1 = "".getBytes();
            String cypherpunk1 = instance.encode(cypherpunk, msg1);
            byte[] ret1 = instance.decode(cypherpunk1);
            assertArrayEquals(msg1, ret1, "msg1");

            byte[] msg2 = ".".getBytes();
            String cypherpunk2 = instance.encode(cypherpunk, msg2);
            byte[] ret2 = instance.decode(cypherpunk2);
            assertArrayEquals(msg2, ret2, "msg2");                        
        }
    }
    
    /**
     * Test of justify method, of class Steganography.
     */
    @Test
    public void testJustify() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        String otrashiervas = FileUtils.readFileAsString(new File("doc",STEGOWINSTEGOYOTRASHIERBASTXT));
        Steganography instance = new Steganography(72, true, true, true);
        String result = instance.justify(otrashiervas);
        assertNotNull(result);
    }
    
}
