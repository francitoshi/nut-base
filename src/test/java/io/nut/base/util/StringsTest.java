/*
 *  StringsTest.java
 *
 *  Copyright (c) 2012-2025 francitoshi@gmail.com
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
package io.nut.base.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class StringsTest
{
    
    public StringsTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
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
     * Test of repeat method, of class Strings.
     */
    @Test
    public void testRepeat_char_int()
    {
        assertEquals("", Strings.repeat('0', 0));
        assertEquals("1", Strings.repeat('1', 1));
        assertEquals("22", Strings.repeat('2', 2));
        assertEquals("333", Strings.repeat('3', 3));
        assertEquals("4444", Strings.repeat('4', 4));
        assertEquals("55555", Strings.repeat('5', 5));
        assertEquals("666666", Strings.repeat('6', 6));
        assertEquals("7777777", Strings.repeat('7', 7));
    }

    /**
     * Test of repeat method, of class Strings.
     */
    @Test
    public void testRepeat_String_int()
    {
        assertEquals("", Strings.repeat("0", 0));
        assertEquals("1", Strings.repeat("1", 1));
        assertEquals("1212", Strings.repeat("12", 2));
        assertEquals("123123123", Strings.repeat("123", 3));
        assertEquals("1234123412341234", Strings.repeat("1234", 4));
        assertEquals("1234512345123451234512345", Strings.repeat("12345", 5));
        assertEquals("123456123456123456123456123456123456", Strings.repeat("123456", 6));
        assertEquals("1234567123456712345671234567123456712345671234567", Strings.repeat("1234567", 7));
    }

    /**
     * Test of fill method, of class Strings.
     */
    @Test
    public void testFill_4args_1()
    {
        StringBuilder builder = new StringBuilder();
        assertEquals("", Strings.fill(builder, '0', 0, false).toString());
        assertEquals("1", Strings.fill(builder, '1', 1, false).toString());
        assertEquals("12", Strings.fill(builder, '2', 2, false).toString());
        assertEquals("123", Strings.fill(builder, '3', 3, false).toString());
        assertEquals("1234", Strings.fill(builder, '4', 4, false).toString());
        assertEquals("12345", Strings.fill(builder, '5', 5, false).toString());
        assertEquals("123456", Strings.fill(builder, '6', 6, false).toString());
        builder = new StringBuilder();
        assertEquals("", Strings.fill(builder, '0', 0, true).toString());
        assertEquals("1", Strings.fill(builder, '1', 1, true).toString());
        assertEquals("21", Strings.fill(builder, '2', 2, true).toString());
        assertEquals("321", Strings.fill(builder, '3', 3, true).toString());
        assertEquals("4321", Strings.fill(builder, '4', 4, true).toString());
        assertEquals("54321", Strings.fill(builder, '5', 5, true).toString());
        assertEquals("654321", Strings.fill(builder, '6', 6, true).toString());
    }

    /**
     * Test of fill method, of class Strings.
     */
    @Test
    public void testFill_4args_2()
    {
        assertEquals("", Strings.fill("", '0', 0, false));
        assertEquals("1", Strings.fill("", '1', 1, false));
        assertEquals("22", Strings.fill("", '2', 2, false));
        assertEquals("033", Strings.fill("0", '3', 3, false));
        assertEquals("0044", Strings.fill("00", '4', 4, false));
        assertEquals("00055", Strings.fill("000", '5', 5, false));
        assertEquals("000066", Strings.fill("0000", '6', 6, false));

        assertEquals("", Strings.fill("", '0', 0, true));
        assertEquals("1", Strings.fill("", '1', 1, true));
        assertEquals("22", Strings.fill("", '2', 2, true));
        assertEquals("330", Strings.fill("0", '3', 3, true));
        assertEquals("4400", Strings.fill("00", '4', 4, true));
        assertEquals("55000", Strings.fill("000", '5', 5, true));
        assertEquals("660000", Strings.fill("0000", '6', 6, true));
    }

    /**
     * Test of fill method, of class Strings.
     */
    @Test
    public void testFill_3args_1()
    {
        StringBuilder builder = new StringBuilder();
        assertEquals("", Strings.fill(builder, '0', 0).toString());
        assertEquals("1", Strings.fill(builder, '1', 1).toString());
        assertEquals("12", Strings.fill(builder, '2', 2).toString());
        assertEquals("123", Strings.fill(builder, '3', 3).toString());
        assertEquals("1234", Strings.fill(builder, '4', 4).toString());
        assertEquals("12345", Strings.fill(builder, '5', 5).toString());
        assertEquals("123456", Strings.fill(builder, '6', 6).toString());
    }

    /**
     * Test of fill method, of class Strings.
     */
    @Test
    public void testFill_3args_2()
    {
        assertEquals("", Strings.fill("", '0', 0));
        assertEquals("1", Strings.fill("", '1', 1));
        assertEquals("22", Strings.fill("", '2', 2));
        assertEquals("033", Strings.fill("0", '3', 3));
        assertEquals("0044", Strings.fill("00", '4', 4));
        assertEquals("00055", Strings.fill("000", '5', 5));
        assertEquals("000066", Strings.fill("0000", '6', 6));
    }

    /**
     * Test of firstNonEmpty method, of class Strings.
     */
    @Test
    public void testFirstNonEmpty()
    {
        // using null as empty
        assertEquals("", Strings.firstNonEmpty(null,(String)null));
        assertEquals("s1", Strings.firstNonEmpty("s1",(String)null));
        assertEquals("s2", Strings.firstNonEmpty(null, "s2"));
        assertEquals("s1", Strings.firstNonEmpty("s1", "s2"));
        
        assertEquals("", Strings.firstNonEmpty(null,null, (String)null));
        assertEquals("s3", Strings.firstNonEmpty(null,null, "s3"));
        assertEquals("s2", Strings.firstNonEmpty(null,"s2", (String)null));
        assertEquals("s2", Strings.firstNonEmpty(null,"s2", "s3"));
        assertEquals("s1", Strings.firstNonEmpty("s1",null, (String)null));
        assertEquals("s1", Strings.firstNonEmpty("s1",null, "s3"));
        assertEquals("s1", Strings.firstNonEmpty("s1","s2", (String)null));
        assertEquals("s1", Strings.firstNonEmpty("s1","s2", "s3"));
        
        // using empty string as empty
        assertEquals("", Strings.firstNonEmpty("",""));
        assertEquals("s1", Strings.firstNonEmpty("s1",""));
        assertEquals("s2", Strings.firstNonEmpty("", "s2"));
        assertEquals("s1", Strings.firstNonEmpty("s1", "s2"));
        
        assertEquals("", Strings.firstNonEmpty("","", ""));
        assertEquals("s3", Strings.firstNonEmpty("","", "s3"));
        assertEquals("s2", Strings.firstNonEmpty("","s2", ""));
        assertEquals("s2", Strings.firstNonEmpty("","s2", "s3"));
        assertEquals("s1", Strings.firstNonEmpty("s1","", ""));
        assertEquals("s1", Strings.firstNonEmpty("s1","", "s3"));
        assertEquals("s1", Strings.firstNonEmpty("s1","s2", ""));
        assertEquals("s1", Strings.firstNonEmpty("s1","s2", "s3"));
     
        assertEquals(" ", Strings.firstNonEmpty(""," ", "s3"));
    }
    /**
     * Test of firstNonBlank method, of class Strings.
     */
    @Test
    public void testFirstNonBlank()
    {
        // using null as empty
        assertEquals("", Strings.firstNonBlank(null,(String)null));
        assertEquals("s1", Strings.firstNonBlank("s1",(String)null));
        assertEquals("s2", Strings.firstNonBlank(null, "s2"));
        assertEquals("s1", Strings.firstNonBlank("s1", "s2"));
        
        assertEquals("", Strings.firstNonBlank(null,null, (String)null));
        assertEquals("s3", Strings.firstNonBlank(null,null, "s3"));
        assertEquals("s2", Strings.firstNonBlank(null,"s2", (String)null));
        assertEquals("s2", Strings.firstNonBlank(null,"s2", "s3"));
        assertEquals("s1", Strings.firstNonBlank("s1",null, (String)null));
        assertEquals("s1", Strings.firstNonBlank("s1",null, "s3"));
        assertEquals("s1", Strings.firstNonBlank("s1","s2", (String)null));
        assertEquals("s1", Strings.firstNonBlank("s1","s2", "s3"));
        
        // using empty string as empty
        assertEquals("", Strings.firstNonBlank("",""));
        assertEquals("s1", Strings.firstNonBlank("s1",""));
        assertEquals("s2", Strings.firstNonBlank("", "s2"));
        assertEquals("s1", Strings.firstNonBlank("s1", "s2"));
        
        assertEquals("", Strings.firstNonBlank("","", ""));
        assertEquals("s3", Strings.firstNonBlank("","", "s3"));
        assertEquals("s2", Strings.firstNonBlank("","s2", ""));
        assertEquals("s2", Strings.firstNonBlank("","s2", "s3"));
        assertEquals("s1", Strings.firstNonBlank("s1","", ""));
        assertEquals("s1", Strings.firstNonBlank("s1","", "s3"));
        assertEquals("s1", Strings.firstNonBlank("s1","s2", ""));
        assertEquals("s1", Strings.firstNonBlank("s1","s2", "s3"));

        assertEquals("s3", Strings.firstNonBlank(""," ", "s3"));
    }
    /**
     * Test of firstNonNull method, of class Strings.
     */
    @Test
    public void testFirstNonNull_3args_1()
    {
        Integer NULL = null;
        Integer s1   = null;
        Integer s2   = null;
        Integer s3   = null;
        
        assertEquals(NULL, Strings.firstNonNull(NULL,NULL));
        assertEquals(s1, Strings.firstNonNull(s1,NULL));
        assertEquals(s2, Strings.firstNonNull(NULL, s2));
        assertEquals(s1, Strings.firstNonNull(s1, s2));
        
        assertEquals(NULL, Strings.firstNonNull(NULL,NULL, NULL));
        assertEquals(s3, Strings.firstNonNull(NULL,NULL, s3));
        assertEquals(s2, Strings.firstNonNull(NULL,s2, NULL));
        assertEquals(s2, Strings.firstNonNull(NULL, s2, s3));
        assertEquals(s1, Strings.firstNonNull(s1,NULL, NULL));
        assertEquals(s1, Strings.firstNonNull(s1,NULL, s3));
        assertEquals(s1, Strings.firstNonNull(s1,s2, NULL));
        assertEquals(s1, Strings.firstNonNull(s1,s2, s3));
    }


    /* Test of toString method, of class Strings.
     */
    @Test
    public void testToString()
    {
        assertNull(Strings.safeToString(null));
        assertEquals("1",Strings.safeToString(1));
        assertEquals("1",Strings.safeToString("1"));
    }


    /**
     * Test of ocurrences method, of class Strings.
     */
    @Test
    public void testOcurrences_String_String()
    {
        assertEquals(0, Strings.ocurrences("", ""));
        assertEquals(0, Strings.ocurrences("", "a"));
        assertEquals(1, Strings.ocurrences("a", "a"));
        assertEquals(3, Strings.ocurrences("aaa", "a"));
        assertEquals(2, Strings.ocurrences("aaaa", "aa"));
        assertEquals(2, Strings.ocurrences("aaaaa", "aa"));
        
        final String quixote = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme";
        assertEquals(11, Strings.ocurrences(quixote, " "));
        assertEquals(6, Strings.ocurrences(quixote, "a"));
    }

    /**
     * Test of ocurrences method, of class Strings.
     */
    @Test
    public void testOcurrences_3args()
    {
        assertEquals(1, Strings.ocurrences("AAaa", "aa", false));
        assertEquals(0, Strings.ocurrences("AAAA", "aa", false));
        assertEquals(2, Strings.ocurrences("AAAA", "aa", true));
    }

    /**
     * Test of ocurrences method, of class Strings.
     */
    @Test
    public void testOcurrences_4args()
    {
        assertEquals(0, Strings.ocurrences("", "", false, true));
        assertEquals(0, Strings.ocurrences("", "a", false, true));
        assertEquals(1, Strings.ocurrences("a", "a", false, true));
        assertEquals(3, Strings.ocurrences("aaa", "a", false, true));
        assertEquals(2, Strings.ocurrences("aaa", "aa", false, true));
        assertEquals(3, Strings.ocurrences("aaaa", "aa", false, true));
        assertEquals(4, Strings.ocurrences("aaaaa", "aa", false, true));

        assertEquals(3, Strings.ocurrences("AAaa", "aa", true, true));
        assertEquals(3, Strings.ocurrences("AAAA", "aa", true, true));
        assertEquals(3, Strings.ocurrences("AAAA", "aa", true, true));
    }

    /**
     * Test of brief method, of class Strings.
     */
    @Test
    public void testBrief()
    {
        assertEquals("", Strings.brief("", 0, "..."));
        assertEquals("", Strings.brief("", 9, "..."));

        assertEquals("...", Strings.brief("01234",0, "..."));
        assertEquals("...", Strings.brief("01234",1, "..."));
        assertEquals("...", Strings.brief("01234",2, "..."));
        assertEquals("...", Strings.brief("01234",3, "..."));
        assertEquals("0...", Strings.brief("01234",4, "..."));
        assertEquals("01234", Strings.brief("01234",5, "..."));
        assertEquals("01234", Strings.brief("01234",6, "..."));
        assertEquals("01234", Strings.brief("01234",7, "..."));
        
        assertEquals("En un lugar de la mancha de...", Strings.brief("En un lugar de la mancha de cuyo nombre",30, "..."));
    }
 
    /**
     * Test of brief method, of class Strings.
     */
    @Test
    public void testBrief_3args()
    {
        String s = "1\n22\n333\n4444\n5555";
        assertEquals("1\n2...", Strings.brief(s, 6, "...", 5));
        assertEquals("1...", Strings.brief(s, 13, "...", 0));
        assertEquals("1\n22...", Strings.brief(s, 13, "...", 1));
        assertEquals("1\n22\n333\n4...", Strings.brief(s, 13, "...", 3));
    }

    /**
     * Test of paddingLeft method, of class Strings.
     */
    @Test
    public void testPaddingLeft()
    {
        assertEquals("", Strings.paddingLeft("", 0, '0'));
        assertEquals("123", Strings.paddingLeft("123", 0, '0'));
        assertEquals("123", Strings.paddingLeft("123", 3, '0'));
        assertEquals("0123", Strings.paddingLeft("123", 4, '0'));
        assertEquals("0000", Strings.paddingLeft("", 4, '0'));
    }

    /**
     * Test of paddingRight method, of class Strings.
     */
    @Test
    public void testPaddingRight()
    {
        assertEquals("", Strings.paddingRight("", 0, '0'));
        assertEquals("123", Strings.paddingRight("123", 0, '0'));
        assertEquals("123", Strings.paddingRight("123", 3, '0'));
        assertEquals("1230", Strings.paddingRight("123", 4, '0'));
        assertEquals("0000", Strings.paddingRight("", 4, '0'));
    }


    /**
     * Test of isEmpty method, of class Strings.
     */
    @Test
    public void testIsEmpty()
    {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("no"));
        assertFalse(Strings.isEmpty("  no  "));
    }
    @Test
    public void testIsNotEmpty()
    {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("yes"));
        assertTrue(Strings.isNotEmpty("  yes  "));
    }

    @Test
    public void testIsBlank()
    {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertFalse(Strings.isBlank("no"));
        assertFalse(Strings.isBlank("  no  "));
    }

    @Test
    public void testIsNotBlank()
    {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertTrue(Strings.isNotBlank("yes"));
        assertTrue(Strings.isNotBlank("  yes  "));
    }

    /**
     * Test of safeToString method, of class Strings.
     */
    @Test
    public void testSafeToString_Object()
    {
        Double doubleNull = null;
        Integer int2 = 2;
        assertNull(Strings.safeToString(doubleNull));
        assertEquals("2",Strings.safeToString(int2));
    }

    /**
     * Test of safeToString method, of class Strings.
     */
    @Test
    public void testSafeToString_Object_String()
    {
        Double doubleNull = null;
        Integer int2 = 2;
        assertNull(Strings.safeToString(doubleNull,null));
        assertEquals("1",Strings.safeToString(doubleNull,"1"));
        assertEquals("2",Strings.safeToString(int2,"1"));
    }

    /**
     * Test of replaceEach method, of class Strings.
     */
    @Test
    public void testReplaceEach()
    {
        assertNull(Strings.replaceEach(null, null, null, true, 0));
        assertEquals("", Strings.replaceEach("", null, null, true, 0)); 
        assertEquals("aba", Strings.replaceEach("aba", null, null, true, 0));
        assertEquals("aba", Strings.replaceEach("aba", new String[0], null, true, 0));
        assertEquals("aba", Strings.replaceEach("aba", null, new String[0], true, 0));
        assertEquals("aba", Strings.replaceEach("aba", new String[]{"a"}, null, true, 0));
        assertEquals("b", Strings.replaceEach("aba", new String[]{"a"}, new String[]{""}, true, 1));
        assertEquals("aba", Strings.replaceEach("aba", new String[]{null}, new String[]{"a"}, true, 1));
        assertEquals("wcte", Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"w", "t"}, true, 1));
        //(example of how it repeats)
        assertEquals("dcte", Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"}, false, 0));
        assertEquals("tcte", Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "t"}, true, 2));
        //assertEqueals("", Strings.replaceEach("abcde", new String[]{"ab", "d"}, new String[]{"d", "ab"}, *) = IllegalStateException
    }

    /**
     * Test of normalize method, of class Strings.
     */
    @Test
    public void testNormalize_String_Locale()
    {
        String sample ="ABCDEFGHIJKLMNÑOPQRSTUVWXYZabcdefghijklmnñopqrstuvwxyz"
                      +"ÀàÈèÌìÒòÙùÁáÉéÍíÓóÚúÝýÂâÊêÎîÔôÛûŶŷÃãÕõÑñÄäËëÏïÖöÜüŸÿÅåÇçŐőŰű"
                      +"ŒÆÂÊÎÔÛœæâêîôûÇçÀÈàèùÉéËÏÜŸëïüÿ"//fr
                      +"ÄÖÜäöüẞß"//de
                      +"АБГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабгдеёжзийклмнопрстуфхцчшщъыьэюя"//ru
                      +""
                      +""
                      +""
                ;
        assertNull(Strings.normalize(null, Locale.US));
        assertEquals("", Strings.normalize("", Locale.US));
        assertEquals("aeiou", Strings.normalize("aeiou", Locale.US));
        assertEquals("aeiou", Strings.normalize("áéíóú", Locale.US));
        assertEquals("AaEeIiOoUuAaEeIiOoUuYyAaEeIiOoUuYyAaOoNnAaEeIiOoUuYyAaCcOoUu", Strings.normalize("ÀàÈèÌìÒòÙùÁáÉéÍíÓóÚúÝýÂâÊêÎîÔôÛûŶŷÃãÕõÑñÄäËëÏïÖöÜüŸÿÅåÇçŐőŰű", Locale.US));
        assertEquals("AaEeIiOoUuÁáÉéÍíÓóÚúYyAaEeIiOoUuYyAaOoÑñAaEeIiOoÜüYyAaCcOoUu", Strings.normalize("ÀàÈèÌìÒòÙùÁáÉéÍíÓóÚúÝýÂâÊêÎîÔôÛûŶŷÃãÕõÑñÄäËëÏïÖöÜüŸÿÅåÇçŐőŰű", new Locale("es","ES")));
        assertEquals("ÀàÈèIiOoUùAaÉéIiOoUuYyÂâÊêÎîÔôÛûYyAaOoNnAaËëÏïOoÜüŸÿAaÇçOoUu", Strings.normalize("ÀàÈèÌìÒòÙùÁáÉéÍíÓóÚúÝýÂâÊêÎîÔôÛûŶŷÃãÕõÑñÄäËëÏïÖöÜüŸÿÅåÇçŐőŰű", Locale.FRANCE));
        assertEquals("AaEeIiOoUuAaEeIiOoUuYyAaEeIiOoUuYyAaOoNnÄäEeIiÖöÜüYyAaCcOoUu", Strings.normalize("ÀàÈèÌìÒòÙùÁáÉéÍíÓóÚúÝýÂâÊêÎîÔôÛûŶŷÃãÕõÑñÄäËëÏïÖöÜüŸÿÅåÇçŐőŰű", Locale.GERMANY));
        //Passport (2013), ICAO
        assertEquals("Vse liudi rozhdaiutsia svobodnymi i ravnymi v svoem dostoinstve i pravakh. Oni nadeleny razumom i sovestiu i dolzhny postupat v otnoshenii drug druga v dukhe bratstva.", 
                    Strings.normalize("Все люди рождаются свободными и равными в своем достоинстве и правах. Они наделены разумом и совестью и должны поступать в отношении друг друга в духе братства.", Locale.US));
    }

    /**
     * Test of delimiterSeparatedValues method, of class Strings.
     */
    @Test
    public void testDelimiterSeparatedValues_3args_2()
    {
        String[] list = {"a","b","c"};
        String sep = ",";
        String nullValue = "";
        String expResult = "a,b,c";
        String result = Strings.join(sep, list, nullValue);
        assertEquals(expResult, result);
    }


    /**
     * Test of trimWhitespaces method, of class Strings.
     */
    @Test
    public void testTrimWhitespaces()
    {
        assertEquals("", Strings.trimWhitespaces(""));
        assertEquals("", Strings.trimWhitespaces(" "));
        assertEquals("a b", Strings.trimWhitespaces(" a b "));
        assertEquals("a  b", Strings.trimWhitespaces(" a  b "));
        assertEquals("a  b", Strings.trimWhitespaces(" \ta  b\t "));
    }

    /**
     * Test of firstNonNull method, of class Strings.
     */
    @Test
    public void testFirstNonNull_3args_2()
    {
        assertEquals("1", Strings.firstNonNull("1", "2", "3"));
        assertEquals("1", Strings.firstNonNull("1", null, "3"));
        assertEquals("1", Strings.firstNonNull("1", "2", (String) null));
        assertEquals("1", Strings.firstNonNull("1", null, (String) null));
        assertEquals("2", Strings.firstNonNull(null, "2", "3"));
        assertEquals("2", Strings.firstNonNull(null, "2", (String) null));
        assertEquals("3", Strings.firstNonNull(null, null, "3"));
        
    }

    /**
     * Test of join method, of class Strings.
     */
    @Test
    public void testJoin()
    {
        assertEquals("abc", Strings.join("a","b","c"));
        assertEquals("abc", Strings.join("ab","c"));
        assertEquals("abc", Strings.join("a","bc"));
        assertEquals("abc", Strings.join("abc"));
        assertEquals("abc", Strings.join("a","","b","","c",""));
    }

    /**
     * Test of equals method, of class Strings.
     */
    @Test
    public void testEquals()
    {
        assertTrue(Strings.equals(null, null));
        assertFalse(Strings.equals("a", null));
        assertFalse(Strings.equals(null, "a"));
        assertTrue(Strings.equals("a", "a"));
        assertFalse(Strings.equals("a", "A"));
        assertFalse(Strings.equals("a", "b"));
    }

    /**
     * Test of equalsIgnoreCase method, of class Strings.
     */
    @Test
    public void testEqualsIgnoreCase()
    {
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertFalse(Strings.equalsIgnoreCase("a", null));
        assertFalse(Strings.equalsIgnoreCase(null, "a"));
        assertTrue(Strings.equalsIgnoreCase("a", "a"));
        assertTrue(Strings.equalsIgnoreCase("a", "A"));
        assertFalse(Strings.equalsIgnoreCase("a", "b"));
    }

    /**
     * Test of startsWith method, of class Strings.
     */
    @Test
    public void testStartsWith()
    {
        assertTrue(Strings.startsWith(null, null));
        assertFalse(Strings.startsWith(null, "prefix"));
        assertFalse(Strings.startsWith("", "prefix"));
        assertTrue(Strings.startsWith("prefix", "prefix"));
        assertTrue(Strings.startsWith("prefix2", "prefix"));
        assertFalse(Strings.startsWith("prefix", "prefix2"));
        
        assertFalse(Strings.startsWith("abc", null));
        assertFalse(Strings.startsWith("abc", "abcdef"));
        assertFalse(Strings.startsWith("abc", "ABCDEF"));

        assertTrue(Strings.startsWith("sss", "s"));
        
        assertFalse(Strings.startsWith("sss", "d"));
    }

    /**
     * Test of startsWithAny method, of class Strings.
     */
    @Test
    public void testStartsWithAny()
    {
        assertTrue(Strings.startsWithAny("sss", "d", "s"));
        assertFalse(Strings.startsWithAny("sss", "d", "r"));
    }

    /**
     * Test of endsWith method, of class Strings.
     */
    @Test
    public void testEndsWith()
    {
        assertTrue(Strings.endsWith(null, null));
        assertFalse(Strings.endsWith(null, "suffix"));
        assertFalse(Strings.endsWith("", "suffix"));
        assertTrue(Strings.endsWith("suffix", "suffix"));
        assertTrue(Strings.endsWith("2suffix", "suffix"));
        assertFalse(Strings.endsWith("suffix", "suffix2"));
        
        assertFalse(Strings.endsWith("abc", null));
        assertFalse(Strings.endsWith("abc", "abcdef"));
        assertFalse(Strings.endsWith("abc", "ABCDEF"));

        assertTrue(Strings.endsWith("sss", "s"));
        
        assertFalse(Strings.endsWith("sss", "d"));
        
    }
    /**
     * Test of endsWithAny method, of class Strings.
     */
    @Test
    public void testEndsWithAny()
    {
        assertTrue(Strings.endsWithAny(null, null, "suffix"));
        assertFalse(Strings.endsWithAny(null, "suffix", "suffix2"));
        assertFalse(Strings.endsWithAny("", "suffix", "suffix2"));
        assertTrue(Strings.endsWithAny("suffix", "suffix", "suffix2"));
        assertTrue(Strings.endsWithAny("2suffix", "suffix", "suffix2"));
        assertFalse(Strings.endsWithAny("suffix", "suffix2", "suffix2"));
    }

    /**
     * Test of toLowerCase method, of class Strings.
     */
    @Test
    public void testSafeToLowerCase_String()
    {
        assertNull(Strings.toLowerCase(null));
        assertEquals("a", Strings.toLowerCase("A"));
    }

    /**
     * Test of toLowerCase method, of class Strings.
     */
    @Test
    public void testSafeToLowerCase_String_Locale()
    {
        assertNull(Strings.toLowerCase(null,Locale.US));
        assertEquals("a", Strings.toLowerCase("A", Locale.US));
    }

    /**
     * Test of toUpperCase method, of class Strings.
     */
    @Test
    public void testSafeToUpperCase_String()
    {
        assertNull(Strings.toUpperCase(null));
        assertEquals("A", Strings.toUpperCase("a"));
    }

    /**
     * Test of toUpperCase method, of class Strings.
     */
    @Test
    public void testSafeToUpperCase_String_Locale()
    {
        assertNull(Strings.toUpperCase(null,Locale.US));
        assertEquals("A", Strings.toUpperCase("a", Locale.US));
    }

    /**
     * Test of nullForEmpty method, of class Strings.
     */
    @Test
    public void testNullForEmpty() 
    {
        assertNull(Strings.nullForEmpty(null));
        assertNull(Strings.nullForEmpty(""));
        assertNotNull(Strings.nullForEmpty("a"));
    }

    /**
     * Test of replace method, of class Strings.
     */
    @Test
    public void testSafeReplace_3args_1()
    {
        assertNull(Strings.replace(null, 'b', 'c'));
        assertEquals("a", Strings.replace("a", 'b', 'c'));
        assertEquals("ac", Strings.replace("ab", 'b', 'c'));
    }

    /**
     * Test of replaceFirst method, of class Strings.
     */
    @Test
    public void testSafeReplaceFirst()
    {
        assertNull(Strings.replaceFirst(null, "b", "c"));
        assertEquals("a", Strings.replaceFirst("a", null, "c"));
        assertEquals("a", Strings.replaceFirst("a", "b", null));
        assertEquals("a", Strings.replaceFirst("a", "b", "c"));
        assertEquals("ac", Strings.replaceFirst("ab", "b", "c"));
    }

    /**
     * Test of replaceAll method, of class Strings.
     */
    @Test
    public void testSafeReplaceAll()
    {
        assertNull(Strings.replaceAll(null, "b", "c"));
        assertEquals("a", Strings.replaceAll("a", null, "c"));
        assertEquals("a", Strings.replaceAll("a", "b", null));
        assertEquals("a", Strings.replaceAll("a", "b", "c"));
        assertEquals("ac", Strings.replaceAll("ab", "b", "c"));
    }

    /**
     * Test of replace method, of class Strings.
     */
    @Test
    public void testSafeReplace_3args_2()
    {
        assertNull(Strings.replace(null, "b", "c"));
        assertEquals("a", Strings.replace("a", null, "c"));
        assertEquals("a", Strings.replace("a", "b", null));
        assertEquals("a", Strings.replace("a", "b", "c"));
        assertEquals("ac", Strings.replace("ab", "b", "c"));
    }

    /**
     * Test of trim method, of class Strings.
     */
    @Test
    public void testTrim_String()
    {
        assertNull(Strings.trim(null));
        assertEquals("", Strings.trim(""));
        assertEquals("", Strings.trim(" "));
        assertEquals("a b", Strings.trim("a b"));
        assertEquals("a b", Strings.trim("a b "));
        assertEquals("a b", Strings.trim(" a b"));
        assertEquals("a b", Strings.trim(" a b "));
    }

    /**
     * Test of trim method, of class Strings.
     */
    @Test
    public void testTrim_CharSequence()
    {
        assertNull(Strings.trim((CharSequence)null));
        assertEquals("", Strings.trim((CharSequence)""));
        assertEquals("", Strings.trim((CharSequence)" "));
        assertEquals("a b", Strings.trim((CharSequence)"a b"));
        assertEquals("a b", Strings.trim((CharSequence)"a b "));
        assertEquals("a b", Strings.trim((CharSequence)" a b"));
        assertEquals("a b", Strings.trim((CharSequence)" a b "));
    }


    @Test
    public void testTrimToNull()
    {
        assertNull(Strings.trimToNull(null));
        assertNull(Strings.trimToNull(""));
        assertNull(Strings.trimToNull("     "));
        assertEquals("abc", Strings.trimToNull("abc"));
        assertEquals("abc", Strings.trimToNull("    abc    "));
    }

    @Test
    public void testTrimToEmpty()
    {
        assertEquals("", Strings.trimToEmpty(null));
        assertEquals("", Strings.trimToEmpty(""));
        assertEquals("", Strings.trimToEmpty("     "));
        assertEquals("abc", Strings.trimToEmpty("abc"));
        assertEquals("abc", Strings.trimToEmpty("    abc    "));
    }

    /**
     * Test of split method, of class Strings.
     */
    @Test
    public void testSplit_String_int() 
    {
        assertArrayEquals(new String[]{}, Strings.split("",3));

        assertArrayEquals(new String[]{"000","111","222"}, Strings.split("000111222",3));
        assertArrayEquals(new String[]{"000","111","222","3"}, Strings.split("0001112223",3));
        assertArrayEquals(new String[]{"000","111","222","33"}, Strings.split("00011122233",3));
        assertArrayEquals(new String[]{"000","111","222","333"}, Strings.split("000111222333",3));
        assertArrayEquals(new String[]{"000","111","222","333","4"}, Strings.split("0001112223334",3));
        
    }

    /**
     * Test of split method, of class Strings.
     */
    @Test
    public void testSplit_3args() 
    {
        assertArrayEquals(new String[]{}, Strings.split("",3));

        assertEquals("000|111|222|", Strings.split("000111222",3,"|"));
        assertEquals("000|111|222|3|", Strings.split("0001112223",3,"|"));
        assertEquals("000|111|222|33|", Strings.split("00011122233",3,"|"));
        assertEquals("000|111|222|333|", Strings.split("000111222333",3,"|"));
        assertEquals("000|111|222|333|4|", Strings.split("0001112223334",3,"|"));
        
    }


    /**
     * Test of isPalindrome method, of class Strings.
     */
    @Test
    public void testIsPalindrome()
    {
        assertTrue(Strings.isPalindrome(""));
        assertTrue(Strings.isPalindrome("1"));
        assertTrue(Strings.isPalindrome("11"));
        assertTrue(Strings.isPalindrome("121"));
        assertTrue(Strings.isPalindrome("1221"));
        assertTrue(Strings.isPalindrome("12321"));
        
        assertFalse(Strings.isPalindrome("ab"));
        assertFalse(Strings.isPalindrome("abc"));
        assertFalse(Strings.isPalindrome("abxyba"));        
    }
    /**
     * Test of commaSeparatedValues method, of class Strings.
     */
    @Test
    public void testJoiner_List_String()
    {
        List<Integer> list = Arrays.asList(new Integer[]{1,2,3,4,5,6,7,8,9,0});
        List<Integer> empty = Arrays.asList(new Integer[]{});
        List<Integer> one = Arrays.asList(new Integer[]{1});
        assertEquals("1|2|3|4|5|6|7|8|9|0", Strings.join("|", list));
        assertEquals("", Strings.join("|", empty));
        assertEquals("1", Strings.join("|", one));
    }

    /**
     * Test of joiner method, of class Strings.
     */
    @Test
    public void testJoin_GenericType_String()
    {
        Integer[] list = new Integer[]{1,2,3,4,5,6,7,8,9,0};
        Integer[] empty = new Integer[]{};
        Integer[] one = new Integer[]{1};
        assertEquals("1|2|3|4|5|6|7|8|9|0", Strings.join("|", list));
        assertEquals("", Strings.join("|", empty));
        assertEquals("1", Strings.join("|", one));
    }

    /**
     * Test of firstNonNull method, of class Strings.
     */
    @Test
    public void testFirstNonNull()
    {
        assertEquals(null, Strings.firstNonNull(null,null));
        assertEquals("s1", Strings.firstNonNull("s1",null));
        assertEquals("s2", Strings.firstNonNull(null, "s2"));
        assertEquals("s1", Strings.firstNonNull("s1", "s2"));
        
        assertEquals(null, Strings.firstNonNull(null,null, (String)null));
        assertEquals("s3", Strings.firstNonNull(null,null, "s3"));
        assertEquals("s2", Strings.firstNonNull(null,"s2", (String)null));
        assertEquals("s2", Strings.firstNonNull(null,"s2", "s3"));
        assertEquals("s1", Strings.firstNonNull("s1",null, (String)null));
        assertEquals("s1", Strings.firstNonNull("s1",null, "s3"));
        assertEquals("s1", Strings.firstNonNull("s1","s2", (String)null));
        assertEquals("s1", Strings.firstNonNull("s1","s2", "s3"));
    }

    /**

     * Test of left method, of class Strings.
     */
    @Test
    public void testLeft()
    {
        assertEquals("", Strings.left("", 0));
        assertEquals("", Strings.left("", 1));
        assertEquals("", Strings.left("", 2));

        assertEquals("", Strings.left("abc", 0));
        assertEquals("a", Strings.left("abc", 1));
        assertEquals("ab", Strings.left("abc", 2));
        assertEquals("abc", Strings.left("abc", 3));
        assertEquals("abc", Strings.left("abc", 4));
    }

    /**
     * Test of right method, of class Strings.
     */
    @Test
    public void testRight()
    {
        assertEquals("", Strings.right("", 0));
        assertEquals("", Strings.right("", 1));
        assertEquals("", Strings.right("", 2));

        assertEquals("", Strings.right("abc", 0));
        assertEquals("c", Strings.right("abc", 1));
        assertEquals("bc", Strings.right("abc", 2));
        assertEquals("abc", Strings.right("abc", 3));
        assertEquals("abc", Strings.right("abc", 4));
    }

    /**
     * Test of codePointCount method, of class Utils.
     */
    @Test
    public void testCodePointCount()
    {
        assertEquals(1, Strings.codePointCount("h"));
        assertEquals(4, Strings.codePointCount("hola"));
        assertEquals(6, Strings.codePointCount("ñiñiñi"));
        assertEquals(1, Strings.codePointCount("\uD801\uDC28"));
    }

    /**
     * Test of codePoints method, of class Strings.
     */
    @Test
    public void testCodePoints()
    {
        assertNull(Strings.codePoints(null));
        assertEquals(1, Strings.codePoints("h").length);
        assertEquals(4, Strings.codePoints("hola").length);
        assertEquals(6, Strings.codePoints("ñiñiñi").length);
        assertEquals(1, Strings.codePoints("\uD801\uDC28").length);
    }

    /**
     * Test of commaSeparatedValues method, of class Strings.
     */
    @Test
    public void testCsv_List()
    {
        List<Integer> list = Arrays.asList(new Integer[]{1,2,3,4,5,6,7,8,9,0});
        List<Integer> empty = Arrays.asList(new Integer[]{});
        List<Integer> one = Arrays.asList(new Integer[]{1});
        assertEquals("1,2,3,4,5,6,7,8,9,0", Strings.csv(list));
        assertEquals("", Strings.csv(empty));
        assertEquals("1", Strings.csv(one));
    }

    /**
     * Test of hexDump method, of class Strings.
     */
    @Test
    public void testHexDump_String() throws Exception
    {
        String[] plainText = 
        {
            "Hello World!!!",
            "Hello World!!! This is a Test.",
            "Hello World!!!\nThis is a Test."
        };
        String[] dumpText = 
        {
            "00000000: 48 65 6C 6C 6F 20 57 6F 72 6C 64 21 21 21        Hello World!!!",

            "00000000: 48 65 6C 6C 6F 20 57 6F 72 6C 64 21 21 21 20 54  Hello World!!! T"+
            "00000010: 68 69 73 20 69 73 20 61 20 54 65 73 74 2E        his is a Test.",

            "00000000: 48 65 6C 6C 6F 20 57 6F 72 6C 64 21 21 21 0A 54  Hello World!!!.T"+
            "00000010: 68 69 73 20 69 73 20 61 20 54 65 73 74 2E        his is a Test."
        };
        Appendable sb = System.out;//new StringBuilder();

        for(String item: plainText)
        {
            String s = Strings.dumpHex(item);
            System.out.println(s);
        }

    }

    /**
     * Test of commaSeparatedValues method, of class Strings.
     */
    @Test
    public void testCsv_GenericType()
    {
        Integer[] list = new Integer[]{1,2,3,4,5,6,7,8,9,0};
        Integer[] empty = new Integer[]{};
        Integer[] one = new Integer[]{1};
        assertEquals("1,2,3,4,5,6,7,8,9,0", Strings.csv(list));
        assertEquals("", Strings.csv(empty));
        assertEquals("1", Strings.csv(one));
    }


    /**
     * Test of joiner method, of class Strings.
     */
    @Test
    public void testJoin_3args_2()
    {
        String[] list = {"a","b","c"};
        String sep = ",";
        String nullValue = "";
        String expResult = "a,b,c";
        String result = Strings.join(sep, list, nullValue);
        assertEquals(expResult, result);
    }

    /**
     * Test of commaSeparatedValues method, of class Strings.
     */
    @Test
    public void testCsv_GenericType_String()
    {
        String[] list = {"a","b","c"};
        String nullValue = "";
        String expResult = "a,b,c";
        String result = Strings.csv(list, nullValue);
        assertEquals(expResult, result);
    }

    /**
     * Test of commaSeparatedValues method, of class Strings.
     */
    @Test
    public void testDelimiterSeparatedValues_List_String()
    {
        List<Integer> list = Arrays.asList(new Integer[]{1,2,3,4,5,6,7,8,9,0});
        List<Integer> empty = Arrays.asList(new Integer[]{});
        List<Integer> one = Arrays.asList(new Integer[]{1});
        assertEquals("1|2|3|4|5|6|7|8|9|0", Strings.join("|", list));
        assertEquals("", Strings.join("|", empty));
        assertEquals("1", Strings.join("|", one));
    }

    /**
     * Test of paddingRight method, of class Strings.
     */
    @Test
    public void testCollectCodePoints()
    {
        assertEquals("", Strings.collectCodePoints("",null));
        assertEquals("", Strings.collectCodePoints("",""));
        assertEquals("aeiou", Strings.collectCodePoints("aeiou",""));
        assertEquals("aeiou", Strings.collectCodePoints("AaEeIiOoUu","AEIOU"));
        assertEquals("aeiou", Strings.collectCodePoints("AaEeIiOoUuAaEeIiOoUu","AEIOU"));
        assertEquals("9876543210", Strings.collectCodePoints("9876543210","AEIOU"));
    }

    /**
     * Test of capitalize method, of class Strings.
     */
    @Test
    public void testCapitalize_String()
    {
        assertNull(Strings.capitalize(null), "null");
        assertEquals("", Strings.capitalize(""));
        assertEquals("I Am FINE", Strings.capitalize("i am FINE"));
    }

    /**
     * Test of capitalize method, of class Strings.
     */
    @Test
    public void testCapitalize_String_charArr()
    {
        assertNull(Strings.capitalize(null));
        assertEquals("",   Strings.capitalize(""));
        assertEquals("a",  Strings.capitalize("a", new char[0]));
        assertEquals("I Am Fine", Strings.capitalize("i am fine", null));
        assertEquals("I aM.Fine", Strings.capitalize("i aM.fine", '.'));
    }

    /**
     * Test of joinerToSetString method, of class Strings.
     */
    @Test
    public void testDelimiterSeparatedValuesToSetString()
    {
        String values = "a,b,c";
        String sep = ",";
        HashSet<String> expResult = new HashSet(Arrays.asList(new String[]{"a","b","c"}));
        HashSet<String> result = Strings.delimiterSeparatedValuesToSetString(values, sep);
        assertEquals(expResult, result);
    }

    /**
     * Test of commaSeparatedValuesToSetString method, of class Strings.
     */
    @Test
    public void testCommaSeparatedValuesToSetString_String_boolean()
    {
        String values = "a , b , c";
        HashSet<String> expResult = new HashSet(Arrays.asList(new String[]{"a","b","c"}));
        HashSet<String> result = Strings.commaSeparatedValuesToSetString(values, true);
        assertEquals(expResult, result);
    }

    /**
     * Test of commaSeparatedValuesToSetString method, of class Strings.
     */
    @Test
    public void testCommaSeparatedValuesToSetString_String()
    {
        String values = "a,b,c";
        HashSet<String> expResult = new HashSet(Arrays.asList(new String[]{"a","b","c"}));
        HashSet<String> result = Strings.commaSeparatedValuesToSetString(values);
        assertEquals(expResult, result);
    }
    

    /**
     * Test of equalsIgnoreCase method, of class Strings.
     */
    @Test
    public void testSafeEqualsIgnoreCase()
    {
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertFalse(Strings.equalsIgnoreCase("a", null));
        assertFalse(Strings.equalsIgnoreCase(null, "a"));
        assertTrue(Strings.equalsIgnoreCase("a", "a"));
        assertTrue(Strings.equalsIgnoreCase("a", "A"));
        assertFalse(Strings.equalsIgnoreCase("a", "b"));
    }



    /**
     * Test of uniqueCodepoints method, of class Strings.
     */
    @Test
    public void testUniqueCodepoints()
    {
        assertEquals("", Strings.uniqueCodepoints(""));
        assertEquals(" ", Strings.uniqueCodepoints(" "));
        assertEquals("a", Strings.uniqueCodepoints("a"));
        assertEquals("\uD83D\uDF01", Strings.uniqueCodepoints("\uD83D\uDF01"));
        
        assertEquals(" ", Strings.uniqueCodepoints("  "));
        assertEquals("a", Strings.uniqueCodepoints("aa"));
        
        assertEquals("a b", Strings.uniqueCodepoints("aa  bb  aa"));
    }

    /**
     * Test of uniqueCodepointCount method, of class Strings.
     */
    @Test
    public void testUniqueCodepointCount()
    {
        assertEquals(0, Strings.uniqueCodepointCount(""));
        assertEquals(1, Strings.uniqueCodepointCount(" "));
        assertEquals(1, Strings.uniqueCodepointCount("a"));
        assertEquals(1, Strings.uniqueCodepointCount("\uD83D\uDF01"));
        
        assertEquals(1, Strings.uniqueCodepointCount("  "));
        assertEquals(1, Strings.uniqueCodepointCount("aa"));
        
        assertEquals(3, Strings.uniqueCodepointCount("aa  bb  aa"));
        assertEquals(3, Strings.uniqueCodepointCount("a\uD83D\uDF01 "));
    }

    /**
     * Test of mergeRows method, of class Strings.
     */
    @Test
    public void testMergeRows()
    {
        String i = "1\n22\n333\n4444\n55555\n";
        String x = "x\nx\nx\nx\nx\n";
        String y = "y\ny\ny\ny\ny\ny";
        
        String mergedix_1 = "1x\n22x\n333x\n4444x\n55555x\n";
        String mergedix0 = "1    x\n22   x\n333  x\n4444 x\n55555x\n";
        String mergedix1 = "1     x\n22    x\n333   x\n4444  x\n55555 x\n";

        String mergediy_1 = "1y\n22y\n333y\n4444y\n55555y\ny\n";
        String mergediy0 = "1    y\n22   y\n333  y\n4444 y\n55555y\n     y\n";
        String mergediy1 = "1     y\n22    y\n333   y\n4444  y\n55555 y\n      y\n";
        
        assertEquals(mergedix_1, Strings.mergeRows(i, x, -1));
        assertEquals(mergedix0, Strings.mergeRows(i, x, 0));
        assertEquals(mergedix1, Strings.mergeRows(i, x, 1));
        assertEquals(mergediy_1, Strings.mergeRows(i, y, -1));
        assertEquals(mergediy0, Strings.mergeRows(i, y, 0));
        assertEquals(mergediy1, Strings.mergeRows(i, y, 1));
    }

    /**
     * Test of overlapped method, of class Strings.
     */
    @Test
    public void testOverlapped()
    {
        assertEquals(0, Strings.overlapped("", ""));
        assertEquals(0, Strings.overlapped("a", ""));
        assertEquals(0, Strings.overlapped("", "a"));
        assertEquals(1, Strings.overlapped("a", "a"));
        
        assertEquals(1, Strings.overlapped("ab", "ba"));
        assertEquals(2, Strings.overlapped("ab", "ab"));
        assertEquals(0, Strings.overlapped("ab", ""));

        assertEquals(4, Strings.overlapped("aacc", "aaccG"));
        assertEquals(4, Strings.overlapped("GgTTC", "gTTC"));
        assertEquals(0, Strings.overlapped("aaccG", "aacc"));
    }

    /**
     * Test of overlap method, of class Strings.
     */
    @Test
    public void testOverlap()
    {
        assertEquals("", Strings.overlap("", ""));
        
        assertEquals("", Strings.overlap("", ""));
        assertEquals("a", Strings.overlap("a", ""));
        assertEquals("a", Strings.overlap("", "a"));
        assertEquals("a", Strings.overlap("a", "a"));
        
        assertEquals("aba", Strings.overlap("ab", "ba"));
        assertEquals("ab", Strings.overlap("ab", "ab"));
        assertEquals("ab", Strings.overlap("ab", ""));

        assertEquals("aaccG", Strings.overlap("aacc", "aaccG"));
        assertEquals("GgTTC", Strings.overlap("GgTTC", "gTTC"));
        assertEquals("aaccGaacc", Strings.overlap("aaccG", "aacc"));
	}

    /**
     * Test of unquote method, of class Strings.
     */
    @Test
    public void testUnquote()
    {
        String samples[][] = 
        {
            {null, null},
            {"",""},
            {"a","a"},
            {"\"a","\"a"},
            {"a\"","a\""},
            {"a","\"a\""},
            {".\"a\".",".\"a\"."},

            {"'a","'a"},
            {"a'","a'"},
            {"a","'a'"},
            {".'a'.",".'a'."},
            
            {"\"a'","\"a'"},
            {"'a\"","'a\""},
            {"a","\"'a'\""},
            {"a","'\"a\"'"},
            {".\"'a'\".",".\"'a'\"."},
        };
        
        for(int i=0;i<samples.length;i++)
        {
            assertEquals(samples[i][0], Strings.unquote(samples[i][1]), ""+i);
        }
    }

    /**
     * Test of unquoteDouble method, of class Strings.
     */
    @Test
    public void testUnquoteDouble()
    {
        String samples[][] = 
        {
            {null, null},
            {"",""},
            {"a","a"},
            {"\"a","\"a"},
            {"a\"","a\""},
            {"a","\"a\""},
            {".\"a\".",".\"a\"."},

            {"'a","'a"},
            {"a'","a'"},
            {"'a'","'a'"},
            {".'a'.",".'a'."},
            
            {"\"a'","\"a'"},
            {"'a\"","'a\""},
            {"'a'","\"'a'\""},
            {"'\"a\"'","'\"a\"'"},
            
            {"\"\"a\"\"","\"\"a\"\""},
        };
        
        for(int i=0;i<samples.length;i++)
        {
            assertEquals(samples[i][0], Strings.unquoteDouble(samples[i][1]), ""+i);
        }
    }

    /**
     * Test of unquoteSingle method, of class Strings.
     */
    @Test
    public void testUnquoteSingle()
    {
        String samples[][] = 
        {
            {null, null},
            {"",""},
            {"a","a"},
            {"\"a","\"a"},
            {"a\"","a\""},
            {"\"a\"","\"a\""},
            {".\"a\".",".\"a\"."},

            {"'a","'a"},
            {"a'","a'"},
            {"a","'a'"},
            {".'a'.",".'a'."},
            
            {"\"a'","\"a'"},
            {"'a\"","'a\""},
            {"\"'a'\"","\"'a'\""},
            {"\"a\"","'\"a\"'"},
            {".\"'a'\".",".\"'a'\"."},
        };
        
        for(int i=0;i<samples.length;i++)
        {
            assertEquals(samples[i][0], Strings.unquoteSingle(samples[i][1]), ""+i);
        }
    }

    /**
     * Test of commonPrefix method, of class Strings.
     */
    @Test
    public void testCommonPrefix()
    {
        assertEquals("", Strings.commonPrefix(null));
        assertEquals("", Strings.commonPrefix(null));
        assertEquals("", Strings.commonPrefix(new String[] {}));
        assertEquals("abc", Strings.commonPrefix(new String[] {"abc"}));
        assertEquals("", Strings.commonPrefix(new String[] {null, null}));
        assertEquals("", Strings.commonPrefix(new String[] {"", ""}));
        assertEquals("", Strings.commonPrefix(new String[] {"", null}));
        assertEquals("", Strings.commonPrefix(new String[] {"abc", null, null}));
        assertEquals("", Strings.commonPrefix(new String[] {null, null, "abc"}));
        assertEquals("", Strings.commonPrefix(new String[] {"", "abc"}));
        assertEquals("", Strings.commonPrefix(new String[] {"abc", ""}));
        assertEquals("abc", Strings.commonPrefix(new String[] {"abc", "abc"}));
        assertEquals("a", Strings.commonPrefix(new String[] {"abc", "a"}));
        assertEquals("ab", Strings.commonPrefix(new String[] {"ab", "abxyz"}));
        assertEquals("ab", Strings.commonPrefix(new String[] {"abcde", "abxyz"}));
        assertEquals("", Strings.commonPrefix(new String[] {"abcde", "xyz"}));
        assertEquals("", Strings.commonPrefix(new String[] {"xyz", "abcde"}));
        assertEquals("i am a ", Strings.commonPrefix(new String[] {"i am a machine", "i am a robot"}));
    }

    /**
     * Test of reverse method, of class Strings.
     */
    @Test
    public void testReverse()
    {
        assertNull(Strings.reverse(null));
        assertEquals("", Strings.reverse(""));
        assertEquals("!", Strings.reverse("!"));
        assertEquals("cba", Strings.reverse("abc"));
        assertEquals("12321", Strings.reverse("12321"));
        assertEquals("úóíéá", Strings.reverse("áéíóú"));
        assertEquals("cba•", Strings.reverse("•abc"));
        assertEquals("1😀2😀3😀4", Strings.reverse("4😀3😀2😀1"));
    }
    /**
     * Test of skip method, of class Strings.
     */
    @Test
    public void testSkip()
    {
        assertNull(Strings.skip(null, 0));
        assertEquals("", Strings.skip("", 10));
        assertEquals("a", Strings.skip("a", 0));
        assertEquals("", Strings.skip("a", 1));
        assertEquals("", Strings.skip("a", 2));
        assertEquals("b", Strings.skip("ab", 1));
    }

    /**
     * Test of endsWith method, of class Strings.
     */
    @Test
    public void testSafeEndsWith()
    {
        assertTrue(Strings.endsWith("hello world", "world"));
        assertTrue(Strings.endsWith("hello world", "hello world"));
        assertFalse(Strings.endsWith("hello world", "hello"));

        assertFalse(Strings.endsWith(null, "hello"));
        assertFalse(Strings.endsWith("hello world", null));
    }

    /**
     * Test of join method, of class Strings.
     */
    @Test
    public void testJoin_CharSequenceArr()
    {
        assertEquals("hello world.", Strings.join("hello"," ","world","."));
    }

    /**
     * Test of quoteSingle method, of class Strings.
     */
    @Test
    public void testQuoteSingle()
    {
        assertNull(Strings.quoteSingle(null));
        assertEquals("''",Strings.quoteSingle(""));
        assertEquals("'hello'",Strings.quoteSingle("hello"));
        assertEquals("'hello \\'yoyo\\' '",Strings.quoteSingle("hello 'yoyo' "));
        assertEquals("'hello \"yoyo\"'",Strings.quoteSingle("hello \"yoyo\""));
    }

    /**
     * Test of quoteDouble method, of class Strings.
     */
    @Test
    public void testQuoteDouble()
    {
        assertNull(Strings.quoteSingle(null));
        assertEquals("\"\"",Strings.quoteDouble(""));
        assertEquals("\"hello\"",Strings.quoteDouble("hello"));
        assertEquals("\"hello 'yoyo' \"",Strings.quoteDouble("hello 'yoyo' "));
        assertEquals("\"hello \"yoyo\"\"",Strings.quoteDouble("hello \"yoyo\""));
    }

    @Test
    public void testCenter_String_int()
    {
     assertNull(Strings.center(null, 0));
     assertEquals("    ", Strings.center("", 4));
     assertEquals("ab", Strings.center("ab", -1));
     assertEquals(" ab ", Strings.center("ab", 4));
     assertEquals("abcd", Strings.center("abcd", 2));
     assertEquals(" a  ", Strings.center("a", 4));
    }

    @Test
    public void testCenter_3args()
    {
     assertNull(Strings.center(null, 0, '0'));
     assertEquals("    ", Strings.center("", 4, ' '));
     assertEquals("ab", Strings.center("ab", -1, ' '));
     assertEquals(" ab ", Strings.center("ab", 4, ' '));
     assertEquals("abcd", Strings.center("abcd", 2, ' '));
     assertEquals(" a  ", Strings.center("a", 4, ' '));
     assertEquals("yayy", Strings.center("a", 4, 'y'));
    }

    @Test
    public void testLength()
    {
        assertEquals(0, Strings.length(null));
        assertEquals(0, Strings.length(""));
        assertEquals(3, Strings.length("123"));
    }

    @Test
    public void testHasNullsOrBlank()
    {
        assertTrue(Strings.hasNullsOrBlank());
        assertTrue(Strings.hasNullsOrBlank((String) null));
        assertTrue(Strings.hasNullsOrBlank(new String[0]));
        assertTrue(Strings.hasNullsOrBlank(new String[]{null}));
        assertTrue(Strings.hasNullsOrBlank(new String[]{null, ""}));
        assertTrue(Strings.hasNullsOrBlank(new String[]{"", ""}));
        assertTrue(Strings.hasNullsOrBlank(new String[]{"", null}));
        assertTrue(Strings.hasNullsOrBlank(new String[]{null, " "}));
        assertTrue(Strings.hasNullsOrBlank(new String[]{"", " "}));
        assertFalse(Strings.hasNullsOrBlank(new String[]{"a", "b"}));
        assertTrue(Strings.hasNullsOrBlank(new String[]{" ", null}));
    }

}
