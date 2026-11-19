/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.math;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExpressionEvaluatorTest
{
    private ExpressionEvaluator eval;

    @BeforeEach
    public void setUp()
    {
        eval = new ExpressionEvaluator();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    public static String poyito(double value)
    {
        return "poyito=" + value;
    }

    public static int sum(int a, int b)
    {
        return a + b;
    }

    public static BigInteger sumBig(BigInteger a, BigInteger b)
    {
        return a.add(b);
    }

    public static BigDecimal sumDec(BigDecimal a, BigDecimal b)
    {
        return a.add(b);
    }

    public static long halfLong(double value)
    {
        return (long) (value / 2);
    }

    public static String repeat(String text, int times)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++)
        {
            sb.append(text);
        }
        return sb.toString();
    }

    public static boolean positive(int value)
    {
        return value > 0;
    }

    public static Character firstChar(String text)
    {
        return text.charAt(0);
    }

    public static double noArgs()
    {
        return 3.5;
    }

    public String nonStatic() throws NoSuchMethodException
    {
        return "";
    }

    private void addMath()
    {
        eval.addFunction("sqrt", MethodOf("sqrt", double.class));
        eval.addFunction("max", MethodOf("max", double.class, double.class));
    }

    private static Method MethodOf(String name, Class<?>... params)
    {
        try
        {
            return Math.class.getMethod(name, params);
        }
        catch (NoSuchMethodException ex)
        {
            throw new AssertionError(ex);
        }
    }

    /**
     * Evaluates the expression asserting that there is no error, and returns
     * its value.
     */
    private static Object value(Evaluator eval, String expression)
    {
        Evaluator.Result result = eval.eval(expression);
        assertEquals(Evaluator.ErrorType.NONE, result.error,
                () -> expression + " -> " + result.error + ": " + result.message);
        return result.value;
    }

    /**
     * Evaluates the expression asserting that it fails with the given error
     * type, and returns the result.
     */
    private static Evaluator.Result error(Evaluator eval, String expression, Evaluator.ErrorType type)
    {
        Evaluator.Result result = eval.eval(expression);
        assertEquals(type, result.error,
                () -> expression + " -> " + result.error + ": " + result.message);
        assertNotNull(result.message, expression);
        assertTrue(result.isError(), expression);
        assertNull(result.value, expression);
        return result;
    }

    private static void assertNumeric(String expected, Object actual)
    {
        BigDecimal e = new BigDecimal(expected);
        BigDecimal a = (actual instanceof BigInteger)
                ? new BigDecimal((BigInteger) actual)
                : (BigDecimal) actual;
        assertEquals(0, e.compareTo(a), () -> "expected " + expected + " but was " + actual);
    }

    // ------------------------------------------------------------------
    // construction
    // ------------------------------------------------------------------

    /**
     * Test of the default constructor.
     */
    @Test
    public void testDefaultConstructor()
    {
        ExpressionEvaluator instance = new ExpressionEvaluator();
        assertNumeric("0.3333333333333333", value(instance, "1 / 3"));
    }

    /**
     * Test of the constructor with decimals.
     */
    @Test
    public void testConstructorDecimals()
    {
        ExpressionEvaluator instance = new ExpressionEvaluator(4);
        assertNumeric("0.3333", value(instance, "1 / 3"));
        assertNumeric("0.3333", value(instance, "1.0 / 3"));
    }

    /**
     * Test of the constructor rejecting negative decimals.
     */
    @Test
    public void testConstructorNegativeDecimals()
    {
        assertThrows(IllegalArgumentException.class, () -> new ExpressionEvaluator(-1));
    }

    /**
     * Test that ExpressionEvaluator is usable through the Evaluator interface.
     */
    @Test
    public void testEvaluatorInterface()
    {
        Evaluator evaluator = new ExpressionEvaluator(4);
        assertNumeric("6", value(evaluator, "2 * 3"));
    }

    // ------------------------------------------------------------------
    // variables
    // ------------------------------------------------------------------

    /**
     * Test of addVariable and variable references.
     */
    @Test
    public void testAddVariable()
    {
        eval.addVariable("x", 3);
        eval.addVariable("y", 1);
        assertEquals(BigInteger.valueOf(4), value(eval, "$x + $y"));
        assertNumeric("3.5", value(eval, "$x + 0.5"));
    }

    /**
     * Test of addVariable returning this for chaining.
     */
    @Test
    public void testAddVariableChaining()
    {
        ExpressionEvaluator instance = eval.addVariable("x", 3);
        assertSame(eval, instance);
    }

    /**
     * Test that addVariable rejects null values.
     */
    @Test
    public void testAddVariableNull()
    {
        assertThrows(NullPointerException.class, () -> eval.addVariable("x", null));
        assertThrows(NullPointerException.class, () -> eval.addVariable(null, 3));
    }

    /**
     * Test of addVariable rejecting unsupported values.
     */
    @Test
    public void testAddVariableUnsupported()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.addVariable("x", new Object()));
    }

    /**
     * Test of addVariables from a map.
     */
    @Test
    public void testAddVariables()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", "text");
        eval.addVariables(map);
        assertEquals(BigInteger.valueOf(5), value(eval, "$a + 3"));
        assertEquals("text", value(eval, "$b"));
    }

    /**
     * Test of addVariables rejecting null.
     */
    @Test
    public void testAddVariablesNull()
    {
        assertThrows(NullPointerException.class, () -> eval.addVariables(null));
    }

    /**
     * Test that an unknown variable is rejected.
     */
    @Test
    public void testUnknownVariable()
    {
        Evaluator.Result result = error(eval, "$missing + 1", Evaluator.ErrorType.UNDEFINED_VARIABLE);
        assertTrue(result.message.contains("missing"), result.message);
    }

    /**
     * Test of variable normalization to integral values.
     */
    @Test
    public void testVariableNormalization()
    {
        eval.addVariable("i", 7);
        eval.addVariable("l", 7L);
        eval.addVariable("f", 7.0);
        assertEquals(BigInteger.valueOf(7), value(eval, "$i"));
        assertEquals(BigInteger.valueOf(7), value(eval, "$l"));
        assertEquals(BigDecimal.valueOf(7.0), value(eval, "$f"));
    }

    /**
     * Test of string variables.
     */
    @Test
    public void testStringVariable()
    {
        eval.addVariable("name", "nut");
        assertEquals("nut", value(eval, "$name"));
        assertEquals("nut#1", value(eval, "$name + \"#\" + 1"));
    }

    // ------------------------------------------------------------------
    // arithmetic
    // ------------------------------------------------------------------

    /**
     * Test of the addition operator.
     */
    @Test
    public void testAddition()
    {
        assertEquals(BigInteger.valueOf(30), value(eval, "10 + 20"));
        assertNumeric("30.5", value(eval, "10.5 + 20"));
        assertNumeric("31", value(eval, "10.5 + 20.5"));
    }

    /**
     * Test of the subtraction operator.
     */
    @Test
    public void testSubtraction()
    {
        assertEquals(BigInteger.valueOf(17), value(eval, "20 - 3"));
        assertEquals(BigInteger.valueOf(-7), value(eval, "3 - 10"));
        assertEquals(BigInteger.valueOf(0), value(eval, "3 - 3"));
    }

    /**
     * Test of the multiplication operator.
     */
    @Test
    public void testMultiplication()
    {
        assertEquals(BigInteger.valueOf(21), value(eval, "7 * 3"));
        assertEquals(new BigDecimal("22.5"), value(eval, "7.5 * 3"));
        assertEquals(BigInteger.valueOf(0), value(eval, "0 * 5"));
    }

    /**
     * Test of the division operator on exact results.
     */
    @Test
    public void testDivisionExact()
    {
        assertEquals(BigInteger.valueOf(5), value(eval, "10 / 2"));
        assertEquals(BigInteger.valueOf(-5), value(eval, "10 / -2"));
        assertEquals(BigInteger.valueOf(0), value(eval, "0 / 2"));
    }

    /**
     * Test of the division operator on non-exact results.
     */
    @Test
    public void testDivisionDecimal()
    {
        assertEquals(new BigDecimal("3.5"), value(eval, "7 / 2"));
        assertEquals(new BigDecimal("0.3333333333333333"), value(eval, "1 / 3"));
    }

    /**
     * Test of the remainder operator.
     */
    @Test
    public void testRemainder()
    {
        assertEquals(BigInteger.valueOf(1), value(eval, "7 % 3"));
        assertEquals(BigInteger.valueOf(0), value(eval, "6 % 3"));
        assertEquals(new BigDecimal("1.5"), value(eval, "7.5 % 2"));
    }

    /**
     * Test of division by zero reported as an error.
     */
    @Test
    public void testDivisionByZero()
    {
        error(eval, "1 / 0", Evaluator.ErrorType.DIVISION_BY_ZERO);
        error(eval, "1.0 / 0", Evaluator.ErrorType.DIVISION_BY_ZERO);
        error(eval, "1 % 0", Evaluator.ErrorType.DIVISION_BY_ZERO);
    }

    /**
     * Test of the power operator.
     */
    @Test
    public void testPower()
    {
        assertEquals(BigInteger.valueOf(1024), value(eval, "2 ^ 10"));
        assertEquals(BigInteger.valueOf(1), value(eval, "5 ^ 0"));
        assertNumeric("0.125", value(eval, "2 ^ -3"));
        assertEquals(BigInteger.valueOf(512), value(eval, "2 ^ 3 ^ 2"));
    }

    /**
     * Test of a fractional exponent using double math.
     */
    @Test
    public void testPowerFractional()
    {
        assertNumeric("5.656854249492381", value(eval, "2 ^ 2.5"));
    }

    /**
     * Test of an out-of-range exponent reported as an overflow error.
     */
    @Test
    public void testPowerExponentTooLarge()
    {
        error(eval, "2 ^ 8589934592", Evaluator.ErrorType.OVERFLOW);
        error(eval, "2 ^ 10000000000000000000", Evaluator.ErrorType.OVERFLOW);
    }

    /**
     * Test of the unary minus operator.
     */
    @Test
    public void testUnaryMinus()
    {
        assertEquals(BigInteger.valueOf(-5), value(eval, "-5"));
        assertEquals(BigInteger.valueOf(5), value(eval, "--5"));
        assertEquals(BigInteger.valueOf(7), value(eval, "-(3 - 10)"));
        assertEquals(BigInteger.valueOf(-25), value(eval, "-5 ^ 2"));
        assertEquals(BigInteger.valueOf(25), value(eval, "(-5) ^ 2"));
    }

    /**
     * Test of the unary plus operator.
     */
    @Test
    public void testUnaryPlus()
    {
        assertEquals(BigInteger.valueOf(5), value(eval, "+5"));
        assertEquals(BigInteger.valueOf(15), value(eval, "+5 + +10"));
    }

    // ------------------------------------------------------------------
    // precedence and parentheses
    // ------------------------------------------------------------------

    /**
     * Test of operator precedence between + and *.
     */
    @Test
    public void testPrecedenceMultiplicationOverAddition()
    {
        assertEquals(BigInteger.valueOf(7), value(eval, "1 + 2 * 3"));
    }

    /**
     * Test of operator precedence between + and ^.
     */
    @Test
    public void testPrecedencePowerOverAddition()
    {
        assertEquals(BigInteger.valueOf(5), value(eval, "1 + 2 ^ 2"));
        assertEquals(BigInteger.valueOf(13), value(eval, "2 ^ 2 + 3 ^ 2"));
    }

    /**
     * Test that parentheses override precedence.
     */
    @Test
    public void testParentheses()
    {
        assertEquals(BigInteger.valueOf(9), value(eval, "(1 + 2) * 3"));
        assertEquals(BigInteger.valueOf(36), value(eval, "(2 + 2 + 2) ^ 2"));
    }

    /**
     * Test of nested parentheses.
     */
    @Test
    public void testNestedParentheses()
    {
        assertEquals(BigInteger.valueOf(25), value(eval, "((1 + 2) * (3 + 2)) + 10"));
    }

    // ------------------------------------------------------------------
    // numbers
    // ------------------------------------------------------------------

    /**
     * Test of integer literals handling with BigInteger.
     */
    @Test
    public void testIntegerLiterals()
    {
        assertEquals(BigInteger.valueOf(1234567890123456789L), value(eval, "1234567890123456789"));
        assertTrue(value(eval, "42") instanceof BigInteger);
    }

    /**
     * Test of decimal literals handling with BigDecimal.
     */
    @Test
    public void testDecimalLiterals()
    {
        assertEquals(new BigDecimal("1.5"), value(eval, "1.5"));
        assertEquals(new BigDecimal("0.5"), value(eval, ".5"));
        assertTrue(value(eval, "1.5") instanceof BigDecimal);
    }

    /**
     * Test of decimal rounding to the configured decimals.
     */
    @Test
    public void testDecimalRounding()
    {
        ExpressionEvaluator instance = new ExpressionEvaluator(2);
        assertNumeric("1.13", value(instance, "0.125 + 1"));
        assertNumeric("3.33", value(instance, "10 / 3"));
        assertNumeric("0.33", value(new ExpressionEvaluator(2), "1 / 3"));
    }

    /**
     * Test of large integer arithmetic.
     */
    @Test
    public void testLargeIntegers()
    {
        String big = "123456789012345678901234567890";
        assertEquals(new BigInteger(big).multiply(BigInteger.valueOf(2)),
                value(eval, big + " + " + big));
        assertEquals(new BigInteger(big).pow(2), value(eval, big + " ^ 2"));
    }

    // ------------------------------------------------------------------
    // strings
    // ------------------------------------------------------------------

    /**
     * Test of string literals.
     */
    @Test
    public void testStringLiteral()
    {
        assertEquals("hello", value(eval, "\"hello\""));
        assertEquals("", value(eval, "\"\""));
    }

    /**
     * Test of escape sequences in string literals.
     */
    @Test
    public void testStringEscapes()
    {
        assertEquals("a\"b", value(eval, "\"a\\\"b\""));
        assertEquals("a\\b", value(eval, "\"a\\\\b\""));
        assertEquals("a\nb", value(eval, "\"a\\nb\""));
        assertEquals("a\tb", value(eval, "\"a\\tb\""));
    }

    /**
     * Test of the concatenation operator.
     */
    @Test
    public void testConcatenation()
    {
        assertEquals("ab", value(eval, "\"a\" + \"b\""));
        assertEquals("a1", value(eval, "\"a\" + 1"));
        assertEquals("1a", value(eval, "1 + \"a\""));
        assertEquals("a1b", value(eval, "\"a\" + 1 + \"b\""));
        assertEquals("33", value(eval, "1 + 2 + \"3\""));
    }

    /**
     * Test that arithmetic operators reject strings.
     */
    @Test
    public void testArithmeticOnStrings()
    {
        error(eval, "\"a\" - \"b\"", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "\"a\" * 2", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "\"a\" / 2", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "\"a\" % 2", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "\"a\" ^ 2", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "-\"a\"", Evaluator.ErrorType.UNEXPECTED_TOKEN);
    }

    // ------------------------------------------------------------------
    // functions
    // ------------------------------------------------------------------

    /**
     * Test of a single-argument function.
     */
    @Test
    public void testFunctionSingleArg() throws Exception
    {
        eval.addFunction("poyito", ExpressionEvaluatorTest.class.getMethod("poyito", double.class));
        assertEquals("poyito=2.5", value(eval, "poyito(2.5)"));
        assertEquals("poyito=5.0", value(eval, "poyito(5)"));
    }

    /**
     * Test of the sqrt function.
     */
    @Test
    public void testFunctionSqrt()
    {
        addMath();
        assertNumeric("3", value(eval, "sqrt(9)"));
        assertNumeric("2.23606797749979", value(eval, "sqrt(5)"));
        assertNumeric("2", value(eval, "sqrt(4)"));
    }

    /**
     * Test of a two-argument function.
     */
    @Test
    public void testFunctionTwoArgs()
    {
        addMath();
        assertNumeric("5", value(eval, "max(2, 5)"));
        assertNumeric("-1", value(eval, "max(-1, -5)"));
    }

    /**
     * Test of an undefined function name.
     */
    @Test
    public void testUnknownFunction()
    {
        Evaluator.Result result = error(eval, "nobody(1)", Evaluator.ErrorType.UNDEFINED_VARIABLE);
        assertTrue(result.message.contains("nobody"), result.message);
    }

    /**
     * Test of a wrong number of arguments for a function.
     */
    @Test
    public void testFunctionArgumentCount()
    {
        addMath();
        Evaluator.Result result = error(eval, "max(1)", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        assertTrue(result.message.contains("2"), result.message);
        error(eval, "max(1, 2, 3)", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "sqrt()", Evaluator.ErrorType.UNEXPECTED_TOKEN);
    }

    /**
     * Test of nested function calls.
     */
    @Test
    public void testNestedFunctions() throws Exception
    {
        addMath();
        eval.addFunction("poyito", ExpressionEvaluatorTest.class.getMethod("poyito", double.class));
        assertEquals("poyito=2.449489742783178", value(eval, "poyito(sqrt(max(6, 6)))"));
    }

    /**
     * Test of a function with an int parameter conversion.
     */
    @Test
    public void testFunctionIntParameter() throws Exception
    {
        eval.addFunction("sum", ExpressionEvaluatorTest.class.getMethod("sum", int.class, int.class));
        assertEquals(BigInteger.valueOf(7), value(eval, "sum(3, 4)"));
        assertEquals(BigInteger.valueOf(7), value(eval, "sum(3.5, 4.5)"));
    }

    /**
     * Test of a function with a BigInteger parameter.
     */
    @Test
    public void testFunctionBigIntegerParameter() throws Exception
    {
        eval.addFunction("sumBig", ExpressionEvaluatorTest.class.getMethod("sumBig", BigInteger.class, BigInteger.class));
        assertEquals(BigInteger.valueOf(7), value(eval, "sumBig(3, 4)"));
        assertEquals(BigInteger.valueOf(4), value(eval, "sumBig(2.5, 2.5)"));
    }

    /**
     * Test of a function with a BigDecimal parameter.
     */
    @Test
    public void testFunctionBigDecimalParameter() throws Exception
    {
        eval.addFunction("sumDec", ExpressionEvaluatorTest.class.getMethod("sumDec", BigDecimal.class, BigDecimal.class));
        assertNumeric("8", value(eval, "sumDec(3.5, 4.5)"));
        assertNumeric("9", value(eval, "sumDec(4, 5)"));
    }

    /**
     * Test of a function with a long parameter.
     */
    @Test
    public void testFunctionLongParameter() throws Exception
    {
        eval.addFunction("halfLong", ExpressionEvaluatorTest.class.getMethod("halfLong", double.class));
        assertEquals(BigInteger.valueOf(2), value(eval, "halfLong(5)"));
    }

    /**
     * Test of a function with a String parameter.
     */
    @Test
    public void testFunctionStringParameter() throws Exception
    {
        eval.addFunction("repeat", ExpressionEvaluatorTest.class.getMethod("repeat", String.class, int.class));
        assertEquals("ababab", value(eval, "repeat(\"ab\", 3)"));
        assertEquals("", value(eval, "repeat(\"\", 5)"));
        assertEquals("ab1", value(eval, "repeat(\"ab\", 1) + 1"));
    }

    /**
     * Test of a function returning a boolean.
     */
    @Test
    public void testFunctionBooleanReturn() throws Exception
    {
        eval.addFunction("positive", ExpressionEvaluatorTest.class.getMethod("positive", int.class));
        assertEquals("true", value(eval, "positive(5)"));
        assertEquals("false", value(eval, "positive(-5)"));
    }

    /**
     * Test of a function with a boolean parameter.
     */
    @Test
    public void testFunctionBooleanParameter() throws Exception
    {
        eval.addFunction("positive", ExpressionEvaluatorTest.class.getMethod("positive", int.class));
        assertEquals("false", value(eval, "positive(0)"));
    }

    /**
     * Test of a function returning a Character.
     */
    @Test
    public void testFunctionCharacterReturn() throws Exception
    {
        eval.addFunction("firstChar", ExpressionEvaluatorTest.class.getMethod("firstChar", String.class));
        assertEquals("h", value(eval, "firstChar(\"hello\")"));
    }

    /**
     * Test of a function with no parameters.
     */
    @Test
    public void testFunctionNoArgs() throws Exception
    {
        eval.addFunction("noArgs", ExpressionEvaluatorTest.class.getMethod("noArgs"));
        assertEquals(new BigDecimal("3.5"), value(eval, "noArgs()"));
    }

    /**
     * Test of a function adding several of the builtin math functions.
     */
    @Test
    public void testFunctionMathBuiltins()
    {
        addMath();
        assertNumeric("6", value(eval, "max(1, 2) + max(3, 4)"));
        assertEquals("3", value(eval, "sqrt(9) + \"\""));
    }

    /**
     * Test of registering a non-static method.
     */
    @Test
    public void testRegisterNonStaticMethod() throws Exception
    {
        Method m = ExpressionEvaluatorTest.class.getMethod("nonStatic");
        assertThrows(IllegalArgumentException.class, () -> eval.addFunction("nonStatic", m));
    }

    /**
     * Test of registering a null function.
     */
    @Test
    public void testRegisterNullFunction()
    {
        Method m = null;
        assertThrows(NullPointerException.class, () -> eval.addFunction("f", m));
    }

    /**
     * Test of a function registered with a DoubleUnaryOperator method reference.
     */
    @Test
    public void testFunctionDoubleUnaryOperator()
    {
        eval.addFunction("sqrt", Math::sqrt);
        eval.addFunction("abs", (DoubleUnaryOperator) Math::abs);
        assertNumeric("3", value(eval, "sqrt(9)"));
        assertNumeric("2.23606797749979", value(eval, "sqrt(5)"));
        assertNumeric("3.5", value(eval, "abs(-3.5)"));
    }

    /**
     * Test of a function registered with a DoubleBinaryOperator method reference.
     */
    @Test
    public void testFunctionDoubleBinaryOperator()
    {
        eval.addFunction("max", (DoubleBinaryOperator) Math::max);
        eval.addFunction("pow", (DoubleBinaryOperator) Math::pow);
        assertNumeric("5", value(eval, "max(2, 5)"));
        assertNumeric("1024", value(eval, "pow(2, 10)"));
        assertNumeric("1.4142135623730951", value(eval, "pow(2, 0.5)"));
    }

    /**
     * Test of a function registered with a LongUnaryOperator lambda.
     */
    @Test
    public void testFunctionLongUnaryOperator()
    {
        eval.addFunction("twice", (LongUnaryOperator) v -> v * 2);
        assertNumeric("18", value(eval, "twice(9)"));
    }

    /**
     * Test of a function registered with a LongBinaryOperator lambda.
     */
    @Test
    public void testFunctionLongBinaryOperator()
    {
        eval.addFunction("product", (LongBinaryOperator) (a, b) -> a * b);
        assertNumeric("21", value(eval, "product(3, 7)"));
    }

    /**
     * Test of a function registered with an IntUnaryOperator method reference.
     */
    @Test
    public void testFunctionIntUnaryOperator()
    {
        eval.addFunction("sign", (IntUnaryOperator) Integer::signum);
        assertNumeric("-1", value(eval, "sign(-7)"));
        assertNumeric("0", value(eval, "sign(0)"));
        assertNumeric("1", value(eval, "sign(5)"));
    }

    /**
     * Test of a function registered with an IntBinaryOperator lambda.
     */
    @Test
    public void testFunctionIntBinaryOperator()
    {
        eval.addFunction("xor", (IntBinaryOperator) (a, b) -> a ^ b);
        assertNumeric("6", value(eval, "xor(5, 3)"));
    }

    /**
     * Test of a function registered with a DoubleSupplier method reference.
     */
    @Test
    public void testFunctionDoubleSupplier()
    {
        eval.addFunction("pi", (DoubleSupplier) () -> Math.PI);
        assertNumeric("3.141592653589793", value(eval, "pi()"));
        assertNumeric("4.141592653589793", value(eval, "pi() + 1"));
    }

    /**
     * Test of a function registered with a LongSupplier lambda.
     */
    @Test
    public void testFunctionLongSupplier()
    {
        eval.addFunction("millis", (LongSupplier) () -> 123L);
        assertNumeric("123", value(eval, "millis()"));
    }

    /**
     * Test of a function registered with an IntSupplier lambda.
     */
    @Test
    public void testFunctionIntSupplier()
    {
        eval.addFunction("count", (IntSupplier) () -> 3);
        assertNumeric("9", value(eval, "count() ^ 2"));
    }

    /**
     * Test of a function registered with a Supplier.
     */
    @Test
    public void testFunctionSupplier()
    {
        eval.addFunction("answer", (Supplier<?>) () -> 42);
        assertNumeric("42", value(eval, "answer()"));
        eval.addFunction("greet", (Supplier<?>) () -> "hello");
        assertEquals("hello", value(eval, "greet()"));
    }

    /**
     * Test of a functional-interface function mixed with arithmetic.
     */
    @Test
    public void testFunctionalMixed()
    {
        eval.addFunction("sqrt", Math::sqrt);
        eval.addFunction("max", (DoubleBinaryOperator) Math::max);
        eval.addFunction("pow", (DoubleBinaryOperator) Math::pow);
        assertNumeric("6", value(eval, "sqrt(max(4, 9)) + pow(2, 1) + 1"));
        assertEquals("3", value(eval, "sqrt(9) + \"\""));
    }

    // ------------------------------------------------------------------
    // errors
    // ------------------------------------------------------------------

    /**
     * Test of a blank expression doing nothing without error.
     */
    @Test
    public void testEmptyExpression()
    {
        Evaluator.Result empty = eval.eval("");
        assertEquals(Evaluator.ErrorType.NONE, empty.error);
        assertNull(empty.value);
        assertEquals(-1, empty.position);
        assertFalse(empty.isError());
        assertNull(eval.eval("   ").value);
        assertThrows(NullPointerException.class, () -> eval.eval(null));
    }

    /**
     * Test of a malformed number being reported.
     */
    @Test
    public void testMalformedNumber()
    {
        error(eval, "1.5.2", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "123.", Evaluator.ErrorType.UNEXPECTED_TOKEN);
    }

    /**
     * Test of an unexpected character being reported.
     */
    @Test
    public void testUnexpectedCharacter()
    {
        error(eval, "1 ~ 2", Evaluator.ErrorType.INVALID_CHARACTER);
    }

    /**
     * Test of an unterminated string being reported.
     */
    @Test
    public void testUnterminatedString()
    {
        error(eval, "\"abc", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(eval, "\"abc\\\"", Evaluator.ErrorType.UNEXPECTED_TOKEN);
    }

    /**
     * Test of a malformed variable reference.
     */
    @Test
    public void testMalformedVariable()
    {
        error(eval, "$", Evaluator.ErrorType.UNEXPECTED_TOKEN);
    }

    /**
     * Test of an incomplete expression being reported.
     */
    @Test
    public void testIncompleteExpression()
    {
        error(eval, "1 +", Evaluator.ErrorType.INCOMPLETE_EXPRESSION);
        error(eval, "(1 + 2", Evaluator.ErrorType.UNCLOSED_PARENTHESIS);
        error(eval, "1 + 2)", Evaluator.ErrorType.UNEXPECTED_CLOSING_PARENTHESIS);
    }
}
