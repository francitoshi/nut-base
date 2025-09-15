/*
 *  Benchmark.java
 *
 *  Copyright (C) 2025 francitoshi@gmail.com
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
package io.nut.base.crypto;

import static io.nut.base.crypto.Kripto.CHACHA20_IV_BITS;
import static io.nut.base.crypto.Kripto.CHACHA20_IV_BYTES;
import static io.nut.base.crypto.Kripto.GCM_IV_BYTES;
import static io.nut.base.crypto.Kripto.GCM_TAG_BITS;
import io.nut.base.crypto.Kripto.SecretKeyTransformation;
import static io.nut.base.crypto.Kripto.SecretKeyTransformation.AES_GCM_NoPadding;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 *
 * @author franci
 */
public class Benchmark
{
    private final Kripto kripto;

    public Benchmark(Kripto kripto)
    {
        this.kripto = kripto!=null ? kripto : Kripto.getInstance();
    }
    public Benchmark()
    {
        this(null);
    }
    
    public static class Result implements Comparable<Result>
    {
        public final SecretKeyTransformation skt;
        public final long count;
        public Result(SecretKeyTransformation skt, long count)
        {
            this.skt = skt;
            this.count = count;
        }
        @Override
        public int compareTo(Result other)
        {
            return Long.compare(this.count, other.count);
        }
    }
        
    public boolean isAESNI()
    {
        try
        {
            Result[] res = benchmark(1000, SecretKeyTransformation.AES_GCM_NoPadding, SecretKeyTransformation.ChaCha20_Poly1305);
            return res[0].skt==SecretKeyTransformation.AES_GCM_NoPadding;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
    public Result[] benchmark(int millis, SecretKeyTransformation... values)
    {
        try
        {
            long nanos = TimeUnit.MILLISECONDS.toNanos(millis);
            Result[] results = new Result[values.length];
            for(int i=0;i<values.length;i++)
            {
                long count = benchmark(values[i], 256, nanos);
                results[i] = new Result(values[i], count);
            }
            Arrays.sort(results, Comparator.reverseOrder());

            return results;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);//"ChaCha20-Poly1305"; // Default seguro
        }
    }

    private long benchmark(Kripto.SecretKeyTransformation skt, int keyBits, long nanos) throws Exception
    {
        KeyGenerator keyGen = this.kripto.getKeyGenerator(skt.algorithm, keyBits);
        SecretKey key = keyGen.generateKey();
        
        byte[] data = new byte[1024 * 1024]; // 1MiB
        
        SecureRandom random = new SecureRandom();
        random.nextBytes(data);
        byte[] iv = new byte[12];
        Cipher cipher;

        // Warmup
        for (int i = 0; i < 50; i++)
        {
            random.nextBytes(iv);
            cipher = getCipher(skt, key);
            cipher.doFinal(data);
        }
        
        // Benchmark
        int count = 0;
        long nanoTime = System.nanoTime() + nanos;
        for(int round=1;nanoTime>System.nanoTime();round++)
        {
            for (int i = 0; i < round; i++, count++)
            {
                random.nextBytes(iv);
            }
            cipher = getCipher(skt, key);
            cipher.doFinal(data);
        }
        return count;
    }

    private Cipher getCipher(Kripto.SecretKeyTransformation skt, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, AssertionError
    {
        Cipher cipher;
        switch (skt)
        {
            case AES_GCM_NoPadding:
                GCMParameterSpec ivGCM = this.kripto.getIvGCM(Kripto.getRand().nextBytes(new byte[GCM_IV_BYTES]), GCM_TAG_BITS);
                cipher = this.kripto.getCipher(key, skt, ivGCM, Cipher.ENCRYPT_MODE);
                break;
            case ChaCha20_Poly1305:
                IvParameterSpec iv = this.kripto.getIv(Kripto.getRand().nextBytes(new byte[CHACHA20_IV_BYTES]), CHACHA20_IV_BITS);
                cipher = this.kripto.getCipher(key, skt, iv, Cipher.ENCRYPT_MODE);
                break;
            default:
                throw new AssertionError();
        }
        return cipher;
    }
    
}
