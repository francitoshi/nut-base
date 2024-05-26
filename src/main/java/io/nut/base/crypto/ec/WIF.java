/*
 *  WIF.java
 *
 *  Copyright (C) 2023-2024 francitoshi@gmail.com
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

import io.nut.base.encoding.Base58;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 *  https://en.bitcoin.it/wiki/Wallet_import_format
 *  https://en.bitcoin.it/wiki/Private_key
 *  https://learnmeabitcoin.com/technical/wif
 * 
 * @author franci
 */
public class WIF
{
    public static final int WIF_P2PKH       =   0x80;
    public static final int WIF_P2WPKH      =   0x81;
    public static final int WIF_P2WPKH_P2SH =	0x82;
    public static final int WIF_P2SH        =   0x85;
    public static final int WIF_P2WSH       =   0x86;
    public static final int WIF_P2WSH_P2SH  =   0x87;
    
    public static final byte MAINNET  =   (byte) 0x80;
    public static final byte TESTNET  =   (byte) 0xef;
    
    private final byte network;
    private final byte[] key;
    private final boolean compressed;
    private final byte[] checksum;
    
    public WIF(byte network, byte[] key, boolean compressed, byte[] checksum)
    {
        this.network = network;
        this.key = key;
        this.compressed = compressed;
        this.checksum = checksum;
    }
    public WIF(byte network, byte[] key, boolean compressed) throws NoSuchAlgorithmException
    {
        this.network = network;
        this.key = key;
        this.compressed = compressed;
        this.checksum = checksum(network, key, compressed);
    }

    public static String encode(WIF wif)
    {
        int cmpr = wif.compressed ? 1 : 0;
        byte[] bytes = new byte[37+cmpr];
        bytes[0] = wif.network;
        System.arraycopy(wif.key, 0, bytes, 1, 32);
        System.arraycopy(wif.checksum, 0, bytes, 33+cmpr, 4);
        return Base58.encode(bytes);
    }

    public static byte[] checksum(byte network, byte[] key, boolean compressed) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA256");
        md.update(network);
        md.update(key);
        if(compressed)
        {
            md.update((byte)1);
        }
        return Arrays.copyOfRange(md.digest(md.digest()),0,4);
    }
    
    public static boolean verify(WIF wif) throws NoSuchAlgorithmException
    {
        byte[] cs = checksum(wif.network, wif.key, wif.compressed);
        for(int i=0;i<wif.checksum.length;i++)
        {
            if(cs[i]!=wif.checksum[i])
            {
                return false;
            }
        }
        return true;
    }

    public static WIF decode(String wif) throws Base58.FormatException
    {
        byte[] bytes = Base58.decode(wif);
        
        byte network = bytes[0];
        byte[] key = Arrays.copyOfRange(bytes, 1, 33);
        boolean compressed = bytes[33]==1 && bytes.length>37;
        int cmpr = compressed ? 1 : 0;
        byte[] checksum = Arrays.copyOfRange(bytes, 33+cmpr, 37+cmpr);
        return new WIF(network, key, compressed, checksum);
    }

    public byte getNetwork()
    {
        return this.network;
    }

    public byte[] getKey()
    {
        return this.key.clone();
    }

    public byte[] getChecksum()
    {
        return this.checksum.clone();
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 67 * hash + this.network;
        hash = 67 * hash + Arrays.hashCode(this.key);
        hash = 67 * hash + (this.compressed ? 1 : 0);
        hash = 67 * hash + Arrays.hashCode(this.checksum);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final WIF other = (WIF) obj;
        if (this.network != other.network)
        {
            return false;
        }
        if (this.compressed != other.compressed)
        {
            return false;
        }
        if (!Arrays.equals(this.key, other.key))
        {
            return false;
        }
        return Arrays.equals(this.checksum, other.checksum);
    }
    
}
