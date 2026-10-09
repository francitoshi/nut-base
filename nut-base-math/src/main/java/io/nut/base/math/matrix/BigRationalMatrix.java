/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.matrix;

import io.nut.base.math.BigRational;
import io.nut.base.math.alu.BigRationalALU;

/**
 * Matrix of {@link BigRational} elements.
 */
public final class BigRationalMatrix extends AbstractMatrix<BigRational>
{
    public BigRationalMatrix(int rows, int cols)
    {
        super(rows, cols, new BigRationalALU());
    }

    /**
     * Creates a matrix filled with zero.
     *
     * @param rows number of rows
     * @param cols number of columns
     * @return a new matrix
     */
    public static BigRationalMatrix instance(int rows, int cols)
    {
        return new BigRationalMatrix(rows, cols);
    }

    /**
     * Creates the {@code n x n} identity matrix.
     *
     * @param n size of the matrix
     * @return a new identity matrix
     */
    public static BigRationalMatrix identity(int n)
    {
        BigRationalMatrix m = new BigRationalMatrix(n, n);
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
    public static BigRationalMatrix zeros(int rows, int cols)
    {
        BigRationalMatrix m = new BigRationalMatrix(rows, cols);
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
    public static BigRationalMatrix ones(int rows, int cols)
    {
        BigRationalMatrix m = new BigRationalMatrix(rows, cols);
        m.fill(BigRational.ONE);
        return m;
    }

    /**
     * Creates a matrix from a two-dimensional array.
     *
     * @param values the values
     * @return a new matrix
     */
    public static BigRationalMatrix of(BigRational[][] values)
    {
        checkRectangular(values);
        BigRationalMatrix m = new BigRationalMatrix(values.length, values[0].length);
        m.set(values);
        return m;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected BigRational[] createVector(int size)
    {
        return new BigRational[size];
    }

    @Override
    protected int elementHashCode(BigRational value)
    {
        return value == null ? 0 : value.hashCode();
    }

    @Override
    protected AbstractMatrix<BigRational> create(int r, int c)
    {
        return new BigRationalMatrix(r, c);
    }

    @Override
    public Matrix<BigRational> copy()
    {
        return copyAsAbstract();
    }
}
