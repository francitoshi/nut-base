/*
 *  ExtKey.java
 *
 *  Copyright (C) 2023-2025 francitoshi@gmail.com
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
import io.nut.base.crypto.Kripto;
import io.nut.base.crypto.alt.RIPEMD160;
import io.nut.base.crypto.ec.Secp256k1;
import io.nut.base.encoding.Base58;
import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author franci
 */
public class ExtKey
{
    private static final Digest SHA256 = new Digest(null, Kripto.MessageDigestAlgorithm.SHA256);
    
    static final int PUBKEY = 1;
    static final int PRVKEY = 2;
    
    private static final BigInteger N = Secp256k1.INSTANCE.n; 

    public final int version;           //4 byte: version bytes (mainnet: 0x0488B21E public, 0x0488ADE4 private; testnet: 0x043587CF public, 0x04358394 private)
    public final byte depth;            //1 byte: depth: 0x00 for master nodes, 0x01 for level-1 derived keys, ....
    public final byte[] fingerprint;    //4 bytes: the fingerprint of the parent's key (0x00000000 if master key)
    public final int childNumber;       //4 bytes: child number. This is ser32(i) for i in xi = xpar/i, with xi the key being serialized. (0x00000000 if master key)
    public final byte[] chainCode;      //32 bytes: the chain code
    public final byte[] key;            //33 bytes: the public key or private key data (serP(K) for public keys, 0x00 || ser256(k) for private keys)    
    private transient final int keyType;
    
    
    public ExtKey(int version, byte depth, byte[] fingerprint, int childNumber, byte[] chainCode, byte[] key) throws InvalidKeyException
    {
        Objects.requireNonNull(depth,"depth is null");
        Objects.requireNonNull(fingerprint,"fingerprint is null");
        Objects.requireNonNull(chainCode, "chainCode is null");
        Objects.requireNonNull(key, "key is null");

        Utils.checkArgument(fingerprint.length==4,"fingerprint.length != 4");
        Utils.checkArgument(chainCode.length==32,"chainCode.length != 32");
        Utils.checkArgument(key.length==33, "key.length != 33 =>"+key.length);
        
        this.version = version;
        this.depth = depth;
        this.fingerprint = fingerprint;
        this.childNumber = childNumber;
        this.chainCode = chainCode;
        this.key = key;
        this.keyType = getPubPrvKey(version);
    }

    public ExtKey(byte[] bytes82) throws InvalidKeyException
    {
        ByteBuffer bb = ByteBuffer.wrap(bytes82).order(ByteOrder.BIG_ENDIAN);
        
        this.version = bb.getInt();
        this.depth = bb.get();
        bb.get(this.fingerprint=new byte[4]);
        this.childNumber = bb.getInt();
        bb.get(this.chainCode = new byte[32]);
        bb.get(this.key = new byte[33]);
        byte[] checksum = new byte[4];
        bb.get(checksum);
        byte[] checksum2 = SHA256.digestTwice(bytes82,0,78);
        if(Utils.compare(checksum, 0, 4, checksum2, 0, 4)!=0)
        {
            throw new IllegalArgumentException("invalid checksum");
        }
        this.keyType = getPubPrvKey(version);
    }
    
    public final ExtKey verify() throws InvalidKeyException
    {
        if(this.keyType==0)
        {
            throw new InvalidKeyException("unknown version "+this.version);
        }
        if(this.keyType==PRVKEY)
        {
            if(this.key[0]!=0)
            {
                throw new InvalidKeyException("this.key[0]!=0");
            }
            BigInteger k = Utils.newBigInteger(this.key, 1, 32);
            if(k.compareTo(BigInteger.ZERO)<=0)
            {
                throw new InvalidKeyException("private key <= 0 not in 1..n-1");
            }
            if(k.compareTo(N)>=0)
            {
                throw new InvalidKeyException("private key >= n not in 1..n-1");
            }
        }
        if(this.keyType==PUBKEY)
        {
            if(this.key[0]!=2 && this.key[0]!=3)
            {
                throw new InvalidKeyException("this.key[0]!=2 && this.key[0]!=3");
            }
            BigInteger k = Utils.newBigInteger(this.key, 0, 33);
            if(k.compareTo(N)>=0)
            {
                throw new InvalidKeyException("pubkey >= n");
            }
        }
        if(this.depth==0)
        {
            if(Utils.asInts(fingerprint)[0]!=0)
            {
                throw new InvalidKeyException("this.depth==0 && fingerprint!=0");
            }
            if(this.childNumber!=0)
            {
                throw new InvalidKeyException("depth==0 && childNumber!=0");
            }
        }
        return this;
    }
    
    public byte[] toBytes78()
    {
        ByteBuffer bb = ByteBuffer.allocate(78).order(ByteOrder.BIG_ENDIAN);
        return bb.putInt(version).put(depth).put(fingerprint).putInt(childNumber).put(chainCode).put(key).array();
    }
    
    public byte[] toBytes82()
    {
        byte[] bytes78 = toBytes78();
        byte[] checksum = Arrays.copyOfRange( SHA256.digestTwice(bytes78), 0, 4);
        return ByteBuffer.allocate(82).put(bytes78).put(checksum).array();
    }
    
    public byte[] toAddressBytes()
    {
        return new RIPEMD160().digest(this.key);
    }
    
    public String toAddressString()
    {
        return Base58.encode(this.toAddressBytes());
    }
    
    public String toString()
    {
        return Base58.encode(toBytes82());
    }
    
    public final boolean isPrvKey()
    {
        return this.keyType==PRVKEY;
    }
    public final boolean isPubKey()
    {
        return this.keyType==PUBKEY;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 53 * hash + this.version;
        hash = 53 * hash + this.depth;
        hash = 53 * hash + Arrays.hashCode(this.fingerprint);
        hash = 53 * hash + this.childNumber;
        hash = 53 * hash + Arrays.hashCode(this.chainCode);
        hash = 53 * hash + Arrays.hashCode(this.key);
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
        final ExtKey other = (ExtKey) obj;
        if (this.version != other.version)
        {
            return false;
        }
        if (this.depth != other.depth)
        {
            return false;
        }
        if (this.childNumber != other.childNumber)
        {
            return false;
        }
        if (!Arrays.equals(this.fingerprint, other.fingerprint))
        {
            return false;
        }
        if (!Arrays.equals(this.chainCode, other.chainCode))
        {
            return false;
        }
        return Arrays.equals(this.key, other.key);
    }
 
    //https://estudiobitcoin.com/que-es-la-codificacion-de-claves-xpub-ypub-zpub/
    public enum PolicyType
    {
        SingleSig, MultiSig;
    }
    public enum ScriptType
    {
        Legacy, NestedSegwit, NativeSegwit, Taproot;
    }
    public enum Network
    {
        MainNet, TestNet, RegTest, SigNet
    }
    
    static class Preset
    {
        public final String xpub;
        public final int pub;
        public final String xprv;
        public final int prv;
        public final ScriptType scriptType;
        public final PolicyType policyType;
        public final Network network;
        public Preset(String xpub, int pub, String xprv, int prv, ScriptType scriptType, PolicyType policyType, Network network)
        {
            this.xpub = xpub;
            this.pub = pub;
            this.xprv = xprv;
            this.prv = prv;
            this.scriptType = scriptType;
            this.policyType = policyType;
            this.network = network;
        }
    }

    static final ScriptType LEGACY = ScriptType.Legacy;
    static final ScriptType NESTEDSEGWIT = ScriptType.NestedSegwit;
    static final ScriptType NATIVESEGWIT = ScriptType.NativeSegwit;
    static final ScriptType TAPROOT = ScriptType.Taproot;
    
    static final PolicyType SINGLESIG = PolicyType.SingleSig;
    static final PolicyType MULTISIG = PolicyType.MultiSig;
    
    static final Network MAINNET = Network.MainNet;
    static final Network TESTNET = Network.TestNet;
    
    private static final Preset[] PRESETS = 
    new Preset[]
    {
        //mainnet single signature
        preset("xpub", 0x0488B21E, "xprv", 0x0488ADE4, LEGACY, SINGLESIG, MAINNET),
        preset("ypub", 0x049D7CB2, "yprv", 0x049D7878, NESTEDSEGWIT, SINGLESIG, MAINNET),
        preset("zpub", 0x04B24746, "zprv", 0x04b2430c, NATIVESEGWIT, SINGLESIG, MAINNET),
        preset("xpub", 0x0488B21E, "xprv", 0x0488ADE4, TAPROOT, SINGLESIG, MAINNET),
        //mainnet multi signature
        preset("Ypub", 0x0295b43f, "Yprv", 0x0295b005, NESTEDSEGWIT, MULTISIG, MAINNET),
        preset("Zpub", 0x02aa7ed3, "Zprv", 0x02aa7a99, NATIVESEGWIT, MULTISIG, MAINNET),
        //testnet single signature
        preset("tpub", 0x043587cf, "tprv", 0x04358394, LEGACY, SINGLESIG, TESTNET),
        preset("upub", 0x044a5262, "uprv", 0x044a4e28, NESTEDSEGWIT, SINGLESIG, TESTNET),
        preset("vpub", 0x045f1cf6, "vprv", 0x045f18bc, NATIVESEGWIT, SINGLESIG, TESTNET),
        preset("tpub", 0x043587cf, "tprv", 0x04358394, TAPROOT, SINGLESIG, TESTNET),
        //testnet multi signature
        preset("Upub", 0x024289ef, "Uprv", 0x024285b5, NESTEDSEGWIT, MULTISIG, TESTNET),
        preset("Vpub", 0x02575483, "Vprv", 0x02575048, NATIVESEGWIT, MULTISIG, TESTNET),
    };
            
    private static Preset preset(String xpub, int pub, String xprv, int prv, ScriptType scriptType, PolicyType policyType, Network network)
    {
        return new Preset(xpub, pub, xprv, prv, scriptType, policyType, network);
    }

    public static int getPubPrvKey(int version)
    {
        for(Preset item : PRESETS)
        {
            if(item.pub==version)
            {
                return PUBKEY;
            }
            if(item.prv==version)
            {
                return PRVKEY;
            }
        }
        return 0;
    }
    
    public static int prv2pub(int version)
    {
        for(ExtKey.Preset item : ExtKey.PRESETS)
        {
            if(item.prv==version)
            {
                return item.pub;
            }
        }
        return version;
    }
    
    public static int getVersion(int keyType, ScriptType scriptType, PolicyType policyType, Network network)
    {
        for(Preset item : PRESETS)
        {
            if(item.scriptType==scriptType && item.policyType==policyType && item.network==network)
            {
                if(keyType==PRVKEY)
                {
                    return item.prv;
                }
                if(keyType==PUBKEY)
                {
                    return item.pub;
                }
                return 0;
            }
        }
        return 0;
    }
    
}
