/*
 *  ExpEvalTest.java
 *
 *  Copyright (C) 2026 francitoshi@gmail.com
 *  SPDX-License-Identifier: GPL-3.0-or-later
 *  See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

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

public class ExpEvalTest
{
    private ExpEval eval;

    @BeforeEach
    public void setUp()
    {
        eval = new ExpEval();
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

    private static java.lang.reflect.Method MethodOf(String name, Class<?>... params)
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
        ExpEval instance = new ExpEval();
        Object result = instance.eval("1 / 3");
        assertEquals(new BigDecimal("0.3333333333333333"), result);
    }

    /**
     * Test of the constructor with decimals.
     */
    @Test
    public void testConstructorDecimals()
    {
        ExpEval instance = new ExpEval(4);
        assertEquals(new BigDecimal("0.3333"), instance.eval("1 / 3"));
        assertEquals(new BigDecimal("0.3333"), instance.eval("1.0 / 3"));
    }

    /**
     * Test of the constructor rejecting negative decimals.
     */
    @Test
    public void testConstructorNegativeDecimals()
    {
        assertThrows(IllegalArgumentException.class, () -> new ExpEval(-1));
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
        assertEquals(BigInteger.valueOf(4), eval.eval("$x + $y"));
        assertNumeric("3.5", eval.eval("$x + 0.5"));
    }

    /**
     * Test of addVariable returning this for chaining.
     */
    @Test
    public void testAddVariableChaining()
    {
        ExpEval instance = eval.addVariable("x", 3);
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
        assertEquals(BigInteger.valueOf(5), eval.eval("$a + 3"));
        assertEquals("text", eval.eval("$b"));
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
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> eval.eval("$missing + 1"));
        assertTrue(ex.getMessage().contains("missing"));
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
        assertEquals(BigInteger.valueOf(7), eval.eval("$i"));
        assertEquals(BigInteger.valueOf(7), eval.eval("$l"));
        assertEquals(BigDecimal.valueOf(7.0), eval.eval("$f"));
    }

    /**
     * Test of string variables.
     */
    @Test
    public void testStringVariable()
    {
        eval.addVariable("name", "nut");
        assertEquals("nut", eval.eval("$name"));
        assertEquals("nut#1", eval.eval("$name + \"#\" + 1"));
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
        assertEquals(BigInteger.valueOf(30), eval.eval("10 + 20"));
        assertNumeric("30.5", eval.eval("10.5 + 20"));
        assertNumeric("31", eval.eval("10.5 + 20.5"));
    }

    /**
     * Test of the subtraction operator.
     */
    @Test
    public void testSubtraction()
    {
        assertEquals(BigInteger.valueOf(17), eval.eval("20 - 3"));
        assertEquals(BigInteger.valueOf(-7), eval.eval("3 - 10"));
        assertEquals(BigInteger.valueOf(0), eval.eval("3 - 3"));
    }

    /**
     * Test of the multiplication operator.
     */
    @Test
    public void testMultiplication()
    {
        assertEquals(BigInteger.valueOf(21), eval.eval("7 * 3"));
        assertEquals(new BigDecimal("22.5"), eval.eval("7.5 * 3"));
        assertEquals(BigInteger.valueOf(0), eval.eval("0 * 5"));
    }

    /**
     * Test of the division operator on exact results.
     */
    @Test
    public void testDivisionExact()
    {
        assertEquals(BigInteger.valueOf(5), eval.eval("10 / 2"));
        assertEquals(BigInteger.valueOf(-5), eval.eval("10 / -2"));
        assertEquals(BigInteger.valueOf(0), eval.eval("0 / 2"));
    }

    /**
     * Test of the division operator on non-exact results.
     */
    @Test
    public void testDivisionDecimal()
    {
        assertEquals(new BigDecimal("3.5"), eval.eval("7 / 2"));
        assertEquals(new BigDecimal("0.3333333333333333"), eval.eval("1 / 3"));
    }

    /**
     * Test of the remainder operator.
     */
    @Test
    public void testRemainder()
    {
        assertEquals(BigInteger.valueOf(1), eval.eval("7 % 3"));
        assertEquals(BigInteger.valueOf(0), eval.eval("6 % 3"));
        assertEquals(new BigDecimal("1.5"), eval.eval("7.5 % 2"));
    }

    /**
     * Test of division by zero throwing an ArithmeticException.
     */
    @Test
    public void testDivisionByZero()
    {
        assertThrows(ArithmeticException.class, () -> eval.eval("1 / 0"));
        assertThrows(ArithmeticException.class, () -> eval.eval("1.0 / 0"));
        assertThrows(ArithmeticException.class, () -> eval.eval("1 % 0"));
    }

    /**
     * Test of the power operator.
     */
    @Test
    public void testPower()
    {
        assertEquals(BigInteger.valueOf(1024), eval.eval("2 ^ 10"));
        assertEquals(BigInteger.valueOf(1), eval.eval("5 ^ 0"));
        assertNumeric("0.125", eval.eval("2 ^ -3"));
        assertEquals(BigInteger.valueOf(512), eval.eval("2 ^ 3 ^ 2"));
    }

    /**
     * Test of a fractional exponent using double math.
     */
    @Test
    public void testPowerFractional()
    {
        assertNumeric("5.656854249492381", eval.eval("2 ^ 2.5"));
    }

    /**
     * Test of an out-of-range exponent throwing an ArithmeticException.
     */
    @Test
    public void testPowerExponentTooLarge()
    {
        assertThrows(ArithmeticException.class, () -> eval.eval("2 ^ 8589934592"));
        assertThrows(ArithmeticException.class, () -> eval.eval("2 ^ 10000000000000000000"));
    }

    /**
     * Test of the unary minus operator.
     */
    @Test
    public void testUnaryMinus()
    {
        assertEquals(BigInteger.valueOf(-5), eval.eval("-5"));
        assertEquals(BigInteger.valueOf(5), eval.eval("--5"));
        assertEquals(BigInteger.valueOf(7), eval.eval("-(3 - 10)"));
        assertEquals(BigInteger.valueOf(-25), eval.eval("-5 ^ 2"));
        assertEquals(BigInteger.valueOf(25), eval.eval("(-5) ^ 2"));
    }

    /**
     * Test of the unary plus operator.
     */
    @Test
    public void testUnaryPlus()
    {
        assertEquals(BigInteger.valueOf(5), eval.eval("+5"));
        assertEquals(BigInteger.valueOf(15), eval.eval("+5 + +10"));
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
        assertEquals(BigInteger.valueOf(7), eval.eval("1 + 2 * 3"));
    }

    /**
     * Test of operator precedence between + and ^.
     */
    @Test
    public void testPrecedencePowerOverAddition()
    {
        assertEquals(BigInteger.valueOf(5), eval.eval("1 + 2 ^ 2"));
        assertEquals(BigInteger.valueOf(13), eval.eval("2 ^ 2 + 3 ^ 2"));
    }

    /**
     * Test that parentheses override precedence.
     */
    @Test
    public void testParentheses()
    {
        assertEquals(BigInteger.valueOf(9), eval.eval("(1 + 2) * 3"));
        assertEquals(BigInteger.valueOf(36), eval.eval("(2 + 2 + 2) ^ 2"));
    }

    /**
     * Test of nested parentheses.
     */
    @Test
    public void testNestedParentheses()
    {
        assertEquals(BigInteger.valueOf(25), eval.eval("((1 + 2) * (3 + 2)) + 10"));
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
        assertEquals(BigInteger.valueOf(1234567890123456789L), eval.eval("1234567890123456789"));
        assertTrue(eval.eval("42") instanceof BigInteger);
    }

    /**
     * Test of decimal literals handling with BigDecimal.
     */
    @Test
    public void testDecimalLiterals()
    {
        assertEquals(new BigDecimal("1.5"), eval.eval("1.5"));
        assertEquals(new BigDecimal("0.5"), eval.eval(".5"));
        assertTrue(eval.eval("1.5") instanceof BigDecimal);
    }

    /**
     * Test of decimal rounding to the configured decimals.
     */
    @Test
    public void testDecimalRounding()
    {
        ExpEval instance = new ExpEval(2);
        assertNumeric("1.13", instance.eval("0.125 + 1"));
        assertNumeric("3.33", instance.eval("10 / 3"));
        assertNumeric("0.33", new ExpEval(2).eval("1 / 3"));
    }

    /**
     * Test of large integer arithmetic.
     */
    @Test
    public void testLargeIntegers()
    {
        String big = "123456789012345678901234567890";
        assertEquals(new BigInteger(big).multiply(BigInteger.valueOf(2)),
                eval.eval(big + " + " + big));
        assertEquals(new BigInteger(big).pow(2), eval.eval(big + " ^ 2"));
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
        assertEquals("hello", eval.eval("\"hello\""));
        assertEquals("", eval.eval("\"\""));
    }

    /**
     * Test of escape sequences in string literals.
     */
    @Test
    public void testStringEscapes()
    {
        assertEquals("a\"b", eval.eval("\"a\\\"b\""));
        assertEquals("a\\b", eval.eval("\"a\\\\b\""));
        assertEquals("a\nb", eval.eval("\"a\\nb\""));
        assertEquals("a\tb", eval.eval("\"a\\tb\""));
    }

    /**
     * Test of the concatenation operator.
     */
    @Test
    public void testConcatenation()
    {
        assertEquals("ab", eval.eval("\"a\" + \"b\""));
        assertEquals("a1", eval.eval("\"a\" + 1"));
        assertEquals("1a", eval.eval("1 + \"a\""));
        assertEquals("a1b", eval.eval("\"a\" + 1 + \"b\""));
        assertEquals("33", eval.eval("1 + 2 + \"3\""));
    }

    /**
     * Test that arithmetic operators reject strings.
     */
    @Test
    public void testArithmeticOnStrings()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.eval("\"a\" - \"b\""));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("\"a\" * 2"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("\"a\" / 2"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("\"a\" % 2"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("\"a\" ^ 2"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("-\"a\""));
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
        eval.addFunction("poyito", ExpEvalTest.class.getMethod("poyito", double.class));
        assertEquals("poyito=2.5", eval.eval("poyito(2.5)"));
        assertEquals("poyito=5.0", eval.eval("poyito(5)"));
    }

    /**
     * Test of the sqrt function.
     */
    @Test
    public void testFunctionSqrt()
    {
        addMath();
        assertNumeric("3", eval.eval("sqrt(9)"));
        assertNumeric("2.23606797749979", eval.eval("sqrt(5)"));
        assertNumeric("2", eval.eval("sqrt(4)"));
    }

    /**
     * Test of a two-argument function.
     */
    @Test
    public void testFunctionTwoArgs()
    {
        addMath();
        assertNumeric("5", eval.eval("max(2, 5)"));
        assertNumeric("-1", eval.eval("max(-1, -5)"));
    }

    /**
     * Test of an undefined function name.
     */
    @Test
    public void testUnknownFunction()
    {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> eval.eval("nobody(1)"));
        assertTrue(ex.getMessage().contains("nobody"));
    }

    /**
     * Test of a wrong number of arguments for a function.
     */
    @Test
    public void testFunctionArgumentCount()
    {
        addMath();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> eval.eval("max(1)"));
        assertTrue(ex.getMessage().contains("2"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("max(1, 2, 3)"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("sqrt()"));
    }

    /**
     * Test of nested function calls.
     */
    @Test
    public void testNestedFunctions() throws Exception
    {
        addMath();
        eval.addFunction("poyito", ExpEvalTest.class.getMethod("poyito", double.class));
        assertEquals("poyito=2.449489742783178", eval.eval("poyito(sqrt(max(6, 6)))"));
    }

    /**
     * Test of a function with an int parameter conversion.
     */
    @Test
    public void testFunctionIntParameter() throws Exception
    {
        eval.addFunction("sum", ExpEvalTest.class.getMethod("sum", int.class, int.class));
        assertEquals(BigInteger.valueOf(7), eval.eval("sum(3, 4)"));
        assertEquals(BigInteger.valueOf(7), eval.eval("sum(3.5, 4.5)"));
    }

    /**
     * Test of a function with a BigInteger parameter.
     */
    @Test
    public void testFunctionBigIntegerParameter() throws Exception
    {
        eval.addFunction("sumBig", ExpEvalTest.class.getMethod("sumBig", BigInteger.class, BigInteger.class));
        assertEquals(BigInteger.valueOf(7), eval.eval("sumBig(3, 4)"));
        assertEquals(BigInteger.valueOf(4), eval.eval("sumBig(2.5, 2.5)"));
    }

    /**
     * Test of a function with a BigDecimal parameter.
     */
    @Test
    public void testFunctionBigDecimalParameter() throws Exception
    {
        eval.addFunction("sumDec", ExpEvalTest.class.getMethod("sumDec", BigDecimal.class, BigDecimal.class));
        assertNumeric("8", eval.eval("sumDec(3.5, 4.5)"));
        assertNumeric("9", eval.eval("sumDec(4, 5)"));
    }

    /**
     * Test of a function with a long parameter.
     */
    @Test
    public void testFunctionLongParameter() throws Exception
    {
        eval.addFunction("halfLong", ExpEvalTest.class.getMethod("halfLong", double.class));
        assertEquals(BigInteger.valueOf(2), eval.eval("halfLong(5)"));
    }

    /**
     * Test of a function with a String parameter.
     */
    @Test
    public void testFunctionStringParameter() throws Exception
    {
        eval.addFunction("repeat", ExpEvalTest.class.getMethod("repeat", String.class, int.class));
        assertEquals("ababab", eval.eval("repeat(\"ab\", 3)"));
        assertEquals("", eval.eval("repeat(\"\", 5)"));
        assertEquals("ab1", eval.eval("repeat(\"ab\", 1) + 1"));
    }

    /**
     * Test of a function returning a boolean.
     */
    @Test
    public void testFunctionBooleanReturn() throws Exception
    {
        eval.addFunction("positive", ExpEvalTest.class.getMethod("positive", int.class));
        assertEquals("true", eval.eval("positive(5)"));
        assertEquals("false", eval.eval("positive(-5)"));
    }

    /**
     * Test of a function with a boolean parameter.
     */
    @Test
    public void testFunctionBooleanParameter() throws Exception
    {
        eval.addFunction("positive", ExpEvalTest.class.getMethod("positive", int.class));
        assertEquals("false", eval.eval("positive(0)"));
    }

    /**
     * Test of a function returning a Character.
     */
    @Test
    public void testFunctionCharacterReturn() throws Exception
    {
        eval.addFunction("firstChar", ExpEvalTest.class.getMethod("firstChar", String.class));
        assertEquals("h", eval.eval("firstChar(\"hello\")"));
    }

    /**
     * Test of a function with no parameters.
     */
    @Test
    public void testFunctionNoArgs() throws Exception
    {
        eval.addFunction("noArgs", ExpEvalTest.class.getMethod("noArgs"));
        assertEquals(new BigDecimal("3.5"), eval.eval("noArgs()"));
    }

    /**
     * Test of a function adding several of the builtin math functions.
     */
    @Test
    public void testFunctionMathBuiltins()
    {
        addMath();
        assertNumeric("6", eval.eval("max(1, 2) + max(3, 4)"));
        assertEquals("3", eval.eval("sqrt(9) + \"\""));
    }

    /**
     * Test of registering a non-static method.
     */
    @Test
    public void testRegisterNonStaticMethod() throws Exception
    {
        java.lang.reflect.Method m = ExpEvalTest.class.getMethod("nonStatic");
        assertThrows(IllegalArgumentException.class, () -> eval.addFunction("nonStatic", m));
    }

    /**
     * Test of registering a null function.
     */
    @Test
    public void testRegisterNullFunction()
    {
        java.lang.reflect.Method m = null;
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
        assertNumeric("3", eval.eval("sqrt(9)"));
        assertNumeric("2.23606797749979", eval.eval("sqrt(5)"));
        assertNumeric("3.5", eval.eval("abs(-3.5)"));
    }

    /**
     * Test of a function registered with a DoubleBinaryOperator method reference.
     */
    @Test
    public void testFunctionDoubleBinaryOperator()
    {
        eval.addFunction("max", (DoubleBinaryOperator) Math::max);
        eval.addFunction("pow", (DoubleBinaryOperator) Math::pow);
        assertNumeric("5", eval.eval("max(2, 5)"));
        assertNumeric("1024", eval.eval("pow(2, 10)"));
        assertNumeric("1.4142135623730951", eval.eval("pow(2, 0.5)"));
    }

    /**
     * Test of a function registered with a LongUnaryOperator lambda.
     */
    @Test
    public void testFunctionLongUnaryOperator()
    {
        eval.addFunction("twice", (LongUnaryOperator) v -> v * 2);
        assertNumeric("18", eval.eval("twice(9)"));
    }

    /**
     * Test of a function registered with a LongBinaryOperator lambda.
     */
    @Test
    public void testFunctionLongBinaryOperator()
    {
        eval.addFunction("product", (LongBinaryOperator) (a, b) -> a * b);
        assertNumeric("21", eval.eval("product(3, 7)"));
    }

    /**
     * Test of a function registered with an IntUnaryOperator method reference.
     */
    @Test
    public void testFunctionIntUnaryOperator()
    {
        eval.addFunction("sign", (IntUnaryOperator) Integer::signum);
        assertNumeric("-1", eval.eval("sign(-7)"));
        assertNumeric("0", eval.eval("sign(0)"));
        assertNumeric("1", eval.eval("sign(5)"));
    }

    /**
     * Test of a function registered with an IntBinaryOperator lambda.
     */
    @Test
    public void testFunctionIntBinaryOperator()
    {
        eval.addFunction("xor", (IntBinaryOperator) (a, b) -> a ^ b);
        assertNumeric("6", eval.eval("xor(5, 3)"));
    }

    /**
     * Test of a function registered with a DoubleSupplier method reference.
     */
    @Test
    public void testFunctionDoubleSupplier()
    {
        eval.addFunction("pi", (DoubleSupplier) () -> Math.PI);
        assertNumeric("3.141592653589793", eval.eval("pi()"));
        assertNumeric("4.141592653589793", eval.eval("pi() + 1"));
    }

    /**
     * Test of a function registered with a LongSupplier lambda.
     */
    @Test
    public void testFunctionLongSupplier()
    {
        eval.addFunction("millis", (LongSupplier) () -> 123L);
        assertNumeric("123", eval.eval("millis()"));
    }

    /**
     * Test of a function registered with an IntSupplier lambda.
     */
    @Test
    public void testFunctionIntSupplier()
    {
        eval.addFunction("count", (IntSupplier) () -> 3);
        assertNumeric("9", eval.eval("count() ^ 2"));
    }

    /**
     * Test of a function registered with a Supplier.
     */
    @Test
    public void testFunctionSupplier()
    {
        eval.addFunction("answer", (Supplier<?>) () -> 42);
        assertNumeric("42", eval.eval("answer()"));
        eval.addFunction("greet", (Supplier<?>) () -> "hello");
        assertEquals("hello", eval.eval("greet()"));
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
        assertNumeric("6", eval.eval("sqrt(max(4, 9)) + pow(2, 1) + 1"));
        assertEquals("3", eval.eval("sqrt(9) + \"\""));
    }

    // ------------------------------------------------------------------
    // errors
    // ------------------------------------------------------------------

    /**
     * Test of an empty expression being rejected.
     */
    @Test
    public void testEmptyExpression()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.eval(""));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("   "));
        assertThrows(NullPointerException.class, () -> eval.eval(null));
    }

    /**
     * Test of a malformed number being rejected.
     */
    @Test
    public void testMalformedNumber()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.eval("1.5.2"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("123."));
    }

    /**
     * Test of an unexpected character being rejected.
     */
    @Test
    public void testUnexpectedCharacter()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.eval("1 ~ 2"));
    }

    /**
     * Test of an unterminated string being rejected.
     */
    @Test
    public void testUnterminatedString()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.eval("\"abc"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("\"abc\\\""));
    }

    /**
     * Test of a malformed variable reference.
     */
    @Test
    public void testMalformedVariable()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.eval("$"));
    }

    /**
     * Test of an incomplete expression being rejected.
     */
    @Test
    public void testIncompleteExpression()
    {
        assertThrows(IllegalArgumentException.class, () -> eval.eval("1 +"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("(1 + 2"));
        assertThrows(IllegalArgumentException.class, () -> eval.eval("1 + 2)"));
    }
}