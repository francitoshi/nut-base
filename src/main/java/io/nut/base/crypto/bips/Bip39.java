/*
 *  BIP39.java
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
package io.nut.base.crypto.bips;

//https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/crypto/MnemonicCode.java

import io.nut.base.crypto.alt.PBKDF2SHA512;
import io.nut.base.crypto.Digest;
import io.nut.base.util.Strings;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 *
 * @author franci
 */
public class Bip39
{

    public enum Words
    {
        Twelve(12), Fifteen(15), Eighteen(18), Twentyone(21), Twentyfour(24);

        public final int bytes;
        public final int bits;

        Words(int bytes)
        {
            this.bytes = bytes;
            this.bits = bytes * 8;
        }
    }
    public enum Language 
    {
        English("mnemonic_wordlist_english.txt"), 
//        Japanese, 
//        Spanish("mnemonic_wordlist_spanish.txt"), 
//        ChineseSimplified, 
//        ChineseTraditional, 
//        French, 
//        Italian, 
//        Korean, 
//        Czech, 
//        Portuguese
        ;
        private final String fileName;
        private Language(String fileName)
        {
            this.fileName = fileName;
        }
        
    }

    public static final int PBKDF2_ROUNDS = 2048;
    
    private static String[] split(String words)
    {
        return words.split("\\s+");
    }
    
    public static String normalizeNFKD(CharSequence s)
    {
        return Normalizer.normalize(s, Normalizer.Form.NFKD);
    }

    public static byte[] seed(String words, String passphrase)
    {
        return seed(split(words), passphrase);
    }

    public static byte[] seed(List<String> words, String passphrase)
    {
        return seed(words.toArray(new String[0]), passphrase);
    }
    
    public static byte[] seed(String[] words, String passphrase)
    {
        Objects.requireNonNull(passphrase, "A null passphrase is not allowed.");
        // To create binary seed from mnemonic, we use PBKDF2 function
        // with mnemonic sentence (in UTF-8) used as a password and
        // string "mnemonic" + passphrase (again in UTF-8) used as a
        // salt. Iteration count is set to 4096 and HMAC-SHA512 is
        // used as a pseudo-random function. Desired length of the
        // derived key is 512 bits (= 64 bytes).
        String pass = normalizeNFKD(Strings.join(" ", words));
        String salt = "mnemonic" + passphrase;
        return PBKDF2SHA512.derive(pass, salt, PBKDF2_ROUNDS, 64);
    }

    private final WordList wordList;

    public Bip39()
    {
        this(Language.English);
    }
    public Bip39(Language language)
    {
        this.wordList = WordList.getInstance(language);
    }

    public byte[] entropy(String words) throws MnemonicLengthException, MnemonicWordException, MnemonicChecksumException
    {
        return entropy(split(words));
    }
    /**
     * Convert character array to entropy data
     * @param words
     * @return 
     * @throws MnemonicLengthException
     * @throws MnemonicWordException
     * @throws MnemonicChecksumException
     */
    public byte[] entropy(String[] words) throws MnemonicLengthException, MnemonicWordException, MnemonicChecksumException
    {
        if(words.length % 3 > 0)
        {
            throw new MnemonicLengthException("Word list size must be multiple of three words.");
        }

        if(words.length == 0)
        {
            throw new MnemonicLengthException("Word list is empty.");
        }

        // Look up all the words in the list and construct the
        // concatenation of the original entropy and the checksum.
        int concatLenBits = words.length * 11;
        boolean[] concatBits = new boolean[concatLenBits];
        int wordindex = 0;
        for (String word : words)
        {
            // Find the words index in the wordlist.
            int ndx = this.wordList.getIndex(word);
            if (ndx < 0)
            {
                throw new MnemonicWordException(word);
            }

            // Set the next 11 bits to the value of the index.
            for (int ii = 0; ii < 11; ++ii)
            {
                concatBits[(wordindex * 11) + ii] = (ndx & (1 << (10 - ii))) != 0;
            }
            ++wordindex;
        }

        int checksumLengthBits = concatLenBits / 33;
        int entropyLengthBits = concatLenBits - checksumLengthBits;

        // Extract original entropy as bytes.
        byte[] entropy = new byte[entropyLengthBits / 8];
        for (int ii = 0; ii < entropy.length; ++ii)
        {
            for (int jj = 0; jj < 8; ++jj)
            {
                if (concatBits[(ii * 8) + jj])
                {
                    entropy[ii] |= 1 << (7 - jj);
                }
            }
        }
        // Take the digest of the entropy.

        byte[] hash = Digest.sha256(entropy);
        boolean[] hashBits = bytesToBits(hash);

        // Check all the checksum bits.
        for (int i = 0; i < checksumLengthBits; ++i)
        {
            if (concatBits[entropyLengthBits + i] != hashBits[i])
            {
                throw new MnemonicChecksumException();
            }
        }
        return entropy;
    }

    /**
     * Convert entropy data to character array
     * @param entropy
     * @return 
     * @throws MnemonicLengthException
     */
    public String[] mnemonic(byte[] entropy) throws MnemonicLengthException
    {
        if (entropy.length % 4 > 0)
        {
            throw new MnemonicLengthException("Entropy length not multiple of 32 bits.");
        }

        if (entropy.length == 0)
        {
            throw new MnemonicLengthException("Entropy is empty.");
        }

        // We take initial entropy of ENT bits and compute its
        // checksum by taking first ENT / 32 bits of its SHA256 hash.
        byte[] hash = Digest.sha256(entropy);
        boolean[] hashBits = bytesToBits(hash);

        boolean[] entropyBits = bytesToBits(entropy);
        int checksumLengthBits = entropyBits.length / 32;

        // We append these bits to the end of the initial entropy. 
        boolean[] concatBits = new boolean[entropyBits.length + checksumLengthBits];
        System.arraycopy(entropyBits, 0, concatBits, 0, entropyBits.length);
        System.arraycopy(hashBits, 0, concatBits, entropyBits.length, checksumLengthBits);

        // Next we take these concatenated bits and split them into
        // groups of 11 bits. Each group encodes number from 0-2047
        // which is a position in a wordlist.  We convert numbers into
        // words and use joined words as mnemonic sentence.
        ArrayList<String> words = new ArrayList<>();
        int nwords = concatBits.length / 11;
        for (int i = 0; i < nwords; ++i)
        {
            int index = 0;
            for (int j = 0; j < 11; ++j)
            {
                index <<= 1;
                if (concatBits[(i * 11) + j])
                {
                    index |= 0x1;
                }
            }
            words.add(this.wordList.getWord(index));
        }

        return words.toArray(new String[0]);
    }

    public void check(String words) throws MnemonicException
    {
        check(split(words));
    }
    /**
     * Check to see if a mnemonic word list is valid.
     * @param words
     * @throws MnemonicException
     */
    public void check(String[] words) throws MnemonicException
    {
        entropy(words);
    }
    /**
     * Check to see if a mnemonic word list is valid.
     * @param words
     * @throws MnemonicException
     */
    public void check(List<String> words) throws MnemonicException
    {
        entropy(words.toArray(new String[0]));
    }

    private static boolean[] bytesToBits(byte[] data)
    {
        boolean[] bits = new boolean[data.length * 8];
        for (int i = 0; i < data.length; ++i)
        {
            for (int j = 0; j < 8; ++j)
            {
                bits[(i * 8) + j] = (data[i] & (1 << (7 - j))) != 0;
            }
        }
        return bits;
    }

    public static class MnemonicException extends Exception
    {
        public MnemonicException()
        {
            super();
        }
        public MnemonicException(String msg)
        {
            super(msg);
        }
    }

    /**
     * Thrown when an argument to MnemonicCode is the wrong length.
     */
    public static class MnemonicLengthException extends MnemonicException
    {
        public MnemonicLengthException(String msg)
        {
            super(msg);
        }
    }

    /**
     * Thrown when a list of MnemonicCode words fails the checksum check.
     */
    public static class MnemonicChecksumException extends MnemonicException
    {
        public MnemonicChecksumException()
        {
            super();
        }
    }

    /**
     * Thrown when a word is encountered which is not in the MnemonicCode's word
     * list.
     */
    public static class MnemonicWordException extends MnemonicException
    {
        /**
         * Contains the word that was not found in the word list.
         */
        public final String badWord;

        public MnemonicWordException(String badWord)
        {
            super();
            this.badWord = badWord;
        }
    }

    static class WordList
    {
        private static final Map<Language,WordList> map = new HashMap<>();

        public static WordList getInstance(Language language)
        {
            WordList ret = map.get(language);
            if(ret==null)
            {
                map.put(language, ret = new WordList(language));
            }
            return ret;
        }

        private final HashMap<String, Integer> s2i = new HashMap<>();
        private final String[] i2s;

        public WordList()
        {
            this(Language.English);
        }

        public WordList(Language language)
        {
            this.i2s = new String[2048];
            try (Scanner sc = new Scanner(WordList.class.getResourceAsStream(language.fileName)))
            {
                for (int i = 0; i < 2048 && sc.hasNext(); i++)
                {
                    String line = sc.nextLine().trim().toLowerCase();
                    i2s[i] = line;
                    s2i.put(line, i);
                    if (i > 0 && i2s[i - 1].compareTo(i2s[i]) >= 0)
                    {
                        throw new RuntimeException(i2s[i - 1] + ">=" + i2s[i]);
                    }
                }
            }
        }

        public String getWord(int i)
        {
            return i2s[i];
        }

        public int getIndex(String s)
        {
            return s2i.getOrDefault(s, -1);
        }
    }
}

