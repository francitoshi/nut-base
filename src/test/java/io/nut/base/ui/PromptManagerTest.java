package io.nut.base.ui;

import io.nut.base.ui.terminal.BoxPrinter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;

public class PromptManagerTest
{
    @Test
    @Disabled
    public void testExamples()
    {
        main();
        main(SWING);
    }

    // ─── Utilities ───────────────────────────────────────────────────────────
    private static ByteArrayInputStream input(String... lines)
    {
        String text = String.join(System.lineSeparator(), lines) + System.lineSeparator();
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void title(String t)
    {
        BoxPrinter sep = new BoxPrinter(BoxPrinter.Style.DOBLE, 52, true, true);
        sep.println(t);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAIN
    // ═════════════════════════════════════════════════════════════════════════
    public static void main(String... args)
    {
        boolean runSwing = args.length > 0 && args[0].equalsIgnoreCase(SWING);

        title("TerminalPromptManager");
        terminalExamples();

        if (runSwing)
        {
            title("SwingPromptManager");
            swingExamples();
        }
        else
        {
            System.out.println(
                    "\n  (Swing examples omitted. Run with --swing to enable them)\n");
        }

        title("END");
    }

    public static final String SWING = "--swing";

    // ═════════════════════════════════════════════════════════════════════════
    // TERMINAL EXAMPLES
    // ═════════════════════════════════════════════════════════════════════════
    static void terminalExamples()
    {
        // ── 1. [Y/n]: empty = yes ────────────────────────────────────────────
        title("Terminal 1 – [Y/n]: empty = yes");
        TerminalPromptManager t1 = new TerminalPromptManager(-1, System.out, input("", "n"));
        System.out.println("  empty → " + t1.ask("Do you want to continue?", PromptManager.AnswerMode.YN));
        System.out.println("  'n'   → " + t1.ask("Are you sure?", PromptManager.AnswerMode.YN));
        System.out.println();

        // ── 2. [y/N]: empty = no ─────────────────────────────────────────────
        title("Terminal 2 – [y/N]: empty = no");
        TerminalPromptManager t2 = new TerminalPromptManager(-1, System.out, input("", "yes"));
        System.out.println("  empty → " + t2.ask("Delete files?", PromptManager.AnswerMode.Yn));
        System.out.println("  'yes' → " + t2.ask("Create backup first?", PromptManager.AnswerMode.Yn));
        System.out.println();

        // ── 3. [yes/no]: strict, 'y' is invalid ──────────────────────────────
        title("Terminal 3 – [yes/no]: 'y' invalid → retries");
        TerminalPromptManager t3 = new TerminalPromptManager(-1, System.out, input("y", "no"));
        System.out.println("  → " + t3.ask("Do you accept the terms?", PromptManager.AnswerMode.YES_NO));
        System.out.println();

        // ── 4. maxRetries exhausted → exception ──────────────────────────────
        title("Terminal 4 – maxRetries=1 exhausted → MaxRetriesException");
        TerminalPromptManager t4 = new TerminalPromptManager(1, System.out,
                input("maybe", "perhaps"));
        try
        {
            t4.ask("Do you confirm?", PromptManager.AnswerMode.YES_NO);
        }
        catch (PromptManager.MaxRetriesException e)
        {
            System.out.println("  ✓ Caught: " + e.getMessage());
        }
        System.out.println();

        // ── 5. askSafe → Optional.empty() ────────────────────────────────────
        title("Terminal 5 – askSafe: retries exhausted → Optional.empty()");
        TerminalPromptManager t5 = new TerminalPromptManager(0, System.out, input("nope"));
        Optional<Boolean> r = t5.askSafe("Shall we continue?", PromptManager.AnswerMode.YN);
        System.out.println("  Is present? " + r.isPresent() + "  (expected: false)");
        System.out.println();

        // ── 6. Basic menu by number ──────────────────────────────────────────
        title("Terminal 6 – Menu by number");
        List<PromptManager.MenuItem> items6 = Arrays.asList(
                new PromptManager.MenuItem("new", "New Game"),
                new PromptManager.MenuItem("load", "Load Game"),
                new PromptManager.MenuItem("settings", "Settings"),
                new PromptManager.MenuItem("exit", "Exit Game")
        );
        TerminalPromptManager t6 = new TerminalPromptManager(-1, System.out, input("3"));
        PromptManager.MenuItem m6 = t6.menu("MAIN MENU", items6);
        System.out.println("  Chosen: [" + m6.getKey() + "] " + m6.getDescription());
        System.out.println();

        // ── 7. Menu with invalid input → repaints ─────────────────────────────
        title("Terminal 7 – Menu with retry (repaints the box)");
        TerminalPromptManager t7 = new TerminalPromptManager(-1, System.out, input("99", "2"));
        PromptManager.MenuItem m7 = t7.menu("PACKAGE MANAGER",
                new PromptManager.MenuItem("install", "Install packages"),
                new PromptManager.MenuItem("update", "Update system"),
                new PromptManager.MenuItem("remove", "Remove packages")
        );
        System.out.println("  Chosen: [" + m7.getKey() + "] " + m7.getDescription());
        System.out.println();

        // ── 8. Menu by string key ────────────────────────────────────────────
        title("Terminal 8 – Menu by key");
        TerminalPromptManager t8 = new TerminalPromptManager(-1, System.out, input("restart"));
        PromptManager.MenuItem m8 = t8.menu("SERVICE CONTROL",
                new PromptManager.MenuItem("start", "Start service"),
                new PromptManager.MenuItem("stop", "Stop service"),
                new PromptManager.MenuItem("restart", "Restart service"),
                new PromptManager.MenuItem("status", "View status")
        );
        System.out.println("  Chosen: [" + m8.getKey() + "] " + m8.getDescription());
        System.out.println();

        // ── 9. Menu with customized DOUBLE BoxPrinter ───────────────────────
        title("Terminal 9 – Menu with DOUBLE centered box");
        BoxPrinter doubleBox = new BoxPrinter(BoxPrinter.Style.DOBLE, 46, false, true);
        TerminalPromptManager t9 = new TerminalPromptManager(-1, System.out, input("1"));
        t9.withMenuBox(doubleBox);
        PromptManager.MenuItem m9 = t9.menu("REPORTS",
                new PromptManager.MenuItem("daily", "View daily report"),
                new PromptManager.MenuItem("monthly", "View monthly report"),
                new PromptManager.MenuItem("export", "Export to CSV"),
                new PromptManager.MenuItem("back", "Back to previous menu")
        );
        System.out.println("  Chosen: [" + m9.getKey() + "] " + m9.getDescription());
        System.out.println();

        // ── 10. menuSafe → Optional.empty() ──────────────────────────────────
        title("Terminal 10 – menuSafe: exhausted → Optional.empty()");
        TerminalPromptManager t10 = new TerminalPromptManager(1, System.out,
                input("99", "88"));
        Optional<PromptManager.MenuItem> om = t10.menuSafe("MENU",
                Arrays.asList(
                        new PromptManager.MenuItem("a", "Option A"),
                        new PromptManager.MenuItem("b", "Option B")
                ));
        System.out.println("  Is present? " + om.isPresent() + "  (expected: false)");
        System.out.println();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SWING EXAMPLES (only run with --swing)
    // ═════════════════════════════════════════════════════════════════════════
    static void swingExamples()
    {
        // ── S1. Question [Y/n] ───────────────────────────────────────────────
        SwingPromptManager sw = new SwingPromptManager(-1);

        boolean s1 = sw.ask("Do you want to update the system now?",
                PromptManager.AnswerMode.YN);
        System.out.println("  S1 [Y/n] → " + s1);

        // ── S2. Question [yes/no] ────────────────────────────────────────────
        boolean s2 = sw.ask("Do you accept the terms and conditions?",
                PromptManager.AnswerMode.YES_NO);
        System.out.println("  S2 [yes/no] → " + s2);

        // ── S3. Simple Menu ──────────────────────────────────────────────────
        PromptManager.MenuItem ms3 = sw.menu("MAIN MENU",
                new PromptManager.MenuItem("new", "New Game"),
                new PromptManager.MenuItem("load", "Load Saved Game"),
                new PromptManager.MenuItem("settings", "Settings and Configuration"),
                new PromptManager.MenuItem("exit", "Exit Game")
        );
        System.out.println("  S3 menu → [" + ms3.getKey() + "] " + ms3.getDescription());

        // ── S4. Menu with Custom Colors ──────────────────────────────────────
        SwingPromptManager swCustom = new SwingPromptManager(-1)
                .setHeaderColor(new java.awt.Color(0x1B5E20), java.awt.Color.WHITE)
                .setErrorColor(new java.awt.Color(0xB71C1C), java.awt.Color.WHITE)
                .setDialogWidth(520);

        PromptManager.MenuItem ms4 = swCustom.menu("SERVICE CONTROL",
                new PromptManager.MenuItem("start", "Start service"),
                new PromptManager.MenuItem("stop", "Stop service"),
                new PromptManager.MenuItem("restart", "Restart service"),
                new PromptManager.MenuItem("status", "View service status")
        );
        System.out.println("  S4 custom menu → [" + ms4.getKey() + "] " + ms4.getDescription());

        // ── S5. askSafe with maxRetries=1 ────────────────────────────────────
        SwingPromptManager swLimited = new SwingPromptManager(1);
        Optional<Boolean> os5 = swLimited.askSafe("Continue? (maximum two attempts)",
                PromptManager.AnswerMode.YES_NO);
        System.out.println("  S5 askSafe → " + (os5.isPresent() ? os5.get() : "cancelled/exhausted"));

        System.out.println();
    }
}
