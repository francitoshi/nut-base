/*
 *  OrdinalFormatTest.java
 *
 *  Copyright (C) 2014-2023 francitoshi@gmail.com
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

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
/**
 *
 * @author franci
 */
public class OrdinalFormatTest
{
    static class T
    {
        final long num;
        final String text;

        public T(long num, String text)
        {
            this.num = num;
            this.text = text;
        }
    }
    static private T __(long num, String text)
    {
        return new T(num,text);
    }
    private static final T[] de_DE = 
    {
        __(1, "erste"),
        __(2, "zweite"),
        __(3, "dritte"),
        __(4 ,"vierte"),
        __(5, "fünfte"),
        __(6, "sechste"),
        __(7, "siebte"),
        __(8, "achte"),
        __(9, "neunte"),
        __(10, "zehnte"),
        __(11, "elfte"),
        __(12, "zwölfte"),
        __(13, "dreizehnte"),
        __(14, "vierzehnte"),
        __(15, "fünfzehnte"),
        __(16, "sechzehnte"),
        __(17, "siebzehnte"),
        __(18, "achtzehnte"),
        __(19, "neunzehnte"),
        __(20, "zwanzigste"),
        __(21, "einundzwanzigste"),
        __(22, "zweiundzwanzigste"),
        __(23, "dreiundzwanzigste"),
        __(24, "vierundzwanzigste"),
        __(26, "sechsundzwanzigste"),
        __(28, "achtundzwanzigste"),
        __(29, "neunundzwanzigste"),
        __(30, "dreißigste"),
        __(31, "einunddreißigste"),
        __(32, "zweiunddreißigste"),
        __(37, "siebenunddreißigste"),
        __(40, "vierzigste"),
        __(41, "einundvierzigste"),
        __(43, "dreiundvierzigste"),
        __(50, "fünfzigste"),
        __(51, "einundfünfzigste"),
        __(55, "fünfundfünfzigste"),
        __(60, "sechzigste"),
        __(61, "einundsechzigste"),
        __(62, "zweiundsechzigste"),
        __(67, "siebenundsechzigste"),
        __(70, "siebzigste"),
        __(78, "achtundsiebzigste"),
        __(79, "neunundsiebzigste"),
        __(89, "neunundachtzigste"),
        __(91, "einundneunzigste"),
        __(100, "einhundertste"),
        __(101, "einhunderterste"),
        __(121, "einhunderteinundzwanzigste"),
        __(124, "einhundertvierundzwanzigste"),
        __(199, "einhundertneunundneunzigste"),
        __(203, "zweihundertdreite"),
        __(221, "zweihunderteinundzwanzigste"),
        __(237, "zweihundertsiebenunddreißigste"),
        __(287, "zweihundertsiebenundachtzigste"),
        __(300, "dreihundertste"),
        __(321, "dreihunderteinundzwanzigste"),
        __(343, "dreihundertdreiundvierzigste"),
        __(356, "dreihundertsechsundfünfzigste"),
        __(410, "vierhundertzehnte"),
        __(434, "vierhundertvierunddreißigste"),
        __(578, "fünfhundertachtundsiebzigste"),
        __(689, "sechshundertneunundachtzigste"),
        __(729, "siebenhundertneunundzwanzigste"),
        __(894, "achthundertvierundneunzigste"),
        __(999, "neunhundertneunundneunzigste"),
        __(1000, "eintausendste"),
        __(1001, "eintausenderste"),
        __(1097, "eintausendsiebenundneunzigste"),
        __(1104, "eintausendeinhundertvierte"),
        __(1243, "eintausendzweihundertdreiundvierzigste"),
        __(1255, "eintausendzweihundertfünfundfünfzigste"),
        __(1321, "eintausenddreihunderteinundzwanzigste"),
        __(2000, "zweitausendste"),
        __(2321, "zweitausenddreihunderteinundzwanzigste"),
        __(2385, "zweitausenddreihundertfünfundachtzigste"),
        __(3321, "dreitausenddreihunderteinundzwanzigste"),
        __(3766, "dreitausendsiebenhundertsechsundsechzigste"),
        __(4196, "viertausendeinhundertsechsundneunzigste"),
        __(4321, "viertausenddreihunderteinundzwanzigste"),
        __(5846, "fünftausendachthundertsechsundvierzigste"),
        __(6459, "sechstausendvierhundertneunundfünfzigste"),
        __(7232, "siebentausendzweihundertzweiunddreißigste"),
        __(8569, "achttausendfünfhundertneunundsechzigste"),
        __(9539, "neuntausendfünfhundertneununddreißigste"),
        __(10000, "zehntausendste"),
        __(10221, "zehntausendzweihunderteinundzwanzigste"),
        __(14321, "vierzehntausenddreihunderteinundzwanzigste"),
        __(54321, "vierundfünfzigtausenddreihunderteinundzwanzigste"),
        __(100000, "einhunderttausendste"),
        __(102321, "einhundertzweitausenddreihunderteinundzwanzigste"),
        __(1000000, "eine Millionste"),
        __(1023321, "eine Million dreiundzwanzigtausenddreihunderteinundzwanzigste"),
        __(10000000, "zehn Millionenste"),
        __(10234321, "zehn Millionen zweihundertvierunddreißigtausenddreihunderteinundzwanzigste"),
        __(100000000, "einhundert Millionenste"),
        __(102344321, "einhundertzwei Millionen dreihundertvierundvierzigtausenddreihunderteinundzwanzigste"),
        __(1000000000, "eine Milliardeste"),
        __(1023454321, "eine Milliarde dreiundzwanzig Millionen vierhundertvierundfünfzigtausenddreihunderteinundzwanzigste"),
        __(10000000000L, "zehn Milliarden"),
        __(10234554321L, "zehn Milliarden zweihundertvierunddreißig Millionen fünfhundertvierundfünfzigtausenddreihunderteinundzwanzigste"),
        __(100000000000L, "einhundert Milliarden"),
        __(102345654321L, "einhundertzwei Milliarden dreihundertfünfundvierzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzigste"),
        __(1000000000000L, "eine Billion"), 
        __(1023456654321L, "eine Billion dreiundzwanzig Milliarden vierhundertsechsundfünfzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(10000000000000L, "zehn Billionen"),
        __(10234567654321L, "zehn Billionen zweihundertvierunddreißig Milliarden fünfhundertsiebenundsechzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(100000000000000L, "einhundert Billionen"),
        __(102345677654321L, "einhundertzwei Billionen dreihundertfünfundvierzig Milliarden sechshundertsiebenundsiebzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(1000000000000000L, "eine Billiarde"),
        __(1023456787654321L, "eine Billiarde dreiundzwanzig Billionen vierhundertsechsundfünfzig Milliarden siebenhundertsiebenundachtzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(10000000000000000L, "zehn Billiarden"),
        __(10234567887654321L, "zehn Billiarden zweihundertvierunddreißig Billionen fünfhundertsiebenundsechzig Milliarden achthundertsiebenundachtzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(100000000000000000L, "einhundert Billiarden"),
        __(102345678987654321L, "einhundertzwei Billiarden dreihundertfünfundvierzig Billionen sechshundertachtundsiebzig Milliarden neunhundertsiebenundachtzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(1000000000000000000L, "eine Trillion"),
        __(1023456789987654321L, "eine Trillion dreiundzwanzig Billiarden vierhundertsechsundfünfzig Billionen siebenhundertneunundachtzig Milliarden neunhundertsiebenundachtzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(9223372036854775807L ,"neun Trillionen zweihundertdreiundzwanzig Billiarden dreihundertzweiundsiebzig Billionen sechsunddreißig Milliarden achthundertvierundfünfzig Millionen siebenhundertfünfundsiebzigtausendachthundertsieben")
    };
    
    // some test have been tested through these webs
    //http://www.tools4noobs.com/online_tools/number_spell_words/
    //http://www.mathcats.com/explore/reallybignumbers.html
    //http://en.wikipedia.org/wiki/List_of_numbers
    private static final T[] es_ES = 
    {
        __(0, "cero"),
        __(1, "primer"),
        __(2, "segundo"),
        __(3, "tercer"),
        __(4 ,"cuarto"),
        __(5, "quinto"),
        __(6, "sexto"),
        __(7, "séptimo"),
        __(8, "octavo"),
        __(9, "noveno"),
        __(10, "décimo"),
        __(11, "undécimo"),
        __(12, "duodécimo"),
        __(13, "decimotercer"),
        __(14, "decimocuarto"),
        __(15, "decimoquinto"),
        __(16, "decimosexto"),
        __(17, "decimoséptimo"),
        __(18, "decimoctavo"),
        __(19, "decimonoveno"),
        __(20, "vigésimo"),
        __(30, "trigésimo"),
        __(40, "cuadragésimo"),
        __(50, "quincuagésimo"),
        __(60, "sexagésimo"),
        __(70, "septuagésimo"),
        __(80, "octogésimo"),
        __(90, "nonagésimo"),
        __(21, "vigésimo primer"),
        __(29, "vigésimo noveno"),
        __(38, "trigésimo octavo"),
        __(47, "cuadragésimo séptimo"),
        __(100, "centésimo"),
        __(200, "ducentésimo"),
        __(300, "tricentésimo"),
        __(400, "cuadringentésimo"),
        __(500, "quingentésimo"),
        __(600, "sexcentésimo"),
        __(700, "septingentésimo"),
        __(800, "octingentésimo"),
        __(900, "noningentésimo"),
        __(101, "centésimo primer"),
        __(212, "ducentésimo duodécimo"),
        __(1000, "milésimo"),
    };
    private static final T[] es_ES_male = 
    {
        __(1, "primero"),
        __(2, "segundo"),
        __(3, "tercero"),
        __(21, "vigésimo primero"),
    };
    private static final T[] es_ES_female = 
    {
        __(1, "primera"),
        __(2, "segunda"),
        __(3, "tercera"),
        __(21, "vigésimo primera"),
        __(43, "cuadragésimo tercera"),
    };    
    private static final T[] en_US = 
    {
        __(0, "zero"),
        __(1, "first"),
        __(2, "second"),
        __(3, "third"),
        __(4, "fourth"),
        __(5, "fifth"),
        __(6, "sixth"),
        __(7, "seventh"),
        __(8, "eighth"),
        __(9, "ninth"),
        __(10, "tenth"),
        __(11, "eleventh"),
        __(12, "twelfth"),
        __(13, "thirteenth"),
        __(14, "fourteenth"),
        __(15, "fifteenth"),
        __(16, "sixteenth"),
        __(17, "seventeenth"),
        __(18, "eighteenth"),
        __(19, "nineteenth"),
        __(20, "twentieth"),
        __(30, "thirtieth"),
        __(40, "fortieth"),
        __(50, "fiftieth"),
        __(60, "sixtieth"),
        __(70, "seventieth"),
        __(80, "eightieth"),
        __(90, "ninetieth"),
        __(21, "twenty-first"),
        __(25, "twenty-fifth"),
        __(32, "thirty-second"),
        __(58, "fifty-eighth"),
        __(64, "sixty-fourth"),
        __(79, "seventy-ninth"),
        __(83, "eighty-third"),
        __(99, "ninety-ninth"),
    };
           
   
    public OrdinalFormatTest()
    {
    }

    @BeforeAll
    public static void setUpClass() throws Exception
    {
        
    }

    @AfterAll
    public static void tearDownClass() throws Exception
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

    /**
     * Test of format method, of class OrdinalFormat.
     */
    @Test
    public void testFormat()
    {
        OrdinalFormat instance;
        
        instance = OrdinalFormat.getInstance(new Locale("",""));
        for(int i=0;i<999;i++)
        {
            assertEquals(Long.toString(i),Long.toString(i),instance.format(i));
        }
        instance = OrdinalFormat.getInstance(new Locale("es","ES"), OrdinalFormat.Gender.NEUTRAL);
        for(T e: es_ES)
        {
            assertEquals(e.text, instance.format(e.num), Long.toString(e.num));
        }
        instance = OrdinalFormat.getInstance(new Locale("es","ES"), OrdinalFormat.Gender.MALE);
        for(T e: es_ES_male)
        {
            assertEquals(e.text, instance.format(e.num), Long.toString(e.num));
        }
        instance = OrdinalFormat.getInstance(new Locale("es","ES"), OrdinalFormat.Gender.FEMALE);
        for(T e: es_ES_female)
        {
            assertEquals(e.text, instance.format(e.num), Long.toString(e.num));
        }
        instance = OrdinalFormat.getInstance(Locale.US);
        for(T e: en_US)
        {
            assertEquals(e.text, instance.format(e.num), Long.toString(e.num));
        }
        instance = OrdinalFormat.getInstance(new Locale("de","DE"), OrdinalFormat.Gender.FEMALE);
        for(T e: de_DE)
        {
            assertEquals(e.text, instance.format(e.num), Long.toString(e.num));
        }
    }
}
