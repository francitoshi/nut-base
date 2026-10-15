/*
 *  CssTest.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *  SPDX-License-Identifier: GPL-3.0-or-later
 *  See LICENSE file in the project root for full license text.
 */
package io.nut.base.web.css;

import static io.nut.base.web.css.Css.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Css}.
 */
public class CssTest
{
    /**
     * Test of the documented usage example.
     */
    @Test
    public void testExample()
    {
        String css = styleSheet(
                rule("body",
                        css("color", "#333"),
                        css("background-color", "white")
                ),
                rule("h1, h2",
                        css("font-family", "sans-serif"),
                        css("margin-bottom", "16px")
                ),
                media("(max-width: 600px)",
                        rule("body",
                                css("font-size", "14px")
                        )
                )
        ).render();

        assertEquals("body{color:#333;background-color:white;}"
                + "h1, h2{font-family:sans-serif;margin-bottom:16px;}"
                + "@media (max-width: 600px){body{font-size:14px;}}", css);
    }

    /**
     * Test of a single rule.
     */
    @Test
    public void testSingleRule()
    {
        assertEquals("a{text-decoration:none;}", rule("a", css("text-decoration", "none")).render());
    }

    /**
     * Test of numeric values rendered through String.valueOf.
     */
    @Test
    public void testNumericValues()
    {
        assertEquals("p{margin:0;}", rule("p", css("margin", 0)).render());
    }

    /**
     * Test of render(Appendable) and toString().
     */
    @Test
    public void testRenderable() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        styleSheet(rule("p", css("color", "red"))).render(sb);
        assertEquals("p{color:red;}", sb.toString());
        assertEquals("p{color:red;}", styleSheet(rule("p", css("color", "red"))).toString());
    }

    /**
     * Test of invalid content being rejected.
     */
    @Test
    public void testInvalidContent()
    {
        assertThrows(IllegalArgumentException.class, () -> styleSheet("not a rule"));
        assertThrows(IllegalArgumentException.class, () -> rule("p", css("color", "red"), "not a declaration"));
    }
}