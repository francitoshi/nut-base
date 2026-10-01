/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.gauge;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MultiProgressBar
 * -----------------
 * A terminal widget that shows several independent progress bars at once,
 * stacked at the bottom of the screen, using the same apt/dpkg-style
 * technique as a single-bar version of this class:
 *
 *  - The scroll region (DECSTBM, "ESC[<top>;<bottom>r") is narrowed so that
 *    normal text (log()) scrolls only above the bars area; the bars area
 *    itself lives outside that region and is never touched by scrolling.
 *  - Each bar is drawn with absolute cursor addressing (save/restore cursor:
 *    ESC7 / ESC8), never by clearing-then-writing (to avoid flicker), and
 *    always with the real terminal cursor hidden during the jump (to avoid
 *    the cursor visibly "flying" between rows).
 *  - Requesting a new bar grows the bars area by one line, reclaiming it
 *    from the bottom of the log area. Closing a bar shrinks the area by one
 *    line and gives it back to the log area.
 *
 * New bars are inserted at the TOP of the bars area (closest to the log
 * text); the bar that has been open the longest ends up at the very bottom
 * of the terminal.
 *
 * Thread-safe: addBar(), log(), and every ProgressBar returned by addBar()
 * can be used freely from different threads (e.g. one thread per parallel
 * download); all terminal writes are serialized internally.
 *
 * Compatible with Java 8. Requires an ANSI/VT100-capable terminal
 * (xterm, VTE-based terminals, Konsole, most Linux/macOS terminals, and
 * Windows Terminal with ANSI mode enabled).
 */
public class MultiProgressBar implements AutoCloseable 
{

    private static final String ESC = "\u001B";
    private static final String RESET = ESC + "[0m";
    private static final String BOLD = ESC + "[1m";
    private static final String HIDE_CURSOR = ESC + "[?25l";
    private static final String SHOW_CURSOR = ESC + "[?25h";

    /** Minimum width (in characters) ever given to the bar itself, even with a very long label. */
    private static final int MIN_BAR_WIDTH = 10;

    /** A handful of common ANSI foreground colors, ready to use as Config.fillColor(). */
    public static final class Colors
    {
        public static final String DEFAULT = "";
        public static final String RED = ESC + "[31m";
        public static final String GREEN = ESC + "[32m";
        public static final String YELLOW = ESC + "[33m";
        public static final String BLUE = ESC + "[34m";
        public static final String MAGENTA = ESC + "[35m";
        public static final String CYAN = ESC + "[36m";
        public static final String WHITE = ESC + "[37m";

        private Colors()
        {
        }
    }

    /**
     * Global rendering configuration, shared by every bar created by this
     * MultiProgressBar. Configure it and pass it to the constructor; it is
     * not meant to be changed afterwards.
     */
    public static final class Config
    {
        private char fillChar = '#';
        private char emptyChar = '.';
        private String fillColor = Colors.GREEN;
        private String labelColor = Colors.DEFAULT;
        private boolean boldPercentage = true;
        private boolean hideCursor = true;
        private boolean detectCursorPosition = true;

        /** Character used for the completed portion of every bar. Default: '#'. */
        public Config fillChar(char c)
        {
            this.fillChar = c;
            return this;
        }

        /** Character used for the pending portion of every bar. Default: '.'. */
        public Config emptyChar(char c)
        {
            this.emptyChar = c;
            return this;
        }

        /** ANSI color escape applied to the filled portion, e.g. {@link Colors#GREEN}. */
        public Config fillColor(String ansiColor)
        {
            this.fillColor = ansiColor == null ? "" : ansiColor;
            return this;
        }

        /**
         * ANSI color escape applied to the label text at the start of each bar,
         * e.g. {@link Colors#YELLOW}. Optional: the default is
         * {@link Colors#DEFAULT} (empty), which means the label is printed
         * with the terminal's normal colors, exactly like before this option
         * existed.
         */
        public Config labelColor(String ansiColor)
        {
            this.labelColor = ansiColor == null ? "" : ansiColor;
            return this;
        }

        /** Whether the percentage text is rendered in bold. Default: true. */
        public Config boldPercentage(boolean boldPercentage)
        {
            this.boldPercentage = boldPercentage;
            return this;
        }

        /**
         * Whether the terminal's real cursor is kept hidden while the bars are
         * active. Recommended (default: true) -- it avoids any visible flicker
         * from the cursor jumping between rows on every redraw.
         */
        public Config hideCursor(boolean hideCursor)
        {
            this.hideCursor = hideCursor;
            return this;
        }

        /**
         * Whether to detect and preserve the caller's actual cursor position
         * (e.g. a shell prompt sitting mid-screen) whenever a bar is added or
         * closed, instead of moving the cursor to just above the bars area.
         * Default: true.
         * <p>
         * This works by querying the terminal for its real cursor position
         * (a Device Status Report, "ESC[6n") right before touching anything,
         * which briefly puts the terminal into raw/no-echo mode and reads the
         * response from {@code System.in}. If your application also reads
         * {@code System.in} concurrently (e.g. a REPL loop), there is a small
         * risk of a race for those few bytes; if that is a concern, or if
         * stdin/stdout are not an interactive terminal, disable this and the
         * cursor will simply rest just above the bars area, as before.
         */
        public Config detectCursorPosition(boolean detectCursorPosition)
        {
            this.detectCursorPosition = detectCursorPosition;
            return this;
        }
    }

    /** A handle to a single row in the bars area. Obtained via {@link #addBar}. */
    public final class ProgressBar extends AbstractGauge implements AutoCloseable
    {
        private final AtomicBoolean barClosed = new AtomicBoolean(false);
        private String label;
        private int percent;

        private ProgressBar(String label)
        {
            super();
            this.label = label == null ? "" : label;
            this.percent = 0;
            setPrefix(this.label);
        }

        /** Updates the percentage (0-100), keeping the current label. */
        public void update(int percent)
        {
            update(percent, null);
        }

        /** Updates the percentage (0-100) and replaces the label. Pass {@code null} to keep it. */
        public void update(int percent, String label)
        {
            if (barClosed.get())
            {
                throw new IllegalStateException("This ProgressBar has already been closed");
            }
            if (label != null)
            {
                setPrefix(label);
            }
            setMax(100);
            setVal(percent);
        }

        /** Removes this bar's row; the bars area shrinks by one line. Safe to call more than once. */
        @Override
        public void close() 
        {
            if (barClosed.compareAndSet(false, true)) 
            {
                super.close();
                removeBar(this);
            }
        }

        @Override
        public void paint(boolean started, int max, int val, double done, String prefix, String prev, String next, String full)
        {
            if (barClosed.get())
            {
                return;
            }
            synchronized (MultiProgressBar.this.lock)
            {
                if (closed.get())
                {
                    return;
                }
                int idx = bars.indexOf(this);
                if (idx < 0)
                {
                    return;
                }
                this.percent = (max > 0) ? (int) Math.round(done * 100.0) : 0;
                this.percent = Math.max(0, Math.min(100, this.percent));
                this.label = prefix == null ? "" : prefix;
                redrawRow(idx, this);
                System.out.flush();
            }
        }
    }

    private final Object lock = new Object();
    private final int rows;
    private int cols; // not final anymore: refreshed periodically, see maybeRefreshWidth()
    private final Config config;
    private final List<ProgressBar> bars = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /** How often (in milliseconds) the terminal width is allowed to be re-checked. */
    private static final long WIDTH_REFRESH_INTERVAL_MS = 1000;
    private long lastWidthCheckMillis = 0;

    /** True once the scroll region has been narrowed at least once (i.e. after the first addBar()). */
    private boolean scrollRegionActive = false;

    public MultiProgressBar()
    {
        this(new Config());
    }

    public MultiProgressBar(Config config)
    {
        this.config = config;
        int[] size = detectTerminalSize();
        this.rows = size[0];
        this.cols = size[1];
        this.lastWidthCheckMillis = System.currentTimeMillis();

        if (config.hideCursor)
        {
            out(HIDE_CURSOR);
            System.out.flush();
        }

        // Safety net: if the process dies abruptly (uncaught exception, Ctrl+C,
        // kill) without going through close(), restore the terminal to a sane
        // state (visible cursor, unrestricted scroll region) regardless.
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                close();
            }
        }));
    }

    /**
     * Requests a new progress bar. It is inserted at the top of the bars
     * area (right above the previous topmost bar) and starts at 0%.
     */
    public ProgressBar addBar(String label)
    {
        synchronized (lock)
        {
            checkOpen();

            int maxBars = rows - 1; // always leave at least one line for normal log output
            if (bars.size() >= maxBars)
            {
                throw new IllegalStateException("No room for another progress bar: the terminal only has " + rows + " rows");
            }

            ProgressBar bar = new ProgressBar(label == null ? "" : label);

            // Remember exactly where the caller's cursor is (e.g. a shell
            // prompt mid-screen) BEFORE we touch anything, so we can put it
            // back there once the new bar is drawn -- instead of leaving it
            // parked just above the bars area.
            int[] restPos = queryCursorPositionOrFallback();

            growAreaByOne(restPos != null ? restPos[0] : -1);
            bars.add(0, bar); // new bars go to the top of the bars zone
            repaintAll();
            restoreCursor(restPos);
            System.out.flush();

            return bar;
        }
    }

    /** Writes a normal line of text that scrolls above the bars area, exactly like log output in apt. */
    public void log(String line)
    {
        synchronized (lock)
        {
            checkOpen();
            out(line + "\n");
            System.out.flush();
        }
    }

    /** Closes every remaining bar and restores the terminal to its normal state. Safe to call more than once. */
    @Override
    public void close()
    {
        synchronized (lock)
        {
            if (closed.compareAndSet(false, true))
            {
                out(RESET);
                out(ESC + "[r"); // remove scroll margins entirely
                out(ESC + "[" + rows + ";1H");
                if (config.hideCursor)
                {
                    out(SHOW_CURSOR);
                }
                out("\n");
                System.out.flush();
            }
        }
    }

    // ------------------------------------------------------------------
    // Internal operations invoked by ProgressBar handles
    // ------------------------------------------------------------------

    private void removeBar(ProgressBar bar)
    {
        synchronized (lock)
        {
            if (closed.get())
            {
                return;
            }
            int idx = bars.indexOf(bar);
            if (idx < 0)
            {
                return; // already removed
            }

            int[] restPos = queryCursorPositionOrFallback();

            bars.remove(idx);
            shrinkAreaByOne();
            repaintAll();
            restoreCursor(restPos);
            System.out.flush();
        }
    }

    // ------------------------------------------------------------------
    // Layout: mapping bars to absolute terminal rows
    // ------------------------------------------------------------------

    /** Absolute row (1-indexed) of the log area's last line, given the current number of bars. */
    private int currentLogBottom()
    {
        return rows - bars.size();
    }

    /** Absolute row for the bar currently at list index i (0 = top of the bars zone). */
    private int rowFor(int index)
    {
        return rows - bars.size() + 1 + index;
    }

    /**
     * Reserves one more line for the bars area by narrowing the scroll region
     * by one row.
     * <p>
     * If we know the caller's real cursor is still strictly above the bottom
     * of the current log area (there is already unused blank space between
     * it and that bottom line, e.g. a shell prompt sitting mid-screen), the
     * new line is simply claimed as-is -- nothing is scrolled, so whatever
     * is already on screen above the cursor stays exactly where it is. Only
     * when the cursor has actually reached that bottom line (or we could not
     * determine its position) do we fall back to the scroll trick: place the
     * cursor on the region's last line and print a newline, which (per
     * DECSTBM semantics) scrolls the region's content up by one, leaving a
     * blank line behind that becomes the newest bar's row.
     *
     * @param knownCursorRow the caller's real cursor row, or -1 if unknown
     *                       (in which case we always play it safe and scroll)
     *
     * Note: called with {@code bars.size()} still reflecting the state
     * BEFORE the new bar is added.
     */
    private void growAreaByOne(int knownCursorRow)
    {
        int oldLogBottom;
        if (!scrollRegionActive)
        {
            oldLogBottom = rows;
            out(ESC + "[1;" + rows + "r"); // establish a known, full-height scroll region first
            scrollRegionActive = true;
        } 
        else 
        {
            oldLogBottom = rows - bars.size();
        }

        boolean needsScroll = knownCursorRow < 0 || knownCursorRow >= oldLogBottom;
        if (needsScroll)
        {
            out(HIDE_CURSOR);
            out(RESET);
            out(ESC + "[" + oldLogBottom + ";1H");
            out("\n"); // scroll the still-active old region, freeing its last line
        }

        int newBarsCount = bars.size() + 1;
        int newLogBottom = rows - newBarsCount;
        out(ESC + "[1;" + newLogBottom + "r");
    }

    /**
     * Symmetric to {@link #growAreaByOne(int)}: gives one line back to the log
     * area. Called with {@code bars.size()} already reflecting the bar that
     * was just removed.
     */
    private void shrinkAreaByOne()
    {
        int newBarsCount = bars.size();
        int newLogBottom = rows - newBarsCount;
        out(ESC + "[1;" + newLogBottom + "r");

        // Blank the reclaimed line so it doesn't show leftover bar content
        // before the log area writes something new there.
        out(HIDE_CURSOR);
        out(RESET);
        out(ESC + "[" + newLogBottom + ";1H");
        out(blankLine());
    }

    /**
     * Redraws every bar at its current row. Used after any structural change
     * (addBar/close), since removing or inserting a bar can shift the
     * absolute row of the bars above it. Individual progress updates
     * ({@link ProgressBar#update}) never call this -- they only touch their
     * own row, via {@link #redrawRow}.
     */
    private void repaintAll()
    {
        for (int i = 0; i < bars.size(); i++)
        {
            redrawRow(i, bars.get(i));
        }
    }

    /**
     * Draws a single bar's row and always leaves the terminal cursor back
     * where it was before the call (or, if the config asks for a visible
     * cursor, at rest in that same spot) -- never mid-jump, to avoid flicker.
     */
    private void redrawRow(int index, ProgressBar bar)
    {
        maybeRefreshWidth();
        out(RESET);
        out(HIDE_CURSOR); // always hidden during the jump itself, regardless of config.hideCursor
        out(ESC + "7"); // save current cursor position
        out(ESC + "[" + rowFor(index) + ";1H");
        out(renderBar(bar.percent, bar.label));
        out(RESET);
        out(ESC + "8"); // restore cursor position
        if (!config.hideCursor)
        {
            out(SHOW_CURSOR);
        }
    }

    /**
     * Re-checks the terminal's width at most once every
     * {@link #WIDTH_REFRESH_INTERVAL_MS}, so that bars adapt if the user
     * resizes the terminal window while they are running. Only the width is
     * refreshed: the number of rows is left untouched, since changing it
     * mid-flight would require re-deriving the whole scroll-region layout
     * (how many lines are reserved for bars, where the log area ends, etc.),
     * which is out of scope here.
     */
    private void maybeRefreshWidth()
    {
        long now = System.currentTimeMillis();
        if (now - lastWidthCheckMillis < WIDTH_REFRESH_INTERVAL_MS)
        {
            return;
        }
        lastWidthCheckMillis = now;
        int[] size = detectTerminalSize();
        this.cols = size[1];
    }

    /**
     * Returns the caller's real cursor position via {@link #queryCursorPosition()}
     * if detection is enabled and it succeeds, or {@code null} otherwise --
     * in which case {@link #restoreCursor} falls back to resting just above
     * the bars area (the previous, simpler default behavior).
     */
    private int[] queryCursorPositionOrFallback()
    {
        if (config.detectCursorPosition)
        {
            return queryCursorPosition();
        }
        return null;
    }

    /**
     * Moves the cursor back to a previously captured position (or, if none
     * was captured, to the current bottom of the log area), clamping the row
     * so it never lands inside the bars area -- if the log area has shrunk
     * past where the cursor used to be, it rests at the new bottom instead.
     */
    private void restoreCursor(int[] pos)
    {
        int row;
        int col;
        if (pos != null) 
        {
            row = pos[0];
            col = pos[1];
        } 
        else 
        {
            row = currentLogBottom(); // freshly computed, reflects the state AFTER the change
            col = 1;
        }
        int logBottom = currentLogBottom();
        if (row > logBottom)
        {
            row = logBottom;
            col = 1;
        }
        out(ESC + "[" + row + ";" + col + "H");
        if (!config.hideCursor)
        {
            out(SHOW_CURSOR);
        }
    }

    /**
     * Queries the terminal for its actual current cursor position using a
     * Device Status Report ("ESC[6n"): the terminal replies on stdin with
     * "ESC[<row>;<col>R". Reading that reply requires briefly switching the
     * terminal to raw, no-echo mode (via "stty"), so this only runs when
     * stdout/stdin look like a real interactive terminal
     * ({@link System#console()} is non-null); it silently gives up (returns
     * {@code null}) if anything about it fails or takes too long, so callers
     * must always have a fallback ready.
     */
    private int[] queryCursorPosition() 
    {
        if (System.console() == null)
        {
            return null; // not an interactive terminal; nothing sensible to query
        }
        String savedState = null;
        try
        {
            savedState = runSttyCapture("-g");
            runStty("raw", "-echo");

            out(ESC + "[6n");
            System.out.flush();

            StringBuilder response = new StringBuilder();
            boolean started = false;
            long deadline = System.currentTimeMillis() + 300;
            while (System.currentTimeMillis() < deadline) 
            {
                if (System.in.available() > 0)
                {
                    int ch = System.in.read();
                    if (ch < 0) 
                    {
                        break;
                    }
                    if (ch == 0x1B) 
                    {
                        started = true;
                        response.setLength(0);
                        response.append((char) ch);
                        continue;
                    }
                    if (started) 
                    {
                        response.append((char) ch);
                        if (ch == 'R') 
                        {
                            break;
                        }
                    }
                } 
                else 
                {
                    Thread.sleep(5);
                }
            }

            String s = response.toString();
            int bracket = s.indexOf('[');
            int semi = s.indexOf(';');
            int rIdx = s.indexOf('R');
            if (bracket >= 0 && semi > bracket && rIdx > semi)
            {
                int row = Integer.parseInt(s.substring(bracket + 1, semi));
                int col = Integer.parseInt(s.substring(semi + 1, rIdx));
                return new int[] { row, col };
            }
        } 
        catch (Exception ignored) 
        {
            // fall through to null below
        } 
        finally 
        {
            if (savedState != null) 
            {
                runStty(savedState);
            } 
            else 
            {
                runStty("sane");
            }
        }
        return null;
    }

    private String runSttyCapture(String... args) throws Exception
    {
        List<String> cmd = new ArrayList<>();
        cmd.add("stty");
        cmd.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        Process p = pb.start();
        String line;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) 
        {
            line = r.readLine();
        }
        p.waitFor();
        return line;
    }

    private void runStty(String... args) 
    {
        try 
        {
            List<String> cmd = new ArrayList<>();
            cmd.add("stty");
            cmd.addAll(Arrays.asList(args));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.start().waitFor();
        } 
        catch (Exception ignored) 
        {
            // best-effort restoration; nothing more we can do here
        }
    }

    private String blankLine() 
    {
        StringBuilder sb = new StringBuilder(cols);
        for (int i = 0; i < cols - 1; i++)
        {
            sb.append(' ');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /**
     * Renders one bar's full line, padded with spaces up to the terminal
     * width so it always fully overwrites whatever was there before (no
     * ESC[K clear-then-write, which is what causes visible flicker on some
     * GPU-accelerated terminals).
     */
    private String renderBar(int percent, String rawLabel)
    {
        String suffix = String.format(" %3d%%", percent); // always 5 characters wide

        int reserved = 1 /* space after label */ + 2 /* brackets */ + suffix.length() + MIN_BAR_WIDTH;
        int maxLabelLen = Math.max(0, (cols - 1) - reserved);
        String label = truncateLabel(rawLabel, maxLabelLen);

        // Visible length of the label portion, ignoring any color codes we
        // might wrap around it below -- those codes take up bytes but no
        // screen columns, so they must never be counted towards the width.
        int prefixVisibleLen = label.isEmpty() ? 0 : label.length() + 1;

        int barWidth = Math.max(MIN_BAR_WIDTH, (cols - 1) - prefixVisibleLen - 2 - suffix.length());
        int filled = (int) Math.round(barWidth * (percent / 100.0));

        StringBuilder sb = new StringBuilder();
        if (!label.isEmpty())
        {
            if (!config.labelColor.isEmpty())
            {
                sb.append(config.labelColor).append(label).append(RESET);
            }
            else 
            {
                sb.append(label);
            }
            sb.append(' ');
        }
        sb.append('[');

        if (filled > 0)
        {
            sb.append(config.fillColor);
            for (int i = 0; i < filled; i++)
            {
                sb.append(config.fillChar);
            }
            sb.append(RESET);
        }
        for (int i = filled; i < barWidth; i++)
        {
            sb.append(config.emptyChar);
        }
        sb.append(']');

        if (config.boldPercentage)
        {
            sb.append(BOLD).append(suffix).append(RESET);
        } 
        else 
        {
            sb.append(suffix);
        }

        int visibleLen = prefixVisibleLen + 1 + barWidth + 1 + suffix.length();
        int pad = (cols - 1) - visibleLen;
        for (int i = 0; i < pad; i++)
        {
            sb.append(' ');
        }

        return sb.toString();
    }

    private String truncateLabel(String label, int maxLen)
    {
        if (label == null || maxLen <= 0)
        {
            return "";
        }
        if (label.length() <= maxLen)
        {
            return label;
        }
        if (maxLen <= 3) {
            return label.substring(0, maxLen);
        }
        return label.substring(0, maxLen - 3) + "...";
    }

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------

    private void checkOpen()
    {
        if (closed.get())
        {
            throw new IllegalStateException("This MultiProgressBar has already been closed");
        }
    }

    private void out(String s)
    {
        System.out.print(s);
    }

    /**
     * Detects terminal rows/columns. Java 8 has no native way to query this
     * (that arrived with java.io.Console#width/height in Java 22), so we
     * shell out to "stty size", with a classic fallback.
     */
    private int[] detectTerminalSize()
    {
        try
        {
            ProcessBuilder pb = new ProcessBuilder("stty", "size");
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream())))
            {
                String line = r.readLine();
                p.waitFor();
                if (line != null)
                {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 2)
                    {
                        return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
                    }
                }
            }
        } 
        catch (Exception ignored)
        {
            // fall through to the default below
        }
        return new int[] { 24, 80 };
    }

    // ------------------------------------------------------------------
    // Demo: several simulated parallel downloads, each with its own bar,
    // started and finished in a random, interleaved order.
    // ------------------------------------------------------------------
    public static void main(String[] args) throws InterruptedException
    {
        Config config = new Config()
                .fillChar('#')
                .fillColor(Colors.GREEN)
                .labelColor(Colors.CYAN)
                .boldPercentage(true);

        try(MultiProgressBar multi = new MultiProgressBar(config))
        {
            multi.log("Reading package lists... Done");

            String[] packages = 
            {
                    "libssl3",
                    "curl",
                    "vim-common-extended-with-a-really-long-package-name",
                    "python3-minimal",
                    "openssh-client"
            };

            Thread[] workers = new Thread[packages.length];
            for (int i = 0; i < packages.length; i++) 
            {
                final String pkg = packages[i];
                workers[i] = new Thread(new Runnable() 
                {
                    @Override
                    public void run()
                    {
                        try(ProgressBar bar = multi.addBar("Downloading " + pkg))
                        {
                            for (int p = 0; p <= 100; p += 4)
                            {
                                bar.update(p);
                                Thread.sleep(60 + (int) (Math.random() * 100));
                            }
                            multi.log(pkg + " downloaded.");
                        } 
                        catch (InterruptedException ignored) 
                        {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }

            for (Thread w : workers)
            {
                w.start();
                Thread.sleep(150); // stagger the start so bars appear one by one
            }
            for (Thread w : workers)
            {
                w.join();
            }
            multi.log("All packages downloaded.");
        }

        System.out.println("Done.");
    }
}
