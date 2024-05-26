/*
 *  CardinalFormatTest.java
 *
 *  Copyright (C) 2011-2024 francitoshi@gmail.com
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
public class CardinalFormatTest
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
    // some test have been tested through these webs
    //http://www.tools4noobs.com/online_tools/number_spell_words/
    //http://www.mathcats.com/explore/reallybignumbers.html
    //http://en.wikipedia.org/wiki/List_of_numbers
    private static final T[] es_ES = 
    {
        __(0, "cero"),
        __(1, "uno"),
        __(2, "dos"),
        __(3, "tres"),
        __(4 , "cuatro"),
        __(5, "cinco"),
        __(6, "seis"),
        __(7, "siete"),
        __(8, "ocho"),
        __(9, "nueve"),
        __(10, "diez"),
        __(11, "once"),
        __(12, "doce"),
        __(13, "trece"),
        __(14, "catorce"),
        __(15, "quince"),
        __(16, "dieciseis"),
        __(17, "diecisiete"),
        __(18, "dieciocho"),
        __(19, "diecinueve"),
        __(20, "veinte"),
        __(30, "treinta"),
        __(40, "cuarenta"),
        __(50, "cincuenta"),
        __(60, "sesenta"),
        __(70, "setenta"),
        __(80, "ochenta"),
        __(90, "noventa"),
        __(29, "veintinueve"),
        __(38, "treinta y ocho"),
        __(47, "cuarenta y siete"),
        __(56, "cincuenta y seis"),
        __(65, "sesenta y cinco"),
        __(74, "setenta y cuatro"),
        __(83, "ochenta y tres"),
        __(92, "noventa y dos"),
        __(100, "cien"),
        __(200, "doscientos"),
        __(300, "trescientos"),
        __(400, "cuatrocientos"),
        __(500, "quinientos"),
        __(600, "seiscientos"),
        __(700, "setecientos"),
        __(800, "ochocientos"),
        __(900, "novecientos"),
        __(101, "ciento uno"),
        __(212, "doscientos doce"),
        __(323, "trescientos veintitres"),
        __(434, "cuatrocientos treinta y cuatro"),
        __(545, "quinientos cuarenta y cinco"),
        __(656, "seiscientos cincuenta y seis"),
        __(767, "setecientos sesenta y siete"),
        __(878, "ochocientos setenta y ocho"),
        __(989, "novecientos ochenta y nueve"),
        __(1000, "mil"),
        __(1001, "mil uno"),
        __(1012, "mil doce"),
        __(1123, "mil ciento veintitres"),
        __(1234, "mil doscientos treinta y cuatro"),
        __(2345, "dos mil trescientos cuarenta y cinco"),
        __(3456, "tres mil cuatrocientos cincuenta y seis"),
        __(4567, "cuatro mil quinientos sesenta y siete"),
        __(5678, "cinco mil seiscientos setenta y ocho"),
        __(6789, "seis mil setecientos ochenta y nueve"),
        __(7890, "siete mil ochocientos noventa"),
        __(8901, "ocho mil novecientos uno"),
        __(9012, "nueve mil doce"),
        __(10000, "diez mil"),
        __(10221, "diez mil doscientos veintiuno"),
        __(100000,  "cien mil"),
        __(102321, "ciento dos mil trescientos veintiuno"),
        __(1000000, "un millón"),
        __(1023321, "un millón veintitres mil trescientos veintiuno"),
        __(10000000, "diez millones"),
        __(10234321, "diez millones doscientos treinta y cuatro mil trescientos veintiuno"),
        __(100000000, "cien millones"),
        __(102344321, "ciento dos millones trescientos cuarenta y cuatro mil trescientos veintiuno"),
        __(1000000000,"mil millones"),
        __(1023454321, "mil veintitres millones cuatrocientos cincuenta y cuatro mil trescientos veintiuno"),
        __(10000000000L, "diez mil millones"),
        __(10234554321L, "diez mil doscientos treinta y cuatro millones quinientos cincuenta y cuatro mil trescientos veintiuno"),
        __(100000000000L, "cien mil millones"),
        __(102345654321L, "ciento dos mil trescientos cuarenta y cinco millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(1000000000000L, "un billón"), 
        __(1023456654321L, "un billón veintitres mil cuatrocientos cincuenta y seis millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(10000000000000L, "diez billones"),
        __(10234567654321L, "diez billones doscientos treinta y cuatro mil quinientos sesenta y siete millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(100000000000000L, "cien billones"),
        __(102345677654321L, "ciento dos billones trescientos cuarenta y cinco mil seiscientos setenta y siete millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(1000000000000000L, "mil billones"),
        __(1023456787654321L, "mil veintitres billones cuatrocientos cincuenta y seis mil setecientos ochenta y siete millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(10000000000000000L, "diez mil billones"),
        __(10234567887654321L, "diez mil doscientos treinta y cuatro billones quinientos sesenta y siete mil ochocientos ochenta y siete millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(100000000000000000L, "cien mil billones"),
        __(102345678987654321L, "ciento dos mil trescientos cuarenta y cinco billones seiscientos setenta y ocho mil novecientos ochenta y siete millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(1000000000000000000L, "un trillón"),
        __(1023456789987654321L, "un trillón veintitres mil cuatrocientos cincuenta y seis billones setecientos ochenta y nueve mil novecientos ochenta y siete millones seiscientos cincuenta y cuatro mil trescientos veintiuno"),
        __(9223372036854775807L ,"nueve trillones doscientos veintitres mil trescientos setenta y dos billones treinta y seis mil ochocientos cincuenta y cuatro millones setecientos setenta y cinco mil ochocientos siete")
    };
    private static final T[] en_US = 
    {
        __(0, "zero"),
        __(1, "one"),
        __(2, "two"),
        __(3, "three"),
        __(4, "four"),
        __(5, "five"),
        __(6, "six"),
        __(7, "seven"),
        __(8, "eight"),
        __(9, "nine"),
        __(10, "ten"),
        __(11, "eleven"),
        __(12, "twelve"),
        __(13, "thirteen"),
        __(14, "fourteen"),
        __(15, "fifteen"),
        __(16, "sixteen"),
        __(17, "seventeen"),
        __(18, "eighteen"),
        __(19, "nineteen"),
        __(20, "twenty"),
        __(30, "thirty"),
        __(40, "forty"),
        __(50, "fifty"),
        __(60, "sixty"),
        __(70, "seventy"),
        __(80, "eighty"),
        __(90, "ninety"),
        __(29, "twenty-nine"),
        __(38, "thirty-eight"),
        __(47, "forty-seven"),
        __(56, "fifty-six"),
        __(65, "sixty-five"),
        __(74, "seventy-four"),
        __(83, "eighty-three"),
        __(92, "ninety-two"),
        __(100, "one hundred"),
        __(200, "two hundred"),
        __(300, "three hundred"),
        __(400, "four hundred"),
        __(500, "five hundred"),
        __(600, "six hundred"),
        __(700, "seven hundred"),
        __(800, "eight hundred"),
        __(900, "nine hundred"),
        __(101, "one hundred one"),
        __(212, "two hundred twelve"),
        __(323, "three hundred twenty-three"),
        __(434, "four hundred thirty-four"),
        __(545, "five hundred forty-five"),
        __(656, "six hundred fifty-six"),
        __(767, "seven hundred sixty-seven"),
        __(878, "eight hundred seventy-eight"),
        __(989, "nine hundred eighty-nine"),
        __(1000, "one thousand"),
        __(1001, "one thousand one"),
        __(1012, "one thousand twelve"),
        __(1123, "one thousand one hundred twenty-three"),
        __(1234, "one thousand two hundred thirty-four"),
        __(2345, "two thousand three hundred forty-five"),
        __(3456, "three thousand four hundred fifty-six"),
        __(4567, "four thousand five hundred sixty-seven"),
        __(5678, "five thousand six hundred seventy-eight"),
        __(6789, "six thousand seven hundred eighty-nine"),
        __(7890, "seven thousand eight hundred ninety"),
        __(8901, "eight thousand nine hundred one"),
        __(9012, "nine thousand twelve"),
        __(10000, "ten thousand"),
        __(10221, "ten thousand two hundred twenty-one"),
        __(100000, "one hundred thousand"),
        __(102321, "one hundred two thousand three hundred twenty-one"),
        __(1000000, "one million"),
        __(1023321, "one million twenty-three thousand three hundred twenty-one"),
        __(10000000, "ten million"),
        __(10234321, "ten million two hundred thirty-four thousand three hundred twenty-one"),
        __(100000000, "one hundred million"),
        __(102344321, "one hundred two million three hundred forty-four thousand three hundred twenty-one"),
        __(1000000000, "one billion"),
        __(1023454321, "one billion twenty-three million four hundred fifty-four thousand three hundred twenty-one"),
        __(10000000000L, "ten billion"),
        __(10234554321L, "ten billion two hundred thirty-four million five hundred fifty-four thousand three hundred twenty-one"),
        __(100000000000L, "one hundred billion"),
        __(102345654321L, "one hundred two billion three hundred forty-five million six hundred fifty-four thousand three hundred twenty-one"),
        __(1000000000000L, "one trillion"), 
        __(1023456654321L, "one trillion twenty-three billion four hundred fifty-six million six hundred fifty-four thousand three hundred twenty-one"),
        __(10000000000000L, "ten trillion"),
        __(10234567654321L, "ten trillion two hundred thirty-four billion five hundred sixty-seven million six hundred fifty-four thousand three hundred twenty-one"),
        __(100000000000000L, "one hundred trillion"),
        __(102345677654321L, "one hundred two trillion three hundred forty-five billion six hundred seventy-seven million six hundred fifty-four thousand three hundred twenty-one"),
        __(1000000000000000L, "one quadrillion"),
        __(1023456787654321L, "one quadrillion twenty-three trillion four hundred fifty-six billion seven hundred eighty-seven million six hundred fifty-four thousand three hundred twenty-one"),
        __(10000000000000000L, "ten quadrillion"),
        __(10234567887654321L, "ten quadrillion two hundred thirty-four trillion five hundred sixty-seven billion eight hundred eighty-seven million six hundred fifty-four thousand three hundred twenty-one"),
        __(100000000000000000L, "one hundred quadrillion"),
        __(102345678987654321L, "one hundred two quadrillion three hundred forty-five trillion six hundred seventy-eight billion nine hundred eighty-seven million six hundred fifty-four thousand three hundred twenty-one"),
        __(1000000000000000000L, "one quintillion"),
        __(1023456789987654321L, "one quintillion twenty-three quadrillion four hundred fifty-six trillion seven hundred eighty-nine billion nine hundred eighty-seven million six hundred fifty-four thousand three hundred twenty-one"),
        __(9223372036854775807L ,"nine quintillion two hundred twenty-three quadrillion three hundred seventy-two trillion thirty-six billion eight hundred fifty-four million seven hundred seventy-five thousand eight hundred seven")
    };
    static private final T[] en_GB =
    {
        __(101, "one hundred and one"),
        __(201, "two hundred and one"),
        __(212, "two hundred and twelve"),
        __(323, "three hundred and twenty-three"),
        __(434, "four hundred and thirty-four"),
        __(545, "five hundred and forty-five"),
        __(656, "six hundred and fifty-six"),
        __(767, "seven hundred and sixty-seven"),
        __(878, "eight hundred and seventy-eight"),
        __(989, "nine hundred and eighty-nine"),
        __(1001, "one thousand and one"),
        __(1012, "one thousand and twelve"),
        __(1123, "one thousand and one hundred and twenty-three"),
        __(1234, "one thousand and two hundred and thirty-four")
    };
    private static final T[] de_DE = 
    {
        __(0, "null"),
        __(1, "eins"),
        __(2, "zwei"),
        __(3, "drei"),
        __(4, "vier"),
        __(5, "fünf"),
        __(6, "sechs"),
        __(7, "sieben"),
        __(8, "acht"),
        __(9, "neun"),
        __(11, "elf"),
        __(12, "zwölf"),
        __(16, "sechzehn"),
        __(19, "neunzehn"),
        __(20, "zwanzig"),
        __(21, "einundzwanzig"),
        __(26, "sechsundzwanzig"),
        __(30, "dreißig"),
        __(31, "einunddreißig"),
        __(37, "siebenunddreißig"),
        __(40, "vierzig"),
        __(41, "einundvierzig"),
        __(43, "dreiundvierzig"),
        __(50, "fünfzig"),
        __(51, "einundfünfzig"),
        __(55, "fünfundfünfzig"),
        __(60, "sechzig"),
        __(61, "einundsechzig"),
        __(67, "siebenundsechzig"),
        __(70, "siebzig"),
        __(79, "neunundsiebzig"),
        __(100, "einhundert"),
        __(101, "einhunderteins"),
        __(121, "einhunderteinundzwanzig"),
        __(199, "einhundertneunundneunzig"),
        __(203, "zweihundertdrei"),
        __(221, "zweihunderteinundzwanzig"),
        __(287, "zweihundertsiebenundachtzig"),
        __(300, "dreihundert"),
        __(321, "dreihunderteinundzwanzig"),
        __(356, "dreihundertsechsundfünfzig"),
        __(410, "vierhundertzehn"),
        __(434, "vierhundertvierunddreißig"),
        __(578, "fünfhundertachtundsiebzig"),
        __(689, "sechshundertneunundachtzig"),
        __(729, "siebenhundertneunundzwanzig"),
        __(894, "achthundertvierundneunzig"),
        __(999, "neunhundertneunundneunzig"),
        __(1000, "eintausend"),
        __(1001, "eintausendeins"),
        __(1097, "eintausendsiebenundneunzig"),
        __(1104, "eintausendeinhundertvier"),
        __(1243, "eintausendzweihundertdreiundvierzig"),
        __(1321, "eintausenddreihunderteinundzwanzig"),
        __(2321, "zweitausenddreihunderteinundzwanzig"),
        __(2385, "zweitausenddreihundertfünfundachtzig"),
        __(3321, "dreitausenddreihunderteinundzwanzig"),
        __(3766, "dreitausendsiebenhundertsechsundsechzig"),
        __(4196, "viertausendeinhundertsechsundneunzig"),
        __(4321, "viertausenddreihunderteinundzwanzig"),
        __(5846, "fünftausendachthundertsechsundvierzig"),
        __(6459, "sechstausendvierhundertneunundfünfzig"),
        __(7232, "siebentausendzweihundertzweiunddreißig"),
        __(8569, "achttausendfünfhundertneunundsechzig"),
        __(9539, "neuntausendfünfhundertneununddreißig"),
        __(10000, "zehntausend"),
        __(10221, "zehntausendzweihunderteinundzwanzig"),
        __(14321, "vierzehntausenddreihunderteinundzwanzig"),
        __(54321, "vierundfünfzigtausenddreihunderteinundzwanzig"),
        __(100000, "einhunderttausend"),
        __(102321, "einhundertzweitausenddreihunderteinundzwanzig"),
        __(1000000, "eine Million"),
        __(1023321, "eine Million dreiundzwanzigtausenddreihunderteinundzwanzig"),
        __(10000000, "zehn Millionen"),
        __(10234321, "zehn Millionen zweihundertvierunddreißigtausenddreihunderteinundzwanzig"),
        __(100000000, "einhundert Millionen"),
        __(102344321, "einhundertzwei Millionen dreihundertvierundvierzigtausenddreihunderteinundzwanzig"),
        __(1000000000, "eine Milliarde"),
        __(1023454321, "eine Milliarde dreiundzwanzig Millionen vierhundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(10000000000L, "zehn Milliarden"),
        __(10234554321L, "zehn Milliarden zweihundertvierunddreißig Millionen fünfhundertvierundfünfzigtausenddreihunderteinundzwanzig"),
        __(100000000000L, "einhundert Milliarden"),
        __(102345654321L, "einhundertzwei Milliarden dreihundertfünfundvierzig Millionen sechshundertvierundfünfzigtausenddreihunderteinundzwanzig"),
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
            
    //http://www.legionxxiv.org/numerals/
    private static final T[] la =
    {
        __(1, "I"),
        __(2, "II"),
        __(3, "III"),
        __(4, "IV"),
        __(5, "V"),
        __(6, "VI"),
        __(7, "VII"),
        __(8, "VIII"),
        __(9, "IX"),
        __(10, "X"),
        __(11, "XI"),
        __(12, "XII"),
        __(13, "XIII"),
        __(14, "XIV"),
        __(15, "XV"),
        __(16, "XVI"),
        __(17, "XVII"),
        __(18, "XVIII"),
        __(19, "XIX"),
        __(20, "XX"),
        __(21, "XXI"),
        __(22, "XXII"),
        __(23, "XXIII"),
        __(24, "XXIV"),
        __(25, "XXV"),
        __(26, "XXVI"),
        __(27, "XXVII"),
        __(28, "XXVIII"),
        __(29, "XXIX"),
        __(30, "XXX"),
        __(31, "XXXI"),
        __(32, "XXXII"),
        __(33, "XXXIII"),
        __(34, "XXXIV"),
        __(35, "XXXV"),
        __(36, "XXXVI"),
        __(37, "XXXVII"),
        __(38, "XXXVIII"),
        __(39, "XXXIX"),
        __(40, "XL"),
        __(41, "XLI"),
        __(42, "XLII"),
        __(43, "XLIII"),
        __(44, "XLIV"),
        __(45, "XLV"),
        __(46, "XLVI"),
        __(47, "XLVII"),
        __(48, "XLVIII"),
        __(49, "XLIX"),
        __(50, "L"),
        __(51, "LI"),
        __(52, "LII"),
        __(53, "LIII"),
        __(54, "LIV"),
        __(55, "LV"),
        __(56, "LVI"),
        __(57, "LVII"),
        __(58, "LVIII"),
        __(59, "LIX"),
        __(60, "LX"),
        __(61, "LXI"),
        __(62, "LXII"),
        __(63, "LXIII"),
        __(64, "LXIV"),
        __(65, "LXV"),
        __(66, "LXVI"),
        __(67, "LXVII"),
        __(68, "LXVIII"),
        __(69, "LXIX"),
        __(70, "LXX"),
        __(71, "LXXI"),
        __(72, "LXXII"),
        __(73, "LXXIII"),
        __(74, "LXXIV"),
        __(75, "LXXV"),
        __(76, "LXXVI"),
        __(77, "LXXVII"),
        __(78, "LXXVIII"),
        __(79, "LXXIX"),
        __(80, "LXXX"),
        __(81, "LXXXI"),
        __(82, "LXXXII"),
        __(83, "LXXXIII"),
        __(84, "LXXXIV"),
        __(85, "LXXXV"),
        __(86, "LXXXVI"),
        __(87, "LXXXVII"),
        __(88, "LXXXVIII"),
        __(89, "LXXXIX"),
        __(90, "XC"),
        __(91, "XCI"),
        __(92, "XCII"),
        __(93, "XCIII"),
        __(94, "XCIV"),
        __(95, "XCV"),
        __(96, "XCVI"),
        __(97, "XCVII"),
        __(98, "XCVIII"),
        __(99, "XCIX"),
        __(100, "C"),
        __(201, "CCI"),
        __(212, "CCXII"),
        __(323, "CCCXXIII"),
        __(434, "CDXXXIV"),
        __(501, "DI"),
        __(530, "DXXX"),
        __(545, "DXLV"),
        __(550, "DL"),
        __(656, "DCLVI"),
        __(707, "DCCVII"),
        __(767, "DCCLXVII"),
        __(878, "DCCCLXXVIII"),
        __(890, "DCCCXC"),
        __(900, "CM"),
        __(989, "CMLXXXIX"),
        __(1001, "MI"),
        __(1012, "MXII"),
        __(1123, "MCXXIII"),
        __(1234, "MCCXXXIV"),
        __(1500, "MD"),
        __(1800, "MDCCC"),
        __(2013, "MMXIII"),
    };
            
    static private final T[] latin =
    {
        __(1, "unum"),
        __(3, "tres"),
        __(10, "decem"),
        __(11, "undecim"),
        __(12, "duodecim"),
        __(13, "tredecim"),
        __(14, "quattuordecim"),
        __(15, "quindecim"),
        __(16, "sedecim"),
        __(17, "septendecim"),
        __(18, "duodeviginti"),
        __(19, "undeviginti"),
        __(20, "viginti"),
        __(22, "viginti duo"),
        __(28, "duodetriginta"),
        __(29, "undetriginta"),
        __(30, "triginta"),
        __(33, "triginta tres"),
        __(38, "duodequadraginta"),
        __(39, "undequadraginta"),
        __(40, "quadraginta"),
        __(44, "quadraginta quattuor"),
        __(48, "duodequinquaginta"),
        __(49, "undequinquaginta"),
        __(50, "quinquaginta"),
        __(55, "quinquaginta quinque"),
        __(58, "duodesexaginta"),
        __(59, "undesexaginta"),
        __(60, "sexaginta"),
        __(66, "sexaginta sex"),
        __(68, "duodeseptuaginta"),
        __(69, "undeseptuaginta"),
        __(70, "septuaginta"),
        __(77, "septuaginta septem"),
        __(78, "duodeoctoginta"),
        __(79, "undeoctoginta"),
        __(80, "octoginta"),
        __(88, "duodenonaginta"),
        __(89, "octoginta novem"),
        __(90, "nonaginta"),
        __(98, "nonaginta octo"),
        __(99, "undecentum"),
        __(100, "centum"),
        __(101, "centum unum"),
        __(198, "centum nonaginta octo"),
        __(199, "centum undecentum"),
        __(201, "ducenti unum"),
        __(212, "ducenti duodecim"),
        __(323, "trecenti viginti tres"),
        __(434, "quadringenti triginta quattuor"),
        __(545, "quingenti quadraginta quinque"),
        __(656, "sescenti quinquaginta sex"),
        __(767, "septingenti sexaginta septem"),
        __(878, "octingenti duodeoctoginta"),
        __(989, "nongenti octoginta novem"),
        __(1001, "mille unum"),
        __(1012, "mille duodecim"),
        __(1123, "mille centum viginti tres"),
        __(1234, "mille ducenti triginta quattuor"),
        __(1500, "mille quingenti"),
        __(1800, "mille octingenti"),
        __(2013, "duo milia tredecim"),
    };
            
    public CardinalFormatTest()
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
     * Test of format method, of class CardinalFormat.
     */
    @Test
    public void testFormat()
    {
        CardinalFormat instance;

        instance = CardinalFormat.getInstance(Locale.ROOT);
        for(int i=0;i<999;i++)
        {
            assertEquals(Long.toString(i),Long.toString(i),instance.format(i));
        }
        instance = CardinalFormat.getInstance(new Locale("de","DE"));
        for(T e: de_DE)
        {
            assertEquals(e.text, instance.format(e.num),"num="+Long.toString(e.num));
        }
        instance = CardinalFormat.getInstance(new Locale("es","ES"));
        for(T e: es_ES)
        {
            assertEquals(e.text, instance.format(e.num),"num="+Long.toString(e.num));
        }
        instance = CardinalFormat.getInstance(Locale.US);
        for(T e: en_US)
        {
            assertEquals(e.text, instance.format(e.num),"num="+Long.toString(e.num));
        }
        instance = CardinalFormat.getInstance(Locale.UK,CardinalFormat.LONG);
        for(T e: en_GB)
        {
            assertEquals(e.text, instance.format(e.num),"num="+Long.toString(e.num));
        }
        instance = CardinalFormat.getInstance(new Locale("la"));
        for(T e: la)
        {
            assertEquals(e.text, instance.format(e.num),"num="+Long.toString(e.num));
        }
        instance = CardinalFormat.getInstance(new Locale("latin"));
        for(T e: latin)
        {
            assertEquals(e.text, instance.format(e.num),"num="+Long.toString(e.num));
        }
        
        instance = CardinalFormat.getInstance(Locale.ENGLISH);
        assertEquals("one","one", instance.format(1));

    }
}
