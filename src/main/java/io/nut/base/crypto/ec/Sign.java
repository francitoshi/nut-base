/*
 *  Sign.java
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

import java.math.BigInteger;
import java.security.InvalidKeyException;

/**
 *
 * @author franci
 */
public abstract class Sign
{
    public static final ECDSA SECP256K1_ECDSA = new ECDSA(Secp256k1.INSTANCE);

    public static final Schnorr SECP256K1_SCHNORR = new Schnorr(Secp256k1.INSTANCE);

    public final Curve curve;
    
    volatile boolean debug;
    volatile boolean lenient;
    
    public Sign(Curve curve)
    {
        this.curve = curve;
    }

    /**
     * sets debug mode: true=> shows debug info. false=> do not show debug info.
     * @param value
     */
    public void setDebug(boolean value)
    {
        this.debug = value;
    }

    /**
     * gets debug mode: true=> shows debug info. false=> do not show debug info.
     * @return the status of debug mode
     */
    public boolean isDebug()
    {
        return debug;
    }

    /**
     * gets if the lenient mode is on
     * @return
     */
    public boolean isLenient()
    {
        return lenient;
    }

    /**
     * sets leniento mode: 
     *      false=> ensure that signature pass verification.
     *      true=> omit testing the signature after signing
     * @param value
     */
    public void setLenient(boolean value)
    {
        this.lenient = value;
    }

    public abstract BigInteger[] sign(BigInteger msg, BigInteger secKey, BigInteger auxRand) throws InvalidKeyException;
    public abstract byte[] sign(byte[] msg, byte[] secKey, byte[] auxRand) throws InvalidKeyException;

    public abstract boolean verify(BigInteger msg, Point pubKey, BigInteger r, BigInteger s) throws InvalidKeyException;
    public abstract boolean verify(byte[] msg, byte[] pubkey, byte[] signature) throws InvalidKeyException;

    public abstract Point getPubKey(BigInteger secKey) throws InvalidKeyException;
    public abstract byte[] getPubKey(byte[] secKey) throws InvalidKeyException;

    public final byte[] genSecKey()
    {
        return curve.genSecKey();
    }
        
    final byte[] asBytes(BigInteger n)
    {
        return curve.asBytes(n);
    }
    
    public final byte[] rawPubKey(BigInteger x, BigInteger y)
    {
        return curve.rawPubKey(x, y);
    }
    
    public final byte[] compressedPubKey(Point point)
    {
        return curve.compressedPubKey(point);
    }
    public final byte[] compressedPubKey(BigInteger x, BigInteger y)
    {
        return curve.compressedPubKey(x, y);
    }
    
    public final byte[] uncompressedPubKey(BigInteger x, BigInteger y)
    {
        return curve.uncompressedPubKey(x, y);
    }
    public final Point pointPubKey(byte[] pubKey) throws InvalidKeyException
    {
        return curve.pointPubKey(pubKey);
    }
    
}
