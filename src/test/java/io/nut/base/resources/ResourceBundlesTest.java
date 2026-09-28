/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.resources;

import io.nut.base.i18n.ResourceBundles;
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
