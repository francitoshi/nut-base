/*
 * BoxPrinter.java
 *
 * Copyright (c) 2026 francitoshi@gmail.com
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
package io.nut.base.ui.terminal;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * BoxPrinter - Draws text boxes with borders on the console or any PrintStream.
 *
 * Available border styles:
 *   SIMPLE → ┌─┐ │ └─┘
 *   DOUBLE → ╔═╗ ║ ╚═╝
 *   BLOCK  → █ █ █ █ █
 *
 * Features:
 *   - Configurable width in the constructor (inner text width).
 *   - Optional text centering.
 *   - Automatic line wrapping without breaking words.
 *   - Optional side margins (with/without side padding).
 *   - Output to System.out, any PrintStream, or a file.
*/
public class BoxPrinter
{

    // ─── Border styles ────────────────────────────────────────────────────────

    public enum Style
    {
        SIMPLE, DOBLE, BLOQUE
    }

    // ─── Internal structure of border characters ──────────────────────────────

    private static class BorderChars
    {
        final String topLeft, topRight, bottomLeft, bottomRight;
        final String horizontal, vertical;

        BorderChars(String tl, String tr, String bl, String br, String h, String v)
        {
            topLeft     = tl;
            topRight    = tr;
            bottomLeft  = bl;
            bottomRight = br;
            horizontal  = h;
            vertical    = v;
        }
    }

    private static final BorderChars CHARS_SIMPLE = new BorderChars("┌", "┐", "└", "┘", "─", "│");
    private static final BorderChars CHARS_DOBLE  = new BorderChars("╔", "╗", "╚", "╝", "═", "║");
    private static final BorderChars CHARS_BLOQUE = new BorderChars("█", "█", "█", "█", "█", "█");

    // ─── Configuration ────────────────────────────────────────────────────────
    
    /**
     * Width of the inner text area (excluding borders and padding).
     */
    private final int innerWidth;
    private final boolean centered;
    private final boolean sidePadding;   // true → adds 1-space padding on each side
    private final BorderChars chars;
    private PrintStream out;

    /**
     * Main constructor.
     *
     * @param style       Border style: SIMPLE, DOBLE, or BLOQUE.
     * @param innerWidth  Inner width (number of text characters per line).
     * @param centered    true to center text inside the box.
     * @param sidePadding true to add a one-space margin on each side (│ text │).
     *                    false for top/bottom border only (│text│).
     */
    public BoxPrinter(Style style, int innerWidth, boolean centered, boolean sidePadding)
    {
        this.innerWidth  = innerWidth;
        this.centered    = centered;
        this.sidePadding = sidePadding;
        this.out         = System.out;
        switch (style)
        {
            case DOBLE:
                chars = CHARS_DOBLE;
                break;
            case BLOQUE:
                chars = CHARS_BLOQUE;
                break;
            default:
                chars = CHARS_SIMPLE;
                break;
        }
    }

    // ─── Output configuration ─────────────────────────────────────────────────

    /**
     * Redirects output to any PrintStream (e.g. System.err or a file stream).
     */
    public BoxPrinter to(PrintStream ps)
    {
        this.out = ps;
        return this;
    }

    /**
     * Redirects output to a file. Creates or overwrites the file.
     */
    public BoxPrinter toFile(String path) throws FileNotFoundException
    {
        this.out = new PrintStream(path);
        return this;
    }

    /**
     * Restores output to System.out.
     */
    public BoxPrinter toStdOut()
    {
        this.out = System.out;
        return this;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Draws a complete box with the given text.
     * The text may span multiple lines; each '\n' forces a line break.
     * Lines exceeding innerWidth are wrapped automatically without breaking words.
     */
    public void print(String text)
    {
        List<String> lines = wrapText(text);
        printTopBorder();
        for (String line : lines)
        {
            printContentLine(line);
        }
        printBottomBorder();
    }

    /**
     * Draws the box and appends a blank line below it.
     */
    public void println(String text)
    {
        print(text);
        out.println();
    }

    /**
     * Draws only the top border (useful for manual composition).
     */
    public void printTopBorder()
    {
        out.println(buildHorizontalBorder(chars.topLeft, chars.topRight));
    }

    /**
     * Draws only the bottom border.
     */
    public void printBottomBorder()
    {
        out.println(buildHorizontalBorder(chars.bottomLeft, chars.bottomRight));
    }

    /**
     * Draws a formatted content line (with side borders).
     */
    public void printContentLine(String line)
    {
        out.println(buildContentLine(line));
    }

    /**
     * Prints an internal horizontal divider line.
     * SIMPLE: ├────┤   DOUBLE: ╠════╣   BLOCK: █████
     */
    public void printDivider()
    {
        String left  = (chars == CHARS_SIMPLE) ? "├"
                     : (chars == CHARS_DOBLE)  ? "╠" : "█";
        String right = (chars == CHARS_SIMPLE) ? "┤"
                     : (chars == CHARS_DOBLE)  ? "╣" : "█";
        out.println(buildHorizontalBorder(left, right));
    }

    // ─── Internal building methods ────────────────────────────────────────────

    /**
     * Total box width = borders (2) + padding (0 or 2) + inner text width.
     */
    private int totalWidth()
    {
        return 2 + (sidePadding ? 2 : 0) + innerWidth;
    }

    private String buildHorizontalBorder(String left, String right)
    {
        int fillWidth = totalWidth() - 2; // excluding corner characters
        StringBuilder sb = new StringBuilder(left);
        for (int i = 0; i < fillWidth; i++)
        {
            sb.append(chars.horizontal);
        }
        sb.append(right);
        return sb.toString();
    }

    private String buildContentLine(String text)
    {
        // text must already be ≤ innerWidth characters
        String padded = sidePadding
                ? " " + formatText(text) + " "
                : formatText(text);
        return chars.vertical + padded + chars.vertical;
    }

    /**
     * Formats text to fit innerWidth: left-aligned or centered.
     * The input text must not exceed innerWidth.
     */
    private String formatText(String text)
    {
        int len     = text.length();
        int padding = innerWidth - len;
        if (padding < 0)
        {
            padding = 0;
        }

        if (centered)
        {
            int left  = padding / 2;
            int right = padding - left;
            return repeat(" ", left) + text + repeat(" ", right);
        }
        else
        {
            return text + repeat(" ", padding);
        }
    }

    /**
     * Splits text into lines respecting innerWidth without breaking words.
     * Explicit '\n' characters force a new line.
     */
    private List<String> wrapText(String text)
    {
        List<String> result = new ArrayList<>();

        // First split on explicit line breaks
        String[] paragraphs = text.split("\n", -1);
        for (String para : paragraphs)
        {
            if (para.isEmpty())
            {
                result.add("");
                continue;
            }
            String[] words = para.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words)
            {
                // If a single word exceeds innerWidth, split it by characters
                if (word.length() > innerWidth)
                {
                    if (current.length() > 0)
                    {
                        result.add(current.toString());
                        current = new StringBuilder();
                    }
                    while (word.length() > innerWidth)
                    {
                        result.add(word.substring(0, innerWidth));
                        word = word.substring(innerWidth);
                    }
                    if (!word.isEmpty())
                    {
                        current.append(word);
                    }
                    continue;
                }

                // Does it fit on the current line?
                int needed = current.length() == 0
                        ? word.length()
                        : current.length() + 1 + word.length();
                if (needed <= innerWidth)
                {
                    if (current.length() > 0)
                    {
                        current.append(" ");
                    }
                    current.append(word);
                }
                else
                {
                    result.add(current.toString());
                    current = new StringBuilder(word);
                }
            }
            if (current.length() > 0 || para.isEmpty())
            {
                result.add(current.toString());
            }
        }
        return result;
    }

    private static String repeat(String s, int n)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++)
        {
            sb.append(s);
        }
        return sb.toString();
    }
}
