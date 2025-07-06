/*
 *  SafeStoreTest.java
 *
 *  Copyright (c) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto;

import java.io.File;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SafeStoreTest
{

    /**
     * Test of getDerivedKey method, of class SafeStore.
     */
    @Test
    public void testGetDerivedKey() throws Exception
    {
        char[] key = "12345678901234567890".toCharArray();
        Kripto kripto = Kripto.getInstance().setMinDeriveRounds(2);
        
        File file = new File("safe-store.test");
        
        {
            SafeStore safe = new SafeStore(file, key, SafeStoreTest.class.getName(), true, 2, kripto);
            Properties properties = new Properties();

            properties.setProperty("a", "A");
            properties.setProperty("b", "B");

            safe.store(properties);

            properties.clear();

            safe.load(properties);

            String a = properties.getProperty("a");

            assertEquals(a, "A");
        }
        
        {
            SafeStore safe = new SafeStore(file, key, SafeStoreTest.class.getName(), true, 2, kripto);
            Properties properties = new Properties();
            safe.load(properties);
            properties.list(System.out);
            
            file.delete();

            assertFalse(safe.load(properties));
        } 
        
    }
    
 

    
}
