/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.math;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MathEvaluator}.
 *
 * @author franci
 */
public class MathEvaluatorTest
{
    /**
     * A static method with two BigDecimal parameters used to test addFunction.
     */
    public static BigDecimal sumDec(BigDecimal a, BigDecimal b)
    {
        return a.add(b);
    }

    /**
     * An instance method used to test that addFunction rejects it.
     */
    public void nonStatic()
    {
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Evaluates the expression asserting that there is no error, and returns
     * its value.
     */
    private static BigDecimal value(Evaluator eval, String expression)
    {
        Evaluator.Result result = eval.eval(expression);
        assertEquals(Evaluator.ErrorType.NONE, result.error,
                () -> expression + " -> " + result.error + ": " + result.message);
        return (BigDecimal) result.value;
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

    private static void assertNumeric(String expected, BigDecimal actual)
    {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
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
        MathEvaluator instance = new MathEvaluator();
        assertNumeric("0.3333333333333333", value(instance, "1 / 3"));
        assertNull(instance.getVariable("x"));
    }

    /**
     * Test of the constructor with decimals.
     */
    @Test
    public void testConstructorDecimals()
    {
        MathEvaluator instance = new MathEvaluator(4, false);
        assertNumeric("0.3333", value(instance, "1 / 3"));
        assertNumeric("0.3333", value(instance, "1.0 / 3"));
    }

    /**
     * Test of the constructor rejecting negative decimals.
     */
    @Test
    public void testConstructorNegativeDecimals()
    {
        assertThrows(IllegalArgumentException.class, () -> new MathEvaluator(-1, false));
    }

    /**
     * Test that MathEvaluator is usable through the Evaluator interface.
     */
    @Test
    public void testEvaluatorInterface()
    {
        Evaluator evaluator = new MathEvaluator(4, false);
        assertNumeric("6", value(evaluator, "2 * 3"));
    }

    // ------------------------------------------------------------------
    // arithmetic
    // ------------------------------------------------------------------

    /**
     * Test of basic arithmetic operations.
     */
    @Test
    public void testArithmetic()
    {
        MathEvaluator instance = new MathEvaluator();
        assertNumeric("7", value(instance, "3 + 4"));
        assertNumeric("-1", value(instance, "3 - 4"));
        assertNumeric("12", value(instance, "3 * 4"));
        assertNumeric("3.5", value(instance, "7 / 2"));
        assertNumeric("1", value(instance, "7 % 2"));
        assertNumeric("8", value(instance, "2 ^ 3"));
        assertNumeric("11", value(instance, "1 + 2 * 5"));
        assertNumeric("15", value(instance, "(1 + 2) * 5"));
        assertNumeric("-8", value(instance, "-2 ^ 3"));
        assertNumeric("-6", value(instance, "-(2 + 4)"));
        assertNumeric("5", value(instance, "+5"));
    }

    /**
     * Test of a table of arithmetic expressions with their expected results.
     */
    @Test
    public void testArithmeticTable()
    {
        String[][] table = {
            { "2+3", "5" },                                 // basic addition
            { "2+3*4", "14" },                              // * before +
            { "2*5+6", "16" },                              // precedence in the other order
            { "(2+3)*4", "20" },                            // parentheses
            { "2*(3+4)*5", "70" },                          // parentheses in the middle
            { "10-4-3", "3" },                              // - left-associative
            { "100/10/5", "2" },                            // / left-associative
            { "7/2", "3.5" },                               // non-integer division
            { "2^3", "8" },                                 // power
            { "2^3^2", "512" },                             // ^ right-associative (2^9, not 8^2)
            { "2*3^2", "18" },                              // ^ before *
            { "2^0.5", "1.4142135623730951" },              // decimal exponent
            { "2^-1", "0.5" },                              // unary in the exponent
            { "((2))", "2" },                               // nested parentheses
            { "2 + 3", "5" },                               // spaces ignored
        };
        MathEvaluator instance = new MathEvaluator();
        for (String[] row : table)
        {
            assertNumeric(row[1], value(instance, row[0]));
        }
    }

    /**
     * Test of a table of unary operator expressions with their expected results.
     */
    @Test
    public void testUnaryTable()
    {
        String[][] table = {
            { "-5+3", "-2" },                               // unary at the start
            { "3*-2", "-6" },                               // unary after an operator
            { "--3", "3" },                                 // double negation
            { "-(2+3)", "-5" },                             // unary over parentheses
        };
        MathEvaluator instance = new MathEvaluator();
        for (String[] row : table)
        {
            assertNumeric(row[1], value(instance, row[0]));
        }
    }

    /**
     * Test of a table of big number expressions with their expected results.
     */
    @Test
    public void testBigNumbers()
    {
        String[][] table = {
            { "6^24", "4738381338321616896" },              // exact big integers (big.Int)
            { "2*5+6^24", "4738381338321616906" },          // big integers with addition
        };
        MathEvaluator instance = new MathEvaluator();
        for (String[] row : table)
        {
            assertNumeric(row[1], value(instance, row[0]));
        }
        // unlike float64, which loses the last digits (4.738381338321617e18)
        BigDecimal result = value(instance, "2*5+6^24");
        assertEquals(0, new BigDecimal("4738381338321616906").compareTo(result),
                () -> "precision lost: " + result);
        assertEquals(4.738381338321617e18, result.doubleValue());
    }

    /**
     * Test of a table of implicit multiplication expressions with their
     * expected results, using x = 4.
     */
    @Test
    public void testImplicitMultiplicationTable()
    {
        String[][] table = {
            { "2x", "8" },                                  // 2*x
            { "1/2 x", "2" },                               // (1/2)*x
            { "1/2x", "0.125" },                            // 1/(2*x): glued binds tighter than /
            { "1 / 2x", "0.125" },                          // 1/(2*x): spaces do not matter
            { "1 / 2 x", "2" },                             // (1/2)*x: spaced binds looser than /
            { "2x^2", "32" },                               // 2*(x^2)
            { "2^3x", "32" },                               // (2^3)*x
            { "2(3+4)", "14" },                             // 2*(3+4)
            { "(1+2)(3+4)", "21" },                         // (1+2)*(3+4)
            { "2 (3+4)", "14" },                            // 2*(3+4)
            { "2x/3x", "0.6666666666666667" },              // (2x)/(3x)
            { "2 -3", "-1" },                               // 2-3: unary is subtraction
            { "-2x", "-8" },                                // -(2*x)
        };
        MathEvaluator instance = new MathEvaluator();
        instance.addVariable("x", new BigDecimal("4"));
        for (String[] row : table)
        {
            assertNumeric(row[1], value(instance, row[0]));
        }
    }

    // ------------------------------------------------------------------
    // errors
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // functions
    // ------------------------------------------------------------------

    /**
     * Test of the built-in functions.
     */
    @Test
    public void testBuiltInFunctions()
    {
        MathEvaluator instance = new MathEvaluator();
        assertNumeric("2", value(instance, "sqrt(4)"));
        assertNumeric("1.414213562373095", value(instance, "sqrt(2)"));
        assertNumeric("7", value(instance, "max(3, 7)"));
        assertNumeric("3", value(instance, "min(3, 7)"));
        assertNumeric("5", value(instance, "abs(-5)"));
        assertNumeric("1", value(instance, "cos(0)"));
        assertNumeric("0", value(instance, "sin(0)"));
        assertNumeric("0", value(instance, "logE(1)"));
        assertNumeric("2", value(instance, "log10(100)"));
        assertNumeric("3", value(instance, "log10(1000)"));
        // functions compose with implicit multiplication and variables
        instance.addVariable("x", new BigDecimal("4"));
        assertNumeric("4", value(instance, "2sqrt(4)"));
        assertNumeric("8", value(instance, "max(2x, 3)"));
        assertNumeric("3", value(instance, "min(abs(-3), 2x)"));
        assertEquals("sqrt(4)", instance.eval("sqrt(4)").interpretation);
        assertEquals("max((2 * x), 3)", instance.eval("max(2x, 3)").interpretation);
    }

    /**
     * Test of registering a function through a static Method.
     */
    @Test
    public void testAddFunctionMethod() throws Exception
    {
        MathEvaluator instance = new MathEvaluator();
        MathEvaluator returned = instance.addFunction("sum",
                MathEvaluatorTest.class.getMethod("sumDec", BigDecimal.class, BigDecimal.class));
        assertSame(instance, returned);
        assertNumeric("7", value(instance, "sum(3, 4)"));
    }

    /**
     * Test of registering a function through a functional interface.
     */
    @Test
    public void testAddFunctionOperator()
    {
        MathEvaluator instance = new MathEvaluator();
        instance.addFunction("cube", (DoubleUnaryOperator) x -> x * x * x);
        assertNumeric("27", value(instance, "cube(3)"));
        instance.addVariable("x", new BigDecimal("2"));
        assertNumeric("64", value(instance, "cube(2x)"));
    }

    /**
     * Test of addFunction rejecting invalid methods.
     */
    @Test
    public void testAddFunctionValidation() throws Exception
    {
        MathEvaluator instance = new MathEvaluator();
        assertThrows(NullPointerException.class, () -> instance.addFunction(null, BigDecimal.class.getDeclaredMethod("abs")));
        assertThrows(NullPointerException.class, () -> instance.addFunction("f", (Method) null));
        assertThrows(IllegalArgumentException.class, () -> instance.addFunction("abs2",
                MathEvaluatorTest.class.getMethod("nonStatic")));
    }

    /**
     * Test of function related errors.
     */
    @Test
    public void testFunctionErrors()
    {
        MathEvaluator instance = new MathEvaluator();
        Evaluator.Result unknown = error(instance, "foo(3)", Evaluator.ErrorType.UNDEFINED_VARIABLE);
        assertTrue(unknown.message.contains("foo"), unknown.message);
        error(instance, "max(1)", Evaluator.ErrorType.UNEXPECTED_TOKEN);
        error(instance, "sqrt(-1)", Evaluator.ErrorType.OVERFLOW);
        error(instance, "sqrt(1 2)", Evaluator.ErrorType.UNCLOSED_PARENTHESIS);
        // a function name without parentheses is not a variable
        error(instance, "2sqrt", Evaluator.ErrorType.UNDEFINED_VARIABLE);
        error(instance, "sqrt", Evaluator.ErrorType.UNDEFINED_VARIABLE);
    }

    /**
     * Test of division by zero.
     */
    @Test
    public void testDivisionByZero()
    {
        MathEvaluator instance = new MathEvaluator();
        error(instance, "1 / 0", Evaluator.ErrorType.DIVISION_BY_ZERO);
        error(instance, "1 % 0", Evaluator.ErrorType.DIVISION_BY_ZERO);
    }

    /**
     * Test of malformed expressions.
     */
    @Test
    public void testMalformedExpressions()
    {
        MathEvaluator instance = new MathEvaluator();
        assertThrows(NullPointerException.class, () -> instance.eval(null));
        error(instance, "1 +", Evaluator.ErrorType.INCOMPLETE_EXPRESSION);
        error(instance, "(1 + 2", Evaluator.ErrorType.UNCLOSED_PARENTHESIS);
        error(instance, "1 @ 2", Evaluator.ErrorType.INVALID_CHARACTER);
        error(instance, "1 = 2", Evaluator.ErrorType.INVALID_ASSIGNMENT);
    }

    /**
     * Test of a table of invalid expressions, each of them reported with a
     * descriptive error instead of breaking the evaluation.
     */
    @Test
    public void testErrorTable()
    {
        MathEvaluator instance = new MathEvaluator(16, true);

        Evaluator.Result division = error(instance, "1/0", Evaluator.ErrorType.DIVISION_BY_ZERO);
        assertTrue(division.message.contains("division by zero"), division.message);
        assertEquals(1, division.position);

        Object[][] table = {
            { "2+", Evaluator.ErrorType.INCOMPLETE_EXPRESSION, 2 },
            { "(2+3", Evaluator.ErrorType.UNCLOSED_PARENTHESIS, 4 },
            { "2+3)", Evaluator.ErrorType.UNEXPECTED_CLOSING_PARENTHESIS, 3 },
            { "z+1", Evaluator.ErrorType.UNDEFINED_VARIABLE, 0 },
            { "2 3", Evaluator.ErrorType.UNEXPECTED_TOKEN, 2 },
            { "2$3", Evaluator.ErrorType.INVALID_CHARACTER, 1 },
            { "3=4", Evaluator.ErrorType.INVALID_ASSIGNMENT, 1 },
            { "x=", Evaluator.ErrorType.INCOMPLETE_EXPRESSION, 2 },
        };
        for (Object[] row : table)
        {
            Evaluator.Result result = error(instance, (String) row[0], (Evaluator.ErrorType) row[1]);
            assertEquals(row[2], result.position, () -> row[0] + " position");
        }

        // an empty line does nothing and reports no error
        Evaluator.Result empty = instance.eval("");
        assertEquals(Evaluator.ErrorType.NONE, empty.error);
        assertNull(empty.value);
        assertNull(empty.interpretation);
        assertNull(empty.modifiedVariable);
        assertNull(empty.message);
        assertEquals(-1, empty.position);
        assertNull(instance.eval("   ").value);
        assertFalse(instance.eval("   ").isError());
    }

    // ------------------------------------------------------------------
    // result fields
    // ------------------------------------------------------------------

    /**
     * Test of the modified variable and the dependencies reported in the
     * result.
     */
    @Test
    public void testResultVariables()
    {
        MathEvaluator instance = new MathEvaluator(16, true);
        instance.addVariable("x", new BigDecimal("4"));

        Evaluator.Result assignment = instance.eval("y = x + 1");
        assertEquals("y", assignment.modifiedVariable);
        assertEquals(Collections.singleton("x"), assignment.dependencies);

        Evaluator.Result plain = instance.eval("x * 2");
        assertNull(plain.modifiedVariable);
        assertEquals(Collections.singleton("x"), plain.dependencies);

        Evaluator.Result constant = instance.eval("2 + 3");
        assertNull(constant.modifiedVariable);
        assertNull(constant.dependencies);

        Evaluator.Result self = instance.eval("x = x + 1");
        assertEquals("x", self.modifiedVariable);
        assertEquals(Collections.singleton("x"), self.dependencies);

        Evaluator.Result failed = error(instance, "z + 1", Evaluator.ErrorType.UNDEFINED_VARIABLE);
        assertNull(failed.modifiedVariable);
        assertNull(failed.dependencies);
    }

    /**
     * Test of the interpretation and the modified variable of the result.
     */
    @Test
    public void testResultFields()
    {
        MathEvaluator instance = new MathEvaluator(16, true);
        Evaluator.Result assignment = instance.eval("x=35*8");
        assertEquals(Evaluator.ErrorType.NONE, assignment.error);
        assertNumeric("280", (BigDecimal) assignment.value);
        assertEquals("x", assignment.modifiedVariable);
        assertEquals("x = 35 * 8", assignment.interpretation);
        assertNull(assignment.message);
        assertEquals(-1, assignment.position);

        instance.addVariable("x", new BigDecimal("4"));
        assertEquals("1 / (2 * x)", instance.eval("1/2x").interpretation);
        assertEquals("2 + 3 * 4", instance.eval("2+3*4").interpretation);
        assertEquals("(2 + 3) * 4", instance.eval("(2+3)*4").interpretation);
        assertEquals("2 ^ 3", instance.eval("2^3").interpretation);
        assertEquals("2 ^ 3 ^ 2", instance.eval("2^3^2").interpretation);
        assertEquals("-(2 + 3)", instance.eval("-(2+3)").interpretation);
        assertEquals("(2 * x)", instance.eval("2x").interpretation);
        assertEquals("(2 * x) / (3 * x)", instance.eval("2x/3x").interpretation);
        assertNull(instance.eval("2 + 3").modifiedVariable);

        // a partial interpretation is kept when the error happens later
        Evaluator.Result failed = error(instance, "2+3)", Evaluator.ErrorType.UNEXPECTED_CLOSING_PARENTHESIS);
        assertEquals("2 + 3", failed.interpretation);
    }

    // ------------------------------------------------------------------
    // variables
    // ------------------------------------------------------------------

    /**
     * Test of addVariable and the use of the variable in expressions.
     */
    @Test
    public void testAddVariable()
    {
        MathEvaluator instance = new MathEvaluator();
        MathEvaluator returned = instance.addVariable("x", new BigDecimal("2.5"));
        assertSame(instance, returned);
        assertNumeric("5", value(instance, "x * 2"));
        assertEquals(new BigDecimal("2.5"), instance.getVariable("x"));
    }

    /**
     * Test of addVariable rejecting nulls.
     */
    @Test
    public void testAddVariableNulls()
    {
        MathEvaluator instance = new MathEvaluator();
        assertThrows(NullPointerException.class, () -> instance.addVariable(null, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> instance.addVariable("x", null));
    }

    /**
     * Test of the reference to an unknown variable.
     */
    @Test
    public void testUnknownVariable()
    {
        MathEvaluator instance = new MathEvaluator();
        Evaluator.Result result = error(instance, "x * 2", Evaluator.ErrorType.UNDEFINED_VARIABLE);
        assertTrue(result.message.contains("x"), result.message);
        assertEquals(0, result.position);
    }

    // ------------------------------------------------------------------
    // memory
    // ------------------------------------------------------------------

    /**
     * Test that with memory the assigned variables are kept for later use.
     */
    @Test
    public void testMemoryAssignment()
    {
        MathEvaluator instance = new MathEvaluator(16, true);
        assertNumeric("30", value(instance, "x = 5 * 6"));
        assertNumeric("30", instance.getVariable("x"));
        assertNumeric("60", value(instance, "x * 2"));
        assertNumeric("900", value(instance, "x ^ 2"));
    }

    /**
     * Test that without memory the assignment returns the value but it is
     * not kept.
     */
    @Test
    public void testNoMemoryAssignment()
    {
        MathEvaluator instance = new MathEvaluator(16, false);
        assertNumeric("30", value(instance, "x = 5 * 6"));
        assertNull(instance.getVariable("x"));
        error(instance, "x * 2", Evaluator.ErrorType.UNDEFINED_VARIABLE);
    }

    /**
     * Test that an assigned variable can be overwritten and used in its own
     * assignment.
     */
    @Test
    public void testMemoryReassignment()
    {
        MathEvaluator instance = new MathEvaluator(16, true);
        instance.addVariable("x", new BigDecimal("10"));
        assertNumeric("20", value(instance, "x = x * 2"));
        assertNumeric("25", value(instance, "x + 5"));
    }

    /**
     * Test that clearVariables removes every stored variable.
     */
    @Test
    public void testClearVariables()
    {
        MathEvaluator instance = new MathEvaluator(16, true);
        value(instance, "x = 1");
        value(instance, "y = 2");
        instance.clearVariables();
        assertNull(instance.getVariable("x"));
        assertNull(instance.getVariable("y"));
        error(instance, "x + y", Evaluator.ErrorType.UNDEFINED_VARIABLE);
    }

    /**
     * Test that a variable added through addVariable is rounded to the
     * configured decimals.
     */
    @Test
    public void testAddVariableRounded()
    {
        MathEvaluator instance = new MathEvaluator(2, false);
        instance.addVariable("x", new BigDecimal("1.999"));
        assertEquals(new BigDecimal("2"), instance.getVariable("x"));
    }

    // ------------------------------------------------------------------
    // ordered variables
    // ------------------------------------------------------------------

    /**
     * Test of variables assigned and used in order.
     */
    @Test
    public void testOrderedVariables()
    {
        String[][] table = {
            { "x=35*8", "280" },                            // assignment (returns the assigned value)
            { "7*5*x+2", "9802" },                          // use the variable (35*280 + 2)
            { "y=x+1", "281" },                             // assign from another variable
            { "x*y", "78680" },                             // several variables
            { "x=x+1", "281" },                             // reassign using its own value
            { "x-y", "0" },                                 // the reassignment persists
        };
        MathEvaluator instance = new MathEvaluator(16, true);
        for (String[] row : table)
        {
            assertNumeric(row[1], value(instance, row[0]));
        }
        assertEquals(new BigDecimal("281"), instance.getVariable("x"));
        assertEquals(new BigDecimal("281"), instance.getVariable("y"));
    }
}
