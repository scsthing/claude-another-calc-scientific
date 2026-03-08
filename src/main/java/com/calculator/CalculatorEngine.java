package com.calculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculator engine: evaluates mathematical expression strings via a
 * hand-written recursive-descent parser.
 *
 * <h3>Grammar (simplified)</h3>
 * <pre>
 *   expr      = addSub
 *   addSub    = mulDiv ( ('+' | '-') mulDiv )*
 *   mulDiv    = power  ( ('*' | '/' | '%') power )*
 *   power     = unary  ( '^' power )?          [right-associative]
 *   unary     = ('-' | '+')* primary
 *   primary   = NUMBER
 *             | CONSTANT
 *             | FUNCTION '(' expr (',' expr)* ')'
 *             | '(' expr ')'
 *   postfix   = primary '!'?
 * </pre>
 *
 * <h3>Supported identifiers</h3>
 * <b>Constants:</b> pi, e, phi, inf<br>
 * <b>1-arg functions:</b> sin, cos, tan, asin/arcsin, acos/arccos, atan/arctan,
 *   sinh, cosh, tanh, asinh, acosh, atanh, log/log10, log2, ln,
 *   exp, sqrt, cbrt, abs, ceil, floor, round, trunc,
 *   fact/factorial, deg, rad, sq/sqr, cube, recip, neg<br>
 * <b>2-arg functions:</b> nthrt/root(n,x), pow(x,n), logb(base,x),
 *   perm/nPr(n,r), comb/nCr/choose(n,r), gcd(a,b), lcm(a,b), atan2(y,x),
 *   hypot(a,b), min(a,b), max(a,b), mod(a,b)
 */
public class CalculatorEngine {

    // -----------------------------------------------------------------------
    // Angle mode
    // -----------------------------------------------------------------------

    public enum AngleMode { DEGREES, RADIANS }

    private AngleMode angleMode = AngleMode.DEGREES;

    public AngleMode getAngleMode()            { return angleMode; }
    public void      setAngleMode(AngleMode m) { angleMode = m;    }
    public boolean   isDegrees()               { return angleMode == AngleMode.DEGREES; }

    public void toggleAngleMode() {
        angleMode = (angleMode == AngleMode.DEGREES) ? AngleMode.RADIANS : AngleMode.DEGREES;
    }

    // -----------------------------------------------------------------------
    // Memory register
    // -----------------------------------------------------------------------

    private double memory = 0.0;

    public double getMemory()                   { return memory; }
    public void   memoryClear()                 { memory = 0.0;  }
    public void   memoryStore(double v)         { memory = v;    }
    public void   memoryAdd(double v)           { memory += v;   }
    public void   memorySubtract(double v)      { memory -= v;   }
    public double memoryRecall()                { return memory; }

    // -----------------------------------------------------------------------
    // Last answer
    // -----------------------------------------------------------------------

    private double lastAnswer = 0.0;

    public double getLastAnswer()            { return lastAnswer; }
    public void   setLastAnswer(double v)    { lastAnswer = v;    }

    // -----------------------------------------------------------------------
    // History
    // -----------------------------------------------------------------------

    public static class HistoryEntry {
        public final String expression;
        public final double result;
        public final String formattedResult;

        public HistoryEntry(String expression, double result) {
            this.expression      = expression;
            this.result          = result;
            this.formattedResult = formatResult(result);
        }

        @Override public String toString() {
            return expression + " = " + formattedResult;
        }
    }

    private final List<HistoryEntry> history = new ArrayList<>();

    public List<HistoryEntry> getHistory() { return Collections.unmodifiableList(history); }

    public void addHistory(String expression, double result) {
        history.add(0, new HistoryEntry(expression, result));
        if (history.size() > 100) history.remove(history.size() - 1);
    }

    public void clearHistory() { history.clear(); }

    // -----------------------------------------------------------------------
    // Evaluate
    // -----------------------------------------------------------------------

    /**
     * Evaluates {@code expression} and returns the numeric result.
     *
     * @throws Exception with a human-readable message on any parse / math error
     */
    public double evaluate(String expression) throws Exception {
        if (expression == null || expression.isBlank()) {
            throw new Exception("Empty expression");
        }
        String normalized = normalize(expression);
        Parser parser = new Parser(normalized, angleMode);
        double result  = parser.parseExpression();
        if (parser.hasRemaining()) {
            throw new Exception("Unexpected token: '" + parser.peekRemaining() + "'");
        }
        return result;
    }

    /** Replaces display symbols with parser-friendly ASCII equivalents. */
    private String normalize(String expr) {
        return expr
            .replace("\u00D7", "*")           // ×
            .replace("\u00F7", "/")           // ÷
            .replace("\u2212", "-")           // − (minus sign)
            .replace("\u2013", "-")           // – (en dash sometimes used)
            .replace("\u03C0", "pi")          // π
            .replace("\u212F", "e")           // ℯ (script small e)
            .replace("ANS", Double.toString(lastAnswer))
            .replace("ans", Double.toString(lastAnswer))
            .replace("MEM", Double.toString(memory))
            .trim();
    }

    // -----------------------------------------------------------------------
    // Number formatting
    // -----------------------------------------------------------------------

    /**
     * Formats {@code value} for display: integers without a decimal point,
     * and real numbers with up to 10 significant digits (no trailing zeros).
     */
    public static String formatResult(double value) {
        if (Double.isNaN(value))              return "Not a Number";
        if (value == Double.POSITIVE_INFINITY) return "Infinity";
        if (value == Double.NEGATIVE_INFINITY) return "-Infinity";

        // Show as integer when possible (and not too large for long)
        if (value == Math.floor(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }

        // 10 significant figures; strip trailing zeros
        String s = String.format("%.10g", value);
        if (s.contains(".") && !s.contains("e") && !s.contains("E")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    // =========================================================================
    // Recursive-descent parser (inner class)
    // =========================================================================

    private static final class Parser {

        private final String    input;
        private       int       pos;
        private final AngleMode angleMode;

        Parser(String input, AngleMode angleMode) {
            this.input     = input;
            this.pos       = 0;
            this.angleMode = angleMode;
        }

        // ----- navigation helpers -------------------------------------------

        private void skipWS() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++;
        }

        boolean hasRemaining() {
            skipWS();
            return pos < input.length();
        }

        String peekRemaining() {
            skipWS();
            return pos < input.length() ? input.substring(pos) : "";
        }

        private char peek() {
            skipWS();
            return pos < input.length() ? input.charAt(pos) : '\0';
        }

        private char consume() {
            skipWS();
            return pos < input.length() ? input.charAt(pos++) : '\0';
        }

        private void expect(char c) throws Exception {
            skipWS();
            if (pos >= input.length() || input.charAt(pos) != c) {
                throw new Exception("Expected '" + c + "' but found '"
                        + (pos < input.length() ? input.charAt(pos) : "end") + "'");
            }
            pos++;
        }

        // ----- grammar rules ------------------------------------------------

        double parseExpression() throws Exception { return parseAddSubtract(); }

        double parseAddSubtract() throws Exception {
            double left = parseMultiplyDivide();
            while (true) {
                skipWS();
                if (pos < input.length() && input.charAt(pos) == '+') { pos++; left += parseMultiplyDivide(); }
                else if (pos < input.length() && input.charAt(pos) == '-') { pos++; left -= parseMultiplyDivide(); }
                else break;
            }
            return left;
        }

        double parseMultiplyDivide() throws Exception {
            double left = parsePower();
            while (true) {
                skipWS();
                if (pos >= input.length()) break;
                char c = input.charAt(pos);
                if (c == '*') {
                    pos++;
                    left *= parsePower();
                } else if (c == '/') {
                    pos++;
                    double d = parsePower();
                    if (d == 0) throw new Exception("Division by zero");
                    left /= d;
                } else if (c == '%') {
                    pos++;
                    double d = parsePower();
                    if (d == 0) throw new Exception("Modulo by zero");
                    left = left % d;
                } else break;
            }
            return left;
        }

        /** Right-associative exponentiation: 2^3^2 = 2^(3^2) = 512. */
        double parsePower() throws Exception {
            double base = parseUnary();
            skipWS();
            if (pos < input.length() && input.charAt(pos) == '^') {
                pos++;
                return Math.pow(base, parsePower());
            }
            return base;
        }

        double parseUnary() throws Exception {
            skipWS();
            if (pos < input.length() && input.charAt(pos) == '-') { pos++; return -parsePrimary(); }
            if (pos < input.length() && input.charAt(pos) == '+') { pos++; return  parsePrimary(); }
            return parsePrimary();
        }

        double parsePrimary() throws Exception {
            skipWS();
            if (pos >= input.length()) throw new Exception("Unexpected end of expression");

            char c = input.charAt(pos);

            // Parenthesised sub-expression
            if (c == '(') {
                pos++;
                double v = parseExpression();
                expect(')');
                return applyPostfix(v);
            }

            // Numeric literal
            if (Character.isDigit(c) || c == '.') {
                return applyPostfix(parseNumber());
            }

            // Identifier: constant or function
            if (Character.isLetter(c) || c == '_') {
                return parseIdentifier();
            }

            throw new Exception("Unexpected character: '" + c + "'");
        }

        // Factorial postfix  (e.g. 5!)
        double applyPostfix(double v) throws Exception {
            skipWS();
            if (pos < input.length() && input.charAt(pos) == '!') {
                pos++;
                return factorial(v);
            }
            return v;
        }

        // ---- number --------------------------------------------------------

        double parseNumber() throws Exception {
            int start = pos;
            boolean dot = false;

            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (Character.isDigit(c))     { pos++; }
                else if (c == '.' && !dot)    { dot = true; pos++; }
                else break;
            }

            // Scientific notation suffix: e+3, E-2, e10 …
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                int save = pos;
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) pos++;
                if (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                    while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
                } else {
                    pos = save; // not scientific notation – rewind
                }
            }

            String token = input.substring(start, pos);
            if (token.isEmpty() || token.equals(".")) throw new Exception("Invalid number");
            try {
                return Double.parseDouble(token);
            } catch (NumberFormatException ex) {
                throw new Exception("Cannot parse number: '" + token + "'");
            }
        }

        // ---- identifier (constant or function) -----------------------------

        double parseIdentifier() throws Exception {
            int start = pos;
            while (pos < input.length() && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) pos++;
            String name = input.substring(start, pos);

            skipWS();
            if (pos < input.length() && input.charAt(pos) == '(') {
                pos++; // consume '('
                return applyPostfix(parseFunction(name));
            }
            return applyPostfix(resolveConstant(name));
        }

        double resolveConstant(String name) throws Exception {
            switch (name.toLowerCase()) {
                case "pi":       return Math.PI;
                case "e":        return Math.E;
                case "phi":      return (1.0 + Math.sqrt(5.0)) / 2.0; // golden ratio
                case "inf":
                case "infinity": return Double.POSITIVE_INFINITY;
                case "nan":      return Double.NaN;
                default:         throw new Exception("Unknown constant: '" + name + "'");
            }
        }

        /** Parses arguments inside the already-consumed '(' and dispatches to the right arity. */
        double parseFunction(String name) throws Exception {
            // Empty argument list  e.g. rand()
            skipWS();
            if (pos < input.length() && input.charAt(pos) == ')') {
                pos++;
                return applyZeroArgFunction(name);
            }

            double arg1 = parseExpression();
            skipWS();

            if (pos < input.length() && input.charAt(pos) == ',') {
                pos++; // consume ','
                double arg2 = parseExpression();
                skipWS();
                // Optional third arg
                if (pos < input.length() && input.charAt(pos) == ',') {
                    pos++;
                    double arg3 = parseExpression();
                    expect(')');
                    return applyThreeArgFunction(name, arg1, arg2, arg3);
                }
                expect(')');
                return applyTwoArgFunction(name, arg1, arg2);
            }
            expect(')');
            return applyOneArgFunction(name, arg1);
        }

        // ---- function dispatch ---------------------------------------------

        double applyZeroArgFunction(String name) throws Exception {
            switch (name.toLowerCase()) {
                case "rand": return Math.random();
                default:     throw new Exception("Unknown zero-arg function: '" + name + "'");
            }
        }

        double applyOneArgFunction(String name, double x) throws Exception {
            double toRad   = (angleMode == AngleMode.DEGREES) ? Math.PI / 180.0 : 1.0;
            double fromRad = (angleMode == AngleMode.DEGREES) ? 180.0 / Math.PI : 1.0;

            switch (name.toLowerCase()) {

                // ---- trigonometric ----
                case "sin":  return Math.sin(x * toRad);
                case "cos":  return Math.cos(x * toRad);
                case "tan": {
                    if (angleMode == AngleMode.DEGREES && Math.abs(x % 180) == 90)
                        throw new Exception("tan(" + x + "°) is undefined");
                    return Math.tan(x * toRad);
                }
                case "asin": case "arcsin": {
                    if (x < -1 || x > 1) throw new Exception("asin: argument must be in [−1, 1]");
                    return Math.asin(x) * fromRad;
                }
                case "acos": case "arccos": {
                    if (x < -1 || x > 1) throw new Exception("acos: argument must be in [−1, 1]");
                    return Math.acos(x) * fromRad;
                }
                case "atan": case "arctan": return Math.atan(x) * fromRad;

                // ---- hyperbolic ----
                case "sinh":  return Math.sinh(x);
                case "cosh":  return Math.cosh(x);
                case "tanh":  return Math.tanh(x);
                case "asinh": return Math.log(x + Math.sqrt(x * x + 1));
                case "acosh": {
                    if (x < 1) throw new Exception("acosh: argument must be ≥ 1");
                    return Math.log(x + Math.sqrt(x * x - 1));
                }
                case "atanh": {
                    if (x <= -1 || x >= 1) throw new Exception("atanh: |argument| must be < 1");
                    return 0.5 * Math.log((1 + x) / (1 - x));
                }

                // ---- logarithms ----
                case "log": case "log10": {
                    if (x <= 0) throw new Exception("log: argument must be > 0");
                    return Math.log10(x);
                }
                case "log2": {
                    if (x <= 0) throw new Exception("log2: argument must be > 0");
                    return Math.log(x) / Math.log(2);
                }
                case "ln": {
                    if (x <= 0) throw new Exception("ln: argument must be > 0");
                    return Math.log(x);
                }

                // ---- exponentials ----
                case "exp":                   return Math.exp(x);
                case "exp2": case "pow2":     return Math.pow(2, x);
                case "exp10": case "pow10":   return Math.pow(10, x);

                // ---- roots ----
                case "sqrt": {
                    if (x < 0) throw new Exception("sqrt: argument must be ≥ 0");
                    return Math.sqrt(x);
                }
                case "cbrt": return Math.cbrt(x);

                // ---- rounding ----
                case "ceil":  return Math.ceil(x);
                case "floor": return Math.floor(x);
                case "round": return (double) Math.round(x);
                case "trunc": return x >= 0 ? Math.floor(x) : Math.ceil(x);
                case "frac":  return x - Math.floor(x);

                // ---- misc ----
                case "abs":                    return Math.abs(x);
                case "sgn": case "sign":
                case "signum":                 return Math.signum(x);
                case "recip": {
                    if (x == 0) throw new Exception("1/0: Division by zero");
                    return 1.0 / x;
                }
                case "fact": case "factorial": return factorial(x);
                case "deg":                    return x * 180.0 / Math.PI;  // rad → deg
                case "rad":                    return x * Math.PI / 180.0;  // deg → rad
                case "sq": case "sqr":         return x * x;
                case "cube":                   return x * x * x;
                case "neg":                    return -x;
                case "not":                    return ~(long) x;
                case "gamma":                  return gamma(x);

                default: throw new Exception("Unknown function: '" + name + "'");
            }
        }

        double applyTwoArgFunction(String name, double a, double b) throws Exception {
            switch (name.toLowerCase()) {
                case "nthrt": case "nthroot": case "root": {
                    if (a == 0) throw new Exception("Root degree cannot be zero");
                    return Math.pow(b, 1.0 / a);
                }
                case "pow": case "power": return Math.pow(a, b);
                case "logb": case "logbase": {
                    if (a <= 0 || a == 1) throw new Exception("Invalid log base: " + a);
                    if (b <= 0)           throw new Exception("logb: second argument must be > 0");
                    return Math.log(b) / Math.log(a);
                }
                case "perm": case "npr": {
                    if (a < 0 || b < 0 || b > a) throw new Exception("Invalid nPr arguments");
                    return permutation((int) a, (int) b);
                }
                case "comb": case "ncr": case "choose": {
                    if (a < 0 || b < 0 || b > a) throw new Exception("Invalid nCr arguments");
                    return combination((int) a, (int) b);
                }
                case "gcd": {
                    long x = Math.abs((long) a), y = Math.abs((long) b);
                    while (y != 0) { long t = y; y = x % y; x = t; }
                    return x;
                }
                case "lcm": {
                    long x = Math.abs((long) a), y = Math.abs((long) b);
                    if (x == 0 || y == 0) return 0;
                    long g = x, r = y;
                    while (r != 0) { long t = r; r = g % r; g = t; }
                    return (x / g) * y;
                }
                case "atan2": return Math.toDegrees(Math.atan2(a, b)); // always degrees
                case "hypot": return Math.hypot(a, b);
                case "min":   return Math.min(a, b);
                case "max":   return Math.max(a, b);
                case "mod": {
                    if (b == 0) throw new Exception("Modulo by zero");
                    return a % b;
                }
                case "and": return (double)((long) a & (long) b);
                case "or":  return (double)((long) a | (long) b);
                case "xor": return (double)((long) a ^ (long) b);
                default:    throw new Exception("Unknown two-arg function: '" + name + "'");
            }
        }

        double applyThreeArgFunction(String name, double a, double b, double c) throws Exception {
            switch (name.toLowerCase()) {
                case "clamp": return Math.max(a, Math.min(b, c)); // clamp(val, min, max)
                default:      throw new Exception("Unknown three-arg function: '" + name + "'");
            }
        }

        // ---- helpers -------------------------------------------------------

        double factorial(double n) throws Exception {
            if (n < 0)              throw new Exception("Factorial: argument must be ≥ 0");
            if (n != Math.floor(n)) throw new Exception("Factorial: argument must be an integer");
            if (n > 170)            throw new Exception("Factorial: result too large (overflow for n > 170)");
            double result = 1;
            for (long i = 2; i <= (long) n; i++) result *= i;
            return result;
        }

        double permutation(int n, int r) {
            double result = 1;
            for (int i = n; i > n - r; i--) result *= i;
            return result;
        }

        double combination(int n, int r) {
            if (r > n - r) r = n - r;
            double result = 1;
            for (int i = 0; i < r; i++) { result *= (n - i); result /= (i + 1); }
            return result;
        }

        /** Stirling approximation for Gamma, adequate for display purposes. */
        double gamma(double z) throws Exception {
            if (z <= 0 && z == Math.floor(z)) throw new Exception("Gamma: undefined for non-positive integers");
            // Lanczos approximation
            final double[] c = {0.99999999999980993, 676.5203681218851, -1259.1392167224028,
                    771.32342877765313, -176.61502916214059, 12.507343278686905,
                    -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7};
            if (z < 0.5) {
                return Math.PI / (Math.sin(Math.PI * z) * gamma(1 - z));
            }
            z -= 1;
            double x = c[0];
            for (int i = 1; i < 9; i++) x += c[i] / (z + i);
            double t = z + 8 - 0.5;
            return Math.sqrt(2 * Math.PI) * Math.pow(t, z + 0.5) * Math.exp(-t) * x;
        }
    }
}
