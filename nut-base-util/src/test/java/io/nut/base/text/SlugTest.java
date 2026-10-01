/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Slug}.
 */
class SlugTest
{

    /**
     * Table-driven cases for {@link Slug#slugify(String)}: {input, expected}.
     */
    private static final Object[][] TESTS =
    {
            {null, null},
            {"", ""},
            {" ", ""},
            {"Hello", "hello"},
            {"Hello World", "hello-world"},
            {"Hello     World", "hello-world"},
            {"Hello\tWorld", "hello-world"},
            {"Hello\nWorld", "hello-world"},
            {"áéíóú", "aeiou"},
            {"ÄËÏÖÜ", "aeiou"},
            {"Ñandú", "nandu"},
            {"façade", "facade"},
            {"Hello!!! World???", "hello-world"},
            {"---Hello---", "hello"},
            {"Java 21", "java-21"},
            {"Hello 🌍 World 🚀", "hello-world"},
            {"C++ & Java", "c-java"}
    };

    private static Stream<Object[]> defaultSlugifyCases()
    {
        return Stream.of(TESTS);
    }

    @ParameterizedTest(name = "slugify({0}) = {1}")
    @MethodSource("defaultSlugifyCases")
    @DisplayName("slugify(text) with default options")
    void slugifyDefaultOptions(String input, String expected)
    {
        assertEquals(expected, Slug.slugify(input));
    }

    @Nested
    @DisplayName("slugify(text, char separator)")
    class SeparatorOverload
    {

        @Test
        void usesUnderscoreSeparator()
        {
            assertEquals("hello_world", Slug.slugify("Hello World", '_'));
        }

        @Test
        void usesDotSeparator()
        {
            assertEquals("hello.world", Slug.slugify("Hello World", '.'));
        }

        @Test
        void collapsesRepeatedPunctuationWithCustomSeparator()
        {
            assertEquals("hello_world", Slug.slugify("Hello!!!   World", '_'));
        }

        @Test
        void nullTextReturnsNull()
        {
            assertNull(Slug.slugify(null, '_'));
        }
    }

    @Nested
    @DisplayName("slugify(text, SlugOptions)")
    class OptionsOverload
    {

        @Test
        void nullOptionsFallsBackToDefaults() 
        {
            assertEquals("hello-world", Slug.slugify("Hello World", (Slug.Options) null));
        }

        @Test
        void lowercaseDisabledPreservesCase()
        {
            Slug.Options options = Slug.Options.builder()
                    .lowercase(false)
                    .build();
            assertEquals("Hello-World", Slug.slugify("Hello World", options));
        }

        @Test
        void asciiDisabledKeepsUnicodeLetters()
        {
            Slug.Options options = Slug.Options.builder()
                    .ascii(false)
                    .build();
            assertEquals("café-résumé", Slug.slugify("Café Résumé", options));
        }

        @Test
        void trimDisabledKeepsLeadingAndTrailingSeparators()
        {
            Slug.Options options = Slug.Options.builder()
                    .trim(false)
                    .build();
            assertEquals("-hello-", Slug.slugify("---Hello---", options));
        }

        @Test
        void collapseSeparatorsDisabledKeepsEachSeparatorInstance()
        {
            Slug.Options options = Slug.Options.builder()
                    .collapseSeparators(false)
                    .build();
            assertEquals("hello---world", Slug.slugify("Hello   World", options));
        }

        @Test
        void maxLengthTruncatesAndDropsDanglingSeparator()
        {
            Slug.Options options = Slug.Options.builder()
                    .maxLength(7)
                    .build();
            // "hello-world" truncated to 7 chars is "hello-w"; no dangling
            // separator to strip in this case, but length is respected.
            assertEquals("hello-w", Slug.slugify("Hello World", options));
        }

        @Test
        void maxLengthDropsTrailingSeparatorAfterCut()
        {
            Slug.Options options = Slug.Options.builder()
                    .maxLength(6)
                    .build();
            // "hello-world" truncated to 6 chars is "hello-"; the trailing
            // separator must be removed since trim() is enabled by default.
            assertEquals("hello", Slug.slugify("Hello World", options));
        }

        @Test
        void removeStopWordsDropsCommonEnglishWords()
        {
            Slug.Options options = Slug.Options.builder()
                    .removeStopWords(true)
                    .build();
            assertEquals("quick-brown-fox", Slug.slugify("The Quick and the Brown Fox", options));
        }

        @Test
        void removeStopWordsNeverReturnsEmptyResult()
        {
            Slug.Options options = Slug.Options.builder()
                    .removeStopWords(true)
                    .build();
            assertEquals("the-and-the", Slug.slugify("The and The", options));
        }

        @Test
        void customLocaleAffectsStopWordList()
        {
            Slug.Options options = Slug.Options.builder()
                    .removeStopWords(true)
                    .locale(new Locale("es"))
                    .build();
            assertEquals("hola-mundo", Slug.slugify("Hola y el Mundo", options));
        }
    }

    @Nested
    @DisplayName("SlugOptions defaults and builder")
    class SlugOptionsTests
    {

        @Test
        void defaultsMatchExpectedValues()
        {
            Slug.Options options = Slug.Options.defaults();
            assertEquals('-', options.separator);
            assertTrue(options.lowercase);
            assertTrue(options.ascii);
            assertTrue(options.trim);
            assertTrue(options.collapseSeparators);
            assertFalse(options.removeStopWords);
            assertEquals(0, options.maxLength);
            assertEquals(Locale.ENGLISH, options.locale);
        }

        @Test
        void builderOverridesIndividualFields()
        {
            Slug.Options options = Slug.Options.builder()
                    .separator('_')
                    .lowercase(false)
                    .ascii(false)
                    .trim(false)
                    .collapseSeparators(false)
                    .removeStopWords(true)
                    .maxLength(10)
                    .locale(Locale.FRENCH)
                    .build();

            assertEquals('_', options.separator);
            assertFalse(options.lowercase);
            assertFalse(options.ascii);
            assertFalse(options.trim);
            assertFalse(options.collapseSeparators);
            assertTrue(options.removeStopWords);
            assertEquals(10, options.maxLength);
            assertEquals(Locale.FRENCH, options.locale);
        }
    }

    @Nested
    @DisplayName("isSlug(text)")
    class IsSlugTests
    {

        @Test
        void nullIsNotASlug()
        {
            assertFalse(Slug.isSlug(null));
        }

        @Test
        void emptyIsNotASlug()
        {
            assertFalse(Slug.isSlug(""));
        }

        @Test
        void wellFormedSlugIsValid()
        {
            assertTrue(Slug.isSlug("hello-world"));
        }

        @Test
        void singleWordIsValid()
        {
            assertTrue(Slug.isSlug("hello"));
        }

        @Test
        void uppercaseIsNotValid()
        {
            assertFalse(Slug.isSlug("Hello-World"));
        }

        @Test
        void leadingSeparatorIsNotValid()
        {
            assertFalse(Slug.isSlug("-hello-world"));
        }

        @Test
        void trailingSeparatorIsNotValid()
        {
            assertFalse(Slug.isSlug("hello-world-"));
        }

        @Test
        void doubleSeparatorIsNotValid()
        {
            assertFalse(Slug.isSlug("hello--world"));
        }

        @Test
        void spacesAreNotValid()
        {
            assertFalse(Slug.isSlug("hello world"));
        }
    }

    @Nested
    @DisplayName("normalize(slug)")
    class NormalizeTests
    {

        @Test
        void nullReturnsNull()
        {
            assertNull(Slug.normalize(null));
        }

        @Test
        void cleansUpMixedCaseAndSeparators()
        {
            assertEquals("hello-world", Slug.normalize("Hello_World"));
        }

        @Test
        void collapsesDuplicatedSeparators()
        {
            assertEquals("hello-world", Slug.normalize("hello---world"));
        }

        @Test
        void alreadyNormalizedSlugIsUnchanged()
        {
            assertEquals("hello-world", Slug.normalize("hello-world"));
        }
    }

    @Nested
    @DisplayName("title(slug)")
    class TitleTests
    {

        @Test
        void nullReturnsNull()
        {
            assertNull(Slug.title(null));
        }

        @Test
        void emptyReturnsEmpty()
        {
            assertEquals("", Slug.title(""));
        }

        @Test
        void convertsSlugToTitleCase()
        {
            assertEquals("Hello World", Slug.title("hello-world"));
        }

        @Test
        void singleWordIsCapitalized()
        {
            assertEquals("Java", Slug.title("java"));
        }

        @Test
        void numbersArePreserved()
        {
            assertEquals("Java 21", Slug.title("java-21"));
        }
    }

    @Nested
    @DisplayName("humanize(slug)")
    class HumanizeTests
    {

        @Test
        void nullReturnsNull()
        {
            assertNull(Slug.humanize(null));
        }

        @Test
        void emptyReturnsEmpty()
        {
            assertEquals("", Slug.humanize(""));
        }

        @Test
        void convertsSlugToSentenceCase()
        {
            assertEquals("Hello world", Slug.humanize("hello-world"));
        }

        @Test
        void singleWordIsCapitalizedOnlyAtStart()
        {
            assertEquals("Java", Slug.humanize("java"));
        }

        @Test
        void onlyFirstLetterIsCapitalized()
        {
            assertEquals("Hello world wide web", Slug.humanize("hello-world-wide-web"));
        }
    }
}
