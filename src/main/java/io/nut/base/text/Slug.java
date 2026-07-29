/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.text;

import io.nut.base.util.As;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for creating and manipulating URL-friendly "slugs".
 *
 * <p>A slug is a lowercase, ASCII-only, hyphen-separated representation of a
 * piece of text, typically used in URLs (e.g. "Hello World!" -&gt; "hello-world").</p>
 *
 * <p>This class is stateless and thread-safe: every method is static and pure.</p>
 */
public final class Slug
{

    /** Matches Unicode combining marks left behind after NFD normalization. */
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");

    /** Canonical slug format: lowercase alphanumerics separated by single hyphens. */
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    /** Minimal stop-word lists, keyed by ISO language code. */
    private static final Map<String, Set<String>> STOP_WORDS = 
            As.map("en", As.set("a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"),
                   "es", As.set("el", "la", "los", "las", "un", "una", "unos", "unas","y", "o", "de", "en", "por", "para", "con"));

    private Slug()
    {
        // Utility class: no instances.
    }

    /**
     * Converts {@code text} into a slug using the default options
     * (hyphen separator, lowercase, ASCII-folded, trimmed, collapsed separators).
     *
     * @param text the input text, may be {@code null}
     * @return the resulting slug, or {@code null} if {@code text} is {@code null}
     */
    public static String slugify(String text)
    {
        return slugify(text, Options.defaults());
    }

    /**
     * Converts {@code text} into a slug using the default options but with a
     * custom separator character.
     *
     * @param text      the input text, may be {@code null}
     * @param separator the character used to join words
     * @return the resulting slug, or {@code null} if {@code text} is {@code null}
     */
    public static String slugify(String text, char separator) 
    {
        return slugify(text, Options.builder().separator(separator).build());
    }

    /**
     * Converts {@code text} into a slug using the given {@link Options}.
     *
     * @param text    the input text, may be {@code null}
     * @param options the options controlling the conversion; if {@code null},
     *                {@link Options#defaults()} is used
     * @return the resulting slug, or {@code null} if {@code text} is {@code null}
     */
    public static String slugify(String text, Options options)
    {
        if (text == null)
        {
            return null;
        }
        Options opts = options != null ? options : Options.defaults();

        String result = text;

        // 1) Fold accented/diacritical characters down to plain ASCII letters,
        //    e.g. "café" -> "cafe", "Ñandú" -> "Nandu".
        if (opts.isAscii()) 
        {
            result = Normalizer.normalize(result, Normalizer.Form.NFD);
            result = DIACRITICS_PATTERN.matcher(result).replaceAll("");
        }

        // 2) Lowercase, using the configured locale (important for locale-sensitive
        //    casing rules, e.g. Turkish "I").
        if (opts.isLowercase()) 
        {
            result = result.toLowerCase(opts.locale);
        }

        // 3) Replace everything that is not a "word" character with the separator.
        //    When ascii-folding is enabled we only accept plain ASCII letters/digits
        //    (so leftover non-ASCII symbols, emoji, etc. are treated as separators).
        //    Otherwise we accept any Unicode letter or digit.
        String nonWordClass = opts.isAscii() ? "[^a-zA-Z0-9]" : "[^\\p{L}\\p{N}]";
        String quantifier = opts.collapseSeparators ? "+" : "";
        String separatorStr = String.valueOf(opts.separator);
        // Matcher.quoteReplacement guards against separators such as '$' or '\'
        // that would otherwise be interpreted as replacement-string metacharacters.
        result = result.replaceAll(nonWordClass + quantifier,
                java.util.regex.Matcher.quoteReplacement(separatorStr));

        // 4) Trim leading/trailing separators.
        if (opts.trim && !separatorStr.isEmpty())
        {
            String quotedSep = Pattern.quote(separatorStr);
            result = result.replaceAll("^(?:" + quotedSep + ")+", "");
            result = result.replaceAll("(?:" + quotedSep + ")+$", "");
        }

        // 5) Optionally drop common stop words (a, the, and, ...).
        if (opts.removeStopWords && !result.isEmpty())
        {
            result = removeStopWords(result, opts);
        }

        // 6) Enforce a maximum length without leaving a dangling separator.
        if (opts.maxLength > 0 && result.length() > opts.maxLength)
        {
            result = result.substring(0, opts.maxLength);
            if (opts.trim && !separatorStr.isEmpty())
            {
                result = result.replaceAll(Pattern.quote(separatorStr) + "+$", "");
            }
        }

        return result;
    }

    /**
     * Checks whether {@code text} is already a well-formed slug: non-null,
     * non-empty, lowercase, ASCII alphanumeric words joined by single hyphens,
     * with no leading, trailing, or duplicated hyphens.
     *
     * @param text the text to check
     * @return {@code true} if {@code text} is a valid slug
     */
    public static boolean isSlug(String text)
    {
        return text != null && !text.isEmpty() && SLUG_PATTERN.matcher(text).matches();
    }

    /**
     * Re-slugifies an already-existing slug using the default options. Useful
     * for cleaning up slugs that may have been produced by another system, or
     * that have drifted from the canonical format (mixed case, underscores,
     * repeated separators, etc.).
     *
     * @param slug the slug-like text to normalize, may be {@code null}
     * @return the normalized slug, or {@code null} if {@code slug} is {@code null}
     */
    public static String normalize(String slug)
    {
        return slugify(slug);
    }

    /**
     * Converts a slug into a Title Case phrase, e.g. {@code "hello-world"} -&gt;
     * {@code "Hello World"}.
     *
     * @param slug the slug to convert, may be {@code null}
     * @return the title-cased phrase, or {@code null} if {@code slug} is {@code null}
     */
    public static String title(String slug)
    {
        if (slug == null)
        {
            return null;
        }
        if (slug.isEmpty())
        {
            return "";
        }
        String[] parts = slug.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts)
        {
            if (part.isEmpty())
            {
                continue;
            }
            if (sb.length() > 0)
            {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1)
            {
                sb.append(part.substring(1).toLowerCase(Locale.ENGLISH));
            }
        }
        return sb.toString();
    }

    /**
     * Converts a slug into a human-readable sentence fragment, e.g.
     * {@code "hello-world"} -&gt; {@code "Hello world"}. Unlike {@link #title},
     * only the first character is capitalized.
     *
     * @param slug the slug to convert, may be {@code null}
     * @return the humanized text, or {@code null} if {@code slug} is {@code null}
     */
    public static String humanize(String slug)
    {
        if (slug == null)
        {
            return null;
        }
        if (slug.isEmpty())
        {
            return "";
        }
        String text = slug.replace('-', ' ').toLowerCase(Locale.ENGLISH).trim();
        if (text.isEmpty())
        {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String removeStopWords(String slugText, Options opts)
    {
        String separatorStr = String.valueOf(opts.separator);
        Set<String> stopWords = STOP_WORDS.getOrDefault((opts.locale).getLanguage(), STOP_WORDS.get("en"));

        String[] parts = slugText.split(Pattern.quote(separatorStr));
        List<String> kept = new ArrayList<>();
        for (String part : parts)
        {
            if (!part.isEmpty() && !stopWords.contains(part))
            {
                kept.add(part);
            }
        }
        // Never return an empty slug just because every word was a stop word.
        return kept.isEmpty() ? slugText : String.join(separatorStr, kept);
    }

    public static final class Options
    {
        public final char separator;
        public final boolean lowercase;
        public final boolean ascii;
        public final boolean trim;
        public final boolean collapseSeparators;
        public final boolean removeStopWords;
        public final int maxLength;
        public final Locale locale;

        public Options(char separator, boolean lowercase, boolean ascii, boolean trim, boolean collapseSeparators, boolean removeStopWords, int maxLength, Locale locale)
        {
            this.separator = separator;
            this.lowercase = lowercase;
            this.ascii = ascii;
            this.trim = trim;
            this.collapseSeparators = collapseSeparators;
            this.removeStopWords = removeStopWords;
            this.maxLength = maxLength;
            this.locale = locale;
        }
    
        /**
         * @return the default options: {@code '-'} separator, lowercase, ASCII
         * folding, trimming, and separator collapsing all enabled, stop words
         * kept, no maximum length, {@link Locale#ENGLISH}.
         */
        public static Options defaults() 
        {
            return new Options('-', true, true, true, true, false, 0, Locale.ENGLISH);
        }
        /**
         * @return a new {@link Builder} pre-populated with the default options.
         */
        public static Builder builder()
        {
            return new Builder();
        }

        public boolean isAscii()
        {
            return ascii;
        }

        public boolean isLowercase()
        {
            return lowercase;
        }


        
    }

    /** *  Fluent builder for {@link Options}. */
    public static final class Builder 
    {
        private char separator = '-';
        private boolean lowercase = true;
        private boolean ascii = true;
        private boolean trim = true;
        private boolean collapseSeparators = true;
        private boolean removeStopWords = false;
        private int maxLength = 0;
        private Locale locale = Locale.ENGLISH;

        private Builder() 
        {
        }

        public Builder separator(char separator)
        {
            this.separator = separator;
            return this;
        }

        public Builder lowercase(boolean lowercase)
        {
            this.lowercase = lowercase;
            return this;
        }

        public Builder ascii(boolean ascii)
        {
            this.ascii = ascii;
            return this;
        }

        public Builder trim(boolean trim)
        {
            this.trim = trim;
            return this;
        }

        public Builder collapseSeparators(boolean collapseSeparators)
        {
            this.collapseSeparators = collapseSeparators;
            return this;
        }

        public Builder removeStopWords(boolean removeStopWords)
        {
            this.removeStopWords = removeStopWords;
            return this;
        }

        public Builder maxLength(int maxLength)
        {
            this.maxLength = maxLength;
            return this;
        }

        public Builder locale(Locale locale)
        {
            this.locale = locale;
            return this;
        }

        public Options build()
        {
            return new Options(separator, lowercase, ascii, trim, collapseSeparators, removeStopWords, maxLength, locale);
        }
    }
}
