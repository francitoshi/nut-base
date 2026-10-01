/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Evaluates arithmetic and string expressions such as
 * {@code poyito(sqrt(max($x * 2, $y + 5)))}.
 *
 * <p><b>Variables</b> are referenced with a {@code $} prefix as in bash and are
 * registered through {@link #addVariable(String, Object)} or
 * {@link #addVariables(Map)}. The value must be a {@link Number} (normalized to
 * {@link BigInteger} or {@link BigDecimal}) or a {@link String}.</p>
 *
 * <p><b>Functions</b> are registered through
 * {@link #addFunction(String, Method)} passing a {@link Method} of a static
 * method with between 0 and 8 parameters, or through
 * {@link #addFunction(String, DoubleUnaryOperator)} and the other functional
 * interface overloads, e.g. {@code eval.addFunction("sqrt", Math::sqrt)}. When
 * the function is called, each argument is converted to the type declared by
 * the method parameter or the abstract method of the functional interface, so a
 * single function can accept numeric or string arguments regardless of how the
 * underlying numeric values are represented.</p>
 *
 * <p><b>Numbers</b>: integral literals are handled with {@link BigInteger} and
 * decimal literals with {@link BigDecimal}. The number of decimals configured
 * in the constructor is used to round the results of the arithmetic operations.
 * The {@code +} operator also concatenates {@link String}s or any value
 * converted to its {@link String} representation, mixing numbers and text.</p>
 *
 * <p>The supported operators are {@code + - * / %} and {@code ^}, with
 * parentheses and unary minus; precedence matches standard arithmetic.</p>
 *
 * @author franci
 */
public class ExpEval
{
    /** The default number of decimals used to round results. */
    public static final int DEFAULT_DECIMALS = 16;

    private static final BigDecimal INT_MAX = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal INT_MIN = BigDecimal.valueOf(Integer.MIN_VALUE);

    private final int decimals;
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
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
     * Constructs an {@code ExpEval} using {@link #DEFAULT_DECIMALS} decimals.
     */
    public ExpEval()
    {
        this(DEFAULT_DECIMALS);
    }

    /**
     * Constructs an {@code ExpEval} rounding the decimal results of the
     * arithmetic operations to the given number of decimals.
     *
     * @param decimals the number of decimals for the results; must not be negative
     * @throws IllegalArgumentException if {@code decimals} is negative
     */
    public ExpEval(int decimals)
    {
        if (decimals < 0)
        {
            throw new IllegalArgumentException("decimals must not be negative: " + decimals);
        }
        this.decimals = decimals;
    }

    /**
     * Registers a variable. The reference in the expression must be prefixed
     * with {@code $}, e.g. {@code $value}.
     *
     * @param name  the variable name without the {@code $} prefix
     * @param value the value; a {@link Number} or a {@link String}
     * @return this evaluator, for chaining
     * @throws NullPointerException     if {@code name} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is neither a {@link Number} nor a {@link String}
     */
    public ExpEval addVariable(String name, Object value)
    {
        variables.put(Objects.requireNonNull(name, "name"), normalizeValue(value));
        return this;
    }

    /**
     * Registers several variables, each of them a {@link Number} or a {@link String}.
     *
     * @param variables the variables to register
     * @return this evaluator, for chaining
     * @throws NullPointerException     if {@code variables} is {@code null}
     * @throws IllegalArgumentException if any value is neither a {@link Number} nor a {@link String}
     */
    public ExpEval addVariables(Map<String, ?> variables)
    {
        Objects.requireNonNull(variables, "variables");
        for (Map.Entry<String, ?> entry : variables.entrySet())
        {
            addVariable(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Registers a function under the given name. The {@link Method} must be a
     * static method with between 0 and 8 parameters. When invoked, each argument
     * is converted to the type declared by the corresponding method parameter.
     *
     * @param name   the function name used in the expression
     * @param method the static method implementing the function
     * @return this evaluator, for chaining
     * @throws NullPointerException     if {@code name} or {@code method} is {@code null}
     * @throws IllegalArgumentException if the method is not static or has more than 8 parameters
     */
    public ExpEval addFunction(String name, Method method)
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
    public ExpEval addFunction(String name, DoubleUnaryOperator fn)
    {
        return addFunction(name, fn, doubleUnaryOperator);
    }

    /**
     * Registers a function with two {@code double} arguments.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, DoubleBinaryOperator fn)
    {
        return addFunction(name, fn, doubleBinaryOperator);
    }

    /**
     * Registers a function with a single {@code long} argument.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, LongUnaryOperator fn)
    {
        return addFunction(name, fn, longUnaryOperator);
    }

    /**
     * Registers a function with two {@code long} arguments.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, LongBinaryOperator fn)
    {
        return addFunction(name, fn, longBinaryOperator);
    }

    /**
     * Registers a function with a single {@code int} argument.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, IntUnaryOperator fn)
    {
        return addFunction(name, fn, intUnaryOperator);
    }

    /**
     * Registers a function with two {@code int} arguments.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, IntBinaryOperator fn)
    {
        return addFunction(name, fn, intBinaryOperator);
    }

    /**
     * Registers a function with no arguments returning a {@code double}.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, DoubleSupplier fn)
    {
        return addFunction(name, fn, doubleSupplier);
    }

    /**
     * Registers a function with no arguments returning a {@code long}.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, LongSupplier fn)
    {
        return addFunction(name, fn, longSupplier);
    }

    /**
     * Registers a function with no arguments returning an {@code int}.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, IntSupplier fn)
    {
        return addFunction(name, fn, intSupplier);
    }

    /**
     * Registers a function with no arguments returning a value.
     *
     * @param name the function name used in the expression
     * @param fn   the functional interface implementing the function
     * @return this evaluator, for chaining
     */
    public ExpEval addFunction(String name, Supplier<?> fn)
    {
        return addFunction(name, fn, supplier);
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

    private ExpEval addFunction(String name, Object fn, Method method)
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
     * Evaluates the given expression and returns its value: a {@link BigInteger},
     * a {@link BigDecimal} or a {@link String}.
     *
     * @param expression the expression to evaluate
     * @return the result of the expression
     * @throws NullPointerException             if {@code expression} is {@code null}
     * @throws IllegalArgumentException         if the expression is malformed, references an
     *                                          unknown variable or function, or a function argument
     *                                          cannot be converted
     * @throws ArithmeticException              on division by zero or an out-of-range exponent
     */
    public Object eval(String expression)
    {
        Objects.requireNonNull(expression, "expression");
        List<Token> tokens = tokenize(expression);
        if (tokens.isEmpty())
        {
            throw new IllegalArgumentException("empty expression");
        }
        int[] idx = { 0 };
        Object value = parseAdditive(tokens, idx);
        if (idx[0] != tokens.size())
        {
            throw new IllegalArgumentException("unexpected token: " + tokens.get(idx[0]));
        }
        return value;
    }

    // ------------------------------------------------------------------
    // tokenizer
    // ------------------------------------------------------------------

    private enum TokenType { NUMBER, STRING, IDENT, VARIABLE, OP, LPAREN, RPAREN, COMMA }

    private static final class Token
    {
        final TokenType type;
        final String text;

        Token(TokenType type, String text)
        {
            this.type = type;
            this.text = text;
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
        while (i < n)
        {
            char c = expression.charAt(i);
            if (Character.isWhitespace(c))
            {
                i++;
                continue;
            }
            if (c == '"')
            {
                int[] next = { i + 1 };
                String value = readStringLiteral(expression, next);
                tokens.add(new Token(TokenType.STRING, value));
                i = next[0];
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
                    throw new IllegalArgumentException("malformed number: " + text);
                }
                tokens.add(new Token(TokenType.NUMBER, text));
                continue;
            }
            if (c == '$')
            {
                i++;
                int start = i;
                while (i < n && isIdentChar(expression.charAt(i)))
                {
                    i++;
                }
                if (start == i)
                {
                    throw new IllegalArgumentException("malformed variable name");
                }
                tokens.add(new Token(TokenType.VARIABLE, expression.substring(start, i)));
                continue;
            }
            if (isIdentStart(c))
            {
                int start = i;
                while (i < n && isIdentChar(expression.charAt(i)))
                {
                    i++;
                }
                tokens.add(new Token(TokenType.IDENT, expression.substring(start, i)));
                continue;
            }
            if ("+-*/%^".indexOf(c) >= 0)
            {
                tokens.add(new Token(TokenType.OP, String.valueOf(c)));
                i++;
                continue;
            }
            switch (c)
            {
                case '(': tokens.add(new Token(TokenType.LPAREN, null)); i++; break;
                case ')': tokens.add(new Token(TokenType.RPAREN, null)); i++; break;
                case ',': tokens.add(new Token(TokenType.COMMA, null)); i++; break;
                default: throw new IllegalArgumentException("unexpected character: " + c);
            }
        }
        return tokens;
    }

    private static String readStringLiteral(String expression, int[] next)
    {
        StringBuilder sb = new StringBuilder();
        int i = next[0];
        int n = expression.length();
        while (i < n)
        {
            char ch = expression.charAt(i++);
            if (ch == '"')
            {
                next[0] = i;
                return sb.toString();
            }
            if (ch == '\\')
            {
                if (i >= n)
                {
                    throw new IllegalArgumentException("unterminated escape sequence");
                }
                char esc = expression.charAt(i++);
                switch (esc)
                {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(esc); break;
                }
            }
            else
            {
                sb.append(ch);
            }
        }
        throw new IllegalArgumentException("unterminated string literal");
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

    private Object parseAdditive(List<Token> tokens, int[] idx)
    {
        Object result = parseMultiplicative(tokens, idx);
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
                result = add(result, parseMultiplicative(tokens, idx));
            }
            else if ("-".equals(token.text))
            {
                idx[0]++;
                result = sub(result, parseMultiplicative(tokens, idx));
            }
            else
            {
                break;
            }
        }
        return result;
    }

    private Object parseMultiplicative(List<Token> tokens, int[] idx)
    {
        Object result = parseUnary(tokens, idx);
        while (idx[0] < tokens.size())
        {
            Token token = tokens.get(idx[0]);
            if (token.type != TokenType.OP)
            {
                break;
            }
            if ("*".equals(token.text))
            {
                idx[0]++;
                result = mul(result, parseUnary(tokens, idx));
            }
            else if ("/".equals(token.text))
            {
                idx[0]++;
                result = div(result, parseUnary(tokens, idx));
            }
            else if ("%".equals(token.text))
            {
                idx[0]++;
                result = mod(result, parseUnary(tokens, idx));
            }
            else
            {
                break;
            }
        }
        return result;
    }

    private Object parseUnary(List<Token> tokens, int[] idx)
    {
        if (idx[0] < tokens.size() && tokens.get(idx[0]).type == TokenType.OP)
        {
            String op = tokens.get(idx[0]).text;
            if ("-".equals(op))
            {
                idx[0]++;
                return negate(parseUnary(tokens, idx));
            }
            if ("+".equals(op))
            {
                idx[0]++;
                return parseUnary(tokens, idx);
            }
        }
        return parsePower(tokens, idx);
    }

    private Object parsePower(List<Token> tokens, int[] idx)
    {
        Object base = parsePrimary(tokens, idx);
        if (idx[0] < tokens.size() && tokens.get(idx[0]).type == TokenType.OP
                && "^".equals(tokens.get(idx[0]).text))
        {
            idx[0]++;
            return pow(base, parseUnary(tokens, idx));
        }
        return base;
    }

    private Object parsePrimary(List<Token> tokens, int[] idx)
    {
        if (idx[0] >= tokens.size())
        {
            throw new IllegalArgumentException("unexpected end of expression");
        }
        Token token = tokens.get(idx[0]);
        switch (token.type)
        {
            case NUMBER:
                idx[0]++;
                return parseNumber(token.text);
            case STRING:
                idx[0]++;
                return token.text;
            case VARIABLE:
                idx[0]++;
                Object value = variables.get(token.text);
                if (value == null)
                {
                    throw new IllegalArgumentException("unknown variable: $" + token.text);
                }
                return value;
            case IDENT:
            {
                idx[0]++;
                String name = token.text;
                expect(TokenType.LPAREN, tokens, idx);
                List<Object> args = new ArrayList<>();
                if (idx[0] < tokens.size() && tokens.get(idx[0]).type != TokenType.RPAREN)
                {
                    args.add(parseAdditive(tokens, idx));
                    while (idx[0] < tokens.size() && tokens.get(idx[0]).type == TokenType.COMMA)
                    {
                        idx[0]++;
                        args.add(parseAdditive(tokens, idx));
                    }
                }
                expect(TokenType.RPAREN, tokens, idx);
                return callFunction(name, args);
            }
            case LPAREN:
                idx[0]++;
                Object result = parseAdditive(tokens, idx);
                expect(TokenType.RPAREN, tokens, idx);
                return result;
            default:
                throw new IllegalArgumentException("unexpected token: " + token);
        }
    }

    private static void expect(TokenType type, List<Token> tokens, int[] idx)
    {
        if (idx[0] >= tokens.size() || tokens.get(idx[0]).type != type)
        {
            throw new IllegalArgumentException("expected " + type);
        }
        idx[0]++;
    }

    private static Object parseNumber(String text)
    {
        if (text.indexOf('.') < 0)
        {
            return new BigInteger(text);
        }
        return new BigDecimal(text);
    }

    private Object callFunction(String name, List<Object> args)
    {
        FunctionEntry entry = functions.get(name);
        if (entry == null)
        {
            throw new IllegalArgumentException("unknown function: " + name);
        }
        Method method = entry.method;
        int declared = method.getParameterCount();
        if (declared != args.size())
        {
            throw new IllegalArgumentException("function " + name + " expects "
                    + declared + " argument(s), but got " + args.size());
        }
        Object[] converted = new Object[args.size()];
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < args.size(); i++)
        {
            converted[i] = convertArg(args.get(i), types[i]);
        }
        Object result;
        try
        {
            result = method.invoke(entry.target, converted);
        }
        catch (IllegalAccessException ex)
        {
            throw new IllegalArgumentException("cannot access function " + name, ex);
        }
        catch (InvocationTargetException ex)
        {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            throw new IllegalArgumentException("function " + name + " threw an exception", cause);
        }
        return normalizeResult(result);
    }

    // ------------------------------------------------------------------
    // value normalization, operators and conversions
    // ------------------------------------------------------------------

    private static Object normalizeValue(Object value)
    {
        Objects.requireNonNull(value, "value");
        if (value instanceof String || value instanceof BigInteger || value instanceof BigDecimal)
        {
            return value;
        }
        if (value instanceof Number)
        {
            Number number = (Number) value;
            if (isIntegral(number))
            {
                return BigInteger.valueOf(number.longValue());
            }
            return new BigDecimal(number.toString());
        }
        throw new IllegalArgumentException("value must be a Number or a String: "
                + value.getClass().getName());
    }

    private Object normalizeResult(Object result)
    {
        if (result == null)
        {
            throw new IllegalArgumentException("function returned null");
        }
        if (result instanceof String)
        {
            return result;
        }
        if (result instanceof BigDecimal)
        {
            return round((BigDecimal) result);
        }
        if (result instanceof BigInteger)
        {
            return result;
        }
        if (result instanceof Number)
        {
            Number number = (Number) result;
            if (isIntegral(number))
            {
                return BigInteger.valueOf(number.longValue());
            }
            return round(new BigDecimal(number.toString()));
        }
        if (result instanceof Boolean || result instanceof Character)
        {
            return result.toString();
        }
        throw new IllegalArgumentException("unsupported function return type: "
                + result.getClass().getName());
    }

    private static boolean isIntegral(Object value)
    {
        return value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long
                || value instanceof AtomicInteger || value instanceof AtomicLong
                || value instanceof BigInteger;
    }

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
            if (value instanceof BigDecimal)
            {
                return ((BigDecimal) value).toBigInteger();
            }
            if (value instanceof Number)
            {
                return BigInteger.valueOf(((Number) value).longValue());
            }
            if (value instanceof String)
            {
                return new BigDecimal((String) value).toBigInteger();
            }
        }
        if (target == BigDecimal.class)
        {
            if (value instanceof BigInteger)
            {
                return new BigDecimal((BigInteger) value);
            }
            if (value instanceof Number)
            {
                return new BigDecimal(value.toString());
            }
            if (value instanceof String)
            {
                return new BigDecimal((String) value);
            }
            throw new IllegalArgumentException("cannot convert \"" + value + "\" to BigDecimal");
        }
        if (target.isPrimitive() || Number.class.isAssignableFrom(target))
        {
            Number number = toNumber(value);
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
            throw new IllegalArgumentException("unsupported numeric parameter type: "
                    + target.getName());
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
        throw new IllegalArgumentException("cannot convert " + value.getClass().getName()
                + " to " + target.getName());
    }

    private static Number toNumber(Object value)
    {
        if (value instanceof Number)
        {
            return (Number) value;
        }
        if (value instanceof String)
        {
            return new BigDecimal((String) value);
        }
        throw new IllegalArgumentException("cannot convert " + value.getClass().getName()
                + " to a number");
    }

    private static String asString(Object value)
    {
        if (value instanceof String)
        {
            return (String) value;
        }
        if (value instanceof BigDecimal)
        {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private BigDecimal round(BigDecimal value)
    {
        return value.setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static BigDecimal asBigDecimal(Object value)
    {
        if (value instanceof BigDecimal)
        {
            return (BigDecimal) value;
        }
        if (value instanceof BigInteger)
        {
            return new BigDecimal((BigInteger) value);
        }
        throw new IllegalArgumentException("cannot use a string in arithmetic");
    }

    private static void requireNumber(Object value, String operator)
    {
        if (value instanceof String)
        {
            throw new IllegalArgumentException("cannot apply '" + operator + "' to a string");
        }
    }

    private Object add(Object a, Object b)
    {
        if (a instanceof String || b instanceof String)
        {
            return asString(a) + asString(b);
        }
        if (a instanceof BigDecimal || b instanceof BigDecimal)
        {
            return round(asBigDecimal(a).add(asBigDecimal(b)));
        }
        return ((BigInteger) a).add((BigInteger) b);
    }

    private Object sub(Object a, Object b)
    {
        requireNumber(a, "-");
        requireNumber(b, "-");
        if (a instanceof BigDecimal || b instanceof BigDecimal)
        {
            return round(asBigDecimal(a).subtract(asBigDecimal(b)));
        }
        return ((BigInteger) a).subtract((BigInteger) b);
    }

    private Object mul(Object a, Object b)
    {
        requireNumber(a, "*");
        requireNumber(b, "*");
        if (a instanceof BigDecimal || b instanceof BigDecimal)
        {
            return round(asBigDecimal(a).multiply(asBigDecimal(b)));
        }
        return ((BigInteger) a).multiply((BigInteger) b);
    }

    private Object div(Object a, Object b)
    {
        requireNumber(a, "/");
        requireNumber(b, "/");
        if (a instanceof BigInteger && b instanceof BigInteger)
        {
            BigInteger divisor = (BigInteger) b;
            if (divisor.signum() == 0)
            {
                throw new ArithmeticException("division by zero");
            }
            BigInteger dividend = (BigInteger) a;
            if (dividend.remainder(divisor).signum() == 0)
            {
                return dividend.divide(divisor);
            }
            return div(asBigDecimal(a), asBigDecimal(b));
        }
        return div(asBigDecimal(a), asBigDecimal(b));
    }

    private Object mod(Object a, Object b)
    {
        requireNumber(a, "%");
        requireNumber(b, "%");
        if (a instanceof BigInteger && b instanceof BigInteger)
        {
            BigInteger divisor = (BigInteger) b;
            if (divisor.signum() == 0)
            {
                throw new ArithmeticException("division by zero");
            }
            return ((BigInteger) a).remainder(divisor);
        }
        BigDecimal divisor = asBigDecimal(b);
        if (divisor.signum() == 0)
        {
            throw new ArithmeticException("division by zero");
        }
        return round(asBigDecimal(a).remainder(divisor));
    }

    private Object negate(Object value)
    {
        requireNumber(value, "-");
        if (value instanceof BigDecimal)
        {
            return round(((BigDecimal) value).negate());
        }
        return ((BigInteger) value).negate();
    }

    private Object pow(Object a, Object b)
    {
        requireNumber(a, "^");
        requireNumber(b, "^");
        if (a instanceof BigInteger && b instanceof BigInteger)
        {
            BigInteger base = (BigInteger) a;
            BigInteger exponent = (BigInteger) b;
            if (exponent.signum() >= 0)
            {
                if (exponent.bitLength() > 31)
                {
                    throw new ArithmeticException("exponent too large");
                }
                return base.pow(exponent.intValue());
            }
            if (base.signum() == 0)
            {
                throw new ArithmeticException("division by zero");
            }
            BigInteger magnitude = exponent.negate();
            if (magnitude.bitLength() > 31)
            {
                throw new ArithmeticException("exponent too large");
            }
            BigDecimal power = new BigDecimal(base.pow(magnitude.intValue()));
            return div(BigDecimal.ONE, power);
        }

        BigDecimal base = asBigDecimal(a);
        BigDecimal exponent = asBigDecimal(b);
        BigDecimal stripped = exponent.stripTrailingZeros();
        if (stripped.scale() <= 0)
        {
            if (stripped.compareTo(INT_MAX) > 0 || stripped.compareTo(INT_MIN) < 0)
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
        double result = Math.pow(base.doubleValue(), exponent.doubleValue());
        if (Double.isNaN(result) || Double.isInfinite(result))
        {
            throw new ArithmeticException("result is " + result);
        }
        return round(BigDecimal.valueOf(result));
    }

    private BigDecimal div(BigDecimal a, BigDecimal b)
    {
        if (b.signum() == 0)
        {
            throw new ArithmeticException("division by zero");
        }
        return round(a.divide(b, decimals, RoundingMode.HALF_UP));
    }
}