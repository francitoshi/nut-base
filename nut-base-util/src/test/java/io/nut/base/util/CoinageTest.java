/*
 * Copyright (C) 2024-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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

        String[] eur_usdc = {"EUR", "USDC"};
        assertArrayEquals(eur_usdc, instance.getPair("EUR-USDC"));
        assertArrayEquals(eur_usdc, instance.getPair("EUR/USDC"));
        assertArrayEquals(eur_usdc, instance.getPair("EURUSDC"));
        
        assertTrue(instance.isAvailablePair("EURUSDC"));
        assertTrue(instance.isAvailablePair("BTCEUR"));
        assertTrue(instance.isAvailablePair("ETHEUR"));
        assertTrue(instance.isAvailablePair("SOLEUR"));
//EURAEUR
        assertTrue(instance.isAvailablePair("XRPEUR"));
        assertTrue(instance.isAvailablePair("BNBEUR"));
        assertTrue(instance.isAvailablePair("SHIBEUR"));
        assertTrue(instance.isAvailablePair("DOGEEUR"));
        assertTrue(instance.isAvailablePair("ADAEUR"));
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
        assertArrayEquals( As.strings("BTC"), instance.getPairOrCoin("BTC"));
        assertArrayEquals( As.strings("EUR"), instance.getPairOrCoin("EUR"));
        assertArrayEquals( As.strings("LTC"), instance.getPairOrCoin("LTC"));
        assertArrayEquals( As.strings("USD"), instance.getPairOrCoin("USD"));
        assertArrayEquals( As.strings("USDT"), instance.getPairOrCoin("USDT"));
        assertArrayEquals( As.strings("BTC","EUR"), instance.getPairOrCoin("BTCEUR"));
        assertArrayEquals( As.strings("ZEC","EUR"), instance.getPairOrCoin("ZECEUR"));
        assertArrayEquals( As.strings("ZEC","BTC"), instance.getPairOrCoin("ZECBTC"));
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
        
        assertEquals("EURUSDC", Coinage.normalize("EUR-USDC"));
        assertEquals("EURUSDC", Coinage.normalize("EUR/USDC"));
    }
    
}
