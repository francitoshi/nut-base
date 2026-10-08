/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.crypto;

import io.nut.base.jca.Kr;
import static io.nut.base.jca.Kr.CHACHA20_IV_BITS;
import static io.nut.base.jca.Kr.CHACHA20_IV_BYTES;
import static io.nut.base.jca.Kr.GCM_IV_BYTES;
import static io.nut.base.jca.Kr.GCM_TAG_BITS;
import io.nut.base.jca.Kr.SecretKeyTransformation;
import static io.nut.base.jca.Kr.SecretKeyTransformation.AES_GCM_NoPadding;
import static io.nut.base.jca.Kr.SecretKeyTransformation.ChaCha20_Poly1305;
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

    private long benchmark(Kr.SecretKeyTransformation skt, int keyBits, long nanos) throws Exception
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

    private Cipher getCipher(Kr.SecretKeyTransformation skt, SecretKey key) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, AssertionError
    {
        Cipher cipher;
        switch (skt)
        {
            case AES_GCM_NoPadding:
                GCMParameterSpec ivGCM = this.kripto.getIvGCM(Rand.getInstance().nextBytes(new byte[GCM_IV_BYTES]), GCM_TAG_BITS);
                cipher = this.kripto.getCipher(key, skt, ivGCM, Cipher.ENCRYPT_MODE);
                break;
            case ChaCha20_Poly1305:
                IvParameterSpec iv = this.kripto.getIv(Rand.getInstance().nextBytes(new byte[CHACHA20_IV_BYTES]), CHACHA20_IV_BITS);
                cipher = this.kripto.getCipher(key, skt, iv, Cipher.ENCRYPT_MODE);
                break;
            default:
                throw new AssertionError();
        }
        return cipher;
    }
    
}
