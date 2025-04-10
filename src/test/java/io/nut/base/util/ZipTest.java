/*
 *  ZipTest.java
 *
 *  Copyright (c) 2023-2025 francitoshi@gmail.com
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

import io.nut.base.encoding.Encoding;
import io.nut.base.profile.Profiler;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Random;
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
public class ZipTest
{
    
    public ZipTest()
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

    static final String[] SAMPLES = {"", "a", "ab", "abc", "abcd", "abcde", "abcdef", "aababcabcdabcdeabcdef"};

    /**
     * Test of gzip method, of class Utils.
     */
    @Test
    public void testGzip() throws Exception
    {
        for (String item : SAMPLES)
        {
            byte[] plain = item.getBytes();
            byte[] zip1 = Zip.gzip(plain);
            byte[] unzip1 = Zip.gunzip(zip1);
            assertArrayEquals(plain, unzip1);
        }
    }

    /**
     * Test of deflate method, of class Utils.
     */
    @Test
    public void testDeflate()
    {
        for (String item : SAMPLES)
        {
            byte[] plain = item.getBytes();
            byte[] zip1 = Zip.deflate(plain, 0, plain.length, 9, false);
            byte[] unzip1 = Zip.inflate(zip1, false);
            assertArrayEquals(plain, unzip1);

            byte[] zip2 = Zip.deflate(plain, 0, plain.length, 9, true);
            byte[] unzip2 = Zip.inflate(zip2, true);
            assertArrayEquals(plain, unzip2);
        }
    }
    
    static final long[][] PLAIN =
    {
        {
            0, 1, 2, 3
        },
        {
            0x11
        },
        {
            0, 0, 0
        },
    };
    static final byte[][] ZIPPP =
    {
        {
            1, 0, 1, 2, 3
        },
        {
            1, 0x11
        },
        {
            1, 0, 0, 0
        },
    };

    /**
     * Test of deflateLong method, of class Utils.
     */
    @Test
    public void testDeflateLong()
    {
        for (int i = 0; i < PLAIN.length; i++)
        {
            assertArrayEquals(ZIPPP[i], Zip.deflateLong(PLAIN[i]), "i=" + i);
        }
    }

    /**
     * Test of inflateLong method, of class Utils.
     */
    @Test
    public void testInflateLong()
    {
        for (int i = 0; i < PLAIN.length; i++)
        {
            assertArrayEquals(PLAIN[i], Zip.inflateLong(ZIPPP[i]), "i=" + i);
        }

        Random random = new Random(123456789L);
        for (int i = 0; i < 3333; i++)
        {
            long[] original = new long[i];
            for (int j = 0; j < original.length; j++)
            {
                original[j] = random.nextLong();
            }
            byte[] deflated = Zip.deflateLong(original);
            long[] inflated = Zip.inflateLong(deflated);
            assertArrayEquals(original, inflated, "a.i=" + i);

            for (int j = 0; j < original.length; j++)
            {
                original[j] = (long) (random.nextGaussian() * original.length);
            }
            deflated = Zip.deflateLong(original);
            inflated = Zip.inflateLong(deflated);
            assertArrayEquals(original, inflated, "b.i=" + i);
        }
    }

    /**
     * Test of deflateLong method, of class Utils.
     */
    @Test
    public void testDeflateLong2()
    {
        byte[] deflated = Zip.deflateLong2(PLAIN);
        long[][] inflated = Zip.inflateLong2(deflated);
        assertArrayEquals(PLAIN, inflated, "PLAIN vs plain");
    }

    /**
     * Test of gzip method, of class Utils.
     */
    @Test
    public void testGzip_byteArr() throws Exception
    {
        assertNull(Zip.gzip(null));
        String src = "Hello world!!!";
        byte[] zip = Zip.gzip(src.getBytes());
        byte[] unzip = Zip.gunzip(zip);
        String dst = new String(unzip);
        assertEquals(src, dst);
    }
    public void testGzip_byteArr_int() throws Exception
    {
        assertNull(Zip.gzip(null));
        String src = "Hello world!!!";
        byte[] zip = Zip.gzip(src.getBytes(), 0, 14);
        byte[] unzip = Zip.gunzip(zip);
        String dst = new String(unzip);
        assertEquals(src, dst);
    }

    /**
     * Test of gunzip method, of class Utils.
     */
    @Test
    public void testGunzip() throws Exception
    {
        assertNull(Zip.gunzip(null));
        String src = "Hello world!!!";
        byte[] zip = Zip.gzip(src.getBytes());
        byte[] unzip = Zip.gunzip(zip);
        String dst = new String(unzip);
        assertEquals(src, dst);
    }

    /**
     * Test of gunzip method, of class Utils.
     */
    @Test
    public void testGunzip_byteArr() throws Exception
    {
        byte[] plain = "hello world".getBytes();
        byte[] gzip  = Zip.gzip(plain);
        byte[] gunzip= Zip.gunzip(gzip);
       
        assertArrayEquals(plain, gunzip);
    }
    /**
     * Test of gunzip method, of class Utils.
     */
    @Test
    public void testGunzip_3args() throws Exception
    {
        String plain = "hello world";

        String gzip0 = Zip.gzip(plain, CharSets.UTF8, Encoding.Type.Base64);
        String gunzip0 = Zip.gunzip(gzip0, CharSets.UTF8, Encoding.Type.Base64);
        assertEquals(plain, gunzip0);

        String gzip1 = Zip.gzip(plain, CharSets.UTF8, Encoding.Type.Base91);
        String gunzip1 = Zip.gunzip(gzip1, CharSets.UTF8, Encoding.Type.Base91);
        assertEquals(plain, gunzip1);
    }

    /**
     * Test of gzip method, of class Utils.
     */
    @Test
    public void testGzip_3args() throws Exception 
    {
        String[] src =
        {
            null,
            "",
            "Now, I am become Death, the destroyer of worlds.",
            "We knew the world would not be the same. A few people laughed, a few people cried, most people were silent. I remembered the line from the Hindu scripture, the Bhagavad-Gita. Vishnu is trying to persuade the Prince that he should do his duty and to impress him takes on his multi-armed form and says, \"Now, I am become Death, the destroyer of worlds.\"I suppose we all thought that one way or another. (-J. Robert Oppenheimer)"
        };
        
        for(int i=0;i<src.length;i++)
        {
            for(Encoding.Type encoding : Encoding.Type.values())
            {
                String gzip  = Zip.gzip(src[i], CharSets.UTF8, encoding);    
                String plain = Zip.gunzip(gzip, CharSets.UTF8, encoding);
                if(src[i]==null)
                {
                    assertNull(gzip);
                    assertNull(plain);
                }
                else
                {
                    assertEquals(src[i], plain, ""+i+" "+encoding.name());                
                    System.out.printf("%d %d %d\n",src[i].length(), gzip.length(), plain.length());
                }
            }
        }
        
    }
    
    /**
     * Test of gzip method, of class Utils.
     */
    @Test
    public void testSpeed() throws Exception 
    {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<1024;i++)
        {
            sb.append(i);
        }
        byte[] bytes = sb.toString().getBytes(UTF_8);
        
        Profiler profiler = new Profiler();
        Profiler.Task gzipTask = profiler.getTask("gzip");
        Profiler.Task deflateTask = profiler.getTask("deflate");
        Profiler.Task gunzipTask = profiler.getTask("gunzip");
        Profiler.Task inflateTask = profiler.getTask("inflate");
        for(int i=0;i<10_000;i++)        
        {
            gzipTask.start();
            byte[] gzip  = Zip.gzip(bytes);
            gzipTask.stop().count();

            deflateTask.start();
            byte[] deflate  = Zip.deflate(bytes);
            deflateTask.stop().count();
            
            gunzipTask.start();
            byte[] gzip2  = Zip.gunzip(gzip);
            gunzipTask.stop().count();

            inflateTask.start();
            byte[] deflate2  = Zip.inflate(deflate);
            inflateTask.stop().count();
        }
        profiler.print();
    }
    
}
