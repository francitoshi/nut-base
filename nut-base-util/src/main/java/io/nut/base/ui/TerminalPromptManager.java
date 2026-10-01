/*
 * TerminalPromptManager.java
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
package io.nut.base.ui;

import io.nut.base.ui.terminal.BoxPrinter;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * TerminalPromptManager – Implementation of {@link PromptManager} for the
 * terminal.
 *
 * <h2>Questions</h2>
 * Prints the question, hint, and error message (if any) on the same line, in
 * the style of {@code apt upgrade}:
 * <pre>
 *   Do you want to continue? [Y/n]
 *   Unrecognized response. Please answer "y", "yes", "n" or "no".
 *   Do you want to continue? [Y/n]
 * </pre>
 *
 * <h2>Menus</h2>
 * Each attempt repaints the entire box from scratch using {@link BoxPrinter}.
 * If it follows a failed attempt, the error message is printed right before
 * repainting the menu:
 * <pre>
 *   ┌─────────────────────────────────────────────────┐
 *   │ MAIN MENU                                       │
 *   ├─────────────────────────────────────────────────┤
 *   │   1) new      New game                          │
 *   │   2) load     Load game                         │
 *   └─────────────────────────────────────────────────┘
 *   Option [1-2]:
 * </pre>
 *
 * <h2>BoxPrinter Configuration</h2>
 * The {@link BoxPrinter} used for menus can be configured via
 * {@link #withMenuBox(BoxPrinter)}.
 */
public class TerminalPromptManager extends PromptManager
{

    // ─── State ───────────────────────────────────────────────────────────────
    private final PrintStream out;
    private final Scanner scanner;
    private BoxPrinter menuBox;

    // ─── Constructors ────────────────────────────────────────────────────────
    /**
     * Full constructor.
     *
     * @param maxRetries Maximum number of retries. -1 = infinite.
     * @param out Output stream.
     * @param in Input stream.
     */
    public TerminalPromptManager(int maxRetries, PrintStream out, InputStream in)
    {
        super(maxRetries);
        this.out = out;
        this.scanner = new Scanner(in, "UTF-8");
        this.menuBox = new BoxPrinter(BoxPrinter.Style.SIMPLE, 48, false, true);
        this.menuBox.to(out);
    }

    /**
     * Convenience constructor: uses {@code System.out} and {@code System.in}.
     *
     * @param maxRetries Maximum number of retries. -1 = infinite.
     */
    public TerminalPromptManager(int maxRetries)
    {
        this(maxRetries, System.out, System.in);
    }

    // ─── Configuration ────────────────────────────────────────────────────────
    /**
     * Replaces the {@link BoxPrinter} used to render menus.
     *
     * @param box Pre-configured BoxPrinter (its output is redirected to
     * this.out).
     * @return this, for fluent chaining.
     */
    public TerminalPromptManager withMenuBox(BoxPrinter box)
    {
        this.menuBox = box;
        this.menuBox.to(out);
        return this;
    }

    // ─── Contract Implementation ─────────────────────────────────────────────
    /**
     * Prints the question to the terminal and reads a line of input. If there
     * is a previous error message, it displays it before the question.
     */
    @Override
    protected String doAsk(String question, String hint, String errorMessage)
    {
        if (errorMessage != null)
        {
            out.println("  " + errorMessage);
        }
        out.print(question + " " + hint + " ");
        return scanner.nextLine();
    }

    /**
     * Repaints the entire menu with {@link BoxPrinter} and reads a line of
     * input. If there is a previous error message, it displays it before
     * repainting the menu.
     */
    @Override
    protected String doMenu(String title, List<MenuItem> items, String errorMessage)
    {
        if (errorMessage != null)
        {
            out.println();
            out.println("  ✗ " + errorMessage);
        }
        renderMenuBox(title, items);
        out.print("Option [1-" + items.size() + "]: ");
        return scanner.nextLine();
    }

    // ─── Menu Box Rendering ──────────────────────────────────────────────────
    /**
     * Draws the menu using {@link BoxPrinter}: header, divider, and option
     * lines. The number/key/description columns are automatically aligned.
     */
    private void renderMenuBox(String title, List<MenuItem> items)
    {
        // Width of the longest key to align columns
        int maxKeyLen = 0;
        for (MenuItem item : items)
        {
            if (item.getKey().length() > maxKeyLen)
            {
                maxKeyLen = item.getKey().length();
            }
        }

        int digits = String.valueOf(items.size()).length();
        String numFmt = "  %" + digits + "d) ";
        String keyFmt = "%-" + maxKeyLen + "s  ";

        menuBox.printTopBorder();
        menuBox.printContentLine(title);
        menuBox.printDivider();
        for (int i = 0; i < items.size(); i++)
        {
            MenuItem item = items.get(i);
            String line = String.format(numFmt, i + 1)
                    + String.format(keyFmt, item.getKey())
                    + item.getDescription();
            menuBox.printContentLine(line);
        }
        menuBox.printBottomBorder();
    }
}
