/*
 *  SchnorrTest.java
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

import io.nut.base.crypto.Digest;
import io.nut.base.crypto.Kripto.MessageDigestAlgorithm;
import io.nut.base.util.Utils;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author franci
 */
public class ECDSATest
{

    public ECDSATest()
    {
    }

    @BeforeAll
    public static void setUpClass()
    {
    }

    @AfterAll
    public static void tearDownClass()
    {
    }

    @BeforeEach
    public void setUp()
    {
    }

    @AfterEach
    public void tearDown()
    {
    }
    
    /**
     * Test of sign method, of class ECDSA.
     * @throws java.lang.Exception
     */
    @Test
    public void testSignVerify1() throws Exception
    {
        //https://learnmeabitcoin.com/technical/ecdsa
        ECDSA instance = Sign.SECP256K1_ECDSA;

        BigInteger msg = new BigInteger("75402077471587956851360588120356244127735644006942973877340910814730793844683", 10);
        BigInteger secKey = new BigInteger("112757557418114203588093402336452206775565751179231977388358956335153294300646", 10);
        BigInteger k = new BigInteger("123456789", 10);
        BigInteger r = new BigInteger("4051293998585674784991639592782214972820158391371785981004352359465450369227", 10);
        BigInteger s = new BigInteger("101656099268479774907861155236876278987061611115278341531512875302287938750185", 10);
        Point pubKey = instance.getPubKey(secKey);
        
        BigInteger[] signature1 = instance.sign(msg, secKey, k);

        assertTrue(signature1[0].compareTo(r)==0);
        assertTrue(signature1[1].compareTo(s)==0);
        
        byte[] msgBytes = instance.asBytes(msg);
        byte[] secKeyBytes = instance.asBytes(secKey);
        byte[] pubKeyBytes = instance.getPubKey(secKeyBytes);
        byte[] kBytes = instance.asBytes(k);
        
        byte[] signature2 = instance.sign(msgBytes, secKeyBytes, kBytes);
        
        assertTrue(instance.verify(msg, pubKey, signature1[0], signature1[1]));
        assertTrue(instance.verify(msgBytes, pubKeyBytes, signature2));
        
    }
 
    static final Digest SHA256 = new Digest(null, MessageDigestAlgorithm.SHA256);
    /**
     * Test of sign method, of class ECDSA.
     * @throws java.lang.Exception
     */
    @Test
    public void testSignVerifyBytes() throws Exception
    {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        Sign instance = Sign.SECP256K1_ECDSA;
        String helloWorld = "Hello World!!!";
        byte[] msg = SHA256.digest(helloWorld.getBytes());
        long t0;
        long t1;
        long ms = 0;
        int count=0;
        
        byte[] auxRand = new byte[256];
        secureRandom.nextBytes(auxRand);

        t0 = System.nanoTime();
        
        for(int i=0;i<1000 && ms<5_000;i++,count++)
        {
            byte[] secKey = instance.genSecKey();
            byte[] signature = instance.sign(msg, secKey, auxRand);
            byte[] pubKey = instance.getPubKey(secKey);

            assertTrue(instance.verify(msg, pubKey, signature), "i="+i);

            t1 = System.nanoTime();
            ms = TimeUnit.NANOSECONDS.toMillis(t1-t0);
        }
        System.out.printf("%d sign+verify, %d ms, %.2f ms, %.2f/s \n",count,ms, ms/(double)count, count*1000.0/ms );
    }
    /**
     * Test of sign method, of class ECDSA.
     */
    @Test
    public void testSignVerifyBigInteger() throws Exception
    {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        Sign instance = Sign.SECP256K1_ECDSA;
        String helloWorld = "Hello World!!!";
        BigInteger msg = Utils.asBigInteger(SHA256.digest(helloWorld.getBytes()));
        long t0;
        long t1;
        long ms = 0;
        int count=0;
        
        byte[] auxRand = new byte[32];
        secureRandom.nextBytes(auxRand);

        t0 = System.nanoTime();
        
        BigInteger k = Utils.asBigInteger(auxRand);
        
        for(int i=0;i<1000 && ms<5_000;i++,count++)
        {
            BigInteger secKey = Utils.asBigInteger(instance.genSecKey());
            BigInteger[] signature = instance.sign(msg, secKey, k);
            Point pubKey = instance.getPubKey(secKey);

            assertTrue(instance.verify(msg, pubKey, signature[0], signature[1]), "i="+i);
            
            t1 = System.nanoTime();
            ms = TimeUnit.NANOSECONDS.toMillis(t1-t0);
        }
        System.out.printf("%d sign+verify, %d ms, %.2f ms, %.2f/s \n",count,ms, ms/(double)count, count*1000.0/ms );
    }
 
    /**
     * Test of sign_message method, of class ECDSA.
     */
    @Test
    public void testSign_message() throws InvalidKeyException
    {
        BigInteger msg = new BigInteger("103318048148376957923607078689899464500752411597387986125144636642406244063093");
        BigInteger secKey = new BigInteger("112757557418114203588093402336452206775565751179231977388358956335153294300646");
        BigInteger k = new BigInteger("12345");
        ECDSA instance = new ECDSA(Secp256k1.INSTANCE);

        BigInteger[] rs = instance.sign(msg, secKey, k);

        Point pubKey = instance.getPubKey(secKey);
        
        boolean verified = instance.verify(msg, pubKey, rs[0], rs[1]);
        assertTrue(verified);

        assertEquals("108607064596551879580190606910245687803607295064141551927605737287325610911759", rs[0].toString(),"r");
        assertEquals("73791001770378044883749956175832052998232581925633570497458784569540878807131", rs[1].toString(),"s");
        
    }

}