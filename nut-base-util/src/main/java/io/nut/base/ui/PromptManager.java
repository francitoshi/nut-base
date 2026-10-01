/*
 * PromptManager.java
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PromptManager – Abstract base class for user interaction.
 *
 * <p>
 * Centralizes all retry logic, response parsing, and menu management, 
 * regardless of the presentation medium (terminal, GUI, etc.).
 *
 * <h2>Contract for subclasses</h2>
 * Subclasses must only implement two low-level methods:
 * <ul>
 * <li>{@link #doAsk(String, String, String)} – presents the question and returns 
 * the raw user response, or {@code null} if cancelled.</li>
 * <li>{@link #doMenu(String, List, String)} – presents the menu and returns 
 * the chosen string, or {@code null} if cancelled.</li>
 * </ul>
 * Both methods are invoked on every attempt, including retries.
 *
 * <h2>Response Modes</h2>
 * <ul>
 * <li>{@link AnswerMode#YN}      → {@code [Y/n]} empty = yes</li>
 * <li>{@link AnswerMode#Yn}      → {@code [y/N]} empty = no</li>
 * <li>{@link AnswerMode#YES_NO}  → {@code [yes/no]} no default value</li>
 * </ul>
 *
 * <h2>Retries</h2> 
 * {@code maxRetries = 0} → a single attempt; throws {@link MaxRetriesException} if it fails.<br> 
 * {@code maxRetries = N} → up to N additional retries (N+1 total attempts).<br> 
 * {@code maxRetries = -1} → infinite.
 */
public abstract class PromptManager
{

    // ═════════════════════════════════════════════════════════════════════════
    // Public Types
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Thrown when the configured maximum number of retries is exceeded.
     */
    public static class MaxRetriesException extends RuntimeException
    {

        private final String prompt;

        public MaxRetriesException(String prompt)
        {
            super("Maximum number of retries exceeded for: \"" + prompt + "\"");
            this.prompt = prompt;
        }

        public String getPrompt()
        {
            return prompt;
        }
    }

    /**
     * Defines the expected response format in {@link #ask}.
     */
    public enum AnswerMode
    {
        /**
         * {@code [Y/n]} – empty or "y"/"yes" = {@code true}.
         */
        YN,
        /**
         * {@code [y/N]} – empty or "n"/"no" = {@code false}.
         */
        Yn,
        /**
         * {@code [yes/no]} – only "yes" or "no" are valid; no default value.
         */
        YES_NO
    }

    /**
     * Represents an option in an interactive menu.
     */
    public static class MenuItem
    {

        private final String key;
        private final String description;

        /**
         * @param key Unique identifier that the user can type.
         * @param description Descriptive text displayed in the menu.
         */
        public MenuItem(String key, String description)
        {
            if (key == null || key.trim().isEmpty())
            {
                throw new IllegalArgumentException("MenuItem key cannot be empty.");
            }
            this.key = key.trim();
            this.description = description != null ? description : "";
        }

        public String getKey()
        {
            return key;
        }

        public String getDescription()
        {
            return description;
        }

        @Override
        public String toString()
        {
            return key + "  " + description;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // State
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Maximum number of retries. -1 = infinite.
     */
    protected final int maxRetries;

    protected PromptManager(int maxRetries)
    {
        this.maxRetries = maxRetries;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Abstract Contract
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Presents the question to the user and captures their response. 
     * It is invoked in <em>every attempt</em> of the retry loop.
     *
     * @param question Question text.
     * @param hint Format hint, e.g., {@code "[Y/n]"}.
     * @param errorMessage Error message from the previous attempt, or {@code null} 
     *                     if it is the first attempt.
     * @return The entered string (may be empty), or {@code null} if cancelled.
     */
    protected abstract String doAsk(String question, String hint, String errorMessage);

    /**
     * Presents the menu to the user and captures their choice. 
     * It is invoked in every attempt, including retries (full repaint).
     *
     * @param title Menu title.
     * @param items List of options in order.
     * @param errorMessage Error message from the previous attempt, or {@code null} 
     *                     if it is the first attempt.
     * @return The entered string (number or key), or {@code null} if cancelled.
     */
    protected abstract String doMenu(String title, List<MenuItem> items, String errorMessage);

    // ═════════════════════════════════════════════════════════════════════════
    // Public API – retry logic (final)
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Asks a yes/no question to the user.
     *
     * @return {@code true} = affirmative, {@code false} = negative.
     * @throws MaxRetriesException If retries are exhausted or the user cancels.
     */
    public final boolean ask(String question, AnswerMode mode)
    {
        String error = null;
        int attempts = 0;

        while (true)
        {
            String raw = doAsk(question, hintFor(mode), error);
            if (raw == null)
            {
                throw new MaxRetriesException(question);
            }

            Optional<Boolean> result = parse(raw.trim(), mode);
            if (result.isPresent())
            {
                return result.get();
            }

            attempts++;
            if (maxRetries >= 0 && attempts > maxRetries)
            {
                throw new MaxRetriesException(question);
            }

            error = "Unrecognized response. Please answer " + validAnswersFor(mode) + ".";
        }
    }

    /**
     * Safe version of {@link #ask}: returns {@link Optional#empty()} instead of 
     * throwing an exception when retries are exhausted or the user cancels.
     */
    public final Optional<Boolean> askSafe(String question, AnswerMode mode)
    {
        try
        {
            return Optional.of(ask(question, mode));
        }
        catch (MaxRetriesException e)
        {
            return Optional.empty();
        }
    }

    /**
     * Displays a menu and waits for the user's choice. The user can respond 
     * by number (1, 2…) or by exact key.
     *
     * @throws MaxRetriesException If retries are exhausted or the user cancels.
     * @throws IllegalArgumentException If items is empty.
     */
    public final MenuItem menu(String title, List<MenuItem> items)
    {
        if (items == null || items.isEmpty())
        {
            throw new IllegalArgumentException("The menu must have at least one item.");
        }

        Map<String, MenuItem> byNum = new LinkedHashMap<>();
        Map<String, MenuItem> byKey = new LinkedHashMap<>();
        for (int i = 0; i < items.size(); i++)
        {
            MenuItem item = items.get(i);
            byNum.put(String.valueOf(i + 1), item);
            byKey.put(item.getKey().toLowerCase(), item);
        }

        String error = null;
        int attempts = 0;

        while (true)
        {
            String raw = doMenu(title, items, error);
            if (raw == null)
            {
                throw new MaxRetriesException(title);
            }

            String trimmed = raw.trim();
            MenuItem chosen = byNum.get(trimmed);
            if (chosen == null)
            {
                chosen = byKey.get(trimmed.toLowerCase());
            }
            if (chosen != null)
            {
                return chosen;
            }

            attempts++;
            if (maxRetries >= 0 && attempts > maxRetries)
            {
                throw new MaxRetriesException(title);
            }

            error = "Invalid option. Please enter a number between 1 and "
                    + items.size() + " or the exact key.";
        }
    }

    /**
     * Varargs variant of {@link #menu(String, List)}.
     */
    public final MenuItem menu(String title, MenuItem... items)
    {
        return menu(title, Arrays.asList(items));
    }

    /**
     * Safe version of {@link #menu}: returns {@link Optional#empty()} when 
     * retries are exhausted or the user cancels.
     */
    public final Optional<MenuItem> menuSafe(String title, List<MenuItem> items)
    {
        try
        {
            return Optional.of(menu(title, items));
        }
        catch (MaxRetriesException e)
        {
            return Optional.empty();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Protected Static Utilities (available to subclasses)
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Hint text for the given mode, e.g., {@code "[Y/n]"}.
     */
    protected static String hintFor(AnswerMode mode)
    {
        switch (mode)
        {
            case YN:
                return "[Y/n]";
            case Yn:
                return "[y/N]";
            case YES_NO:
                return "[yes/no]";
            default:
                return "[Y/n]";
        }
    }

    /**
     * Textual description of valid answers for error messages.
     */
    protected static String validAnswersFor(AnswerMode mode)
    {
        switch (mode)
        {
            case YN:
                return "\"y\", \"yes\", \"n\" or \"no\" (empty = yes)";
            case Yn:
                return "\"y\", \"yes\", \"n\" or \"no\" (empty = no)";
            case YES_NO:
                return "\"yes\" or \"no\"";
            default:
                return "\"yes\" or \"no\"";
        }
    }

    /**
     * Parses the raw input. Returns {@link Optional#empty()} if invalid.
     */
    protected static Optional<Boolean> parse(String raw, AnswerMode mode)
    {
        String lower = raw.toLowerCase();
        switch (mode)
        {
            case YN:
                if (raw.isEmpty() || lower.equals("y") || lower.equals("yes"))
                {
                    return Optional.of(true);
                }
                if (lower.equals("n") || lower.equals("no"))
                {
                    return Optional.of(false);
                }
                return Optional.empty();
            case Yn:
                if (raw.isEmpty() || lower.equals("n") || lower.equals("no"))
                {
                    return Optional.of(false);
                }
                if (lower.equals("y") || lower.equals("yes"))
                {
                    return Optional.of(true);
                }
                return Optional.empty();
            case YES_NO:
                if (lower.equals("yes"))
                {
                    return Optional.of(true);
                }
                if (lower.equals("no"))
                {
                    return Optional.of(false);
                }
                return Optional.empty();
            default:
                return Optional.empty();
        }
    }
}
