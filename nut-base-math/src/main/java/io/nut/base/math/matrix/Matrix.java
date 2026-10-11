/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.matrix;

import io.nut.base.math.BigRational;
import io.nut.base.math.alu.ALU;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Interface for numeric matrices with m rows and n columns.
 * <p>
 * This interface is the public working type: clients should only reference
 * {@code Matrix<N>}, never the internal implementation. Instances are
 * obtained through the static factory methods.
 *
 * @param <N> the numeric type of matrix elements
 */
public interface Matrix<N>
{
    /**
     * Returns the number of rows in this matrix.
     *
     * @return number of rows
     */
    int getRows();

    /**
     * Returns the number of columns in this matrix.
     *
     * @return number of columns
     */
    int getCols();

    /**
     * Returns true if this matrix is square.
     *
     * @return true if square matrix
     */
    boolean isSquare();

    /**
     * Returns the element at position (row, column).
     *
     * @param row    row index (0-based)
     * @param column column index (0-based)
     * @return element value
     * @throws IndexOutOfBoundsException if indices are out of range
     */
    N get(int row, int column);

    /**
     * Sets the element at position (row, column).
     *
     * @param row    row index (0-based)
     * @param column column index (0-based)
     * @param value  value to set
     * @throws IndexOutOfBoundsException if indices are out of range
     */
    void set(int row, int column, N value);

    /**
     * Assigns the values to the row indicated by {@code row}.
     *
     * @param row    row index (0-based)
     * @param values values to assign
     * @throws IndexOutOfBoundsException if the row is out of range
     * @throws IllegalArgumentException  if the number of values does not match the columns
     */
    void setRow(int row, N... values);

    /**
     * Assigns all elements from a two-dimensional {@code int} array.
     *
     * @param values values to assign
     * @throws IllegalArgumentException if the dimensions do not match this matrix
     */
    void set(int[][] values);

    /**
     * Assigns all elements from a two-dimensional {@code long} array.
     *
     * @param values values to assign
     * @throws IllegalArgumentException if the dimensions do not match this matrix
     */
    void set(long[][] values);

    /**
     * Assigns all elements from a two-dimensional {@code float} array.
     *
     * @param values values to assign
     * @throws IllegalArgumentException if the dimensions do not match this matrix
     */
    void set(float[][] values);

    /**
     * Assigns all elements from a two-dimensional {@code double} array.
     *
     * @param values values to assign
     * @throws IllegalArgumentException if the dimensions do not match this matrix
     */
    void set(double[][] values);

    /**
     * Assigns all elements from a two-dimensional {@link BigInteger} array.
     *
     * @param values values to assign
     * @throws IllegalArgumentException if the dimensions do not match this matrix
     */
    void set(BigInteger[][] values);

    /**
     * Assigns all elements from a two-dimensional {@link BigDecimal} array.
     *
     * @param values values to assign
     * @throws IllegalArgumentException if the dimensions do not match this matrix
     */
    void set(BigDecimal[][] values);

    /**
     * Assigns all elements from a two-dimensional {@link BigRational} array.
     *
     * @param values values to assign
     * @throws IllegalArgumentException if the dimensions do not match this matrix
     */
    void set(BigRational[][] values);

    /**
     * Fills every element with the given value.
     *
     * @param value value to assign to all elements
     */
    void fill(N value);

    /**
     * Sets every diagonal element to the given value.
     *
     * @param value value to assign to the diagonal
     */
    void setDiagonal(N value);

    /**
     * Turns this matrix into the identity matrix.
     */
    void identity();

    /**
     * Turns this matrix into the zero matrix.
     */
    void zeroMatrix();

    /**
     * Returns true if every element equals zero.
     *
     * @return true if this is the zero matrix
     */
    boolean isZero();

    /**
     * Adds another matrix.
     *
     * @param other matrix to add
     * @return a new matrix with the sum
     */
    Matrix<N> add(Matrix<N> other);

    /**
     * Subtracts another matrix.
     *
     * @param other matrix to subtract
     * @return a new matrix with the difference
     */
    Matrix<N> sub(Matrix<N> other);

    /**
     * Multiplies this matrix by a scalar.
     *
     * @param scalar scalar value
     * @return a new scaled matrix
     */
    Matrix<N> mul(N scalar);

    /**
     * Negates every element of this matrix.
     *
     * @return a new negated matrix
     */
    Matrix<N> negate();

    /**
     * Multiplies this matrix by another matrix.
     *
     * @param other matrix to multiply by
     * @return a new matrix with the product
     */
    Matrix<N> mul(Matrix<N> other);

    /**
     * Returns the transpose of this matrix.
     *
     * @return a new transposed matrix
     */
    Matrix<N> transpose();

    /**
     * Adds a scalar to every element.
     *
     * @param scalar scalar value
     * @return a new matrix
     */
    Matrix<N> addScalar(N scalar);

    /**
     * Subtracts a scalar from every element.
     *
     * @param scalar scalar value
     * @return a new matrix
     */
    Matrix<N> subScalar(N scalar);

    /**
     * Divides every element by a scalar.
     *
     * @param scalar scalar value
     * @return a new matrix
     */
    Matrix<N> divScalar(N scalar);

    /**
     * Element-wise (Hadamard) multiplication.
     *
     * @param other matrix to multiply by
     * @return a new matrix
     */
    Matrix<N> hadamardMul(Matrix<N> other);

    /**
     * Element-wise (Hadamard) division.
     *
     * @param other matrix to divide by
     * @return a new matrix
     */
    Matrix<N> hadamardDiv(Matrix<N> other);

    /**
     * Raises this square matrix to a non-negative power.
     *
     * @param power exponent
     * @return a new matrix
     */
    Matrix<N> pow(int power);

    /**
     * Returns an identity matrix with the same number of rows.
     *
     * @return a new identity matrix
     */
    Matrix<N> identityMatrix();

    /**
     * Returns the determinant of this square matrix.
     *
     * @return the determinant
     */
    N determinant();

    /**
     * Returns the inverse of this square matrix.
     *
     * @return a new inverse matrix
     */
    Matrix<N> inverse();

    /**
     * Returns the adjugate of this square matrix.
     *
     * @return a new adjugate matrix
     */
    Matrix<N> adjugate();

    /**
     * Returns the row echelon form of this matrix.
     *
     * @return a new matrix
     */
    Matrix<N> rowEchelonForm();

    /**
     * Returns the reduced row echelon form of this matrix.
     *
     * @return a new matrix
     */
    Matrix<N> reducedRowEchelonForm();

    /**
     * Returns the LU decomposition of this square matrix.
     *
     * @return an array {@code [L, U]}
     */
    Matrix<N>[] luDecomposition();

    /**
     * Solves the linear system {@code A * x = b}.
     *
     * @param b right-hand side vector
     * @return the solution vector
     * @throws InconsistentSystemException if the system has no solution
     * @throws UnderdeterminedSystemException if the system has infinitely many
     * solutions
     */
    N[] solveLinearSystem(N[] b);

    /**
     * Solves the linear system {@code A * x = b}.
     *
     * @param b right-hand side vector
     * @return the solution vector
     */
    N[] solve(N[] b);

    /**
     * Returns the rank of this matrix, i.e. the number of linearly independent
     * rows (or columns).
     *
     * @return the rank
     */
    int rank();

    /**
     * Returns the nullity of this matrix, i.e. the dimension of its null space.
     * It equals the number of columns minus the rank.
     *
     * @return the nullity
     */
    int nullity();

    /**
     * Returns whether the linear system {@code A * x = b} is consistent, i.e.
     * whether it has at least one solution.
     *
     * @param b right-hand side vector
     * @return true if the system has at least one solution
     */
    boolean isConsistent(N[] b);

    /**
     * Returns whether the linear system {@code A * x = b} has exactly one
     * solution.
     *
     * @param b right-hand side vector
     * @return true if the system has a unique solution
     */
    boolean hasUniqueSolution(N[] b);

    /**
     * Swaps two rows.
     *
     * @param r1 first row index
     * @param r2 second row index
     */
    void swapRows(int r1, int r2);

    /**
     * Swaps two columns.
     *
     * @param c1 first column index
     * @param c2 second column index
     */
    void swapCols(int c1, int c2);

    /**
     * Returns the trace of this square matrix.
     *
     * @return the trace
     */
    N trace();

    /**
     * Returns the 1-norm (maximum absolute column sum) of this matrix.
     *
     * @return the 1-norm
     */
    N norm1();

    /**
     * Returns the infinity norm (maximum absolute row sum) of this matrix.
     *
     * @return the infinity norm
     */
    N normInf();

    /**
     * Returns the arithmetic logic unit used by this matrix.
     *
     * @return the ALU
     */
    ALU<N> getAlu();

    /**
     * Creates a deep copy of this matrix.
     *
     * @return new matrix with same elements
     */
    Matrix<N> copy();

    /**
     * Returns a copy of the requested row.
     *
     * @param row row index (0-based)
     * @return the row values
     * @throws IndexOutOfBoundsException if the row is out of range
     */
    N[] getRow(int row);

    /**
     * Returns a copy of the requested column.
     *
     * @param col column index (0-based)
     * @return the column values
     * @throws IndexOutOfBoundsException if the column is out of range
     */
    N[] getCol(int col);

    /**
     * Assigns the values to the column indicated by {@code col}.
     *
     * @param col    column index (0-based)
     * @param values values to assign
     * @throws IndexOutOfBoundsException if the column is out of range
     * @throws IllegalArgumentException  if the number of values does not match the rows
     */
    void setCol(int col, N... values);

    /**
     * Returns a sub-matrix using half-open ranges {@code [r0, r1)} x {@code [c0, c1)}.
     *
     * @param r0 first row (inclusive)
     * @param c0 first column (inclusive)
     * @param r1 last row (exclusive)
     * @param c1 last column (exclusive)
     * @return a new sub-matrix
     */
    Matrix<N> subMatrix(int r0, int c0, int r1, int c1);

    /**
     * Returns the matrix obtained by removing the given row and column.
     *
     * @param row row index to remove
     * @param col column index to remove
     * @return a new minor matrix
     */
    Matrix<N> minor(int row, int col);

    /**
     * Returns a new matrix with the given shape containing the same elements
     * in row-major order.
     *
     * @param newRows new number of rows
     * @param newCols new number of columns
     * @return a reshaped matrix
     */
    Matrix<N> reshape(int newRows, int newCols);

    /**
     * Returns a {@code 1 x (rows * cols)} row matrix with the same elements.
     *
     * @return a flattened matrix
     */
    Matrix<N> flatten();

    /**
     * Returns true if this matrix is the identity matrix.
     *
     * @return true if identity
     */
    boolean isIdentity();

    /**
     * Returns true if this matrix equals its transpose.
     *
     * @return true if symmetric
     */
    boolean isSymmetric();

    /**
     * Returns true if every non-diagonal element is zero.
     *
     * @return true if diagonal
     */
    boolean isDiagonal();

    /**
     * Returns true if every element below the diagonal is zero.
     *
     * @return true if upper triangular
     */
    boolean isUpperTriangular();

    /**
     * Returns true if every element above the diagonal is zero.
     *
     * @return true if lower triangular
     */
    boolean isLowerTriangular();

    /**
     * Returns true if this matrix is upper or lower triangular.
     *
     * @return true if triangular
     */
    boolean isTriangular();

    /**
     * Returns true if this matrix is square and has a non-zero determinant.
     *
     * @return true if invertible
     */
    boolean isInvertible();

    /**
     * Returns true if this matrix is square and has a zero determinant.
     *
     * @return true if singular
     */
    boolean isSingular();
}
