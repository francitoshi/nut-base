/*
 *  PassphraseBuilder.java
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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class for generating random passphrases with specified character
 * requirements. Passphrases are generated as {@code char[]} arrays to allow
 * secure handling and cleanup. The class supports customizable character sets
 * and ensures minimum counts of uppercase letters, lowercase letters, and
 * special characters. It also provides safe character sets excluding visually
 * ambiguous characters.
 */
public class PassphraseBuilder
{

    /**
     * The {@link SecureRandom} instance used for random character selection.
     */
    private static final SecureRandom random = Kripto.getSecureRandomStrong();

    /**
     * Regular expression matching visually ambiguous characters (I, O, l, 0,
     * 1).
     */
    public static final String AMBIGUOUS_REGEX = "[IOl01]";

    /**
     * Default character set including all allowed characters (uppercase,
     * lowercase, digits, special).
     */
    public static final String ALL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    /**
     * Character set containing only uppercase letters.
     */
    public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * Character set containing only lowercase letters.
     */
    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Character set containing only digits.
     */
    public static final String NUMBERS = "0123456789";

    /**
     * Character set containing only special characters.
     */
    public static final String SPECIAL = "!@#$%^&*";

    /**
     * Safe character set excluding ambiguous characters from {@link #ALL}.
     */
    public static final String SAFE_ALL = ALL.replaceAll(AMBIGUOUS_REGEX, "");

    /**
     * Safe character set excluding ambiguous characters from
     * {@link #UPPERCASE}.
     */
    public static final String SAFE_UPPERCASE = UPPERCASE.replaceAll(AMBIGUOUS_REGEX, "");

    /**
     * Safe character set excluding ambiguous characters from
     * {@link #LOWERCASE}.
     */
    public static final String SAFE_LOWERCASE = LOWERCASE.replaceAll(AMBIGUOUS_REGEX, "");

    /**
     * Safe character set excluding ambiguous characters from {@link #NUMBERS}.
     */
    public static final String SAFE_NUMBERS = NUMBERS.replaceAll(AMBIGUOUS_REGEX, "");

    /**
     * Safe character set excluding ambiguous characters from {@link #SPECIAL}.
     */
    public static final String SAFE_SPECIAL = SPECIAL.replaceAll(AMBIGUOUS_REGEX, "");

    /**
     * Minimum number of uppercase letters required in the passphrase.
     */
    private final int uppercase;

    /**
     * Minimum number of lowercase letters required in the passphrase.
     */
    private final int lowercase;

    /**
     * Minimum number of digits required in the passphrase.
     */
    private final int number;

    /**
     * Minimum number of special characters required in the passphrase.
     */
    private final int special;

    /**
     * Array of all allowed characters from the provided charsets.
     */
    private final char[] allChars;

    /**
     * Array of uppercase characters available for passphrase generation.
     */
    private final char[] uppercaseChars;

    /**
     * Array of lowercase characters available for passphrase generation.
     */
    private final char[] lowercaseChars;

    /**
     * Array of digit characters available for passphrase generation.
     */
    private final char[] numberChars;

    /**
     * Array of special characters available for passphrase generation.
     */
    private final char[] specialChars;

    /**
     * Constructs a new {@code Passphrase} instance with specified minimum
     * character requirements and optional custom character sets.
     *
     * @param uppercase Minimum number of uppercase letters required.
     * @param lowercase Minimum number of lowercase letters required.
     * @param number Minimum number of digits required.
     * @param special Minimum number of special characters required.
     * @param charsets Optional custom character sets to use; defaults to
     * {@link #ALL} if none provided.
     * @throws IllegalArgumentException if any parameter is negative, no
     * charsets are provided, or required character types are not available in
     * the charsets.
     */
    public PassphraseBuilder(int uppercase, int lowercase, int number, int special, String... charsets)
    {
        if (uppercase < 0 || lowercase < 0 || number < 0 || special < 0)
        {
            throw new IllegalArgumentException("Parameters cannot be negative");
        }
        if (charsets == null || charsets.length == 0)
        {
            charsets = new String[]
            {
                ALL
            };
        }
        this.uppercase = uppercase;
        this.lowercase = lowercase;
        this.number = number;
        this.special = special;

        StringBuilder all = new StringBuilder();
        for (String item : charsets)
        {
            all.append(item);
        }
        this.allChars = all.toString().toCharArray();

        StringBuilder up = new StringBuilder();
        StringBuilder low = new StringBuilder();
        StringBuilder num = new StringBuilder();
        StringBuilder sp = new StringBuilder();

        for (char c : allChars)
        {
            if (Character.isUpperCase(c))
            {
                up.append(c);
            }
            else if (Character.isLowerCase(c))
            {
                low.append(c);
            }
            else if (Character.isDigit(c))
            {
                num.append(c);
            }
            else
            {
                sp.append(c);
            }
        }

        this.uppercaseChars = up.toString().toCharArray();
        this.lowercaseChars = low.toString().toCharArray();
        this.numberChars = num.toString().toCharArray();
        this.specialChars = sp.toString().toCharArray();

        if (uppercase > 0 && uppercaseChars.length == 0)
        {
            throw new IllegalArgumentException("No uppercase characters available for the specified minimum");
        }
        if (lowercase > 0 && lowercaseChars.length == 0)
        {
            throw new IllegalArgumentException("No lowercase characters available for the specified minimum");
        }
        if (number > 0 && numberChars.length == 0)
        {
            throw new IllegalArgumentException("No digits available for the specified minimum");
        }
        if (special > 0 && specialChars.length == 0)
        {
            throw new IllegalArgumentException("No special characters available for the specified minimum");
        }
    }

    /**
     * Generates a random passphrase of the specified size that meets the
     * minimum character requirements. The passphrase is returned as a
     * {@code char[]} to allow secure cleanup by the caller.
     *
     * @param size The total length of the passphrase.
     * @return A {@code char[]} containing the generated passphrase.
     * @throws IllegalArgumentException if size is negative or if the sum of
     * minimum requirements exceeds the specified size.
     */
    public char[] generate(int size)
    {
        if (size < 0)
        {
            throw new IllegalArgumentException("Size cannot be negative");
        }
        if (uppercase + lowercase + number + special > size)
        {
            throw new IllegalArgumentException("Sum of minimum requirements exceeds specified size");
        }

        List<Character> password = new ArrayList<>();

        addRandomChars(password, uppercaseChars, uppercase);
        addRandomChars(password, lowercaseChars, lowercase);
        addRandomChars(password, numberChars, number);
        addRandomChars(password, specialChars, special);
        addRandomChars(password, allChars, size - (uppercase + lowercase + number + special));
        Collections.shuffle(password, random);

        char[] result = new char[size];
        for (int i = 0; i < size; i++)
        {
            result[i] = password.get(i);
        }

        Collections.sort(password);
        password.clear();

        return result;
    }

    /**
     * Adds a specified number of random characters from a character set to the
     * password list.
     *
     * @param password The list to which characters will be added.
     * @param charset The character set from which to select random characters.
     * @param count The number of characters to add.
     */
    private static void addRandomChars(List<Character> password, char[] charset, int count)
    {
        for (int i = 0; i < count; i++)
        {
            if (charset.length > 0)
            {
                int index = random.nextInt(charset.length);
                password.add(charset[index]);
            }
        }
    }
}
