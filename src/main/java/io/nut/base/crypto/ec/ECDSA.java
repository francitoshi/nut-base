/*
 *  ECDSA.java
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

import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.util.Objects;

public class ECDSA extends Sign
{
    //https://www.geeksforgeeks.org/blockchain-elliptic-curve-digital-signature-algorithm-ecdsa/
    //https://andrea.corbellini.name/2015/05/30/elliptic-curve-cryptography-ecdh-and-ecdsa/
    //https://lewismcombes.github.io/downloads/research/mathematics_of_bitcoin_ecdsa_lmc.pdf
    //https://en.bitcoin.it/wiki/Elliptic_Curve_Digital_Signature_Algorithm
    //https://habr.com/en/articles/692072/

    public ECDSA(Curve curve)
    {
        super(curve);
    }
    
    @Override
    public BigInteger[] sign(BigInteger msg, BigInteger secKey, BigInteger k) throws InvalidKeyException
    {
        Objects.requireNonNull(msg, "msg is null");
        Objects.requireNonNull(secKey, "secKey is null");
        Objects.requireNonNull(k, "k is null");

        Point r_point = curve.mul(curve.G, k);
        BigInteger r = r_point.x.mod(curve.n);
        if(r.compareTo(BigInteger.ZERO) == 0)
        {
            return null;
        }
        BigInteger k_inverse = k.modInverse(curve.n);
        BigInteger s = k_inverse.multiply(msg.add(r.multiply(secKey))).mod(curve.n);
        
        Point pubKey = this.getPubKey(secKey);
        if( !this.lenient && !verify(msg, pubKey, r, s) )
        {
            throw new RuntimeException("The signature does not pass verification.");
        }
        return new BigInteger[]{r, s};
    }

    @Override
    public byte[] sign(byte[] msg, byte[] secKey, byte[] auxRand) throws InvalidKeyException
    {
        Objects.requireNonNull(msg, "msg is null");
        Objects.requireNonNull(secKey, "secKey is null");
        Objects.requireNonNull(auxRand, "auxRand is null");

        if(msg.length != curve.bytes)
        {
            throw new InvalidParameterException("The message must be a "+curve.bytes+"-byte array.");
        }
        if(secKey.length != curve.bytes)
        {
            throw new InvalidParameterException("The secKey must be a "+curve.bytes+"-byte array.");
        }
        BigInteger msgNum = Utils.asBigInteger(msg);
        BigInteger secKeyNum = Utils.asBigInteger(secKey);

        BigInteger[] rs=null;
        while(rs==null)
        {
//            BigInteger k = Utils.asBigInteger(auxRand=Digest.sha256(msg, secKey,auxRand)).mod(curve.n);
            BigInteger k = Utils.asBigInteger(auxRand).mod(curve.n);
            rs = sign(msgNum, secKeyNum, k);
        }
        return DER.encode(rs);
    }
    //https://github.com/bipinkh/ecdsa/blob/master/src/main/java/ecdsa/Signature.java
    //https://github.com/Archerxy/ecdsa_java/blob/master/archer/algorithm/ecdsa/Ecdsa.java
    //https://github.com/Archerxy/ecdsa_java/blob/master/archer/algorithm/ecdsa/Ecdsa.java
    //https://github.com/bipinkh/ecdsa/blob/master/src/main/java/ecdsa/Signature.java

    @Override
    public boolean verify(BigInteger msg, Point pubKey, BigInteger r, BigInteger s) throws InvalidKeyException
    {
        Objects.requireNonNull(msg, "msg is null");
        Objects.requireNonNull(pubKey, "pubKey is null");
        Objects.requireNonNull(r, "r is null");
        Objects.requireNonNull(s, "s is null");

        
        BigInteger s_inverse = s.modInverse(curve.n);
        BigInteger u = msg.multiply(s_inverse).mod(curve.n);
        BigInteger v = r.multiply(s_inverse).mod(curve.n);
        
        Point c_point = curve.G.mul(u).add(pubKey.mul(v));
        
        return c_point.x.compareTo(r) == 0;
    }
    
    @Override
    public boolean verify(byte[] msg, byte[] pubKey, byte[] signature) throws InvalidKeyException
    {
        Objects.requireNonNull(msg, "msg is null");
        Objects.requireNonNull(pubKey, "pubKey is null");
        Objects.requireNonNull(signature, "signature is null");

        BigInteger mm = Utils.asBigInteger(msg);
        Point PK = this.pointPubKey(pubKey);
        
        BigInteger[] rs = DER.decode(signature);
        return verify(mm, PK, rs[0], rs[1]);
    }
    
    @Override
    public final Point getPubKey(BigInteger secKey) throws InvalidKeyException
    {
        Objects.requireNonNull(secKey, "secKey is null");

        if(secKey.compareTo(BigInteger.ONE) < 0 || secKey.compareTo(curve.n) >= 0)
        {
            throw new InvalidKeyException("The secret key must be an integer in the range 1..n-1.");
        }
        return curve.G.mul(secKey);
    }

    @Override
    public final byte[] getPubKey(byte[] secKey) throws InvalidKeyException
    {
        Objects.requireNonNull(secKey, "secKey is null");

        Point P = this.getPubKey(Utils.asBigInteger(secKey));
        return compressedPubKey(P.x, P.y);
    }
    
}
