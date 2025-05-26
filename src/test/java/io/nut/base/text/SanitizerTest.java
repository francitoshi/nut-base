/*
 * SanitizerTest.java
 *
 * Copyright (c) 2025 francitoshi@gmail.com
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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author franci
 */
public class SanitizerTest
{

    static final Sanitizer instance = new Sanitizer(true, true, 1000);

    @Test
    void testSanitizeMessageWithANSIEscapeSequences()
    {
        String input = "Hola \u001B[31mMundo!\u001B[0m";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "Hola Mundo!";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería eliminar secuencias ANSI de color");
    }

    @Test
    void testSanitizeMessageWithANSIEscapeSequencesHarmless()
    {
        Sanitizer instance2 = new Sanitizer(true, true, 1000, "•");
        String input = "Hola \u001B[31mMundo!\u001B[0m";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "Hola •Mundo!•";
        String result = instance2.sanitize(inputBytes);
        assertEquals(expected, result, "Debería eliminar secuencias ANSI de color");
    }

    @Test
    void testSanitizeMessageWithMultipleANSIEscapes()
    {
        String input = "\u001B[1;34mTexto en azul\u001B[0m y \u001B[41mFondo rojo\u001B[0m";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "Texto en azul y Fondo rojo";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería eliminar múltiples secuencias ANSI");
    }

    @Test
    void testSanitizeMessageWithNonPrintableCharacters()
    {
        String input = "Hola\u0000Mundo\u0003!\u0007";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "HolaMundo!";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería eliminar caracteres no imprimibles");
    }

    @Test
    void testSanitizeMessageWithHTMLTags()
    {
        // Mensaje con etiquetas <script>
        String input = "Hola <script>alert('malicious')</script>\u001B[31mMundo";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "Hola Mundo";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería eliminar etiquetas <script> con su contenido y secuencias ANSI");
    }

    @Test
    void testSanitizeMessageWithComplexHTML()
    {
        // Mensaje con múltiples etiquetas HTML, incluyendo <script>
        String input = "<div>Hola <p style=\"color:red\">Mundo</p></div><script>malicious</script>";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "Hola Mundo";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería eliminar etiquetas HTML y <script> con su contenido");
    }

    @Test
    void testSanitizeMessageWithUnicodeCharacters()
    {
        String input = "Hola 世界 \u001B[31mMundo";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "Hola 世界 Mundo";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería preservar caracteres Unicode válidos");
    }

    @Test
    void testSanitizeMessageWithEmptyInput()
    {
        byte[] inputBytes = new byte[0];
        String expected = "";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería devolver cadena vacía para entrada vacía");
    }

    @Test
    void testSanitizeMessageWithNullInput()
    {
        byte[] inputBytes = null;
        String expected = "";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería devolver cadena vacía para entrada nula");
    }

    @Test
    void testSanitizeMessageWithLongInput()
    {
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 1500; i++)
        {
            longInput.append("a");
        }
        longInput.insert(500, "\u001B[31m<script>malicious</script>");
        byte[] inputBytes = longInput.toString().getBytes(StandardCharsets.UTF_8);
        String result = instance.sanitize(inputBytes);
        assertTrue(result.length() <= 1000, "Debería truncar el mensaje a 1000 caracteres");
        assertFalse(result.contains("\u001B[31m"), "Debería eliminar secuencias ANSI en mensaje largo");
        assertFalse(result.contains("malicious"), "Debería eliminar contenido de <script> en mensaje largo");
    }

    @Test
    void testSanitizeMessageWithControlCharactersAndANSIAndHTML()
    {
        String input = "Test\u0001\u001B[31mColor<div>HTML</div><script>malicious</script>\u0002\u001B[0mEnd";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "TestColorHTMLEnd";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería eliminar caracteres de control, secuencias ANSI y <script> con su contenido");
    }

    @Test
    void testSanitizeMessageWithValidPunctuation()
    {
        String input = "Hola, mundo! ¿Cómo estás? \u001B[31mTest<div>HTML</div><script>malicious</script>";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        String expected = "Hola, mundo! ¿Cómo estás? TestHTML";
        String result = instance.sanitize(inputBytes);
        assertEquals(expected, result, "Debería preservar puntuación válida y eliminar <script> con su contenido");
    }
}
