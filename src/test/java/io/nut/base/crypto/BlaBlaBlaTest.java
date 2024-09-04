/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package io.nut.base.crypto;

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
public class BlaBlaBlaTest
{
    
    public BlaBlaBlaTest()
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
     * Test of getCodex method, of class BlaBlaBla.
     */
    @Test
    public void testGetCodex()
    {
        String ABC = "ABCDEFGHIJKLM NOPQRSTUVWXYZ 01234 56789";
        String ZYX = "ZYXWVUTSRQPON MLKJIHGFEDCBA 98765 43210";
         
        Atbash instance = new Atbash(0, false);
        assertEquals(ZYX, instance.get(ABC));
        assertEquals(ABC, instance.get(ZYX));

        instance = new Atbash(0, true);
        assertEquals(ZYX.toUpperCase(), instance.get(ABC.toLowerCase()));
        assertEquals(ABC.toUpperCase(), instance.get(ZYX.toLowerCase()));

        assertEquals(ZYX.toLowerCase(), instance.get(ABC.toUpperCase()));
        assertEquals(ABC.toLowerCase(), instance.get(ZYX.toUpperCase()));
        
        String abc = "AB CD EF GH IJ KL MN OP QR ST UV WX YZ 01 23 45 67 89";
        System.out.println();
        System.out.println("  "+abc);
        for(int i=0;i<10;i++)
        {
            Atbash bbb = new Atbash(i, false);
            System.out.printf("%d %s\n", i, bbb.get(abc).toUpperCase());
        }
        
    }

       
}
