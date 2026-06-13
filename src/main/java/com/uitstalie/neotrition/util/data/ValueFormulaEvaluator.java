package com.uitstalie.neotrition.util.data;

import com.uitstalie.neotrition.util.log.Log;

import java.util.Map;

/**
 * 表达式求值器：解析并计算 {@code value_formula} 表达式。
 *
 * <p>支持：四则运算 + 括号 + 幂运算（{@code ^}），变量 {@code healing} (int) 和 {@code saturation} (float)。
 * 标准数学优先级（{@code ^} 高于 {@code * /} 高于 {@code + -}），{@code ^} 右结合，全程 double 运算，最终 {@code (int) floor} 截断。</p>
 *
 * <h3>错误处理</h3>
 * <ul>
 *   <li>解析失败 → 异常向上抛出（上层在启动时崩溃）</li>
 *   <li>运行时除零 → log warn，除数替换为 1</li>
 * </ul>
 */
public final class ValueFormulaEvaluator {

    private ValueFormulaEvaluator() {}

    /**
     * 编译并求值。
     *
     * @param formula    公式字符串，如 {@code "healing * 2 + saturation * 0.5"}
     * @param healing    原版 healing 值
     * @param saturation 原版 saturation 值
     * @return 计算结果（int floor）
     * @throws FormulaParseException 解析失败
     */
    public static int evaluate(String formula, int healing, float saturation) {
        Map<String, Double> vars = Map.of("healing", (double) healing, "saturation", (double) saturation);
        Tokenizer tokens = new Tokenizer(formula);
        Expr expr = parseExpr(tokens);
        if (tokens.hasNext()) {
            throw new FormulaParseException("Unexpected token after expression: " + tokens.peek());
        }
        return (int) Math.floor(expr.eval(vars));
    }

    // ────── Parser (recursive descent) ──────

    private static Expr parseExpr(Tokenizer tokens) {
        Expr left = parseTerm(tokens);
        while (tokens.hasNext() && tokens.peek().isOp('+', '-')) {
            Token op = tokens.next();
            Expr right = parseTerm(tokens);
            left = new BinaryExpr(left, op, right);
        }
        return left;
    }

    private static Expr parseTerm(Tokenizer tokens) {
        Expr left = parsePower(tokens);
        while (tokens.hasNext() && tokens.peek().isOp('*', '/')) {
            Token op = tokens.next();
            Expr right = parsePower(tokens);
            left = new BinaryExpr(left, op, right);
        }
        return left;
    }

    /** 幂运算：右结合，优先级高于 * /。 */
    private static Expr parsePower(Tokenizer tokens) {
        Expr base = parseFactor(tokens);
        if (tokens.hasNext() && tokens.peek().isOp('^')) {
            tokens.next(); // consume '^'
            Expr exponent = parsePower(tokens); // right-associative recursion
            return new BinaryExpr(base, new Token(TokenType.OP, "^"), exponent);
        }
        return base;
    }

    private static Expr parseFactor(Tokenizer tokens) {
        if (!tokens.hasNext()) throw new FormulaParseException("Unexpected end of expression");

        Token t = tokens.peek();

        if (t.type == TokenType.NUMBER) {
            tokens.next();
            return new NumberExpr(Double.parseDouble(t.value));
        }
        if (t.type == TokenType.VARIABLE) {
            tokens.next();
            return new VarExpr(t.value);
        }
        if (t.type == TokenType.OP && t.value.equals("(")) {
            tokens.next(); // consume '('
            Expr expr = parseExpr(tokens);
            if (!tokens.hasNext() || !tokens.peek().value.equals(")")) {
                throw new FormulaParseException("Missing closing ')'");
            }
            tokens.next(); // consume ')'
            return expr;
        }
        if (t.type == TokenType.OP && t.value.equals("-")) {
            tokens.next(); // consume unary '-'
            Expr right = parseFactor(tokens);
            return new BinaryExpr(new NumberExpr(0), new Token(TokenType.OP, "-"), right);
        }

        throw new FormulaParseException("Unexpected token: " + t.value + " (" + t.type + ")");
    }

    // ────── AST Nodes ──────

    private interface Expr {
        double eval(Map<String, Double> vars);
    }

    private record NumberExpr(double value) implements Expr {
        @Override
        public double eval(Map<String, Double> vars) {
            return value;
        }
    }

    private record VarExpr(String name) implements Expr {
        @Override
        public double eval(Map<String, Double> vars) {
            Double v = vars.get(name);
            if (v == null) throw new FormulaEvalException("Unknown variable: " + name);
            return v;
        }
    }

    private record BinaryExpr(Expr left, Token op, Expr right) implements Expr {
        @Override
        public double eval(Map<String, Double> vars) {
            double l = left.eval(vars);
            double r = right.eval(vars);
            return switch (op.value) {
                case "+" -> l + r;
                case "-" -> l - r;
                case "*" -> l * r;
                case "^" -> Math.pow(l, r);
                case "/" -> {
                    if (r == 0) {
                        Log.w("Formula", "Division by zero in formula, divisor replaced with 1");
                        yield l / 1;
                    }
                    yield l / r;
                }
                default -> throw new FormulaEvalException("Unknown operator: " + op.value);
            };
        }
    }

    // ────── Tokenizer ──────

    private static class Tokenizer {
        private final String input;
        private int pos;
        private Token peeked;

        Tokenizer(String input) {
            this.input = input;
            this.pos = 0;
        }

        boolean hasNext() {
            if (peeked != null) return true;
            skipWhitespace();
            return pos < input.length();
        }

        Token peek() {
            if (peeked == null) peeked = readNext();
            return peeked;
        }

        Token next() {
            if (peeked != null) {
                Token t = peeked;
                peeked = null;
                return t;
            }
            return readNext();
        }

        private Token readNext() {
            skipWhitespace();
            if (pos >= input.length()) throw new FormulaParseException("Unexpected end of input");

            char c = input.charAt(pos);

            // number
            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                boolean hasDot = false;
                while (pos < input.length()) {
                    char ch = input.charAt(pos);
                    if (Character.isDigit(ch)) {
                        sb.append(ch);
                        pos++;
                    } else if (ch == '.' && !hasDot) {
                        sb.append(ch);
                        hasDot = true;
                        pos++;
                    } else {
                        break;
                    }
                }
                return new Token(TokenType.NUMBER, sb.toString());
            }

            // identifier / variable
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                while (pos < input.length()) {
                    char ch = input.charAt(pos);
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        sb.append(ch);
                        pos++;
                    } else {
                        break;
                    }
                }
                String name = sb.toString();
                if (name.equals("healing") || name.equals("saturation")) {
                    return new Token(TokenType.VARIABLE, name);
                }
                throw new FormulaParseException("Unknown variable: " + name + " (expected 'healing' or 'saturation')");
            }

            // operator / paren
            pos++; // advance past the single char
            return new Token(TokenType.OP, String.valueOf(c));
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }
    }

    private record Token(TokenType type, String value) {
        boolean isOp(char... expected) {
            if (type != TokenType.OP) return false;
            for (char e : expected) {
                if (value.length() == 1 && value.charAt(0) == e) return true;
            }
            return false;
        }
    }

    private enum TokenType { NUMBER, VARIABLE, OP }

    // ────── Exceptions ──────

    /**
     * 公式解析失败。上层应导致启动崩溃。
     */
    public static class FormulaParseException extends RuntimeException {
        public FormulaParseException(String message) {
            super("Formula parse error: " + message);
        }
    }

    /**
     * 运行时求值失败（除零已内部消化，此异常用于其他不可恢复错误）。
     */
    public static class FormulaEvalException extends RuntimeException {
        public FormulaEvalException(String message) {
            super("Formula eval error: " + message);
        }
    }
}
