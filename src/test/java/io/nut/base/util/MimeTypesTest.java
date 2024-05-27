/*
 *  MimeTypes.java
 *
 *  Copyright (C) 2014-2023 francitoshi@gmail.com
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
package io.nut.base.util;

import io.nut.base.util.MimeTypes;
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
