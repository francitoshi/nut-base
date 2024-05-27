/*
 *  ExtKey.java
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
package io.nut.base.crypto.bips;

import io.nut.base.crypto.Digest;
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
    private static final BigInteger N = Secp256k1.INSTANCE.n; 
    
    public final int version;           //4 byte: version bytes (mainnet: 0x0488B21E public, 0x0488ADE4 private; testnet: 0x043587CF public, 0x04358394 private)
    public final byte depth;            //1 byte: depth: 0x00 for master nodes, 0x01 for level-1 derived keys, ....
    public final byte[] fingerprint;    //4 bytes: the fingerprint of the parent's key (0x00000000 if master key)
    public final int childNumber;       //4 bytes: child number. This is ser32(i) for i in xi = xpar/i, with xi the key being serialized. (0x00000000 if master key)
    public final byte[] chainCode;      //32 bytes: the chain code
    public final byte[] key;            //33 bytes: the public key or private key data (serP(K) for public keys, 0x00 || ser256(k) for private keys)    

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
        byte[] checksum2 = Digest.sha256Twice(bytes82,0,78);
        if(Utils.compare(checksum, 0, 4, checksum2, 0, 4)!=0)
        {
            throw new IllegalArgumentException("invalid checksum");
        }
    }
    public final ExtKey verify() throws InvalidKeyException
    {
        if(this.version!=Bip32.MAINNET_PUB && this.version!=Bip32.MAINNET_PRV && this.version!=Bip32.TESTNET_PUB && this.version!=Bip32.TESTNET_PRV)
        {
            throw new InvalidKeyException("unknown version "+this.version);
        }
        if(this.isPrvKey())
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
        if(this.isPubKey())
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
        byte[] checksum = Arrays.copyOfRange( Digest.sha256Twice(bytes78), 0, 4);
        return ByteBuffer.allocate(82).put(bytes78).put(checksum).array();
    }
    public String toString()
    {
        return Base58.encode(toBytes82());
    }
    
    public final boolean isPrvKey()
    {
        return (this.version==Bip32.MAINNET_PRV || this.version==Bip32.TESTNET_PRV);
    }
    public final boolean isPubKey()
    {
        return (this.version==Bip32.MAINNET_PUB || this.version==Bip32.TESTNET_PUB);
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
    
}
