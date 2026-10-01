/*
 * URLsTest.java
 *
 * Copyright (c) 2014-2025 francitoshi@gmail.com
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
package io.nut.base.net;

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
public class URLsTest
{
    
    public URLsTest()
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
     * Test of replaceSpaces method, of class URLs.
     */
    @Test
    public void testReplaceSpaces()
    {
        assertEquals("http://www.redninjastudio.com/Don%20Quijote%20de%20la%20Mancha", URLs.replaceSpaces("http://www.redninjastudio.com/Don Quijote de la Mancha"));
    }

    /**
     * Test of extractDomain method, of class URLs.
     */
    @Test
    public void testExtractDomain()
    {
        assertEquals("es.wikipedia.org", URLs.extractDomain("http://es.wikipedia.org/wiki/Don_Quijote_de_la_Mancha", false));
        assertEquals("es.wikipedia.org", URLs.extractDomain("es.wikipedia.org/wiki/Don_Quijote_de_la_Mancha", true));
        assertEquals("192.168.1.33", URLs.extractDomain("http://192.168.1.33/wiki/Don_Quijote_de_la_Mancha", false));
    }

    /**
     * Test of isWebURL method, of class URLs.
     */
    @Test
    public void testIsWebURL_String()
    {
        assertTrue(URLs.isWebURL("http://www.redninjastudio.com/Don%20Quijote%20de%20la%20Mancha"));
        assertFalse(URLs.isWebURL("https://6335919209122865836_3d507a4c919b131a980ca9d00ca6aacf539f2356.blogspot.com/b/post-preview?token=EjzZ-UcBAAA"));
    }

    /**
     * Test of isWebURL method, of class URLs.
     */
    @Test
    public void testIsWebURL_String_boolean()
    {
        assertTrue(URLs.isWebURL("https://6335919209122865836_3d507a4c919b131a980ca9d00ca6aacf539f2356.blogspot.com/b/post-preview?token=EjzZ-UcBAAA", true));
        assertFalse(URLs.isWebURL("https://6335919209122865836_3d507a4c919b131a980ca9d00ca6aacf539f2356.blogspot.com/b/post-preview?token=EjzZ-UcBAAA", false));
    }

    
    private static final String[][] SAMPLES =
    {
        {"100% true", "100%25%20true", "100%25+true"}, 
        {"a e i o u", "a%20e%20i%20o%20u", "a+e+i+o+u"},
        {" ", "%20", "+"},
        {"test A.aspx#anchor B", "test%20A.aspx#anchor%20B", "test+A.aspx#anchor+B"},
        {"test A.aspx?hmm#anchor B", "test%20A.aspx?hmm#anchor%20B", "test+A.aspx?hmm#anchor+B"},
        {"my test.asp?name=ståle&car=saab","my%20test.asp?name=st%c3%a5le&car=saab", "my+test.asp?name=st%c3%a5le&car=saab"},
        {"http://www.w3schools.com","http://www.w3schools.com","http://www.w3schools.com"},
    };
    
    /**
     * Test of encode method, of class URLs.
     */
    @Test
    public void testEncode()
    {
        assertNull(URLs.encode(null, 0));
        for (String[] sample : SAMPLES)
        {
            assertEquals(sample[1], URLs.encode(sample[0], 0));
            assertEquals(sample[2], URLs.encode(sample[0], URLs.PLUS_SPACE));
        }
    }

    /**
     * Test of decode method, of class URLs.
     */
    @Test
    public void testDecode()
    {
        assertNull(URLs.decode(null));
        for (String[] sample : SAMPLES)
        {
            assertEquals(sample[0], URLs.decode(sample[1]));
            assertEquals(sample[0], URLs.decode(sample[2]));
        }
    }

    /**
     * Test of simplifyDomain method, of class URLs.
     */
    @Test
    public void testSimplifyDomain()
    {
        assertNull(URLs.simplifyDomain(null));
        assertEquals("redninjastudio.com", URLs.simplifyDomain("www.redninjastudio.com"));
        assertEquals("home.redninjastudio.com", URLs.simplifyDomain("home.redninjastudio.com"));
        assertEquals("red.redninjastudio.com", URLs.simplifyDomain("www.red.redninjastudio.com"));
    }

    /**
     * Test of cleanTrackingParameters method, of class URLs.
     */
    @Test
    public void testCleanTrackingParameters()
    {
        String[][] urls = 
        {
            {"http://www.example.com/","?utm_source=Newsletter&utm_medium=Email&utm_campaign=Blogpost"},
            {"http://www.example.com/","?utm_source=CrazyEggBlog&utm_medium=Banner1&utm_campaign=Blogpost"},
            {"http://www.example.com/","?utm_source=twitterfeed&utm_medium=facebook"},
            {"http://www.example.com/","?utm_content=buffer78fe3&utm_medium=social&utm_source=twitter.com&utm_campaign=buffer"},
            {"https://www.google.com/search?q=talktexts&newwindow=1&biw=1920&bih=971&source=lnms&tbm=shop&sa=X&ei=HlLuU4z8K6XF7AalkIGICg&ved=0CAkQ_AUoBDgU",""},
            {"https://www.google.com/#newwindow=1&q=talktexts&start=20",""},
            {"http://redninjastudio.github.io/","?utm_source=Source&utm_medium=Medium&utm_campaign=Name&utm_term=Term&utm_content=Content"},
            {"http://www.eldiario.es/politica/Zoido-medallero-condecorando-Cristo-Legion_0_628188163.html","?utm_source=Source&utm_medium=Medium&utm_campaign=Name&utm_term=Term&utm_content=Content"},
        };
        for(int i=0;i<urls.length;i++)
        {
            assertEquals(urls[i][0], URLs.cleanTrackingParameters(urls[i][0]+urls[i][1]));
        }
    }
    
}
