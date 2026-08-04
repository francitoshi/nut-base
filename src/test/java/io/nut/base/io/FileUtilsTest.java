/*
 * Copyright (C) 2013-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPOutputStream;
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
public class FileUtilsTest
{
    
    public FileUtilsTest()
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

    /**
     * Test of getExtension method, of class FileUtils.
     */
    @Test
    public void testGetExtension()
    {
        assertEquals("", FileUtils.getExtension(""));
        assertEquals("", FileUtils.getExtension("ab"));
        assertEquals("", FileUtils.getExtension("ab."));
        assertEquals("c", FileUtils.getExtension("ab.c"));
        assertEquals("cpp", FileUtils.getExtension("ab.cpp"));
        assertEquals("mp3", FileUtils.getExtension("ab.mp3"));
        assertEquals("mp3", FileUtils.getExtension("ab.mp3.mp3"));
        assertEquals("mp3", FileUtils.getExtension("ab.xyz.mp3"));
        assertEquals("", FileUtils.getExtension("ab.xyz."));

        assertEquals("", FileUtils.getExtension("",true));
        assertEquals("", FileUtils.getExtension("ab",true));
        assertEquals(".", FileUtils.getExtension("ab.",true));
        assertEquals(".c", FileUtils.getExtension("ab.c",true));
        assertEquals(".cpp", FileUtils.getExtension("ab.cpp",true));
        assertEquals(".mp3", FileUtils.getExtension("ab.mp3",true));
        assertEquals(".mp3", FileUtils.getExtension("ab.mp3.mp3",true));
        assertEquals(".mp3", FileUtils.getExtension("ab.xyz.mp3",true));
        assertEquals(".", FileUtils.getExtension("ab.xyz.",true));
        
    }
    
    /**
     * Test of createTempFile method, of class FileUtils.
     */
    @Test
    public void testCreateTempFile_String_String() throws IOException
    {
        File f1 = FileUtils.createTempFile("a", "b");
        assertEquals(false, f1.exists());
        File f2 = FileUtils.createTempFile("a", "b");
        assertEquals(false, f2.exists());
        File f3 = FileUtils.createTempFile("a", "b");
        assertEquals(false, f3.exists());
    }

    /**
     * Test of bytesFromFileGZ method, of class FileUtils.
     * @throws java.lang.Exception
     */
    @Test
    public void testBytesFromFileGZ_3args_1() throws Exception
    {
        String abc = "abc123";
        byte[] bytes = abc.getBytes("utf-8");        
        InputStream in;
        byte[] result;
        ByteArrayOutputStream baos;
        OutputStream out;
        
        //plain text
        in = new ByteArrayInputStream(bytes);
        result = FileUtils.bytesFromFileGZ(in, 100, false);
        assertArrayEquals(bytes, result);
        
        //gzip text
        baos = new ByteArrayOutputStream(32);
        out = new GZIPOutputStream(baos);
        out.write(bytes);
        out.close();
        in = new ByteArrayInputStream(baos.toByteArray());
        result = FileUtils.bytesFromFileGZ(in, 100, false);
        assertArrayEquals(bytes, result);
        
        //plain text error
        in = new ByteArrayInputStream(bytes);
        try
        {
            FileUtils.bytesFromFileGZ(in, 100, true);
            throw new Exception("format error not detected");
        }
        catch(Exception ex)
        {
            //do nothing is ok
        }
        
        //gzip text forced
        baos = new ByteArrayOutputStream(32);
        out = new GZIPOutputStream(baos);
        out.write(bytes);
        out.close();
        in = new ByteArrayInputStream(baos.toByteArray());
        result = FileUtils.bytesFromFileGZ(in, 100, true);
        assertArrayEquals(bytes, result);
        
    }

    final String HELLO_WORLD = "Hello World!!!";
    /**
     * Test of writeFile method, of class FileUtils.
     * @throws java.lang.Exception
     */
    @Test
    public void testWriteFile_String_File() throws Exception
    {
        File file = FileUtils.createTempFile("", ".tmp");
        FileUtils.writeFile(HELLO_WORLD,file);
        assertTrue(file.exists());
    }

    /**
     * Test of writeFile method, of class FileUtils.
     * @throws java.lang.Exception
     */
    @Test
    public void testWriteFile_byteArr_File() throws Exception
    {
        File file = FileUtils.createTempFile("", ".tmp");
        FileUtils.writeFile(HELLO_WORLD.getBytes(),file);
        assertEquals(HELLO_WORLD, FileUtils.readFileAsString(file));
    }

    /**
     * Test of readFileAsString method, of class FileUtils.
     * @throws java.lang.Exception
     */
    @Test
    public void testReadFileAsString() throws Exception
    {
        File file = FileUtils.createTempFile("", ".tmp");
        FileUtils.writeFile(HELLO_WORLD,file);
        assertEquals(HELLO_WORLD, FileUtils.readFileAsString(file));
    }

    /**
     * Test of copy method, of class FileUtils.
     */
    @Test
    public void testCopy_File_File() throws Exception
    {
        File file0 = FileUtils.createTempFile("0", ".tmp");
        File file1 = FileUtils.createTempFile("1", ".tmp");
        FileUtils.writeFile(HELLO_WORLD,file0);
        FileUtils.copy(file0, file1);
        assertEquals(HELLO_WORLD, FileUtils.readFileAsString(file1));
    }


    /**
     * Test of move method, of class FileUtils.
     * @throws java.lang.Exception
     */
    @Test
    public void testMove() throws Exception
    {
        File file0 = FileUtils.createTempFile("0", ".tmp");
        File file1 = FileUtils.createTempFile("1", ".tmp");
        FileUtils.writeFile(HELLO_WORLD,file0);
        FileUtils.move(file0, file1);
        assertEquals(HELLO_WORLD, FileUtils.readFileAsString(file1));
    }

    /**
     * Test of digestFileName method, of class FileUtils.
     */
    @Test
    public void testDigestFileName() throws UnsupportedEncodingException
    {
        System.out.println("01-02-2014".replaceAll("([0-9][0-9])[-/]([0-9][0-9])[-/]([0-9][0-9][0-9][0-9])", "$3-$2-$1"));
        String sample1 = "Pastafarismo. \"Con millones, si no miles [sic] de fieles devotos, la Iglesia del Monstruo del Espagueti Volador...";
        String sample2 = "PRÓXIMA ASAMBLEA SÁBADO 20 DE DICIEMBRE EN PARQUE SAN PABLO 16:00H";
        String sample3 = "Próxima Asamblea Sábado 20 de diciembre en Parque San Pablo 16:00h";
        
        assertEquals("pastafarismo-con-millones-si-no-miles.html", FileUtils.digestFileName(sample1, ".html", "-", 42, false, false));
//        assertEquals("proxima-asamblea-sabado-20-de-diciembre-en-parque-san-pablo-1600h", FileUtils.digestFileName(sample2, "", "-", 42, false, false));
//        assertEquals("proxima-asamblea-sabado-20-de-diciembre-en-parque-san-pablo-1600h", FileUtils.digestFileName(sample3, "", "-", 42, false, false));
        
        
    }
}
