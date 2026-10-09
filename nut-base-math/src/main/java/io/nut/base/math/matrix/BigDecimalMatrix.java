/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.matrix;

import io.nut.base.math.alu.BigDecimalALU;
import java.math.BigDecimal;

/**
 * Matrix of {@link BigDecimal} elements.
 */
public final class BigDecimalMatrix extends AbstractMatrix<BigDecimal>
{
    public BigDecimalMatrix(int rows, int cols)
    {
        super(rows, cols, new BigDecimalALU());
    }

    /**
     * Creates a matrix filled with zero.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return a new matrix
     */
    public static BigDecimalMatrix instance(int rows, int cols)
    {
        return new BigDecimalMatrix(rows, cols);
    }

    /**
     * Creates the {@code n x n} identity matrix.
     *
     * @param n size of the matrix
     * @return a new identity matrix
     */
    public static BigDecimalMatrix identity(int n)
    {
        BigDecimalMatrix m = new BigDecimalMatrix(n, n);
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
    public static BigDecimalMatrix zeros(int rows, int cols)
    {
        BigDecimalMatrix m = new BigDecimalMatrix(rows, cols);
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
    public static BigDecimalMatrix ones(int rows, int cols)
    {
        BigDecimalMatrix m = new BigDecimalMatrix(rows, cols);
        m.fill(BigDecimal.ONE);
        return m;
    }

    /**
     * Creates a matrix from a two-dimensional array.
     *
     * @param values the values
     * @return a new matrix
     */
    public static BigDecimalMatrix of(BigDecimal[][] values)
    {
        checkRectangular(values);
        BigDecimalMatrix m = new BigDecimalMatrix(values.length, values[0].length);
        m.set(values);
        return m;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BigDecimal[] createVector(int size)
    {
        return new BigDecimal[size];
    }

    @Override
    protected int elementHashCode(BigDecimal value)
    {
        return value == null ? 0 : value.stripTrailingZeros().hashCode();
    }

    @Override
    protected AbstractMatrix<BigDecimal> create(int r, int c)
    {
        return new BigDecimalMatrix(r, c);
    }

    @Override
    public Matrix<BigDecimal> copy()
    {
        return copyAsAbstract();
    }
}
