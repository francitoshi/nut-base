/*
 * Copyright (C) 2010-2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.github.francitoshi.nut.base.image;

import java.awt.Dimension;

/**
 * Identity scaling strategy; it always returns the source size unchanged.
 *
 * @author franci
 */
public class NullScaleDimension implements ScaleDimension
{
    /** Default constructor. */
    public NullScaleDimension()
    {
    }
    /**
     * Returns the supplied size unchanged.
     *
     * @param size the source image size
     * @return the same {@link Dimension} instance
     */
    @Override
    public Dimension convert(Dimension size)
    {
        return size;
    }
}
