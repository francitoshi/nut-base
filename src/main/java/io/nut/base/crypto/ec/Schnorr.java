/*
 *  Schnorr.java
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
package io.nut.base.crypto.ec;

import io.nut.base.crypto.Digest;
import io.nut.base.crypto.Kripto;
import io.nut.base.util.Joins;
import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.util.Objects;

public class Schnorr extends Sign
{    
    private static final Digest SHA256 = new Digest(null, Kripto.MessageDigestAlgorithm.SHA256);
    
    public Schnorr(Curve curve)
    {
        super(curve);
    }
    
    private static byte[] taggedHash(String tag, byte[] msg)
    {
        MessageDigest md = SHA256.get();
        //sha256(tag)
        byte[] tagHash = md.digest(tag.getBytes());
        //sha256(sha256(tag)+sha256(tag)+msg)
        md.update(tagHash);
        md.update(tagHash);
        md.update(msg);
        return md.digest();
    }
    
    @Override
    public BigInteger[] sign(BigInteger msg, BigInteger secKey, BigInteger auxRand) throws InvalidKeyException
    {
        Objects.requireNonNull(msg, "msg must not be null");
        Objects.requireNonNull(secKey, "secKey must not be null");
        Objects.requireNonNull(auxRand, "auxRand must not be null");
        
        byte[] msgBytes = asBytes(msg);
        
        final Point P = curve.mul(curve.G, secKey);
        final byte[] pxBytes = asBytes(P.x);
        if(!P.hasEvenY())    
        {
            secKey = curve.n.subtract(secKey);
        }
        
        byte[] t = Utils.xor(asBytes(secKey), taggedHash("BIP0340/aux", asBytes(auxRand)));
        //t = xor_bytes(bytes_from_int(d), tagged_hash("BIP0340/aux", aux_rand))
        //k0 = int_from_bytes(tagged_hash("BIP0340/nonce", t + bytes_from_point(P) + msg)) % n
        BigInteger k0 = Utils.asBigInteger(taggedHash("BIP0340/nonce", Joins.join(t, pxBytes, msgBytes)));
        if(k0.compareTo(BigInteger.ZERO) == 0)    
        {
            throw new ArithmeticException("Failure. This happens only with negligible probability.");
        }
        final Point R = curve.mul(curve.G, k0);

        final byte[] rbytes = asBytes(R.x);

        BigInteger k = R.hasEvenY() ? k0 : curve.n.subtract(k0);

        byte[] buf = Joins.join(rbytes,pxBytes, msgBytes);
        BigInteger e = Utils.asBigInteger(taggedHash("BIP0340/challenge", buf)).mod(curve.n);
        BigInteger kes = k.add(e.multiply(secKey)).mod(curve.n);

        BigInteger r = R.x;
        BigInteger s = kes;
        Point pubKey = this.getPubKey(secKey);
        
        if(!verify(msg, pubKey, r, s))
        {
            throw new RuntimeException("The signature does not pass verification.");
        }
        
        return new BigInteger[]{r, s};
    }
    
    @Override
    public byte[] sign(byte[] msg, byte[] secKey, byte[] auxRand) throws InvalidKeyException 
    {
        Objects.requireNonNull(msg, "msg must not be null");
        Objects.requireNonNull(secKey, "secKey must not be null");
        Objects.requireNonNull(auxRand, "auxRand must not be null");

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
        for(int i=0;rs==null;i++)
        {
            BigInteger k = Utils.asBigInteger( i==0 ? auxRand : (auxRand=SHA256.digest(auxRand)));
            rs = sign(msgNum, secKeyNum, k);
        }
        byte[] r = asBytes(rs[0]);
        byte[] s = asBytes(rs[1]);
        
        assert r.length == curve.bytes;
        assert s.length == curve.bytes;
        return Joins.join(r,s);
    }
    @Override
    public boolean verify(BigInteger msg, Point pubKey, BigInteger r, BigInteger s) throws InvalidKeyException
    {
        Objects.requireNonNull(msg, "msg must not be null");
        Objects.requireNonNull(pubKey, "pubKey must not be null");
        Objects.requireNonNull(r, "r must not be null");
        Objects.requireNonNull(s, "s must not be null");

        byte[] msgBytes = asBytes(msg);

        if(r.compareTo(curve.p) >= 0 || s.compareTo(curve.n) >= 0)    
        {
            return false;
        }
        //e = int_from_bytes(tagged_hash("BIP0340/challenge", sig[0:32] + pubkey + msg)) % n
        BigInteger e = Utils.asBigInteger(taggedHash("BIP0340/challenge", Joins.join(asBytes(r), asBytes(pubKey.x), msgBytes)));
        
        Point R = curve.add(curve.mul(curve.G, s), curve.mul(pubKey, curve.n.subtract(e)));
       
        return !(R == null || R.x==null || R.y==null || !R.hasEvenY() || (R.x).compareTo(r) != 0);
    }

    @Override
    public boolean verify(byte[] msg, byte[] pubKey, byte[] signature) throws InvalidKeyException
    {
        Objects.requireNonNull(msg, "msg must not be null");
        Objects.requireNonNull(pubKey, "pubKey must not be null");
        Objects.requireNonNull(signature, "signature must not be null");

        BigInteger message = Utils.asBigInteger(msg);
        Point PK = this.pointPubKey(pubKey);
        if(PK==null)
        {
            return false;
        }
        
        BigInteger r = Utils.newBigInteger(1, signature, 0, curve.bytes);
        BigInteger s = Utils.newBigInteger(1, signature, curve.bytes, curve.bytes);
        
        return verify(message, PK, r, s);
    }

    @Override
    public final Point getPubKey(BigInteger secKey) throws InvalidKeyException
    {
        Objects.requireNonNull(secKey, "secKey must not be null");

        if(secKey.compareTo(BigInteger.ONE) < 0 || secKey.compareTo(curve.n) >= 0)
        {
            throw new InvalidKeyException("The secret key must be an integer in the range 1..n-1.");
        }
        Point pubKey = curve.G.mul(secKey);
        if(!pubKey.hasEvenY())
        {
            secKey = curve.n.subtract(secKey);
            pubKey = curve.G.mul(secKey);
        }
        return pubKey;
    }

    @Override
    public final byte[] getPubKey(byte[] secKey) throws InvalidKeyException
    {
        Objects.requireNonNull(secKey, "secKey must not be null");

        Point P = this.getPubKey(Utils.asBigInteger(secKey));
        return asBytes(P.x);
    }
}
