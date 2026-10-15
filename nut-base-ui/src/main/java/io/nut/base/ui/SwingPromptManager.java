/*
 * SwingPromptManager.java
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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SwingPromptManager – Implementation of {@link PromptManager} using the Swing
 * interface.
 *
 * <h2>Questions</h2>
 * Displays a modal dialog with:
 * <ul>
 * <li>Question icon.</li>
 * <li>Question text and format hint ({@code [Y/n]}, etc.).</li>
 * <li>Error panel highlighted in red if the previous attempt was invalid.</li>
 * <li>Text field for free-form response (YN / Yn / YES_NO modes).</li>
 * <li><em>OK</em> and <em>Cancel</em> buttons.</li>
 * </ul>
 *
 * <h2>Menus</h2>
 * Displays a modal dialog with:
 * <ul>
 * <li>Title in a colored header.</li>
 * <li>Selectable list ({@link JList}) of options.</li>
 * <li>Highlighted error panel if the previous selection was invalid.</li>
 * <li><em>OK</em> and <em>Cancel</em> buttons.</li>
 * <li>Double-click or Enter on the list confirms the selection directly.</li>
 * </ul>
 *
 * <p>
 * All dialogs are modal and created on the Event Dispatch Thread (EDT). If the
 * window is closed or Cancel is pressed, {@code doAsk}/{@code doMenu} return
 * {@code null}, which the base class interprets as a cancellation.
 *
 * <h2>Visual Customization</h2>
 * Colors, fonts, and dimensions can be changed using the setters:  {@link #setHeaderColor}, {@link #setHeaderFont}, {@link #setErrorColor}, 
 * {@link #setDialogFont}, and {@link #setDialogWidth}.
 */
public class SwingPromptManager extends PromptManager
{

    // ─── Visual Configuration ─────────────────────────────────────────────────
    private Color headerColor = new Color(0x2C3E50);
    private Color headerFg = Color.WHITE;
    private Color errorColor = new Color(0xC0392B);
    private Color errorFg = Color.WHITE;
    private Font headerFont = new Font("SansSerif", Font.BOLD, 14);
    private Font dialogFont = new Font("SansSerif", Font.PLAIN, 13);
    private Font monoFont = new Font("Monospaced", Font.PLAIN, 13);
    private int dialogWidth = 460;

    // ─── Constructors ─────────────────────────────────────────────────────────
    /**
     * @param maxRetries Maximum number of retries. -1 = infinite.
     */
    public SwingPromptManager(int maxRetries)
    {
        super(maxRetries);
    }

    // ─── Customization Setters ───────────────────────────────────────────────
    public SwingPromptManager setHeaderColor(Color bg, Color fg)
    {
        this.headerColor = bg;
        this.headerFg = fg;
        return this;
    }

    public SwingPromptManager setErrorColor(Color bg, Color fg)
    {
        this.errorColor = bg;
        this.errorFg = fg;
        return this;
    }

    public SwingPromptManager setHeaderFont(Font f)
    {
        this.headerFont = f;
        return this;
    }

    public SwingPromptManager setDialogFont(Font f)
    {
        this.dialogFont = f;
        return this;
    }

    public SwingPromptManager setDialogWidth(int w)
    {
        this.dialogWidth = w;
        return this;
    }

    // ─── Contract Implementation ─────────────────────────────────────────────
    /**
     * Shows a yes/no question dialog and returns the raw response, or
     * {@code null} if the user closes/cancels.
     */
    @Override
    protected String doAsk(String question, String hint, String errorMessage)
    {
        AtomicReference<String> result = new AtomicReference<>(null);

        Runnable task = () ->
        {
            JDialog dialog = new JDialog((Frame) null, "Confirmation", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);

            JPanel root = new JPanel(new BorderLayout(0, 0));
            root.setBackground(Color.WHITE);

            // ── Header ────────────────────────────────────────────────────
            JLabel header = new JLabel("  " + question, JLabel.LEFT);
            header.setFont(headerFont);
            header.setForeground(headerFg);
            header.setBackground(headerColor);
            header.setOpaque(true);
            header.setBorder(new EmptyBorder(12, 14, 12, 14));
            root.add(header, BorderLayout.NORTH);

            // ── Body ──────────────────────────────────────────────────────
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(Color.WHITE);
            body.setBorder(new EmptyBorder(14, 20, 10, 20));

            // Error panel (only if there's a message)
            if (errorMessage != null)
            {
                JLabel errLabel = buildErrorLabel(errorMessage);
                body.add(errLabel);
                body.add(Box.createVerticalStrut(10));
            }

            // Hint label
            JLabel hintLabel = new JLabel("Answer " + hint + ":");
            hintLabel.setFont(dialogFont);
            hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(hintLabel);
            body.add(Box.createVerticalStrut(6));

            // Text field
            JTextField field = new JTextField(20);
            field.setFont(monoFont);
            field.setMaximumSize(new Dimension(dialogWidth, 30));
            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(field);

            root.add(body, BorderLayout.CENTER);

            // ── Buttons ───────────────────────────────────────────────────
            JPanel btnPanel = buildButtonPanel(
                    dialog,
                    () -> result.set(field.getText()), // Accept
                    () -> result.set(null) // Cancel
            );
            root.add(btnPanel, BorderLayout.SOUTH);

            // Enter confirms
            field.addActionListener(e ->
            {
                result.set(field.getText());
                dialog.dispose();
            });

            // Escape cancels
            bindEscape(dialog, () -> result.set(null));

            dialog.setContentPane(root);
            dialog.pack();
            dialog.setMinimumSize(new Dimension(dialogWidth, dialog.getHeight()));
            dialog.setLocationRelativeTo(null);
            field.requestFocusInWindow();
            dialog.setVisible(true); // blocks until dispose()
        };

        runOnEDT(task);
        return result.get();
    }

    /**
     * Shows a menu dialog with a {@link JList} and returns the chosen string
     * (ordinal number as String), or {@code null} if the user cancels.
     */
    @Override
    protected String doMenu(String title, List<MenuItem> items, String errorMessage)
    {
        AtomicReference<String> result = new AtomicReference<>(null);

        Runnable task = () ->
        {
            JDialog dialog = new JDialog((Frame) null, title, true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);

            JPanel root = new JPanel(new BorderLayout(0, 0));
            root.setBackground(Color.WHITE);

            // ── Header ────────────────────────────────────────────────────
            JLabel header = new JLabel("  " + title, JLabel.LEFT);
            header.setFont(headerFont);
            header.setForeground(headerFg);
            header.setBackground(headerColor);
            header.setOpaque(true);
            header.setBorder(new EmptyBorder(12, 14, 12, 14));
            root.add(header, BorderLayout.NORTH);

            // ── Body ──────────────────────────────────────────────────────
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(Color.WHITE);
            body.setBorder(new EmptyBorder(14, 20, 10, 20));

            // Error panel (only if there's a message)
            if (errorMessage != null)
            {
                body.add(buildErrorLabel(errorMessage));
                body.add(Box.createVerticalStrut(10));
            }

            // Build list model
            DefaultListModel<String> model = new DefaultListModel<>();
            int maxKeyLen = items.stream()
                    .mapToInt(i -> i.getKey().length()).max().orElse(0);
            int digits = String.valueOf(items.size()).length();
            String numFmt = "%" + digits + "d)  %-" + maxKeyLen + "s  %s";
            for (int i = 0; i < items.size(); i++)
            {
                MenuItem item = items.get(i);
                model.addElement(String.format(numFmt,
                        i + 1, item.getKey(), item.getDescription()));
            }

            JList<String> list = new JList<>(model);
            list.setFont(monoFont);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectedIndex(0);
            list.setBackground(new Color(0xF8F9FA));
            list.setSelectionBackground(headerColor);
            list.setSelectionForeground(headerFg);
            list.setBorder(new LineBorder(new Color(0xCED4DA)));
            list.setFixedCellHeight(28);
            list.setAlignmentX(Component.LEFT_ALIGNMENT);

            JScrollPane scroll = new JScrollPane(list);
            scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
            scroll.setMaximumSize(new Dimension(dialogWidth, 220));
            scroll.setBorder(BorderFactory.createEmptyBorder());
            body.add(scroll);

            root.add(body, BorderLayout.CENTER);

            // ── Buttons ───────────────────────────────────────────────────
            JPanel btnPanel = buildButtonPanel(
                    dialog,
                    () ->
            {
                int idx = list.getSelectedIndex();
                if (idx >= 0)
                {
                    result.set(String.valueOf(idx + 1));
                }
            },
                    () -> result.set(null)
            );
            root.add(btnPanel, BorderLayout.SOUTH);

            // Double-click confirms
            list.addMouseListener(new java.awt.event.MouseAdapter()
            {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e)
                {
                    if (e.getClickCount() == 2)
                    {
                        int idx = list.getSelectedIndex();
                        if (idx >= 0)
                        {
                            result.set(String.valueOf(idx + 1));
                            dialog.dispose();
                        }
                    }
                }
            });

            // Enter on list confirms
            list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirm");
            list.getActionMap().put("confirm", new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    int idx = list.getSelectedIndex();
                    if (idx >= 0)
                    {
                        result.set(String.valueOf(idx + 1));
                        dialog.dispose();
                    }
                }
            });

            // Escape cancels
            bindEscape(dialog, () -> result.set(null));

            dialog.setContentPane(root);
            dialog.pack();
            dialog.setMinimumSize(new Dimension(dialogWidth, dialog.getHeight()));
            dialog.setLocationRelativeTo(null);
            list.requestFocusInWindow();
            dialog.setVisible(true); // blocks until dispose()
        };

        runOnEDT(task);
        return result.get();
    }

    // ─── UI Construction Helpers ─────────────────────────────────────────────
    /**
     * Error label with a red background.
     */
    private JLabel buildErrorLabel(String message)
    {
        JLabel lbl = new JLabel("  ✗  " + message);
        lbl.setFont(dialogFont.deriveFont(Font.BOLD));
        lbl.setForeground(errorFg);
        lbl.setBackground(errorColor);
        lbl.setOpaque(true);
        lbl.setBorder(new EmptyBorder(6, 10, 6, 10));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return lbl;
    }

    /**
     * Bottom panel with OK and Cancel buttons. Actions receive the work to be
     * executed <em>before</em> closing the dialog.
     */
    private JPanel buildButtonPanel(JDialog dialog,
            Runnable onAccept,
            Runnable onCancel)
    {
        JButton btnOk = new JButton("OK");
        JButton btnCancel = new JButton("Cancel");

        btnOk.setFont(dialogFont.deriveFont(Font.BOLD));
        btnCancel.setFont(dialogFont);

        // Main button style
        btnOk.setBackground(headerColor);
        btnOk.setForeground(headerFg);
        btnOk.setFocusPainted(false);
        btnOk.setBorderPainted(false);
        btnOk.setOpaque(true);

        btnOk.addActionListener(e ->
        {
            onAccept.run();
            dialog.dispose();
        });
        btnCancel.addActionListener(e ->
        {
            onCancel.run();
            dialog.dispose();
        });

        // Closing window = cancel
        dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                onCancel.run();
            }
        });

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(new Color(0xF1F3F5));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                new Color(0xDEE2E6)));
        panel.add(btnCancel);
        panel.add(btnOk);

        // Default button when Enter is pressed outside the list/field
        dialog.getRootPane().setDefaultButton(btnOk);

        return panel;
    }

    /**
     * Binds the Escape key to close/cancel the dialog.
     */
    private void bindEscape(JDialog dialog, Runnable onCancel)
    {
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        dialog.getRootPane().getActionMap().put("escape", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                onCancel.run();
                dialog.dispose();
            }
        });
    }

    // ─── EDT ─────────────────────────────────────────────────────────────────
    /**
     * Executes the task on the EDT and waits for it to finish (invokeAndWait).
     * If already on the EDT, it runs immediately.
     */
    private static void runOnEDT(Runnable task)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            task.run();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(task);
            }
            catch (Exception ex)
            {
                throw new RuntimeException("Error executing Swing dialog on EDT", ex);
            }
        }
    }
}
