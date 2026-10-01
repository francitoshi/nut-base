/*
 *  Coinage.java
 *
 *  Copyright (c) 2023-2025 francitoshi@gmail.com
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author franci
 */
public class Coinage
{
    private enum Holder
    {
        INSTANCE;
        final String[] FIAT_COINS = "AUD,CAD,CHF,EUR,GBP,JPY,USD".split(",");
        final String[] CRYPTO_BASE_COINS = "ADA,BEAM,BNB,BTC,DASH,DOGE,DOT,ETH,GRIN,LTC,POL,SHIB,SOL,TRX,USDC,USDT,XMR,XRP,ZEC".split(",");
        final String[] CRYPTO_COUNTER_COINS = "BTC,BNB,ETH,LTC,USDT,USDC".split(",");
        final String[] EXTRA_PAIRS = "EUR-USDC".split(",");
    }
    
    public static Coinage getDefaultInstance()
    {
        return new Coinage(Holder.INSTANCE.FIAT_COINS, Holder.INSTANCE.CRYPTO_BASE_COINS, Holder.INSTANCE.CRYPTO_COUNTER_COINS, Holder.INSTANCE.EXTRA_PAIRS);
    }
    
    private final Set<String> fiatCoins = new HashSet<>();
    private final Set<String> cryptoCoins = new HashSet<>();
    private final Set<String> allCoins = new HashSet<>();
    private final Map<String,String[]> pairs = new HashMap<>();
            
    public Coinage(String[] fiatCoins, String[] baseCoins, String[] counterCoins, String[] extraPairs)
    {
        Collections.addAll(this.fiatCoins, fiatCoins);
        Collections.addAll(this.cryptoCoins, baseCoins);
        Collections.addAll(this.cryptoCoins, counterCoins);

        Collections.addAll(this.allCoins, fiatCoins);
        Collections.addAll(this.allCoins, baseCoins);
        Collections.addAll(this.allCoins, counterCoins);

        for(String base : baseCoins)
        {
            for(String counter : fiatCoins)
            {
                this.pairs.put(base+counter, new String[]{base,counter});
            }
            for(String counter : counterCoins)
            {
                if(!counter.equalsIgnoreCase(base))
                {
                    this.pairs.put(base+counter, new String[]{base,counter});
                }
            }
        }
        for(String pair : extraPairs)
        {
            String[] p = splitPair(pair);
            this.pairs.put(p[0]+p[1], new String[]{p[0],p[1]});
        }
    }
    
    public Set<String> getAvailableCoins()
    {
        return new HashSet<>(this.allCoins);
    }
    public Set<String> getFiatCoins()
    {
        return new HashSet<>(this.allCoins);
    }
    public Set<String> getCryptoCoins()
    {
        return new HashSet<>(this.allCoins);
    }

    public boolean isAvailableCoin(String code)
    {
        return this.allCoins.contains(normalize(code));
    }
    public boolean isFiatCoin(String code)
    {
        return this.fiatCoins.contains(normalize(code));
    }
    public boolean isCryptoCoin(String code)
    {
        return this.cryptoCoins.contains(normalize(code));
    }

    public Map<String,String[]> getAvailablePairs()
    {
        return new HashMap<>(this.pairs);
    }
    public boolean isAvailablePair(String code)
    {
        return this.pairs.containsKey(normalize(code));
    }
    public String[] getPairOrCoin(String code)
    {
        String[] pair = this.pairs.get(normalize(code));
        if(pair==null && this.allCoins.contains(code))
        {
            return new String[]{code};
        }
        return pair;
    }
    public String[] getPair(String code)
    {
        String[] pair = getPairOrCoin(code);
        return pair!=null && pair.length==2 ? pair : null;
    }
    public Coinage addBaseCoins(String... bases)
    {
        String[] fiatCoins = Holder.INSTANCE.FIAT_COINS;
        String[] counterCoins = Holder.INSTANCE.CRYPTO_COUNTER_COINS;
        
        for(String base : bases)
        {
            this.allCoins.add(base);
            this.pairs.put(base, new String[]{base});
            for(String fiat : fiatCoins)
            {
                if(!fiat.equalsIgnoreCase(base))
                {
                    this.pairs.put(base+fiat, new String[]{base,fiat});
                }
            }
            for(String counter : counterCoins)
            {
                if(!counter.equalsIgnoreCase(base))
                {
                    this.pairs.put(base+counter, new String[]{base,counter});
                }
            }
        }
        return this;
    }
    
    public static String[] splitPair(String pair)
    {
        return pair.split("[-/_]");
    }    
    public static String normalize(String pair)
    {
        return pair.replaceAll("[-/_]+", "").toUpperCase();
    }    
}
