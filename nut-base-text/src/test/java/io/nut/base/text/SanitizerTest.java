/*
 * Copyright (C) 2025-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
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

    @Test
    void testSanitizeMessageWithOSCSequences()
    {
        String input = "a\u001B]8;;http://evil.example\u0007click\u001B]8;;\u0007b";
        assertEquals("aclickb", instance.sanitize(input), "Debería eliminar secuencias OSC (enlaces OSC-8)");
    }

    @Test
    void testSanitizeMessageWithSingleCharacterEscapes()
    {
        assertEquals("ab", instance.sanitize("a\u001B7b"), "Debería eliminar ESC 7 (save cursor)");
        assertEquals("ab", instance.sanitize("a\u001B=b"), "Debería eliminar ESC = (keypad)");
    }

    @Test
    void testSanitizeMessageNormalizesCarriageReturn()
    {
        assertEquals("l1\nl2", instance.sanitize("l1\r\nl2"), "Debería normalizar CRLF a LF");
        assertEquals("l1\nOVER", instance.sanitize("l1\rOVER"), "Debería neutralizar CR para no sobrescribir la línea");
    }

    @Test
    void testSanitizeMessageRemovesEmojiAndSymbols()
    {
        assertEquals("hola  fin", instance.sanitize("hola \uD83D\uDE00 fin"), "Debería eliminar emoji");
        assertEquals("ab", instance.sanitize("a\u2603b"), "Debería eliminar símbolos (U+2603 snowman)");
    }

    @Test
    void testSanitizeMessageRemovesFormatCharacters()
    {
        assertEquals("ab", instance.sanitize("a\u202Eb"), "Debería eliminar override RTL (U+202E)");
        assertEquals("ab", instance.sanitize("a\u200Bb"), "Debería eliminar zero-width space (U+200B)");
        assertEquals("ab", instance.sanitize("a\u00ADb"), "Debería eliminar soft hyphen (U+00AD)");
        assertEquals("ab", instance.sanitize("a\u200Eb"), "Debería eliminar directional embedding (U+200E)");
    }

    @Test
    void testSanitizeMessageKeepsCommonTextSymbols()
    {
        Sanitizer textOnly = new Sanitizer(true, false, 1000);
        assertEquals("a < b + c = d > e", textOnly.sanitize("a < b + c = d > e"),
                "Debería conservar los símbolos de texto habituales (<>+=) en modo texto");
        // En modo HTML los <...> son etiquetas y se eliminan.
        assertEquals("a  e", instance.sanitize("a < b + c = d > e"),
                "En modo HTML un <...> se trata como etiqueta y se elimina");
    }

    @Test
    void testSanitizeMessageRemovesMarksAndPrivateUse()
    {
        assertEquals("ab", instance.sanitize("a\u0301b"), "Debería eliminar marcas combinantes (U+0301)");
        assertEquals("ab", instance.sanitize("a\uE000b"), "Debería eliminar uso privado (U+E000)");
    }
}
