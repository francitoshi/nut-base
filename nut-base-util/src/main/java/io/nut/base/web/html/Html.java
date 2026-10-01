/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.web.html;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class provides static factory methods for creating an HTML document
 * tree using a fluent, hierarchical style similar to kotlinx.html, e.g.
 * <pre>
 * import static io.nut.base.web.html.Html.*;
 *
 * String page = html(
 *     head(
 *         title("Test"),
 *         meta("charset", "UTF-8")
 *     ),
 *     body(
 *         h1("Hello"),
 *         p("Lorem ipsum"),
 *         ul(
 *             li("Uno"),
 *             li("Dos"),
 *             li("Tres")
 *         )
 *     )
 * ).render();
 * </pre>
 *
 * The content of any tag is a vararg of {@code Object}s: {@link String}s (or
 * other values) are rendered as escaped text, {@link HtmlTag}s as child
 * elements, {@link #attrs} as attributes and {@link #raw} as unescaped HTML.
 *
 * @author franci
 */
public final class Html
{
    private Html()
    {
        // Prevent instantiation
    }

    /** The HTML void elements, which never have children or a closing tag. */
    private static final Set<String> VOID_TAGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")));

    /**
     * Creates a tag with an arbitrary name, for custom elements.
     *
     * @param name    the tag name
     * @param content the attributes and child content
     * @return a new {@link HtmlTag}
     */
    public static HtmlTag tag(String name, Object... content)
    {
        return new HtmlTag(name, content);
    }

    // ------------------------------------------------------------------
    // document structure
    // ------------------------------------------------------------------

    /** Creates an {@code <html>} element. */
    public static HtmlTag html(Object... content)
    {
        return tag("html", content);
    }

    /** Creates a {@code <head>} element. */
    public static HtmlTag head(Object... content)
    {
        return tag("head", content);
    }

    /** Creates a {@code <body>} element. */
    public static HtmlTag body(Object... content)
    {
        return tag("body", content);
    }

    /** Creates a {@code <title>} element. */
    public static HtmlTag title(Object... content)
    {
        return tag("title", content);
    }

    /** Creates a {@code <meta>} element with a single attribute. */
    public static HtmlTag meta(String name, String value)
    {
        return tag("meta").attr(name, value);
    }

    /** Creates a {@code <meta>} element. */
    public static HtmlTag meta(Object... content)
    {
        return tag("meta", content);
    }

    // ------------------------------------------------------------------
    // headings
    // ------------------------------------------------------------------

    /** Creates an {@code <h1>} element. */
    public static HtmlTag h1(Object... content)
    {
        return tag("h1", content);
    }

    /** Creates an {@code <h2>} element. */
    public static HtmlTag h2(Object... content)
    {
        return tag("h2", content);
    }

    /** Creates an {@code <h3>} element. */
    public static HtmlTag h3(Object... content)
    {
        return tag("h3", content);
    }

    /** Creates an {@code <h4>} element. */
    public static HtmlTag h4(Object... content)
    {
        return tag("h4", content);
    }

    /** Creates an {@code <h5>} element. */
    public static HtmlTag h5(Object... content)
    {
        return tag("h5", content);
    }

    /** Creates an {@code <h6>} element. */
    public static HtmlTag h6(Object... content)
    {
        return tag("h6", content);
    }

    // ------------------------------------------------------------------
    // text and phrasing
    // ------------------------------------------------------------------

    /** Creates a {@code <p>} element. */
    public static HtmlTag p(Object... content)
    {
        return tag("p", content);
    }

    /** Creates a {@code <span>} element. */
    public static HtmlTag span(Object... content)
    {
        return tag("span", content);
    }

    /** Creates a {@code <div>} element. */
    public static HtmlTag div(Object... content)
    {
        return tag("div", content);
    }

    /** Creates a {@code <strong>} element. */
    public static HtmlTag strong(Object... content)
    {
        return tag("strong", content);
    }

    /** Creates an {@code <em>} element. */
    public static HtmlTag em(Object... content)
    {
        return tag("em", content);
    }

    /** Creates a {@code <b>} element. */
    public static HtmlTag b(Object... content)
    {
        return tag("b", content);
    }

    /** Creates an {@code <i>} element. */
    public static HtmlTag i(Object... content)
    {
        return tag("i", content);
    }

    /** Creates a {@code <small>} element. */
    public static HtmlTag small(Object... content)
    {
        return tag("small", content);
    }

    /** Creates a {@code <code>} element. */
    public static HtmlTag code(Object... content)
    {
        return tag("code", content);
    }

    /** Creates a {@code <pre>} element. */
    public static HtmlTag pre(Object... content)
    {
        return tag("pre", content);
    }

    /** Creates a {@code <blockquote>} element. */
    public static HtmlTag blockquote(Object... content)
    {
        return tag("blockquote", content);
    }

    /** Creates an {@code <a>} element. */
    public static HtmlTag a(Object... content)
    {
        return tag("a", content);
    }

    // ------------------------------------------------------------------
    // lists
    // ------------------------------------------------------------------

    /** Creates a {@code <ul>} element. */
    public static HtmlTag ul(Object... content)
    {
        return tag("ul", content);
    }

    /** Creates an {@code <ol>} element. */
    public static HtmlTag ol(Object... content)
    {
        return tag("ol", content);
    }

    /** Creates a {@code <li>} element. */
    public static HtmlTag li(Object... content)
    {
        return tag("li", content);
    }

    /** Creates a {@code <dl>} element. */
    public static HtmlTag dl(Object... content)
    {
        return tag("dl", content);
    }

    /** Creates a {@code <dt>} element. */
    public static HtmlTag dt(Object... content)
    {
        return tag("dt", content);
    }

    /** Creates a {@code <dd>} element. */
    public static HtmlTag dd(Object... content)
    {
        return tag("dd", content);
    }

    // ------------------------------------------------------------------
    // media and void elements
    // ------------------------------------------------------------------

    /** Creates an {@code <img>} element (void). */
    public static HtmlTag img(Object... content)
    {
        return tag("img", content);
    }

    /** Creates a {@code <br>} element (void). */
    public static HtmlTag br(Object... content)
    {
        return tag("br", content);
    }

    /** Creates an {@code <hr>} element (void). */
    public static HtmlTag hr(Object... content)
    {
        return tag("hr", content);
    }

    // ------------------------------------------------------------------
    // tables
    // ------------------------------------------------------------------

    /** Creates a {@code <table>} element. */
    public static HtmlTag table(Object... content)
    {
        return tag("table", content);
    }

    /** Creates a {@code <thead>} element. */
    public static HtmlTag thead(Object... content)
    {
        return tag("thead", content);
    }

    /** Creates a {@code <tbody>} element. */
    public static HtmlTag tbody(Object... content)
    {
        return tag("tbody", content);
    }

    /** Creates a {@code <tfoot>} element. */
    public static HtmlTag tfoot(Object... content)
    {
        return tag("tfoot", content);
    }

    /** Creates a {@code <tr>} element. */
    public static HtmlTag tr(Object... content)
    {
        return tag("tr", content);
    }

    /** Creates a {@code <th>} element. */
    public static HtmlTag th(Object... content)
    {
        return tag("th", content);
    }

    /** Creates a {@code <td>} element. */
    public static HtmlTag td(Object... content)
    {
        return tag("td", content);
    }

    // ------------------------------------------------------------------
    // forms
    // ------------------------------------------------------------------

    /** Creates a {@code <form>} element. */
    public static HtmlTag form(Object... content)
    {
        return tag("form", content);
    }

    /** Creates an {@code <input>} element (void). */
    public static HtmlTag input(Object... content)
    {
        return tag("input", content);
    }

    /** Creates a {@code <label>} element. */
    public static HtmlTag label(Object... content)
    {
        return tag("label", content);
    }

    /** Creates a {@code <button>} element. */
    public static HtmlTag button(Object... content)
    {
        return tag("button", content);
    }

    /** Creates a {@code <select>} element. */
    public static HtmlTag select(Object... content)
    {
        return tag("select", content);
    }

    /** Creates an {@code <option>} element. */
    public static HtmlTag option(Object... content)
    {
        return tag("option", content);
    }

    /** Creates a {@code <textarea>} element. */
    public static HtmlTag textarea(Object... content)
    {
        return tag("textarea", content);
    }

    // ------------------------------------------------------------------
    // semantic sections
    // ------------------------------------------------------------------

    /** Creates a {@code <section>} element. */
    public static HtmlTag section(Object... content)
    {
        return tag("section", content);
    }

    /** Creates an {@code <article>} element. */
    public static HtmlTag article(Object... content)
    {
        return tag("article", content);
    }

    /** Creates an {@code <aside>} element. */
    public static HtmlTag aside(Object... content)
    {
        return tag("aside", content);
    }

    /** Creates a {@code <header>} element. */
    public static HtmlTag header(Object... content)
    {
        return tag("header", content);
    }

    /** Creates a {@code <footer>} element. */
    public static HtmlTag footer(Object... content)
    {
        return tag("footer", content);
    }

    /** Creates a {@code <nav>} element. */
    public static HtmlTag nav(Object... content)
    {
        return tag("nav", content);
    }

    /** Creates a {@code <main>} element. */
    public static HtmlTag main(Object... content)
    {
        return tag("main", content);
    }

    // ------------------------------------------------------------------
    // raw and attributes
    // ------------------------------------------------------------------

    /**
     * Declares a set of attributes as name/value pairs.
     *
     * @param nameValuePairs an even number of {@code name, value} pairs
     * @return an object to be passed as content to a tag factory method
     * @throws IllegalArgumentException if the number of arguments is odd
     */
    public static Attrs attrs(String... nameValuePairs)
    {
        return new Attrs(nameValuePairs);
    }

    /**
     * Wraps a string that must be inserted verbatim without HTML escaping.
     *
     * @param html the raw HTML
     * @return an object to be passed as content to a tag factory method
     */
    public static Raw raw(String html)
    {
        return new Raw(html);
    }

    /**
     * The attribute set built by {@link #attrs}.
     */
    public static final class Attrs
    {
        final Map<String, String> map = new LinkedHashMap<>();

        Attrs(String... nameValuePairs)
        {
            if (nameValuePairs != null)
            {
                if (nameValuePairs.length % 2 != 0)
                {
                    throw new IllegalArgumentException("attrs requires an even number of name/value pairs");
                }
                for (int i = 0; i < nameValuePairs.length; i += 2)
                {
                    map.put(nameValuePairs[i], nameValuePairs[i + 1]);
                }
            }
        }
    }

    /**
     * The raw HTML wrapper built by {@link #raw}.
     */
    public static final class Raw
    {
        final String html;

        Raw(String html)
        {
            this.html = Objects.requireNonNull(html, "html");
        }
    }

    static boolean isVoid(String name)
    {
        return VOID_TAGS.contains(name);
    }
}