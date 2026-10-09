/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.matrix;

import io.nut.base.math.alu.BigIntegerALU;
import java.math.BigInteger;

/**
 * Matrix of {@link BigInteger} elements.
 */
public final class BigIntegerMatrix extends AbstractMatrix<BigInteger>
{
    public BigIntegerMatrix(int rows, int cols)
    {
        super(rows, cols, new BigIntegerALU());
    }

    /**
     * Creates a matrix filled with zero.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return a new matrix
     */
    public static BigIntegerMatrix instance(int rows, int cols)
    {
        return new BigIntegerMatrix(rows, cols);
    }

    /**
     * Creates the {@code n x n} identity matrix.
     *
     * @param n size of the matrix
     * @return a new identity matrix
     */
    public static BigIntegerMatrix identity(int n)
    {
        BigIntegerMatrix m = new BigIntegerMatrix(n, n);
        m.identity();
        return m;
    }

    /**
     * Creates a zero matrix.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return a new zero matrix
     */
    public static BigIntegerMatrix zeros(int rows, int cols)
    {
        BigIntegerMatrix m = new BigIntegerMatrix(rows, cols);
        m.zeroMatrix();
        return m;
    }

    /**
     * Creates a matrix filled with one.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return a new matrix filled with one
     */
    public static BigIntegerMatrix ones(int rows, int cols)
    {
        BigIntegerMatrix m = new BigIntegerMatrix(rows, cols);
        m.fill(BigInteger.ONE);
        return m;
    }

    /**
     * Creates a matrix from a two-dimensional array.
     *
     * @param values the values
     * @return a new matrix
     */
    public static BigIntegerMatrix of(BigInteger[][] values)
    {
        checkRectangular(values);
        BigIntegerMatrix m = new BigIntegerMatrix(values.length, values[0].length);
        m.set(values);
        return m;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BigInteger[] createVector(int size)
    {
        return new BigInteger[size];
    }

    @Override
    protected int elementHashCode(BigInteger value)
    {
        return value == null ? 0 : value.hashCode();
    }

    @Override
    protected AbstractMatrix<BigInteger> create(int r, int c)
    {
        return new BigIntegerMatrix(r, c);
    }

    @Override
    public Matrix<BigInteger> copy()
    {
        return copyAsAbstract();
    }
}
