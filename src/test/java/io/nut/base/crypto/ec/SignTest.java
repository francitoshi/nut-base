/*
 * Copyright (C) 2023-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto.ec;

import io.nut.base.crypto.Digest;
import io.nut.base.crypto.Kripto;
import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SignTest
{
    
    static final Digest SHA256 = new Digest(null, Kripto.MessageDigestAlgorithm.SHA256);

 
    /**
     * Test of rawPubKey method, of class Sign.
     */
    @Test
    public void testRawPubKey() throws InvalidKeyException
    {
         Sign instance = Sign.SECP256K1_ECDSA;
        
        byte[] secKey = instance.genSecKey();
        BigInteger secKeyNum = Utils.asBigInteger(secKey);
        Point expected = instance.getPubKey(secKeyNum);
        
        byte[] cpk = instance.rawPubKey(expected.x, expected.y);

        Point result = instance.pointPubKey(cpk);

        assertEquals(expected.x, result.x);

    }

    /**
     * Test of compressedPubKey method, of class Sign.
     */
    @Test
    public void testCompressedPubKey() throws InvalidKeyException
    {
         Sign instance = Sign.SECP256K1_ECDSA;
        
        byte[] secKey = instance.genSecKey();
        BigInteger secKeyNum = Utils.asBigInteger(secKey);
        Point expected = instance.getPubKey(secKeyNum);
        
        byte[] cpk = instance.compressedPubKey(expected.x, expected.y);

        Point result = instance.pointPubKey(cpk);

        assertEquals(expected.x, result.x);

    }

    /**
     * Test of uncompressedPubKey method, of class Sign.
     */
    @Test
    public void testUncompressedPubKey() throws InvalidKeyException
    {
         Sign instance = Sign.SECP256K1_ECDSA;
        
        byte[] secKey = instance.genSecKey();
        BigInteger secKeyNum = Utils.asBigInteger(secKey);
        Point expected = instance.getPubKey(secKeyNum);
        
        byte[] cpk = instance.uncompressedPubKey(expected.x, expected.y);

        Point result = instance.pointPubKey(cpk);

        assertEquals(expected.x, result.x);

    }

    /**
     * Test of pointPubKey method, of class Sign.
     */
    @Test
    public void testPointPubKey() throws Exception
    {
        ECDSA ecdsa = Sign.SECP256K1_ECDSA;

        byte[] secKey0 = ecdsa.genSecKey();
        BigInteger secKey1 = Utils.asBigInteger(secKey0);
        
        byte[] pubKey0 = ecdsa.getPubKey(secKey0);
        Point pubKey1 = ecdsa.pointPubKey(pubKey0);
        
        Point pubKey2 = ecdsa.getPubKey(secKey1);
       
        assertEquals(pubKey1, pubKey2);
        
    }
    /**
     * Test of sign method, of class ECDSA.
     * @throws java.lang.Exception
     */
    @Test
    public void testSpeed() throws Exception
    {
        int LOOPS = 50;
        int MS_TO_LOOP = 5;
        
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        Sign[] sign = new Sign[2];
        sign[0] = Sign.SECP256K1_ECDSA;
        sign[1] = Sign.SECP256K1_SCHNORR;
        long[] nanos = new long[sign.length];
        
        String helloWorld = "Hello World!!!";
        byte[] msg = SHA256.digest(helloWorld.getBytes());

        long untilNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(MS_TO_LOOP);
        int[] count = new int[sign.length];
        
        byte[] auxRand = new byte[32];
        secureRandom.nextBytes(auxRand);
        
        for(int loop=0;System.nanoTime()<untilNanos;loop++)
        {
            for(int i=0;i<sign.length;i++)
            {
                byte[] secKey = sign[i].genSecKey();
                byte[] signature = sign[i].sign(msg, secKey, auxRand);
                byte[] pubKey = sign[i].getPubKey(secKey);
                assertTrue(sign[i].verify(msg, pubKey, signature), "loop="+loop+" i="+i);
                long t0 = System.nanoTime();
                for(int j=0;j<LOOPS;j++)
                {
                    assertTrue(sign[i].verify(msg, pubKey, signature), "loop="+loop+" i="+i);
                    count[i]++;
                }
                long t1 = System.nanoTime();
                nanos[i] += t1-t0;
//                System.out.println(sign[i].getClass().getSimpleName());
            }
        }
        for(int i=0;i<sign.length;i++)
        {
            long millis = TimeUnit.NANOSECONDS.toMillis(nanos[i]);
            System.out.printf("%s %d sign+verify, %d ms, %.2f ms, %.2f/s \n",
                    sign[i].getClass().getSimpleName(), count[i],
                    millis, millis/(double)count[i], count[i]/(double)millis );
            
        }
    }
    
}
