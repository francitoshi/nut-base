/*
 * SentencesTest.java
 *
 * Copyright (c) 2013-2025 francitoshi@gmail.com
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author franci
 */
public class SentencesTest
{
    static final String[][][] samples =
        {
           {
                {"La historia de la escritura comprende los distintos sistemas de escritura que surgieron desde la Edad del bronce (finales del milenio IV a. C.)."},
                {"La historia de la escritura comprende los distintos sistemas de escritura que surgieron desde la Edad del bronce","(finales del milenio IV a.","C.)."},
                {"La historia de la escritura comprende los distintos sistemas de escritura que surgieron desde la Edad del bronce","(finales del milenio IV a. C.)."},
            },
           {
                {"How old are you? http://www.example.com/ignore?key=75&a%b"},
                {"How old are you?","http://www.example.com/ignore?key=75&a%b"},
                {"How old are you?","http://www.example.com/ignore?key=75&a%b"},
            },
//            {
//                {"“Si haces una afirmación, un programador dice ‘Sí, pero…’, mientras que un diseñador dice ‘Sí, y…’” \n-- André Bensoussan\n  “Hemos hecho los botones de la pantalla tan bonitos que querrás lamerlos” \n-- Steve Jobs   Tecnología “Estoy trabajando en un catecismo para robots. Necesitarán saber de dónde vienen.” "},
//                {"“Si haces una afirmación, un programador dice","‘Sí, pero…’,","mientras que un diseñador dice","‘Sí, y…’”","-- André Bensoussan","“Hemos hecho los botones de la pantalla tan bonitos que querrás lamerlos”","-- Steve Jobs   Tecnología","“Estoy trabajando en un catecismo para robots. Necesitarán saber de dónde vienen.”"},
//                {"“Si haces una afirmación, un programador dice","‘Sí, pero…’,","mientras que un diseñador dice","‘Sí, y…’”","-- André Bensoussan","“Hemos hecho los botones de la pantalla tan bonitos que querrás lamerlos”","-- Steve Jobs   Tecnología","“Estoy trabajando en un catecismo para robots. Necesitarán saber de dónde vienen.”"},
//            },
            {
                {"Dijkstra\n “La belleza es más importante en informática que en ninguna otra tecnología debido a la gran complejidad del software. La belleza es la defensa definitiva contra la complejidad”\n-- David Gelernter\n“La principal causa de complejidad en el software es que los fabricantes implementan casi todas las características que solicitan los usuarios”"},
                {"Dijkstra","“La belleza es más importante en informática que en ninguna otra tecnología debido a la gran complejidad del software.","La belleza es la defensa definitiva contra la complejidad”","-- David Gelernter","“La principal causa de complejidad en el software es que los fabricantes implementan casi todas las características que solicitan los usuarios”"},
                {"Dijkstra","“La belleza es más importante en informática que en ninguna otra tecnología debido a la gran complejidad del software.","La belleza es la defensa definitiva contra la complejidad”","-- David Gelernter","“La principal causa de complejidad en el software es que los fabricantes implementan casi todas las características que solicitan los usuarios”"}
            },
            {
                {"“¿Que cuándo usar desarrollo iterativo? Deberías usar un desarrollo iterativo sólo en los proyectos en los que quieras tener éxito” \n-- Martin Fowler \n “Corrige los errores en las especificaciones lo antes posible. Si lo haces más tarde, costará un 500% más si te encuentras en la fase de diseño, 1000% más si estás codificando, 2.000% más en fase de pruebas, y 20.000% si el sistema está en producción” \n-- Barry Boehm\n"},
                {"“¿Que cuándo usar desarrollo iterativo?","Deberías usar un desarrollo iterativo sólo en los proyectos en los que quieras tener éxito”","-- Martin Fowler","“Corrige los errores en las especificaciones lo antes posible.","Si lo haces más tarde, costará un 500% más si te encuentras en la fase de diseño, 1000% más si estás codificando, 2.000% más en fase de pruebas, y 20.000% si el sistema está en producción”","-- Barry Boehm"},
                {"“¿Que cuándo usar desarrollo iterativo?","Deberías usar un desarrollo iterativo sólo en los proyectos en los que quieras tener éxito”","-- Martin Fowler","“Corrige los errores en las especificaciones lo antes posible.","Si lo haces más tarde, costará un 500% más si te encuentras en la fase de diseño, 1000% más si estás codificando, 2.000% más en fase de pruebas, y 20.000% si el sistema está en producción”","-- Barry Boehm"},
            },
            {
                {"Because of its widespread influence, Don Quixote also helped cement the modern Spanish language. The opening sentence of the book created a classic Spanish cliché with the phrase \"de cuyo nombre no quiero acordarme\" (\"whose name I do not wish to recall\"): \"En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no hace mucho tiempo que vivía un hidalgo de los de lanza en astillero, adarga antigua, rocín flaco y galgo corredor. \" (\"In a village of La Mancha, whose name I do not wish to recall, there lived, not very long ago, one of those gentlemen with a lance in the lance-rack, an ancient shield, a skinny old horse, and a fast greyhound. The novel's farcical elements make use of punning and similar verbal playfulness. Character-naming in Don Quixote makes ample figural use of contradiction, inversion, and irony, such as the names Rocinante (a reversal) and Dulcinea (an allusion to illusion), and the word quixote itself, possibly a pun on quijada (jaw) but certainly cuixot (Catalan: thighs), a reference to a horse's rump. As a military term, the word quijote refers to cuisses, part of a full suit of plate armour protecting the thighs. The Spanish suffix -ote denotes the augmentative—for example, grande means large, but grandote means extra large. Following this example, Quixote would suggest \"The Great Quijano\", a play on words that makes much sense in light of the character's delusions of grandeur. La Mancha is a region of Spain, but mancha (Spanish word) means spot, mark, stain, region and word are not etymologically related (from Arab origin the former, from Latin macula the latter), so \"de La Mancha\" (lit. both Stained Quixote and Quixote from La Mancha) is a hilarious name for a spotless knight."},
                {
                    "Because of its widespread influence, Don Quixote also helped cement the modern Spanish language.",
                    "The opening sentence of the book created a classic Spanish cliché with the phrase",
                    "\"de cuyo nombre no quiero acordarme\"",
                    "(\"whose name I do not wish to recall\"):",
                    "\"En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no hace mucho tiempo que vivía un hidalgo de los de lanza en astillero, adarga antigua, rocín flaco y galgo corredor. \"",
                    "(\"In a village of La Mancha, whose name I do not wish to recall, there lived, not very long ago, one of those gentlemen with a lance in the lance-rack, an ancient shield, a skinny old horse, and a fast greyhound.",
                    "The novel's farcical elements make use of punning and similar verbal playfulness.",
                    "Character-naming in Don Quixote makes ample figural use of contradiction, inversion, and irony, such as the names Rocinante",
                    "(a reversal)",
                    "and Dulcinea",
                    "(an allusion to illusion),",
                    "and the word quixote itself, possibly a pun on quijada",
                    "(jaw)",
                    "but certainly cuixot",
                    "(Catalan:",
                    "thighs),",
                    "a reference to a horse's rump.",
                    "As a military term, the word quijote refers to cuisses, part of a full suit of plate armour protecting the thighs.",
                    "The Spanish suffix -ote denotes the augmentative—",
                    "for example, grande means large, but grandote means extra large.",
                    "Following this example, Quixote would suggest",
                    "\"The Great Quijano\",",
                    "a play on words that makes much sense in light of the character's delusions of grandeur.",
                    "La Mancha is a region of Spain, but mancha",
                    "(Spanish word)",
                    "means spot, mark, stain, region and word are not etymologically related",
                    "(from Arab origin the former, from Latin macula the latter),",
                    "so",
                    "\"de La Mancha\"",
                    "(lit. both Stained Quixote and Quixote from La Mancha)",
                    "is a hilarious name for a spotless knight."
                },
                {
                    "Because of its widespread influence, Don Quixote also helped cement the modern Spanish language.",
                    "The opening sentence of the book created a classic Spanish cliché with the phrase",
                    "\"de cuyo nombre no quiero acordarme\"",
                    "(\"whose name I do not wish to recall\"):",
                    "\"En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no hace mucho tiempo que vivía un hidalgo de los de lanza en astillero, adarga antigua, rocín flaco y galgo corredor. \"",
                    "(\"In a village of La Mancha, whose name I do not wish to recall, there lived, not very long ago, one of those gentlemen with a lance in the lance-rack, an ancient shield, a skinny old horse, and a fast greyhound.",
                    "The novel's farcical elements make use of punning and similar verbal playfulness.",
                    "Character-naming in Don Quixote makes ample figural use of contradiction, inversion, and irony, such as the names Rocinante",
                    "(a reversal)",
                    "and Dulcinea",
                    "(an allusion to illusion),",
                    "and the word quixote itself, possibly a pun on quijada",
                    "(jaw) but certainly cuixot",
                    "(Catalan:",
                    "thighs),",
                    "a reference to a horse's rump.",
                    "As a military term, the word quijote refers to cuisses, part of a full suit of plate armour protecting the thighs.",
                    "The Spanish suffix -ote denotes the augmentative—",
                    "for example, grande means large, but grandote means extra large.",
                    "Following this example, Quixote would suggest",
                    "\"The Great Quijano\",",
                    "a play on words that makes much sense in light of the character's delusions of grandeur.",
                    "La Mancha is a region of Spain, but mancha",
                    "(Spanish word)",
                    "means spot, mark, stain, region and word are not etymologically related",
                    "(from Arab origin the former, from Latin macula the latter),",
                    "so \"de La Mancha\"",
                    "(lit. both Stained Quixote and Quixote from La Mancha)",
                    "is a hilarious name for a spotless knight."
                }
            },
            {
                {"“lingüístico”… o, en el extremo, sea el mero cifrado de un grupo social en términos de casta, con sus mandatos y reservas delimitados)."},
                {"“lingüístico”…","o, en el extremo, sea el mero cifrado de un grupo social en términos de casta, con sus mandatos y reservas delimitados)."},
                {"“lingüístico”…","o, en el extremo, sea el mero cifrado de un grupo social en términos de casta, con sus mandatos y reservas delimitados)."}
            },
            {
                {"静脉，看到的，傻瓜。"},
                {"静脉，看到的，傻瓜。"},
                {"静脉，看到的，傻瓜。"}
            },
            {
                {"静脉，看到的，傻瓜。静脉，看到的，傻瓜。"},
                {"静脉，看到的，傻瓜。","静脉，看到的，傻瓜。"},
                {"静脉，看到的，傻瓜。","静脉，看到的，傻瓜。"}
            },
            {
                {"静脉, 看到的, 傻瓜."},
                {"静脉, 看到的, 傻瓜."},
                {"静脉, 看到的, 傻瓜."}
            },
            {
                {"静脉，看到的，傻瓜。 静脉，看到的，傻瓜。"},
                {"静脉，看到的，傻瓜。","静脉，看到的，傻瓜。"},
                {"静脉，看到的，傻瓜。","静脉，看到的，傻瓜。"}
            },
            {
                {"প্রিয় লেখক ও চলচ্চিত্র পরিচালক সত্যজিৎ রায়। ছোটদের জন্য দারুণ সব গল্প-উপন্যাস লিখেছেন তিনি।"},
                {"প্রিয় লেখক ও চলচ্চিত্র পরিচালক সত্যজিৎ রায়।","ছোটদের জন্য দারুণ সব গল্প-উপন্যাস লিখেছেন তিনি।"},
                {"প্রিয় লেখক ও চলচ্চিত্র পরিচালক সত্যজিৎ রায়।","ছোটদের জন্য দারুণ সব গল্প-উপন্যাস লিখেছেন তিনি।"}
            },
            {
                {"colon: http://www.example.com/ignore/this/text and 12:30 and 2013.02.08"},
                {"colon:","http://www.example.com/ignore/this/text and 12:30 and 2013.02.08"},
                {"colon: http://www.example.com/ignore/this/text and 12:30 and 2013.02.08"},
            },
        };
    
    static final String[][][] samplesAC =
        {
            {
                {"Rómulo y Remo nacieron el año 771 a. C. en Alba Longa."},
                {"Rómulo y Remo nacieron el año 771 a.","C. en Alba Longa."},
                {"Rómulo y Remo nacieron el año 771 a. C. en Alba Longa."},
            },
            {
                {"Muy pronto (340 a. C.) su padre lo asoció a tareas del gobierno nombrándolo regente, a pesar de su juventud.11 En el 338 a. C."},
                {"Muy pronto","(340 a.","C.)","su padre lo asoció a tareas del gobierno nombrándolo regente, a pesar de su juventud.11 En el 338 a.","C."},
                {"Muy pronto","(340 a. C.)","su padre lo asoció a tareas del gobierno nombrándolo regente, a pesar de su juventud.11 En el 338 a. C."},
            },
            
        };
    
    public SentencesTest()
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

    static class SpanishBeforeCristFilter implements StringFilter
    {
        public String filter(String s)
        {
            return filter(s, new Locale(""));
        }
        public String filter(String s, Locale locale)
        {
            return s.replace("a. C.","antes de Cristo");
        }
    }
    static final SpanishBeforeCristFilter spanishBeforeCristFilter = new SpanishBeforeCristFilter();
    /**
     * Test of split method, of class Sentences
     */
    @Test
    public void testSplit_String()
    {
        Sentences cutter = new Sentences();
        for(int i=0;i<samples.length;i++)
        {
            String[] result = cutter.split(samples[i][0][0]);
            //assertArrayEquals(samples[i][0][0],samples[i][1], result);
            assertArrayEquals(samples[i][1], result);
        }
        //not using filter
        for(int i=0;i<samplesAC.length;i++)
        {
            String[] result = cutter.split(samplesAC[i][0][0]);
            assertArrayEquals(samplesAC[i][1], result);
        }
        //using filter
        cutter = new Sentences(spanishBeforeCristFilter, new Locale(""));
        for(int i=0;i<samplesAC.length;i++)
        {
            String[] result = cutter.split(samplesAC[i][0][0]);
            assertArrayEquals(samplesAC[i][2], result);
        }
    }

    /**
     * Test of split method, of class Sentences.
     */
    @Test
    public void testSplit_String_int()
    {
        Sentences cutter = new Sentences();
        for(int i=0;i<samples.length;i++)
        {
            String[] result = cutter.split(samples[i][0][0], 8);
            assertArrayEquals(samples[i][2], result, "i="+i);
        }
    }
}
