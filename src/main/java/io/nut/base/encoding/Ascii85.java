/*
 *  Ascii85.java
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
package io.nut.base.encoding;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
* Implements Ascii85 (also known as Base85) encoding and decoding without using 
* the String class to prevent sensitive data from remaining in memory.
* <p>
* This class follows the principle of robustness
*
*/
public class Ascii85
{

    private static final int ASCII_OFFSET = 33; // '!'
    private static final char Z_COMPRESSION_CHAR = 'z';

    // Pre-calculated powers of 85 to improve performance
    private static final long P1 = 85L;
    private static final long P2 = 85L * 85L;
    private static final long P3 = 85L * 85L * 85L;
    private static final long P4 = 85L * 85L * 85L * 85L;

    /**
    * Encodes a byte array into a character array using the Ascii85 format.
    *
    * @param data The byte array to encode.
    * @return A character array with the Ascii85 representation, or null if
    * input is null.
    */
    public static char[] encode(byte[] data)
    {
        if (data == null)
        {
            return null;
        }
        if (data.length == 0)
        {
            return new char[0];
        }

        // Calculate the maximum possible size for the output buffer.
        // Every 4 bytes are converted into 5 chars.
        int maxLen = (data.length + 3) / 4 * 5;
        char[] result = new char[maxLen];
        int resultIndex = 0;

        int i = 0;
        // Processes complete blocks of 4 bytes
        while (i + 3 < data.length)
        {
            // Combines 4 bytes into a 32-bit unsigned integer.
            // 'long' and the 0xFF mask are used to avoid problems with sign extension.
            long chunk = ((data[i] & 0xFFL) << 24)
                    | ((data[i + 1] & 0xFFL) << 16)
                    | ((data[i + 2] & 0xFFL) << 8)
                    | (data[i + 3] & 0xFFL);

            // Special compression case: 4 null bytes are encoded as 'z'
            if (chunk == 0)
            {
                result[resultIndex++] = Z_COMPRESSION_CHAR;
            }
            else
            {
                // Converts the 32-bit chunk to 5 digits in base85
                char[] blockChars = new char[5];
                for (int j = 4; j >= 0; j--)
                {
                    blockChars[j] = (char) ((chunk % 85) + ASCII_OFFSET);
                    chunk /= 85;
                }
                for (char c : blockChars)
                {
                    result[resultIndex++] = c;
                }
            }
            i += 4;
        }

        // Handles the last block if it is not 4 bytes (padding)
        int remainingBytes = data.length - i;
        if (remainingBytes > 0)
        {
            byte[] paddedBlock = new byte[4];
            System.arraycopy(data, i, paddedBlock, 0, remainingBytes);

            long chunk = ((paddedBlock[0] & 0xFFL) << 24)
                    | ((paddedBlock[1] & 0xFFL) << 16)
                    | ((paddedBlock[2] & 0xFFL) << 8)
                    | (paddedBlock[3] & 0xFFL);

            char[] blockChars = new char[5];
            for (int j = 4; j >= 0; j--)
            {
                blockChars[j] = (char) ((chunk % 85) + ASCII_OFFSET);
                chunk /= 85;
            }

            // Only the necessary characters are added according to the padding
            // 1 input byte -> 2 output chars
            // 2 input bytes -> 3 output chars
            // 3 input bytes -> 4 output chars
            for (int j = 0; j < remainingBytes + 1; j++)
            {
                result[resultIndex++] = blockChars[j];
            }
        }
        
        // An array of the exact size is returned, removing any excess buffer space.
        // for security purposes result is cleaned to prevent passphrases in memory
        if(result.length!=resultIndex)
        {
            char[] ret = Arrays.copyOf(result, resultIndex);
            Arrays.fill(result, '\0');
            result = ret;
        }
        return result;
    }

    /**
    * Decodes an array of ASCII85 characters to a byte array. Ignores any
    * whitespace characters in the input.
    *
    * @param encodedData The array of ASCII85 characters to decode.
    * @return A byte array containing the decoded data, or null if the input is null.
    * @throws IllegalArgumentException if the encoded data is malformed.
    */
    public static byte[] decode(char[] encodedData)
    {
        if (encodedData == null)
        {
            return null;
        }
        if (encodedData.length == 0)
        {
            return new byte[0];
        }

        // Filter out whitespace to be lenient with input.
        char[] cleanData = filterWhitespace(encodedData);
        if (cleanData.length == 0)
        {
            return new byte[0];
        }

        // We use ByteArrayOutputStream to build the output byte array
        // dynamically, since its final size is not trivial to calculate.
        ByteArrayOutputStream decodedBytes = new ByteArrayOutputStream();

        int i = 0;
        while (i < cleanData.length)
        {
            char currentChar = cleanData[i];

            // Manejo del caso especial de compresión 'z'
            if (currentChar == Z_COMPRESSION_CHAR)
            {
                decodedBytes.write(0);
                decodedBytes.write(0);
                decodedBytes.write(0);
                decodedBytes.write(0);
                i++;
                continue;
            }

            // Determines if we are in the last block (which may have padding)
            int remainingChars = cleanData.length - i;
            if (remainingChars < 2)
            {
                throw new IllegalArgumentException("Invalid final Ascii85 block. Length:" + remainingChars);
            }

            boolean isPaddedBlock = remainingChars < 5;
            int blockSize = isPaddedBlock ? remainingChars : 5;

            long value = 0;
            char[] paddedBlock = { 'u', 'u', 'u', 'u', 'u' }; // It is filled with the character of highest value

            for (int j = 0; j < blockSize; j++)
            {
                char c = cleanData[i + j];
                if (c < '!' || c > 'u')
                {
                    throw new IllegalArgumentException("Invalid character for Ascii85: '" + c + "'");
                }
                paddedBlock[j] = c;
            }

            // Converts the 5 characters (real or padded) to their numeric value
            value += (paddedBlock[0] - ASCII_OFFSET) * P4;
            value += (paddedBlock[1] - ASCII_OFFSET) * P3;
            value += (paddedBlock[2] - ASCII_OFFSET) * P2;
            value += (paddedBlock[3] - ASCII_OFFSET) * P1;
            value += (paddedBlock[4] - ASCII_OFFSET);

            // Extracts the bytes from the numeric value
            byte[] chunkBytes = new byte[4];
            chunkBytes[0] = (byte) (value >> 24);
            chunkBytes[1] = (byte) (value >> 16);
            chunkBytes[2] = (byte) (value >> 8);
            chunkBytes[3] = (byte) (value);

            // Writes the corresponding bytes to the output stream
            int bytesToWrite = isPaddedBlock ? blockSize - 1 : 4;
            decodedBytes.write(chunkBytes, 0, bytesToWrite);

            i += blockSize;
        }

        return decodedBytes.toByteArray();
    }

    /**
     * Helper method to remove whitespace characters from a char array.
     */
    private static char[] filterWhitespace(char[] input)
    {
        // First we count how many are not spaces to know the final size.
        int nonWhitespaceCount = 0;
        for (char c : input)
        {
            if (c > ' ')
            { // Quick way to check that it is not a space, tab, \n, \r, etc.
                nonWhitespaceCount++;
            }
        }

        if (nonWhitespaceCount == input.length)
        {
            return input; // There are no spaces, we return the original
        }

        char[] result = new char[nonWhitespaceCount];
        int index = 0;
        for (char c : input)
        {
            if (c > ' ')
            {
                result[index++] = c;
            }
        }
        return result;
    }
}
