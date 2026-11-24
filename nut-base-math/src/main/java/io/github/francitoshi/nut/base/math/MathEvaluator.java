/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.math;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

/**
 * Evaluates mathematical expressions such as {@code x = 5 * 6} or
 * {@code sqrt(x) + y / 2}, keeping the values of the variables to use them
 * in later evaluations.
 *
 * <p><b>Variables</b> are referenced by their name, without any prefix, and
 * can be registered before evaluating through
 * {@link #addVariable(String, BigDecimal)} or created inside an expression
 * with an assignment such as {@code x = 5 * 6}. When {@code memory} is
 * {@code true} in the constructor, the assigned value is also kept and can be
 * used in subsequent evaluations, e.g. {@code x * 2}. When {@code memory} is
 * {@code false}, the assignment still evaluates and returns the value, but it
 * is not kept.</p>
 *
 * <p><b>Functions</b> are called by name with their arguments between
 * parentheses and separated by commas, e.g. {@code max(2x, 3)}. The built-in
 * functions are {@code sqrt}, {@code max}, {@code min}, {@code abs},
 * {@code cos}, {@code sin}, {@code logE} and {@code log10}. More functions
 * can be registered through {@link #addFunction(String, Method)} passing a
 * static {@link Method} with between 0 and 8 parameters, or through
 * {@link #addFunction(String, DoubleUnaryOperator)} and the other functional
 * interface overloads. When a function is called, each argument is converted
 * to the type declared by the method parameter or the abstract method of the
 * functional interface.</p>
 *
 * <p><b>Numbers</b>: all the arithmetic is done with {@link BigDecimal}. The
 * number of decimals configured in the constructor is used to round the
 * results of the arithmetic operations and functions.</p>
 *
 * <p>The supported operators are {@code + - * / %} and {@code ^}, with
 * parentheses and unary minus; precedence matches standard arithmetic.
 * Implicit multiplication is also supported: a number, a variable or a
 * closing parenthesis written right before a variable or an opening
 * parenthesis multiplies it, e.g. {@code 2x}, {@code 2(3+4)} or
 * {@code (1+2)(3+4)}. Juxtaposition without whitespace binds tighter than
 * {@code *}, {@code /} and {@code %}, so {@code 1/2x} is {@code 1/(2*x)},
 * while {@code 1 / 2 x} is {@code (1/2)*x}.</p>
 *
 * <p><b>Results</b>: {@link #eval(String)} never throws a domain exception;
 * it returns a {@link Evaluator.Result} whose {@code error} field is
 * {@link Evaluator.ErrorType#NONE} on success and describes the problem otherwise,
 * including the position of the offending character. The
 * {@code interpretation} field holds the explicit form of the expression as
 * it was parsed, e.g. {@code 1 / (2 * x)} for {@code 1/2x}; juxtaposed groups
 * of more than one factor are parenthesized. The {@code modifiedVariable}
 * field names the variable assigned by the expression, if any, and the
 * {@code dependencies} field lists the variables read by the expression, so
 * cached results can be invalidated when they change.</p>
 *
 * @author franci
 */
public class MathEvaluator implements Evaluator
{
    /** The default number of decimals used to round results. */
    public static final int DEFAULT_DECIMALS = 16;

    /** Sentinel position for an error caused by the end of the expression. */
    private static final int EOF = Integer.MIN_VALUE;

    private final int decimals;
    private final boolean memory;
    private final Map<String, BigDecimal> variables = new ConcurrentHashMap<>();
    private final Map<String, FunctionEntry> functions = new ConcurrentHashMap<>();

    private static final class FunctionEntry
    {
        final Method method;
        final Object target;

        FunctionEntry(Method method, Object target)
        {
            this.method = method;
            this.target = target;
        }
    }

    /**
     * Constructs a {@code MathEvaluator} using {@link #DEFAULT_DECIMALS}
     * decimals and no variable memory.
     */
    public MathEvaluator()
    {
        this(DEFAULT_DECIMALS, false);
    }

    /**
     * Constructs a {@code MathEvaluator} rounding the decimal results of the
     * arithmetic operations to the given number of decimals.
     *
     * @param decimals the number of decimals for the results; must not be negative
     * @param memory   whether the values assigned in the expressions are kept
     *                 for subsequent evaluations
     * @throws IllegalArgumentException if {@code decimals} is negative
     */
    public MathEvaluator(int decimals, boolean memory)
    {
        if (decimals < 0)
        {
            throw new IllegalArgumentException("decimals must not be negative: " + decimals);
        }
        this.decimals = decimals;
        this.memory = memory;
        registerBuiltins();
    }

    private void registerBuiltins()
    {
        register("sqrt", method("sqrt", BigDecimal.class));
        register("max", method("max", BigDecimal.class, BigDecimal.class));
        register("min", method("min", BigDecimal.class, BigDecimal.class));
        register("abs", method("abs", BigDecimal.class));
        register("cos", method("cos", BigDecimal.class));
        register("sin", method("sin", BigDecimal.class));
        register("logE", method("logE", BigDecimal.class));
        register("log10", method("log10", BigDecimal.class));
    }

    private void register(String name, Method method)
    {
        try
        {
            method.setAccessible(true);
        }
        catch (RuntimeException ignore)
        {
            // keep going; the invocation will fail if access is really denied
        }
        functions.put(name, new FunctionEntry(method, this));
    }

    private Method method(String name, Class<?>... params)
    {
        try
        {
            return MathEvaluator.class.getDeclaredMethod(name, params);
        }
        catch (NoSuchMethodException ex)
        {
            throw new AssertionError(ex);
        }
    }

    /**
     * Registers a variable that can be used in the evaluated expressions by
     * its name.
     *
     * @param name  the variable name
     * @param value the value of the variable
     * @return this evaluator, for chaining
     * @throws NullPointerException if {@code name} or {@code value} is {@code null}
     */
    public MathEvaluator addVariable(String name, BigDecimal value)
    {
        variables.put(Objects.requireNonNull(name, "name"), round(Objects.requireNonNull(value, "value")));
        return this;
    }

    /**
     * Returns the current value of a variable.
     *
     * @param name the variable name
     * @return the value of the variable, or {@code null} if it is not registered
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public BigDecimal getVariable(String name)
    {
        return variables.get(Objects.requireNonNull(name, "name"));
    }

    /**
     * Removes all the registered variables.
     */
    public void clearVariables()
    {
        variables.clear();
    }

    /**
     * Registers a function under the given name. The {@link Method} must be a
     * static method with between 0 and 8 parameters. When invoked, each
     * argument is converted to the type declared by the corresponding method
     * parameter. A function registered under the name of a built-in function
     * replaces it.
     *
     * @param name   the function name used in the expression
     * @param method the static method implementing the function
     * @return this evaluator, for chaining
     * @throws NullPointerException     if {@code name} or {@code method} is {@code null}
     * @throws IllegalArgumentException if the method is not static or has more than 8 parameters
     */
    public MathEvaluator addFunction(String name, Method method)
    {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(method, "method");
        if (method.getParameterCount() > 8)
        {
            throw new IllegalArgumentException("method must have at most 8 parameters: " + method);
        }
        if (!Modifier.isStatic(method.getModifiers()))
        {
            throw new IllegalArgumentException("method must be static: " + method);
        }
        try
        {
            method.setAccessible(true);
        }
        catch (RuntimeException ignore)
        {
            // keep going; the invocation will fail if access is really denied
        }
        functions.put(name, new FunctionEntry(method, null));
        return this;
    }

    /**
     * Registers a function with a single {@code double} argument.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, DoubleUnaryOperator fn)
    {
        return addFunction(name, (Object) fn, doubleUnaryOperator);
    }

    /**
     * Registers a function with two {@code double} arguments.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, DoubleBinaryOperator fn)
    {
        return addFunction(name, (Object) fn, doubleBinaryOperator);
    }

    /**
     * Registers a function with a single {@code long} argument.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, LongUnaryOperator fn)
    {
        return addFunction(name, (Object) fn, longUnaryOperator);
    }

    /**
     * Registers a function with two {@code long} arguments.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, LongBinaryOperator fn)
    {
        return addFunction(name, (Object) fn, longBinaryOperator);
    }

    /**
     * Registers a function with a single {@code int} argument.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, IntUnaryOperator fn)
    {
        return addFunction(name, (Object) fn, intUnaryOperator);
    }

    /**
     * Registers a function with two {@code int} arguments.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, IntBinaryOperator fn)
    {
        return addFunction(name, (Object) fn, intBinaryOperator);
    }

    /**
     * Registers a function with no arguments returning a {@code double}.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, DoubleSupplier fn)
    {
        return addFunction(name, (Object) fn, doubleSupplier);
    }

    /**
     * Registers a function with no arguments returning a {@code long}.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, LongSupplier fn)
    {
        return addFunction(name, (Object) fn, longSupplier);
    }

    /**
     * Registers a function with no arguments returning an {@code int}.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, IntSupplier fn)
    {
        return addFunction(name, (Object) fn, intSupplier);
    }

    /**
     * Registers a function with no arguments returning a value.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public MathEvaluator addFunction(String name, Supplier<?> fn)
    {
        return addFunction(name, (Object) fn, supplier);
    }

    private static final Method doubleUnaryOperator = methodOf(DoubleUnaryOperator.class, "applyAsDouble", double.class);
    private static final Method doubleBinaryOperator = methodOf(DoubleBinaryOperator.class, "applyAsDouble", double.class, double.class);
    private static final Method longUnaryOperator = methodOf(LongUnaryOperator.class, "applyAsLong", long.class);
    private static final Method longBinaryOperator = methodOf(LongBinaryOperator.class, "applyAsLong", long.class, long.class);
    private static final Method intUnaryOperator = methodOf(IntUnaryOperator.class, "applyAsInt", int.class);
    private static final Method intBinaryOperator = methodOf(IntBinaryOperator.class, "applyAsInt", int.class, int.class);
    private static final Method doubleSupplier = methodOf(DoubleSupplier.class, "getAsDouble");
    private static final Method longSupplier = methodOf(LongSupplier.class, "getAsLong");
    private static final Method intSupplier = methodOf(IntSupplier.class, "getAsInt");
    private static final Method supplier = methodOf(Supplier.class, "get");

    private static Method methodOf(Class<?> type, String name, Class<?>... params)
    {
        try
        {
            Method method = type.getMethod(name, params);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException ex)
        {
            throw new AssertionError(type.getName() + "." + name, ex);
        }
    }

    private MathEvaluator addFunction(String name, Object fn, Method method)
    {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(fn, "fn");
        if (method.getParameterCount() > 8)
        {
            throw new IllegalArgumentException("method must have at most 8 parameters: " + method);
        }
        functions.put(name, new FunctionEntry(method, fn));
        return this;
    }

    /**
     * Evaluates the given expression and returns a {@link Evaluator.Result} with its
     * value as a {@link BigDecimal}. If the expression is an assignment such
     * as {@code x = 5 * 6} and {@code memory} is {@code true}, the value is
     * also kept for subsequent evaluations. A blank expression does nothing:
     * the result has a {@code null} value and no error. Errors are reported
     * in the result with the error type, the message and the position of the
     * offending character, they are never thrown.
     *
     * @param expression the expression to evaluate
     * @return the result of the expression
     * @throws NullPointerException if {@code expression} is {@code null}
     */
    @Override
    public Result eval(String expression)
    {
        Objects.requireNonNull(expression, "expression");
        StringBuilder out = new StringBuilder();
        Set<String> deps = new LinkedHashSet<>();
        String assigned = null;
        try
        {
            List<Token> tokens = tokenize(expression);
            if (tokens.isEmpty())
            {
                return new Result(null, null, null, ErrorType.NONE, null, -1);
            }
            List<Token> body = tokens;
            if (tokens.size() >= 2 && tokens.get(0).type == TokenType.IDENT
                    && tokens.get(1).type == TokenType.OP
                    && "=".equals(tokens.get(1).text))
            {
                assigned = tokens.get(0).text;
                body = tokens.subList(2, tokens.size());
                if (body.isEmpty())
                {
                    throw new EvalException(ErrorType.INCOMPLETE_EXPRESSION,
                            "missing expression after \"=\"", EOF);
                }
                out.append(assigned).append(" = ");
            }
            BigDecimal value = evaluate(body, out, deps);
            if (assigned != null && memory)
            {
                variables.put(assigned, value);
            }
            return new Result(value, out.toString(), assigned,
                    deps.isEmpty() ? null : Collections.unmodifiableSet(deps),
                    ErrorType.NONE, null, -1);
        }
        catch (EvalException ex)
        {
            int position = ex.position == EOF ? expression.length() : ex.position;
            String interpretation = out.length() == 0 ? null : out.toString();
            return new Result(null, interpretation, assigned, ex.type, ex.getMessage(), position);
        }
    }

    private BigDecimal evaluate(List<Token> tokens, StringBuilder out, Set<String> deps)
    {
        int[] idx = { 0 };
        BigDecimal value = parseAdditive(tokens, idx, out, deps);
        if (idx[0] != tokens.size())
        {
            Token token = tokens.get(idx[0]);
            if (token.type == TokenType.OP && "=".equals(token.text))
            {
                throw new EvalException(ErrorType.INVALID_ASSIGNMENT,
                        "cannot assign to a number: " + token, token.start);
            }
            if (token.type == TokenType.RPAREN)
            {
                throw new EvalException(ErrorType.UNEXPECTED_CLOSING_PARENTHESIS,
                        "unexpected \")\"", token.start);
            }
            throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "unexpected token: " + token, token.start);
        }
        return value;
    }

    // ------------------------------------------------------------------
    // tokenizer
    // ------------------------------------------------------------------

    private enum TokenType { NUMBER, IDENT, OP, LPAREN, RPAREN, COMMA }

    private static final class Token
    {
        final TokenType type;
        final String text;
        final int start;
        /** Whether there is no whitespace between this token and the previous one. */
        final boolean glued;

        Token(TokenType type, String text, int start, boolean glued)
        {
            this.type = type;
            this.text = text;
            this.start = start;
            this.glued = glued;
        }

        @Override
        public String toString()
        {
            return type + (text == null ? "" : "(" + text + ")");
        }
    }

    private static List<Token> tokenize(String expression)
    {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int n = expression.length();
        boolean space = false;
        while (i < n)
        {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c))
            {
                space = true;
                i++;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(expression.charAt(i + 1))))
            {
                int start = i;
                boolean dot = false;
                while (i < n && (Character.isDigit(expression.charAt(i))
                        || (expression.charAt(i) == '.' && !dot)))
                {
                    if (expression.charAt(i) == '.')
                    {
                        dot = true;
                    }
                    i++;
                }
                String text = expression.substring(start, i);
                if (text.endsWith("."))
                {
                    throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "malformed number: " + text, start);
                }
                tokens.add(new Token(TokenType.NUMBER, text, start, !space));
                space = false;
                continue;
            }
            if (isIdentStart(c))
            {
                int start = i;
                while (i < n && isIdentChar(expression.charAt(i)))
                {
                    i++;
                }
                tokens.add(new Token(TokenType.IDENT, expression.substring(start, i), start, !space));
                space = false;
                continue;
            }
            if ("+-*/%^=".indexOf(c) >= 0)
            {
                tokens.add(new Token(TokenType.OP, String.valueOf(c), i, !space));
                space = false;
                i++;
                continue;
            }
            switch (c)
            {
                case '(': tokens.add(new Token(TokenType.LPAREN, null, i, !space)); i++; break;
                case ')': tokens.add(new Token(TokenType.RPAREN, null, i, !space)); i++; break;
                case ',': tokens.add(new Token(TokenType.COMMA, null, i, !space)); i++; break;
                default:
                    throw new EvalException(ErrorType.INVALID_CHARACTER,
                            "unexpected character: " + c, i);
            }
        }
        return tokens;
    }

    private static boolean isIdentStart(char c)
    {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentChar(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    // ------------------------------------------------------------------
    // parser
    // ------------------------------------------------------------------

    private BigDecimal parseAdditive(List<Token> tokens, int[] idx, StringBuilder out, Set<String> deps)
    {
        BigDecimal result = parseMultiplicative(tokens, idx, out, deps);
        while (idx[0] < tokens.size())
        {
            Token token = tokens.get(idx[0]);
            if (token.type != TokenType.OP)
            {
                break;
            }
            if ("+".equals(token.text))
            {
                idx[0]++;
                int mark = out.length();
                BigDecimal rhs = parseMultiplicative(tokens, idx, out, deps);
                out.insert(mark, " + ");
                result = round(result.add(rhs));
            }
            else if ("-".equals(token.text))
            {
                idx[0]++;
                int mark = out.length();
                BigDecimal rhs = parseMultiplicative(tokens, idx, out, deps);
                out.insert(mark, " - ");
                result = round(result.subtract(rhs));
            }
            else
            {
                break;
            }
        }
        return result;
    }

    private BigDecimal parseMultiplicative(List<Token> tokens, int[] idx, StringBuilder out, Set<String> deps)
    {
        BigDecimal result = parseUnary(tokens, idx, out, deps);
        while (idx[0] < tokens.size())
        {
            Token token = tokens.get(idx[0]);
            if (token.type == TokenType.OP)
            {
                if ("*".equals(token.text))
                {
                    idx[0]++;
                    int mark = out.length();
                    BigDecimal rhs = parseUnary(tokens, idx, out, deps);
                    out.insert(mark, " * ");
                    result = round(result.multiply(rhs));
                }
                else if ("/".equals(token.text))
                {
                    idx[0]++;
                    int mark = out.length();
                    BigDecimal rhs = parseUnary(tokens, idx, out, deps);
                    out.insert(mark, " / ");
                    try
                    {
                        result = div(result, rhs);
                    }
                    catch (ArithmeticException ex)
                    {
                        throw arithmetic(ex, token.start);
                    }
                }
                else if ("%".equals(token.text))
                {
                    idx[0]++;
                    int mark = out.length();
                    BigDecimal rhs = parseUnary(tokens, idx, out, deps);
                    out.insert(mark, " % ");
                    try
                    {
                        result = mod(result, rhs);
                    }
                    catch (ArithmeticException ex)
                    {
                        throw arithmetic(ex, token.start);
                    }
                }
                else
                {
                    break;
                }
            }
            else if (!token.glued && isImplicitMultiply(tokens, idx))
            {
                // spaced implicit multiplication, e.g. "1 / 2 x" or "2 (3+4)"
                int mark = out.length();
                BigDecimal rhs = parseUnary(tokens, idx, out, deps);
                out.insert(mark, " * ");
                result = round(result.multiply(rhs));
            }
            else
            {
                break;
            }
        }
        return result;
    }

    private BigDecimal parseUnary(List<Token> tokens, int[] idx, StringBuilder out, Set<String> deps)
    {
        if (idx[0] < tokens.size() && tokens.get(idx[0]).type == TokenType.OP)
        {
            String op = tokens.get(idx[0]).text;
            if ("-".equals(op))
            {
                idx[0]++;
                out.append("-");
                return parseUnary(tokens, idx, out, deps).negate();
            }
            if ("+".equals(op))
            {
                idx[0]++;
                return parseUnary(tokens, idx, out, deps);
            }
        }
        return parseJuxtaposition(tokens, idx, out, deps);
    }

    /**
     * Parses a chain of factors written next to each other without
     * whitespace, e.g. {@code 2x}. It binds tighter than {@code *}, {@code /}
     * and {@code %}, so {@code 1/2x} is {@code 1/(2*x)}. The chain is
     * rendered in the interpretation parenthesized whenever it has more than
     * one factor.
     */
    private BigDecimal parseJuxtaposition(List<Token> tokens, int[] idx, StringBuilder out, Set<String> deps)
    {
        int mark = out.length();
        BigDecimal result = parsePower(tokens, idx, out, deps);
        int factors = 1;
        while (idx[0] < tokens.size() && tokens.get(idx[0]).glued && isImplicitMultiply(tokens, idx))
        {
            int inner = out.length();
            BigDecimal rhs = parsePower(tokens, idx, out, deps);
            out.insert(inner, " * ");
            result = round(result.multiply(rhs));
            factors++;
        }
        if (factors > 1)
        {
            out.insert(mark, "(");
            out.append(")");
        }
        return result;
    }

    private BigDecimal parsePower(List<Token> tokens, int[] idx, StringBuilder out, Set<String> deps)
    {
        BigDecimal base = parsePrimary(tokens, idx, out, deps);
        if (idx[0] < tokens.size() && tokens.get(idx[0]).type == TokenType.OP
                && "^".equals(tokens.get(idx[0]).text))
        {
            Token token = tokens.get(idx[0]);
            idx[0]++;
            int mark = out.length();
            BigDecimal exponent = parseUnaryPower(tokens, idx, out, deps);
            out.insert(mark, " ^ ");
            try
            {
                return pow(base, exponent);
            }
            catch (ArithmeticException ex)
            {
                throw arithmetic(ex, token.start);
            }
        }
        return base;
    }

    private BigDecimal parseUnaryPower(List<Token> tokens, int[] idx, StringBuilder out, Set<String> deps)
    {
        if (idx[0] < tokens.size() && tokens.get(idx[0]).type == TokenType.OP)
        {
            String op = tokens.get(idx[0]).text;
            if ("-".equals(op))
            {
                idx[0]++;
                out.append("-");
                return parseUnaryPower(tokens, idx, out, deps).negate();
            }
            if ("+".equals(op))
            {
                idx[0]++;
                return parseUnaryPower(tokens, idx, out, deps);
            }
        }
        return parsePower(tokens, idx, out, deps);
    }

    private BigDecimal parsePrimary(List<Token> tokens, int[] idx, StringBuilder out, Set<String> deps)
    {
        if (idx[0] >= tokens.size())
        {
            throw new EvalException(ErrorType.INCOMPLETE_EXPRESSION, "unexpected end of expression", EOF);
        }
        Token token = tokens.get(idx[0]);
        switch (token.type)
        {
            case NUMBER:
                idx[0]++;
                out.append(token.text);
                return new BigDecimal(token.text);
            case IDENT:
            {
                idx[0]++;
                String name = token.text;
                FunctionEntry entry = functions.get(name);
                if (entry != null)
                {
                    if (idx[0] >= tokens.size() || tokens.get(idx[0]).type != TokenType.LPAREN)
                    {
                        throw new EvalException(ErrorType.UNDEFINED_VARIABLE,
                                "unknown variable: " + name
                                        + " (it is a function, call it as " + name + "(...))", token.start);
                    }
                    return callFunction(name, entry, tokens, idx, out, token.start, deps);
                }
                BigDecimal value = variables.get(name);
                if (value == null)
                {
                    throw new EvalException(ErrorType.UNDEFINED_VARIABLE,
                            "unknown variable: " + name, token.start);
                }
                deps.add(name);
                out.append(name);
                return value;
            }
            case LPAREN:
                idx[0]++;
                out.append("(");
                BigDecimal result = parseAdditive(tokens, idx, out, deps);
                expect(TokenType.RPAREN, tokens, idx, out);
                return result;
            case RPAREN:
                throw new EvalException(ErrorType.UNEXPECTED_CLOSING_PARENTHESIS,
                        "unexpected \")\"", token.start);
            default:
                throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "unexpected token: " + token, token.start);
        }
    }

    private BigDecimal callFunction(String name, FunctionEntry entry, List<Token> tokens,
            int[] idx, StringBuilder out, int position, Set<String> deps)
    {
        out.append(name).append("(");
        idx[0]++; // consume the opening parenthesis
        List<BigDecimal> args = new ArrayList<>();
        if (idx[0] < tokens.size() && tokens.get(idx[0]).type != TokenType.RPAREN)
        {
            args.add(parseAdditive(tokens, idx, out, deps));
            while (idx[0] < tokens.size() && tokens.get(idx[0]).type == TokenType.COMMA)
            {
                idx[0]++;
                out.append(", ");
                args.add(parseAdditive(tokens, idx, out, deps));
            }
        }
        expect(TokenType.RPAREN, tokens, idx, out);

        Method method = entry.method;
        int declared = method.getParameterCount();
        if (declared != args.size())
        {
            throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "function " + name + " expects "
                    + declared + " argument(s), but got " + args.size(), position);
        }
        Object[] converted = new Object[args.size()];
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < args.size(); i++)
        {
            try
            {
                converted[i] = convertArg(args.get(i), types[i]);
            }
            catch (RuntimeException ex)
            {
                throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "cannot convert argument "
                        + (i + 1) + " of " + name + ": " + ex.getMessage(), position);
            }
        }
        Object result;
        try
        {
            result = method.invoke(entry.target, converted);
        }
        catch (IllegalAccessException ex)
        {
            throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "cannot access function " + name, position);
        }
        catch (InvocationTargetException ex)
        {
            Throwable cause = ex.getCause();
            if (cause instanceof ArithmeticException)
            {
                throw arithmetic((ArithmeticException) cause, position);
            }
            throw new EvalException(ErrorType.UNEXPECTED_TOKEN,
                    "function " + name + " failed: " + cause, position);
        }
        return normalizeResult(result, name, position);
    }

    private BigDecimal normalizeResult(Object result, String name, int position)
    {
        if (result == null)
        {
            throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "function " + name + " returned null", position);
        }
        if (result instanceof BigDecimal)
        {
            return round((BigDecimal) result);
        }
        if (result instanceof BigInteger)
        {
            return round(new BigDecimal((BigInteger) result));
        }
        if (result instanceof Number)
        {
            Number number = (Number) result;
            if (isIntegral(number))
            {
                return round(BigDecimal.valueOf(number.longValue()));
            }
            return round(new BigDecimal(number.toString()));
        }
        throw new EvalException(ErrorType.UNEXPECTED_TOKEN, "unsupported return type of function "
                + name + ": " + result.getClass().getName(), position);
    }

    /**
     * Returns whether the token at the current position starts an implicit
     * multiplication, e.g. the {@code x} of {@code 2x} or the parenthesis of
     * {@code 2(3+4)}. A number right after another number is not a valid
     * juxtaposition, so {@code 2 3} remains an error.
     */
    private static boolean isImplicitMultiply(List<Token> tokens, int[] idx)
    {
        Token next = tokens.get(idx[0]);
        switch (next.type)
        {
            case IDENT:
            case LPAREN:
                return true;
            case NUMBER:
                return idx[0] > 0 && tokens.get(idx[0] - 1).type == TokenType.RPAREN;
            default:
                return false;
        }
    }

    private static void expect(TokenType type, List<Token> tokens, int[] idx, StringBuilder out)
    {
        if (idx[0] >= tokens.size())
        {
            throw new EvalException(ErrorType.UNCLOSED_PARENTHESIS, "missing \")\"", EOF);
        }
        Token token = tokens.get(idx[0]);
        if (token.type != type)
        {
            throw new EvalException(ErrorType.UNCLOSED_PARENTHESIS,
                    "expected \")\" but found " + token, token.start);
        }
        idx[0]++;
        out.append(")");
    }

    // ------------------------------------------------------------------
    // built-in functions
    // ------------------------------------------------------------------

    private BigDecimal sqrt(BigDecimal x)
    {
        if (x.signum() < 0)
        {
            throw new ArithmeticException("sqrt of a negative number");
        }
        MathContext context = new MathContext(decimals + 10);
        if (BIG_DECIMAL_SQRT != null)
        {
            try
            {
                return round((BigDecimal) BIG_DECIMAL_SQRT.invoke(x, context));
            }
            catch (InvocationTargetException ex)
            {
                Throwable cause = ex.getCause();
                if (cause instanceof ArithmeticException)
                {
                    throw (ArithmeticException) cause;
                }
                throw new IllegalStateException("BigDecimal.sqrt failed", cause);
            }
            catch (IllegalAccessException ex)
            {
                // fall back to the internal implementation
            }
        }
        return round(sqrtInternal(x, context));
    }

    /**
     * {@link BigDecimal#sqrt(MathContext)}, or {@code null} on Java 8, where
     * it does not exist and {@link #sqrtInternal(BigDecimal, MathContext)} is
     * used instead.
     */
    private static final Method BIG_DECIMAL_SQRT = bigDecimalSqrtMethod();

    private static Method bigDecimalSqrtMethod()
    {
        try
        {
            return BigDecimal.class.getMethod("sqrt", MathContext.class);
        }
        catch (NoSuchMethodException ex)
        {
            return null;
        }
    }

    /**
     * Square root computed in this class with an integer Newton iteration, so
     * it works on Java 8 where {@link BigDecimal#sqrt(MathContext)} is not
     * available. It computes the floor square root of
     * {@code value * 10^(2k)} with enough guard digits and scales the result
     * back by {@code 10^-k}.
     *
     * @param value   a non-negative value
     * @param context the desired precision, in digits
     * @return the square root, correctly rounded within the guard digits
     */
    private static BigDecimal sqrtInternal(BigDecimal value, MathContext context)
    {
        int precision = context.getPrecision();
        // k such that n = value * 10^(2k) is an integer whose square root
        // has about `precision` digits
        int k = Math.max((2 * precision - value.precision() + value.scale() + 1) / 2, 0);
        BigInteger n = value.unscaledValue().multiply(BigInteger.TEN.pow(2 * k - value.scale()));
        return new BigDecimal(isqrt(n), k);
    }

    /**
     * Returns the floor square root of a non-negative integer using Newton
     * iteration with an overestimating initial guess.
     */
    private static BigInteger isqrt(BigInteger n)
    {
        if (n.signum() == 0)
        {
            return BigInteger.ZERO;
        }
        BigInteger x = BigInteger.ONE.shiftLeft((n.bitLength() + 1) / 2);
        while (true)
        {
            BigInteger y = x.add(n.divide(x)).shiftRight(1);
            if (y.compareTo(x) >= 0)
            {
                return x;
            }
            x = y;
        }
    }

    private BigDecimal max(BigDecimal a, BigDecimal b)
    {
        return a.max(b);
    }

    private BigDecimal min(BigDecimal a, BigDecimal b)
    {
        return a.min(b);
    }

    private BigDecimal abs(BigDecimal x)
    {
        return x.abs();
    }

    private BigDecimal cos(BigDecimal x)
    {
        return doubleOf(Math.cos(x.doubleValue()));
    }

    private BigDecimal sin(BigDecimal x)
    {
        return doubleOf(Math.sin(x.doubleValue()));
    }

    private BigDecimal logE(BigDecimal x)
    {
        if (x.signum() <= 0)
        {
            throw new ArithmeticException("log of a non-positive number");
        }
        return doubleOf(Math.log(x.doubleValue()));
    }

    private BigDecimal log10(BigDecimal x)
    {
        if (x.signum() <= 0)
        {
            throw new ArithmeticException("log of a non-positive number");
        }
        return doubleOf(Math.log10(x.doubleValue()));
    }

    private BigDecimal doubleOf(double value)
    {
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            throw new ArithmeticException("result is " + value);
        }
        return round(BigDecimal.valueOf(value));
    }

    // ------------------------------------------------------------------
    // arithmetic
    // ------------------------------------------------------------------

    private BigDecimal round(BigDecimal value)
    {
        return value.setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private BigDecimal div(BigDecimal a, BigDecimal b)
    {
        if (b.signum() == 0)
        {
            throw new ArithmeticException("division by zero");
        }
        return round(a.divide(b, decimals, RoundingMode.HALF_UP));
    }

    private BigDecimal mod(BigDecimal a, BigDecimal b)
    {
        if (b.signum() == 0)
        {
            throw new ArithmeticException("division by zero");
        }
        return round(a.remainder(b));
    }

    private BigDecimal pow(BigDecimal base, BigDecimal exponent)
    {
        BigDecimal stripped = exponent.stripTrailingZeros();
        if (stripped.scale() > 0)
        {
            double result = Math.pow(base.doubleValue(), exponent.doubleValue());
            if (Double.isNaN(result) || Double.isInfinite(result))
            {
                throw new ArithmeticException("result is " + result);
            }
            return round(BigDecimal.valueOf(result));
        }
        if (stripped.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0
                || stripped.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0)
        {
            throw new ArithmeticException("exponent does not fit in an int");
        }
        int exp = stripped.intValue();
        if (exp >= 0)
        {
            return round(base.pow(exp));
        }
        if (base.signum() == 0)
        {
            throw new ArithmeticException("division by zero");
        }
        return div(BigDecimal.ONE, base.pow(-exp));
    }

    // ------------------------------------------------------------------
    // conversions
    // ------------------------------------------------------------------

    private static Object convertArg(Object value, Class<?> target)
    {
        if (target.isInstance(value))
        {
            return value;
        }
        if (target == String.class)
        {
            return asString(value);
        }
        if (target == BigInteger.class)
        {
            return ((BigDecimal) value).toBigInteger();
        }
        if (target == BigDecimal.class)
        {
            return value;
        }
        if (target.isPrimitive() || Number.class.isAssignableFrom(target))
        {
            Number number = (Number) value;
            if (target == long.class || target == Long.class)
            {
                return number.longValue();
            }
            if (target == int.class || target == Integer.class)
            {
                return number.intValue();
            }
            if (target == short.class || target == Short.class)
            {
                return number.shortValue();
            }
            if (target == byte.class || target == Byte.class)
            {
                return number.byteValue();
            }
            if (target == double.class || target == Double.class)
            {
                return number.doubleValue();
            }
            if (target == float.class || target == Float.class)
            {
                return number.floatValue();
            }
            throw new IllegalArgumentException("unsupported numeric parameter type: " + target.getName());
        }
        if (target == char.class || target == Character.class)
        {
            String s = asString(value);
            if (s.length() == 1)
            {
                return s.charAt(0);
            }
            throw new IllegalArgumentException("cannot convert \"" + s + "\" to char");
        }
        if (target == boolean.class || target == Boolean.class)
        {
            String s = asString(value).trim().toLowerCase(Locale.ROOT);
            if ("true".equals(s) || "1".equals(s))
            {
                return Boolean.TRUE;
            }
            if ("false".equals(s) || "0".equals(s))
            {
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("cannot convert \"" + asString(value) + "\" to boolean");
        }
        throw new IllegalArgumentException("cannot convert to " + target.getName());
    }

    private static boolean isIntegral(Object value)
    {
        return value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                || value instanceof AtomicInteger || value instanceof AtomicLong
                || value instanceof BigInteger;
    }

    private static String asString(Object value)
    {
        if (value instanceof BigDecimal)
        {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static EvalException arithmetic(ArithmeticException ex, int position)
    {
        String message = String.valueOf(ex.getMessage());
        ErrorType type = message.contains("division by zero")
                ? ErrorType.DIVISION_BY_ZERO : ErrorType.OVERFLOW;
        return new EvalException(type, message, position);
    }

    private static final class EvalException extends RuntimeException
    {
        final ErrorType type;
        final int position;

        EvalException(ErrorType type, String message, int position)
        {
            super(message);
            this.type = type;
            this.position = position;
        }
    }
}
