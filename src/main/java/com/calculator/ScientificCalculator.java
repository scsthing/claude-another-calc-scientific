package com.calculator;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Scientific Calculator — Main entry point.
 *
 * A full-featured scientific calculator built with the Java Swing GUI toolkit.
 * Supports arithmetic, trigonometric, logarithmic, exponential, and statistical
 * functions, plus memory storage and a full expression-history panel.
 *
 * Run:  java -jar scientific-calculator-standalone.jar
 */
public class ScientificCalculator {

    public static void main(String[] args) {
        // Use the system look-and-feel so the application feels native on each OS.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the cross-platform Metal L&F – still usable.
        }

        SwingUtilities.invokeLater(() -> {
            CalculatorUI ui = new CalculatorUI();
            ui.setVisible(true);
        });
    }
}
