/*
 *  ROT.java
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
package io.nut.base.crypto.human;

/**
 * Utility class that provides methods for encrypting text using various
 * rotation (ROT) ciphers. The logic is centralized in a single 'cipher' method
 * which uses the 'CipherType' enumeration to determine the operation.
 */
public class ROT
{

    /**
     * Enumeration defining the supported ROT cipher types.
     */
    public enum Type
    {
        ROT5,
        ROT13,
        ROT18,
        ROT47
    }

    // --- PUBLIC SHORTCUT METHODS ---
    /**
     * Encrypts a character array using the ROT5 cipher.
     *
     * @param plainText The character array to be encrypted.
     * @return A new character array with the encrypted text.
     */
    public static char[] rot5(char[] plainText)
    {
        return cipher(plainText, Type.ROT5);
    }

    /**
     * Encrypts a character array using the ROT13 cipher.
     *
     * @param plainText The character array to be encrypted.
     * @return A new character array with the encrypted text.
     */
    public static char[] rot13(char[] plainText)
    {
        return cipher(plainText, Type.ROT13);
    }

    /**
     * Encrypts a character array using the ROT18 cipher.
     *
     * @param plainText The character array to be encrypted.
     * @return A new character array with the encrypted text.
     */
    public static char[] rot18(char[] plainText)
    {
        return cipher(plainText, Type.ROT18);
    }

    /**
     * Encrypts a character array using the ROT47 cipher.
     *
     * @param plainText The character array to be encrypted.
     * @return A new character array with the encrypted text.
     */
    public static char[] rot47(char[] plainText)
    {
        return cipher(plainText, Type.ROT47);
    }

    // --- CENTRAL CIPHER METHOD ---
    /**
     * Main method that performs the encryption operation based on the specified
     * type.
     *
     * @param plainText The character array to be encrypted.
     * @param type The type of cipher to apply (from the Type enum).
     * @return A new character array with the encrypted text.
     */
    public static char[] cipher(char[] plainText, Type type)
    {
        if (plainText == null || type == null)
        {
            return plainText;
        }

        char[] cipherText = new char[plainText.length];
        for (int i = 0; i < plainText.length; i++)
        {
            cipherText[i] = cipher(plainText[i], type);
        }
        return cipherText;
    }

    /**
     * Encrypts a String using the ROT5 cipher.
     *
     * @param plainText The String to be encrypted.
     * @return A new String with the encrypted text.
     */
    public static String rot5(String plainText)
    {
        return cipher(plainText, Type.ROT5);
    }

    /**
     * Encrypts a String using the ROT13 cipher.
     *
     * @param plainText The String to be encrypted.
     * @return A new String with the encrypted text.
     */
    public static String rot13(String plainText)
    {
        return cipher(plainText, Type.ROT13);
    }

    /**
     * Encrypts a String using the ROT18 cipher.
     *
     * @param plainText The String to be encrypted.
     * @return A new String with the encrypted text.
     */
    public static String rot18(String plainText)
    {
        return cipher(plainText, Type.ROT18);
    }

    /**
     * Encrypts a String using the ROT47 cipher.
     *
     * @param plainText The String to be encrypted.
     * @return A new String with the encrypted text.
     */
    public static String rot47(String plainText)
    {
        return cipher(plainText, Type.ROT47);
    }

    // --- CENTRAL CIPHER METHOD ---
    /**
     * Main method that performs the encryption operation based on the specified
     * type.
     *
     * @param plainText The String to be encrypted.
     * @param type The type of cipher to apply (from the Type enum).
     * @return A new String with the encrypted text.
     */
    public static String cipher(String plainText, Type type)
    {
        if(plainText==null || plainText.isEmpty())
        {
            return plainText;
        }
        return new String(cipher(plainText.toCharArray(), type));
    }

    // --- PRIVATE HELPER METHOD ---
    /**
     * Transforms a single character according to the specified cipher type.
     *
     * @param c The character to be transformed.
     * @param type The type of cipher to apply.
     * @return The transformed character, or the original if no rule applies.
     */
    private static char cipher(char c, Type type)
    {
        switch (type)
        {
            case ROT5:
                if (c >= '0' && c <= '9')
                {
                    return (char) (((c - '0' + 5) % 10) + '0');
                }
                break;

            case ROT13:
                if (c >= 'a' && c <= 'z')
                {
                    return (char) (((c - 'a' + 13) % 26) + 'a');
                }
                else if (c >= 'A' && c <= 'Z')
                {
                    return (char) (((c - 'A' + 13) % 26) + 'A');
                }
                break;

            case ROT18:
                if (c >= 'a' && c <= 'z')
                {
                    return (char) (((c - 'a' + 13) % 26) + 'a');
                }
                else if (c >= 'A' && c <= 'Z')
                {
                    return (char) (((c - 'A' + 13) % 26) + 'A');
                }
                else if (c >= '0' && c <= '9')
                {
                    return (char) (((c - '0' + 5) % 10) + '0');
                }
                break;

            case ROT47:
                if (c >= '!' && c <= '~')
                {
                    return (char) (((c - '!' + 47) % 94) + '!');
                }
                break;
        }
        // If no rule applies, return the original character
        return c;
    }

}
