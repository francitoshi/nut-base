/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.math.matrix;

/**
 * Thrown when a linear system is inconsistent, i.e. it has no solution.
 */
public class InconsistentSystemException extends ArithmeticException
{
    private static final long serialVersionUID = 1L;

    public InconsistentSystemException()
    {
        super();
    }

    public InconsistentSystemException(String message)
    {
        super(message);
    }
}
