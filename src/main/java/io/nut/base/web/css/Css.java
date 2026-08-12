/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.web.css;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Static factory methods for building a CSS stylesheet with a fluent,
 * hierarchical style equivalent to {@link io.nut.base.web.html.Html}, e.g.
 * <pre>
 * import static io.nut.base.web.css.Css.*;
 *
 * String css = styleSheet(
 *     rule("body",
 *         css("color", "#333"),
 *         css("background-color", "white")
 *     ),
 *     rule("h1, h2",
 *         css("font-family", "sans-serif"),
 *         css("margin-bottom", "16px")
 *     ),
 *     media("(max-width: 600px)",
 *         rule("body",
 *             css("font-size", "14px")
 *         )
 *     )
 * ).render();
 * </pre>
 * Rendered output is compact, e.g.
 * {@code body{color:#333;background-color:white;}h1,h2{font-family:sans-serif;margin-bottom:16px;}}.
 *
 * @author franci
 */
public final class Css
{
    private Css()
    {
        // Prevent instantiation
    }

    /**
     * Creates a stylesheet from the given rules.
     *
     * @param rules the top-level rules of the stylesheet
     * @return a new {@link Sheet}
     */
    public static Sheet styleSheet(Object... rules)
    {
        return new Sheet(rules);
    }

    /**
     * Creates a style rule applying the given declarations to the selector.
     *
     * @param selector     the CSS selector, e.g. {@code "body"} or {@code "h1, h2"}
     * @param declarations the declarations and/or nested rules
     * @return a new {@link Rule}
     */
    public static Rule rule(String selector, Object... declarations)
    {
        return new Rule(selector, declarations);
    }

    /**
     * Creates a media query rule wrapping the given rules.
     *
     * @param query  the media condition, e.g. {@code "(max-width: 600px)"}
     * @param rules  the rules enclosed by the media query
     * @return a new {@link Rule}
     */
    public static Rule media(String query, Object... rules)
    {
        return new Rule("@media", query, rules);
    }

    /**
     * Creates a single declaration, e.g. {@code color: #333;}.
     *
     * @param property the CSS property name
     * @param value    the property value, rendered with {@link String#valueOf}
     * @return a new {@link Decl}
     */
    public static Decl css(String property, Object value)
    {
        return new Decl(property, value);
    }

    /**
     * A single declaration {@code property: value}, built by {@link #css}.
     */
    public static final class Decl
    {
        final String property;
        final String value;

        Decl(String property, Object value)
        {
            this.property = Objects.requireNonNull(property, "property");
            this.value = String.valueOf(Objects.requireNonNull(value, "value"));
        }

        void render(Appendable out) throws IOException
        {
            out.append(property).append(':').append(value).append(';');
        }
    }

    /**
     * A stylesheet holding a list of rules, built by {@link #styleSheet}.
     */
    public static final class Sheet
    {
        private final List<Rule> rules = new ArrayList<>();

        Sheet(Object... content)
        {
            addAll(content);
        }

        private void addAll(Object... content)
        {
            if (content != null)
            {
                for (Object item : content)
                {
                    if (item instanceof Rule)
                    {
                        rules.add((Rule) item);
                    }
                    else if (item != null)
                    {
                        throw new IllegalArgumentException("expected a rule but got " + item.getClass().getName());
                    }
                }
            }
        }

        /**
         * Renders this stylesheet to a string.
         *
         * @return the CSS string
         */
        public String render()
        {
            StringBuilder out = new StringBuilder();
            try
            {
                render(out);
            }
            catch (IOException cannotHappen)
            {
                throw new AssertionError(cannotHappen);
            }
            return out.toString();
        }

        /**
         * Renders this stylesheet to the given {@link Appendable}.
         *
         * @param out the target appendable
         * @throws IOException if an I/O error occurs
         */
        public void render(Appendable out) throws IOException
        {
            for (Rule rule : rules)
            {
                rule.render(out);
            }
        }

        /**
         * Renders this stylesheet, equivalent to {@link #render()}.
         *
         * @return the CSS string
         */
        @Override
        public String toString()
        {
            return render();
        }
    }

    /**
     * A CSS rule: a selector (or at-rule header) followed by its declarations
     * and nested rules, built by {@link #rule} or {@link #media}.
     */
    public static final class Rule
    {
        private final String selector;
        private final String params;
        private final List<Decl> declarations = new ArrayList<>();
        private final List<Rule> nested = new ArrayList<>();

        Rule(String selector, String params, Object... content)
        {
            this.selector = Objects.requireNonNull(selector, "selector");
            this.params = params;
            if (content != null)
            {
                for (Object item : content)
                {
                    if (item instanceof Decl)
                    {
                        declarations.add((Decl) item);
                    }
                    else if (item instanceof Rule)
                    {
                        nested.add((Rule) item);
                    }
                    else if (item != null)
                    {
                        throw new IllegalArgumentException("expected a declaration or rule but got "
                                + item.getClass().getName());
                    }
                }
            }
        }

        Rule(String selector, Object... content)
        {
            this(selector, null, content);
        }

        /**
         * Renders this rule to a string.
         *
         * @return the CSS string
         */
        public String render()
        {
            StringBuilder out = new StringBuilder();
            try
            {
                render(out);
            }
            catch (IOException cannotHappen)
            {
                throw new AssertionError(cannotHappen);
            }
            return out.toString();
        }

        /**
         * Renders this rule and its nested rules to the given {@link Appendable}.
         *
         * @param out the target appendable
         * @throws IOException if an I/O error occurs
         */
        public void render(Appendable out) throws IOException
        {
            if (params != null)
            {
                out.append(selector).append(' ').append(params);
            }
            else
            {
                out.append(selector);
            }
            out.append('{');
            for (Decl declaration : declarations)
            {
                declaration.render(out);
            }
            for (Rule rule : nested)
            {
                rule.render(out);
            }
            out.append('}');
        }

        /**
         * Renders this rule, equivalent to {@link #render()}.
         *
         * @return the CSS string
         */
        @Override
        public String toString()
        {
            return render();
        }
    }
}