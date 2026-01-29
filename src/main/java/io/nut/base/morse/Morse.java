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
package io.nut.base.morse;

import io.nut.base.math.Nums;
import io.nut.base.queue.CircularQueueInt;
import io.nut.base.util.Joins;
import io.nut.base.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Morse
{
    static final char DIT = 1;  //1u or dot
    static final char DAH = 3;  //3u or dash
    
    static final char[] TEXTS_ASCII  = {0,'.', 0 ,'-'};
    static final char[] TEXTS_MIDDLE = {0,'·', 0 ,'—'};
    static final char[] TEXTS_BOLD   = {0,'•', 0 ,'━'};
    
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
    static final char[][] ACCENTED_LETTERS =
    {
        {'É', DIT, DIT, DAH, DIT, DIT },    // accented E
        {'Ñ', DAH, DAH, DIT, DAH, DAH },
    };
    
    public static final String CT_START_COPYING = "CT";         //CT, Start copying
    public static final String HH_ERROR_IN_SENDING = "HH";      //HH - Error in sending
//    public static final String KA_BEGINNING_OF_MESSAGE = "KA";//KA, Beginning of message
//    public static final String KN_END_OF_TRANSMISSION = "KN"; //KN,  End of transmission
    public static final String SK_END_OF_TRANSMISION = "SK";    //SK, End of transmission
    public static final String SN_UNDE = "SN";                  //Understood (also VE) di- di- di- dah- dit
    public static final String SOS = "SOS";                     //SOS, Distress message
    
    static final String[] PROSIGNS =
    {
        CT_START_COPYING,
        HH_ERROR_IN_SENDING,
        SK_END_OF_TRANSMISION,
        SN_UNDE,
        SOS,
        //do not add AR, KA, KN because they collision with characters
    };
    
    public static final int FLAG_MIDLE = 1; //use middle characters
    public static final int FLAG_BOLD  = 2; // use bold characters
    public static final int FLAG_WG5U  = 8; //word gap 5 units (default 7)

    public static final int DEFAULT_WMP = 20;

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
    
    private final HashMap<Character,Letter> encodeMap = new HashMap<>();
    private final HashMap<String,Letter> prosignMap = new HashMap<>();
    private final HashMap<String,Letter> decodeMap = new HashMap<>();
    private final String[] allowedLetters;
    private final String[] allowedProsigns;
    private final String[] allowedItems;

    public final int ditMillis;
    public final int dahMillis;
    public final int gapMillis;
    public final int charGapMillis;
    public final int wordGapMillis;
    volatile int startGapMillis;
    
    public final int maxUnits;
    public final int maxTerms;
    
    private static class Letter
    {
        final String letter;
        final boolean prosign;
        final String morse;
        final byte[] units;

        public Letter(String letter, String morse, byte[] units)
        {
            this.letter = letter;
            this.prosign = letter.length()>1;
            this.morse = morse;
            this.units = units;
        }
    }
    
    public Morse()
    {
        this(DEFAULT_WMP, DEFAULT_WMP, 0, 0);
    }
    
    public Morse(int wpm, int ewpm, int flags, int startGapMultiplier)
    {
        boolean middle   = (flags & FLAG_MIDLE)    == FLAG_MIDLE;
        boolean bold     = (flags & FLAG_BOLD)     == FLAG_BOLD;
        boolean wg5u     = (flags & FLAG_WG5U)     == FLAG_WG5U;

        //wpm must be as fast as ewpm
        wpm = Math.max(wpm,ewpm);

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
    
    public void updateStartGap(int multiplier)
    {
        this.startGapMillis = multiplier * this.wordGapMillis;
    }
    
    private final static Pattern PROSIGN_PATTERN = Pattern.compile("<([A-Za-z]+)>");
    
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

    //pattern always start with a 0 gap
    public int[] encodePattern(String plainText)
    {
        return join(encodeUnits(plainText), true);
    }

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

    public void decodePattern(Iterable<int[]> pattern, Consumer<String> action)
    {
        Objects.requireNonNull(pattern, "pattern must not be null");
        Objects.requireNonNull(action, "action must not be null");

        StringBuilder currentLetter = new StringBuilder();
        
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
                    // Decidir si es punto o raya
                    currentLetter.append(ms < pulseThreshold ? "." : "-");
                }
                else if(currentLetter.length()>maxTerms)
                {
                    //too long discard
                    action.accept("<?> ");
                }
                else if(ms >= gapInterWordThreshold)
                {   
                    //It is a silence, deciding whether to finish a letter or a word.
                    decodeLetter(currentLetter, action);
                    action.accept(" ");
                }
                else if (ms >= gapInterCharacterThreshold)
                {
                    decodeLetter(currentLetter, action);
                }
                //If the silence is very short, it is only the internal separation of the letter (it is ignored)
            });
        }

        // Procesar la última letra si quedó algo pendiente
        decodeLetter(currentLetter, action);
    }
    
    protected void decodeLetter(StringBuilder currentLetter, Consumer<String> action)
    {
        if (currentLetter.length() > 0)
        {
            String symbol = currentLetter.toString();
            Letter letter = decodeMap.getOrDefault(symbol, null);
            if(letter==null)
            {
                action.accept("?");
            }
            else if(letter.prosign)
            {
                action.accept('<'+letter.letter+'>');
            }
            else
            {
                action.accept(letter.letter);
            }
            currentLetter.setLength(0); // Limpiar para la siguiente letra
        }
    }
    
    public String decodePattern(int[] pattern)
    {
        StringBuilder decodedMessage = new StringBuilder();
        List<int[]> list = new ArrayList<>();
        list.add(pattern);
        decodePattern(list, (letter) -> decodedMessage.append(letter));
        return decodedMessage.toString();
    } 

    static int baseUnit(int[] pattern)
    {
        int m = Integer.MAX_VALUE;
        for(int p : pattern)
        {
            m = (p>0) ? Math.min(m,p) : m;
        }
        return m;
    }
    
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
