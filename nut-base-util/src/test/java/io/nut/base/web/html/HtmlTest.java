/*
 *  HtmlTest.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *  SPDX-License-Identifier: GPL-3.0-or-later
 *  See LICENSE file in the project root for full license text.
 */
package io.nut.base.web.html;

import static io.nut.base.web.html.Html.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Html} and {@link HtmlTag}.
 */
public class HtmlTest
{
    /**
     * Test of the documented usage example.
     */
    @Test
    public void testExample()
    {
        String html = html(
                head(
                        title("Test"),
                        meta("charset", "UTF-8")
                ),
                body(
                        h1("Hello"),
                        p("Lorem ipsum"),
                        ul(
                                li("Uno"),
                                li("Dos"),
                                li("Tres")
                        )
                )
        ).render();

        assertEquals("<html><head><title>Test</title><meta charset=\"UTF-8\"></head>"
                + "<body><h1>Hello</h1><p>Lorem ipsum</p>"
                + "<ul><li>Uno</li><li>Dos</li><li>Tres</li></ul></body></html>", html);
    }

    /**
     * Test of generic tags.
     */
    @Test
    public void testCustomTag()
    {
        assertEquals("<custom id=\"x\">text</custom>", tag("custom", attrs("id", "x"), "text").render());
    }

    /**
     * Test of attributes.
     */
    @Test
    public void testAttributes()
    {
        assertEquals("<a href=\"https://example.com\" class=\"link\">go</a>",
                a(attrs("href", "https://example.com", "class", "link"), "go").render());
        assertEquals("<div id=\"main\" data-on=\"true\">x</div>",
                div(attrs("id", "main"), "x").attr("data-on", true).render());
    }

    /**
     * Test of void elements.
     */
    @Test
    public void testVoidElements()
    {
        assertEquals("<br>", br().render());
        assertEquals("<hr>", hr().render());
        assertEquals("<img src=\"a.png\" alt=\"a\">", img(attrs("src", "a.png", "alt", "a")).render());
    }

    /**
     * Test of text escaping.
     */
    @Test
    public void testEscaping()
    {
        assertEquals("<p>a &amp; b &lt; c</p>", p("a & b < c").render());
        assertEquals("<p title=\"&quot;q&quot; &amp; x\">t</p>", p("t").attr("title", "\"q\" & x").render());
    }

    /**
     * Test of raw elements.
     */
    @Test
    public void testRaw()
    {
        assertEquals("<div><b>bold</b></div>", div(raw("<b>bold</b>")).render());
    }

    /**
     * Test of a mixed numeric and string content.
     */
    @Test
    public void testMixedContent()
    {
        assertEquals("<p>2 + 3 = 5</p>", p("2 + 3 = ", 2 + 3).render());
    }

    /**
     * Test of render(Appendable) and toString().
     */
    @Test
    public void testRenderable() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        p("hi").render(sb);
        assertEquals("<p>hi</p>", sb.toString());
        assertEquals("<p>hi</p>", p("hi").toString());
    }

    /**
     * Test of dynamically appending content after creation.
     */
    @Test
    public void testDynamicContent()
    {
        HtmlTag list = ul();
        for (String item : new String[] { "Uno", "Dos", "Tres" })
        {
            list.append(li(item));
        }
        list.append(attrs("class", "lst"));
        assertEquals("<ul class=\"lst\"><li>Uno</li><li>Dos</li><li>Tres</li></ul>", list.render());

        HtmlTag div = div("a").append("b").append(p("c"));
        assertEquals("<div>ab<p>c</p></div>", div.render());
    }
}
