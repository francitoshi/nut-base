/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.matrix;

/**
 * Thrown when a linear system is underdetermined, i.e. it has infinitely many
 * solutions.
 */
public class UnderdeterminedSystemException extends ArithmeticException
{
    private static final long serialVersionUID = 1L;

    public UnderdeterminedSystemException()
    {
        super();
    }

    public UnderdeterminedSystemException(String message)
    {
        super(message);
    }
}
