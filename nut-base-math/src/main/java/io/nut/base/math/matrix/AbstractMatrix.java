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
import java.util.Arrays;

/**
 * Abstract base implementation for numeric matrices.
 * Stores elements in row-major order and uses an {@link ALU} for arithmetic.
 *
 * @param <N> numeric type
 */
public abstract class AbstractMatrix<N> extends Matrix<N>
{
    protected final int rows;
    protected final int cols;
    protected final N[][] data;
    protected final ALU<N> alu;

    @SuppressWarnings("unchecked")
    protected AbstractMatrix(int rows, int cols, ALU<N> alu)
    {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive");
        }
        if (alu == null) {
            throw new NullPointerException("ALU must not be null");
        }
        this.rows = rows;
        this.cols = cols;
        this.alu = alu;
        this.data = (N[][]) new Object[rows][cols];
        fill(alu.zero());
    }

    @Override
    public int getRows()
    {
        return rows;
    }

    @Override
    public int getCols()
    {
        return cols;
    }

    @Override
    public boolean isSquare()
    {
        return rows == cols;
    }

    @Override
    public N get(int row, int column)
    {
        checkIndex(row, column);
        return data[row][column];
    }

    @Override
    public void set(int row, int column, N value)
    {
        checkIndex(row, column);
        data[row][column] = value;
    }

    @SuppressWarnings("unchecked")
    public void setRow(int row, N... values)
    {
        if (row < 0 || row >= rows)
        {
            throw new IndexOutOfBoundsException("Row out of range: " + row);
        }
        if (values == null || values.length != cols)
        {
            throw new IllegalArgumentException("Expected " + cols + " values for row " + row);
        }
        System.arraycopy(values, 0, data[row], 0, cols);
    }

    public void set(int[][] values)
    {
        checkValuesDimensions(values == null ? -1 : values.length);
        for (int i = 0; i < rows; i++)
        {
            if (values[i].length != cols)
            {
                throw new IllegalArgumentException("Row " + i + " must have " + cols + " columns");
            }
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = alu.fromInt(values[i][j]);
            }
        }
    }

    public void set(long[][] values)
    {
        checkValuesDimensions(values == null ? -1 : values.length);
        for (int i = 0; i < rows; i++)
        {
            if (values[i].length != cols)
            {
                throw new IllegalArgumentException("Row " + i + " must have " + cols + " columns");
            }
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = alu.fromLong(values[i][j]);
            }
        }
    }

    public void set(float[][] values)
    {
        checkValuesDimensions(values == null ? -1 : values.length);
        for (int i = 0; i < rows; i++)
        {
            if (values[i].length != cols)
            {
                throw new IllegalArgumentException("Row " + i + " must have " + cols + " columns");
            }
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = alu.fromFloat(values[i][j]);
            }
        }
    }

    public void set(double[][] values)
    {
        checkValuesDimensions(values == null ? -1 : values.length);
        for (int i = 0; i < rows; i++)
        {
            if (values[i].length != cols)
            {
                throw new IllegalArgumentException("Row " + i + " must have " + cols + " columns");
            }
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = alu.fromDouble(values[i][j]);
            }
        }
    }

    public void set(BigInteger[][] values)
    {
        checkValuesDimensions(values == null ? -1 : values.length);
        for (int i = 0; i < rows; i++)
        {
            if (values[i].length != cols)
            {
                throw new IllegalArgumentException("Row " + i + " must have " + cols + " columns");
            }
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = alu.fromBigInteger(values[i][j]);
            }
        }
    }

    public void set(BigDecimal[][] values)
    {
        checkValuesDimensions(values == null ? -1 : values.length);
        for (int i = 0; i < rows; i++)
        {
            if (values[i].length != cols)
            {
                throw new IllegalArgumentException("Row " + i + " must have " + cols + " columns");
            }
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = alu.fromBigDecimal(values[i][j]);
            }
        }
    }

    public void set(BigRational[][] values)
    {
        checkValuesDimensions(values == null ? -1 : values.length);
        for (int i = 0; i < rows; i++)
        {
            if (values[i].length != cols)
            {
                throw new IllegalArgumentException("Row " + i + " must have " + cols + " columns");
            }
            for (int j = 0; j < cols; j++)
            {
                data[i][j] = alu.fromBigRational(values[i][j]);
            }
        }
    }

    protected void checkValuesDimensions(int r)
    {
        if (r != rows)
        {
            throw new IllegalArgumentException("Expected " + rows + " rows but got " + r);
        }
    }

    protected static void checkRectangular(Object[][] values)
    {
        if (values == null || values.length == 0)
        {
            throw new IllegalArgumentException("Values must not be null or empty");
        }
        int cols = values[0] == null ? -1 : values[0].length;
        if (cols <= 0)
        {
            throw new IllegalArgumentException("Values must have at least one column");
        }
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] == null || values[i].length != cols)
            {
                throw new IllegalArgumentException("Values must be rectangular");
            }
        }
    }

    protected void checkIndex(int row, int column)
    {
        if (row < 0 || row >= rows || column < 0 || column >= cols)
        {
            throw new IndexOutOfBoundsException("Index out of range: (" + row + "," + column + ")");
        }
    }

    public void fill(N value)
    {
        for (int i = 0; i < rows; i++) 
        {
            Arrays.fill(data[i], value);
        }
    }

    public void setDiagonal(N value)
    {
        int limit = Math.min(rows, cols);
        for (int i = 0; i < limit; i++)
        {
            data[i][i] = value;
        }
    }

    public void identity()
    {
        fill(alu.zero());
        setDiagonal(alu.one());
    }

    public void zeroMatrix()
    {
        fill(alu.zero());
    }

    public boolean isZero()
    {
        N z = alu.zero();
        for (int i = 0; i < rows; i++) 
        {
            for (int j = 0; j < cols; j++)
            {
                if (!alu.equals(data[i][j], z))
                {
                    return false;
                }
            }
        }
        return true;
    }

    protected abstract int elementHashCode(N value);

    @Override
    public N[] getRow(int row)
    {
        if (row < 0 || row >= rows)
        {
            throw new IndexOutOfBoundsException("Row out of range: " + row);
        }
        N[] result = createVector(cols);
        System.arraycopy(data[row], 0, result, 0, cols);
        return result;
    }

    @Override
    public N[] getCol(int col)
    {
        if (col < 0 || col >= cols)
        {
            throw new IndexOutOfBoundsException("Column out of range: " + col);
        }
        N[] result = createVector(rows);
        for (int i = 0; i < rows; i++)
        {
            result[i] = data[i][col];
        }
        return result;
    }

    @Override
    public void setCol(int col, N... values)
    {
        if (col < 0 || col >= cols)
        {
            throw new IndexOutOfBoundsException("Column out of range: " + col);
        }
        if (values == null || values.length != rows)
        {
            throw new IllegalArgumentException("Expected " + rows + " values for column " + col);
        }
        for (int i = 0; i < rows; i++)
        {
            data[i][col] = values[i];
        }
    }

    @Override
    public AbstractMatrix<N> subMatrix(int r0, int c0, int r1, int c1)
    {
        if (r0 < 0 || c0 < 0 || r1 > rows || c1 > cols || r0 >= r1 || c0 >= c1)
        {
            throw new IndexOutOfBoundsException("Invalid sub-matrix range: ("
                    + r0 + "," + c0 + ")-(" + r1 + "," + c1 + ")");
        }
        AbstractMatrix<N> result = create(r1 - r0, c1 - c0);
        for (int i = r0; i < r1; i++)
        {
            for (int j = c0; j < c1; j++)
            {
                result.set(i - r0, j - c0, data[i][j]);
            }
        }
        return result;
    }

    @Override
    public AbstractMatrix<N> minor(int row, int col)
    {
        if (rows < 2 || cols < 2)
        {
            throw new IllegalArgumentException("Minor requires at least 2 rows and 2 columns");
        }
        checkIndex(row, col);
        AbstractMatrix<N> result = create(rows - 1, cols - 1);
        int r = 0;
        for (int i = 0; i < rows; i++)
        {
            if (i == row)
            {
                continue;
            }
            int c = 0;
            for (int j = 0; j < cols; j++)
            {
                if (j == col)
                {
                    continue;
                }
                result.set(r, c++, data[i][j]);
            }
            r++;
        }
        return result;
    }

    @Override
    public AbstractMatrix<N> reshape(int newRows, int newCols)
    {
        if (newRows <= 0 || newCols <= 0)
        {
            throw new IllegalArgumentException("Dimensions must be positive");
        }
        if ((long) newRows * newCols != (long) rows * cols)
        {
            throw new IllegalArgumentException("Cannot reshape " + rows + "x" + cols
                    + " into " + newRows + "x" + newCols);
        }
        AbstractMatrix<N> result = create(newRows, newCols);
        int k = 0;
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(k / newCols, k % newCols, data[i][j]);
                k++;
            }
        }
        return result;
    }

    @Override
    public AbstractMatrix<N> flatten()
    {
        return reshape(1, rows * cols);
    }

    @Override
    public boolean isIdentity()
    {
        if (rows != cols)
        {
            return false;
        }
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                N expected = (i == j) ? alu.one() : alu.zero();
                if (!alu.equals(data[i][j], expected))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isSymmetric()
    {
        if (rows != cols)
        {
            return false;
        }
        for (int i = 0; i < rows; i++)
        {
            for (int j = i + 1; j < cols; j++)
            {
                if (!alu.equals(data[i][j], data[j][i]))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isDiagonal()
    {
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                if (i != j && !alu.equals(data[i][j], alu.zero()))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isUpperTriangular()
    {
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < i && j < cols; j++)
            {
                if (!alu.equals(data[i][j], alu.zero()))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isLowerTriangular()
    {
        for (int i = 0; i < rows; i++)
        {
            for (int j = i + 1; j < cols; j++)
            {
                if (!alu.equals(data[i][j], alu.zero()))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isTriangular()
    {
        return isUpperTriangular() || isLowerTriangular();
    }

    @Override
    public boolean isInvertible()
    {
        return isSquare() && !alu.equals(determinant(), alu.zero());
    }

    @Override
    public boolean isSingular()
    {
        return isSquare() && alu.equals(determinant(), alu.zero());
    }

    // Basic arithmetic

    public AbstractMatrix<N> add(Matrix<N> other)
    {
        checkSameSize(other);
        AbstractMatrix<N> result = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(i, j, alu.add(this.get(i, j), other.get(i, j)));
            }
        }
        return result;
    }

    public AbstractMatrix<N> sub(Matrix<N> other)
    {
        checkSameSize(other);
        AbstractMatrix<N> result = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(i, j, alu.sub(this.get(i, j), other.get(i, j)));
            }
        }
        return result;
    }

    public AbstractMatrix<N> mul(N scalar)
    {
        AbstractMatrix<N> result = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(i, j, alu.mul(this.get(i, j), scalar));
            }
        }
        return result;
    }

    public AbstractMatrix<N> negate()
    {
        return mul(alu.sub(alu.zero(), alu.one()));
    }

    public AbstractMatrix<N> mul(Matrix<N> other)
    {
        if (this.cols != other.getRows())
        {
            throw new IllegalArgumentException("Incompatible dimensions for multiplication");
        }
        AbstractMatrix<N> result = create(rows, other.getCols());
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < other.getCols(); j++)
            {
                N sum = alu.zero();
                for (int k = 0; k < cols; k++)
                {
                    N term = alu.mul(this.get(i, k), other.get(k, j));
                    sum = alu.add(sum, term);
                }
                result.set(i, j, sum);
            }
        }
        return result;
    }

    public AbstractMatrix<N> transpose()
    {
        AbstractMatrix<N> result = create(cols, rows);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(j, i, this.get(i, j));
            }
        }
        return result;
    }

    public AbstractMatrix<N> addScalar(N scalar)
    {
        AbstractMatrix<N> result = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(i, j, alu.add(this.get(i, j), scalar));
            }
        }
        return result;
    }

    public AbstractMatrix<N> subScalar(N scalar)
    {
        return addScalar(alu.sub(alu.zero(), scalar));
    }

    public AbstractMatrix<N> divScalar(N scalar)
    {
        if (alu.equals(scalar, alu.zero()))
        {
            throw new ArithmeticException("Division by zero");
        }
        AbstractMatrix<N> result = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(i, j, alu.div(this.get(i, j), scalar));
            }
        }
        return result;
    }

    public AbstractMatrix<N> hadamardMul(Matrix<N> other)
    {
        checkSameSize(other);
        AbstractMatrix<N> result = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result.set(i, j, alu.mul(this.get(i, j), other.get(i, j)));
            }
        }
        return result;
    }

    public AbstractMatrix<N> hadamardDiv(Matrix<N> other)
    {
        checkSameSize(other);
        AbstractMatrix<N> result = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                N b = other.get(i, j);
                if (alu.equals(b, alu.zero()))
                {
                    throw new ArithmeticException("Division by zero");
                }
                result.set(i, j, alu.div(this.get(i, j), b));
            }
        }
        return result;
    }

    public AbstractMatrix<N> pow(int power)
    {
        if (!isSquare())
        {
            throw new IllegalArgumentException("Only square matrix can be raised to power");
        }
        if (power < 0)
        {
            throw new IllegalArgumentException("Power must be non-negative");
        }
        AbstractMatrix<N> result = identityMatrix();
        AbstractMatrix<N> base = (AbstractMatrix<N>) copy();
        int p = power;
        while (p > 0)
        {
            if ((p & 1) == 1)
            {
                result = (AbstractMatrix<N>) result.mul(base);
            }
            base = (AbstractMatrix<N>) base.mul(base);
            p >>= 1;
        }
        return result;
    }

    public AbstractMatrix<N> identityMatrix()
    {
        AbstractMatrix<N> id = create(rows, rows);
        id.identity();
        return id;
    }

    protected void checkSameSize(Matrix<N> other)
    {
        if (other.getRows() != rows || other.getCols() != cols)
        {
            throw new IllegalArgumentException("Matrix dimensions must be equal");
        }
    }

    protected abstract AbstractMatrix<N> create(int r, int c);

    // Equation solving utilities
    public N determinant()
    {
        if (!isSquare())
        {
            throw new IllegalArgumentException("Determinant is defined only for square matrices");
        }
        return determinant(copyAsAbstract());
    }

    protected AbstractMatrix<N> copyAsAbstract()
    {
        AbstractMatrix<N> copy = create(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            System.arraycopy(data[i], 0, copy.data[i], 0, cols);
        }
        return copy;
    }

    protected N determinant(AbstractMatrix<N> mat)
    {
        int n = mat.getRows();
        if (n == 1)
        {
            return mat.get(0, 0);
        }
        N det = alu.zero();
        AbstractMatrix<N> temp = create(n - 1, n - 1);
        int sign = 1;
        for (int f = 0; f < n; f++)
        {
            getCofactor(mat, temp, 0, f, n);
            N term = alu.mul(mat.get(0, f), determinant(temp));
            if (sign == -1)
            {
                det = alu.sub(det, term);
            }
            else
            {
                det = alu.add(det, term);
            }
            sign = -sign;
        }
        return det;
    }

    protected void getCofactor(AbstractMatrix<N> mat, AbstractMatrix<N> temp, int p, int q, int n)
    {
        int i = 0, j = 0;
        for (int row = 0; row < n; row++)
        {
            for (int col = 0; col < n; col++)
            {
                if (row != p && col != q)
                {
                    temp.set(i, j++, mat.get(row, col));
                    if (j == n - 1)
                    {
                        j = 0;
                        i++;
                    }
                }
            }
        }
    }

    public AbstractMatrix<N> inverse()
    {
        if (!isSquare())
        {
            throw new IllegalArgumentException("Inverse is defined only for square matrices");
        }
        N det = determinant();
        if (alu.equals(det, alu.zero()))
        {
            throw new ArithmeticException("Matrix is singular, inverse does not exist");
        }
        int n = rows;
        AbstractMatrix<N> adj = adjugate();
        return adj.divScalar(det);
    }

    public AbstractMatrix<N> adjugate()
    {
        if (!isSquare())
        {
            throw new IllegalArgumentException("Adjugate is defined only for square matrices");
        }
        int n = rows;
        if (n == 1)
        {
            AbstractMatrix<N> res = create(1, 1);
            res.set(0, 0, alu.one());
            return res;
        }
        AbstractMatrix<N> adj = create(n, n);
        AbstractMatrix<N> temp = create(n - 1, n - 1);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                getCofactor(this, temp, i, j, n);
                int sign = ((i + j) % 2 == 0) ? 1 : -1;
                N cof = determinant(temp);
                if (sign == -1)
                {
                    cof = alu.sub(alu.zero(), cof);
                }
                adj.set(j, i, cof);
            }
        }
        return adj;
    }

    public AbstractMatrix<N> rowEchelonForm()
    {
        AbstractMatrix<N> mat = copyAsAbstract();
        int lead = 0;
        int r = mat.getRows();
        int c = mat.getCols();
        for (int k = 0; k < r && lead < c; k++)
        {
            int i = k;
            while (alu.equals(mat.get(i, lead), alu.zero()))
            {
                i++;
                if (i == r)
                {
                    i = k;
                    lead++;
                    if (lead == c)
                    {
                        return mat;
                    }
                }
            }
            mat.swapRows(i, k);
            N lv = mat.get(k, lead);
            if (!alu.equals(lv, alu.zero()))
            {
                for (int j = 0; j < c; j++)
                {
                    mat.set(k, j, alu.div(mat.get(k, j), lv));
                }
            }
            for (int i2 = 0; i2 < r; i2++)
            {
                if (i2 != k && !alu.equals(mat.get(i2, lead), alu.zero()))
                {
                    N factor = mat.get(i2, lead);
                    for (int j = 0; j < c; j++)
                    {
                        mat.set(i2, j, alu.sub(mat.get(i2, j), alu.mul(factor, mat.get(k, j))));
                    }
                }
            }
            lead++;
        }
        return mat;
    }

    public AbstractMatrix<N> reducedRowEchelonForm()
    {
        return rowEchelonForm();
    }

    @Override
    public int rank()
    {
        AbstractMatrix<N> rref = rowEchelonForm();
        int rank = 0;
        for (int i = 0; i < rref.rows; i++)
        {
            if (!isZeroRow(rref, i))
            {
                rank++;
            }
        }
        return rank;
    }

    @Override
    public int nullity()
    {
        return cols - rank();
    }

    private boolean isZeroRow(AbstractMatrix<N> matrix, int row)
    {
        for (int j = 0; j < matrix.cols; j++)
        {
            if (!alu.equals(matrix.data[row][j], alu.zero()))
            {
                return false;
            }
        }
        return true;
    }

    private AbstractMatrix<N> augment(N[] b)
    {
        AbstractMatrix<N> aug = create(rows, cols + 1);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                aug.set(i, j, get(i, j));
            }
            aug.set(i, cols, b[i]);
        }
        return aug;
    }

    @Override
    public boolean isConsistent(N[] b)
    {
        checkRightHandSide(b);
        return rank() == augment(b).rank();
    }

    @Override
    public boolean hasUniqueSolution(N[] b)
    {
        checkRightHandSide(b);
        return isConsistent(b) && rank() == cols;
    }

    private void checkRightHandSide(N[] b)
    {
        if (b == null || b.length != rows)
        {
            throw new IllegalArgumentException("Invalid right-hand side vector");
        }
    }

    public AbstractMatrix<N>[] luDecomposition()
    {
        if (!isSquare())
        {
            throw new IllegalArgumentException("LU decomposition requires square matrix");
        }
        int n = rows;
        AbstractMatrix<N> L = create(n, n);
        AbstractMatrix<N> U = create(n, n);
        L.identity();
        for (int i = 0; i < n; i++)
        {
            for (int k = i; k < n; k++)
            {
                N sum = alu.zero();
                for (int j = 0; j < i; j++)
                {
                    sum = alu.add(sum, alu.mul(L.get(i, j), U.get(j, k)));
                }
                U.set(i, k, alu.sub(this.get(i, k), sum));
            }
            for (int i2 = i; i2 < n; i2++)
            {
                if (alu.equals(U.get(i, i), alu.zero()))
                {
                    throw new ArithmeticException("Zero pivot encountered in LU decomposition");
                }
                N sum = alu.zero();
                for (int j = 0; j < i; j++)
                {
                    sum = alu.add(sum, alu.mul(L.get(i2, j), U.get(j, i)));
                }
                L.set(i2, i, alu.div(alu.sub(this.get(i2, i), sum), U.get(i, i)));
            }
        }
        @SuppressWarnings("unchecked")
        AbstractMatrix<N>[] res = new AbstractMatrix[2];
        res[0] = L;
        res[1] = U;
        return res;
    }

    public N[] solveLinearSystem(N[] b)
    {
        checkRightHandSide(b);
        AbstractMatrix<N> aug = augment(b);
        int rankA = rank();
        int rankAug = aug.rank();
        if (rankA < rankAug)
        {
            throw new InconsistentSystemException("Inconsistent system: no solution");
        }
        if (rankA < cols)
        {
            throw new UnderdeterminedSystemException("Underdetermined system: infinitely many solutions (nullity=" + (cols - rankA) + ")");
        }
        AbstractMatrix<N> rref = aug.rowEchelonForm();
        N[] x = createVector(cols);
        for (int i = 0; i < cols; i++)
        {
            x[i] = rref.get(i, cols);
        }
        return x;
    }

    public N[] solve(N[] b)
    {
        return solveLinearSystem(b);
    }

    protected N[] createVector(int size)
    {
        return (N[]) new Object[size];
    }

    public void swapRows(int r1, int r2)
    {
        if (r1 == r2)
        {
            return;
        }
        N[] tmp = data[r1];
        data[r1] = data[r2];
        data[r2] = tmp;
    }

    public void swapCols(int c1, int c2)
    {
        if (c1 == c2)
        {
            return;
        }
        for (int i = 0; i < rows; i++)
        {
            N tmp = data[i][c1];
            data[i][c1] = data[i][c2];
            data[i][c2] = tmp;
        }
    }

    public N trace()
    {
        if (!isSquare())
        {
            throw new IllegalArgumentException("Trace is defined only for square matrices");
        }
        N sum = alu.zero();
        for (int i = 0; i < rows; i++)
        {
            sum = alu.add(sum, get(i, i));
        }
        return sum;
    }

    public N norm1()
    {
        N maxColSum = alu.zero();
        for (int j = 0; j < cols; j++)
        {
            N colSum = alu.zero();
            for (int i = 0; i < rows; i++)
            {
                colSum = alu.add(colSum, alu.abs(get(i, j)));
            }
            if (alu.compare(colSum, maxColSum) > 0)
            {
                maxColSum = colSum;
            }
        }
        return maxColSum;
    }

    public N normInf()
    {
        N maxRowSum = alu.zero();
        for (int i = 0; i < rows; i++)
        {
            N rowSum = alu.zero();
            for (int j = 0; j < cols; j++)
            {
                rowSum = alu.add(rowSum, alu.abs(get(i, j)));
            }
            if (alu.compare(rowSum, maxRowSum) > 0)
            {
                maxRowSum = rowSum;
            }
        }
        return maxRowSum;
    }

    @Override
    public abstract Matrix<N> copy();

    @Override
    public ALU<N> getAlu()
    {
        return alu;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        AbstractMatrix<N> other = (AbstractMatrix<N>) o;
        if (rows != other.rows || cols != other.cols)
        {
            return false;
        }
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                if (!alu.equals(data[i][j], other.data[i][j]))
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 1;
        result = 31 * result + rows;
        result = 31 * result + cols;
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                result = 31 * result + elementHashCode(data[i][j]);
            }
        }
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < rows; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append('[');
            for (int j = 0; j < cols; j++)
            {
                if (j > 0)
                {
                    sb.append(", ");
                }
                sb.append(data[i][j]);
            }
            sb.append(']');
        }
        sb.append(']');
        return sb.toString();
    }
}
