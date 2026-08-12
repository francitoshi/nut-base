/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.web.html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single HTML element holding a tag name, a set of attributes and a list of
 * child segments (text, raw HTML or nested elements). Instances are created
 * through the factory methods of {@link Html} and rendered with
 * {@link #render()}.
 *
 * <p>Void elements such as {@code meta}, {@code br} or {@code img} are
 * rendered without a closing tag and their children (if any) are ignored.</p>
 *
 * @author franci
 */
public final class HtmlTag
{
    private static final class Segment
    {
        enum Kind { TEXT, RAW }

        final Kind kind;
        final String data;
        final HtmlTag child;

        Segment(Kind kind, String data)
        {
            this.kind = kind;
            this.data = data;
            this.child = null;
        }

        Segment(HtmlTag child)
        {
            this.kind = Kind.RAW; // unused for children
            this.data = null;
            this.child = child;
        }
    }

    private final String name;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<Segment> segments = new ArrayList<>();

    /**
     * Creates a new element. This constructor is intended to be called from
     * the factory methods of {@link Html}; every content item may be a
     * {@link String} (or any other value, rendered as escaped text), an
     * {@link HtmlTag} (a child element), an {@link Html.Attrs} (attributes) or
     * an {@link Html.Raw} (unescaped HTML).
     *
     * @param name    the tag name
     * @param content the attributes and child content
     */
    public HtmlTag(String name, Object... content)
    {
        this.name = Objects.requireNonNull(name, "name");
        if (content != null)
        {
            for (Object item : content)
            {
                add(item);
            }
        }
    }

    private void add(Object item)
    {
        if (item == null)
        {
            return;
        }
        if (item instanceof Html.Attrs)
        {
            attributes.putAll(((Html.Attrs) item).map);
        }
        else if (item instanceof Html.Raw)
        {
            segments.add(new Segment(Segment.Kind.RAW, ((Html.Raw) item).html));
        }
        else if (item instanceof HtmlTag)
        {
            segments.add(new Segment((HtmlTag) item));
        }
        else
        {
            segments.add(new Segment(Segment.Kind.TEXT, String.valueOf(item)));
        }
    }

    /**
     * Appends more attribute(s) and child content to this element, useful for
     * building content dynamically, e.g. in a loop. Accepts the same kinds of
     * items as the factory methods of {@link Html}.
     *
     * @param content the attributes and child content to append
     * @return this element, for chaining
     */
    public HtmlTag append(Object... content)
    {
        if (content != null)
        {
            for (Object item : content)
            {
                add(item);
            }
        }
        return this;
    }

    /**
     * Adds an attribute. The value is HTML-escaped when rendered.
     *
     * @param name  the attribute name
     * @param value the attribute value
     * @return this element, for chaining
     * @throws NullPointerException if {@code name} or {@code value} is {@code null}
     */
    public HtmlTag attr(String name, String value)
    {
        attributes.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
        return this;
    }

    /**
     * Adds an attribute with a boolean value, rendered as {@code "true"} or
     * {@code "false"}.
     *
     * @param name  the attribute name
     * @param value the attribute value
     * @return this element, for chaining
     */
    public HtmlTag attr(String name, boolean value)
    {
        return attr(name, String.valueOf(value));
    }

    /**
     * Renders this element and all its children to a string.
     *
     * @return the HTML string
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
     * Renders this element and all its children to the given {@link Appendable}.
     *
     * @param out the target appendable
     * @throws IOException if an I/O error occurs
     */
    public void render(Appendable out) throws IOException
    {
        out.append('<').append(name);
        for (Map.Entry<String, String> entry : attributes.entrySet())
        {
            out.append(' ').append(entry.getKey()).append("=\"");
            appendEscaped(out, entry.getValue(), true);
            out.append('"');
        }
        if (Html.isVoid(name))
        {
            out.append('>');
            return;
        }
        out.append('>');
        for (Segment segment : segments)
        {
            if (segment.child != null)
            {
                segment.child.render(out);
            }
            else if (segment.kind == Segment.Kind.RAW)
            {
                out.append(segment.data);
            }
            else
            {
                appendEscaped(out, segment.data, false);
            }
        }
        out.append("</").append(name).append('>');
    }

    /**
     * Renders this element, equivalent to {@link #render()}.
     *
     * @return the HTML string
     */
    @Override
    public String toString()
    {
        return render();
    }

    private static void appendEscaped(Appendable out, String s, boolean attribute) throws IOException
    {
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            switch (c)
            {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append(attribute ? "&quot;" : "\""); break;
                case '\'': out.append(attribute ? "&#39;" : "'"); break;
                default: out.append(c);
            }
        }
    }
}