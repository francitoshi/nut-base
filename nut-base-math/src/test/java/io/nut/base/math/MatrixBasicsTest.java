/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import io.nut.base.math.matrix.BigDecimalMatrix;
import io.nut.base.math.matrix.BigIntegerMatrix;
import io.nut.base.math.matrix.BigRationalMatrix;
import io.nut.base.math.matrix.Matrix;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MatrixBasicsTest
{
    private static BigDecimal bd(String s)
    {
        return new BigDecimal(s);
    }

    @Test
    public void testEqualsHashCodeToString()
    {
        Matrix<BigDecimal> a = BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2")},
            {bd("3"), bd("4")}});
        Matrix<BigDecimal> b = BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1.0"), bd("2.00")},
            {bd("3"), bd("4")}});

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, BigDecimalMatrix.identity(2));
        assertEquals("[[1, 2], [3, 4]]", a.toString());
    }

    @Test
    public void testRowAndColumnAccess()
    {
        Matrix<BigDecimal> a = BigDecimalMatrix.instance(3, 3);
        a.set(new int[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}});

        assertArrayEquals(new BigDecimal[]{bd("1"), bd("2"), bd("3")}, a.getRow(0));
        assertArrayEquals(new BigDecimal[]{bd("3"), bd("6"), bd("9")}, a.getCol(2));

        a.setCol(1, bd("10"), bd("20"), bd("30"));
        assertArrayEquals(new BigDecimal[]{bd("4"), bd("20"), bd("6")}, a.getRow(1));
    }

    @Test
    public void testSubMatrixMinorReshapeFlatten()
    {
        Matrix<BigDecimal> a = BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2"), bd("3")},
            {bd("4"), bd("5"), bd("6")},
            {bd("7"), bd("8"), bd("9")}});

        Matrix<BigDecimal> sub = a.subMatrix(0, 1, 2, 3);
        assertEquals("[[2, 3], [5, 6]]", sub.toString());

        assertEquals("[[1, 3], [7, 9]]", a.minor(1, 1).toString());

        Matrix<BigDecimal> twoByThree = BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2"), bd("3")},
            {bd("4"), bd("5"), bd("6")}});
        Matrix<BigDecimal> reshaped = twoByThree.reshape(3, 2);
        assertEquals(BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2")},
            {bd("3"), bd("4")},
            {bd("5"), bd("6")}}), reshaped);

        assertEquals(BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2"), bd("3"), bd("4"), bd("5"), bd("6"), bd("7"), bd("8"), bd("9")}}),
                a.flatten());
    }

    @Test
    public void testPredicates()
    {
        Matrix<BigDecimal> id = BigDecimalMatrix.identity(3);
        assertTrue(id.isIdentity());
        assertTrue(id.isDiagonal());
        assertTrue(id.isSymmetric());
        assertTrue(id.isUpperTriangular());
        assertTrue(id.isLowerTriangular());
        assertTrue(id.isTriangular());
        assertTrue(id.isInvertible());
        assertFalse(id.isSingular());

        Matrix<BigDecimal> sym = BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2")},
            {bd("2"), bd("3")}});
        assertTrue(sym.isSymmetric());
        assertFalse(sym.isIdentity());

        Matrix<BigDecimal> upper = BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2")},
            {bd("0"), bd("3")}});
        assertTrue(upper.isUpperTriangular());
        assertFalse(upper.isLowerTriangular());

        Matrix<BigDecimal> singular = BigDecimalMatrix.of(new BigDecimal[][]{
            {bd("1"), bd("2")},
            {bd("2"), bd("4")}});
        assertFalse(singular.isInvertible());
        assertTrue(singular.isSingular());
    }

    @Test
    public void testStaticFactories()
    {
        assertEquals("[[0, 0], [0, 0]]", BigDecimalMatrix.zeros(2, 2).toString());
        assertEquals("[[1, 1], [1, 1]]", BigDecimalMatrix.ones(2, 2).toString());
        assertEquals("[[1, 0], [0, 1]]", BigDecimalMatrix.identity(2).toString());

        Matrix<BigInteger> bi = BigIntegerMatrix.of(new BigInteger[][]{
            {BigInteger.ONE, BigInteger.valueOf(2)},
            {BigInteger.valueOf(2), BigInteger.ONE}});
        assertEquals("[[1, 2], [2, 1]]", bi.toString());
        assertEquals(BigInteger.valueOf(2), bi.get(1, 0));

        assertEquals("[[1/1, 0/1], [0/1, 1/1]]", BigRationalMatrix.identity(2).toString());
    }
}
