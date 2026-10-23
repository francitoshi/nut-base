package io.nut.base.commons.jca;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-XTS compatible with dm-crypt / LUKS {@code aes-xts-plain64} using a
 * 512-bit key (AES-256).
 *
 * <p>
 * The 64-byte key is split in half exactly as dm-crypt does: the first 32 bytes
 * encrypt the data (K1) and the last 32 bytes encrypt the tweak (K2).</p>
 *
 * <p>
 * The data unit size must be a power of two between 512 bytes and 16 MiB (2^20
 * AES blocks, the IEEE 1619 limit). Otherwise the constructor throws
 * {@link IllegalArgumentException}.</p>
 *
 * <p>
 * The IV is the data unit number as a 64-bit little-endian integer, padded
 * with zeros to 16 bytes (plain64). What the number means depends on how the
 * volume was created:</p>
 * <ul>
 * <li>LUKS1, or LUKS2 with 512-byte sectors: number of the 512-byte sector.
 * With a larger {@code dataUnitSize} without large-sector IVs, pass
 * {@code unitIndex * (dataUnitSize / 512)}.</li>
 * <li>LUKS2 with {@code sector_size} &gt; 512 (dm-crypt
 * {@code iv_large_sectors}): index of the data unit, used as is.</li>
 * </ul>
 * <p>
 * Numbering starts at the first sector of the data area (after the LUKS
 * header), not at the start of the device or file. The number is treated as
 * unsigned 64-bit.</p>
 *
 * <p>
 * Thread-safe: each thread keeps its own initialized ciphers and buffers.
 * Provides confidentiality only, not authentication.</p>
 */
public final class AesXtsPlain64
{

    private static final int AES_BLOCK_SIZE = 16;

    /** Required key length in bytes (512 bits = 2 x AES-256). */
    public static final int KEY_SIZE = 64;

    /** Minimum data unit size in bytes. */
    public static final int MIN_DATA_UNIT_SIZE = 512;

    /** Maximum data unit size in bytes (2^20 AES blocks). */
    public static final int MAX_DATA_UNIT_SIZE = (1 << 20) * AES_BLOCK_SIZE;

    private static final String AES_ECB = "AES/ECB/NoPadding";

    private final int dataUnitSize;
    private final SecretKeySpec key1;
    private final SecretKeySpec key2;
    private final ThreadLocal<Context> contexts = new ThreadLocal<>();

    /**
     * Per-thread state: initialized ciphers and work buffers.
     */
    private static final class Context
    {

        final Cipher encrypt;
        final Cipher decrypt;
        final Cipher tweakCipher;
        final byte[] iv = new byte[AES_BLOCK_SIZE];
        final byte[] tweaks;

        Context(AesXtsPlain64 owner) throws GeneralSecurityException
        {
            encrypt = Cipher.getInstance(AES_ECB);
            encrypt.init(Cipher.ENCRYPT_MODE, owner.key1);
            decrypt = Cipher.getInstance(AES_ECB);
            decrypt.init(Cipher.DECRYPT_MODE, owner.key1);
            tweakCipher = Cipher.getInstance(AES_ECB);
            tweakCipher.init(Cipher.ENCRYPT_MODE, owner.key2);
            tweaks = new byte[owner.dataUnitSize];
        }
    }

    /**
     * Creates an AES-XTS cipher (aes-xts-plain64, 512-bit key).
     *
     * @param dataUnitSize size of each data unit in bytes; power of two,
     * between {@link #MIN_DATA_UNIT_SIZE} and {@link #MAX_DATA_UNIT_SIZE}
     * @param key 64-byte key; first half is K1 (data), second half is K2
     * (tweak). It is copied; the caller may wipe its array afterwards.
     * @throws IllegalArgumentException if the size or the key are invalid
     * @throws GeneralSecurityException if AES-256 is not available
     */
    public AesXtsPlain64(int dataUnitSize, byte[] key) throws GeneralSecurityException
    {
        if (dataUnitSize < MIN_DATA_UNIT_SIZE
                || dataUnitSize > MAX_DATA_UNIT_SIZE
                || (dataUnitSize & (dataUnitSize - 1)) != 0)
        {
            throw new IllegalArgumentException(
                    "Data unit size must be a power of two between "
                    + MIN_DATA_UNIT_SIZE + " and " + MAX_DATA_UNIT_SIZE
                    + ": " + dataUnitSize);
        }

        if (key == null || key.length != KEY_SIZE)
        {
            throw new IllegalArgumentException(
                    "Key must be exactly " + KEY_SIZE + " bytes");
        }

        final int half = KEY_SIZE / 2;
        byte[] k1 = Arrays.copyOfRange(key, 0, half);
        byte[] k2 = Arrays.copyOfRange(key, half, KEY_SIZE);

        try
        {
            // IEEE 1619-2018 requires K1 != K2.
            if (MessageDigest.isEqual(k1, k2))
            {
                throw new IllegalArgumentException("Both halves of the key must be different");
            }
            this.key1 = new SecretKeySpec(k1, "AES");
            this.key2 = new SecretKeySpec(k2, "AES");
        }
        finally
        {
            Arrays.fill(k1, (byte) 0);
            Arrays.fill(k2, (byte) 0);
        }

        this.dataUnitSize = dataUnitSize;

        // Fail early if AES-256 is not supported by the runtime.
        contexts.set(new Context(this));
    }

    /**
     * Encrypts one complete data unit.
     *
     * @param plaintext source, exactly {@code dataUnitSize} bytes
     * @param ciphertext destination, exactly {@code dataUnitSize} bytes (may be
     * the same array as the source)
     * @param unit data unit number (IV), see class description
     * @throws IllegalArgumentException if the arguments are invalid
     * @throws GeneralSecurityException if the cryptographic operation fails
     */
    public void encrypt(byte[] plaintext, byte[] ciphertext, long unit) throws GeneralSecurityException
    {
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        requireExact(plaintext, "plaintext");
        requireExact(ciphertext, "ciphertext");
        transform(true, plaintext, 0, ciphertext, 0, unit);
    }

    /**
     * Decrypts one complete data unit.
     *
     * @param ciphertext source, exactly {@code dataUnitSize} bytes
     * @param plaintext destination, exactly {@code dataUnitSize} bytes (may be
     * the same array as the source)
     * @param unit data unit number (IV), see class description
     * @throws IllegalArgumentException if the arguments are invalid
     * @throws GeneralSecurityException if the cryptographic operation fails
     */
    public void decrypt(byte[] ciphertext, byte[] plaintext, long unit) throws GeneralSecurityException
    {
        Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        Objects.requireNonNull(plaintext, "plaintext cannot be null");
        requireExact(ciphertext, "ciphertext");
        requireExact(plaintext, "plaintext");
        transform(false, ciphertext, 0, plaintext, 0, unit);
    }

    /**
     * Encrypts one data unit located inside larger arrays (for example an I/O
     * buffer holding several units).
     *
     * <p>
     * Source and destination may be the same region, but must not partially
     * overlap.</p>
     *
     * @param src source array
     * @param srcOff offset of the unit in {@code src}
     * @param dst destination array
     * @param dstOff offset of the unit in {@code dst}
     * @param unit data unit number (IV), see class description
     * @throws IllegalArgumentException if the arguments are invalid
     * @throws GeneralSecurityException if the cryptographic operation fails
     */
    public void encrypt(byte[] src, int srcOff, byte[] dst, int dstOff, long unit) throws GeneralSecurityException
    {
        transform(true, src, srcOff, dst, dstOff, unit);
    }

    /**
     * Decrypts one data unit located inside larger arrays.
     *
     * <p>
     * Source and destination may be the same region, but must not partially
     * overlap.</p>
     *
     * @param src source array (ciphertext)
     * @param srcOff offset of the unit in {@code src}
     * @param dst destination array (plaintext)
     * @param dstOff offset of the unit in {@code dst}
     * @param unit data unit number (IV), see class description
     * @throws IllegalArgumentException if the arguments are invalid
     * @throws GeneralSecurityException if the cryptographic operation fails
     */
    public void decrypt(byte[] src, int srcOff, byte[] dst, int dstOff, long unit) throws GeneralSecurityException
    {
        transform(false, src, srcOff, dst, dstOff, unit);
    }

    /**
     * Returns the configured data unit size in bytes.
     *
     * @return data unit size
     */
    public int getDataUnitSize()
    {
        return dataUnitSize;
    }

    private void requireExact(byte[] array, String name)
    {
        if (array.length != dataUnitSize)
        {
            throw new IllegalArgumentException(name + " must be exactly " + dataUnitSize + " bytes");
        }
    }

    private Context context() throws GeneralSecurityException
    {
        Context c = contexts.get();
        if (c == null)
        {
            c = new Context(this);
            contexts.set(c);
        }
        return c;
    }

    private void transform(boolean encrypt, byte[] in, int inOff, byte[] out, int outOff, long unit) throws GeneralSecurityException
    {
        Objects.requireNonNull(in, "input cannot be null");
        Objects.requireNonNull(out, "output cannot be null");

        final int n = dataUnitSize;

        if (inOff < 0 || inOff > in.length - n)
        {
            throw new IllegalArgumentException("input range out of bounds: need " + n + " bytes at " + inOff);
        }
        if (outOff < 0 || outOff > out.length - n)
        {
            throw new IllegalArgumentException("output range out of bounds: need " + n + " bytes at " + outOff);
        }
        if (in == out && inOff != outOff && Math.abs((long) inOff - outOff) < n)
        {
            throw new IllegalArgumentException("input and output regions partially overlap");
        }

        final Context c = context();
        final byte[] t = c.tweaks;

        computeTweaks(c, unit);

        // X = P xor T (all blocks at once, directly in the output region)
        for (int i = 0; i < n; i++)
        {
            out[outOff + i] = (byte) (in[inOff + i] ^ t[i]);
        }

        // Y = AES_K1(X): a single ECB call for the whole unit
        (encrypt ? c.encrypt : c.decrypt).doFinal(out, outOff, n, out, outOff);

        // C = Y xor T
        for (int i = 0; i < n; i++)
        {
            out[outOff + i] ^= t[i];
        }
    }

    /**
     * Fills {@code c.tweaks} with T_j = AES_K2(IV) * alpha^j for every block of
     * the unit.
     */
    private static void computeTweaks(Context c, long unit) throws GeneralSecurityException
    {
        final byte[] iv = c.iv;
        final byte[] t = c.tweaks;

        // plain64: unit number little-endian, remaining 8 bytes stay zero
        for (int i = 0; i < 8; i++)
        {
            iv[i] = (byte) (unit >>> (8 * i));
        }

        c.tweakCipher.doFinal(iv, 0, AES_BLOCK_SIZE, t, 0);

        long lo = readLE(t, 0);
        long hi = readLE(t, 8);

        for (int off = AES_BLOCK_SIZE; off < t.length; off += AES_BLOCK_SIZE)
        {
            // multiply by alpha in GF(2^128), polynomial x^128 + x^7 + x^2 + x + 1
            final long msb = hi >>> 63;
            hi = (hi << 1) | (lo >>> 63);
            lo = (lo << 1) ^ (msb * 0x87L);

            writeLE(t, off, lo);
            writeLE(t, off + 8, hi);
        }
    }

    private static long readLE(byte[] b, int off)
    {
        return (b[off] & 0xFFL)
                | (b[off + 1] & 0xFFL) << 8
                | (b[off + 2] & 0xFFL) << 16
                | (b[off + 3] & 0xFFL) << 24
                | (b[off + 4] & 0xFFL) << 32
                | (b[off + 5] & 0xFFL) << 40
                | (b[off + 6] & 0xFFL) << 48
                | (b[off + 7] & 0xFFL) << 56;
    }

    private static void writeLE(byte[] b, int off, long v)
    {
        b[off] = (byte) v;
        b[off + 1] = (byte) (v >>> 8);
        b[off + 2] = (byte) (v >>> 16);
        b[off + 3] = (byte) (v >>> 24);
        b[off + 4] = (byte) (v >>> 32);
        b[off + 5] = (byte) (v >>> 40);
        b[off + 6] = (byte) (v >>> 48);
        b[off + 7] = (byte) (v >>> 56);
    }
}