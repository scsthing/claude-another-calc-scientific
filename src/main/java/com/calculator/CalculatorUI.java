package com.calculator;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Main Swing window for the Scientific Calculator.
 *
 * <h3>Layout (7 columns × 8 button rows)</h3>
 * <pre>
 *   Col:  0        1        2     |  3     4     5     6
 *   ─────────────────────────────────────────────────────
 *   R0:  DEG/RAD  ANS      π    |  MC    MR    M+    M−
 *   R1:  sin      cos      tan  |  C     ⌫     (     )
 *   R2:  sin⁻¹   cos⁻¹   tan⁻¹|  7     8     9     ÷
 *   R3:  log      ln      log₂ |  4     5     6     ×
 *   R4:  10ˣ      eˣ       e   |  1     2     3     −
 *   R5:  x²       x³      xⁿ  |  ±     0     .     +
 *   R6:  √x       ∛x     ⁿ√y  |  n!    %    1/x    =
 *   R7:  |x|     nCr     nPr  |  HIS   MS  HIST_CLR =  (= spans R6..R7)
 * </pre>
 *
 * <h3>Keyboard shortcuts</h3>
 * Digits, {@code . + - * / ^ % ( )}, Enter/= → evaluate,
 * Escape → clear, Backspace → delete last, {@code p} → π, {@code e} → e.
 */
public class CalculatorUI extends JFrame {

    // -----------------------------------------------------------------------
    // Colour palette  (dark theme)
    // -----------------------------------------------------------------------
    private static final Color C_WINDOW      = new Color(0x1C1C1E);
    private static final Color C_DISPLAY_BG  = new Color(0x2C2C2E);
    private static final Color C_SCI_BTN     = new Color(0x3A3A3C);
    private static final Color C_DIGIT_BTN   = new Color(0x48484A);
    private static final Color C_OP_BTN      = new Color(0xFF9500);  // orange
    private static final Color C_SPECIAL_BTN = new Color(0x636366);  // mid-grey
    private static final Color C_MODE_BTN    = new Color(0x2C2C8E);  // indigo
    private static final Color C_EQ_BTN      = new Color(0x30D158);  // green
    private static final Color C_WHITE       = Color.WHITE;
    private static final Color C_EXPR_TEXT   = new Color(0xAEAEB2);
    private static final Color C_MODE_TEXT   = new Color(0x0A84FF);

    // -----------------------------------------------------------------------
    // Fonts
    // -----------------------------------------------------------------------
    private static final Font FONT_RESULT  = deriveFont(Font.PLAIN, 46);
    private static final Font FONT_EXPR    = deriveFont(Font.PLAIN, 17);
    private static final Font FONT_BTN     = deriveFont(Font.PLAIN, 15);
    private static final Font FONT_BTN_SM  = deriveFont(Font.PLAIN, 12);
    private static final Font FONT_MODE    = deriveFont(Font.BOLD,  11);

    private static Font deriveFont(int style, float size) {
        // Try modern fonts; fall back to logical SansSerif family.
        for (String name : new String[]{"Helvetica Neue", "Segoe UI", "Ubuntu", Font.SANS_SERIF}) {
            Font f = new Font(name, style, (int) size);
            if (!f.getFamily().equals("Dialog") || name.equals(Font.SANS_SERIF)) return f;
        }
        return new Font(Font.SANS_SERIF, style, (int) size);
    }

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------
    private final CalculatorEngine engine = new CalculatorEngine();

    /** The expression string the user is building. */
    private final StringBuilder currentExpr = new StringBuilder();

    /** True immediately after '=' is pressed; next digit input starts fresh. */
    private boolean justEvaluated = false;

    /** True when the display is showing an error message. */
    private boolean errorState = false;

    // -----------------------------------------------------------------------
    // Display widgets
    // -----------------------------------------------------------------------
    private JLabel lblResult;
    private JLabel lblExpression;
    private JLabel lblMode;
    private JLabel lblMemory;

    // -----------------------------------------------------------------------
    // History sidebar
    // -----------------------------------------------------------------------
    private DefaultListModel<String> historyModel;
    private JList<String>            historyList;
    private JPanel                   historySidebar;
    private boolean                  historyVisible = false;

    // DEG/RAD toggle button reference (so we can update its text)
    private CalcButton btnDegRad;

    // =========================================================================
    // Constructor
    // =========================================================================

    public CalculatorUI() {
        super("Scientific Calculator");
        buildUI();
        updateDisplay();
    }

    // =========================================================================
    // UI construction
    // =========================================================================

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(C_WINDOW);

        // Main calculator column
        JPanel calcCol = new JPanel(new BorderLayout(0, 6));
        calcCol.setBackground(C_WINDOW);
        calcCol.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        calcCol.add(buildDisplayPanel(), BorderLayout.NORTH);
        calcCol.add(buildButtonGrid(),  BorderLayout.CENTER);
        root.add(calcCol, BorderLayout.CENTER);

        // History sidebar (initially hidden)
        historyModel   = new DefaultListModel<>();
        historyList    = new JList<>(historyModel);
        historyList.setBackground(new Color(0x1C1C2E));
        historyList.setForeground(C_EXPR_TEXT);
        historyList.setFont(FONT_BTN_SM);
        historyList.setSelectionBackground(new Color(0x3A3A5C));
        historyList.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        historySidebar = new JPanel(new BorderLayout());
        historySidebar.setBackground(new Color(0x1C1C2E));
        historySidebar.setPreferredSize(new Dimension(220, 0));

        JLabel histTitle = new JLabel("  History");
        histTitle.setForeground(C_EXPR_TEXT);
        histTitle.setFont(FONT_MODE.deriveFont(Font.BOLD, 13f));
        histTitle.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

        JButton btnClearHist = makeTextButton("Clear", C_SPECIAL_BTN);
        btnClearHist.addActionListener(e -> { engine.clearHistory(); refreshHistory(); });

        JPanel histHeader = new JPanel(new BorderLayout());
        histHeader.setBackground(new Color(0x1C1C2E));
        histHeader.add(histTitle, BorderLayout.CENTER);
        histHeader.add(btnClearHist, BorderLayout.EAST);

        historySidebar.add(histHeader, BorderLayout.NORTH);
        historySidebar.add(new JScrollPane(historyList), BorderLayout.CENTER);
        historySidebar.setVisible(false);
        root.add(historySidebar, BorderLayout.EAST);

        // History list click — re-enter the expression
        historyList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String sel = historyList.getSelectedValue();
                    if (sel != null && sel.contains(" = ")) {
                        String expr = sel.substring(0, sel.lastIndexOf(" = "));
                        currentExpr.setLength(0);
                        currentExpr.append(expr);
                        justEvaluated = false;
                        errorState    = false;
                        updateDisplay();
                    }
                }
            }
        });

        add(root);
        pack();
        setLocationRelativeTo(null);
        setupKeyboard();
    }

    // -----------------------------------------------------------------------
    // Display panel
    // -----------------------------------------------------------------------

    private JPanel buildDisplayPanel() {
        JPanel outer = new RoundPanel(14, C_DISPLAY_BG);
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        outer.setPreferredSize(new Dimension(560, 115));

        // Top row: mode label + memory indicator
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        lblMode = new JLabel("DEG");
        lblMode.setFont(FONT_MODE);
        lblMode.setForeground(C_MODE_TEXT);

        lblMemory = new JLabel("");
        lblMemory.setFont(FONT_MODE);
        lblMemory.setForeground(C_MODE_TEXT);

        topRow.add(lblMode,   BorderLayout.WEST);
        topRow.add(lblMemory, BorderLayout.EAST);
        outer.add(topRow);
        outer.add(Box.createVerticalStrut(2));

        // Expression label (right-aligned, smaller, grey)
        lblExpression = new JLabel(" ");
        lblExpression.setFont(FONT_EXPR);
        lblExpression.setForeground(C_EXPR_TEXT);
        lblExpression.setHorizontalAlignment(SwingConstants.RIGHT);
        lblExpression.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel exprRow = new JPanel(new BorderLayout());
        exprRow.setOpaque(false);
        exprRow.add(lblExpression, BorderLayout.CENTER);
        outer.add(exprRow);

        // Result label (right-aligned, large, white)
        lblResult = new JLabel("0");
        lblResult.setFont(FONT_RESULT);
        lblResult.setForeground(C_WHITE);
        lblResult.setHorizontalAlignment(SwingConstants.RIGHT);
        lblResult.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel resultRow = new JPanel(new BorderLayout());
        resultRow.setOpaque(false);
        resultRow.add(lblResult, BorderLayout.CENTER);
        outer.add(resultRow);

        return outer;
    }

    // -----------------------------------------------------------------------
    // Button grid  (7 cols × 8 rows)
    // -----------------------------------------------------------------------

    /**
     * Each entry: { label, action, row, col, colSpan, rowSpan, type }
     * Type chars: S=scientific, D=digit, O=operator, P=special, M=mode, E=equals
     */
    private void addBtn(JPanel p, GridBagConstraints g,
                        String label, String action,
                        int row, int col, int cspan, int rspan, char type) {
        Color bg;
        Font  font = FONT_BTN;
        switch (type) {
            case 'S': bg = C_SCI_BTN;     break;
            case 'D': bg = C_DIGIT_BTN;   break;
            case 'O': bg = C_OP_BTN;      break;
            case 'P': bg = C_SPECIAL_BTN; break;
            case 'M': bg = C_MODE_BTN;    break;
            case 'E': bg = C_EQ_BTN;      break;
            default:  bg = C_SCI_BTN;
        }
        if (label.length() > 4) font = FONT_BTN_SM;

        CalcButton btn = new CalcButton(label, bg, font);
        btn.addActionListener(e -> handleAction(action));

        // Stash the DEG/RAD button for later text updates
        if ("DEG_RAD".equals(action)) btnDegRad = btn;

        g.gridx      = col;
        g.gridy      = row;
        g.gridwidth  = cspan;
        g.gridheight = rspan;
        p.add(btn, g);
    }

    private JPanel buildButtonGrid() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(C_WINDOW);

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.BOTH;
        g.insets  = new Insets(3, 3, 3, 3);
        g.weightx = 1.0;
        g.weighty = 1.0;
        g.ipadx   = 0;
        g.ipady   = 12;

        // Row 0 – mode / memory
        addBtn(p,g, "DEG",   "DEG_RAD", 0, 0, 1, 1, 'M');
        addBtn(p,g, "ANS",   "ANS",     0, 1, 1, 1, 'P');
        addBtn(p,g, "π",     "pi",      0, 2, 1, 1, 'S');
        addBtn(p,g, "MC",    "MC",      0, 3, 1, 1, 'P');
        addBtn(p,g, "MR",    "MR",      0, 4, 1, 1, 'P');
        addBtn(p,g, "M+",    "M_PLUS",  0, 5, 1, 1, 'P');
        addBtn(p,g, "M−",    "M_MINUS", 0, 6, 1, 1, 'P');

        // Row 1 – basic trig / clear
        addBtn(p,g, "sin",   "sin(",    1, 0, 1, 1, 'S');
        addBtn(p,g, "cos",   "cos(",    1, 1, 1, 1, 'S');
        addBtn(p,g, "tan",   "tan(",    1, 2, 1, 1, 'S');
        addBtn(p,g, "C",     "CLEAR",   1, 3, 1, 1, 'P');
        addBtn(p,g, "⌫",    "BACK",    1, 4, 1, 1, 'P');
        addBtn(p,g, "(",     "(",       1, 5, 1, 1, 'P');
        addBtn(p,g, ")",     ")",       1, 6, 1, 1, 'P');

        // Row 2 – inverse trig / 7 8 9 ÷
        addBtn(p,g, "sin⁻¹", "asin(",  2, 0, 1, 1, 'S');
        addBtn(p,g, "cos⁻¹", "acos(",  2, 1, 1, 1, 'S');
        addBtn(p,g, "tan⁻¹", "atan(",  2, 2, 1, 1, 'S');
        addBtn(p,g, "7",     "7",       2, 3, 1, 1, 'D');
        addBtn(p,g, "8",     "8",       2, 4, 1, 1, 'D');
        addBtn(p,g, "9",     "9",       2, 5, 1, 1, 'D');
        addBtn(p,g, "÷",    "/",        2, 6, 1, 1, 'O');

        // Row 3 – logarithms / 4 5 6 ×
        addBtn(p,g, "log",   "log(",    3, 0, 1, 1, 'S');
        addBtn(p,g, "ln",    "ln(",     3, 1, 1, 1, 'S');
        addBtn(p,g, "log₂",  "log2(",   3, 2, 1, 1, 'S');
        addBtn(p,g, "4",     "4",       3, 3, 1, 1, 'D');
        addBtn(p,g, "5",     "5",       3, 4, 1, 1, 'D');
        addBtn(p,g, "6",     "6",       3, 5, 1, 1, 'D');
        addBtn(p,g, "×",    "*",        3, 6, 1, 1, 'O');

        // Row 4 – powers / 1 2 3 −
        addBtn(p,g, "10ˣ",  "10^(",     4, 0, 1, 1, 'S');
        addBtn(p,g, "eˣ",   "exp(",     4, 1, 1, 1, 'S');
        addBtn(p,g, "e",    "e",        4, 2, 1, 1, 'S');
        addBtn(p,g, "1",    "1",        4, 3, 1, 1, 'D');
        addBtn(p,g, "2",    "2",        4, 4, 1, 1, 'D');
        addBtn(p,g, "3",    "3",        4, 5, 1, 1, 'D');
        addBtn(p,g, "−",   "-",         4, 6, 1, 1, 'O');

        // Row 5 – squares/power / ± 0 . +
        addBtn(p,g, "x²",   "^2",       5, 0, 1, 1, 'S');
        addBtn(p,g, "x³",   "^3",       5, 1, 1, 1, 'S');
        addBtn(p,g, "xⁿ",  "^(",        5, 2, 1, 1, 'S');
        addBtn(p,g, "±",    "NEGATE",   5, 3, 1, 1, 'D');
        addBtn(p,g, "0",    "0",        5, 4, 1, 1, 'D');
        addBtn(p,g, ".",    ".",        5, 5, 1, 1, 'D');
        addBtn(p,g, "+",    "+",        5, 6, 1, 1, 'O');

        // Row 6 – roots / n! % 1/x  =  (spans rows 6+7)
        addBtn(p,g, "√x",   "sqrt(",    6, 0, 1, 1, 'S');
        addBtn(p,g, "∛x",   "cbrt(",    6, 1, 1, 1, 'S');
        addBtn(p,g, "ⁿ√y",  "nthrt(",   6, 2, 1, 1, 'S');
        addBtn(p,g, "n!",   "FACT",     6, 3, 1, 1, 'S');
        addBtn(p,g, "%",    "PCT",      6, 4, 1, 1, 'S');
        addBtn(p,g, "1/x",  "RECIP",    6, 5, 1, 1, 'S');
        addBtn(p,g, "=",    "EVAL",     6, 6, 1, 2, 'E');   // rowspan=2

        // Row 7 – abs, nCr, nPr, HIST, MS, HISTORY
        addBtn(p,g, "|x|",  "abs(",     7, 0, 1, 1, 'S');
        addBtn(p,g, "nCr",  "nCr(",     7, 1, 1, 1, 'S');
        addBtn(p,g, "nPr",  "nPr(",     7, 2, 1, 1, 'S');
        addBtn(p,g, "HIST", "HISTORY",  7, 3, 1, 1, 'P');
        addBtn(p,g, "MS",   "MS",       7, 4, 1, 1, 'P');
        addBtn(p,g, "CLR H","HIST_CLR", 7, 5, 1, 1, 'P');
        // col 6 is the spanning "=" — no extra button needed

        return p;
    }

    // -----------------------------------------------------------------------
    // Small text button (for sidebar header)
    // -----------------------------------------------------------------------

    private JButton makeTextButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(FONT_BTN_SM);
        b.setBackground(bg);
        b.setForeground(C_WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        return b;
    }

    // =========================================================================
    // Action handler
    // =========================================================================

    private void handleAction(String action) {
        errorState = false;

        switch (action) {

            // ---- digits & decimal -----------------------------------------
            case "0": case "1": case "2": case "3": case "4":
            case "5": case "6": case "7": case "8": case "9":
                if (justEvaluated) { currentExpr.setLength(0); justEvaluated = false; }
                currentExpr.append(action);
                break;

            case ".":
                if (justEvaluated) { currentExpr.setLength(0); justEvaluated = false; }
                // Avoid double dot in the current number segment
                if (!lastSegmentHasDot()) currentExpr.append(".");
                break;

            // ---- binary operators -----------------------------------------
            case "+": case "-": case "*": case "/": case "^":
                if (justEvaluated) {
                    // Continue from last answer
                    currentExpr.setLength(0);
                    currentExpr.append(CalculatorEngine.formatResult(engine.getLastAnswer()));
                    justEvaluated = false;
                }
                currentExpr.append(action);
                break;

            // ---- parentheses & related ------------------------------------
            case "(": case ")":
                if (justEvaluated) { currentExpr.setLength(0); justEvaluated = false; }
                currentExpr.append(action);
                break;

            // ---- constants ------------------------------------------------
            case "pi": case "e":
                if (justEvaluated) { currentExpr.setLength(0); justEvaluated = false; }
                currentExpr.append(action);
                break;

            // ---- unary function openers  (append "sin(" etc.) -------------
            case "sin(": case "cos(": case "tan(":
            case "asin(": case "acos(": case "atan(":
            case "log(": case "ln(": case "log2(":
            case "sqrt(": case "cbrt(": case "nthrt(":
            case "exp(": case "abs(":
            case "nCr(": case "nPr(":
                if (justEvaluated) { currentExpr.setLength(0); justEvaluated = false; }
                currentExpr.append(action);
                break;

            // ---- power shortcuts ------------------------------------------
            case "^2": case "^3": case "^(": case "10^(":
                if (justEvaluated) {
                    currentExpr.setLength(0);
                    currentExpr.append(CalculatorEngine.formatResult(engine.getLastAnswer()));
                    justEvaluated = false;
                }
                currentExpr.append(action);
                break;

            // ---- factorial (postfix) --------------------------------------
            case "FACT":
                if (justEvaluated) {
                    currentExpr.setLength(0);
                    currentExpr.append(CalculatorEngine.formatResult(engine.getLastAnswer()));
                    justEvaluated = false;
                }
                currentExpr.append("!");
                break;

            // ---- percentage -----------------------------------------------
            case "PCT":
                // Appends /100 to the current expression to convert to percent
                if (justEvaluated) {
                    currentExpr.setLength(0);
                    currentExpr.append(CalculatorEngine.formatResult(engine.getLastAnswer()));
                    justEvaluated = false;
                }
                currentExpr.append("/100");
                break;

            // ---- reciprocal -----------------------------------------------
            case "RECIP": {
                String base = justEvaluated
                        ? CalculatorEngine.formatResult(engine.getLastAnswer())
                        : currentExpr.toString();
                if (!base.isEmpty()) {
                    currentExpr.setLength(0);
                    currentExpr.append("1/(").append(base).append(")");
                    justEvaluated = false;
                }
                break;
            }

            // ---- negate ---------------------------------------------------
            case "NEGATE": {
                if (justEvaluated) {
                    double neg = -engine.getLastAnswer();
                    currentExpr.setLength(0);
                    currentExpr.append(CalculatorEngine.formatResult(neg));
                    justEvaluated = false;
                } else {
                    String s = currentExpr.toString();
                    if (s.startsWith("-(") && s.endsWith(")")) {
                        currentExpr.setLength(0);
                        currentExpr.append(s, 2, s.length() - 1);
                    } else if (!s.isEmpty()) {
                        currentExpr.setLength(0);
                        currentExpr.append("-(").append(s).append(")");
                    } else {
                        currentExpr.append("-");
                    }
                }
                break;
            }

            // ---- last answer ----------------------------------------------
            case "ANS":
                if (justEvaluated) { currentExpr.setLength(0); justEvaluated = false; }
                currentExpr.append(CalculatorEngine.formatResult(engine.getLastAnswer()));
                break;

            // ---- clear / backspace ----------------------------------------
            case "CLEAR":
                currentExpr.setLength(0);
                justEvaluated = false;
                lblExpression.setText(" ");
                break;

            case "BACK":
                if (justEvaluated) {
                    currentExpr.setLength(0);
                    justEvaluated = false;
                } else if (currentExpr.length() > 0) {
                    currentExpr.deleteCharAt(currentExpr.length() - 1);
                }
                break;

            // ---- evaluate (=) ---------------------------------------------
            case "EVAL":
                evaluateExpression();
                updateDisplay();
                return;   // updateDisplay() already called inside

            // ---- memory ---------------------------------------------------
            case "MC":
                engine.memoryClear();
                updateMemoryLabel();
                break;

            case "MR":
                if (justEvaluated) { currentExpr.setLength(0); justEvaluated = false; }
                currentExpr.append(CalculatorEngine.formatResult(engine.getMemory()));
                break;

            case "MS":
                try {
                    double val = resolveCurrentValue();
                    engine.memoryStore(val);
                    updateMemoryLabel();
                } catch (Exception ignored) {}
                break;

            case "M_PLUS":
                try {
                    engine.memoryAdd(resolveCurrentValue());
                    updateMemoryLabel();
                } catch (Exception ignored) {}
                break;

            case "M_MINUS":
                try {
                    engine.memorySubtract(resolveCurrentValue());
                    updateMemoryLabel();
                } catch (Exception ignored) {}
                break;

            // ---- angle mode toggle ----------------------------------------
            case "DEG_RAD":
                engine.toggleAngleMode();
                lblMode.setText(engine.isDegrees() ? "DEG" : "RAD");
                if (btnDegRad != null) btnDegRad.setText(engine.isDegrees() ? "DEG" : "RAD");
                break;

            // ---- history --------------------------------------------------
            case "HISTORY":
                historyVisible = !historyVisible;
                historySidebar.setVisible(historyVisible);
                pack();
                break;

            case "HIST_CLR":
                engine.clearHistory();
                refreshHistory();
                break;

            default:
                // Unknown action — ignore
                break;
        }

        updateDisplay();
    }

    // -----------------------------------------------------------------------
    // Evaluate the current expression
    // -----------------------------------------------------------------------

    private void evaluateExpression() {
        String expr = currentExpr.toString().trim();
        if (expr.isEmpty()) return;

        try {
            double result = engine.evaluate(expr);
            engine.setLastAnswer(result);
            engine.addHistory(expr, result);
            refreshHistory();

            lblExpression.setText(expr + "  =");
            String formatted = CalculatorEngine.formatResult(result);
            lblResult.setText(formatted);
            adjustResultFont(formatted);

            currentExpr.setLength(0);
            justEvaluated = true;
            errorState    = false;

        } catch (Exception ex) {
            lblResult.setText("Error: " + ex.getMessage());
            lblResult.setFont(FONT_BTN);
            lblExpression.setText(expr);
            errorState    = true;
            justEvaluated = false;
        }
    }

    // -----------------------------------------------------------------------
    // Display update
    // -----------------------------------------------------------------------

    private void updateDisplay() {
        if (!justEvaluated && !errorState) {
            String text = currentExpr.length() == 0 ? "0" : currentExpr.toString();
            lblResult.setText(text);
            adjustResultFont(text);
            lblExpression.setText(" ");
        }
        lblMode.setText(engine.isDegrees() ? "DEG" : "RAD");
        updateMemoryLabel();
    }

    private void adjustResultFont(String text) {
        int len = text.length();
        if      (len > 20) lblResult.setFont(FONT_BTN.deriveFont(Font.PLAIN, 22f));
        else if (len > 14) lblResult.setFont(FONT_BTN.deriveFont(Font.PLAIN, 30f));
        else if (len > 10) lblResult.setFont(FONT_BTN.deriveFont(Font.PLAIN, 36f));
        else               lblResult.setFont(FONT_RESULT);
    }

    private void updateMemoryLabel() {
        double m = engine.getMemory();
        lblMemory.setText(m != 0 ? "M=" + CalculatorEngine.formatResult(m) : "");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns the value to use for memory operations. */
    private double resolveCurrentValue() throws Exception {
        if (justEvaluated) return engine.getLastAnswer();
        if (currentExpr.length() > 0) return engine.evaluate(currentExpr.toString());
        return 0;
    }

    /** Returns true when the rightmost number segment already contains a '.'. */
    private boolean lastSegmentHasDot() {
        String s = currentExpr.toString();
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == '.') return true;
            if (!Character.isDigit(c)) return false;
        }
        return false;
    }

    private void refreshHistory() {
        historyModel.clear();
        List<CalculatorEngine.HistoryEntry> list = engine.getHistory();
        for (CalculatorEngine.HistoryEntry e : list) historyModel.addElement(e.toString());
    }

    // -----------------------------------------------------------------------
    // Keyboard support
    // -----------------------------------------------------------------------

    private void setupKeyboard() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(e -> {
                if (e.getID() != KeyEvent.KEY_PRESSED) return false;
                handleKeyEvent(e);
                return false;
            });
    }

    private void handleKeyEvent(KeyEvent e) {
        int    code = e.getKeyCode();
        char   ch   = e.getKeyChar();

        if (ch >= '0' && ch <= '9')  { handleAction(String.valueOf(ch)); return; }
        if (ch == '.')               { handleAction("."); return; }
        if (ch == '+')               { handleAction("+"); return; }
        if (ch == '-')               { handleAction("-"); return; }
        if (ch == '*')               { handleAction("*"); return; }
        if (ch == '/')               { handleAction("/"); return; }
        if (ch == '^')               { handleAction("^("); return; }
        if (ch == '%')               { handleAction("PCT"); return; }
        if (ch == '(')               { handleAction("("); return; }
        if (ch == ')')               { handleAction(")"); return; }
        if (ch == '!')               { handleAction("FACT"); return; }
        if (ch == 'p' || ch == 'P')  { handleAction("pi"); return; }
        if (ch == 'e')               { handleAction("e"); return; }
        if (ch == 's')               { handleAction("sin("); return; }
        if (ch == 'c')               { handleAction("cos("); return; }
        if (ch == 't')               { handleAction("tan("); return; }
        if (ch == 'l')               { handleAction("log("); return; }
        if (ch == 'n')               { handleAction("ln("); return; }
        if (ch == 'r')               { handleAction("sqrt("); return; }

        switch (code) {
            case KeyEvent.VK_ENTER:     handleAction("EVAL");  break;
            case KeyEvent.VK_EQUALS:    if (e.isShiftDown()) handleAction("+"); else handleAction("EVAL"); break;
            case KeyEvent.VK_ESCAPE:    handleAction("CLEAR"); break;
            case KeyEvent.VK_BACK_SPACE: handleAction("BACK"); break;
            case KeyEvent.VK_DELETE:    handleAction("CLEAR"); break;
        }
    }

    // =========================================================================
    // Custom button component
    // =========================================================================

    private static final class CalcButton extends JButton {

        private final Color normalBg;
        private       Color currentBg;

        CalcButton(String text, Color bg, Font font) {
            super(text);
            this.normalBg  = bg;
            this.currentBg = bg;

            setFont(font);
            setForeground(Color.WHITE);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(74, 52));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e)  { currentBg = brighten(normalBg, 0.20f); repaint(); }
                @Override public void mouseExited(MouseEvent e)   { currentBg = normalBg;                  repaint(); }
                @Override public void mousePressed(MouseEvent e)  { currentBg = darken(normalBg, 0.20f);   repaint(); }
                @Override public void mouseReleased(MouseEvent e) { currentBg = brighten(normalBg, 0.10f); repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(currentBg);
            g2.fill(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 10, 10));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override protected void paintBorder(Graphics g) { /* intentionally empty */ }

        private static Color brighten(Color c, float factor) {
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            hsb[2] = Math.min(1f, hsb[2] + factor);
            return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        }

        private static Color darken(Color c, float factor) {
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            hsb[2] = Math.max(0f, hsb[2] - factor);
            return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        }
    }

    // =========================================================================
    // Rounded panel (for the display area)
    // =========================================================================

    private static final class RoundPanel extends JPanel {
        private final int   radius;
        private final Color bg;

        RoundPanel(int radius, Color bg) {
            this.radius = radius;
            this.bg     = bg;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
