/*
 * ResourceBundlesTest.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.resources;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class ResourceBundlesTest
{
    @Test
    public void testGetBundleStrict_String_LocaleArr()
    {
        ResourceBundle rb1 = ResourceBundles.getBundleStrict(ResourceBundlesTest.class.getName(), Locale.CANADA, Locale.CANADA_FRENCH, Locale.UK, Locale.US, Locale.ROOT);
        assertEquals("ROOT", rb1.getString("name"));
        
        ResourceBundle rb2 = ResourceBundles.getBundleStrict(ResourceBundlesTest.class.getName(), Locale.ITALY, Locale.UK);
        assertEquals("it_IT", rb2.getString("name"));
        
        ResourceBundle rb3 = ResourceBundles.getBundleStrict(ResourceBundlesTest.class.getName(), Locale.FRANCE, Locale.UK);
        assertEquals("ROOT", rb3.getString("name"));        
    }

    @Test
    public void testGetBundleStrict_Class_LocaleArr()
    {
        ResourceBundle rb1 = ResourceBundles.getBundleStrict(ResourceBundlesTest.class, Locale.CANADA, Locale.CANADA_FRENCH, Locale.UK, Locale.US, Locale.ROOT);
        assertEquals("ROOT", rb1.getString("name"));
        
        ResourceBundle rb2 = ResourceBundles.getBundleStrict(ResourceBundlesTest.class, Locale.ITALY, Locale.UK);
        assertEquals("it_IT", rb2.getString("name"));
        
        ResourceBundle rb3 = ResourceBundles.getBundleStrict(ResourceBundlesTest.class, Locale.FRANCE, Locale.UK);
        assertEquals("ROOT", rb3.getString("name"));        
    }

    @Test
    public void testGetResourceAsString_3args()
    {
        String result1 = ResourceBundles.getResourceAsString(ResourceBundlesTest.class, "text.txt", "");
        assertEquals("hello world", result1);

        String result2 = ResourceBundles.getResourceAsString(ResourceBundlesTest.class, "notext.txt", "blablabla");
        assertEquals("blablabla", result2);

    }

    @Test    
    public void testGetResourceAsString_Class_String()
    {
        String result1 = ResourceBundles.getResourceAsString(ResourceBundlesTest.class, "text.txt");
        assertEquals("hello world", result1);

        assertNull(ResourceBundles.getResourceAsString(ResourceBundlesTest.class, "notext.txt"));
        
    }

    
}
