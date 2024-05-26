/*
 * MorseCode.java
 *
 * Copyright (c) 2013-2024 francitoshi@gmail.com
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
package io.nut.base.text;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: franci
 * Date: 2/03/13
 * Time: 20:49
 * To change this template use File | Settings | File Templates.
 */
public class MorseCode
{

    static final int gap=0;         //1u
    static final int dit=1;         //1u
    static final int dah=2;         //3u
    static final int gap_letter=3;  //3u
    static final int gap_word=4;    //7u
    static final int gap_error=5;
    
    static final int[] units5 = {1,1,3,3,5};
    static final int[] units7 = {1,1,3,3,7};
    static final String[] TEXTS_NORMAL = {" ","•","–","   ","       "};
    static final String[] TEXTS_BRIEF = {"","•","–"," ","  "};
    
    static final int[][] LETTERS =
    {
        {'A', dit, dah },
        {'B', dah, dit, dit, dit },
        {'C', dah, dit, dah, dit },
        {'D', dah, dit, dit },
        {'E', dit },
        {'F', dit, dit, dah, dit },
        {'G', dah, dah, dit },
        {'H', dit, dit, dit, dit },
        {'I', dit, dit },
        {'J', dit, dah, dah, dah },
        {'K', dah, dit, dah },
        {'L', dit, dah, dit, dit },
        {'M', dah, dah },
        {'N', dah, dit },
        {'O', dah, dah, dah },
        {'P', dit, dah, dah, dit },
        {'Q', dah, dah, dit, dah },
        {'R', dit, dah, dit },
        {'S', dit, dit, dit },
        {'T', dah },
        {'U', dit, dit, dah },
        {'V', dit, dit, dit, dah },
        {'W', dit, dah, dah },
        {'X', dah, dit, dit, dah },
        {'Y', dah, dit, dah, dah },
        {'Z', dah, dah, dit, dit },
    };
    static final int[][] LETTERS_EXTRA=
            {
        {'Ä', dit, dah, dit, dah },
        {'Á', dit, dah, dah, dit, dah },
        {'Å', dit, dah, dah, dit, dah },
        //{'Ch',dah, dah, dah, dah },
        {'É', dit, dit, dah, dit, dit },
        {'Ñ', dah, dah, dit, dah, dah },
        {'Ö', dah, dah, dah, dit },
        {'Ü', dit, dit, dah, dah },
    };
    static final int[][] NUMBERS =
    {
        {'1', dit, dah, dah, dah, dah },
        {'2', dit, dit, dah, dah, dah },
        {'3', dit, dit, dit, dah, dah },
        {'4', dit, dit, dit, dit, dah },
        {'5', dit, dit, dit, dit, dit },
        {'6', dah, dit, dit, dit, dit },
        {'7', dah, dah, dit, dit, dit },
        {'8', dah, dah, dah, dit, dit },
        {'9', dah, dah, dah, dah, dit },
        {'0', dah, dah, dah, dah, dah },
    };
    static final int[][] PUNCTUATION =
    {
        {'.', dit, dah, dit, dah, dit, dah },
        {',', dah, dah, dit, dit, dah, dah },
        {':', dah, dah, dah, dit, dit, dit },       //Colon 
        {'?', dit, dit, dah, dah, dit, dit },
        {'\'',dit, dah, dah, dah, dah, dit },
        {'!', dah, dit, dah, dit, dah, dah },       //Exclamation mark 
        {'/', dah, dit, dit, dah, dit },            //Slash
        {'(', dah, dit, dah, dah, dit},
        {')', dah, dit, dah, dah, dit, dah },
        {';', dah, dit, dah, dit, dah, dit },       //Semicolon
        {'-', dah, dit, dit, dit, dit, dah },       //Hyphen, Minus 
        {'"', dit, dah, dit, dit, dah, dit },       //Quotation mark
        {'@', dit, dah, dah, dit, dah, dit },       //(=A+C)
        {'=', dah, dit, dit, dit, dah },            //Double dash
        {'+', dit, dah, dit, dah, dit },            //Plus 
        {'_', dit, dit, dah, dah, dit, dah },       //Underscore 
        {'$', dit, dit, dit, dah, dit, dit, dah },  //Dollar sign
        {'&', dit, dah, dit, dit, dit }             //Ampersand, Wait
};
    static final int[][] NON_LATIN =
    {
        {'Ä', dit, dah, dit, dah },
        {'Æ', dit, dah, dit, dah },
        {'Ą', dit, dah, dit, dah },
        {'È', dit, dah, dit, dit, dah },
        {'Ł', dit, dah, dit, dit, dah },
        {'Ñ', dah, dah, dit, dah, dah }, 
        {'Ń', dah, dah, dit, dah, dah },
        {'À', dit, dah, dah, dit, dah },
        {'Å', dit, dah, dah, dit, dah },
        {'É', dit, dit, dah, dit, dit },
        {'Đ', dit, dit, dah, dit, dit },
        {'Ę', dit, dit, dah, dit, dit },        
        {'Ö', dah, dah, dah, dit },
        {'Ø', dah, dah, dah, dit },
        {'Ó', dah, dah, dah, dit },
        {'Ç', dah, dit, dah, dit, dit },
        {'Ĉ', dah, dit, dah, dit, dit },
        {'Ć', dah, dit, dah, dit, dit },
        {'Ĝ', dah, dah, dit, dah, dit },	
        {'Ŝ', dit, dit, dit, dah, dit },	        
        //{'ch', dah, dah, dah, dah },
        {'Š', dah, dah, dah, dah },       
        {'Ĥ', dah, dah, dah, dah },	//obsolete version was – · – – · 
        {'Þ', dit, dah, dah, dit, dit }, // ("Thorn")	
        {'Ð', dit, dit, dah, dah, dit }, // ("Eth")
        {'Ĵ', dit, dah, dah, dah, dit },	
        {'Ü', dit, dit, dah, dah },	
        {'Ŭ', dit, dit, dah, dah },	
        {'Ś', dit, dit, dit, dah, dit, dit, dit },	
        {'Ź', dah, dah, dit, dit, dah, dit },	
        {'Ż', dah, dah, dit, dit, dah },
    };
    static final int[][] GREEK =
    {
        {'Α', dit, dah },             // A	
        {'Β', dah, dit, dit, dit },   // B	
        {'Δ', dah, dit, dit },        // D	
        {'Γ', dah, dah, dit },        // G
        {'Ε', dit },                  // E	
        {'Ζ', dah, dah, dit, dit },   // Z	
        {'Η', dit, dit, dit, dit },   // H	
        {'Θ', dah, dit, dah, dit },   // C	
        {'Ι', dit, dit },             // I	
        {'Κ', dah, dit, dah },        // K	
        {'Λ', dit, dah, dit, dit },   // L	
        {'Μ', dah, dah },             // M	
        {'Ν', dah, dit },             // N	
        {'Ξ', dah, dit, dit, dah, },  // X	
        {'Ο', dah, dah, dah, },       // O	
        {'Π', dit, dah, dah, dit },   // P	
        {'Ρ', dit, dah, dit },        // R	
        {'Σ', dit, dit, dit },        // S	
        {'Τ', dah },                  // T	
        {'Υ', dah, dit, dah, dah },   // Y	
        {'Φ', dit, dit, dah, dit },   // F	
        {'Χ', dah, dah, dah, dah },   // CH	
        {'Ψ', dah, dah, dit, dah },   // Q	
        {'Ω', dit, dah, dah },        // W	
    };
    static final int[][] CYRILLIC =
    {
//А	A	• −	Л
//Б	B	− • • •
//В	W	• − −
//Г	G	− − •
//Д	D	− • •
//Е	E	•
//Ж	V	• • • −
//З	Z	− − • •
//И	I	• •
//Й	J	• − − −
//К	K	− • −
//L	• − • •
//М	M	− −
//Н	N	− •
//О	O	− − −
//П	P	• − − •
//Р	R	• − •
//С	S	• • •
//Т	T	−
//У	U	• • −
//Ф	F	• • − •
//Х	H	• • • •
//Ц	C	− • − •
//Ч	Ö	− − − •
//Ш	CH	− − − −
//Щ	Q	− − • −
//Ь (Ъ)	X	− • • −
//Ы (Ь)	Y	− • − −
//Э	É	• • − • •
//Ю	Ü	• • − −
//Я	Ä	• − • −
    };
    static final int[][] HEBREW =
    {
//        א	A	•-	ל	L	•-••
//        ב	B	-•••	מ	M	--
//        ג	G	--•	נ	N	-•
//        ד	D	-••	ס	C	-•-•
//        ה	O	---	ע	J	•---
//        ו	E	•	פ	P	•--•
//        ז	Z	--••	צ	W	•--
//        ח	H	••••	ק	Q	--•-
//        ט	U	••-	ר	R	•-•
//        י	I	••	ש	S	•••
//        כ	K	-•-	ת	T	-
    };
    static final int[][] ARABIC =
    {
//ا	A	•-	د	D	-••	ض	V	•••-	ك	K	-•-	ﺀ	E	•
//ب	B	-•••	ذ	Z	--••	ط	U	••-	ل	L	•-••
//ت	T	-	ر	R	•-•	ظ	Y	-•--	م	M	--
//ث	C	-•-•	ز	Ö	---•	ع	Ä	•-•-	ن	N	-•
//ج	J	•---	س	S	•••	غ	G	--•	ه	É	••-••
//ح	H	••••	ش	SH	----	ف	F	••-•	و	W	•--
//خ	O	---	ص	X	-••-	ق	Q	--•-	ي	I	••
    };
    static final int[][] PERSIAN =
    {
//ا	A	•-	خ	X	-••-	ص	Ä	•-•-	ک	K	-•-
//ب	B	-•••	د	D	-••	ض	É	••-••	گ	Q	--•-
//پ	P	•--•	ذ	V	•••-	ط	U	••-	ل	L	•-••
//ت	T	-	ر	R	•-•	ظ	Y	-•--	م	M	--
//ث	C	-•-•	ز	Z	--••	ع	O	---	ن	N	-•
//ج	J	•---	ژ	G	--•	غ	Ü	••--	و	W	•--
//چ	Ö	---•	س	S	•••	ف	F	••-•	ه	E	•
//ح	H	••••	ش	Š	----	ق		•••---	ی	I	••
    };
    static final int[][] WABUN =
    {
//a ア	--•--	ka カ	•-••	sa サ	-•-•-	ta タ	-•	na ナ	•-•	ha ハ	-•••	ma マ	-••-	ya ヤ	•--	ra ラ	•••	wa ワ	-•-	Dakuten ◌゛	••
//i イ	•-	ki キ	-•-••	shiシ	--•-•	chiチ	••-•	ni ニ	-•-•	hi ヒ	--••-	mi ミ	••-•-			ri リ	--•	(wi ヰ)	•-••-	Handakuten ◌゜	••--•
//u ウ	••-	ku ク	•••-	su ス	---•-	tsuツ	•--•	nu ヌ	••••	fu フ	--••	mu ム	-	yu ユ	-••--	ru ル	-•--•	n ン	•-•-•	Long vowel ◌̄	•--•-
//e エ	-•---	ke ケ	-•--	se セ	•---•	te テ	•-•--	ne ネ	--•-	he ヘ	•	me メ	-•••-			re レ	---	(we ヱ)	•--••	Comma 、	•-•-•-
//o オ	•-•••	ko コ	----	so ソ	---•	to ト	••-••	no ノ	••--	ho ホ	-••	mo モ	-••-•	yo ヨ	--	ro ロ	•-•-	wo ヲ	•---	Full stop 。	•-•-••
    };

    static final String[] PROSIGNS =
    {
    };
//prosigns
//AA, New line di- dah- di- dah
//AS, Wait di- dah- di- di- dit
//BK, Break dah- di- di- di- dah- di- dah
//BT , New paragraph dah- di- di- di- dah
//CL, Going off the air (" clear" ) dah- di- dah- di- di- dah- di- dit
//CT, Start copying dah- di- dah- di- dah
//DO, Change to wabun code dah- di- di- dah- dah- dah
//KN, Invite a specific station to transmit dah- di- dah- dah- dit
    public static final String SK_END_OF_TRANSMISION = "SK";//SK, End of transmission di- di- di- dah- di- dah
//SN, Understood (also VE) di- di- di- dah- dit
    public static final String SOS = "SOS";                 //SOS, Distress message di- di- di- dah- dah- dah- di- di- dit

    public static final String AR_END_OF_MESSAGE = "AR";        //AR, End of message
//AS ----- Stand by
//BK ----- Invite receiving station to transmit
//BT ----- Pause; Break For Text
    public static final String KA_BEGINNING_OF_MESSAGE = "KA";  //KA ----- Beginning of message
//KN ----- end of transmission
//CL ----- Going off the air (clear)
//CQ ----- Calling any amateur radio station
//K ------ Go, invite any station to transmit
//KN ----- Go only, invite a specific station to transmit
//R ------ All received OK
//SK ----- End of contact (sent before call)
//VE ----- Understood (VE)         
        
//abreviaturas        
//AA - All after
//AB - All before
//ABT - About
//ADEE - Addressee
//ADR - Address
//AGN - Again
//AM - Amplitude Modulation
//ANT - Antenna
//BCI - Broadcast Interference
//BCL - Broadcast Listener
//BCNU - Be seeing you
//BK - Break, Break in
//BN - All between; Been
//BT - Separation (break) between addr & text; between txt & signature
//BTR - Better
//BUG - Semi-Automatic key
//B4 - Before 
//C - Yes, Correct
//CFM - Confirm; I confirm
//CK - Ckeck
//CKT - Circuit
//CL - I am closing my station; Call
//CLBK - Callbook
//CLD - Called
//CLG - Calling
//CNT - Can't
//CONDX - Conditions
//CQ - Calling any station
//CU - See You
//CUL - See You later
//CUM - Come
//CW - Continuous wave
//DA - Day
//DE - From, This Is
//DIFF - Difference
//DLD - Delivered
//DLVD - Delivered
//DN - Down
//DR - Dear
//EL - Element
//ES - AndDX - Distance
//ES - And FB - Fine Business, excellent
//FER - For 
//FM - Frequency Modulation: From 
//GA - Go ahead; Good Afternoon
//GB - Good bye, God Bless
//GD - Good
//GE - Good Evening
//GESS - Guess
//GG - Going
//GM - Good morning
//GN - Good night
//GND - Ground
//GUD - Good
//GV - Give
//GVG - Giving
    public static final String HH_ERROR_IN_SENDING = "HH";  //HH - Error in sending
//HI - The telegraph laugh; High
//HPE - Hope
//HQ - Headquarters
//HR - Here; Hear
//HV - Have
//HW - How, How Copy?
//IMI - Repeat, Say Again
//INFO - Info
//LID - A poor operator
//LNG - Long
//LTR - Later; letter
//LV - Leave
//LVG - Leaving
//MA - Millamperes
//MILL - Typewiter
//MILS - Millamperes
//MSG - Message; Prefix to radiogram
//N - No, Negative, Incorrect, No More
//NCS - Net Control Station
//ND - Nothing Doing NIL - Nothing; I have nothing for you
//NM - No more
//NR - Number
//NW - Now; I resume transmission
//
//OB - Old boy
//OC - Old chap
//OM - Old man
//OP - Operator
//OPR - Operator
//OT - Old timer; Old top
//PBL - Preamble
//PKG - Package
//PSE - Please
//PT - Point
//PWR - Power
//PX - Press
//R - Received as transmitted; Are; Decimal Point
//RC - Ragchew
//RCD - Received
//RCVR - Receiver
//RE - Concerning; Regarding
//REF - Refer to; Referring to; Reference
//RFI - Radio frequency interference
//RIG - Station equipment
//RPT - Repeat, Report
//RTTY - Radio teletype
//RST - Readability, strength, tone
//RX - Receive, Receiver
//SASE - Self-addressed, stamped envelope
//SED - Said
//SEZ - Says
//SGD - Signed
//SIG - Signature; Signal
//SINE - Operator's personal initials or nickname
//SKED - Schedule
//SRI - Sorry
//SS - Sweepstakes
//SSB - Single Side Band
//STN - Station
//SUM - Some
//SVC - Service; Prefix to service message
//T - Zero
//TFC - Traffic
//TMW - Tomorrow
//TKS - Thanks
//TNX - Thanks
//TR - Transmit
//T/R - Transmit/Receive
//TRIX - Tricks
//TT - That
//TTS - That is
//TU - Thank you
//TVI - Television interference
//TX - Transmitter; Transmit
//TXT - Text
//U - You
//UR - Your; You're
//URS - Yours
//VFB - Very fune business
//VFO - Variable Frequency Oscillator
//VY - Very
//W - Watts
//WA - Word after
//WB - Word before
//WD - Word
//WDS - Words
//WID - With
//WKD - Worked
//WKG - Working
//WL - Well; Will
//WPM - Words Per Minute
//WRD - Word
//WUD - Would
//WX- Weather
//XCVR - Transceiver
//XMTR - Transmitter
//XTAL - Crystal
//XYL - Wife
//YL - Young lady
//YR - Year
//30 - I have no more to send
//73 - Best Regards
//88 - Love and kisses
//161 - 73+88=161        
            
//O t he r Phrase s Ab b re viat io n
//Over K
//Roger R
//See you later CUL
//Be seeing you BCNU
//You're UR
//Signal report RST
//Best regards 73
//Love and kisses 88

//QSL I acknowledge receipt
//QSL? Do you acknowledge?
//QRX Wait
//QRX? Should I wait?
//QRV I am ready to copy
//QRV? Are you ready to copy?
//QRL The frequency is in use
//QRL? Is the frequency in use?
//QTH My location is...
//QTH? What is your location?
    
    static private int[] inflate(int[] morse)
    {
        int num = morse.length-1;
        int[] code = new int[num*2-1];

        //fill gaps
        for(int i=1;i<code.length;i+=2)
        {
            code[i]=gap;
        }
        //fill dits and dah
        for(int i=0,j=1;i<code.length;i+=2,j++)
        {
            code[i]=morse[j];
        }
        return code;
    }
    
    public static int WPM_TO_CPM = 5;

    public static final int FLAG_EXTRA    = 1;
    public static final int FLAG_NONLATIN = 2;
    public static final int FLAG_GREEK    = 4;
    public static final int FLAG_WG5U     = 8; //word gap 5 units (default 7)

    public static final int FLAG_ALL_CHARS = FLAG_EXTRA | FLAG_NONLATIN | FLAG_GREEK;

    private final HashMap<Integer,int[]> map = new HashMap<>();
    private final int[] units;
    private volatile int cbase;
    private volatile int sbase;
    private final int errorCodePoint = 0;

    public MorseCode()
    {
        this(12, 12, FLAG_ALL_CHARS);
    }
    public MorseCode(int wpm, int ewpm)
    {
        this(wpm, ewpm, FLAG_ALL_CHARS);

    }
    private MorseCode(int wpm, int ewpm, int flags)
    {
        boolean extra    = (flags & FLAG_EXTRA)    == FLAG_EXTRA;
        boolean nonlatin = (flags & FLAG_NONLATIN) == FLAG_NONLATIN;
        boolean greek    = (flags & FLAG_GREEK)    == FLAG_GREEK;
        boolean wg5u     = (flags & FLAG_WG5U)     == FLAG_WG5U;

        //---------------------------------------------------------

        //wpm must be as fast as ewpm
        wpm = Math.max(wpm,ewpm);

        //PARIS rule and Farnsworth method
        int c = 1200/wpm;       // unit of time used for characters
        int u = 1200/ewpm;      // efective or average unit of time
        int s = (u*50-c*31)/19; // unit of time used for spaces

        this.cbase = c;
        this.sbase = s;

        //---------------------------------------------------------

        this.units =  wg5u ? units5 : units7;
        
        for(int[] item : LETTERS)
        {
            map.put(item[0], inflate(item));
        }
        if(extra)
        {
            for(int[] item : LETTERS_EXTRA)
            {
                map.put(item[0], inflate(item));
            }
        }
        for(int[] item : NUMBERS)
        {
            map.put(item[0], inflate(item));
        }
        for(int[] item : PUNCTUATION)
        {
            map.put(item[0], inflate(item));
        }
        if(nonlatin)
        {
            for(int[] item : NON_LATIN)
            {
                map.put(item[0], inflate(item));
            }
        }
        if(greek)
        {
            for(int[] item : GREEK)
            {
                map.put(item[0], inflate(item));
            }
        }
    }
    
    static final int[] EMPTY = new int[0];
    private int[] pattern(int c) 
    {
        int[] patt = map.get(Character.toUpperCase(c));
        if(patt==null)
        {
            if(errorCodePoint==0 && (patt=map.get(errorCodePoint))==null )
            {
                patt = EMPTY;
            }
        }
        return patt;
    }
    private int[] patternMillis(int c)
    {
        int[] p = pattern(c);
        int[] ms =  new int[p.length];
        for(int i=0;i<ms.length;i++)
        {
            ms[i]=units[p[i]]*cbase;
        }
        return ms;
    }
    private int patternSize(int c)
    {
        return pattern(c).length;
    }
    public int[] pattern(String s, boolean endWithWordGap)
    {
        final int lgap = units[gap_letter]* sbase;
        final int wgap = units[gap_word]  * sbase;
        
        boolean lastWasWhitespace=true;
        
        int count=2;
        int num = s.codePointCount(0, s.length());
        
        //calculate the size of the array
        for(int i=0;i<num;i++)
        {
            int codePoint = s.codePointAt(i);
            boolean whiteSpace = Character.isWhitespace(codePoint);
            
            if (whiteSpace) 
            {
                if (!lastWasWhitespace) 
                {
                    count++;
                    lastWasWhitespace = true;
                }
                continue;
            } 

            int size = patternSize(codePoint);
            if(size>0)
            {
                if (!lastWasWhitespace) 
                {
                    count++;
                }
                lastWasWhitespace = false;
                count += size;
            }
        }

        if(endWithWordGap && !lastWasWhitespace)
        {
            count++;
        }

        int[] patt = new int[count];
        patt[0] = 0;
        int index = 1;
        lastWasWhitespace = true;
        for (int i=0; i<num; i++) 
        {
            int codePoint = s.codePointAt(i);
            boolean whiteSpace = Character.isWhitespace(codePoint);
            
            if(whiteSpace) 
            {
                if (!lastWasWhitespace) 
                {
                    patt[index++] = wgap;
                    lastWasWhitespace = true;
                }
                continue;
            } 
            
            int[] letter = patternMillis(codePoint);
            if(letter.length>0)
            {
                if (!lastWasWhitespace) 
                {
                    patt[index++] = lgap;
                }
                lastWasWhitespace = false;

                System.arraycopy(letter, 0, patt, index, letter.length);
                index += letter.length;
            }
        }

        if(endWithWordGap && !lastWasWhitespace)
        {
            patt[index++]=wgap;
        }

        patt[index++]=0;
        assert(count==index);
        return patt;
   }
    public int[] pattern(String s)
    {
        return this.pattern(s, false);
    }

    public int[] prosign(String s, boolean endWithWordGap)
    {
        final int wgap = units[gap_word] * sbase;

        int count=1;
        int size = s.length();
        int[][] pattern = new int[size][];
        for(int i=0;i<size;i++)
        {
            pattern[i] = pattern(s.substring(i, i+1), false);
            count+=pattern[i].length-1;
        }
        int[] merged = new int[count];

        int k=1;
        for(int i=0;i<size;i++)
        {
            for(int u=1;u<pattern[i].length-1;u++)
            {
                merged[k++] = pattern[i][u];
            }
            merged[k++] = (i+1==size) ? (endWithWordGap ? wgap : 0) : sbase;
        }
        return merged;
    }

/////////////////////////////////////////////////////////////////////
}