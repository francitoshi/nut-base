/*
 *  Bip32.java
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
package io.nut.base.crypto.bips;

import io.nut.base.crypto.Digest;
import io.nut.base.crypto.HMAC;
import io.nut.base.crypto.ec.ECDSA;
import io.nut.base.crypto.ec.Secp256k1;
import io.nut.base.crypto.ec.Sign;
import io.nut.base.encoding.Base58;
import io.nut.base.encoding.Hex;
import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author franci
 */
public class Bip32
{
    private static final BigInteger N = Secp256k1.INSTANCE.n; 
    private static final ECDSA ECDSA = Sign.SECP256K1_ECDSA;
    
    enum AddrType
    {
        P2PK, P2PKH, P2MS, P2SH, P2WPKH, P2WSH, P2TR
    }
    
    public static final int HARDENED = 0x80000000;
    
    public static int hardened(int i) 
    {
        return i | HARDENED;
    }
    public static int unhardened(int i) 
    {
        return i & (~HARDENED);
    }

    public static boolean isHardened(int i) 
    {
        return (i & HARDENED) != 0;
    }
    
    public static int version2pub(int version)
    {
        return ExtKey.prv2pub(version);
    }
    
    private static final byte[] BITCOIN_SEED = "Bitcoin seed".getBytes();
    
    public static ExtKey masterKeyGeneration(byte[] seed, int keyType, ExtKey.ScriptType scriptType, ExtKey.PolicyType policyType, ExtKey.Network network) throws InvalidKeyException
    {
        byte[] I = HMAC.hmacSHA512(BITCOIN_SEED,seed);
        byte[] IL = new byte[33];
        byte[] IR = new byte[32];
        ByteBuffer.wrap(I).get(IL, 1, 32).get(IR);

        BigInteger il = Utils.newBigInteger(1, IL, 1,32);
        if(il.compareTo(BigInteger.ZERO)==0)
        {
            throw new InvalidKeyException("masterSecretKey is 0");
        }
        if(il.compareTo(N)>=0)
        {
            throw new InvalidKeyException("masterChainCode >= n");
        }

        int version = ExtKey.getVersion(keyType, scriptType, policyType, network);
        
        byte[] fingerprint = {0,0,0,0};
        
        if(keyType==ExtKey.PUBKEY)
        {
            IL = ECDSA.getPubKey(Arrays.copyOfRange(IL, 1, 33));
        }        
        return new ExtKey(version, (byte)0, fingerprint, 0, IR, IL);
    }
    
    public static ExtKey parse(String s) throws Base58.FormatException, InvalidKeyException
    {
        return new ExtKey(Base58.decode(s));
    }
    
    public static ExtKey ckdPrv(ExtKey parent, int childNumber) throws InvalidKeyException
    {
        Utils.checkArgument(parent.isPrvKey(),"parent not a private key");
        return ckdPrvPrv(parent, childNumber);
    }
    
    public static ExtKey ckdPub(ExtKey parent, int childNumber) throws InvalidKeyException
    {
        return ckdPubPub(neutered(parent), childNumber);
    }
    
    private static ExtKey ckdPrvPrv(ExtKey parent, int childNumber) throws InvalidKeyException
    {
        boolean hardened = isHardened(childNumber);
        if(hardened && parent.key[0]!=0)
        {
            throw new InvalidKeyException("parent.key[0]!=0");
        }

        byte[] data = new byte[33+4];
        if(hardened)
        {
            ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).put(parent.key).putInt(childNumber);
        }
        else
        {
            byte[] pubKey = ECDSA.getPubKey(parent.key);
            ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).put(pubKey).putInt(childNumber);
        }
        
        byte[] I = HMAC.hmacSHA512(parent.chainCode, data);
        byte[] IL = new byte[32];
        byte[] IR = new byte[32];
        ByteBuffer.wrap(I).get(IL).get(IR);

        BigInteger kpar = Utils.newBigInteger(1, parent.key, 1, 32);
        BigInteger il = new BigInteger(1, IL);
        if(il.compareTo(N)>=0)
        {
            return null;
        }
        BigInteger ki = il.add(kpar).mod(N);
        if(ki.compareTo(BigInteger.ZERO)==0)
        {
            return null;
        }

        byte[] fingerprint = getFingerprint(parent);
        
        IL = Utils.asBytes(ki, 33);
        
        return new ExtKey(parent.version, (byte)(parent.depth+1), fingerprint, childNumber, IR, IL);
    }
    
    public static ExtKey neutered(ExtKey key) throws InvalidKeyException
    {
        if(key.isPrvKey())
        {
            byte[] K = ECDSA.getPubKey(key.key);
            key = new ExtKey(version2pub(key.version), key.depth, key.fingerprint, key.childNumber, key.chainCode, K);
        }
        return key;
    }
    
    private static ExtKey ckdPubPub(ExtKey parent, int childNumber) throws InvalidKeyException
    {
        Utils.checkArgument(parent.isPubKey(),"parent key not neutered");
        Utils.checkArgument(!isHardened(childNumber),"hardened child not allowed");
        
        byte[] data = new byte[33+4];
        ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).put(parent.key).putInt(childNumber);
        byte[] I = HMAC.hmacSHA512(parent.chainCode, data);
        byte[] IL = new byte[32];
        byte[] IR = new byte[32];
        ByteBuffer.wrap(I).get(IL).get(IR);

        BigInteger Kpar = Utils.newBigInteger(1, parent.key, 1, 32);
        BigInteger il = new BigInteger(1, IL);
        if(il.compareTo(N)>=0)
        {
            return null;
        }
        BigInteger Ki = il.add(Kpar).mod(N);
        if(Ki.compareTo(BigInteger.ZERO)==0)
        {
            return null;
        }

        byte[] fingerprint = getFingerprint(parent);
        
        IL = Utils.asBytes(Ki, 33);
        
        return new ExtKey(parent.version, (byte)(parent.depth+1), fingerprint, childNumber, IR, IL);
    }
    
    private static byte[] getFingerprint(ExtKey parent) throws InvalidKeyException
    {
        byte[] pubKey;
        if(parent.key[0]==0)
        {
            pubKey = ECDSA.getPubKey(Arrays.copyOfRange(parent.key, 1, 33));
        }
        else
        {
            pubKey = parent.key;
        }
        return Arrays.copyOf(Digest.ripemd160(Digest.sha256(pubKey)),4);
    }
        
    private final ExtKey masterPub;
    private final ExtKey masterPrv;
    
    public Bip32(byte[] seed, ExtKey.ScriptType scriptType, ExtKey.PolicyType policyType, ExtKey.Network network) throws InvalidKeyException
    {
        this.masterPub = Bip32.masterKeyGeneration(seed, ExtKey.PUBKEY, scriptType, policyType, network);
        this.masterPrv = Bip32.masterKeyGeneration(seed, ExtKey.PRVKEY, scriptType, policyType, network);
    }
    
    public static Bip32 build(byte[] seed, ExtKey.ScriptType scriptType, ExtKey.PolicyType policyType, ExtKey.Network network, boolean cached) throws InvalidKeyException
    {
        return cached ? new Bip32.Cached(seed, scriptType, policyType, network) : new Bip32(seed, scriptType, policyType, network);
    }
    public ExtKey xprv(int... childNumber) throws InvalidKeyException
    {
        if(childNumber.length==0)
        {
            return this.masterPrv;
        }
        ExtKey parent = xprv(getParent(childNumber));
        Objects.requireNonNull(parent, "parent not accesible");
        return Bip32.ckdPrv(parent, getChild(childNumber));
    }

    public ExtKey xpub(int... childNumber) throws InvalidKeyException
    {
        if(childNumber.length==0)
        {
            return this.masterPub;
        }
        ExtKey extKey = xprv(childNumber);
        if(extKey!=null)
        {
            return neutered(extKey);
        }
        int child = getChild(childNumber);
        if(isHardened(child))
        {
            return null;
        }
        ExtKey parent = xpub(getParent(childNumber));
        Objects.requireNonNull(parent, "parent not accesible");
        return Bip32.ckdPub(parent, child);
    }

    public final ExtKey xprv(String path) throws InvalidKeyException
    {
        return xprv(parsePath(path));
    }
    public final ExtKey xpub(String path) throws InvalidKeyException
    {
        return xpub(parsePath(path));
    }
    
    public byte[] addr(int[] childNumber) throws InvalidKeyException
    {
        ExtKey key = xpub(childNumber);
        return key.toAddressBytes();
    }
    public final byte[] addr(int[] childNumber, int child) throws InvalidKeyException
    {
        return addr(Utils.cat(childNumber,child));
    }
    public final byte[] addr(String path) throws InvalidKeyException
    {
        return addr(parsePath(path));
    }
    
    public static int[] getParent(int[] childNumber)
    {
        return Arrays.copyOf(childNumber, childNumber.length-1);
    }
    public static String getParent(String path)
    {
        return formatPath(getParent(parsePath(path)));
    }
    public static int getChild(int[] childNumber)
    {
        return childNumber[childNumber.length-1];
    }
   
    public static int[] parsePath(String path) throws NumberFormatException
    {
        path = path.replace('\'', 'h').toLowerCase();
        String[] items = path.toLowerCase().split("/");
        int[] childNumbers = new int[items.length-1];
        for(int i=0;i<childNumbers.length;i++)
        {
            boolean hardened = items[i+1].endsWith("h");
            items[i+1] = hardened ? items[i+1].replace("h", "") : items[i+1];
            int value = Integer.parseInt(items[i+1]);
            childNumbers[i] = hardened ? Bip32.hardened(value) : value;
        }
        return childNumbers;
    }
    public static String formatPath(int[] childNumbers) throws NumberFormatException
    {
        StringBuilder s = new StringBuilder("m");

        for(int i=0;i<childNumbers.length;i++)
        {
            boolean hardened = Bip32.isHardened(childNumbers[i]);
            int value = Bip32.unhardened(childNumbers[i]);
            s.append('/').append(value);
            if(hardened)
            {
                s.append("h");
            }
        }
        return s.toString();
    }
    
    static class Cached extends Bip32
    {
        private final Map<String,ExtKey> pub = new HashMap<>();
        private final Map<String,ExtKey> prv = new HashMap<>();

        public Cached(byte[] seed, ExtKey.ScriptType scriptType, ExtKey.PolicyType policyType, ExtKey.Network network) throws InvalidKeyException
        {
            super(seed, scriptType, policyType, network);
        }

        @Override
        public byte[] addr(int... childNumber) throws InvalidKeyException
        {
            return super.addr(childNumber); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        }

        @Override
        public ExtKey xpub(int... childNumber) throws InvalidKeyException
        {
            if(childNumber.length==0)
            {
                return super.masterPub;
            }
            String path = formatPath(childNumber);
            ExtKey extKey = this.pub.get(path);
            if(extKey!=null)
            {
                return extKey;
            }
            extKey = super.xpub(childNumber);
            if(extKey!=null)
            {
                this.pub.put(path, extKey);
            }
            return extKey;
        }

        @Override
        public ExtKey xprv(int... childNumber) throws InvalidKeyException
        {
            if(childNumber.length==0)
            {
                return super.masterPrv;
            }
            String path = formatPath(childNumber);
            ExtKey extKey = this.prv.get(path);
            if(extKey!=null)
            {
                return extKey;
            }
            extKey = super.xprv(childNumber);
            if(extKey!=null)
            {
                this.prv.put(path, extKey);
            }
            return extKey;
        }
    }
}
