/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.alu;

import io.nut.base.math.BigRational;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ALUTest
{
    @Test
    public void testBigIntegerALU()
    {
        BigIntegerALU alu = new BigIntegerALU();

        assertEquals(BigInteger.valueOf(4), alu.sqrt(BigInteger.valueOf(16)));
        assertEquals(BigInteger.valueOf(4), alu.sqrt(BigInteger.valueOf(17)));
        assertEquals(BigInteger.ZERO, alu.sqrt(BigInteger.ZERO));
        assertEquals(BigInteger.valueOf(1000), alu.sqrt(BigInteger.valueOf(1000000)));
        assertThrows(ArithmeticException.class, () -> alu.sqrt(BigInteger.valueOf(-1)));

        assertEquals(BigInteger.valueOf(5), alu.abs(BigInteger.valueOf(-5)));
        assertTrue(alu.equals(BigInteger.valueOf(3), BigInteger.valueOf(3)));
        assertTrue(alu.equals(null, BigInteger.ZERO));
        assertTrue(alu.compare(BigInteger.valueOf(3), BigInteger.valueOf(2)) > 0);
        assertEquals(0, alu.compare(BigInteger.valueOf(2), BigInteger.valueOf(2)));
        assertTrue(alu.compare(BigInteger.valueOf(1), BigInteger.valueOf(2)) < 0);
    }

    @Test
    public void testBigDecimalALU()
    {
        BigDecimalALU alu = new BigDecimalALU();

        assertEquals(0, alu.sqrt(new BigDecimal("16")).compareTo(new BigDecimal("4")));
        assertEquals(1.4142135623730951, alu.sqrt(new BigDecimal("2")).doubleValue(), 1e-12);
        assertEquals(0, alu.sqrt(new BigDecimal("0.25")).compareTo(new BigDecimal("0.5")));
        assertThrows(ArithmeticException.class, () -> alu.sqrt(new BigDecimal("-1")));

        assertEquals(0, alu.abs(new BigDecimal("-2.5")).compareTo(new BigDecimal("2.5")));
        assertTrue(alu.equals(new BigDecimal("2.0"), new BigDecimal("2.00")));
        assertTrue(alu.compare(new BigDecimal("2.5"), new BigDecimal("2.4")) > 0);
    }

    @Test
    public void testBigRationalALU()
    {
        BigRationalALU alu = new BigRationalALU();

        assertEquals(BigRational.valueOf(4), alu.sqrt(BigRational.valueOf(16)));
        assertEquals(new BigRational(2, 3), alu.sqrt(new BigRational(4, 9)));
        assertEquals(BigRational.ZERO, alu.sqrt(BigRational.ZERO));
        assertThrows(ArithmeticException.class, () -> alu.sqrt(BigRational.valueOf(2)));

        assertEquals(BigRational.valueOf(5), alu.abs(BigRational.valueOf(-5)));
        assertTrue(alu.equals(new BigRational(1, 2), new BigRational(2, 4)));
        assertTrue(alu.compare(BigRational.valueOf(1), BigRational.ZERO) > 0);
    }
}
