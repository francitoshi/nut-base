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
import io.nut.base.encoding.Hex;
import io.nut.base.util.Utils;
import io.nut.base.util.concurrent.pipeline.Pipe;
import io.nut.base.util.concurrent.pipeline.PipeLine;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SchnorrTest
{

    public SchnorrTest()
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
    static final String[] DATA =
    {
        "F9308A019258C31049344F85F89D5229B531C845836F99B08601F113BCE036F9,0000000000000000000000000000000000000000000000000000000000000000,E907831F80848D1069A5371B402410364BDF1C5F8307B0084C55F1CE2DCA821525F66A4A85EA8B71E482A74F382D2CE5EBEEE8FDB2172F477DF4900D310536C0,TRUE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,6896BD60EEAE296DB48A229FF71DFE071BDE413E6D43F917DC8DCF8C78DE33418906D11AC976ABCCB20B091292BFF4EA897EFCB639EA871CFA95F6DE339E4B0A,TRUE",
        "DD308AFEC5777E13121FA72B9CC1B7CC0139715309B086C960E18FD969774EB8,7E2D58D8B3BCDF1ABADEC7829054F90DDA9805AAB56C77333024B9D0A508B75C,5831AAEED7B44BB74E5EAB94BA9D4294C49BCF2A60728D8B4C200F50DD313C1BAB745879A5AD954A72C45A91C3A51D3C7ADEA98D82F8481E0E1E03674A6F3FB7,TRUE",
        "25D1DFF95105F5253C4022F628A996AD3A0D95FBF21D468A1B33F8C160D8F517,FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF,7EB0509757E246F19449885651611CB965ECC1A187DD51B64FDA1EDC9637D5EC97582B9CB13DB3933705B32BA982AF5AF25FD78881EBB32771FC5922EFC66EA3,TRUE",
        "D69C3509BB99E412E68B0FE8544E72837DFA30746D8BE2AA65975F29D22DC7B9,4DF3C3F68FCC83B27E9D42C90431A72499F17875C81A599B566C9889B9696703,00000000000000000000003B78CE563F89A0ED9414F5AA28AD0D96D6795F9C6376AFB1548AF603B3EB45C9F8207DEE1060CB71C04E80F593060B07D28308D7F4,TRUE",
        "EEFDEA4CDB677750A420FEE807EACF21EB9898AE79B9768766E4FAA04A2D4A34,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,6CFF5C3BA86C69EA4B7376F31A9BCB4F74C1976089B2D9963DA2E5543E17776969E89B4C5564D00349106B8497785DD7D1D713A8AE82B32FA79D5F7FC407D39B,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,FFF97BD5755EEEA420453A14355235D382F6472F8568A18B2F057A14602975563CC27944640AC607CD107AE10923D9EF7A73C643E166BE5EBEAFA34B1AC553E2,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,1FA62E331EDBC21C394792D2AB1100A7B432B013DF3F6FF4F99FCB33E0E1515F28890B3EDB6E7189B630448B515CE4F8622A954CFE545735AAEA5134FCCDB2BD,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,6CFF5C3BA86C69EA4B7376F31A9BCB4F74C1976089B2D9963DA2E5543E177769961764B3AA9B2FFCB6EF947B6887A226E8D7C93E00C5ED0C1834FF0D0C2E6DA6,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,0000000000000000000000000000000000000000000000000000000000000000123DDA8328AF9C23A94C1FEECFD123BA4FB73476F0D594DCB65C6425BD186051,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,00000000000000000000000000000000000000000000000000000000000000017615FBAF5AE28864013C099742DEADB4DBA87F11AC6754F93780D5A1837CF197,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,4A298DACAE57395A15D0795DDBFD1DCB564DA82B0F269BC70A74F8220429BA1D69E89B4C5564D00349106B8497785DD7D1D713A8AE82B32FA79D5F7FC407D39B,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F69E89B4C5564D00349106B8497785DD7D1D713A8AE82B32FA79D5F7FC407D39B,FALSE",
        "DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,6CFF5C3BA86C69EA4B7376F31A9BCB4F74C1976089B2D9963DA2E5543E177769FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141,FALSE",
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC30,243F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C89,6CFF5C3BA86C69EA4B7376F31A9BCB4F74C1976089B2D9963DA2E5543E17776969E89B4C5564D00349106B8497785DD7D1D713A8AE82B32FA79D5F7FC407D39B,FALSE",
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC30,233F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C88,6CFF5C3BA86C69EA4B7376F31A9BCB4F74C1976089B2D9963DA2E5543E17776969E89B4C5564D00349106B8497785DD7D1D713A8AE82B32FA79D5F7FC407D39B,FALSE",
        "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC29,233F6A8885A308D313198A2E03707344A4093822299F31D0082EFA98EC4E6C88,6CFF5C3BA86C69EA4B7376F31A9BCB4F74C1976089B2D9963DA2E5543E17776969E89B4C5564D00349106B8497785DD7D1D713A8AE82B32FA79D5F7FC407D39B,FALSE"
    };

    /**
     * Test of sign method, of class Schnorr.
     */
    @Test
    public void testDATA() throws Exception
    {
        Schnorr schnorr = Sign.SECP256K1_SCHNORR;

        for (String item : DATA)
        {
            String[] data = item.split(",");
            byte[] pubkey = Hex.decode(data[0]);
            byte[] msg = Hex.decode(data[1]);
            byte[] sig = Hex.decode(data[2]);
            boolean expected = Boolean.parseBoolean(data[3]);
            boolean result;
            try
            {
                result = schnorr.verify(msg, pubkey, sig);
            }
            catch (InvalidKeyException ex)
            {
                result = false;
            }
            assertEquals(expected, result);
        }
    }

    static final int LOOPS = 1000;
    static final int MS_TO_LOOP = 2_000;
    
    /**
     * Test of sign method, of class Schnorr.
     */
    @Test
    public void testBulkTimed() throws Exception
    {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        Schnorr schnorr = Sign.SECP256K1_SCHNORR;
        
        String helloWorld = "Hello World!!!";
        byte[] msg = Digest.sha256(helloWorld.getBytes());
        long t0 = System.nanoTime();
        long t1 = 0;
        long ms = 0;
        int count = 0;
        for (int i = 0; i < LOOPS && ms < MS_TO_LOOP; i++, count++)
        {
            byte[] randomBytes = new byte[32];
            byte[] secKey = schnorr.genSecKey();
            byte[] pubKey = schnorr.getPubKey(secKey);
            secureRandom.nextBytes(randomBytes);
            
            byte[] signature = schnorr.sign(msg, secKey, randomBytes);

            boolean trust = schnorr.verify(msg, pubKey, signature);
            assertTrue(trust);

            signature[i % signature.length]++;
            boolean fake = schnorr.verify(msg, pubKey, signature);
            assertFalse(fake);

            t1 = System.nanoTime();
            ms = TimeUnit.NANOSECONDS.toMillis(t1 - t0);
        }
        System.out.printf("testBulkTimed %d sign+verify, %d ms, %.2f ms, %.2f/s \n", count, ms, ms / (double) count, count * 1000.0 / ms);
    }
    
    static class Data
    {
        final byte[] msg;
        final byte[] secKey;
        volatile byte[] pubKey;
        volatile byte[] randomBytes;
        volatile byte[] signature;
        volatile boolean trust;
        volatile boolean fail;

        public Data(byte[] msg, byte[] secKey)
        {
            this.msg = msg;
            this.secKey = secKey;
        }
    }
    static final Data POISON = new Data(null,null);
    static final Object lastLock = new Object();
    
    /**
     * Test of sign method, of class Schnorr.
     */
    @Test
    public void testBulkTimedPiped() throws Exception
    {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        Schnorr schnorr = Sign.SECP256K1_SCHNORR;

        final int ths = Runtime.getRuntime().availableProcessors()/4+1;

        Pipe<Data, Data> pipe = new PipeLine<Data, Data>(ths)//readable
        {
            @Override
            public Data filter(Data data)//min+focus
            {
                if(data!=POISON)
                {
                    try //min+focus
                    {
                        data.pubKey = schnorr.getPubKey(data.secKey);
                        data.randomBytes = new byte[32];
                        secureRandom.nextBytes(data.randomBytes);
                    }
                    catch (InvalidKeyException ex)
                    {
                        Logger.getLogger(SchnorrTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return data;
            }
        }.link(new PipeLine<Data, Data>(ths)//split
        {
            @Override
            public Data filter(Data data)
            {
                if(data!=POISON)
                {
                    try
                    {
                        data.signature = schnorr.sign(data.msg, data.pubKey, data.randomBytes);
                    }
                    catch (InvalidKeyException ex)
                    {
                        Logger.getLogger(SchnorrTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return data;
            }
        }.link(new PipeLine<Data, Data>(ths)//split
        {
            @Override
            public Data filter(Data data)
            {
                if(data!=POISON)
                {
                    try
                    {
                        data.trust = schnorr.verify(data.msg, data.pubKey, data.signature);
                    }
                    catch (InvalidKeyException ex)
                    {
                        Logger.getLogger(SchnorrTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return data;
            }
        }).link(new PipeLine<Data, Data>(ths)
        {
            @Override
            public Data filter(Data data)
            {
                if(data!=POISON)
                {
                    data.signature[0] = 1;
                    try
                    {
                        data.fail = !schnorr.verify(data.msg, data.pubKey, data.signature);
                    }
                    catch (InvalidKeyException ex)
                    {
                        Logger.getLogger(SchnorrTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else
                {
                    synchronized (lastLock)
                    {
                        lastLock.notifyAll();
                    }
                }
                return data;
            }
        }));

        String helloWorld = "Hello World!!!";
        byte[] msg = Digest.sha256(helloWorld.getBytes());
        long t0 = System.nanoTime();
        long t1 = 0;
        long ms = 0;
        int count = 0;
        for (int i = 0; i < LOOPS && ms < MS_TO_LOOP; i++, count++)
        {
            byte[] secKey = schnorr.genSecKey();
            pipe.put(new Data(msg, secKey));
            t1 = System.nanoTime();
            ms = TimeUnit.NANOSECONDS.toMillis(t1 - t0);
        }
        synchronized (lastLock)
        {
            pipe.put(POISON);
            lastLock.wait();
        }
        pipe.isAlive();
        t1 = System.nanoTime();
        ms = TimeUnit.NANOSECONDS.toMillis(t1 - t0);
        System.out.printf("testBulkTimedPiped %d sign+verify, %d ms, %.2f ms, %.2f/s \n", count, ms, ms / (double) count, count * 1000.0 / ms);
    }

    /**
     * Test of sign method, of class Schnorr.
     */
    @Test
    public void testBip340Examples() throws Exception
    {
        Schnorr schnorr = Sign.SECP256K1_SCHNORR;

        CSVFormat csvFmt = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setCommentMarker('#')
                .build();

        CSVParser parser = csvFmt.parse(new InputStreamReader(SchnorrTest.class.getResourceAsStream("schnoor-test-vectors.csv")));
        for (CSVRecord values : parser)
        {
            //index,secret_key,public_key,aux_rand,message,signature,verification_result,comment
            int index = Integer.parseInt(values.get("index"));

            byte[] secKey = Hex.decode(values.get("secret_key"));
            byte[] pubKey = Hex.decode(values.get("public_key"));
            byte[] randomBytes = Hex.decode(values.get("aux_rand"));
            byte[] msg = Hex.decode(values.get("message"));
            byte[] signature = Hex.decode(values.get("signature"));
            boolean verification_result = Boolean.parseBoolean(values.get("verification_result"));
            String comment = values.get("comment");

            if (secKey.length == 32 && msg.length == 32)
            {
                byte[] sig = schnorr.sign(msg, secKey, randomBytes);
                assertArrayEquals(signature, sig, index + " " + comment);
            }
            if (msg.length == 32)
            {
                boolean verified = schnorr.verify(msg, pubKey, signature);
                assertEquals(verification_result, verified, index + " " + comment);
            }
        }
    }
    /**
     * Test of sign method, of class ECDSA.
     * @throws java.lang.Exception
     */
    @Test
    public void testSignVerifyBytes() throws Exception
    {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        Schnorr instance = Sign.SECP256K1_SCHNORR;
        String helloWorld = "Hello World!!!";
        byte[] msg = Digest.sha256(helloWorld.getBytes());
        long t0;
        long t1;
        long ms = 0;
        int count=0;
        
        byte[] auxRand = new byte[32];
        secureRandom.nextBytes(auxRand);

        t0 = System.nanoTime();
        
        for(int i=0;i<LOOPS && ms<MS_TO_LOOP;i++,count++)
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
        Sign instance = Sign.SECP256K1_SCHNORR;
        
        String helloWorld = "Hello World!!!";
        BigInteger msg = Utils.asBigInteger(Digest.sha256(helloWorld.getBytes()));
        long t0;
        long t1;
        long ms = 0;
        int count=0;
        
        byte[] auxRand = new byte[32];
        secureRandom.nextBytes(auxRand);

        t0 = System.nanoTime();
        
        BigInteger k = Utils.asBigInteger(auxRand);
        
        for(int i=0;i<LOOPS && ms<MS_TO_LOOP;i++,count++)
        {
            BigInteger secKey = Utils.asBigInteger(instance.genSecKey());
            BigInteger[] signature = instance.sign(msg, secKey, k);
            Point pubKey = instance.getPubKey(secKey);
            
            boolean ok = instance.verify(msg, pubKey, signature[0], signature[1]);
            assertTrue(ok, "i="+i);
            
            t1 = System.nanoTime();
            ms = TimeUnit.NANOSECONDS.toMillis(t1-t0);
        }
        System.out.printf("%d sign+verify, %d ms, %.2f ms, %.2f/s \n",count,ms, ms/(double)count, count*1000.0/ms );
    }

    /**
     * Test of sign method, of class Schnorr.
     */
    @Test
    public void testReference() throws Exception
    {
        Schnorr schnorr = Sign.SECP256K1_SCHNORR;
        
        schnorr.setDebug(false);
        
        byte[] msg = Hex.decode("0000000000000000000000000000000000000000000000000000000000000000");
        byte[] seckey = Hex.decode("0000000000000000000000000000000000000000000000000000000000000003");
        byte[] aux_rand = Hex.decode("0000000000000000000000000000000000000000000000000000000000000000");
        
        BigInteger msgNum = Utils.asBigInteger(msg);
        BigInteger seckeyNum = Utils.asBigInteger(seckey);
        BigInteger aux_randNum = Utils.asBigInteger(aux_rand);
        
        BigInteger[] rs = schnorr.sign(msgNum, seckeyNum, aux_randNum);

        Point pubkey = schnorr.getPubKey(seckeyNum);

        boolean ok = schnorr.verify(msgNum, pubkey, rs[0], rs[1]);
        
        System.out.println("ok="+ok);
        assertTrue(ok,"ok");
    }

    /**
     * Test of sign method, of class Schnorr.
     */
    @Test
    public void testReference2() throws Exception
    {
        Schnorr schnorr = Sign.SECP256K1_SCHNORR;
        
        schnorr.setDebug(false);
        
        byte[] msg = Hex.decode("243f6a8885a308d313198a2e03707344a4093822299f31d0082efa98ec4e6c89");
        byte[] seckey = Hex.decode("b7e151628aed2a6abf7158809cf4f3c762e7160f38b4da56a784d9045190cfef");
        byte[] aux_rand = Hex.decode("0000000000000000000000000000000000000000000000000000000000000001");
        
        BigInteger msgNum = Utils.asBigInteger(msg);
        BigInteger seckeyNum = Utils.asBigInteger(seckey);
        BigInteger aux_randNum = Utils.asBigInteger(aux_rand);
        
        BigInteger[] rs = schnorr.sign(msgNum, seckeyNum, aux_randNum);

        Point pubkey = schnorr.getPubKey(seckeyNum);

        boolean ok = schnorr.verify(msgNum, pubkey, rs[0], rs[1]);
        
        System.out.println("ok="+ok);
        assertTrue(ok,"ok");

    }
    
    /**
     * Test of sign method, of class Schnorr.
     */
    @Test
    public void testReference3() throws Exception
    {
        Schnorr schnorr = Sign.SECP256K1_SCHNORR;
        
        schnorr.setDebug(false);
        
        byte[] msg = Hex.decode("7e2d58d8b3bcdf1abadec7829054f90dda9805aab56c77333024b9d0a508b75c");
        byte[] seckey = Hex.decode("c90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b14e5c9");
        byte[] aux_rand = Hex.decode("c87aa53824b4d7ae2eb035a2b5bbbccc080e76cdc6d1692c4b0b62d798e6d906");
        
        BigInteger msgNum = Utils.asBigInteger(msg);
        BigInteger seckeyNum = Utils.asBigInteger(seckey);
        BigInteger aux_randNum = Utils.asBigInteger(aux_rand);
        
        BigInteger[] rs = schnorr.sign(msgNum, seckeyNum, aux_randNum);

        Point pubkey = schnorr.getPubKey(seckeyNum);

        boolean ok = schnorr.verify(msgNum, pubkey, rs[0], rs[1]);
        
        System.out.println("ok="+ok);
        assertTrue(ok,"ok");

    }
}
