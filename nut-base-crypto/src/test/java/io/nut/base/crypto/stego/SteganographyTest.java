/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.stego;

import io.nut.base.crypto.Kripto;
import io.nut.base.io.IO;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SteganographyTest
{
    final Kripto kripto = Kripto.getInstance(true);
    
    /**
     * Test of splitWords method, of class Steganography.
     */
    @Test
    public void testSplitWords()
    {
        Steganography instance = kripto.getSteganography(72, true, true, false);
        
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
            Steganography instance = kripto.getSteganography(72, true, true, false);
            assertArrayEquals(new String[][][]{{{"a","b","c","d","e","f"}},{{"A","B","C","D","E","F"}}}, instance.splitParagraphs("a b c\nd e f\n\nA B C\nD E F"));
        }
        {
            Steganography instance = kripto.getSteganography(72, false, false, false);
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
    public void testEncode() throws IOException, InvalidKeySpecException
    {
        {
            Steganography instance = new Steganography(72, true, true, false, 10_000);
            String txt11 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus id ultrices ligula. Praesent in sem at turpis sagittis scelerisque. Nunc massa nulla, eleifend ac maximus sit amet, commodo vitae arcu. Etiam et elit vel magna volutpat consectetur ut quis nisi. Etiam dignissim sem vitae consectetur dapibus. Proin molestie eleifend cursus. Mauris ut fermentum neque, aliquam hendrerit felis. Sed nec quam in erat tincidunt mollis quis nec neque. Aliquam venenatis nibh ac commodo congue. Quisque blandit ligula eu dui ultrices ultrices. Suspendisse placerat efficitur ex, pharetra congue magna viverra in.";
            byte[] msg11 = ".".getBytes();

            String txt12 = instance.encode(txt11, msg11);
    
            byte[] msg2 = instance.decode(txt12);
            assertArrayEquals(msg11, msg2);
        }
        {
            Steganography instance = new Steganography(72, true, true, false, 10_000);
            byte[] msg21 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit".getBytes();
            String otrashiervas = IO.readFileAsString(new File("../doc", STEGOWINSTEGOYOTRASHIERBASTXT));
            char[] passphrase = "eureka".toCharArray();

            String otrashiervas2 = instance.encode(otrashiervas, msg21);
            byte[] msg22 = instance.decode(otrashiervas2);
            assertArrayEquals(msg21, msg22);
            
            String otrashiervas3 = instance.encode(otrashiervas, msg21, passphrase);
            byte[] msg3 = instance.decode(otrashiervas3, passphrase);
            assertArrayEquals(msg21, msg3);
        }
        {
            Steganography instance = new Steganography(72, true, true, true, 10_000);
            
            byte[] msg1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.".getBytes();
            char[] pass = "eureka".toCharArray();
            String otrashiervas = IO.readFileAsString(new File("../doc", STEGOWINSTEGOYOTRASHIERBASTXT));

            String otrashiervas2 = instance.encode(otrashiervas, msg1);

            byte[] msg2 = instance.decode(otrashiervas2);
            assertArrayEquals(msg1, msg2);

            String otrashiervas3 = instance.encode(otrashiervas, msg1, pass);
            byte[] msg3 = instance.decode(otrashiervas3, pass);
            assertArrayEquals(msg1, msg3);
        }
        {
            Steganography instance = new Steganography(72, true, true, true, 10_000);
            String cypherpunk = IO.readFileAsString(new File("../doc", "cypherpunk-manifesto.txt"));

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
        String otrashiervas = IO.readFileAsString(new File("../doc",STEGOWINSTEGOYOTRASHIERBASTXT));
        Steganography instance = kripto.getSteganography(72, true, true, true);
        String result = instance.justify(otrashiervas);
        assertNotNull(result);
    }
    
}
