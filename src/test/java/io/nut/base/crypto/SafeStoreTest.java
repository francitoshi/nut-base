/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
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
        Kripto kripto = Kripto.getInstance().setMinimumPbkdf2Rounds(2);
        
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
