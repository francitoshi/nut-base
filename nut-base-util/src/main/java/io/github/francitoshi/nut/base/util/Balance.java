/*
 * Copyright (C) 2020-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A class to add and substract amounts by keys, a key can be any class type and not just a String.
 * 
 * @author franci
 * @param <K>
 */
public class Balance<K>
{
    private final Object lock = new Object();
    
    private final HashMap<K,BigDecimal> map = new HashMap<>();
    
    private final MathContext mc;
    private final int scale;

    /**
     *
     * @param mc
     * @param scale
     */
    public Balance(MathContext mc, int scale)
    {
        this.mc = mc;
        this.scale = scale;
    }

    /**
     *
     * @param precision
     * @param roundingMode
     * @param scale
     */
    public Balance(int precision, RoundingMode roundingMode, int scale)
    {
        this.mc = new MathContext(precision, roundingMode);
        this.scale = scale;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public BigDecimal add(K key, BigDecimal value)
    {
        synchronized(lock)
        {
            BigDecimal amount = this.map.getOrDefault(key, BigDecimal.ZERO);
            amount = round(amount.add(value, mc));
            this.map.put(key, amount);
            return amount;
        }
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public BigDecimal subtract(K key, BigDecimal value)
    {
        synchronized(lock)
        {
            BigDecimal amount = this.map.getOrDefault(key, BigDecimal.ZERO);
            amount = round(amount.subtract(value, mc));
            this.map.put(key, amount);
            return amount;
        }
    }

    /**
     *
     * @param key
     * @return
     */
    public BigDecimal get(K key)
    {
        synchronized(lock)
        {
            return this.map.getOrDefault(key, BigDecimal.ZERO);
        }
    }

    /**
     *
     * @param key
     * @return
     */
    public BigDecimal[] get(K[] key)
    {
        synchronized(lock)
        {
            BigDecimal[] values = new BigDecimal[key.length];
            for(int i=0;i<key.length;i++)
            {
                values[i] = get(key[i]);
            }
            return values;
        }
    }

    /**
     *
     * @param key
     * @return
     */
    public List<BigDecimal> get(List<K> key)
    {
        synchronized(lock)
        {
            ArrayList<BigDecimal> values = new ArrayList<>();
            for(K item: key)
            {
                values.add(get(item));
            }
            return values;
        }
    }
    private BigDecimal round(BigDecimal value)
    {
        return scale>=0 ? value.setScale(scale, mc.getRoundingMode()) : value;
    }
    
}
