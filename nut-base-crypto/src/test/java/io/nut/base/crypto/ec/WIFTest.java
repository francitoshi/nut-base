/*
 *  WIFTest.java
 *
 *  Copyright (C) 2023 francitoshi@gmail.com
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
package io.nut.base.crypto.ec;

import io.nut.base.encoding.Hex;
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
public class WIFTest
{
    
    public WIFTest()
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
    
    //https://learnmeabitcoin.com/technical/wif
    
    static final String[][] DATA =
    {
        {"5Kdc3UAwGmHHuj6fQD1LDmKR6J3SwYyFWyHgxKAZ2cKRzVCRETY", "ef235aacf90d9f4aadd8c92e4b2562e1d9eb97f0df9ba3b508258739cb013db2","6e9aa607"},
        {"5KfpsGZoi3M3aEku65ky3hMFFwi9NGjdgeXwkyXBTRg2SshdZWe", "f42e6fe35fe2baa0f1542118c7a6175abe61e3ceba9cc27f1c87912e56467849","e60d9257"},
        {"L5EZftvrYaSudiozVRzTqLcHLNDoVn7H5HSfM9BAN6tMJX8oTWz6", "ef235aacf90d9f4aadd8c92e4b2562e1d9eb97f0df9ba3b508258739cb013db2","66557e53"},
        {"L5QNGshoA9MUMhikuS14Nu6MvxUbvtTAuzfah6UD9duFNm1PfNPf", "f42e6fe35fe2baa0f1542118c7a6175abe61e3ceba9cc27f1c87912e56467849","98886fe6"},
        
        {"KzbTzbKeotsem631kMcsvxovtH8atjurUwyNo7baXNN3G1nKkiVx", "64b43677f7ba50565530bbeef0d6a041d40b0b01bedc8176f0ec031fca1ef367","da48f3cb"},
        {"5Jadvq73DtdQ4C7M7CUAGrM9apcFUajnebf9ZekBKD6DkLPr371",  "64b43677f7ba50565530bbeef0d6a041d40b0b01bedc8176f0ec031fca1ef367","8534074c"},
    };
                                       
    /**
     * Test of decode method, of class WIF.
     */
    @Test
    public void testDecode() throws Exception
    {
        for(int i=0;i<DATA.length;i++)
        {
            WIF wif = WIF.decode(DATA[i][0]);
            assertEquals(DATA[i][1], Hex.encode(wif.getKey()),"i="+i);
            assertTrue(WIF.verify(wif),"i="+i);
            assertEquals(DATA[i][2],Hex.encode(wif.getChecksum()),"i="+i);
        }
        
    }
    
}
