/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import io.nut.base.math.matrix.BigDecimalMatrix;
import io.nut.base.math.matrix.Matrix;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MatrixSolve4x4Test
{
    @Test
    public void testSolve4x4BigDecimal()
    {
        // Solve system:
        // 2x + 3y + 4z + 5w = 10
        // 1x + 2y + 3z + 4w = 9
        // 3x + 1y + 2z + 1w = 6
        // 1x + 1y + 1z + 2w = 5
        Matrix<BigDecimal> A = BigDecimalMatrix.instance(4, 4);
        A.set(new int[][]
        {
            {2, 3, 4, 5},
            {1, 2, 3, 4},
            {3, 1, 2, 1},
            {1, 1, 1, 2}
        });

        BigDecimal[] b = new BigDecimal[4];
        b[0] = bd("10");
        b[1] = bd("9");
        b[2] = bd("6");
        b[3] = bd("5");

        BigDecimal[] x = A.solve(b);
        // Expected approximate solution
        assertEquals(2.0, x[0].doubleValue(), 1e-9);
        assertEquals(-6.0, x[1].doubleValue(), 1e-9);
        assertEquals(1.0, x[2].doubleValue(), 1e-9);
        assertEquals(4.0, x[3].doubleValue(), 1e-9);

        // Verify A * x ≈ b
        BigDecimal[] Ax = multiply(A, x);
        assertEquals(b[0].doubleValue(), Ax[0].doubleValue(), 1e-8);
        assertEquals(b[1].doubleValue(), Ax[1].doubleValue(), 1e-8);
        assertEquals(b[2].doubleValue(), Ax[2].doubleValue(), 1e-8);
        assertEquals(b[3].doubleValue(), Ax[3].doubleValue(), 1e-8);
    }

    private static BigDecimal bd(String s)
    {
        return new BigDecimal(s);
    }

    private static BigDecimal[] multiply(Matrix<BigDecimal> A, BigDecimal[] x)
    {
        int n = A.getRows();
        BigDecimal[] res = new BigDecimal[n];
        for (int i = 0; i < n; i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = 0; j < n; j++) {
                sum = sum.add(A.get(i, j).multiply(x[j]));
            }
            res[i] = sum;
        }
        return res;
    }
}
