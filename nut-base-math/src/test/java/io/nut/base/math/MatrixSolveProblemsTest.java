/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math;

import io.nut.base.math.matrix.BigRationalMatrix;
import io.nut.base.math.matrix.InconsistentSystemException;
import io.nut.base.math.matrix.Matrix;
import io.nut.base.math.matrix.UnderdeterminedSystemException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Solves classic word problems by setting them up as linear systems.
 */
public class MatrixSolveProblemsTest
{
    @Test
    public void testChickensAndRabbits()
    {
        // x = chickens, y = rabbits
        // x + y = 70   (heads)
        // 2x + 4y = 200 (legs)
        Matrix<BigRational> A = BigRationalMatrix.instance(2, 2);
        A.set(new long[][]
        {
            {1, 1},
            {2, 4}
        });

        BigRational[] b =
        {
            BigRational.valueOf(70),
            BigRational.valueOf(200)
        };

        BigRational[] x = A.solve(b);

        assertEquals(40, x[0].intValue());
        assertEquals(30, x[1].intValue());

        // Verify the solution satisfies the original equations
        assertEquals(70, A.get(0, 0).mul(x[0]).add(A.get(0, 1).mul(x[1])).intValue());
        assertEquals(200, A.get(1, 0).mul(x[0]).add(A.get(1, 1).mul(x[1])).intValue());
    }

    @Test
    public void testFurnitureFactory()
    {
        // S = chairs, M = tables, A = cupboards
        // 2S + 3M + 6A = 140 (wood)
        // 1S + 3M + 4A = 110 (plastic)
        // 1S + 2M + 3A =  80 (aluminium)
        //
        // NOTE: the statement's 5 units of wood per cupboard make the cupboard
        // consume exactly a chair plus a table, so the system is singular and
        // inconsistent (M + A would be both 30 and 20). Using 6 units of wood
        // per cupboard gives a unique solution.
        Matrix<BigRational> A = BigRationalMatrix.instance(3, 3);
        A.set(new long[][]
        {
            {2, 3, 6},
            {1, 3, 4},
            {1, 2, 3}
        });

        BigRational[] b =
        {
            BigRational.valueOf(140),
            BigRational.valueOf(110),
            BigRational.valueOf(80)
        };

        BigRational[] x = A.solve(b);

        System.out.println("Furniture factory solution:");
        System.out.println("  Chairs (S)    = " + x[0]);
        System.out.println("  Tables (M)    = " + x[1]);
        System.out.println("  Cupboards (A) = " + x[2]);

        assertEquals(10, x[0].intValue());
        assertEquals(20, x[1].intValue());
        assertEquals(10, x[2].intValue());

        // Verify the solution satisfies the original equations
        assertEquals(140, A.get(0, 0).mul(x[0]).add(A.get(0, 1).mul(x[1])).add(A.get(0, 2).mul(x[2])).intValue());
        assertEquals(110, A.get(1, 0).mul(x[0]).add(A.get(1, 1).mul(x[1])).add(A.get(1, 2).mul(x[2])).intValue());
        assertEquals(80, A.get(2, 0).mul(x[0]).add(A.get(2, 1).mul(x[1])).add(A.get(2, 2).mul(x[2])).intValue());
    }

    @Test
    public void testInconsistentSystem()
    {
        // Original furniture statement: the cupboard consumes exactly a chair
        // plus a table, so the system is singular and inconsistent.
        Matrix<BigRational> A = BigRationalMatrix.of(new BigRational[][]{
            {new BigRational(2), new BigRational(3), new BigRational(5)},
            {new BigRational(1), new BigRational(3), new BigRational(4)},
            {new BigRational(1), new BigRational(2), new BigRational(3)}});
        BigRational[] b =
        {
            BigRational.valueOf(140),
            BigRational.valueOf(110),
            BigRational.valueOf(80)
        };

        assertEquals(2, A.rank());
        assertEquals(1, A.nullity());
        assertFalse(A.isConsistent(b));
        assertFalse(A.hasUniqueSolution(b));
        assertThrows(InconsistentSystemException.class, () -> A.solve(b));
    }

    @Test
    public void testUnderdeterminedSystem()
    {
        // x + y + z = 20 ; 2x + y + z/2 = 20 -> infinitely many solutions
        Matrix<BigRational> A = BigRationalMatrix.of(new BigRational[][]{
            {BigRational.ONE, BigRational.ONE, BigRational.ONE},
            {BigRational.TWO, BigRational.ONE, new BigRational(1, 2)}});
        BigRational[] b =
        {
            BigRational.valueOf(20),
            BigRational.valueOf(20)
        };

        assertEquals(2, A.rank());
        assertEquals(1, A.nullity());
        assertTrue(A.isConsistent(b));
        assertFalse(A.hasUniqueSolution(b));
        assertThrows(UnderdeterminedSystemException.class, () -> A.solve(b));
    }

    @Test
    public void testUniqueSystem()
    {
        Matrix<BigRational> A = BigRationalMatrix.of(new BigRational[][]{
            {BigRational.ONE, BigRational.ONE},
            {BigRational.TWO, BigRational.valueOf(4)}});
        BigRational[] b =
        {
            BigRational.valueOf(70),
            BigRational.valueOf(200)
        };

        assertEquals(2, A.rank());
        assertEquals(0, A.nullity());
        assertTrue(A.isConsistent(b));
        assertTrue(A.hasUniqueSolution(b));

        BigRational[] x = A.solve(b);
        assertEquals(40, x[0].intValue());
        assertEquals(30, x[1].intValue());
    }
}
