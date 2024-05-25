package io.nut.base.encoding;

/*
    from Drongo library in SparrowWallet
*/

import java.nio.charset.StandardCharsets;

public class Base43
{

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$*+-./:".toCharArray();

    private static final int[] INDEXES = new int[128];

    static
    {
        for (int i = 0; i < INDEXES.length; i++)
        {
            INDEXES[i] = -1;
        }

        for (int i = 0; i < ALPHABET.length; i++)
        {
            INDEXES[ALPHABET[i]] = i;
        }
    }

    public static String encode(byte[] input)
    {
        if (input.length == 0)
        {
            return "";
        }

        input = copyOfRange(input, 0, input.length);

        // Count leading zeroes.
        int zeroCount = 0;
        while (zeroCount < input.length && input[zeroCount] == 0)
        {
            ++zeroCount;
        }

        // The actual encoding.
        final byte[] temp = new byte[input.length * 2];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input.length)
        {
            byte mod = divmod43(input, startAt);
            if (input[startAt] == 0)
            {
                ++startAt;
            }
            temp[--j] = (byte) ALPHABET[mod];
        }

        // Strip extra '1' if there are some after decoding.
        while (j < temp.length && temp[j] == ALPHABET[0])
        {
            ++j;
        }

        // Add as many leading '1' as there were leading zeros.
        while (--zeroCount >= 0)
        {
            temp[--j] = (byte) ALPHABET[0];
        }

        final byte[] output = copyOfRange(temp, j, temp.length);

        return new String(output, StandardCharsets.US_ASCII);
    }

    public static byte[] decode(final String input) throws IllegalArgumentException
    {
        if (input.isEmpty())
        {
            return new byte[0];
        }

        final byte[] input43 = new byte[input.length()];
        // Transform the String to a base43 byte sequence
        for (int i = 0; i < input.length(); ++i)
        {
            final char c = input.charAt(i);

            int digit43 = -1;

            if (c >= 0 && c < 128)
            {
                digit43 = INDEXES[c];
            }

            if (digit43 < 0)
            {
                throw new IllegalArgumentException("Illegal character " + c + " at " + i);
            }

            input43[i] = (byte) digit43;
        }

        // Count leading zeroes
        int zeroCount = 0;
        while (zeroCount < input43.length && input43[zeroCount] == 0)
        {
            ++zeroCount;
        }

        // The encoding
        final byte[] temp = new byte[input.length()];
        int j = temp.length;

        int startAt = zeroCount;
        while (startAt < input43.length)
        {
            byte mod = divmod256(input43, startAt);
            if (input43[startAt] == 0)
            {
                ++startAt;
            }

            temp[--j] = mod;
        }

        // Do no add extra leading zeroes, move j to first non null byte.
        while (j < temp.length && temp[j] == 0)
        {
            ++j;
        }

        return copyOfRange(temp, j - zeroCount, temp.length);
    }

    //
    // number -> number / 43, returns number % 43
    //
    private static byte divmod43(final byte[] number, final int startAt)
    {
        int remainder = 0;
        for (int i = startAt; i < number.length; i++)
        {
            final int digit256 = (int) number[i] & 0xFF;
            final int temp = remainder * 256 + digit256;

            number[i] = (byte) (temp / 43);

            remainder = temp % 43;
        }

        return (byte) remainder;
    }

    //
    // number -> number / 256, returns number % 256
    //
    private static byte divmod256(final byte[] number43, final int startAt)
    {
        int remainder = 0;
        for (int i = startAt; i < number43.length; i++)
        {
            final int digit58 = (int) number43[i] & 0xFF;
            final int temp = remainder * 43 + digit58;

            number43[i] = (byte) (temp / 256);

            remainder = temp % 256;
        }

        return (byte) remainder;
    }

    private static byte[] copyOfRange(final byte[] source, final int from, final int to)
    {
        final byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);

        return range;
    }
}
