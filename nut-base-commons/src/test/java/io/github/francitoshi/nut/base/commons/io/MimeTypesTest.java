/*
 * Copyright (C) 2014-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.commons.io;

import java.io.File;
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
public class MimeTypesTest
{
    
    public MimeTypesTest()
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
     * Test of getMimeType method, of class MimeTypes.
     */
    @Test
    public void testGetMimeType_String()
    {
        assertEquals(MimeTypes.MIME_DEFAULT_BINARY, MimeTypes.getMimeType(""));
        
        for(int i=0;i<MimeTypes.EXT_TYPES.length;i++)
        {
            String ext = MimeTypes.EXT_TYPES[i][0];
            String type= MimeTypes.EXT_TYPES[i][1];
            
            assertEquals(type, MimeTypes.getMimeType(ext), ext+"="+type);
            assertEquals(type, MimeTypes.getMimeType(ext.toLowerCase()), ext+"="+type);
            assertEquals(type, MimeTypes.getMimeType(ext.toUpperCase()), ext+"="+type);
            assertEquals(type, MimeTypes.getMimeType("."+ext), ext+"="+type);
        }
        assertEquals(MimeTypes.MIME_DEFAULT_BINARY, MimeTypes.getMimeType("filegz"));
        assertEquals(MimeTypes.MIME_DEFAULT_BINARY, MimeTypes.getMimeType("filetgz"));
    }    

    /**
     * Test of getMimeType method, of class MimeTypes.
     */
    @Test
    public void testGetMimeType_File()
    {
        assertEquals(MimeTypes.TEXT_PLAIN, MimeTypes.getMimeType(new File("file.txt")));
    }

    /**
     * Test of getMimeExtension method, of class MimeTypes.
     */
    @Test
    public void testGetMimeExtension()
    {
        assertEquals("TXT", MimeTypes.getMimeExtension("FILE.TXT"));
        assertEquals("TXT", MimeTypes.getMimeExtension("TXT"));
        assertEquals("txt", MimeTypes.getMimeExtension(".txt"));
        assertEquals("tar.gz", MimeTypes.getMimeExtension("file.tar.gz"));
        assertEquals("tar.gz", MimeTypes.getMimeExtension(".tar.gz"));
        assertEquals("tar.gz", MimeTypes.getMimeExtension("tar.gz"));
    }
}
