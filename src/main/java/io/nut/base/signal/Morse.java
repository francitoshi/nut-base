/*
 * Morse.java
 *
 * Copyright (c) 2013-2026 francitoshi@gmail.com
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
package io.nut.base.signal;

import io.nut.base.math.Nums;
import io.nut.base.queue.CircularQueueInt;
import io.nut.base.util.Joins;
import io.nut.base.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encodes and decodes International Morse Code.
 *
 * <p>
 * Supports the full ITU character set including letters (A–Z), digits (0–9),
 * punctuation marks, accented letters, and prosigns. Timing is calculated using
 * the PARIS standard with optional Farnsworth spacing for effective WPM
 * control.
 *
 * <p>
 * Typical usage:
 * <pre>{@code
 * Morse morse = new Morse(20, 15, 0, 0);
 * String[][] encoded = morse.encode("Hello World");
 * String morseText  = morse.join(encoded);   // ".... . .-.. .-.. --- / .-- --- .-. .-.. -.."
 * String decoded    = morse.decode(morseText);
 * }</pre>
 */
public class Morse
{

   /**
     * Duration of a dit (dot) in unit multiples: 1 unit.
     */
    static final char DIT = 1;

    /**
     * Duration of a dah (dash) in unit multiples: 3 units.
     */
    static final char DAH = 3;
    
    /**
     * Plain ASCII representations of dit and dah. Index 1 → {@code '.'} (dit),
     * index 3 → {@code '-'} (dah).
     */

    static final char[] TEXTS_ASCII  = {0,'.', 0 ,'-'};
    /**
     * Middle-dot / em-dash Unicode representations of dit and dah. Index 1 →
     * {@code '·'} (dit), index 3 → {@code '—'} (dah).
     */
    static final char[] TEXTS_MIDDLE = {0,'·', 0 ,'—'};

    /**
     * Bullet / heavy-dash Unicode representations of dit and dah. Index 1 →
     * {@code '•'} (dit), index 3 → {@code '━'} (dah).
     */
    static final char[] TEXTS_BOLD   = {0,'•', 0 ,'━'};
    
    /**
     * Morse code sequences for the 26 letters A–Z. Each sub-array begins with
     * the character followed by its {@link #DIT}/{@link #DAH} sequence.
     */
    static final char[][] LETTERS =
    {
        {'A', DIT, DAH },
        {'B', DAH, DIT, DIT, DIT },
        {'C', DAH, DIT, DAH, DIT },
        {'D', DAH, DIT, DIT },
        {'E', DIT },
        {'F', DIT, DIT, DAH, DIT },
        {'G', DAH, DAH, DIT },
        {'H', DIT, DIT, DIT, DIT },
        {'I', DIT, DIT },
        {'J', DIT, DAH, DAH, DAH },
        {'K', DAH, DIT, DAH },
        {'L', DIT, DAH, DIT, DIT },
        {'M', DAH, DAH },
        {'N', DAH, DIT },
        {'O', DAH, DAH, DAH },
        {'P', DIT, DAH, DAH, DIT },
        {'Q', DAH, DAH, DIT, DAH },
        {'R', DIT, DAH, DIT },
        {'S', DIT, DIT, DIT },
        {'T', DAH },
        {'U', DIT, DIT, DAH },
        {'V', DIT, DIT, DIT, DAH },
        {'W', DIT, DAH, DAH },
        {'X', DAH, DIT, DIT, DAH },     //X or Multiplication sign
        {'Y', DAH, DIT, DAH, DAH },
        {'Z', DAH, DAH, DIT, DIT },
    };

    /**
     * Morse code sequences for digits 0–9. Each sub-array begins with the digit
     * character followed by its {@link #DIT}/{@link #DAH} sequence.
     */
    static final char[][] NUMBERS =
    {
        {'1', DIT, DAH, DAH, DAH, DAH },
        {'2', DIT, DIT, DAH, DAH, DAH },
        {'3', DIT, DIT, DIT, DAH, DAH },
        {'4', DIT, DIT, DIT, DIT, DAH },
        {'5', DIT, DIT, DIT, DIT, DIT },
        {'6', DAH, DIT, DIT, DIT, DIT },
        {'7', DAH, DAH, DIT, DIT, DIT },
        {'8', DAH, DAH, DAH, DIT, DIT },
        {'9', DAH, DAH, DAH, DAH, DIT },
        {'0', DAH, DAH, DAH, DAH, DAH },
    };

    /**
     * Morse code sequences for common punctuation characters. Each sub-array
     * begins with the punctuation character followed by its
     * {@link #DIT}/{@link #DAH} sequence.
     */
    static final char[][] PUNCTUATION =
    {
        {'.', DIT, DAH, DIT, DAH, DIT, DAH },
        {',', DAH, DAH, DIT, DIT, DAH, DAH },
        {':', DAH, DAH, DAH, DIT, DIT, DIT },       //Colon 
        {'?', DIT, DIT, DAH, DAH, DIT, DIT },
        {'\'', DIT, DAH, DAH, DAH, DAH, DIT },       //Apostrophe
        {'-', DAH, DIT, DIT, DIT, DIT, DAH },       //Hyphen or dash or subtraction sign
        {'/', DAH, DIT, DIT, DAH, DIT },            //Slash
        {'(', DAH, DIT, DAH, DAH, DIT},
        {')', DAH, DIT, DAH, DAH, DIT, DAH },
        {'"', DIT, DAH, DIT, DIT, DAH, DIT },       //Quotation mark
        {'=', DAH, DIT, DIT, DIT, DAH },            //Double hyphen
        {'!', DAH, DIT, DAH, DIT, DAH, DAH },       //Exclamation mark 
        
        {';', DAH, DIT, DAH, DIT, DAH, DIT },       //Semicolon
        {'@', DIT, DAH, DAH, DIT, DAH, DIT },       //(=A+C)
        {'+', DIT, DAH, DIT, DAH, DIT },            //Plus 
        {'_', DIT, DIT, DAH, DAH, DIT, DAH },       //Underscore 
        {'$', DIT, DIT, DIT, DAH, DIT, DIT, DAH },  //Dollar sign
        {'&', DIT, DAH, DIT, DIT, DIT },            //Ampersand, Wait
    };

    /**
     * Morse code sequences for accented (non-ASCII) letters. Each sub-array
     * begins with the accented character followed by its
     * {@link #DIT}/{@link #DAH} sequence.
     */
    static final char[][] ACCENTED_LETTERS =
    {
        {'É', DIT, DIT, DAH, DIT, DIT },    // accented E
        {'Ñ', DAH, DAH, DIT, DAH, DAH },
    };
    
    /**
     * Prosign CT — "Start copying" / "Attention".
     */
    public static final String CT_START_COPYING = "CT";
    
    /**
     * Prosign HH — "Error in sending".
     */
    public static final String HH_ERROR_IN_SENDING = "HH";

    /**
     * Prosign SK — "End of transmission".
     */
    public static final String SK_END_OF_TRANSMISION = "SK";

    /**
     * Prosign SN — "Understood" (also VE).
     */
    public static final String SN_UNDE = "SN";

    /**
     * Prosign SOS — "Distress message".
     */
    public static final String SOS = "SOS";

    /**
     * All supported prosign strings. Note: AR, KA, and KN are intentionally
     * excluded to avoid character collisions.
     */
    static final String[] PROSIGNS =
    {
        CT_START_COPYING,
        HH_ERROR_IN_SENDING,
        SK_END_OF_TRANSMISION,
        SN_UNDE,
        SOS,
        //do not add AR, KA, KN because they collision with characters
    };
    
    /**
     * Flag: use middle-dot / em-dash Unicode characters for dit/dah rendering.
     */
    public static final int FLAG_MIDLE = 1;

    /**
     * Flag: use bullet / heavy-dash Unicode characters for dit/dah rendering.
     */
    public static final int FLAG_BOLD = 2;

    /**
     * Flag: use a 5-unit word gap instead of the standard 7-unit word gap. Some
     * training software (e.g. G5RV style) uses the shorter gap.
     */
    public static final int FLAG_WG5U = 8;

    /**
     * Default sending speed in words per minute (WPM).
     */
    public static final int DEFAULT_WMP = 20;

    /**
     * Inflates a raw character template array into a {@link Letter} object.
     *
     * @param template the raw template array: {@code template[0]} is the
     * plain-text character; the remaining elements are {@link #DIT} or
     * {@link #DAH} unit values.
     * @param morse a four-element char array mapping unit values (1 or 3) to
     * their display characters (e.g. {@code '.'} and {@code '-'}).
     * @return a fully populated {@link Letter}.
     */
    private static Letter inflateLetter(char[] template, char[] morse)
    {
        String letter = new String(template,0,1);
        int size = template.length-1;
        StringBuilder code = new StringBuilder();
        byte[] units = new byte[size];
        for(int i=0;i<size;i++)
        {
            char item = template[i+1];
            code.append(morse[item]);
            units[i] = (byte) item;
        }
        return new Letter(letter, code.toString(), units);
    }
    
    /**
     * Maps plain-text characters to their {@link Letter} representations for
     * encoding.
     */
    private final HashMap<Character, Letter> encodeMap = new HashMap<>();

    /**
     * Maps prosign strings (e.g. {@code "SOS"}) to their {@link Letter}
     * representations.
     */
    private final HashMap<String, Letter> prosignMap = new HashMap<>();

    /**
     * Maps morse-code strings (e.g. {@code ".-"}) to their {@link Letter}
     * representations for decoding.
     */
    private final HashMap<String, Letter> decodeMap = new HashMap<>();

    /**
     * All supported plain-text letter strings (excludes prosigns).
     */
    private final String[] allowedLetters;

    /**
     * All supported prosign strings.
     */
    private final String[] allowedProsigns;

    /**
     * All supported items: letters plus prosigns.
     */
    private final String[] allowedItems;

    /**
     * Duration of a dit (dot) pulse in milliseconds.
     */
    public final int ditMillis;

    /**
     * Duration of a dah (dash) pulse in milliseconds.
     */
    public final int dahMillis;

    /**
     * Duration of the intra-character gap (between dits/dahs within a single
     * character) in milliseconds.
     */
    public final int gapMillis;

    /**
     * Duration of the inter-character gap (between letters within a word) in
     * milliseconds.
     */
    public final int charGapMillis;

    /**
     * Duration of the inter-word gap in milliseconds (7 units standard, or 5
     * units if {@link #FLAG_WG5U} is set).
     */
    public final int wordGapMillis;

    /**
     * Duration of the initial silence before the first symbol in milliseconds.
     * Computed as {@code startGapMultiplier * wordGapMillis} and can be updated
     * via {@link #updateStartGap(int)}.
     */
    volatile int startGapMillis;
    
    /**
     * Maximum total unit count across all supported characters and prosigns.
     * Used as a guard when decoding patterns to detect over-long sequences.
     */
    public final int maxUnits;

    /**
     * Maximum number of dit/dah terms across all supported characters and
     * prosigns. Used during pattern decoding to discard impossibly long symbol
     * sequences.
     */
    public final int maxTerms;
    
    /**
     * Immutable value object that holds the plain-text representation,
     * morse-code string, and raw unit array for a single character or prosign.
     */
    private static class Letter
    {

        /**
         * Plain-text character or prosign string (e.g. {@code "A"} or
         * {@code "SOS"}).
         */
        final String letter;

        /**
         * {@code true} if this entry represents a prosign (i.e. its
         * {@link #letter} has more than one character).
         */
        final boolean prosign;

        /**
         * The morse-code string built from dit/dah display characters (e.g.
         * {@code ".-"}).
         */
        final String morse;

        /**
         * Sequence of raw unit values ({@link Morse#DIT} or {@link Morse#DAH})
         * that make up this character or prosign.
         */
        final byte[] units;

        /**
         * Constructs a new {@code Letter}.
         *
         * @param letter plain-text character or prosign string
         * @param morse morse-code display string
         * @param units raw dit/dah unit array
         */
        public Letter(String letter, String morse, byte[] units)
        {
            this.letter = letter;
            this.prosign = letter.length() > 1;
            this.morse = morse;
            this.units = units;
        }
    }
    
    /**
     * Constructs a {@code Morse} instance with default settings:
     * {@value #DEFAULT_WMP} WPM sending and effective speed, no flags, no start
     * gap.
     */
    public Morse()
    {
        this(DEFAULT_WMP, DEFAULT_WMP, 0, 0);
    }
    
    /**
     * Constructs a {@code Morse} instance with full control over timing and
     * display.
     *
     * <p>
     * Timing is derived from the PARIS standard ({@code 1200 / wpm} ms per
     * unit) with Farnsworth spacing applied when {@code ewpm < wpm}.
     *
     * @param wpm character sending speed in words per minute; if less than
     * {@code ewpm} it is raised to match.
     * @param ewpm effective (average) speed in words per minute, used to
     * calculate spacing via the Farnsworth method.
     * @param flags bitfield of display/timing flags:
     * {@link #FLAG_MIDLE}, {@link #FLAG_BOLD}, {@link #FLAG_WG5U}.
     * @param startGapMultiplier multiplier applied to {@link #wordGapMillis} to
     * produce the initial silence before the first symbol; use {@code 0} for no
     * initial silence.
     */
    public Morse(int wpm, int ewpm, int flags, int startGapMultiplier)
    {
        boolean middle   = (flags & FLAG_MIDLE)    == FLAG_MIDLE;
        boolean bold     = (flags & FLAG_BOLD)     == FLAG_BOLD;
        boolean wg5u     = (flags & FLAG_WG5U)     == FLAG_WG5U;

        //wpm must be as fast as ewpm
        wpm = Math.max(wpm, ewpm);

        //PARIS rule and Farnsworth method
        int c = 1200/wpm;       // unit of time used for characters
        int u = 1200/ewpm;      // efective or average unit of time
        int s = (u*50-c*31)/19; // unit of time used for spaces

        this.ditMillis = c * DIT;
        this.dahMillis = c * DAH;
        this.gapMillis = s;
        this.charGapMillis = s * 3;
        this.wordGapMillis = s * (wg5u ? 5 : 7);
        this.startGapMillis = startGapMultiplier * this.wordGapMillis;
        
        char[] morse = bold ? TEXTS_BOLD : ( middle ? TEXTS_MIDDLE : TEXTS_ASCII);  
        List<char[][]> list = Utils.listOf(LETTERS, NUMBERS, PUNCTUATION, ACCENTED_LETTERS);

        List<String> letterList = new ArrayList<>();
        List<String> prosignList = new ArrayList<>();
        List<String> itemList = new ArrayList<>();
        
        int maxUnits = 0;
        int maxTerms = 0;
        
        for(char[][] item : list)
        {
            for(char[] subitem : item)
            {
                Letter letter = inflateLetter(subitem, morse);
                encodeMap.put(subitem[0], letter);
                decodeMap.put(letter.morse, letter);
                maxUnits = Math.max(maxUnits, (int)(Nums.sum(letter.units)+letter.units.length));
                maxTerms = Math.max(maxTerms, letter.units.length);
                letterList.add(letter.letter);
                itemList.add(letter.letter);
            }            
        }
        for(String item : PROSIGNS)
        {
            char[] letters = item.toCharArray();
            byte[] units = new byte[0];
            StringBuilder code = new StringBuilder();
            for(int i=0;i<letters.length;i++)
            {
                Letter letter = encodeMap.getOrDefault(letters[i], null);
                if(letter==null)
                {
                    throw new NullPointerException("letter must not be null for "+letters[i]);
                }
                code.append(letter.morse);
                units = Joins.join(units, letter.units);
            }
            Letter letter = new Letter(item, code.toString(), units);
            prosignMap.put(item, letter);
            Letter collision = decodeMap.put(letter.morse, letter);
            if(collision!=null)
            {
                throw new RuntimeException("Prosign collision"+letter.letter+" > "+collision.letter);
            }
            maxUnits = Math.max(maxUnits, (int)(Nums.sum(units)+units.length));
            maxTerms = Math.max(maxTerms, (int)units.length); 
            prosignList.add(letter.letter);
            itemList.add(letter.letter);
        }
        this.maxUnits = maxUnits;
        this.maxTerms = maxTerms;
        this.allowedLetters = letterList.toArray(new String[0]);
        this.allowedProsigns = prosignList.toArray(new String[0]);
        this.allowedItems = itemList.toArray(new String[0]);
    }
    
    /**
     * Updates the initial silence duration before the first transmitted symbol.
     *
     * @param multiplier the number of word-gap lengths to wait before the first
     * symbol; {@code 0} means no initial silence.
     */
    public void updateStartGap(int multiplier)
    {
        this.startGapMillis = multiplier * this.wordGapMillis;
    }
    
    /**
     * Pattern used to detect prosign tokens written in angle-bracket notation,
     * e.g. {@code <SOS>} or {@code <SK>}.
     */
    private final static Pattern PROSIGN_PATTERN = Pattern.compile("<([A-Za-z]+)>");
    
    /**
     * Encodes a plain-text string into its Morse code representation.
     *
     * <p>
     * The input is trimmed, converted to upper-case, and split on whitespace
     * into words. Prosigns must be written in angle-bracket notation (e.g.
     * {@code <SOS>}). Unknown characters are silently skipped with a warning
     * written to {@code System.err}.
     *
     * @param plainText the human-readable text to encode.
     * @return a two-dimensional array where the first dimension corresponds to
     * words and the second to individual characters (or one element per
     * prosign); each element is the morse-code display string for that
     * character (e.g. {@code ".-"} for {@code 'A'}).
     */
    public String[][] encode(String plainText)
    {
        String[] plainWords = plainText.trim().toUpperCase().split("\\s+");
        String[][] codes = new String[plainWords.length][];
        for(int i=0;i<plainWords.length;i++)
        {
            String word = plainWords[i];
            Matcher matcher = PROSIGN_PATTERN.matcher(word);
            if(matcher.matches())
            {
                Letter prosign = prosignMap.getOrDefault(matcher.group(1), null);
                if(prosign!=null)
                {
                    codes[i] = new String[]{prosign.morse};
                    continue;
                }
            }
            char[] plainChars = word.toCharArray();
            codes[i] = new String[plainChars.length];
            for(int j=0;j<plainChars.length;j++)
            {
                Letter letter =  encodeMap.getOrDefault(plainChars[j], null);
                if(letter!=null)
                {
                    codes[i][j] = letter.morse;
                }
                else
                {
                    System.err.println("uknown code "+plainChars[j]);
                }
            }            
        }
        return codes;
    }
    
    /**
     * Encodes a plain-text string into raw dit/dah unit arrays.
     *
     * <p>
     * The structure mirrors {@link #encode(String)}: a three-dimensional array
     * where {@code [word][character][unit]} holds each raw {@link #DIT} or
     * {@link #DAH} byte value. Unknown characters produce an empty unit array.
     *
     * @param plainText the human-readable text to encode.
     * @return a three-dimensional byte array of dit/dah unit values.
     */
    public byte[][][] encodeUnits(String plainText)
    {
        String[] plainWords = plainText.trim().toUpperCase().split("\\s+");
        byte[][][] units = new byte[plainWords.length][][];
        for(int i=0;i<plainWords.length;i++)
        {
            String word = plainWords[i];
            Matcher matcher = PROSIGN_PATTERN.matcher(word);
            if(matcher.matches())
            {
                Letter prosign = prosignMap.getOrDefault(matcher.group(1), null);
                if(prosign!=null)
                {
                    units[i] = new byte[][]{prosign.units};
                    continue;
                }
            }
            char[] plainChars = word.toCharArray();
            units[i] = new byte[plainChars.length][];
            for(int j=0;j<plainChars.length;j++)
            {
                Letter letter =  encodeMap.getOrDefault(plainChars[j], null);
                if(letter!=null)
                {
                    units[i][j] = letter.units;
                }
                else
                {
                    units[i][j] = new byte[0];
                }
            }            
        }
        return units;
    }

    /**
     * Encodes a plain-text string into an Android-style vibration pattern.
     *
     * <p>
     * The returned array alternates between silence and pulse durations in
     * milliseconds and always begins with a silence element (the start gap). It
     * is suitable for use with
     * {@code android.os.Vibrator.vibrate(long[], int)}.
     *
     * @param plainText the human-readable text to encode.
     * @return an alternating silence/pulse array in milliseconds.
     * @see #join(byte[][][], boolean)
     */
    public int[] encodePattern(String plainText)
    {
        return join(encodeUnits(plainText), true);
    }

    /**
     * Converts a three-dimensional unit array into a flat vibration-style
     * pattern.
     *
     * <p>
     * The array alternates between silence and pulse values; the first element
     * is always a silence equal to {@link #startGapMillis}. When
     * {@code useMillis} is {@code true} the timing constants
     * ({@link #ditMillis}, {@link #gapMillis}, etc.) are used; when
     * {@code false}, every unit is treated as {@code 1}.
     *
     * @param units three-dimensional byte array as returned by
     * {@link #encodeUnits(String)}.
     * @param useMillis {@code true} to produce real-time millisecond durations;
     * {@code false} to produce raw unit counts.
     * @return a flat alternating silence/pulse integer array.
     */
    public int[] join(byte[][][] units, boolean useMillis)
    {
        final int ditMs = useMillis ? this.ditMillis : 1;
        final int gapMs = useMillis ? this.gapMillis : 1;
        final int charGapMs = useMillis ? this.charGapMillis : 1;
        final int wordGapMs = useMillis ? this.wordGapMillis : 1;
        
        ArrayList<Integer> pattern = new ArrayList<>();
        int gap = startGapMillis;
        for(byte[][] word : units)
        {
            for(byte[] letter : word)
            {
                for(byte unit : letter)
                {
                    pattern.add(gap);
                    pattern.add(unit*ditMs);
                    gap = gapMs;
                }
                gap = charGapMs;
            }
            gap = wordGapMs;
        }

        int[] ret = new int[pattern.size()];
        for(int i=0;i<pattern.size();i++)
        {
            ret[i] = pattern.get(i);
        }
        return ret;
    }

    /**
     * Concatenates multiple vibration-style patterns into a single pattern.
     *
     * <p>
     * Adjacent patterns are merged intelligently: consecutive silence values
     * are summed, and when a pulse-ending pattern is followed by a new pattern,
     * a word-gap silence is inserted automatically.
     *
     * @param patterns one or more alternating silence/pulse arrays to
     * concatenate.
     * @return a single merged alternating silence/pulse array; an empty array
     * if all inputs are {@code null} or empty.
     */
    public int[] join(int[]... patterns)
    {
        if (patterns == null || patterns.length == 0)
        {
            return new int[0];
        }

        List<Integer> out = new ArrayList<>();

        boolean prevEndsWithSilence = false;

        for (int[] p : patterns)
        {
            if (p == null || p.length == 0)
            {
                continue;
            }

            if (out.isEmpty())
            {
                // Primer patrón: se copia tal cual
                for (int v : p)
                {
                    out.add(v);
                }
            }
            else if (prevEndsWithSilence)
            {
                // último era silencio → sumar con el primer silencio del nuevo
                int lastIndex = out.size() - 1;
                out.set(lastIndex, out.get(lastIndex) + p[0]);

                // copiar el resto
                for (int i = 1; i < p.length; i++)
                {
                    out.add(p[i]);
                }
            }
            else
            {
                // último era pulso → al primer silencio se le suma 444
                out.add(p[0] + this.wordGapMillis);

                for (int i = 1; i < p.length; i++)
                {
                    out.add(p[i]);
                }
            }

            // determinar cómo termina este patrón
            prevEndsWithSilence = (p.length % 2) == 1;
        }

        // convertir a int[]
        int[] result = new int[out.size()];
        for (int i = 0; i < out.size(); i++)
        {
            result[i] = out.get(i);
        }
        return result;
    }
    
    /**
     * Formats an encoded morse word/character array as a human-readable string.
     *
     * <p>
     * Individual character codes are joined with spaces; words are separated by
     * {@code " / "}. For example, encoding {@code "HI"} would produce
     * {@code ".... .."}.
     *
     * @param morse the two-dimensional morse array as returned by
     * {@link #encode(String)}.
     * @return a formatted morse-code string.
     */
    public String join(String[][] morse)
    {
        StringJoiner plainText = new StringJoiner(" / ");
        for(String[] word : morse)
        {
            StringJoiner plainWord = new StringJoiner(" ");
            for(String letter : word)
            {
                plainWord.add(letter);
            }
            plainText.add(plainWord.toString());
        }
        return plainText.toString();
    }
    
    /**
     * Decodes a formatted morse-code string back into plain text.
     *
     * <p>
     * Words must be separated by {@code "/"} (with optional surrounding
     * spaces). Characters within a word must be separated by one or more
     * spaces. Prosigns are returned in angle-bracket notation (e.g.
     * {@code <SOS>}). Unknown sequences are silently skipped with a warning on
     * {@code System.err}.
     *
     * @param morse a formatted morse string such as
     * {@code ".... . .-.. .-.. --- / .-- --- .-. .-.. -.."}.
     * @return the decoded plain-text string with words separated by single
     * spaces.
     */
    public String decode(String morse)
    {
        StringJoiner text = new StringJoiner(" ");
        for (String morseWord : morse.split("\\s*/\\s*"))
        {
            StringBuilder word = new StringBuilder();
            String[] plainChars = morseWord.split("\\s+");
            for(int j=0;j<plainChars.length;j++)
            {
                Letter letter =  decodeMap.getOrDefault(plainChars[j], null);
                if(letter!=null)
                {
                    word.append(letter.letter);
                }
                else
                {
                    System.err.println("uknown code "+plainChars[j]);
                }
            }
            text.add(word);
        }
        return text.toString();
    }
    
    protected boolean decodeLetter(StringBuilder currentLetter, Consumer<String> action)
    {
        boolean actionExecuted = false;
        if (currentLetter.length() > 0)
        {
            String symbol = currentLetter.toString();
            Letter letter = decodeMap.getOrDefault(symbol, null);
            if(letter==null)
            {
                action.accept("?");
                actionExecuted = true;
            }
            else if(letter.prosign)
            {
                action.accept('<'+letter.letter+'>');
                actionExecuted = true;
            }
            else
            {
                action.accept(letter.letter);
                actionExecuted = true;
            }
            currentLetter.setLength(0); // Limpiar para la siguiente letra
        }
        return actionExecuted;
    }
    
    /**
     * Decodes an iterable of vibration-style patterns into plain text,
     * streaming each decoded token to the supplied consumer.
     *
     * <p>
     * Each pattern is an alternating silence/pulse array in milliseconds. The
     * base unit duration is inferred from the smallest non-zero value in each
     * pattern. Timing thresholds are derived from that base unit:
     * <ul>
     * <li>Pulses shorter than {@code 2 × unit} are treated as dits; longer as
     * dahs.</li>
     * <li>Gaps shorter than {@code 2 × unit} are intra-character gaps
     * (ignored).</li>
     * <li>Gaps between {@code 2 × unit} and {@code 5 × unit} are
     * inter-character gaps — a letter boundary is emitted.</li>
     * <li>Gaps of {@code 5 × unit} or longer are inter-word gaps — a space is
     * emitted after the current letter.</li>
     * </ul>
     *
     * <p>
     * Prosigns are delivered as {@code <PROSIGN>} tokens; unknown sequences as
     * {@code "?"}; overly long sequences as {@code "<?>"}. A trailing space is
     * never appended automatically.
     *
     * @param pattern an iterable of alternating silence/pulse arrays
     * (milliseconds); must not be {@code null}.
     * @param action a consumer invoked for each decoded character, space,
     * prosign, or error token; must not be {@code null}.
     * @throws NullPointerException if {@code pattern} or {@code action} is
     * {@code null}.
     */
    public void decodePattern(Iterable<int[]> pattern, Consumer<String> action)
    {
        Objects.requireNonNull(pattern, "pattern must not be null");
        Objects.requireNonNull(action, "action must not be null");

        StringBuilder currentLetter = new StringBuilder();
        final AtomicBoolean spaceEnabled = new AtomicBoolean();
        final AtomicInteger count = new AtomicInteger(0);

        for(int[] item : pattern)
        {
            CircularQueueInt cqi = new CircularQueueInt(item);
            
            long unitT = baseUnit(cqi.array());
            long pulseThreshold = unitT * 2;
            long gapInterCharacterThreshold = unitT * 2;
            long gapInterWordThreshold = unitT * 5;
            cqi.foreach((ms) ->
            {
                boolean isPulse = (count.getAndIncrement() % 2 == 1);
                if(ms==0)
                {
                    //do nothink is a dummy pulse
                }
                else if(isPulse)
                {
                    // decode dit or dah
                    currentLetter.append(ms < pulseThreshold ? "." : "-");
                }
                else if(currentLetter.length()>maxTerms)
                {
                    //too long discard
                    action.accept("<?>");
                    spaceEnabled.set(true);
                }
                else if(ms >= gapInterWordThreshold)
                {   
                    //It is a silence, deciding whether to finish a letter or a word.
                    boolean done = decodeLetter(currentLetter, action);
                    if(done || spaceEnabled.get())
                    {
                        action.accept(" ");
                        spaceEnabled.set(false);                    
                    }
                }
                else if (ms >= gapInterCharacterThreshold)
                {
                    decodeLetter(currentLetter, action);
                    spaceEnabled.set(true);
                }
                //If the silence is very short, it is only the internal separation of the letter (it is ignored)
            });
        }

        // Procesar la última letra si quedó algo pendiente
        decodeLetter(currentLetter, action);
    }

    /**
     * Decodes a single vibration-style pattern array into a plain-text string.
     *
     * <p>
     * This is a convenience wrapper around
     * {@link #decodePattern(Iterable, Consumer)} that wraps the single array in
     * a list and collects the output into a {@link String}.
     *
     * @param pattern an alternating silence/pulse array in milliseconds.
     * @return the decoded plain-text string.
     */
    public String decodePattern(int[] pattern)
    {
        StringBuilder decodedMessage = new StringBuilder();
        List<int[]> list = new ArrayList<>();
        list.add(pattern);
        decodePattern(list, (letter) -> decodedMessage.append(letter));
        return decodedMessage.toString();
    } 

    /**
     * Determines the base unit duration from a vibration-style pattern array.
     *
     * <p>
     * The base unit is the smallest positive (non-zero) value in the array,
     * which corresponds to the duration of a single dit pulse or
     * intra-character gap. It is used as the timing reference for all threshold
     * calculations during pattern decoding.
     *
     * @param pattern an alternating silence/pulse array; zero values are
     * ignored.
     * @return the smallest positive value found, or {@link Integer#MAX_VALUE}
     * if no positive values are present.
     */
    static int baseUnit(int[] pattern)
    {
        int m = Integer.MAX_VALUE;
        for(int p : pattern)
        {
            m = (p>0) ? Math.min(m,p) : m;
        }
        return m;
    }
    
    /**
     * Returns a copy of the set of supported character and/or prosign strings.
     *
     * @param letters {@code true} to include individual characters (A–Z, 0–9,
     * punctuation, accented letters).
     * @param prosigns {@code true} to include prosigns (e.g. {@code "SOS"}).
     * @return a new array containing the requested items; an empty array if
     * both parameters are {@code false}.
     */
    public String[] allowed(boolean letters, boolean prosigns)
    {
        if(letters && prosigns)
        {
            return this.allowedItems.clone();
        }
        if(letters)
        {
            return this.allowedLetters.clone();
        }
        if(prosigns)
        {
            return this.allowedProsigns.clone();
        }
        return new String[0];
    }
}
