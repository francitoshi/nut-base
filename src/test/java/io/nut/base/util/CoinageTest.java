/*
 *  CoinageTest.java
 *
 *  Copyright (c) 2023 francitoshi@gmail.com
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

import java.util.Map;
import java.util.Set;
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
public class CoinageTest
{
    
    public CoinageTest()
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
     * Test of getDefaultInstance method, of class Coinage.
     */
    @Test
    public void testGetDefaultInstance()
    {
        Coinage instance = Coinage.getDefaultInstance();
        assertNotNull(instance);
    }

    /**
     * Test of getAvailableCoins method, of class Coinage.
     */
    @Test
    public void testGetAvailableCoins()
    {
        Coinage instance = Coinage.getDefaultInstance();
        Set<String> result = instance.getAvailableCoins();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Test of getFiatCoins method, of class Coinage.
     */
    @Test
    public void testGetFiatCoins()
    {
        Coinage instance = Coinage.getDefaultInstance();
        Set<String> result = instance.getFiatCoins();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Test of getCryptoCoins method, of class Coinage.
     */
    @Test
    public void testGetCryptoCoins()
    {
        Coinage instance = Coinage.getDefaultInstance();
        Set<String> result = instance.getCryptoCoins();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Test of isAvailableCoin method, of class Coinage.
     */
    @Test
    public void testIsAvailableCoin()
    {
        Coinage instance = Coinage.getDefaultInstance();
        assertTrue(instance.isAvailableCoin("USD"));
        assertFalse(instance.isAvailableCoin("xx"));
    }

    /**
     * Test of isFiatCoin method, of class Coinage.
     */
    @Test
    public void testIsFiatCoin()
    {
        Coinage instance = Coinage.getDefaultInstance();
        assertTrue(instance.isFiatCoin("USD"));
        assertFalse(instance.isFiatCoin("BTC"));
    }

    /**
     * Test of getCryptoCoin method, of class Coinage.
     */
    @Test
    public void testIsCryptoCoin()
    {
        Coinage instance = Coinage.getDefaultInstance();
        assertFalse(instance.isCryptoCoin("USD"));
        assertTrue(instance.isCryptoCoin("BTC"));
    }

    /**
     * Test of getAvailablePairs method, of class Coinage.
     */
    @Test
    public void testGetAvailablePairs()
    {
        Coinage instance = Coinage.getDefaultInstance();
        Map result = instance.getAvailablePairs();
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    /**
     * Test of isAvailablePair method, of class Coinage.
     */
    @Test
    public void testIsAvailablePair()
    {
        Coinage instance = Coinage.getDefaultInstance();
        assertTrue(instance.isAvailablePair("BTCUSD"));
        assertFalse(instance.isAvailablePair("BTCBTC"));
        
        String[] btc_usd = {"BTC", "USD"};
        assertArrayEquals(btc_usd, instance.getPair("BTCUSD"));
        assertArrayEquals(btc_usd, instance.getPair("BTC-USD"));
        assertArrayEquals(btc_usd, instance.getPair("BTC/USD"));
    }

    /**
     * Test of getPair method, of class Coinage.
     */
    @Test
    public void testGetPair()
    {
        Coinage instance = Coinage.getDefaultInstance();
        String[] btcUsd = {"BTC","USD"};
        String[] btcEur = {"BTC","EUR"};
        assertArrayEquals(btcUsd, instance.getPair("BTCUSD"));
        assertArrayEquals(btcEur, instance.getPair("BTCEUR"));
        
    }

    /**
     * Test of getPairOrCoin method, of class Coinage.
     */
    @Test
    public void testGetPairOrCoin()
    {
        Coinage instance = Coinage.getDefaultInstance();
        assertArrayEquals( Utils.asStrings("BTC"), instance.getPairOrCoin("BTC"));
        assertArrayEquals( Utils.asStrings("EUR"), instance.getPairOrCoin("EUR"));
        assertArrayEquals( Utils.asStrings("LTC"), instance.getPairOrCoin("LTC"));
        assertArrayEquals( Utils.asStrings("USD"), instance.getPairOrCoin("USD"));
        assertArrayEquals( Utils.asStrings("USDT"), instance.getPairOrCoin("USDT"));
        assertArrayEquals( Utils.asStrings("BTC","EUR"), instance.getPairOrCoin("BTCEUR"));
        assertArrayEquals( Utils.asStrings("ZEC","EUR"), instance.getPairOrCoin("ZECEUR"));
        assertArrayEquals( Utils.asStrings("ZEC","BTC"), instance.getPairOrCoin("ZECBTC"));
    }

    /**
     * Test of normalize method, of class Coinage.
     */
    @Test
    public void testNormalize()
    {
        assertEquals("BTCUSD", Coinage.normalize("BTCUSD"));
        assertEquals("BTCUSD", Coinage.normalize("BTC-USD"));
        assertEquals("BTCUSD", Coinage.normalize("BTC/USD"));
        assertEquals("BTCUSD", Coinage.normalize("btcusd"));
    }
    
}
